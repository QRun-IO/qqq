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

package com.kingsrook.qqq.api.model.metadata.processes;


import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiProcessInput
{
   private ApiProcessInputFieldsContainer pathParams;
   private ApiProcessInputFieldsContainer queryStringParams;
   private ApiProcessInputFieldsContainer formParams;
   private ApiProcessInputFieldsContainer recordBodyParams;

   private QFieldMetaData bodyField;
   private String         bodyFieldContentType;



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getRecordIdsParamName()
   {
      if(pathParams != null && pathParams.getRecordIdsField() != null)
      {
         return (pathParams.getRecordIdsField().getName());
      }

      if(queryStringParams != null && queryStringParams.getRecordIdsField() != null)
      {
         return (queryStringParams.getRecordIdsField().getName());
      }

      if(formParams != null && formParams.getRecordIdsField() != null)
      {
         return (formParams.getRecordIdsField().getName());
      }

      if(recordBodyParams != null && recordBodyParams.getRecordIdsField() != null)
      {
         return (recordBodyParams.getRecordIdsField().getName());
      }

      return (null);
   }



   /*******************************************************************************
    ** Getter for queryStringParams
    *******************************************************************************/
   public ApiProcessInputFieldsContainer getQueryStringParams()
   {
      return (this.queryStringParams);
   }



   /*******************************************************************************
    ** Setter for queryStringParams
    *******************************************************************************/
   public void setQueryStringParams(ApiProcessInputFieldsContainer queryStringParams)
   {
      this.queryStringParams = queryStringParams;
   }



   /*******************************************************************************
    ** Fluent setter for queryStringParams
    *******************************************************************************/
   public ApiProcessInput withQueryStringParams(ApiProcessInputFieldsContainer queryStringParams)
   {
      this.queryStringParams = queryStringParams;
      return (this);
   }



   /*******************************************************************************
    ** Getter for formParams
    *******************************************************************************/
   public ApiProcessInputFieldsContainer getFormParams()
   {
      return (this.formParams);
   }



   /*******************************************************************************
    ** Setter for formParams
    *******************************************************************************/
   public void setFormParams(ApiProcessInputFieldsContainer formParams)
   {
      this.formParams = formParams;
   }



   /*******************************************************************************
    ** Fluent setter for formParams
    *******************************************************************************/
   public ApiProcessInput withFormParams(ApiProcessInputFieldsContainer formParams)
   {
      this.formParams = formParams;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordBodyParams
    *******************************************************************************/
   public ApiProcessInputFieldsContainer getObjectBodyParams()
   {
      return (this.recordBodyParams);
   }



   /*******************************************************************************
    ** Setter for recordBodyParams
    *******************************************************************************/
   public void setRecordBodyParams(ApiProcessInputFieldsContainer recordBodyParams)
   {
      this.recordBodyParams = recordBodyParams;
   }



   /*******************************************************************************
    ** Fluent setter for recordBodyParams
    *******************************************************************************/
   public ApiProcessInput withRecordBodyParams(ApiProcessInputFieldsContainer recordBodyParams)
   {
      this.recordBodyParams = recordBodyParams;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bodyField
    *******************************************************************************/
   public QFieldMetaData getBodyField()
   {
      return (this.bodyField);
   }



   /*******************************************************************************
    ** Setter for bodyField
    *******************************************************************************/
   public void setBodyField(QFieldMetaData bodyField)
   {
      this.bodyField = bodyField;
   }



   /*******************************************************************************
    ** Fluent setter for bodyField
    *******************************************************************************/
   public ApiProcessInput withBodyField(QFieldMetaData bodyField)
   {
      this.bodyField = bodyField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bodyFieldContentType
    *******************************************************************************/
   public String getBodyFieldContentType()
   {
      return (this.bodyFieldContentType);
   }



   /*******************************************************************************
    ** Setter for bodyFieldContentType
    *******************************************************************************/
   public void setBodyFieldContentType(String bodyFieldContentType)
   {
      this.bodyFieldContentType = bodyFieldContentType;
   }



   /*******************************************************************************
    ** Fluent setter for bodyFieldContentType
    *******************************************************************************/
   public ApiProcessInput withBodyFieldContentType(String bodyFieldContentType)
   {
      this.bodyFieldContentType = bodyFieldContentType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for pathParams
    *******************************************************************************/
   public ApiProcessInputFieldsContainer getPathParams()
   {
      return (this.pathParams);
   }



   /*******************************************************************************
    ** Setter for pathParams
    *******************************************************************************/
   public void setPathParams(ApiProcessInputFieldsContainer pathParams)
   {
      this.pathParams = pathParams;
   }



   /*******************************************************************************
    ** Fluent setter for pathParams
    *******************************************************************************/
   public ApiProcessInput withPathParams(ApiProcessInputFieldsContainer pathParams)
   {
      this.pathParams = pathParams;
      return (this);
   }


}
