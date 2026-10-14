/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.RecordListWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEvent;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEventType;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessOutcome;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiInvoker;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiRequest;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiResponse;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Field access contract through the same Javalin harness used by v1 spec tests.
 *******************************************************************************/
class FieldAccessUiApiTest extends SpecTestBase
{
   private static final String SENTINEL = "SENTINEL-PHI-7f3a91c2";
   private static final Map<String, FieldAccessDecision> USERS = new ConcurrentHashMap<>();
   private static final List<FieldAccessEvent> EVENTS = new CopyOnWriteArrayList<>();
   private final SentinelResponseScanner scanner = new SentinelResponseScanner();
   private String user;



   /*******************************************************************************
    ** Query plus every related egress endpoint under test.
    *******************************************************************************/
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new TableQuerySpecV1());
   }



   /*******************************************************************************
    ** Register actual endpoint specs, without bypassing middleware authentication.
    *******************************************************************************/
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new TableMetaDataSpecV1(), new TableGetSpecV1(), new TableCountSpecV1(), new TableExportSpecV1(),
         new ProcessInitSpecV1(), new ProcessRecordsSpecV1(), new WidgetSpecV1(), new RecordSearchSpecV1(), new StandalonePossibleValuesSpecV1()));
   }



   /*******************************************************************************
    ** Current API major.
    *******************************************************************************/
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** Static session-keyed resolver shared with server request threads.
    *******************************************************************************/
   protected QInstance defineQInstance() throws QException
   {
      USERS.clear();
      EVENTS.clear();
      QInstance instance = TestUtils.defineInstance();
      instance.setEnforcePermissionsInCoreActions(false);
      instance.getTable("person").withSearchFields("firstName", "lastName").withRecordLabelFields("lastName").withRecordLabelFormat("%s");
      FieldAccessResolverMetaData.ofOrWithNew(instance).withResolverInstance(new TestResolver());
      instance.addWidget(RecordListWidgetRenderer.widgetMetaDataBuilder("securedRecords").withTableName("person").withFilter(new QQueryFilter()).getWidgetMetaData());
      instance.addProcess(new QProcessMetaData().withName("securedProcess").withTableName("person")
         .withStep(new QBackendStepMetaData().withName("records").withCode(new QCodeReference(RecordsStep.class))));
      new AuditsMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      return (instance);
   }



   /*******************************************************************************
    ** Seed real, recognizable sensitive values and an audit history row.
    *******************************************************************************/
   protected void primeTestData(QInstance instance) throws Exception
   {
      super.primeTestData(instance);
      QContext.init(instance, new QSystemUserSession());
      new UpdateAction().execute(new UpdateInput("person").withRecords(List.of(new QRecord().withValue("id", 1).withValue("lastName", SENTINEL))));
      AuditAction.execute("person", 1, Map.of(), SENTINEL, List.of(new QRecord().withValue("fieldName", "lastName")
         .withValue("oldValue", SENTINEL).withValue("newValue", SENTINEL).withValue("message", "Changed " + SENTINEL)));
      QContext.clear();
   }



   /*******************************************************************************
    ** Exercise FULL egress.
    *******************************************************************************/
   @Test
   void full() throws Exception
   {
      egress(FieldAccessTier.FULL);
   }



   /*******************************************************************************
    ** Exercise REVEALABLE egress.
    *******************************************************************************/
   @Test
   void revealable() throws Exception
   {
      egress(FieldAccessTier.REVEALABLE);
   }



   /*******************************************************************************
    ** Exercise MASKED egress.
    *******************************************************************************/
   @Test
   void masked() throws Exception
   {
      egress(FieldAccessTier.MASKED);
   }



   /*******************************************************************************
    ** Exercise HIDDEN egress.
    *******************************************************************************/
   @Test
   void hidden() throws Exception
   {
      egress(FieldAccessTier.HIDDEN);
   }



   /*******************************************************************************
    ** All four tiers exercise records, metadata, search, process, widgets and history.
    *******************************************************************************/
   void egress(FieldAccessTier tier) throws Exception
   {
      user = tier.name();
      USERS.put(user, new FieldAccessDecision(tier, FieldMaskStyle.FULL, "test"));
      JSONObject metadata = new JSONObject(get("/metaData/table/person", 200).getBody());
      String metadataText = metadata.toString();
      if(tier == FieldAccessTier.HIDDEN)
      {
         assertFalse(metadataText.contains("\"lastName\":{"));
      }
      else if(tier != FieldAccessTier.FULL)
      {
         assertTrue(metadataText.contains("\"accessTier\":\"" + tier + "\""));
         assertTrue(metadataText.contains("\"isSortable\":false"));
         assertTrue(metadataText.contains("allowedFilterOperators"));
      }
      if(tier != FieldAccessTier.FULL)
      {
         assertTrue(metadataText.contains("\"hasRestrictedFields\":true"));
      }
      HttpResponse<String> query = post("/table/person/query", Map.of(), 200);
      assertRecord(tier, new JSONObject(query.getBody()).getJSONArray("records").getJSONObject(0));
      assertRecord(tier, new JSONObject(get("/table/person/1", 200).getBody()).getJSONObject("record"));
      QQueryFilter exact = new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, SENTINEL));
      int exactStatus = tier == FieldAccessTier.FULL || tier == FieldAccessTier.REVEALABLE ? 200 : 403;
      post("/table/person/count", Map.of("filter", exact), exactStatus);
      post("/table/person/query", Map.of("filter", new QQueryFilter().withOrderBy(new QFilterOrderBy("lastName"))), tier == FieldAccessTier.FULL ? 200 : 403);
      if(tier == FieldAccessTier.REVEALABLE)
      {
         assertRecord(tier, new JSONObject(post("/table/person/query", Map.of("filter", exact), 200).getBody()).getJSONArray("records").getJSONObject(0));
      }
      post("/table/person/export", Map.of("format", "csv"), 200);
      if(tier == FieldAccessTier.MASKED || tier == FieldAccessTier.HIDDEN)
      {
         post("/table/person/export", Map.of("format", "csv", "filter", exact), 403);
      }
      HttpResponse<String> init = Unirest.post(getBaseUrlAndPath() + "/processes/securedProcess/init").cookie("sessionId", user)
         .multiPartContent().field("recordsParam", "recordIds").field("recordIds", "1").asString();
      capture(init, 200);
      String uuid = new JSONObject(init.getBody()).getString("processUUID");
      HttpResponse<String> process = get("/processes/securedProcess/" + uuid + "/records", 200);
      assertRecord(tier, new JSONObject(process.getBody()).getJSONArray("records").getJSONObject(0));
      post("/widget/securedRecords", Map.of(), 200);
      HttpResponse<String> search = post("/search", Map.of("searchTerm", SENTINEL, "tableNames", List.of("person")), 200);
      if(tier != FieldAccessTier.FULL)
      {
         assertEquals(0, new JSONObject(search.getBody()).optJSONArray("results") == null ? 0 : new JSONObject(search.getBody()).getJSONArray("results").length());
      }
      post("/search", Map.of("searchTerm", "Darin", "tableNames", List.of("person")), 200);
      post("/possibleValues/person", Map.of("ids", List.of(1)), 200);
      post("/table/auditDetail/query", Map.of(), 200);
      post("/table/audit/query", Map.of("joins", List.of(Map.of("joinTable", "auditDetail", "select", true, "type", "LEFT"))), 200);
      post("/table/audit/query", Map.of("joins", List.of(Map.of("joinTable", "auditDetail", "alias", "details", "select", true, "type", "LEFT"))), 200);
      get("/table/auditDetail/1", tier == FieldAccessTier.HIDDEN ? 404 : 200);
      if(tier == FieldAccessTier.MASKED || tier == FieldAccessTier.HIDDEN)
      {
         UiApiResponse response = UiApiInvoker.forBaseUrl("http://localhost:" + PORT, new MiddlewareVersionV1()).withClientChannel("assistant")
            .invoke(new UiApiRequest().withMethod(HttpMethod.POST).withPath("/table/{tableName}/query").withPathParam("tableName", "person").withSessionUUID(user).withJsonBody(Map.of()));
         assertEquals(200, response.getStatus());
         scanner.collect(response.getBody(), response.getHeaders().toString());
         scanner.assertSafe(SENTINEL);
      }
   }



   /*******************************************************************************
    ** Resolver refusal reaches the client unchanged; completion events carry counts.
    *******************************************************************************/
   @Test
   void exactMatchPolicy()
   {
      user = "REFUSED";
      USERS.put(user, new FieldAccessDecision(FieldAccessTier.REVEALABLE, FieldMaskStyle.FULL, null));
      Map<String, Object> filter = Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, SENTINEL)));
      assertTrue(post("/table/person/query", filter, 403).getBody().contains("Exact-match search limit reached."));
      assertTrue(EVENTS.stream().anyMatch(event -> event.getOutcome() == FieldAccessOutcome.REFUSED));
      user = "REVEALABLE";
      USERS.put(user, new FieldAccessDecision(FieldAccessTier.REVEALABLE, FieldMaskStyle.FULL, null));
      EVENTS.clear();
      post("/table/person/query", filter, 200);
      FieldAccessEvent match = EVENTS.stream().filter(event -> event.getType() == FieldAccessEventType.EXACT_MATCH_FILTER).findFirst().orElseThrow();
      assertEquals(FieldAccessOutcome.MATCH, match.getOutcome());
      assertEquals(1, match.getRecordCount());
      assertEquals(List.of("opaque-search-fingerprint"), match.getSearchValueFingerprints());
      assertFalse(JsonUtils.toJson(EVENTS).contains(SENTINEL));
      EVENTS.clear();
      post("/table/person/count", Map.of("filter", new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "not-found"))), 200);
      FieldAccessEvent miss = EVENTS.stream().filter(event -> event.getType() == FieldAccessEventType.EXACT_MATCH_FILTER).findFirst().orElseThrow();
      assertEquals(FieldAccessOutcome.NO_MATCH, miss.getOutcome());
      assertEquals(0, miss.getRecordCount());
      USERS.put(user, new FieldAccessDecision(FieldAccessTier.REVEALABLE, FieldMaskStyle.FULL, null, false));
      post("/table/person/query", filter, 403);
   }



   /*******************************************************************************
    ** Assert a seeded record's value channels and label.
    *******************************************************************************/
   private void assertRecord(FieldAccessTier tier, JSONObject record)
   {
      JSONObject values = record.getJSONObject("values");
      if(tier == FieldAccessTier.HIDDEN)
      {
         assertFalse(values.has("lastName"));
      }
      else
      {
         assertEquals(tier == FieldAccessTier.FULL ? SENTINEL : "***", values.getString("lastName"));
      }
      if(tier != FieldAccessTier.FULL)
      {
         assertTrue(record.isNull("recordLabel") || !record.has("recordLabel"));
      }
   }



   /*******************************************************************************
    ** Capture all HTTP responses as well as checking their status.
    *******************************************************************************/
   private HttpResponse<String> capture(HttpResponse<String> response, int status)
   {
      scanner.collect(response);
      assertEquals(status, response.getStatus(), response.getBody());
      return (response);
   }



   /*******************************************************************************
    ** Authenticated JSON request.
    *******************************************************************************/
   private HttpResponse<String> post(String path, Object body, int status)
   {
      return (capture(Unirest.post(getBaseUrlAndPath() + path).cookie("sessionId", user).contentType("application/json").body(JsonUtils.toJson(body)).asString(), status));
   }



   /*******************************************************************************
    ** Authenticated read request.
    *******************************************************************************/
   private HttpResponse<String> get(String path, int status)
   {
      return (capture(Unirest.get(getBaseUrlAndPath() + path).cookie("sessionId", user).asString(), status));
   }



   /*******************************************************************************
    ** Simulate policy and enumeration limits using only opaque search fingerprints.
    *******************************************************************************/
   public static class TestResolver implements FieldAccessResolverInterface
   {
      /*******************************************************************************
       ** Restrict the seeded field for each authenticated session.
       *******************************************************************************/
      public FieldAccessDecision resolve(QTableMetaData table, QFieldMetaData field)
      {
         return ("person".equals(table.getName()) && "lastName".equals(field.getName()) ? USERS.getOrDefault(QContext.getQSession().getUuid(), FieldAccessDecision.FULL) : FieldAccessDecision.FULL);
      }



      /*******************************************************************************
       ** Reject before querying.
       *******************************************************************************/
      public void checkExactMatch(FieldAccessEvent event) throws QPermissionDeniedException
      {
         if("REFUSED".equals(QContext.getQSession().getUuid()))
         {
            throw (new QPermissionDeniedException("Exact-match search limit reached."));
         }
      }



      /*******************************************************************************
       ** Opaque stand-in for a keyed HMAC.
       *******************************************************************************/
      public String fingerprintSearchValue(Serializable value)
      {
         return ("opaque-search-fingerprint");
      }



      /*******************************************************************************
       ** Keep events for assertions on the test thread.
       *******************************************************************************/
      public void onAccess(FieldAccessEvent event)
      {
         EVENTS.add(event);
      }
   }



   /*******************************************************************************
    ** Return known-table records in process values as well as process records.
    *******************************************************************************/
   public static class RecordsStep implements BackendStep
   {
      /*******************************************************************************
       ** Deliberately use unmasked internal records to exercise the egress boundary.
       *******************************************************************************/
      public void run(RunBackendStepInput input, RunBackendStepOutput output)
      {
         QRecord record = new QRecord().withValue("id", 1).withValue("lastName", SENTINEL);
         output.setRecords(new ArrayList<>(List.of(record)));
         output.addValue("record", record);
         output.addValue("records", new ArrayList<>(List.of(record)));
      }
   }



   /*******************************************************************************
    ** Middleware READ denial remains its existing check, regardless of field opt-in.
    *******************************************************************************/
   @Test
   void tableReadPermissionIsUnchanged() throws Exception
   {
      user = "no-read";
      USERS.put(user, new FieldAccessDecision(FieldAccessTier.MASKED, FieldMaskStyle.FULL, "test"));
      serverQInstance.getTable("person").withPermissionRules(new QPermissionRules()
         .withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS).withPermissionBaseName("ungranted"));
      post("/table/person/query", Map.of(), 403);
      serverQInstance.getSupplementalMetaData().remove(FieldAccessResolverMetaData.NAME);
      post("/table/person/query", Map.of(), 403);
   }
}
