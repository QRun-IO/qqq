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

package com.kingsrook.qqq.backend.core.model.metadata.possiblevalues;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/*******************************************************************************
 ** Meta-data to represent a "Possible value" - e.g., a translation of a foreign
 ** key and/or a limited set of "possible values" for a field (e.g., from a foreign
 ** table or an enum).
 **
 *******************************************************************************/
public class QPossibleValueSource implements TopLevelMetaDataInterface
{
   private String                   name;
   private String                   label;
   private QPossibleValueSourceType type;

   private QFieldType idType;

   private String       valueFormat           = PVSValueFormatAndFields.LABEL_ONLY.getFormat();
   private List<String> valueFields           = PVSValueFormatAndFields.LABEL_ONLY.getFields();
   private String       valueFormatIfNotFound = null;
   private List<String> valueFieldsIfNotFound = null;

   // todo - optimization hints, such as "table is static, fully cache" or "table is small, so we can pull the whole thing into memory?"

   //////////////////////
   // for type = TABLE //
   //////////////////////
   private String               tableName;
   private String overrideIdField;
   private List<String>         searchFields;
   private List<QFilterOrderBy> orderByFields;

   /////////////////////
   // for type = ENUM //
   /////////////////////
   private List<QPossibleValue<?>> enumValues;

   ///////////////////////
   // for type = CUSTOM //
   ///////////////////////
   private QCodeReference customCodeReference;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource()
   {
   }



   /*******************************************************************************
    ** Create a new possible value source, for a table, with default settings.
    ** e.g., name & table name from the tableName parameter; type=TABLE; and LABEL_ONLY format
    *******************************************************************************/
   public static QPossibleValueSource newForTable(String tableName)
   {
      return new QPossibleValueSource()
         .withName(tableName)
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName(tableName)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY);
   }



   /*******************************************************************************
    ** Create a new possible value source, for an enum, with default settings.
    ** e.g., type=ENUM; name from param values from the param; LABEL_ONLY format
    *******************************************************************************/
   public static <I extends Serializable, T extends PossibleValueEnum<I>> QPossibleValueSource newForEnum(String name, T[] values)
   {
      return new QPossibleValueSource()
         .withName(name)
         .withType(QPossibleValueSourceType.ENUM)
         .withValuesFromEnum(values)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    **
    *******************************************************************************/
   public QPossibleValueSource withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSourceType getType()
   {
      return type;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setType(QPossibleValueSourceType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource withType(QPossibleValueSourceType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for valueFormat
    **
    *******************************************************************************/
   public String getValueFormat()
   {
      return valueFormat;
   }



   /*******************************************************************************
    ** Setter for valueFormat
    **
    *******************************************************************************/
   public void setValueFormat(String valueFormat)
   {
      this.valueFormat = valueFormat;
   }



   /*******************************************************************************
    ** Fluent setter for valueFormat
    **
    *******************************************************************************/
   public QPossibleValueSource withValueFormat(String valueFormat)
   {
      this.valueFormat = valueFormat;
      return (this);
   }



   /*******************************************************************************
    ** Getter for valueFields
    **
    *******************************************************************************/
   public List<String> getValueFields()
   {
      return valueFields;
   }



   /*******************************************************************************
    ** Setter for valueFields
    **
    *******************************************************************************/
   public void setValueFields(List<String> valueFields)
   {
      this.valueFields = valueFields;
   }



   /*******************************************************************************
    ** Fluent setter for valueFields
    **
    *******************************************************************************/
   public QPossibleValueSource withValueFields(List<String> valueFields)
   {
      this.valueFields = valueFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for valueFormatIfNotFound
    **
    *******************************************************************************/
   public String getValueFormatIfNotFound()
   {
      return valueFormatIfNotFound;
   }



   /*******************************************************************************
    ** Setter for valueFormatIfNotFound
    **
    *******************************************************************************/
   public void setValueFormatIfNotFound(String valueFormatIfNotFound)
   {
      this.valueFormatIfNotFound = valueFormatIfNotFound;
   }



   /*******************************************************************************
    ** Fluent setter for valueFormatIfNotFound
    **
    *******************************************************************************/
   public QPossibleValueSource withValueFormatIfNotFound(String valueFormatIfNotFound)
   {
      this.valueFormatIfNotFound = valueFormatIfNotFound;
      return (this);
   }



   /*******************************************************************************
    ** Getter for valueFieldsIfNotFound
    **
    *******************************************************************************/
   public List<String> getValueFieldsIfNotFound()
   {
      return valueFieldsIfNotFound;
   }



   /*******************************************************************************
    ** Setter for valueFieldsIfNotFound
    **
    *******************************************************************************/
   public void setValueFieldsIfNotFound(List<String> valueFieldsIfNotFound)
   {
      this.valueFieldsIfNotFound = valueFieldsIfNotFound;
   }



   /*******************************************************************************
    ** Fluent setter for valueFieldsIfNotFound
    **
    *******************************************************************************/
   public QPossibleValueSource withValueFieldsIfNotFound(List<String> valueFieldsIfNotFound)
   {
      this.valueFieldsIfNotFound = valueFieldsIfNotFound;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.type = QPossibleValueSourceType.TABLE;
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    **
    *******************************************************************************/
   public QPossibleValueSource withTableName(String tableName)
   {
      setTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for searchFields
    **
    *******************************************************************************/
   public List<String> getSearchFields()
   {
      return searchFields;
   }



   /*******************************************************************************
    ** Setter for searchFields
    **
    *******************************************************************************/
   public void setSearchFields(List<String> searchFields)
   {
      this.searchFields = searchFields;
   }



   /*******************************************************************************
    ** Fluent setter for searchFields
    **
    *******************************************************************************/
   public QPossibleValueSource withSearchFields(List<String> searchFields)
   {
      this.searchFields = searchFields;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for searchFields
    **
    *******************************************************************************/
   public QPossibleValueSource withSearchField(String searchField)
   {
      if(this.searchFields == null)
      {
         this.searchFields = new ArrayList<>();
      }
      this.searchFields.add(searchField);
      return (this);
   }



   /*******************************************************************************
    ** Getter for orderByFields
    **
    *******************************************************************************/
   public List<QFilterOrderBy> getOrderByFields()
   {
      return orderByFields;
   }



   /*******************************************************************************
    ** Setter for orderByFields
    **
    *******************************************************************************/
   public void setOrderByFields(List<QFilterOrderBy> orderByFields)
   {
      this.orderByFields = orderByFields;
   }



   /*******************************************************************************
    ** Fluent setter for orderByFields
    **
    *******************************************************************************/
   public QPossibleValueSource withOrderByFields(List<QFilterOrderBy> orderByFields)
   {
      this.orderByFields = orderByFields;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for orderByFields
    **
    *******************************************************************************/
   public QPossibleValueSource withOrderByField(QFilterOrderBy orderByField)
   {
      if(this.orderByFields == null)
      {
         this.orderByFields = new ArrayList<>();
      }
      this.orderByFields.add(orderByField);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for orderByFields - default to ASCENDING
    **
    *******************************************************************************/
   public QPossibleValueSource withOrderByField(String fieldName)
   {
      return (withOrderByField(new QFilterOrderBy(fieldName)));
   }



   /*******************************************************************************
    ** Fluent setter for orderByFields
    **
    *******************************************************************************/
   public QPossibleValueSource withOrderByField(String fieldName, boolean isAscending)
   {
      return (withOrderByField(new QFilterOrderBy(fieldName, isAscending)));
   }



   /*******************************************************************************
    ** Getter for enumValues
    **
    *******************************************************************************/
   public List<QPossibleValue<?>> getEnumValues()
   {
      return enumValues;
   }



   /*******************************************************************************
    ** Setter for enumValues
    **
    *******************************************************************************/
   public void setEnumValues(List<QPossibleValue<?>> enumValues)
   {
      this.enumValues = enumValues;
      setType(QPossibleValueSourceType.ENUM);
   }



   /*******************************************************************************
    ** Fluent setter for enumValues
    **
    *******************************************************************************/
   public QPossibleValueSource withEnumValues(List<QPossibleValue<?>> enumValues)
   {
      setEnumValues(enumValues);
      return this;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addEnumValue(QPossibleValue<?> possibleValue)
   {
      if(this.enumValues == null)
      {
         this.enumValues = new ArrayList<>();
      }
      this.enumValues.add(possibleValue);
      setType(QPossibleValueSourceType.ENUM);
   }



   /*******************************************************************************
    ** This is the easiest way to add the values from an enum to a PossibleValueSource.
    ** Make sure the enum implements PossibleValueEnum - then call as:
    **   myPossibleValueSource.withValuesFromEnum(MyEnum.values()));
    **
    *******************************************************************************/
   public <I extends Serializable, T extends PossibleValueEnum<I>> QPossibleValueSource withValuesFromEnum(T[] values)
   {
      Set<I> usedIds = new HashSet<>();
      List<I> duplicatedIds = new ArrayList<>();

      for(T t : values)
      {
         if(usedIds.contains(t.getPossibleValueId()))
         {
            duplicatedIds.add(t.getPossibleValueId());
         }

         addEnumValue(new QPossibleValue<>(t.getPossibleValueId(), t.getPossibleValueLabel()));
         usedIds.add(t.getPossibleValueId());
      }

      if(!duplicatedIds.isEmpty())
      {
         throw (new QRuntimeException("Error:  Duplicated id(s) found in enum values: " + duplicatedIds));
      }

      return (this);
   }



   /*******************************************************************************
    ** Getter for customCodeReference
    **
    *******************************************************************************/
   public QCodeReference getCustomCodeReference()
   {
      return customCodeReference;
   }



   /*******************************************************************************
    ** Setter for customCodeReference
    **
    *******************************************************************************/
   public void setCustomCodeReference(QCodeReference customCodeReference)
   {
      this.customCodeReference = customCodeReference;
      setType(QPossibleValueSourceType.CUSTOM);
   }



   /*******************************************************************************
    ** Fluent setter for customCodeReference
    **
    *******************************************************************************/
   public QPossibleValueSource withCustomCodeReference(QCodeReference customCodeReference)
   {
      setCustomCodeReference(customCodeReference);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setValueFormatAndFields(PVSValueFormatAndFields valueFormatAndFields)
   {
      this.valueFormat = valueFormatAndFields.getFormat();
      this.valueFields = valueFormatAndFields.getFields();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource withValueFormatAndFields(PVSValueFormatAndFields valueFormatAndFields)
   {
      setValueFormatAndFields(valueFormatAndFields);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addPossibleValueSource(this);
   }



   /*******************************************************************************
    ** Getter for overrideIdField
    *******************************************************************************/
   public String getOverrideIdField()
   {
      return (this.overrideIdField);
   }



   /*******************************************************************************
    ** Setter for overrideIdField
    *******************************************************************************/
   public void setOverrideIdField(String overrideIdField)
   {
      this.overrideIdField = overrideIdField;
   }



   /*******************************************************************************
    ** Fluent setter for overrideIdField
    *******************************************************************************/
   public QPossibleValueSource withOverrideIdField(String overrideIdField)
   {
      this.overrideIdField = overrideIdField;
      return (this);
   }


   /*******************************************************************************
    ** Getter for idType
    *******************************************************************************/
   public QFieldType getIdType()
   {
      return (this.idType);
   }



   /*******************************************************************************
    ** Setter for idType
    *******************************************************************************/
   public void setIdType(QFieldType idType)
   {
      this.idType = idType;
   }



   /*******************************************************************************
    ** Fluent setter for idType
    *******************************************************************************/
   public QPossibleValueSource withIdType(QFieldType idType)
   {
      this.idType = idType;
      return (this);
   }


}
