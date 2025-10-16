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

package com.kingsrook.qqq.backend.core.model.scheduledjobs;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.CronUIWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUISetupData;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.customizers.ScheduledJobParameterTableCustomizer;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.customizers.ScheduledJobTableCustomizer;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScheduledJobsMetaDataProvider
{
   private static final String JOB_PARAMETER_JOIN_NAME = QJoinMetaData.makeInferredJoinName(ScheduledJob.TABLE_NAME, ScheduledJobParameter.TABLE_NAME);

   private static final String CRON_WIDGET_NAME = ScheduledJob.TABLE_NAME + "CronUI";


   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      defineStandardTables(instance, backendName, backendDetailEnricher);
      instance.addPossibleValueSource(QPossibleValueSource.newForTable(ScheduledJob.TABLE_NAME));
      instance.addPossibleValueSource(defineScheduledJobTypePossibleValueSource());
      instance.addPossibleValueSource(defineSchedulersPossibleValueSource());
      defineStandardJoins(instance);
      defineStandardWidgets(instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardWidgets(QInstance instance)
   {
      QJoinMetaData join = instance.getJoin(JOB_PARAMETER_JOIN_NAME);
      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(join)
         .withCanAddChildRecord(true)
         .withManageAssociationName(ScheduledJobParameter.TABLE_NAME)
         .withLabel("Parameters")
         .getWidgetMetaData()
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)));

      CronUISetupData cronUISetupData = new CronUISetupData(ScheduledJob.TABLE_NAME, "cronExpression", "cronTimeZoneId");
      instance.addWidget(CronUIWidgetRenderer.buildWidgetMetaData(CRON_WIDGET_NAME, "Schedule", cronUISetupData));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardJoins(QInstance instance)
   {
      instance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(ScheduledJob.TABLE_NAME)
         .withRightTable(ScheduledJobParameter.TABLE_NAME)
         .withJoinOn(new JoinOn("id", "scheduledJobId"))
         .withOrderBy(new QFilterOrderBy("id"))
         .withInferredName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineStandardTables(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      for(QTableMetaData tableMetaData : defineStandardTables(backendName, backendDetailEnricher))
      {
         instance.addTable(tableMetaData);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QTableMetaData> defineStandardTables(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      List<QTableMetaData> rs = new ArrayList<>();
      rs.add(enrich(backendDetailEnricher, defineScheduledJobTable(backendName)));
      rs.add(enrich(backendDetailEnricher, defineScheduledJobParameterTable(backendName)));
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
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(fieldsFromEntity);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScheduledJobTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, ScheduledJob.TABLE_NAME, ScheduledJob.class)
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("label")
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "label", "description")))
         .withSection(new QFieldSection("repeat", new QIcon().withName("alarm"), Tier.T2, List.of("repeatSeconds")))
         .withSection(new QFieldSection("schedule", new QIcon().withName("schedule"), Tier.T2).withWidgetName(CRON_WIDGET_NAME))
         .withSection(new QFieldSection("hidden", new QIcon().withName("visibility_off"), Tier.T2, List.of("cronExpression", "cronDescription", "cronTimeZoneId")).withIsHidden(true))
         .withSection(new QFieldSection("settings", new QIcon().withName("tune"), Tier.T2, List.of("type", "isActive", "schedulerName", "foreignKeyType", "foreignKeyValue")))
         .withSection(new QFieldSection("parameters", new QIcon().withName("list"), Tier.T2).withWidgetName(JOB_PARAMETER_JOIN_NAME))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      QCodeReference customizerReference = new QCodeReference(ScheduledJobTableCustomizer.class);
      tableMetaData.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, customizerReference);
      tableMetaData.withCustomizer(TableCustomizers.POST_INSERT_RECORD, customizerReference);
      tableMetaData.withCustomizer(TableCustomizers.POST_UPDATE_RECORD, customizerReference);
      tableMetaData.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, customizerReference);
      tableMetaData.withCustomizer(TableCustomizers.POST_DELETE_RECORD, customizerReference);

      tableMetaData.withAssociation(new Association()
         .withName(ScheduledJobParameter.TABLE_NAME)
         .withAssociatedTableName(ScheduledJobParameter.TABLE_NAME)
         .withJoinName(JOB_PARAMETER_JOIN_NAME));

      tableMetaData.withExposedJoin(new ExposedJoin()
         .withJoinTable(ScheduledJobParameter.TABLE_NAME)
         .withJoinPath(List.of(JOB_PARAMETER_JOIN_NAME))
         .withLabel("Parameters"));

      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineScheduledJobParameterTable(String backendName) throws QException
   {
      QTableMetaData tableMetaData = defineStandardTable(backendName, ScheduledJobParameter.TABLE_NAME, ScheduledJobParameter.class)
         .withRecordLabelFormat("%s - %s")
         .withRecordLabelFields("scheduledJobId", "key")
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "scheduledJobId", "key", "value")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));


      QCodeReference customizerReference = new QCodeReference(ScheduledJobParameterTableCustomizer.class);
      tableMetaData.withCustomizer(TableCustomizers.POST_INSERT_RECORD, customizerReference);
      tableMetaData.withCustomizer(TableCustomizers.POST_UPDATE_RECORD, customizerReference);
      tableMetaData.withCustomizer(TableCustomizers.POST_DELETE_RECORD, customizerReference);

      tableMetaData.withExposedJoin(new ExposedJoin()
         .withJoinTable(ScheduledJob.TABLE_NAME)
         .withJoinPath(List.of(JOB_PARAMETER_JOIN_NAME))
         .withLabel("Scheduled Job"));

      return (tableMetaData);
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   private QPossibleValueSource defineScheduledJobTypePossibleValueSource()
   {
      return (new QPossibleValueSource()
         .withName(ScheduledJobTypePossibleValueSource.NAME)
         .withType(QPossibleValueSourceType.CUSTOM)
         .withCustomCodeReference(new QCodeReference(ScheduledJobTypePossibleValueSource.class)));
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   private QPossibleValueSource defineSchedulersPossibleValueSource()
   {
      return (new QPossibleValueSource()
         .withName(SchedulersPossibleValueSource.NAME)
         .withType(QPossibleValueSourceType.CUSTOM)
         .withCustomCodeReference(new QCodeReference(SchedulersPossibleValueSource.class)));
   }

}
