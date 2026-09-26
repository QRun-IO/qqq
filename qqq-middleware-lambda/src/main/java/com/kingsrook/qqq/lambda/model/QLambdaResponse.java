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

package com.kingsrook.qqq.lambda.model;


import java.util.Map;


/*******************************************************************************
 ** QQQ abstraction over an AWS Lambda Response.
 *******************************************************************************/
public class QLambdaResponse
{
   private int                 statusCode;
   private boolean             isBase64Encoded = false;
   private Map<String, String> headers         = Map.of("Content-Type", "application/json");
   private Body                body;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QLambdaResponse(int statusCode, Body body)
   {
      this.statusCode = statusCode;
      this.body = body;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QLambdaResponse(int statusCode, String errorMessage)
   {
      this.statusCode = statusCode;
      this.body = new Body(errorMessage);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QLambdaResponse(int statusCode)
   {
      this.statusCode = statusCode;
      this.body = new Body();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QLambdaResponse(int statusCode, boolean isBase64Encoded, Map<String, String> headers, Body body)
   {
      this.statusCode = statusCode;
      this.isBase64Encoded = isBase64Encoded;
      this.headers = headers;
      this.body = body;
   }



   /*******************************************************************************
    ** Getter for statusCode
    **
    *******************************************************************************/
   public int getStatusCode()
   {
      return statusCode;
   }



   /*******************************************************************************
    ** Getter for isBase64Encoded
    **
    *******************************************************************************/
   public boolean getIsBase64Encoded()
   {
      return isBase64Encoded;
   }



   /*******************************************************************************
    ** Getter for headers
    **
    *******************************************************************************/
   public Map<String, String> getHeaders()
   {
      return headers;
   }



   /*******************************************************************************
    ** Getter for body
    **
    *******************************************************************************/
   public Body getBody()
   {
      return body;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Body
   {
      private String              requestId;
      private String              errorMessage;
      private Map<String, Object> body;



      /*******************************************************************************
       **
       *******************************************************************************/
      public Body()
      {
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Body(String errorMessage)
      {
         this.errorMessage = errorMessage;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Body(String requestId, String errorMessage)
      {
         this.requestId = requestId;
         this.errorMessage = errorMessage;
      }



      /*******************************************************************************
       ** Setter for requestId
       **
       *******************************************************************************/
      public void setRequestId(String requestId)
      {
         this.requestId = requestId;
      }



      /*******************************************************************************
       ** Getter for requestId
       **
       *******************************************************************************/
      public String getRequestId()
      {
         return requestId;
      }



      /*******************************************************************************
       ** Getter for errorMessage
       **
       *******************************************************************************/
      public String getErrorMessage()
      {
         return errorMessage;
      }



      /*******************************************************************************
       ** Getter for body
       **
       *******************************************************************************/
      public Map<String, Object> getBody()
      {
         return body;
      }



      /*******************************************************************************
       ** Setter for body
       **
       *******************************************************************************/
      public void setBody(Map<String, Object> body)
      {
         this.body = body;
      }
   }

}
