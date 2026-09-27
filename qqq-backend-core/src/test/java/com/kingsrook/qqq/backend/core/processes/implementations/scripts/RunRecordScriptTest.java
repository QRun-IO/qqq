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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Test full process of RunRecordScript (should its Extract and Load classes).
 *******************************************************************************/
class RunRecordScriptTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new ScriptsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(new QRecord().withValue("id", 1)));
      TestUtils.insertRecords(qInstance.getTable(Script.TABLE_NAME), List.of(new QRecord().withValue("id", 1).withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)));

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(ScriptsMetaDataProvider.RUN_RECORD_SCRIPT_PROCESS_NAME);
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      runProcessInput.setValues(MapBuilder.of(
         "recordIds", "1,2,3",
         "tableName", TestUtils.TABLE_NAME_PERSON_MEMORY,
         "scriptId", 1
      ));

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // so, turns out, we don't know how to do a join yet in memory backend, so this can't quite work //
      // still good to run the code and at least get this far w/ an expected exception.                //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

      @SuppressWarnings("unchecked")
      List<ProcessSummaryLineInterface> processResults = (List<ProcessSummaryLineInterface>) runProcessOutput.getValues().get("processResults");
      assertTrue(processResults.stream().anyMatch(psli -> psli instanceof ProcessSummaryLine psl && psl.getMessage().contains("error that was not logged")));
   }

}