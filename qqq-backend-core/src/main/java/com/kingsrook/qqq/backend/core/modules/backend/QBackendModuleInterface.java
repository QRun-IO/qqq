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

package com.kingsrook.qqq.backend.core.modules.backend;


import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.GetInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.InsertInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;


/*******************************************************************************
 ** Interface that a QBackendModule must implement.
 **
 ** Note, all methods have a default version, which throws a 'not implemented'
 ** exception.
 **
 *******************************************************************************/
public interface QBackendModuleInterface
{
   /*******************************************************************************
    ** Method where a backend module must be able to provide its type (name).
    *******************************************************************************/
   String getBackendType();

   /*******************************************************************************
    ** Method to identify the class used for backend meta data for this module.
    *******************************************************************************/
   default Class<? extends QBackendMetaData> getBackendMetaDataClass()
   {
      return (QBackendMetaData.class);
   }



   /*******************************************************************************
    ** Capabilities this module can never provide (for example, writes on a
    ** read-only module).  The instance enricher disables them on every backend of
    ** this type, unless the backend explicitly enables them, so tables, their
    ** generated bulk processes, permissions and frontends all agree.
    *******************************************************************************/
   default Set<Capability> getUnsupportedCapabilities()
   {
      return (Set.of());
   }

   /*******************************************************************************
    ** Method to identify the class used for table-backend details for this module.
    *******************************************************************************/
   default Class<? extends QTableBackendDetails> getTableBackendDetailsClass()
   {
      return QTableBackendDetails.class;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default CountInterface getCountInterface()
   {
      throwNotImplemented("Count");
      return null;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default QueryInterface getQueryInterface()
   {
      throwNotImplemented("Query");
      return null;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default GetInterface getGetInterface()
   {
      throwNotImplemented("Get");
      return null;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default InsertInterface getInsertInterface()
   {
      throwNotImplemented("Insert");
      return null;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default UpdateInterface getUpdateInterface()
   {
      throwNotImplemented("Update");
      return null;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default DeleteInterface getDeleteInterface()
   {
      throwNotImplemented("Delete");
      return null;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default AggregateInterface getAggregateInterface()
   {
      throwNotImplemented("Aggregate");
      return null;
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default QStorageInterface getStorageInterface()
   {
      throwNotImplemented("StorageInterface");
      return null;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default QBackendTransaction openTransaction(AbstractTableActionInput input) throws QException
   {
      return (new QBackendTransaction());
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   private void throwNotImplemented(String actionName)
   {
      throw new IllegalStateException(actionName + " is not implemented in this module: " + this.getClass().getSimpleName());
   }

}
