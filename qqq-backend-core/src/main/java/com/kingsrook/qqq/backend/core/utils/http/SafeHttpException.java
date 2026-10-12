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

package com.kingsrook.qqq.backend.core.utils.http;


/*******************************************************************************
 ** An outbound request was refused by policy or failed before a response.
 ** Messages never include request bodies, headers or secrets.
 *******************************************************************************/
public class SafeHttpException extends Exception
{
   /*******************************************************************************
    ** What went wrong. The first three are policy refusals (no connection was
    ** attempted); the rest are network failures.
    *******************************************************************************/
   public enum Reason
   {
      INVALID_URL(true),
      BLOCKED_SCHEME(true),
      BLOCKED_ADDRESS(true),
      DNS_FAILURE(false),
      CONNECT_TIMEOUT(false),
      READ_TIMEOUT(false),
      TOTAL_TIMEOUT(false),
      TLS_FAILURE(false),
      CONNECTION_FAILED(false);

      private final boolean policy;



      /*******************************************************************************
       ** Constructor.
       *******************************************************************************/
      Reason(boolean policy)
      {
         this.policy = policy;
      }



      /*******************************************************************************
       ** True when the request was refused by policy (retrying will not help
       ** until the URL or the allowlist changes).
       *******************************************************************************/
      public boolean isPolicy()
      {
         return (policy);
      }
   }



   private final Reason reason;



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public SafeHttpException(Reason reason, String message)
   {
      super(message);
      this.reason = reason;
   }



   /*******************************************************************************
    ** Constructor with a cause.
    *******************************************************************************/
   public SafeHttpException(Reason reason, String message, Throwable cause)
   {
      super(message, cause);
      this.reason = reason;
   }



   /*******************************************************************************
    ** Getter for reason.
    *******************************************************************************/
   public Reason getReason()
   {
      return (reason);
   }
}
