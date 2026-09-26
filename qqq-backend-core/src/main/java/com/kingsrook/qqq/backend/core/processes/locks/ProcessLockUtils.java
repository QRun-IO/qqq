/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.locks;


import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility class for working with ProcessLock table - creating, checking-in,
 ** and releasing process locks.
 *******************************************************************************/
public class ProcessLockUtils
{
   private static final QLogger LOG = QLogger.getLogger(ProcessLockUtils.class);

   private static Memoization<String, ProcessLockType> getProcessLockTypeByNameMemoization = new Memoization<String, ProcessLockType>()
      .withTimeout(Duration.ofHours(1))
      .withMayStoreNullValues(false);

   private static Memoization<Integer, ProcessLockType> getProcessLockTypeByIdMemoization = new Memoization<Integer, ProcessLockType>()
      .withTimeout(Duration.ofHours(1))
      .withMayStoreNullValues(false);



   /*******************************************************************************
    ** try to create a process lock, of a given key & type - but immediately fail
    ** if the lock already exists.
    **
    ** @param key along with typeName, part of Unique Key for the lock.
    ** @param typeName along with key, part of Unique Key for the lock.  Must be a
    *              defined lock type, from which we derive defaultExpirationSeconds.
    ** @param details advice to show users re: who/what created the lock.
    *******************************************************************************/
   public static ProcessLock create(String key, String typeName, String details) throws UnableToObtainProcessLockException, QException
   {
      Map<String, ProcessLockOrException> locks = createMany(List.of(key), typeName, details);
      return getProcessLockOrThrow(key, locks);
   }



   /*******************************************************************************
    ** try to create a process lock, of a given key & type - and re-try if it failed.
    ** (e.g., wait until existing lock holder releases the lock).
    **
    ** @param key along with typeName, part of Unique Key for the lock.
    ** @param typeName along with key, part of Unique Key for the lock.  Must be a
    *              defined lock type, from which we derive defaultExpirationSeconds.
    ** @param details advice to show users re: who/what created the lock.
    ** @param sleepBetweenTries how long to sleep between retries.
    ** @param maxWait max amount of that will be waited between call to this method
    *                 and an eventual UnableToObtainProcessLockException (plus or minus
    *                 one sleepBetweenTries (actually probably just plus that).
    **
    *******************************************************************************/
   public static ProcessLock create(String key, String typeName, String details, Duration sleepBetweenTries, Duration maxWait) throws UnableToObtainProcessLockException, QException
   {
      Map<String, ProcessLockOrException> locks = createMany(List.of(key), typeName, details, sleepBetweenTries, maxWait);
      return getProcessLockOrThrow(key, locks);
   }



   /***************************************************************************
    ** For the single-lock versions of create, either return the lock identified by
    ** key, or throw.
    ***************************************************************************/
   private static ProcessLock getProcessLockOrThrow(String key, Map<String, ProcessLockOrException> locks) throws UnableToObtainProcessLockException
   {
      if(locks.get(key) != null && locks.get(key).processLock() != null)
      {
         return (locks.get(key).processLock());
      }
      else if(locks.get(key) != null && locks.get(key).unableToObtainProcessLockException() != null)
      {
         throw (locks.get(key).unableToObtainProcessLockException());
      }
      else
      {
         throw (new UnableToObtainProcessLockException("Missing key [" + key + "] in response from request to create lock.  Lock not created."));
      }
   }



   /*******************************************************************************
    ** try to create many process locks, of list of keys & a type - but immediately
    ** fail (on a one-by-one basis) if the lock already exists.
    **
    ** @param keys along with typeName, part of Unique Key for the lock.
    ** @param typeName along with key, part of Unique Key for the lock.  Must be a
    *              defined lock type, from which we derive defaultExpirationSeconds.
    ** @param details advice to show users re: who/what created the lock.
    *******************************************************************************/
   public static Map<String, ProcessLockOrException> createMany(List<String> keys, String typeName, String details) throws QException
   {
      Map<String, ProcessLockOrException> rs = new HashMap<>();

      ProcessLockType lockType = getProcessLockTypeByName(typeName);
      if(lockType == null)
      {
         throw (new QException("Unrecognized process lock type: " + typeName));
      }

      QSession qSession = QContext.getQSession();

      Instant           now                      = Instant.now();
      Integer           defaultExpirationSeconds = lockType.getDefaultExpirationSeconds();
      List<ProcessLock> processLocksToInsert     = new ArrayList<>();

      Function<String, ProcessLock> constructProcessLockFromKey = (key) ->
      {
         ProcessLock processLock = new ProcessLock()
            .withKey(key)
            .withProcessLockTypeId(lockType.getId())
            .withSessionUUID(ObjectUtils.tryAndRequireNonNullElse(() -> qSession.getUuid(), null))
            .withUserId(ObjectUtils.tryAndRequireNonNullElse(() -> qSession.getUser().getIdReference(), null))
            .withDetails(details)
            .withCheckInTimestamp(now);

         if(defaultExpirationSeconds != null)
         {
            processLock.setExpiresAtTimestamp(now.plusSeconds(defaultExpirationSeconds));
         }

         return (processLock);
      };

      for(String key : keys)
      {
         processLocksToInsert.add(constructProcessLockFromKey.apply(key));
      }

      Map<String, ProcessLockOrException> insertResultMap = tryToInsertMany(processLocksToInsert);

      ////////////////////////////////////////
      // look at which (if any) keys failed //
      ////////////////////////////////////////
      Set<String> failedKeys = new HashSet<>();
      for(Map.Entry<String, ProcessLockOrException> entry : insertResultMap.entrySet())
      {
         if(entry.getValue().unableToObtainProcessLockException() != null)
         {
            failedKeys.add(entry.getKey());
         }
      }

      //////////////////////////////////////////////////////////////////////
      // if any keys failed, try to get the existing locks for those keys //
      //////////////////////////////////////////////////////////////////////
      Map<String, QRecord> existingLockRecords = new HashMap<>();
      if(CollectionUtils.nullSafeHasContents(failedKeys))
      {
         QueryOutput queryOutput = new QueryAction().execute(new QueryInput(ProcessLock.TABLE_NAME).withFilter(new QQueryFilter()
            .withCriteria("processLockTypeId", QCriteriaOperator.EQUALS, lockType.getId())
            .withCriteria("key", QCriteriaOperator.IN, failedKeys)));
         for(QRecord record : queryOutput.getRecords())
         {
            existingLockRecords.put(record.getValueString("key"), record);
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // loop over results from insert call - either adding successes to the output structure, or adding details about failures, //
      // OR - deleting expired locks and trying a second insert!                                                                 //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<Serializable>       deleteIdList           = new ArrayList<>();
      List<ProcessLock>        tryAgainList           = new ArrayList<>();
      Map<String, String>      existingLockDetailsMap = new HashMap<>();
      Map<String, ProcessLock> existingLockMap        = new HashMap<>();
      for(Map.Entry<String, ProcessLockOrException> entry : insertResultMap.entrySet())
      {
         String      key         = entry.getKey();
         ProcessLock processLock = entry.getValue().processLock();

         //////////////////////////////////////////////////////////////////////////
         // if inserting failed... see if we found an existing lock for this key //
         //////////////////////////////////////////////////////////////////////////
         StringBuilder existingLockDetails = new StringBuilder();
         ProcessLock   existingLock        = null;

         if(processLock != null)
         {
            rs.put(key, new ProcessLockOrException(processLock));
         }
         else
         {
            QRecord existingLockRecord = existingLockRecords.get(key);
            if(existingLockRecord != null)
            {
               existingLock = new ProcessLock(existingLockRecord);
               if(StringUtils.hasContent(existingLock.getUserId()))
               {
                  existingLockDetails.append("Held by: ").append(existingLock.getUserId());
               }

               if(StringUtils.hasContent(existingLock.getDetails()))
               {
                  existingLockDetails.append("; with details: ").append(existingLock.getDetails());
               }

               Instant expiresAtTimestamp = existingLock.getExpiresAtTimestamp();
               if(expiresAtTimestamp != null)
               {
                  ZonedDateTime zonedExpiresAt = expiresAtTimestamp.atZone(ValueUtils.getSessionOrInstanceZoneId());
                  existingLockDetails.append("; expiring at: ").append(QValueFormatter.formatDateTimeWithZone(zonedExpiresAt));
               }

               existingLockDetailsMap.put(key, existingLockDetails.toString());
               existingLockMap.put(key, existingLock);

               if(expiresAtTimestamp != null && expiresAtTimestamp.isBefore(now))
               {
                  /////////////////////////////////////////////////////////////////////////////////
                  // if existing lock has expired, then we can delete it and try to insert again //
                  /////////////////////////////////////////////////////////////////////////////////
                  LOG.info("Existing lock has expired - deleting it and trying again.", logPair("id", existingLock.getId()),
                     logPair("key", key), logPair("type", typeName), logPair("details", details), logPair("expiresAtTimestamp", expiresAtTimestamp));
                  deleteIdList.add(existingLock.getId());
                  tryAgainList.add(constructProcessLockFromKey.apply(key));
               }
            }
            else
            {
               ///////////////////////////////////////////////////////////////////////////////
               // if existing lock doesn't exist now (e.g., it was deleted before the UC    //
               // check failed and when we looked for it), then just try to insert it again //
               ///////////////////////////////////////////////////////////////////////////////
               tryAgainList.add(constructProcessLockFromKey.apply(key));
            }
         }
      }

      /////////////////////////////////////////////////////
      // if there are expired locks to delete, do so now //
      /////////////////////////////////////////////////////
      if(!deleteIdList.isEmpty())
      {
         new DeleteAction().execute(new DeleteInput(ProcessLock.TABLE_NAME).withPrimaryKeys(deleteIdList));
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if there are any to try again (either because we just deleted their now-expired locks, or because we otherwise couldn't find their locks, do so now //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(!tryAgainList.isEmpty())
      {
         Map<String, ProcessLockOrException> tryAgainResult = tryToInsertMany(tryAgainList);
         for(Map.Entry<String, ProcessLockOrException> entry : tryAgainResult.entrySet())
         {
            String                             key                                = entry.getKey();
            ProcessLock                        processLock                        = entry.getValue().processLock();
            UnableToObtainProcessLockException unableToObtainProcessLockException = entry.getValue().unableToObtainProcessLockException();

            if(processLock != null)
            {
               rs.put(key, new ProcessLockOrException(processLock));
            }
            else
            {
               rs.put(key, new ProcessLockOrException(Objects.requireNonNullElseGet(unableToObtainProcessLockException, () -> new UnableToObtainProcessLockException("Process lock not created, but no details available."))));
            }
         }
      }

      ////////////////////////////////////////////////////////////////////
      // put anything not successfully created into result map as error //
      ////////////////////////////////////////////////////////////////////
      for(ProcessLock processLock : processLocksToInsert)
      {
         String key = processLock.getKey();
         if(rs.containsKey(key))
         {
            LOG.info("Created process lock", logPair("id", processLock.getId()),
               logPair("key", key), logPair("type", typeName), logPair("details", details), logPair("expiresAtTimestamp", processLock.getExpiresAtTimestamp()));
         }
         else
         {
            if(existingLockDetailsMap.containsKey(key))
            {
               rs.put(key, new ProcessLockOrException(new UnableToObtainProcessLockException("A Process Lock already exists for key [" + key + "] of type [" + typeName + "], " + existingLockDetailsMap.get(key))
                  .withExistingLock(existingLockMap.get(key))));
            }
            else
            {
               rs.put(key, new ProcessLockOrException(new UnableToObtainProcessLockException("Process lock for key [" + key + "] of type [" + typeName + "] was not created...")));
            }
         }
      }

      return (rs);
   }



   /*******************************************************************************
    ** Try to do an insert - noting that an exception from the InsertAction will be
    ** caught in here, and placed in the records as an Error!
    *******************************************************************************/
   private static Map<String, ProcessLockOrException> tryToInsertMany(List<ProcessLock> processLocks)
   {
      Map<String, ProcessLockOrException> rs = new HashMap<>();

      try
      {
         List<QRecord> insertedRecords = new InsertAction().execute(new InsertInput(ProcessLock.TABLE_NAME).withRecordEntities(processLocks)).getRecords();
         for(QRecord insertedRecord : insertedRecords)
         {
            String key = insertedRecord.getValueString("key");
            if(CollectionUtils.nullSafeHasContents(insertedRecord.getErrors()))
            {
               rs.put(key, new ProcessLockOrException(new UnableToObtainProcessLockException(insertedRecord.getErrors().get(0).getMessage())));
            }
            else
            {
               rs.put(key, new ProcessLockOrException(new ProcessLock(insertedRecord)));
            }
         }
      }
      catch(Exception e)
      {
         for(ProcessLock processLock : processLocks)
         {
            rs.put(processLock.getKey(), new ProcessLockOrException(new UnableToObtainProcessLockException("Error attempting to insert process lock: " + e.getMessage())));
         }
      }

      return (rs);
   }



   /*******************************************************************************
    ** try to create many process locks, of a given list of key & a type - and re-try
    ** upon failures (e.g., wait until existing lock holder releases the lock).
    **
    ** @param keys along with typeName, part of Unique Key for the lock.
    ** @param typeName along with key, part of Unique Key for the lock.  Must be a
    *              defined lock type, from which we derive defaultExpirationSeconds.
    ** @param details advice to show users re: who/what created the lock.
    ** @param sleepBetweenTries how long to sleep between retries.
    ** @param maxWait max amount of that will be waited between call to this method
    *                 and an eventual UnableToObtainProcessLockException (plus or minus
    *                 one sleepBetweenTries (actually probably just plus that).
    **
    *******************************************************************************/
   public static Map<String, ProcessLockOrException> createMany(List<String> keys, String typeName, String details, Duration sleepBetweenTries, Duration maxWait) throws QException
   {
      Map<String, ProcessLockOrException>             rs                   = new HashMap<>();
      Map<String, UnableToObtainProcessLockException> lastExceptionsPerKey = new HashMap<>();
      Set<String>                                     stillNeedCreated     = new HashSet<>(keys);

      Instant giveUpTime = Instant.now().plus(maxWait);

      UnableToObtainProcessLockException lastCaughtUnableToObtainProcessLockException = null;
      while(true)
      {
         Map<String, ProcessLockOrException> createManyResult = createMany(stillNeedCreated.size() == keys.size() ? keys : new ArrayList<>(stillNeedCreated), typeName, details);
         for(Map.Entry<String, ProcessLockOrException> entry : createManyResult.entrySet())
         {
            String                 key                    = entry.getKey();
            ProcessLockOrException processLockOrException = entry.getValue();
            if(processLockOrException.processLock() != null)
            {
               rs.put(key, processLockOrException);
               stillNeedCreated.remove(key);
            }
            else if(processLockOrException.unableToObtainProcessLockException() != null)
            {
               lastExceptionsPerKey.put(key, processLockOrException.unableToObtainProcessLockException());
            }
         }

         if(stillNeedCreated.isEmpty())
         {
            //////////////////////////////////////////////////////////
            // if they've all been created now, great, return them! //
            //////////////////////////////////////////////////////////
            return (rs);
         }

         /////////////////////////////////////////////////////////////////////////////
         // oops, let's sleep (if we're before the give up time) and then try again //
         /////////////////////////////////////////////////////////////////////////////
         if(Instant.now().plus(sleepBetweenTries).isBefore(giveUpTime))
         {
            SleepUtils.sleep(sleepBetweenTries);
         }
         else
         {
            /////////////////////////////////
            // else, break if out of time! //
            /////////////////////////////////
            break;
         }
      }

      ////////////////////////////////////////////////////////////////////////////////////////////
      // any that didn't get created, they need their last error (or a new error) put in the rs //
      ////////////////////////////////////////////////////////////////////////////////////////////
      for(String key : stillNeedCreated)
      {
         rs.put(key, new ProcessLockOrException(lastExceptionsPerKey.getOrDefault(key, new UnableToObtainProcessLockException("Missing key [" + key + "] in response from request to create lock.  Lock not created."))));
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ProcessLock getById(Integer id) throws QException
   {
      if(id == null)
      {
         return (null);
      }

      QRecord existingLockRecord = new GetAction().executeForRecord(new GetInput(ProcessLock.TABLE_NAME).withPrimaryKey(id));
      if(existingLockRecord != null)
      {
         return (new ProcessLock(existingLockRecord));
      }
      return (null);
   }



   /*******************************************************************************
    ** input wrapper for an overload of the checkin method, to allow more flexibility
    ** w/ whether or not you want to update details & expiresAtTimestamp (e.g., so a
    ** null can be passed in, to mean "set it to null" vs. "don't update it").
    *******************************************************************************/
   public static class CheckInInput
   {
      private ProcessLock processLock;
      private Instant     expiresAtTimestamp         = null;
      private boolean     wasGivenExpiresAtTimestamp = false;
      private String      details                    = null;
      private boolean     wasGivenDetails            = false;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public CheckInInput(ProcessLock processLock)
      {
         this.processLock = processLock;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public CheckInInput withExpiresAtTimestamp(Instant expiresAtTimestamp)
      {
         this.expiresAtTimestamp = expiresAtTimestamp;
         this.wasGivenExpiresAtTimestamp = true;
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public CheckInInput withDetails(String details)
      {
         this.details = details;
         this.wasGivenDetails = true;
         return (this);
      }
   }



   /*******************************************************************************
    ** Do a check-in, with a specific value for the expiresAtTimestamp - which can
    ** be set to null to make it null in the lock.
    **
    ** If you don't want to specify the expiresAtTimestamp, call the overload that
    ** doesn't take the timestamp - in which case it'll either stay the same as it
    ** was, or will be set based on the type's default.
    *******************************************************************************/
   public static void checkIn(CheckInInput input)
   {
      ProcessLock processLock = input.processLock;

      try
      {
         if(processLock == null)
         {
            LOG.debug("Null processLock passed in - will not checkin.");
            return;
         }

         QRecord recordToUpdate = new QRecord()
            .withValue("id", processLock.getId())
            .withValue("checkInTimestamp", Instant.now());

         ///////////////////////////////////////////////////////////////////
         // if the input was given a details string, update the details   //
         // use boolean instead of null to know whether or not to do this //
         ///////////////////////////////////////////////////////////////////
         if(input.wasGivenDetails)
         {
            recordToUpdate.setValue("details", input.details);
         }

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the input object had an expires-at timestamp put in it, then use that value (null or otherwise) for the expires-at-timestamp //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(input.wasGivenExpiresAtTimestamp)
         {
            recordToUpdate.setValue("expiresAtTimestamp", input.expiresAtTimestamp);
         }
         else
         {
            ////////////////////////////////////////////////////////////////////////////////
            // else, do the default thing - which is, look for a default in the lock type //
            ////////////////////////////////////////////////////////////////////////////////
            ProcessLockType lockType = getProcessLockTypeById(processLock.getProcessLockTypeId());
            if(lockType != null)
            {
               Integer defaultExpirationSeconds = lockType.getDefaultExpirationSeconds();
               if(defaultExpirationSeconds != null)
               {
                  recordToUpdate.setValue("expiresAtTimestamp", Instant.now().plusSeconds(defaultExpirationSeconds));
               }
            }
         }

         new UpdateAction().execute(new UpdateInput(ProcessLock.TABLE_NAME).withRecord(recordToUpdate));
         LOG.debug("Checked in on process lock", logPair("id", processLock.getId()));
      }
      catch(Exception e)
      {
         LOG.warn("Error checking-in on process lock", e, logPair("processLockId", () -> processLock.getId()));
      }
   }



   /*******************************************************************************
    ** Do a check-in, with a specific value for the expiresAtTimestamp - which can
    ** be set to null to make it null in the lock.
    **
    ** If you don't want to specify the expiresAtTimestamp, call the overload that
    ** doesn't take the timestamp - in which case it'll either stay the same as it
    ** was, or will be set based on the type's default.
    *******************************************************************************/
   public static void checkIn(ProcessLock processLock, Instant expiresAtTimestamp)
   {
      checkIn(new CheckInInput(processLock).withExpiresAtTimestamp(expiresAtTimestamp));
   }



   /*******************************************************************************
    ** Do a check-in, updating the expires-timestamp based on the lock type's default.
    ** (or leaving it the same as it was (null or otherwise) if there is no default
    ** on the type).
    *******************************************************************************/
   public static void checkIn(ProcessLock processLock)
   {
      checkIn(new CheckInInput(processLock));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void releaseById(Integer id)
   {
      if(id == null)
      {
         LOG.debug("No ids passed in to releaseById - returning with noop");
         return;
      }

      releaseByIds(List.of(id));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void releaseByIds(List<? extends Serializable> ids)
   {
      List<Serializable> nonNullIds = ids == null ? Collections.emptyList() : ids.stream().filter(Objects::nonNull).map(o -> (Serializable) o).toList();

      if(CollectionUtils.nullSafeIsEmpty(nonNullIds))
      {
         LOG.debug("No ids passed in to releaseById - returning with noop");
         return;
      }

      try
      {
         DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(ProcessLock.TABLE_NAME).withPrimaryKeys(nonNullIds));
         if(CollectionUtils.nullSafeHasContents(deleteOutput.getRecordsWithErrors()))
         {
            throw (new QException("Error deleting processLocks: " + deleteOutput.getRecordsWithErrors().get(0).getErrorsAsString()));
         }

         LOG.info("Released process locks", logPair("ids", nonNullIds));
      }
      catch(QException e)
      {
         LOG.warn("Exception releasing processLocks byId", e, logPair("ids", ids));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void release(ProcessLock processLock)
   {
      if(processLock == null)
      {
         LOG.debug("No process lock passed in to release - returning with noop");
         return;
      }

      releaseMany(List.of(processLock));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void releaseMany(List<ProcessLock> processLocks)
   {
      if(CollectionUtils.nullSafeIsEmpty(processLocks))
      {
         LOG.debug("No process locks passed in to release - returning with noop");
         return;
      }

      List<Serializable> ids = processLocks.stream()
         .filter(Objects::nonNull)
         .map(pl -> (Serializable) pl.getId())
         .toList();
      releaseByIds(ids);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ProcessLockType getProcessLockTypeByName(String name)
   {
      Optional<ProcessLockType> result = getProcessLockTypeByNameMemoization.getResult(name, n ->
      {
         QRecord qRecord = new GetAction().executeForRecord(new GetInput(ProcessLockType.TABLE_NAME).withUniqueKey(Map.of("name", name)));

         if(qRecord != null)
         {
            return (new ProcessLockType(qRecord));
         }

         return (null);
      });

      return (result.orElse(null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ProcessLockType getProcessLockTypeById(Integer id)
   {
      Optional<ProcessLockType> result = getProcessLockTypeByIdMemoization.getResult(id, i ->
      {
         QRecord qRecord = new GetAction().executeForRecord(new GetInput(ProcessLockType.TABLE_NAME).withPrimaryKey(id));

         if(qRecord != null)
         {
            return (new ProcessLockType(qRecord));
         }

         return (null);
      });

      return (result.orElse(null));
   }
}
