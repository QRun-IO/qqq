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

package com.kingsrook.qqq.backend.module.api.exceptions;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Exception to be thrown by a request that uses OAuth, if the current token
 ** is expired.  Generally should signal that the token needs refreshed, and the
 ** request should be tried again.
 **
 *******************************************************************************/
public class OAuthExpiredTokenException extends QException
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public OAuthExpiredTokenException(String message)
   {
      super(message);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public OAuthExpiredTokenException(String errorMessage, Exception e)
   {
      super(errorMessage, e);
   }
}
