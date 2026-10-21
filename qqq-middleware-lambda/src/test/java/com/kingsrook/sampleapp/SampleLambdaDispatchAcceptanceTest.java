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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.amazonaws.services.lambda.runtime.Context;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobState;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.lambda.QAbstractLambdaHandler;
import com.kingsrook.qqq.lambda.QBaseCustomLambdaHandler;
import com.kingsrook.qqq.lambda.QStandardLambdaHandler;
import com.kingsrook.qqq.lambda.model.QLambdaRequest;
import com.kingsrook.qqq.lambda.model.QLambdaResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Local sample consumer of the real Lambda stream/process APIs; no AWS calls.
 *******************************************************************************/
@Timeout(15)
class SampleLambdaDispatchAcceptanceTest
{
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private QInstance instance;
   private final AtomicInteger processCalls = new AtomicInteger();
   private final List<RunProcessInput> processInputs = new ArrayList<>();
   private final UUIDAndTypeStateKey unrelatedProcessKey = new UUIDAndTypeStateKey(UUID.randomUUID(), StateType.PROCESS_STATUS);
   private final UUIDAndTypeStateKey unrelatedJobKey = new UUIDAndTypeStateKey(UUID.randomUUID(), StateType.ASYNC_JOB_STATUS);



   /*******************************************************************************
    ** Each test owns its metadata and identities, using the existing MOCK provider.
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      QContext.clear();
      RunProcessAction.getStateProvider().put(unrelatedProcessKey, "unrelated-process");
      InMemoryStateProvider.getInstance().put(unrelatedJobKey, "unrelated-job");
      instance = new QInstance();
      instance.addBackend(new QBackendMetaData().withName("ownedMemory").withBackendType(MemoryBackendModule.class));
      instance.addTable(new QTableMetaData().withName("owned").withBackendName("ownedMemory")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withPrimaryKeyField("id"));
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData()
         .withName("ownedLambdaMock").withType(QAuthenticationType.MOCK));
      instance.addProcess(new QProcessMetaData().withName("ownedEcho")
         .withStep(new QBackendStepMetaData().withName("echo").withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
         {
            processCalls.incrementAndGet();
            output.addValue("receivedBody", input.getValueString("body"));
            output.addValue("owner", QContext.getQSession().getUser().getIdReference());
            output.addValue("instanceMatches", QContext.getQInstance() == instance);
         }))));
      instance.addProcess(new QProcessMetaData().withName("ownedFailure")
         .withStep(new QBackendStepMetaData().withName("fail").withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
         {
            processCalls.incrementAndGet();
            if("user-facing".equals(input.getValueString("body")))
            {
               throw new QUserFacingException("Owned process refusal");
            }
            throw new QException("Owned process failure");
         }))));
   }



   /*******************************************************************************
    ** Remove only this fixture's state and preserve unrelated entries and context.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         List<UUIDAndTypeStateKey> processKeys = new ArrayList<>();
         List<UUIDAndTypeStateKey> jobKeys = new ArrayList<>();
         for(RunProcessInput input : processInputs)
         {
            processKeys.add(new UUIDAndTypeStateKey(UUID.fromString(input.getProcessUUID()), StateType.PROCESS_STATUS));
            jobKeys.add(ownedJobKey(input));
         }
         processKeys.forEach(key -> RunProcessAction.getStateProvider().remove(key));
         jobKeys.forEach(key -> InMemoryStateProvider.getInstance().remove(key));
         processKeys.forEach(key -> assertThat(RunProcessAction.getStateProvider().get(Serializable.class, key)).isEmpty());
         jobKeys.forEach(key -> assertThat(InMemoryStateProvider.getInstance().get(Serializable.class, key)).isEmpty());
         assertThat(RunProcessAction.getStateProvider().get(String.class, unrelatedProcessKey)).contains("unrelated-process");
         assertThat(InMemoryStateProvider.getInstance().get(String.class, unrelatedJobKey)).contains("unrelated-job");
      }
      finally
      {
         RunProcessAction.getStateProvider().remove(unrelatedProcessKey);
         InMemoryStateProvider.getInstance().remove(unrelatedJobKey);
         QContext.clear();
         QContext.init(previousContext);
         QContext.setObjects(previousObjects);
      }
   }



   /*******************************************************************************
    ** Failed validation still creates a job, but its UUID is not in the response.
    ** Inspect only the callback attached to our exact input, never global state.
    *******************************************************************************/
   private UUIDAndTypeStateKey ownedJobKey(RunProcessInput input) throws Exception
   {
      Field jobUUID = AsyncJobCallback.class.getDeclaredField("jobUUID");
      jobUUID.setAccessible(true);
      return new UUIDAndTypeStateKey((UUID) jobUUID.get(input.getAsyncJobCallback()), StateType.ASYNC_JOB_STATUS);
   }



   /*******************************************************************************
    ** Prove cleanup has real entries to remove, including jobs rejected pre-step.
    *******************************************************************************/
   private void assertOwnedState(int expectedProcesses, int expectedJobs) throws Exception
   {
      int storedProcesses = 0;
      assertEquals(expectedJobs, processInputs.size());
      for(RunProcessInput input : processInputs)
      {
         if(RunProcessAction.getStateProvider().get(Serializable.class,
            new UUIDAndTypeStateKey(UUID.fromString(input.getProcessUUID()), StateType.PROCESS_STATUS)).isPresent())
         {
            storedProcesses++;
         }
         AsyncJobStatus status = InMemoryStateProvider.getInstance().get(AsyncJobStatus.class, ownedJobKey(input)).orElseThrow();
         assertThat(status.getState()).isIn(AsyncJobState.COMPLETE, AsyncJobState.ERROR);
      }
      assertEquals(expectedProcesses, storedProcesses);
   }



   /*******************************************************************************
    ** Parsing and serialization must preserve request identity and custom values.
    *******************************************************************************/
   @Test
   void testCustomStreamRequestResponse() throws Exception
   {
      CustomHandler handler = new CustomHandler();
      JSONObject first = invoke(handler, event("/owned/custom", "POST", "request-one", "{\"value\":\"café λ\"}"));
      assertEquals("request-one", first.getString("requestId"));
      assertEquals(Map.of("value", "café λ", "path", "/owned/custom", "query", "owned=one", "header", "synthetic"), first.getJSONObject("body").toMap());
      assertFalse(first.has("errorMessage"));
      assertFalse(first.has("statusCode"), "The stream writes Body, not an API Gateway proxy envelope");
      JSONObject second = invoke(handler, event("/owned/second", "POST", "request-two", "{\"value\":\"second\"}"));
      assertEquals("request-two", second.getString("requestId"));
      assertEquals("second", second.getJSONObject("body").getString("value"));
      assertEquals("/owned/second", second.getJSONObject("body").getString("path"));
      assertEquals(2, handler.calls);
   }



   /*******************************************************************************
    ** Invalid envelope/body/content type must not reach custom application logic.
    *******************************************************************************/
   @Test
   void testMalformedCustomRequestsNeverDispatch() throws Exception
   {
      CustomHandler handler = new CustomHandler();
      JSONObject malformed = invoke(handler, "not-json");
      assertEquals("unknown", malformed.getString("requestId"));
      assertThat(malformed.getString("errorMessage")).startsWith("Unable to parse input as JSON:");
      JSONObject badBody = invoke(handler, event("/owned/custom", "POST", "bad-body", "not-json"));
      assertEquals("bad-body", badBody.getString("requestId"));
      assertThat(badBody.getString("errorMessage")).startsWith("Unable to parse request body as JSON:");
      JSONObject wrongType = event("/owned/custom", "POST", "wrong-type", "{}");
      wrongType.getJSONObject("headers").put("content-type", "text/plain");
      assertThat(invoke(handler, wrongType).getString("errorMessage")).contains("Unsupported content-type:");
      JSONObject missingContext = event("/owned/custom", "POST", "missing-context", "{}");
      missingContext.remove("requestContext");
      assertThat(invoke(handler, missingContext).getString("errorMessage")).startsWith("Uncaught error handing request:");
      assertEquals(0, handler.calls);
   }



   /*******************************************************************************
    ** Both custom failure branches must return correlated errors, not success data.
    *******************************************************************************/
   @Test
   void testCustomExceptionsPreserveRequestIdentity() throws Exception
   {
      CustomHandler handler = new CustomHandler();
      for(String failure : List.of("user-facing", "internal"))
      {
         JSONObject result = invoke(handler, event("/owned/custom", "POST", failure, new JSONObject().put("fail", failure).toString()));
         assertEquals(failure, result.getString("requestId"));
         assertThat(result.getString("errorMessage")).contains("Owned custom " + failure);
         assertFalse(result.has("body"));
         assertCleanContext();
      }
      assertEquals(2, handler.calls);
   }



   /*******************************************************************************
    ** Real process execution must receive the application's current instance/session.
    *******************************************************************************/
   @Test
   void testConfiguredProcessInitAndRequestIsolation() throws Exception
   {
      ApplicationHandler handler = new ApplicationHandler(instance, session("owner-one"));
      JSONObject first = invoke(handler, event("/processes/ownedEcho/init", "POST", "process-one", "first café"));
      assertProcessResult(first, "process-one", "first café", "owner-one");
      handler.session = session("owner-two");
      JSONObject second = invoke(handler, event("/processes/ownedEcho/init/", "POST", "process-two", "second λ"));
      assertProcessResult(second, "process-two", "second λ", "owner-two");
      assertNotEquals(first.getJSONObject("body").getString("processUUID"), second.getJSONObject("body").getString("processUUID"));
      assertEquals(2, processCalls.get());
      assertOwnedState(2, 2);
   }



   /*******************************************************************************
    ** Missing/invalid application sessions must fail before running the backend step.
    *******************************************************************************/
   @Test
   void testMissingAndInvalidSessionCannotRunProcess() throws Exception
   {
      ApplicationHandler handler = new ApplicationHandler(instance, null);
      JSONObject missing = invoke(handler, event("/processes/ownedEcho/init", "POST", "missing", "ignored"));
      assertThat(missing.getJSONObject("body").getString("error")).contains("QSession was not set in QContext");
      handler.session = session("invalid-owner").withValue("isInvalid", "true");
      JSONObject invalid = invoke(handler, event("/processes/ownedEcho/init", "POST", "invalid", "ignored"));
      assertThat(invalid.getJSONObject("body").getString("error")).contains("Invalid session");
      assertEquals(0, processCalls.get());
      assertOwnedState(0, 2);
      handler.session = session("valid-owner");
      assertProcessResult(invoke(handler, event("/processes/ownedEcho/init", "POST", "valid", "accepted")), "valid", "accepted", "valid-owner");
      assertEquals(1, processCalls.get());
   }



   /*******************************************************************************
    ** setQInstance alone does not initialize context; setupSession is a no-op.
    *******************************************************************************/
   @Test
   void testUnconfiguredStandardHandlerDoesNotSupplyContext() throws Exception
   {
      QStandardLambdaHandler handler = new TrackingStandardHandler();
      handler.setQInstance(instance);
      JSONObject result = invoke(handler, event("/processes/ownedEcho/init", "POST", "unconfigured", "ignored"));
      assertThat(result.getJSONObject("body").getString("error")).contains("QInstance was not set in QContext");
      assertEquals(0, processCalls.get());
      assertOwnedState(0, 1);
   }



   /*******************************************************************************
    ** Process exceptions are body errors; cleanup permits a subsequent good request.
    *******************************************************************************/
   @Test
   void testProcessFailuresAndRecoveryCleanContext() throws Exception
   {
      ApplicationHandler handler = new ApplicationHandler(instance, session("failure-owner"));
      for(String body : List.of("user-facing", "internal"))
      {
         JSONObject result = invoke(handler, event("/processes/ownedFailure/init", "POST", body, body));
         assertEquals(body, result.getString("requestId"));
         JSONObject payload = result.getJSONObject("body");
         assertThat(payload.getString("error")).contains(body.equals("user-facing") ? "Owned process refusal" : "Owned process failure");
         assertEquals(body.equals("user-facing"), payload.has("userFacingError"));
         assertFalse(payload.has("values"));
         assertFalse(payload.has("jobUUID"));
      }
      handler.session = session("recovery-owner");
      assertProcessResult(invoke(handler, event("/processes/ownedEcho/init", "POST", "recovery", "recovered")), "recovery", "recovered", "recovery-owner");
      assertEquals(3, processCalls.get());
      assertOwnedState(3, 3);
   }



   /*******************************************************************************
    ** Unknown route/process cannot accidentally dispatch a registered process.
    *******************************************************************************/
   @Test
   void testUnknownPathAndProcessDoNotExecute() throws Exception
   {
      ApplicationHandler handler = new ApplicationHandler(instance, session("route-owner"));
      JSONObject unknown = invoke(handler, event("/owned/unknown", "POST", "unknown-path", "{}"));
      assertEquals("Unrecognized path: /owned/unknown", unknown.getString("errorMessage"));
      JSONObject process = invoke(handler, event("/processes/notRegistered/init", "POST", "unknown-process", "{}"));
      assertThat(process.getJSONObject("body").getString("error")).contains("Process [notRegistered] is not defined");
      assertEquals(0, processCalls.get());
   }



   /*******************************************************************************
    ** TODO branches remain unavailable; this does not certify table/widget CRUD.
    *******************************************************************************/
   @Test
   void testUnimplementedStandardRoutesStayUnavailable() throws Exception
   {
      ApplicationHandler handler = new ApplicationHandler(instance, session("route-owner"));
      for(String path : List.of("/metaData", "/metaData/table/owned", "/metaData/process/ownedEcho",
         "/data/table/owned", "/data/table/owned/query", "/data/table/owned/count",
         "/data/table/owned/export", "/data/table/owned/export/file", "/data/table/owned/prossibleValues/name"))
      {
         JSONObject result = invoke(handler, event(path, "GET", "unavailable", "{}"));
         assertEquals("Unrecognized path: " + path, result.getString("errorMessage"));
         assertFalse(result.has("body"));
      }
      for(String method : List.of("GET", "PATCH", "PUT", "DELETE"))
      {
         JSONObject result = invoke(handler, event("/data/table/owned/1", method, "unavailable", "{}"));
         assertEquals("Unrecognized path: /data/table/owned/1", result.getString("errorMessage"));
      }
      assertEquals("Internal Server Error", invoke(handler, event("/widget/owned", "GET", "widget", "{}")).getString("errorMessage"));
      assertEquals(0, processCalls.get());
   }



   /*******************************************************************************
    ** All requests are synthetic, in memory, and use the public stream entrypoint.
    *******************************************************************************/
   private JSONObject invoke(QAbstractLambdaHandler handler, JSONObject event) throws IOException
   {
      return invoke(handler, event.toString());
   }



   /*******************************************************************************
    ** Assert cleanup before returning, so test teardown cannot hide a request leak.
    *******************************************************************************/
   private JSONObject invoke(QAbstractLambdaHandler handler, String event) throws IOException
   {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      handler.handleRequest(new ByteArrayInputStream(event.getBytes(StandardCharsets.UTF_8)), output, null);
      assertCleanContext();
      return new JSONObject(output.toString(StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** Minimal Lambda URL-shaped event, not an AWS integration fixture.
    *******************************************************************************/
   private JSONObject event(String path, String method, String requestId, String body)
   {
      return new JSONObject().put("rawPath", path).put("rawQueryString", "owned=one")
         .put("headers", new JSONObject().put("content-type", "application/json").put("x-owned", "synthetic"))
         .put("requestContext", new JSONObject().put("requestId", requestId).put("http", new JSONObject().put("method", method)))
         .put("body", body);
   }



   /*******************************************************************************
    ** A configured test identity for the existing MOCK provider, not authentication.
    *******************************************************************************/
   private QSession session(String owner)
   {
      return new QSession().withUser(new QUser().withIdReference(owner)).withPermissions();
   }



   /*******************************************************************************
    ** Assert real backend outputs, correlation and the absence of error/async data.
    *******************************************************************************/
   private void assertProcessResult(JSONObject result, String requestId, String body, String owner)
   {
      assertEquals(requestId, result.getString("requestId"));
      assertFalse(result.has("errorMessage"));
      JSONObject payload = result.getJSONObject("body");
      assertFalse(payload.has("error"));
      assertFalse(payload.has("jobUUID"));
      UUID.fromString(payload.getString("processUUID"));
      JSONObject values = payload.getJSONObject("values");
      assertEquals(body, values.getString("receivedBody"));
      assertEquals(owner, values.getString("owner"));
      assertEquals(true, values.getBoolean("instanceMatches"));
   }



   /*******************************************************************************
    ** Check caller cleanup before any @AfterEach restoration.
    *******************************************************************************/
   private void assertCleanContext()
   {
      assertNull(QContext.getQInstance());
      assertNull(QContext.getQSession());
      assertNull(QContext.getQBackendTransaction());
      assertNull(QContext.getObjects());
   }



   /*******************************************************************************
    ** A custom application handler; the base class still parses and writes streams.
    *******************************************************************************/
   private static class CustomHandler extends QBaseCustomLambdaHandler
   {
      private int calls;

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      protected QLambdaResponse handleJsonRequest(QLambdaRequest request, JSONObject body) throws QException
      {
         calls++;
         if(body.has("fail"))
         {
            if("user-facing".equals(body.getString("fail")))
            {
               throw new QUserFacingException("Owned custom user-facing");
            }
            throw new QException("Owned custom internal");
         }
         QLambdaResponse response = new QLambdaResponse(201);
         response.getBody().setBody(Map.of("value", body.getString("value"), "path", request.getPath(),
            "query", request.getQueryString(), "header", request.getHeaders().getString("x-owned")));
         return response;
      }
   }



   /*******************************************************************************
    ** Retain the real input before validation, without supplying a session/context.
    *******************************************************************************/
   private class TrackingStandardHandler extends QStandardLambdaHandler
   {
      /***************************************************************************
       ** The production handler later attaches the process UUID and job callback.
       ***************************************************************************/
      @Override
      protected void setupSession(QLambdaRequest request, AbstractActionInput input)
      {
         if(input instanceof RunProcessInput processInput)
         {
            processInputs.add(processInput);
         }
         super.setupSession(request, input);
      }
   }



   /*******************************************************************************
    ** Explicit application-owned session/context wiring; not supplied by Lambda.
    *******************************************************************************/
   private class ApplicationHandler extends TrackingStandardHandler
   {
      private QSession session;

      /***************************************************************************
       **
       ***************************************************************************/
      private ApplicationHandler(QInstance instance, QSession session)
      {
         setQInstance(instance);
         this.session = session;
      }

      /***************************************************************************
       ** A request boundary owns cleanup even when parsing or process work fails.
       ***************************************************************************/
      @Override
      public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException
      {
         QContext.clear();
         try
         {
            super.handleRequest(input, output, context);
         }
         finally
         {
            QContext.clear();
         }
      }

      /***************************************************************************
       ** Supply the already-resolved application session; use real core validation.
       ***************************************************************************/
      @Override
      protected void setupSession(QLambdaRequest request, AbstractActionInput input)
      {
         super.setupSession(request, input);
         QContext.init(qInstance, session);
         QContext.setObject("ownedLambdaRequest", request.getRequestContext().getString("requestId"));
      }
   }
}
