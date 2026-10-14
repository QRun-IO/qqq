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


import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectKeys;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectRequest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectResult;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectTarget;
import com.kingsrook.qqq.backend.core.jobs.effects.ReconcileResult;


/*******************************************************************************
 ** HTTP delivery with a stable provider idempotency key and conservative outcome reporting.
 *******************************************************************************/
public abstract class HttpEffectTarget implements EffectTarget
{
   private final HttpClient client = HttpClient.newHttpClient();



   /*******************************************************************************
    ** Subclasses supply the URL, method, body and any provider-specific authentication.
    *******************************************************************************/
   protected abstract HttpRequest.Builder buildRequest(EffectRequest request);



   /*******************************************************************************
    ** Maximum duration of a delivery request.
    *******************************************************************************/
   protected Duration timeout()
   {
      return (Duration.ofSeconds(30));
   }



   /*******************************************************************************
    ** Subclasses can extract the provider's delivery identifier.
    *******************************************************************************/
   protected String providerRef(HttpResponse<String> response)
   {
      return (null);
   }



   /*******************************************************************************
    ** Only explicit rejections or a failed connection are safe to retry as undelivered.
    *******************************************************************************/
   @Override
   public EffectResult deliver(EffectRequest request)
   {
      try
      {
         HttpRequest httpRequest = buildRequest(request)
            .setHeader("Idempotency-Key", EffectKeys.idempotencyKeyHeader(request.idempotencyKey()))
            .timeout(timeout()).build();
         HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
         int                  status   = response.statusCode();
         String               message  = "HTTP " + status;
         if((status >= 200 && status < 300) || status == 409)
         {
            return (EffectResult.sent(providerRef(response)));
         }
         if(status == 429 || status == 503)
         {
            return (EffectResult.retry(message));
         }
         if(status == 408 || status == 502 || status == 504)
         {
            return (EffectResult.unknown(message));
         }
         if(status >= 400 && status < 500)
         {
            return (EffectResult.failed(message));
         }
         return (EffectResult.unknown(message));
      }
      catch(ConnectException e)
      {
         return (EffectResult.retry(e.getMessage()));
      }
      catch(IOException e)
      {
         return (EffectResult.unknown(e.getMessage()));
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         return (EffectResult.unknown(e.getMessage()));
      }
   }



   /*******************************************************************************
    ** Delegate reconciliation to the provider-specific lookup.
    *******************************************************************************/
   @Override
   public ReconcileResult reconcile(EffectRequest request) throws Exception
   {
      return (lookup(request));
   }



   /*******************************************************************************
    ** Providers without a lookup must leave ambiguous deliveries for review.
    *******************************************************************************/
   protected ReconcileResult lookup(EffectRequest request) throws Exception
   {
      return (ReconcileResult.unknown("no lookup"));
   }
}
