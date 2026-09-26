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

package com.kingsrook.qqq.backend.module.filesystem.processes.implementations.filesystem.importer;


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;


/*******************************************************************************
 ** Class to serve as a template for producing an instance of a process & tables
 ** that provide the QQQ service to manage importing files (e.g., partner feeds on S3).
 **
 ** The template contains the following components:
 ** - A process that loads files from a source-table (e.g., of filesystem, cardinality=ONE)
 **   and stores them in the following tables:
 ** - {baseName}importFile table - simple header for imported files.
 ** - {baseName}importRecord table - a record foreach record in an imported file.
 ** - PVS for the importFile table
 ** - Join & Widget (to show importRecords on importFile view screen)
 **
 ** Most likely one would add all the meta-data objects in an instance of this
 ** template, then either use tableAutomations or a basepull process against records
 ** in the importRecord table, to run through a process (e.g., an AbstractTableSync)
 ** to result in final values for your business case.
 **
 ** A typical usage may look like:
 **
 ** <pre>
 // set up the process that'll be used to import the files.
 FilesystemImporterProcessMetaDataBuilder importerProcessBuilder = (FilesystemImporterProcessMetaDataBuilder) new FilesystemImporterProcessMetaDataBuilder()
 .withFileFormat("csv")
 .withSourceTableName(MyFeedSourceTableMetaDataProducer.NAME)
 .withRemoveFileAfterImport(true)
 .withUpdateFileIfNameExists(false)
 .withName("myFeedImporter")
 .withSchedule(new QScheduleMetaData().withRepeatSeconds(300));

 FilesystemImporterMetaDataTemplate template = new FilesystemImporterMetaDataTemplate(qInstance, "myFeed", MongoDBMetaDataProducer.NAME, importerProcessBuilder, table ->
 {
 // whatever customizations you may need on the tables
 table.withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED));
 });

 // set up automations on the table
 template.addAutomationStatusField(template.getImportRecordTable(), getStandardAutomationStatusField().withBackendName("metaData.automationStatus"));
 template.addStandardPostInsertAutomation(template.getImportRecordTable(), getBasicTableAutomationDetails(), "myFeedTableSyncProcess");

 // finally, add all the meta-data from the template to a QInstance
 template.addToInstance(qInstance);
 </pre>
 **
 *******************************************************************************/
public class FilesystemImporterMetaDataTemplate
{
   public static final String IMPORT_FILE_TABLE_SUFFIX       = "ImportFile";
   public static final String IMPORT_RECORD_TABLE_SUFFIX     = "ImportRecord";
   public static final String IMPORT_FILE_RECORD_JOIN_SUFFIX = "ImportFileImportRecordJoin";

   private QTableMetaData                           importFileTable;
   private QTableMetaData                           importRecordTable;
   private QPossibleValueSource                     importFilePVS;
   private QJoinMetaData                            importFileImportRecordJoin;
   private QWidgetMetaDataInterface                 importFileImportRecordJoinWidget;
   private FilesystemImporterProcessMetaDataBuilder importerProcessMetaDataBuilder;



   /*******************************************************************************
    **
    *******************************************************************************/
   public FilesystemImporterMetaDataTemplate(QInstance qInstance, String importBaseName, String backendName, FilesystemImporterProcessMetaDataBuilder importerProcessMetaDataBuilder, Consumer<QTableMetaData> tableEnricher)
   {
      QBackendMetaData backend = qInstance.getBackend(backendName);

      this.importFileTable = defineTableImportFile(backend, importBaseName);
      this.importRecordTable = defineTableImportRecord(backend, importBaseName);

      for(QTableMetaData table : List.of(this.importFileTable, this.importRecordTable))
      {
         table.setBackendName(backendName);
         if(tableEnricher != null)
         {
            tableEnricher.accept(table);
         }
      }

      this.importFilePVS = QPossibleValueSource.newForTable(this.importFileTable.getName());

      this.importFileImportRecordJoin = defineImportFileImportRecordJoin(importBaseName);
      this.importFileImportRecordJoinWidget = defineImportFileImportRecordChildWidget(this.importFileImportRecordJoin);

      this.importerProcessMetaDataBuilder = importerProcessMetaDataBuilder
         .withImportFileTable(this.importFileTable.getName())
         .withImportRecordTable(this.importRecordTable.getName());
   }



   /*******************************************************************************
    ** Set up importRecord table being built by this template to hve an automation-
    ** status field on it, and an automation details object attached to it.
    *******************************************************************************/
   public void addImportRecordAutomations(QFieldMetaData automationStatusField, QTableAutomationDetails automationDetails)
   {
      getImportRecordTable().addField(automationStatusField);
      getImportRecordTable().getSections().get(1).getFieldNames().add(0, automationStatusField.getName());
      getImportRecordTable().withAutomationDetails(automationDetails);
   }



   /*******************************************************************************
    ** Add 1 process as a post-insert automation-action on this template's importRecord
    ** table.
    **
    ** The automation action is returned - which you may want for changing things, e.g.,
    ** its priority (e.g., addImportRecordPostInsertAutomationAction(...).withPriority(1);
    *******************************************************************************/
   public TableAutomationAction addImportRecordPostInsertAutomationAction(String processName)
   {
      if(getImportRecordTable().getAutomationDetails() == null)
      {
         throw (new IllegalStateException(getImportRecordTable().getName() + " does not have automationDetails - do you need to call addAutomations first?"));
      }

      TableAutomationAction action = new TableAutomationAction()
         .withName(processName)
         .withTriggerEvent(TriggerEvent.POST_INSERT)
         .withProcessName(processName);

      getImportRecordTable().getAutomationDetails().withAction(action);

      return (action);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QWidgetMetaDataInterface defineImportFileImportRecordChildWidget(QJoinMetaData join)
   {
      return ChildRecordListRenderer.widgetMetaDataBuilder(join)
         .withName(join.getName())
         .withLabel("Import Records")
         .withMaxRows(100)
         .withCanAddChildRecord(false)
         .getWidgetMetaData();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QJoinMetaData defineImportFileImportRecordJoin(String importBaseName)
   {
      return new QJoinMetaData()
         .withLeftTable(importBaseName + IMPORT_FILE_TABLE_SUFFIX)
         .withRightTable(importBaseName + IMPORT_RECORD_TABLE_SUFFIX)
         .withName(importBaseName + IMPORT_FILE_RECORD_JOIN_SUFFIX)
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "importFileId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineTableImportFile(QBackendMetaData backend, String importBaseName)
   {
      QFieldType idType = getIdFieldType(backend);

      QTableMetaData qTableMetaData = new QTableMetaData()
         .withName(importBaseName + IMPORT_FILE_TABLE_SUFFIX)
         .withIcon(new QIcon().withName("upload_file"))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("sourceFileName")
         .withPrimaryKeyField("id")
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD))

         .withField(new QFieldMetaData("id", idType).withIsEditable(false).withBackendName(getIdFieldBackendName(backend)))
         .withField(new QFieldMetaData("sourceFileName", QFieldType.STRING))
         .withField(new QFieldMetaData("archivedPath", QFieldType.STRING))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))

         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "sourceFileName", "archivedPath")))
         .withSection(new QFieldSection("records", new QIcon().withName("power_input"), Tier.T2).withWidgetName(importBaseName + IMPORT_FILE_RECORD_JOIN_SUFFIX))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")))

         .withAssociation(new Association().withName("importRecords").withJoinName(importBaseName + IMPORT_FILE_RECORD_JOIN_SUFFIX).withAssociatedTableName(importBaseName + IMPORT_RECORD_TABLE_SUFFIX));

      return (qTableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldType getIdFieldType(QBackendMetaData backend)
   {
      QFieldType idType = QFieldType.INTEGER;
      if("mongodb".equals(backend.getBackendType()))
      {
         idType = QFieldType.STRING;
      }
      return idType;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getIdFieldBackendName(QBackendMetaData backend)
   {
      if("mongodb".equals(backend.getBackendType()))
      {
         return ("_id");
      }
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineTableImportRecord(QBackendMetaData backend, String importBaseName)
   {
      QFieldType idType = getIdFieldType(backend);

      QTableMetaData qTableMetaData = new QTableMetaData()
         .withName(importBaseName + IMPORT_RECORD_TABLE_SUFFIX)
         .withIcon(new QIcon().withName("power_input"))
         .withRecordLabelFormat("%s - Record %s")
         .withRecordLabelFields("importFileId", "recordNo")
         .withPrimaryKeyField("id")
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD))
         .withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(ImportRecordPostQueryCustomizer.class))

         .withField(new QFieldMetaData("id", idType).withIsEditable(false).withBackendName(getIdFieldBackendName(backend)))

         .withField(new QFieldMetaData("importFileId", idType).withBackendName("metaData.importFileId")
            .withPossibleValueSourceName(importBaseName + IMPORT_FILE_TABLE_SUFFIX))
         .withField(new QFieldMetaData("recordNo", QFieldType.INTEGER).withBackendName("metaData.recordNo"))

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // so, we'll use this field as a "virtual" field, e.g., populated with JSON in table post-query customizer, with all un-structured values //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         .withField(new QFieldMetaData("values", QFieldType.TEXT)
            .withIsEditable(false)
            .withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR)
               .withValue(AdornmentType.CodeEditorValues.languageMode("json"))))

         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("metaData.createDate").withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("metaData.modifyDate").withIsEditable(false))

         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "importFileId", "recordNo")))
         .withSection(new QFieldSection("data", new QIcon().withName("text_snippet"), Tier.T2, List.of("values")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      return (qTableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addToInstance(QInstance instance)
   {
      instance.add(importFileTable);
      instance.add(importRecordTable);
      instance.add(importFilePVS);
      instance.add(importFileImportRecordJoin);
      instance.add(importFileImportRecordJoinWidget);
      instance.add(importerProcessMetaDataBuilder.getProcessMetaData());
   }



   /*******************************************************************************
    ** Getter for importFileTable
    *******************************************************************************/
   public QTableMetaData getImportFileTable()
   {
      return (this.importFileTable);
   }



   /*******************************************************************************
    ** Setter for importFileTable
    *******************************************************************************/
   public void setImportFileTable(QTableMetaData importFileTable)
   {
      this.importFileTable = importFileTable;
   }



   /*******************************************************************************
    ** Fluent setter for importFileTable
    *******************************************************************************/
   public FilesystemImporterMetaDataTemplate withImportFileTable(QTableMetaData importFileTable)
   {
      this.importFileTable = importFileTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for importRecordTable
    *******************************************************************************/
   public QTableMetaData getImportRecordTable()
   {
      return (this.importRecordTable);
   }



   /*******************************************************************************
    ** Setter for importRecordTable
    *******************************************************************************/
   public void setImportRecordTable(QTableMetaData importRecordTable)
   {
      this.importRecordTable = importRecordTable;
   }



   /*******************************************************************************
    ** Fluent setter for importRecordTable
    *******************************************************************************/
   public FilesystemImporterMetaDataTemplate withImportRecordTable(QTableMetaData importRecordTable)
   {
      this.importRecordTable = importRecordTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for importFilePVS
    *******************************************************************************/
   public QPossibleValueSource getImportFilePVS()
   {
      return (this.importFilePVS);
   }



   /*******************************************************************************
    ** Setter for importFilePVS
    *******************************************************************************/
   public void setImportFilePVS(QPossibleValueSource importFilePVS)
   {
      this.importFilePVS = importFilePVS;
   }



   /*******************************************************************************
    ** Fluent setter for importFilePVS
    *******************************************************************************/
   public FilesystemImporterMetaDataTemplate withImportFilePVS(QPossibleValueSource importFilePVS)
   {
      this.importFilePVS = importFilePVS;
      return (this);
   }



   /*******************************************************************************
    ** Getter for importFileImportRecordJoin
    *******************************************************************************/
   public QJoinMetaData getImportFileImportRecordJoin()
   {
      return (this.importFileImportRecordJoin);
   }



   /*******************************************************************************
    ** Setter for importFileImportRecordJoin
    *******************************************************************************/
   public void setImportFileImportRecordJoin(QJoinMetaData importFileImportRecordJoin)
   {
      this.importFileImportRecordJoin = importFileImportRecordJoin;
   }



   /*******************************************************************************
    ** Fluent setter for importFileImportRecordJoin
    *******************************************************************************/
   public FilesystemImporterMetaDataTemplate withImportFileImportRecordJoin(QJoinMetaData importFileImportRecordJoin)
   {
      this.importFileImportRecordJoin = importFileImportRecordJoin;
      return (this);
   }



   /*******************************************************************************
    ** Getter for importFileImportRecordJoinWidget
    *******************************************************************************/
   public QWidgetMetaDataInterface getImportFileImportRecordJoinWidget()
   {
      return (this.importFileImportRecordJoinWidget);
   }



   /*******************************************************************************
    ** Setter for importFileImportRecordJoinWidget
    *******************************************************************************/
   public void setImportFileImportRecordJoinWidget(QWidgetMetaDataInterface importFileImportRecordJoinWidget)
   {
      this.importFileImportRecordJoinWidget = importFileImportRecordJoinWidget;
   }



   /*******************************************************************************
    ** Fluent setter for importFileImportRecordJoinWidget
    *******************************************************************************/
   public FilesystemImporterMetaDataTemplate withImportFileImportRecordJoinWidget(QWidgetMetaDataInterface importFileImportRecordJoinWidget)
   {
      this.importFileImportRecordJoinWidget = importFileImportRecordJoinWidget;
      return (this);
   }



   /*******************************************************************************
    ** Getter for importerProcessMetaDataBuilder
    *******************************************************************************/
   public FilesystemImporterProcessMetaDataBuilder getImporterProcessMetaDataBuilder()
   {
      return (this.importerProcessMetaDataBuilder);
   }



   /*******************************************************************************
    ** Setter for importerProcessMetaDataBuilder
    *******************************************************************************/
   public void setImporterProcessMetaDataBuilder(FilesystemImporterProcessMetaDataBuilder importerProcessMetaDataBuilder)
   {
      this.importerProcessMetaDataBuilder = importerProcessMetaDataBuilder;
   }



   /*******************************************************************************
    ** Fluent setter for importerProcessMetaDataBuilder
    *******************************************************************************/
   public FilesystemImporterMetaDataTemplate withImporterProcessMetaDataBuilder(FilesystemImporterProcessMetaDataBuilder importerProcessMetaDataBuilder)
   {
      this.importerProcessMetaDataBuilder = importerProcessMetaDataBuilder;
      return (this);
   }

}
