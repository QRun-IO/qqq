/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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


import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.openapi.model.Components;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Info;
import com.kingsrook.qqq.openapi.model.Method;
import com.kingsrook.qqq.openapi.model.OpenAPI;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.SecuritySchemeType;
import com.kingsrook.qqq.openapi.model.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Sample consumer contracts for the legacy models, without schema validation.
 *******************************************************************************/
class SampleOpenApiModelsAcceptanceTest
{
   @TempDir
   Path classes;



   /*******************************************************************************
    ** Assert the complete wire shape, including references and operation security.
    *******************************************************************************/
   @Test
   void testDocumentSerialization() throws Exception
   {
      Schema person = new Schema().withType(Type.OBJECT).withProperties(Map.of(
         "id", new Schema().withType(Type.INTEGER).withFormat("int64"),
         "name", new Schema().withType(Type.STRING).withEnumValues(List.of("Ada", "Grace"))));
      Method getPerson = new Method().withOperationId("getPerson")
         .withParameters(List.of(new Parameter().withName("id").withIn(In.PATH).withRequired(true)
            .withSchema(new Schema().withType(Type.INTEGER).withFormat("int64"))))
         .withResponses(Map.of(200, new Response().withDescription("Person found")
            .withContent(Map.of("application/json", new Content()
               .withSchema(new Schema().withRef("#/components/schemas/Person"))))))
         .withSecurity(List.of(Map.of("BearerAuth", List.of())));
      OpenAPI document = new OpenAPI()
         .withInfo(new Info().withTitle("Sample people").withVersion("1.0"))
         .withPaths(Map.of("/people/{id}", new com.kingsrook.qqq.openapi.model.Path().withGet(getPerson)))
         .withComponents(new Components().withSchemas(Map.of("Person", person))
            .withSecuritySchemes(Map.of("BearerAuth", new SecurityScheme().withType(SecuritySchemeType.HTTP)
               .withScheme("bearer").withBearerFormat("JWT"))));

      assertJson("""
         {
           "openapi": "3.0.3",
           "info": {"title": "Sample people", "version": "1.0"},
           "paths": {
             "/people/{id}": {"get": {
               "operationId": "getPerson",
               "parameters": [{"name": "id", "in": "path", "required": true,
                 "schema": {"type": "integer", "format": "int64"}}],
               "responses": {"200": {"description": "Person found", "content": {
                 "application/json": {"schema": {"$ref": "#/components/schemas/Person"}}}}},
               "security": [{"BearerAuth": []}]
             }}
           },
           "components": {
             "schemas": {"Person": {"type": "object", "properties": {
               "id": {"type": "integer", "format": "int64"},
               "name": {"type": "string", "enum": ["Ada", "Grace"]}
             }}},
             "securitySchemes": {"BearerAuth": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}}
           }
         }
         """, document);
   }



   /*******************************************************************************
    ** Default NON_EMPTY serialization loses a security requirement's empty scopes.
    *******************************************************************************/
   @Test
   void testDefaultSerializerDropsEmptySecurityScopes() throws Exception
   {
      Method method = new Method().withSecurity(List.of(Map.of("BearerAuth", List.of())));
      assertEquals(JsonUtils.toObject("{\"security\":[{}]}", JsonNode.class),
         JsonUtils.toObject(JsonUtils.toJson(method), JsonNode.class));
      assertJson("{\"security\":[{\"BearerAuth\":[]}]}", method);
   }



   /*******************************************************************************
    ** Literal wire oracles must not repeat the implementation's lowercase logic.
    *******************************************************************************/
   @Test
   void testExactEnumWireValues() throws Exception
   {
      Map<In, String> locations = Map.of(In.PATH, "path", In.QUERY, "query", In.HEADER, "header", In.COOKIE, "cookie");
      assertEquals(locations.keySet(), java.util.Set.of(In.values()));
      for(var entry : locations.entrySet())
      {
         Parameter parameter = new Parameter();
         parameter.setIn(entry.getKey());
         assertEquals(entry.getValue(), parameter.getIn());
         assertJson("{\"in\":\"" + entry.getValue() + "\"}", parameter);
         assertJson("{\"in\":\"" + entry.getValue() + "\"}", new Parameter().withIn(entry.getKey()));
      }

      Map<Type, String> types = Map.of(Type.ARRAY, "array", Type.BOOLEAN, "boolean", Type.INTEGER, "integer",
         Type.NUMBER, "number", Type.OBJECT, "object", Type.STRING, "string");
      assertEquals(types.keySet(), java.util.Set.of(Type.values()));
      for(var entry : types.entrySet())
      {
         Schema schema = new Schema();
         schema.setType(entry.getKey());
         assertEquals(entry.getValue(), schema.getType());
         assertJson("{\"type\":\"" + entry.getValue() + "\"}", schema);
         assertJson("{\"type\":\"" + entry.getValue() + "\"}", new Schema().withType(entry.getKey()));
      }
   }



   /*******************************************************************************
    ** Characterize missing validation; these shapes are not valid OpenAPI evidence.
    *******************************************************************************/
   @Test
   void testMissingAndInvalidShapesAreNotValidated() throws Exception
   {
      assertJson("{\"openapi\":\"3.0.3\"}", new OpenAPI());
      OpenAPI invalid = new OpenAPI().withInfo(new Info())
         .withPaths(Map.of("/people/{id}", new com.kingsrook.qqq.openapi.model.Path()
            .withGet(new Method().withParameters(List.of(new Parameter().withIn(In.PATH).withRequired(false))))));
      assertJson("""
         {"openapi":"3.0.3", "info":{}, "paths":{"/people/{id}":{"get":{
           "parameters":[{"in":"path", "required":false}]
         }}}}
         """, invalid);
      assertJson("{\"type\":\"array\"}", new Schema().withType(Type.ARRAY));
   }



   /*******************************************************************************
    ** Null enum behavior is a DTO boundary, not a required-shape validator.
    *******************************************************************************/
   @Test
   void testNullEnumBoundaries() throws Exception
   {
      Parameter parameter = new Parameter().withIn(In.QUERY);
      assertThrows(NullPointerException.class, () -> parameter.setIn(null));
      assertEquals("query", parameter.getIn());
      assertThrows(NullPointerException.class, () -> parameter.withIn(null));
      assertEquals("query", parameter.getIn());

      Schema schema = new Schema().withType(Type.STRING);
      schema.setType(null);
      assertNull(schema.getType());
      assertJson("{}", schema);
      assertJson("{}", new Schema().withType(Type.STRING).withType(null));
   }



   /*******************************************************************************
    ** Compile each typed replacement before rejecting its removed String overload.
    *******************************************************************************/
   @Test
   void testRemovedStringOverloadsDoNotCompile() throws Exception
   {
      Map<String, String> statements = Map.of(
         "parameter.setIn(In.QUERY);", "parameter.setIn(\"query\");",
         "parameter.withIn(In.QUERY);", "parameter.withIn(\"query\");",
         "schema.setType(Type.STRING);", "schema.setType(\"string\");",
         "schema.withType(Type.STRING);", "schema.withType(\"string\");");
      for(var entry : statements.entrySet())
      {
         assertEquals(List.of(), compile(entry.getKey(), true));
         List<Diagnostic<? extends JavaFileObject>> errors = compile(entry.getValue(), false);
         assertEquals(1, errors.size(), errors.toString());
         assertEquals("compiler.err.prob.found.req", errors.getFirst().getCode(), errors.toString());
         assertEquals(8, errors.getFirst().getLineNumber(), errors.toString());
      }
   }



   /*******************************************************************************
    ** Use the sample's actual resolved classpath and the same consumer per control.
    *******************************************************************************/
   private List<Diagnostic<? extends JavaFileObject>> compile(String statement, boolean expectedSuccess) throws Exception
   {
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      assertNotNull(compiler, "Acceptance requires a JDK with JavaCompiler");
      String source = """
         package acceptance;
         import com.kingsrook.qqq.openapi.model.In;
         import com.kingsrook.qqq.openapi.model.Parameter;
         import com.kingsrook.qqq.openapi.model.Schema;
         import com.kingsrook.qqq.openapi.model.Type;
         class EnumConsumer {
            void use(Parameter parameter, Schema schema) {
               %s
            }
         }
         """.formatted(statement);
      JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///acceptance/EnumConsumer.java"), JavaFileObject.Kind.SOURCE)
      {
         @Override
         public CharSequence getCharContent(boolean ignoreEncodingErrors)
         {
            return source;
         }
      };
      DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
      String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
      try(var fileManager = compiler.getStandardFileManager(diagnostics, null, null))
      {
         boolean success = compiler.getTask(null, fileManager, diagnostics,
            List.of("--release", "21", "-proc:none", "-classpath", classpath, "-d", classes.toString()), null, List.of(file)).call();
         assertEquals(expectedSuccess, success, statement + ": " + diagnostics.getDiagnostics());
      }
      return diagnostics.getDiagnostics().stream().filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR).toList();
   }



   /*******************************************************************************
    ** Preserve empty security scope arrays through the existing serializer API.
    *******************************************************************************/
   private void assertJson(String expected, Object value) throws Exception
   {
      String json = JsonUtils.toJsonCustomized(value, builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL));
      assertEquals(JsonUtils.toObject(expected, JsonNode.class), JsonUtils.toObject(json, JsonNode.class));
   }
}
