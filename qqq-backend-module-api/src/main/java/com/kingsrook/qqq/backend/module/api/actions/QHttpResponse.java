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

package com.kingsrook.qqq.backend.module.api.actions;


import java.util.Arrays;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;


/*******************************************************************************
 ** class to contain httpResponse data from closable responses
 **
 *******************************************************************************/
public class QHttpResponse
{
   private Integer      statusCode;
   private String       statusProtocolVersion;
   private String       statusReasonPhrase;
   private List<Header> headerList;
   private String       content;
   private byte[]       contentBytes;



   /*******************************************************************************
    ** Default Constructor for qHttpResponse
    **
    *******************************************************************************/
   public QHttpResponse()
   {
   }



   /*******************************************************************************
    ** Constructor for QHttpResponse that allows reading content as bytes
    **
    *******************************************************************************/
   public QHttpResponse(HttpResponse httpResponse, boolean readContentAsBytes) throws Exception
   {
      if(!readContentAsBytes)
      {
         new QHttpResponse(httpResponse);
         return;
      }

      setGeneralHttpResponseData(httpResponse);
      if(this.statusCode == null || this.statusCode != 204)
      {
         this.contentBytes = httpResponse.getEntity().getContent().readAllBytes();
      }
   }



   /*******************************************************************************
    ** Constructor for qHttpResponse
    **
    *******************************************************************************/
   public QHttpResponse(HttpResponse httpResponse) throws Exception
   {
      setGeneralHttpResponseData(httpResponse);
      if(this.statusCode == null || this.statusCode != 204)
      {
         this.content = EntityUtils.toString(httpResponse.getEntity());
      }
   }



   /*******************************************************************************
    ** Sets data into this entity from an HttpResponse but doesnt read response data
    **
    *******************************************************************************/
   private void setGeneralHttpResponseData(HttpResponse httpResponse) throws Exception
   {
      this.headerList = Arrays.asList(httpResponse.getAllHeaders());
      if(httpResponse.getStatusLine() != null)
      {
         this.statusCode = httpResponse.getStatusLine().getStatusCode();
         this.statusReasonPhrase = httpResponse.getStatusLine().getReasonPhrase();
         if(httpResponse.getStatusLine().getProtocolVersion() != null)
         {
            this.statusProtocolVersion = httpResponse.getStatusLine().getProtocolVersion().toString();
         }
      }
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
    ** Getter for statusProtocolVersion
    **
    *******************************************************************************/
   public String getStatusProtocolVersion()
   {
      return statusProtocolVersion;
   }



   /*******************************************************************************
    ** Setter for statusProtocolVersion
    **
    *******************************************************************************/
   public void setStatusProtocolVersion(String statusProtocolVersion)
   {
      this.statusProtocolVersion = statusProtocolVersion;
   }



   /*******************************************************************************
    ** Getter for statusReasonPhrase
    **
    *******************************************************************************/
   public String getStatusReasonPhrase()
   {
      return statusReasonPhrase;
   }



   /*******************************************************************************
    ** Setter for statusReasonPhrase
    **
    *******************************************************************************/
   public void setStatusReasonPhrase(String statusReasonPhrase)
   {
      this.statusReasonPhrase = statusReasonPhrase;
   }



   /*******************************************************************************
    ** Getter for headerList
    **
    *******************************************************************************/
   public List<Header> getHeaderList()
   {
      return headerList;
   }



   /*******************************************************************************
    ** Setter for headerList
    **
    *******************************************************************************/
   public void setHeaderList(List<Header> headerList)
   {
      this.headerList = headerList;
   }



   /*******************************************************************************
    ** Getter for content
    **
    *******************************************************************************/
   public String getContent()
   {
      return content;
   }



   /*******************************************************************************
    ** Setter for content
    **
    *******************************************************************************/
   public void setContent(String content)
   {
      this.content = content;
   }



   /*******************************************************************************
    ** Fluent setter for content
    **
    *******************************************************************************/
   public QHttpResponse withContent(String content)
   {
      this.content = content;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for statusCode
    *******************************************************************************/
   public QHttpResponse withStatusCode(Integer statusCode)
   {
      this.statusCode = statusCode;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for statusProtocolVersion
    *******************************************************************************/
   public QHttpResponse withStatusProtocolVersion(String statusProtocolVersion)
   {
      this.statusProtocolVersion = statusProtocolVersion;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for statusReasonPhrase
    *******************************************************************************/
   public QHttpResponse withStatusReasonPhrase(String statusReasonPhrase)
   {
      this.statusReasonPhrase = statusReasonPhrase;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for headerList
    *******************************************************************************/
   public QHttpResponse withHeaderList(List<Header> headerList)
   {
      this.headerList = headerList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contentBytes
    *******************************************************************************/
   public byte[] getContentBytes()
   {
      return (this.contentBytes);
   }



   /*******************************************************************************
    ** Setter for contentBytes
    *******************************************************************************/
   public void setContentBytes(byte[] contentBytes)
   {
      this.contentBytes = contentBytes;
   }



   /*******************************************************************************
    ** Fluent setter for contentBytes
    *******************************************************************************/
   public QHttpResponse withContentBytes(byte[] contentBytes)
   {
      this.contentBytes = contentBytes;
      return (this);
   }

}
