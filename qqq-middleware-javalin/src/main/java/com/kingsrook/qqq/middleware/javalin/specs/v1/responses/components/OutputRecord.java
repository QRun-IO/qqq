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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapValueType;


/***************************************************************************
 **
 ***************************************************************************/
public class OutputRecord implements ToSchema
{
   @OpenAPIExclude()
   private QRecord wrapped;

   @OpenAPIExclude()
   private boolean includeEmptyValues = false;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public OutputRecord(QRecord wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor - with includeEmptyValues, null and empty values stay in the
    ** values map (as on the legacy record routes), so a client can tell a cleared
    ** field from one it did not receive.  Query results leave them out.
    *******************************************************************************/
   public OutputRecord(QRecord wrapped, boolean includeEmptyValues)
   {
      this.wrapped = wrapped;
      this.includeEmptyValues = includeEmptyValues;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public OutputRecord()
   {
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   @OpenAPIDescription("Name of the table that the record is from.")
   public String getTableName()
   {
      return this.wrapped.getTableName();
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   @OpenAPIDescription("Label to identify the record to a user.")
   public String getRecordLabel()
   {
      return this.wrapped.getRecordLabel();
   }



   /*******************************************************************************
    ** Getter for values
    **
    *******************************************************************************/
   @OpenAPIDescription("Raw values that make up the record.  Keys are Strings, which match the table's field names. Values can be any type, as per the table's fields.  Single-record responses (get, insert, update) include fields whose value is null; query results omit them.")
   @JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.ALWAYS)
   public Map<String, Serializable> getValues()
   {
      Map<String, Serializable> values = this.wrapped.getValues();
      if(this.includeEmptyValues || values == null)
      {
         return (values);
      }

      Map<String, Serializable> nonEmpty = new LinkedHashMap<>();
      values.forEach((name, value) ->
      {
         if(!isEmpty(value))
         {
            nonEmpty.put(name, value);
         }
      });
      return (nonEmpty);
   }



   /*******************************************************************************
    ** What the default (NON_EMPTY) serialization leaves out of a values map.
    *******************************************************************************/
   private static boolean isEmpty(Serializable value)
   {
      return (value == null
         || (value instanceof String string && string.isEmpty())
         || (value instanceof byte[] bytes && bytes.length == 0)
         || (value instanceof Collection<?> collection && collection.isEmpty())
         || (value instanceof Map<?, ?> map && map.isEmpty()));
   }



   /*******************************************************************************
    ** Getter for displayValues
    **
    *******************************************************************************/
   @OpenAPIDescription("Formatted string versions of the values that make up the record.  Keys are Strings, which match the table's field names. Values are all Strings.")
   public Map<String, String> getDisplayValues()
   {
      return this.wrapped.getDisplayValues();
   }



   /*******************************************************************************
    ** Getter for associatedRecords - only present when associations were requested
    ** (record get with includeAssociations) or written (insert/update).  An
    ** association without records is an empty list (as on QRecord), so a client
    ** can tell "no records" from "not included".
    *******************************************************************************/
   @OpenAPIDescription("Records associated with this record, keyed by association name.  Each value is a list of records with the same shape as this one.  Only present when associations were requested or written.")
   @OpenAPIMapValueType(value = List.class)
   @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
   public Map<String, List<OutputRecord>> getAssociatedRecords()
   {
      if(CollectionUtils.nullSafeIsEmpty(this.wrapped.getAssociatedRecords()))
      {
         return (null);
      }

      Map<String, List<OutputRecord>> associatedRecords = new LinkedHashMap<>();
      this.wrapped.getAssociatedRecords().forEach((name, records) -> associatedRecords.put(name, CollectionUtils.nonNullList(records).stream().map(child -> new OutputRecord(child, this.includeEmptyValues)).toList()));
      return (associatedRecords);
   }



   /*******************************************************************************
    ** Getter for errors
    *******************************************************************************/
   @OpenAPIDescription("Messages for errors on this record (for example on an associated record that could not be written).  Omitted when there are none.")
   @OpenAPIListItems(value = String.class)
   public List<String> getErrors()
   {
      return (messages(this.wrapped.getErrors()));
   }



   /*******************************************************************************
    ** Getter for warnings
    *******************************************************************************/
   @OpenAPIDescription("Messages for warnings on this record.  Omitted when there are none.")
   @OpenAPIListItems(value = String.class)
   public List<String> getWarnings()
   {
      return (messages(this.wrapped.getWarnings()));
   }



   /*******************************************************************************
    ** Status messages as text, or null for none (so the property is omitted).
    *******************************************************************************/
   private static List<String> messages(List<? extends QStatusMessage> statusMessages)
   {
      if(CollectionUtils.nullSafeIsEmpty(statusMessages))
      {
         return (null);
      }
      return (statusMessages.stream().map(QStatusMessage::getMessage).toList());
   }

}
