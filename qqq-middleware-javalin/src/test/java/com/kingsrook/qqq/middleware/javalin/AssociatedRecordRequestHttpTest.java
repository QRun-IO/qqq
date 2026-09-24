/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Recursive legacy form writes are verified through HTTP and independent SQL.
 ** The extra table is an owned protocol fixture; no database cascade hides work.
 *******************************************************************************/
class AssociatedRecordRequestHttpTest extends QJavalinTestBase
{
   private static final String DETAIL = "transportDetail";
   private static final String GROUP = "details.[owned]";
   private static final byte[] BYTES = new byte[] { 0, 1, -1, -128, 65, 0 };
   private static List<String> permissions = List.of();
   private UnirestInstance client;
   private boolean hideMetadata;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void closeClient() throws Exception
   {
      if(client != null)
      {
         client.close();
      }
      sql("DROP TABLE IF EXISTS transport_detail");
   }



   /*******************************************************************************
    ** Exact names, two aliases to the same target, typed scalars and nested binary.
    *******************************************************************************/
   @Test
   void testRecursiveInsertPreservesBothNamedBranchesAndBytes() throws Exception
   {
      start("person.insert", "pet.insert", DETAIL + ".insert");
      String originalPeople = rows("SELECT * FROM person ORDER BY id");
      String originalPets = rows("SELECT * FROM pet ORDER BY id");
      assertEquals(403, client.get(BASE_URL + "/data/pet").asString().getStatus());
      JSONObject groups = new JSONObject().put("pets", new JSONArray().put(pet("first", true)))
         .put("pet list.2", new JSONArray().put(pet("second", true)));
      HttpResponse<String> response = insert(groups.toString());
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject saved = JsonUtils.toJSONObject(response.getBody()).getJSONArray("records").getJSONObject(0);
      int personId = saved.getJSONObject("values").getInt("id");
      assertEquals(originalPeople, rows("SELECT * FROM person WHERE id<>" + personId + " ORDER BY id"));
      assertEquals(originalPets, rows("SELECT * FROM pet WHERE owner_person_id<>" + personId + " ORDER BY id"));
      assertEquals("2", scalar("SELECT COUNT(*) FROM pet WHERE owner_person_id=" + personId));
      assertEquals("2", scalar("SELECT COUNT(*) FROM transport_detail"));
      assertEquals("2", scalar("SELECT COUNT(*) FROM transport_detail d JOIN pet p ON p.id=d.pet_id WHERE p.owner_person_id=" + personId));
      for(String name : List.of("pets", "pet list.2"))
      {
         JSONObject child = saved.getJSONObject("associatedRecords").getJSONArray(name).getJSONObject(0);
         JSONObject detail = child.getJSONObject("associatedRecords").getJSONArray(GROUP).getJSONObject(0);
         assertTrue(child.getJSONObject("values").getInt("id") > 2);
         assertTrue(detail.getJSONObject("values").getInt("id") > 0);
      }
      assertEquals("2", scalar("SELECT COUNT(*) FROM transport_detail WHERE flag=false AND amount=0 AND note IS NULL AND literal='null' AND legacy_values='field named values'"));
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineDefaultH2Backend());
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT payload FROM transport_detail"))
      {
         while(result.next())
         {
            assertEquals(HexFormat.of().formatHex(BYTES), HexFormat.of().formatHex(result.getBytes(1)));
         }
      }
   }



   /*******************************************************************************
    ** A denied grandchild INSERT rejects the entire request before parent mutation.
    *******************************************************************************/
   @Test
   void testDeniedGrandchildLeavesEveryTableUnchanged() throws Exception
   {
      start("person.insert", "pet.insert");
      String before = snapshot();
      HttpResponse<String> response = insert(new JSONObject().put("pets", new JSONArray().put(pet("denied", true))).toString());
      assertEquals(403, response.getStatus(), response.getBody());
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Parse failures cannot be downgraded into a successful parent-only write.
    *******************************************************************************/
   @Test
   void testMalformedRecursivePayloadHasNoMutation() throws Exception
   {
      start("person.insert", "pet.insert", DETAIL + ".insert");
      String before = snapshot();
      for(String groups : List.of(
         "[]", "{pets:[]}", "{'pets':[]}", "{\"pets\":null}", "{\"pets\":[null]}",
         "{\"pets\":[{\"name\":\"flat\"}]}", "{\"pets\":[{\"values\":[],\"associatedRecords\":{}}]}",
         "{\"pets\":[{\"values\":{},\"tableName\":\"person\"}]}", "{\"pets\":[{\"values\":{},\"associatedRecords\":null}]}",
         "{\"unknown\":[]}", "{\"pets\":[{\"values\":{\"unknown\":1}}]}", "{\"pets\":[]} trailing", "{\"pets\":[],\"pets\":[]}"))
      {
         HttpResponse<String> response = insert(groups);
         assertEquals(400, response.getStatus(), groups + ": " + response.getBody());
         assertEquals(before, snapshot());
      }
   }



   /*******************************************************************************
    ** Invalid binary and object-shaped scalar data are rejected, never stringified.
    *******************************************************************************/
   @Test
   void testMalformedBinaryHasNoMutation() throws Exception
   {
      start("person.insert", "pet.insert", DETAIL + ".insert");
      String before = snapshot();
      for(String encoded : List.of("%%%", "AQ="))
      {
         JSONObject child = pet("binary", true);
         child.getJSONObject("associatedRecords").getJSONArray(GROUP).getJSONObject(0).getJSONObject("values")
            .put("payload", new JSONObject().put("base64", encoded));
         assertEquals(400, insert(new JSONObject().put("pets", new JSONArray().put(child)).toString()).getStatus());
         assertEquals(before, snapshot());
      }
   }



   /*******************************************************************************
    ** An empty nested group is a replacement; omission preserves its stored rows.
    *******************************************************************************/
   @Test
   void testNestedUpdateDistinguishesEmptyAndOmitted() throws Exception
   {
      start("person.insert", "person.edit", "pet.insert", "pet.edit", DETAIL + ".insert", DETAIL + ".delete");
      HttpResponse<String> inserted = insert(new JSONObject().put("pets", new JSONArray().put(pet("replace", true))).toString());
      assertEquals(200, inserted.getStatus(), inserted.getBody());
      JSONObject saved = JsonUtils.toJSONObject(inserted.getBody()).getJSONArray("records").getJSONObject(0);
      int personId = saved.getJSONObject("values").getInt("id");
      int petId = saved.getJSONObject("associatedRecords").getJSONArray("pets").getJSONObject(0).getJSONObject("values").getInt("id");
      JSONObject child = new JSONObject().put("values", new JSONObject().put("id", petId).put("name", "preserved"));
      JSONObject groups = new JSONObject().put("pets", new JSONArray().put(child));
      assertEquals(200, update(personId, groups).getStatus());
      assertEquals("1", scalar("SELECT COUNT(*) FROM transport_detail"));
      child.put("associatedRecords", new JSONObject().put(GROUP, new JSONArray()));
      assertEquals(200, update(personId, groups).getStatus());
      assertEquals("0", scalar("SELECT COUNT(*) FROM transport_detail"));
      assertEquals("preserved", scalar("SELECT name FROM pet WHERE id=" + petId));
   }



   /*******************************************************************************
    ** Legacy flat children retain a nullable field named values without envelopes.
    *******************************************************************************/
   @Test
   void testLegacyFlatNullAndTextCompatibility() throws Exception
   {
      start("person.insert", "pet.insert");
      HttpResponse<String> response = client.post(BASE_URL + "/data/person").field("firstName", "Legacy").field("lastName", "Parent").field("email", "legacy@example.test")
         .field("associations", "{\"pets\":[{\"name\":\"flat\",\"species\":\"dog\",\"values\":null}]}").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals("1", scalar("SELECT COUNT(*) FROM pet WHERE name='flat' AND transport_values IS NULL"));
   }



   /*******************************************************************************
    ** Explicit JSON and explicit format declarations cannot fall back to empty forms.
    *******************************************************************************/
   @Test
   void testMalformedBodyAndUnknownVersionHaveNoMutation() throws Exception
   {
      start("person.insert", "pet.insert");
      String before = snapshot();
      assertEquals(400, client.post(BASE_URL + "/data/person").header("Content-Type", "application/json").body("{").asString().getStatus());
      assertEquals(400, client.post(BASE_URL + "/data/person").header(AssociatedRecordRequest.HEADER, "future")
         .field("firstName", "Bad").field("associations", "{}").asString().getStatus());
      assertEquals(400, client.post(BASE_URL + "/data/person").header(AssociatedRecordRequest.HEADER, AssociatedRecordRequest.FORMAT)
         .field("firstName", "Missing").asString().getStatus());
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Removed fields and descendant groups remain unavailable to USER requests.
    *******************************************************************************/
   @Test
   void testPersonalizedChildMetadataRejectsBeforeMutation() throws Exception
   {
      hideMetadata = true;
      start("person.insert", "pet.insert", DETAIL + ".insert");
      String before = snapshot();
      assertEquals(400, insert(new JSONObject().put("pets", new JSONArray().put(pet("hidden field", false))).toString()).getStatus());
      JSONObject nested = pet("hidden group", true);
      nested.getJSONObject("values").remove("values");
      assertEquals(400, insert(new JSONObject().put("pets", new JSONArray().put(nested)).toString()).getStatus());
      assertEquals(before, snapshot());
      JSONObject allowed = new JSONObject().put("values", new JSONObject().put("name", "allowed").put("species", "dog"));
      HttpResponse<String> response = insert(new JSONObject().put("pets", new JSONArray().put(allowed)).toString());
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals("1", scalar("SELECT COUNT(*) FROM pet WHERE name='allowed'"));
   }



   /*******************************************************************************
    ** Expanded source JSON distinguishes stored null/empty values and exact bytes.
    *******************************************************************************/
   @Test
   void testExpandedGetPreservesExplicitNullAndBinaryValues() throws Exception
   {
      start("person.insert", "person.read", "pet.insert", "pet.read", DETAIL + ".insert", DETAIL + ".read");
      HttpResponse<String> inserted = insert(new JSONObject().put("pets", new JSONArray().put(pet("source", true))).toString());
      assertEquals(200, inserted.getStatus(), inserted.getBody());
      int id = JsonUtils.toJSONObject(inserted.getBody()).getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id");
      HttpResponse<String> response = client.get(BASE_URL + "/data/person/" + id).queryString("includeAssociations", true).asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject saved = JsonUtils.toJSONObject(response.getBody());
      JSONObject values = saved.getJSONObject("associatedRecords").getJSONArray("pets").getJSONObject(0)
         .getJSONObject("associatedRecords").getJSONArray(GROUP).getJSONObject(0).getJSONObject("values");
      assertTrue(values.has("note"), response.getBody());
      assertTrue(values.isNull("note"));
      assertEquals("null", values.getString("literal"));
      assertEquals(false, values.getBoolean("flag"));
      assertEquals(0, values.getInt("amount"));
      assertEquals(Base64.getEncoder().encodeToString(BYTES), values.getString("payload"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject pet(String name, boolean includeDetail)
   {
      JSONObject child = new JSONObject().put("values", new JSONObject().put("name", name).put("species", "dog").put("values", "child scalar"));
      if(includeDetail)
      {
         JSONObject values = new JSONObject().put("flag", false).put("amount", 0).put("note", JSONObject.NULL)
            .put("literal", "null").put("values", "field named values").put("payload", new JSONObject().put("base64", Base64.getEncoder().encodeToString(BYTES)));
         child.put("associatedRecords", new JSONObject().put(GROUP, new JSONArray().put(new JSONObject().put("values", values))));
      }
      return child;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> insert(String groups)
   {
      return client.post(BASE_URL + "/data/person").header(AssociatedRecordRequest.HEADER, AssociatedRecordRequest.FORMAT)
         .field("firstName", "Recursive").field("lastName", "Parent").field("email", "recursive@example.test").field("associations", groups).asString();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> update(int personId, JSONObject groups)
   {
      return client.patch(BASE_URL + "/data/person/" + personId).header(AssociatedRecordRequest.HEADER, AssociatedRecordRequest.FORMAT)
         .field("associations", groups.toString()).asString();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void start(String... grants) throws Exception
   {
      permissions = List.of(grants);
      sql("CREATE TABLE transport_detail(id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, pet_id INTEGER, flag BOOLEAN, amount INTEGER, note VARCHAR(100), literal VARCHAR(100), legacy_values VARCHAR(100), payload VARBINARY(100))");
      QInstance instance = TestUtils.defineInstance();
      sql("ALTER TABLE pet ADD COLUMN transport_values VARCHAR(100)");
      instance.getTable("pet").withField(new QFieldMetaData("values", QFieldType.STRING).withBackendName("transport_values"));
      QTableMetaData detail = new QTableMetaData().withName(DETAIL).withBackendName(TestUtils.defineDefaultH2Backend().getName()).withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("transport_detail"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("petId", QFieldType.INTEGER).withBackendName("pet_id"))
         .withField(new QFieldMetaData("flag", QFieldType.BOOLEAN)).withField(new QFieldMetaData("amount", QFieldType.INTEGER))
         .withField(new QFieldMetaData("note", QFieldType.STRING)).withField(new QFieldMetaData("literal", QFieldType.STRING))
         .withField(new QFieldMetaData("values", QFieldType.STRING).withBackendName("legacy_values")).withField(new QFieldMetaData("payload", QFieldType.BLOB));
      instance.addTable(detail);
      instance.getTable("person").withAssociation(new Association().withName("pet list.2").withAssociatedTableName("pet").withJoinName("personJoinPet"));
      instance.getTable("pet").withAssociation(new Association().withName(GROUP).withAssociatedTableName(DETAIL).withJoinName("petTransportDetail"));
      instance.addJoin(new QJoinMetaData().withName("petTransportDetail").withLeftTable("pet").withRightTable(DETAIL)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "petId")));
      for(String name : List.of("person", "pet", DETAIL))
      {
         instance.getTable(name).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      instance.getAuthentication().setCustomizer(new QCodeReference(Grants.class));
      if(hideMetadata)
      {
         instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(HideChildMetadata.class));
      }
      restartServerWithInstance(instance);
      client = Unirest.spawnInstance();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void sql(String statement) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineDefaultH2Backend()); Statement sql = connection.createStatement())
      {
         sql.execute(statement);
      }
   }



   /*******************************************************************************
    ** Snapshot every column using stable binary content rather than array identity.
    *******************************************************************************/
   private String rows(String query) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineDefaultH2Backend());
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               Object value = result.getObject(column);
               if(value instanceof Blob)
               {
                  value = result.getBytes(column);
               }
               row.add(value instanceof byte[] bytes ? HexFormat.of().formatHex(bytes) : value == null ? null : value.toString());
            }
            rows.add(row);
         }
      }
      return rows.toString();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String scalar(String query) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineDefaultH2Backend());
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         assertTrue(result.next());
         return result.getString(1);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String snapshot() throws Exception
   {
      return rows("SELECT * FROM person ORDER BY id") + rows("SELECT * FROM pet ORDER BY id") + rows("SELECT * FROM transport_detail ORDER BY id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Grants implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions(permissions.toArray(String[]::new));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class HideChildMetadata implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         QTableMetaData table = input.getTable();
         if("pet".equals(table.getName()) && input.getInputSource() == QInputSource.USER)
         {
            table = table.clone();
            table.getFields().remove("values");
            table.setAssociations(List.of());
         }
         return table;
      }
   }
}
