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

package com.kingsrook.qqq.backend.core.model.data;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static com.kingsrook.qqq.backend.core.model.data.QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS;
import static com.kingsrook.qqq.backend.core.model.data.QRecord.BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QRecord 
 *******************************************************************************/
class QRecordTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCopyConstructor()
   {
      String jsonValue = """
         {"key": [1,2]}
         """;
      Map<String, Integer> fieldLengths = MapBuilder.of("a", 1, "b", 2);

      QRecord original = new QRecord()
         .withTableName("myTable")
         .withRecordLabel("My Record")
         .withValue("one", 1)
         .withValue("two", "two")
         .withValue("three", new BigDecimal("3"))
         .withValue("false", false)
         .withValue("empty", null)
         .withDisplayValue("three", "3.00")
         .withBackendDetail(BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT, jsonValue)
         .withBackendDetail(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, new HashMap<>(fieldLengths))
         .withError(new BadInputStatusMessage("Bad Input"))
         .withAssociatedRecord("child", new QRecord().withValue("id", "child1"))
         .withAssociatedRecord("child", new QRecord().withValue("id", "child2"))
         .withAssociatedRecord("nephew", new QRecord().withValue("id", "nephew1"));

      QRecord clone = new QRecord(original);

      //////////////////////////////////////////////////////////////
      // assert equality on all the members values in the records //
      //////////////////////////////////////////////////////////////
      assertEquals("myTable", clone.getTableName());
      assertEquals("My Record", clone.getRecordLabel());
      assertEquals(1, clone.getValue("one"));
      assertEquals("two", clone.getValue("two"));
      assertEquals(new BigDecimal("3"), clone.getValue("three"));
      assertEquals(false, clone.getValue("false"));
      assertNull(clone.getValue("empty"));
      assertEquals("3.00", clone.getDisplayValue("three"));
      assertEquals(jsonValue, clone.getBackendDetail(BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT));
      assertEquals(fieldLengths, clone.getBackendDetail(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS));
      assertEquals(1, clone.getErrors().size());
      assertEquals(BadInputStatusMessage.class, clone.getErrors().get(0).getClass());
      assertEquals("Bad Input", clone.getErrors().get(0).getMessage());
      assertEquals(0, clone.getWarnings().size());
      assertEquals(2, clone.getAssociatedRecords().size());
      assertEquals(2, clone.getAssociatedRecords().get("child").size());
      assertEquals("child1", clone.getAssociatedRecords().get("child").get(0).getValue("id"));
      assertEquals("child2", clone.getAssociatedRecords().get("child").get(1).getValue("id"));
      assertEquals(1, clone.getAssociatedRecords().get("nephew").size());
      assertEquals("nephew1", clone.getAssociatedRecords().get("nephew").get(0).getValue("id"));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure the associated record data structures are not the same (e.g., not the same map & lists) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      assertNotSame(clone.getAssociatedRecords(), original.getAssociatedRecords());
      assertNotSame(clone.getAssociatedRecords().get("child"), original.getAssociatedRecords().get("child"));

      /////////////////////////////////////////////////////////////////////////////////////
      // but we'll be okay with the same records inside the associated records structure //
      /////////////////////////////////////////////////////////////////////////////////////
      assertSame(clone.getAssociatedRecords().get("child").get(0), original.getAssociatedRecords().get("child").get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCopyConstructorEdgeCases()
   {
      QRecord nullValuesRecord = new QRecord();
      nullValuesRecord.setValues(null);
      assertNull(new QRecord(nullValuesRecord).getValues());

      QRecord nullDisplayValuesRecord = new QRecord();
      nullDisplayValuesRecord.setDisplayValues(null);
      assertNull(new QRecord(nullDisplayValuesRecord).getDisplayValues());

      QRecord nullBackendDetailsRecord = new QRecord();
      nullBackendDetailsRecord.setBackendDetails(null);
      assertNull(new QRecord(nullBackendDetailsRecord).getBackendDetails());

      QRecord nullAssociations = new QRecord();
      nullAssociations.setAssociatedRecords(null);
      assertNull(new QRecord(nullAssociations).getAssociatedRecords());

      QRecord nullErrors = new QRecord();
      nullErrors.setErrors(null);
      assertNull(new QRecord(nullErrors).getErrors());

      QRecord nullWarnings = new QRecord();
      nullWarnings.setWarnings(null);
      assertNull(new QRecord(nullWarnings).getWarnings());

      QRecord byteArrayValue = new QRecord().withValue("myBytes", new byte[] { 65, 66, 67, 68 });
      assertArrayEquals(new byte[] { 65, 66, 67, 68 }, new QRecord(byteArrayValue).getValueByteArray("myBytes"));

      ////////////////////////////////////////////
      // qrecord as a value inside another (!?) //
      ////////////////////////////////////////////
      QRecord nestedQRecordValue     = new QRecord().withValue("myRecord", new QRecord().withValue("A", 1));
      QRecord cloneWithNestedQRecord = new QRecord(nestedQRecordValue);
      assertEquals(1, ((QRecord) cloneWithNestedQRecord.getValue("myRecord")).getValueInteger("A"));
      assertNotSame(cloneWithNestedQRecord.getValue("myRecord"), nestedQRecordValue.getValue("myRecord"));

      QRecord emptyRecord = new QRecord();
      QRecord emptyClone  = new QRecord(emptyRecord);
      assertNull(emptyClone.getTableName());
      assertNull(emptyClone.getRecordLabel());
      assertEquals(0, emptyClone.getValues().size());
      assertEquals(0, emptyClone.getDisplayValues().size());
      assertEquals(0, emptyClone.getBackendDetails().size());
      assertEquals(0, emptyClone.getErrors().size());
      assertEquals(0, emptyClone.getWarnings().size());
      assertEquals(0, emptyClone.getAssociatedRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testArrayListAsValue()
   {
      ArrayList<Integer> originalArrayList        = new ArrayList<>(List.of(1, 2, 3));
      QRecord            recordWithArrayListValue = new QRecord().withValue("myList", originalArrayList);
      QRecord            cloneWithArrayListValue  = new QRecord(recordWithArrayListValue);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // the clone list and original list should be equals (have contents that are equals), but not be the same (reference) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(List.of(1, 2, 3), cloneWithArrayListValue.getValue("myList"));
      assertNotSame(originalArrayList, cloneWithArrayListValue.getValue("myList"));

      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure a change to the original list doesn't change the cloned list (as it was cloned deeply) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      originalArrayList.add(4);
      assertNotEquals(originalArrayList, cloneWithArrayListValue.getValue("myList"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLinkedListAsValue()
   {
      LinkedList<Integer> originalLinkedList        = new LinkedList<>(List.of(1, 2, 3));
      QRecord             recordWithLinkedListValue = new QRecord().withValue("myList", originalLinkedList);
      QRecord             cloneWithLinkedListValue  = new QRecord(recordWithLinkedListValue);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // the clone list and original list should be equals (have contents that are equals), but not be the same (reference) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(List.of(1, 2, 3), cloneWithLinkedListValue.getValue("myList"));
      assertNotSame(originalLinkedList, cloneWithLinkedListValue.getValue("myList"));

      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure a change to the original list doesn't change the cloned list (as it was cloned deeply) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      originalLinkedList.add(4);
      assertNotEquals(originalLinkedList, cloneWithLinkedListValue.getValue("myList"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMapAsValue()
   {
      LinkedHashMap<String, Integer> originalMap        = new LinkedHashMap<>(Map.of("one", 1, "two", 2, "three", 3));
      QRecord                        recordWithMapValue = new QRecord().withValue("myMap", originalMap);
      QRecord                        cloneWithMapValue  = new QRecord(recordWithMapValue);

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // the clone map and original map should be equals (have contents that are equals), but not be the same (reference) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(originalMap, cloneWithMapValue.getValue("myMap"));
      assertNotSame(originalMap, cloneWithMapValue.getValue("myMap"));

      //////////////////////////////////////////////////////////
      // make sure we re-created it as the same subtype (LHM) //
      //////////////////////////////////////////////////////////
      assertThat(cloneWithMapValue.getValue("myMap")).isInstanceOf(LinkedHashMap.class);

      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure a change to the original list doesn't change the cloned list (as it was cloned deeply) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      originalMap.put("four", 4);
      assertNotEquals(originalMap, cloneWithMapValue.getValue("myMap"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetErrorsAndWarningsAsString()
   {
      assertEquals("", new QRecord().getErrorsAsString());
      assertEquals("one", new QRecord()
         .withError(new BadInputStatusMessage("one"))
         .getErrorsAsString());
      assertEquals("one; two", new QRecord()
         .withError(new BadInputStatusMessage("one"))
         .withError(new SystemErrorStatusMessage("two"))
         .getErrorsAsString());

      assertEquals("", new QRecord().getWarningsAsString());
      assertEquals("A", new QRecord()
         .withWarning(new QWarningMessage("A"))
         .getWarningsAsString());
      assertEquals("A; B; C", new QRecord()
         .withWarning(new QWarningMessage("A"))
         .withWarning(new QWarningMessage("B"))
         .withWarning(new QWarningMessage("C"))
         .getWarningsAsString());

      ///////////////////////////////////////////////////////////////////////////////////
      // make sure this AsString method doesn't get included in our json serialization //
      ///////////////////////////////////////////////////////////////////////////////////
      String json = JsonUtils.toJson(new QRecord()
         .withError(new BadInputStatusMessage("one")));
      JSONObject jsonObject = new JSONObject(json);
      assertFalse(jsonObject.has("errorsAsString"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAddJoinedRecordValues()
   {
      QRecord order = new QRecord().withValue("id", 1).withValue("shipTo", "St. Louis");
      order.addJoinedRecordValues("orderInstructions", null);
      assertEquals(2, order.getValues().size());

      QRecord orderInstructions = new QRecord().withValue("id", 100).withValue("instructions", "Be Careful");
      order.addJoinedRecordValues("orderInstructions", orderInstructions);

      assertEquals(4, order.getValues().size());
      assertEquals(100, order.getValue("orderInstructions.id"));
      assertEquals("Be Careful", order.getValue("orderInstructions.instructions"));
   }



   /*******************************************************************************
    ** Raw field values distinguish explicit null/empty content from absent fields.
    *******************************************************************************/
   @Test
   void testJsonPreservesExplicitEmptyValues()
   {
      QRecord record = new QRecord().withValue("nullValue", null).withValue("emptyText", "")
         .withValue("emptyArray", new ArrayList<>()).withValue("falseValue", false).withValue("zero", 0);
      JSONObject values = JsonUtils.toJSONObject(JsonUtils.toJson(record)).getJSONObject("values");
      assertTrue(values.has("nullValue"));
      assertTrue(values.isNull("nullValue"));
      assertEquals("", values.getString("emptyText"));
      assertTrue(values.getJSONArray("emptyArray").isEmpty());
      assertFalse(values.getBoolean("falseValue"));
      assertEquals(0, values.getInt("zero"));
      assertFalse(values.has("absent"));
      assertTrue(JsonUtils.toJSONObject(JsonUtils.toJson(new QRecord())).getJSONObject("values").isEmpty());
      assertFalse(JsonUtils.toJSONObject(JsonUtils.toJson(new QRecord())).has("errors"));
   }



   /*******************************************************************************
    ** Raw provider payload stays internal, including when records are nested and
    ** a caller explicitly enables otherwise omitted JSON properties.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testJsonOmitsRawProviderPayloadButPreservesOtherBackendDetails(boolean includeAlways)
   {
      String parentPayload = "{\"password\":\"raw-parent-secret\",\"unmapped\":\"private-parent-data\"}";
      String childPayload = "{\"password\":\"raw-child-secret\"}";
      QRecord child = new QRecord().withValue("id", 2).withValue("password", "************")
         .withBackendDetail(BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT, childPayload)
         .withBackendDetail(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, new HashMap<>(Map.of("attachment", 23)))
         .withBackendDetail("providerReceipt", "safe-child-receipt");
      QRecord parent = new QRecord().withValue("id", 1).withValue("password", "************")
         .withBackendDetail(BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT, parentPayload)
         .withBackendDetail(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, new HashMap<>(Map.of("attachment", 17)))
         .withBackendDetail("providerReceipt", "safe-parent-receipt")
         .withAssociatedRecord("arbitraryChildren", child);
      Map<String, Serializable> parentDetails = new LinkedHashMap<>(parent.getBackendDetails());
      Map<String, Serializable> childDetails = new LinkedHashMap<>(child.getBackendDetails());
      QRecord copy = new QRecord(parent);

      String json = includeAlways
         ? JsonUtils.toJsonCustomized(parent, builder -> builder.serializationInclusion(JsonInclude.Include.ALWAYS))
         : JsonUtils.toJson(parent);
      JSONObject output = new JSONObject(json);
      JSONObject childOutput = output.getJSONObject("associatedRecords").getJSONArray("arbitraryChildren").getJSONObject(0);
      JSONObject publicParentDetails = output.getJSONObject("backendDetails");
      JSONObject publicChildDetails = childOutput.getJSONObject("backendDetails");
      assertAll(
         () -> assertEquals(Set.of(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, "providerReceipt"), publicParentDetails.keySet()),
         () -> assertEquals(Set.of(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, "providerReceipt"), publicChildDetails.keySet()),
         () -> assertFalse(json.contains("raw-parent-secret")),
         () -> assertFalse(json.contains("private-parent-data")),
         () -> assertFalse(json.contains("raw-child-secret")),
         () -> assertEquals(17, publicParentDetails.getJSONObject(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS).getInt("attachment")),
         () -> assertEquals(23, publicChildDetails.getJSONObject(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS).getInt("attachment")),
         () -> assertEquals("safe-parent-receipt", publicParentDetails.getString("providerReceipt")),
         () -> assertEquals("safe-child-receipt", publicChildDetails.getString("providerReceipt")),
         () -> assertEquals("************", output.getJSONObject("values").getString("password")),
         () -> assertEquals("************", childOutput.getJSONObject("values").getString("password")),
         () -> assertEquals(parentDetails, parent.getBackendDetails()),
         () -> assertEquals(childDetails, child.getBackendDetails()),
         () -> assertEquals(parentDetails, copy.getBackendDetails()),
         () -> assertEquals(childDetails, copy.getAssociatedRecords().get("arbitraryChildren").get(0).getBackendDetails()),
         () -> assertEquals(parentPayload, parent.getBackendDetailString(BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT)),
         () -> assertEquals(childPayload, child.getBackendDetailString(BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT)));
   }



   /*******************************************************************************
    ** Serialization privacy must not remove the existing internal import contract.
    *******************************************************************************/
   @Test
   void testJsonDeserializationRetainsInternalProviderPayload() throws Exception
   {
      String rawPayload = "{\"sourceKey\":\"internal-provider-value\"}";
      JSONObject input = new JSONObject().put("values", new JSONObject().put("id", 1))
         .put("backendDetails", new JSONObject()
            .put(BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT, rawPayload)
            .put(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, new JSONObject().put("attachment", 17))
            .put("providerReceipt", "safe-receipt"));
      QRecord record = JsonUtils.toObject(input.toString(), QRecord.class);
      assertEquals(1, record.getValueInteger("id"));
      assertEquals(rawPayload, record.getBackendDetailString(BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT));
      assertEquals(Map.of("attachment", 17), record.getBackendDetail(BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS));
      assertEquals("safe-receipt", record.getBackendDetailString("providerReceipt"));
      assertEquals(record.getBackendDetails(), new QRecord(record).getBackendDetails());
   }
}
