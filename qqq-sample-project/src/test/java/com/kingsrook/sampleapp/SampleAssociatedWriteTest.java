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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** HTTP association writes must honor child grants before mutating sample data.
 *******************************************************************************/
public class SampleAssociatedWriteTest
{
   private static String[] permissions = new String[0];

   private SampleJavalinServer server;
   private HttpClient client;
   private URI base;
   private boolean protectPetsByParentName;



   /*******************************************************************************
    ** The default sequential suite owns a fresh server, session and H2 fixture.
    *******************************************************************************/
   private void start(String... grantedPermissions) throws Exception
   {
      permissions = grantedPermissions;
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(WritePermissions.class));
      for(String table : List.of("person", "pet", "petNote"))
      {
         instance.getTable(table).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      if(protectPetsByParentName)
      {
         instance.addSecurityKeyType(new QSecurityKeyType().withName("parentName"));
         instance.getTable("pet").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("parentName")
            .withFieldName("person.firstName").withJoinNameChain(List.of("personJoinPet")));
      }
      server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      base = URI.create("http://localhost:" + service.get().port());
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      if(client != null)
      {
         client.close();
      }
      if(server != null)
      {
         server.stop();
      }
      permissions = new String[0];
      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeniedChildInsertDoesNotInsertParent() throws Exception
   {
      start("person.insert", "pet.read");
      assertEquals(403, request("POST", "/data/pet", "name=Denied&speciesId=1&personId=1").statusCode());
      assertDeniedWithoutMutation("POST", "/data/person", "firstName=Denied&lastName=Parent&email=denied%40example.invalid&" + associations(new JSONArray().put(newPet())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeniedChildEditDoesNotUpdateParent() throws Exception
   {
      start("person.edit", "pet.read");
      JSONArray children = existingPets(4);
      children.getJSONObject(0).put("name", "Edited child");
      assertDeniedWithoutMutation("PATCH", "/data/person/1", "firstName=Edited&" + associations(children));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeniedChildInsertThroughUpdateDoesNotMutate() throws Exception
   {
      start("person.edit", "pet.edit", "pet.delete");
      assertDeniedWithoutMutation("PATCH", "/data/person/1", "firstName=Edited&" + associations(existingPets(4).put(newPet())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeniedOmissionDeleteDoesNotMutate() throws Exception
   {
      start("person.edit", "pet.edit", "pet.insert");
      assertDeniedWithoutMutation("PATCH", "/data/person/1", "firstName=Edited&" + associations(existingPets(3)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeniedEmptyReplacementDoesNotMutate() throws Exception
   {
      start("person.edit", "pet.edit", "pet.insert");
      assertDeniedWithoutMutation("PATCH", "/data/person/1", "firstName=Edited&" + associations(new JSONArray()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeniedCascadeDoesNotDeleteParent() throws Exception
   {
      start("person.delete", "pet.read");
      assertDeniedWithoutMutation("DELETE", "/data/person/1", null);
   }



   /*******************************************************************************
    ** Parent changes must not make previously hidden children escape authorization.
    *******************************************************************************/
   @Test
   void testParentSecurityChangeCannotEnableDeniedChildDeletion() throws Exception
   {
      protectPetsByParentName = true;
      start("person.edit");
      assertDeniedWithoutMutation("PATCH", "/data/person/1", "firstName=Changed&" + associations(new JSONArray()));
   }



   /*******************************************************************************
    ** Cascade authorization includes grandchildren before deleting any ancestor.
    *******************************************************************************/
   @Test
   void testDeniedGrandchildCascadeDoesNotMutateAnyLevel() throws Exception
   {
      start("person.delete", "pet.delete", "petNote.read");
      assertDeniedWithoutMutation("DELETE", "/data/person/1", null);
   }



   /*******************************************************************************
    ** Granular DELETE grants are sufficient without public READ at either depth.
    *******************************************************************************/
   @Test
   void testAllowedCascadeWithoutReadPreservesOtherFamily() throws Exception
   {
      start("person.delete", "pet.delete", "petNote.delete");
      HttpResponse<String> response = request("DELETE", "/data/person/1", null);
      assertEquals(200, response.statusCode(), response.body());
      assertEquals(List.of(), rows("SELECT id FROM person WHERE id=1"));
      assertEquals(List.of(), rows("SELECT id FROM pet WHERE person_id=1"));
      assertEquals(List.of(List.of("2", "5", "Other parent note")), rows("SELECT id,pet_id,note FROM pet_note ORDER BY id"));
      assertEquals(List.of(List.of("Toby")), rows("SELECT name FROM pet WHERE person_id=2"));
   }



   /*******************************************************************************
    ** A child write grant does not require a public child READ grant.
    *******************************************************************************/
   @Test
   void testAllowedInsertWithoutChildReadPersistsRelationship() throws Exception
   {
      start("person.insert", "pet.insert");
      HttpResponse<String> response = request("POST", "/data/person", "firstName=New&lastName=Parent&email=new%40example.invalid&" + associations(new JSONArray().put(newPet())));
      assertEquals(200, response.statusCode(), response.body());
      int id = new JSONObject(response.body()).getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id");
      assertEquals(List.of(List.of("New")), rows("SELECT first_name FROM person WHERE id=" + id));
      assertEquals(List.of(List.of("New child", "1")), rows("SELECT name,species_id FROM pet WHERE person_id=" + id));
      assertEquals(403, request("GET", "/data/pet", null).statusCode());
   }



   /*******************************************************************************
    ** A replacement with all existing IDs edits children without requiring DELETE.
    *******************************************************************************/
   @Test
   void testAllowedEditWithoutReadOrDeletePreservesOtherChildren() throws Exception
   {
      start("person.edit", "pet.edit");
      JSONArray children = existingPets(4);
      children.getJSONObject(0).put("name", "Edited child");
      HttpResponse<String> response = request("PATCH", "/data/person/1", "firstName=Edited&" + associations(children));
      assertEquals(200, response.statusCode(), response.body());
      assertEquals(List.of(List.of("Edited")), rows("SELECT first_name FROM person WHERE id=1"));
      assertEquals(List.of(List.of("1", "Edited child"), List.of("2", "Coco"), List.of("3", "Louie"), List.of("4", "Barkley")), rows("SELECT id,name FROM pet WHERE person_id=1 ORDER BY id"));
      assertEquals(List.of(List.of("Toby")), rows("SELECT name FROM pet WHERE person_id=2"));
   }



   /*******************************************************************************
    ** An omitted association must leave children untouched without child grants.
    *******************************************************************************/
   @Test
   void testParentOnlyEditLeavesDeniedChildrenUntouched() throws Exception
   {
      start("person.edit");
      List<List<String>> before = rows("SELECT id,name,person_id FROM pet ORDER BY id");
      HttpResponse<String> response = request("PATCH", "/data/person/1", "firstName=Edited");
      assertEquals(200, response.statusCode(), response.body());
      assertEquals(List.of(List.of("Edited")), rows("SELECT first_name FROM person WHERE id=1"));
      assertEquals(before, rows("SELECT id,name,person_id FROM pet ORDER BY id"));
   }



   /*******************************************************************************
    ** An intentional empty replacement needs DELETE, independently of READ/EDIT.
    *******************************************************************************/
   @Test
   void testAllowedReplacementDeleteOnlyRemovesItsRelationship() throws Exception
   {
      start("person.edit", "pet.delete", "petNote.delete");
      HttpResponse<String> response = request("PATCH", "/data/person/1", associations(new JSONArray()));
      assertEquals(200, response.statusCode(), response.body());
      assertEquals(List.of(), rows("SELECT id FROM pet WHERE person_id=1"));
      assertEquals(List.of(List.of("5", "Toby"), List.of("6", "Mae")), rows("SELECT id,name FROM pet ORDER BY id"));
   }



   /*******************************************************************************
    ** The ordinary runnable seed exposes named grandchildren and known empty groups.
    *******************************************************************************/
   @Test
   void testCanonicalSeedExpandedGetIncludesPetNotes() throws Exception
   {
      start("person.read", "pet.read", "petNote.read");
      HttpResponse<String> response = request("GET", "/data/person/1?includeAssociations=true", null);
      assertEquals(200, response.statusCode(), response.body());
      JSONArray pets = new JSONObject(response.body()).getJSONObject("associatedRecords").getJSONArray("pets");
      assertEquals(4, pets.length());
      for(int i = 0; i < pets.length(); i++)
      {
         JSONObject pet = pets.getJSONObject(i);
         JSONArray notes = pet.getJSONObject("associatedRecords").getJSONArray("notes");
         if(pet.getJSONObject("values").getInt("id") == 1)
         {
            assertEquals(1, notes.length());
            assertEquals("Target note", notes.getJSONObject(0).getJSONObject("values").getString("note"));
         }
         else
         {
            assertEquals(0, notes.length());
         }
      }
   }



   /*******************************************************************************
    ** The original flat form remains valid for the same canonical Pet/notes model.
    *******************************************************************************/
   @Test
   void testLegacyFlatFormInsertsCanonicalPetNote() throws Exception
   {
      start("pet.insert", "petNote.insert");
      JSONObject groups = new JSONObject().put("notes", new JSONArray().put(new JSONObject().put("note", "Legacy note")));
      HttpResponse<String> response = request("POST", "/data/pet", "name=Legacy&speciesId=1&personId=1&associations=" + encoded(groups));
      assertEquals(200, response.statusCode(), response.body());
      int petId = new JSONObject(response.body()).getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id");
      assertEquals(List.of(List.of(Integer.toString(petId), "Legacy note")), rows("SELECT pet_id,note FROM pet_note WHERE pet_id=" + petId));
      assertEquals(List.of(List.of("1", "Target note"), List.of("5", "Other parent note")), rows("SELECT pet_id,note FROM pet_note WHERE pet_id<>" + petId + " ORDER BY id"));
   }



   /*******************************************************************************
    ** The canonical Person/pets/notes graph survives the explicit recursive wire.
    *******************************************************************************/
   @Test
   void testCanonicalRecursiveInsertReturnsEveryGeneratedIdentity() throws Exception
   {
      start("person.insert", "pet.insert", "petNote.insert");
      List<List<String>> originalPeople = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> originalPets = rows("SELECT * FROM pet ORDER BY id");
      List<List<String>> originalNotes = rows("SELECT * FROM pet_note ORDER BY id");
      assertEquals(403, request("GET", "/data/petNote", null).statusCode());
      HttpResponse<String> response = recursiveInsert();
      assertEquals(200, response.statusCode(), response.body());
      JSONObject parent = new JSONObject(response.body()).getJSONArray("records").getJSONObject(0);
      int personId = parent.getJSONObject("values").getInt("id");
      JSONObject pet = parent.getJSONObject("associatedRecords").getJSONArray("pets").getJSONObject(0);
      int petId = pet.getJSONObject("values").getInt("id");
      JSONArray notes = pet.getJSONObject("associatedRecords").getJSONArray("notes");
      int firstNoteId = notes.getJSONObject(0).getJSONObject("values").getInt("id");
      int secondNoteId = notes.getJSONObject(1).getJSONObject("values").getInt("id");
      assertTrue(personId > 5 && petId > 6 && firstNoteId > 2 && secondNoteId > firstNoteId);
      assertEquals(List.of(List.of(Integer.toString(petId), "Recursive pet", Integer.toString(personId))),
         rows("SELECT id,name,person_id FROM pet WHERE person_id=" + personId));
      assertEquals(List.of(List.of(Integer.toString(firstNoteId), Integer.toString(petId), "First new note"),
         List.of(Integer.toString(secondNoteId), Integer.toString(petId), "Second new note")),
         rows("SELECT id,pet_id,note FROM pet_note WHERE pet_id=" + petId + " ORDER BY id"));
      assertEquals(originalPeople, rows("SELECT * FROM person WHERE id<>" + personId + " ORDER BY id"));
      assertEquals(originalPets, rows("SELECT * FROM pet WHERE id<>" + petId + " ORDER BY id"));
      assertEquals(originalNotes, rows("SELECT * FROM pet_note WHERE pet_id<>" + petId + " ORDER BY id"));
   }



   /*******************************************************************************
    ** Nested omission preserves notes; explicit [] removes only that pet's notes.
    *******************************************************************************/
   @Test
   void testCanonicalNestedUpdateDistinguishesOmittedAndEmptyNotes() throws Exception
   {
      start("person.insert", "person.edit", "pet.insert", "pet.edit", "petNote.insert", "petNote.delete");
      HttpResponse<String> inserted = recursiveInsert();
      assertEquals(200, inserted.statusCode(), inserted.body());
      JSONObject parent = new JSONObject(inserted.body()).getJSONArray("records").getJSONObject(0);
      int personId = parent.getJSONObject("values").getInt("id");
      int petId = parent.getJSONObject("associatedRecords").getJSONArray("pets").getJSONObject(0).getJSONObject("values").getInt("id");
      List<List<String>> originalNotes = rows("SELECT * FROM pet_note ORDER BY id");
      List<List<String>> unrelatedNotes = rows("SELECT * FROM pet_note WHERE pet_id<>" + petId + " ORDER BY id");
      JSONObject child = new JSONObject().put("values", new JSONObject().put("id", petId).put("name", "Renamed pet"));
      JSONObject groups = new JSONObject().put("pets", new JSONArray().put(child));
      HttpResponse<String> omitted = requestRecursive("PATCH", "/data/person/" + personId, "associations=" + encoded(groups));
      assertEquals(200, omitted.statusCode(), omitted.body());
      assertEquals(originalNotes, rows("SELECT * FROM pet_note ORDER BY id"));
      child.put("associatedRecords", new JSONObject().put("notes", new JSONArray()));
      HttpResponse<String> emptied = requestRecursive("PATCH", "/data/person/" + personId, "associations=" + encoded(groups));
      assertEquals(200, emptied.statusCode(), emptied.body());
      assertEquals(unrelatedNotes, rows("SELECT * FROM pet_note ORDER BY id"));
      assertEquals(List.of(List.of("Renamed pet", Integer.toString(personId))), rows("SELECT name,person_id FROM pet WHERE id=" + petId));
   }



   /*******************************************************************************
    ** The denied deepest INSERT must reject before any canonical table changes.
    *******************************************************************************/
   @Test
   void testDeniedCanonicalRecursiveInsertHasNoMutation() throws Exception
   {
      start("person.insert", "pet.insert");
      List<List<String>> people = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      List<List<String>> notes = rows("SELECT * FROM pet_note ORDER BY id");
      HttpResponse<String> response = recursiveInsert();
      assertAll(() -> assertEquals(403, response.statusCode(), response.body()),
         () -> assertEquals(people, rows("SELECT * FROM person ORDER BY id")),
         () -> assertEquals(pets, rows("SELECT * FROM pet ORDER BY id")),
         () -> assertEquals(notes, rows("SELECT * FROM pet_note ORDER BY id")));
   }



   /*******************************************************************************
    ** A malformed descendant cannot silently become a successful parent-only write.
    *******************************************************************************/
   @Test
   void testMalformedCanonicalRecursiveTransportHasNoMutation() throws Exception
   {
      start("person.insert", "pet.insert", "petNote.insert");
      List<List<String>> people = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      List<List<String>> notes = rows("SELECT * FROM pet_note ORDER BY id");
      for(String malformed : List.of("{\"pets\":[{\"values\":{\"name\":\"Bad\",\"speciesId\":1},\"associatedRecords\":{\"unknown\":[]}}]}",
         "{\"pets\":[{\"values\":{\"name\":\"Bad\",\"speciesId\":1},\"associatedRecords\":{\"notes\":[{\"note\":\"flat\"}]}}]}"))
      {
         HttpResponse<String> response = requestRecursive("POST", "/data/person", "firstName=Bad&lastName=Parent&email=bad%40example.invalid&associations="
            + URLEncoder.encode(malformed, StandardCharsets.UTF_8));
         assertAll(() -> assertEquals(400, response.statusCode(), response.body()),
            () -> assertEquals(people, rows("SELECT * FROM person ORDER BY id")),
            () -> assertEquals(pets, rows("SELECT * FROM pet ORDER BY id")),
            () -> assertEquals(notes, rows("SELECT * FROM pet_note ORDER BY id")));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> recursiveInsert() throws Exception
   {
      JSONArray notes = new JSONArray().put(new JSONObject().put("values", new JSONObject().put("note", "First new note")))
         .put(new JSONObject().put("values", new JSONObject().put("note", "Second new note")));
      JSONObject pet = new JSONObject().put("values", new JSONObject().put("name", "Recursive pet").put("speciesId", 1))
         .put("associatedRecords", new JSONObject().put("notes", notes));
      JSONObject groups = new JSONObject().put("pets", new JSONArray().put(pet));
      return requestRecursive("POST", "/data/person", "firstName=Recursive&lastName=Parent&email=recursive%40example.invalid&associations=" + encoded(groups));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String encoded(JSONObject groups)
   {
      return URLEncoder.encode(groups.toString(), StandardCharsets.UTF_8);
   }



   /*******************************************************************************
    ** Check response and persisted state independently so a late denial is a failure.
    *******************************************************************************/
   private void assertDeniedWithoutMutation(String method, String path, String body) throws Exception
   {
      List<List<String>> people = rows("SELECT * FROM person ORDER BY id");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      List<List<String>> notes = rows("SELECT id,pet_id,note FROM pet_note ORDER BY id");
      HttpResponse<String> response = request(method, path, body);
      assertAll(
         () -> assertEquals(403, response.statusCode(), response.body()),
         () -> assertEquals(people, rows("SELECT * FROM person ORDER BY id"), "Parent state must not change on denial"),
         () -> assertEquals(pets, rows("SELECT * FROM pet ORDER BY id"), "Child state must not change on denial"),
         () -> assertEquals(notes, rows("SELECT id,pet_id,note FROM pet_note ORDER BY id"), "Grandchild state must not change on denial"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONArray existingPets(int count)
   {
      JSONArray records = new JSONArray();
      for(int id = 1; id <= count; id++)
      {
         records.put(new JSONObject().put("id", id));
      }
      return records;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject newPet()
   {
      return new JSONObject().put("name", "New child").put("speciesId", 1);
   }



   /*******************************************************************************
    ** Preserve the legacy named association wire format without envelope guesses.
    *******************************************************************************/
   private String associations(JSONArray children)
   {
      return "associations=" + URLEncoder.encode(new JSONObject().put("pets", children).toString(), StandardCharsets.UTF_8);
   }



   /*******************************************************************************
    ** Requests stay on this test's ephemeral loopback server.
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path, String body) throws Exception
   {
      return request(method, path, body, false);
   }



   /*******************************************************************************
    ** Explicitly opt in; legacy association tests continue without the new header.
    *******************************************************************************/
   private HttpResponse<String> requestRecursive(String method, String path, String body) throws Exception
   {
      return request(method, path, body, true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path, String body, boolean recursive) throws Exception
   {
      URI target = base.resolve(path);
      assertEquals(base.getAuthority(), target.getAuthority());
      HttpRequest.Builder request = HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(15))
         .header("Content-Type", "application/x-www-form-urlencoded");
      if(recursive)
      {
         request.header("X-QQQ-Association-Format", "record-v1");
      }
      return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** Trusted SQL reads verify storage without relying on the denied public API.
    *******************************************************************************/
   private List<List<String>> rows(String sql) throws Exception
   {
      List<List<String>> result = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement();
          ResultSet rows = statement.executeQuery(sql))
      {
         while(rows.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= rows.getMetaData().getColumnCount(); column++)
            {
               row.add(rows.getString(column));
            }
            result.add(row);
         }
      }
      return result;
   }



   /*******************************************************************************
    ** Synthetic per-scenario permissions, never a downstream account or service.
    *******************************************************************************/
   public static class WritePermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions(permissions);
         session.withSecurityKeyValue("parentName", "Changed");
      }
   }
}
