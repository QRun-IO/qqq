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

package com.kingsrook.qqq.backend.core.actions.preferences;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceChange;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceProviderInterface;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRequestContext;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceValues;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Default scalar preferences provider using JSON-encoded values.
 *******************************************************************************/
public class KeyValueUserPreferenceProvider implements UserPreferenceProviderInterface
{
   /*******************************************************************************
    ** Load only rows owned by the session user and this section; absent rows use defaults.
    *******************************************************************************/
   @Override
   public UserPreferenceValues load(UserPreferenceRequestContext context) throws QException
   {
      Map<String, QRecord> records = records(context);
      UserPreferenceValues result = new UserPreferenceValues();
      for(QFieldMetaData field : context.getSection().getFields())
      {
         Object value = field.getDefaultValue();
         if(records.containsKey(field.getName()))
         {
            try
            {
               value = JsonUtils.toObject(records.get(field.getName()).getValueString("value"), Object.class);
            }
            catch(Exception e)
            {
               throw (new QException("Invalid stored preference " + field.getName(), e));
            }
         }
         result.getValues().put(field.getName(), ValueUtils.getValueAsFieldType(field.getType(), value));
      }
      return (result);
   }



   /*******************************************************************************
    ** Write partial changes through standard actions, retaining audit attribution.
    *******************************************************************************/
   @Override
   public void store(UserPreferenceRequestContext context, UserPreferenceChange change) throws QException
   {
      Map<String, QRecord> records = records(context);
      String table = UserPreferencesMetaData.of(QContext.getQInstance()).getKeyValueTableName();
      for(String name : change.getValues().keySet())
      {
         QRecord existing = records.get(name);
         QRecord record = new QRecord().withValue("value", JsonUtils.toJson(change.getValues().get(name)));
         List<QRecord> written;
         if(existing == null)
         {
            record.withValue("userId", context.getUserId()).withValue("sectionName", context.getSection().getName()).withValue("preferenceName", name);
            written = new InsertAction().execute(new InsertInput(table).withRecords(List.of(record))).getRecords();
         }
         else
         {
            record.withValue("id", existing.getValue("id"));
            written = new UpdateAction().execute(new UpdateInput(table).withRecords(List.of(record))).getRecords();
         }
         for(QRecord saved : written)
         {
            if(CollectionUtils.nullSafeHasContents(saved.getErrors()))
            {
               throw (new QException("Could not store preference " + name + ": " + saved.getErrors()));
            }
         }
      }
   }



   /*******************************************************************************
    ** Identity predicates are always supplied by the trusted context.
    *******************************************************************************/
   private Map<String, QRecord> records(UserPreferenceRequestContext context) throws QException
   {
      String table = UserPreferencesMetaData.of(QContext.getQInstance()).getKeyValueTableName();
      List<QRecord> records = new QueryAction().execute(new QueryInput(table).withFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("userId", QCriteriaOperator.EQUALS, context.getUserId()))
         .withCriteria(new QFilterCriteria("sectionName", QCriteriaOperator.EQUALS, context.getSection().getName())))).getRecords();
      Map<String, QRecord> result = new LinkedHashMap<>();
      for(QRecord record : records)
      {
         result.put(record.getValueString("preferenceName"), record);
      }
      return (result);
   }
}
