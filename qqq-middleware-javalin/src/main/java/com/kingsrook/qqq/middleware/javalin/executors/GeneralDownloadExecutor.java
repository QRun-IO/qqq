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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.net.URLConnection;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.GeneralDownloadInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.GeneralDownloadOutputInterface;


/*******************************************************************************
 ** Executor for the general file download endpoint. Resolves a file from
 ** server-side temp storage (either filesystem path or storage table reference)
 ** and provides the content for download.
 *******************************************************************************/
public class GeneralDownloadExecutor extends AbstractMiddlewareExecutor<GeneralDownloadInput, GeneralDownloadOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(GeneralDownloadExecutor.class);



   /***************************************************************************
    ** Serve a file a process made available for download, exactly as the legacy
    ** download route does: a server file only when this session was granted it
    ** (ProcessFileDownload), a storage file only when its table and reference were
    ** registered for this session.
    ***************************************************************************/
   @Override
   public void execute(GeneralDownloadInput input, GeneralDownloadOutputInterface output) throws QException
   {
      try
      {
         String filename = input.getFile();
         output.setFilename(filename);

         ///////////////////////////////////////////////////////////////
         // infer content type from the filename extension if present //
         ///////////////////////////////////////////////////////////////
         if(StringUtils.hasContent(filename))
         {
            String contentType = URLConnection.guessContentTypeFromName(filename);
            if(StringUtils.hasContent(contentType))
            {
               output.setContentType(contentType);
            }
         }

         String filePath         = input.getFilePath();
         String storageTableName = input.getStorageTableName();
         String storageReference = input.getStorageReference();

         if(StringUtils.hasContent(filePath))
         {
            output.setInputStream(ProcessFileDownload.open(filePath));
         }
         else if(StringUtils.hasContent(storageTableName) && StringUtils.hasContent(storageReference))
         {
            StorageInput storageInput = new StorageInput(storageTableName).withReference(storageReference);
            if(!ProcessFileDownload.isStorageRegistered(storageInput))
            {
               throw (new QPermissionDeniedException("This file is not an authorized process download"));
            }
            output.setInputStream(new StorageAction().getInputStream(storageInput));
         }
         else
         {
            throw (new QBadRequestException("Missing query parameters to identify file to download"));
         }
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error executing file download", e));
      }
   }

}
