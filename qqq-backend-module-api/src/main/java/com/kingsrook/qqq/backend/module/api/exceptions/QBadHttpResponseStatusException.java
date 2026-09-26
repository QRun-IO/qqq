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

package com.kingsrook.qqq.backend.module.api.exceptions;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.module.api.actions.QHttpResponse;


/*******************************************************************************
 ** Exception thrown when an API HTTP request failed due to a bad status code.
 ** This exception includes the status code as a field, as well as the full
 ** response object.
 *******************************************************************************/
public class QBadHttpResponseStatusException extends QException
{
   private int           statusCode;
   private QHttpResponse response;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QBadHttpResponseStatusException(String message, QHttpResponse response)
   {
      super(message);

      this.statusCode = response.getStatusCode();
      this.response = response;
   }



   /*******************************************************************************
    ** Getter for statusCode
    *******************************************************************************/
   public int getStatusCode()
   {
      return (this.statusCode);
   }



   /*******************************************************************************
    ** Setter for statusCode
    *******************************************************************************/
   public void setStatusCode(int statusCode)
   {
      this.statusCode = statusCode;
   }



   /*******************************************************************************
    ** Fluent setter for statusCode
    *******************************************************************************/
   public QBadHttpResponseStatusException withStatusCode(int statusCode)
   {
      this.statusCode = statusCode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for response
    *******************************************************************************/
   public QHttpResponse getResponse()
   {
      return (this.response);
   }



   /*******************************************************************************
    ** Setter for response
    *******************************************************************************/
   public void setResponse(QHttpResponse response)
   {
      this.response = response;
   }



   /*******************************************************************************
    ** Fluent setter for response
    *******************************************************************************/
   public QBadHttpResponseStatusException withResponse(QHttpResponse response)
   {
      this.response = response;
      return (this);
   }

}
