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


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.middleware.javalin.JoinedTablePermissions;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableGetInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableGetOutputInterface;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableGetExecutor extends AbstractMiddlewareExecutor<TableGetInput, TableGetOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(TableGetExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableGetInput input, TableGetOutputInterface output) throws QException
   {
      try
      {
         GetInput getInput = new GetInput();
         getInput.setTableName(input.getTableName());
         getInput.setInputSource(QInputSource.USER);
         getInput.setShouldGenerateDisplayValues(true);
         getInput.setShouldTranslatePossibleValues(true);
         getInput.setShouldFetchHeavyFields(true);
         getInput.setPrimaryKey(input.getPrimaryKey());
         getInput.setQueryJoins(input.getQueryJoins());

         if(BooleanUtils.isTrue(input.getIncludeAssociations()))
         {
            getInput.setIncludeAssociations(true);
         }

         ExecutorSessionUtils.setTableVariantInSession(input.getTableVariant());

         PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);
         JoinedTablePermissions.checkReadPermissions(getInput, getInput.getQueryJoins(), null);
         JoinedTablePermissions.checkAssociationReadPermissions(getInput);

         GetOutput getOutput = new GetAction().execute(getInput);

         ///////////////////////////////////////////////////////
         // throw a not found error if the record isn't found //
         ///////////////////////////////////////////////////////
         QRecord record = getOutput.getRecord();
         if(record == null)
         {
            QTableMetaData table = QContext.getQInstance().getTable(input.getTableName());
            throw (new QNotFoundException("Could not find " + table.getLabel() + " with "
               + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + input.getPrimaryKey()));
         }

         QValueFormatter.setBlobValuesToDownloadUrls(QContext.getQInstance().getTable(input.getTableName()), List.of(record));

         output.setRecord(record);
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
         throw (new QException("Unexpected error occurred while executing get: " + e.getMessage(), e));
      }
   }

}
