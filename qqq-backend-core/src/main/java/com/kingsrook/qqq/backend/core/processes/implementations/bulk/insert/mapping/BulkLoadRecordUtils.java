/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Utility methods for working with records in a bulk-load.
 **
 ** Originally added for working with backendDetails around the source rows.
 *******************************************************************************/
public class BulkLoadRecordUtils
{

   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public static QRecord addBackendDetailsAboutFileRows(QRecord record, BulkLoadFileRow fileRow)
   {
      return (addBackendDetailsAboutFileRows(record, new ArrayList<>(List.of(fileRow))));
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public static QRecord addBackendDetailsAboutFileRows(QRecord record, ArrayList<BulkLoadFileRow> fileRows)
   {
      if(CollectionUtils.nullSafeHasContents(fileRows))
      {
         Integer firstRowNo = fileRows.get(0).getRowNo();
         Integer lastRowNo  = fileRows.get(fileRows.size() - 1).getRowNo();

         if(Objects.equals(firstRowNo, lastRowNo))
         {
            record.addBackendDetail("rowNos", "Row " + firstRowNo);
         }
         else
         {
            record.addBackendDetail("rowNos", "Rows " + firstRowNo + "-" + lastRowNo);
         }
      }
      else
      {
         record.addBackendDetail("rowNos", "Rows ?");
      }

      record.addBackendDetail("fileRows", fileRows);
      return (record);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static String getRowNosString(QRecord record)
   {
      return (record.getBackendDetailString("rowNos"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @SuppressWarnings("unchecked")
   public static ArrayList<BulkLoadFileRow> getFileRows(QRecord record)
   {
      return (ArrayList<BulkLoadFileRow>) record.getBackendDetail("fileRows");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static List<Integer> getFileRowNos(QRecord record)
   {
      return (getFileRows(record).stream().map(row -> row.getRowNo()).toList());
   }

}
