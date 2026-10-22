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

package com.kingsrook.qqq.backend.core.model.actions.metadata;


import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;


/*******************************************************************************
 * Output for a table-metaData action
 *
 *******************************************************************************/
public class TableMetaDataOutput extends AbstractActionOutput
{
   private QFrontendTableMetaData table;



   /*******************************************************************************
    ** Getter for table
    **
    *******************************************************************************/
   public QFrontendTableMetaData getTable()
   {
      return table;
   }



   /*******************************************************************************
    ** Setter for table
    **
    *******************************************************************************/
   public void setTable(QFrontendTableMetaData table)
   {
      this.table = table;
   }
}
