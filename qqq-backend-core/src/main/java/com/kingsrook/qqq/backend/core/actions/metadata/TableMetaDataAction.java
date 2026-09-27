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

package com.kingsrook.qqq.backend.core.actions.metadata;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Action to fetch meta-data for a table.
 **
 *******************************************************************************/
public class TableMetaDataAction
{


   /*******************************************************************************
    **
    *******************************************************************************/
   public TableMetaDataOutput execute(TableMetaDataInput tableMetaDataInput) throws QException
   {
      ActionHelper.validateSession(tableMetaDataInput);
      TableMetaDataOutput tableMetaDataOutput = new TableMetaDataOutput();

      QTableMetaData table = QContext.getQInstance().getTable(tableMetaDataInput.getTableName());
      if(table == null)
      {
         throw (new QNotFoundException("Table [" + tableMetaDataInput.getTableName() + "] was not found."));
      }
      table = TableMetaDataPersonalizerAction.execute(tableMetaDataInput);

      QBackendMetaData backendForTable = QContext.getQInstance().getBackendForTable(table.getName());
      tableMetaDataOutput.setTable(new QFrontendTableMetaData(tableMetaDataInput, backendForTable, table, true, true));

      Optional<TableCustomizerInterface> postMetaDataCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.POST_META_DATA_ACTION.getRole());
      if(postMetaDataCustomizer.isPresent())
      {
         postMetaDataCustomizer.get().postMetaDataAction(tableMetaDataInput, tableMetaDataOutput);
      }

      return tableMetaDataOutput;
   }
}
