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
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for QueryActionCacheHelper 
 *******************************************************************************/
class QueryActionCacheHelperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyCacheSingleFieldUniqueKeySingleRecordUseCases() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      String sourceTableName = TestUtils.TABLE_NAME_SHAPE;
      String cacheTableName  = TestUtils.TABLE_NAME_SHAPE_CACHE;

      /////////////////////////////////////
      // insert rows in the source table //
      /////////////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(sourceTableName), List.of(
         new QRecord().withValue("id", 1).withValue("name", "Triangle").withValue("noOfSides", 3),
         new QRecord().withValue("id", 2).withValue("name", "Square").withValue("noOfSides", 4),
         new QRecord().withValue("id", 3).withValue("name", "Pentagon").withValue("noOfSides", 5),
         new QRecord().withValue("id", 4).withValue("name", "ServerErrorGon").withValue("noOfSides", 503),
         new QRecord().withValue("id", 5).withValue("name", "ManyGon").withValue("noOfSides", 999)
      ));

      /////////////////////////////////////////////////////////////////////////////
      // get from the table which caches it - confirm they are (magically) found //
      /////////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertNotEquals(0, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(3, queryOutput.getRecords().get(0).getValue("noOfSides"));
         assertNotNull(queryOutput.getRecords().get(0).getValue("id"));
      }

      ////////////////////////////////////////////////////////////////////////////////////
      // try to get from the table which caches it - it should be found, but not cached //
      // because use case should filter out because of matching 503                     //
      ////////////////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "ServerErrorGon")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "ManyGon")));
         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
      }

      ///////////////////////////////////////////////////////////////////////////
      // request a row that doesn't exist in cache or source, should miss both //
      ///////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Line"))); // lines aren't shapes :)
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // update the record in the source table - then re-get from cache table - shouldn't see new value. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("noOfSides", 6)));
         new UpdateAction().execute(updateInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertNotEquals(0, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(3, queryOutput.getRecords().get(0).getValue("noOfSides"));
      }

      ///////////////////////////////////////////////////////////////////////////
      // delete the cached record; re-get, and we should see the updated value //
      ///////////////////////////////////////////////////////////////////////////
      {
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(cacheTableName);
         deleteInput.setQueryFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")));
         new DeleteAction().execute(deleteInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertNotEquals(0, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(6, queryOutput.getRecords().get(0).getValue("noOfSides"));
      }

      ///////////////////////////////////////////////////////////////////
      // update the source record; see that it isn't updated in cache. //
      ///////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("noOfSides", 7)));
         new UpdateAction().execute(updateInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertNotEquals(0, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(6, queryOutput.getRecords().get(0).getValue("noOfSides"));

         ///////////////////////////////////////////////////////////////////////
         // then artificially move back the cachedDate in the cache table.    //
         // then re-get from cache table, and we should see the updated value //
         ///////////////////////////////////////////////////////////////////////
         updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(List.of(queryOutput.getRecords().get(0).withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))));
         new UpdateAction().execute(updateInput);

         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(7, queryOutput.getRecords().get(0).getValue("noOfSides"));
      }

      /////////////////////////////////////////////////
      // should only be 1 cache record at this point //
      /////////////////////////////////////////////////
      assertEquals(1, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));

      //////////////////////////////////////////////////////////////////////
      // delete the source record - it will still be in the cache though. //
      //////////////////////////////////////////////////////////////////////
      {
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(sourceTableName);
         deleteInput.setPrimaryKeys(List.of(1));
         new DeleteAction().execute(deleteInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         assertEquals(1, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));

         ////////////////////////////////////////////////////////////////////
         // then artificially move back the cachedDate in the cache table. //
         // then re-get from cache table, and now it should go away        //
         ////////////////////////////////////////////////////////////////////
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(List.of(queryOutput.getRecords().get(0).withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))));
         new UpdateAction().execute(updateInput);

         queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")));
         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoNotQuerySourceTableFlag() throws QException
   {
      //////////////////////////////////////////////////////////////////////////////////////////////
      // start out here like a copy of testUniqueKeyCacheSingleFieldUniqueKeySingleRecordUseCases //
      //////////////////////////////////////////////////////////////////////////////////////////////
      QInstance qInstance = QContext.getQInstance();

      String sourceTableName = TestUtils.TABLE_NAME_SHAPE;
      String cacheTableName  = TestUtils.TABLE_NAME_SHAPE_CACHE;

      /////////////////////////////////////
      // insert rows in the source table //
      /////////////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(sourceTableName), List.of(
         new QRecord().withValue("id", 1).withValue("name", "Triangle").withValue("noOfSides", 3)
      ));

      assertEquals(0, new CountAction().execute(new CountInput(cacheTableName)).getCount());

      /////////////////////////////////////////////////////////////////////////////////
      // get from the table which caches it - normally it would (magically) be found //
      // and inserted - but with the DO_NOT_QUERY flag, it won't be                  //
      /////////////////////////////////////////////////////////////////////////////////
      assertEquals(0, new QueryAction().execute(new QueryInput(cacheTableName).withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")))
         .withFlag(QueryActionCacheHelper.CacheActionFlags.DO_NOT_QUERY_SOURCE_TABLE)).getRecords().size());
      assertEquals(0, new CountAction().execute(new CountInput(cacheTableName)).getCount());

      //////////////////////////////////////////////////////////////////////////////////////////////////
      // query again, w/o the flag - the row should get inserted (as it is found in the source table) //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(1, new QueryAction().execute(new QueryInput(cacheTableName).withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle"))))
         .getRecords().size());
      assertEquals(1, new CountAction().execute(new CountInput(cacheTableName)).getCount());

      ///////////////////////////////////////////////////////////////////////////////////
      // and query again w/ the flag - now that cache record exists, it'll be returned //
      ///////////////////////////////////////////////////////////////////////////////////
      assertEquals(1, new QueryAction().execute(new QueryInput(cacheTableName).withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")))
         .withFlag(QueryActionCacheHelper.CacheActionFlags.DO_NOT_QUERY_SOURCE_TABLE)).getRecords().size());
      assertEquals(1, new CountAction().execute(new CountInput(cacheTableName)).getCount());

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyCacheSingleFieldUniqueKeyMultiRecordUseCases() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      String sourceTableName = TestUtils.TABLE_NAME_SHAPE;
      String cacheTableName  = TestUtils.TABLE_NAME_SHAPE_CACHE;

      /////////////////////////////////////
      // insert rows in the source table //
      /////////////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(sourceTableName), List.of(
         new QRecord().withValue("id", 1).withValue("name", "Triangle").withValue("noOfSides", 3),
         new QRecord().withValue("id", 2).withValue("name", "Square").withValue("noOfSides", 4),
         new QRecord().withValue("id", 3).withValue("name", "Pentagon").withValue("noOfSides", 5),
         new QRecord().withValue("id", 4).withValue("name", "ServerErrorGon").withValue("noOfSides", 503),
         new QRecord().withValue("id", 5).withValue("name", "ManyGon").withValue("noOfSides", 999)
      ));

      /////////////////////////////////////////////////////////////////////////////
      // get from the table which caches it - confirm they are (magically) found //
      /////////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN, "Triangle", "Square", "Pentagon")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(3, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try to get records through the cache table, which meet the conditions that cause them to not be cached.                              //
      // so we should get results from the Query request - but - then let's go directly to the backend to confirm the records are not cached. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         assertEquals(3, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);

         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "ServerErrorGon")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());

         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN, "ManyGon", "ServerErrorGon")));
         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(2, queryOutput.getRecords().size());

         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN, "ManyGon", "Square")));
         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(2, queryOutput.getRecords().size());

         assertEquals(3, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }

      ///////////////////////////////////////////////////////////////////////////
      // request a row that doesn't exist in cache or source, should miss both //
      ///////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Line"))); // lines aren't shapes :)
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////
      // update one source record; delete another - query and should still find the previously cached //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("noOfSides", 6)));
         new UpdateAction().execute(updateInput);

         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(sourceTableName);
         deleteInput.setPrimaryKeys(List.of(2)); // delete Square
         new DeleteAction().execute(deleteInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN, "Triangle", "Square", "Pentagon", "ServerErrorGon")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(4, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(3, queryOutput.getRecords().get(0).getValue("noOfSides"));

         /////////////////////////////////////////////////////////////////////////////////////////////
         // then artificially move back the cachedDate in the cache table.                          //
         // then re-get from cache table, and we should see the updated value (and the deleted one) //
         /////////////////////////////////////////////////////////////////////////////////////////////
         updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(Stream.of(1, 2, 3).map(id -> new QRecord().withValue("id", id).withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))).toList());
         new UpdateAction().execute(updateInput);

         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(3, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(6, queryOutput.getRecords().stream().filter(r -> r.getValueString("name").equals("Triangle")).findFirst().get().getValue("noOfSides"));

         assertEquals(2, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyCacheMultiFieldUniqueKeySingleRecordUseCases() throws QException
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
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("George", "Washington"));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(5, queryOutput.getRecords().get(0).getValue("noOfShoes"));
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try to get records through the cache table, which meet the conditions that cause them to not be cached.                            //
      // so we should get results from the Get request - but - then let's go directly to the backend to confirm the records are not cached. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("James", "Garfield"));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());

         queryInput.setFilter(getFilterForPerson("Abraham", "Lincoln"));
         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());

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
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("Bill", "Clinton"));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));

         assertEquals(1, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Clinton"))));

         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 6).withValue("noOfShoes", 503)));
         new UpdateAction().execute(updateInput);

         updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(List.of(queryOutput.getRecords().get(0).withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))));
         new UpdateAction().execute(updateInput);

         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(503, queryOutput.getRecords().get(0).getValue("noOfShoes"));

         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Clinton"))));
      }

      ///////////////////////////////////////////////////////////////////////////
      // request a row that doesn't exist in cache or source, should miss both //
      ///////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("John", "McCain"));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // update the record in the source table - then re-get from cache table - shouldn't see new value. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("noOfShoes", 6)));
         new UpdateAction().execute(updateInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("George", "Washington"));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(5, queryOutput.getRecords().get(0).getValue("noOfShoes"));
      }

      ///////////////////////////////////////////////////////////////////////////
      // delete the cached record; re-get, and we should see the updated value //
      ///////////////////////////////////////////////////////////////////////////
      {
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(cacheTableName);
         deleteInput.setQueryFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "George")));
         new DeleteAction().execute(deleteInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("George", "Washington"));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(6, queryOutput.getRecords().get(0).getValue("noOfShoes"));
      }

      ///////////////////////////////////////////////////////////////////
      // update the source record; see that it isn't updated in cache. //
      ///////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("noOfShoes", 7)));
         new UpdateAction().execute(updateInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("George", "Washington"));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(6, queryOutput.getRecords().get(0).getValue("noOfShoes"));

         ///////////////////////////////////////////////////////////////////////
         // then artificially move back the cachedDate in the cache table.    //
         // then re-get from cache table, and we should see the updated value //
         ///////////////////////////////////////////////////////////////////////
         updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(List.of(queryOutput.getRecords().get(0).withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))));
         new UpdateAction().execute(updateInput);

         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(7, queryOutput.getRecords().get(0).getValue("noOfShoes"));
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

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("George", "Washington"));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());

         ////////////////////////////////////////////////////////////////////
         // then artificially move back the cachedDate in the cache table. //
         // then re-get from cache table, and now it should go away        //
         ////////////////////////////////////////////////////////////////////
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(List.of(queryOutput.getRecords().get(0).withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))));
         new UpdateAction().execute(updateInput);

         queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPerson("George", "Washington"));
         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyCacheMultiFieldUniqueKeyMultiRecordUseCases() throws QException
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
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPersons(getFilterForPerson("George", "Washington"), getFilterForPerson("John", "Adams"), getFilterForPerson("Thomas", "Jefferson")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(3, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try to get records through the cache table, which meet the conditions that cause them to not be cached.                              //
      // so we should get results from the Query request - but - then let's go directly to the backend to confirm the records are not cached. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         assertEquals(3, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);

         queryInput.setFilter(getFilterForPersons(getFilterForPerson("James", "Garfield"), getFilterForPerson("Abraham", "Lincoln"), getFilterForPerson("Thomas", "Jefferson")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(3, queryOutput.getRecords().size());

         assertEquals(3, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }

      ///////////////////////////////////////////////////////////////////////////
      // request a row that doesn't exist in cache or source, should miss both //
      ///////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPersons(getFilterForPerson("John", "McCain"), getFilterForPerson("John", "Kerry")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // fetch a record through the cache, so it gets cached.                                                        //
      // then update the source record so that it meets the condition that doesn't allow it to be cached.            //
      // and delete another one.                                                                                     //
      // then expire the cached records.                                                                             //
      // then re-fetch through cache - which should see the expiration, re-fetch from source, and delete from cache. //
      // assert record is no longer in cache.                                                                        //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(getFilterForPersons(getFilterForPerson("George", "Washington"), getFilterForPerson("Bill", "Clinton")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(2, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));

         assertEquals(1, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Clinton"))));
         assertEquals(1, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Washington"))));

         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 6).withValue("noOfShoes", 503)));
         new UpdateAction().execute(updateInput);

         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(sourceTableName);
         deleteInput.setPrimaryKeys(List.of(1)); // delete Washington
         new DeleteAction().execute(deleteInput);

         updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(Stream.of(1, 2, 3, 4).map(id -> new QRecord().withValue("id", id).withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))).toList());
         new UpdateAction().execute(updateInput);

         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(503, queryOutput.getRecords().stream().filter(r -> r.getValueString("lastName").equals("Clinton")).findFirst().get().getValue("noOfShoes"));

         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Clinton"))));
         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Washington"))));
      }

      /*
      //////////////////////////////////////////////////////////////////////////////////////////////////
      // update one source record; delete another - query and should still find the previously cached //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(sourceTableName);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("noOfSides", 6)));
         new UpdateAction().execute(updateInput);

         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(sourceTableName);
         deleteInput.setPrimaryKeys(List.of(2)); // delete Square
         new DeleteAction().execute(deleteInput);

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN, "Triangle", "Square", "Pentagon", "ServerErrorGon")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(4, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(3, queryOutput.getRecords().get(0).getValue("noOfSides"));

         /////////////////////////////////////////////////////////////////////////////////////////////
         // then artificially move back the cachedDate in the cache table.                          //
         // then re-get from cache table, and we should see the updated value (and the deleted one) //
         /////////////////////////////////////////////////////////////////////////////////////////////
         updateInput = new UpdateInput();
         updateInput.setTableName(cacheTableName);
         updateInput.setRecords(Stream.of(1, 2, 3).map(id -> new QRecord().withValue("id", id).withValue("cachedDate", Instant.parse("2001-01-01T00:00:00Z"))).toList());
         new UpdateAction().execute(updateInput);

         queryOutput = new QueryAction().execute(queryInput);
         assertEquals(3, queryOutput.getRecords().size());
         assertNotNull(queryOutput.getRecords().get(0).getValue("cachedDate"));
         assertEquals(6, queryOutput.getRecords().stream().filter(r -> r.getValueString("name").equals("Triangle")).findFirst().get().getValue("noOfSides"));

         assertEquals(2, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }
    */
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyCacheNonCachingUseCases() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      String sourceTableName = TestUtils.TABLE_NAME_SHAPE;
      String cacheTableName  = TestUtils.TABLE_NAME_SHAPE_CACHE;

      /////////////////////////////////////
      // insert rows in the source table //
      /////////////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(sourceTableName), List.of(
         new QRecord().withValue("id", 1).withValue("name", "Triangle").withValue("noOfSides", 3),
         new QRecord().withValue("id", 2).withValue("name", "Square").withValue("noOfSides", 4),
         new QRecord().withValue("id", 3).withValue("name", "Pentagon").withValue("noOfSides", 5),
         new QRecord().withValue("id", 4).withValue("name", "ServerErrorGon").withValue("noOfSides", 503),
         new QRecord().withValue("id", 5).withValue("name", "ManyGon").withValue("noOfSides", 999)));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // do queries on the cache table that we aren't allowed to do caching with - confirm that cache remains empty //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         ///////////////
         // no filter //
         ///////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }

      {
         //////////////////////////////
         // unique key not in filter //
         //////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("noOfSides", QCriteriaOperator.LESS_THAN_OR_EQUALS, 5)));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }

      {
         ////////////////////////////////////////////////
         // unsupported operator in filter on UK field //
         ////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.STARTS_WITH, "T")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }

      {
         ///////////////////////////////////////////////////////////////////////////
         // an AND sub-filter                                                     //
         // (technically we could do this, since only 1 sub-filter, but we don't) //
         ///////////////////////////////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter().withSubFilters(List.of(
            new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle"))
         )));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }

      {
         //////////////////////////////////////////////
         // an OR sub-filter, but unsupported fields //
         //////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilters(List.of(
            new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")),
            new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 3))
         )));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }

      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         // an OR sub-filter, but with unsupported operator (IN - supported w/o subqueries, but not like this) //
         // (technically we could do this, since only 1 sub-filter, but we don't)                              //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilters(List.of(
            new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN, "Triangle", "Square"))
         )));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(0, queryOutput.getRecords().size());
         assertEquals(0, countCachedRecordsDirectlyInBackend(cacheTableName, new QQueryFilter()));
      }

      ///////////////////////////////////////////////////////////////////////////////////////////
      // finally - queries that DO hit cache (so note, cache will stop being empty after here) //
      ///////////////////////////////////////////////////////////////////////////////////////////
      {
         ///////////////////////////////////////////////////////////
         // an OR sub-filter, with supported ops, and UKey fields //
         ///////////////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(cacheTableName);
         queryInput.setFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withSubFilters(List.of(
            new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle")),
            new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Square"))
         )));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(2, queryOutput.getRecords().size());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QQueryFilter getFilterForPerson(String firstName, String lastName)
   {
      return new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, firstName), new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, lastName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QQueryFilter getFilterForPersons(QQueryFilter... subFilters)
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
      for(QQueryFilter subFilter : subFilters)
      {
         filter.addSubFilter(subFilter);
      }
      return (filter);
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