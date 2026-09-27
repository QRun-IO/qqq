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

package com.kingsrook.qqq.api.middleware.executors;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsOutput;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.TableMetaDataExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableMetaDataInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableMetaDataOutputInterface;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiAwareTableMetaDataExecutor extends TableMetaDataExecutor implements ApiAwareExecutorInterface
{
   private static final QLogger LOG = QLogger.getLogger(ApiAwareTableMetaDataExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableMetaDataInput input, TableMetaDataOutputInterface output) throws QException
   {
      QTableMetaData table = getQTableMetaData(input);

      Map<String, QFieldMetaData> fieldMap = getFieldsForApiVersion(input.getTableName());

      ///////////////////////////////////////////////////////////////////////////////////
      // we don't call TableMetaDataAction here, because, well, it doesn't do much,    //
      // but we need to do things differently anyway, as in, we need to sue a fieldMap //
      // that takes api versions into account.  Note that table-personalization will   //
      // also have been applied, via GetTableApiFieldsAction                           //
      ///////////////////////////////////////////////////////////////////////////////////
      com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput tableMetaDataInput = new com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput();
      tableMetaDataInput.setTableName(input.getTableName());
      PermissionsHelper.checkTablePermissionThrowing(tableMetaDataInput, TablePermissionSubType.READ);

      QBackendMetaData    backendForTable     = QContext.getQInstance().getBackendForTable(table.getName());
      TableMetaDataOutput tableMetaDataOutput = new TableMetaDataOutput();
      tableMetaDataOutput.setTable(new QFrontendTableMetaData(tableMetaDataInput, backendForTable, table, true, true, fieldMap));

      adjustExposedJoinsForApi(tableMetaDataOutput);

      output.setTableMetaData(tableMetaDataOutput.getTable());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void adjustExposedJoinsForApi(TableMetaDataOutput tableMetaDataOutput) throws QException
   {
      if(CollectionUtils.nullSafeIsEmpty(tableMetaDataOutput.getTable().getExposedJoins()))
      {
         return;
      }

      Iterator<QFrontendExposedJoin> iterator = tableMetaDataOutput.getTable().getExposedJoins().iterator();
      while(iterator.hasNext())
      {
         QFrontendExposedJoin frontendExposedJoin = iterator.next();
         String               tableName           = frontendExposedJoin.getJoinTable().getName();

         try
         {
            QTableMetaData              joinTable       = QContext.getQInstance().getTable(tableName);
            QBackendMetaData            backendForTable = QContext.getQInstance().getBackendForTable(tableName);
            Map<String, QFieldMetaData> joinFieldMap    = getFieldsForApiVersion(tableName);

            com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput tableMetaDataInput = new com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput();
            frontendExposedJoin.setJoinTable(new QFrontendTableMetaData(tableMetaDataInput, backendForTable, joinTable, true, false, joinFieldMap));
         }
         catch(QNotFoundException e)
         {
            LOG.debug("Removing exposed-join table that isn't in api version", logPair("mainTable", tableMetaDataOutput.getTable().getName()), logPair("joinTable", tableName), logPair("apiName", getApiName()), logPair("apiVersion", getApiVersion()));
            iterator.remove();
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private QTableMetaData getQTableMetaData(TableMetaDataInput input) throws QNotFoundException
   {
      String         tableName = input.getTableName();
      QTableMetaData table     = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         throw (new QNotFoundException("Table [" + tableName + "] was not found."));
      }
      return table;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private Map<String, QFieldMetaData> getFieldsForApiVersion(String tableName) throws QException
   {
      GetTableApiFieldsInput getTableApiFieldsInput = new GetTableApiFieldsInput()
         .withApiName(getApiName())
         .withVersion(getApiVersion())
         .withTableName(tableName)
         .withInputSource(QInputSource.USER);

      GetTableApiFieldsOutput     tableApiFieldsOutput = new GetTableApiFieldsAction().execute(getTableApiFieldsInput);
      List<QFieldMetaData>        fields               = tableApiFieldsOutput.getFields();
      Map<String, QFieldMetaData> fieldMap             = CollectionUtils.listToMap(fields, f -> f.getName());
      return fieldMap;
   }
}
