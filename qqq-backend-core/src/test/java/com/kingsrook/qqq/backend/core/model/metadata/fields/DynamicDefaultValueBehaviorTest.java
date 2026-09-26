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


import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for DynamicDefaultValueBehavior
 *******************************************************************************/
class DynamicDefaultValueBehaviorTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCreateDateHappyPath()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      QRecord record = new QRecord().withValue("id", 1);
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, List.of(record), null);

      assertNotNull(record.getValue("createDate"));
      assertNotNull(record.getValue("modifyDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testModifyDateHappyPath()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      QRecord record = new QRecord().withValue("id", 1);
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.UPDATE, qInstance, table, List.of(record), null);

      assertNull(record.getValue("createDate"));
      assertNotNull(record.getValue("modifyDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOmitModifyDateUpdate()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      Set<FieldBehavior<?>> behaviorsToOmit = Set.of(DynamicDefaultValueBehavior.MODIFY_DATE);
      QRecord               record          = new QRecord().withValue("id", 1);
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.UPDATE, qInstance, table, List.of(record), behaviorsToOmit);

      assertNull(record.getValue("createDate"));
      assertNull(record.getValue("modifyDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNone()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("createDate").withBehavior(DynamicDefaultValueBehavior.NONE);
      table.getField("modifyDate").withBehavior(DynamicDefaultValueBehavior.NONE);

      QRecord record = new QRecord().withValue("id", 1);

      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, List.of(record), null);
      assertNull(record.getValue("createDate"));
      assertNull(record.getValue("modifyDate"));

      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.UPDATE, qInstance, table, List.of(record), null);
      assertNull(record.getValue("createDate"));
      assertNull(record.getValue("modifyDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDateInsteadOfDateTimeField()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("createDate").withType(QFieldType.DATE);

      QRecord record = new QRecord().withValue("id", 1);
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, List.of(record), null);
      assertNotNull(record.getValue("createDate"));
      assertThat(record.getValue("createDate")).isInstanceOf(LocalDate.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonDateField()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("firstName").withBehavior(DynamicDefaultValueBehavior.CREATE_DATE);

      QRecord record = new QRecord().withValue("id", 1);
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, List.of(record), null);
      assertNull(record.getValue("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUserId()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("firstName").withBehavior(DynamicDefaultValueBehavior.USER_ID);

      {
         ////////////////////////////////
         // set it (if null) on insert //
         ////////////////////////////////
         QRecord record = new QRecord().withValue("id", 1);
         ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, List.of(record), null);
         assertEquals(QContext.getQSession().getUser().getIdReference(), record.getValue("firstName"));
      }

      {
         ////////////////////////////////
         // set it (if null) on update //
         ////////////////////////////////
         QRecord record = new QRecord().withValue("id", 1);
         ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.UPDATE, qInstance, table, List.of(record), null);
         assertEquals(QContext.getQSession().getUser().getIdReference(), record.getValue("firstName"));
      }

      {
         ////////////////////////////////////////////////////////////////////
         // only set it if it wasn't previously set (both insert & update) //
         ////////////////////////////////////////////////////////////////////
         QRecord record = new QRecord().withValue("id", 1).withValue("firstName", "Bob");
         ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, List.of(record), null);
         assertEquals("Bob", record.getValue("firstName"));

         ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.UPDATE, qInstance, table, List.of(record), null);
         assertEquals("Bob", record.getValue("firstName"));
      }
   }



   /*******************************************************************************
    ** Reading a record must not attribute a missing stored user ID to the reader.
    *******************************************************************************/
   @Test
   void testUserIdDoesNotDefaultOnRead()
   {
      QInstance qInstance = QContext.getQInstance();
      QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("firstName").withBehavior(DynamicDefaultValueBehavior.USER_ID);
      QRecord missing = new QRecord().withValue("id", 1);
      QRecord empty = new QRecord().withValue("id", 2).withValue("firstName", "");
      QRecord explicit = new QRecord().withValue("id", 3).withValue("firstName", "stored-user");

      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.READ, qInstance, table, List.of(missing, empty, explicit), null);
      assertNull(missing.getValue("firstName"));
      assertEquals("", empty.getValue("firstName"));
      assertEquals("stored-user", explicit.getValue("firstName"));
   }

}
