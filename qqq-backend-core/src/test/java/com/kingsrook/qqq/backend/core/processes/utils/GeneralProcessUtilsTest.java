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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.testentities.Shape;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for GeneralProcessUtils
 *******************************************************************************/
class GeneralProcessUtilsTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   @BeforeEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetForeignRecordMap() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("favoriteShapeId", 3),
         new QRecord().withValue("favoriteShapeId", 1)
      ));
      TestUtils.insertDefaultShapes(instance);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      Map<Serializable, QRecord> foreignRecordMap = GeneralProcessUtils.getForeignRecordMap(queryOutput.getRecords(), "favoriteShapeId", TestUtils.TABLE_NAME_SHAPE, "id");

      assertEquals(2, foreignRecordMap.size());
      assertEquals(1, foreignRecordMap.get(1).getValueInteger("id"));
      assertEquals(3, foreignRecordMap.get(3).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetForeignRecordMapWithAdditionalFilter() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("favoriteShapeId", 3),
         new QRecord().withValue("favoriteShapeId", 1)
      ));
      TestUtils.insertDefaultShapes(instance);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      QQueryFilter               additionalFilter = new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Circle"));
      Map<Serializable, QRecord> foreignRecordMap = GeneralProcessUtils.getForeignRecordMap(queryOutput.getRecords(), "favoriteShapeId", TestUtils.TABLE_NAME_SHAPE, "id", additionalFilter);

      assertEquals(1, foreignRecordMap.size());
      assertEquals(3, foreignRecordMap.get(3).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetForeignRecordListingHashMap() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("favoriteShapeId", 3),
         new QRecord().withValue("id", 2).withValue("favoriteShapeId", 3),
         new QRecord().withValue("id", 3).withValue("favoriteShapeId", 1)
      ));
      TestUtils.insertDefaultShapes(instance);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_SHAPE);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      ListingHash<Serializable, QRecord> foreignRecordListingHashMap = GeneralProcessUtils.getForeignRecordListingHashMap(queryOutput.getRecords(), "id", TestUtils.TABLE_NAME_PERSON_MEMORY, "favoriteShapeId");

      assertEquals(2, foreignRecordListingHashMap.size());

      assertEquals(1, foreignRecordListingHashMap.get(1).size());
      assertEquals(Set.of(3), foreignRecordListingHashMap.get(1).stream().map(r -> r.getValueInteger("id")).collect(Collectors.toSet()));

      assertEquals(2, foreignRecordListingHashMap.get(3).size());
      assertEquals(Set.of(1, 2), foreignRecordListingHashMap.get(3).stream().map(r -> r.getValueInteger("id")).collect(Collectors.toSet()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAddForeignRecordsToRecordList() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("favoriteShapeId", 3),
         new QRecord().withValue("favoriteShapeId", 1)
      ));
      TestUtils.insertDefaultShapes(instance);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      GeneralProcessUtils.addForeignRecordsToRecordList(queryOutput.getRecords(), "favoriteShapeId", TestUtils.TABLE_NAME_SHAPE, "id");

      for(QRecord record : queryOutput.getRecords())
      {
         assertEquals(record.getValue("favoriteShapeId"), ((QRecord) record.getValue(TestUtils.TABLE_NAME_SHAPE)).getValue("id"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAddForeignRecordsListToRecordList() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("favoriteShapeId", 3),
         new QRecord().withValue("id", 2).withValue("favoriteShapeId", 3),
         new QRecord().withValue("id", 3).withValue("favoriteShapeId", 1)
      ));
      TestUtils.insertDefaultShapes(instance);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_SHAPE);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      GeneralProcessUtils.addForeignRecordsListToRecordList(queryOutput.getRecords(), "id", TestUtils.TABLE_NAME_PERSON_MEMORY, "favoriteShapeId");

      for(QRecord record : queryOutput.getRecords())
      {
         @SuppressWarnings("unchecked")
         List<QRecord> foreignRecordList = (List<QRecord>) record.getValue(TestUtils.TABLE_NAME_PERSON_MEMORY);

         if(record.getValueInteger("id").equals(3))
         {
            assertEquals(2, foreignRecordList.size());
         }
         else if(record.getValueInteger("id").equals(2))
         {
            assertNull(foreignRecordList);
            continue;
         }

         for(QRecord foreignRecord : foreignRecordList)
         {
            assertEquals(record.getValue("id"), foreignRecord.getValue("favoriteShapeId"));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetRecordListByField() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("favoriteShapeId", 3),
         new QRecord().withValue("id", 2).withValue("favoriteShapeId", 3),
         new QRecord().withValue("id", 3).withValue("favoriteShapeId", 1)
      ));

      QueryInput    queryInput = new QueryInput();
      List<QRecord> records    = GeneralProcessUtils.getRecordListByField(TestUtils.TABLE_NAME_PERSON_MEMORY, "favoriteShapeId", 3);
      assertEquals(2, records.size());
      assertTrue(records.stream().anyMatch(r -> r.getValue("id").equals(1)));
      assertTrue(records.stream().anyMatch(r -> r.getValue("id").equals(2)));
      assertTrue(records.stream().noneMatch(r -> r.getValue("id").equals(3)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetRecordById() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin"),
         new QRecord().withValue("id", 2).withValue("firstName", "James"),
         new QRecord().withValue("id", 3).withValue("firstName", "Tim")
      ));

      QueryInput        queryInput = new QueryInput();
      Optional<QRecord> record     = GeneralProcessUtils.getRecordByField(TestUtils.TABLE_NAME_PERSON_MEMORY, "firstName", "James");
      assertTrue(record.isPresent());
      assertEquals(2, record.get().getValueInteger("id"));

      record = GeneralProcessUtils.getRecordByField(TestUtils.TABLE_NAME_PERSON_MEMORY, "firstName", "Bobby");
      assertFalse(record.isPresent());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLoadTable() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin"),
         new QRecord().withValue("id", 2).withValue("firstName", "James"),
         new QRecord().withValue("id", 3).withValue("firstName", "Tim")
      ));

      QueryInput    queryInput = new QueryInput();
      List<QRecord> records    = GeneralProcessUtils.loadTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals(3, records.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLoadTableToMap() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin"),
         new QRecord().withValue("id", 2).withValue("firstName", "James"),
         new QRecord().withValue("id", 3).withValue("firstName", "Tim")
      ));

      QueryInput                 queryInput    = new QueryInput();
      Map<Serializable, QRecord> recordMapById = GeneralProcessUtils.loadTableToMap(TestUtils.TABLE_NAME_PERSON_MEMORY, "id");
      assertEquals(3, recordMapById.size());
      assertEquals("Darin", recordMapById.get(1).getValueString("firstName"));
      assertEquals("James", recordMapById.get(2).getValueString("firstName"));

      Map<Serializable, QRecord> recordMapByFirstName = GeneralProcessUtils.loadTableToMap(TestUtils.TABLE_NAME_PERSON_MEMORY, "firstName");
      assertEquals(3, recordMapByFirstName.size());
      assertEquals(1, recordMapByFirstName.get("Darin").getValueInteger("id"));
      assertEquals(3, recordMapByFirstName.get("Tim").getValueInteger("id"));

      Map<String, QRecord> recordMapByFirstNameAsString = GeneralProcessUtils.loadTableToMap(TestUtils.TABLE_NAME_PERSON_MEMORY, String.class, "firstName");
      assertEquals(3, recordMapByFirstName.size());
      assertEquals(1, recordMapByFirstName.get("Darin").getValueInteger("id"));
      assertEquals(3, recordMapByFirstName.get("Tim").getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLoadTableToListingHash() throws QException
   {
      QInstance instance = QContext.getQInstance();

      TestUtils.insertRecords(instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff"),
         new QRecord().withValue("id", 2).withValue("firstName", "James").withValue("lastName", "Maes"),
         new QRecord().withValue("id", 3).withValue("firstName", "James").withValue("lastName", "Brown")
      ));

      QueryInput                         queryInput = new QueryInput();
      ListingHash<Serializable, QRecord> map        = GeneralProcessUtils.loadTableToListingHash(TestUtils.TABLE_NAME_PERSON_MEMORY, "firstName");
      assertEquals(2, map.size());
      assertEquals(1, map.get("Darin").size());
      assertEquals(2, map.get("James").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetRecordByFieldOrElseThrow() throws QException
   {
      QInstance instance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(instance);

      assertNotNull(GeneralProcessUtils.getRecordByFieldOrElseThrow(TestUtils.TABLE_NAME_SHAPE, "name", "Triangle"));
      assertThrows(QException.class, () -> GeneralProcessUtils.getRecordByFieldOrElseThrow(TestUtils.TABLE_NAME_SHAPE, "name", "notAShape"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetRecordByPrimaryKey() throws QException
   {
      QInstance instance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(instance);

      AbstractActionInput actionInput = new AbstractActionInput();
      assertTrue(GeneralProcessUtils.getRecordByPrimaryKey(TestUtils.TABLE_NAME_SHAPE, 1).isPresent());
      assertFalse(GeneralProcessUtils.getRecordByPrimaryKey(TestUtils.TABLE_NAME_SHAPE, -1).isPresent());
      assertNotNull(GeneralProcessUtils.getRecordByPrimaryKeyOrElseThrow(TestUtils.TABLE_NAME_SHAPE, 1));
      assertThrows(QException.class, () -> GeneralProcessUtils.getRecordByPrimaryKeyOrElseThrow(TestUtils.TABLE_NAME_SHAPE, -1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCount() throws QException
   {
      QInstance instance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(instance);
      AbstractActionInput actionInput = new AbstractActionInput();

      assertEquals(3, GeneralProcessUtils.count(TestUtils.TABLE_NAME_SHAPE, null));
      assertEquals(1, GeneralProcessUtils.count(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2))));
      assertEquals(0, GeneralProcessUtils.count(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IS_BLANK))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetEntityByField() throws QException
   {
      QInstance instance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(instance);
      AbstractActionInput actionInput = new AbstractActionInput();

      assertEquals("Triangle", GeneralProcessUtils.getEntityByField(TestUtils.TABLE_NAME_SHAPE, "name", "Triangle", Shape.class).get().getName());
      assertFalse(GeneralProcessUtils.getEntityByField(TestUtils.TABLE_NAME_SHAPE, "name", "notAShape", Shape.class).isPresent());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLoadTableAsEntities() throws QException
   {
      QInstance instance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(instance);
      AbstractActionInput actionInput = new AbstractActionInput();

      List<Shape> shapes = GeneralProcessUtils.loadTable(TestUtils.TABLE_NAME_SHAPE, Shape.class);
      assertEquals(3, shapes.size());
      assertTrue(shapes.get(0) instanceof Shape);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLoadTableToMapAsEntities() throws QException
   {
      QInstance instance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(instance);
      AbstractActionInput actionInput = new AbstractActionInput();

      Map<Serializable, Shape> map = GeneralProcessUtils.loadTableToMap(TestUtils.TABLE_NAME_SHAPE, "id", Shape.class);
      assertEquals(3, map.size());
      assertTrue(map.get(1) instanceof Shape);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordsToEntities() throws QException
   {
      List<Shape> shapes = GeneralProcessUtils.recordsToEntities(Shape.class, List.of(
         new QRecord().withValue("id", 99).withValue("name", "round"),
         new QRecord().withValue("id", 98).withValue("name", "flat")
      ));

      assertEquals(2, shapes.size());
      assertEquals(99, shapes.get(0).getId());
      assertEquals("round", shapes.get(0).getName());
   }

}
