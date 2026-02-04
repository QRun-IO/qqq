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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeConsumer;


/*******************************************************************************
 ** Object to connect a producer of records with a consumer.
 ** Best for those to be on different threads, to avoid deadlock.
 *******************************************************************************/
public class RecordPipe
{
   private static final QLogger LOG = QLogger.getLogger(RecordPipe.class);

   private static final long BLOCKING_SLEEP_MILLIS = 100;
   private static final int  DEFAULT_CAPACITY      = 1_000;

   private static long MAX_SLEEP_LOOP_MILLIS = 300_000; // 5 minutes

   private int capacity = DEFAULT_CAPACITY;
   private ArrayBlockingQueue<QRecord> queue = new ArrayBlockingQueue<>(capacity);

   private boolean isTerminated = false;

   private UnsafeConsumer<List<QRecord>, QException> postRecordActions = null;

   /////////////////////////////////////
   // See usage below for explanation //
   /////////////////////////////////////
   private List<QRecord> singleRecordListForPostRecordActions = new ArrayList<>();

   private int totalRecordCount = 0;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public RecordPipe()
   {

   }



   /*******************************************************************************
    ** Construct a record pipe, with an alternative capacity for the internal queue.
    **
    ** overrideCapacity is allowed to be null - in which case, DEFAULT_CAPACITY is used.
    *******************************************************************************/
   public RecordPipe(Integer overrideCapacity)
   {
      this.capacity = Objects.requireNonNullElse(overrideCapacity, DEFAULT_CAPACITY);
      queue = new ArrayBlockingQueue<>(this.capacity);
   }



   /*******************************************************************************
    ** Turn off the pipe.  Stop accepting new records (just ignore them in the add
    ** method).  Clear the existing queue.  Don't return any more records.  Note that
    ** if consumeAvailableRecords was running in another thread, it may still return
    ** some records that it read before this call.
    *******************************************************************************/
   public void terminate()
   {
      isTerminated = true;
      queue.clear();
   }



   /*******************************************************************************
    ** Add a record to the pipe.  Will block if the pipe is full.  Will noop if pipe is terminated.
    *******************************************************************************/
   public void addRecord(QRecord record) throws QException
   {
      if(isTerminated)
      {
         return;
      }

      if(postRecordActions != null)
      {
         ////////////////////////////////////////////////////////////////////////////////////
         // the initial use-case of this method is to call QueryAction.postRecordActions   //
         // that method requires that the list param be modifiable.  Originally we used    //
         // List.of here - but that is immutable, so, instead use this single-record-list  //
         // (which we'll create as a field in this class, to avoid always re-constructing) //
         ////////////////////////////////////////////////////////////////////////////////////
         singleRecordListForPostRecordActions.add(record);
         postRecordActions.run(singleRecordListForPostRecordActions);
         record = singleRecordListForPostRecordActions.remove(0);
      }

      doAddRecord(record);
      totalRecordCount++;
   }



   /*******************************************************************************
    ** Private internal version of add record - assumes the postRecordActions have
    ** already ran.
    *******************************************************************************/
   private void doAddRecord(QRecord record)
   {
      boolean offerResult = queue.offer(record);

      if(!offerResult && !isTerminated)
      {
         LOG.debug("Pipe is full.  Waiting.");
         long sleepLoopStartTime = System.currentTimeMillis();
         long now                = System.currentTimeMillis();
         while(!offerResult && !isTerminated)
         {
            if(now - sleepLoopStartTime > MAX_SLEEP_LOOP_MILLIS)
            {
               LOG.warn("Giving up adding record to pipe, due to pipe being full for more than " + MAX_SLEEP_LOOP_MILLIS + " millis");
               throw (new IllegalStateException("Giving up adding record to pipe, due to pipe staying full too long."));
            }
            LOG.trace("Record pipe.add failed (due to full pipe).  Blocking.");
            SleepUtils.sleep(BLOCKING_SLEEP_MILLIS, TimeUnit.MILLISECONDS);
            offerResult = queue.offer(record);
            now = System.currentTimeMillis();
         }
         LOG.debug("Pipe has opened up.  Resuming.");
      }
   }



   /*******************************************************************************
    ** Add a list of records to the pipe.  Will block if the pipe is full.  Will noop if pipe is terminated.
    *******************************************************************************/
   public void addRecords(List<QRecord> records) throws QException
   {
      if(postRecordActions != null)
      {
         postRecordActions.run(records);
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure to go to the private version of doAddRecord - to avoid re-running the post-actions //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      records.forEach(this::doAddRecord);
      totalRecordCount += records.size();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> consumeAvailableRecords()
   {
      List<QRecord> rs = new ArrayList<>();

      while(!isTerminated)
      {
         QRecord record = queue.poll();
         if(record == null)
         {
            break;
         }
         rs.add(record);
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public int countAvailableRecords()
   {
      if(isTerminated)
      {
         return (0);
      }

      return (queue.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setPostRecordActions(UnsafeConsumer<List<QRecord>, QException> postRecordActions)
   {
      this.postRecordActions = postRecordActions;
   }



   /*******************************************************************************
    ** Getter for capacity
    **
    *******************************************************************************/
   public int getCapacity()
   {
      return capacity;
   }



   /*******************************************************************************
    ** Getter for recordCount
    **
    *******************************************************************************/
   public int getTotalRecordCount()
   {
      return totalRecordCount;
   }
}
