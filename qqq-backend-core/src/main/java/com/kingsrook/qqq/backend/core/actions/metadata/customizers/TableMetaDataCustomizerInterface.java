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

package com.kingsrook.qqq.backend.core.actions.metadata.customizers;


import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public interface TableMetaDataCustomizerInterface
{
   QLogger LOG = QLogger.getLogger(TableMetaDataCustomizerInterface.class);

   /*******************************************************************************
    ** custom actions to run after a query (or get!) takes place.
    **
    *******************************************************************************/
   default void preExecute(TableMetaDataInput tableMetaDataInput, TableMetaDataOutput tableMetaDataOutput)
   {
      LOG.info("A default implementation of preExecute is running...  Probably not expected!", logPair("tableName", tableMetaDataInput.getTableName()));
   }


   /*******************************************************************************
    ** custom actions before an insert takes place.
    **
    *******************************************************************************/
   default void postExecute(TableMetaDataInput tableMetaDataInput, TableMetaDataOutput tableMetaDataOutput)
   {
      LOG.info("A default implementation of postExecute is running...  Probably not expected!", logPair("tableName", tableMetaDataInput.getTableName()));
   }
}
