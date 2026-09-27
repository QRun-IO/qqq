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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for ChildInserterPostInsertCustomizer
 **
 ** We'll use person & shape tables here, w/ the favoriteShapeId foreign key,
 ** so a rule of "every time we insert a person, if they aren't already pointed at
 ** a favoriteShape, insert a new shape for them".
 *******************************************************************************/
class ChildInserterPostInsertCustomizerTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEmptyCases() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      addPostInsertActionToPersonTable(qInstance);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of());

      ////////////////////////////////////////
      // just looking for no exception here //
      ////////////////////////////////////////
      new InsertAction().execute(insertInput);
      assertEquals(0, TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_SHAPE).size());

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now insert one person, but they shouldn't need a favoriteShape to be inserted - again, make sure we don't blow up and that no shapes get inserted. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      insertInput.setRecords(List.of(new QRecord().withValue("firstName", "James").withValue("favoriteShapeId", -1)));
      new InsertAction().execute(insertInput);
      assertEquals(0, TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_SHAPE).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void addPostInsertActionToPersonTable(QInstance qInstance)
   {
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(PersonPostInsertAddFavoriteShapeCustomizer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void addPostInsertActionToShapeTable(QInstance qInstance)
   {
      qInstance.getTable(TestUtils.TABLE_NAME_SHAPE)
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(ShapePostInsertAddPersonCustomizer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSimpleParentPointsAtChildCase() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      addPostInsertActionToPersonTable(qInstance);

      assertEquals(0, TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_SHAPE).size());

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "Darin")
      ));
      InsertOutput insertOutput    = new InsertAction().execute(insertInput);
      Serializable favoriteShapeId = insertOutput.getRecords().get(0).getValue("favoriteShapeId");
      assertNotNull(favoriteShapeId);

      List<QRecord> shapeRecords = TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_SHAPE);
      assertEquals(1, shapeRecords.size());
      assertEquals(favoriteShapeId, shapeRecords.get(0).getValue("id"));
      assertEquals("Darin's favorite shape!", shapeRecords.get(0).getValue("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testComplexParentPointsAtChildCase() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      addPostInsertActionToPersonTable(qInstance);

      assertEquals(0, TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_SHAPE).size());

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "Darin"),
         new QRecord().withValue("firstName", "James").withValue("favoriteShapeId", -1),
         new QRecord().withValue("firstName", "Tim"),
         new QRecord().withValue("firstName", "Garret").withValue("favoriteShapeId", -2)
      ));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(1, insertOutput.getRecords().get(0).getValue("favoriteShapeId"));
      assertEquals(-1, insertOutput.getRecords().get(1).getValue("favoriteShapeId"));
      assertEquals(2, insertOutput.getRecords().get(2).getValue("favoriteShapeId"));
      assertEquals(-2, insertOutput.getRecords().get(3).getValue("favoriteShapeId"));

      List<QRecord> shapeRecords = TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_SHAPE);
      assertEquals(2, shapeRecords.size());
      assertEquals(1, shapeRecords.get(0).getValue("id"));
      assertEquals(2, shapeRecords.get(1).getValue("id"));
      assertEquals("Darin's favorite shape!", shapeRecords.get(0).getValue("name"));
      assertEquals("Tim's favorite shape!", shapeRecords.get(1).getValue("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSimpleChildPointsAtParentCase() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      addPostInsertActionToShapeTable(qInstance);

      assertEquals(0, TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY).size());
      assertEquals(0, TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_SHAPE).size());

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_SHAPE);
      insertInput.setRecords(List.of(
         new QRecord().withValue("name", "Circle")
      ));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      Integer      shapeId      = insertOutput.getRecords().get(0).getValueInteger("id");

      List<QRecord> personRecords = TestUtils.queryTable(qInstance, TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals(1, personRecords.size());
      assertEquals(shapeId, personRecords.get(0).getValue("favoriteShapeId"));
      assertEquals("loves Circle", personRecords.get(0).getValue("lastName"));
   }



   /*******************************************************************************
    ** for the person table - where we do PARENT_POINTS_AT_CHILD
    *******************************************************************************/
   public static class PersonPostInsertAddFavoriteShapeCustomizer extends ChildInserterPostInsertCustomizer
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QRecord buildChildForRecord(QRecord parentRecord) throws QException
      {
         return (new QRecord().withValue("name", parentRecord.getValue("firstName") + "'s favorite shape!"));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getChildTableName()
      {
         return (TestUtils.TABLE_NAME_SHAPE);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getForeignKeyFieldName()
      {
         return ("favoriteShapeId");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RelationshipType getRelationshipType()
      {
         return (RelationshipType.PARENT_POINTS_AT_CHILD);
      }
   }



   /*******************************************************************************
    ** for the shape table - where we do CHILD_POINTS_AT_PARENT
    *******************************************************************************/
   public static class ShapePostInsertAddPersonCustomizer extends ChildInserterPostInsertCustomizer
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QRecord buildChildForRecord(QRecord parentRecord) throws QException
      {
         return (new QRecord()
            .withValue("firstName", "Someone who")
            .withValue("lastName", "loves " + parentRecord.getValue("name"))
            .withValue("favoriteShapeId", parentRecord.getValue("id")));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getChildTableName()
      {
         return (TestUtils.TABLE_NAME_PERSON_MEMORY);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RelationshipType getRelationshipType()
      {
         return (RelationshipType.CHILD_POINTS_AT_PARENT);
      }
   }

}