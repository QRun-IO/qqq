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

package com.kingsrook.qqq.backend.core.scheduler.quartz.tables;


import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostQueryCustomizer;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.apache.commons.lang3.SerializationUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class QuartzJobDataPostQueryCustomizer extends AbstractPostQueryCustomizer
{
   private static final QLogger LOG = QLogger.getLogger(QuartzJobDataPostQueryCustomizer.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records)
   {
      for(QRecord record : records)
      {
         if(record.getValue("jobData") != null)
         {
            try
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////////////
               // this field has a blob of essentially a serialized map - so, deserialize that, then convert to JSON //
               ////////////////////////////////////////////////////////////////////////////////////////////////////////
               byte[] value       = record.getValueByteArray("jobData");
               if(value.length > 0)
               {
                  Object deserialize = SerializationUtils.deserialize(value);
                  String json        = JsonUtils.toJson(deserialize);
                  record.setValue("jobData", json);
               }
            }
            catch(Exception e)
            {
               LOG.info("Error deserializing quartz job data", e);
            }
         }

         formatEpochTime(record, "nextFireTime");
         formatEpochTime(record, "prevFireTime");
         formatEpochTime(record, "startTime");
      }
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void formatEpochTime(QRecord record, String fieldName)
   {
      Long value = record.getValueLong(fieldName);

      try
      {
         if(value != null && value > 0)
         {
            Instant instant = Instant.ofEpochMilli(value);
            record.setDisplayValue(fieldName, String.format("%,d", value) + " (" + QValueFormatter.formatDateTimeWithZone(instant.atZone(ZoneId.of(QContext.getQInstance().getDefaultTimeZoneId()))) + ")");
         }
      }
      catch(Exception e)
      {
         LOG.info("Error formatting an epoc time value", e, logPair("fieldName", fieldName), logPair("value", value));
      }
   }

}
