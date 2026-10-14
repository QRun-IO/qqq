/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.help;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiHelpText;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentHash;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentSource;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentSyncContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpContentVisibility;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpTextRenderer;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundleEntry;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundleLayer;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.testing.HelpBundleAssertions;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Bundle lifecycle tests through real table actions and the memory backend.
 *******************************************************************************/
class HelpBundlesTest extends BaseTest
{
   private static final List<String> alerts = new CopyOnWriteArrayList<>();
   private static volatile CountDownLatch completed = new CountDownLatch(1);
   private static volatile boolean systemSession;
   private final HelpBundleSyncAction sync = new HelpBundleSyncAction();


   /*******************************************************************************
    ** A small registered bundle keeps merge scenarios independent of core help.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      new HelpContentMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      QContext.getQInstance().withSupplementalMetaData(new HelpBundles());
      AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new QCodeReference(RecordingSink.class));
      alerts.clear();
      completed = new CountDownLatch(1);
      systemSession = false;
      register("v1", "CORE", "First summary", "First text");
   }



   /*******************************************************************************
    ** Loader defaults, named errors, strict grammar, roles and length validation.
    *******************************************************************************/
   @Test
   void parsingAndValidation()
   {
      var bundle = HelpBundles.load("test.yaml", loader("bundle: example\nversion: 1.0\nlayer: QBIT\nentries:\n  - key: table:person;slot:help\n    content: Hello\n"));
      assertEquals("example", bundle.getId());
      assertEquals("ALL_SCREENS", bundle.getEntries().getFirst().getRole());
      assertEquals("MARKDOWN", bundle.getEntries().getFirst().getFormat());
      for(String invalid : List.of("entries: [", "bundle: no-version", "bundle: a\nversion: 1\nlayer: WRONG"))
      {
         assertThat(assertThrows(IllegalArgumentException.class, () -> HelpBundles.load("broken.yaml", loader(invalid)))).hasMessageContaining("broken.yaml");
      }
      assertThrows(IllegalArgumentException.class, () -> HelpBundles.load("does-not-exist.yaml", null));
      assertThrows(IllegalArgumentException.class, () -> HelpBundles.validateEntry(new HelpBundleEntry().withKey("table:person")));
      assertThrows(IllegalArgumentException.class, () -> HelpBundles.validateEntry(new HelpBundleEntry().withKey("app:x;field:y")));
      assertThrows(IllegalArgumentException.class, () -> HelpBundles.validateEntry(new HelpBundleEntry().withKey("table:x;slot:help").withRole("BAD")));
      assertThrows(IllegalArgumentException.class, () -> HelpBundles.validateEntry(new HelpBundleEntry().withKey("table:x;slot:help").withSummary("x".repeat(501))));
   }



   /*******************************************************************************
    ** Highest layer wins, with later registration breaking ties independently by role.
    *******************************************************************************/
   @Test
   void layering()
   {
      register("qbit", "QBIT", "Qbit", "Text");
      register("host", "HOST", "Host", "Text");
      register("laterCore", "CORE", "Core", "Text");
      assertEquals("host", winner().bundle().getId());
      register("laterHost", "HOST", "Later", "Text");
      assertEquals("laterHost", winner().bundle().getId());
      assertEquals(HelpBundleLayer.HOST, winner().bundle().getLayer());
      assertEquals(5, HelpBundles.of(QContext.getQInstance()).getBundles().size());
      winner().bundle().getEntries().add(new HelpBundleEntry().withKey("table:person;slot:help").withRole("VIEW_SCREEN").withContent("View help"));
      assertEquals(2, HelpBundles.of(QContext.getQInstance()).resolveWinningEntries().size());
   }



   /*******************************************************************************
    ** Hash null normalization, separators and all three fields are significant.
    *******************************************************************************/
   @Test
   void hashes()
   {
      assertEquals(HelpContentHash.of(null, null, null), HelpContentHash.of("", "", ""));
      assertThat(HelpContentHash.of("MARKDOWN", "Summary", "Text")).matches("[a-f0-9]{64}");
      assertNotEquals(HelpContentHash.of("a", "bc", ""), HelpContentHash.of("ab", "c", ""));
      assertNotEquals(HelpContentHash.of("TEXT", "", "body"), HelpContentHash.of("MARKDOWN", "", "body"));
   }



   /*******************************************************************************
    ** Supplemental metadata remains serializable, including setting resolvers.
    *******************************************************************************/
   @Test
   void serializableMetadata()
   {
      HelpBundles original = HelpBundles.of(QContext.getQInstance()).withValueProvider(name -> Optional.of("live"));
      HelpBundles copy = SerializationUtils.clone(original);
      assertEquals("live", copy.getValueProviders().getFirst().value("example").orElseThrow());
      copy.getBundles().getFirst().getEntries().getFirst().setSummary("Changed copy");
      assertEquals("First summary", original.getBundles().getFirst().getEntries().getFirst().getSummary());
   }



   /*******************************************************************************
    ** Insert, idempotent replay and unedited replacement preserve provenance.
    *******************************************************************************/
   @Test
   void insertAndUneditedUpdate() throws Exception
   {
      assertEquals(1, sync.preview().inserted());
      assertThat(rows()).isEmpty();
      assertThat(alerts).isEmpty();
      assertEquals(1, sync.execute().inserted());
      QRecord first = row();
      assertEquals("CORE", first.getValueString("source"));
      assertEquals(HelpContentHash.of(first), first.getValueString("lastSeededHash"));
      assertEquals(1, sync.execute().unchanged());
      assertEquals(first.getValue("modifyDate"), row().getValue("modifyDate"));
      register("v2", "HOST", "Second summary", "Second text");
      assertEquals(1, sync.execute().updated());
      assertEquals("Second text", row().getValueString("content"));
      assertEquals("CORE", row().getValueString("source"));
      assertEquals("v2", row().getValueString("bundleId"));
      assertEquals(1, sync.execute().unchanged());
   }



   /*******************************************************************************
    ** Edited text is retained, pending reviews are idempotent, and alerts resolve.
    *******************************************************************************/
   @Test
   void editedUpstreamAndReview() throws Exception
   {
      sync.execute();
      edit("My text");
      assertEquals(DEFAULT_USER_ID, row().getValueString("modifiedBy"));
      assertEquals("CORE", row().getValueString("source"));
      assertEquals(1, sync.execute().unchanged());
      register("v2", "CORE", "New summary", "New text");
      assertEquals(1, sync.execute().upstreamChanged());
      QRecord pending = row();
      assertEquals("My text", pending.getValueString("content"));
      assertEquals("New text", pending.getValueString("upstreamContent"));
      assertEquals("New summary", pending.getValueString("upstreamSummary"));
      assertThat(alerts).contains("raise:help.upstream.changed:help-sync");
      sync.execute();
      assertEquals(pending.getValue("modifyDate"), row().getValue("modifyDate"));
      sync.keepMine(List.of(pending.getValue("id")));
      assertEquals("My text", row().getValueString("content"));
      assertEquals("CURRENT", row().getValueString("upstreamStatus"));
      assertEquals(HelpContentHash.of("MARKDOWN", "New summary", "New text"), row().getValueString("lastSeededHash"));
      assertEquals(1, sync.execute().unchanged());
      register("v3", "CORE", "Newest summary", "Newest text");
      winner().entry().setFormat("TEXT");
      sync.execute();
      sync.acceptUpstream(List.of(pending.getValue("id")));
      assertEquals("Newest text", row().getValueString("content"));
      assertEquals("TEXT", row().getValueString("format"));
      assertEquals(HelpContentHash.of(row()), row().getValueString("lastSeededHash"));
      assertEquals("CURRENT", row().getValueString("upstreamStatus"));
      assertThat(alerts.getLast()).isEqualTo("resolve:help.upstream.changed:help-sync");
   }



   /*******************************************************************************
    ** Explicit retirements delete unedited text; removals retain edited text.
    *******************************************************************************/
   @Test
   void retirements() throws Exception
   {
      sync.execute();
      winner().entry().setRetired(true);
      assertEquals(1, sync.execute().deleted());
      assertThat(rows()).isEmpty();
      assertEquals(0, sync.execute().inserted());
      winner().entry().setRetired(false);
      sync.execute();
      edit("Keep this retired help");
      QContext.getQInstance().withSupplementalMetaData(new HelpBundles());
      assertEquals(1, sync.execute().retired());
      assertEquals("RETIRED", row().getValueString("upstreamStatus"));
      assertEquals("Keep this retired help", row().getValueString("content"));
      assertEquals(1, sync.execute().unchanged());
   }



   /*******************************************************************************
    ** User-created rows are neither overwritten nor retired, even with matching keys.
    *******************************************************************************/
   @Test
   void userRowsAndScopes() throws Exception
   {
      new InsertAction().execute(new InsertInput("helpContent").withRecordEntity(new HelpContent()
         .withKey("table:person;slot:help").withRole("ALL_SCREENS").withContent("Mine").withSource("CORE")));
      assertEquals("USER", row().getValueString("source"));
      assertEquals(1, sync.execute().unchanged());
      QContext.getQInstance().withSupplementalMetaData(new HelpBundles());
      assertEquals(1, sync.execute().unchanged());
      assertFalse(HelpContentSyncContext.isActive());
      try(HelpContentSyncContext outer = new HelpContentSyncContext())
      {
         try(HelpContentSyncContext inner = new HelpContentSyncContext())
         {
            assertTrue(HelpContentSyncContext.isActive());
         }
         assertTrue(HelpContentSyncContext.isActive());
      }
      assertFalse(HelpContentSyncContext.isActive());
   }



   /*******************************************************************************
    ** Every reference type, unresolved values, and safe Markdown substitutions.
    *******************************************************************************/
   @Test
   void rendering() throws Exception
   {
      var instance = QContext.getQInstance();
      instance.getTable("person").withLabel("People");
      instance.getTable("person").getField("email").withLabel("Email").withDefaultValue("none").withMaxLength(99);
      instance.addApp(new QAppMetaData().withName("testApp").withLabel("Workspace"));
      instance.addProcess(new QProcessMetaData().withName("testProcess").withLabel("Run work"));
      instance.addPossibleValueSource(QPossibleValueSource.newForEnum("sources", HelpContentSource.values()));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("person"));
      HelpBundles.of(instance).withValueProvider(name -> name.equals("special") ? Optional.of("*[safe]_") : Optional.empty());
      HelpTextRenderer renderer = new HelpTextRenderer(instance);
      assertEquals("People Email Workspace Run work none 99 CORE, QBIT, HOST, USER", renderer.render("{{label:person}} {{label:person.email}} {{label:testApp}} {{label:testProcess}} {{default:person.email}} {{maxLength:person.email}} {{values:sources}}"));
      assertEquals("\\*\\[safe\\]\\_", renderer.render("{{setting:special}}"));
      assertEquals("", renderer.render("{{label:missing}}{{label:person.missing}}{{values:person}}{{values:missing}}{{setting:missing}}{{what:x}}"));
      assertEquals("", renderer.render("{{label:missing}}"));
      assertThat(renderer.unresolved("{{bad:x}} {{label:person}} {{broken")).hasSize(2);
      QHelpContent raw = new QHelpContent().withContentAsMarkdown("Hello {{label:person}}").withSummary("Find {{label:person}}");
      instance.getTable("person").withHelpContent("help", raw);
      assertThat(HelpContentVisibility.filter(List.of(raw)).getFirst().getContentAsHtml()).contains("Hello People");
      assertThat(AiHelpText.plain(List.of(raw))).contains("Hello People");
      assertEquals("Hello {{label:person}}", raw.getContent());
      assertThat(new HelpSearchAction().execute(new HelpSearchInput().withQuery("People"))).anyMatch(result -> result.getSummary().contains("Find People"));
      TableMetaDataInput input = new TableMetaDataInput();
      input.setTableName("person");
      var frontend = new TableMetaDataAction().execute(input).getTable();
      assertThat(frontend.getHelpContents().get("help").getFirst().getContentAsHtml()).contains("Hello People");
   }



   /*******************************************************************************
    ** The coverage gate reports every problem and respects both exclusion forms.
    *******************************************************************************/
   @Test
   void coverageFailuresAndExclusions()
   {
      var instance = QContext.getQInstance();
      var gate = HelpBundleAssertions.forInstance(instance).owning(t -> ("person".equals(t.table()) && !"process".equals(t.kind())) || "helpBundleSync".equals(t.name()));
      assertThat(gate.report()).contains("Missing help: table:person;slot:help", "Missing help: table:person;field:email", "Missing help: process:helpBundleSync;slot:help");
      assertThat(gate.report()).doesNotContain("Missing help: table:person;field:id", "Missing help: table:person;field:createDate");
      assertThat(HelpBundleAssertions.forInstance(instance).owning(t -> "table".equals(t.kind()) && "person".equals(t.name())).report())
         .contains("Missing help: table:person;field:email");
      assertThrows(AssertionError.class, gate::assertComplete);
      gate.excluding("table:person;slot:help", "process:helpBundleSync;slot:help").excludingFields(field -> true).assertComplete();
      var invalid = HelpBundleAssertions.forInstance(instance).bundles("help/invalid-coverage.yaml").owning(t -> false).report();
      assertThat(invalid).anyMatch(p -> p.contains("Unresolved key"));
      assertThat(invalid).anyMatch(p -> p.contains("Summary must"));
      assertThat(invalid).anyMatch(p -> p.contains("Unresolved reference"));
      assertTrue(HelpBundleAssertions.resolves(instance, "process:*BundleSync;slot:help"));
      assertTrue(HelpBundleAssertions.resolves(instance, "instanceLevel:true;slot:custom"));
      assertTrue(HelpBundleAssertions.resolves(instance, "table:person;slot:custom"));
      assertThat(HelpBundleAssertions.forInstance(instance).bundles("missing.yaml").owning(t -> false).report())
         .anyMatch(p -> p.contains("missing.yaml"));
   }



   /*******************************************************************************
    ** Core-owned help and all bundled targets resolve with their providers installed.
    *******************************************************************************/
   @Test
   void coreBundleGate() throws Exception
   {
      var instance = QContext.getQInstance();
      new SavedViewsMetaDataProvider().withIsQuickSavedViewEnabled(true).defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      new SavedReportsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      new AuditsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      new ScriptsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      new QInstanceEnricher(instance).enrich();
      HelpBundleAssertions.forInstance(instance).bundles("help/qqq-core.yaml")
         .owning(target -> "helpContent".equals(target.table())).assertComplete();
   }



   /*******************************************************************************
    ** Standard process review is dry, and result runs the same sync decisions.
    *******************************************************************************/
   @Test
   void processAndStartup() throws Exception
   {
      var output = new RunProcessAction().execute(new RunProcessInput().withProcessName("helpBundleSync"));
      assertThat(rows()).isEmpty();
      assertThat(output.getValue("validationSummary").toString()).contains("would be inserted");
      new RunProcessAction().execute(new RunProcessInput().withProcessName("helpBundleSync")
         .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      assertThat(rows()).hasSize(1);
      assertDoesNotThrow(() -> HelpBundles.syncOnStartup(null));
      assertDoesNotThrow(() -> HelpBundles.syncOnStartup(new QInstance()));
   }



   /*******************************************************************************
    ** The independent provenance opt-out removes fields and management processes.
    *******************************************************************************/
   @Test
   void legacyProvenance() throws Exception
   {
      var instance = TestUtils.defineInstance();
      new HelpContentMetaDataProvider().withProvenanceFields(false).defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      assertThat(instance.getTable("helpContent").getFields()).containsKey("summary").doesNotContainKeys("source", "bundleId", "upstreamContent");
      assertThat(instance.getProcesses()).doesNotContainKey("helpBundleSync");
   }



   /*******************************************************************************
    ** Reads and every kind of write cross a batch boundary without dropping rows.
    *******************************************************************************/
   @Test
   void batchesAndFailures() throws Exception
   {
      var bundle = winner().bundle();
      for(int i = 0; i < 251; i++)
      {
         bundle.getEntries().add(new HelpBundleEntry().withKey("table:person;slot:batch" + i).withContent("Before"));
      }
      assertEquals(252, sync.execute().inserted());
      bundle.getEntries().forEach(entry -> entry.setContent("After"));
      assertEquals(252, sync.execute().updated());
      assertEquals(252, sync.execute().unchanged());
      bundle.getEntries().forEach(entry -> entry.setRetired(true));
      assertEquals(252, sync.execute().deleted());
      assertThat(rows()).isEmpty();
      winner().entry().withRetired(false).withSummary("x".repeat(501));
      assertThrows(QException.class, sync::execute);
      assertFalse(HelpContentSyncContext.isActive());
      assertThat(rows()).isEmpty();
   }



   /*******************************************************************************
    ** Review actions support both selected IDs and select-all filters.
    *******************************************************************************/
   @Test
   void reviewProcessesAndStaleUpstream() throws Exception
   {
      sync.execute();
      edit("Mine");
      register("v2", "HOST", "Upstream", "Version two");
      sync.execute();
      new RunProcessAction().execute(new RunProcessInput().withProcessName("helpKeepMine")
         .withValue("recordIds", row().getValueString("id")).withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      assertEquals("CURRENT", row().getValueString("upstreamStatus"));
      assertEquals("Mine", row().getValueString("content"));
      register("v3", "HOST", "Upstream", "Version three");
      sync.execute();
      register("v4", "HOST", "Upstream", "Version four");
      assertThrows(QException.class, () -> sync.acceptUpstream(List.of(row().getValue("id"))));
      sync.execute();
      assertThrows(QException.class, () -> new RunProcessAction().execute(new RunProcessInput().withProcessName("helpAcceptUpstream")
         .withValue("queryFilterJson", "null").withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP)));
      new RunProcessAction().execute(new RunProcessInput().withProcessName("helpAcceptUpstream")
         .withValue("queryFilterJson", "{}").withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      assertEquals("Version four", row().getValueString("content"));
      assertEquals("CURRENT", row().getValueString("upstreamStatus"));
   }



   /*******************************************************************************
    ** Startup runs in the background as system, restoring the caller's session.
    *******************************************************************************/
   @Test
   void startupSystemSession() throws Exception
   {
      var instance = QContext.getQInstance();
      var session = QContext.getQSession();
      HelpBundles.syncOnStartup(instance);
      assertTrue(completed.await(10, TimeUnit.SECONDS));
      assertTrue(systemSession);
      assertSame(session, QContext.getQSession());
      assertThat(rows()).hasSize(1);
      ////////////////////////////////////////////////////////////////////////
      // with durable jobs configured, the startup sync still runs and ends //
      ////////////////////////////////////////////////////////////////////////
      instance.withSupplementalMetaData(new DurableJobsMetaData().withEligibleProcessName("helpBundleSync"));
      completed = new CountDownLatch(1);
      HelpBundles.syncOnStartup(instance);
      assertTrue(completed.await(10, TimeUnit.SECONDS));
      assertThat(rows()).hasSize(1);
   }



   /*******************************************************************************
    ** Register synthetic classpath content through the same parser as production.
    *******************************************************************************/
   private void register(String id, String layer, String summary, String content)
   {
      HelpBundles.register(QContext.getQInstance(), "test.yaml", loader("bundle: " + id + "\nversion: 1.0\nlayer: " + layer
         + "\nentries:\n  - key: table:person;slot:help\n    summary: " + summary + "\n    content: " + content + "\n"));
   }



   /*******************************************************************************
    ** Deterministic classpath loader without temporary files.
    *******************************************************************************/
   private ClassLoader loader(String yaml)
   {
      return (new ClassLoader()
      {
         /*******************************************************************************
          ** Supply the requested fixture bytes.
          *******************************************************************************/
         @Override
         public InputStream getResourceAsStream(String name)
         {
            return (new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
         }
      });
   }



   /*******************************************************************************
    ** Winner for the single merge fixture.
    *******************************************************************************/
   private HelpBundles.WinningEntry winner()
   {
      return (HelpBundles.of(QContext.getQInstance()).resolveWinningEntries().values().iterator().next());
   }



   /*******************************************************************************
    ** Read persisted rows through the normal query action.
    *******************************************************************************/
   private List<QRecord> rows() throws Exception
   {
      return (new QueryAction().execute(new QueryInput("helpContent")).getRecords());
   }



   /*******************************************************************************
    ** Current single fixture row.
    *******************************************************************************/
   private QRecord row() throws Exception
   {
      return (rows().getFirst());
   }



   /*******************************************************************************
    ** Exercise partial user updates and provenance customizers.
    *******************************************************************************/
   private void edit(String text) throws Exception
   {
      new UpdateAction().execute(new UpdateInput("helpContent").withRecord(new QRecord().withValue("id", row().getValue("id")).withValue("content", text)));
   }



   /*******************************************************************************
    ** Alert sink used by the real QAlerts dispatcher.
    *******************************************************************************/
   public static class RecordingSink implements AlertSinkInterface
   {
      /*******************************************************************************
       ** Capture alert type and stable dedup key.
       *******************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         alerts.add("raise:" + signal.getAlertTypeName() + ":" + signal.getEffectiveDedupKey());
      }


      /*******************************************************************************
       ** Capture resolution.
       *******************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
         alerts.add("resolve:" + resolution.getAlertTypeName() + ":" + resolution.getEffectiveDedupKey());
         systemSession = QContext.getQSession() instanceof QSystemUserSession;
         completed.countDown();
      }


      /*******************************************************************************
       ** No heartbeat is expected from help synchronization.
       *******************************************************************************/
      @Override
      public void heartbeat(String key, Instant at)
      {
         alerts.add("heartbeat:" + key);
      }
   }
}
