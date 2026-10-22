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


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.api.ApiSupplementType;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QSupplementalProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiProcessMetaDataContainer extends QSupplementalProcessMetaData
{
   private Map<String, ApiProcessMetaData> apis;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ApiProcessMetaDataContainer()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ApiProcessMetaDataContainer of(QProcessMetaData process)
   {
      return ((ApiProcessMetaDataContainer) process.getSupplementalMetaData(ApiSupplementType.NAME));
   }



   /*******************************************************************************
    ** either get the container attached to a field - or create a new one and attach
    ** it to the field, and return that.
    *******************************************************************************/
   public static ApiProcessMetaDataContainer ofOrWithNew(QProcessMetaData process)
   {
      ApiProcessMetaDataContainer apiProcessMetaDataContainer = (ApiProcessMetaDataContainer) process.getSupplementalMetaData(ApiSupplementType.NAME);
      if(apiProcessMetaDataContainer == null)
      {
         apiProcessMetaDataContainer = new ApiProcessMetaDataContainer();
         process.withSupplementalMetaData(apiProcessMetaDataContainer);
      }
      return (apiProcessMetaDataContainer);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getType()
   {
      return (ApiSupplementType.NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void enrich(QInstanceEnricher qInstanceEnricher, QProcessMetaData process)
   {
      super.enrich(qInstanceEnricher, process);

      for(Map.Entry<String, ApiProcessMetaData> entry : CollectionUtils.nonNullMap(apis).entrySet())
      {
         entry.getValue().enrich(qInstanceEnricher, entry.getKey(), process);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QProcessMetaData process, QInstanceValidator qInstanceValidator)
   {
      super.validate(qInstance, process, qInstanceValidator);

      for(Map.Entry<String, ApiProcessMetaData> entry : CollectionUtils.nonNullMap(apis).entrySet())
      {
         entry.getValue().validate(qInstance, process, qInstanceValidator, entry.getKey());
      }
   }



   /*******************************************************************************
    ** Getter for apis
    *******************************************************************************/
   public Map<String, ApiProcessMetaData> getApis()
   {
      return (this.apis);
   }



   /*******************************************************************************
    ** Getter for apis
    *******************************************************************************/
   public ApiProcessMetaData getApiProcessMetaData(String apiName)
   {
      if(this.apis == null)
      {
         return (null);
      }

      return (this.apis.get(apiName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ApiProcessMetaData getApiProcessMetaDataOrWithNew(String apiName)
   {
      ApiProcessMetaData apiProcessMetaData = getApiProcessMetaData(apiName);
      if(apiProcessMetaData == null)
      {
         apiProcessMetaData = new ApiProcessMetaData();
         withApiProcessMetaData(apiName, apiProcessMetaData);
      }
      return (apiProcessMetaData);
   }



   /*******************************************************************************
    ** Setter for apis
    *******************************************************************************/
   public void setApis(Map<String, ApiProcessMetaData> apis)
   {
      this.apis = apis;
   }



   /*******************************************************************************
    ** Fluent setter for apis
    *******************************************************************************/
   public ApiProcessMetaDataContainer withApis(Map<String, ApiProcessMetaData> apis)
   {
      this.apis = apis;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for apis
    *******************************************************************************/
   public ApiProcessMetaDataContainer withApiProcessMetaData(String apiName, ApiProcessMetaData apiProcessMetaData)
   {
      if(this.apis == null)
      {
         this.apis = new LinkedHashMap<>();
      }
      this.apis.put(apiName, apiProcessMetaData);
      return (this);
   }
}
