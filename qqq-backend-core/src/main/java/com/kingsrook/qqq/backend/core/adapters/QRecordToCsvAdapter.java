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
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Class to convert QRecords to CSV Strings.
 *******************************************************************************/
public class QRecordToCsvAdapter
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public String recordToCsv(QTableMetaData table, QRecord record)
   {
      return (recordToCsv(table, record, new ArrayList<>(table.getFields().values())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String recordToCsv(QTableMetaData table, QRecord record, List<QFieldMetaData> fields)
   {
      StringBuilder rs      = new StringBuilder();
      int           fieldNo = 0;

      for(QFieldMetaData field : fields)
      {
         if(fieldNo++ > 0)
         {
            rs.append(',');
         }
         rs.append('"');
         Serializable value         = record.getValue(field.getName());
         String       valueAsString = ValueUtils.getValueAsString(value);
         if(StringUtils.hasContent(valueAsString))
         {
            rs.append(sanitize(valueAsString));
         }
         rs.append('"');
      }
      rs.append('\n');
      return (rs.toString());
   }



   /*******************************************************************************
    ** todo - kinda weak... can we find this in a CSV lib??
    *******************************************************************************/
   static String sanitize(String value)
   {
      /////////////////////////////////////////////////////////////////////////////////////
      // especially in big exports, we see a TON of memory allocated and CPU spent here, //
      // if we just blindly replaceAll.  So, only do it if needed.                       //
      /////////////////////////////////////////////////////////////////////////////////////
      if(value.contains("\""))
      {
         value = value.replaceAll("\"", "\"\"");
      }

      if(value.contains("\n"))
      {
         value = value.replaceAll("\n", " ");
      }

      return (value);
   }

}
