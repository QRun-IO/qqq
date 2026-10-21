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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.enumeration;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEnum;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.utils.BackendQueryFilterUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class EnumerationQueryAction implements QueryInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueryOutput execute(QueryInput queryInput) throws QException
   {
      try
      {
         QTableMetaData                 table          = queryInput.getTable();
         EnumerationTableBackendDetails backendDetails = (EnumerationTableBackendDetails) table.getBackendDetails();
         Class<? extends QRecordEnum>   enumClass      = backendDetails.getEnumClass();
         QRecordEnum[]                  values         = (QRecordEnum[]) enumClass.getMethod("values").invoke(null);

         //////////////////////////////////////////////
         // note - not good streaming behavior here. //
         //////////////////////////////////////////////

         List<QRecord> recordList = new ArrayList<>();
         for(QRecordEnum value : values)
         {
            QRecord record        = value.toQRecord();
            boolean recordMatches = BackendQueryFilterUtils.doesRecordMatch(queryInput.getFilter(), record);
            if(recordMatches)
            {
               recordList.add(record);
            }
         }

         BackendQueryFilterUtils.sortRecordList(queryInput.getFilter(), recordList);
         recordList = BackendQueryFilterUtils.applySkipAndLimit(queryInput.getFilter(), recordList);

         QueryOutput queryOutput = new QueryOutput(queryInput);
         queryOutput.addRecords(recordList);
         return queryOutput;
      }
      catch(Exception e)
      {
         throw (new QException("Error executing query", e));
      }
   }

}
