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

package com.kingsrook.qqq.api.model.metadata;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import com.kingsrook.qqq.api.model.APILog;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiInstanceMetaDataProvider
{
   public static final String TABLE_NAME_API_LOG      = "apiLog";
   public static final String TABLE_NAME_API_LOG_USER = "apiLogUser";

   public static final String API_NAME_PVS_NAME    = "apiName";
   public static final String API_VERSION_PVS_NAME = "apiVersion";



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void defineAll(QInstance qInstance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      definePossibleValueSourcesUsedByApiLogTable(qInstance);
      definePossibleValueSourcesForApiNameAndVersion(qInstance);
      defineAPILogTable(qInstance, backendName, backendDetailEnricher);
      defineAPILogUserTable(qInstance, backendName, backendDetailEnricher);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void definePossibleValueSourcesUsedByApiLogTable(QInstance instance)
   {
      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(TABLE_NAME_API_LOG_USER)
         .withTableName(TABLE_NAME_API_LOG_USER)
         .withOrderByField("name"));

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName("apiMethod")
         .withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(
            new QPossibleValue<>("GET"),
            new QPossibleValue<>("POST"),
            new QPossibleValue<>("PATCH"),
            new QPossibleValue<>("DELETE")
         )));

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName("apiStatusCode")
         .withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(
            new QPossibleValue<>(200, "200 (OK)"),
            new QPossibleValue<>(201, "201 (Created)"),
            new QPossibleValue<>(204, "204 (No Content)"),
            new QPossibleValue<>(207, "207 (Multi-Status)"),
            new QPossibleValue<>(400, "400 (Bad Request)"),
            new QPossibleValue<>(401, "401 (Not Authorized)"),
            new QPossibleValue<>(403, "403 (Forbidden)"),
            new QPossibleValue<>(404, "404 (Not Found)"),
            new QPossibleValue<>(429, "429 (Too Many Requests)"),
            new QPossibleValue<>(500, "500 (Internal Server Error)")
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void definePossibleValueSourcesForApiNameAndVersion(QInstance instance)
   {
      ////////////////////////////////////////////////////////////////////////////
      // loop over api names and versions, building out possible values sources //
      ////////////////////////////////////////////////////////////////////////////
      List<QPossibleValue<?>> apiNamePossibleValues    = new ArrayList<>();
      List<QPossibleValue<?>> apiVersionPossibleValues = new ArrayList<>();

      //////////////////////////////////////////////////////////////////
      // todo... apiName should maybe be a field on apiLog table, eh? //
      //////////////////////////////////////////////////////////////////
      TreeSet<APIVersion>          allVersions                  = new TreeSet<>();
      ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = ApiInstanceMetaDataContainer.of(instance);
      for(Map.Entry<String, ApiInstanceMetaData> entry : apiInstanceMetaDataContainer.getApis().entrySet())
      {
         apiNamePossibleValues.add(new QPossibleValue<>(entry.getKey(), entry.getValue().getLabel()));

         ApiInstanceMetaData apiInstanceMetaData = entry.getValue();
         allVersions.addAll(CollectionUtils.nonNullCollection(apiInstanceMetaData.getPastVersions()));
         allVersions.addAll(CollectionUtils.nonNullCollection(apiInstanceMetaData.getSupportedVersions()));

         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         // I think we don't want future-versions in this dropdown, I think...                                //
         // grr, actually todo maybe we want this to be a table-backed enum, with past/present/future columns //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         allVersions.addAll(CollectionUtils.nonNullCollection(apiInstanceMetaData.getFutureVersions()));
      }

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(API_NAME_PVS_NAME)
         .withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(apiNamePossibleValues));

      for(APIVersion version : allVersions)
      {
         apiVersionPossibleValues.add(new QPossibleValue<>(version.toString()));
      }

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(API_VERSION_PVS_NAME)
         .withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(apiVersionPossibleValues));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineAPILogUserTable(QInstance qInstance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData tableMetaData = new QTableMetaData()
         .withName(TABLE_NAME_API_LOG_USER)
         .withLabel("API Log User")
         .withIcon(new QIcon().withName("person"))
         .withBackendName(backendName)
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("name")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("name"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true))
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "name")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(tableMetaData);
      }

      qInstance.addTable(tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineAPILogTable(QInstance qInstance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData tableMetaData = new QTableMetaData()
         .withName(TABLE_NAME_API_LOG)
         .withLabel("API Log")
         .withIcon(new QIcon().withName("data_object"))
         .withBackendName(backendName)
         .withRecordLabelFormat("%s")
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(APILog.class)
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "apiLogUserId")))
         .withSection(new QFieldSection("request", new QIcon().withName("arrow_upward"), Tier.T2, List.of("method", "version", "path", "queryString", "requestBody")))
         .withSection(new QFieldSection("response", new QIcon().withName("arrow_downward"), Tier.T2, List.of("statusCode", "responseBody")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("timestamp")))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);

      tableMetaData.getField("requestBody").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("json")));
      tableMetaData.getField("responseBody").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("json")));

      tableMetaData.getField("method").withFieldAdornment(new FieldAdornment(AdornmentType.CHIP)
         .withValue(AdornmentType.ChipValues.colorValue("GET", AdornmentType.ChipValues.COLOR_INFO))
         .withValue(AdornmentType.ChipValues.colorValue("POST", AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValue(AdornmentType.ChipValues.colorValue("DELETE", AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue("PATCH", AdornmentType.ChipValues.COLOR_WARNING)));

      tableMetaData.getField("statusCode").withFieldAdornment(new FieldAdornment(AdornmentType.CHIP)
         .withValue(AdornmentType.ChipValues.colorValue(200, AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValue(AdornmentType.ChipValues.colorValue(201, AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValue(AdornmentType.ChipValues.colorValue(204, AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValue(AdornmentType.ChipValues.colorValue(207, AdornmentType.ChipValues.COLOR_INFO))
         .withValue(AdornmentType.ChipValues.colorValue(400, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(401, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(403, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(404, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(429, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(500, AdornmentType.ChipValues.COLOR_ERROR)));

      ///////////////////////////////////////////
      // these are the lengths of a MySQL TEXT //
      ///////////////////////////////////////////
      tableMetaData.getField("requestBody").withMaxLength(16_777_215).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);
      tableMetaData.getField("responseBody").withMaxLength(16_777_215).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);

      //////////////////////////////////////////////////////////////////////////////////////////////
      // internet doesn't agree on max-length for a URL, but let's go with ... 4K on query string //
      //////////////////////////////////////////////////////////////////////////////////////////////
      tableMetaData.getField("queryString").withMaxLength(4096).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);

      ////////////////////////////////////////
      // and we expect short paths, 100 max //
      ////////////////////////////////////////
      tableMetaData.getField("path").withMaxLength(100).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(tableMetaData);
      }

      qInstance.addTable(tableMetaData);
   }
}
