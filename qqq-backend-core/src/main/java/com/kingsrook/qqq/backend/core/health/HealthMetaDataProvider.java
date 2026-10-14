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


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.UseTablePermissionCustomPermissionChecker;
import com.kingsrook.qqq.backend.core.alerts.AlertDefaultMonitor;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.QAlertTypeMetaData;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;


/*******************************************************************************
 ** Define the central health tables, alerts, core probes, action and app.
 *******************************************************************************/
public class HealthMetaDataProvider
{
   public static final String CHECK_TABLE = "health_check";
   public static final String RESULT_TABLE = "health_check_result";
   public static final String COMPONENT_TABLE = "health_component";
   public static final String RESULT_JOIN = "healthCheckResults";



   /*******************************************************************************
    ** Install metadata on the application's chosen backend.
    *******************************************************************************/
   public void defineAll(QInstance qInstance, String backendName, Consumer<QTableMetaData> enricher) throws QException
   {
      HealthMonitoringMetaData.ofOrWithNew(qInstance).setBackendName(backendName);
      qInstance.addPossibleValueSource(QPossibleValueSource.newForEnum(HealthStatus.NAME, HealthStatus.values()));
      qInstance.addPossibleValueSource(QPossibleValueSource.newForEnum(HealthResultStatus.NAME, HealthResultStatus.values()));
      qInstance.addPossibleValueSource(QPossibleValueSource.newForEnum(HealthCheckKind.NAME, HealthCheckKind.values()));
      qInstance.addPossibleValueSource(QPossibleValueSource.newForEnum(HealthCategory.NAME, HealthCategory.values()));
      qInstance.addPossibleValueSource(QPossibleValueSource.newForEnum(HealthTriggeredBy.NAME, HealthTriggeredBy.values()));

      QTableMetaData checks = table(CHECK_TABLE, "Health Checks", backendName).withUniqueKey(new UniqueKey("name"));
      checks.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_DELETE);
      fields(checks, QFieldType.STRING, "name", "label", "component", "category", "kind", "severity", "currentStatus", "currentMessage", "recentTransitionsJson");
      fields(checks, QFieldType.INTEGER, "intervalSeconds", "timeoutMs", "failureThreshold", "recoveryThreshold", "consecutiveFailures", "consecutiveSuccesses");
      fields(checks, QFieldType.LONG, "lastLatencyMs");
      fields(checks, QFieldType.BOOLEAN, "enabled", "registered", "flapping");
      fields(checks, QFieldType.DATE_TIME, "currentSince", "lastRunAt", "nextDueAt");
      checks.getField("category").withPossibleValueSourceName(HealthCategory.NAME);
      checks.getField("kind").withPossibleValueSourceName(HealthCheckKind.NAME);
      checks.getField("currentStatus").withPossibleValueSourceName(HealthStatus.NAME);
      checks.getField("currentMessage").withMaxLength(500);
      checks.getField("recentTransitionsJson").withMaxLength(65536).withIsHidden(true);
      checks.getField("severity").withInlinePossibleValueSource(new QPossibleValueSource().withName("healthSeverity").withType(QPossibleValueSourceType.ENUM).withIdType(QFieldType.STRING)
         .withEnumValues(List.of(new QPossibleValue<>("INFO", "Info"), new QPossibleValue<>("WARNING", "Warning"),
            new QPossibleValue<>("ERROR", "Error"), new QPossibleValue<>("CRITICAL", "Critical"))));
      for(String editable : List.of("enabled", "intervalSeconds", "timeoutMs", "failureThreshold", "recoveryThreshold"))
      {
         checks.getField(editable).setIsEditable(true);
      }
      checks.withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "name", "label", "component", "category", "kind", "severity", "registered")))
         .withSection(new QFieldSection("schedule", new QIcon("schedule"), Tier.T2, List.of("enabled", "intervalSeconds", "timeoutMs", "failureThreshold", "recoveryThreshold", "nextDueAt")))
         .withSection(new QFieldSection("currentState", new QIcon("monitor_heart"), Tier.T2, List.of("currentStatus", "currentMessage", "currentSince", "lastRunAt", "lastLatencyMs", "consecutiveFailures", "consecutiveSuccesses", "flapping")))
         .withAssociation(new Association().withName("results").withAssociatedTableName(RESULT_TABLE).withJoinName(RESULT_JOIN))
         .withSection(new QFieldSection("recentResults", new QIcon("history"), Tier.T2).withWidgetName(RESULT_JOIN))
         .withSection(new QFieldSection("latency", new QIcon("show_chart"), Tier.T2).withWidgetName(HealthLatencyWidget.NAME));

      QTableMetaData results = table(RESULT_TABLE, "Health Check Results", backendName)
         .withUniqueKey(new UniqueKey("healthCheckId", "slot", "onDemandRequestId"));
      fields(results, QFieldType.INTEGER, "healthCheckId");
      fields(results, QFieldType.LONG, "slot", "latencyMs");
      fields(results, QFieldType.STRING, "onDemandRequestId", "status", "message", "detailsJson", "metricsJson", "runnerPod", "triggeredBy");
      fields(results, QFieldType.DATE_TIME, "startedAt", "finishedAt", "leaseExpiresAt");
      fields(results, QFieldType.BOOLEAN, "isTransition");
      results.getField("onDemandRequestId").withDefaultValue("");
      // The runner always writes "" for scheduled claims, never null (SQL unique keys).
      results.getField("status").withPossibleValueSourceName(HealthResultStatus.NAME);
      results.getField("triggeredBy").withPossibleValueSourceName(HealthTriggeredBy.NAME);
      results.getField("message").withMaxLength(500);
      results.getField("detailsJson").withMaxLength(4096);
      results.getField("metricsJson").withMaxLength(65536);
      results.setRecordLabelFields(List.of("status"));

      QTableMetaData components = table(COMPONENT_TABLE, "Health Components", backendName).withUniqueKey(new UniqueKey("name"));
      fields(components, QFieldType.STRING, "name", "label", "status");
      fields(components, QFieldType.INTEGER, "checksUp", "checksDegraded", "checksDown", "checksUnknown", "worstCheckId");
      fields(components, QFieldType.DATE_TIME, "since", "updatedAt");
      components.getField("status").withPossibleValueSourceName(HealthStatus.NAME);
      for(QTableMetaData table : List.of(checks, results, components))
      {
         if(table != checks)
         {
            table.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
            table.withSection(new QFieldSection("identity", new QIcon("monitor_heart"), Tier.T1, new ArrayList<>(table.getFields().keySet())));
         }
         if(enricher != null)
         {
            enricher.accept(table);
         }
         qInstance.addTable(table);
      }
      qInstance.addJoin(new QJoinMetaData().withName(RESULT_JOIN).withLeftTable(CHECK_TABLE).withRightTable(RESULT_TABLE)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "healthCheckId")));
      qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin(RESULT_JOIN)).withLabel("Recent results")
         .withCanAddChildRecord(false).withOrderBys(List.of(new QFilterOrderBy("startedAt", false))).getWidgetMetaData());
      qInstance.addWidget(new QWidgetMetaData().withName(HealthLatencyWidget.NAME).withLabel("Latency")
         .withType(WidgetType.SMALL_LINE_CHART.getType()).withCodeReference(new QCodeReference(HealthLatencyWidget.class))
         .withPermissionRules(readPermissions()).withGridColumns(12));
      defineAlerts(qInstance);
      registerCoreChecks(qInstance);
      qInstance.addProcess(HealthRunCheckNowProcess.define());
      HealthWidgets.define(qInstance);
      qInstance.addApp(new QAppMetaData().withName("systemHealth").withLabel("System Health").withIcon(new QIcon("monitor_heart"))
         .withChild(checks).withWidgets(List.of("healthSummary", "healthComponents", "healthProblems", "healthTransitions"))
         .withPermissionRules(readPermissions()));
   }



   /*******************************************************************************
    ** Core uses health.read / health.write for table permissions. These are the
    ** standard equivalents of the spec's health.view / health.admin; use the
    ** same table checker for app, widgets and record action to avoid extra grants.
    *******************************************************************************/
   static QPermissionRules readPermissions()
   {
      return (new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withCustomPermissionChecker(UseTablePermissionCustomPermissionChecker.build(CHECK_TABLE, TablePermissionSubType.READ)));
   }



   /*******************************************************************************
    ** Build a table using normal read/write rules and snake_case storage names.
    *******************************************************************************/
   private QTableMetaData table(String name, String label, String backend)
   {
      return (new QTableMetaData().withName(name).withLabel(label).withBackendName(backend).withPrimaryKeyField("id")
         .withRecordLabelFields("label").withRecordLabelFormat("%s").withIcon(new QIcon("monitor_heart"))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS).withPermissionBaseName("health"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("id").withIsEditable(false)));
   }



   /*******************************************************************************
    ** Shared column enricher; callers may add backend-specific schema metadata.
    *******************************************************************************/
   private void fields(QTableMetaData table, QFieldType type, String... names)
   {
      for(String name : names)
      {
         table.withField(new QFieldMetaData(name, type).withBackendName(name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT))
            .withIsEditable(false));
      }
   }



   /*******************************************************************************
    ** Alert sinks and heartbeat monitors remain supplied by the alerts SPI.
    *******************************************************************************/
   private void defineAlerts(QInstance qInstance)
   {
      AlertsInstanceMetaData alerts = AlertsInstanceMetaData.ofOrWithNew(qInstance);
      for(String suffix : List.of("down", "degraded", "flapping"))
      {
         alerts.withAlertType(new QAlertTypeMetaData().withName("health.check." + suffix).withLabel("Health check " + suffix)
            .withDefaultSeverity(AlertSeverity.WARNING).withSubjectTableName(CHECK_TABLE).withVisibilityPermission("health.read"));
      }
      alerts.withAlertType(new QAlertTypeMetaData().withName("health.runner").withLabel("Health runner not running")
         .withDefaultSeverity(AlertSeverity.ERROR).withVisibilityPermission("health.read")
         .withDefaultMonitor(AlertDefaultMonitor.heartbeat("health.runner", "health.runner", 60, 180)));
   }



   /*******************************************************************************
    ** Core checks have no dependency on RDBMS, Quartz internals or other qbits.
    *******************************************************************************/
   private void registerCoreChecks(QInstance qInstance) throws QException
   {
      HealthChecksMetaData registry = HealthChecksMetaData.ofOrWithNew(qInstance);
      registry.withCheck(new HealthCheckMetaData().withName("core.scheduler").withLabel("Scheduler").withComponent("core")
         .withCategory(HealthCategory.CORE).withKind(HealthCheckKind.READINESS).withSeverity(AlertSeverity.ERROR)
         .withCheck(new QCodeReference(HealthSchedulerCheck.class)));
      if(QSessionStoreRegistry.getInstance().isAvailable())
      {
         registry.withCheck(new HealthCheckMetaData().withName("core.session_store").withLabel("Session store").withComponent("core")
            .withCategory(HealthCategory.CORE).withKind(HealthCheckKind.READINESS).withSeverity(AlertSeverity.ERROR).withIntervalSeconds(30)
            .withCheck(new QCodeReference(HealthSessionStoreCheck.class)));
      }
      registry.withCheck(new HealthCheckMetaData().withName("core.health.retention").withLabel("Health retention").withComponent("core")
         .withCategory(HealthCategory.CORE).withKind(HealthCheckKind.DEPENDENCY).withIntervalSeconds(86400)
         .withCheckInstance(input -> HealthCheckOutcome.up("Health history retained")
            .withMetrics(List.of(new HealthMetric().withName("deleted").withValue(HealthRetention.sweep(input.getQInstance(), Instant.now())).withUnit("rows")))));
      for(QBackendMetaData backend : qInstance.getBackends().values())
      {
         HealthCheck check = new QBackendModuleDispatcher().getQBackendModule(backend).getHealthCheck(backend);
         if(check != null)
         {
            registry.withCheck(new HealthCheckMetaData().withName("core.database." + backend.getName()).withLabel("Database " + backend.getName())
               .withComponent("database").withCategory(HealthCategory.DATABASE).withKind(HealthCheckKind.READINESS)
               .withSeverity(AlertSeverity.CRITICAL).withIncludeInReadiness(true).withIntervalSeconds(30).withTimeoutMs(3000)
               .withFailureThreshold(1).withCheckInstance(check));
         }
      }
   }
}
