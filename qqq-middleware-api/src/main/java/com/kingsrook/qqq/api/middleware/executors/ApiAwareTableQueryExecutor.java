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
import com.kingsrook.qqq.api.actions.QRecordApiAdapter;
import com.kingsrook.qqq.api.actions.io.QRecordApiAdapterToApiInput;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.ApiOperation;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.utils.ApiQueryFilterUtils;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.javalin.QJavalinMetaData;
import com.kingsrook.qqq.backend.javalin.QJavalinUtils;
import com.kingsrook.qqq.middleware.javalin.executors.TableQueryExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableQueryInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableQueryOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiAwareTableQueryExecutor extends TableQueryExecutor implements ApiAwareExecutorInterface
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableQueryInput input, TableQueryOutputInterface output) throws QException
   {
      List<String> badRequestMessages = new ArrayList<>();

      // todo - new operation?  move all this to the api impl class??
      // todo table api name vs. internal name??
      String         apiName    = getApiName();
      String         apiVersion = getApiVersion();
      QTableMetaData table      = ApiImplementation.validateTableAndVersion(getApiInstanceMetaData(), apiVersion, input.getTableName(), ApiOperation.QUERY_BY_QUERY_STRING);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(input.getTableName());
      queryInput.setInputSource(QInputSource.USER);

      PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);
      Map<String, QFieldMetaData> tableApiFields = GetTableApiFieldsAction.getTableApiFieldMap(
         new GetTableApiFieldsInput().withApiName(apiName).withVersion(apiVersion).withTableName(table.getName()).withInputSource(QInputSource.USER));

      queryInput.setIncludeAssociations(true);
      // queryInput.setShouldFetchHeavyFields(true); // diffs from raw api
      queryInput.setShouldGenerateDisplayValues(true);
      queryInput.setShouldTranslatePossibleValues(true);
      queryInput.setTimeoutSeconds(DEFAULT_QUERY_TIMEOUT_SECONDS); // todo param
      queryInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

      queryInput.setQueryJoins(input.getJoins()); // todo - what are version implications here??

      QQueryFilter filter = Objects.requireNonNullElseGet(input.getFilter(), () -> new QQueryFilter());
      queryInput.setFilter(filter);

      if(filter.getLimit() == null)
      {
         QJavalinUtils.handleQueryNullLimit(QJavalinMetaData.of(QContext.getQInstance()), queryInput, null);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // take care of managing order-by fields and criteria, which may not be in this version, etc //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      manageOrderByFields(filter, tableApiFields, badRequestMessages, apiName, queryInput);
      ApiQueryFilterUtils.manageCriteriaFields(filter, tableApiFields, badRequestMessages, apiName, apiVersion, queryInput);

      //////////////////////////////////////////
      // no more badRequest checks below here //
      //////////////////////////////////////////
      ApiQueryFilterUtils.throwIfBadRequestMessages(badRequestMessages);

      ///////////////////////
      // execute the query //
      ///////////////////////
      QueryAction queryAction = new QueryAction();
      QueryOutput queryOutput = queryAction.execute(queryInput);

      ////////////////////////////////////////////////////////////////////////////////////////////
      // map from QRecords to this version of the api - with the flag to include exposed joins! //
      ////////////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> versionedRecords = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(new QRecordApiAdapterToApiInput()
         .withInputRecords(queryOutput.getRecords())
         .withTableName(table.getName())
         .withApiName(apiName)
         .withApiVersion(apiVersion)
         .withIncludeExposedJoins(true));

      QValueFormatter.setDisplayValuesInRecordsIncludingPossibleValueTranslations(table, versionedRecords);

      output.setRecords(versionedRecords);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void manageOrderByFields(QQueryFilter filter, Map<String, QFieldMetaData> tableApiFields, List<String> badRequestMessages, String apiName, QueryInput queryInput)
   {
      for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(filter.getOrderBys()))
      {
         String         apiFieldName = orderBy.getFieldName();
         QFieldMetaData field        = tableApiFields.get(apiFieldName);
         if(field == null)
         {
            badRequestMessages.add("Unrecognized orderBy field name: " + apiFieldName + ".");
         }
         else
         {
            ApiFieldMetaData apiFieldMetaData = ObjectUtils.tryAndRequireNonNullElse(() -> ApiFieldMetaDataContainer.of(field).getApiFieldMetaData(apiName), new ApiFieldMetaData());
            if(StringUtils.hasContent(apiFieldMetaData.getReplacedByFieldName()))
            {
               orderBy.setFieldName(apiFieldMetaData.getReplacedByFieldName());
            }
            else if(apiFieldMetaData.getCustomValueMapper() != null)
            {
               ApiFieldCustomValueMapper customValueMapper = QCodeLoader.getAdHoc(ApiFieldCustomValueMapper.class, apiFieldMetaData.getCustomValueMapper());
               customValueMapper.customizeFilterOrderBy(queryInput, orderBy, apiFieldName, apiFieldMetaData);
            }
         }

      }
   }

}
