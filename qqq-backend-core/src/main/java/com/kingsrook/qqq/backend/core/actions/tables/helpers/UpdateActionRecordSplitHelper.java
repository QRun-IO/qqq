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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;


/*******************************************************************************
 ** Helper for backends that want to do their updates on records grouped by the
 ** set of fields that are being changed, and/or by the values those fields are
 ** being set to.
 **
 ** e.g., RDBMS, for n records where some sub-set of fields are all having values
 ** set the same (say, a status=x), we can do that as 1 query where id in (?,?,...,?).
 *******************************************************************************/
public class UpdateActionRecordSplitHelper
{
   private ListingHash<List<String>, QRecord> recordsByFieldBeingUpdated = new ListingHash<>();
   private boolean                            haveAnyWithoutErrors       = false;
   private List<QRecord>                      outputRecords              = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public void init(UpdateInput updateInput)
   {
      QTableMetaData table = updateInput.getTable();
      Instant        now   = Instant.now();

      for(QRecord record : updateInput.getRecords())
      {
         List<String> updatableFields = table.getFields().values().stream()
            .map(QFieldMetaData::getName)
            // todo - intent here is to avoid non-updateable fields - but this
            //  should be like based on field.isUpdatable once that attribute exists
            .filter(name -> !name.equals(table.getPrimaryKeyField()))
            .filter(name -> record.getValues().containsKey(name))
            .toList();
         recordsByFieldBeingUpdated.add(updatableFields, record);

         if(CollectionUtils.nullSafeIsEmpty(record.getErrors()))
         {
            haveAnyWithoutErrors = true;
         }

         //////////////////////////////////////////////////////////////////////////////
         // go ahead and put the record into the output list at this point in time,  //
         // so that the output list's order matches the input list order             //
         // note that if we want to capture updated values (like modify dates), then //
         // we may want a map of primary key to output record, for easy updating.    //
         //////////////////////////////////////////////////////////////////////////////
         QRecord outputRecord = new QRecord(record);
         outputRecords.add(outputRecord);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean areAllValuesBeingUpdatedTheSame(UpdateInput updateInput, List<QRecord> recordList, List<String> fieldsBeingUpdated)
   {
      if(updateInput.getAreAllValuesBeingUpdatedTheSame() != null)
      {
         ////////////////////////////////////////////////////////////
         // if input told us what value to use here, then trust it //
         ////////////////////////////////////////////////////////////
         return (updateInput.getAreAllValuesBeingUpdatedTheSame());
      }
      else
      {
         if(recordList.size() == 1)
         {
            //////////////////////////////////////////////////////
            // if a single record, then yes, that always counts //
            //////////////////////////////////////////////////////
            return (true);
         }

         ///////////////////////////////////////////////////////////////////////
         // else iterate over the records, comparing them to the first record //
         // return a false if any diffs are found. if no diffs, return true.  //
         ///////////////////////////////////////////////////////////////////////
         QRecord firstRecord = recordList.get(0);
         for(int i = 1; i < recordList.size(); i++)
         {
            QRecord record = recordList.get(i);

            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               ///////////////////////////////////////////////////////
               // skip records w/ errors (that we won't be updating //
               ///////////////////////////////////////////////////////
               continue;
            }

            for(String fieldName : fieldsBeingUpdated)
            {
               if(!Objects.equals(firstRecord.getValue(fieldName), record.getValue(fieldName)))
               {
                  return (false);
               }
            }
         }

         return (true);
      }
   }



   /*******************************************************************************
    ** Getter for haveAnyWithoutErrors
    **
    *******************************************************************************/
   public boolean getHaveAnyWithoutErrors()
   {
      return haveAnyWithoutErrors;
   }



   /*******************************************************************************
    ** Getter for recordsByFieldBeingUpdated
    **
    *******************************************************************************/
   public ListingHash<List<String>, QRecord> getRecordsByFieldBeingUpdated()
   {
      return recordsByFieldBeingUpdated;
   }



   /*******************************************************************************
    ** Getter for outputRecords
    **
    *******************************************************************************/
   public List<QRecord> getOutputRecords()
   {
      return outputRecords;
   }
}
