/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.processes.implementations.savedviews;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Process used by the delete view dialog
 *******************************************************************************/
public class DeleteSavedViewProcess implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(DeleteSavedViewProcess.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData getProcessMetaData()
   {
      return (new QProcessMetaData()
         .withName("deleteSavedView")
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withCode(new QCodeReference(DeleteSavedViewProcess.class))
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
         Integer savedViewId = runBackendStepInput.getValueInteger("id");

         DeleteInput input = new DeleteInput();
         input.setTableName(SavedView.TABLE_NAME);
         input.setInputSource(QInputSource.USER);
         input.setPrimaryKeys(List.of(savedViewId));
         PermissionsHelper.checkTablePermissionThrowing(input, TablePermissionSubType.DELETE);
         DeleteOutput output = new DeleteAction().execute(input);
         if(CollectionUtils.nullSafeHasContents(output.getRecordsWithErrors()))
         {
            throw new QUserFacingException("Error deleting saved view: " + output.getRecordsWithErrors().get(0).getErrorsAsString());
         }
         if(output.getDeletedRecordCount() != 1)
         {
            throw new QUserFacingException("No saved view was found to delete.");
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error deleting saved view", e);
         throw (e);
      }
   }
}
