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
