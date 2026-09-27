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


import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.automation.PollingAutomationProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.automation.QAutomationProviderMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


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



   /*******************************************************************************
    ** A typo must fail before status changes, and the same pending row can recover.
    *******************************************************************************/
   @Test
   void testUnknownExplicitProviderRejectsAndValidRetryProcessesPendingRecord() throws Exception
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("automationProviderName", "missingProvider");
      RunBackendStepOutput output = new RunBackendStepOutput();
      QException failure = assertThrows(QException.class, () -> new RunTableAutomationsProcessStep().run(input, output));
      assertEquals("Unrecognized automationProviderName: missingProvider", failure.getMessage());
      assertNull(output.getValue("ok"));
      assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), status(1));
      input.addValue("automationProviderName", TestUtils.POLLING_AUTOMATION);
      new RunTableAutomationsProcessStep().run(input, output);
      assertEquals("true", output.getValue("ok"));
      assertEquals(AutomationStatus.OK.getId(), status(1));
   }



   /*******************************************************************************
    ** Null, empty and whitespace retain the existing single-provider default.
    *******************************************************************************/
   @Test
   void testBlankProviderUsesSoleConfiguredProvider() throws Exception
   {
      for(String provider : Arrays.asList(null, "", " \t"))
      {
         QRecord inserted = InsertAction.executeForRecords(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord())).get(0);
         Integer id = inserted.getValueInteger("id");
         assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), status(id));
         RunBackendStepInput input = new RunBackendStepInput();
         input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
         input.addValue("automationProviderName", provider);
         RunBackendStepOutput output = new RunBackendStepOutput();
         new RunTableAutomationsProcessStep().run(input, output);
         assertEquals("true", output.getValue("ok"));
         assertEquals(AutomationStatus.OK.getId(), status(id));
      }
   }



   /*******************************************************************************
    ** Zero or multiple providers must not acquire a new implicit default.
    *******************************************************************************/
   @Test
   void testImplicitProviderRequiresExactlyOneConfiguredProvider() throws Exception
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      Map<String, QAutomationProviderMetaData> providers = QContext.getQInstance().getAutomationProviders();
      Map<String, QAutomationProviderMetaData> original = new LinkedHashMap<>(providers);
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      RunBackendStepOutput output = new RunBackendStepOutput();
      providers.clear();
      QException missing = assertThrows(QException.class, () -> new RunTableAutomationsProcessStep().run(input, output));
      assertEquals("Missing required input value: automationProviderName (and there is not exactly 1 in the active instance)", missing.getMessage());
      assertNull(output.getValue("ok"));
      assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), status(1));
      providers.putAll(original);
      QContext.getQInstance().addAutomationProvider(new PollingAutomationProviderMetaData().withName("otherProvider"));
      input.addValue("automationProviderName", " ");
      QException ambiguous = assertThrows(QException.class, () -> new RunTableAutomationsProcessStep().run(input, output));
      assertEquals(missing.getMessage(), ambiguous.getMessage());
      assertNull(output.getValue("ok"));
      assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), status(1));
   }



   /*******************************************************************************
    ** A known provider without matching table work retains the existing no-op.
    *******************************************************************************/
   @Test
   void testKnownProviderWithNoApplicableWorkRemainsNoOp() throws Exception
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      QContext.getQInstance().addAutomationProvider(new PollingAutomationProviderMetaData().withName("otherProvider"));
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("automationProviderName", "otherProvider");
      RunBackendStepOutput output = new RunBackendStepOutput();
      new RunTableAutomationsProcessStep().run(input, output);
      assertEquals("true", output.getValue("ok"));
      assertEquals(AutomationStatus.PENDING_INSERT_AUTOMATIONS.getId(), status(1));
   }



   /*******************************************************************************
    ** Read status through the public record API, not the runner implementation.
    *******************************************************************************/
   private Integer status(Integer id) throws QException
   {
      return (new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(id)).getValueInteger("qqqAutomationStatus"));
   }

}