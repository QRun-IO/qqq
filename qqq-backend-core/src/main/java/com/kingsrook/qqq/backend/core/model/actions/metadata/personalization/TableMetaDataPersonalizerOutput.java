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


import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 * Output for the action to personalize the table-metaData
 *
 *******************************************************************************/
public class TableMetaDataPersonalizerOutput extends AbstractActionOutput
{
   private QTableMetaData table;



   /*******************************************************************************
    * Getter for table
    * @see #withTable(QTableMetaData)
    *******************************************************************************/
   public QTableMetaData getTable()
   {
      return (this.table);
   }



   /*******************************************************************************
    * Setter for table
    * @see #withTable(QTableMetaData)
    *******************************************************************************/
   public void setTable(QTableMetaData table)
   {
      this.table = table;
   }



   /*******************************************************************************
    * Fluent setter for table
    *
    * @param table
    * Potentially a clone of the input table, "personalized" for the user making the
    * request.  Else, if not personalized, then should be the input table.
    * @return this
    *******************************************************************************/
   public TableMetaDataPersonalizerOutput withTable(QTableMetaData table)
   {
      this.table = table;
      return (this);
   }
}
