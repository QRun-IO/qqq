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

package com.kingsrook.qqq.backend.module.api.model;


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 **
 *******************************************************************************/
public class OutboundAPILogMetaDataProvider
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public static void defineAll(QInstance qInstance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      definePossibleValueSources(qInstance);
      defineOutboundAPILogTable(qInstance, backendName, backendDetailEnricher);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void definePossibleValueSources(QInstance instance)
   {
      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName("outboundApiMethod")
         .withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(
            new QPossibleValue<>("GET"),
            new QPossibleValue<>("POST"),
            new QPossibleValue<>("PUT"),
            new QPossibleValue<>("PATCH"),
            new QPossibleValue<>("DELETE")
         )));

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName("outboundApiStatusCode")
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
            new QPossibleValue<>(422, "422 (Unprocessable Entity)"),
            new QPossibleValue<>(429, "429 (Too Many Requests)"),
            new QPossibleValue<>(500, "500 (Internal Server Error)"),
            new QPossibleValue<>(502, "502 (Bad Gateway)"),
            new QPossibleValue<>(503, "503 (Service Unavailable)"),
            new QPossibleValue<>(504, "500 (Gateway Timeout)")
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineOutboundAPILogTable(QInstance qInstance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {

      QTableMetaData tableMetaData = new QTableMetaData()
         .withName(OutboundAPILog.TABLE_NAME)
         .withLabel("Outbound API Log")
         .withIcon(new QIcon().withName("data_object"))
         .withBackendName(backendName)
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("id")
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(OutboundAPILog.class)
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id")))
         .withSection(new QFieldSection("request", new QIcon().withName("arrow_upward"), Tier.T2, List.of("method", "url", "requestBody")))
         .withSection(new QFieldSection("response", new QIcon().withName("arrow_downward"), Tier.T2, List.of("statusCode", "responseBody")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("timestamp")))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);

      tableMetaData.getField("requestBody").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("json")));
      tableMetaData.getField("responseBody").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("json")));

      tableMetaData.getField("method").withFieldAdornment(new FieldAdornment(AdornmentType.CHIP)
         .withValue(AdornmentType.ChipValues.colorValue("GET", AdornmentType.ChipValues.COLOR_INFO))
         .withValue(AdornmentType.ChipValues.colorValue("POST", AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValue(AdornmentType.ChipValues.colorValue("DELETE", AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue("PATCH", AdornmentType.ChipValues.COLOR_WARNING))
         .withValue(AdornmentType.ChipValues.colorValue("PUT", AdornmentType.ChipValues.COLOR_WARNING)));

      tableMetaData.getField("statusCode").withFieldAdornment(new FieldAdornment(AdornmentType.CHIP)
         .withValue(AdornmentType.ChipValues.colorValue(200, AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValue(AdornmentType.ChipValues.colorValue(201, AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValue(AdornmentType.ChipValues.colorValue(204, AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValue(AdornmentType.ChipValues.colorValue(207, AdornmentType.ChipValues.COLOR_INFO))
         .withValue(AdornmentType.ChipValues.colorValue(400, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(401, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(403, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(404, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(422, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(429, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(500, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(502, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(503, AdornmentType.ChipValues.COLOR_ERROR))
         .withValue(AdornmentType.ChipValues.colorValue(504, AdornmentType.ChipValues.COLOR_ERROR))
      );

      ///////////////////////////////////////////
      // these are the lengths of a MySQL TEXT //
      ///////////////////////////////////////////
      tableMetaData.getField("requestBody").withMaxLength(16_777_215).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);
      tableMetaData.getField("responseBody").withMaxLength(16_777_215).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);

      /////////////////////////
      // limit url to 250... //
      /////////////////////////
      tableMetaData.getField("url").withMaxLength(4096).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);
      tableMetaData.getField("url").withFieldAdornment(AdornmentType.Size.XLARGE.toAdornment());

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(tableMetaData);
      }

      qInstance.addTable(tableMetaData);
   }
}
