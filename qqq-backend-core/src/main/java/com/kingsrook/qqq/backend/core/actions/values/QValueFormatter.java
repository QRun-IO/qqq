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

package com.kingsrook.qqq.backend.core.actions.values;


import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility to apply display formats to values for records and fields.
 **
 *******************************************************************************/
public class QValueFormatter
{
   private static final QLogger LOG = QLogger.getLogger(QValueFormatter.class);

   private static DateTimeFormatter dateTimeFormatter         = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
   private static DateTimeFormatter dateTimeWithZoneFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a z");
   private static DateTimeFormatter dateFormatter             = DateTimeFormatter.ofPattern("yyyy-MM-dd");
   private static DateTimeFormatter localTimeFormatter        = DateTimeFormatter.ofPattern("h:mm:ss a");



   /*******************************************************************************
    ** For a field, and its value, apply the field's displayFormat.
    *******************************************************************************/
   public static String formatValue(QFieldMetaData field, Serializable value)
   {
      return (formatValue(field.getDisplayFormat(), field.getType(), field.getName(), value));
   }



   /*******************************************************************************
    ** For a display format string (e.g., %d), and a value, apply the displayFormat.
    *******************************************************************************/
   public static String formatValue(String displayFormat, Serializable value)
   {
      return (formatValue(displayFormat, null, "", value));
   }



   /*******************************************************************************
    ** For a display format string, an optional fieldName (only used for logging),
    ** and a value, apply the format.
    *******************************************************************************/
   private static String formatValue(String displayFormat, QFieldType fieldType, String fieldName, Serializable value)
   {
      //////////////////////////////////
      // null values get null results //
      //////////////////////////////////
      if(value == null)
      {
         return (null);
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // try to apply some type-specific defaults, if we were requested to just format as a string. //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      if("%s".equals(displayFormat))
      {
         if(value instanceof Boolean b)
         {
            return formatBoolean(b);
         }

         if(QFieldType.BOOLEAN.equals(fieldType))
         {
            return formatBoolean(ValueUtils.getValueAsBoolean(value));
         }

         if(value instanceof LocalTime lt)
         {
            return formatLocalTime(lt);
         }

         //////////////////////////////////////////////////////////////////////////////////////////
         // else, just return the value as a string, rather than going through String.formatted  //
         // this saves some overhead incurred by String.formatted when called millions of times. //
         //////////////////////////////////////////////////////////////////////////////////////////
         return (ValueUtils.getValueAsString(value));
      }

      ////////////////////////////////////////////////////////
      // if the field has a display format, try to apply it //
      ////////////////////////////////////////////////////////
      if(StringUtils.hasContent(displayFormat))
      {
         try
         {
            return (displayFormat.formatted(value));
         }
         catch(Exception e)
         {
            try
            {
               // todo - revisit if we actually want this - or - if you should get an error if you mis-configure your table this way (ideally during validation!)
               if(e.getMessage().equals("f != java.lang.Integer"))
               {
                  return formatValue(displayFormat, ValueUtils.getValueAsBigDecimal(value));
               }
               else if(e.getMessage().equals("f != java.lang.String"))
               {
                  return formatValue(displayFormat, ValueUtils.getValueAsBigDecimal(value));
               }
               else if(e.getMessage().equals("d != java.math.BigDecimal") || e.getMessage().equals("d != java.lang.String"))
               {
                  return formatValue(displayFormat, ValueUtils.getValueAsInteger(value));
               }
               else
               {
                  LOG.warn("Error formatting value [" + value + "] for field [" + fieldName + "] with format [" + displayFormat + "]: " + e.getMessage());
               }
            }
            catch(Exception e2)
            {
               LOG.warn("Caught secondary exception trying to convert type on field [" + fieldName + "] for formatting", e);
            }
         }
      }

      ////////////////////////////////////////
      // by default, just get back a string //
      ////////////////////////////////////////
      return (ValueUtils.getValueAsString(value));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String formatDate(LocalDate date)
   {
      return (dateFormatter.format(date));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String formatDateTime(LocalDateTime dateTime)
   {
      return (dateTimeFormatter.format(dateTime));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String formatDateTimeWithZone(ZonedDateTime dateTime)
   {
      return (dateTimeWithZoneFormatter.format(dateTime));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String formatLocalTime(LocalTime localTime)
   {
      return (localTimeFormatter.format(localTime));
   }



   /*******************************************************************************
    ** Make a string from a table's recordLabelFormat and fields, for a given record.
    *******************************************************************************/
   public static String formatRecordLabel(QTableMetaData table, QRecord record)
   {
      if(!StringUtils.hasContent(table.getRecordLabelFormat()))
      {
         return (formatRecordLabelExceptionalCases(table, record));
      }

      try
      {
         return formatStringWithFields(table.getRecordLabelFormat(), table.getRecordLabelFields(), record.getDisplayValues(), record.getValues());
      }
      catch(Exception e)
      {
         LOG.debug("Error formatting record label", e);
         return (formatRecordLabelExceptionalCases(table, record));
      }
   }



   /*******************************************************************************
    ** For a given format string, and a list of fields, look in displayValueMap and
    ** rawValueMap to get the values to apply to the format.
    *******************************************************************************/
   private static String formatStringWithFields(String formatString, List<String> formatFields, Map<String, String> displayValueMap, Map<String, Serializable> rawValueMap)
   {
      List<Serializable> values = formatFields.stream()
         .map(fieldName ->
         {
            ///////////////////////////////////////////////////////////////////////////
            // if there's a display value set, then use it.  Else, use the raw value //
            ///////////////////////////////////////////////////////////////////////////
            String displayValue = displayValueMap.get(fieldName);
            if(displayValue != null)
            {
               return (displayValue);
            }
            return rawValueMap.get(fieldName);
         })
         .map(v -> v == null ? "" : v)
         .toList();
      return (formatString.formatted(values.toArray()));
   }



   /*******************************************************************************
    ** For a given format string, and a list of values, apply the format.  Note, null
    ** values in the list become "".
    *******************************************************************************/
   public static String formatStringWithValues(String formatString, List<String> formatValues)
   {
      List<String> values = formatValues.stream()
         .map(v -> v == null ? "" : v)
         .toList();
      return (formatString.formatted(values.toArray()));
   }



   /*******************************************************************************
    ** Deal with non-happy-path cases for making a record label.
    *******************************************************************************/
   private static String formatRecordLabelExceptionalCases(QTableMetaData table, QRecord record)
   {
      //////////////////////////////////////////////////////////////////////////////////////
      // if the record already has a label (say, from a query-customizer), then return it //
      //////////////////////////////////////////////////////////////////////////////////////
      if(record.getRecordLabel() != null)
      {
         return (record.getRecordLabel());
      }

      ///////////////////////////////////////////////////////////////////////////////////////
      // if there's no record label format, then just return the primary key display value //
      ///////////////////////////////////////////////////////////////////////////////////////
      String pkeyDisplayValue = record.getDisplayValue(table.getPrimaryKeyField());
      if(StringUtils.hasContent(pkeyDisplayValue))
      {
         return (pkeyDisplayValue);
      }

      String pkeyRawValue = ValueUtils.getValueAsString(record.getValue(table.getPrimaryKeyField()));
      if(StringUtils.hasContent(pkeyRawValue))
      {
         return (pkeyRawValue);
      }

      ///////////////////////////////////////////////////////////////////////////////
      // worst case scenario, return empty string, but never null from this method //
      ///////////////////////////////////////////////////////////////////////////////
      return ("");
   }



   /*******************************************************************************
    ** For a list of records, set their recordLabels and display values - including
    ** record label (e.g., from the table meta data).
    *******************************************************************************/
   public static void setDisplayValuesInRecordsIncludingPossibleValueTranslations(QTableMetaData table, List<QRecord> records)
   {
      QPossibleValueTranslator possibleValueTranslator = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession());
      possibleValueTranslator.translatePossibleValuesInRecords(table, records);
      setDisplayValuesInRecords(table, records);
   }



   /*******************************************************************************
    ** For a list of records, set their recordLabels and display values - including
    ** record label (e.g., from the table meta data).
    *******************************************************************************/
   public static void setDisplayValuesInRecords(QTableMetaData table, List<QRecord> records)
   {
      if(records == null)
      {
         return;
      }

      Map<String, QFieldMetaData> fieldMap = new HashMap<>();

      for(QRecord record : records)
      {
         for(String fieldName : record.getValues().keySet())
         {
            if(!fieldMap.containsKey(fieldName))
            {
               try
               {
                  if(fieldName.contains("."))
                  {
                     String[] nameParts = fieldName.split("\\.", 2);
                     for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
                     {
                        if(exposedJoin.getJoinTable().equals(nameParts[0]))
                        {
                           QTableMetaData joinTable = QContext.getQInstance().getTable(nameParts[0]);
                           if(joinTable.getFields().containsKey(nameParts[1]))
                           {
                              fieldMap.put(fieldName, joinTable.getField(nameParts[1]));
                           }
                        }
                     }
                  }
                  else
                  {
                     if(table.getFields().containsKey(fieldName))
                     {
                        fieldMap.put(fieldName, table.getField(fieldName));
                     }
                  }
               }
               catch(Exception e)
               {
                  LOG.warn("Error getting field for setting display value", e, logPair("fieldName", fieldName), logPair("tableName", table.getName()));
               }
            }

            //////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if we didn't find the field definition, put an empty field in the map, so no formatting will be done //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!fieldMap.containsKey(fieldName))
            {
               fieldMap.put(fieldName, new QFieldMetaData());
            }
         }

         ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.FORMATTING, QContext.getQInstance(), table, records, null);

         setDisplayValuesInRecord(table, fieldMap, record, true);
         record.setRecordLabel(formatRecordLabel(table, record));
      }
   }



   /*******************************************************************************
    ** For a list of records, set their recordLabels and display values
    *******************************************************************************/
   public static void setDisplayValuesInRecords(QTableMetaData table, Map<String, QFieldMetaData> fields, List<QRecord> records)
   {
      if(records == null)
      {
         return;
      }

      if(table != null)
      {
         ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.FORMATTING, QContext.getQInstance(), table, records, null);
      }

      for(QRecord record : records)
      {
         setDisplayValuesInRecord(table, fields, record, true);
      }
   }



   /*******************************************************************************
    ** For a single record, set its display values - public version of this.
    *******************************************************************************/
   public static void setDisplayValuesInRecord(QTableMetaData table, Map<String, QFieldMetaData> fields, QRecord record)
   {
      setDisplayValuesInRecord(table, fields, record, false);
   }



   /*******************************************************************************
    ** For a single record, set its display values - where caller (meant to stay private)
    ** can specify if they've already done fieldBehaviors (to avoid re-doing).
    *******************************************************************************/
   private static void setDisplayValuesInRecord(QTableMetaData table, Map<String, QFieldMetaData> fields, QRecord record, boolean alreadyAppliedFieldDisplayBehaviors)
   {
      if(!alreadyAppliedFieldDisplayBehaviors)
      {
         if(table != null)
         {
            ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.FORMATTING, QContext.getQInstance(), table, List.of(record), null);
         }
      }

      for(Map.Entry<String, QFieldMetaData> entry : fields.entrySet())
      {
         String         fieldName = entry.getKey();
         QFieldMetaData field     = entry.getValue();

         if(record.getDisplayValue(fieldName) == null)
         {
            String formattedValue = formatValue(field, record.getValue(fieldName));
            record.setDisplayValue(fieldName, formattedValue);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String formatBoolean(Boolean b)
   {
      if(b == null)
      {
         return (null);
      }
      else if(b)
      {
         return ("Yes");
      }
      else
      {
         return ("No");
      }
   }



   /*******************************************************************************
    ** For any BLOB type fields in the list of records, change their value to
    ** the URL where they can be downloaded, and set their display value to a file name.
    *******************************************************************************/
   public static void setBlobValuesToDownloadUrls(QTableMetaData table, List<QRecord> records)
   {
      for(QFieldMetaData field : table.getFields().values())
      {
         Optional<FieldAdornment> fileDownloadAdornment = field.getAdornment(AdornmentType.FILE_DOWNLOAD);
         if(fileDownloadAdornment.isPresent())
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // file name comes from:                                                                                            //
            // if there's a FILE_DOWNLOAD adornment, with a FILE_NAME_FIELD value, then the full filename comes from that field //
            // - unless it was empty - then we do the "default thing":                                                          //
            // else - the "default thing" is:                                                                                   //
            // - tableLabel primaryKey fieldLabel                                                                               //
            // - and - if the FILE_DOWNLOAD adornment had a DEFAULT_EXTENSION, then it gets added (preceded by a dot)           //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Map<String, Serializable> adornmentValues = fileDownloadAdornment.get().getValues();

            String fileNameField    = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.FileDownloadValues.FILE_NAME_FIELD));
            String fileNameFormat   = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT));
            String defaultExtension = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.FileDownloadValues.DEFAULT_EXTENSION));

            Boolean downloadUrlDynamic = ValueUtils.getValueAsBoolean(adornmentValues.get(AdornmentType.FileDownloadValues.DOWNLOAD_URL_DYNAMIC));

            for(QRecord record : records)
            {
               if(!doesFieldHaveValue(field, record))
               {
                  continue;
               }

               if(BooleanUtils.isTrue(downloadUrlDynamic))
               {
                  continue;
               }

               Serializable primaryKey = record.getValue(table.getPrimaryKeyField());
               String       fileName   = null;

               //////////////////////////////////////////////////
               // try to make file name from the fileNameField //
               //////////////////////////////////////////////////
               if(StringUtils.hasContent(fileNameField))
               {
                  fileName = record.getValueString(fileNameField);
               }

               if(!StringUtils.hasContent(fileName))
               {
                  if(StringUtils.hasContent(fileNameFormat))
                  {
                     @SuppressWarnings("unchecked") // instance validation should make this safe!
                     List<String> fileNameFormatFields = (List<String>) adornmentValues.get(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT_FIELDS);
                     List<String> values = CollectionUtils.nullSafeHasContents(fileNameFormatFields) ? fileNameFormatFields.stream().map(f -> ValueUtils.getValueAsString(record.getValue(f))).toList() : Collections.emptyList();
                     fileName = QValueFormatter.formatStringWithValues(fileNameFormat, values);
                  }
               }

               if(!StringUtils.hasContent(fileName))
               {
                  //////////////////////////////////
                  // make default name if missing //
                  //////////////////////////////////
                  fileName = table.getLabel() + " " + primaryKey + " " + field.getLabel();

                  if(StringUtils.hasContent(defaultExtension))
                  {
                     //////////////////////////////////////////
                     // add default extension if we have one //
                     //////////////////////////////////////////
                     fileName += "." + defaultExtension;
                  }
               }

               ////////////////////////////////////////////////////////////////////////////////////////////////
               // if field type is blob OR if there's a supplemental process or code-ref that needs to run - //
               // then update its value to be a callback-url that'll give access to the bytes to download.   //
               // implied here is that a String value (w/o supplemental code/proc) has its value stay as a   //
               // URL, which is where the file is directly downloaded from.  And in the case of a String     //
               // with code-to-run, then the code should run, followed by a redirect to the value URL.       //
               ////////////////////////////////////////////////////////////////////////////////////////////////
               if(QFieldType.BLOB.equals(field.getType())
                  || adornmentValues.containsKey(AdornmentType.FileDownloadValues.SUPPLEMENTAL_CODE_REFERENCE)
                  || adornmentValues.containsKey(AdornmentType.FileDownloadValues.SUPPLEMENTAL_PROCESS_NAME))
               {
                  record.setValue(field.getName(), AdornmentType.FileDownloadValues.makeFieldDownloadUrl(table.getName(), primaryKey, field.getName(), fileName));
               }
               record.setDisplayValue(field.getName(), fileName);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean doesFieldHaveValue(QFieldMetaData field, QRecord record)
   {
      boolean fieldHasValue = false;

      try
      {
         if(record.getValue(field.getName()) != null)
         {
            fieldHasValue = true;
         }
         else if(field.getIsHeavy())
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // heavy fields that weren't fetched - they should have a backend-detail specifying their length (or null if null) //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            @SuppressWarnings("unchecked")
            Map<String, Serializable> heavyFieldLengths = (Map<String, Serializable>) record.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS);
            if(heavyFieldLengths != null)
            {
               Integer fieldLength = ValueUtils.getValueAsInteger(heavyFieldLengths.get(field.getName()));
               if(fieldLength != null && fieldLength > 0)
               {
                  fieldHasValue = true;
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.info("Error checking if field has value", e, logPair("fieldName", field.getName()), logPair("record", record));
      }

      return fieldHasValue;
   }

}
