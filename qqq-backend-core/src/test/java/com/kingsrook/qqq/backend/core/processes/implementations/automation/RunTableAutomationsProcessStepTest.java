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

package com.kingsrook.qqq.backend.core.processes.implementations.automation;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RunTableAutomationsProcessStep 
 *******************************************************************************/
class RunTableAutomationsProcessStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws Exception
   {
      UnsafeSupplier<Integer, ?> getAutomationStatus = () -> new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(1)).getValueInteger("qqqAutomationStatus");

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), getAutomationStatus.get());

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      RunBackendStepOutput output = new RunBackendStepOutput();
      new RunTableAutomationsProcessStep().run(input, output);
      assertEquals("true", output.getValue("ok"));

      assertEquals(AutomationStatus.OK.getId(), getAutomationStatus.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testThrowsWithoutTableName() throws QException
   {
      RunBackendStepInput  input  = new RunBackendStepInput();
      RunBackendStepOutput output = new RunBackendStepOutput();
      assertThatThrownBy(() -> new RunTableAutomationsProcessStep().run(input, output))
         .hasMessageContaining("Missing required input value: tableName");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testThrowsWithInvalidTableName() throws QException
   {
      RunBackendStepInput  input  = new RunBackendStepInput();
      RunBackendStepOutput output = new RunBackendStepOutput();
      input.addValue("tableName", "asdf");
      assertThatThrownBy(() -> new RunTableAutomationsProcessStep().run(input, output))
         .hasMessageContaining("Unrecognized table name: asdf");
   }

}