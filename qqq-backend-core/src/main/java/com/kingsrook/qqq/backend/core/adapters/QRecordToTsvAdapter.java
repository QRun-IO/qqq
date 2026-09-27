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

package com.kingsrook.qqq.backend.core.adapters;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 * Class to convert QRecords to TSV (Tab Separated Value) Strings.
 *
 * By default, escapes \n, \t, \r, and \ chars within values as \n, \t, \r, and \\
 * This behavior can be customized via sanitizationType property.
 *******************************************************************************/
public class QRecordToTsvAdapter
{
   private SanitizationType sanitizationType = SanitizationType.ESCAPE;



   /***************************************************************************
    * options for how special-char values (\n, \t, \r, and \\) should be sanitized
    ***************************************************************************/
   public enum SanitizationType
   {
      ESCAPE,
      STRIP,
      NONE
   }



   /*******************************************************************************
    * Convert one record (with all of its table's fields) to TSV.
    * @param table the table that the record is from
    * @param record the record full of values
    * @return TSV string - ENDING with newline (\n)!
    *******************************************************************************/
   public String recordToTsv(QTableMetaData table, QRecord record)
   {
      return (recordToTsv(table, record, new ArrayList<>(table.getFields().values())));
   }



   /*******************************************************************************
    * Convert one record (with all of its table's fields) to TSV, with a specified
    * set of fields.
    * @param table the table that the record is from
    * @param record the record full of values
    * @param fields list of fields to include.  "joinTable.field" notation will work
    *               if the record contains joined fields.
    * @return TSV string - ENDING with newline (\n)!
    *******************************************************************************/
   public String recordToTsv(QTableMetaData table, QRecord record, List<QFieldMetaData> fields)
   {
      StringBuilder rs      = new StringBuilder();
      int           fieldNo = 0;

      for(QFieldMetaData field : fields)
      {
         if(fieldNo++ > 0)
         {
            rs.append('\t');
         }
         Serializable value         = record.getValue(field.getName());
         String       valueAsString = ValueUtils.getValueAsString(value);
         if(StringUtils.hasContent(valueAsString))
         {
            switch(sanitizationType)
            {
               case ESCAPE -> rs.append(escape(valueAsString));
               case STRIP -> rs.append(strip(valueAsString));
               case NONE -> rs.append(valueAsString);
               default -> throw new QRuntimeException("Invalid sanitization type: " + sanitizationType);
            }
         }
      }
      rs.append('\n');
      return (rs.toString());
   }



   /*******************************************************************************
    * escape tsv-special values in a string
    * @param value input value
    * @return value with \t replaced by \\t, etc.
    *******************************************************************************/
   static String escape(String value)
   {
      if(value != null)
      {
         /////////////////////////////////////////////////////////////////////////////////////
         // especially in big exports, we see a TON of memory allocated and CPU spent here, //
         // if we just blindly replaceAll.  So, only do it if needed.                       //
         /////////////////////////////////////////////////////////////////////////////////////
         if(value.contains("\\"))
         {
            value = value.replaceAll("\\\\", "\\\\\\\\");
         }
         if(value.contains("\n"))
         {
            value = value.replaceAll("\n", "\\\\" + "n");
         }
         if(value.contains("\r"))
         {
            value = value.replaceAll("\r", "\\\\" + "r");
         }
         if(value.contains("\t"))
         {
            value = value.replaceAll("\t", "\\\\" + "t");
         }
      }

      return (value);
   }



   /*******************************************************************************
    * strip tsv-special values from a string
    * @param value input value
    * @return value with \t replaced by ' ', etc.  Note, \\ does not get stripped.
    *******************************************************************************/
   static String strip(String value)
   {
      if(value.contains("\n"))
      {
         value = value.replaceAll("\n", " ");
      }
      if(value.contains("\r"))
      {
         value = value.replaceAll("\r", " ");
      }
      if(value.contains("\t"))
      {
         value = value.replaceAll("\t", " ");
      }

      return (value);
   }



   /*******************************************************************************
    * Getter for sanitizationType
    * @see #withSanitizationType(SanitizationType)
    *******************************************************************************/
   public SanitizationType getSanitizationType()
   {
      return (this.sanitizationType);
   }



   /*******************************************************************************
    * Setter for sanitizationType
    * @see #withSanitizationType(SanitizationType)
    *******************************************************************************/
   public void setSanitizationType(SanitizationType sanitizationType)
   {
      this.sanitizationType = sanitizationType;
   }



   /*******************************************************************************
    * Fluent setter for sanitizationType
    *
    * @param sanitizationType how tsv-special chars should be handled by this instance.
    * @return this
    *******************************************************************************/
   public QRecordToTsvAdapter withSanitizationType(SanitizationType sanitizationType)
   {
      this.sanitizationType = sanitizationType;
      return (this);
   }

}
