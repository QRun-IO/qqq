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

package com.kingsrook.qqq.backend.core.health;


import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.LineChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreProviderInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Registry validation, synchronization, core probes and UI integration.
 *******************************************************************************/
class HealthMetaDataTest extends BaseTest
{
   /*******************************************************************************
    ** Install health tables in the normal memory-backed core fixture.
    *******************************************************************************/
   private QInstance install() throws QException
   {
      QInstance instance = QContext.getQInstance();
      new HealthMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, table -> assertThat(table.getBackendName()).isEqualTo(TestUtils.MEMORY_BACKEND_NAME));
      QContext.getQSession().withPermissions("health.read", "health.write");
      return (instance);
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void definitionsHaveSnakeCaseStorageAndNativeUiMetadata() throws Exception
   {
      QInstance instance = install();
      new QInstanceEnricher(instance).enrich();
      new QInstanceValidator().validate(instance);
      assertThat(instance.getApp("systemHealth").getChildren()).hasSize(1);
      assertThat(instance.getProcess(HealthRunCheckNowProcess.NAME).getTableName()).isEqualTo(HealthMetaDataProvider.CHECK_TABLE);
      assertThat(instance.getTable(HealthMetaDataProvider.RESULT_TABLE).getUniqueKeys()).hasSize(1);
      assertThat(instance.getTable(HealthMetaDataProvider.RESULT_TABLE).getField("onDemandRequestId").getDefaultValue()).isEqualTo("");
      for(String table : List.of(HealthMetaDataProvider.CHECK_TABLE, HealthMetaDataProvider.RESULT_TABLE, HealthMetaDataProvider.COMPONENT_TABLE))
      {
         for(QFieldMetaData field : instance.getTable(table).getFields().values())
         {
            assertThat(field.getBackendName()).matches("[a-z][a-z0-9_]*");
         }
      }
      assertThat(instance.getTable(HealthMetaDataProvider.CHECK_TABLE).getField("enabled").getIsEditable()).isTrue();
      assertThat(instance.getTable(HealthMetaDataProvider.RESULT_TABLE).getFields().values()).allSatisfy(field -> assertThat(field.getIsEditable()).isFalse());
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void validatesDuplicatesNamesIntervalsAndLivenessTimeouts() throws Exception
   {
      QInstance instance = install();
      HealthChecksMetaData checks = new HealthChecksMetaData();
      checks.withCheck(valid("duplicate")).withCheck(valid("duplicate")).withCheck(valid("BAD name"))
         .withCheck(valid("short").withIntervalSeconds(9))
         .withCheck(valid("timeout").withIntervalSeconds(10).withTimeoutMs(10000))
         .withCheck(valid("liveness").withKind(HealthCheckKind.LIVENESS).withTimeoutMs(1001));
      QInstanceValidator validator = new QInstanceValidator();
      checks.validate(instance, validator);
      assertThat(validator.getErrors()).hasSize(5);
      assertThatThrownBy(() -> checks.resolveAll(instance)).isInstanceOf(QException.class);
      HealthMonitoringMetaData settings = HealthMonitoringMetaData.ofOrWithNew(instance);
      settings.withBackendName("missing").withTickSeconds(0).withMaxConcurrentChecks(0).withResultRetentionDays(0).withRunnerUserName("");
      QInstanceValidator settingsValidator = new QInstanceValidator();
      settings.validate(instance, settingsValidator);
      assertThat(settingsValidator.getErrors()).hasSize(5);
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void supplierResolutionAndSyncPreserveOverridesAndUnregisterMissingChecks() throws Exception
   {
      QInstance instance = install();
      HealthChecksMetaData registry = HealthChecksMetaData.ofOrWithNew(instance);
      registry.withSupplier(new HealthCheckSupplierMetaData().withNamePrefix("dynamic").withComponent("fixture").withSupplier(new QCodeReference(Supplier.class)));
      Supplier.present = true;
      new HealthCheckSync().sync(instance);
      QRecord row = HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", "dynamic.one");
      assertThat(row.getValueInteger("intervalSeconds")).isNull();
      HealthStore.update(HealthMetaDataProvider.CHECK_TABLE, new QRecord().withValue("id", row.getValue("id")).withValue("intervalSeconds", 120).withValue("enabled", false));
      new HealthCheckSync().sync(instance);
      row = HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", "dynamic.one");
      assertThat(row.getValueInteger("intervalSeconds")).isEqualTo(120);
      assertThat(row.getValueBoolean("enabled")).isFalse();
      Supplier.present = false;
      new HealthCheckSync().sync(instance);
      assertThat(HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", "dynamic.one").getValueBoolean("registered")).isFalse();
      assertThat(registry.getSuppliers()).hasSize(1);
      assertThat(registry.getChecks()).isNotEmpty();
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void schedulerAndSessionStoreProbeRealInterfaces() throws Exception
   {
      try
      {
         QScheduleManager.getInstance().unInit();
      }
      catch(IllegalStateException ignored)
      {
         // It is already uninitialized.
      }
      assertThat(new HealthSchedulerCheck().run(new HealthCheckInput()).getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      QInstance instance = QContext.getQInstance();
      QScheduleManager manager = QScheduleManager.initInstance(instance, QSession::new);
      assertThat(new HealthSchedulerCheck().run(new HealthCheckInput()).getStatus()).isEqualTo(HealthStatus.DOWN);
      manager.start();
      assertThat(new HealthSchedulerCheck().run(new HealthCheckInput()).getStatus()).isEqualTo(HealthStatus.UP);
      manager.stop();
      manager.unInit();
      QSessionStoreRegistry registry = QSessionStoreRegistry.getInstance();
      registry.clear();
      assertThat(new HealthSessionStoreCheck().run(new HealthCheckInput()).getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      Map<String, QSession> sessions = new HashMap<>();
      registry.register(new QSessionStoreProviderInterface()
      {
         @Override
         public void store(String id, QSession session, Duration ttl)
         {
            assertThat(ttl).isEqualTo(Duration.ofSeconds(60));
            sessions.put(id, session);
         }

         @Override
         public Optional<QSession> load(String id)
         {
            return Optional.ofNullable(sessions.get(id));
         }

         @Override
         public void remove(String id)
         {
            sessions.remove(id);
         }

         @Override
         public void touch(String id)
         {
            // The health probe does not touch real sessions.
         }

         @Override
         public Duration getDefaultTtl()
         {
            return Duration.ofSeconds(60);
         }
      });
      try
      {
         install();
         assertThat(HealthChecksMetaData.ofOrWithNew(instance).getChecks()).anyMatch(check -> "core.session_store".equals(check.getName()));
         assertThat(new HealthSessionStoreCheck().run(new HealthCheckInput()).getStatus()).isEqualTo(HealthStatus.UP);
         assertThat(sessions).isEmpty();
      }
      finally
      {
         registry.clear();
      }
   }



   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void widgetsRenderEmptyAndRealDataAndProcessProvidesReviewAndResults() throws Exception
   {
      QInstance instance = install();
      HealthWidgets widgets = new HealthWidgets();
      for(String name : instance.getApp("systemHealth").getWidgets())
      {
         QWidgetData data = widgets.render(new RenderWidgetInput().withWidgetMetaData(instance.getWidget(name))).getWidgetData();
         if(data instanceof TableData table)
         {
            assertThat(table.getRows()).isEmpty();
            assertThat(table.getNoRowsFoundHTML()).isEqualTo("No checks have run yet");
         }
         else
         {
            assertThat(((MultiStatisticsData) data).getTitle()).isEqualTo("No checks have run yet");
         }
      }
      HealthChecksMetaData.ofOrWithNew(instance).withCheck(valid("test.process").withCheckInstance(input -> HealthCheckOutcome.down("failure")).withFailureThreshold(1));
      new HealthCheckSync().sync(instance);
      QRecord check = HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", "test.process");
      RenderWidgetInput chartInput = new RenderWidgetInput().withWidgetMetaData(instance.getWidget(HealthLatencyWidget.NAME));
      assertThat(((LineChartData) new HealthLatencyWidget().render(chartInput).getWidgetData()).getTitle()).isEqualTo("No checks have run yet");
      HealthRunCheckNowProcess.Review review = new HealthRunCheckNowProcess.Review();
      RunBackendStepInput input = new RunBackendStepInput().withRecords(List.of(check));
      RunBackendStepOutput output = new RunBackendStepOutput();
      review.runOnePage(input, output);
      assertThat(review.getProcessSummary(output, false)).extracting(ProcessSummaryLineInterface::getMessage).containsExactly("1 checks will run");
      assertThat(HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter())).isEmpty();
      new HealthRunCheckNowProcess.Run().withTransformStep(review).runOnePage(input, output);
      chartInput.getQueryParams().put("id", check.getValueString("id"));
      LineChartData chart = (LineChartData) new HealthLatencyWidget().render(chartInput).getWidgetData();
      assertThat(chart.getType()).isEqualTo("smallLineChart");
      assertThat(chart.getChartData().getDatasets().getFirst().getData()).hasSize(1);
      assertThat(review.getProcessSummary(output, true)).extracting(ProcessSummaryLineInterface::getMessage)
         .containsExactly("0 UP", "0 DEGRADED", "1 DOWN", "0 UNKNOWN");
      for(String name : List.of("healthComponents", "healthProblems", "healthTransitions"))
      {
         TableData table = (TableData) widgets.render(new RenderWidgetInput().withWidgetMetaData(instance.getWidget(name))).getWidgetData();
         assertThat(table.getRows()).isNotEmpty();
      }
      MultiStatisticsData summary = (MultiStatisticsData) widgets.render(new RenderWidgetInput().withWidgetMetaData(instance.getWidget("healthSummary"))).getWidgetData();
      assertThat(summary.getTitle()).isEqualTo("System Health: DOWN");
   }



   /*******************************************************************************
    ** Minimal valid fixture definition.
    *******************************************************************************/
   private static HealthCheckMetaData valid(String name)
   {
      return (new HealthCheckMetaData().withName(name).withComponent("fixture").withCheckInstance(input -> HealthCheckOutcome.up("ok")));
   }



   /*******************************************************************************
    ** Exercise the real streamed ETL pipeline, including standard frontend steps.
    *******************************************************************************/
   @Test
   void recordActionRunsThroughTheProcessEngine() throws Exception
   {
      QInstance instance = install();
      HealthChecksMetaData.ofOrWithNew(instance).withCheck(valid("test.engine"));
      new HealthCheckSync().sync(instance);
      QRecord record = HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", "test.engine");
      RunProcessOutput output = new RunProcessAction().execute(new RunProcessInput().withProcessName(HealthRunCheckNowProcess.NAME)
         .withValue("recordIds", record.getValueString("id")).withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      assertThat(output.getException()).isEmpty();
      assertThat(HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter())).hasSize(1);
      assertThat(output.getValue("processResults").toString()).contains("1 UP");
   }



   /*******************************************************************************
    ** Module probes are registered without importing backend implementations.
    *******************************************************************************/
   @Test
   void registersOptInBackendReadinessProbe() throws Exception
   {
      QBackendModuleDispatcher.registerBackendModule(new ProbeBackend());
      QContext.getQInstance().addBackend(new QBackendMetaData().withName("probe").withBackendType(ProbeBackend.class));
      QInstance instance = install();
      HealthCheckMetaData probe = HealthChecksMetaData.ofOrWithNew(instance).resolveAll(instance).stream()
         .filter(check -> "core.database.probe".equals(check.getName())).findFirst().orElseThrow();
      assertThat(probe.getIncludeInReadiness()).isTrue();
      assertThat(probe.getIntervalSeconds()).isEqualTo(30);
      assertThat(probe.getTimeoutMs()).isEqualTo(3000);
      assertThat(probe.getFailureThreshold()).isEqualTo(1);
      assertThat(probe.getCheckInstance().run(new HealthCheckInput()).getStatus()).isEqualTo(HealthStatus.UP);
   }



   /*******************************************************************************
    ** A backend with just an optional readiness probe.
    *******************************************************************************/
   public static class ProbeBackend implements QBackendModuleInterface
   {
      @Override
      public String getBackendType()
      {
         return ("health-test-probe");
      }

      @Override
      public HealthCheck getHealthCheck(QBackendMetaData backend)
      {
         return (input -> HealthCheckOutcome.up("backend reachable"));
      }
   }



   /*******************************************************************************
    ** A data-driven supplier whose connection can be removed between syncs.
    *******************************************************************************/
   public static class Supplier implements HealthCheckSupplier
   {
      static boolean present;

      @Override
      public List<HealthCheckMetaData> supply(QInstance instance)
      {
         return (present ? List.of(valid("dynamic.one").withComponent(null)) : List.of());
      }
   }
}

