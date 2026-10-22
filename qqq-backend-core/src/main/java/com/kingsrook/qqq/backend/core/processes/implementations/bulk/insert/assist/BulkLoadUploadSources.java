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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** The registered {@link BulkLoadUploadSource} (one per server).
 *******************************************************************************/
public final class BulkLoadUploadSources
{
   public static final String NOT_CONFIGURED_MESSAGE = "File uploads are not configured on this server.";

   private static volatile BulkLoadUploadSource source;



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private BulkLoadUploadSources()
   {
   }



   /*******************************************************************************
    ** Register the upload source (replacing any earlier one).
    *******************************************************************************/
   public static void register(BulkLoadUploadSource uploadSource)
   {
      source = uploadSource;
   }



   /*******************************************************************************
    ** The registered source, or null.
    *******************************************************************************/
   public static BulkLoadUploadSource get()
   {
      return (source);
   }



   /*******************************************************************************
    ** Forget the registered source (tests).
    *******************************************************************************/
   public static void clear()
   {
      source = null;
   }



   /*******************************************************************************
    ** Open an upload through the registered source, or fail with a user-facing
    ** error when there is none or no ref was given.
    *******************************************************************************/
   public static StorageInput open(String uploadRef) throws QException
   {
      if(!StringUtils.hasContent(uploadRef))
      {
         throw (new QUserFacingException("An uploadRef is required."));
      }

      BulkLoadUploadSource current = source;
      if(current == null)
      {
         throw (new QUserFacingException(NOT_CONFIGURED_MESSAGE));
      }
      return (current.open(uploadRef));
   }
}
