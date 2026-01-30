/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.async;


import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.reporting.BufferedRecordPipe;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Class that knows how to Run an asynchronous job (lambda, supplier) that writes into a
 ** RecordPipe, with another lambda (consumer) that consumes records from the pipe.
 **
 ** Takes care of the job status monitoring, blocking when the pipe is empty, etc.
 *******************************************************************************/
public class AsyncRecordPipeLoop
{
   private static final QLogger LOG = QLogger.getLogger(AsyncRecordPipeLoop.class);

   private static final int TIMEOUT_AFTER_NO_RECORDS_MS = 10 * 60 * 1000;

   private static final int MAX_SLEEP_MS  = 1000;
   private static final int INIT_SLEEP_MS = 10;

   private Integer minRecordsToConsume = 10;
   private String  forcedJobUUID;

   private static boolean doFinalFlushInSupplierThread = System.getProperty("qqq.AsyncRecordPipeLoop.doFinalFlushInSupplierThread", "true").equalsIgnoreCase("true");


   /*******************************************************************************
    ** Run an async-record-pipe-loop.
    **
    ** @param jobName name for the async job thread
    ** @param recordLimit optionally, cancel the supplier/job after this number of records.
    *                     e.g., for a preview step.
    ** @param recordPipe constructed before this call, and used in both of the lambdas
    ** @param supplier lambda that adds records into the pipe.
    *                  e.g., a query or extract step.
    ** @param consumer lambda that consumes records from the pipe
    *                  e.g., a transform/load step.
    *******************************************************************************/
   public int run(String jobName, Integer recordLimit, RecordPipe recordPipe, UnsafeFunction<AsyncJobCallback, ? extends Serializable, QException> supplier, UnsafeSupplier<Integer, QException> consumer) throws QException
   {
      ///////////////////////////////////////////////////
      // start the extraction function as an async job //
      ///////////////////////////////////////////////////
      AsyncJobManager asyncJobManager = new AsyncJobManager();
      if(getForcedJobUUID() != null)
      {
         ////////////////////////////////////////////////////////////
         // if a forced job uuid is set, set it in the job manager //
         ////////////////////////////////////////////////////////////
         asyncJobManager.setForcedJobUUID(getForcedJobUUID());
      }

      String jobUUID = asyncJobManager.startJob(jobName, (callback) ->
         {
            Serializable output = supplier.apply(callback);

            if(doFinalFlushInSupplierThread)
            {
               if(recordPipe instanceof BufferedRecordPipe bufferedRecordPipe)
               {
                  bufferedRecordPipe.finalFlush();
               }
            }

            return (output);
         });
      LOG.debug("Started supplier job [" + jobUUID + "] for record pipe.");

      AsyncJobState  jobState       = AsyncJobState.RUNNING;
      AsyncJobStatus asyncJobStatus = null;

      int     recordCount           = 0;
      int     nextSleepMillis       = INIT_SLEEP_MS;
      long    lastReceivedRecordsAt = System.currentTimeMillis();
      long    jobStartTime          = System.currentTimeMillis();
      boolean everCalledConsumer    = false;

      ////////////////////////////////////////////////////////////////////////////
      // in case the pipe capacity has been made very small (useful in tests!), //
      // then make the minRecordsToConsume match it.                            //
      ////////////////////////////////////////////////////////////////////////////
      if(recordPipe.getCapacity() < minRecordsToConsume)
      {
         minRecordsToConsume = recordPipe.getCapacity();
      }

      while(jobState.equals(AsyncJobState.RUNNING))
      {
         if(recordPipe.countAvailableRecords() < minRecordsToConsume)
         {
            ///////////////////////////////////////////////////////////////
            // if the pipe is too empty, sleep to let the producer work. //
            // todo - smarter sleep?  like get notified vs. sleep?       //
            ///////////////////////////////////////////////////////////////
            LOG.trace("Too few records are available in the pipe. Sleeping [" + nextSleepMillis + "] ms to give producer a chance to work");
            SleepUtils.sleep(nextSleepMillis, TimeUnit.MILLISECONDS);
            nextSleepMillis = Math.min(nextSleepMillis * 2, MAX_SLEEP_MS);

            long timeSinceLastReceivedRecord = System.currentTimeMillis() - lastReceivedRecordsAt;
            if(timeSinceLastReceivedRecord > TIMEOUT_AFTER_NO_RECORDS_MS)
            {
               throw (new QException("Job appears to have stopped producing records (last record received " + timeSinceLastReceivedRecord + " ms ago)."));
            }
         }
         else
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if the pipe has records, consume them.  reset the sleep timer so if we sleep again it'll be short. //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            lastReceivedRecordsAt = System.currentTimeMillis();
            nextSleepMillis = INIT_SLEEP_MS;

            everCalledConsumer = true;
            recordCount += consumer.get();
            LOG.debug(String.format("Processed %,d records so far", recordCount));

            if(recordLimit != null && recordCount >= recordLimit)
            {
               asyncJobManager.cancelJob(jobUUID);

               ////////////////////////////////////////////////////////////////////////////////////////////////////
               // in case the extract function doesn't recognize the cancellation request,                       //
               // tell the pipe to "terminate" - meaning - flush its queue and just noop when given new records. //
               // this should prevent anyone writing to such a pipe from potentially filling & blocking.         //
               ////////////////////////////////////////////////////////////////////////////////////////////////////
               recordPipe.terminate();

               break;
            }
         }

         //////////////////////////////
         // refresh the job's status //
         //////////////////////////////
         Optional<AsyncJobStatus> optionalAsyncJobStatus = asyncJobManager.getJobStatus(jobUUID);
         if(optionalAsyncJobStatus.isEmpty())
         {
            /////////////////////////////////////////////////
            // todo - ... maybe some version of try-again? //
            /////////////////////////////////////////////////
            throw (new QException("Could not get status of job [" + jobUUID + "]"));
         }
         asyncJobStatus = optionalAsyncJobStatus.get();
         jobState = asyncJobStatus.getState();
      }

      if(!doFinalFlushInSupplierThread)
      {
         if(recordPipe instanceof BufferedRecordPipe bufferedRecordPipe)
         {
            bufferedRecordPipe.finalFlush();
         }
      }

      LOG.debug("Job [" + jobUUID + "][" + jobName + "] completed with status: " + asyncJobStatus);

      ///////////////////////////////////
      // propagate errors from the job //
      ///////////////////////////////////
      if(asyncJobStatus != null && asyncJobStatus.getState().equals(AsyncJobState.ERROR))
      {
         throw (new QException("Job failed with an error", asyncJobStatus.getCaughtException()));
      }

      ///////////////////////////////////////////////////////////////////////////////////////////
      // send the final records to the consumer                                                //
      // note - we'll only make this "final" call to the consumer if:                          //
      // - there are currently records in the pipe                                             //
      // - OR we never called the consumer (e.g., there were 0 rows produced by the supplier   //
      // This prevents cases where a consumer may get pages of records in the loop, but then   //
      // be called here post-loop w/ 0 records, and may interpret it as a sign that no records //
      // were ever supplied.                                                                   //
      ///////////////////////////////////////////////////////////////////////////////////////////
      if(recordPipe.countAvailableRecords() > 0 || !everCalledConsumer)
      {
         recordCount += consumer.get();
      }

      long endTime = System.currentTimeMillis();

      if(recordCount > 0)
      {
         LOG.debug("End of job summary", logPair("recordCount", recordCount), logPair("jobName", jobName), logPair("millis", endTime - jobStartTime), logPair("recordsPerSecond", 1000d * (recordCount / (.001d + (endTime - jobStartTime)))));
      }

      return (recordCount);
   }



   /*******************************************************************************
    ** Getter for minRecordsToConsume
    *******************************************************************************/
   public Integer getMinRecordsToConsume()
   {
      return (this.minRecordsToConsume);
   }



   /*******************************************************************************
    ** Setter for minRecordsToConsume
    *******************************************************************************/
   public void setMinRecordsToConsume(Integer minRecordsToConsume)
   {
      this.minRecordsToConsume = minRecordsToConsume;
   }



   /*******************************************************************************
    ** Fluent setter for minRecordsToConsume
    *******************************************************************************/
   public AsyncRecordPipeLoop withMinRecordsToConsume(Integer minRecordsToConsume)
   {
      this.minRecordsToConsume = minRecordsToConsume;
      return (this);
   }



   /*******************************************************************************
    ** Getter for forcedJobUUID
    *******************************************************************************/
   public String getForcedJobUUID()
   {
      return (this.forcedJobUUID);
   }



   /*******************************************************************************
    ** Setter for forcedJobUUID
    *******************************************************************************/
   public void setForcedJobUUID(String forcedJobUUID)
   {
      this.forcedJobUUID = forcedJobUUID;
   }



   /*******************************************************************************
    ** Fluent setter for forcedJobUUID
    *******************************************************************************/
   public AsyncRecordPipeLoop withForcedJobUUID(String forcedJobUUID)
   {
      this.forcedJobUUID = forcedJobUUID;
      return (this);
   }

}
