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

package com.kingsrook.qqq.backend.module.filesystem.processes.implementations.etl.basic;


import java.util.Arrays;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.RunBackendStepAction;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for BasicETLCollectSourceFileNamesFunction
 *******************************************************************************/
class BasicETLCollectSourceFileNamesStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testOneFile() throws Exception
   {
      String file   = "/tmp/test1.csv";
      String result = runTest(file);
      assertEquals(file, result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testTwoFiles() throws Exception
   {
      String file1  = "/tmp/test1.csv";
      String file2  = "/tmp/test2.csv";
      String result = runTest(file1, file2);

      //////////////////////////////////////////////////////////////////////
      // the names go into a set, so they can come out in either order... //
      //////////////////////////////////////////////////////////////////////
      assertTrue(result.equals(file1 + "," + file2) || result.equals((file2 + "," + file1)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testDuplicatedFile() throws Exception
   {
      String file   = "/tmp/test1.csv";
      String result = runTest(file, file);
      assertEquals(file, result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String runTest(String... fileNames) throws Exception
   {
      QInstance            qInstance           = TestUtils.defineInstance();
      QBackendStepMetaData backendStepMetaData = new BasicETLCollectSourceFileNamesStep().defineStepMetaData();
      QProcessMetaData     qProcessMetaData    = new QProcessMetaData().withName("testScaffold").withStep(backendStepMetaData);
      qInstance.addProcess(qProcessMetaData);
      reInitInstanceInContext(qInstance);

      List<QRecord> records = Arrays.stream(fileNames).map(fileName ->
         new QRecord().withBackendDetail(FilesystemRecordBackendDetailFields.FULL_PATH, fileName)).toList();

      RunBackendStepInput runBackendStepInput = new RunBackendStepInput();
      runBackendStepInput.setStepName(backendStepMetaData.getName());
      runBackendStepInput.setProcessName(qProcessMetaData.getName());
      runBackendStepInput.setRecords(records);

      RunBackendStepAction runBackendStepAction = new RunBackendStepAction();
      RunBackendStepOutput result               = runBackendStepAction.execute(runBackendStepInput);

      return ((String) result.getValues().get(BasicETLCollectSourceFileNamesStep.FIELD_SOURCE_FILE_PATHS));
   }
}