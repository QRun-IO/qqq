/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.utils;


import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for RecordLookupHelper
 *******************************************************************************/
class RecordLookupHelperTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
      MemoryRecordStore.resetStatistics();
      MemoryRecordStore.setCollectStatistics(true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithoutPreload() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(qInstance);
      RecordLookupHelper recordLookupHelper = new RecordLookupHelper();

      MemoryRecordStore.setCollectStatistics(true);
      assertEquals(2, recordLookupHelper.getRecordId(TestUtils.TABLE_NAME_SHAPE, "name", "Square"));
      assertEquals(2, recordLookupHelper.getRecordId(TestUtils.TABLE_NAME_SHAPE, "name", "Square", Integer.class));
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));

      assertEquals("Circle", recordLookupHelper.getRecordValue(TestUtils.TABLE_NAME_SHAPE, "name", "id", 3));
      assertEquals("Circle", recordLookupHelper.getRecordValue(TestUtils.TABLE_NAME_SHAPE, "name", "id", 3, String.class));
      assertEquals(2, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));

      assertNull(recordLookupHelper.getRecordId(TestUtils.TABLE_NAME_SHAPE, "name", "notAShape"));
      assertNull(recordLookupHelper.getRecordId(TestUtils.TABLE_NAME_SHAPE, "name", "notAShape", Integer.class));
      assertEquals(3, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithPreload() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(qInstance);

      RecordLookupHelper recordLookupHelper = new RecordLookupHelper();
      recordLookupHelper.preloadRecords(TestUtils.TABLE_NAME_SHAPE, "name");
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));

      assertNotNull(recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Triangle"));
      assertEquals(1, recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Triangle").getValueInteger("id"));
      assertEquals("Triangle", recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Triangle").getValueString("name"));
      assertEquals(2, recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Square").getValueInteger("id"));
      assertEquals("Square", recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Square").getValueString("name"));
      assertEquals(3, recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Circle").getValueInteger("id"));
      assertEquals("Circle", recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Circle").getValueString("name"));

      /////////////////////////////////////////////////////
      // all those gets should run no additional queries //
      /////////////////////////////////////////////////////
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));

      ////////////////////////////////////////////////////////////////////
      // make sure we don't re-do the query in a second call to preload //
      ////////////////////////////////////////////////////////////////////
      recordLookupHelper.preloadRecords(TestUtils.TABLE_NAME_SHAPE, "name");
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));

      ///////////////////////////////////////////////////
      // make sure we can preload by a different field //
      ///////////////////////////////////////////////////
      recordLookupHelper.preloadRecords(TestUtils.TABLE_NAME_SHAPE, "id");
      assertEquals(2, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithPreloadInListToCacheMisses() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(qInstance);

      RecordLookupHelper recordLookupHelper = new RecordLookupHelper();
      recordLookupHelper.preloadRecords(TestUtils.TABLE_NAME_SHAPE, "name", List.of("Square", "Circle"));
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));

      //////////////////////////////////////////////
      // assert we cached a record that was found //
      //////////////////////////////////////////////
      assertNotNull(recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Triangle"));
      assertEquals(1, recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Triangle").getValueInteger("id"));
      assertEquals("Triangle", recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Triangle").getValueString("name"));

      //////////////////////////////////////////////////
      // assert we cached a null for a name not found //
      //////////////////////////////////////////////////
      assertNull(recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Hexagon"));

      ////////////////////////////////////////////////
      // those gets should run 2 additional queries //
      ////////////////////////////////////////////////
      assertEquals(3, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithPreloadAndDisabledOneOffLookups() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(qInstance);

      RecordLookupHelper recordLookupHelper = new RecordLookupHelper();
      recordLookupHelper.preloadRecords(TestUtils.TABLE_NAME_SHAPE, "name", List.of("Triangle", "Square", "Octagon"));
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));

      ////////////////////////////////////////////////////////
      // this is the key thing being tested in this method. //
      ////////////////////////////////////////////////////////
      recordLookupHelper.setMayNotDoOneOffLookups(TestUtils.TABLE_NAME_SHAPE, "name");

      ////////////////////////////////////////////////////////////
      // assert we do not find a record if it was not preloaded //
      ////////////////////////////////////////////////////////////
      assertNull(recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Circle"));

      //////////////////////////////////////////////////
      // assert we cached a null for a name not found //
      //////////////////////////////////////////////////
      assertNull(recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Octagon"));

      //////////////////////////////////////////////////////////////////////////////////////////
      // assert we do not try to look up a name that we didn't rep-load, and that isn't found //
      //////////////////////////////////////////////////////////////////////////////////////////
      assertNull(recordLookupHelper.getRecordByKey(TestUtils.TABLE_NAME_SHAPE, "name", "Hexagon"));

      //////////////////////////////////////////////////////////
      // there shouldn't have been any additional queries ran //
      //////////////////////////////////////////////////////////
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN));
   }



   /*******************************************************************************
    ** run a lot of threads (eg, 100), each trying to do lots of work in a
    ** shared recordLookupHelper.  w/o the flag to use sync'ed collections, this
    ** (usually?) fails with a ConcurrentModificationException - but with the sync'ed
    ** collections, is safe.
    *******************************************************************************/
   @Test
   void testConcurrentModification() throws InterruptedException, ExecutionException
   {
      ExecutorService    executorService    = Executors.newFixedThreadPool(100);
      RecordLookupHelper recordLookupHelper = new RecordLookupHelper(true);

      CapturedContext capture = QContext.capture();

      List<Future<?>> futures = new ArrayList<>();
      for(int i = 0; i < 100; i++)
      {
         int finalI = i;
         Future<?> future = executorService.submit(() ->
         {
            QContext.init(capture);
            for(int j = 0; j < 25000; j++)
            {
               try
               {
                  recordLookupHelper.getRecordByKey(String.valueOf(j), "id", j);
               }
               catch(ConcurrentModificationException cme)
               {
                  fail("CME!", cme);
               }
               catch(Exception e)
               {
                  //////////////
                  // expected //
                  //////////////
               }
            }
         });
         futures.add(future);
      }

      for(Future<?> future : futures)
      {
         future.get();
      }
   }

}