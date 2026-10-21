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

package com.kingsrook.qqq.backend.core.exceptions;


/*******************************************************************************
 * Exception thrown doing authentication
 *
 *******************************************************************************/
public class AccessTokenException extends QAuthenticationException
{
   private Integer statusCode;



   /*******************************************************************************
    ** Constructor of message
    **
    *******************************************************************************/
   public AccessTokenException(String message)
   {
      super(message);
   }



   /*******************************************************************************
    ** Constructor of message
    **
    *******************************************************************************/
   public AccessTokenException(String message, int statusCode)
   {
      super(message);
      this.statusCode = statusCode;
   }



   /*******************************************************************************
    ** Constructor of message & cause
    **
    *******************************************************************************/
   public AccessTokenException(String message, Throwable cause)
   {
      super(message, cause);
   }



   /*******************************************************************************
    ** Constructor of message & cause
    **
    *******************************************************************************/
   public AccessTokenException(String message, Throwable cause, int statusCode)
   {
      super(message, cause);
      this.statusCode = statusCode;
   }



   /*******************************************************************************
    ** Getter for statusCode
    **
    *******************************************************************************/
   public Integer getStatusCode()
   {
      return statusCode;
   }



   /*******************************************************************************
    ** Setter for statusCode
    **
    *******************************************************************************/
   public void setStatusCode(Integer statusCode)
   {
      this.statusCode = statusCode;
   }



   /*******************************************************************************
    ** Fluent setter for statusCode
    **
    *******************************************************************************/
   public AccessTokenException withStatusCode(Integer statusCode)
   {
      this.statusCode = statusCode;
      return (this);
   }

}
