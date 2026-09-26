/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.openapi.model;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class Method
{
   private String                 method;
   private String                 summary;
   private String                 description;
   private String                 operationId;
   private List<String>           tags;
   private RequestBody            requestBody;
   private List<Parameter>        parameters;
   private Map<Integer, Response> responses;

   private List<Map<String, List<String>>> security;



   /*******************************************************************************
    ** Getter for method
    *******************************************************************************/
   public String getMethod()
   {
      return (this.method);
   }



   /*******************************************************************************
    ** Setter for method
    *******************************************************************************/
   public void setMethod(String method)
   {
      this.method = method;
   }



   /*******************************************************************************
    ** Fluent setter for method
    *******************************************************************************/
   public Method withMethod(String method)
   {
      this.method = method;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public Method withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public Method withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for operationId
    *******************************************************************************/
   public String getOperationId()
   {
      return (this.operationId);
   }



   /*******************************************************************************
    ** Setter for operationId
    *******************************************************************************/
   public void setOperationId(String operationId)
   {
      this.operationId = operationId;
   }



   /*******************************************************************************
    ** Fluent setter for operationId
    *******************************************************************************/
   public Method withOperationId(String operationId)
   {
      this.operationId = operationId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tags
    *******************************************************************************/
   public List<String> getTags()
   {
      return (this.tags);
   }



   /*******************************************************************************
    ** Setter for tags
    *******************************************************************************/
   public void setTags(List<String> tags)
   {
      this.tags = tags;
   }



   /*******************************************************************************
    ** Fluent setter for tags
    *******************************************************************************/
   public Method withTags(List<String> tags)
   {
      this.tags = tags;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for tags
    *******************************************************************************/
   public Method withTag(String tag)
   {
      if(this.tags == null)
      {
         this.tags = new ArrayList<>();
      }
      this.tags.add(tag);
      return (this);
   }



   /*******************************************************************************
    ** Getter for requestBody
    *******************************************************************************/
   public RequestBody getRequestBody()
   {
      return (this.requestBody);
   }



   /*******************************************************************************
    ** Setter for requestBody
    *******************************************************************************/
   public void setRequestBody(RequestBody requestBody)
   {
      this.requestBody = requestBody;
   }



   /*******************************************************************************
    ** Fluent setter for requestBody
    *******************************************************************************/
   public Method withRequestBody(RequestBody requestBody)
   {
      this.requestBody = requestBody;
      return (this);
   }



   /*******************************************************************************
    ** Getter for parameters
    *******************************************************************************/
   public List<Parameter> getParameters()
   {
      return (this.parameters);
   }



   /*******************************************************************************
    ** Setter for parameters
    *******************************************************************************/
   public void setParameters(List<Parameter> parameters)
   {
      this.parameters = parameters;
   }



   /*******************************************************************************
    ** Fluent setter for parameters
    *******************************************************************************/
   public Method withParameters(List<Parameter> parameters)
   {
      this.parameters = parameters;
      return (this);
   }



   /*******************************************************************************
    ** Getter for responses
    *******************************************************************************/
   public Map<Integer, Response> getResponses()
   {
      return (this.responses);
   }



   /*******************************************************************************
    ** Setter for responses
    *******************************************************************************/
   public void setResponses(Map<Integer, Response> responses)
   {
      this.responses = responses;
   }



   /*******************************************************************************
    ** Fluent setter for responses
    *******************************************************************************/
   public Method withResponses(Map<Integer, Response> responses)
   {
      this.responses = responses;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Method withResponse(Integer code, Response response)
   {
      if(this.responses == null)
      {
         this.responses = new LinkedHashMap<>();
      }
      this.responses.put(code, response);
      return (this);
   }



   /*******************************************************************************
    ** Getter for security
    *******************************************************************************/
   public List<Map<String, List<String>>> getSecurity()
   {
      return (this.security);
   }



   /*******************************************************************************
    ** Setter for security
    *******************************************************************************/
   public void setSecurity(List<Map<String, List<String>>> security)
   {
      this.security = security;
   }



   /*******************************************************************************
    ** Fluent setter for security
    *******************************************************************************/
   public Method withSecurity(List<Map<String, List<String>>> security)
   {
      this.security = security;
      return (this);
   }

}
