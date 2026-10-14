/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Compatibility facade for field locks; enforcement is owned by FieldAccessHelper.
 *******************************************************************************/
public final class FieldSecurityLockHelper
{



   /*******************************************************************************
    ** Static utility class.
    *******************************************************************************/
   private FieldSecurityLockHelper()
   {

   }



   /*******************************************************************************
    ** Whether the field is visible in metadata. Read/write checks use the full tier contract.
    *******************************************************************************/
   public static boolean isFieldAccessible(QFieldMetaData field)
   {
      if(field == null)
      {
         return (true);
      }
      if(QContext.getQInstance() != null)
      {
         for(QTableMetaData table : QContext.getQInstance().getTables().values())
         {
            if(CollectionUtils.nonNullMap(table.getFields()).containsValue(field) || CollectionUtils.nonNullMap(table.getVirtualFields()).containsValue(field))
            {
               return (FieldAccessHelper.getDecision(table, field).tier().isInMetaData());
            }
         }
      }
      return (FieldAccessHelper.getDecision(null, field).tier().isInMetaData());
   }



   /*******************************************************************************
    ** Only HIDDEN fields retain the legacy denied-field meaning.
    *******************************************************************************/
   public static Set<String> getDeniedFieldNames(QTableMetaData table)
   {
      Set<String> denied = new LinkedHashSet<>();
      FieldAccessHelper.getTiers(table).forEach((name, tier) ->
      {
         if(tier == FieldAccessTier.HIDDEN)
         {
            denied.add(name);
         }
      });
      return (denied);
   }



   /*******************************************************************************
    ** Delegate to tier-aware enforcement.
    *******************************************************************************/
   public static QTableMetaData removeDeniedFields(QTableMetaData table)
   {
      return (FieldAccessHelper.applyToTable(table));
   }



   /*******************************************************************************
    ** Delegate to tier-aware enforcement.
    *******************************************************************************/
   public static void assertCriteriaAccessible(String tableName, List<QueryJoin> joins, QQueryFilter filter, Collection<String> additionalFieldNames) throws QPermissionDeniedException
   {
      FieldAccessHelper.assertCriteriaAllowed(tableName, joins, filter, additionalFieldNames);
   }



   /*******************************************************************************
    ** Delegate to tier-aware enforcement.
    *******************************************************************************/
   public static void assertWritesAccessible(QTableMetaData table, List<QRecord> records) throws QPermissionDeniedException
   {
      FieldAccessHelper.assertWritesAllowed(table, records);
   }



   /*******************************************************************************
    ** Delegate to tier-aware enforcement.
    *******************************************************************************/
   public static void removeDeniedValues(String tableName, List<QueryJoin> joins, List<QRecord> records)
   {
      FieldAccessHelper.applyToRecords(tableName, joins, records, "query");
   }
}
