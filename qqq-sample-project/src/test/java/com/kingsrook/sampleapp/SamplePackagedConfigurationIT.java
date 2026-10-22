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


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** The configuration launcher must use packaged resources and reject bad input.
 *******************************************************************************/
class SamplePackagedConfigurationIT
{
   @TempDir
   Path directory;



   /*******************************************************************************
    ** Both defaults and JSON additions run the entire sample from an empty cwd.
    *******************************************************************************/
   @Test
   void testBundledApplicationAndJsonAdditions() throws Exception
   {
      Path additionalMetadata = Files.createDirectory(directory.resolve("metadata"));
      Files.writeString(additionalMetadata.resolve("configuredApp.json"), """
         {"class":"QAppMetaData","version":1,"name":"configuredApp","label":"Configured App",
          "sections":[{"name":"records","tables":["person"]}]}
         """);
      for(List<String> arguments : List.of(List.<String>of(), List.of(additionalMetadata.toString())))
      {
         try(PackagedSampleServer server = PackagedSampleServer.start(ConfigFileBasedSampleJavalinServer.class, directory, arguments);
             HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
         {
            URI base = server.awaitReady();
            JSONObject metadata = getJson(client, base.resolve("/metaData"));
            assertTrue(metadata.getJSONObject("tables").has("person"));
            assertTrue(metadata.getJSONObject("tables").has("fieldLab"), "This must run the entire canonical sample");
            assertTrue(metadata.getJSONObject("processes").has("greet"));
            if(!arguments.isEmpty())
            {
               assertTrue(metadata.getJSONObject("apps").has("configuredApp"));
            }
            JSONObject records = getJson(client, base.resolve("/data/person"));
            assertEquals(5, records.getJSONArray("records").length());
            assertEquals("Sample", records.getJSONArray("records").getJSONObject(0).getJSONObject("values").getString("lastName"));
         }
      }
   }



   /*******************************************************************************
    ** A second packaged process starts from the original seed after a real write.
    *******************************************************************************/
   @Test
   void testRepeatStartupReseedsInSeparateOwnedFixtures() throws Exception
   {
      try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         try(PackagedSampleServer first = PackagedSampleServer.start(SampleJavalinServer.class, directory, List.of()))
         {
            URI base = first.awaitReady();
            assertEquals(5, getJson(client, base.resolve("/data/person/count")).getInt("count"));
            HttpResponse<String> added = client.send(HttpRequest.newBuilder(base.resolve("/data/person"))
               .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
               .POST(HttpRequest.BodyPublishers.ofString("""
                  {"firstName":"Bootstrap","lastName":"Disposable","email":"bootstrap@example.invalid"}
                  """)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, added.statusCode(), added.body());
            assertEquals(6, getJson(client, base.resolve("/data/person/count")).getInt("count"));
         }
         try(PackagedSampleServer second = PackagedSampleServer.start(SampleJavalinServer.class, directory, List.of()))
         {
            URI base = second.awaitReady();
            assertEquals(5, getJson(client, base.resolve("/data/person/count")).getInt("count"));
         }
      }
      try(var fixtures = Files.list(directory))
      {
         assertEquals(2, fixtures.filter(path -> path.getFileName().toString().startsWith("packaged-")).count());
      }
   }



   /*******************************************************************************
    ** Invalid directories and misspelled fields must fail the actual Java process.
    *******************************************************************************/
   @Test
   void testInvalidConfigurationHasNonzeroExit() throws Exception
   {
      Path invalid = Files.createDirectory(directory.resolve("invalid"));
      Files.writeString(invalid.resolve("bad.json"), """
         {"class":"QAppMetaData","name":"brokenApp","misspelledProperty":true}
         """);
      for(Path configuration : List.of(directory.resolve("missing"), invalid))
      {
         try(PackagedSampleServer server = PackagedSampleServer.start(ConfigFileBasedSampleJavalinServer.class, directory, List.of(configuration.toString())))
         {
            assertNotEquals(0, server.awaitExit(), server.output());
            assertTrue(server.output().contains(configuration.getFileName().toString()));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDuplicateMetadataHasNonzeroExit() throws Exception
   {
      Path duplicate = Files.createDirectory(directory.resolve("duplicate"));
      try(var resource = getClass().getResourceAsStream("/metadata/personTable.yaml"))
      {
         Files.copy(resource, duplicate.resolve("personTable.yaml"));
      }
      try(PackagedSampleServer server = PackagedSampleServer.start(ConfigFileBasedSampleJavalinServer.class, directory, List.of(duplicate.toString())))
      {
         assertNotEquals(0, server.awaitExit(), server.output());
         assertTrue(server.output().contains("Attempted to add a second table with name: person"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject getJson(HttpClient client, URI uri) throws Exception
   {
      HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode(), response.body());
      return new JSONObject(response.body());
   }
}
