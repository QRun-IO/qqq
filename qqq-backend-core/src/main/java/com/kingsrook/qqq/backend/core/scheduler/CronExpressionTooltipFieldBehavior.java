/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldDisplayBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Field display behavior, to add a human-redable tooltip to cron-expressions.
 *******************************************************************************/
public class CronExpressionTooltipFieldBehavior implements FieldDisplayBehavior<CronExpressionTooltipFieldBehavior>
{

   /***************************************************************************
    ** Add both this behavior, and the tooltip adornment to a field
    ** Note, if either was already there, then that part is left alone.
    ***************************************************************************/
   public static void addToField(QFieldMetaData fieldMetaData)
   {
      CronExpressionTooltipFieldBehavior existingBehavior = fieldMetaData.getBehaviorOnlyIfSet(CronExpressionTooltipFieldBehavior.class);
      if(existingBehavior == null)
      {
         fieldMetaData.withBehavior(new CronExpressionTooltipFieldBehavior());
      }

      if(fieldMetaData.getAdornment(AdornmentType.TOOLTIP).isEmpty())
      {
         fieldMetaData.withFieldAdornment((new FieldAdornment(AdornmentType.TOOLTIP)
            .withValue(AdornmentType.TooltipValues.TOOLTIP_DYNAMIC, true)));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      for(QRecord record : recordList)
      {
         try
         {
            String cronExpression = record.getValueString(field.getName());
            if(StringUtils.hasContent(cronExpression))
            {
               String description = CronDescriber.getDescription(cronExpression);
               record.setDisplayValue(field.getName() + ":" + AdornmentType.TooltipValues.TOOLTIP_DYNAMIC, description);
            }
         }
         catch(Exception e)
         {
            /////////////////////
            // just leave null //
            /////////////////////
         }
      }
   }

}
