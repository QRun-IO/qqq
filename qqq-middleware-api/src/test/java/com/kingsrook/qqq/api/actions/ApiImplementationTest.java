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


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.javalin.QBadRequestException;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapperBulkSupportInterface;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiAssociationMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ApiImplementation 
 *******************************************************************************/
class ApiImplementationTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      GetTableApiFieldsAction.clearCaches();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExcludedAssociation() throws QException
   {
      QInstance           qInstance           = QContext.getQInstance();
      ApiInstanceMetaData apiInstanceMetaData = ApiInstanceMetaDataContainer.of(qInstance).getApiInstanceMetaData(TestUtils.API_NAME);

      TestUtils.insert1Order3Lines4LineExtrinsicsAnd1OrderExtrinsic();

      /////////////////////////////////////////////////
      // get the order - make sure it has extrinsics //
      /////////////////////////////////////////////////
      Map<String, Serializable> order = ApiImplementation.get(apiInstanceMetaData, TestUtils.CURRENT_API_VERSION, "order", "1");
      assertTrue(order.containsKey("extrinsics"));

      /////////////////////////////////////////////////////
      // turn off the extrinsics association for the api //
      /////////////////////////////////////////////////////
      QTableMetaData            table                     = qInstance.getTable(TestUtils.TABLE_NAME_ORDER);
      ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
      ApiTableMetaData          apiTableMetaData          = apiTableMetaDataContainer.getApiTableMetaData(TestUtils.API_NAME);
      apiTableMetaData.withApiAssociationMetaData("extrinsics", new ApiAssociationMetaData().withIsExcluded(true));

      /////////////////////////////////////////////////
      // re-fetch - should no longer have extrinsics //
      /////////////////////////////////////////////////
      order = ApiImplementation.get(apiInstanceMetaData, TestUtils.CURRENT_API_VERSION, "order", "1");
      assertFalse(order.containsKey("extrinsics"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAssociationVersions() throws QException
   {
      QInstance           qInstance           = QContext.getQInstance();
      ApiInstanceMetaData apiInstanceMetaData = ApiInstanceMetaDataContainer.of(qInstance).getApiInstanceMetaData(TestUtils.API_NAME);

      TestUtils.insert1Order3Lines4LineExtrinsicsAnd1OrderExtrinsic();

      /////////////////////////////////////////////////
      // get the order - make sure it has extrinsics //
      /////////////////////////////////////////////////
      Map<String, Serializable> order = ApiImplementation.get(apiInstanceMetaData, TestUtils.CURRENT_API_VERSION, "order", "1");
      assertTrue(order.containsKey("extrinsics"));

      /////////////////////////////////////////////////
      // set the initial version for the association //
      /////////////////////////////////////////////////
      QTableMetaData            table                     = qInstance.getTable(TestUtils.TABLE_NAME_ORDER);
      ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
      ApiTableMetaData          apiTableMetaData          = apiTableMetaDataContainer.getApiTableMetaData(TestUtils.API_NAME);
      apiTableMetaData.withApiAssociationMetaData("extrinsics", new ApiAssociationMetaData().withInitialVersion(TestUtils.V2023_Q1));

      ////////////////////////////////////////////////////
      // re-fetch - should have or not based on version //
      ////////////////////////////////////////////////////
      assertFalse(ApiImplementation.get(apiInstanceMetaData, TestUtils.V2022_Q4, "order", "1").containsKey("extrinsics"));
      assertTrue(ApiImplementation.get(apiInstanceMetaData, TestUtils.V2023_Q1, "order", "1").containsKey("extrinsics"));

      /////////////////////////////////////////////////
      // set the final version for the association //
      /////////////////////////////////////////////////
      apiTableMetaData.withApiAssociationMetaData("extrinsics", new ApiAssociationMetaData().withInitialVersion(TestUtils.V2022_Q4).withFinalVersion(TestUtils.V2022_Q4));

      ////////////////////////////////////////////////////
      // re-fetch - should have or not based on version //
      ////////////////////////////////////////////////////
      assertTrue(ApiImplementation.get(apiInstanceMetaData, TestUtils.V2022_Q4, "order", "1").containsKey("extrinsics"));
      assertFalse(ApiImplementation.get(apiInstanceMetaData, TestUtils.V2023_Q1, "order", "1").containsKey("extrinsics"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValueCustomizer() throws QException
   {
      QInstance           qInstance           = QContext.getQInstance();
      ApiInstanceMetaData apiInstanceMetaData = ApiInstanceMetaDataContainer.of(qInstance).getApiInstanceMetaData(TestUtils.API_NAME);
      TestUtils.insertSimpsons();

      ////////////////////////////////////////////////////////////////////
      // set up a custom value mapper on lastName field of person table //
      ////////////////////////////////////////////////////////////////////
      QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_PERSON);
      QFieldMetaData field = table.getField("lastName");
      field.withSupplementalMetaData(new ApiFieldMetaDataContainer()
         .withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData()
            .withInitialVersion(TestUtils.V2022_Q4)
            .withCustomValueMapper(new QCodeReference(PersonLastNameApiValueCustomizer.class))));

      ////////////////////////////////////////////////
      // get a person - make sure custom method ran //
      ////////////////////////////////////////////////
      Map<String, Serializable> person = ApiImplementation.get(apiInstanceMetaData, TestUtils.CURRENT_API_VERSION, "person", "1");
      assertEquals("customValue-Simpson", person.get("lastName"));

      ////////////////////////////////////////////////////
      // insert a person - make sure custom method runs //
      ////////////////////////////////////////////////////
      ApiImplementation.insert(apiInstanceMetaData, TestUtils.CURRENT_API_VERSION, "person", """
         {"firstName": "Ned", "lastName": "stripThisAway-Flanders"}
         """);
      QRecord insertedPerson = new GetAction().executeForRecord(new GetInput(TestUtils.TABLE_NAME_PERSON).withPrimaryKey(6));
      assertEquals("Flanders", insertedPerson.getValueString("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBulkValueCustomizer() throws QException
   {
      QInstance           qInstance           = QContext.getQInstance();
      ApiInstanceMetaData apiInstanceMetaData = ApiInstanceMetaDataContainer.of(qInstance).getApiInstanceMetaData(TestUtils.API_NAME);
      TestUtils.insertSimpsons();

      ////////////////////////////////////////////////////////////////////
      // set up a custom value mapper on lastName field of person table //
      ////////////////////////////////////////////////////////////////////
      QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_PERSON);
      QFieldMetaData field = table.getField("lastName");
      field.withSupplementalMetaData(new ApiFieldMetaDataContainer()
         .withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData()
            .withInitialVersion(TestUtils.V2022_Q4)
            .withCustomValueMapper(new QCodeReference(PersonLastNameBulkApiValueCustomizer.class))));

      ////////////////////////////////////////////////
      // get a person - make sure custom method ran //
      ////////////////////////////////////////////////
      Map<String, Serializable> person = ApiImplementation.get(apiInstanceMetaData, TestUtils.CURRENT_API_VERSION, "person", "1");
      assertEquals("value from prepareToProduceApiValues", person.get("lastName"));
      assertEquals(1, PersonLastNameBulkApiValueCustomizer.prepareWasCalledWithThisNoOfRecords);

      /////////////////////////////////////////////////////
      // query for persons - make sure custom method ran //
      /////////////////////////////////////////////////////
      Map<String, Serializable> queryResult = ApiImplementation.query(apiInstanceMetaData, TestUtils.CURRENT_API_VERSION, "person", Collections.emptyMap());
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> records = (List<Map<String, Object>>) queryResult.get("records");
      assertEquals("value from prepareToProduceApiValues", records.get(0).get("lastName"));
      assertEquals(queryResult.get("count"), PersonLastNameBulkApiValueCustomizer.prepareWasCalledWithThisNoOfRecords);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryWithRemovedFields() throws QException
   {
      QInstance           qInstance           = QContext.getQInstance();
      ApiInstanceMetaData apiInstanceMetaData = ApiInstanceMetaDataContainer.of(qInstance).getApiInstanceMetaData(TestUtils.API_NAME);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(new QRecord()
         .withValue("firstName", "Tim")
         .withValue("noOfShoes", 2)
         .withValue("birthDate", LocalDate.of(1980, Month.MAY, 31))
         .withValue("cost", new BigDecimal("3.50"))
         .withValue("price", new BigDecimal("9.99"))
         .withValue("photo", "ABCD".getBytes())));

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // query by a field that wasn't in an old api version, but is in the table now - should fail //
      ///////////////////////////////////////////////////////////////////////////////////////////////

      assertThatThrownBy(() ->
         ApiImplementation.query(apiInstanceMetaData, TestUtils.V2022_Q4, TestUtils.TABLE_NAME_PERSON, MapBuilder.of("noOfShoes", List.of("2"))))
         .isInstanceOf(QBadRequestException.class)
         .hasMessageContaining("Unrecognized filter criteria field");

      {
         /////////////////////////////////////////////
         // query by a removed field (was replaced) //
         /////////////////////////////////////////////
         Map<String, Serializable> queryResult = ApiImplementation.query(apiInstanceMetaData, TestUtils.V2022_Q4, TestUtils.TABLE_NAME_PERSON, MapBuilder.of("shoeCount", List.of("2")));
         assertEquals(1, queryResult.get("count"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonLastNameApiValueCustomizer extends ApiFieldCustomValueMapper
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Serializable produceApiValue(QRecord record, String apiFieldName)
      {
         return ("customValue-" + record.getValueString("lastName"));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void consumeApiValue(QRecord record, Object value, JSONObject fullApiJsonObject, String apiFieldName)
      {
         String valueString = ValueUtils.getValueAsString(value);
         valueString = valueString.replaceFirst("^stripThisAway-", "");
         record.setValue("lastName", valueString);
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonLastNameBulkApiValueCustomizer extends ApiFieldCustomValueMapper implements ApiFieldCustomValueMapperBulkSupportInterface
   {
      static Integer prepareWasCalledWithThisNoOfRecords = null;

      private String valueToPutInRecords = null;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Serializable produceApiValue(QRecord record, String apiFieldName)
      {
         return (valueToPutInRecords);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void prepareToProduceApiValues(List<QRecord> records)
      {
         prepareWasCalledWithThisNoOfRecords = records.size();
         valueToPutInRecords = "value from prepareToProduceApiValues";
      }
   }

}