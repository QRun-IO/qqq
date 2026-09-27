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
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevisionFile;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for StoreAssociatedScriptAction
 *******************************************************************************/
class StoreAssociatedScriptActionTest extends BaseTest
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
   void test() throws QException
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = instance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withField(new QFieldMetaData("testScriptId", QFieldType.INTEGER))
         .withAssociatedScript(new AssociatedScript()
            .withScriptTypeId(1)
            .withFieldName("testScriptId")
         )
         .withField(new QFieldMetaData("otherScriptId", QFieldType.INTEGER))
         .withAssociatedScript(new AssociatedScript()
            .withScriptTypeId(2)
            .withFieldName("otherScriptId")
         );

      new ScriptsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);

      TestUtils.insertRecords(table, List.of(
         new QRecord().withValue("id", 1),
         new QRecord().withValue("id", 2),
         new QRecord().withValue("id", 3)
      ));

      TestUtils.insertRecords(instance.getTable("scriptType"), List.of(
         new QRecord().withValue("id", 1).withValue("name", "Test Script"),
         new QRecord().withValue("id", 2).withValue("name", "Other Script")
      ));

      StoreAssociatedScriptInput storeAssociatedScriptInput = new StoreAssociatedScriptInput();
      storeAssociatedScriptInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      storeAssociatedScriptInput.setRecordPrimaryKey(1);
      String code = "var i = 0;";
      storeAssociatedScriptInput.setCode(code);
      storeAssociatedScriptInput.setCommitMessage("Test commit");
      storeAssociatedScriptInput.setFieldName("testScriptId");
      StoreAssociatedScriptOutput storeAssociatedScriptOutput = new StoreAssociatedScriptOutput();

      ///////////////////////////////////////////////
      // insert 1st version of script for record 1 //
      ///////////////////////////////////////////////
      new StoreAssociatedScriptAction().run(storeAssociatedScriptInput, storeAssociatedScriptOutput);
      assertValueInField(instance, TestUtils.TABLE_NAME_PERSON_MEMORY, 1, "testScriptId", 1);
      assertValueInField(instance, Script.TABLE_NAME, 1, "currentScriptRevisionId", 1);
      assertValueInField(instance, ScriptRevisionFile.TABLE_NAME, 1, "contents", code);

      ////////////////////////////////////////////
      // add 2nd version of script for record 1 //
      ////////////////////////////////////////////
      code = "var i = 1;";
      storeAssociatedScriptInput.setCode(code);
      storeAssociatedScriptInput.setCommitMessage("2nd commit");
      new StoreAssociatedScriptAction().run(storeAssociatedScriptInput, storeAssociatedScriptOutput);
      assertValueInField(instance, TestUtils.TABLE_NAME_PERSON_MEMORY, 1, "testScriptId", 1);
      assertValueInField(instance, Script.TABLE_NAME, 1, "currentScriptRevisionId", 2);
      assertValueInField(instance, ScriptRevisionFile.TABLE_NAME, 2, "contents", code);
      assertValueInField(instance, ScriptRevisionFile.TABLE_NAME, 2, "scriptRevisionId", 2);

      ///////////////////////////////////////////////
      // insert 1st version of script for record 3 //
      ///////////////////////////////////////////////
      code = "var i = 2;";
      storeAssociatedScriptInput.setRecordPrimaryKey(3);
      storeAssociatedScriptInput.setCode(code);
      storeAssociatedScriptInput.setCommitMessage("First Commit here");
      new StoreAssociatedScriptAction().run(storeAssociatedScriptInput, storeAssociatedScriptOutput);
      assertValueInField(instance, TestUtils.TABLE_NAME_PERSON_MEMORY, 3, "testScriptId", 2);
      assertValueInField(instance, Script.TABLE_NAME, 2, "currentScriptRevisionId", 3);
      assertValueInField(instance, ScriptRevisionFile.TABLE_NAME, 3, "contents", code);
      assertValueInField(instance, ScriptRevisionFile.TABLE_NAME, 3, "scriptRevisionId", 3);

      /////////////////////////////////////
      // make sure no script on record 2 //
      /////////////////////////////////////
      assertValueInField(instance, TestUtils.TABLE_NAME_PERSON_MEMORY, 2, "testScriptId", null);

      ////////////////////////////////////
      // add another script to record 1 //
      ////////////////////////////////////
      storeAssociatedScriptInput.setRecordPrimaryKey(1);
      storeAssociatedScriptInput.setCode("var i = 3;");
      storeAssociatedScriptInput.setCommitMessage("Other field");
      storeAssociatedScriptInput.setFieldName("otherScriptId");
      new StoreAssociatedScriptAction().run(storeAssociatedScriptInput, storeAssociatedScriptOutput);
      assertValueInField(instance, TestUtils.TABLE_NAME_PERSON_MEMORY, 1, "testScriptId", 1);
      assertValueInField(instance, TestUtils.TABLE_NAME_PERSON_MEMORY, 1, "otherScriptId", 3);
      assertValueInField(instance, Script.TABLE_NAME, 3, "currentScriptRevisionId", 4);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertValueInField(QInstance instance, String tableName, Serializable recordId, String fieldName, Serializable value) throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName(tableName);
      getInput.setPrimaryKey(recordId);
      GetOutput getOutput = new GetAction().execute(getInput);
      if(getOutput.getRecord() == null)
      {
         fail("Expected value [" + value + "] in field [" + fieldName + "], record [" + tableName + "][" + recordId + "], but the record wasn't found...");
      }
      Serializable actual = getOutput.getRecord().getValue(fieldName);
      assertEquals(value, actual, "Expected value in field [" + fieldName + "], record [" + tableName + "][" + recordId + "]");
   }

}