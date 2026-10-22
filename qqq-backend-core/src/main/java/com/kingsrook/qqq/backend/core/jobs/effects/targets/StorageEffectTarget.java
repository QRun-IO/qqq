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


package com.kingsrook.qqq.backend.core.jobs.effects.targets;


import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectKeys;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectRequest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectResult;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectTarget;
import com.kingsrook.qqq.backend.core.jobs.effects.ReconcileResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;


/*******************************************************************************
 ** An idempotent external effect that streams content to a deterministic storage object.
 *******************************************************************************/
public class StorageEffectTarget implements EffectTarget
{
   /*******************************************************************************
    ** Repeated deliveries overwrite the same reference, without using the filesystem.
    *******************************************************************************/
   @Override
   public EffectResult deliver(EffectRequest request)
   {
      try
      {
         StorageInput input   = storageInput(request);
         String       content = (String) request.payload().get("content");
         try(InputStream source = content == null
            ? Base64.getDecoder().wrap(new ByteArrayInputStream(((String) request.payload().get("contentBase64")).getBytes(StandardCharsets.US_ASCII)))
            : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            OutputStream destination = new StorageAction().createOutputStream(input))
         {
            source.transferTo(destination);
         }
         return (EffectResult.sent(input.getReference()));
      }
      catch(Exception e)
      {
         return (EffectResult.unknown(e.getMessage()));
      }
   }



   /*******************************************************************************
    ** Opening an object proves it exists; only an explicit missing-object error proves absence.
    *******************************************************************************/
   @Override
   public ReconcileResult reconcile(EffectRequest request)
   {
      try
      {
         StorageInput input = storageInput(request);
         try(InputStream stream = new StorageAction().getInputStream(input))
         {
            return (stream == null ? ReconcileResult.unknown("Storage returned no stream") : ReconcileResult.found(input.getReference()));
         }
      }
      catch(Exception e)
      {
         for(Throwable cause = e; cause != null; cause = cause.getCause())
         {
            if(cause instanceof QNotFoundException || cause instanceof FileNotFoundException)
            {
               return (ReconcileResult.notFound());
            }
         }
         return (ReconcileResult.unknown(e.getMessage()));
      }
   }



   /*******************************************************************************
    ** Use the same deterministic reference for delivery and reconciliation.
    *******************************************************************************/
   private StorageInput storageInput(EffectRequest request)
   {
      String tableName = (String) request.payload().get("storageTableName");
      if(tableName == null || tableName.isBlank())
      {
         throw (new IllegalArgumentException("storageTableName is required"));
      }
      return (new StorageInput(tableName).withReference(EffectKeys.objectName((String) request.payload().get("prefix"),
         request.idempotencyKey(), (String) request.payload().get("extension"))));
   }
}
