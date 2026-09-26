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


import org.json.JSONObject;


/*******************************************************************************
 ** QQQ abstraction over an AWS Lambda Request.
 *******************************************************************************/
public class QLambdaRequest
{
   private JSONObject headers;
   private String     path;
   private String     queryString;
   private String     body;
   private JSONObject requestContext;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QLambdaRequest(JSONObject inputJsonObject)
   {
      this.headers = inputJsonObject.optJSONObject("headers");
      this.path = inputJsonObject.optString("rawPath");
      this.queryString = inputJsonObject.optString("rawQueryString");
      this.body = inputJsonObject.optString("body");
      this.requestContext = inputJsonObject.optJSONObject("requestContext");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QLambdaRequest(JSONObject headers, String path, String queryString, String body)
   {
      this.headers = headers;
      this.path = path;
      this.queryString = queryString;
      this.body = body;
   }



   /*******************************************************************************
    ** Getter for headers
    **
    *******************************************************************************/
   public JSONObject getHeaders()
   {
      return headers;
   }



   /*******************************************************************************
    ** Getter for path
    **
    *******************************************************************************/
   public String getPath()
   {
      return path;
   }



   /*******************************************************************************
    ** Getter for queryString
    **
    *******************************************************************************/
   public String getQueryString()
   {
      return queryString;
   }



   /*******************************************************************************
    ** Getter for body
    **
    *******************************************************************************/
   public String getBody()
   {
      return body;
   }



   /*******************************************************************************
    ** Getter for requestContext
    **
    *******************************************************************************/
   public JSONObject getRequestContext()
   {
      return requestContext;
   }
}