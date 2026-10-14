/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


package com.kingsrook.qqq.backend.core.actions.tables;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.actions.interfaces.ConditionalUpdateInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;


/*******************************************************************************
 ** Runs an atomic conditional (compare-and-set) update against a table's backend.
 ** See ConditionalUpdateInterface. Throws if the backend lacks the capability.
 *******************************************************************************/
public class ConditionalUpdateAction
{

   /*******************************************************************************
    ** Run the update; return the number of rows updated.
    *******************************************************************************/
   public int execute(ConditionalUpdateInput input) throws QException
   {
      QTableMetaData table = input.getTable();
      if(table == null)
      {
         throw (new QException("Unrecognized table name: " + input.getTableName()));
      }
      if(input.getFilter() == null || !input.getFilter().hasAnyCriteria())
      {
         throw (new QException("A conditional update requires a filter with criteria."));
      }
      if(input.getValues() == null || input.getValues().isEmpty())
      {
         throw (new QException("A conditional update requires at least one value to set."));
      }
      for(String fieldName : input.getValues().keySet())
      {
         table.getField(fieldName);
      }

      return (getInterface(input.getBackend()).execute(input));
   }



   /*******************************************************************************
    ** The backend clock for the backend that holds a table.
    *******************************************************************************/
   public Instant databaseNow(String tableName) throws QException
   {
      QBackendMetaData backend = QContext.getQInstance().getBackendForTable(tableName);
      return (getInterface(backend).databaseNow(backend));
   }



   /*******************************************************************************
    ** Whether the backend that holds a table supports conditional updates.
    *******************************************************************************/
   public static boolean isSupported(QBackendMetaData backend)
   {
      try
      {
         QBackendModuleInterface module = new QBackendModuleDispatcher().getQBackendModule(backend);
         return (module.getConditionalUpdateInterface() != null);
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ConditionalUpdateInterface getInterface(QBackendMetaData backend) throws QException
   {
      QBackendModuleInterface    module = new QBackendModuleDispatcher().getQBackendModule(backend);
      ConditionalUpdateInterface cui    = module.getConditionalUpdateInterface();
      if(cui == null)
      {
         throw (new QException("Backend [" + backend.getName() + "] does not support conditional updates (CONDITIONAL_UPDATE)."));
      }
      return (cui);
   }

}
