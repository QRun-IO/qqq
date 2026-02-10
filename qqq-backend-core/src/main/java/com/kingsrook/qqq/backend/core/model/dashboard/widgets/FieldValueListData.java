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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

      QTableMetaData table = null;
      if(record != null && record.getTableName() != null)
      {
         QInstance qInstance = QContext.getQInstance();
         table = qInstance.getTable(record.getTableName());

         QValueFormatter.setDisplayValuesInRecordsIncludingPossibleValueTranslations(table, List.of(record));
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
