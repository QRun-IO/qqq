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


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.processes.RunBackendStepAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.basic.BasicETLProcess;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for BasicETLCleanupSourceFilesFunction
 *******************************************************************************/
public class BasicETLCleanupSourceFilesStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testDelete1Record1File() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      String    filePath  = getRandomFilePathPersonTable(qInstance);
      testDelete(qInstance, List.of(filePath));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testDelete2Records1File() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      String    filePath  = getRandomFilePathPersonTable(qInstance);
      testDelete(qInstance, List.of(filePath, filePath));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testDelete2Record2File() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      String    filePath1 = getRandomFilePathPersonTable(qInstance);
      String    filePath2 = getRandomFilePathPersonTable(qInstance);
      testDelete(qInstance, List.of(filePath1, filePath2));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testMove1Record1File() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      String    filePath  = getRandomFilePathPersonTable(qInstance);
      testMove(qInstance, List.of(filePath));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testMove2Records1File() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      String    filePath  = getRandomFilePathPersonTable(qInstance);
      testMove(qInstance, List.of(filePath, filePath));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testMove2Record2File() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      String    filePath1 = getRandomFilePathPersonTable(qInstance);
      String    filePath2 = getRandomFilePathPersonTable(qInstance);
      testMove(qInstance, List.of(filePath1, filePath2));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void testDelete(QInstance qInstance, List<String> filePaths) throws Exception
   {
      RunBackendStepOutput runBackendStepOutput = runFunction(qInstance, filePaths, Map.of(
         BasicETLCleanupSourceFilesStep.FIELD_MOVE_OR_DELETE, BasicETLCleanupSourceFilesStep.VALUE_DELETE,
         // todo - even though this field isn't needed, since we gave a value of "delete"
         //  the RunFunctionAction considers any missing input to be an error...
         BasicETLCleanupSourceFilesStep.FIELD_DESTINATION_FOR_MOVES, ""));

      assertNull(runBackendStepOutput.getException());
      for(String filePath : filePaths)
      {
         assertFalse(new File(filePath).exists(), "File should have been deleted.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void testMove(QInstance qInstance, List<String> filePaths) throws Exception
   {
      String trashDir = File.separator + "tmp" + File.separator + "trash";
      RunBackendStepOutput runBackendStepOutput = runFunction(qInstance, filePaths, Map.of(
         BasicETLCleanupSourceFilesStep.FIELD_MOVE_OR_DELETE, BasicETLCleanupSourceFilesStep.VALUE_MOVE,
         BasicETLCleanupSourceFilesStep.FIELD_DESTINATION_FOR_MOVES, trashDir));

      assertNull(runBackendStepOutput.getException());

      for(String filePath : filePaths)
      {
         assertFalse(new File(filePath).exists(), "File should have been moved.");

         String movedPath = trashDir + File.separator + (new File(filePath).getName());
         assertTrue(new File(movedPath).exists(), "File should have been moved.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunBackendStepOutput runFunction(QInstance qInstance, List<String> filePaths, Map<String, String> values) throws Exception
   {
      QBackendStepMetaData backendStepMetaData = new BasicETLCleanupSourceFilesStep().defineStepMetaData();
      QProcessMetaData     qProcessMetaData    = new QProcessMetaData().withName("testScaffold").withStep(backendStepMetaData);
      qInstance.addProcess(qProcessMetaData);

      HashSet<String> filePathsSet = new HashSet<>(filePaths);
      for(String filePath : filePathsSet)
      {
         File file = new File(filePath);
         FileUtils.writeStringToFile(file, "content", StandardCharsets.UTF_8);
      }

      // List<QRecord> records = filePaths.stream()
      //    .map(filePath -> new QRecord().withBackendDetail(FilesystemRecordBackendDetailFields.FULL_PATH, filePath)).toList();

      RunBackendStepInput runBackendStepInput = new RunBackendStepInput();
      runBackendStepInput.setStepName(backendStepMetaData.getName());
      runBackendStepInput.setProcessName(qProcessMetaData.getName());
      // runFunctionRequest.setRecords(records);
      runBackendStepInput.addValue(BasicETLProcess.FIELD_SOURCE_TABLE, TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      runBackendStepInput.addValue(BasicETLProcess.FIELD_DESTINATION_TABLE, TestUtils.TABLE_NAME_PERSON_S3);
      runBackendStepInput.addValue(BasicETLCollectSourceFileNamesStep.FIELD_SOURCE_FILE_PATHS, StringUtils.join(",", filePathsSet));

      for(Map.Entry<String, String> entry : values.entrySet())
      {
         runBackendStepInput.addValue(entry.getKey(), entry.getValue());
      }

      RunBackendStepAction runFunctionAction = new RunBackendStepAction();
      return (runFunctionAction.execute(runBackendStepInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getRandomFilePathPersonTable(QInstance qInstance)
   {
      FilesystemBackendMetaData     backend        = (FilesystemBackendMetaData) qInstance.getBackend(TestUtils.BACKEND_NAME_LOCAL_FS);
      FilesystemTableBackendDetails backendDetails = (FilesystemTableBackendDetails) qInstance.getTable(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON).getBackendDetails();
      String                        tablePath      = backend.getBasePath() + File.separator + backendDetails.getBasePath();
      String                        filePath       = tablePath + File.separator + UUID.randomUUID();
      return filePath;
   }

}