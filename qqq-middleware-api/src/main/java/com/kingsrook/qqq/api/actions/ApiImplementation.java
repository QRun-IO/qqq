/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.api.javalin.QBadRequestException;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.actions.HttpApiResponse;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiOperation;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessCustomizers;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInput;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInputFieldsContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessOutputInterface;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessUtils;
import com.kingsrook.qqq.api.model.metadata.processes.PostRunApiProcessCustomizer;
import com.kingsrook.qqq.api.model.metadata.processes.PreRunApiProcessCustomizer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobState;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.async.JobGoingAsyncException;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.CouldNotFindQueryFilterForExtractStepException;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IN;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiImplementation
{
   private static final QLogger LOG = QLogger.getLogger(ApiImplementation.class);

   ///////////////////////////////////////////////////////////////////
   // key:  Pair<apiName, apiVersion>, value: Map<name => metaData> //
   ///////////////////////////////////////////////////////////////////
   private static Map<Pair<String, String>, Map<String, QTableMetaData>> tableApiNameMap = new HashMap<>();



   /*******************************************************************************
    ** Allow tests (that manipulate meta-data) to clear field caches.
    *******************************************************************************/
   public static void clearCaches()
   {
      tableApiNameMap.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<String, Serializable> query(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, Map<String, List<String>> paramMap) throws QException
   {
      List<String> badRequestMessages = new ArrayList<>();

      QTableMetaData table     = validateTableAndVersion(apiInstanceMetaData, version, tableApiName, ApiOperation.QUERY_BY_QUERY_STRING);
      String         tableName = table.getName();
      String         apiName   = apiInstanceMetaData.getName();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setInputSource(QInputSource.USER);
      queryInput.setIncludeAssociations(true);
      queryInput.setShouldFetchHeavyFields(true);
      queryInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

      PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);

      String pageSizeParam     = getSingleParam(paramMap, "pageSize");
      String pageNoParam       = getSingleParam(paramMap, "pageNo");
      String booleanOperator   = getSingleParam(paramMap, "booleanOperator");
      String includeCountParam = getSingleParam(paramMap, "includeCount");
      String orderBy           = getSingleParam(paramMap, "orderBy");

      Integer pageSize = 50;
      if(StringUtils.hasContent(pageSizeParam))
      {
         try
         {
            pageSize = ValueUtils.getValueAsInteger(pageSizeParam);
         }
         catch(Exception e)
         {
            badRequestMessages.add("Could not parse pageSize as an integer");
         }
      }
      if(pageSize < 1 || pageSize > 1000)
      {
         badRequestMessages.add("pageSize must be between 1 and 1000.");
      }

      Integer pageNo = 1;
      if(StringUtils.hasContent(pageNoParam))
      {
         try
         {
            pageNo = ValueUtils.getValueAsInteger(pageNoParam);
         }
         catch(Exception e)
         {
            badRequestMessages.add("Could not parse pageNo as an integer");
         }
      }
      if(pageNo < 1)
      {
         badRequestMessages.add("pageNo must be greater than 0.");
      }

      QQueryFilter filter = new QQueryFilter();
      filter.setLimit(pageSize);
      filter.setSkip((pageNo - 1) * pageSize);

      // queryInput.setQueryJoins(processQueryJoinsParam(context));

      if("and".equalsIgnoreCase(booleanOperator))
      {
         filter.setBooleanOperator(QQueryFilter.BooleanOperator.AND);
      }
      else if("or".equalsIgnoreCase(booleanOperator))
      {
         filter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);
      }
      else if(StringUtils.hasContent(booleanOperator))
      {
         badRequestMessages.add("booleanOperator must be either AND or OR.");
      }

      boolean includeCount = true;
      if("true".equalsIgnoreCase(includeCountParam))
      {
         includeCount = true;
      }
      else if("false".equalsIgnoreCase(includeCountParam))
      {
         includeCount = false;
      }
      else if(StringUtils.hasContent(includeCountParam))
      {
         badRequestMessages.add("includeCount must be either true or false");
      }

      Map<String, QFieldMetaData> tableApiFields = GetTableApiFieldsAction.getTableApiFieldMap(new GetTableApiFieldsInput().withApiName(apiName).withVersion(version).withTableName(table.getName()).withInputSource(QInputSource.USER));

      if(StringUtils.hasContent(orderBy))
      {
         for(String orderByPart : orderBy.split(","))
         {
            orderByPart = orderByPart.trim();
            String[] orderByNameDirection = orderByPart.split(" +");
            boolean  asc                  = true;
            String   apiFieldName         = orderByNameDirection[0];
            if(orderByNameDirection.length == 2)
            {
               if("asc".equalsIgnoreCase(orderByNameDirection[1]))
               {
                  asc = true;
               }
               else if("desc".equalsIgnoreCase(orderByNameDirection[1]))
               {
                  asc = false;
               }
               else
               {
                  badRequestMessages.add("orderBy direction for field " + apiFieldName + " must be either ASC or DESC.");
               }
            }
            else if(orderByNameDirection.length > 2)
            {
               badRequestMessages.add("Unrecognized format for orderBy clause: " + orderByPart + ".  Expected:  fieldName [ASC|DESC].");
            }

            QFieldMetaData field = tableApiFields.get(apiFieldName);
            if(field == null)
            {
               badRequestMessages.add("Unrecognized orderBy field name: " + apiFieldName + ".");
            }
            else
            {
               QFilterOrderBy filterOrderBy = new QFilterOrderBy(field.getName(), asc);

               ApiFieldMetaData apiFieldMetaData = ObjectUtils.tryAndRequireNonNullElse(() -> ApiFieldMetaDataContainer.of(field).getApiFieldMetaData(apiName), new ApiFieldMetaData());
               if(StringUtils.hasContent(apiFieldMetaData.getReplacedByFieldName()))
               {
                  filterOrderBy.setFieldName(apiFieldMetaData.getReplacedByFieldName());
               }
               else if(apiFieldMetaData.getCustomValueMapper() != null)
               {
                  ApiFieldCustomValueMapper customValueMapper = QCodeLoader.getAdHoc(ApiFieldCustomValueMapper.class, apiFieldMetaData.getCustomValueMapper());
                  customValueMapper.customizeFilterOrderBy(queryInput, filterOrderBy, apiFieldName, apiFieldMetaData);
               }

               filter.withOrderBy(filterOrderBy);
            }
         }
      }
      else
      {
         filter.withOrderBy(new QFilterOrderBy(table.getPrimaryKeyField(), false));
      }

      Set<String> nonFilterParams = Set.of("pageSize", "pageNo", "orderBy", "booleanOperator", "includeCount");

      ////////////////////////////
      // look for filter params //
      ////////////////////////////
      for(Map.Entry<String, List<String>> entry : paramMap.entrySet())
      {
         String       name   = entry.getKey();
         List<String> values = entry.getValue();

         if(nonFilterParams.contains(name))
         {
            continue;
         }

         QFieldMetaData field = tableApiFields.get(name);
         if(field == null)
         {
            badRequestMessages.add("Unrecognized filter criteria field: " + name);
         }
         else
         {
            ApiFieldMetaData apiFieldMetaData = ObjectUtils.tryAndRequireNonNullElse(() -> ApiFieldMetaDataContainer.of(field).getApiFieldMetaData(apiName), new ApiFieldMetaData());
            for(String value : values)
            {
               if(StringUtils.hasContent(value))
               {
                  try
                  {
                     QFilterCriteria criteria = parseQueryParamToCriteria(field, name, value);

                     /////////////////////////////////////////////
                     // deal with replaced or customized fields //
                     /////////////////////////////////////////////
                     if(StringUtils.hasContent(apiFieldMetaData.getReplacedByFieldName()))
                     {
                        criteria.setFieldName(apiFieldMetaData.getReplacedByFieldName());
                     }
                     else if(apiFieldMetaData.getCustomValueMapper() != null)
                     {
                        ApiFieldCustomValueMapper customValueMapper = QCodeLoader.getAdHoc(ApiFieldCustomValueMapper.class, apiFieldMetaData.getCustomValueMapper());
                        customValueMapper.customizeFilterCriteriaForQueryOrCount(queryInput, filter, criteria, name, apiFieldMetaData);
                     }

                     filter.addCriteria(criteria);
                  }
                  catch(Exception e)
                  {
                     badRequestMessages.add(e.getMessage());
                  }
               }
            }
         }
      }

      //////////////////////////////////////////
      // no more badRequest checks below here //
      //////////////////////////////////////////
      if(!badRequestMessages.isEmpty())
      {
         if(badRequestMessages.size() == 1)
         {
            throw (new QBadRequestException(badRequestMessages.get(0)));
         }
         else
         {
            throw (new QBadRequestException("Request failed with " + badRequestMessages.size() + " reasons: " + StringUtils.join("\n", badRequestMessages)));
         }
      }

      //////////////////
      // do the query //
      //////////////////
      QueryAction queryAction = new QueryAction();
      queryInput.setFilter(filter);
      QueryOutput queryOutput = queryAction.execute(queryInput);

      Map<String, Serializable> output = new LinkedHashMap<>();
      output.put("pageNo", pageNo);
      output.put("pageSize", pageSize);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // map record fields for api                                                                                  //
      // note - don't put them in the output until after the count, just because that looks a little nicer, i think //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      ArrayList<Map<String, Serializable>> records = QRecordApiAdapter.qRecordsToApiMapList(queryOutput.getRecords(), tableName, apiName, version);

      /////////////////////////////
      // optionally do the count //
      /////////////////////////////
      if(includeCount)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // todo - at one time we wondered if we might need a call to customValueMapper.customizeFilterCriteriaForQueryOrCount //
         // as the filter would have already gone through there, but not other attributes of the input, e.g, joins...          //
         // but, instead we're trying to just put the query joins in here FROM the query input...                              //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         CountInput countInput = new CountInput();
         countInput.setTableName(tableName);
         countInput.setQueryJoins(queryInput.getQueryJoins());
         countInput.setFilter(filter);
         countInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
         CountOutput countOutput = new CountAction().execute(countInput);
         output.put("count", countOutput.getCount());
      }

      output.put("records", records);

      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<String, Serializable> insert(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, String body) throws QException
   {
      QTableMetaData table     = validateTableAndVersion(apiInstanceMetaData, version, tableApiName, ApiOperation.INSERT);
      String         tableName = table.getName();

      InsertInput insertInput = new InsertInput();
      insertInput.setInputSource(QInputSource.USER);

      insertInput.setTableName(tableName);

      PermissionsHelper.checkTablePermissionThrowing(insertInput, TablePermissionSubType.INSERT);

      try
      {
         if(!StringUtils.hasContent(body))
         {
            throw (new QBadRequestException("Missing required POST body"));
         }

         JSONTokener jsonTokener = new JSONTokener(body.trim());
         JSONObject  jsonObject  = new JSONObject(jsonTokener);

         insertInput.setRecords(List.of(QRecordApiAdapter.apiJsonObjectToQRecord(jsonObject, tableName, apiInstanceMetaData.getName(), version, false, QInputSource.USER)));

         if(jsonTokener.more())
         {
            throw (new QBadRequestException("Body contained more than a single JSON object."));
         }
      }
      catch(QBadRequestException qbre)
      {
         throw (qbre);
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Body could not be parsed as a JSON object: " + e.getMessage(), e));
      }

      InsertAction insertAction = new InsertAction();
      InsertOutput insertOutput = insertAction.execute(insertInput);

      List<QErrorMessage> errors = insertOutput.getRecords().get(0).getErrors();
      if(CollectionUtils.nullSafeHasContents(errors))
      {
         boolean isBadRequest = areAnyErrorsBadRequest(errors);

         String message = "Error inserting " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(errors);
         if(isBadRequest)
         {
            throw (new QBadRequestException(message));
         }
         else
         {
            throw (new QException(message));
         }
      }

      LinkedHashMap<String, Serializable> outputRecord = new LinkedHashMap<>();
      outputRecord.put(table.getPrimaryKeyField(), insertOutput.getRecords().get(0).getValue(table.getPrimaryKeyField()));

      List<QWarningMessage> warnings = insertOutput.getRecords().get(0).getWarnings();
      if(CollectionUtils.nullSafeHasContents(warnings))
      {
         outputRecord.put("warning", "Warning inserting " + table.getLabel() + ", some data may have been inserted: " + joinErrorsWithCommasAndAnd(warnings));
      }

      return (outputRecord);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<Map<String, Serializable>> bulkInsert(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, String body) throws QException
   {
      QTableMetaData table     = validateTableAndVersion(apiInstanceMetaData, version, tableApiName, ApiOperation.BULK_INSERT);
      String         tableName = table.getName();

      InsertInput insertInput = new InsertInput();
      insertInput.setInputSource(QInputSource.USER);
      insertInput.setTableName(tableName);

      PermissionsHelper.checkTablePermissionThrowing(insertInput, TablePermissionSubType.INSERT);

      /////////////////
      // build input //
      /////////////////
      try
      {
         if(!StringUtils.hasContent(body))
         {
            throw (new QBadRequestException("Missing required POST body"));
         }

         ArrayList<QRecord> recordList = new ArrayList<>();
         insertInput.setRecords(recordList);

         JSONTokener jsonTokener = new JSONTokener(body.trim());
         JSONArray   jsonArray   = new JSONArray(jsonTokener);

         for(int i = 0; i < jsonArray.length(); i++)
         {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            recordList.add(QRecordApiAdapter.apiJsonObjectToQRecord(jsonObject, tableName, apiInstanceMetaData.getName(), version, false, QInputSource.USER));
         }

         if(jsonTokener.more())
         {
            throw (new QBadRequestException("Body contained more than a single JSON array."));
         }

         if(recordList.isEmpty())
         {
            throw (new QBadRequestException("No records were found in the POST body"));
         }
      }
      catch(QBadRequestException qbre)
      {
         throw (qbre);
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Body could not be parsed as a JSON array: " + e.getMessage(), e));
      }

      //////////////
      // execute! //
      //////////////
      InsertAction insertAction = new InsertAction();
      InsertOutput insertOutput = insertAction.execute(insertInput);

      ///////////////////////////////////////
      // process records to build response //
      ///////////////////////////////////////
      List<Map<String, Serializable>> response = new ArrayList<>();
      for(QRecord record : insertOutput.getRecords())
      {
         LinkedHashMap<String, Serializable> outputRecord = new LinkedHashMap<>();
         response.add(outputRecord);

         List<QErrorMessage>   errors   = record.getErrors();
         List<QWarningMessage> warnings = record.getWarnings();
         if(CollectionUtils.nullSafeHasContents(errors))
         {
            if(areAnyErrorsBadRequest(errors))
            {
               outputRecord.put("statusCode", HttpStatus.Code.BAD_REQUEST.getCode());
               outputRecord.put("statusText", HttpStatus.Code.BAD_REQUEST.getMessage());
            }
            else
            {
               outputRecord.put("statusCode", HttpStatus.Code.INTERNAL_SERVER_ERROR.getCode());
               outputRecord.put("statusText", HttpStatus.Code.INTERNAL_SERVER_ERROR.getMessage());
            }
            outputRecord.put("error", "Error inserting " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(errors));
         }
         else if(CollectionUtils.nullSafeHasContents(warnings))
         {
            outputRecord.put("statusCode", HttpStatus.Code.CREATED.getCode());
            outputRecord.put("statusText", HttpStatus.Code.CREATED.getMessage());
            outputRecord.put("warning", "Warning inserting " + table.getLabel() + ", some data may have been inserted: " + joinErrorsWithCommasAndAnd(warnings));
            outputRecord.put(table.getPrimaryKeyField(), record.getValue(table.getPrimaryKeyField()));
         }
         else
         {
            outputRecord.put("statusCode", HttpStatus.Code.CREATED.getCode());
            outputRecord.put("statusText", HttpStatus.Code.CREATED.getMessage());
            outputRecord.put(table.getPrimaryKeyField(), record.getValue(table.getPrimaryKeyField()));
         }
      }

      return (response);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<String, Serializable> get(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, String primaryKey) throws QException
   {
      QTableMetaData table     = validateTableAndVersion(apiInstanceMetaData, version, tableApiName, ApiOperation.GET);
      String         tableName = table.getName();

      GetInput getInput = new GetInput();
      getInput.setTableName(tableName);
      getInput.setInputSource(QInputSource.USER);
      getInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

      PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);

      getInput.setPrimaryKey(primaryKey);
      getInput.setIncludeAssociations(true);
      getInput.setShouldFetchHeavyFields(true);

      GetAction getAction = new GetAction();
      GetOutput getOutput = getAction.execute(getInput);

      ///////////////////////////////////////////////////////
      // throw a not found error if the record isn't found //
      ///////////////////////////////////////////////////////
      QRecord record = getOutput.getRecord();
      if(record == null)
      {
         throw (new QNotFoundException("Could not find " + table.getLabel() + " with "
            + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
      }

      Map<String, Serializable> outputRecord = QRecordApiAdapter.qRecordsToApiMapList(List.of(record), tableName, apiInstanceMetaData.getName(), version).get(0);
      return (outputRecord);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void update(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, String primaryKey, String body) throws QException
   {
      QTableMetaData table     = validateTableAndVersion(apiInstanceMetaData, version, tableApiName, ApiOperation.UPDATE);
      String         tableName = table.getName();

      UpdateInput updateInput = new UpdateInput();
      updateInput.setInputSource(QInputSource.USER);
      updateInput.setTableName(tableName);

      PermissionsHelper.checkTablePermissionThrowing(updateInput, TablePermissionSubType.EDIT);

      try
      {
         if(!StringUtils.hasContent(body))
         {
            throw (new QBadRequestException("Missing required PATCH body"));
         }

         JSONTokener jsonTokener = new JSONTokener(body.trim());
         JSONObject  jsonObject  = new JSONObject(jsonTokener);

         QRecord qRecord = QRecordApiAdapter.apiJsonObjectToQRecord(jsonObject, tableName, apiInstanceMetaData.getName(), version, false, QInputSource.USER);
         qRecord.setValue(table.getPrimaryKeyField(), primaryKey);
         updateInput.setRecords(List.of(qRecord));

         if(jsonTokener.more())
         {
            throw (new QBadRequestException("Body contained more than a single JSON object."));
         }
      }
      catch(QBadRequestException qbre)
      {
         throw (qbre);
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Body could not be parsed as a JSON object: " + e.getMessage(), e));
      }

      UpdateAction updateAction = new UpdateAction();
      UpdateOutput updateOutput = updateAction.execute(updateInput);

      List<QErrorMessage> errors = updateOutput.getRecords().get(0).getErrors();
      // todo - do we want, if there were warnings, to return a 200 w/ a body w/ the warnings?  maybe...
      if(CollectionUtils.nullSafeHasContents(errors))
      {
         if(areAnyErrorsNotFound(errors))
         {
            throw (new QNotFoundException("Could not find " + table.getLabel() + " with " + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
         }
         else
         {
            boolean isBadRequest = areAnyErrorsBadRequest(errors);

            String message = "Error updating " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(errors);
            if(isBadRequest)
            {
               throw (new QBadRequestException(message));
            }
            else
            {
               throw (new QException(message));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<Map<String, Serializable>> bulkUpdate(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, String body) throws QException
   {
      QTableMetaData table     = validateTableAndVersion(apiInstanceMetaData, version, tableApiName, ApiOperation.BULK_UPDATE);
      String         tableName = table.getName();

      UpdateInput updateInput = new UpdateInput();
      updateInput.setInputSource(QInputSource.USER);
      updateInput.setTableName(tableName);

      PermissionsHelper.checkTablePermissionThrowing(updateInput, TablePermissionSubType.EDIT);

      /////////////////
      // build input //
      /////////////////
      try
      {
         if(!StringUtils.hasContent(body))
         {
            throw (new QBadRequestException("Missing required PATCH body"));
         }

         ArrayList<QRecord> recordList = new ArrayList<>();
         updateInput.setRecords(recordList);

         JSONTokener jsonTokener = new JSONTokener(body.trim());
         JSONArray   jsonArray   = new JSONArray(jsonTokener);

         for(int i = 0; i < jsonArray.length(); i++)
         {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            recordList.add(QRecordApiAdapter.apiJsonObjectToQRecord(jsonObject, tableName, apiInstanceMetaData.getName(), version, true, QInputSource.USER));
         }

         if(jsonTokener.more())
         {
            throw (new QBadRequestException("Body contained more than a single JSON array."));
         }

         if(recordList.isEmpty())
         {
            throw (new QBadRequestException("No records were found in the PATCH body"));
         }
      }
      catch(QBadRequestException qbre)
      {
         throw (qbre);
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Body could not be parsed as a JSON array: " + e.getMessage(), e));
      }

      //////////////
      // execute! //
      //////////////
      UpdateAction updateAction = new UpdateAction();
      UpdateOutput updateOutput = updateAction.execute(updateInput);

      ///////////////////////////////////////
      // process records to build response //
      ///////////////////////////////////////
      List<Map<String, Serializable>> response = new ArrayList<>();
      int                             i        = 0;
      for(QRecord record : updateOutput.getRecords())
      {
         LinkedHashMap<String, Serializable> outputRecord = new LinkedHashMap<>();
         response.add(outputRecord);

         try
         {
            QRecord      inputRecord = updateInput.getRecords().get(i);
            Serializable primaryKey  = inputRecord.getValue(table.getPrimaryKeyField());
            outputRecord.put(table.getPrimaryKeyField(), primaryKey);
         }
         catch(Exception e)
         {
            //////////
            // omit //
            //////////
         }

         List<QErrorMessage> errors = record.getErrors();

         HttpStatus.Code statusCode;
         if(CollectionUtils.nullSafeHasContents(errors))
         {
            outputRecord.put("error", "Error updating " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(errors));
            if(areAnyErrorsNotFound(errors))
            {
               statusCode = HttpStatus.Code.NOT_FOUND;
            }
            else if(areAnyErrorsBadRequest(errors))
            {
               statusCode = HttpStatus.Code.BAD_REQUEST;
            }
            else
            {
               statusCode = HttpStatus.Code.INTERNAL_SERVER_ERROR;
            }
         }
         else
         {
            statusCode = HttpStatus.Code.NO_CONTENT;

            List<QWarningMessage> warnings = record.getWarnings();
            if(CollectionUtils.nullSafeHasContents(warnings))
            {
               outputRecord.put("warning", "Warning updating " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(warnings));
            }
         }

         outputRecord.put("statusCode", statusCode.getCode());
         outputRecord.put("statusText", statusCode.getMessage());

         i++;
      }

      return (response);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void delete(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, String primaryKey) throws QException
   {
      QTableMetaData table     = validateTableAndVersion(apiInstanceMetaData, version, tableApiName, ApiOperation.DELETE);
      String         tableName = table.getName();

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setInputSource(QInputSource.USER);
      deleteInput.setTableName(tableName);
      deleteInput.setPrimaryKeys(List.of(primaryKey));

      PermissionsHelper.checkTablePermissionThrowing(deleteInput, TablePermissionSubType.DELETE);

      ///////////////////
      // do the delete //
      ///////////////////
      DeleteAction deleteAction = new DeleteAction();
      DeleteOutput deleteOutput = deleteAction.execute(deleteInput);
      if(CollectionUtils.nullSafeHasContents(deleteOutput.getRecordsWithErrors()))
      {
         List<QErrorMessage> errors = deleteOutput.getRecordsWithErrors().get(0).getErrors();
         if(areAnyErrorsNotFound(errors))
         {
            throw (new QNotFoundException("Could not find " + table.getLabel() + " with " + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
         }
         else if(areAnyErrorsBadRequest(errors))
         {
            throw (new QBadRequestException("Error deleting " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(errors)));
         }
         // todo - do we want, if there were warnings, to return a 200 w/ a body w/ the warnings?  maybe...
         else
         {
            throw (new QException("Error deleting " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(errors)));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<Map<String, Serializable>> bulkDelete(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, String body) throws QException
   {
      QTableMetaData table     = validateTableAndVersion(apiInstanceMetaData, version, tableApiName, ApiOperation.BULK_DELETE);
      String         tableName = table.getName();

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setInputSource(QInputSource.USER);
      deleteInput.setTableName(tableName);

      PermissionsHelper.checkTablePermissionThrowing(deleteInput, TablePermissionSubType.DELETE);

      /////////////////
      // build input //
      /////////////////
      try
      {
         if(!StringUtils.hasContent(body))
         {
            throw (new QBadRequestException("Missing required DELETE body"));
         }

         ArrayList<Serializable> primaryKeyList = new ArrayList<>();
         deleteInput.setPrimaryKeys(primaryKeyList);

         JSONTokener jsonTokener = new JSONTokener(body.trim());
         JSONArray   jsonArray   = new JSONArray(jsonTokener);

         for(int i = 0; i < jsonArray.length(); i++)
         {
            Object object = jsonArray.get(i);
            if(object instanceof JSONArray || object instanceof JSONObject)
            {
               throw (new QBadRequestException("One or more elements inside the DELETE body JSONArray was not a primitive value"));
            }
            primaryKeyList.add(String.valueOf(object));
         }

         if(jsonTokener.more())
         {
            throw (new QBadRequestException("Body contained more than a single JSON array."));
         }

         if(primaryKeyList.isEmpty())
         {
            throw (new QBadRequestException("No primary keys were found in the DELETE body"));
         }
      }
      catch(QBadRequestException qbre)
      {
         throw (qbre);
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Body could not be parsed as a JSON array: " + e.getMessage(), e));
      }

      //////////////
      // execute! //
      //////////////
      DeleteAction deleteAction = new DeleteAction();
      DeleteOutput deleteOutput = deleteAction.execute(deleteInput);

      ///////////////////////////////////////
      // process records to build response //
      ///////////////////////////////////////
      List<Map<String, Serializable>> response = new ArrayList<>();

      List<QRecord>                    recordsWithErrors     = deleteOutput.getRecordsWithErrors();
      Map<String, List<QErrorMessage>> primaryKeyToErrorsMap = new HashMap<>();
      for(QRecord recordWithError : CollectionUtils.nonNullList(recordsWithErrors))
      {
         String primaryKey = recordWithError.getValueString(table.getPrimaryKeyField());
         primaryKeyToErrorsMap.put(primaryKey, recordWithError.getErrors());
      }

      for(Serializable primaryKey : deleteInput.getPrimaryKeys())
      {
         LinkedHashMap<String, Serializable> outputRecord = new LinkedHashMap<>();
         response.add(outputRecord);
         outputRecord.put(table.getPrimaryKeyField(), primaryKey);

         String              primaryKeyString = ValueUtils.getValueAsString(primaryKey);
         List<QErrorMessage> errors           = primaryKeyToErrorsMap.get(primaryKeyString);
         if(CollectionUtils.nullSafeHasContents(errors))
         {
            outputRecord.put("error", "Error deleting " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(errors));
            if(areAnyErrorsNotFound(errors))
            {
               outputRecord.put("statusCode", HttpStatus.Code.NOT_FOUND.getCode());
               outputRecord.put("statusText", HttpStatus.Code.NOT_FOUND.getMessage());
            }
            else
            {
               outputRecord.put("statusCode", HttpStatus.Code.BAD_REQUEST.getCode());
               outputRecord.put("statusText", HttpStatus.Code.BAD_REQUEST.getMessage());
            }
         }
         else
         {
            outputRecord.put("statusCode", HttpStatus.Code.NO_CONTENT.getCode());
            outputRecord.put("statusText", HttpStatus.Code.NO_CONTENT.getMessage());
         }
      }

      return (response);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static HttpApiResponse runProcess(ApiInstanceMetaData apiInstanceMetaData, String version, String processApiName, Map<String, String> paramMap) throws QException
   {
      Pair<ApiProcessMetaData, QProcessMetaData> pair = ApiProcessUtils.getProcessMetaDataPair(apiInstanceMetaData, version, processApiName);

      ApiProcessMetaData apiProcessMetaData = pair.getA();
      QProcessMetaData   process            = pair.getB();
      String             processName        = process.getName();

      List<String> badRequestMessages = new ArrayList<>();

      String processUUID = UUID.randomUUID().toString();

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(processName);
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      runProcessInput.setProcessUUID(processUUID);
      // todo i don't think runProcessInput.setAsyncJobCallback();

      PermissionsHelper.checkProcessPermissionThrowing(runProcessInput, processName);

      //////////////////////
      // map input values //
      //////////////////////
      ApiProcessInput apiProcessInput = apiProcessMetaData.getInput();
      if(apiProcessInput != null)
      {
         processProcessInputFields(paramMap, badRequestMessages, runProcessInput, apiProcessInput.getPathParams());
         processProcessInputFields(paramMap, badRequestMessages, runProcessInput, apiProcessInput.getQueryStringParams());
         processProcessInputFields(paramMap, badRequestMessages, runProcessInput, apiProcessInput.getFormParams());
         processProcessInputFields(paramMap, badRequestMessages, runProcessInput, apiProcessInput.getObjectBodyParams());

         if(apiProcessInput.getBodyField() != null)
         {
            processSingleProcessInputField(apiProcessInput.getBodyField(), paramMap, badRequestMessages, runProcessInput);
         }
      }

      ////////////////////////////////////////
      // get records for process, if needed //
      ////////////////////////////////////////
      // if(process.getMinInputRecords() != null && process.getMinInputRecords() > 0)
      if(apiProcessInput != null && apiProcessInput.getRecordIdsParamName() != null)
      {
         String idParam = apiProcessInput.getRecordIdsParamName();
         if(StringUtils.hasContent(idParam) && StringUtils.hasContent(paramMap.get(idParam)))
         {
            String[] ids = paramMap.get(idParam).split(",");

            if(StringUtils.hasContent(process.getTableName()))
            {
               QTableMetaData table  = QContext.getQInstance().getTable(process.getTableName());
               QQueryFilter   filter = new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), IN, Arrays.asList(ids)));
               runProcessInput.setCallback(getProcessCallback(filter));
            }
            else
            {
               runProcessInput.addValue(idParam, paramMap.get(idParam));
            }
         }
      }

      /////////////////////////////////////////
      // throw if bad inputs have been noted //
      /////////////////////////////////////////
      if(!badRequestMessages.isEmpty())
      {
         if(badRequestMessages.size() == 1)
         {
            throw (new QBadRequestException(badRequestMessages.get(0)));
         }
         else
         {
            throw (new QBadRequestException("Request failed with " + badRequestMessages.size() + " reasons: " + StringUtils.join("\n", badRequestMessages)));
         }
      }

      /////////////////////////////////////////
      // run pre-customizer, if there is one //
      /////////////////////////////////////////
      Map<String, QCodeReference> customizers = apiProcessMetaData.getCustomizers();
      if(customizers != null && customizers.containsKey(ApiProcessCustomizers.PRE_RUN.getRole()))
      {
         PreRunApiProcessCustomizer preRunCustomizer = QCodeLoader.getAdHoc(PreRunApiProcessCustomizer.class, customizers.get(ApiProcessCustomizers.PRE_RUN.getRole()));
         preRunCustomizer.preApiRun(runProcessInput);
      }

      boolean async = false;
      if(ApiProcessMetaData.AsyncMode.ALWAYS.equals(apiProcessMetaData.getAsyncMode())
         || (ApiProcessMetaData.AsyncMode.OPTIONAL.equals(apiProcessMetaData.getAsyncMode()) && "true".equalsIgnoreCase(paramMap.get("async"))))
      {
         async = true;
      }

      if(async)
      {
         try
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            // note - in other implementations, the process gets its own UUID (for process state to be stashed) //
            // and the job gets its own (where we check in on running/complete).                                //
            // but in this implementation, we want to just pass back one UUID to the caller, so make the job    //
            // manager use the process's uuid as the job uuid, and all will be revealed!                        //
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            // todo?  to help w/ StreamedETLPreview "should i count?"  runProcessInput.setIsAsync(true);
            new AsyncJobManager().withForcedJobUUID(processUUID).startJob(processName, 0, TimeUnit.MILLISECONDS, (callback) ->
            {
               runProcessInput.setAsyncJobCallback(callback);
               return (new RunProcessAction().execute(runProcessInput));
            });
         }
         catch(JobGoingAsyncException jgae)
         {
            LinkedHashMap<String, Object> response = new LinkedHashMap<>();
            response.put("jobId", jgae.getJobUUID());
            return (new HttpApiResponse(HttpStatus.Code.ACCEPTED, response));
         }

         ////////////////////////////////////////////////////////////////////////////////////////
         // passing 0 as the timeout to startJob *should* make it always throw the JGAE.  But, //
         // in case it didn't, we don't have a uuid to return to the caller, so that's a fail. //
         ////////////////////////////////////////////////////////////////////////////////////////
         throw (new QException("Error starting asynchronous job - no job id was returned."));
      }

      /////////////////////
      // run the process //
      /////////////////////
      RunProcessOutput runProcessOutput;

      try
      {
         RunProcessAction runProcessAction = new RunProcessAction();
         runProcessOutput = runProcessAction.execute(runProcessInput);
      }
      catch(CouldNotFindQueryFilterForExtractStepException e)
      {
         throw (new QBadRequestException("Records to run through this process were not specified."));
      }
      catch(Exception e)
      {
         String concatenation = ExceptionUtils.concatenateMessagesFromChain(e);
         throw (new QException(concatenation, e));
      }

      return (buildResponseAfterProcess(apiProcessMetaData, runProcessInput, runProcessOutput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static HttpApiResponse buildResponseAfterProcess(ApiProcessMetaData apiProcessMetaData, RunProcessInput runProcessInput, RunProcessOutput runProcessOutput) throws QException
   {
      //////////////////////////////////////////
      // run post-customizer, if there is one //
      //////////////////////////////////////////
      Map<String, QCodeReference> customizers = apiProcessMetaData.getCustomizers();
      if(customizers != null && customizers.containsKey(ApiProcessCustomizers.POST_RUN.getRole()))
      {
         PostRunApiProcessCustomizer postRunCustomizer = QCodeLoader.getAdHoc(PostRunApiProcessCustomizer.class, customizers.get(ApiProcessCustomizers.POST_RUN.getRole()));
         postRunCustomizer.postApiRun(runProcessInput, runProcessOutput);
      }

      ///////////////////////
      // map output values //
      ///////////////////////
      ApiProcessOutputInterface output = apiProcessMetaData.getOutput();
      if(output != null)
      {
         Serializable    outputForProcess = output.getOutputForProcess(runProcessInput, runProcessOutput);
         HttpApiResponse httpApiResponse  = new HttpApiResponse(output.getSuccessStatusCode(runProcessInput, runProcessOutput), outputForProcess);
         output.customizeHttpApiResponse(httpApiResponse, runProcessInput, runProcessOutput);
         return httpApiResponse;
      }
      else
      {
         return (new HttpApiResponse(HttpStatus.Code.NO_CONTENT, ""));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processProcessInputFields(Map<String, String> paramMap, List<String> badRequestMessages, RunProcessInput runProcessInput, ApiProcessInputFieldsContainer fieldsContainer)
   {
      if(fieldsContainer == null)
      {
         return;
      }

      for(QFieldMetaData inputField : CollectionUtils.nonNullList(fieldsContainer.getFields()))
      {
         processSingleProcessInputField(inputField, paramMap, badRequestMessages, runProcessInput);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processSingleProcessInputField(QFieldMetaData inputField, Map<String, String> paramMap, List<String> badRequestMessages, RunProcessInput runProcessInput)
   {
      String value = paramMap.get(inputField.getName());

      if(!StringUtils.hasContent(value) && inputField.getDefaultValue() != null)
      {
         value = ValueUtils.getValueAsString(inputField.getDefaultValue());
      }

      if(!StringUtils.hasContent(value) && inputField.getIsRequired())
      {
         badRequestMessages.add("Missing value for required input field " + inputField.getName());
         return;
      }

      // todo - types?

      runProcessInput.addValue(inputField.getName(), value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static HttpApiResponse getProcessStatus(ApiInstanceMetaData apiInstanceMetaData, String version, String apiProcessName, String jobUUID) throws QException
   {
      Optional<AsyncJobStatus> optionalJobStatus = new AsyncJobManager().getJobStatus(jobUUID);
      if(optionalJobStatus.isEmpty())
      {
         throw (new QException("Could not find status of process job: " + jobUUID));
      }

      AsyncJobStatus jobStatus = optionalJobStatus.get();

      // resultForCaller.put("jobStatus", jobStatus);
      LOG.debug("Job status is " + jobStatus.getState() + " for " + jobUUID);

      if(jobStatus.getState().equals(AsyncJobState.COMPLETE))
      {
         ///////////////////////////////////////////////////////////////////////////////////////
         // if the job is complete, get the process result from state provider, and return it //
         // this output should look like it did if the job finished synchronously!!           //
         ///////////////////////////////////////////////////////////////////////////////////////
         Optional<ProcessState> processState = RunProcessAction.getState(jobUUID);
         if(processState.isPresent())
         {
            RunProcessOutput runProcessOutput = new RunProcessOutput(processState.get());
            RunProcessInput  runProcessInput  = new RunProcessInput();
            runProcessInput.seedFromProcessState(processState.get());

            Pair<ApiProcessMetaData, QProcessMetaData> pair = ApiProcessUtils.getProcessMetaDataPair(apiInstanceMetaData, version, apiProcessName);

            ApiProcessMetaData apiProcessMetaData = pair.getA();
            return (buildResponseAfterProcess(apiProcessMetaData, runProcessInput, runProcessOutput));
         }
         else
         {
            throw (new QException("Could not find results for completed of process job: " + jobUUID));
         }
      }
      else if(jobStatus.getState().equals(AsyncJobState.ERROR))
      {
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the job had an error (e.g., a process step threw), "nicely" serialize its exception for the caller //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(jobStatus.getCaughtException() != null)
         {
            throw (new QException(jobStatus.getCaughtException()));
         }
         else
         {
            throw (new QException("Job failed with an unspecified error."));
         }
      }
      else
      {
         LinkedHashMap<String, Object> response = new LinkedHashMap<>();
         response.put("jobId", jobUUID);
         response.put("message", jobStatus.getMessage());
         if(jobStatus.getCurrent() != null && jobStatus.getTotal() != null)
         {
            response.put("current", jobStatus.getCurrent());
            response.put("total", jobStatus.getTotal());
         }
         return (new HttpApiResponse(HttpStatus.Code.ACCEPTED, response));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String joinErrorsWithCommasAndAnd(List<? extends QStatusMessage> errors)
   {
      return StringUtils.joinWithCommasAndAnd(errors.stream().map(QStatusMessage::getMessage).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String getSingleParam(Map<String, List<String>> paramMap, String name)
   {
      if(CollectionUtils.nullSafeHasContents(paramMap.get(name)))
      {
         return (paramMap.get(name).get(0));
      }

      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private enum Operator
   {
      ///////////////////////////////////////////////////////////////////////////////////
      // order of these is important (e.g., because some are a sub-string of others!!) //
      ///////////////////////////////////////////////////////////////////////////////////
      EQ("=", QCriteriaOperator.EQUALS, QCriteriaOperator.NOT_EQUALS, 1),
      LTE("<=", QCriteriaOperator.LESS_THAN_OR_EQUALS, null, 1),
      GTE(">=", QCriteriaOperator.GREATER_THAN_OR_EQUALS, null, 1),
      LT("<", QCriteriaOperator.LESS_THAN, null, 1),
      GT(">", QCriteriaOperator.GREATER_THAN, null, 1),
      EMPTY("EMPTY", QCriteriaOperator.IS_BLANK, QCriteriaOperator.IS_NOT_BLANK, 0),
      BETWEEN("BETWEEN ", QCriteriaOperator.BETWEEN, QCriteriaOperator.NOT_BETWEEN, 2),
      IN("IN ", QCriteriaOperator.IN, QCriteriaOperator.NOT_IN, null),
      LIKE("LIKE ", QCriteriaOperator.LIKE, QCriteriaOperator.NOT_LIKE, 1);


      private final String            prefix;
      private final QCriteriaOperator positiveOperator;
      private final QCriteriaOperator negativeOperator;
      private final Integer           noOfValues; // null means many (IN)



      /*******************************************************************************
       **
       *******************************************************************************/
      Operator(String prefix, QCriteriaOperator positiveOperator, QCriteriaOperator negativeOperator, Integer noOfValues)
      {
         this.prefix = prefix;
         this.positiveOperator = positiveOperator;
         this.negativeOperator = negativeOperator;
         this.noOfValues = noOfValues;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QFilterCriteria parseQueryParamToCriteria(QFieldMetaData field, String name, String value) throws QException
   {
      ///////////////////////////////////
      // process & discard a leading ! //
      ///////////////////////////////////
      boolean isNot = false;
      if(value.startsWith("!") && value.length() > 1)
      {
         isNot = true;
         value = value.substring(1);
      }

      //////////////////////////
      // look for an operator //
      //////////////////////////
      Operator selectedOperator = null;
      for(Operator op : Operator.values())
      {
         if(value.startsWith(op.prefix))
         {
            selectedOperator = op;
            if(selectedOperator.negativeOperator == null && isNot)
            {
               throw (new QBadRequestException("Unsupported operator: !" + selectedOperator.prefix));
            }
            break;
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////////
      // if an operator was found, strip it away from the value for figuring out the values part //
      /////////////////////////////////////////////////////////////////////////////////////////////
      if(selectedOperator != null)
      {
         value = value.substring(selectedOperator.prefix.length());
      }
      else
      {
         ////////////////////////////////////////////////////////////////
         // else - assume the default operator, and use the full value //
         ////////////////////////////////////////////////////////////////
         selectedOperator = Operator.EQ;
      }

      ////////////////////////////////////
      // figure out the criteria values //
      // todo - quotes?                 //
      ////////////////////////////////////
      List<Serializable> criteriaValues;
      if(selectedOperator.noOfValues == null)
      {
         criteriaValues = Arrays.stream(value.split(",")).map(Serializable.class::cast).toList();
      }
      else if(selectedOperator.noOfValues == 1)
      {
         criteriaValues = ListBuilder.of(value);
      }
      else if(selectedOperator.noOfValues == 0)
      {
         if(StringUtils.hasContent(value))
         {
            throw (new QBadRequestException("Unexpected value after operator " + selectedOperator.prefix + " for field " + name));
         }
         criteriaValues = null;
      }
      else if(selectedOperator.noOfValues == 2)
      {
         criteriaValues = Arrays.stream(value.split(",")).map(Serializable.class::cast).toList();
         if(criteriaValues.size() != 2)
         {
            throw (new QBadRequestException("Operator " + selectedOperator.prefix + " for field " + name + " requires 2 values (received " + criteriaValues.size() + ")"));
         }
      }
      else
      {
         throw (new QException("Unexpected noOfValues [" + selectedOperator.noOfValues + "] in operator [" + selectedOperator + "]"));
      }

      if(field.getType().equals(QFieldType.BLOB))
      {
         if(!selectedOperator.equals(Operator.EMPTY))
         {
            throw (new QBadRequestException("Operator " + selectedOperator.prefix + " may not be used for field " + name + " (blob fields only support operators EMPTY or !EMPTY)"));
         }
      }

      return (new QFilterCriteria(name, isNot ? selectedOperator.negativeOperator : selectedOperator.positiveOperator, criteriaValues));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData validateTableAndVersion(ApiInstanceMetaData apiInstanceMetaData, String version, String tableApiName, ApiOperation operation) throws QNotFoundException
   {
      QTableMetaData table    = getTableByApiName(apiInstanceMetaData.getName(), version, tableApiName);
      LogPair[]      logPairs = new LogPair[] { logPair("apiName", apiInstanceMetaData.getName()), logPair("version", version), logPair("tableApiName", tableApiName), logPair("operation", operation) };

      if(table == null)
      {
         LOG.info("404 because table is null (tableApiName=" + tableApiName + ")", logPairs);
         throw (new QNotFoundException("Could not find a table named " + tableApiName + " in this api."));
      }

      if(BooleanUtils.isTrue(table.getIsHidden()))
      {
         LOG.info("404 because table isHidden", logPairs);
         throw (new QNotFoundException("Could not find a table named " + tableApiName + " in this api."));
      }

      ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
      if(apiTableMetaDataContainer == null)
      {
         LOG.info("404 because table apiTableMetaDataContainer is null", logPairs);
         throw (new QNotFoundException("Could not find a table named " + tableApiName + " in this api."));
      }

      ApiTableMetaData apiTableMetaData = apiTableMetaDataContainer.getApiTableMetaData(apiInstanceMetaData.getName());
      if(apiTableMetaData == null)
      {
         LOG.info("404 because table apiTableMetaData is null", logPairs);
         throw (new QNotFoundException("Could not find a table named " + tableApiName + " in this api."));
      }

      if(BooleanUtils.isTrue(apiTableMetaData.getIsExcluded()))
      {
         LOG.info("404 because table is excluded", logPairs);
         throw (new QNotFoundException("Could not find a table named " + tableApiName + " in this api."));
      }

      if(!operation.isOperationEnabled(List.of(apiInstanceMetaData, apiTableMetaData)))
      {
         LOG.info("404 because api operation is not enabled", logPairs);
         throw (new QNotFoundException("Cannot perform operation [" + operation + "] on table named " + tableApiName + " in this api."));
      }

      if(!table.isCapabilityEnabled(QContext.getQInstance().getBackendForTable(table.getName()), operation.getCapability()))
      {
         LOG.info("404 because table capability is not enabled", logPairs);
         throw (new QNotFoundException("Cannot perform operation [" + operation + "] on table named " + tableApiName + " in this api."));
      }

      APIVersion       requestApiVersion = new APIVersion(version);
      List<APIVersion> supportedVersions = apiInstanceMetaData.getSupportedVersions();
      if(CollectionUtils.nullSafeIsEmpty(supportedVersions) || !supportedVersions.contains(requestApiVersion))
      {
         LOG.info("404 because requested version is not supported", logPairs);
         throw (new QNotFoundException(version + " is not a supported version in this api."));
      }

      if(!apiTableMetaData.getApiVersionRange().includes(requestApiVersion))
      {
         LOG.info("404 because table version range does not include requested version", logPairs);
         throw (new QNotFoundException(version + " is not a supported version for table " + tableApiName + " in this api."));
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData getTableByApiName(String apiName, String version, String tableApiName)
   {
      /////////////////////////////////////////////////////////////////////////////////////////////
      // tableApiNameMap is a map of (apiName,apiVersion) => Map<String, QTableMetaData>.        //
      // that is to say, a 2-level map.  The first level is keyed by (apiName,apiVersion) pairs. //
      // the second level is keyed by tableApiNames.                                             //
      /////////////////////////////////////////////////////////////////////////////////////////////
      Pair<String, String> key = new Pair<>(apiName, version);
      if(tableApiNameMap.get(key) == null)
      {
         Map<String, QTableMetaData> map = new HashMap<>();

         for(QTableMetaData table : QContext.getQInstance().getTables().values())
         {
            ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
            if(apiTableMetaDataContainer != null)
            {
               ApiTableMetaData apiTableMetaData = apiTableMetaDataContainer.getApiTableMetaData(apiName);
               if(apiTableMetaData != null)
               {
                  String name = table.getName();
                  if(StringUtils.hasContent(apiTableMetaData.getApiTableName()))
                  {
                     name = apiTableMetaData.getApiTableName();
                  }
                  map.put(name, table);
               }
            }
         }

         tableApiNameMap.put(key, map);
      }

      return (tableApiNameMap.get(key).get(tableApiName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean areAnyErrorsBadRequest(List<QErrorMessage> errors)
   {
      return errors.stream().anyMatch(e -> (e instanceof BadInputStatusMessage) || (e instanceof PermissionDeniedMessage));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean areAnyErrorsNotFound(List<QErrorMessage> errors)
   {
      return errors.stream().anyMatch(e -> (e instanceof NotFoundStatusMessage));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessCallback getProcessCallback(QQueryFilter filter)
   {
      return new QProcessCallback()
      {
         @Override
         public QQueryFilter getQueryFilter()
         {
            return (filter);
         }



         @Override
         public Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
         {
            return (Collections.emptyMap());
         }
      };
   }

}
