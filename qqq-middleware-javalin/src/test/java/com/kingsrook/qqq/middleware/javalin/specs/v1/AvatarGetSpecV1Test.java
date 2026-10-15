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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.profile.UserProfileMetaDataProvider;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.TableBasedAuthenticationModule;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Avatars over real authenticated HTTP: upload through POST /uploads, set in
 ** the profile section, read back from the session and GET /avatars.
 *******************************************************************************/
class AvatarGetSpecV1Test extends SpecTestBase
{
   private static final String STORAGE_TABLE = "testStorage";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new AvatarGetSpecV1());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new ManageSessionSpecV1(), new UploadsSpecV1(), new PreferenceSectionGetSpecV1(), new PreferenceSectionPatchSpecV1()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = TestUtils.defineInstance();
      TableBasedAuthenticationMetaData auth = new TableBasedAuthenticationMetaData();
      instance.addTable(auth.defineStandardUserTable(TestUtils.BACKEND_NAME_MEMORY));
      instance.addTable(auth.defineStandardSessionTable(TestUtils.BACKEND_NAME_MEMORY));
      new AuditsMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      new DurableJobsMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      instance.addTable(new QTableMetaData()
         .withName(STORAGE_TABLE)
         .withBackendName(TestUtils.BACKEND_NAME_MEMORY)
         .withPrimaryKeyField("reference")
         .withField(new QFieldMetaData("reference", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB)));
      instance.withSupplementalMetaData(new DurableJobsMetaData().withStorageTableName(STORAGE_TABLE));
      new UserProfileMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, STORAGE_TABLE);
      return (instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected void primeTestData(QInstance instance) throws Exception
   {
      super.primeTestData(instance);
      QContext.init(instance, new QSession());
      for(String name : List.of("alice", "bob"))
      {
         TestUtils.insertRecords(instance, instance.getTable("user"), List.of(new QRecord().withValue("username", name)
            .withValue("fullName", name).withValue("passwordHash", TableBasedAuthenticationModule.PasswordHasher.createHashedPassword("test-password"))));
      }
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new TableBasedAuthenticationMetaData());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void noSharedCookies()
   {
      Unirest.config().reset().enableCookieManagement(false).followRedirects(false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   static void resetClient()
   {
      Unirest.config().reset();
   }



   /*******************************************************************************
    ** The whole flow: alice uploads and sets a photo; her session carries its
    ** URL; bob (any signed-in user) can load it; nobody signed out can.
    *******************************************************************************/
   @Test
   void testUploadSetAndServe() throws Exception
   {
      String alice = login("alice").cookie();
      assertThat(login("alice").user().has("avatarUrl")).isFalse();

      JSONObject section = ok(Unirest.get(getBaseUrlAndPath() + "/preferences/profile").header("Cookie", alice).asString());
      JSONObject avatar  = section.getJSONArray("fields").getJSONObject(0);
      assertEquals("avatar", avatar.getString("name"));
      JSONObject adornment = avatar.getJSONArray("adornments").getJSONObject(0);
      assertEquals("FILE_UPLOAD", adornment.getString("type"));
      assertThat(adornment.getJSONObject("values").getBoolean("valueIsUploadRef")).isTrue();

      String ref = upload(alice, "me.png", png());
      ok(patch(alice, "{\"values\":{\"avatar\":\"" + ref + "\",\"displayName\":\"Alice A.\"}}"));

      JSONObject user = login("alice").user();
      assertEquals("Alice A.", user.getString("displayName"));
      String url = user.getString("avatarUrl");
      assertThat(url).startsWith("/qqq/v1/avatars/alice?v=");

      String               bob      = login("bob").cookie();
      HttpResponse<byte[]> response = Unirest.get(getBaseUrlAndPath() + url.substring("/qqq/v1".length()) + "&size=64").header("Cookie", bob).asBytes();
      assertEquals(200, response.getStatus());
      assertEquals("image/png", response.getHeaders().getFirst("Content-Type"));
      assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
      assertThat(response.getHeaders().getFirst("Cache-Control")).contains("immutable");
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getBody()));
      assertEquals(64, image.getWidth());

      ////////////////////////////////////////////////////////////////////
      // without the version: short cache and an ETag that answers 304 //
      ////////////////////////////////////////////////////////////////////
      HttpResponse<byte[]> plain = Unirest.get(getBaseUrlAndPath() + "/avatars/alice").header("Cookie", bob).asBytes();
      assertEquals(200, plain.getStatus());
      assertEquals("private, max-age=300", plain.getHeaders().getFirst("Cache-Control"));
      String etag = plain.getHeaders().getFirst("ETag");
      assertEquals(304, Unirest.get(getBaseUrlAndPath() + "/avatars/alice").header("Cookie", bob).header("If-None-Match", etag).asBytes().getStatus());

      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/avatars/bob").header("Cookie", bob).asString().getStatus());
      assertEquals(400, Unirest.get(getBaseUrlAndPath() + "/avatars/alice?size=7").header("Cookie", bob).asString().getStatus());
      assertEquals(401, Unirest.get(getBaseUrlAndPath() + "/avatars/alice").asString().getStatus());

      ///////////////////////////////////////////////////////////////////
      // an object that is gone from storage is "no avatar", not a 500 //
      ///////////////////////////////////////////////////////////////////
      String key = url.substring(url.indexOf("?v=") + 3);
      QContext.init(serverQInstance, new QSystemUserSession());
      new StorageAction().deleteObject(new StorageInput(STORAGE_TABLE).withReference("avatars/" + key + "/32.png"));
      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/avatars/alice?size=32").header("Cookie", bob).asString().getStatus());
      assertEquals(200, Unirest.get(getBaseUrlAndPath() + "/avatars/alice?size=64").header("Cookie", bob).asBytes().getStatus());

      ok(patch(alice, "{\"values\":{\"avatar\":null}}"));
      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/avatars/alice").header("Cookie", bob).asString().getStatus());
   }



   /*******************************************************************************
    ** An SVG is refused (by content, whatever it's called), and nobody can set
    ** another user's upload as their photo.
    *******************************************************************************/
   @Test
   void testRefusals()
   {
      String alice = login("alice").cookie();
      String svg   = upload(alice, "me.png", "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"/>".getBytes(StandardCharsets.UTF_8));
      HttpResponse<String> refused = patch(alice, "{\"values\":{\"avatar\":\"" + svg + "\"}}");
      assertEquals(400, refused.getStatus());
      assertThat(refused.getBody()).contains("Use a PNG, JPEG, WebP or GIF image.");

      String bob = login("bob").cookie();
      assertEquals(400, patch(bob, "{\"values\":{\"avatar\":\"" + svg + "\"}}").getStatus());
      assertEquals(400, patch(bob, "{\"userId\":\"alice\",\"values\":{\"displayName\":\"x\"}}").getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record Login(String cookie, JSONObject user)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Login login(String name)
   {
      String auth = Base64.getEncoder().encodeToString((name + ":test-password").getBytes(StandardCharsets.UTF_8));
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/manageSession").header("Authorization", "Basic " + auth)
         .header("Content-Type", "application/json").body("{}").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject values = new JSONObject(response.getBody()).getJSONObject("values");
      return (new Login("sessionUUID=" + response.getCookies().getNamed("sessionUUID").getValue(), values.getJSONObject("user")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String upload(String cookie, String name, byte[] bytes)
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/uploads").header("Cookie", cookie)
         .field("file", new ByteArrayInputStream(bytes), name).asString();
      return (ok(response).getString("ref"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> patch(String cookie, String json)
   {
      return (Unirest.patch(getBaseUrlAndPath() + "/preferences/profile").header("Cookie", cookie)
         .header("Content-Type", "application/json").body(json).asString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject ok(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus(), response.getBody());
      return (new JSONObject(response.getBody()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static byte[] png() throws Exception
   {
      BufferedImage image    = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
      Graphics2D    graphics = image.createGraphics();
      graphics.setColor(Color.ORANGE);
      graphics.fillRect(0, 0, 120, 80);
      graphics.dispose();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, "png", out);
      return (out.toByteArray());
   }
}
