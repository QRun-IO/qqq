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
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.api.actions.io.ApiOutputMapWrapper;
import com.kingsrook.qqq.api.actions.io.ApiOutputQRecordWrapper;
import com.kingsrook.qqq.api.actions.io.ApiOutputRecordWrapperInterface;
import com.kingsrook.qqq.api.actions.io.QRecordApiAdapterToApiInput;
import com.kingsrook.qqq.api.javalin.QBadRequestException;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.APIVersionRange;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapperBulkSupportInterface;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiAssociationMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import org.apache.commons.lang.BooleanUtils;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** Methods for going back and forth from QRecords to API-versions of objects
 *******************************************************************************/
public class QRecordApiAdapter
{
   private static final QLogger LOG = QLogger.getLogger(QRecordApiAdapter.class);



   /*******************************************************************************
    ** Simple/short form of convert a QRecord to a map for the API - e.g., meant for
    ** public consumption.
    *******************************************************************************/
   public static Map<String, Serializable> qRecordToApiMap(QRecord record, String tableName, String apiName, String apiVersion) throws QException
   {
      return qRecordsToApiMapList(ListBuilder.of(record), tableName, apiName, apiVersion).get(0);
   }



   /*******************************************************************************
    ** bulk-version of the qRecordToApiMap - will use
    ** ApiFieldCustomValueMapperBulkSupportInterface's in the bulky way.
    *******************************************************************************/
   public static ArrayList<Map<String, Serializable>> qRecordsToApiMapList(List<QRecord> records, String tableName, String apiName, String apiVersion) throws QException
   {
      Map<String, ApiFieldCustomValueMapper> fieldValueMappers = getFieldValueMappers(records, tableName, apiName, apiVersion);

      QRecordApiAdapterToApiInput input = new QRecordApiAdapterToApiInput()
         .withInputRecords(records)
         .withTableName(tableName)
         .withApiName(apiName)
         .withApiVersion(apiVersion);

      ArrayList<Map<String, Serializable>> rs = new ArrayList<>();
      qRecordsToApi(input, fieldValueMappers, () -> new ApiOutputMapWrapper(new LinkedHashMap<>()), rs);
      return (rs);
   }



   /*******************************************************************************
    ** version of the qRecordToApiMap that returns QRecords, not maps.
    ** useful for cases where we're staying inside QQQ, but working with an api-
    ** versioned application.
    *******************************************************************************/
   public static List<QRecord> qRecordsToApiVersionedQRecordList(List<QRecord> records, String tableName, String apiName, String apiVersion) throws QException
   {
      return qRecordsToApiVersionedQRecordList(new QRecordApiAdapterToApiInput()
         .withInputRecords(records)
         .withTableName(tableName)
         .withApiName(apiName)
         .withApiVersion(apiVersion));
   }



   /*******************************************************************************
    ** version of the qRecordToApiMap that returns QRecords, not maps.
    ** useful for cases where we're staying inside QQQ, but working with an api-
    ** versioned application.
    *******************************************************************************/
   public static List<QRecord> qRecordsToApiVersionedQRecordList(QRecordApiAdapterToApiInput input) throws QException
   {
      Map<String, ApiFieldCustomValueMapper> fieldValueMappers = getFieldValueMappers(input.getInputRecords(), input.getTableName(), input.getApiName(), input.getApiVersion());

      List<QRecord> rs = new ArrayList<>();
      qRecordsToApi(input, fieldValueMappers, () -> new ApiOutputQRecordWrapper(new QRecord().withTableName(input.getTableName())), rs);
      return (rs);
   }



   /***************************************************************************
    * prepare a map of ApiFieldCustomValueMapper objects for a given
    * table/apiName/version - which, if those custom value mappers implement the
    * {@link ApiFieldCustomValueMapperBulkSupportInterface}, then the input
    * records will get passed through there too.
    ***************************************************************************/
   private static Map<String, ApiFieldCustomValueMapper> getFieldValueMappers(List<QRecord> records, String tableName, String apiName, String apiVersion) throws QException
   {
      Map<String, ApiFieldCustomValueMapper> fieldValueMappers = new HashMap<>();

      List<QFieldMetaData> tableApiFields = GetTableApiFieldsAction.getTableApiFieldList(new GetTableApiFieldsInput().withApiName(apiName).withVersion(apiVersion).withTableName(tableName));
      for(QFieldMetaData field : tableApiFields)
      {
         ApiFieldMetaData apiFieldMetaData = ObjectUtils.tryAndRequireNonNullElse(() -> ApiFieldMetaDataContainer.of(field).getApiFieldMetaData(apiName), new ApiFieldMetaData());
         if(apiFieldMetaData.getCustomValueMapper() != null)
         {
            if(!fieldValueMappers.containsKey(apiFieldMetaData.getCustomValueMapper().getName()))
            {
               ApiFieldCustomValueMapper customValueMapper = QCodeLoader.getAdHoc(ApiFieldCustomValueMapper.class, apiFieldMetaData.getCustomValueMapper());
               fieldValueMappers.put(apiFieldMetaData.getCustomValueMapper().getName(), customValueMapper);

               if(customValueMapper instanceof ApiFieldCustomValueMapperBulkSupportInterface bulkMapper)
               {
                  bulkMapper.prepareToProduceApiValues(records);
               }
            }
         }
      }
      return fieldValueMappers;
   }



   /*******************************************************************************
    ** private version of convert a QRecord to a map for the API (or, another
    ** QRecord - whatever object is in the `O output` param). Takes params to
    ** support working in bulk w/ customizers much better.
    *
    * @param input main input wrapper for the action
    * @param fieldValueMappers map of field name to ApiFieldCustomValueMapper's
    * which have had the bulk/pre method ran on them if applicable.
    * @param outputCollectionElementSupplier supplier of objects that wrap those that
    * go into the output list (so either wrappers of Maps or QRecords)
    * @param outputList list that holds the output objects.
    * @param <C> the type of objects that go into the outputList
    * @param <O> the type of wrapper object that wraps the objects that go in the
    * output list.  Provides a common interface for Map.put vs. QRecord.setValue, etc.
    *******************************************************************************/
   private static <C, O extends ApiOutputRecordWrapperInterface<C, O>> void qRecordsToApi(QRecordApiAdapterToApiInput input, Map<String, ApiFieldCustomValueMapper> fieldValueMappers, Supplier<O> outputCollectionElementSupplier, List<C> outputList) throws QException
   {
      String apiVersion = input.getApiVersion();
      String apiName    = input.getApiName();
      String tableName  = input.getTableName();

      List<QFieldMetaData> tableApiFields = GetTableApiFieldsAction.getTableApiFieldList(new GetTableApiFieldsInput().withApiName(apiName).withVersion(apiVersion).withTableName(tableName));
      if(fieldValueMappers == null)
      {
         fieldValueMappers = new HashMap<>();
      }

      Map<String, List<QFieldMetaData>> exposedJoinApiFields = new HashMap<>();
      if(input.getIncludeExposedJoins())
      {
         QTableMetaData table = QContext.getQInstance().getTable(tableName);
         for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
         {
            String joinTableName = exposedJoin.getJoinTable();
            exposedJoinApiFields.put(joinTableName, GetTableApiFieldsAction.getTableApiFieldList(new GetTableApiFieldsInput().withApiName(apiName).withVersion(apiVersion).withTableName(joinTableName)));
         }
      }

      for(QRecord inputRecord : input.getInputRecords())
      {
         if(inputRecord == null)
         {
            outputList.add(null);
            continue;
         }

         O output = outputCollectionElementSupplier.get();

         /////////////////////////////////////////
         // iterate over the table's api fields //
         /////////////////////////////////////////
         for(QFieldMetaData field : tableApiFields)
         {
            processFieldIntoApiObject(fieldValueMappers, inputRecord, field, apiName, null, output);
         }

         ///////////////////////////////////////////
         // process exposed joins if we have them //
         ///////////////////////////////////////////
         for(Map.Entry<String, List<QFieldMetaData>> entry : exposedJoinApiFields.entrySet())
         {
            String joinTableName = entry.getKey();
            List<QFieldMetaData> joinFields = entry.getValue();

            boolean recordHasAnyFieldsFromThisJoin = false;
            for(String fieldName : inputRecord.getValues().keySet())
            {
               if(fieldName.startsWith(joinTableName + "."))
               {
                  recordHasAnyFieldsFromThisJoin = true;
                  break;
               }
            }

            if(recordHasAnyFieldsFromThisJoin)
            {
               for(QFieldMetaData joinField : joinFields)
               {
                  processFieldIntoApiObject(fieldValueMappers, inputRecord, joinField, apiName, joinTableName, output);
               }
            }
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////
         // todo - should probably define in meta-data if an association is included in the api or not!! //
         //  and what its name is too...                                                                 //
         //////////////////////////////////////////////////////////////////////////////////////////////////
         QTableMetaData table = QContext.getQInstance().getTable(tableName);
         for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
         {
            if(isAssociationOmitted(apiName, apiVersion, table, association))
            {
               continue;
            }

            List<QRecord> associatedInputRecords = CollectionUtils.nonNullList(CollectionUtils.nonNullMap(inputRecord.getAssociatedRecords()).get(association.getName()));

            QRecordApiAdapterToApiInput associationInput = new QRecordApiAdapterToApiInput()
               .withInputRecords(associatedInputRecords)
               .withTableName(association.getAssociatedTableName())
               .withApiName(apiName)
               .withApiVersion(apiVersion);

            //////////////////////////////////////////////////////////////////////////////////////////
            // note that we are missing out on the running of join-field custom field value mappers //
            // through the ApiFieldCustomValueMapperBulkSupportInterface.prepareToProduceApiValues  //
            // method, if they support it...                                                        //
            //////////////////////////////////////////////////////////////////////////////////////////
            List<C> associationOutputContentsList = new ArrayList<>();
            qRecordsToApi(associationInput, fieldValueMappers, outputCollectionElementSupplier, associationOutputContentsList);
            output.putAssociation(association.getName(), associationOutputContentsList);
         }

         outputList.add(output.getContents());
      }
   }



   /***************************************************************************
    * as part of qRecordsToApi, process a single field for a single record.
    * @param fieldValueMappers any ApiFieldCustomValueMapper objects needed for
    * @param inputRecord input record with values being put into an API object
    * @param field the field being processed
    * @param apiName name of the api this is for
    * @param joinTableName if this field is from a join table, its name - else null.
    * @param output the output object wrapper (map or other QRecord).
    ***************************************************************************/
   private static <C, O extends ApiOutputRecordWrapperInterface<C, O>> void processFieldIntoApiObject(Map<String, ApiFieldCustomValueMapper> fieldValueMappers, QRecord inputRecord, QFieldMetaData field, String apiName, String joinTableName, O output)
   {
      ApiFieldMetaData apiFieldMetaData = ObjectUtils.tryAndRequireNonNullElse(() -> ApiFieldMetaDataContainer.of(field).getApiFieldMetaData(apiName), new ApiFieldMetaData());
      String           apiFieldName     = ApiFieldMetaData.getEffectiveApiFieldName(apiName, field);

      /////////////////////////////////////////////////////////////////////////
      // if there's a join table, then it's table.fieldName, else no prefix. //
      /////////////////////////////////////////////////////////////////////////
      String joinTableNamePrefix = joinTableName == null ? "" : joinTableName + ".";

      Serializable value;
      if(StringUtils.hasContent(apiFieldMetaData.getReplacedByFieldName()))
      {
         value = inputRecord.getValue(joinTableNamePrefix + apiFieldMetaData.getReplacedByFieldName());
      }
      else if(apiFieldMetaData.getCustomValueMapper() != null)
      {
         String customValueMapperName = apiFieldMetaData.getCustomValueMapper().getName();
         if(!fieldValueMappers.containsKey(customValueMapperName))
         {
            fieldValueMappers.put(customValueMapperName, QCodeLoader.getAdHoc(ApiFieldCustomValueMapper.class, apiFieldMetaData.getCustomValueMapper()));
         }

         ApiFieldCustomValueMapper customValueMapper = fieldValueMappers.get(customValueMapperName);
         value = customValueMapper.produceApiValue(inputRecord, joinTableNamePrefix + apiFieldName);
      }
      else
      {
         value = inputRecord.getValue(joinTableNamePrefix + field.getName());
      }

      if(field.getType().equals(QFieldType.BLOB) && value instanceof byte[] bytes)
      {
         value = Base64.getEncoder().encodeToString(bytes);
      }

      output.putValue(joinTableNamePrefix + apiFieldName, value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isAssociationOmitted(String apiName, String apiVersion, QTableMetaData table, Association association)
   {
      ApiTableMetaData       thisApiTableMetaData   = ObjectUtils.tryAndRequireNonNullElse(() -> ApiTableMetaDataContainer.of(table).getApiTableMetaData(apiName), new ApiTableMetaData());
      ApiAssociationMetaData apiAssociationMetaData = thisApiTableMetaData.getApiAssociationMetaData().get(association.getName());
      if(apiAssociationMetaData != null)
      {
         if(BooleanUtils.isTrue(apiAssociationMetaData.getIsExcluded()))
         {
            return (true);
         }

         APIVersionRange apiVersionRange = apiAssociationMetaData.getApiVersionRange();
         if(!apiVersionRange.includes(new APIVersion(apiVersion)))
         {
            return true;
         }
      }
      return false;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QRecord apiJsonObjectToQRecord(JSONObject jsonObject, String tableName, String apiName, String apiVersion, boolean includeNonEditableFields) throws QException
   {
      return apiJsonObjectToQRecord(jsonObject, tableName, apiName, apiVersion, includeNonEditableFields, QInputSource.SYSTEM);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QRecord apiJsonObjectToQRecord(JSONObject jsonObject, String tableName, String apiName, String apiVersion, boolean includeNonEditableFields, InputSource inputSource) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////
      // make map of apiFieldNames (e.g., names as api uses them) to QFieldMetaData //
      ////////////////////////////////////////////////////////////////////////////////
      Map<String, QFieldMetaData> apiFieldsMap           = GetTableApiFieldsAction.getTableApiFieldMap(new GetTableApiFieldsInput().withApiName(apiName).withVersion(apiVersion).withTableName(tableName).withInputSource(inputSource));
      List<String>                unrecognizedFieldNames = new ArrayList<>();
      QRecord                     qRecord                = new QRecord();

      Map<String, Association> associationMap = new HashMap<>();
      QTableMetaData           table          = QContext.getQInstance().getTable(tableName);
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         if(!isAssociationOmitted(apiName, apiVersion, table, association))
         {
            associationMap.put(association.getName(), association);
         }
      }

      //////////////////////////////////////////
      // iterate over keys in the json object //
      //////////////////////////////////////////
      for(String jsonKey : jsonObject.keySet())
      {
         ////////////////////////////////////////////////
         // if it's a valid api field name, process it //
         ////////////////////////////////////////////////
         if(apiFieldsMap.containsKey(jsonKey))
         {
            setValueFromApiFieldInQRecord(jsonObject, jsonKey, apiName, apiFieldsMap, qRecord, includeNonEditableFields);
         }
         else if(associationMap.containsKey(jsonKey))
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////
            // else, if it's an association - process that (recursively as a list of other records)         //
            // todo - should probably define in meta-data if an association is included in the api or not!! //
            // and what its name is too...                                                                  //
            //////////////////////////////////////////////////////////////////////////////////////////////////
            Association association = associationMap.get(jsonKey);
            Object      value       = jsonObject.get(jsonKey);
            if(value instanceof JSONArray jsonArray)
            {
               for(Object subObject : jsonArray)
               {
                  if(subObject instanceof JSONObject subJsonObject)
                  {
                     ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     // make sure to always include primary keys (boolean param) on calls for children - to help determine insert/update cases //
                     ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     QRecord subRecord = apiJsonObjectToQRecord(subJsonObject, association.getAssociatedTableName(), apiName, apiVersion, true);
                     qRecord.withAssociatedRecord(association.getName(), subRecord);
                  }
                  else
                  {
                     throw (new QBadRequestException("Found a " + value.getClass().getSimpleName() + " in the array under key " + jsonKey + ", but a JSON object is required here."));
                  }
               }
            }
            else
            {
               throw (new QBadRequestException("Found a " + value.getClass().getSimpleName() + " at key " + jsonKey + ", but a JSON array is required here."));
            }
         }
         else
         {
            ///////////////////////////////////////////////////
            // else add it to the list of unrecognized names //
            ///////////////////////////////////////////////////
            unrecognizedFieldNames.add(jsonKey);
         }
      }

      if(!unrecognizedFieldNames.isEmpty())
      {
         List<String> otherVersionHints = new ArrayList<>();
         try
         {
            for(String unrecognizedFieldName : unrecognizedFieldNames)
            {
               String hint = lookForFieldInOtherVersions(unrecognizedFieldName, tableName, apiName, apiVersion);
               if(hint != null)
               {
                  otherVersionHints.add(hint);
               }
            }
         }
         catch(Exception e)
         {
            LOG.warn("Error looking for unrecognized field names in other api versions", e);
         }

         throw (new QBadRequestException("Request body contained "
            + (unrecognizedFieldNames.size() + " unrecognized field name" + StringUtils.plural(unrecognizedFieldNames) + ": " + StringUtils.joinWithCommasAndAnd(unrecognizedFieldNames) + ". ")
            + (CollectionUtils.nullSafeIsEmpty(otherVersionHints) ? "" : StringUtils.join(" ", otherVersionHints))
         ));
      }

      return (qRecord);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static void setValueFromApiFieldInQRecord(JSONObject apiObject, String apiFieldName, String apiName, Map<String, QFieldMetaData> apiFieldsMap, QRecord qRecord, boolean includeNonEditableFields) throws QException
   {
      QFieldMetaData field = apiFieldsMap.get(apiFieldName);
      if(field == null)
      {
         throw (new QException("Unrecognized apiFieldName: " + apiFieldName));
      }

      /////////////////////////////////////////////////////////////////////////////////////
      // generally, omit non-editable fields, unless the param to include them is given. //
      /////////////////////////////////////////////////////////////////////////////////////
      if(!field.getIsEditable())
      {
         if(includeNonEditableFields)
         {
            LOG.trace("Even though field [" + field.getName() + "] is not editable, we'll use it, because we've been asked to include non-editable fields");
         }
         else
         {
            return;
         }
      }

      ///////////////////////////////////////
      // get the value from the api object //
      ///////////////////////////////////////
      Object value = apiObject.isNull(apiFieldName) ? null : apiObject.get(apiFieldName);
      if(field.getType().equals(QFieldType.BLOB) && value instanceof String s)
      {
         value = Base64.getDecoder().decode(s);
      }

      ApiFieldMetaData apiFieldMetaData = ObjectUtils.tryAndRequireNonNullElse(() -> ApiFieldMetaDataContainer.of(field).getApiFieldMetaData(apiName), new ApiFieldMetaData());
      if(StringUtils.hasContent(apiFieldMetaData.getReplacedByFieldName()))
      {
         ///////////////////////////////////////////////////////////////
         // if field was replaced, then set OLD field name in QRecord //
         ///////////////////////////////////////////////////////////////
         qRecord.setValue(apiFieldMetaData.getReplacedByFieldName(), value);
      }
      else if(apiFieldMetaData.getCustomValueMapper() != null)
      {
         ///////////////////////////////////////////////////////////
         // if a custom value mapper is to be used, then do so... //
         ///////////////////////////////////////////////////////////
         ApiFieldCustomValueMapper customValueMapper = QCodeLoader.getAdHoc(ApiFieldCustomValueMapper.class, apiFieldMetaData.getCustomValueMapper());
         customValueMapper.consumeApiValue(qRecord, value, apiObject, apiFieldName);
      }
      else
      {
         //////////////////////////////////////////////////////
         // else, default case, set the value in the qrecord //
         //////////////////////////////////////////////////////
         qRecord.setValue(field.getName(), value);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String lookForFieldInOtherVersions(String unrecognizedFieldName, String tableName, String apiName, String apiVersion) throws QException
   {
      ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = ApiInstanceMetaDataContainer.of(QContext.getQInstance());
      ApiInstanceMetaData          apiInstanceMetaData          = apiInstanceMetaDataContainer.getApiInstanceMetaData(apiName);

      List<String> versionsWithThisField = new ArrayList<>();
      for(APIVersion supportedVersion : apiInstanceMetaData.getSupportedVersions())
      {
         Map<String, QFieldMetaData> versionFields = GetTableApiFieldsAction.getTableApiFieldMap(new GetTableApiFieldsInput().withApiName(apiName).withVersion(supportedVersion.toString()).withTableName(tableName));
         if(versionFields.containsKey(unrecognizedFieldName))
         {
            versionsWithThisField.add(supportedVersion.toString());
         }
      }

      if(versionsWithThisField.contains(apiVersion))
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // handle case where the field /is/ in this version, but not for this user (e.g., based on a table //
         // personalizer). it seems like maybe we should cover that, to avoid saying "not in version X, but //
         // in version: X...                                                                                //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         return (unrecognizedFieldName + " is not allowed for the current user");
      }

      if(CollectionUtils.nullSafeHasContents(versionsWithThisField))
      {
         return (unrecognizedFieldName + " does not exist in version " + apiVersion + ", but does exist in versions: " + StringUtils.joinWithCommasAndAnd(versionsWithThisField) + ". ");
      }

      return (null);
   }
}
