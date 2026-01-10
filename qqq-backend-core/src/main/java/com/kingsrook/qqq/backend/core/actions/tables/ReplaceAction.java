/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyHelper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 ** Action to do a "replace" - e.g: Update rows with unique-key values that are
 ** already in the table; insert rows whose unique keys weren't already in the
 ** table, and delete rows that weren't in the input (all based on a
 ** UniqueKey that's part of the input)
 **
 ** Note - the filter in the ReplaceInput - its role is to limit what rows are
 ** potentially deleted.  e.g., if you have a table that's segmented, and you're
 ** only replacing a particular segment of it (say, for 1 client), then you pass
 ** in a filter that finds  rows matching that segment.  See Test for example.
 *******************************************************************************/
public class ReplaceAction extends AbstractQActionFunction<ReplaceInput, ReplaceOutput>
{
   private static final QLogger LOG = QLogger.getLogger(ReplaceAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ReplaceOutput execute(ReplaceInput input) throws QException
   {
      ReplaceOutput output = new ReplaceOutput();

      QBackendTransaction transaction         = input.getTransaction();
      boolean             weOwnTheTransaction = false;

      try
      {
         QTableMetaData table                     = input.getTable();
         UniqueKey      uniqueKey                 = input.getKey();
         String         primaryKeyField           = table.getPrimaryKeyField();
         boolean        allowNullKeyValuesToEqual = BooleanUtils.isTrue(input.getAllowNullKeyValuesToEqual());

         if(transaction == null)
         {
            transaction = QBackendTransaction.openFor(new InsertInput(input.getTableName()));
            weOwnTheTransaction = true;
         }

         List<QRecord>      insertList        = new ArrayList<>();
         List<QRecord>      updateList        = new ArrayList<>();
         List<Serializable> primaryKeysToKeep = new ArrayList<>();

         for(List<QRecord> page : CollectionUtils.getPages(input.getRecords(), 1000))
         {
            ///////////////////////////////////////////////////////////////////////////////////
            // originally it was thought that we'd need to pass the filter in here           //
            // but, it's been decided not to.  the filter only applies to what we can delete //
            ///////////////////////////////////////////////////////////////////////////////////
            Map<List<Serializable>, Serializable> existingKeys = UniqueKeyHelper.getExistingKeys(transaction, table, page, uniqueKey, allowNullKeyValuesToEqual);

            for(QRecord record : page)
            {
               Optional<List<Serializable>> keyValues = UniqueKeyHelper.getKeyValues(table, uniqueKey, record, allowNullKeyValuesToEqual);
               if(keyValues.isPresent())
               {
                  if(existingKeys.containsKey(keyValues.get()))
                  {
                     Serializable primaryKey = existingKeys.get(keyValues.get());
                     record.setValue(primaryKeyField, primaryKey);
                     updateList.add(record);
                     primaryKeysToKeep.add(primaryKey);
                  }
                  else
                  {
                     insertList.add(record);
                  }
               }
            }
         }

         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(table.getName());
         insertInput.setRecords(insertList);
         insertInput.withFlags(input.getFlags());
         insertInput.setTransaction(transaction);
         insertInput.setOmitDmlAudit(input.getOmitDmlAudit());
         InsertOutput insertOutput = new InsertAction().execute(insertInput);
         primaryKeysToKeep.addAll(insertOutput.getRecords().stream().map(r -> r.getValue(primaryKeyField)).toList());
         output.setInsertOutput(insertOutput);

         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(table.getName());
         updateInput.setRecords(updateList);
         updateInput.withFlags(input.getFlags());
         updateInput.setTransaction(transaction);
         updateInput.setOmitDmlAudit(input.getOmitDmlAudit());
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         output.setUpdateOutput(updateOutput);

         if(input.getPerformDeletes())
         {
            QQueryFilter deleteFilter = new QQueryFilter(new QFilterCriteria(primaryKeyField, QCriteriaOperator.NOT_IN, primaryKeysToKeep));
            if(input.getFilter() != null)
            {
               deleteFilter.addSubFilter(input.getFilter());
            }

            DeleteInput deleteInput = new DeleteInput();
            deleteInput.setTableName(table.getName());
            deleteInput.setQueryFilter(deleteFilter);
            deleteInput.withFlags(input.getFlags());
            deleteInput.setTransaction(transaction);
            deleteInput.setOmitDmlAudit(input.getOmitDmlAudit());
            DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);
            output.setDeleteOutput(deleteOutput);
         }

         if(input.getSetPrimaryKeyInInsertedRecords())
         {
            for(int i = 0; i < insertList.size(); i++)
            {
               if(i < insertOutput.getRecords().size())
               {
                  insertList.get(i).setValue(primaryKeyField, insertOutput.getRecords().get(i).getValue(primaryKeyField));
               }
            }
         }

         if(weOwnTheTransaction)
         {
            transaction.commit();
         }

         return (output);
      }
      catch(Exception e)
      {
         if(weOwnTheTransaction)
         {
            LOG.warn("Caught top-level ReplaceAction exception - rolling back exception", e);
            transaction.rollback();
         }
         throw (new QException("Error executing replace action", e));
      }
      finally
      {
         if(weOwnTheTransaction)
         {
            transaction.close();
         }
      }
   }
}
