/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecInput;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecOutput;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Virtual fields with explicit api field meta data are output-only api
 ** fields: in record output and response schemas (readOnly), never in the
 ** field list used for input, filtering and sorting.
 *******************************************************************************/
class ApiVirtualFieldsTest extends BaseTest
{
   private static final String TABLE_NAME = "page";



   /*******************************************************************************
    ** A table with one stored field and three virtual fields: one exposed (and
    ** renamed), one without api meta data, one excluded.
    *******************************************************************************/
   @BeforeEach
   void defineTable() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME)
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("slug", QFieldType.STRING))
         .withVirtualFields(List.of(
            new QVirtualFieldMetaData("sitePath", QFieldType.STRING).withLabel("Site Path")
               .withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME,
                  new ApiFieldMetaData().withInitialVersion(TestUtils.V2022_Q4).withApiFieldName("path"))),
            new QVirtualFieldMetaData("internalScore", QFieldType.INTEGER),
            new QVirtualFieldMetaData("excluded", QFieldType.STRING)
               .withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME,
                  new ApiFieldMetaData().withInitialVersion(TestUtils.V2022_Q4).withIsExcluded(true)))))
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData()
            .withInitialVersion(TestUtils.V2022_Q4))));
      new QInstanceEnricher(qInstance).enrich();
      GetTableApiFieldsAction.clearCaches();
   }



   /*******************************************************************************
    ** Listed only when asked for, and only with explicit, non-excluded meta data.
    *******************************************************************************/
   @Test
   void virtualFieldsAreListedOnlyOnRequest() throws QException
   {
      GetTableApiFieldsInput input = new GetTableApiFieldsInput().withApiName(TestUtils.API_NAME).withTableName(TABLE_NAME)
         .withVersion(TestUtils.CURRENT_API_VERSION);
      assertThat(GetTableApiFieldsAction.getTableApiFieldMap(input)).containsOnlyKeys("id", "slug");

      Map<String, QFieldMetaData> withVirtual = GetTableApiFieldsAction.getTableApiFieldMap(new GetTableApiFieldsInput().withApiName(TestUtils.API_NAME)
         .withTableName(TABLE_NAME).withVersion(TestUtils.CURRENT_API_VERSION).withIncludeVirtualFields(true));
      assertThat(withVirtual).containsOnlyKeys("id", "slug", "path");
      assertThat(withVirtual.get("path")).isInstanceOf(QVirtualFieldMetaData.class);

      //////////////////////////////////////////////////////////
      // the memoized lists of both shapes stay independent //
      //////////////////////////////////////////////////////////
      assertThat(GetTableApiFieldsAction.getTableApiFieldList(input)).extracting(QFieldMetaData::getName).containsOnly("id", "slug");
   }



   /*******************************************************************************
    ** Record output carries the value the record holds (e.g. from a post-query
    ** customizer) under the api field name.
    *******************************************************************************/
   @Test
   void recordOutputIncludesVirtualValues() throws QException
   {
      QRecord record = new QRecord().withValue("id", 1).withValue("slug", "about").withValue("sitePath", "/about").withValue("internalScore", 7);
      Map<String, Serializable> apiRecord = QRecordApiAdapter.qRecordToApiMap(record, TABLE_NAME, TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION);
      assertEquals("/about", apiRecord.get("path"));
      assertThat(apiRecord).doesNotContainKeys("sitePath", "internalScore", "excluded");

      apiRecord = QRecordApiAdapter.qRecordToApiMap(new QRecord().withValue("id", 2), TABLE_NAME, TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION);
      assertTrue(apiRecord.containsKey("path"));
      assertNull(apiRecord.get("path"));
   }



   /*******************************************************************************
    ** The spec lists the field as a readOnly property, but not as a query filter.
    *******************************************************************************/
   @Test
   void specMarksVirtualFieldsReadOnlyAndNotFilterable() throws QException
   {
      GenerateOpenApiSpecOutput output = new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput()
         .withVersion(TestUtils.CURRENT_API_VERSION).withApiName(TestUtils.API_NAME));

      Map<String, Schema> properties = output.getOpenAPI().getComponents().getSchemas().get(TABLE_NAME).getProperties();
      assertThat(properties).containsKeys("id", "slug", "path").doesNotContainKeys("sitePath", "internalScore", "excluded");
      assertEquals(Boolean.TRUE, properties.get("path").getReadOnly());
      assertNull(properties.get("slug").getReadOnly());

      String queryPath = output.getOpenAPI().getPaths().keySet().stream().filter(p -> p.endsWith("/" + TABLE_NAME + "/query")).findFirst().orElseThrow();
      List<String> parameters = output.getOpenAPI().getPaths().get(queryPath).getGet().getParameters().stream().map(Parameter::getName).toList();
      assertThat(parameters).contains("slug").doesNotContain("path");
   }
}
