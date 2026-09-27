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

package com.kingsrook.qqq.backend.core.model.querystats;


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;


/*******************************************************************************
 **
 *******************************************************************************/
public class QueryStatMetaDataProvider
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      addJoins(instance);

      defineQueryStatTable(instance, backendName, backendDetailEnricher);

      instance.addTable(defineStandardTable(QueryStatJoinTable.TABLE_NAME, QueryStatJoinTable.class, backendName, backendDetailEnricher));

      instance.addTable(defineStandardTable(QueryStatCriteriaField.TABLE_NAME, QueryStatCriteriaField.class, backendName, backendDetailEnricher)
         .withIcon(new QIcon().withName("filter_alt"))
         .withExposedJoin(new ExposedJoin().withJoinTable(QueryStat.TABLE_NAME))
      );

      instance.addTable(defineStandardTable(QueryStatOrderByField.TABLE_NAME, QueryStatOrderByField.class, backendName, backendDetailEnricher));

      instance.addPossibleValueSource(defineQueryStatPossibleValueSource());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addJoins(QInstance instance)
   {
      instance.addJoin(new QJoinMetaData()
         .withLeftTable(QueryStat.TABLE_NAME)
         .withRightTable(QueryStatJoinTable.TABLE_NAME)
         .withInferredName()
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "queryStatId")));

      instance.addJoin(new QJoinMetaData()
         .withLeftTable(QueryStat.TABLE_NAME)
         .withRightTable(QueryStatCriteriaField.TABLE_NAME)
         .withInferredName()
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "queryStatId")));

      instance.addJoin(new QJoinMetaData()
         .withLeftTable(QueryStat.TABLE_NAME)
         .withRightTable(QueryStatOrderByField.TABLE_NAME)
         .withInferredName()
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "queryStatId")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineQueryStatTable(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      String joinTablesJoinName     = QJoinMetaData.makeInferredJoinName(QueryStat.TABLE_NAME, QueryStatJoinTable.TABLE_NAME);
      String criteriaFieldsJoinName = QJoinMetaData.makeInferredJoinName(QueryStat.TABLE_NAME, QueryStatCriteriaField.TABLE_NAME);
      String orderByFieldsJoinName  = QJoinMetaData.makeInferredJoinName(QueryStat.TABLE_NAME, QueryStatOrderByField.TABLE_NAME);

      QTableMetaData table = new QTableMetaData()
         .withName(QueryStat.TABLE_NAME)
         .withIcon(new QIcon().withName("query_stats"))
         .withBackendName(backendName)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("id")
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(QueryStat.class)
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "action", "qqqTableId", "sessionId")))
         .withSection(new QFieldSection("data", new QIcon().withName("dataset"), Tier.T2, List.of("queryText", "startTimestamp", "firstResultTimestamp", "firstResultMillis")))
         .withSection(new QFieldSection("joins", new QIcon().withName("merge"), Tier.T2).withWidgetName(joinTablesJoinName + "Widget"))
         .withSection(new QFieldSection("criteria", new QIcon().withName("filter_alt"), Tier.T2).withWidgetName(criteriaFieldsJoinName + "Widget"))
         .withSection(new QFieldSection("orderBys", new QIcon().withName("sort_by_alpha"), Tier.T2).withWidgetName(orderByFieldsJoinName + "Widget"))
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(instance.getJoin(joinTablesJoinName)).withName(joinTablesJoinName + "Widget").withLabel("Join Tables").getWidgetMetaData());
      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(instance.getJoin(criteriaFieldsJoinName)).withName(criteriaFieldsJoinName + "Widget").withLabel("Criteria Fields").getWidgetMetaData());
      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(instance.getJoin(orderByFieldsJoinName)).withName(orderByFieldsJoinName + "Widget").withLabel("Order by Fields").getWidgetMetaData());

      table.withExposedJoin(new ExposedJoin().withJoinTable(QueryStatCriteriaField.TABLE_NAME));
      table.withExposedJoin(new ExposedJoin().withJoinTable(QueryStatJoinTable.TABLE_NAME));
      table.withExposedJoin(new ExposedJoin().withJoinTable(QueryStatOrderByField.TABLE_NAME));

      table.withAssociation(new Association().withName("queryStatJoinTables").withJoinName(joinTablesJoinName).withAssociatedTableName(QueryStatJoinTable.TABLE_NAME))
         .withAssociation(new Association().withName("queryStatCriteriaFields").withJoinName(criteriaFieldsJoinName).withAssociatedTableName(QueryStatCriteriaField.TABLE_NAME))
         .withAssociation(new Association().withName("queryStatOrderByFields").withJoinName(orderByFieldsJoinName).withAssociatedTableName(QueryStatOrderByField.TABLE_NAME));

      table.getField("queryText").withFieldAdornment(new FieldAdornment(AdornmentType.CODE_EDITOR).withValue(AdornmentType.CodeEditorValues.languageMode("sql")));
      table.getField("firstResultMillis").withDisplayFormat(DisplayFormat.COMMAS);

      instance.addTable(table);
      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineStandardTable(String tableName, Class<? extends QRecordEntity> entityClass, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(tableName)
         .withBackendName(backendName)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withRecordLabelFormat("%d")
         .withRecordLabelFields("id")
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(entityClass)
         .withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource defineQueryStatPossibleValueSource()
   {
      return (new QPossibleValueSource()
         .withType(QPossibleValueSourceType.TABLE)
         .withName(QueryStat.TABLE_NAME)
         .withTableName(QueryStat.TABLE_NAME))
         .withOrderByField("id", false);
   }

}
