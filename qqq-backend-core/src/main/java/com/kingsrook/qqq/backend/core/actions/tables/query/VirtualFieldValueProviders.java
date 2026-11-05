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

package com.kingsrook.qqq.backend.core.actions.tables.query;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Share value-provider selection and execution between query and native GET.
 *******************************************************************************/
public final class VirtualFieldValueProviders
{
   /***************************************************************************
    ** Utility methods only.
    ***************************************************************************/
   private VirtualFieldValueProviders()
   {
   }

   /***************************************************************************
    ** Select providers using the originating request's field-access decision.
    ***************************************************************************/
   public static List<Selection> selected(QueryInput input, boolean enforce) throws QException
   {
      List<Selection> result = new ArrayList<>();
      select(input, input.getTable(), "", enforce, result);
      for(QueryJoin join : CollectionUtils.nonNullList(input.getQueryJoins()))
      {
         if(join.getSelect())
         {
            QTableMetaData table = QContext.getQInstance().getTable(join.getJoinTable());
            if(table == null)
            {
               throw new QException("Unknown joined table for virtual-field provider");
            }
            table = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput()
               .withTableMetaData(table).withInputSource(input.getInputSource()), enforce);
            select(input, table, join.getJoinTableOrItsAlias() + ".", enforce, result);
         }
      }
      return (result);
   }

   /***************************************************************************
    ** Keep the source table and output prefix with each selected provider.
    ***************************************************************************/
   public record Selection(QTableMetaData table, String prefix, QVirtualFieldMetaData field)
   {
   }

   /***************************************************************************
    ** Apply the same visibility and projection policy to base and joined fields.
    ***************************************************************************/
   private static void select(QueryInput input, QTableMetaData table, String prefix, boolean enforce, List<Selection> result)
   {
      if(table == null)
      {
         return;
      }
      for(QVirtualFieldMetaData field : CollectionUtils.nonNullMap(table.getVirtualFields()).values())
      {
         if(field.getIsQuerySelectable() && field.getValueProvider() != null
            && (input.getFieldNamesToInclude() == null || input.getFieldNamesToInclude().contains(prefix + field.getName()))
            && !(input.getShouldOmitHiddenFields() && field.getIsHidden())
            && !(enforce && FieldAccessHelper.getTier(table, field.getName()) == FieldAccessTier.HIDDEN))
         {
            result.add(new Selection(table, prefix, field));
         }
      }
   }

   /***************************************************************************
    ** Fill each selected field once after host customization, before final masking.
    ***************************************************************************/
   public static void fill(QueryInput input, List<QRecord> records, boolean enforce) throws QException
   {
      if(records.isEmpty())
      {
         return;
      }
      for(Selection selection : selected(input, enforce))
      {
         VirtualFieldValueProviderInterface provider = QCodeLoader.getAdHoc(VirtualFieldValueProviderInterface.class, selection.field().getValueProvider());
         if(selection.prefix().isEmpty())
         {
            provider.fill(selection.table(), selection.field().getName(), records, input);
            continue;
         }
         List<QRecord> joinedRecords = new ArrayList<>();
         List<QRecord> sourceRecords = new ArrayList<>();
         for(QRecord source : records)
         {
            if(source.getValue(selection.prefix() + selection.table().getPrimaryKeyField()) == null)
            {
               source.setValue(selection.prefix() + selection.field().getName(), null);
               continue;
            }
            QRecord joined = new QRecord();
            source.getValues().forEach((name, value) ->
            {
               if(name.startsWith(selection.prefix()))
               {
                  joined.setValue(name.substring(selection.prefix().length()), value);
               }
            });
            joinedRecords.add(joined);
            sourceRecords.add(source);
         }
         if(!joinedRecords.isEmpty())
         {
            provider.fill(selection.table(), selection.field().getName(), joinedRecords, input);
            for(int i = 0; i < sourceRecords.size(); i++)
            {
               sourceRecords.get(i).setValue(selection.prefix() + selection.field().getName(), joinedRecords.get(i).getValue(selection.field().getName()));
            }
         }
      }
   }
}
