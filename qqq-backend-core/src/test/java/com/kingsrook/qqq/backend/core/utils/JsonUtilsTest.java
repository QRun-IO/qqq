/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for JsonUtils.
 **
 *******************************************************************************/
class JsonUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toJsonQRecordInput()
   {
      QRecord qRecord = getQRecord();
      String  json    = JsonUtils.toJson(qRecord);
      assertEquals("""
         {"tableName":"foo","values":{"foo":"Foo","bar":3.14159,"baz":null}}""", json);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toJsonQRecordInputWithNullValues()
   {
      QRecord qRecord = getQRecord();
      String json = JsonUtils.toJsonCustomized(qRecord, builder ->
      {
         builder.serializationInclusion(JsonInclude.Include.ALWAYS);
      });

      assertThat(json).contains("""
         "values":{"foo":"Foo","bar":3.14159,"baz":null}""");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class LooksLikeAnEntityButJustThrowsInItsGetterMethod
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public String getValue()
      {
         throw new IllegalStateException("Test");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toJsonNull()
   {
      assertThrowsExactly(IllegalArgumentException.class,
         () -> JsonUtils.toJson(new LooksLikeAnEntityButJustThrowsInItsGetterMethod()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toPrettyJsonNull()
   {
      assertThrowsExactly(IllegalArgumentException.class,
         () -> JsonUtils.toPrettyJson(new LooksLikeAnEntityButJustThrowsInItsGetterMethod()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toPrettyJsonQRecordInput()
   {
      QRecord qRecord = getQRecord();
      String  json    = JsonUtils.toPrettyJson(qRecord);
      // todo assertEquals("{\"tableName\":\"foo\",\"primaryKey\":1,\"values\":{\"foo\":\"Foo\",\"bar\":3.14159}}", json);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toJSONObject()
   {
      JSONObject jsonObject = JsonUtils.toJSONObject("""
         {
            "Foo": "Bar",
            "Baz": [1, 2, 3]
         }
         """);
      assertNotNull(jsonObject);
      assertTrue(jsonObject.has("Foo"));
      assertEquals("Bar", jsonObject.getString("Foo"));
      assertTrue(jsonObject.has("Baz"));
      assertEquals(3, jsonObject.getJSONArray("Baz").length());
      assertEquals(1, jsonObject.getJSONArray("Baz").get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toJSONObject_malformed()
   {
      // todo - what do we want to throw here?
      assertThrows(JSONException.class, () ->
      {
         JsonUtils.toJSONObject("""
            {
               "Foo": "Bar",
            """);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toJSONObjectNonJsonObject()
   {
      try
      {
         JsonUtils.toJSONObject("");
      }
      catch(JSONException je)
      {
         System.out.println("Caught Expected exception");
         return;
      }

      fail("Did not catch expected exception");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toJSONArray()
   {
      JSONArray jsonArray = JsonUtils.toJSONArray("""
         [
            {"Foo": "Bar"},
            {"Baz": [1, 2, 3]}
         ]
         """);
      assertNotNull(jsonArray);
      assertEquals(2, jsonArray.length());
      assertTrue(jsonArray.getJSONObject(0).has("Foo"));
      assertEquals("Bar", jsonArray.getJSONObject(0).getString("Foo"));
      assertTrue(jsonArray.getJSONObject(1).has("Baz"));
      assertEquals(3, jsonArray.getJSONObject(1).getJSONArray("Baz").length());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord getQRecord()
   {
      QRecord qRecord = new QRecord();
      qRecord.setTableName("foo");
      qRecord.setValue("id", 1);
      Map<String, Serializable> values = new LinkedHashMap<>();
      qRecord.setValues(values);
      values.put("foo", "Foo");
      values.put("bar", new BigDecimal("3.14159"));
      values.put("baz", null);
      return qRecord;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toJsonNullInput()
   {
      String json = JsonUtils.toJson(null);
      assertEquals("null", json);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_looksLikeObject()
   {
      assertFalse(JsonUtils.looksLikeObject(""));
      assertFalse(JsonUtils.looksLikeObject(null));
      assertFalse(JsonUtils.looksLikeObject("json"));
      assertFalse(JsonUtils.looksLikeObject("[]"));
      assertTrue(JsonUtils.looksLikeObject("{}"));
      assertTrue(JsonUtils.looksLikeObject("  {}"));
      assertTrue(JsonUtils.looksLikeObject("\n\n\n{}"));
      assertTrue(JsonUtils.looksLikeObject("\n{\n[]\n}\n"));
      assertTrue(JsonUtils.looksLikeObject("\n\n\n  { \n}\n\n\n"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_looksLikeArray()
   {
      assertFalse(JsonUtils.looksLikeArray(""));
      assertFalse(JsonUtils.looksLikeArray(null));
      assertFalse(JsonUtils.looksLikeArray("json"));
      assertFalse(JsonUtils.looksLikeArray("{json[]}"));
      assertFalse(JsonUtils.looksLikeArray("{}"));
      assertTrue(JsonUtils.looksLikeArray("[]"));
      assertTrue(JsonUtils.looksLikeArray("  []"));
      assertTrue(JsonUtils.looksLikeArray("\n\n\n[]"));
      assertTrue(JsonUtils.looksLikeArray("\n[\n{}\n}\n"));
      assertTrue(JsonUtils.looksLikeArray("\n\n\n  [ \n]\n\n\n"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_toObject() throws IOException
   {
      String json = JsonUtils.toJson(new QQueryFilter()
         .withCriteria(new QFilterCriteria().withFieldName("id").withOperator(QCriteriaOperator.EQUALS).withValues(List.of(1)))
         .withCriteria(new QFilterCriteria().withFieldName("name").withOperator(QCriteriaOperator.IN).withValues(List.of("Darin", "James")))
         .withOrderBy(new QFilterOrderBy().withFieldName("age").withIsAscending(true)));

      System.out.println(json);

      QQueryFilter qQueryFilter = JsonUtils.toObject(json, QQueryFilter.class);
      assertEquals(2, qQueryFilter.getCriteria().size());
      assertEquals("id", qQueryFilter.getCriteria().get(0).getFieldName());
      assertEquals(QCriteriaOperator.EQUALS, qQueryFilter.getCriteria().get(0).getOperator());
      assertEquals(List.of(1), qQueryFilter.getCriteria().get(0).getValues());
      assertEquals("name", qQueryFilter.getCriteria().get(1).getFieldName());
      assertEquals(QCriteriaOperator.IN, qQueryFilter.getCriteria().get(1).getOperator());
      assertEquals(List.of("Darin", "James"), qQueryFilter.getCriteria().get(1).getValues());
      assertEquals(1, qQueryFilter.getOrderBys().size());
      assertEquals("age", qQueryFilter.getOrderBys().get(0).getFieldName());
      assertTrue(qQueryFilter.getOrderBys().get(0).getIsAscending());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNullKeyInMap()
   {
      Map<Object, String> mapWithNullKey = MapBuilder.of(null, "foo");

      //////////////////////////////////////////////////////
      // assert default behavior throws with null map key //
      //////////////////////////////////////////////////////
      assertThatThrownBy(() -> JsonUtils.toJson(mapWithNullKey)).rootCause().hasMessageContaining("Null key for a Map not allowed in JSON");

      ////////////////////////////////////////////////////////////////////////
      // assert that the nullKeyToEmptyStringSerializer does what we expect //
      ////////////////////////////////////////////////////////////////////////
      assertEquals("""
         {"":"foo"}""", JsonUtils.toJsonWithMapper(mapWithNullKey, mapper -> mapper.getSerializerProvider().setNullKeySerializer(JsonUtils.nullKeyToEmptyStringSerializer)));
   }

}
