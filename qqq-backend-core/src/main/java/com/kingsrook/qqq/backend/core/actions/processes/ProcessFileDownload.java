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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Authorize server-produced files for the current application and session.
 ** Registration must occur in trusted process code, never from HTTP input values.
 ** Grants expire after 30 minutes and are bounded to 10,000 active files.
 *******************************************************************************/
public final class ProcessFileDownload
{
   private static final Cache<DownloadKey, Boolean> AUTHORIZED_FILES = CacheBuilder.newBuilder()
      .expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10_000).build();



   /*******************************************************************************
    **
    *******************************************************************************/
   private ProcessFileDownload()
   {
   }



   /*******************************************************************************
    ** Grant access to an existing process output and return its canonical path.
    *******************************************************************************/
   public static String register(File file) throws QException
   {
      try
      {
         String path = file.getCanonicalPath();
         if(!file.isFile())
         {
            throw new QException("The process download must be an existing file");
         }
         AUTHORIZED_FILES.put(key(null, path), true);
         return path;
      }
      catch(IOException e)
      {
         throw new QException("Could not register process download", e);
      }
   }



   /*******************************************************************************
    ** Open only an exact registered path belonging to the active session.
    *******************************************************************************/
   public static InputStream open(String path) throws QException
   {
      if(AUTHORIZED_FILES.getIfPresent(key(null, path)) == null)
      {
         throw new QPermissionDeniedException("This file is not an authorized process download");
      }
      try
      {
         return new FileInputStream(path);
      }
      catch(IOException e)
      {
         throw new QException("Could not open process download", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void registerStorage(StorageInput input) throws QAuthenticationException
   {
      AUTHORIZED_FILES.put(key(input.getTableName(), input.getReference()), true);
   }



   /*******************************************************************************
    ** A trusted process may grant its exact output without granting table-wide READ.
    *******************************************************************************/
   public static boolean isStorageRegistered(StorageInput input) throws QAuthenticationException
   {
      return AUTHORIZED_FILES.getIfPresent(key(input.getTableName(), input.getReference())) != null;
   }



   /*******************************************************************************
    ** Authentication references survive per-request session reconstruction.
    ** Do not log this key: an authentication reference can contain a credential.
    *******************************************************************************/
   private static DownloadKey key(String tableName, String path) throws QAuthenticationException
   {
      QSession session = QContext.getQSession();
      if(QContext.getQInstance() == null || session == null || session.getUuid() == null)
      {
         throw new QAuthenticationException("A session is required for process downloads");
      }
      String sessionKey = StringUtils.hasContent(session.getIdReference()) ? "reference:" + session.getIdReference() : "uuid:" + session.getUuid();
      Map<String, Serializable> variants = session.getBackendVariants() == null ? Map.of() : new HashMap<>(session.getBackendVariants());
      return new DownloadKey(QContext.getQInstance(), sessionKey, variants, tableName, path);
   }



   /*******************************************************************************
    ** Metadata identity keeps separate application instances isolated.
    *******************************************************************************/
   private record DownloadKey(QInstance instance, String sessionKey, Map<String, Serializable> variants, String tableName, String path)
   {
   }
}
