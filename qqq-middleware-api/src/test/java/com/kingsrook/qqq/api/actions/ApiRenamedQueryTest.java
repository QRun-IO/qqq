/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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
import com.fasterxml.jackson.databind.JsonNode;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.javalin.QBadRequestException;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Preserve the API field selection boundary while mapping native query criteria.
 *******************************************************************************/
class ApiRenamedQueryTest extends BaseTest
{
   /*******************************************************************************
    ** Two distinguishable records make ignored filters observable.
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      GetTableApiFieldsAction.clearCaches();
      ApiFieldMetaDataContainer.of(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getField("firstName"))
         .getApiFieldMetaData(TestUtils.API_NAME).withApiFieldName("givenName");
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Ada").withValue("lastName", "One").withValue("noOfShoes", 2),
         new QRecord().withValue("id", 2).withValue("firstName", "Bea").withValue("lastName", "Two").withValue("noOfShoes", 4))));
   }



   /*******************************************************************************
    ** Tests mutate owned metadata; clear its static field-list memoization.
    *******************************************************************************/
   @AfterEach
   void clearCaches()
   {
      GetTableApiFieldsAction.clearCaches();
   }



   /*******************************************************************************
    ** API aliases resolve to native names in both supported historical/current versions.
    *******************************************************************************/
   @Test
   void testRenamedStringFiltersAndOrdering() throws Exception
   {
      for(String version : List.of(TestUtils.V2022_Q4, TestUtils.CURRENT_API_VERSION))
      {
         assertRecord(query(TestUtils.API_NAME, version, Map.of("givenName", List.of("Ada"))), 1);
         assertRecord(query(TestUtils.API_NAME, version, Map.of("givenName", List.of("!Ada"))), 2);
         assertEquals(0, query(TestUtils.API_NAME, version, Map.of("givenName", List.of("Absent"))).path("count").asInt());
         assertRecord(query(TestUtils.API_NAME, version, Map.of("orderBy", List.of("givenName ASC"), "pageSize", List.of("1"), "includeCount", List.of("false"))), 1);
      }
   }



   /*******************************************************************************
    ** Lookup stays scoped to API and version; internal/private names are not fallback inputs.
    *******************************************************************************/
   @Test
   void testApiScopeAndUnknownFieldBoundaries() throws Exception
   {
      assertRecord(query(TestUtils.ALTERNATIVE_API_NAME, TestUtils.CURRENT_API_VERSION, Map.of("firstName", List.of("Ada"))), 1);
      for(String field : List.of("firstName", "price", "unknown"))
      {
         assertEquals("Unrecognized filter criteria field: " + field, assertThrows(QBadRequestException.class,
            () -> query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, Map.of(field, List.of("x")))).getMessage());
      }
      assertThrows(QBadRequestException.class, () -> query(TestUtils.ALTERNATIVE_API_NAME, TestUtils.CURRENT_API_VERSION, Map.of("givenName", List.of("Ada"))));
      assertThrows(QBadRequestException.class, () -> query(TestUtils.API_NAME, TestUtils.V2022_Q4, Map.of("noOfShoes", List.of("2"))));
      assertTrue(assertThrows(QBadRequestException.class,
         () -> query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, Map.of("givenName", List.of("BETWEEN Ada")))).getMessage().contains("for field givenName requires 2 values"));
   }



   /*******************************************************************************
    ** Historical replacements retain priority over the removed field's native name.
    *******************************************************************************/
   @Test
   void testHistoricalReplacementMapping() throws Exception
   {
      assertRecord(query(TestUtils.API_NAME, TestUtils.V2022_Q4, Map.of("shoeCount", List.of("2"))), 1);
      assertRecord(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, Map.of("noOfShoes", List.of("4"))), 2);
      assertThrows(QBadRequestException.class, () -> query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, Map.of("shoeCount", List.of("2"))));
   }



   /*******************************************************************************
    ** Custom mappers keep their API-name input and control the final native criterion.
    *******************************************************************************/
   @Test
   void testCustomMapperContractIsPreserved() throws Exception
   {
      ApiFieldMetaDataContainer.of(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getField("firstName"))
         .getApiFieldMetaData(TestUtils.API_NAME).withCustomValueMapper(new QCodeReference(OwnedQueryMapper.class));
      assertRecord(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, Map.of("givenName", List.of("Ada"))), 2);
   }



   /*******************************************************************************
    ** Use the normal application API query and inspect consumer-visible results.
    *******************************************************************************/
   private JsonNode query(String api, String version, Map<String, List<String>> parameters) throws Exception
   {
      Map<String, Serializable> result = ApiImplementation.query(ApiInstanceMetaDataContainer.of(QContext.getQInstance())
         .getApiInstanceMetaData(api), version, TestUtils.TABLE_NAME_PERSON, parameters);
      return JsonUtils.toObject(JsonUtils.toJson(result), JsonNode.class);
   }



   /*******************************************************************************
    ** Exact record identity distinguishes ignored filters and mapper priority changes.
    *******************************************************************************/
   private void assertRecord(JsonNode result, int id)
   {
      assertEquals(1, result.path("records").size());
      assertEquals(id, result.at("/records/0/id").asInt());
   }



   /*******************************************************************************
    ** Owned callback proves mapping does not overwrite custom criteria or API-name arguments.
    *******************************************************************************/
   public static class OwnedQueryMapper extends ApiFieldCustomValueMapper
   {
      /*******************************************************************************
       ** Deliberately select the other record using a different native field.
       *******************************************************************************/
      @Override
      public void customizeFilterCriteriaForQueryOrCount(QueryOrCountInputInterface input, QQueryFilter filter, QFilterCriteria criteria, String apiFieldName, ApiFieldMetaData metadata)
      {
         assertEquals("givenName", apiFieldName);
         assertEquals("givenName", criteria.getFieldName());
         criteria.setFieldName("lastName");
         criteria.setValues(List.of("Two"));
      }
   }
}
