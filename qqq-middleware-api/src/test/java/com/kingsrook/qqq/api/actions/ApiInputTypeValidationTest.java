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
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.javalin.QBadRequestException;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** User field conversion errors belong at the API input boundary, not in the
 ** generic HTTP server-error handler. Native coercions and mapper inputs survive.
 *******************************************************************************/
class ApiInputTypeValidationTest extends BaseTest
{
   /*******************************************************************************
    ** Each BaseTest instance has fresh metadata, including its field mappings.
    *******************************************************************************/
   @AfterEach
   void clearCaches()
   {
      GetTableApiFieldsAction.clearCaches();
   }



   /*******************************************************************************
    ** Current, externally renamed and historical fields reject invalid values.
    *******************************************************************************/
   @Test
   void testInvalidNativeInteger()
   {
      GetTableApiFieldsAction.clearCaches();
      assertInvalid("noOfShoes", "not-an-integer", TestUtils.V2023_Q1);

   }



   /*******************************************************************************
    ** Errors identify the external API field name.
    *******************************************************************************/
   @Test
   void testInvalidRenamedDate()
   {
      GetTableApiFieldsAction.clearCaches();
      assertInvalid("birthDay", "not-a-date", TestUtils.V2023_Q1);
   }



   /*******************************************************************************
    ** Historical replacements target a current physical field.
    *******************************************************************************/
   @Test
   void testInvalidHistoricalReplacement()
   {
      GetTableApiFieldsAction.clearCaches();
      assertInvalid("shoeCount", "not-an-integer", TestUtils.V2022_Q4);
   }



   /*******************************************************************************
    ** Validation does not replace existing raw values or native coercion semantics.
    *******************************************************************************/
   @Test
   void testValidCoercionsNullAndNonEditableFields() throws QException
   {
      GetTableApiFieldsAction.clearCaches();
      QRecord record = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"noOfShoes":"4","birthDay":null,"id":"ignored-invalid-id"}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1, false);
      assertEquals("4", record.getValue("noOfShoes"));
      assertEquals(4, record.getValueInteger("noOfShoes"));
      assertNull(record.getValue("birthDate"));
      assertFalse(record.getValues().containsKey("id"));
      record = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("{\"shoeCount\":\"6\"}"), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4, true);
      assertEquals(6, record.getValueInteger("noOfShoes"));
   }



   /*******************************************************************************
    ** Custom mappers may intentionally accept a non-native representation.
    *******************************************************************************/
   @Test
   void testCustomMapperOwnsItsInputFormat() throws QException
   {
      ApiFieldMetaDataContainer.of(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getField("noOfShoes"))
         .getApiFieldMetaData(TestUtils.API_NAME).setCustomValueMapper(new QCodeReference(PairCountMapper.class));
      GetTableApiFieldsAction.clearCaches();
      QRecord record = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("{\"noOfShoes\":\"two pairs\"}"), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1, true);
      assertEquals(4, record.getValueInteger("noOfShoes"));
   }



   /*******************************************************************************
    ** The public input adapter must reject before a table action can execute.
    *******************************************************************************/
   private void assertInvalid(String field, String value, String version)
   {
      QBadRequestException exception = assertThrows(QBadRequestException.class, () -> QRecordApiAdapter.apiJsonObjectToQRecord(
         new JSONObject().put(field, value), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, version, true));
      assertTrue(exception.getMessage().contains(field));
      assertFalse(exception.getMessage().contains(value));
   }



   /*******************************************************************************
    ** Application-level input syntax deliberately differs from the integer field.
    *******************************************************************************/
   public static class PairCountMapper extends ApiFieldCustomValueMapper
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Serializable produceApiValue(QRecord record, String apiFieldName)
      {
         return "two pairs";
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void consumeApiValue(QRecord record, Object value, JSONObject fullApiJsonObject, String apiFieldName)
      {
         assertEquals("two pairs", value);
         record.setValue("noOfShoes", 4);
      }
   }
}
