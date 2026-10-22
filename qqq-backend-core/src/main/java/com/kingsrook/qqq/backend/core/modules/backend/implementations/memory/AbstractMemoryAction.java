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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Base class for all core actions in the Memory backend module.
 *******************************************************************************/
public abstract class AbstractMemoryAction
{

   /*******************************************************************************
    ** If the table has a field with the given name, then set the given value in the
    ** given record - flag added to control overwriting value.
    *******************************************************************************/
   protected void setValueIfTableHasField(QRecord record, QTableMetaData table, String fieldName, Serializable value, boolean overwriteIfSet)
   {
      try
      {
         if(table.getFields().containsKey(fieldName))
         {
            ///////////////////////////////////////////////////////////////////////
            // always set value if boolean to overwrite is true, otherwise,      //
            // only set the value if there is currently no content for the field //
            ///////////////////////////////////////////////////////////////////////
            if(overwriteIfSet || !StringUtils.hasContent(record.getValueString(fieldName)))
            {
               record.setValue(fieldName, value);
            }
         }
      }
      catch(Exception e)
      {
         /////////////////////////////////////////////////
         // this means field doesn't exist, so, ignore. //
         /////////////////////////////////////////////////
      }
   }

}

