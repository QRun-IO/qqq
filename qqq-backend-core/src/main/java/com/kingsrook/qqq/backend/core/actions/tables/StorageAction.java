/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.io.InputStream;
import java.io.OutputStream;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;


/*******************************************************************************
 ** Action to do (generally, "mass") storage operations in a backend.
 **
 ** e.g., store a (potentially large) file - specifically - by working with it
 ** as either an InputStream or OutputStream.
 **
 ** May not be implemented in all backends.
 **
 *******************************************************************************/
public class StorageAction
{

   /*******************************************************************************
    ** create an output stream in the storage backend - that can be written to,
    ** for the purpose of inserting or writing a file into storage.
    *******************************************************************************/
   public OutputStream createOutputStream(StorageInput storageInput) throws QException
   {
      QBackendModuleInterface qBackendModuleInterface = preAction(storageInput);
      QStorageInterface       storageInterface        = qBackendModuleInterface.getStorageInterface();
      return (storageInterface.createOutputStream(storageInput));
   }



   /*******************************************************************************
    ** create an input stream in the storage backend - that can be read from,
    ** for the purpose of getting or reading a file from storage.
    *******************************************************************************/
   public InputStream getInputStream(StorageInput storageInput) throws QException
   {
      QBackendModuleInterface qBackendModuleInterface = preAction(storageInput);
      QStorageInterface       storageInterface        = qBackendModuleInterface.getStorageInterface();
      return (storageInterface.getInputStream(storageInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QBackendModuleInterface preAction(StorageInput storageInput) throws QException
   {
      ActionHelper.validateSession(storageInput);

      if(storageInput.getTableName() == null)
      {
         throw (new QException("Table name was not specified in storage input"));
      }

      QTableMetaData table = storageInput.getTable();
      if(table == null)
      {
         throw (new QException("A table named [" + storageInput.getTableName() + "] was not found in the active QInstance"));
      }

      QBackendMetaData         backend                  = storageInput.getBackend();
      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(backend);
      return (qModule);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void makePublic(StorageInput storageInput) throws QException
   {
      QBackendModuleInterface qBackendModuleInterface = preAction(storageInput);
      QStorageInterface       storageInterface        = qBackendModuleInterface.getStorageInterface();
      storageInterface.makePublic(storageInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getDownloadURL(StorageInput storageInput) throws QException
   {
      QBackendModuleInterface qBackendModuleInterface = preAction(storageInput);
      QStorageInterface       storageInterface        = qBackendModuleInterface.getStorageInterface();
      return (storageInterface.getDownloadURL(storageInput));
   }
}
