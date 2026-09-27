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

package com.kingsrook.qqq.backend.module.api.actions;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.utils.BackendQueryFilterUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class APIQueryAction extends AbstractAPIAction implements QueryInterface
{
   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutput execute(QueryInput queryInput) throws QException
   {
      QTableMetaData table = queryInput.getTable();
      preAction(queryInput);
      return (apiActionUtil.doQuery(table, queryInput));
   }



   /*******************************************************************************
    ** Complete key visibility must be supplied by the configured provider utility.
    *******************************************************************************/
   @Override
   public List<QRecord> lookupUniqueKey(UniqueKeyLookup.Input input) throws QException
   {
      preAction(input.newQueryInput());
      return apiActionUtil.lookupUniqueKey(input);
   }



   /*******************************************************************************
    ** The configured provider must explicitly support complete stored parent values.
    *******************************************************************************/
   @Override
   public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
   {
      preAction(input.newQueryInput());
      return apiActionUtil.readAssociationValues(input);
   }



   /*******************************************************************************
    ** API filter syntax is application-specific, so read all pages and match locally.
    *******************************************************************************/
   @Override
   public List<Serializable> findAssociatedPrimaryKeys(AssociatedRecordDiscovery.Input input) throws QException
   {
      QueryInput queryInput = input.newQueryInput();
      QQueryFilter filter = queryInput.getFilter();
      queryInput.setFilter(new QQueryFilter());
      queryInput.setFieldNamesToInclude(null);
      List<Serializable> keys = new ArrayList<>();
      for(QRecord record : execute(queryInput).getRecords())
      {
         if(BackendQueryFilterUtils.doesRecordMatch(filter, record))
         {
            keys.add(record.getValue(queryInput.getTable().getPrimaryKeyField()));
         }
      }
      return keys;
   }

}
