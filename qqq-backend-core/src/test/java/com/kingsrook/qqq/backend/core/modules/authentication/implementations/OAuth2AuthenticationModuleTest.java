/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for OAuth2AuthenticationModule
 *******************************************************************************/
class OAuth2AuthenticationModuleTest extends BaseTest
{
   /***************************************************************************
    ** Test that the customizer is called when configured
    ***************************************************************************/
   @Test
   void testCustomizerCalledOnCreateSessionFromToken() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      OAuth2AuthenticationMetaData authMetaData = new OAuth2AuthenticationMetaData();
      authMetaData.setName("oauth2");
      authMetaData.setBaseUrl("https://example.com");
      authMetaData.setClientId("test-client");
      authMetaData.setClientSecret("test-secret");
      authMetaData.setCustomizer(new QCodeReference(TestCustomizer.class));
      qInstance.setAuthentication(authMetaData);

      //////////////////////////////////////////////////////////////////////////
      // Create a test JWT token with claims                                  //
      // JWT format: header.payload.signature (we only need header + payload) //
      //////////////////////////////////////////////////////////////////////////
      JSONObject header = new JSONObject();
      header.put("alg", "none");
      header.put("typ", "JWT");

      JSONObject payload = new JSONObject();
      payload.put("sub", "test-user-id");
      payload.put("email", "test@example.com");
      payload.put("name", "Test User");
      payload.put("exp", Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond());
      payload.put("iat", Instant.now().getEpochSecond());
      payload.put("testClaim", "customValue");

      String headerBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toString().getBytes());
      String payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toString().getBytes());
      String testJwt = headerBase64 + "." + payloadBase64 + ".fakesignature";

      ///////////////////////////////////////////////////////////////////////
      // Use reflection to call the private createSessionFromToken method  //
      ///////////////////////////////////////////////////////////////////////
      OAuth2AuthenticationModule module = new OAuth2AuthenticationModule();
      var method = OAuth2AuthenticationModule.class.getDeclaredMethod("createSessionFromToken", String.class);
      method.setAccessible(true);
      QSession session = (QSession) method.invoke(module, testJwt);

      ///////////////////////////////////////////////////////////////
      // Verify the session was created with user info from the JWT //
      ///////////////////////////////////////////////////////////////
      assertNotNull(session);
      assertNotNull(session.getUser());
      assertEquals("test@example.com", session.getUser().getIdReference());
      assertEquals("Test User", session.getUser().getFullName());

      ///////////////////////////////////////////////////////////////
      // Verify the customizer was called and set the security key //
      ///////////////////////////////////////////////////////////////
      assertTrue(session.hasSecurityKeyValue("testSecurityKey", "fromCustomizer"));
   }



   /*******************************************************************************
    ** Test customizer that sets a security key when customizeSession is called
    *******************************************************************************/
   public static class TestCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         ///////////////////////////////////////////////////////////
         // Verify we received the JWT payload in context         //
         ///////////////////////////////////////////////////////////
         if(context.containsKey("jwtPayloadJsonObject"))
         {
            JSONObject jwtPayload = (JSONObject) context.get("jwtPayloadJsonObject");
            if(jwtPayload.has("testClaim"))
            {
               /////////////////////////////////////////////////////
               // Set a security key to prove the customizer ran  //
               /////////////////////////////////////////////////////
               qSession.withSecurityKeyValue("testSecurityKey", "fromCustomizer");
            }
         }
      }
   }

}
