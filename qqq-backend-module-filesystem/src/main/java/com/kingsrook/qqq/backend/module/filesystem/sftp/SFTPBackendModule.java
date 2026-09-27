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

package com.kingsrook.qqq.backend.module.filesystem.sftp;


import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.InsertInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemBackendModuleInterface;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.AbstractSFTPAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.SFTPCountAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.SFTPDeleteAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.SFTPInsertAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.SFTPQueryAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.SFTPStorageAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.SFTPUpdateAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.SFTPDirEntryWithPath;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPTableBackendDetails;


/*******************************************************************************
 ** QQQ Backend module for working with SFTP filesystems (as a client)
 *******************************************************************************/
public class SFTPBackendModule implements QBackendModuleInterface, FilesystemBackendModuleInterface
{
   public static final String BACKEND_TYPE = "sftp";

   static
   {
      QBackendModuleDispatcher.registerBackendModule(new SFTPBackendModule());
   }

   /*******************************************************************************
    ** For filesystem backends, get the module-specific action base-class, that helps
    ** with functions like listing and deleting files.
    *******************************************************************************/
   @Override
   public AbstractBaseFilesystemAction<SFTPDirEntryWithPath> getActionBase()
   {
      return (new AbstractSFTPAction());
   }



   /*******************************************************************************
    ** Method where a backend module must be able to provide its type (name).
    *******************************************************************************/
   @Override
   public String getBackendType()
   {
      return (BACKEND_TYPE);
   }



   /*******************************************************************************
    ** Method to identify the class used for backend meta data for this module.
    *******************************************************************************/
   @Override
   public Class<? extends QBackendMetaData> getBackendMetaDataClass()
   {
      return (SFTPBackendMetaData.class);
   }



   /*******************************************************************************
    ** Method to identify the class used for table-backend details for this module.
    *******************************************************************************/
   @Override
   public Class<? extends QTableBackendDetails> getTableBackendDetailsClass()
   {
      return (SFTPTableBackendDetails.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueryInterface getQueryInterface()
   {
      return new SFTPQueryAction();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public CountInterface getCountInterface()
   {
      return new SFTPCountAction();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public InsertInterface getInsertInterface()
   {
      return (new SFTPInsertAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public UpdateInterface getUpdateInterface()
   {
      return (new SFTPUpdateAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public DeleteInterface getDeleteInterface()
   {
      return (new SFTPDeleteAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QStorageInterface getStorageInterface()
   {
      return new SFTPStorageAction();
   }

}
