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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.ProcessInitSpecV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.ProcessStepSpecV1;
import io.javalin.http.Context;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Explicit JSON nulls remain field clears through native multipart process calls.
 *******************************************************************************/
class ProcessNullValuesHttpTest extends SpecTestBase
{
   private static final String PROCESS_NAME = "nullableNativeInput";



   /***************************************************************************
    ** Register the same process init and step routes used by native clients.
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessInitSpecV1();
   }



   /***************************************************************************
    ** Include the actual step route rather than calling the parser alone.
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new ProcessStepSpecV1());
   }



   /***************************************************************************
    ** The correction applies to the existing versioned contract.
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** Observe presence separately from value so omission cannot masquerade as clear.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = super.defineQInstance();
      instance.addProcess(new QProcessMetaData().withName(PROCESS_NAME)
         .withStep(new QFrontendStepMetaData().withName("input")
            .withFormField(new QFieldMetaData("sessionId", QFieldType.STRING))
            .withFormField(new QFieldMetaData("reason", QFieldType.STRING)))
         .withStep(new QBackendStepMetaData().withName("observe")
            .withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
            {
               output.addValue("reasonPresent", input.getValues().containsKey("reason"));
               output.addValue("reasonNull", input.getValue("reason") == null);
               output.addValue("reasonSeen", input.getValue("reason") == null ? "<null>" : input.getValueString("reason"));
            })))
         .withStep(new QFrontendStepMetaData().withName("result")));
      return instance;
   }



   /***************************************************************************
    ** A nullable optional reason must not fail before process execution.
    ***************************************************************************/
   @Test
   void testNativeStepPreservesExplicitNullOmissionAndText()
   {
      for(String values : List.of("{\"sessionId\":\"fixture-session\",\"reason\":null}",
         "{\"sessionId\":\"fixture-session\"}", "{\"sessionId\":\"fixture-session\",\"reason\":\"operator request\"}"))
      {
         JSONObject result = step("{}", values);
         boolean present = new JSONObject(values).has("reason");
         assertEquals(present, result.getBoolean("reasonPresent"));
         assertEquals(!values.contains("operator request"), result.getBoolean("reasonNull"));
         assertEquals(values.contains("operator request") ? "operator request" : "<null>", result.getString("reasonSeen"));
      }
   }



   /***************************************************************************
    ** Null clears an existing value; omission preserves that existing value.
    ***************************************************************************/
   @Test
   void testExplicitNullClearsExistingValueWithoutDroppingTheKey()
   {
      JSONObject cleared = step("{\"reason\":\"existing\"}", "{\"reason\":null}");
      assertTrue(cleared.getBoolean("reasonPresent"));
      assertTrue(cleared.getBoolean("reasonNull"));
      JSONObject retained = step("{\"reason\":\"existing\"}", "{}");
      assertTrue(retained.getBoolean("reasonPresent"));
      assertFalse(retained.getBoolean("reasonNull"));
      assertEquals("existing", retained.getString("reasonSeen"));
   }



   /***************************************************************************
    ** Scalar types and existing unsupported structured-value handling are unchanged.
    ***************************************************************************/
   @Test
   void testParserKeepsNullKeyAndOtherScalarTypes()
   {
      Map<String, Serializable> values = parse("{\"reason\":null,\"count\":2,\"enabled\":false,\"label\":\"test\"}");
      assertTrue(values.containsKey("reason"));
      assertNull(values.get("reason"));
      assertFalse(values.containsKey("omitted"));
      assertEquals(2, values.get("count"));
      assertEquals(false, values.get("enabled"));
      assertEquals("test", values.get("label"));
      assertThrows(QRuntimeException.class, () -> parse("{\"reason\":{}}"));
      assertThrows(QRuntimeException.class, () -> parse("{\"reason\":[]}"));
   }



   /***************************************************************************
    ** Execute real multipart init and step requests on the isolated test server.
    ***************************************************************************/
   private JSONObject step(String initialValues, String values)
   {
      HttpResponse<String> initialized = Unirest.post(getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/init")
         .cookie("sessionId", "nullable-input-fixture")
         .multiPartContent().field("values", initialValues).field("stepTimeoutMillis", "10000").asString();
      assertEquals(200, initialized.getStatus());
      JSONObject process = new JSONObject(initialized.getBody());
      assertEquals("input", process.getString("nextStep"));
      HttpResponse<String> submitted = Unirest.post(getBaseUrlAndPath() + "/processes/" + PROCESS_NAME + "/" + process.getString("processUUID") + "/step/input")
         .cookie("sessionId", "nullable-input-fixture")
         .multiPartContent().field("values", values).field("stepTimeoutMillis", "10000").asString();
      assertEquals(200, submitted.getStatus());
      JSONObject response = new JSONObject(submitted.getBody());
      assertEquals("result", response.getString("nextStep"));
      return response.getJSONObject("values");
   }



   /***************************************************************************
    ** Exercise the shared parser without replacing the production HTTP route.
    ***************************************************************************/
   private Map<String, Serializable> parse(String values)
   {
      Context context = mock(Context.class);
      when(context.contentType()).thenReturn("multipart/form-data; boundary=fixture");
      when(context.formParam("values")).thenReturn(values);
      return getSpec().getRequestParamMap(context, "values");
   }
}
