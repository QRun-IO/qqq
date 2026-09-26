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

package com.kingsrook.qqq.backend.module.filesystem.processes.implementations.filesystem.sync;


import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemBackendModuleInterface;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;


/*******************************************************************************
 ** BackendStep to sync two filesystem tables (copying the new files to a 3rd
 ** location as well...)
 **
 *******************************************************************************/
@SuppressWarnings("unchecked")
public class FilesystemSyncStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(FilesystemSyncStep.class);

   public static final String STEP_NAME = "sync";



   /*******************************************************************************
    ** Execute the step - using the request as input, and the result as output.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      // defer to a private method here, so we can add a type-parameter for that method to use              //
      // would think we could do that here, but get compiler error, since this method comes from base class //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      doRun(runBackendStepInput, runBackendStepOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private <F> void doRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      QTableMetaData sourceTable     = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FilesystemSyncProcess.FIELD_SOURCE_TABLE));
      QTableMetaData archiveTable    = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FilesystemSyncProcess.FIELD_ARCHIVE_TABLE));
      QTableMetaData processingTable = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FilesystemSyncProcess.FIELD_PROCESSING_TABLE));

      QBackendMetaData                    sourceBackend    = QContext.getQInstance().getBackendForTable(sourceTable.getName());
      FilesystemBackendModuleInterface<F> sourceModule     = (FilesystemBackendModuleInterface<F>) new QBackendModuleDispatcher().getQBackendModule(sourceBackend);
      AbstractBaseFilesystemAction<F>     sourceActionBase = sourceModule.getActionBase();
      sourceActionBase.preAction(sourceBackend);
      Map<String, F> sourceFiles = getFileNames(sourceActionBase, sourceTable, sourceBackend);

      QBackendMetaData                    archiveBackend    = QContext.getQInstance().getBackendForTable(archiveTable.getName());
      FilesystemBackendModuleInterface<F> archiveModule     = (FilesystemBackendModuleInterface<F>) new QBackendModuleDispatcher().getQBackendModule(archiveBackend);
      AbstractBaseFilesystemAction<F>     archiveActionBase = archiveModule.getActionBase();
      archiveActionBase.preAction(archiveBackend);
      Set<String> archiveFiles = getFileNames(archiveActionBase, archiveTable, archiveBackend).keySet();

      QBackendMetaData                    processingBackend    = QContext.getQInstance().getBackendForTable(processingTable.getName());
      FilesystemBackendModuleInterface<F> processingModule     = (FilesystemBackendModuleInterface<F>) new QBackendModuleDispatcher().getQBackendModule(processingBackend);
      AbstractBaseFilesystemAction<F>     processingActionBase = processingModule.getActionBase();
      processingActionBase.preAction(processingBackend);

      Integer maxFilesToSync  = runBackendStepInput.getValueInteger(FilesystemSyncProcess.FIELD_MAX_FILES_TO_ARCHIVE);
      int     syncedFileCount = 0;
      for(Map.Entry<String, F> sourceEntry : sourceFiles.entrySet())
      {
         try
         {
            String sourceFileName = sourceEntry.getKey();
            if(!archiveFiles.contains(sourceFileName))
            {
               LOG.info("Syncing file [" + sourceFileName + "] to [" + archiveTable + "] and [" + processingTable + "]");
               try(InputStream inputStream = sourceActionBase.readFile(sourceEntry.getValue()))
               {
                  byte[] bytes = inputStream.readAllBytes();

                  String archivePath = archiveActionBase.getFullBasePath(archiveTable, archiveBackend);
                  archiveActionBase.writeFile(archiveBackend, archiveTable, null, archivePath + File.separator + sourceFileName, bytes);

                  String processingPath = processingActionBase.getFullBasePath(processingTable, processingBackend);
                  processingActionBase.writeFile(processingBackend, processingTable, null, processingPath + File.separator + sourceFileName, bytes);
                  syncedFileCount++;

                  if(maxFilesToSync != null && syncedFileCount >= maxFilesToSync)
                  {
                     LOG.info("Breaking after syncing " + syncedFileCount + " files");
                     break;
                  }
               }
            }
         }
         catch(Exception e)
         {
            LOG.error("Error processing file: " + sourceEntry, e);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private <F> Map<String, F> getFileNames(AbstractBaseFilesystemAction<F> actionBase, QTableMetaData table, QBackendMetaData backend) throws QException
   {
      List<F>        files = actionBase.listFiles(table, backend);
      Map<String, F> rs    = new LinkedHashMap<>();

      for(F file : files)
      {
         String fileName = actionBase.stripBackendAndTableBasePathsFromFileName(actionBase.getFullPathForFile(file), backend, table);
         rs.put(fileName, file);
      }

      return (rs);
   }

}
