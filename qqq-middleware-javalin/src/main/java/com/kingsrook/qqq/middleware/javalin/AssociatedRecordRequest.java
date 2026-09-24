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

package com.kingsrook.qqq.middleware.javalin;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONParserConfiguration;
import org.json.JSONTokener;


/*******************************************************************************
 ** Explicit record envelopes preserve named descendant groups in legacy forms.
 ** Parsing never performs DML or changes the instance, session or source metadata.
 *******************************************************************************/
final class AssociatedRecordRequest
{
   static final String HEADER = "X-QQQ-Association-Format";
   static final String FORMAT = "record-v1";
   private static final int MAX_DEPTH = 64;



   /*******************************************************************************
    **
    *******************************************************************************/
   private AssociatedRecordRequest()
   {
   }



   /*******************************************************************************
    ** Reject malformed or unsupported shapes before permission preflight and DML.
    *******************************************************************************/
   static Map<String, List<QRecord>> read(String json, AbstractTableActionInput input) throws QException
   {
      try
      {
         JSONTokener tokener = new JSONTokener(json, new JSONParserConfiguration().withStrictMode().withMaxNestingDepth(MAX_DEPTH * 3 + 4));
         Object value = tokener.nextValue();
         if(!(value instanceof JSONObject groups) || tokener.nextClean() != 0)
         {
            throw new QBadRequestException("Associated records must be a JSON object");
         }
         return readGroups(groups, input, 0);
      }
      catch(JSONException | IllegalArgumentException e)
      {
         throw new QBadRequestException("Invalid associated record payload", e);
      }
   }



   /*******************************************************************************
    ** Resolve only declared, active association names; payloads cannot select tables.
    *******************************************************************************/
   private static Map<String, List<QRecord>> readGroups(JSONObject groups, AbstractTableActionInput input, int depth) throws QException
   {
      QTableMetaData table = TableMetaDataPersonalizerAction.execute(input);
      Map<String, List<QRecord>> result = new LinkedHashMap<>();
      for(String name : groups.keySet())
      {
         Association association = CollectionUtils.nonNullList(table.getAssociations()).stream()
            .filter(candidate -> name.equals(candidate.getName())).findFirst()
            .orElseThrow(() -> new QBadRequestException("Unknown association [" + name + "]"));
         Object group = groups.get(name);
         if(!(group instanceof JSONArray records))
         {
            throw new QBadRequestException("Association [" + name + "] must be an array");
         }
         if(depth >= MAX_DEPTH && !records.isEmpty())
         {
            throw new QBadRequestException("Associated records exceed the maximum depth of " + MAX_DEPTH);
         }
         QTableMetaData childTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
         List<QRecord> children = new ArrayList<>();
         result.put(name, children);
         for(Object child : records)
         {
            if(!(child instanceof JSONObject envelope) || !Set.of("values", "associatedRecords").containsAll(envelope.keySet())
               || !(envelope.opt("values") instanceof JSONObject values))
            {
               throw new QBadRequestException("Each associated record must contain a values object and optional associatedRecords object");
            }
            AbstractTableActionInput childInput = input instanceof UpdateInput && values.has(childTable.getPrimaryKeyField()) && !values.isNull(childTable.getPrimaryKeyField())
               ? new UpdateInput(childTable.getName()).withInputSource(input.getInputSource())
               : new InsertInput(childTable.getName()).withInputSource(input.getInputSource());
            QTableMetaData activeChild = TableMetaDataPersonalizerAction.execute(childInput);
            QRecord record = new QRecord().withTableName(childTable.getName());
            for(String fieldName : values.keySet())
            {
               QFieldMetaData field = activeChild.getField(fieldName);
               if(field == null)
               {
                  throw new QBadRequestException("Unknown field [" + fieldName + "] in associated record");
               }
               record.setValue(fieldName, readValue(values.get(fieldName), field));
            }
            if(envelope.has("associatedRecords"))
            {
               if(!(envelope.get("associatedRecords") instanceof JSONObject nested))
               {
                  throw new QBadRequestException("associatedRecords must be an object");
               }
               record.setAssociatedRecords(readGroups(nested, childInput, depth + 1));
            }
            children.add(record);
         }
      }
      return result;
   }



   /*******************************************************************************
    ** Binary data is explicit; ordinary text, false, zero and null stay distinct.
    *******************************************************************************/
   private static Serializable readValue(Object value, QFieldMetaData field) throws QBadRequestException
   {
      if(value == JSONObject.NULL)
      {
         return null;
      }
      if(value instanceof JSONObject binary)
      {
         if(field.getType() != QFieldType.BLOB || !binary.keySet().equals(Set.of("base64")) || !(binary.opt("base64") instanceof String encoded))
         {
            throw new QBadRequestException("Object field values require a BLOB base64 envelope");
         }
         try
         {
            return Base64.getDecoder().decode(encoded);
         }
         catch(IllegalArgumentException e)
         {
            throw new QBadRequestException("Invalid base64 value for field [" + field.getName() + "]", e);
         }
      }
      if(value instanceof JSONArray array)
      {
         ArrayList<Serializable> values = new ArrayList<>();
         for(Object item : array)
         {
            if(item instanceof JSONArray || item instanceof JSONObject)
            {
               throw new QBadRequestException("Field arrays may contain only scalar values");
            }
            values.add(item == JSONObject.NULL ? null : (Serializable) item);
         }
         return values;
      }
      return (Serializable) value;
   }
}
