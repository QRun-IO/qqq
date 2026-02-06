/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

      Optional<TableCustomizerInterface> postMetaDataCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.POST_META_DATA.getRole());
      if(postMetaDataCustomizer.isPresent())
      {
         postMetaDataCustomizer.get().postMetaData(tableMetaDataInput, tableMetaDataOutput);
      }

      return tableMetaDataOutput;
   }
}
