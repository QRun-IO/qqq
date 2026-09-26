/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.scheduler.CronDescriber;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Field Display Behavior class for customizing the display values used
 ** in cron expression fields
 *******************************************************************************/
public class CronExpressionDisplayValueBehavior implements FieldDisplayBehavior<CronExpressionDisplayValueBehavior>
{
   private static final QLogger LOG = QLogger.getLogger(CronExpressionDisplayValueBehavior.class);

   private static CronExpressionDisplayValueBehavior NOOP = new CronExpressionDisplayValueBehavior();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public CronExpressionDisplayValueBehavior getDefault()
   {
      return NOOP;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      for(QRecord record : CollectionUtils.nonNullList(recordList))
      {
         try
         {
            String cronExpression = record.getValueString(field.getName());
            if(!StringUtils.hasContent(cronExpression))
            {
               continue;
            }

            record.setDisplayValue(field.getName(), cronExpression + " (" + CronDescriber.getDescription(cronExpression) + ")");
         }
         catch(Exception e)
         {
            LOG.info("Error applying cronExpression display value behavior", logPair("table", table.getName()), logPair("field", field.getName()), logPair("id", record.getValue(table.getPrimaryKeyField())));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<String> validateBehaviorConfiguration(QTableMetaData tableMetaData, QFieldMetaData fieldMetaData)
   {
      List<String> errors      = new ArrayList<>();
      String       errorSuffix = " field [" + fieldMetaData.getName() + "]";
      if(tableMetaData != null)
      {
         errorSuffix += " in table [" + tableMetaData.getName() + "]";
      }

      if(!QFieldType.STRING.equals(fieldMetaData.getType()))
      {
         errors.add("A CronExpressionDisplayValueBehavior was a applied to a non-STRING" + errorSuffix);
      }

      return (errors);
   }

}
