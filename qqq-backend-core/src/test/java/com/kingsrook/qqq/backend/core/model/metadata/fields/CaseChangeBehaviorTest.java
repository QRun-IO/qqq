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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for CaseChangeBehavior 
 *******************************************************************************/
class CaseChangeBehaviorTest extends BaseTest
{
   public static final String FIELD = "firstName";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNone()
   {
      assertNull(applyToRecord(CaseChangeBehavior.NONE, new QRecord(), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));
      assertNull(applyToRecord(CaseChangeBehavior.NONE, new QRecord().withValue(FIELD, null), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));
      assertEquals("John", applyToRecord(CaseChangeBehavior.NONE, new QRecord().withValue(FIELD, "John"), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));

      assertEquals(ListBuilder.of("John", null, "Jane"), applyToRecords(CaseChangeBehavior.NONE, List.of(
            new QRecord().withValue(FIELD, "John"),
            new QRecord(),
            new QRecord().withValue(FIELD, "Jane")),
         ValueBehaviorApplier.Action.INSERT).stream().map(r -> r.getValueString(FIELD)).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testToUpperCase()
   {
      assertNull(applyToRecord(CaseChangeBehavior.TO_UPPER_CASE, new QRecord(), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));
      assertNull(applyToRecord(CaseChangeBehavior.TO_UPPER_CASE, new QRecord().withValue(FIELD, null), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));
      assertEquals("JOHN", applyToRecord(CaseChangeBehavior.TO_UPPER_CASE, new QRecord().withValue(FIELD, "John"), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));

      assertEquals(ListBuilder.of("JOHN", null, "JANE"), applyToRecords(CaseChangeBehavior.TO_UPPER_CASE, List.of(
            new QRecord().withValue(FIELD, "John"),
            new QRecord(),
            new QRecord().withValue(FIELD, "Jane")),
         ValueBehaviorApplier.Action.INSERT).stream().map(r -> r.getValueString(FIELD)).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testToLowerCase()
   {
      assertNull(applyToRecord(CaseChangeBehavior.TO_LOWER_CASE, new QRecord(), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));
      assertNull(applyToRecord(CaseChangeBehavior.TO_LOWER_CASE, new QRecord().withValue(FIELD, null), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));
      assertEquals("john", applyToRecord(CaseChangeBehavior.TO_LOWER_CASE, new QRecord().withValue(FIELD, "John"), ValueBehaviorApplier.Action.INSERT).getValue(FIELD));

      assertEquals(ListBuilder.of("john", null, "jane"), applyToRecords(CaseChangeBehavior.TO_LOWER_CASE, List.of(
            new QRecord().withValue(FIELD, "John"),
            new QRecord(),
            new QRecord().withValue(FIELD, "Jane")),
         ValueBehaviorApplier.Action.INSERT).stream().map(r -> r.getValueString(FIELD)).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord applyToRecord(CaseChangeBehavior behavior, QRecord record, ValueBehaviorApplier.Action action)
   {
      return (applyToRecords(behavior, List.of(record), action).get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> applyToRecords(CaseChangeBehavior behavior, List<QRecord> records, ValueBehaviorApplier.Action action)
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      behavior.apply(action, records, QContext.getQInstance(), table, table.getField(FIELD));
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReads() throws QException
   {
      TestUtils.insertDefaultShapes(QContext.getQInstance());

      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_SHAPE, null);
      assertEquals(Set.of("Triangle", "Square", "Circle"), records.stream().map(r -> r.getValueString("name")).collect(Collectors.toSet()));

      QFieldMetaData field = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_SHAPE).getField("name");
      field.setBehaviors(Set.of(CaseChangeBehavior.TO_UPPER_CASE));

      records = QueryAction.execute(TestUtils.TABLE_NAME_SHAPE, null);
      assertEquals(Set.of("TRIANGLE", "SQUARE", "CIRCLE"), records.stream().map(r -> r.getValueString("name")).collect(Collectors.toSet()));

      field.setBehaviors(Set.of(CaseChangeBehavior.TO_LOWER_CASE));
      assertEquals("triangle", GetAction.execute(TestUtils.TABLE_NAME_SHAPE, 1).getValueString("name"));

      field.setBehaviors(Set.of(CaseChangeBehavior.NONE));
      assertEquals("Triangle", GetAction.execute(TestUtils.TABLE_NAME_SHAPE, 1).getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWrites() throws QException
   {
      Integer id = 100;

      QFieldMetaData field = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_SHAPE).getField("name");
      field.setBehaviors(Set.of(CaseChangeBehavior.TO_UPPER_CASE));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SHAPE).withRecord(new QRecord().withValue("id", id).withValue("name", "Octagon")));

      //////////////////////////////////////////////////////////////////////////////////
      // turn off the to-upper-case behavior, so we'll see what was actually inserted //
      //////////////////////////////////////////////////////////////////////////////////
      field.setBehaviors(Collections.emptySet());
      assertEquals("OCTAGON", GetAction.execute(TestUtils.TABLE_NAME_SHAPE, id).getValueString("name"));

      ////////////////////////////////////////////
      // change to toLowerCase and do an update //
      ////////////////////////////////////////////
      field.setBehaviors(Set.of(CaseChangeBehavior.TO_LOWER_CASE));
      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_SHAPE).withRecord(new QRecord().withValue("id", id).withValue("name", "Octagon")));

      ////////////////////////////////////////////////////////////////////////////////////
      // turn off the to-lower-case behavior, so we'll see what was actually udpated to //
      ////////////////////////////////////////////////////////////////////////////////////
      field.setBehaviors(Collections.emptySet());
      assertEquals("octagon", GetAction.execute(TestUtils.TABLE_NAME_SHAPE, id).getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilter()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);
      QFieldMetaData field     = table.getField("name");
      field.setBehaviors(Set.of(CaseChangeBehavior.TO_UPPER_CASE));
      assertEquals("SQUARE", CaseChangeBehavior.TO_UPPER_CASE.applyToFilterCriteriaValue("square", qInstance, table, field));

      field.setBehaviors(Set.of(CaseChangeBehavior.TO_LOWER_CASE));
      assertEquals("triangle", CaseChangeBehavior.TO_LOWER_CASE.applyToFilterCriteriaValue("Triangle", qInstance, table, field));

      field.setBehaviors(Set.of(CaseChangeBehavior.NONE));
      assertEquals("Circle", CaseChangeBehavior.NONE.applyToFilterCriteriaValue("Circle", qInstance, table, field));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidation()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_SHAPE);

      ///////////////////////////////////////////
      // should be no errors on a string field //
      ///////////////////////////////////////////
      assertTrue(CaseChangeBehavior.TO_UPPER_CASE.validateBehaviorConfiguration(table, table.getField("name")).isEmpty());

      //////////////////////////////////////////
      // should be an error on a number field //
      //////////////////////////////////////////
      assertEquals(1, CaseChangeBehavior.TO_LOWER_CASE.validateBehaviorConfiguration(table, table.getField("id")).size());

      /////////////////////////////////////////
      // NONE should be allowed on any field //
      /////////////////////////////////////////
      assertTrue(CaseChangeBehavior.NONE.validateBehaviorConfiguration(table, table.getField("id")).isEmpty());
   }

}