/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.reporting.BufferedRecordPipe;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for AsyncRecordPipeLoop 
 *******************************************************************************/
class AsyncRecordPipeLoopTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach() throws Exception
   {
      ////////////////////////////////////////////////
      // reset the private fields manipulated below //
      ////////////////////////////////////////////////
      setPrivateStaticField(AsyncRecordPipeLoop.class, "doFinalFlushInSupplierThread", true);
      setPrivateStaticField(RecordPipe.class, "MAX_SLEEP_LOOP_MILLIS", 300_000);
   }



   /*******************************************************************************
    * This test established a baseline, where, before the finalFlush call was moved
    * to the supplier thread, that the pipe would fill up and deadlock.
    *
    * To trigger this condition, we set up some specific sizes.  These are certainly
    * not the only way things could line up to make this condition happen, but these
    * do make it happen 100% of the time, so this test and testPipeDoesNotFillAndDeadlockWithFinalFlushInSupplierThread
    * both set up the same way.
    *
    * The conditions are:
    * - Make a BufferedPipe, whose buffer size is 1002.  It'll internally wrap a
    * record pipe of size 1000.
    * - Have the supplier write (all in 1 call) 1001 records to the buffered pipe.
    * those records will fit in its buffer, and not be sent to the underlying pipe.
    * - Then the supplier job will finish, and a finalFlush will be called, to send
    * the 1001 records to the underlying pipe, which will block after putting 1000
    * records in the pipe.
    * - But - with the bug (consumer & supplier on same thread), it'll block until
    * the MAX_SLEEP_LOOP_MILLIS timeout happens.
    *******************************************************************************/
   @Test
   void testPipeDidFillAndDeadlockWithFinalFlushNotInSupplierThread() throws Exception
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure the feature flag is set to false - to demonstrate the deadlock situation that occurs //
      // also make it only block for 10 ms, to not waste CI time                                        //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      setPrivateStaticField(AsyncRecordPipeLoop.class, "doFinalFlushInSupplierThread", false);
      setPrivateStaticField(RecordPipe.class, "MAX_SLEEP_LOOP_MILLIS", 10);

      Integer            rowsToSupply = 1_001;
      BufferedRecordPipe recordPipe   = new BufferedRecordPipe(rowsToSupply + 1);
      AtomicInteger      rowsConsumed = new AtomicInteger(0);

      assertThatThrownBy(() -> new AsyncRecordPipeLoop().run("Test", null, recordPipe, (c) -> supplier(recordPipe, rowsToSupply), () -> consumer(recordPipe, rowsConsumed)))
         .isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Giving up adding record to pipe, due to pipe staying full too long");
   }



   /*******************************************************************************
    * This test, with the feature flag set to true, should successfully complete.
    *
    * It should use the same setup as testPipeDidFillAndDeadlockWithFinalFlushNotInSupplierThread,
    * but with finalFlush moved to the supplier thread, the deadlock is avoided.
    *******************************************************************************/
   @Test
   void testPipeDoesNotFillAndDeadlockWithFinalFlushInSupplierThread() throws Exception
   {
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure the feature flag is set to true - as this is the test that we expect to work successfully //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      setPrivateStaticField(AsyncRecordPipeLoop.class, "doFinalFlushInSupplierThread", true);

      Integer            rowsToSupply = 1_001;
      BufferedRecordPipe recordPipe   = new BufferedRecordPipe(rowsToSupply + 1);
      AtomicInteger      rowsConsumed = new AtomicInteger(0);

      new AsyncRecordPipeLoop().run("Test", null, recordPipe, (c) -> supplier(recordPipe, rowsToSupply), () -> consumer(recordPipe, rowsConsumed));

      assertEquals(rowsToSupply, rowsConsumed.get());
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private Serializable supplier(RecordPipe recordPipe, Integer rowsToSupply) throws QException
   {
      recordPipe.addRecords(Collections.nCopies(rowsToSupply, new QRecord()));
      return true;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private Integer consumer(RecordPipe recordPipe, AtomicInteger rowsConsumed) throws QException
   {
      SleepUtils.sleep(5, TimeUnit.MILLISECONDS);
      List<QRecord> records = recordPipe.consumeAvailableRecords();
      rowsConsumed.addAndGet(records.size());
      return records.size();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static void setPrivateStaticField(Class<?> targetClass, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException
   {
      Field field = targetClass.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(null, value);
   }

}