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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.middleware.javalin.AssociatedWritePermissions;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableDeleteInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableDeleteOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableDeleteExecutor extends AbstractMiddlewareExecutor<TableDeleteInput, TableDeleteOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(TableDeleteExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableDeleteInput input, TableDeleteOutputInterface output) throws QException
   {
      try
      {
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(input.getTableName());
         deleteInput.setInputSource(QInputSource.USER);

         List<Serializable> primaryKeys = new ArrayList<>();
         primaryKeys.add(input.getPrimaryKey());
         deleteInput.setPrimaryKeys(primaryKeys);

         PermissionsHelper.checkTablePermissionThrowing(deleteInput, TablePermissionSubType.DELETE);
         AssociatedWritePermissions.check(deleteInput);

         DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);

         output.setDeletedRecordCount(deleteOutput.getDeletedRecordCount());

         List<String> errors = CollectionUtils.nonNullList(deleteOutput.getRecordsWithErrors()).stream()
            .flatMap(record -> CollectionUtils.nonNullList(record.getErrors()).stream())
            .map(QStatusMessage::getMessage)
            .toList();
         if(!errors.isEmpty())
         {
            output.setErrors(errors);
         }
      }
      catch(QException e)
      {
         QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(e, QUserFacingException.class);
         if(userFacingException != null)
         {
            throw userFacingException;
         }

         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Unexpected error occurred while executing delete: " + e.getMessage(), e));
      }
   }

}
