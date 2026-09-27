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

package com.kingsrook.qqq.api.model.metadata;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.api.ApiSupplementType;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiInstanceMetaDataContainer implements QSupplementalInstanceMetaData
{
   private Map<String, ApiInstanceMetaData> apis;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ApiSupplementType.NAME;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ApiInstanceMetaDataContainer()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ApiInstanceMetaDataContainer of(QInstance qInstance)
   {
      return ((ApiInstanceMetaDataContainer) qInstance.getSupplementalMetaData(ApiSupplementType.NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      for(Map.Entry<String, ApiInstanceMetaData> entry : CollectionUtils.nonNullMap(apis).entrySet())
      {
         entry.getValue().validate(entry.getKey(), qInstance, validator);
      }
   }



   /*******************************************************************************
    ** Getter for apis
    *******************************************************************************/
   public Map<String, ApiInstanceMetaData> getApis()
   {
      return (this.apis);
   }



   /*******************************************************************************
    ** Getter for apis
    *******************************************************************************/
   public ApiInstanceMetaData getApiInstanceMetaData(String apiName)
   {
      if(this.apis == null)
      {
         return (null);
      }

      return (this.apis.get(apiName));
   }



   /*******************************************************************************
    ** Setter for apis
    *******************************************************************************/
   public void setApis(Map<String, ApiInstanceMetaData> apis)
   {
      this.apis = apis;
   }



   /*******************************************************************************
    ** Fluent setter for apis
    *******************************************************************************/
   public ApiInstanceMetaDataContainer withApis(Map<String, ApiInstanceMetaData> apis)
   {
      this.apis = apis;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for apis
    *******************************************************************************/
   public ApiInstanceMetaDataContainer withApiInstanceMetaData(ApiInstanceMetaData apiInstanceMetaData)
   {
      if(this.apis == null)
      {
         this.apis = new LinkedHashMap<>();
      }
      this.apis.put(apiInstanceMetaData.getName(), apiInstanceMetaData);
      return (this);
   }

}