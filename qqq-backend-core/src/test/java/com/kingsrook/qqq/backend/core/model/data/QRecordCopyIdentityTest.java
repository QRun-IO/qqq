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

package com.kingsrook.qqq.backend.core.model.data;


import java.util.List;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Copy tracking identifies in-process origins independently of values and keys.
 ** It supplies no serializable caller-controlled authority over native identity.
 *******************************************************************************/
class QRecordCopyIdentityTest
{
   /*******************************************************************************
    ** Once requested, a token follows direct and transitive QRecord copies.
    *******************************************************************************/
   @Test
   void testTrackedCopiesRetainStableOrigin()
   {
      QRecord original = new QRecord().withValue("name", "Original");
      Object token = original.trackCopies();
      QRecord copy = new QRecord(original);
      QRecord secondCopy = new QRecord(copy);
      secondCopy.setValue("name", "Changed copy");

      assertAll(
         () -> assertSame(token, original.trackCopies()),
         () -> assertSame(token, copy.trackCopies()),
         () -> assertSame(token, secondCopy.trackCopies()),
         () -> assertNotSame(token, new QRecord().trackCopies()),
         () -> assertEquals("Original", original.getValueString("name")),
         () -> assertEquals("Changed copy", secondCopy.getValueString("name")));
   }



   /*******************************************************************************
    ** An ordinary untracked copy must not implicitly start shared tracking.
    *******************************************************************************/
   @Test
   void testCopiesMadeBeforeTrackingHaveIndependentOrigins()
   {
      QRecord original = new QRecord().withValue("name", "Same values");
      QRecord untrackedCopy = new QRecord(original);
      Object originalToken = original.trackCopies();
      Object copyToken = untrackedCopy.trackCopies();

      assertAll(
         () -> assertNotSame(originalToken, copyToken),
         () -> assertSame(originalToken, new QRecord(original).trackCopies()),
         () -> assertSame(copyToken, new QRecord(untrackedCopy).trackCopies()));
   }



   /*******************************************************************************
    ** Identical nullable drafts stay distinct after immutable clone reordering.
    *******************************************************************************/
   @Test
   void testIdenticalDraftsKeepTheirOwnOriginsWhenReordered()
   {
      QRecord first = new QRecord().withValue("key", null).withValue("name", "Identical");
      QRecord second = new QRecord().withValue("key", null).withValue("name", "Identical");
      Object firstToken = first.trackCopies();
      Object secondToken = second.trackCopies();
      List<QRecord> reordered = List.of(new QRecord(second), new QRecord(first));

      assertAll(
         () -> assertEquals(first.getValues(), second.getValues()),
         () -> assertNotSame(firstToken, secondToken),
         () -> assertSame(secondToken, reordered.get(0).trackCopies()),
         () -> assertSame(firstToken, reordered.get(1).trackCopies()),
         () -> assertSame(secondToken, new QRecord(reordered.get(0)).trackCopies()));
   }



   /*******************************************************************************
    ** The ordinary values map can itself have a field named copyIdentity. Tracking
    ** must not change or read it, nor create an extra backend/public property.
    *******************************************************************************/
   @Test
   void testTrackingLeavesPublicRecordUnchanged() throws Exception
   {
      QRecord record = new QRecord().withValue("copyIdentity", "Caller field")
         .withValue("empty", null).withValue("enabled", false).withValue("count", 0)
         .withDisplayValue("count", "Zero");
      record.setRecordLabel("Public label");
      record.getBackendDetails().put("publicDetail", "Existing value");
      String before = JsonUtils.toJson(record);
      Object token = record.trackCopies();
      QRecord copy = new QRecord(record);

      assertAll(
         () -> assertNotEquals("Caller field", token),
         () -> assertSame(token, copy.trackCopies()),
         () -> assertEquals(before, JsonUtils.toJson(record)),
         () -> assertEquals(before, JsonUtils.toJson(copy)),
         () -> assertEquals("Caller field", record.getValueString("copyIdentity")),
         () -> assertEquals(record.getBackendDetails(), copy.getBackendDetails()),
         () -> assertEquals(record.getDisplayValues(), copy.getDisplayValues()));
   }



   /*******************************************************************************
    ** Opaque Object tokens cannot be serialized; both public round trips must
    ** omit them and start independent tracking only when explicitly requested.
    *******************************************************************************/
   @Test
   void testCopyIdentityDoesNotSurviveSerialization() throws Exception
   {
      QRecord original = new QRecord().withValue("name", "Visible");
      Object token = original.trackCopies();
      String json = JsonUtils.toJson(original);
      QRecord javaCopy = SerializationUtils.clone(original);
      QRecord jsonCopy = JsonUtils.toObject(json, QRecord.class);
      Object javaToken = javaCopy.trackCopies();
      Object jsonToken = jsonCopy.trackCopies();

      assertAll(
         () -> assertFalse(json.contains("copyIdentity")),
         () -> assertNotSame(token, javaToken),
         () -> assertNotSame(token, jsonToken),
         () -> assertNotSame(javaToken, jsonToken),
         () -> assertSame(javaToken, new QRecord(javaCopy).trackCopies()),
         () -> assertSame(jsonToken, new QRecord(jsonCopy).trackCopies()),
         () -> assertEquals(json, JsonUtils.toJson(javaCopy)),
         () -> assertEquals(json, JsonUtils.toJson(jsonCopy)));
   }



   /*******************************************************************************
    ** Default strict input mapping still rejects the private property; an
    ** explicitly lenient mapper cannot install the supplied token either.
    *******************************************************************************/
   @Test
   void testJsonCannotInjectCopyIdentity() throws Exception
   {
      String json = """
         {"values":{"name":"Visible"},"copyIdentity":"attacker-token"}
         """;
      assertThrows(UnrecognizedPropertyException.class, () -> JsonUtils.toObject(json, QRecord.class));
      QRecord first = JsonUtils.toObject(json, QRecord.class, mapper -> mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
      QRecord second = JsonUtils.toObject(json, QRecord.class, mapper -> mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
      Object firstToken = first.trackCopies();
      Object secondToken = second.trackCopies();

      assertAll(
         () -> assertNotEquals("attacker-token", firstToken),
         () -> assertNotEquals("attacker-token", secondToken),
         () -> assertNotSame(firstToken, secondToken),
         () -> assertFalse(JsonUtils.toJson(first).contains("copyIdentity")),
         () -> assertFalse(JsonUtils.toJson(first).contains("attacker-token")),
         () -> assertEquals("Visible", first.getValueString("name")),
         () -> assertNull(first.getValue("copyIdentity")));
   }



   /*******************************************************************************
    ** Origin tracking does not grant permission to retarget a captured native key.
    *******************************************************************************/
   @Test
   void testCopyOriginDoesNotOverridePrimaryKeyGuard() throws Exception
   {
      QTableMetaData table = new QTableMetaData().withName("copyIdentityTable").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      QRecord original = new QRecord().withValue("id", 11);
      original.capturePrimaryKey(table, 11);
      Object token = original.trackCopies();
      QRecord copy = new QRecord(original);
      copy.getValues().put("id", 12);

      assertAll(
         () -> assertSame(token, copy.trackCopies()),
         () -> assertEquals(11, original.resolvePrimaryKey(table)),
         () -> assertThrows(QException.class, () -> copy.resolvePrimaryKey(table)),
         () -> assertEquals(12, copy.getValueInteger("id")));
   }



   /*******************************************************************************
    ** Separate top-level inputs may share an earlier lineage. A new operation
    ** starts each root independently without changing previously created copies.
    *******************************************************************************/
   @Test
   void testRestartTrackingSeparatesSiblingRoots()
   {
      QRecord previousRoot = new QRecord().withValue("name", "Identical");
      Object previousToken = previousRoot.trackCopies();
      QRecord first = new QRecord(previousRoot);
      QRecord second = new QRecord(previousRoot);
      assertSame(previousToken, first.trackCopies());
      assertSame(previousToken, second.trackCopies());

      first.startCopyTracking();
      Object firstToken = first.trackCopies();
      QRecord firstCopy = new QRecord(first);
      second.startCopyTracking();
      Object secondToken = second.trackCopies();
      QRecord secondCopy = new QRecord(second);

      assertAll(
         () -> assertSame(previousToken, previousRoot.trackCopies()),
         () -> assertNotSame(previousToken, firstToken),
         () -> assertNotSame(previousToken, secondToken),
         () -> assertNotSame(firstToken, secondToken),
         () -> assertSame(firstToken, first.trackCopies()),
         () -> assertSame(secondToken, second.trackCopies()),
         () -> assertSame(firstToken, firstCopy.trackCopies()),
         () -> assertSame(secondToken, secondCopy.trackCopies()),
         () -> assertSame(firstToken, new QRecord(firstCopy).trackCopies()),
         () -> assertSame(secondToken, new QRecord(secondCopy).trackCopies()),
         () -> assertEquals(previousRoot.getValues(), first.getValues()),
         () -> assertEquals(previousRoot.getValues(), second.getValues()));

      first.startCopyTracking();
      assertAll(
         () -> assertNotSame(firstToken, first.trackCopies()),
         () -> assertSame(firstToken, firstCopy.trackCopies()),
         () -> assertSame(secondToken, second.trackCopies()),
         () -> assertSame(first.trackCopies(), new QRecord(first).trackCopies()));
   }
}
