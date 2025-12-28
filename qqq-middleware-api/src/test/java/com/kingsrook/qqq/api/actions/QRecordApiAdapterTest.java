/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.api.actions;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.actions.io.QRecordApiAdapterToApiInput;
import com.kingsrook.qqq.api.javalin.QBadRequestException;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QRecordApiAdapter
 *******************************************************************************/
class QRecordApiAdapterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNullInputRecord() throws QException
   {
      assertNull(QRecordApiAdapter.qRecordToApiMap(null, TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordToApiMap() throws QException
   {
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // QRecord has values corresponding to what's defined in the QInstance (and the underlying backend system) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QRecord person = TestUtils.getTim2ShoesRecord();

      Map<String, Serializable> pastApiRecord = QRecordApiAdapter.qRecordToApiMap(person, TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4);
      assertEquals(2, pastApiRecord.get("shoeCount")); // old field name - not currently in the QTable, but we can still get its value!
      assertFalse(pastApiRecord.containsKey("noOfShoes")); // current field name - doesn't appear in old api-version
      assertFalse(pastApiRecord.containsKey("cost")); // a current field name, but also not in this old api version
      assertEquals("QUJDRA==", pastApiRecord.get("photo")); // base64 version of "ABCD".getBytes()

      Map<String, Serializable> currentApiRecord = QRecordApiAdapter.qRecordToApiMap(person, TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1);
      assertFalse(currentApiRecord.containsKey("shoeCount")); // old field name - not in this current api version
      assertEquals(2, currentApiRecord.get("noOfShoes")); // current field name - value here as we expect
      assertFalse(currentApiRecord.containsKey("cost")); // future field name - not in the current api (we added the field during new dev, and didn't change the api)

      Map<String, Serializable> futureApiRecord = QRecordApiAdapter.qRecordToApiMap(person, TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q2);
      assertFalse(futureApiRecord.containsKey("shoeCount")); // old field name - also not in this future api version
      assertEquals(2, futureApiRecord.get("noOfShoes")); // current field name - still here.
      assertEquals(new BigDecimal("3.50"), futureApiRecord.get("cost")); // future field name appears now that we've requested this future api version.

      for(Map<String, Serializable> apiRecord : List.of(pastApiRecord, currentApiRecord, futureApiRecord))
      {
         assertEquals(LocalDate.parse("1980-05-31"), apiRecord.get("birthDay")); // use the apiFieldName
         assertFalse(apiRecord.containsKey("price")); // excluded field never appears
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // confirm that for the alternative api, we get a record that looks just like the input record (per its api meta data) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(String version : List.of(TestUtils.V2022_Q4, TestUtils.V2023_Q1, TestUtils.V2023_Q2))
      {
         Map<String, Serializable> alternativeApiRecord = QRecordApiAdapter.qRecordToApiMap(person, TestUtils.TABLE_NAME_PERSON, TestUtils.ALTERNATIVE_API_NAME, version);
         for(String key : person.getValues().keySet())
         {
            if(key.equals("photo"))
            {
               ////////////////////////////////////////////////////////////////////////////////////////
               // ok, well, skip the blob field (should be base64 version, and is covered elsewhere) //
               ////////////////////////////////////////////////////////////////////////////////////////
               continue;
            }

            assertEquals(person.getValueString(key), ValueUtils.getValueAsString(alternativeApiRecord.get(key)));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testApiJsonObjectToQRecord() throws QException
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // past version took shoeCount - so we still take that, but now put it in noOfShoes field of qRecord //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      QRecord recordFromOldApi = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"firstName": "Tim", "shoeCount": 2, "photo": "QUJDRA=="}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4, true);
      assertEquals(2, recordFromOldApi.getValueInteger("noOfShoes"));
      assertArrayEquals("ABCD".getBytes(), recordFromOldApi.getValueByteArray("photo"));

      ///////////////////////////////////////////
      // current version takes it as noOfShoes //
      ///////////////////////////////////////////
      QRecord recordFromCurrentApi = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"firstName": "Tim", "noOfShoes": 2}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1, true);
      assertEquals(2, recordFromCurrentApi.getValueInteger("noOfShoes"));

      /////////////////////////////////////////////
      // future version supports cost field too! //
      /////////////////////////////////////////////
      QRecord recordFromFutureApi = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"firstName": "Tim", "noOfShoes": 2, "cost": 3.50}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q2, true);
      assertEquals(2, recordFromFutureApi.getValueInteger("noOfShoes"));
      assertEquals(new BigDecimal("3.50"), recordFromFutureApi.getValueBigDecimal("cost"));

      ///////////////////////////////////////////////////////////////////
      // make sure apiFieldName is used (instead of table's fieldName) //
      ///////////////////////////////////////////////////////////////////
      QRecord recordWithApiFieldName = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"firstName": "Tim", "birthDay": "1976-05-28"}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q2, true);
      assertEquals("1976-05-28", recordWithApiFieldName.getValueString("birthDate"));

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // past version didn't have noOfShoes field (they called it shoeCount) -- fail if it was sent //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"firstName": "Tim", "noOfShoes": 2}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4, true))
         .isInstanceOf(QBadRequestException.class)
         .hasMessageContaining("unrecognized field name: noOfShoes")
         .hasMessageContaining("noOfShoes does not exist in version 2022.Q4, but does exist in versions: 2023.Q1");

      /////////////////////////////////////////////////////////////////////////
      // current version doesn't have cost field - fail if you send it to us //
      /////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"firstName": "Tim", "cost": 2}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1, true))
         .isInstanceOf(QBadRequestException.class)
         .hasMessageContaining("unrecognized field name: cost")
         .hasMessageNotContaining("cost does not exist in version 2023.Q1, but does exist in versions: 2023.Q2"); // this field only appears in a future version, not any current/supported versions.

      /////////////////////////////////
      // excluded field always fails //
      /////////////////////////////////
      for(String version : List.of(TestUtils.V2022_Q4, TestUtils.V2023_Q1, TestUtils.V2023_Q2))
      {
         assertThatThrownBy(() -> QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
            {"firstName": "Tim", "price": 2}
            """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, version, true))
            .isInstanceOf(QBadRequestException.class)
            .hasMessageContaining("unrecognized field name: price")
            .hasMessageNotContaining("price does not exist in version"); // this field never appears, so no message about when it appears.
      }

      ////////////////////////////////////////////
      // assert non-editable fields are omitted //
      ////////////////////////////////////////////
      QRecord recordWithoutNonEditableFields = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"firstName": "Tim", "birthDay": "1976-05-28", "createDate": "2023-03-31T11:44:28Z", "id": 256}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1, false);
      assertFalse(recordWithoutNonEditableFields.getValues().containsKey("createDate"));
      assertFalse(recordWithoutNonEditableFields.getValues().containsKey("id"));

      //////////////////////////////////////////////////////////////
      // assert non-editable fields ARE included, if so requested //
      //////////////////////////////////////////////////////////////
      QRecord recordWithoutNonEditablePrimaryKeyFields = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
         {"firstName": "Tim", "birthDay": "1976-05-28", "createDate": "2023-03-31T11:44:28Z", "id": 256}
         """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1, true);
      assertTrue(recordWithoutNonEditablePrimaryKeyFields.getValues().containsKey("createDate"));
      assertEquals(256, recordWithoutNonEditablePrimaryKeyFields.getValues().get("id"));

      try
      {
         TestUtils.TablePersonalizer.register(QContext.getQInstance());

         ////////////////////////////////////////////////////////////////////
         // create date is removed by personalizer - so message is special //
         ////////////////////////////////////////////////////////////////////
         assertThatThrownBy(() -> QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("""
            {"createDate": "2025-01-01T00:00:00Z"}
            """), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1, true, QInputSource.USER))
            .isInstanceOf(QBadRequestException.class)
            .hasMessageContaining("unrecognized field name: createDate")
            .hasMessageContaining("createDate is not allowed for the current user");
      }
      finally
      {
         TestUtils.TablePersonalizer.unregister(QContext.getQInstance());
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordsToApiVersionedQRecordList() throws QException
   {
      QRecord person = TestUtils.getTim2ShoesRecord();

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // QRecord has values corresponding to what's defined in the QInstance (and the underlying backend system) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QRecord pastQRecordRecord = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(List.of(person), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4).get(0);
      assertEquals(2, pastQRecordRecord.getValueInteger("shoeCount")); // old field name - not currently in the QTable, but we can still get its value!
      assertFalse(pastQRecordRecord.getValues().containsKey("noOfShoes")); // current field name - doesn't appear in old api-version
      assertFalse(pastQRecordRecord.getValues().containsKey("cost")); // a current field name, but also not in this old api version
      assertEquals("QUJDRA==", pastQRecordRecord.getValueString("photo")); // base64 version of "ABCD".getBytes()

      QRecord currentQRecord = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(List.of(person), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q1).get(0);
      assertFalse(currentQRecord.getValues().containsKey("shoeCount")); // old field name - not in this current api version
      assertEquals(2, currentQRecord.getValueInteger("noOfShoes")); // current field name - value here as we expect
      assertFalse(currentQRecord.getValues().containsKey("cost")); // future field name - not in the current api (we added the field during new dev, and didn't change the api)

      QRecord futureQRecord = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(List.of(person), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2023_Q2).get(0);
      assertFalse(futureQRecord.getValues().containsKey("shoeCount")); // old field name - also not in this future api version
      assertEquals(2, futureQRecord.getValueInteger("noOfShoes")); // current field name - still here.
      assertEquals(new BigDecimal("3.50"), futureQRecord.getValueBigDecimal("cost")); // future field name appears now that we've requested this future api version.

      for(QRecord specialRecord : List.of(pastQRecordRecord, currentQRecord, futureQRecord))
      {
         assertEquals(LocalDate.parse("1980-05-31"), specialRecord.getValueLocalDate("birthDay")); // use the apiFieldName
         assertFalse(specialRecord.getValues().containsKey("price")); // excluded field never appears
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // confirm that for the alternative api, we get a record that looks just like the input record (per its api meta data) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(String version : List.of(TestUtils.V2022_Q4, TestUtils.V2023_Q1, TestUtils.V2023_Q2))
      {
         QRecord alternativeQRecord = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(List.of(person), TestUtils.TABLE_NAME_PERSON, TestUtils.ALTERNATIVE_API_NAME, version).get(0);
         for(String key : person.getValues().keySet())
         {
            if(key.equals("photo"))
            {
               ////////////////////////////////////////////////////////////////////////////////////////
               // ok, well, skip the blob field (should be base64 version, and is covered elsewhere) //
               ////////////////////////////////////////////////////////////////////////////////////////
               continue;
            }

            assertEquals(person.getValueString(key), ValueUtils.getValueAsString(alternativeQRecord.getValueString(key)));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetValueFromApiFieldInQRecord() throws QException
   {
      QRecord                     record       = new QRecord();
      Map<String, QFieldMetaData> apiFieldsMap = GetTableApiFieldsAction.getTableApiFieldMap(new GetTableApiFieldsAction.ApiNameVersionAndTableName(TestUtils.API_NAME, TestUtils.V2022_Q4, TestUtils.TABLE_NAME_PERSON));
      JSONObject                  apiObject    = new JSONObject(Map.of("shoeCount", 2, "firstName", "Tim"));
      QRecordApiAdapter.setValueFromApiFieldInQRecord(apiObject, "firstName", TestUtils.API_NAME, apiFieldsMap, record, false);
      QRecordApiAdapter.setValueFromApiFieldInQRecord(apiObject, "shoeCount", TestUtils.API_NAME, apiFieldsMap, record, false);
      assertEquals("Tim", record.getValueString("firstName"));
      assertEquals(2, record.getValueInteger("noOfShoes"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordsToApiVersionedQRecordListIncludingExposedJoins() throws QException
   {
      Integer orderId = TestUtils.insert1Order3Lines4LineExtrinsicsAnd1OrderExtrinsic();

      ////////////////////////////////////
      // fetch order with lines w/o api //
      ////////////////////////////////////
      List<QRecord> orderRecordsWithLines = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_ORDER)
            .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_LINE_ITEM).withSelect(true))
            .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, orderId))))
         .getRecords();

      ////////////////////////
      // map to api records //
      ////////////////////////
      QRecordApiAdapterToApiInput qRecordApiAdapterToApiInput = new QRecordApiAdapterToApiInput()
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withApiName(TestUtils.API_NAME)
         .withApiVersion(TestUtils.CURRENT_API_VERSION)
         .withInputRecords(orderRecordsWithLines)
         .withIncludeExposedJoins(true);
      List<QRecord> apiVersionedRecordsWithLines = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(qRecordApiAdapterToApiInput);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert the line item fields came through, and there aren't any extrinsic fields (they weren't fetched //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(apiVersionedRecordsWithLines)
         .allMatch(r -> r.getValueInteger(TestUtils.TABLE_NAME_LINE_ITEM + ".orderId").equals(orderId))
         .allMatch(r -> !r.getValues().containsKey(TestUtils.TABLE_NAME_ORDER_EXTRINSIC + ".orderId"));

      /////////////////////////////////////////////
      // re-do now with extrinsics but not lines //
      /////////////////////////////////////////////
      List<QRecord> orderRecordsWithExtrinsics = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_ORDER)
            .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER_EXTRINSIC).withSelect(true))
            .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, orderId))))
         .getRecords();

      qRecordApiAdapterToApiInput.setInputRecords(orderRecordsWithExtrinsics);
      List<QRecord> apiVersionedRecordsWithExtrinsics = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(qRecordApiAdapterToApiInput);

      assertThat(apiVersionedRecordsWithExtrinsics)
         .allMatch(r -> r.getValueInteger(TestUtils.TABLE_NAME_ORDER_EXTRINSIC + ".orderId").equals(orderId))
         .allMatch(r -> !r.getValues().containsKey(TestUtils.TABLE_NAME_LINE_ITEM + ".orderId"));

      ////////////////////////////////////////////////////////////////
      // make sure the custom value mapper that upshfits values ran //
      ////////////////////////////////////////////////////////////////
      assertThat(apiVersionedRecordsWithExtrinsics)
         .allMatch(r ->
         {
            String keyValue = r.getValueString(TestUtils.TABLE_NAME_ORDER_EXTRINSIC + ".key");
            return (keyValue.toUpperCase().equals(keyValue));
         });

      //////////////////////////////////////////////////
      // finally re-do with both extrinsics and lines //
      //////////////////////////////////////////////////
      List<QRecord> orderRecordsWithExtrinsicsAndLines = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_ORDER)
            .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_LINE_ITEM).withSelect(true))
            .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER_EXTRINSIC).withSelect(true))
            .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, orderId))))
         .getRecords();

      qRecordApiAdapterToApiInput.setInputRecords(orderRecordsWithExtrinsicsAndLines);
      List<QRecord> apiVersionedRecordsWithExtrinsicsAndLines = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(qRecordApiAdapterToApiInput);

      assertThat(apiVersionedRecordsWithExtrinsicsAndLines)
         .allMatch(r -> r.getValueInteger(TestUtils.TABLE_NAME_LINE_ITEM + ".orderId").equals(orderId))
         .allMatch(r -> r.getValueInteger(TestUtils.TABLE_NAME_ORDER_EXTRINSIC + ".orderId").equals(orderId));

      ////////////////////////////////////////////////////////////////////////////////////
      // make sure these records have lineNumber field -                                //
      // then run for an older version - which shouldn't have lineItem.lineNumber field //
      ////////////////////////////////////////////////////////////////////////////////////
      assertThat(apiVersionedRecordsWithExtrinsicsAndLines).allMatch(r -> r.getValue(TestUtils.TABLE_NAME_LINE_ITEM + ".lineNumber") != null);
      List<QRecord> apiVersionedRecordsWithExtrinsicsAndLinesOlderVersion = QRecordApiAdapter.qRecordsToApiVersionedQRecordList(qRecordApiAdapterToApiInput.withApiVersion(TestUtils.V2022_Q4));
      assertThat(apiVersionedRecordsWithExtrinsicsAndLinesOlderVersion).allMatch(r -> r.getValue(TestUtils.TABLE_NAME_LINE_ITEM + ".lineNumber") == null);
   }

}