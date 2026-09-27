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

package com.kingsrook.qqq.backend.core.model.metadata.messaging.ses;


import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.modules.messaging.QMessagingProviderDispatcher;


/*******************************************************************************
 **
 *******************************************************************************/
public class SESMessagingProviderMetaData extends QMessagingProviderMetaData
{
   private String accessKey;
   private String secretKey;
   private String region;

   public static final String TYPE = "SES";

   static
   {
      QMessagingProviderDispatcher.registerMessagingProvider(new SESMessagingProvider());
   }

   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SESMessagingProviderMetaData()
   {
      super();
      setType(TYPE);
   }



   /*******************************************************************************
    ** Getter for accessKey
    *******************************************************************************/
   public String getAccessKey()
   {
      return (this.accessKey);
   }



   /*******************************************************************************
    ** Setter for accessKey
    *******************************************************************************/
   public void setAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
   }



   /*******************************************************************************
    ** Fluent setter for accessKey
    *******************************************************************************/
   public SESMessagingProviderMetaData withAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for secretKey
    *******************************************************************************/
   public String getSecretKey()
   {
      return (this.secretKey);
   }



   /*******************************************************************************
    ** Setter for secretKey
    *******************************************************************************/
   public void setSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
   }



   /*******************************************************************************
    ** Fluent setter for secretKey
    *******************************************************************************/
   public SESMessagingProviderMetaData withSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for region
    *******************************************************************************/
   public String getRegion()
   {
      return (this.region);
   }



   /*******************************************************************************
    ** Setter for region
    *******************************************************************************/
   public void setRegion(String region)
   {
      this.region = region;
   }



   /*******************************************************************************
    ** Fluent setter for region
    *******************************************************************************/
   public SESMessagingProviderMetaData withRegion(String region)
   {
      this.region = region;
      return (this);
   }

}
