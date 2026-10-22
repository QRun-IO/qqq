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

package com.kingsrook.qqq.backend.module.filesystem.processes.implementations.etl.streamed;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed.StreamedETLBackendStep;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import com.kingsrook.qqq.backend.module.filesystem.processes.implementations.etl.basic.BasicETLCollectSourceFileNamesStep;


/*******************************************************************************
 ** Extension to the base StreamedETLBackendStep, unique for where the source
 ** table is a filesystem, where we want/need to collect the filenames that were
 ** processed in the Extract step, so they can be passed into the cleanup step.
 **
 ** Similar in purpose to the BasicETLCollectSourceFileNamesStep - only in this
 ** case, due to the streaming behavior of the StreamedETLProcess, we can't really
 ** inject this code as a separate backend step - so instead we extend that step,
 ** and override its postTransform method to intercept the records & file names.
 *******************************************************************************/
public class StreamedETLFilesystemBackendStep extends StreamedETLBackendStep
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected void preTransform(List<QRecord> qRecords, RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
   {
      Set<String> sourceFiles = qRecords.stream()
         .map(record -> record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH))
         .collect(Collectors.toSet());

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // expect that we'll be called on multiple "pages" of records as they run through the pipe.                                        //
      // each time we're called, we need to:                                                                                             //
      // - get the unique file paths in this list of records                                                                             //
      // - if we previously set the list of file names in the output, then split that value up and add those names to the set we see now //
      // - set the list of name (joined by commas) in the output                                                                         //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      String existingListOfFileNames = runBackendStepOutput.getValueString(BasicETLCollectSourceFileNamesStep.FIELD_SOURCE_FILE_PATHS);
      if(existingListOfFileNames != null)
      {
         sourceFiles.addAll(List.of(existingListOfFileNames.split(",")));
      }
      runBackendStepOutput.addValue(BasicETLCollectSourceFileNamesStep.FIELD_SOURCE_FILE_PATHS, StringUtils.join(",", sourceFiles));
   }

}
