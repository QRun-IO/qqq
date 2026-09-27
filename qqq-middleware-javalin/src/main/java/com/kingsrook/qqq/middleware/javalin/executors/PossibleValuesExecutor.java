/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.PossibleValuesInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.PossibleValuesOutputInterface;


/*******************************************************************************
 ** Executor for searching possible value sources.  The spec's buildInput method
 ** handles filter cloning and interpretation; this executor checks that the
 ** user may use the table or process the field belongs to, then passes the
 ** prepared input through to the SearchPossibleValueSourceAction.
 *******************************************************************************/
public class PossibleValuesExecutor extends AbstractMiddlewareExecutor<PossibleValuesInput, PossibleValuesOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(PossibleValuesInput input, PossibleValuesOutputInterface output) throws QException
   {
      SearchPossibleValueSourceInput searchInput = new SearchPossibleValueSourceInput();
      checkPermissions(input, searchInput);

      searchInput.setPossibleValueSourceName(input.getPossibleValueSourceName());
      searchInput.setSearchTerm(input.getSearchTerm());
      searchInput.setDefaultQueryFilter(input.getDefaultFilter());
      searchInput.setOtherValues(input.getOtherValues());

      if(CollectionUtils.nullSafeHasContents(input.getIdList()))
      {
         searchInput.setIdList(new ArrayList<>(input.getIdList()));
      }

      if(CollectionUtils.nullSafeHasContents(input.getLabelList()))
      {
         searchInput.setLabelList(input.getLabelList());
      }

      if(input.getPathParams() != null)
      {
         searchInput.setPathParamMap(input.getPathParams());
      }

      if(input.getQueryParams() != null)
      {
         searchInput.setQueryParamMap(input.getQueryParams());
      }

      SearchPossibleValueSourceOutput searchOutput = new SearchPossibleValueSourceAction().execute(searchInput);
      output.setOptions(searchOutput.getResults());
   }



   /***************************************************************************
    ** A table field's values are for users who can read, insert or edit that
    ** table (queries, filters and forms); a process field's values are for
    ** users who may run the process.  Standalone sources are not tied to a
    ** table or process and are not checked here.
    ***************************************************************************/
   private static void checkPermissions(PossibleValuesInput input, SearchPossibleValueSourceInput searchInput) throws QPermissionDeniedException
   {
      if(input.getTableName() != null)
      {
         String  tableName = input.getTableName();
         boolean mayUse    = PermissionsHelper.hasTablePermission(searchInput, tableName, TablePermissionSubType.READ)
            || PermissionsHelper.hasTablePermission(searchInput, tableName, TablePermissionSubType.INSERT)
            || PermissionsHelper.hasTablePermission(searchInput, tableName, TablePermissionSubType.EDIT);
         if(!mayUse)
         {
            throw (new QPermissionDeniedException("Permission denied."));
         }
      }
      else if(input.getProcessName() != null)
      {
         PermissionsHelper.checkProcessPermissionThrowing(searchInput, input.getProcessName());
      }
   }

}
