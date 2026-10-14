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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;


/*******************************************************************************
 ** The difference between two table schema fingerprints' descriptors: fields
 ** added, removed and changed, plus any change to the table-level properties
 ** (name, primary key, backend type, unique keys, associations).
 **
 ** "before" is typically the stored fingerprint (e.g., from a backup manifest)
 ** and "after" the current table.
 *******************************************************************************/
public final class SchemaDiff
{
   private final List<String>          addedFields;
   private final List<String>          removedFields;
   private final List<FieldChange>     changedFields;
   private final List<PropertyChange>  changedTableProperties;



   /*******************************************************************************
    ** One changed field: which attributes differ, with their before and after values.
    *******************************************************************************/
   public record FieldChange(String fieldName, Map<String, Object> before, Map<String, Object> after, List<String> changedAttributes)
   {
   }



   /*******************************************************************************
    ** One changed table-level property (e.g., primaryKeyField or uniqueKeys).
    *******************************************************************************/
   public record PropertyChange(String property, Object before, Object after)
   {
   }



   /*******************************************************************************
    ** Private constructor - use of(...)
    *******************************************************************************/
   private SchemaDiff(List<String> addedFields, List<String> removedFields, List<FieldChange> changedFields, List<PropertyChange> changedTableProperties)
   {
      this.addedFields = Collections.unmodifiableList(addedFields);
      this.removedFields = Collections.unmodifiableList(removedFields);
      this.changedFields = Collections.unmodifiableList(changedFields);
      this.changedTableProperties = Collections.unmodifiableList(changedTableProperties);
   }



   /*******************************************************************************
    ** Compare two fingerprints' descriptors.
    *******************************************************************************/
   public static SchemaDiff of(TableSchemaFingerprint before, TableSchemaFingerprint after)
   {
      Map<String, Object> beforeDescriptor = before.descriptorNoCopy();
      Map<String, Object> afterDescriptor  = after.descriptorNoCopy();

      Map<String, Map<String, Object>> beforeFields = fieldsByName(beforeDescriptor);
      Map<String, Map<String, Object>> afterFields  = fieldsByName(afterDescriptor);

      List<String>      added   = new ArrayList<>();
      List<String>      removed = new ArrayList<>();
      List<FieldChange> changed = new ArrayList<>();

      for(String name : new TreeSet<>(afterFields.keySet()))
      {
         if(!beforeFields.containsKey(name))
         {
            added.add(name);
         }
      }

      for(String name : new TreeSet<>(beforeFields.keySet()))
      {
         if(!afterFields.containsKey(name))
         {
            removed.add(name);
            continue;
         }

         Map<String, Object> beforeField = beforeFields.get(name);
         Map<String, Object> afterField  = afterFields.get(name);

         List<String> changedAttributes = new ArrayList<>();
         for(String attribute : new TreeSet<>(union(beforeField, afterField)))
         {
            if(!Objects.equals(beforeField.get(attribute), afterField.get(attribute)))
            {
               changedAttributes.add(attribute);
            }
         }

         if(!changedAttributes.isEmpty())
         {
            changed.add(new FieldChange(name, beforeField, afterField, changedAttributes));
         }
      }

      List<PropertyChange> tableProperties = new ArrayList<>();
      for(String key : new TreeSet<>(union(beforeDescriptor, afterDescriptor)))
      {
         if("fields".equals(key))
         {
            continue;
         }

         if(!Objects.equals(beforeDescriptor.get(key), afterDescriptor.get(key)))
         {
            tableProperties.add(new PropertyChange(key, beforeDescriptor.get(key), afterDescriptor.get(key)));
         }
      }

      return (new SchemaDiff(added, removed, changed, tableProperties));
   }



   /*******************************************************************************
    ** Names of fields in "after" but not in "before", sorted.
    *******************************************************************************/
   public List<String> getAddedFields()
   {
      return (addedFields);
   }



   /*******************************************************************************
    ** Names of fields in "before" but not in "after", sorted.
    *******************************************************************************/
   public List<String> getRemovedFields()
   {
      return (removedFields);
   }



   /*******************************************************************************
    ** Fields in both, whose descriptor attributes differ, sorted by field name.
    *******************************************************************************/
   public List<FieldChange> getChangedFields()
   {
      return (changedFields);
   }



   /*******************************************************************************
    ** Changed table-level descriptor properties (table, primaryKeyField,
    ** backendType, uniqueKeys, associations), sorted by property name.
    *******************************************************************************/
   public List<PropertyChange> getChangedTableProperties()
   {
      return (changedTableProperties);
   }



   /*******************************************************************************
    ** True when nothing differs.
    *******************************************************************************/
   public boolean isEmpty()
   {
      return (addedFields.isEmpty() && removedFields.isEmpty() && changedFields.isEmpty() && changedTableProperties.isEmpty());
   }



   /*******************************************************************************
    ** Short human-readable summary, e.g., for logs and review lines.
    *******************************************************************************/
   @Override
   public String toString()
   {
      if(isEmpty())
      {
         return ("no schema changes");
      }

      List<String> parts = new ArrayList<>();
      if(!addedFields.isEmpty())
      {
         parts.add("added fields " + addedFields);
      }
      if(!removedFields.isEmpty())
      {
         parts.add("removed fields " + removedFields);
      }
      for(FieldChange change : changedFields)
      {
         parts.add("changed field " + change.fieldName() + " " + change.changedAttributes());
      }
      if(!changedTableProperties.isEmpty())
      {
         parts.add("changed table properties " + changedTableProperties.stream().map(PropertyChange::property).toList());
      }
      return (String.join("; ", parts));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static Map<String, Map<String, Object>> fieldsByName(Map<String, Object> descriptor)
   {
      Map<String, Map<String, Object>> rs = new LinkedHashMap<>();
      Object                           fields = descriptor.get("fields");
      if(fields instanceof List<?> list)
      {
         for(Object field : list)
         {
            Map<String, Object> fieldMap = (Map<String, Object>) field;
            rs.put(String.valueOf(fieldMap.get("name")), fieldMap);
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> union(Map<String, Object> a, Map<String, Object> b)
   {
      List<String> rs = new ArrayList<>(a.keySet());
      for(String key : b.keySet())
      {
         if(!a.containsKey(key))
         {
            rs.add(key);
         }
      }
      return (rs);
   }

}
