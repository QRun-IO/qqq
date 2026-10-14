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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.QRelatedRecordPicker;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.RelatedRecordPickerMode;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** HTTP tests of the relatedRecordPicker routes and the picker table metadata.
 *******************************************************************************/
class RelatedRecordPickerSpecV1Test extends SpecTestBase
{
   private static final String SKU   = "rrp_sku";
   private static final String ORDER = "rrp_order";
   private static final String LINE  = "rrp_order_line";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new RelatedRecordPickerQuerySpecV1();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new RelatedRecordPickerPrepareSpecV1(), new RelatedRecordPickerAddSpecV1(), new RelatedRecordPickerRemoveSpecV1(), new TableMetaDataSpecV1());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      String    backend   = TestUtils.BACKEND_NAME_MEMORY;
      qInstance.addTable(new QTableMetaData().withName(SKU).withBackendName(backend).withPrimaryKeyField("id")
         .withRecordLabelFormatAndFields("%s", "code")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("code", QFieldType.STRING))
         .withField(new QFieldMetaData("price", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("isActive", QFieldType.BOOLEAN)));
      qInstance.addPossibleValueSource(QPossibleValueSource.newForTable(SKU));
      qInstance.addJoin(new QJoinMetaData().withName("rrpOrderLine").withLeftTable(ORDER).withRightTable(LINE).withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "orderId")));
      qInstance.addTable(new QTableMetaData().withName(LINE).withBackendName(backend).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("orderId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("skuId", QFieldType.INTEGER).withPossibleValueSourceName(SKU))
         .withField(new QFieldMetaData("quantity", QFieldType.INTEGER).withIsRequired(true))
         .withField(new QFieldMetaData("unitPrice", QFieldType.DECIMAL)));
      qInstance.addTable(new QTableMetaData().withName(ORDER).withBackendName(backend).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("orderNo", QFieldType.STRING))
         .withAssociation(new Association().withName("orderLines").withAssociatedTableName(LINE).withJoinName("rrpOrderLine"))
         .withRelatedRecordPicker(new QRelatedRecordPicker().withName("addSkus").withMode(RelatedRecordPickerMode.MULTI_CHILD)
            .withAssociationName("orderLines").withChildTargetFieldName("skuId")
            .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)))
            .withChildFieldDefaults(Map.of("quantity", 1))
            .withChildValuesFromTarget(Map.of("unitPrice", "price"))
            .withReviewFieldNames(List.of("quantity", "unitPrice")))
         .withRelatedRecordPicker(new QRelatedRecordPicker().withName("skuLinks").withMode(RelatedRecordPickerMode.LINK)
            .withAssociationName("orderLines").withChildTargetFieldName("skuId").withChildFieldDefaults(Map.of("quantity", 1))));
      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected void primeTestData(QInstance qInstance) throws Exception
   {
      super.primeTestData(qInstance);
      QContext.withTemporaryContext(new com.kingsrook.qqq.backend.core.context.CapturedContext(qInstance, new QSystemUserSession()), () ->
      {
         new InsertAction().execute(new InsertInput(SKU).withRecords(List.of(
            new QRecord().withValue("id", 11).withValue("code", "WID-1").withValue("price", new BigDecimal("9.50")).withValue("isActive", true),
            new QRecord().withValue("id", 12).withValue("code", "GAD-2").withValue("price", new BigDecimal("4.00")).withValue("isActive", true),
            new QRecord().withValue("id", 13).withValue("code", "OLD-3").withValue("price", new BigDecimal("1.00")).withValue("isActive", false))));
         new InsertAction().execute(new InsertInput(ORDER).withRecords(List.of(new QRecord().withValue("id", 101).withValue("orderNo", "A-1"))));
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> post(String pickerName, String operation, Map<String, Object> body)
   {
      return (Unirest.post(getBaseUrlAndPath() + "/table/" + ORDER + "/relatedRecordPicker/" + pickerName + "/" + operation)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(body))
         .asString());
   }



   /*******************************************************************************
    ** query, prepare, add, then query again shows the linked SKU.
    *******************************************************************************/
   @Test
   void testQueryPrepareAdd()
   {
      HttpResponse<String> response = post("addSkus", "query", Map.of("primaryKey", 101, "searchTerm", "wid"));
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject body = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, body.getJSONArray("records").length());
      assertEquals("WID-1", body.getJSONArray("records").getJSONObject(0).getJSONObject("values").getString("code"));
      assertFalse(body.getBoolean("hasMore"));

      response = post("addSkus", "prepare", Map.of("primaryKey", 101, "targetIds", List.of(11, 12)));
      assertEquals(200, response.getStatus(), response.getBody());
      body = JsonUtils.toJSONObject(response.getBody());
      JSONArray drafts = body.getJSONArray("records");
      assertEquals(2, drafts.length());
      assertEquals(101, drafts.getJSONObject(0).getJSONObject("values").getInt("orderId"));
      assertEquals("WID-1", drafts.getJSONObject(0).getJSONObject("displayValues").getString("skuId"));
      assertEquals(List.of("quantity", "unitPrice"), body.getJSONArray("reviewFieldNames").toList());

      response = post("addSkus", "add", Map.of("primaryKey", 101, "records", List.of(
         Map.of("values", Map.of("skuId", 11, "quantity", 4)),
         Map.of("values", Map.of("skuId", 12, "quantity", 2, "orderId", 999)))));
      assertEquals(200, response.getStatus(), response.getBody());
      body = JsonUtils.toJSONObject(response.getBody());
      assertEquals(2, body.getJSONArray("records").length());
      assertEquals(101, body.getJSONArray("records").getJSONObject(1).getJSONObject("values").getInt("orderId"));

      response = post("addSkus", "query", Map.of("primaryKey", 101));
      body = JsonUtils.toJSONObject(response.getBody());
      assertEquals(List.of("11", "12"), body.getJSONArray("linkedIds").toList().stream().sorted().toList());
   }



   /*******************************************************************************
    ** Link and unlink; errors are 400/404.
    *******************************************************************************/
   @Test
   void testLinkRemoveAndErrors()
   {
      assertEquals(200, post("skuLinks", "add", Map.of("primaryKey", 101, "targetIds", List.of(11))).getStatus());
      HttpResponse<String> response = post("skuLinks", "remove", Map.of("primaryKey", 101, "targetIds", List.of(11)));
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals(1, JsonUtils.toJSONObject(response.getBody()).getInt("deletedCount"));

      assertEquals(400, post("addSkus", "remove", Map.of("primaryKey", 101, "targetIds", List.of(11))).getStatus());
      assertEquals(400, post("addSkus", "add", Map.of("primaryKey", 101, "targetIds", List.of(13))).getStatus());
      assertEquals(400, post("addSkus", "query", Map.of("filter", Map.of("criteria", List.of(Map.of("fieldName", "nope", "operator", "EQUALS", "values", List.of(1)))))).getStatus());
      assertEquals(404, post("nope", "query", Map.of()).getStatus());
      assertEquals(404, post("addSkus", "query", Map.of("primaryKey", 999)).getStatus());
   }



   /*******************************************************************************
    ** The table metadata publishes the pickers.
    *******************************************************************************/
   @Test
   void testTableMetaData()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/table/" + ORDER).asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONArray pickers = JsonUtils.toJSONObject(response.getBody()).getJSONArray("relatedRecordPickers");
      assertEquals(2, pickers.length());
      JSONObject addSkus = pickers.getJSONObject(0);
      assertEquals("MULTI_CHILD", addSkus.getString("mode"));
      assertEquals(SKU, addSkus.getString("targetTableName"));
      assertEquals(LINE, addSkus.getString("childTableName"));
      assertTrue(addSkus.getBoolean("canAddChildren"));
      assertEquals("isActive", addSkus.getJSONObject("filter").getJSONArray("criteria").getJSONObject(0).getString("fieldName"));
   }
}
