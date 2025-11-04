/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.backends;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility class for accessing QQQBackend records (well, just their ids at this time)
 ** Takes care of inserting upon a miss, and dealing with the cache table.
 *******************************************************************************/
public class QQQBackendTableManager
{
   private static final QLogger LOG = QLogger.getLogger(QQQBackendTableManager.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Integer getQQQBackendId(QInstance qInstance, String backendName) throws QException
   {
      /////////////////////////////
      // look in the cache table //
      /////////////////////////////
      GetInput getInput = new GetInput();
      getInput.setTableName(QQQBackendsMetaDataProvider.QQQ_BACKEND_CACHE_TABLE_NAME);
      getInput.setUniqueKey(MapBuilder.of("name", backendName));
      GetOutput getOutput = new GetAction().execute(getInput);

      ////////////////////////
      // upon cache miss... //
      ////////////////////////
      if(getOutput.getRecord() == null)
      {
         QBackendMetaData backendMetaData = qInstance.getBackend(backendName);
         if(backendMetaData == null)
         {
            LOG.info("No such backend", logPair("backendName", backendName));
            return (null);
         }

         ///////////////////////////////////////////////////////
         // insert the record (into the table, not the cache) //
         ///////////////////////////////////////////////////////
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(QQQBackend.TABLE_NAME);
         insertInput.setRecords(List.of(new QRecord().withValue("name", backendName).withValue("label", backendMetaData.getName())));
         InsertOutput insertOutput = new InsertAction().execute(insertInput);

         ///////////////////////////////////
         // repeat the get from the cache //
         ///////////////////////////////////
         getOutput = new GetAction().execute(getInput);
      }

      return getOutput.getRecord().getValueInteger("id");
   }
}
