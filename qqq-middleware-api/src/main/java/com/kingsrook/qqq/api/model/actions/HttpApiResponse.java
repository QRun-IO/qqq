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

package com.kingsrook.qqq.api.model.actions;


import java.io.InputStream;
import java.io.Serializable;
import org.eclipse.jetty.http.HttpStatus;


/*******************************************************************************
 ** class to contain http api responses.
 **
 *******************************************************************************/
public class HttpApiResponse
{
   private HttpStatus.Code statusCode;
   private Serializable    responseBodyObject;

   private String  contentType;

   private InputStream inputStream;

   ////////////////////////////////////////////////////////////////////////////////////////////////////////
   // by default - QJavalinApiHandler will format the responseBodyObject as JSON.                        //
   // set this field to false if you don't want it to do that (e.g., if your response is, say, a byte[]) //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////
   private boolean needsFormattedAsJson = true;



   /*******************************************************************************
    ** Default Constructor
    **
    *******************************************************************************/
   public HttpApiResponse()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public HttpApiResponse(HttpStatus.Code statusCode, Serializable responseBodyObject)
   {
      this.statusCode = statusCode;
      this.responseBodyObject = responseBodyObject;
   }



   /*******************************************************************************
    ** Getter for statusCode
    *******************************************************************************/
   public HttpStatus.Code getStatusCode()
   {
      return (this.statusCode);
   }



   /*******************************************************************************
    ** Setter for statusCode
    *******************************************************************************/
   public void setStatusCode(HttpStatus.Code statusCode)
   {
      this.statusCode = statusCode;
   }



   /*******************************************************************************
    ** Fluent setter for statusCode
    *******************************************************************************/
   public HttpApiResponse withStatusCode(HttpStatus.Code statusCode)
   {
      this.statusCode = statusCode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for responseBodyObject
    *******************************************************************************/
   public Serializable getResponseBodyObject()
   {
      return (this.responseBodyObject);
   }



   /*******************************************************************************
    ** Setter for responseBodyObject
    *******************************************************************************/
   public void setResponseBodyObject(Serializable responseBodyObject)
   {
      this.responseBodyObject = responseBodyObject;
   }



   /*******************************************************************************
    ** Fluent setter for responseBodyObject
    *******************************************************************************/
   public HttpApiResponse withResponseBodyObject(Serializable responseBodyObject)
   {
      this.responseBodyObject = responseBodyObject;
      return (this);
   }



   /*******************************************************************************
    ** Getter for needsFormattedAsJson
    *******************************************************************************/
   public boolean getNeedsFormattedAsJson()
   {
      return (this.needsFormattedAsJson);
   }



   /*******************************************************************************
    ** Setter for needsFormattedAsJson
    *******************************************************************************/
   public void setNeedsFormattedAsJson(boolean needsFormattedAsJson)
   {
      this.needsFormattedAsJson = needsFormattedAsJson;
   }



   /*******************************************************************************
    ** Fluent setter for needsFormattedAsJson
    *******************************************************************************/
   public HttpApiResponse withNeedsFormattedAsJson(boolean needsFormattedAsJson)
   {
      this.needsFormattedAsJson = needsFormattedAsJson;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contentType
    *******************************************************************************/
   public String getContentType()
   {
      return (this.contentType);
   }



   /*******************************************************************************
    ** Setter for contentType
    *******************************************************************************/
   public void setContentType(String contentType)
   {
      this.contentType = contentType;
   }



   /*******************************************************************************
    ** Fluent setter for contentType
    *******************************************************************************/
   public HttpApiResponse withContentType(String contentType)
   {
      this.contentType = contentType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for inputStream
    *******************************************************************************/
   public InputStream getInputStream()
   {
      return (this.inputStream);
   }



   /*******************************************************************************
    ** Setter for inputStream
    *******************************************************************************/
   public void setInputStream(InputStream inputStream)
   {
      this.inputStream = inputStream;
   }



   /*******************************************************************************
    ** Fluent setter for inputStream
    *******************************************************************************/
   public HttpApiResponse withInputStream(InputStream inputStream)
   {
      this.inputStream = inputStream;
      return (this);
   }


}