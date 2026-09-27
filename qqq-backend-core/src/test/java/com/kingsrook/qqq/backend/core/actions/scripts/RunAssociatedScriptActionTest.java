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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.AccumulatingBuildScriptLogAndScriptLogLineExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAssociatedScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.AssociatedScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptLog;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RunAssociatedScriptAction
 *******************************************************************************/
class RunAssociatedScriptActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
      MemoryRecordStore.resetStatistics();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      setupInstance();

      insertScript(1, """
         return "Hello";
         """);

      RunAssociatedScriptInput runAssociatedScriptInput = new RunAssociatedScriptInput();
      runAssociatedScriptInput.setInputValues(Map.of());
      runAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAssociatedScriptInput.setCodeReference(new AssociatedScriptCodeReference()
         .withRecordTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecordPrimaryKey(1)
         .withFieldName("testScriptId")
      );
      RunAssociatedScriptOutput runAssociatedScriptOutput = new RunAssociatedScriptOutput();

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // ok - since the core module doesn't have the javascript language support module as a dep, this action will fail - but at least we can confirm it fails with this specific exception! //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> new RunAssociatedScriptAction().run(runAssociatedScriptInput, runAssociatedScriptOutput))
         .isInstanceOf(QException.class)
         .hasRootCauseInstanceOf(ClassNotFoundException.class)
         .hasRootCauseMessage("com.kingsrook.qqq.languages.javascript.QJavaScriptExecutor");

      /////////////////////////////////////
      // assert that a log was generated //
      /////////////////////////////////////
      assertEquals(1, TestUtils.queryTable(ScriptLog.TABLE_NAME).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOverridingLoggerAndCachingScriptLookups() throws QException
   {
      setupInstance();

      insertScript(1, """
         return "Hello";
         """);

      AccumulatingBuildScriptLogAndScriptLogLineExecutionLogger scriptLogger = new AccumulatingBuildScriptLogAndScriptLogLineExecutionLogger();

      RunAssociatedScriptInput runAssociatedScriptInput = new RunAssociatedScriptInput();
      runAssociatedScriptInput.setInputValues(Map.of());
      runAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAssociatedScriptInput.setLogger(scriptLogger);
      runAssociatedScriptInput.setCodeReference(new AssociatedScriptCodeReference()
         .withRecordTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecordPrimaryKey(1)
         .withFieldName("testScriptId")
      );
      RunAssociatedScriptOutput runAssociatedScriptOutput = new RunAssociatedScriptOutput();

      MemoryRecordStore.setCollectStatistics(true);
      RunAssociatedScriptAction runAssociatedScriptAction = new RunAssociatedScriptAction();

      int N = 10;
      for(int i = 0; i < N; i++)
      {
         assertThatThrownBy(() -> runAssociatedScriptAction.run(runAssociatedScriptInput, runAssociatedScriptOutput));
      }

      scriptLogger.storeAndClear();

      /////////////////////////////////////
      // assert that logs were generated //
      /////////////////////////////////////
      assertEquals(N, TestUtils.queryTable(ScriptLog.TABLE_NAME).size());

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // and we should have just ran 1 inserts - for the log (no longer run one for empty insert of 0 log-lines) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(1, MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_INSERTS_RAN));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // and we shouldn't have run N queries (which we would have (at least), if we would have built a new Action object inside the loop) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(MemoryRecordStore.getStatistics().get(MemoryRecordStore.STAT_QUERIES_RAN)).isLessThan(N);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setupInstance() throws QException
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withField(new QFieldMetaData("testScriptId", QFieldType.INTEGER))
         .withAssociatedScript(new AssociatedScript()
            .withScriptTypeId(1)
            .withFieldName("testScriptId")
         );

      new ScriptsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);

      TestUtils.insertRecords(table, List.of(
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
   @Test
   void testRecordNotFound() throws QException
   {
      setupInstance();

      RunAssociatedScriptInput runAssociatedScriptInput = new RunAssociatedScriptInput();
      runAssociatedScriptInput.setInputValues(Map.of());
      runAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAssociatedScriptInput.setCodeReference(new AssociatedScriptCodeReference()
         .withRecordTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecordPrimaryKey(-9999)
         .withFieldName("testScriptId")
      );
      RunAssociatedScriptOutput runAssociatedScriptOutput = new RunAssociatedScriptOutput();

      assertThatThrownBy(() -> new RunAssociatedScriptAction().run(runAssociatedScriptInput, runAssociatedScriptOutput))
         .isInstanceOf(QNotFoundException.class)
         .hasMessageMatching("The requested record.*was not found.*");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoScriptInRecord() throws QException
   {
      setupInstance();

      RunAssociatedScriptInput runAssociatedScriptInput = new RunAssociatedScriptInput();
      runAssociatedScriptInput.setInputValues(Map.of());
      runAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAssociatedScriptInput.setCodeReference(new AssociatedScriptCodeReference()
         .withRecordTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecordPrimaryKey(1)
         .withFieldName("testScriptId")
      );
      RunAssociatedScriptOutput runAssociatedScriptOutput = new RunAssociatedScriptOutput();

      assertThatThrownBy(() -> new RunAssociatedScriptAction().run(runAssociatedScriptInput, runAssociatedScriptOutput))
         .isInstanceOf(QNotFoundException.class)
         .hasMessageMatching("The input record.*does not have a script specified for.*");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadScriptIdInRecord() throws QException
   {
      setupInstance();

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("testScriptId", -9998)));
      new UpdateAction().execute(updateInput);

      RunAssociatedScriptInput runAssociatedScriptInput = new RunAssociatedScriptInput();
      runAssociatedScriptInput.setInputValues(Map.of());
      runAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAssociatedScriptInput.setCodeReference(new AssociatedScriptCodeReference()
         .withRecordTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecordPrimaryKey(1)
         .withFieldName("testScriptId")
      );
      RunAssociatedScriptOutput runAssociatedScriptOutput = new RunAssociatedScriptOutput();

      assertThatThrownBy(() -> new RunAssociatedScriptAction().run(runAssociatedScriptInput, runAssociatedScriptOutput))
         .isInstanceOf(QNotFoundException.class)
         .hasMessageMatching("The script for record .* was not found.*");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoCurrentScriptRevisionOnScript() throws QException
   {
      setupInstance();

      insertScript(1, """
         return "Hello";
         """);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      GetOutput getOutput = new GetAction().execute(getInput);
      Integer   scriptId  = getOutput.getRecord().getValueInteger("testScriptId");

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName("script");
      updateInput.setRecords(List.of(new QRecord().withValue("id", scriptId).withValue("currentScriptRevisionId", null)));
      new UpdateAction().execute(updateInput);

      RunAssociatedScriptInput runAssociatedScriptInput = new RunAssociatedScriptInput();
      runAssociatedScriptInput.setInputValues(Map.of());
      runAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAssociatedScriptInput.setCodeReference(new AssociatedScriptCodeReference()
         .withRecordTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecordPrimaryKey(1)
         .withFieldName("testScriptId")
      );
      RunAssociatedScriptOutput runAssociatedScriptOutput = new RunAssociatedScriptOutput();

      assertThatThrownBy(() -> new RunAssociatedScriptAction().run(runAssociatedScriptInput, runAssociatedScriptOutput))
         .isInstanceOf(QNotFoundException.class)
         .hasMessageMatching("The script for record .* does not have a current version.*");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadCurrentScriptRevisionOnScript() throws QException
   {
      setupInstance();

      insertScript(1, """
         return "Hello";
         """);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      GetOutput getOutput = new GetAction().execute(getInput);
      Integer   scriptId  = getOutput.getRecord().getValueInteger("testScriptId");

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName("script");
      updateInput.setRecords(List.of(new QRecord().withValue("id", scriptId).withValue("currentScriptRevisionId", 9997)));
      new UpdateAction().execute(updateInput);

      RunAssociatedScriptInput runAssociatedScriptInput = new RunAssociatedScriptInput();
      runAssociatedScriptInput.setInputValues(Map.of());
      runAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      runAssociatedScriptInput.setCodeReference(new AssociatedScriptCodeReference()
         .withRecordTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecordPrimaryKey(1)
         .withFieldName("testScriptId")
      );
      RunAssociatedScriptOutput runAssociatedScriptOutput = new RunAssociatedScriptOutput();

      assertThatThrownBy(() -> new RunAssociatedScriptAction().run(runAssociatedScriptInput, runAssociatedScriptOutput))
         .isInstanceOf(QNotFoundException.class)
         .hasMessageMatching("The current revision of the script for record .* was not found.*");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void insertScript(Serializable recordId, String code) throws QException
   {
      StoreAssociatedScriptInput storeAssociatedScriptInput = new StoreAssociatedScriptInput();
      storeAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      storeAssociatedScriptInput.setRecordPrimaryKey(recordId);
      storeAssociatedScriptInput.setCode(code);
      storeAssociatedScriptInput.setFieldName("testScriptId");
      StoreAssociatedScriptOutput storeAssociatedScriptOutput = new StoreAssociatedScriptOutput();
      new StoreAssociatedScriptAction().run(storeAssociatedScriptInput, storeAssociatedScriptOutput);
   }

}