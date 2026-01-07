/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.actions.audits;


import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditOutput;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditSingleInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLockFilters;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Insert 1 or more audits (and optionally their children, auditDetails)
 **
 ** Takes care of managing the foreign key tables (auditTable, auditUser).
 **
 ** Enforces that security key values are provided, if the table has any.  Note that
 ** might mean a null is given for a particular key, but at least the key must be present.
 *******************************************************************************/
public class AuditAction extends AbstractQActionFunction<AuditInput, AuditOutput>
{
   private static final QLogger LOG = QLogger.getLogger(AuditAction.class);

   private Map<Pair<String, String>, Integer> cachedFetches = new HashMap<>();

   private static boolean warnedAboutAuditMissingTables = false;

   private static AuditActionCustomizerInterface defaultAuditActionCustomizerInterface = new AuditActionCustomizerInterface() {};


   /*******************************************************************************
    ** Execute to insert 1 audit, with no details (child records)
    *******************************************************************************/
   public static void execute(String tableName, Serializable recordId, Map<String, Serializable> securityKeyValues, String message)
   {
      execute(tableName, recordId, securityKeyValues, message, null);
   }



   /*******************************************************************************
    ** Execute to insert 1 audit, with a list of detail child records provided as just string messages
    *******************************************************************************/
   public static void executeWithStringDetails(String tableName, Serializable recordId, Map<String, Serializable> securityKeyValues, String message, List<String> detailMessages)
   {
      List<QRecord> detailRecords = null;
      if(CollectionUtils.nullSafeHasContents(detailMessages))
      {
         detailRecords = detailMessages.stream().map(m -> new QRecord().withValue("message", m)).toList();
      }
      execute(tableName, recordId, securityKeyValues, message, detailRecords);
   }



   /*******************************************************************************
    ** Execute to insert 1 audit, with a list of detail child records
    *******************************************************************************/
   public static void execute(String tableName, Serializable recordId, Map<String, Serializable> securityKeyValues, String message, List<QRecord> details)
   {
      new AuditAction().execute(new AuditInput().withAuditSingleInput(new AuditSingleInput()
         .withAuditTableName(tableName)
         .withRecordId(recordId)
         .withSecurityKeyValues(securityKeyValues)
         .withMessage(message)
         .withDetails(details)
      ));
   }



   /*******************************************************************************
    ** Simple overload that internally figures out primary key and security key values
    **
    ** Be aware - if the record doesn't have its security key values set (say it's a
    ** partial record as part of an update), then those values won't be in the
    ** security key map...  This should probably be considered a bug.
    *******************************************************************************/
   public static void appendToInput(AuditInput auditInput, QTableMetaData table, QRecord record, String auditMessage)
   {
      appendToInput(auditInput, table.getName(), record.getValue(table.getPrimaryKeyField()), getRecordSecurityKeyValues(table, record, Optional.empty()), auditMessage);
   }



   /*******************************************************************************
    ** Add 1 auditSingleInput to an AuditInput object - with no details (child records).
    *******************************************************************************/
   public static void appendToInput(AuditInput auditInput, String tableName, Serializable recordId, Map<String, Serializable> securityKeyValues, String message)
   {
      appendToInput(auditInput, tableName, recordId, securityKeyValues, message, null);
   }



   /*******************************************************************************
    ** Add 1 auditSingleInput to an AuditInput object - with a list of details (child records).
    *******************************************************************************/
   public static AuditInput appendToInput(AuditInput auditInput, String tableName, Serializable recordId, Map<String, Serializable> securityKeyValues, String message, List<QRecord> details)
   {
      if(auditInput == null)
      {
         auditInput = new AuditInput();
      }

      return auditInput.withAuditSingleInput(new AuditSingleInput()
         .withAuditTableName(tableName)
         .withRecordId(recordId)
         .withSecurityKeyValues(securityKeyValues)
         .withMessage(message)
         .withDetails(details)
      );
   }



   /*******************************************************************************
    ** For a given record, from a given table, build a map of the record's security
    ** key values.
    **
    ** If, in case, the record has null value(s), and the oldRecord is given (e.g.,
    ** for the case of an update, where the record may not have all fields set, and
    ** oldRecord should be known for doing field-diffs), then try to get the value(s)
    ** from oldRecord.
    **
    ** Currently, will leave values null if they aren't found after that.
    **
    ** An alternative could be to re-fetch the record from its source if needed...
    *******************************************************************************/
   public static Map<String, Serializable> getRecordSecurityKeyValues(QTableMetaData table, QRecord record, Optional<QRecord> oldRecord)
   {
      Map<String, Serializable> securityKeyValues = new HashMap<>();
      for(RecordSecurityLock recordSecurityLock : RecordSecurityLockFilters.filterForReadLocks(CollectionUtils.nonNullList(table.getRecordSecurityLocks())))
      {
         getRecordSecurityKeyValues(table, record, oldRecord, recordSecurityLock, securityKeyValues);
      }
      return securityKeyValues;
   }



   /***************************************************************************
    ** recursive implementation of getRecordSecurityKeyValues, for dealing with
    ** multi-locks
    ***************************************************************************/
   private static void getRecordSecurityKeyValues(QTableMetaData table, QRecord record, Optional<QRecord> oldRecord, RecordSecurityLock recordSecurityLock, Map<String, Serializable> securityKeyValues)
   {
      //////////////////////////////////////////////////////
      // special case with recursive call for multi-locks //
      //////////////////////////////////////////////////////
      if(recordSecurityLock instanceof MultiRecordSecurityLock multiRecordSecurityLock)
      {
         for(RecordSecurityLock subLock : RecordSecurityLockFilters.filterForReadLocks(CollectionUtils.nonNullList(multiRecordSecurityLock.getLocks())))
         {
            getRecordSecurityKeyValues(table, record, oldRecord, subLock, securityKeyValues);
         }

         return;
      }

      ///////////////////////////////////////////
      // by default, deal with non-multi locks //
      ///////////////////////////////////////////
      Serializable keyValue = record == null ? null : record.getValue(recordSecurityLock.getFieldName());

      if(keyValue == null && oldRecord.isPresent())
      {
         LOG.debug("Table with a securityLock, but value not found in field", logPair("table", table.getName()), logPair("field", recordSecurityLock.getFieldName()));
         keyValue = oldRecord.get().getValue(recordSecurityLock.getFieldName());
      }

      if(keyValue == null)
      {
         LOG.debug("Table with a securityLock, but value not found in field", logPair("table", table.getName()), logPair("field", recordSecurityLock.getFieldName()), logPair("oldRecordIsPresent", oldRecord.isPresent()));
      }

      securityKeyValues.put(recordSecurityLock.getSecurityKeyType(), keyValue);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public AuditOutput execute(AuditInput input)
   {
      AuditOutput auditOutput = new AuditOutput();

      if(CollectionUtils.nullSafeHasContents(input.getAuditSingleInputList()))
      {
         if(QContext.getQInstance().getTable("audit") == null)
         {
            if(!warnedAboutAuditMissingTables)
            {
               warnedAboutAuditMissingTables = true;
               LOG.info("Audit table not found, so won't be able to store audit records...");
            }
            for(AuditSingleInput auditSingleInput : CollectionUtils.nonNullList(input.getAuditSingleInputList()))
            {
               LOG.info("audit",
                  logPair("tableName", auditSingleInput.getAuditTableName()),
                  logPair("recordId", auditSingleInput.getRecordId()),
                  logPair("userName", auditSingleInput.getAuditUserName()),
                  logPair("details", auditSingleInput.getDetails()),
                  logPair("message", auditSingleInput.getMessage()));
            }
            return (auditOutput);
         }

         try
         {
            List<QRecord> auditRecords = new ArrayList<>();

            AuditActionCustomizerInterface auditActionCustomizer = getAuditActionCustomizer();

            for(AuditSingleInput auditSingleInput : CollectionUtils.nonNullList(input.getAuditSingleInputList()))
            {
               ///////////////////////////////////////////////////
               // allow customizer a chance to adjust the input //
               ///////////////////////////////////////////////////
               auditActionCustomizer.customizeInput(auditSingleInput);

               /////////////////////////////////////////
               // validate table is known in instance //
               /////////////////////////////////////////
               QTableMetaData table = QContext.getQInstance().getTable(auditSingleInput.getAuditTableName());
               if(table == null)
               {
                  throw (new QException("Requested audit for an unrecognized table name: " + auditSingleInput.getAuditTableName()));
               }

               ///////////////////////////////////////////////////////
               // validate security keys on the table are given     //
               // originally, this case threw...                    //
               // but i think it's better to record the audit, just //
               // missing its security key value, then to fail...   //
               // but, maybe should be configurable, etc...         //
               ///////////////////////////////////////////////////////
               if(!validateSecurityKeys(auditSingleInput, table))
               {
                  LOG.debug("Missing securityKeyValue in audit request", logPair("table", auditSingleInput.getAuditTableName()), logPair("auditMessage", auditSingleInput.getMessage()), logPair("recordId", auditSingleInput.getRecordId()));
               }

               ////////////////////////////////////////////////
               // map names to ids and handle default values //
               ////////////////////////////////////////////////
               Integer auditTableId = getIdForName("auditTable", auditSingleInput.getAuditTableName());
               Integer auditUserId  = getIdForName("auditUser", Objects.requireNonNullElse(auditSingleInput.getAuditUserName(), getSessionUserName()));
               Instant timestamp    = Objects.requireNonNullElse(auditSingleInput.getTimestamp(), Instant.now());

               //////////////////
               // build record //
               //////////////////
               QRecord record = new QRecord()
                  .withValue("auditTableId", auditTableId)
                  .withValue("auditUserId", auditUserId)
                  .withValue("timestamp", timestamp)
                  .withValue("message", auditSingleInput.getMessage())
                  .withValue("recordId", auditSingleInput.getRecordId());

               if(auditSingleInput.getSecurityKeyValues() != null)
               {
                  for(Map.Entry<String, Serializable> entry : auditSingleInput.getSecurityKeyValues().entrySet())
                  {
                     record.setValue(entry.getKey(), entry.getValue());
                  }
               }

               ///////////////////////////////////////////////////////////
               // allow customizer to perform adjustments to the record //
               ///////////////////////////////////////////////////////////
               auditActionCustomizer.customizeRecord(record, auditSingleInput);

               auditRecords.add(record);
            }

            /////////////////////////////
            // do a single bulk insert //
            /////////////////////////////
            InsertInput insertInput = new InsertInput();
            insertInput.setTableName("audit");
            insertInput.setTransaction(input.getTransaction());
            insertInput.setRecords(auditRecords);
            InsertOutput insertOutput = new InsertAction().execute(insertInput);

            //////////////////////////////////////////
            // now look for children (auditDetails) //
            //////////////////////////////////////////
            int           i                  = 0;
            List<QRecord> auditDetailRecords = new ArrayList<>();
            for(AuditSingleInput auditSingleInput : CollectionUtils.nonNullList(input.getAuditSingleInputList()))
            {
               Long auditId = insertOutput.getRecords().get(i++).getValueLong("id");
               if(auditId == null)
               {
                  LOG.warn("Missing an id for inserted audit - so won't be able to store its child details...");
                  continue;
               }

               for(QRecord detail : CollectionUtils.nonNullList(auditSingleInput.getDetails()))
               {
                  auditDetailRecords.add(detail.withValue("auditId", auditId));
               }
            }

            if(!auditDetailRecords.isEmpty())
            {
               insertInput = new InsertInput();
               insertInput.setTableName("auditDetail");
               insertInput.setTransaction(input.getTransaction());
               insertInput.setRecords(auditDetailRecords);
               new InsertAction().execute(insertInput);
            }
         }
         catch(Exception e)
         {
            LOG.warn("Error performing an audit", e);
         }
      }

      return (auditOutput);
   }



   /***************************************************************************
    * if the instance has a customizer on it, get an instance - else - return
    * the default no-op implementation.
    ***************************************************************************/
   private AuditActionCustomizerInterface getAuditActionCustomizer()
   {
      QCodeReference customizerCodeReference = QContext.getQInstance().getSupplementalCustomizer(AuditActionCustomizerInterface.CUSTOMIZER_TYPE);
      if(customizerCodeReference == null)
      {
         return defaultAuditActionCustomizerInterface;
      }
      else
      {
         return QCodeLoader.getAdHoc(AuditActionCustomizerInterface.class, customizerCodeReference);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static boolean validateSecurityKeys(AuditSingleInput auditSingleInput, QTableMetaData table)
   {
      boolean allAreValid = true;
      for(RecordSecurityLock recordSecurityLock : RecordSecurityLockFilters.filterForReadLocks(CollectionUtils.nonNullList(table.getRecordSecurityLocks())))
      {
         boolean lockIsValid = validateSecurityKeysForLock(auditSingleInput, recordSecurityLock);
         if(!lockIsValid)
         {
            allAreValid = false;
         }
      }

      return (allAreValid);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static boolean validateSecurityKeysForLock(AuditSingleInput auditSingleInput, RecordSecurityLock recordSecurityLock)
   {
      if(recordSecurityLock instanceof MultiRecordSecurityLock multiRecordSecurityLock)
      {
         boolean allSubLocksAreValid = true;
         boolean anySubLocksAreValid = false;
         for(RecordSecurityLock lock : RecordSecurityLockFilters.filterForReadLocks(CollectionUtils.nonNullList(multiRecordSecurityLock.getLocks())))
         {
            boolean subLockIsValid = validateSecurityKeysForLock(auditSingleInput, lock);
            if(subLockIsValid)
            {
               anySubLocksAreValid = true;
            }
            else
            {
               allSubLocksAreValid = false;
            }
         }

         if(multiRecordSecurityLock.getOperator().equals(MultiRecordSecurityLock.BooleanOperator.OR))
         {
            return (anySubLocksAreValid);
         }
         else if(multiRecordSecurityLock.getOperator().equals(MultiRecordSecurityLock.BooleanOperator.AND))
         {
            return (allSubLocksAreValid);
         }
      }
      else
      {
         if(auditSingleInput.getSecurityKeyValues() == null || !auditSingleInput.getSecurityKeyValues().containsKey(recordSecurityLock.getSecurityKeyType()))
         {
            return (false);
         }
      }

      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String getSessionUserName()
   {
      QUser user = QContext.getQSession().getUser();
      if(user == null)
      {
         return ("Unknown");
      }
      return (user.getFullName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer getIdForName(String tableName, String nameValue) throws QException
   {
      Pair<String, String> key = new Pair<>(tableName, nameValue);
      if(!cachedFetches.containsKey(key))
      {

         Integer id = fetchIdFromName(tableName, nameValue);
         if(id != null)
         {
            cachedFetches.put(key, id);
            return id;
         }

         try
         {
            LOG.debug("Inserting " + tableName + " named " + nameValue);
            InsertInput insertInput = new InsertInput();
            insertInput.setTableName(tableName);
            QRecord record = new QRecord().withValue("name", nameValue);

            if(tableName.equals("auditTable"))
            {
               QTableMetaData table = QContext.getQInstance().getTable(nameValue);
               if(table != null)
               {
                  record.setValue("label", table.getLabel());
               }
            }

            insertInput.setRecords(List.of(record));
            InsertOutput insertOutput = new InsertAction().execute(insertInput);
            id = insertOutput.getRecords().get(0).getValueInteger("id");
            if(id != null)
            {
               cachedFetches.put(key, id);
               return id;
            }
         }
         catch(Exception e)
         {
            ////////////////////////////////////////////////////////////////////
            // assume this may mean a dupe-key - so - try another fetch below //
            ////////////////////////////////////////////////////////////////////
            LOG.debug("Caught error inserting " + tableName + " named " + nameValue + " - will try to re-fetch", e);
         }

         id = fetchIdFromName(tableName, nameValue);
         if(id != null)
         {
            cachedFetches.put(key, id);
            return id;
         }

         /////////////
         // give up //
         /////////////
         throw (new QException("Unable to get id for " + tableName + " named " + nameValue));
      }

      return (cachedFetches.get(key));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer fetchIdFromName(String tableName, String nameValue) throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName(tableName);
      getInput.setUniqueKey(Map.of("name", nameValue));
      GetOutput getOutput = new GetAction().execute(getInput);
      if(getOutput.getRecord() != null)
      {
         return (getOutput.getRecord().getValueInteger("id"));
      }

      return (null);
   }

}
