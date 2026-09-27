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

package com.kingsrook.qqq.backend.core.processes.implementations.scripts;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.scripts.RecordScriptTestInterface;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for LoadScriptTestDetailsProcessStep 
 *******************************************************************************/
class LoadScriptTestDetailsProcessStepTest extends BaseTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new ScriptsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(ScriptType.TABLE_NAME);
      insertInput.setRecords(List.of(new ScriptType()
         .withName("TestScriptType")
         .withTestScriptInterfaceName(RecordScriptTestInterface.class.getName())
         .toQRecord()));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("scriptTypeId", insertOutput.getRecords().get(0).getValueInteger("id"));
      RunBackendStepOutput output = new RunBackendStepOutput();
      new LoadScriptTestDetailsProcessStep().run(input, output);

      Serializable inputFields = output.getValue("testInputFields");
      assertThat(inputFields).isInstanceOf(List.class);
      @SuppressWarnings("unchecked")
      List<QFieldMetaData> inputFieldsList = (List<QFieldMetaData>) inputFields;
      assertEquals(1, inputFieldsList.size());
      assertEquals("recordPrimaryKeyList", inputFieldsList.get(0).getName());
   }

}