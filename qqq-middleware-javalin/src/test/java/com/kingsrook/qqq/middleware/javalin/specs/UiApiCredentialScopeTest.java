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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QCredentialScope;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Credential scopes over HTTP: a session created from a scoped credential
 ** (set here by an authentication customizer, as qbit-authentik does for access
 ** tokens) sees and reads only its tables, cannot write when read-only, is
 ** refused operations outside its operation ids, and is labelled token:<id>.
 *******************************************************************************/
class UiApiCredentialScopeTest
{
   private static final int    PORT   = 6275;
   private static final String NOTES  = "scopedNote";
   private static final String OTHER  = "otherNote";

   private QApplicationJavalinServer javalinServer;



   /***************************************************************************
    ** gives sessions named scoped-* a read-only scope over the notes table
    ***************************************************************************/
   public static class ScopeCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         if(qSession.getUuid() != null && qSession.getUuid().startsWith("scoped-"))
         {
            qSession.withCredentialScope(new QCredentialScope()
               .withCredentialId("tok-42")
               .withReadOnly(true)
               .withTables(Set.of(NOTES))
               .withOperationIds(Set.of("GET /metaData", "POST /table/{tableName}/query", "POST /table/{tableName}/count", "GET /table/{tableName}/{primaryKey}",
                  "POST /table/{tableName}", "DELETE /table/{tableName}/{primaryKey}")));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class ScopeApplication extends AbstractQQQApplication
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QInstance defineQInstance()
      {
         QInstance qInstance = TestUtils.defineInstance();
         qInstance.getAuthentication().withCustomizer(new QCodeReference(ScopeCustomizer.class));
         for(String name : List.of(NOTES, OTHER))
         {
            qInstance.addTable(new QTableMetaData()
               .withName(name)
               .withBackendName(TestUtils.BACKEND_NAME_MEMORY)
               .withPrimaryKeyField("id")
               .withField(new QFieldMetaData("id", QFieldType.INTEGER))
               .withField(new QFieldMetaData("title", QFieldType.STRING)));
         }
         return (qInstance);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      if(javalinServer != null)
      {
         javalinServer.stop();
      }
      QJavalinImplementation.setSessionCookieHttpOnly(false);
      MemoryRecordStore.fullReset();
      Unirest.config().reset();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private HttpResponse<String> call(String method, String path, String session, String body)
   {
      String url = "http://localhost:" + PORT + "/qqq/v1" + path;
      return (switch(method)
      {
         case "GET" -> Unirest.get(url).header("Cookie", "sessionUUID=" + session).header("X-QQQ-Client", "next-ui").asString();
         case "DELETE" -> Unirest.delete(url).header("Cookie", "sessionUUID=" + session).asString();
         default -> Unirest.post(url).header("Cookie", "sessionUUID=" + session).header("Content-Type", "application/json").body(body == null ? "{}" : body).asString();
      });
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testScopedSessions()
   {
      javalinServer = new QApplicationJavalinServer(new ScopeApplication())
         .withPort(PORT)
         .withServeFrontendNext(false)
         .withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false);
      try
      {
         javalinServer.start();
      }
      catch(Exception e)
      {
         throw (new RuntimeException(e));
      }

      ////////////////////////////////////////////////////////
      // an unscoped session (same user model) sees both   //
      ////////////////////////////////////////////////////////
      JSONObject plainTables = new JSONObject(call("GET", "/metaData", "plain-session", null).getBody()).getJSONObject("tables");
      assertTrue(plainTables.has(NOTES) && plainTables.has(OTHER));
      assertEquals(200, call("POST", "/table/" + OTHER + "/query", "plain-session", null).getStatus());

      ////////////////////////////////////////////////////////////////////
      // the scoped session: only its table, read-only, only its ops    //
      ////////////////////////////////////////////////////////////////////
      JSONObject scopedTables = new JSONObject(call("GET", "/metaData", "scoped-1", null).getBody()).getJSONObject("tables");
      assertTrue(scopedTables.has(NOTES));
      assertFalse(scopedTables.has(OTHER));

      assertEquals(200, call("POST", "/table/" + NOTES + "/query", "scoped-1", null).getStatus());
      assertEquals(200, call("POST", "/table/" + NOTES + "/count", "scoped-1", null).getStatus());
      assertEquals(403, call("POST", "/table/" + OTHER + "/query", "scoped-1", null).getStatus());

      /////////////////////////////////////////////////////////////////////
      // read-only: insert refused even though the operation is listed  //
      /////////////////////////////////////////////////////////////////////
      assertEquals(403, call("POST", "/table/" + NOTES, "scoped-1", "{\"values\": {\"id\": 9, \"title\": \"x\"}}").getStatus());

      ////////////////////////////////////////////////////
      // an operation outside the scope's operation ids //
      ////////////////////////////////////////////////////
      assertEquals(403, call("GET", "/metaData/table/" + NOTES, "scoped-1", null).getStatus());

      /////////////////////////////////////////////////////////////////////////
      // the channel is token:<id> whatever the request says (next-ui above) //
      /////////////////////////////////////////////////////////////////////////
      QSession session = new QSession().withCredentialScope(new QCredentialScope().withCredentialId("tok-42"));
      assertEquals("token:tok-42", session.getCredentialScope().getClientChannel());
      assertEquals("token", new QSession().withClientChannel("token:tok-42").getClientChannelName());
   }
}
