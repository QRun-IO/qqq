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

package com.kingsrook.qqq.backend.core.actions.permissions;


import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkTableActionProcessPermissionChecker implements CustomPermissionChecker
{
   private static final QLogger LOG = QLogger.getLogger(BulkTableActionProcessPermissionChecker.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void checkPermissionsThrowing(AbstractActionInput actionInput, MetaDataWithPermissionRules metaDataWithPermissionRules) throws QPermissionDeniedException
   {
      String processName = metaDataWithPermissionRules.getName();
      if(processName != null && processName.indexOf('.') > -1)
      {
         String[] parts          = processName.split("\\.", 2);
         String   tableName      = parts[0];
         String   bulkActionName = parts[1];

         AbstractTableActionInput tableActionInput = new AbstractTableActionInput();
         tableActionInput.setTableName(tableName);

         switch(bulkActionName)
         {
            case "bulkInsert" -> PermissionsHelper.checkTablePermissionThrowing(tableActionInput, TablePermissionSubType.INSERT);
            case "bulkEdit", "bulkEditWithFile" -> PermissionsHelper.checkTablePermissionThrowing(tableActionInput, TablePermissionSubType.EDIT);
            case "bulkDelete" -> PermissionsHelper.checkTablePermissionThrowing(tableActionInput, TablePermissionSubType.DELETE);
            default -> LOG.warn("Unexpected bulk action name when checking permissions for process: " + processName);
         }
      }
   }

}
