/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.backend.core.state;


import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.cluster.QNodeRoles;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Process and async job state shared through a QQQ table, so any node of a
 ** multi-node application can serve any process step, status poll or cancel
 ** (spec 2026-10-15-cluster-ha-design, section B). Works on any backend; the
 ** table is defined by defineTable and created by the application's migrations.
 **
 ** Every operation runs as the system user with no transaction of its own, so
 ** state is visible to other nodes immediately, whatever the caller's
 ** transaction does. Values are serialized by StateSerialization (gzip, and
 ** AES-GCM with a key). Entries expire after ttlHours; every node sweeps
 ** expired rows every cleanIntervalMinutes.
 **
 ** A value that cannot be serialized is kept in this node's memory instead (and
 ** logged once per class), so a process keeps working on a single node.
 *******************************************************************************/
public class TableStateProvider implements StateProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(TableStateProvider.class);

   public static final String FIELD_STATE_KEY   = "stateKey";
   public static final String FIELD_STATE_TYPE  = "stateType";
   public static final String FIELD_PAYLOAD     = "payload";
   public static final String FIELD_NODE_ID     = "nodeId";
   public static final String FIELD_CREATE_DATE = "createDate";
   public static final String FIELD_MODIFY_DATE = "modifyDate";
   public static final String FIELD_EXPIRES_AT  = "expiresAt";

   private static final Set<String> WARNED_CLASSES = ConcurrentHashMap.newKeySet();

   ////////////////////////////////////////////////////////////////////////
   // weak: QStateProviders keeps providers in a map that is weak on the   //
   // instance, and a strong reference here would keep it from collection //
   ////////////////////////////////////////////////////////////////////////
   private final WeakReference<QInstance> instance;
   private final StateProviderMetaData metaData;
   private final StateSerialization    serialization;
   private final InMemoryStateProvider  localFallback = InMemoryStateProvider.getInstance();

   private ScheduledExecutorService sweeper;



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public TableStateProvider(QInstance instance, StateProviderMetaData metaData) throws QException
   {
      this.instance = new WeakReference<>(instance);
      this.metaData = metaData;
      this.serialization = new StateSerialization(metaData);
   }



   /*******************************************************************************
    ** The state table: stateKey (primary key), stateType, payload (text), nodeId,
    ** createDate, modifyDate, expiresAt; columns are snake_case. Only holders of
    ** the table's permissions see it (the payload is opaque - and encrypted with
    ** a key); the provider reads and writes it as the system user.
    *******************************************************************************/
   public static QTableMetaData defineTable(String tableName, String backendName)
   {
      return (new QTableMetaData()
         .withName(tableName)
         .withLabel("Process State")
         .withBackendName(backendName)
         .withPrimaryKeyField(FIELD_STATE_KEY)
         .withRecordLabelFormat("%s")
         .withRecordLabelFields(FIELD_STATE_KEY)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS).withDenyBehavior(DenyBehavior.HIDDEN))
         .withField(new QFieldMetaData(FIELD_STATE_KEY, QFieldType.STRING).withBackendName("state_key").withMaxLength(100))
         .withField(new QFieldMetaData(FIELD_STATE_TYPE, QFieldType.STRING).withBackendName("state_type").withMaxLength(40))
         .withField(new QFieldMetaData(FIELD_PAYLOAD, QFieldType.TEXT).withBackendName("payload"))
         .withField(new QFieldMetaData(FIELD_NODE_ID, QFieldType.STRING).withBackendName("node_id").withMaxLength(250))
         .withField(new QFieldMetaData(FIELD_CREATE_DATE, QFieldType.DATE_TIME).withBackendName("create_date"))
         .withField(new QFieldMetaData(FIELD_MODIFY_DATE, QFieldType.DATE_TIME).withBackendName("modify_date"))
         .withField(new QFieldMetaData(FIELD_EXPIRES_AT, QFieldType.DATE_TIME).withBackendName("expires_at")));
   }



   /*******************************************************************************
    ** The stored key for a state key.
    *******************************************************************************/
   public static String storedKey(AbstractStateKey key)
   {
      if(key instanceof UUIDAndTypeStateKey uuidKey)
      {
         return (uuidKey.getStateType().name() + ":" + uuidKey.getUuid());
      }
      return ("SIMPLE:" + key.getUniqueIdentifier());
   }



   /*******************************************************************************
    ** The stored type for a state key.
    *******************************************************************************/
   private static String storedType(AbstractStateKey key)
   {
      return (key instanceof UUIDAndTypeStateKey uuidKey ? uuidKey.getStateType().name() : "SIMPLE");
   }



   /*******************************************************************************
    ** Start the expired-entry sweeper (idempotent).
    *******************************************************************************/
   public synchronized void startSweeper()
   {
      if(sweeper != null)
      {
         return;
      }
      long periodSeconds = metaData.getCleanIntervalMinutes() * 60L;
      long initialDelay  = periodSeconds / 2 + ThreadLocalRandom.current().nextLong(Math.max(1, periodSeconds / 2));
      sweeper = Executors.newSingleThreadScheduledExecutor(new PrefixedDefaultThreadFactory(this));
      sweeper.scheduleAtFixedRate(() ->
      {
         try
         {
            clean(Instant.now());
         }
         catch(Exception e)
         {
            LOG.warn("Error sweeping expired state", e);
         }
      }, initialDelay, periodSeconds, TimeUnit.SECONDS);
   }



   /*******************************************************************************
    ** Stop the sweeper.
    *******************************************************************************/
   public synchronized void stopSweeper()
   {
      if(sweeper != null)
      {
         sweeper.shutdownNow();
         sweeper = null;
      }
   }



   /*******************************************************************************
    ** Run a body as the system user, outside any caller transaction.
    *******************************************************************************/
   private <T> T asSystem(SystemBody<T> body) throws QException
   {
      Object[] result = new Object[1];
      QInstance qInstance = instance.get();
      if(qInstance == null)
      {
         throw (new QException("The instance of this state provider is gone"));
      }
      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession(qInstance), null, null), () -> result[0] = body.run());
      @SuppressWarnings("unchecked")
      T typed = (T) result[0];
      return (typed);
   }



   /*******************************************************************************
    ** A body run as the system user.
    *******************************************************************************/
   @FunctionalInterface
   private interface SystemBody<T>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      T run() throws QException;
   }



   /*******************************************************************************
    ** Store a value.
    *******************************************************************************/
   @Override
   public <T extends Serializable> void put(AbstractStateKey key, T data)
   {
      String stored = storedKey(key);
      String payload;
      try
      {
         payload = serialization.write(stored, data);
      }
      catch(QException e)
      {
         String className = data == null ? "null" : data.getClass().getName();
         if(WARNED_CLASSES.add(className))
         {
            LOG.warn("State value cannot be shared across nodes; keeping it on this node only", e, logPair("stateKey", stored), logPair("class", className));
         }
         localFallback.put(key, data);
         removeQuietly(stored);
         return;
      }

      try
      {
         Instant now     = Instant.now();
         Instant expires = now.plus(metaData.getTtlHours(), ChronoUnit.HOURS);
         String  table   = metaData.getTableName();
         asSystem(() ->
         {
            QRecord existing = new GetAction().executeForRecord(new GetInput(table).withPrimaryKey(stored));
            QRecord record = new QRecord()
               .withValue(FIELD_STATE_KEY, stored)
               .withValue(FIELD_PAYLOAD, payload)
               .withValue(FIELD_NODE_ID, QNodeRoles.nodeId())
               .withValue(FIELD_MODIFY_DATE, now)
               .withValue(FIELD_EXPIRES_AT, expires);
            if(existing != null)
            {
               new UpdateAction().execute(new UpdateInput(table).withRecord(record).withOmitDmlAudit(true));
               return (null);
            }

            record.withValue(FIELD_STATE_TYPE, storedType(key)).withValue(FIELD_CREATE_DATE, now);
            InsertOutput insertOutput = new InsertAction().execute(new InsertInput(table).withRecord(record).withOmitDmlAudit(true));
            if(CollectionUtils.nonNullList(insertOutput.getRecords()).stream().anyMatch(r -> CollectionUtils.nullSafeHasContents(r.getErrors())))
            {
               ///////////////////////////////////////////////////////////
               // another writer inserted first (same key): update it  //
               ///////////////////////////////////////////////////////////
               new UpdateAction().execute(new UpdateInput(table).withRecord(record).withOmitDmlAudit(true));
            }
            return (null);
         });
         localFallback.remove(key);
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////
         // never fail the user's process because the shared store had a //
         // problem: keep the value on this node, and say so loudly      //
         //////////////////////////////////////////////////////////////////
         LOG.error("Could not write shared state; keeping it on this node only", e, logPair("stateKey", stored));
         localFallback.put(key, data);
      }
   }



   /*******************************************************************************
    ** Read a value.
    *******************************************************************************/
   @Override
   public <T extends Serializable> Optional<T> get(Class<? extends T> type, AbstractStateKey key)
   {
      String stored = storedKey(key);
      try
      {
         QRecord record = asSystem(() -> new GetAction().executeForRecord(new GetInput(metaData.getTableName()).withPrimaryKey(stored)));
         if(record != null)
         {
            Instant expires = record.getValueInstant(FIELD_EXPIRES_AT);
            if(expires != null && expires.isBefore(Instant.now()))
            {
               return (Optional.empty());
            }
            Serializable value = serialization.read(stored, record.getValueString(FIELD_PAYLOAD));
            return (Optional.ofNullable(type.cast(value)));
         }
      }
      catch(ClassCastException e)
      {
         throw (new RuntimeException("Stored state value could not be cast to desired type", e));
      }
      catch(Exception e)
      {
         LOG.warn("Could not read shared state", e, logPair("stateKey", stored));
      }

      return (localFallback.get(type, key));
   }



   /*******************************************************************************
    ** Remove a value.
    *******************************************************************************/
   @Override
   public void remove(AbstractStateKey key)
   {
      localFallback.remove(key);
      removeQuietly(storedKey(key));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void removeQuietly(String stored)
   {
      try
      {
         asSystem(() -> new DeleteAction().execute(new DeleteInput(metaData.getTableName()).withPrimaryKey(stored).withOmitDmlAudit(true)));
      }
      catch(Exception e)
      {
         LOG.warn("Could not remove shared state", e, logPair("stateKey", stored));
      }
   }



   /*******************************************************************************
    ** Status text.
    *******************************************************************************/
   @Override
   public String status()
   {
      return ("TableStateProvider table: " + metaData.getTableName() + ", encrypted: " + serialization.isEncrypted());
   }



   /*******************************************************************************
    ** Delete entries that expired before now (the argument is ignored: expiry is
    ** per entry).
    *******************************************************************************/
   @Override
   public void clean(Instant startTime)
   {
      try
      {
         DeleteOutput output = asSystem(() -> new DeleteAction().execute(new DeleteInput(metaData.getTableName())
            .withQueryFilter(new QQueryFilter(new QFilterCriteria(FIELD_EXPIRES_AT, QCriteriaOperator.LESS_THAN, Instant.now())))
            .withOmitDmlAudit(true)));
         if(output != null && output.getDeletedRecordCount() > 0)
         {
            LOG.info("Swept expired shared state", logPair("deleted", output.getDeletedRecordCount()));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Could not sweep expired shared state", e);
      }
   }



   /*******************************************************************************
    ** Whether stored values are encrypted (a state encryption key is configured).
    *******************************************************************************/
   public boolean isEncrypted()
   {
      return (serialization.isEncrypted());
   }



   /*******************************************************************************
    ** The meta data.
    *******************************************************************************/
   public StateProviderMetaData getMetaData()
   {
      return (metaData);
   }
}
