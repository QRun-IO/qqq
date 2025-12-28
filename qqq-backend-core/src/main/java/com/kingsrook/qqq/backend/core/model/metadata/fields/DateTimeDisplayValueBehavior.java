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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Field Display Behavior class for customizing the display values used
 ** in date-time fields
 *******************************************************************************/
public class DateTimeDisplayValueBehavior implements FieldDisplayBehavior<DateTimeDisplayValueBehavior>
{
   private static final QLogger LOG = QLogger.getLogger(DateTimeDisplayValueBehavior.class);

   private String zoneIdFromFieldName;
   private String fallbackZoneId;

   private String defaultZoneId;

   private static DateTimeDisplayValueBehavior NOOP = new DateTimeDisplayValueBehavior();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public DateTimeDisplayValueBehavior getDefault()
   {
      return NOOP;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      if(StringUtils.hasContent(defaultZoneId))
      {
         applyDefaultZoneId(recordList, table, field);
      }
      else if(StringUtils.hasContent(zoneIdFromFieldName))
      {
         applyZoneIdFromFieldName(recordList, table, field);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void applyDefaultZoneId(List<QRecord> recordList, QTableMetaData table, QFieldMetaData field)
   {
      for(QRecord record : CollectionUtils.nonNullList(recordList))
      {
         try
         {
            Instant instant = record.getValueInstant(field.getName());

            if(instant == null)
            {
               continue;
            }

            ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(defaultZoneId));
            record.setDisplayValue(field.getName(), QValueFormatter.formatDateTimeWithZone(zonedDateTime));
         }
         catch(Exception e)
         {
            LOG.info("Error applying defaultZoneId DateTimeDisplayValueBehavior", logPair("table", table.getName()), logPair("field", field.getName()), logPair("id", record.getValue(table.getPrimaryKeyField())));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void applyZoneIdFromFieldName(List<QRecord> recordList, QTableMetaData table, QFieldMetaData field)
   {
      for(QRecord record : CollectionUtils.nonNullList(recordList))
      {
         try
         {
            Instant instant = record.getValueInstant(field.getName());
            if(instant == null)
            {
               continue;
            }

            String zoneString = record.getValueString(zoneIdFromFieldName);

            ZoneId zoneId = null;
            if(StringUtils.hasContent(zoneString))
            {
               try
               {
                  zoneId = ZoneId.of(zoneString);
               }
               catch(Exception e)
               {
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // we probably(?) don't need a stack trace here (and it could get noisy?), so just info w/ the exception message... //
                  // and we expect this might be somewhat frequent, if you might have invalid values in your zoneId field...          //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  LOG.info("Exception applying zoneIdFromFieldName behavior", logPair("message", e.getMessage()), logPair("table", table.getName()), logPair("field", field.getName()), logPair("id", record.getValue(table.getPrimaryKeyField())));
               }
            }

            if(zoneId == null)
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////
               // if the zone string from the other field isn't valid, and we have a fallback, try to use it //
               ////////////////////////////////////////////////////////////////////////////////////////////////
               if(StringUtils.hasContent(fallbackZoneId))
               {
                  ////////////////////////////////////////////////////////////////////////////////////////////
                  // assume that validation has confirmed this is a valid zone - so no try-catch right here //
                  ////////////////////////////////////////////////////////////////////////////////////////////
                  zoneId = ZoneId.of(fallbackZoneId);
               }
            }

            if(zoneId != null)
            {
               ZonedDateTime zonedDateTime = instant.atZone(zoneId);
               record.setDisplayValue(field.getName(), QValueFormatter.formatDateTimeWithZone(zonedDateTime));
            }
         }
         catch(Exception e)
         {
            ///////////////////////////////////////////////////////////////////////
            // we don't expect this to ever hit - so warn it w/ stack if it does //
            ///////////////////////////////////////////////////////////////////////
            LOG.warn("Unexpected error applying zoneIdFromFieldName behavior", e, logPair("table", table.getName()), logPair("field", field.getName()), logPair("id", record.getValue(table.getPrimaryKeyField())));
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

      if(!QFieldType.DATE_TIME.equals(fieldMetaData.getType()))
      {
         errors.add("A DateTimeDisplayValueBehavior was a applied to a non-DATE_TIME" + errorSuffix);
      }

      //////////////////////////////////////////////////
      // validate rules if zoneIdFromFieldName is set //
      //////////////////////////////////////////////////
      if(StringUtils.hasContent(zoneIdFromFieldName))
      {
         if(StringUtils.hasContent(defaultZoneId))
         {
            errors.add("You may not specify both zoneIdFromFieldName and defaultZoneId in DateTimeDisplayValueBehavior on" + errorSuffix);
         }

         if(tableMetaData == null || !tableMetaData.getFields().containsKey(zoneIdFromFieldName))
         {
            errors.add("Unrecognized field name [" + zoneIdFromFieldName + "] for [zoneIdFromFieldName] in DateTimeDisplayValueBehavior on" + errorSuffix);
         }
         else
         {
            QFieldMetaData zoneIdField = tableMetaData.getFields().get(zoneIdFromFieldName);
            if(!QFieldType.STRING.equals(zoneIdField.getType()))
            {
               errors.add("A non-STRING type [" + zoneIdField.getType() + "] was specified as the zoneIdFromFieldName field [" + zoneIdFromFieldName + "] in DateTimeDisplayValueBehavior on" + errorSuffix);
            }
         }
      }

      ////////////////////////////////////////////
      // validate rules if defaultZoneId is set //
      ////////////////////////////////////////////
      if(StringUtils.hasContent(defaultZoneId))
      {
         /////////////////////////////////////////////////////////////////////////////////////////////
         // would check that you didn't specify from zoneIdFromFieldName - but that's covered above //
         /////////////////////////////////////////////////////////////////////////////////////////////

         if(StringUtils.hasContent(fallbackZoneId))
         {
            errors.add("You may not specify both defaultZoneId and fallbackZoneId in DateTimeDisplayValueBehavior on" + errorSuffix);
         }

         try
         {
            ZoneId.of(defaultZoneId);
         }
         catch(Exception e)
         {
            errors.add("Invalid ZoneId [" + defaultZoneId + "] for [defaultZoneId] in DateTimeDisplayValueBehavior on" + errorSuffix + "; " + e.getMessage());
         }
      }

      /////////////////////////////////////////////
      // validate rules if fallbackZoneId is set //
      /////////////////////////////////////////////
      if(StringUtils.hasContent(fallbackZoneId))
      {
         if(!StringUtils.hasContent(zoneIdFromFieldName))
         {
            errors.add("You may only set fallbackZoneId if using zoneIdFromFieldName in DateTimeDisplayValueBehavior on" + errorSuffix);
         }

         try
         {
            ZoneId.of(fallbackZoneId);
         }
         catch(Exception e)
         {
            errors.add("Invalid ZoneId [" + fallbackZoneId + "] for [fallbackZoneId] in DateTimeDisplayValueBehavior on" + errorSuffix + "; " + e.getMessage());
         }
      }

      return (errors);
   }



   /*******************************************************************************
    ** Getter for zoneIdFromFieldName
    *******************************************************************************/
   public String getZoneIdFromFieldName()
   {
      return (this.zoneIdFromFieldName);
   }



   /*******************************************************************************
    ** Setter for zoneIdFromFieldName
    *******************************************************************************/
   public void setZoneIdFromFieldName(String zoneIdFromFieldName)
   {
      this.zoneIdFromFieldName = zoneIdFromFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for zoneIdFromFieldName
    *******************************************************************************/
   public DateTimeDisplayValueBehavior withZoneIdFromFieldName(String zoneIdFromFieldName)
   {
      this.zoneIdFromFieldName = zoneIdFromFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultZoneId
    *******************************************************************************/
   public String getDefaultZoneId()
   {
      return (this.defaultZoneId);
   }



   /*******************************************************************************
    ** Setter for defaultZoneId
    *******************************************************************************/
   public void setDefaultZoneId(String defaultZoneId)
   {
      this.defaultZoneId = defaultZoneId;
   }



   /*******************************************************************************
    ** Fluent setter for defaultZoneId
    *******************************************************************************/
   public DateTimeDisplayValueBehavior withDefaultZoneId(String defaultZoneId)
   {
      this.defaultZoneId = defaultZoneId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fallbackZoneId
    *******************************************************************************/
   public String getFallbackZoneId()
   {
      return (this.fallbackZoneId);
   }



   /*******************************************************************************
    ** Setter for fallbackZoneId
    *******************************************************************************/
   public void setFallbackZoneId(String fallbackZoneId)
   {
      this.fallbackZoneId = fallbackZoneId;
   }



   /*******************************************************************************
    ** Fluent setter for fallbackZoneId
    *******************************************************************************/
   public DateTimeDisplayValueBehavior withFallbackZoneId(String fallbackZoneId)
   {
      this.fallbackZoneId = fallbackZoneId;
      return (this);
   }

}
