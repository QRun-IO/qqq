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

package com.kingsrook.qqq.backend.core.model.actions.metadata.personalization;


import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Input for the action to personalize the meta-data for a table.
 **
 *******************************************************************************/
public class TableMetaDataPersonalizerInput extends AbstractTableActionInput
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public TableMetaDataPersonalizerInput()
   {
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public TableMetaDataPersonalizerInput withTableName(String tableName)
   {
      super.withTableName(tableName);
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public TableMetaDataPersonalizerInput withTableMetaData(QTableMetaData tableMetaData)
   {
      super.withTableMetaData(tableMetaData);
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public TableMetaDataPersonalizerInput withInputSource(InputSource inputSource)
   {
      super.withInputSource(inputSource);
      return (this);
   }
}
