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

package com.kingsrook.sampleapp;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.templates.RenderTemplateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.templates.RenderTemplateInput;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.templates.TemplateType;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.velocity.exception.ParseErrorException;
import org.jsoup.nodes.Entities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** First-party consumers of the canonical and both static rendering entrypoints.
 *******************************************************************************/
class SampleTemplateRenderingTest
{
   private static final String RESOURCE_DIRECTORY = "/templates/acceptance/";



   /*******************************************************************************
    ** Use the sample application context without starting a database or server.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Resource loading belongs to the consumer; the renderer accepts its UTF-8 text.
    *******************************************************************************/
   @Test
   void testResourceFormsRenderLoopsAndBothConditionBranches() throws Exception
   {
      String template = resource("order.vm");
      assertAllForms("order.vm", template,
         Map.of("customer", "Zoë", "items", List.of("Tea", "Cake"), "paid", true),
         "Order for Zoë\n1: Tea\n2: Cake\nPaid\n");
      assertAllForms("order.vm", template,
         Map.of("customer", "René", "items", List.of("Coffee"), "paid", false),
         "Order for René\n1: Coffee\nPayment due\n");
      assertAllForms("order.vm", template,
         Map.of("customer", "Sam", "items", List.of(), "paid", false),
         "Order for Sam\nPayment due\n");
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStringFormsRenderLoopsAndBothConditionBranches()
   {
      String template = "Hello, ${customer}:#foreach($item in $items) [$item]#end #if($paid)paid#{else}due#end";
      assertAllForms("inline-order", template,
         Map.of("customer", "Zoë", "items", List.of("Tea", "Cake"), "paid", true),
         "Hello, Zoë: [Tea] [Cake] paid");
      assertAllForms("inline-order", template,
         Map.of("customer", "René", "items", List.of("Coffee"), "paid", false),
         "Hello, René: [Coffee] due");
      assertAllForms("inline-order", template,
         Map.of("customer", "Sam", "items", List.of(), "paid", false),
         "Hello, Sam: due");
   }



   /*******************************************************************************
    ** Escaped references and literal blocks must survive with their VTL unevaluated.
    *******************************************************************************/
   @Test
   void testLiteralEscapesAcrossStringAndResourceForms() throws Exception
   {
      String inline = "\\${name}|#[[${name} #if($paid)literal#end]]#|${name}\n";
      for(String template : List.of(inline, resource("literal.vm")))
      {
         assertAllForms("literal.vm", template, Map.of("name", "Zoë", "paid", true),
            "${name}|${name} #if($paid)literal#end|Zoë\n");
      }
   }



   /*******************************************************************************
    ** HTML text encoding is explicit at the caller; raw values are not sanitized.
    *******************************************************************************/
   @Test
   void testCallerEscapedHtmlAndRawDataAcrossForms() throws Exception
   {
      String raw = "<script>alert(\"tea & cake\")</script>";
      Map<String, Object> context = Map.of("safe", Entities.escape(raw), "raw", raw);
      for(String template : List.of("<p>${safe}</p>|<p>${raw}</p>\n", resource("html.vm")))
      {
         assertAllForms("html.vm", template, context,
            "<p>&lt;script&gt;alert(&quot;tea &amp; cake&quot;)&lt;/script&gt;</p>|<p><script>alert(\"tea & cake\")</script></p>\n");
      }
   }



   /*******************************************************************************
    ** A value containing VTL is data, even though it remains unsafe as raw HTML.
    *******************************************************************************/
   @Test
   void testContextDataIsNotReevaluatedAsTemplateCode() throws Exception
   {
      Map<String, Object> context = Map.of("value", "#set($injected = true)${customer}", "customer", "Zoë");
      for(String template : List.of("before:${value}:after|$!injected\n", resource("data.vm")))
      {
         assertAllForms("data.vm", template, context, "before:#set($injected = true)${customer}:after|\n");
      }
   }



   /*******************************************************************************
    ** Optional context and absent keys follow Velocity's literal and quiet semantics.
    *******************************************************************************/
   @Test
   void testMissingContextAndValuesAcrossForms() throws Exception
   {
      String inline = "Named: ${customer}|Quiet: $!customer|#if($customer)present#{else}absent#end\n";
      for(String template : List.of(inline, resource("context.vm")))
      {
         assertAllForms("context.vm", template, Map.of("customer", "Zoë"), "Named: Zoë|Quiet: Zoë|present\n");
         assertAllForms("context.vm", template, Map.of(), "Named: ${customer}|Quiet: |absent\n");
         assertAllForms("context.vm", template, null, "Named: ${customer}|Quiet: |absent\n");
      }
   }



   /*******************************************************************************
    ** Code is required; an intentionally empty or whitespace-only template is valid.
    *******************************************************************************/
   @Test
   void testMissingCodeRejectedAndEmptyCodeAccepted()
   {
      assertAll(
         () -> assertThrows(NullPointerException.class, () -> new RenderTemplateAction().execute(new RenderTemplateInput()
            .withTemplateType(TemplateType.VELOCITY).withContext(Map.of()))),
         () -> assertThrows(NullPointerException.class, () -> RenderTemplateAction.render(TemplateType.VELOCITY, Map.of(), null)),
         () -> assertThrows(NullPointerException.class, () -> RenderTemplateAction.renderVelocity(Map.of(), null)));
      assertAllForms("empty", "", null, "");
      assertAllForms("whitespace", " \n", null, " \n");
   }



   /*******************************************************************************
    ** Resource lookup fails at the consumer boundary, before invoking the renderer.
    *******************************************************************************/
   @Test
   void testMissingResourceIsRejected()
   {
      FileNotFoundException exception = assertThrows(FileNotFoundException.class, () -> resource("does-not-exist.vm"));
      assertEquals(RESOURCE_DIRECTORY + "does-not-exist.vm", exception.getMessage());
   }



   /*******************************************************************************
    ** Malformed code is rejected consistently, and does not poison the next render.
    *******************************************************************************/
   @Test
   void testSyntaxErrorsAcrossStringAndResourceForms() throws Exception
   {
      for(String code : List.of("#if($paid)Paid\n", resource("invalid.vm")))
      {
         ParseErrorException exception = assertThrows(ParseErrorException.class, () -> new RenderTemplateAction().execute(new RenderTemplateInput()
            .withTemplateIdentifier("invalid.vm").withTemplateType(TemplateType.VELOCITY)
            .withCode(code).withContext(Map.of("paid", true))));
         assertTrue(exception.getMessage().contains("invalid.vm"), exception.getMessage());
         assertThrows(ParseErrorException.class, () -> RenderTemplateAction.render(TemplateType.VELOCITY, Map.of("paid", true), code));
         assertThrows(ParseErrorException.class, () -> RenderTemplateAction.renderVelocity(Map.of("paid", true), code));
      }
      assertAllForms("after-invalid", "Hello, ${customer}", Map.of("customer", "Zoë"), "Hello, Zoë");
   }



   /*******************************************************************************
    ** Only the explicit-type entrypoints can omit their required template language.
    *******************************************************************************/
   @Test
   void testMissingTemplateTypeIsRejected()
   {
      QException canonical = assertThrows(QException.class, () -> new RenderTemplateAction().execute(new RenderTemplateInput()
         .withCode("Hello, ${customer}").withContext(Map.of("customer", "Zoë"))));
      assertEquals("Unsupported Template Type: null", canonical.getMessage());
      QException convenient = assertThrows(QException.class, () -> RenderTemplateAction.render(null, Map.of(), "Hello"));
      assertEquals("Unsupported Template Type: null", convenient.getMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String resource(String name) throws IOException
   {
      try(InputStream input = getClass().getResourceAsStream(RESOURCE_DIRECTORY + name))
      {
         if(input == null)
         {
            throw (new FileNotFoundException(RESOURCE_DIRECTORY + name));
         }
         return (new String(input.readAllBytes(), StandardCharsets.UTF_8));
      }
   }



   /*******************************************************************************
    ** Each API must match the literal oracle, independently of the other APIs.
    *******************************************************************************/
   private void assertAllForms(String identifier, String code, Map<String, Object> context, String expected)
   {
      assertAll(
         () -> assertEquals(expected, new RenderTemplateAction().execute(new RenderTemplateInput()
            .withTemplateIdentifier(identifier).withTemplateType(TemplateType.VELOCITY)
            .withCode(code).withContext(context)).getResult(), "canonical"),
         () -> assertEquals(expected, RenderTemplateAction.render(TemplateType.VELOCITY, context, code), "typed convenience"),
         () -> assertEquals(expected, RenderTemplateAction.renderVelocity(context, code), "Velocity convenience"));
   }
}
