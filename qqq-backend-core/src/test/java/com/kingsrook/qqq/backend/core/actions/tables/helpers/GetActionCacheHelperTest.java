/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for GetActionCacheHelper 
 *******************************************************************************/
class GetActionCacheHelperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyCache() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      String sourceTableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      String cacheTableName  = TestUtils.TABLE_NAME_PERSON_MEMORY_CACHE;

      /////////////////////////////////////
      // insert rows in the source table //
      /////////////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(sourceTableName), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "George").withValue("lastName", "Washington").withValue("noOfShoes", 5),
         new QRecord().withValue("id", 2).withValue("firstName", "John").withValue("lastName", "Adams"),
         new QRecord().withValue("id", 3).withValue("firstName", "Thomas").withValue("lastName", "Jefferson"),
         new QRecord().withValue("id", 4).withValue("firstName", "James").withValue("lastName", "Garfield").withValue("noOfShoes", 503),
         new QRecord().withValue("id", 5).withValue("firstName", "Abraham").withValue("lastName", "Lincoln").withValue("noOfShoes", 999),
         new QRecord().withValue("id", 6).withValue("firstName", "Bill").withValue("lastName", "Clinton")
      ));

      /////////////////////////////////////////////////////////////////////////////
      // get from the table which caches it - confirm they are (magically) found //
      /////////////////////////////////////////////////////////////////////////////
      {
         GetInput getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "George", "lastName", "Washington"));
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());
         assertNotNull(getOutput.getRecord().getValue("cachedDate"));
         assertEquals(5, getOutput.getRecord().getValue("noOfShoes"));
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try to get records through the cache table, which meet the conditions that cause them to not be cached.                            //
      // so we should get results from the Get request - but - then let's go directly to the backend to confirm the records are not cached. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         GetInput getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "James", "lastName", "Garfield"));
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());

         getInput.setUniqueKey(Map.of("firstName", "Abraham", "lastName", "Lincoln"));
         getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());

         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.IN, "Abraham", "James"))));
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // fetch a record through the cache, so it gets cached.                                                        //
      // then update the source record so that it meets the condition that doesn't allow it to be cached.            //
      // then expire the cached record.                                                                              //
      // then re-fetch through cache - which should see the expiration, re-fetch from source, and delete from cache. //
      // assert record is no longer in cache.                                                                        //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         GetInput getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "Bill", "lastName", "Clinton"));
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());
         assertNotNull(getOutput.getRecord().getValue("cachedDate"));

         assertEquals(1, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Clinton"))));

         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 6).withValue("noOfShoes", 503)));
         new UpdateAction().execute(updateInput);

         updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(List.of(getOutput.getRecord().withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))));
         new UpdateAction().execute(updateInput);

         getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "Bill", "lastName", "Clinton"));
         getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());
         assertEquals(503, getOutput.getRecord().getValue("noOfShoes"));

         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Clinton"))));
      }

      ///////////////////////////////////////////////////////////////////////////
      // request a row that doesn't exist in cache or source, should miss both //
      ///////////////////////////////////////////////////////////////////////////
      {
         GetInput getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "John", "lastName", "McCain"));
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNull(getOutput.getRecord());
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // update the record in the source table - then re-get from cache table - shouldn't see new value. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("noOfShoes", 6)));
         new UpdateAction().execute(updateInput);

         GetInput getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "George", "lastName", "Washington"));
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());
         assertNotNull(getOutput.getRecord().getValue("cachedDate"));
         assertEquals(5, getOutput.getRecord().getValue("noOfShoes"));
      }

      ///////////////////////////////////////////////////////////////////////////
      // delete the cached record; re-get, and we should see the updated value //
      ///////////////////////////////////////////////////////////////////////////
      {
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(cacheTableName);
         deleteInput.setQueryFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "George")));
         new DeleteAction().execute(deleteInput);

         GetInput getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "George", "lastName", "Washington"));
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());
         assertNotNull(getOutput.getRecord().getValue("cachedDate"));
         assertEquals(6, getOutput.getRecord().getValue("noOfShoes"));
      }

      ///////////////////////////////////////////////////////////////////
      // update the source record; see that it isn't updated in cache. //
      ///////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("noOfShoes", 7)));
         new UpdateAction().execute(updateInput);

         GetInput getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "George", "lastName", "Washington"));
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());
         assertNotNull(getOutput.getRecord().getValue("cachedDate"));
         assertEquals(6, getOutput.getRecord().getValue("noOfShoes"));

         ///////////////////////////////////////////////////////////////////////
         // then artificially move back the cachedDate in the cache table.    //
         // then re-get from cache table, and we should see the updated value //
         ///////////////////////////////////////////////////////////////////////
         updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(List.of(getOutput.getRecord().withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))));
         new UpdateAction().execute(updateInput);

         getOutput = new GetAction().execute(getInput);
         assertEquals(7, getOutput.getRecord().getValue("noOfShoes"));
      }

      /////////////////////////////////////////////////
      // should only be 1 cache record at this point //
      /////////////////////////////////////////////////
      assertEquals(1, TestUtils.queryTable(QContext.getQInstance(), cacheTableName).size());

      //////////////////////////////////////////////////////////////////////
      // delete the source record - it will still be in the cache though. //
      //////////////////////////////////////////////////////////////////////
      {
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(sourceTableName);
         deleteInput.setPrimaryKeys(List.of(1));
         new DeleteAction().execute(deleteInput);

         GetInput getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "George", "lastName", "Washington"));
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNotNull(getOutput.getRecord());

         ////////////////////////////////////////////////////////////////////
         // then artificially move back the cachedDate in the cache table. //
         // then re-get from cache table, and now it should go away        //
         ////////////////////////////////////////////////////////////////////
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(List.of(getOutput.getRecord().withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))));
         new UpdateAction().execute(updateInput);

         getInput = new GetInput();
         getInput.setTableName(cacheTableName);
         getInput.setUniqueKey(Map.of("firstName", "George", "lastName", "Washington"));
         getOutput = new GetAction().execute(getInput);
         assertNull(getOutput.getRecord());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int countCachedRecordsDirectlyInBackend(String tableName, QQueryFilter filter) throws QException
   {
      List<QRecord> cachedRecords = MemoryRecordStore.getInstance().query(new QueryInput()
         .withTableName(tableName)
         .withFilter(filter));
      return cachedRecords.size();
   }

}