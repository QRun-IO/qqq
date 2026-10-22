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


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.routeproviders.ProcessBasedRouterPayload;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TableFormAdjusterSpecV1
 *******************************************************************************/
class TableFormAdjusterSpecV1Test extends SpecTestBase
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new TableFormAdjusterSpecV1());
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
      QInstance instance = super.defineQInstance();
      instance.addProcess(new QProcessMetaData()
         .withName(TableFormAdjusterSpecV1.FORM_ADJUSTER_PROCESS_NAME)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withStep(new QBackendStepMetaData().withName("execute").withCode(new QCodeReference(EchoAdjusterStep.class))));
      return (instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> post(String path, Map<String, Object> body)
   {
      return (Unirest.post(getBaseUrlAndPath() + path)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(body))
         .asString());
   }



   /*******************************************************************************
    ** onChange: the adjuster gets the path and the body (newValue as text,
    ** allValues as JSON) and its output comes back as the response.
    *******************************************************************************/
   @Test
   void testOnChangeRunsAdjuster()
   {
      Map<String, Object> allValues = new LinkedHashMap<>();
      allValues.put("firstName", "Darin");
      allValues.put("noOfShoes", 3);

      HttpResponse<String> response = post("/table/person/formAdjuster/person.firstName/onChange",
         Map.of("fieldName", "firstName", "newValue", 42, "allValues", allValues));
      assertEquals(200, response.getStatus(), response.getBody());

      JSONObject output = new JSONObject(response.getBody());
      JSONObject values = output.getJSONObject("updatedFieldValues");
      assertEquals("person.firstName", values.getString("identifier"));
      assertEquals("onChange", values.getString("event"));
      assertEquals("firstName", values.getString("fieldName"));
      assertEquals("42", values.getString("newValue"));
      assertEquals("Darin", new JSONObject(values.getString("allValues")).getString("firstName"));
      assertEquals(List.of("lastName"), output.getJSONArray("fieldsToClear").toList());
      assertFalse(output.getBoolean("isFormDisabled"));
   }



   /*******************************************************************************
    ** onLoad with an empty body: no field, allValues defaults to {}.
    *******************************************************************************/
   @Test
   void testOnLoadEmptyBody()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person/formAdjuster/table:person/onLoad").asString();
      assertEquals(200, response.getStatus(), response.getBody());

      JSONObject values = new JSONObject(response.getBody()).getJSONObject("updatedFieldValues");
      assertEquals("table:person", values.getString("identifier"));
      assertEquals("onLoad", values.getString("event"));
      assertFalse(values.has("fieldName"));
      assertFalse(values.has("newValue"));
      assertEquals("{}", values.getString("allValues"));
   }



   /*******************************************************************************
    ** the adjuster runs as the caller, not as the system user.
    *******************************************************************************/
   @Test
   void testRunsInCallersSession()
   {
      HttpResponse<String> response = post("/table/person/formAdjuster/table:person/onLoad", Map.of());
      assertEquals(200, response.getStatus(), response.getBody());
      String sessionClass = new JSONObject(response.getBody()).getJSONObject("updatedFieldDisplayValues").getString("sessionClass");
      assertFalse(sessionClass.equals(QSystemUserSession.class.getSimpleName()), sessionClass);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownEvent()
   {
      HttpResponse<String> response = post("/table/person/formAdjuster/table:person/onSave", Map.of());
      assertEquals(400, response.getStatus(), response.getBody());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownTableIsForbidden()
   {
      HttpResponse<String> response = post("/table/notATable/formAdjuster/table:notATable/onLoad", Map.of());
      assertEquals(403, response.getStatus(), response.getBody());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoInsertOrEditPermissionIsForbidden()
   {
      QPermissionRules original = serverQInstance.getTable("person").getPermissionRules();
      serverQInstance.getTable("person").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      try
      {
         HttpResponse<String> response = post("/table/person/formAdjuster/table:person/onLoad", Map.of());
         assertEquals(403, response.getStatus(), response.getBody());
      }
      finally
      {
         serverQInstance.getTable("person").setPermissionRules(original);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoAdjusterProcessIsNotFound()
   {
      serverQInstance.getProcesses().remove(TableFormAdjusterSpecV1.FORM_ADJUSTER_PROCESS_NAME);
      HttpResponse<String> response = post("/table/person/formAdjuster/table:person/onLoad", Map.of());
      assertEquals(404, response.getStatus(), response.getBody());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAdjusterFailureIsServerError()
   {
      HttpResponse<String> response = post("/table/person/formAdjuster/fail/onLoad", Map.of());
      assertEquals(500, response.getStatus(), response.getBody());
      assertTrue(response.getBody().contains("No form adjuster"), response.getBody());
   }



   /*******************************************************************************
    ** stands in for the Material Dashboard's RunFormAdjusterProcess: echoes its
    ** input back as updatedFieldValues.
    *******************************************************************************/
   public static class EchoAdjusterStep implements BackendStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         ProcessBasedRouterPayload payload    = runBackendStepInput.getProcessPayload(ProcessBasedRouterPayload.class);
         String                    identifier = payload.getPathParams().get("identifier");
         if("fail".equals(identifier))
         {
            throw (new QException("No form adjuster found for identifier: fail"));
         }

         Map<String, Serializable> values = new LinkedHashMap<>();
         values.put("identifier", identifier);
         values.put("event", payload.getPathParams().get("event"));
         for(String name : List.of("fieldName", "newValue", "allValues"))
         {
            if(payload.getFormParam(name) != null)
            {
               values.put(name, payload.getFormParam(name));
            }
         }

         Map<String, Object> output = new LinkedHashMap<>();
         output.put("updatedFieldValues", values);
         output.put("updatedFieldDisplayValues", Map.of("sessionClass", QContext.getQSession().getClass().getSimpleName()));
         output.put("fieldsToClear", List.of("lastName"));
         output.put("isFormDisabled", false);
         payload.setResponseString(JsonUtils.toJson(output));
         runBackendStepOutput.setProcessPayload(payload);
      }
   }

}
