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

package com.kingsrook.qqq.api.actions;


import java.util.Set;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecInput;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecOutput;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for GenerateOpenApiSpecAction
 *******************************************************************************/
class GenerateOpenApiSpecActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      for(ApiInstanceMetaData apiInstanceMetaData : ApiInstanceMetaDataContainer.of(QContext.getQInstance()).getApis().values())
      {
         for(APIVersion supportedVersion : apiInstanceMetaData.getSupportedVersions())
         {
            //////////////////////////////////////////////////////////////////////
            // just making sure we don't throw on any apis in the test instance //
            //////////////////////////////////////////////////////////////////////
            GenerateOpenApiSpecOutput output = new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput()
               .withVersion(supportedVersion.toString())
               .withApiName(apiInstanceMetaData.getName()));
            // System.out.println(output.getYaml());
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSingleTable() throws QException
   {
      for(ApiInstanceMetaData apiInstanceMetaData : ApiInstanceMetaDataContainer.of(QContext.getQInstance()).getApis().values())
      {
         for(APIVersion supportedVersion : apiInstanceMetaData.getSupportedVersions())
         {
            for(QTableMetaData table : QContext.getQInstance().getTables().values())
            {
               //////////////////////////////////////////////////////////////////////
               // just making sure we don't throw on any apis in the test instance //
               //////////////////////////////////////////////////////////////////////
               GenerateOpenApiSpecOutput output = new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput()
                  .withTableName(table.getName())
                  .withVersion(supportedVersion.toString())
                  .withApiName(apiInstanceMetaData.getName()));

               if(table.getName().equals(TestUtils.TABLE_NAME_PERSON))
               {
                  assertThat(output.getYaml())
                     .contains("Query on the First Name field.  Can prefix value with an operator")
                     .contains("Query on the Photo field.  Can only query for EMPTY or !EMPTY");
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExcludedTables() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      qInstance.addTable(new QTableMetaData()
         .withName("supportedTable")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData()
            .withInitialVersion(TestUtils.V2022_Q4))));

      qInstance.addTable(new QTableMetaData()
         .withName("hiddenTable")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withIsHidden(true)
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData()
            .withInitialVersion(TestUtils.V2022_Q4))));

      qInstance.addTable(new QTableMetaData()
         .withName("excludedTable")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData()
            .withIsExcluded(true))));

      qInstance.addTable(new QTableMetaData()
         .withName("tableWithoutApiMetaData")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)));

      qInstance.addTable(new QTableMetaData()
         .withName("tableWithFutureVersion")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData()
            .withInitialVersion(TestUtils.V2023_Q2))));

      qInstance.addTable(new QTableMetaData()
         .withName("tableWithOnlyPastVersions")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData()
            .withInitialVersion(TestUtils.V2022_Q4)
            .withFinalVersion(TestUtils.V2022_Q4))));

      qInstance.addTable(new QTableMetaData()
         .withName("tableWithNoSupportedCapabilities")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withoutCapabilities(Capability.TABLE_QUERY, Capability.TABLE_GET, Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE)
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData()
            .withInitialVersion(TestUtils.V2022_Q4))));

      new QInstanceEnricher(qInstance).enrich();

      GenerateOpenApiSpecOutput output   = new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withVersion(TestUtils.CURRENT_API_VERSION).withApiName(TestUtils.API_NAME));
      Set<String>               apiPaths = output.getOpenAPI().getPaths().keySet();
      assertTrue(apiPaths.stream().anyMatch(s -> s.contains("/supportedTable/")));
      assertTrue(apiPaths.stream().noneMatch(s -> s.contains("/hiddenTable/")));
      assertTrue(apiPaths.stream().noneMatch(s -> s.contains("/excludedTable/")));
      assertTrue(apiPaths.stream().noneMatch(s -> s.contains("/tableWithoutApiMetaData/")));
      assertTrue(apiPaths.stream().noneMatch(s -> s.contains("/tableWithoutApiMetaData/")));
      assertTrue(apiPaths.stream().noneMatch(s -> s.contains("/tableWithFutureVersion/")));
      assertTrue(apiPaths.stream().noneMatch(s -> s.contains("/tableWithNoSupportedCapabilities/")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testApiTableName() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      qInstance.addTable(new QTableMetaData()
         .withName("internalName")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData()
            .withApiTableName("externalName")
            .withInitialVersion(TestUtils.V2022_Q4))));
      
      new QInstanceEnricher(qInstance).enrich();

      GenerateOpenApiSpecOutput output   = new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withVersion(TestUtils.CURRENT_API_VERSION).withApiName(TestUtils.API_NAME));
      Set<String>               apiPaths = output.getOpenAPI().getPaths().keySet();
      assertTrue(apiPaths.stream().anyMatch(s -> s.contains("/externalName/")));
      assertTrue(apiPaths.stream().noneMatch(s -> s.contains("/internalName/")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadVersion()
   {
      assertThatThrownBy(() -> new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withApiName(TestUtils.API_NAME)))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Missing required input: version");

      assertThatThrownBy(() -> new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withVersion("NotAVersion").withApiName(TestUtils.API_NAME)))
         .isInstanceOf(QException.class)
         .hasMessageContaining("not a supported API Version");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadApiName()
   {
      assertThatThrownBy(() -> new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withApiName("Not an api")))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Could not find apiInstanceMetaData named");

      assertThatThrownBy(() -> new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withVersion(TestUtils.CURRENT_API_VERSION)))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Missing required input: apiName");
   }

}