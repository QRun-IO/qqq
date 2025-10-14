/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.QInstanceValidatorPluginInterface;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUISetupData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUIWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.scheduler.CronDescriber;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.quartz.CronScheduleBuilder;


/*******************************************************************************
 ** Widget for editing cron expression and timeZone (if used) fields
 *******************************************************************************/
public class CronUIWidgetRenderer extends AbstractWidgetRenderer
{
   private static final QLogger LOG = QLogger.getLogger(CronUIWidgetRenderer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QWidgetMetaData buildWidgetMetaData(String name, String label, CronUISetupData setupData)
   {
      return (new QWidgetMetaData()
         .withName(name)
         .withIsCard(true)
         .withCodeReference(new QCodeReference(CronUIWidgetRenderer.class))
         .withType(WidgetType.CRON_UI.getType())
         .withLabel(label)
         .withDefaultValue("tableName", setupData.getTableName())
         .withDefaultValue("cronExpressionFieldName", setupData.getCronExpressionFieldName())
         .withDefaultValue("timeZoneFieldName", setupData.getTimeZoneFieldName())
         .withDefaultValue("includeOnRecordEditScreen", true)
         .withValidatorPlugin(new CronUIWidgetValidator())
      );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      Map<String, Serializable> defaultValues           = input.getWidgetMetaData().getDefaultValues();
      String                    tableName               = ValueUtils.getValueAsString(defaultValues.get("tableName"));
      String                    cronExpressionFieldName = ValueUtils.getValueAsString(defaultValues.get("cronExpressionFieldName"));

      String cronDescription = "No description available";
      String cronExpression  = null;
      String error           = null;

      String recordId = input.getQueryParams().get("id");
      if(StringUtils.hasContent(recordId))
      {
         QRecord record = GetAction.execute(tableName, recordId);
         cronExpression = record.getValueString(cronExpressionFieldName);
      }
      else
      {
         cronExpression = input.getQueryParams().get("cronExpression");
      }

      if(StringUtils.hasContent(cronExpression))
      {
         try
         {
            cronDescription = CronDescriber.getDescription(cronExpression);

            /////////////////////////////////////
            // also make sure quartz passes it //
            /////////////////////////////////////
            CronScheduleBuilder.cronScheduleNonvalidatedExpression(cronExpression);
         }
         catch(Exception e)
         {
            LOG.warn("Error building cron description", e);
            error = e.getMessage();
         }
      }



      return new RenderWidgetOutput(new CronUIWidgetData()
         .withCronDescription(cronDescription)
         .withError(error)
         .withLabel(input.getWidgetMetaData().getLabel()));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static class CronUIWidgetValidator implements QInstanceValidatorPluginInterface<QWidgetMetaDataInterface>
   {

      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public void validate(QWidgetMetaDataInterface widgetMetaData, QInstance qInstance, QInstanceValidator qInstanceValidator)
      {
         String tableName               = ValueUtils.getValueAsString(widgetMetaData.getDefaultValues().get("tableName"));
         String cronExpressionFieldName = ValueUtils.getValueAsString(widgetMetaData.getDefaultValues().get("cronExpressionFieldName"));
         String timeZoneFieldName       = ValueUtils.getValueAsString(widgetMetaData.getDefaultValues().get("timeZoneFieldName"));

         qInstanceValidator.assertCondition(StringUtils.hasContent(cronExpressionFieldName), "cronExpressionFieldName must be given");

         if(qInstanceValidator.assertCondition(StringUtils.hasContent(tableName), "tableName must be given"))
         {
            QTableMetaData table = qInstance.getTable(tableName);
            if(qInstanceValidator.assertCondition(table != null, "Unrecognized table name: " + tableName))
            {
               if(StringUtils.hasContent(cronExpressionFieldName))
               {
                  QFieldMetaData cronExpressionField = table.getFields().get(cronExpressionFieldName);
                  if(qInstanceValidator.assertCondition(cronExpressionField != null, "Unrecognized cronExpressionFieldName: " + cronExpressionFieldName))
                  {
                     qInstanceValidator.assertCondition(cronExpressionField.getType().equals(QFieldType.STRING), "CronExpressionField must be of type: STRING");
                  }
               }

               if(StringUtils.hasContent(timeZoneFieldName))
               {
                  QFieldMetaData timeZoneField = table.getFields().get(timeZoneFieldName);
                  if(qInstanceValidator.assertCondition(timeZoneField != null, "Unrecognized timeZoneFieldName: " + timeZoneFieldName))
                  {
                     qInstanceValidator.assertCondition(timeZoneField.getType().equals(QFieldType.STRING), "TimeZoneField must be of type: STRING");
                  }
               }
            }
         }
      }
   }
}
