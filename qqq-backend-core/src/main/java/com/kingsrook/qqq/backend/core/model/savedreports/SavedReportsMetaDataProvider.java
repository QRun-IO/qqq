/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.savedreports;


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.CronUIWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.DefaultWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormatPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.common.TimeZonePossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUISetupData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CronExpressionDisplayValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareScopePossibleValueMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableAudienceType;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.RenderSavedReportMetaDataProducer;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.RunScheduledReportMetaDataProducer;


/*******************************************************************************
 **
 *******************************************************************************/
public class SavedReportsMetaDataProvider
{
   public static final String REPORT_STORAGE_TABLE_NAME = "reportStorage";

   public static final String SAVED_REPORT_JOIN_SCHEDULED_REPORT    = "scheduledReportJoinSavedReport";
   public static final String SHARED_SAVED_REPORT_JOIN_SAVED_REPORT = "sharedSavedReportJoinSavedReport";

   public static final String SCHEDULED_REPORT_VALUES_WIDGET      = "scheduledReportValuesWidget";
   public static final String SCHEDULED_REPORT_CRON_WIDGET        = "scheduledReportCronWidget";
   public static final String RENDER_REPORT_PROCESS_VALUES_WIDGET = "renderReportProcessValuesWidget";



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String recordTablesBackendName, String reportStorageBackendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      instance.addTable(defineSavedReportTable(recordTablesBackendName, backendDetailEnricher));
      instance.addTable(defineRenderedReportTable(recordTablesBackendName, backendDetailEnricher));

      instance.addPossibleValueSource(QPossibleValueSource.newForTable(SavedReport.TABLE_NAME));
      instance.addPossibleValueSource(QPossibleValueSource.newForEnum(ReportFormatPossibleValueEnum.NAME, ReportFormatPossibleValueEnum.values()));
      instance.addPossibleValueSource(QPossibleValueSource.newForEnum(RenderedReportStatus.NAME, RenderedReportStatus.values()));

      instance.addTable(defineReportStorageTable(reportStorageBackendName, backendDetailEnricher));

      QProcessMetaData renderSavedReportProcess = new RenderSavedReportMetaDataProducer().produce(instance);
      instance.addProcess(renderSavedReportProcess);
      renderSavedReportProcess.getInputFields().stream()
         .filter(f -> RenderSavedReportMetaDataProducer.FIELD_NAME_STORAGE_TABLE_NAME.equals(f.getName()))
         .findFirst()
         .ifPresent(f -> f.setDefaultValue(REPORT_STORAGE_TABLE_NAME));
      instance.addWidget(defineRenderReportProcessValuesWidget());

      instance.addWidget(defineReportSetupWidget());
      instance.addWidget(definePivotTableSetupWidget());

      ////////////////////////////////////////
      // todo - param to enable scheduling? //
      ////////////////////////////////////////
      instance.addTable(defineScheduledReportTable(recordTablesBackendName, backendDetailEnricher));
      QJoinMetaData join = defineSavedReportJoinScheduledReport();
      instance.addJoin(join);
      instance.addWidget(defineScheduledReportJoinSavedReportWidget(join));
      QProcessMetaData scheduledReportSyncToScheduledJobProcess = new ScheduledReportSyncToScheduledJobProcess().produce(instance);
      instance.addProcess(scheduledReportSyncToScheduledJobProcess);
      instance.addWidget(defineScheduledReportValuesWidget());
      instance.addWidget(defineScheduledReportCronWidget());

      QProcessMetaData runScheduledReportProcess = new RunScheduledReportMetaDataProducer().produce(instance);
      instance.addProcess(runScheduledReportProcess);

      if(instance.getPossibleValueSource(TimeZonePossibleValueSourceMetaDataProvider.NAME) == null)
      {
         instance.addPossibleValueSource(new TimeZonePossibleValueSourceMetaDataProvider().produce());
      }

      /////////////////////////////////////
      // todo - param to enable sharing? //
      /////////////////////////////////////
      instance.addTable(defineSharedSavedReportTable(recordTablesBackendName, backendDetailEnricher));
      instance.addJoin(defineSharedSavedReportJoinSavedReport());
      if(instance.getPossibleValueSource(ShareScopePossibleValueMetaDataProducer.NAME) == null)
      {
         instance.addPossibleValueSource(new ShareScopePossibleValueMetaDataProducer().produce(new QInstance()));
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private QWidgetMetaDataInterface defineScheduledReportCronWidget()
   {
      CronUISetupData cronUISetupData = new CronUISetupData(ScheduledJob.TABLE_NAME, "cronExpression", "cronTimeZoneId");
      return (CronUIWidgetRenderer.buildWidgetMetaData(SCHEDULED_REPORT_CRON_WIDGET, "Schedule", cronUISetupData));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QWidgetMetaDataInterface defineScheduledReportValuesWidget()
   {
      return new QWidgetMetaData()
         .withName(SCHEDULED_REPORT_VALUES_WIDGET)
         .withType(WidgetType.DYNAMIC_FORM.getType())
         .withIsCard(true)
         .withLabel("Variable Values")
         .withCodeReference(new QCodeReference(ReportValuesDynamicFormWidgetRenderer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QWidgetMetaDataInterface defineRenderReportProcessValuesWidget()
   {
      return new QWidgetMetaData()
         .withName(RENDER_REPORT_PROCESS_VALUES_WIDGET)
         .withType(WidgetType.DYNAMIC_FORM.getType())
         .withIsCard(false)
         .withDefaultValue("isEditable", true)
         .withCodeReference(new QCodeReference(ReportValuesDynamicFormWidgetRenderer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QJoinMetaData defineSavedReportJoinScheduledReport()
   {
      return (new QJoinMetaData()
         .withName(SAVED_REPORT_JOIN_SCHEDULED_REPORT)
         .withLeftTable(SavedReport.TABLE_NAME)
         .withRightTable(ScheduledReport.TABLE_NAME)
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "savedReportId")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QWidgetMetaDataInterface defineScheduledReportJoinSavedReportWidget(QJoinMetaData join)
   {
      return ChildRecordListRenderer.widgetMetaDataBuilder(join)
         .withLabel("Schedules")
         .withCanAddChildRecord(true)
         .getWidgetMetaData();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QJoinMetaData defineSharedSavedReportJoinSavedReport()
   {
      return (new QJoinMetaData()
         .withName(SHARED_SAVED_REPORT_JOIN_SAVED_REPORT)
         .withLeftTable(SharedSavedReport.TABLE_NAME)
         .withRightTable(SavedReport.TABLE_NAME)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("savedReportId", "id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineReportStorageTable(String backendName, Consumer<QTableMetaData> backendDetailEnricher)
   {
      QTableMetaData table = new QTableMetaData()
         .withName(REPORT_STORAGE_TABLE_NAME)
         .withBackendName(backendName)
         .withPrimaryKeyField("reference")
         .withField(new QFieldMetaData("reference", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB));

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QWidgetMetaDataInterface defineReportSetupWidget()
   {
      return new QWidgetMetaData()
         .withName("reportSetupWidget")
         .withLabel("Filters and Columns")
         .withIsCard(true)
         .withType(WidgetType.FILTER_AND_COLUMNS_SETUP.getType())
         .withCodeReference(new QCodeReference(SavedReportsFilterAndColumnsSetupRenderer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QWidgetMetaDataInterface definePivotTableSetupWidget()
   {
      return new QWidgetMetaData()
         .withName("pivotTableSetupWidget")
         .withLabel("Pivot Table")
         .withIsCard(true)
         .withType(WidgetType.PIVOT_TABLE_SETUP.getType())
         .withCodeReference(new QCodeReference(DefaultWidgetRenderer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineSavedReportTable(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(SavedReport.TABLE_NAME)
         .withLabel("Report")
         .withIcon(new QIcon().withName("article"))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("label")
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(SavedReport.class)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD))
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "label", "tableName")))
         .withSection(new QFieldSection("filtersAndColumns", new QIcon().withName("table_chart"), Tier.T2).withLabel("Filters and Columns").withWidgetName("reportSetupWidget"))
         .withSection(new QFieldSection("pivotTable", new QIcon().withName("pivot_table_chart"), Tier.T2).withLabel("Pivot Table").withWidgetName("pivotTableSetupWidget"))
         .withSection(new QFieldSection("schedule", new QIcon().withName("schedule"), Tier.T2).withWidgetName(SAVED_REPORT_JOIN_SCHEDULED_REPORT))
         .withSection(new QFieldSection("data", new QIcon().withName("text_snippet"), Tier.T2, List.of("queryFilterJson", "columnsJson", "pivotTableJson")).withIsHidden(true))
         .withSection(new QFieldSection("hidden", new QIcon().withName("text_snippet"), Tier.T2, List.of("inputFieldsJson", "userId")).withIsHidden(true))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      table.getField("queryFilterJson").withBehavior(SavedReportJsonFieldDisplayValueFormatter.getInstance());
      table.getField("columnsJson").withBehavior(SavedReportJsonFieldDisplayValueFormatter.getInstance());
      table.getField("pivotTableJson").withBehavior(SavedReportJsonFieldDisplayValueFormatter.getInstance());

      table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(SavedReportTableCustomizer.class));
      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(SavedReportTableCustomizer.class));
      table.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(SavedReportTableCustomizer.class));

      table.withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(SharedSavedReport.TABLE_NAME)
         .withAssetIdFieldName("savedReportId")
         .withScopeFieldName("scope")
         .withThisTableOwnerIdFieldName("userId")
         .withAudienceType(new ShareableAudienceType().withName("user").withFieldName("userId")));

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineSharedSavedReportTable(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(SharedSavedReport.TABLE_NAME)
         .withLabel("Shared Report")
         .withIcon(new QIcon().withName("share"))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("savedReportId")
         .withBackendName(backendName)
         .withUniqueKey(new UniqueKey("savedReportId", "userId"))
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(SharedSavedReport.class)
         // todo - security key
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD))
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "savedReportId", "userId")))
         .withSection(new QFieldSection("data", new QIcon().withName("text_snippet"), Tier.T2, List.of("scope")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineRenderedReportTable(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(RenderedReport.TABLE_NAME)
         .withIcon(new QIcon().withName("print"))
         .withRecordLabelFormat("%s - %s")
         .withRecordLabelFields("savedReportId", "startTime")
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(RenderedReport.class)
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "savedReportId", "renderedReportStatusId")))
         .withSection(new QFieldSection("input", new QIcon().withName("input"), Tier.T2, List.of("userId", "reportFormat")))
         .withSection(new QFieldSection("output", new QIcon().withName("output"), Tier.T2, List.of("jobUuid", "resultPath", "rowCount", "errorMessage", "startTime", "endTime")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      table.getField("renderedReportStatusId").setAdornments(List.of(new FieldAdornment(AdornmentType.CHIP)
         .withValues(AdornmentType.ChipValues.iconAndColorValues(RenderedReportStatus.RUNNING.getId(), "pending", AdornmentType.ChipValues.COLOR_SECONDARY))
         .withValues(AdornmentType.ChipValues.iconAndColorValues(RenderedReportStatus.COMPLETE.getId(), "check", AdornmentType.ChipValues.COLOR_SUCCESS))
         .withValues(AdornmentType.ChipValues.iconAndColorValues(RenderedReportStatus.FAILED.getId(), "error", AdornmentType.ChipValues.COLOR_ERROR))));

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScheduledReportTable(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(ScheduledReport.TABLE_NAME)
         .withIcon(new QIcon().withName("schedule_send"))
         .withRecordLabelFormat("%s (Schedule %s)")
         .withRecordLabelFields("savedReportId", "id")
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(ScheduledReport.class)
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "savedReportId")))
         .withSection(new QFieldSection("settings", new QIcon().withName("settings"), Tier.T2, List.of("isActive", "format")))
         .withSection(new QFieldSection("schedule", new QIcon().withName("schedule"), Tier.T2).withWidgetName(SCHEDULED_REPORT_CRON_WIDGET))
         .withSection(new QFieldSection("email", new QIcon().withName("email"), Tier.T2, List.of("toAddresses", "subject")))
         .withSection(new QFieldSection("variableValues", new QIcon().withName("data_object"), Tier.T2).withWidgetName(SCHEDULED_REPORT_VALUES_WIDGET))
         .withSection(new QFieldSection("hidden", new QIcon().withName("visibility_off"), Tier.T2, List.of("inputValues", "userId", "cronExpression", "cronTimeZoneId")).withIsHidden(true))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      table.getField("cronExpression").withBehavior(new CronExpressionDisplayValueBehavior());

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(ScheduledReportTableCustomizer.class));
      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(ScheduledReportTableCustomizer.class));
      table.withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(ScheduledReportTableCustomizer.class));
      table.withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(ScheduledReportTableCustomizer.class));
      table.withCustomizer(TableCustomizers.POST_DELETE_RECORD, new QCodeReference(ScheduledReportTableCustomizer.class));

      return (table);
   }

}
