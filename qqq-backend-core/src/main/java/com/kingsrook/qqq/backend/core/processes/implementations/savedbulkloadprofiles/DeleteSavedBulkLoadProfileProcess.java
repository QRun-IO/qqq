/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfile;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Process used by the delete bulkLoadProfile dialog
 *******************************************************************************/
public class DeleteSavedBulkLoadProfileProcess implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(DeleteSavedBulkLoadProfileProcess.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData getProcessMetaData()
   {
      return (new QProcessMetaData()
         .withName("deleteSavedBulkLoadProfile")
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withCode(new QCodeReference(DeleteSavedBulkLoadProfileProcess.class))
               .withName("delete")
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ActionHelper.validateSession(runBackendStepInput);

      try
      {
         Integer savedBulkLoadProfileId = runBackendStepInput.getValueInteger("id");

         DeleteInput input = new DeleteInput();
         input.setTableName(SavedBulkLoadProfile.TABLE_NAME);
         input.setPrimaryKeys(List.of(savedBulkLoadProfileId));
         DeleteOutput output = new DeleteAction().execute(input);

         /////////////////////////////////////////////////////////////////////
         // a delete the backend refused (e.g., a record lock: only the     //
         // owner may delete a shared profile) is an error, not a success   //
         /////////////////////////////////////////////////////////////////////
         if(CollectionUtils.nullSafeHasContents(output.getRecordsWithErrors()))
         {
            throw (new QUserFacingException("Error deleting Bulk Load Profile: " + output.getRecordsWithErrors().get(0).getErrors().get(0).getMessage()));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error deleting saved bulkLoadProfile", e);
         throw (e);
      }
   }
}
