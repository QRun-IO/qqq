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
import java.time.LocalDate;
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
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** API DATE criteria should use the same native values as typed callers.
 *******************************************************************************/
class ApiDateQueryTest extends BaseTest
{
   /*******************************************************************************
    ** Normal insertion establishes typed dates and distinguishable records.
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      GetTableApiFieldsAction.clearCaches();
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "2000-01-01").withValue("birthDate", LocalDate.of(2000, 1, 1)),
         new QRecord().withValue("id", 2).withValue("firstName", "second").withValue("birthDate", LocalDate.of(2001, 2, 2)))));
   }



   /*******************************************************************************
    ** Keep owned metadata mutations out of other tests' cached field lists.
    *******************************************************************************/
   @AfterEach
   void clearCaches()
   {
      GetTableApiFieldsAction.clearCaches();
   }



   /*******************************************************************************
    ** Typed native positive and raw-string negative isolate the API conversion boundary.
    *******************************************************************************/
   @Test
   void testTypedNativeAndApiDateStringEquality() throws Exception
   {
      var typed = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON).withFilter(
         new QQueryFilter(new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, LocalDate.of(2000, 1, 1)))));
      assertEquals(1, typed.getRecords().size());
      assertEquals(1, typed.getRecords().get(0).getValueInteger("id"));
      assertEquals(0, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON).withFilter(
         new QQueryFilter(new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, "2000-01-01")))).getRecords().size());
      for(String version : List.of(TestUtils.V2022_Q4, TestUtils.CURRENT_API_VERSION))
      {
         for(String value : List.of("2000-01-01", "1/1/2000", "20000101"))
         {
            assertIds(query(TestUtils.API_NAME, version, "birthDay", value), List.of(1));
            assertIds(query(TestUtils.ALTERNATIVE_API_NAME, version, "birthDate", value), List.of(1));
         }
      }
   }



   /*******************************************************************************
    ** Convert DATE values without changing operators, blank behavior or string fields.
    *******************************************************************************/
   @Test
   void testDateOperatorsAndStringFields() throws Exception
   {
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", "!2000-01-01"), List.of(2));
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", "IN 2000-01-01,2001-02-02"), List.of(2, 1));
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", "BETWEEN 2000-01-01,2000-12-31"), List.of(1));
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", ">2000-01-01"), List.of(2));
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", "1999-01-01"), List.of());
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", "EMPTY"), List.of());
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", "!EMPTY"), List.of(2, 1));
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "firstName", "2000-01-01"), List.of(1));
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "firstName", "LIKE 2000%"), List.of(1));
   }



   /*******************************************************************************
    ** The replacement's native DATE type wins over legacy STRING metadata and mapper.
    *******************************************************************************/
   @Test
   void testHistoricalReplacementUsesNativeDateType() throws Exception
   {
      QFieldMetaData legacy = new QFieldMetaData("oldBirthday", QFieldType.STRING).withSupplementalMetaData(
         new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData()
            .withInitialVersion(TestUtils.V2022_Q4).withFinalVersion(TestUtils.V2022_Q4).withApiFieldName("legacyDay")
            .withReplacedByFieldName("birthDate").withCustomValueMapper(new QCodeReference(DateTokenMapper.class))));
      new QInstanceEnricher(QContext.getQInstance()).enrichField(legacy);
      ApiTableMetaDataContainer.of(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)).getApiTableMetaData(TestUtils.API_NAME)
         .withRemovedApiField(legacy);
      assertIds(query(TestUtils.API_NAME, TestUtils.V2022_Q4, "legacyDay", "2000-01-01"), List.of(1));
      assertThrows(QBadRequestException.class, () -> query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "legacyDay", "2000-01-01"));
      assertThrows(QBadRequestException.class, () -> query(TestUtils.API_NAME, TestUtils.V2022_Q4, "legacyDay", "not-a-date"));
   }



   /*******************************************************************************
    ** Custom mappers still receive raw values and may select a different native field.
    *******************************************************************************/
   @Test
   void testCustomMapperKeepsRawValueAndPrecedence() throws Exception
   {
      ApiFieldMetaDataContainer.of(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getField("birthDate"))
         .getApiFieldMetaData(TestUtils.API_NAME).withCustomValueMapper(new QCodeReference(DateTokenMapper.class));
      assertIds(query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", "owned-token"), List.of(2));
   }



   /*******************************************************************************
    ** Existing bad-request aggregation handles conversion errors before native query execution.
    *******************************************************************************/
   @Test
   void testInvalidDateValuesAreBadRequests()
   {
      for(String value : List.of("not-a-date", "IN 2000-01-01,not-a-date", "BETWEEN 2000-01-01,not-a-date"))
      {
         QBadRequestException error = assertThrows(QBadRequestException.class,
            () -> query(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION, "birthDay", value));
         assertTrue(error.getMessage().contains("not-a-date"));
      }
   }



   /*******************************************************************************
    ** Exercise the normal application API entry point with public field names.
    *******************************************************************************/
   private JsonNode query(String api, String version, String field, String value) throws Exception
   {
      Map<String, Serializable> result = ApiImplementation.query(ApiInstanceMetaDataContainer.of(QContext.getQInstance())
         .getApiInstanceMetaData(api), version, TestUtils.TABLE_NAME_PERSON, Map.of(field, List.of(value)));
      return JsonUtils.toObject(JsonUtils.toJson(result), JsonNode.class);
   }



   /*******************************************************************************
    ** Exact count and identity catch ignored filters and overbroad matches.
    *******************************************************************************/
   private void assertIds(JsonNode result, List<Integer> ids)
   {
      assertEquals(ids.size(), result.path("count").asInt());
      assertEquals(ids.size(), result.path("records").size());
      for(int i = 0; i < ids.size(); i++)
      {
         assertEquals(ids.get(i).intValue(), result.path("records").get(i).path("id").asInt());
      }
   }



   /*******************************************************************************
    ** This callback must see unconverted token input and control the output criterion.
    *******************************************************************************/
   public static class DateTokenMapper extends ApiFieldCustomValueMapper
   {
      /*******************************************************************************
       ** A token selects another record, proving native DATE conversion is bypassed.
       *******************************************************************************/
      @Override
      public void customizeFilterCriteriaForQueryOrCount(QueryOrCountInputInterface input, QQueryFilter filter, QFilterCriteria criteria, String apiFieldName, ApiFieldMetaData metadata)
      {
         assertEquals("birthDay", apiFieldName);
         assertEquals("birthDay", criteria.getFieldName());
         assertEquals(List.of("owned-token"), criteria.getValues());
         criteria.setFieldName("firstName");
         criteria.setValues(List.of("second"));
      }
   }
}
