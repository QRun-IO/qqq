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

package com.kingsrook.qqq.middleware.javalin;


import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.TableBasedAuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import kong.unirest.Cookie;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Tests of QJavalinImplementation, but specifically, of the authentication
 ** code - which uses a different qInstance, and hence javalin server instance
 ** than the other tests in this package - hence its own before/after, etc.
 *******************************************************************************/
public class QJavalinImplementationAuthenticationTest extends QJavalinTestBase
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws QInstanceValidationException
   {
      Unirest.config().reset().enableCookieManagement(false);
      setupTableBasedAuthenticationInstance();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   public static void afterAll()
   {
      if(qJavalinImplementation != null)
      {
         qJavalinImplementation.stopJavalinServer();
      }
      Unirest.config().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthentication_noCredentialsProvided()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData").asString();
      assertEquals(401, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("Session ID was not provided", jsonObject.getString("error"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthentication_basicAuthSuccess()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData")
         .header("Authorization", "Basic " + encodeBasicAuth("juser", "987zyx"))
         .asString();
      assertEquals(200, response.getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthentication_basicAuthBadCredentials()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData")
         .header("Authorization", "Basic " + encodeBasicAuth("not-juser", "987zyx"))
         .asString();
      assertEquals(401, response.getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthentication_authorizationNotBasic()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData")
         .header("Authorization", "not-Basic " + encodeBasicAuth("juser", "987zyx"))
         .asString();
      assertEquals(401, response.getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthentication_basicAuthSuccessThenSessionIdFromCookie()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData")
         .header("Authorization", "Basic " + encodeBasicAuth("juser", "987zyx"))
         .asString();
      assertEquals(200, response.getStatus());

      Cookies       cookies            = response.getCookies();
      String        sessionId          = cookies.getNamed("sessionId").getValue();
      ZonedDateTime originalExpiration = cookies.getNamed("sessionId").getExpiration();
      assertNotNull(sessionId);

      SleepUtils.sleep(1, TimeUnit.SECONDS);

      response = Unirest.get(BASE_URL + "/metaData")
         .cookie(new Cookie("sessionId", sessionId))
         .asString();
      assertEquals(200, response.getStatus());
      assertEquals(sessionId, response.getCookies().getNamed("sessionId").getValue());
      assertNotEquals(originalExpiration, response.getCookies().getNamed("sessionId").getExpiration());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthentication_badSessionIdCookie()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData")
         .cookie(new Cookie("sessionId", "not-a-sessionId"))
         .asString();
      assertEquals(401, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("Session not found.", jsonObject.getString("error"));
   }



   /*******************************************************************************
    ** Process init and step authenticate like every other route: a missing or
    ** unknown session is a 401, not a process error with HTTP 200.
    *******************************************************************************/
   @Test
   void testAuthentication_processInitAndStepRequireSession()
   {
      HttpResponse<String> init = Unirest.post(BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE + "/init").asString();
      assertEquals(401, init.getStatus());
      assertFalse(init.getBody().contains("Error message:"));

      HttpResponse<String> step = Unirest.post(BASE_URL + "/processes/" + TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE + "/some-uuid/step/setup")
         .cookie(new Cookie("sessionId", "not-a-sessionId"))
         .asString();
      assertEquals(401, step.getStatus());
      assertFalse(step.getBody().contains("Error message:"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void setupTableBasedAuthenticationInstance() throws QInstanceValidationException
   {
      QInstance                        qInstance                        = TestUtils.defineInstance();
      TableBasedAuthenticationMetaData tableBasedAuthenticationMetaData = new TableBasedAuthenticationMetaData();
      qInstance.addTable(tableBasedAuthenticationMetaData.defineStandardUserTable(TestUtils.BACKEND_NAME_MEMORY));
      qInstance.addTable(tableBasedAuthenticationMetaData.defineStandardSessionTable(TestUtils.BACKEND_NAME_MEMORY));
      QContext.init(qInstance, new QSession());

      try
      {
         TestUtils.insertRecords(qInstance, qInstance.getTable("user"), List.of(new QRecord()
            .withValue("username", "juser")
            .withValue("fullName", "Johnny User")
            .withValue("passwordHash", TableBasedAuthenticationModule.PasswordHasher.createHashedPassword("987zyx"))));
      }
      catch(Exception e)
      {
         fail("Error inserting test user.", e);
      }

      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), tableBasedAuthenticationMetaData);

      restartServerWithInstance(qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String encodeBasicAuth(String username, String password)
   {
      Base64.Encoder encoder        = Base64.getEncoder();
      String         originalString = username + ":" + password;
      return (encoder.encodeToString(originalString.getBytes()));
   }
}
