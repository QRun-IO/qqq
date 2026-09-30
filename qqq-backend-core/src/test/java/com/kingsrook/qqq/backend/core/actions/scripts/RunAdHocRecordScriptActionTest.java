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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.Log4jCodeExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.RunAdHocScriptCustomizers;
import com.kingsrook.qqq.backend.core.model.metadata.code.AdHocScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for RunAdHocRecordScriptAction
 *******************************************************************************/
class RunAdHocRecordScriptActionTest extends BaseTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testScriptRevisionNotFound() throws QException
   {
      setupInstance();

      RunAdHocRecordScriptInput runAdHocRecordScriptInput = new RunAdHocRecordScriptInput();
      runAdHocRecordScriptInput.setRecordPrimaryKeyList(List.of(1));
      runAdHocRecordScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAdHocRecordScriptInput.setCodeReference(new AdHocScriptCodeReference().withScriptRevisionId(-1));
      runAdHocRecordScriptInput.setLogger(new Log4jCodeExecutionLogger());

      RunAdHocRecordScriptOutput runAdHocRecordScriptOutput = new RunAdHocRecordScriptOutput();

      new RunAdHocRecordScriptAction().run(runAdHocRecordScriptInput, runAdHocRecordScriptOutput);
      assertThat(runAdHocRecordScriptOutput.getException()).isPresent().get()
         .isInstanceOf(QException.class)
         .hasFieldOrPropertyWithValue("message", "Script revision was not found.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @Disabled("Doesn't work, because javascript module not available to backend-core")
   public void testCustomizer() throws QException
   {
      setupInstance();
      QContext.getQInstance().addSupplementalCustomizer(RunAdHocScriptCustomizers.PRE_EXECUTE, new QCodeReference(TestCustomizer.class));

      ////////////////////////////
      // insert script revision //
      ////////////////////////////
      Integer scriptRevisionId = insertScriptRevision("""
         return "Hello";
         """);

      /////////////////
      // setup input //
      /////////////////
      RunAdHocRecordScriptInput runAdHocRecordScriptInput = new RunAdHocRecordScriptInput();
      runAdHocRecordScriptInput.setRecordPrimaryKeyList(List.of(1));
      runAdHocRecordScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAdHocRecordScriptInput.setCodeReference(new AdHocScriptCodeReference().withScriptRevisionId(scriptRevisionId));
      runAdHocRecordScriptInput.setLogger(new Log4jCodeExecutionLogger());

      ////////////////////////////
      // ensure no script utils //
      ////////////////////////////
      assertThat(runAdHocRecordScriptInput.getScriptUtils()).isNull();

      ///////////////////////////////////
      // set customizer and run action //
      ///////////////////////////////////
      RunAdHocRecordScriptOutput runAdHocRecordScriptOutput = new RunAdHocRecordScriptOutput();
      new RunAdHocRecordScriptAction().run(runAdHocRecordScriptInput, runAdHocRecordScriptOutput);

      ////////////////////////////////
      // assert customizer was used //
      ////////////////////////////////
      assertThat(runAdHocRecordScriptInput.getScriptUtils()).isEqualTo("test");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @Disabled("Doesn't work, because javascript module not available to backend-core")
   void test() throws QException
   {
      setupInstance();

      Integer scriptRevisionId = insertScriptRevision("""
         return "Hello";
         """);

      RunAdHocRecordScriptInput runAdHocRecordScriptInput = new RunAdHocRecordScriptInput();
      runAdHocRecordScriptInput.setRecordPrimaryKeyList(List.of(1));
      runAdHocRecordScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAdHocRecordScriptInput.setCodeReference(new AdHocScriptCodeReference().withScriptRevisionId(scriptRevisionId));
      runAdHocRecordScriptInput.setLogger(new Log4jCodeExecutionLogger());

      RunAdHocRecordScriptOutput runAdHocRecordScriptOutput = new RunAdHocRecordScriptOutput();
      new RunAdHocRecordScriptAction().run(runAdHocRecordScriptInput, runAdHocRecordScriptOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class TestCustomizer implements RunAdHocRecordScriptCustomizerInterface
   {
      @Override
      public void preExecuteCode(RunAdHocRecordScriptInput input)
      {
         input.setScriptUtils("test");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setupInstance() throws QException
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData personMemory = instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withField(new QFieldMetaData("testScriptId", QFieldType.INTEGER))
         .withAssociatedScript(new AssociatedScript()
            .withScriptTypeId(1)
            .withFieldName("testScriptId")
         );

      new ScriptsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);

      TestUtils.insertRecords(personMemory, List.of(
         new QRecord().withValue("id", 1),
         new QRecord().withValue("id", 2)
      ));

      TestUtils.insertRecords(instance.getTable("scriptType"), List.of(
         new QRecord().withValue("id", 1).withValue("name", "Test Script Type")
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insertScriptRevision(String code) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("script");
      insertInput.setRecords(List.of(new QRecord().withValue("name", "Test script")));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      Integer      scriptId     = insertOutput.getRecords().get(0).getValueInteger("id");

      insertInput = new InsertInput();
      insertInput.setTableName("scriptRevision");
      insertInput.setRecords(List.of(new QRecord().withValue("scriptId", scriptId).withValue("code", code)));
      insertOutput = new InsertAction().execute(insertInput);
      Integer scriptRevisionId = insertOutput.getRecords().get(0).getValueInteger("id");

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName("script");
      updateInput.setRecords(List.of(new QRecord().withValue("id", scriptId).withValue("currentScriptRevisionId", scriptRevisionId)));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);

      return (scriptRevisionId);
   }
}
