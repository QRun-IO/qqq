/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.middleware.executors;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.api.actions.ApiImplementation;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.ApiOperation;
import com.kingsrook.qqq.api.utils.ApiQueryFilterUtils;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.executors.ExecutorSessionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.TableCountExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableCountInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableCountOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiAwareTableCountExecutor extends TableCountExecutor implements ApiAwareExecutorInterface
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableCountInput input, TableCountOutputInterface output) throws QException
   {
      ExecutorSessionUtils.setTableVariantInSession(input.getTableVariant());
      List<String> badRequestMessages = new ArrayList<>();

      // todo - new operation?  move all this to the api impl class??
      // todo table api name vs. internal name??
      String         apiName    = getApiName();
      String         apiVersion = getApiVersion();
      QTableMetaData table      = ApiImplementation.validateTableAndVersion(getApiInstanceMetaData(), apiVersion, input.getTableName(), ApiOperation.QUERY_BY_QUERY_STRING);

      CountInput countInput = new CountInput();
      countInput.setTableName(input.getTableName());
      countInput.setInputSource(QInputSource.USER);

      PermissionsHelper.checkTablePermissionThrowing(countInput, TablePermissionSubType.READ);
      Map<String, QFieldMetaData> tableApiFields = GetTableApiFieldsAction.getTableApiFieldMap(
         new GetTableApiFieldsInput().withApiName(apiName).withVersion(apiVersion).withTableName(table.getName()).withInputSource(QInputSource.USER));

      countInput.setTimeoutSeconds(DEFAULT_QUERY_TIMEOUT_SECONDS); // todo param
      countInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      countInput.setIncludeDistinctCount(input.getIncludeDistinct());

      countInput.setQueryJoins(input.getJoins()); // todo - what are version implications here??

      ///////////////////////////////////////////////////////////////////////////
      // take care of managing criteria, which may not be in this version, etc //
      ///////////////////////////////////////////////////////////////////////////
      QQueryFilter filter = Objects.requireNonNullElseGet(input.getFilter(), () -> new QQueryFilter());
      ApiQueryFilterUtils.manageCriteriaFields(filter, tableApiFields, badRequestMessages, apiName, apiVersion, countInput);

      //////////////////////////////////////////
      // no more badRequest checks below here //
      //////////////////////////////////////////
      ApiQueryFilterUtils.throwIfBadRequestMessages(badRequestMessages);

      CountAction countAction = new CountAction();
      countInput.setFilter(filter);
      CountOutput countOutput = countAction.execute(countInput);

      output.setCount(ValueUtils.getValueAsLong(countOutput.getCount()));
      output.setDistinctCount(ValueUtils.getValueAsLong(countOutput.getDistinctCount()));
   }

}
