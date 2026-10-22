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

package com.kingsrook.qqq.backend.core.model.actions.messaging;


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Optional messaging fields preserve null defaults and fluent bean conventions.
 *******************************************************************************/
class SendMessageModelsTest
{
   /*******************************************************************************
    ** All new input fields default to null and support fluent and bean setters.
    *******************************************************************************/
   @Test
   void testInputFields()
   {
      SendMessageInput input = new SendMessageInput();
      Map<String, Serializable> variables = new HashMap<>(Map.of("name", "Ada"));
      assertNull(input.getTemplateName());
      assertSame(input, input.withTemplateName("welcome"));
      assertEquals("welcome", input.getTemplateName());
      input.setTemplateName(null);
      assertNull(input.getTemplateName());
      input.setTemplateName("welcome");
      assertEquals("welcome", input.getTemplateName());

      assertNull(input.getVariables());
      assertSame(input, input.withVariables(variables));
      assertEquals(variables, input.getVariables());
      input.setVariables(null);
      assertNull(input.getVariables());
      input.setVariables(variables);
      assertEquals(variables, input.getVariables());

      assertNull(input.getIdempotencyKey());
      assertSame(input, input.withIdempotencyKey("order:42"));
      assertEquals("order:42", input.getIdempotencyKey());
      input.setIdempotencyKey(null);
      assertNull(input.getIdempotencyKey());
      input.setIdempotencyKey("order:42");
      assertEquals("order:42", input.getIdempotencyKey());

      assertNull(input.getCategory());
      assertSame(input, input.withCategory("TRANSACTIONAL"));
      assertEquals("TRANSACTIONAL", input.getCategory());
      input.setCategory(null);
      assertNull(input.getCategory());
      input.setCategory("TRANSACTIONAL");
      assertEquals("TRANSACTIONAL", input.getCategory());

      assertNull(input.getLinkedTableName());
      assertSame(input, input.withLinkedTableName("order"));
      assertEquals("order", input.getLinkedTableName());
      input.setLinkedTableName(null);
      assertNull(input.getLinkedTableName());
      input.setLinkedTableName("order");
      assertEquals("order", input.getLinkedTableName());

      assertNull(input.getLinkedRecordId());
      assertSame(input, input.withLinkedRecordId(42L));
      assertEquals(42L, input.getLinkedRecordId());
      input.setLinkedRecordId(null);
      assertNull(input.getLinkedRecordId());
      input.setLinkedRecordId(42L);
      assertEquals(42L, input.getLinkedRecordId());

      assertNull(input.getSenderName());
      assertSame(input, input.withSenderName("qbit-orders"));
      assertEquals("qbit-orders", input.getSenderName());
      input.setSenderName(null);
      assertNull(input.getSenderName());
      input.setSenderName("qbit-orders");
      assertEquals("qbit-orders", input.getSenderName());

      assertNull(input.getLocale());
      assertSame(input, input.withLocale("en-US"));
      assertEquals("en-US", input.getLocale());
      input.setLocale(null);
      assertNull(input.getLocale());
      input.setLocale("en-US");
      assertEquals("en-US", input.getLocale());

      assertSame(variables, input.getVariables());
      assertSame(input, input.withVariable("count", 2));
      assertEquals(2, input.getVariables().get("count"));
      assertSame(input, input.withVariable("count", 3));
      assertEquals(3, input.getVariables().get("count"));
      input.setVariables(null);
      assertSame(input, input.withVariable("name", "Grace"));
      assertEquals(Map.of("name", "Grace"), input.getVariables());
      assertSame(input, input.withLinkedRecord("invoice", "INV-7"));
      assertEquals("invoice", input.getLinkedTableName());
      assertEquals("INV-7", input.getLinkedRecordId());
      input.withLinkedRecord(null, null);
      assertNull(input.getLinkedTableName());
      assertNull(input.getLinkedRecordId());

      Party to = new MultiParty().withParty(new Party().withAddress("to@example.com"));
      Party from = new Party().withAddress("from@example.com");
      assertSame(input, input.withTo(to).withFrom(from));
      assertSame(to, input.getTo());
      assertSame(from, input.getFrom());
   }



   /*******************************************************************************
    ** Output fields are optional and preserve provider identifiers and explanations.
    *******************************************************************************/
   @Test
   void testOutputFields()
   {
      SendMessageOutput output = new SendMessageOutput();
      List<String> reasons = List.of("Recipient opted out");
      assertNull(output.getMessageId());
      assertSame(output, output.withMessageId(42L));
      assertEquals(42L, output.getMessageId());
      output.setMessageId(null);
      assertNull(output.getMessageId());
      output.setMessageId(42L);
      assertEquals(42L, output.getMessageId());

      assertNull(output.getStatus());
      assertSame(output, output.withStatus(SendMessageStatus.SUPPRESSED));
      assertEquals(SendMessageStatus.SUPPRESSED, output.getStatus());
      output.setStatus(null);
      assertNull(output.getStatus());
      output.setStatus(SendMessageStatus.SUPPRESSED);
      assertEquals(SendMessageStatus.SUPPRESSED, output.getStatus());

      assertNull(output.getReasons());
      assertSame(output, output.withReasons(reasons));
      assertEquals(reasons, output.getReasons());
      output.setReasons(null);
      assertNull(output.getReasons());
      output.setReasons(reasons);
      assertEquals(reasons, output.getReasons());

      assertSame(reasons, output.getReasons());
      output.withMessageId("provider-id");
      assertEquals("provider-id", output.getMessageId());
   }
}
