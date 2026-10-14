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

package com.kingsrook.qqq.backend.core.model.audits;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.processes.implementations.audits.GetAuditsForRecordProcess;


/*******************************************************************************
 ** MetaData provider for the QQQ audit system tables.
 **
 ** By default, the audit table's recordId field is INTEGER, which supports
 ** auditing tables with integer primary keys. To audit tables with String or
 ** UUID primary keys, configure the provider with a STRING recordId type:
 **
 ** <pre>
 ** new AuditsMetaDataProvider()
 **    .withRecordIdType(QFieldType.STRING)
 **    .defineAll(qInstance, backendName, enricher);
 ** </pre>
 *******************************************************************************/
public class AuditsMetaDataProvider
{
   public static final String TABLE_NAME_AUDIT_TABLE  = "auditTable";
   public static final String TABLE_NAME_AUDIT_USER   = "auditUser";
   public static final String TABLE_NAME_AUDIT        = "audit";
   public static final String TABLE_NAME_AUDIT_DETAIL = "auditDetail";

   /** the audit record view's section listing its field changes (audit details) */
   public static final String FIELD_CHANGES_SECTION_NAME = "fieldChanges";

   private QFieldType recordIdType = QFieldType.INTEGER;

   //////////////////////////////////////////////////////////////////////////
   // opt in to the audit record screens (see defineStandardAuditScreens). //
   // Off by default: an application that adds fields to the audit tables //
   // after defineAll must also add them to these sections.               //
   //////////////////////////////////////////////////////////////////////////
   private boolean defineRecordScreens = false;

   ///////////////////////////////////////////////////////////////////////////////
   // opt in to the audit.clientChannel field (which client made the change -   //
   // e.g. next-ui, assistant/<turnId>).  Off by default: the backing table     //
   // needs the column (VARCHAR(100), nullable) before turning this on.         //
   ///////////////////////////////////////////////////////////////////////////////
   private boolean includeClientChannelField = false;

   public static final String FIELD_CLIENT_CHANNEL = "clientChannel";



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      defineStandardAuditTables(instance, backendName, backendDetailEnricher);
      defineStandardAuditPossibleValueSources(instance);
      defineStandardAuditJoins(instance);
      if(defineRecordScreens)
      {
         defineStandardAuditScreens(instance);
      }

      GetAuditsForRecordProcess getAuditsForRecordProcess = new GetAuditsForRecordProcess();
      if(getAuditsForRecordProcess.isEnabled())
      {
         instance.addProcess(getAuditsForRecordProcess.produce(instance));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void defineStandardAuditJoins(QInstance instance)
   {
      instance.addJoin(new QJoinMetaData()
         .withLeftTable(TABLE_NAME_AUDIT)
         .withRightTable(TABLE_NAME_AUDIT_TABLE)
         .withInferredName()
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("auditTableId", "id")));

      instance.addJoin(new QJoinMetaData()
         .withLeftTable(TABLE_NAME_AUDIT)
         .withRightTable(TABLE_NAME_AUDIT_USER)
         .withInferredName()
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("auditUserId", "id")));

      instance.addJoin(new QJoinMetaData()
         .withLeftTable(TABLE_NAME_AUDIT)
         .withRightTable(TABLE_NAME_AUDIT_DETAIL)
         .withInferredName()
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "auditId")));

   }



   /*******************************************************************************
    ** Record screens for the audit tables: named sections instead of the
    ** generic ones, and an audit record's field changes (its audit details)
    ** listed on it. Needs the tables and the audit-to-detail join; a table or
    ** join that is absent is skipped, and so is a table that already has
    ** sections (an application's own layout wins). Run by defineAll when
    ** withDefineRecordScreens(true); callable directly after defineAll.
    *******************************************************************************/
   public void defineStandardAuditScreens(QInstance instance)
   {
      QTableMetaData audit = instance.getTable(TABLE_NAME_AUDIT);
      if(audit != null && hasNoSections(audit))
      {
         audit.getField("auditTableId").withLabel("Table");
         audit.getField("auditUserId").withLabel("User");
         audit.withSection(new QFieldSection("audit", "Audit", new QIcon("history"), Tier.T1,
            audit.getFields().containsKey(FIELD_CLIENT_CHANNEL)
               ? List.of("id", "timestamp", "auditUserId", FIELD_CLIENT_CHANNEL, "auditTableId", "recordId", "message")
               : List.of("id", "timestamp", "auditUserId", "auditTableId", "recordId", "message")));

         QJoinMetaData join = instance.getJoin(QJoinMetaData.makeInferredJoinName(TABLE_NAME_AUDIT, TABLE_NAME_AUDIT_DETAIL));
         if(join != null && instance.getTable(TABLE_NAME_AUDIT_DETAIL) != null)
         {
            instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(join)
               .withLabel("Field Changes")
               .withMaxRows(100)
               .withCanAddChildRecord(false)
               .withOnlyIncludeFieldNames(List.of("fieldName", "oldValue", "newValue", "message"))
               .getWidgetMetaData());
            audit.withSection(new QFieldSection(FIELD_CHANGES_SECTION_NAME, new QIcon("difference"), Tier.T2)
               .withLabel("Field Changes")
               .withWidgetName(join.getName()));
         }
      }

      QTableMetaData detail = instance.getTable(TABLE_NAME_AUDIT_DETAIL);
      if(detail != null && hasNoSections(detail))
      {
         detail.withRecordLabelFields("fieldName");
         detail.getField("auditId").withLabel("Audit");
         detail.withSection(new QFieldSection("change", "Field Change", new QIcon("difference"), Tier.T1,
            List.of("id", "auditId", "fieldName", "oldValue", "newValue", "message")));
      }

      QTableMetaData auditTable = instance.getTable(TABLE_NAME_AUDIT_TABLE);
      if(auditTable != null && hasNoSections(auditTable))
      {
         auditTable.withSection(new QFieldSection("auditTable", "Audited Table", new QIcon("table_chart"), Tier.T1, List.of("id", "label", "name")));
         auditTable.withSection(new QFieldSection("dates", "Dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));
      }

      QTableMetaData auditUser = instance.getTable(TABLE_NAME_AUDIT_USER);
      if(auditUser != null && hasNoSections(auditUser))
      {
         auditUser.withSection(new QFieldSection("auditUser", "Audited User", new QIcon("person"), Tier.T1, List.of("id", "name")));
         auditUser.withSection(new QFieldSection("dates", "Dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));
      }
   }



   /*******************************************************************************
    ** Whether a table has no sections yet.
    *******************************************************************************/
   private static boolean hasNoSections(QTableMetaData table)
   {
      return (table.getSections() == null || table.getSections().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardAuditTables(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      for(QTableMetaData tableMetaData : defineStandardAuditTables(backendName, backendDetailEnricher))
      {
         instance.addTable(tableMetaData);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardAuditPossibleValueSources(QInstance instance)
   {
      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(TABLE_NAME_AUDIT_TABLE)
         .withTableName(TABLE_NAME_AUDIT_TABLE)
         .withOrderByField("name")
      );

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(TABLE_NAME_AUDIT_USER)
         .withTableName(TABLE_NAME_AUDIT_USER)
         .withOrderByField("name")
      );

      instance.addPossibleValueSource(new QPossibleValueSource()
         .withName(TABLE_NAME_AUDIT)
         .withTableName(TABLE_NAME_AUDIT)
         .withOrderByField("id", false)
      );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QTableMetaData> defineStandardAuditTables(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      List<QTableMetaData> rs = new ArrayList<>();
      rs.add(enrich(backendDetailEnricher, defineAuditUserTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineAuditTableTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineAuditTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineAuditDetailTable(backendName)));
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
   private QTableMetaData defineAuditTableTable(String backendName)
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_AUDIT_TABLE)
         .withBackendName(backendName)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("label")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("name"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("label", QFieldType.STRING))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineAuditUserTable(String backendName)
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_AUDIT_USER)
         .withBackendName(backendName)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("name")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("name"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineAuditTable(String backendName)
   {
      QTableMetaData table = new QTableMetaData()
         .withName(TABLE_NAME_AUDIT)
         .withBackendName(backendName)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("auditTableId", "recordId")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.LONG))
         .withField(new QFieldMetaData("auditTableId", QFieldType.INTEGER).withPossibleValueSourceName(TABLE_NAME_AUDIT_TABLE))
         .withField(new QFieldMetaData("auditUserId", QFieldType.INTEGER).withPossibleValueSourceName(TABLE_NAME_AUDIT_USER))
         .withField(new QFieldMetaData("recordId", recordIdType))
         .withField(new QFieldMetaData("message", QFieldType.STRING).withMaxLength(250).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS))
         .withField(new QFieldMetaData("timestamp", QFieldType.DATE_TIME))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);

      if(includeClientChannelField)
      {
         table.withField(new QFieldMetaData(FIELD_CLIENT_CHANNEL, QFieldType.STRING).withLabel("Client").withMaxLength(100).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS));
      }
      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineAuditDetailTable(String backendName)
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_AUDIT_DETAIL)
         .withBackendName(backendName)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("id")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.LONG))
         .withField(new QFieldMetaData("auditId", QFieldType.LONG).withPossibleValueSourceName(TABLE_NAME_AUDIT))
         .withField(new QFieldMetaData("message", QFieldType.STRING).withMaxLength(250).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS))
         .withField(new QFieldMetaData("fieldName", QFieldType.STRING).withMaxLength(100).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS))
         .withField(new QFieldMetaData("oldValue", QFieldType.STRING).withMaxLength(250).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS))
         .withField(new QFieldMetaData("newValue", QFieldType.STRING).withMaxLength(250).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
   }



   /*******************************************************************************
    ** Getter for recordIdType
    *******************************************************************************/
   public QFieldType getRecordIdType()
   {
      return (this.recordIdType);
   }



   /*******************************************************************************
    ** Setter for recordIdType
    *******************************************************************************/
   public void setRecordIdType(QFieldType recordIdType)
   {
      this.recordIdType = recordIdType;
   }



   /*******************************************************************************
    ** Fluent setter for recordIdType
    *******************************************************************************/
   public AuditsMetaDataProvider withRecordIdType(QFieldType recordIdType)
   {
      this.recordIdType = recordIdType;
      return (this);
   }


   /*******************************************************************************
    ** Fluent setter for includeClientChannelField: whether the audit table has
    ** the clientChannel field (which client made each change).  The backing
    ** table needs the column first.
    *******************************************************************************/
   public AuditsMetaDataProvider withClientChannelField(boolean includeClientChannelField)
   {
      this.includeClientChannelField = includeClientChannelField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeClientChannelField
    *******************************************************************************/
   public boolean getIncludeClientChannelField()
   {
      return (this.includeClientChannelField);
   }



   /*******************************************************************************
    ** Getter for defineRecordScreens
    *******************************************************************************/
   public boolean getDefineRecordScreens()
   {
      return (this.defineRecordScreens);
   }



   /*******************************************************************************
    ** Setter for defineRecordScreens
    *******************************************************************************/
   public void setDefineRecordScreens(boolean defineRecordScreens)
   {
      this.defineRecordScreens = defineRecordScreens;
   }



   /*******************************************************************************
    ** Fluent setter for defineRecordScreens: whether defineAll also defines the
    ** audit record screens (sections and the Field Changes child list).
    *******************************************************************************/
   public AuditsMetaDataProvider withDefineRecordScreens(boolean defineRecordScreens)
   {
      this.defineRecordScreens = defineRecordScreens;
      return (this);
   }

}
