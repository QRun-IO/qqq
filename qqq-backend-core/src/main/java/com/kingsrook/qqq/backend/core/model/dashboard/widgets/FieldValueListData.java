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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Model containing data structure expected by frontend FieldValueListData widget
 **
 *******************************************************************************/
public class FieldValueListData extends QWidgetData
{
   private List<QFieldMetaData> fields;
   private QRecord              record;

   private Map<String, String>  fieldLabelPrefixIconNames;
   private Map<String, String>  fieldLabelPrefixIconColors;
   private Map<String, Integer> fieldIndentLevels;



   /*******************************************************************************
    **
    *******************************************************************************/
   public FieldValueListData()
   {
      this.fields = new ArrayList<>();
      this.record = new QRecord();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData addField(QFieldMetaData field)
   {
      fields.add(field);
      return (field);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setValue(String fieldName, Serializable value)
   {
      record.setValue(fieldName, value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setDisplayValue(String fieldName, String displayValue)
   {
      record.setDisplayValue(fieldName, displayValue);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData addFieldWithValue(String fieldName, QFieldType type, Serializable value)
   {
      return (addFieldWithValue(fieldName, type, value, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData addFieldWithValue(String fieldName, QFieldType type, Serializable value, String displayValue)
   {
      QFieldMetaData field = new QFieldMetaData(fieldName, type);
      addField(field);

      record.setValue(fieldName, value);
      if(displayValue != null)
      {
         record.setDisplayValue(fieldName, displayValue);
      }

      return (field);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public FieldValueListData(List<QFieldMetaData> fields, QRecord record)
   {
      this.fields = fields;
      this.record = record;
      enrich();
   }



   /*******************************************************************************
    ** do some enrichment on fields (e.g., name -> label) and set display values in the record.
    *******************************************************************************/
   public void enrich()
   {
      for(QFieldMetaData field : fields)
      {
         if(!StringUtils.hasContent(field.getLabel()))
         {
            field.setLabel(QInstanceEnricher.nameToLabel(field.getName()));
         }
      }

      if(record != null && record.getTableName() != null)
      {
         QInstance      qInstance = QContext.getQInstance();
         QTableMetaData table     = qInstance.getTable(record.getTableName());
         QValueFormatter.setDisplayValuesInRecordsIncludingPossibleValueTranslations(table, List.of(record));
      }
      else
      {
         QValueFormatter.setDisplayValuesInRecord(null, fields.stream().collect(Collectors.toMap(f -> f.getName(), f -> f)), record);
      }
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.FIELD_VALUE_LIST.getType();
   }



   /*******************************************************************************
    ** Getter for fields
    **
    *******************************************************************************/
   public List<QFieldMetaData> getFields()
   {
      return fields;
   }



   /*******************************************************************************
    ** Setter for fields
    **
    *******************************************************************************/
   public void setFields(List<QFieldMetaData> fields)
   {
      this.fields = fields;
   }



   /*******************************************************************************
    ** Fluent setter for fields
    **
    *******************************************************************************/
   public FieldValueListData withFields(List<QFieldMetaData> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for record
    **
    *******************************************************************************/
   public QRecord getRecord()
   {
      return record;
   }



   /*******************************************************************************
    ** Setter for record
    **
    *******************************************************************************/
   public void setRecord(QRecord record)
   {
      this.record = record;
   }



   /*******************************************************************************
    ** Fluent setter for record
    **
    *******************************************************************************/
   public FieldValueListData withRecord(QRecord record)
   {
      this.record = record;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setFieldLabelPrefixIconAndColor(String fieldName, Pair<String, String> iconAndColorPair)
   {
      setFieldLabelPrefixIconAndColor(fieldName, iconAndColorPair.getA(), iconAndColorPair.getB());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setFieldLabelPrefixIconAndColor(String fieldName, String iconName, String color)
   {
      if(fieldLabelPrefixIconNames == null)
      {
         fieldLabelPrefixIconNames = new HashMap<>();
      }

      if(fieldLabelPrefixIconColors == null)
      {
         fieldLabelPrefixIconColors = new HashMap<>();
      }

      fieldLabelPrefixIconNames.put(fieldName, iconName);
      fieldLabelPrefixIconColors.put(fieldName, color);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setFieldIndentLevel(String fieldName, Integer indentLevel)
   {
      if(fieldIndentLevels == null)
      {
         fieldIndentLevels = new HashMap<>();
      }

      fieldIndentLevels.put(fieldName, indentLevel);
   }



   /*******************************************************************************
    ** Getter for fieldLabelPrefixIconNames
    **
    *******************************************************************************/
   public Map<String, String> getFieldLabelPrefixIconNames()
   {
      return fieldLabelPrefixIconNames;
   }



   /*******************************************************************************
    ** Getter for fieldLabelPrefixIconColors
    **
    *******************************************************************************/
   public Map<String, String> getFieldLabelPrefixIconColors()
   {
      return fieldLabelPrefixIconColors;
   }



   /*******************************************************************************
    ** Getter for fieldIndentLevels
    **
    *******************************************************************************/
   public Map<String, Integer> getFieldIndentLevels()
   {
      return fieldIndentLevels;
   }
}
