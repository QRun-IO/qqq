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

package com.kingsrook.qqq.esb.envelope;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 * Thrown by EsbEventCodec when a message isn't a CloudEvents 1.0 JSON event
 * that QQQ can read: not a JMS TextMessage, not a JSON object, missing or
 * invalid attributes, or data that isn't a JSON object.
 *
 * Retrying can't fix such a message, so a trigger dead-letters it right away.
 * The message text names what was wrong, never the message body.
 *******************************************************************************/
public class EsbUnparseableMessageException extends QException
{
   private static final long serialVersionUID = 1L;



   /*******************************************************************************
    ** Constructor of message
    *******************************************************************************/
   public EsbUnparseableMessageException(String message)
   {
      super(message);
   }



   /*******************************************************************************
    ** Constructor of message and cause
    *******************************************************************************/
   public EsbUnparseableMessageException(String message, Throwable cause)
   {
      super(message, cause);
   }

}
