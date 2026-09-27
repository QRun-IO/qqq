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


import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;


/*******************************************************************************
 ** BackendStep for collecting the file names that were discovered in the
 ** Extract step.  These will be lost during the transform, so we capture them here,
 ** so that our Clean step can move or delete them.
 **
 ** TODO - need unit test!!
 *******************************************************************************/
public class BasicETLCollectSourceFileNamesStep implements BackendStep
{
   public static final String STEP_NAME               = "collectSourceFileNames";
   public static final String FIELD_SOURCE_FILE_PATHS = "sourceFilePaths";



   /*******************************************************************************
    ** Execute the step - using the request as input, and the result as output.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      Set<String> sourceFiles = runBackendStepInput.getRecords().stream()
         .map(record -> record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH))
         .collect(Collectors.toSet());
      runBackendStepOutput.addValue(FIELD_SOURCE_FILE_PATHS, StringUtils.join(",", sourceFiles));
   }



   /*******************************************************************************
    ** define the metaData that describes this step
    *******************************************************************************/
   public QBackendStepMetaData defineStepMetaData()
   {
      return (new QBackendStepMetaData()
         .withName(STEP_NAME)
         .withCode(new QCodeReference()
            .withName(this.getClass().getName())
            .withCodeType(QCodeType.JAVA))
         .withOutputMetaData(new QFunctionOutputMetaData()
            .withField(new QFieldMetaData(FIELD_SOURCE_FILE_PATHS, QFieldType.STRING))));
   }
}
