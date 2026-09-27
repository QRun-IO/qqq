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

package com.kingsrook.qqq.backend.module.filesystem.local.actions;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.jetbrains.annotations.NotNull;


/*******************************************************************************
 ** (mass, streamed) storage action for filesystem module
 *******************************************************************************/
public class FilesystemStorageAction extends AbstractFilesystemAction implements QStorageInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public OutputStream createOutputStream(StorageInput storageInput) throws QException
   {
      try
      {
         String fullPath = getFullPath(storageInput);
         File   file     = new File(fullPath);
         if(!file.getParentFile().exists())
         {
            if(!file.getParentFile().mkdirs())
            {
               throw (new QException("Could not make directory required for storing file: " + fullPath));
            }
         }

         return (new FileOutputStream(fullPath));
      }
      catch(IOException e)
      {
         throw (new QException("IOException creating output stream for file", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @NotNull
   private String getFullPath(StorageInput storageInput) throws IOException, QBadRequestException
   {
      QTableMetaData table = storageInput.getTable();
      QBackendMetaData backend = storageInput.getBackend();
      File base = new File(getFullBasePath(table, backend)).getCanonicalFile();
      String reference = storageInput.getReference();
      if(reference == null || reference.isBlank() || new File(reference).isAbsolute())
      {
         throw new QBadRequestException("Storage reference must be a relative file path");
      }
      File file = new File(base, reference).getCanonicalFile();
      if(file.equals(base) || !file.toPath().startsWith(base.toPath()))
      {
         throw new QBadRequestException("Storage reference must remain inside its table directory");
      }
      return file.getPath();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public InputStream getInputStream(StorageInput storageInput) throws QException
   {
      try
      {
         return (new FileInputStream(getFullPath(storageInput)));
      }
      catch(IOException e)
      {
         throw (new QException("IOException getting input stream for file", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getDownloadURL(StorageInput storageInput) throws QException
   {
      try
      {
         return (Path.of(getFullPath(storageInput)).toUri().toASCIIString());
      }
      catch(IOException e)
      {
         throw new QException("Could not resolve storage download path", e);
      }
   }

}
