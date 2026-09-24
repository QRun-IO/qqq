/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.automation;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Unit tests for RecordAutomationInput — verifying fluent builder contracts
 ** and the record-list / action-field round-trips.
 *******************************************************************************/
class RecordAutomationInputTest
{

   /*******************************************************************************
    ** No-arg constructor should leave action and recordList null.
    *******************************************************************************/
   @Test
   void testNoArgConstructor_fieldsAreNull()
   {
      RecordAutomationInput input = new RecordAutomationInput();

      assertNull(input.getAction());
      assertNull(input.getRecordList());
   }



   /*******************************************************************************
    ** setAction / getAction round-trip.
    *******************************************************************************/
   @Test
   void testSetAction_getAction_roundTrip()
   {
      RecordAutomationInput  input  = new RecordAutomationInput();
      TableAutomationAction  action = new TableAutomationAction();
      input.setAction(action);

      assertSame(action, input.getAction());
   }



   /*******************************************************************************
    ** withAction fluent builder should return the same instance.
    *******************************************************************************/
   @Test
   void testWithAction_returnsSameInstance()
   {
      RecordAutomationInput input = new RecordAutomationInput();
      TableAutomationAction action = new TableAutomationAction();

      RecordAutomationInput returned = input.withAction(action);

      assertSame(input, returned);
      assertSame(action, input.getAction());
   }



   /*******************************************************************************
    ** setRecordList / getRecordList should preserve the exact list reference.
    *******************************************************************************/
   @Test
   void testSetRecordList_getRecordList_sameReference()
   {
      RecordAutomationInput input  = new RecordAutomationInput();
      List<QRecord>         records = List.of(new QRecord(), new QRecord());
      input.setRecordList(records);

      assertSame(records, input.getRecordList());
   }



   /*******************************************************************************
    ** withRecordList fluent builder should return the same instance.
    *******************************************************************************/
   @Test
   void testWithRecordList_returnsSameInstance()
   {
      RecordAutomationInput input   = new RecordAutomationInput();
      List<QRecord>         records = List.of(new QRecord());

      RecordAutomationInput returned = input.withRecordList(records);

      assertSame(input, returned);
      assertSame(records, input.getRecordList());
   }



   /*******************************************************************************
    ** Fluent chain: withAction + withRecordList should set both fields.
    *******************************************************************************/
   @Test
   void testFluentChain_bothFieldsSet()
   {
      TableAutomationAction action  = new TableAutomationAction();
      List<QRecord>         records = List.of(new QRecord());

      RecordAutomationInput input = new RecordAutomationInput()
         .withAction(action)
         .withRecordList(records);

      assertSame(action, input.getAction());
      assertThat(input.getRecordList()).hasSize(1);
   }



   /*******************************************************************************
    ** Setting recordList to an empty list should be stored, not treated as null.
    *******************************************************************************/
   @Test
   void testSetRecordList_emptyList_storedNotNull()
   {
      RecordAutomationInput input = new RecordAutomationInput();
      input.setRecordList(List.of());

      assertThat(input.getRecordList()).isNotNull().isEmpty();
   }

}
