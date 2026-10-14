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

package com.kingsrook.qqq.api.javalin;


import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.specs.v1.TableApisProviderInterface;
import com.kingsrook.qqq.middleware.javalin.specs.v1.TableApisProviderInterface.TableApi;
import com.kingsrook.qqq.middleware.javalin.specs.v1.TableApisProviderInterface.TableApiVersion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ModelApiTableApisProvider
 *******************************************************************************/
class ModelApiTableApisProviderTest extends BaseTest
{

   /*******************************************************************************
    ** person is in both test APIs; order only in the main one.
    *******************************************************************************/
   @Test
   void testListsApisThatExposeTheTable() throws Exception
   {
      QInstance qInstance = TestUtils.defineInstance();

      List<TableApi> personApis = new ModelApiTableApisProvider().getTableApis(qInstance, TestUtils.TABLE_NAME_PERSON);
      assertEquals(List.of(TestUtils.ALTERNATIVE_API_NAME, TestUtils.API_NAME), personApis.stream().map(TableApi::name).sorted().toList());

      TableApi main = personApis.stream().filter(api -> api.name().equals(TestUtils.API_NAME)).findFirst().orElseThrow();
      assertEquals("Test API", main.label());
      assertEquals(TestUtils.API_PATH, main.path());
      assertEquals(TestUtils.CURRENT_API_VERSION, main.currentVersion());
      assertEquals(List.of(TestUtils.V2022_Q4, TestUtils.V2023_Q1), main.versions().stream().map(TableApiVersion::version).toList());
      assertEquals("/api/2023.Q1/person/openapi.json", main.versions().get(1).tableSpecPath());
      assertNull(main.versions().get(1).semanticVersion());

      List<TableApi> orderApis = new ModelApiTableApisProvider().getTableApis(qInstance, TestUtils.TABLE_NAME_ORDER);
      assertEquals(List.of(TestUtils.API_NAME), orderApis.stream().map(TableApi::name).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSemanticVersionsExcludedAndUnknown() throws Exception
   {
      QInstance qInstance = TestUtils.defineInstance();
      ApiInstanceMetaDataContainer.of(qInstance).getApiInstanceMetaData(TestUtils.API_NAME).setSemanticVersions(Map.of(TestUtils.V2023_Q1, "3.1.0", TestUtils.V2022_Q4, "3.0.0"));
      ApiTableMetaDataContainer.of(qInstance.getTable(TestUtils.TABLE_NAME_ORDER)).withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withIsExcluded(true));

      TableApi main = new ModelApiTableApisProvider().getTableApis(qInstance, TestUtils.TABLE_NAME_PERSON).stream()
         .filter(api -> api.name().equals(TestUtils.API_NAME)).findFirst().orElseThrow();
      assertEquals("3.1.0", main.versions().get(1).semanticVersion());

      assertTrue(new ModelApiTableApisProvider().getTableApis(qInstance, TestUtils.TABLE_NAME_ORDER).isEmpty());
      assertTrue(new ModelApiTableApisProvider().getTableApis(qInstance, "notATable").isEmpty());
      assertTrue(new ModelApiTableApisProvider().getTableApis(new QInstance(), TestUtils.TABLE_NAME_PERSON).isEmpty());
   }



   /*******************************************************************************
    ** the UI API finds this provider through META-INF/services.
    *******************************************************************************/
   @Test
   void testRegisteredAsService()
   {
      assertTrue(ServiceLoader.load(TableApisProviderInterface.class).stream().anyMatch(provider -> provider.type().equals(ModelApiTableApisProvider.class)));
   }

}
