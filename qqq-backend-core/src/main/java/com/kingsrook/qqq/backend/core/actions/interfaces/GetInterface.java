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

package com.kingsrook.qqq.backend.core.actions.interfaces;


import java.util.HashSet;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Interface for the Get action.
 **
 *******************************************************************************/
public interface GetInterface
{
   /*******************************************************************************
    **
    *******************************************************************************/
   GetOutput execute(GetInput getInput) throws QException;

   /*******************************************************************************
    **
    *******************************************************************************/
   default void validateInput(GetInput getInput) throws QException
   {
      if(getInput.getPrimaryKey() != null & getInput.getUniqueKey() != null)
      {
         throw new QException("A GetInput may not contain both a primary key [" + getInput.getPrimaryKey() + "] and unique key [" + getInput.getUniqueKey() + "]");
      }

      if(getInput.getUniqueKey() != null)
      {
         QTableMetaData table      = getInput.getTable();
         boolean        foundMatch = false;
         for(UniqueKey uniqueKey : CollectionUtils.nonNullList(table.getUniqueKeys()))
         {
            if(new HashSet<>(uniqueKey.getFieldNames()).equals(getInput.getUniqueKey().keySet()))
            {
               foundMatch = true;
               break;
            }
         }

         if(!foundMatch)
         {
            throw new QException("Table [" + table.getName() + "] does not have a unique key defined on fields: " + getInput.getUniqueKey().keySet().stream().sorted().toList());
         }
      }
   }
}
