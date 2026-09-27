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

package com.kingsrook.qqq.api.model.metadata.fields;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.api.ApiSupplementType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QSupplementalFieldMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiFieldMetaDataContainer extends QSupplementalFieldMetaData
{
   private Map<String, ApiFieldMetaData> apis;

   private ApiFieldMetaData defaultApiFieldMetaData;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ApiFieldMetaDataContainer()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ApiFieldMetaDataContainer of(QFieldMetaData field)
   {
      return ((ApiFieldMetaDataContainer) field.getSupplementalMetaData(ApiSupplementType.NAME));
   }



   /*******************************************************************************
    ** either get the container attached to a field - or create a new one and attach
    ** it to the field, and return that.
    *******************************************************************************/
   public static ApiFieldMetaDataContainer ofOrWithNew(QFieldMetaData field)
   {
      ApiFieldMetaDataContainer apiFieldMetaDataContainer = (ApiFieldMetaDataContainer) field.getSupplementalMetaData(ApiSupplementType.NAME);
      if(apiFieldMetaDataContainer == null)
      {
         apiFieldMetaDataContainer = new ApiFieldMetaDataContainer();
         field.withSupplementalMetaData(apiFieldMetaDataContainer);
      }
      return (apiFieldMetaDataContainer);
   }



   /*******************************************************************************
    ** either get the container attached to a field - or a new one - note - the new
    ** one will NOT be attached to the field!!
    *******************************************************************************/
   public static ApiFieldMetaDataContainer ofOrNew(QFieldMetaData field)
   {
      return (Objects.requireNonNullElseGet(of(field), ApiFieldMetaDataContainer::new));
   }



   /*******************************************************************************
    ** Getter for apis
    *******************************************************************************/
   public Map<String, ApiFieldMetaData> getApis()
   {
      return (this.apis);
   }



   /*******************************************************************************
    ** Getter the apiFieldMetaData for a specific api, or the container's default
    *******************************************************************************/
   public ApiFieldMetaData getApiFieldMetaData(String apiName)
   {
      if(this.apis == null)
      {
         return (defaultApiFieldMetaData);
      }

      return (this.apis.getOrDefault(apiName, defaultApiFieldMetaData));
   }



   /*******************************************************************************
    ** Setter for apis
    *******************************************************************************/
   public void setApis(Map<String, ApiFieldMetaData> apis)
   {
      this.apis = apis;
   }



   /*******************************************************************************
    ** Fluent setter for apis
    *******************************************************************************/
   public ApiFieldMetaDataContainer withApis(Map<String, ApiFieldMetaData> apis)
   {
      this.apis = apis;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for apis
    *******************************************************************************/
   public ApiFieldMetaDataContainer withApiFieldMetaData(String apiName, ApiFieldMetaData apiFieldMetaData)
   {
      if(this.apis == null)
      {
         this.apis = new LinkedHashMap<>();
      }
      this.apis.put(apiName, apiFieldMetaData);
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultApiFieldMetaData
    *******************************************************************************/
   public ApiFieldMetaData getDefaultApiFieldMetaData()
   {
      return (this.defaultApiFieldMetaData);
   }



   /*******************************************************************************
    ** Setter for defaultApiFieldMetaData
    *******************************************************************************/
   public void setDefaultApiFieldMetaData(ApiFieldMetaData defaultApiFieldMetaData)
   {
      this.defaultApiFieldMetaData = defaultApiFieldMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for defaultApiFieldMetaData
    *******************************************************************************/
   public ApiFieldMetaDataContainer withDefaultApiFieldMetaData(ApiFieldMetaData defaultApiFieldMetaData)
   {
      this.defaultApiFieldMetaData = defaultApiFieldMetaData;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getType()
   {
      return (ApiSupplementType.NAME);
   }
}
