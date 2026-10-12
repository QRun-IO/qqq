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

package com.kingsrook.qqq.backend.core.utils.webhooks;


/*******************************************************************************
 ** A webhook request failed verification. The message never contains a
 ** secret or a signature, so it is safe to log and to return to the caller.
 *******************************************************************************/
public class WebhookVerificationException extends Exception
{
   /*******************************************************************************
    ** Why verification failed.
    *******************************************************************************/
   public enum Reason
   {
      MISSING_HEADERS,
      BAD_TIMESTAMP,
      TIMESTAMP_OUT_OF_TOLERANCE,
      NO_MATCHING_SIGNATURE
   }



   private final Reason reason;



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public WebhookVerificationException(Reason reason, String message)
   {
      super(message);
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
