/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ValueMapper 
 *******************************************************************************/
class BulkLoadValueMapperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      BulkInsertMapping mapping = new BulkInsertMapping().withFieldNameToValueMapping(Map.of(
         "storeId", Map.of("QQQMart", 1, "Q'R'Us", 2),
         "shipToName", Map.of("HoJu", "Homer", "Bart", "Bartholomew"),
         "orderLine.sku", Map.of("ABC", "Alphabet"),
         "orderLine.extrinsics.value", Map.of("foo", "bar", "bar", "baz"),
         "extrinsics.key", Map.of("1", "one", "2", "two")
      ));

      QRecord inputRecord = new QRecord()
         .withValue("storeId", "QQQMart")
         .withValue("shipToName", "HoJu")
         .withAssociatedRecord("orderLine", new QRecord()
            .withValue("sku", "ABC")
            .withAssociatedRecord("extrinsics", new QRecord()
               .withValue("key", "myKey")
               .withValue("value", "foo")
            )
            .withAssociatedRecord("extrinsics", new QRecord()
               .withValue("key", "yourKey")
               .withValue("value", "bar")
            )
         )
         .withAssociatedRecord("extrinsics", new QRecord()
            .withValue("key", 1)
            .withValue("value", "foo")
         );
      JSONObject beforeJson = recordToJson(inputRecord);

      QRecord expectedRecord = new QRecord()
         .withValue("storeId", 1)
         .withValue("shipToName", "Homer")
         .withAssociatedRecord("orderLine", new QRecord()
            .withValue("sku", "Alphabet")
            .withAssociatedRecord("extrinsics", new QRecord()
               .withValue("key", "myKey")
               .withValue("value", "bar")
            )
            .withAssociatedRecord("extrinsics", new QRecord()
               .withValue("key", "yourKey")
               .withValue("value", "baz")
            )
         )
         .withAssociatedRecord("extrinsics", new QRecord()
            .withValue("key", "one")
            .withValue("value", "foo")
         );
      JSONObject expectedJson = recordToJson(expectedRecord);

      BulkLoadValueMapper.valueMapping(List.of(inputRecord), mapping, QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER));
      JSONObject actualJson = recordToJson(inputRecord);

      System.out.println("Before");
      System.out.println(beforeJson.toString(3));
      System.out.println("Actual");
      System.out.println(actualJson.toString(3));
      System.out.println("Expected");
      System.out.println(expectedJson.toString(3));

      assertThat(actualJson).usingRecursiveComparison().isEqualTo(expectedJson);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   void testPossibleValue(Serializable inputValue, Serializable expectedValue, boolean expectErrors) throws QException
   {
      QRecord inputRecord = new QRecord().withValue("homeStateId", inputValue);
      BulkLoadValueMapper.valueMapping(List.of(inputRecord), new BulkInsertMapping(), QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY));
      assertEquals(expectedValue, inputRecord.getValue("homeStateId"));

      if(expectErrors)
      {
         assertThat(inputRecord.getErrors().get(0)).isInstanceOf(BulkLoadPossibleValueError.class);
      }
      else
      {
         assertThat(inputRecord.getErrors()).isNullOrEmpty();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValues() throws QException
   {
      testPossibleValue(1, 1, false);
      testPossibleValue("1", 1, false);
      testPossibleValue("1.0", 1, false);
      testPossibleValue(new BigDecimal("1.0"), 1, false);
      testPossibleValue("IL", 1, false);
      testPossibleValue("il", 1, false);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // unmappables - should have a null value (this used to not be the case - the bad-value would come through...) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      testPossibleValue(512, null, true); // an int, but not in the PVS
      testPossibleValue("USA", null, true);
      testPossibleValue(true, null, true);
      testPossibleValue(new BigDecimal("4.7"), null, true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static JSONObject recordToJson(QRecord record)
   {
      JSONObject jsonObject = new JSONObject();
      for(Map.Entry<String, Serializable> valueEntry : CollectionUtils.nonNullMap(record.getValues()).entrySet())
      {
         jsonObject.put(valueEntry.getKey(), valueEntry.getValue());
      }
      for(Map.Entry<String, List<QRecord>> associationEntry : CollectionUtils.nonNullMap(record.getAssociatedRecords()).entrySet())
      {
         JSONArray jsonArray = new JSONArray();
         for(QRecord associationRecord : CollectionUtils.nonNullList(associationEntry.getValue()))
         {
            jsonArray.put(recordToJson(associationRecord));
         }
         jsonObject.put(associationEntry.getKey(), jsonArray);
      }
      return (jsonObject);
   }

}