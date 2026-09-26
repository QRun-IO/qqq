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


import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Definition for Filesystem sync process.
 **
 ** Job is to:
 ** - list all files in the source table.
 ** - list all files in the archive table.
 ** - if any files exist in the source, but not in the archive, then:
 **   - copy the file to both the archive and the processing table.
 **
 ** The maxFilesToArchive field can be used to only sync up to that many files
 ** (an help an initial sync, if you want to do it in smaller batches)
 **
 ** The idea being, that the source is read-only, and we want to move files out of
 ** processing after they've been processed - and the archive is what we can have
 ** in-between the two.
 *******************************************************************************/
public class FilesystemSyncProcess
{
   public static final String PROCESS_NAME = "filesystem.sync";

   public static final String FIELD_SOURCE_TABLE         = "sourceTable";
   public static final String FIELD_ARCHIVE_TABLE        = "archiveTable";
   public static final String FIELD_PROCESSING_TABLE     = "processingTable";
   public static final String FIELD_MAX_FILES_TO_ARCHIVE = "maxFilesToArchive";



   /*******************************************************************************
    **
    *******************************************************************************/
   public QProcessMetaData defineProcessMetaData()
   {
      QBackendStepMetaData syncStep = new QBackendStepMetaData()
         .withName(FilesystemSyncStep.STEP_NAME)
         .withCode(new QCodeReference()
            .withName(FilesystemSyncStep.class.getName())
            .withCodeType(QCodeType.JAVA))
         .withInputData(new QFunctionInputMetaData()
            .withField(new QFieldMetaData(FIELD_SOURCE_TABLE, QFieldType.STRING))
            .withField(new QFieldMetaData(FIELD_ARCHIVE_TABLE, QFieldType.STRING))
            .withField(new QFieldMetaData(FIELD_MAX_FILES_TO_ARCHIVE, QFieldType.INTEGER).withDefaultValue(Integer.MAX_VALUE))
            .withField(new QFieldMetaData(FIELD_PROCESSING_TABLE, QFieldType.STRING)));

      return new QProcessMetaData()
         .withName(PROCESS_NAME)
         .withStep(syncStep);
   }
}
