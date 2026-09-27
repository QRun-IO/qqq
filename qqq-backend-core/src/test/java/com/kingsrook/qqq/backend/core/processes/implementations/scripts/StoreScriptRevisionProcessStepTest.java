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
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevisionFile;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for StoreScriptRevisionProcessStep
 *******************************************************************************/
class StoreScriptRevisionProcessStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSingleFileScriptType() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new ScriptsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      Integer scriptId       = 1701;
      String  scriptContents = "logger.log('Hi');";

      TestUtils.insertRecords(qInstance.getTable(Script.TABLE_NAME), List.of(new QRecord().withValue("id", scriptId)));
      List<QRecord> scripts = TestUtils.queryTable(Script.TABLE_NAME);
      assertNull(scripts.get(0).getValueInteger("currentScriptRevisionId"));

      new StoreScriptRevisionProcessStep().run(new RunBackendStepInput().withValues(MapBuilder.of(
         "scriptId", scriptId,
         "fileNames", "script",
         "fileContents:script", scriptContents
      )), new RunBackendStepOutput());

      scripts = TestUtils.queryTable(Script.TABLE_NAME);
      assertEquals(1, scripts.get(0).getValueInteger("currentScriptRevisionId"));

      List<QRecord> scriptRevisions = TestUtils.queryTable(ScriptRevision.TABLE_NAME);
      QRecord       scriptRevision  = scriptRevisions.get(0);
      assertEquals(scriptId, scriptRevision.getValueInteger("scriptId"));
      assertEquals(1, scriptRevision.getValueInteger("sequenceNo"));
      assertEquals("Initial version", scriptRevision.getValueString("commitMessage"));

      List<QRecord> scriptRevisionFiles = TestUtils.queryTable(ScriptRevisionFile.TABLE_NAME);
      QRecord       scriptRevisionFile  = scriptRevisionFiles.get(0);
      assertEquals(scriptContents, scriptRevisionFile.getValueString("contents"));

      String updatedScriptContents = "logger.log('Really, Hi');";
      new StoreScriptRevisionProcessStep().run(new RunBackendStepInput().withValues(MapBuilder.of(
         "scriptId", scriptId,
         "fileNames", "script",
         "fileContents:script", updatedScriptContents
      )), new RunBackendStepOutput());

      scripts = TestUtils.queryTable(Script.TABLE_NAME);
      assertEquals(2, scripts.get(0).getValueInteger("currentScriptRevisionId"));

      scriptRevisions = TestUtils.queryTable(ScriptRevision.TABLE_NAME).stream().filter(r -> r.getValueInteger("id").equals(2)).collect(Collectors.toList());
      scriptRevision = scriptRevisions.get(0);
      Integer newScriptRevisionId = scriptRevision.getValueInteger("id");
      assertEquals(scriptId, scriptRevision.getValueInteger("scriptId"));
      assertEquals(2, scriptRevision.getValueInteger("sequenceNo"));
      assertEquals("No commit message given", scriptRevision.getValueString("commitMessage"));

      scriptRevisionFiles = TestUtils.queryTable(ScriptRevisionFile.TABLE_NAME);
      scriptRevisionFile = scriptRevisionFiles.stream().filter(r -> r.getValueInteger("scriptRevisionId").equals(newScriptRevisionId)).findFirst().get();
      assertEquals(updatedScriptContents, scriptRevisionFile.getValueString("contents"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultiFileScriptType() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new ScriptsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      Integer scriptId         = 1701;
      String  scriptContents   = "logger.log('Hi');";
      String  templateContents = "<h1>Hey</h1>";

      TestUtils.insertRecords(qInstance.getTable(Script.TABLE_NAME), List.of(new QRecord().withValue("id", scriptId)));
      List<QRecord> scripts = TestUtils.queryTable(Script.TABLE_NAME);
      assertNull(scripts.get(0).getValueInteger("currentScriptRevisionId"));

      RunBackendStepInput runBackendStepInput = new RunBackendStepInput();
      runBackendStepInput.addValue("scriptId", scriptId);
      runBackendStepInput.addValue("fileNames", "script,template");
      runBackendStepInput.addValue("fileContents:script", scriptContents);
      runBackendStepInput.addValue("fileContents:template", templateContents);
      new StoreScriptRevisionProcessStep().run(runBackendStepInput, new RunBackendStepOutput());

      scripts = TestUtils.queryTable(Script.TABLE_NAME);
      assertEquals(1, scripts.get(0).getValueInteger("currentScriptRevisionId"));

      List<QRecord> scriptRevisions = TestUtils.queryTable(ScriptRevision.TABLE_NAME);
      QRecord       scriptRevision  = scriptRevisions.get(0);
      assertEquals(scriptId, scriptRevision.getValueInteger("scriptId"));
      assertEquals(1, scriptRevision.getValueInteger("sequenceNo"));
      assertEquals("Initial version", scriptRevision.getValueString("commitMessage"));
      assertNull(scriptRevision.getValueString("contents"));

      List<QRecord> scriptRevisionFiles = TestUtils.queryTable(ScriptRevisionFile.TABLE_NAME);
      assertThat(scriptRevisionFiles.stream().filter(srf -> srf.getValueString("fileName").equals("script")).findFirst())
         .isPresent().get()
         .matches(r -> r.getValueString("contents").equals(scriptContents));

      assertThat(scriptRevisionFiles.stream().filter(srf -> srf.getValueString("fileName").equals("template")).findFirst())
         .isPresent().get()
         .matches(r -> r.getValueString("contents").equals(templateContents));

      ////////////////////////////
      // now add a new revision //
      ////////////////////////////
      String updatedScriptContents   = "logger.log('Really, Hi');";
      String updatedTemplateContents = "<h1>Hey, what's up</h1>";

      runBackendStepInput = new RunBackendStepInput();
      runBackendStepInput.addValue("scriptId", scriptId);
      runBackendStepInput.addValue("fileNames", "script,template");
      runBackendStepInput.addValue("fileContents:script", updatedScriptContents);
      runBackendStepInput.addValue("fileContents:template", updatedTemplateContents);
      runBackendStepInput.addValue("commitMessage", "Updated files");
      new StoreScriptRevisionProcessStep().run(runBackendStepInput, new RunBackendStepOutput());

      scripts = TestUtils.queryTable(Script.TABLE_NAME);
      assertEquals(2, scripts.get(0).getValueInteger("currentScriptRevisionId"));

      scriptRevisions = TestUtils.queryTable(ScriptRevision.TABLE_NAME).stream().filter(r -> r.getValueInteger("id").equals(2)).collect(Collectors.toList());
      scriptRevision = scriptRevisions.get(0);
      assertEquals(scriptId, scriptRevision.getValueInteger("scriptId"));
      assertEquals(2, scriptRevision.getValueInteger("id"));
      assertEquals(2, scriptRevision.getValueInteger("sequenceNo"));
      assertEquals("Updated files", scriptRevision.getValueString("commitMessage"));
      assertNull(scriptRevision.getValueString("contents"));

      scriptRevisionFiles = TestUtils.queryTable(ScriptRevisionFile.TABLE_NAME);
      assertThat(scriptRevisionFiles.stream().filter(srf -> srf.getValueString("fileName").equals("script") && srf.getValueInteger("scriptRevisionId").equals(2)).findFirst())
         .isPresent().get()
         .matches(r -> r.getValueString("contents").equals(updatedScriptContents));

      assertThat(scriptRevisionFiles.stream().filter(srf -> srf.getValueString("fileName").equals("template") && srf.getValueInteger("scriptRevisionId").equals(2)).findFirst())
         .isPresent().get()
         .matches(r -> r.getValueString("contents").equals(updatedTemplateContents));
   }

}