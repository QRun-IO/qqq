/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.scripts;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.DefaultWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.automation.TableTrigger;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PVSValueFormatAndFields;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TablesSupportingAutomationsPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.scripts.LoadScriptTestDetailsProcessStep;
import com.kingsrook.qqq.backend.core.processes.implementations.scripts.RunRecordScriptExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.scripts.RunRecordScriptLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.scripts.RunRecordScriptTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.scripts.StoreScriptRevisionProcessStep;
import com.kingsrook.qqq.backend.core.processes.implementations.scripts.TestScriptProcessStep;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScriptsMetaDataProvider
{
   public static final String RUN_RECORD_SCRIPT_PROCESS_NAME        = "runRecordScript";
   public static final String STORE_SCRIPT_REVISION_PROCESS_NAME    = "storeScriptRevision";
   public static final String TEST_SCRIPT_PROCESS_NAME              = "testScript";
   public static final String LOAD_SCRIPT_TEST_DETAILS_PROCESS_NAME = "loadScriptTestDetails";

   public static final String SCRIPT_TYPE_NAME_RECORD = "Record Script";

   public static final String CURRENT_SCRIPT_REVISION_JOIN_NAME = "currentScriptRevision";



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      defineStandardScriptsTables(instance, backendName, backendDetailEnricher);
      defineStandardScriptsPossibleValueSources(instance);
      defineStandardScriptsJoins(instance);
      defineStandardScriptsWidgets(instance);
      instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      instance.addPossibleValueSource(TablesSupportingAutomationsPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      instance.addProcess(defineStoreScriptRevisionProcess());
      instance.addProcess(defineTestScriptProcess());
      instance.addProcess(defineLoadScriptTestDetailsProcess());
      instance.addProcess(defineRunRecordScriptProcess());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QProcessMetaData defineLoadScriptTestDetailsProcess()
   {
      return (new QProcessMetaData()
         .withName(LOAD_SCRIPT_TEST_DETAILS_PROCESS_NAME)
         .withTableName(Script.TABLE_NAME)
         .withIsHidden(true)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("main")
               .withCode(new QCodeReference(LoadScriptTestDetailsProcessStep.class)))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QProcessMetaData defineStoreScriptRevisionProcess()
   {
      return (new QProcessMetaData()
         .withName(STORE_SCRIPT_REVISION_PROCESS_NAME)
         .withTableName(Script.TABLE_NAME)
         .withIsHidden(true)
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("main")
               .withCode(new QCodeReference(StoreScriptRevisionProcessStep.class))
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QProcessMetaData defineTestScriptProcess()
   {
      return (new QProcessMetaData()
         .withName(TEST_SCRIPT_PROCESS_NAME)
         .withTableName(Script.TABLE_NAME)
         .withIsHidden(true)
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("main")
               .withCode(new QCodeReference(TestScriptProcessStep.class))
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QProcessMetaData defineRunRecordScriptProcess()
   {
      QProcessMetaData processMetaData = StreamedETLWithFrontendProcess.processMetaDataBuilder()
         .withName(RUN_RECORD_SCRIPT_PROCESS_NAME)
         .withLabel("Run Script")
         .withIcon(new QIcon().withName("data_object"))
         .withSupportsFullValidation(false)
         .withExtractStepClass(RunRecordScriptExtractStep.class)
         .withTransformStepClass(RunRecordScriptTransformStep.class)
         .withLoadStepClass(RunRecordScriptLoadStep.class)
         .getProcessMetaData();

      processMetaData.withStep(0, new QFrontendStepMetaData()
         .withName("input")
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM))
         .withFormField(new QFieldMetaData("scriptId", QFieldType.INTEGER).withPossibleValueSourceName(Script.TABLE_NAME)
            .withPossibleValueSourceFilter(new QQueryFilter(
               new QFilterCriteria("scriptType.name", QCriteriaOperator.EQUALS, SCRIPT_TYPE_NAME_RECORD),
               new QFilterCriteria("tableName", QCriteriaOperator.EQUALS, "${input.tableName}")
            ))));

      return (processMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardScriptsWidgets(QInstance instance)
   {
      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(instance.getJoin(QJoinMetaData.makeInferredJoinName(ScriptLog.TABLE_NAME, ScriptLogLine.TABLE_NAME)))
         .withLabel("Log Lines")
         .getWidgetMetaData());

      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(instance.getJoin(QJoinMetaData.makeInferredJoinName(Script.TABLE_NAME, ScriptLog.TABLE_NAME))).withMaxRows(50)
         .withLabel("Recent Logs")
         .getWidgetMetaData());

      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(instance.getJoin(QJoinMetaData.makeInferredJoinName(ScriptType.TABLE_NAME, ScriptTypeFileSchema.TABLE_NAME)))
         .withLabel("File Schema")
         .getWidgetMetaData());

      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(instance.getJoin(QJoinMetaData.makeInferredJoinName(ScriptRevision.TABLE_NAME, ScriptRevisionFile.TABLE_NAME))).withMaxRows(50)
         .withLabel("Files")
         .getWidgetMetaData());

      instance.addWidget(new QWidgetMetaData()
         .withName("scriptViewer")
         .withLabel("Contents")
         .withIsCard(true)
         .withType(WidgetType.SCRIPT_VIEWER.getType())
         .withCodeReference(new QCodeReference(DefaultWidgetRenderer.class)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardScriptsJoins(QInstance instance)
   {
      instance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(ScriptLog.TABLE_NAME)
         .withRightTable(ScriptLogLine.TABLE_NAME)
         .withJoinOn(new JoinOn("id", "scriptLogId"))
         .withOrderBy(new QFilterOrderBy("id"))
         .withInferredName());

      instance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(Script.TABLE_NAME)
         .withRightTable(ScriptRevision.TABLE_NAME)
         .withJoinOn(new JoinOn("id", "scriptId"))
         .withOrderBy(new QFilterOrderBy("id"))
         .withInferredName());

      instance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_ONE)
         .withLeftTable(Script.TABLE_NAME)
         .withRightTable(ScriptRevision.TABLE_NAME)
         .withJoinOn(new JoinOn("currentScriptRevisionId", "id"))
         .withName(CURRENT_SCRIPT_REVISION_JOIN_NAME));

      instance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(ScriptType.TABLE_NAME)
         .withRightTable(Script.TABLE_NAME)
         .withJoinOn(new JoinOn("id", "scriptTypeId"))
         .withOrderBy(new QFilterOrderBy("id"))
         .withInferredName());

      instance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(Script.TABLE_NAME)
         .withRightTable(ScriptLog.TABLE_NAME)
         .withJoinOn(new JoinOn("id", "scriptId"))
         .withOrderBy(new QFilterOrderBy("id"))
         .withInferredName());

      instance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(ScriptType.TABLE_NAME)
         .withRightTable(ScriptTypeFileSchema.TABLE_NAME)
         .withJoinOn(new JoinOn("id", "scriptTypeId"))
         .withOrderBy(new QFilterOrderBy("id"))
         .withInferredName());

      instance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(ScriptRevision.TABLE_NAME)
         .withRightTable(ScriptRevisionFile.TABLE_NAME)
         .withJoinOn(new JoinOn("id", "scriptRevisionId"))
         .withOrderBy(new QFilterOrderBy("id"))
         .withInferredName());

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardScriptsTables(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      for(QTableMetaData tableMetaData : defineStandardScriptsTables(backendName, backendDetailEnricher))
      {
         instance.addTable(tableMetaData);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardScriptsPossibleValueSources(QInstance instance) throws QException
   {
      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(Script.TABLE_NAME)
         .withTableName(Script.TABLE_NAME)
         .withOrderByField("name"));

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(ScriptRevision.TABLE_NAME)
         .withTableName(ScriptRevision.TABLE_NAME)
         .withOrderByField("scriptId")
         .withOrderByField("sequenceNo", false));

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(ScriptType.TABLE_NAME)
         .withTableName(ScriptType.TABLE_NAME)
         .withOrderByField("name"));

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(ScriptLog.TABLE_NAME)
         .withTableName(ScriptLog.TABLE_NAME)
         .withOrderByField("id", false));

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(ScriptTypeFileMode.NAME)
         .withType(QPossibleValueSourceType.ENUM)
         .withValuesFromEnum(ScriptTypeFileMode.values())
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QTableMetaData> defineStandardScriptsTables(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      List<QTableMetaData> rs = new ArrayList<>();
      rs.add(enrich(backendDetailEnricher, defineScriptTypeTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineScriptTypeFileSchemaTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineScriptTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineScriptRevisionTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineScriptRevisionFileTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineScriptLogTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineScriptLogLineTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineTableTriggerTable(backendName)));
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData enrich(Consumer<QTableMetaData> backendDetailEnricher, QTableMetaData table)
   {
      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }
      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineStandardTable(String backendName, String name, Class<? extends QRecordEntity> fieldsFromEntity) throws QException
   {
      return new QTableMetaData()
         .withName(name)
         .withBackendName(backendName)
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("name")
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(fieldsFromEntity);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineTableTriggerTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, TableTrigger.TABLE_NAME, TableTrigger.class)
         .withRecordLabelFields("id")
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id")))
         .withSection(new QFieldSection("contents", new QIcon().withName("data_object"), Tier.T2, List.of("tableName", "filterId", "scriptId", "priority", "postInsert", "postUpdate")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      tableMetaData.getField("scriptId").withPossibleValueSourceFilter(new QQueryFilter(
         new QFilterCriteria("scriptType.name", QCriteriaOperator.EQUALS, SCRIPT_TYPE_NAME_RECORD),
         new QFilterCriteria("script.tableName", QCriteriaOperator.EQUALS, "${input.tableName}")
      ));

      tableMetaData.getField("filterId").withPossibleValueSourceFilter(new QQueryFilter(
         new QFilterCriteria("tableName", QCriteriaOperator.EQUALS, "${input.tableName}")
      ));

      return tableMetaData;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScriptTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, Script.TABLE_NAME, Script.class)
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "name", "scriptTypeId", "currentScriptRevisionId")))
         .withSection(new QFieldSection("recordScriptSettings", new QIcon().withName("table_rows"), Tier.T2, List.of("tableName", "maxBatchSize")))
         .withSection(new QFieldSection("contents", new QIcon().withName("data_object"), Tier.T2).withWidgetName("scriptViewer"))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")))
         .withSection(new QFieldSection("lines", new QIcon().withName("horizontal_rule"), Tier.T2).withWidgetName(QJoinMetaData.makeInferredJoinName(Script.TABLE_NAME, ScriptLog.TABLE_NAME)));

      tableMetaData.getField("name").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      tableMetaData.getField("currentScriptRevisionId").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      tableMetaData.getField("currentScriptRevisionId").withIsEditable(false);

      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScriptTypeTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, ScriptType.TABLE_NAME, ScriptType.class)
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "name")))
         .withSection(new QFieldSection("details", new QIcon().withName("dataset"), Tier.T2, List.of("helpText", "sampleCode", "fileMode", "testScriptInterfaceName")))
         .withSection(new QFieldSection("files", new QIcon().withName("description"), Tier.T2).withWidgetName(QJoinMetaData.makeInferredJoinName(ScriptType.TABLE_NAME, ScriptTypeFileSchema.TABLE_NAME)))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));
      tableMetaData.getField("sampleCode").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("javascript")));
      tableMetaData.getField("helpText").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("text")));
      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScriptTypeFileSchemaTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, ScriptTypeFileSchema.TABLE_NAME, ScriptTypeFileSchema.class)
         .withRecordLabelFormat("%s - %s")
         .withRecordLabelFields(List.of("scriptTypeId", "name"))
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "name", "scriptTypeId")))
         .withSection(new QFieldSection("details", new QIcon().withName("dataset"), Tier.T2, List.of("fileType")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));
      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScriptRevisionTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, ScriptRevision.TABLE_NAME, ScriptRevision.class)
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE)
         .withRecordLabelFormat("%s v%s")
         .withRecordLabelFields(List.of("scriptId", "sequenceNo"))
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "scriptId", "sequenceNo")))
         .withSection(new QFieldSection("files", new QIcon().withName("description"), Tier.T2).withWidgetName(QJoinMetaData.makeInferredJoinName(ScriptRevision.TABLE_NAME, ScriptRevisionFile.TABLE_NAME)))
         .withSection(new QFieldSection("changeManagement", new QIcon().withName("history"), Tier.T2, List.of("commitMessage", "author")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")))

         .withAssociation(new Association()
            .withName("files")
            .withAssociatedTableName(ScriptRevisionFile.TABLE_NAME)
            .withJoinName(QJoinMetaData.makeInferredJoinName(ScriptRevision.TABLE_NAME, ScriptRevisionFile.TABLE_NAME)));

      tableMetaData.getField("scriptId").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());

      try
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the api module is loaded, then add a section to the table for the api name & version fields //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         Class.forName("com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataProvider");
         tableMetaData.getSections().add(1, new QFieldSection("api", "API", new QIcon().withName("code"), Tier.T2, List.of("apiName", "apiVersion")));
      }
      catch(ClassNotFoundException e)
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the api module is not loaded, then make sure we don't have these fields in our scripts table //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         tableMetaData.getFields().remove("apiName");
         tableMetaData.getFields().remove("apiVersion");
      }

      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScriptRevisionFileTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, ScriptRevisionFile.TABLE_NAME, ScriptRevisionFile.class)
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE)
         .withRecordLabelFormat("%s - %s")
         .withRecordLabelFields(List.of("scriptRevisionId", "fileName"))
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "scriptRevisionId", "fileName")))
         .withSection(new QFieldSection("code", new QIcon().withName("data_object"), Tier.T2, List.of("contents")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      tableMetaData.getField("contents").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("velocity"))); // todo - dynamic?!
      tableMetaData.getField("scriptRevisionId").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());

      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScriptLogTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, ScriptLog.TABLE_NAME, ScriptLog.class)
         .withRecordLabelFields(List.of("id"))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE)
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id")))
         .withSection(new QFieldSection("script", new QIcon().withName("data_object"), Tier.T2, List.of("scriptId", "scriptRevisionId")))
         .withSection(new QFieldSection("timing", new QIcon().withName("schedule"), Tier.T2, List.of("startTimestamp", "endTimestamp", "runTimeMillis", "createDate", "modifyDate")))
         .withSection(new QFieldSection("error", "Error", new QIcon().withName("error_outline"), Tier.T2, List.of("hadError", "error")))
         .withSection(new QFieldSection("inputOutput", "Input/Output", new QIcon().withName("chat"), Tier.T2, List.of("input", "output")))
         .withSection(new QFieldSection("lines", new QIcon().withName("horizontal_rule"), Tier.T2).withWidgetName(QJoinMetaData.makeInferredJoinName(ScriptLog.TABLE_NAME, ScriptLogLine.TABLE_NAME)));

      tableMetaData.getField("scriptId").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      tableMetaData.getField("scriptRevisionId").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      tableMetaData.getField("input").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());
      tableMetaData.getField("output").withFieldAdornment(AdornmentType.Size.LARGE.toAdornment());

      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScriptLogLineTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, ScriptLogLine.TABLE_NAME, ScriptLogLine.class)
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE)
         .withRecordLabelFields(List.of("id"));

      tableMetaData.getField("text").withFieldAdornment(AdornmentType.Size.XLARGE.toAdornment());

      return (tableMetaData);
   }

}
