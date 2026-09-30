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

package com.kingsrook.qqq.backend.core.model.metadata.queues;


/*******************************************************************************
 ** Meta-data for an source of Amazon SQS queues (e.g, an aws account/credential
 ** set, with a common base URL).
 **
 ** Scheduled can be defined here, to apply to all queues in the provider - or
 ** each can supply their own schedule.
 *******************************************************************************/
public class SQSQueueProviderMetaData extends QQueueProviderMetaData
{
   private String accessKey;
   private String secretKey;
   private String region;
   private String baseURL;

   private SQSPollerSettings pollerSettings;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SQSQueueProviderMetaData()
   {
      super();
      setType(QueueType.SQS);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public SQSQueueProviderMetaData withName(String name)
   {
      super.withName(name);
      return (this);
   }



   /*******************************************************************************
    ** Getter for accessKey
    **
    *******************************************************************************/
   public String getAccessKey()
   {
      return accessKey;
   }



   /*******************************************************************************
    ** Setter for accessKey
    **
    *******************************************************************************/
   public void setAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
   }



   /*******************************************************************************
    ** Fluent setter for accessKey
    **
    *******************************************************************************/
   public SQSQueueProviderMetaData withAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for secretKey
    **
    *******************************************************************************/
   public String getSecretKey()
   {
      return secretKey;
   }



   /*******************************************************************************
    ** Setter for secretKey
    **
    *******************************************************************************/
   public void setSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
   }



   /*******************************************************************************
    ** Fluent setter for secretKey
    **
    *******************************************************************************/
   public SQSQueueProviderMetaData withSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for region
    **
    *******************************************************************************/
   public String getRegion()
   {
      return region;
   }



   /*******************************************************************************
    ** Setter for region
    **
    *******************************************************************************/
   public void setRegion(String region)
   {
      this.region = region;
   }



   /*******************************************************************************
    ** Fluent setter for region
    **
    *******************************************************************************/
   public SQSQueueProviderMetaData withRegion(String region)
   {
      this.region = region;
      return (this);
   }



   /*******************************************************************************
    ** Getter for baseURL
    **
    *******************************************************************************/
   public String getBaseURL()
   {
      return baseURL;
   }



   /*******************************************************************************
    ** Setter for baseURL
    **
    *******************************************************************************/
   public void setBaseURL(String baseURL)
   {
      this.baseURL = baseURL;
   }



   /*******************************************************************************
    ** Fluent setter for baseURL
    **
    *******************************************************************************/
   public SQSQueueProviderMetaData withBaseURL(String baseURL)
   {
      this.baseURL = baseURL;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pollerSettings
    *******************************************************************************/
   public SQSPollerSettings getPollerSettings()
   {
      return (this.pollerSettings);
   }



   /*******************************************************************************
    ** Setter for pollerSettings
    *******************************************************************************/
   public void setPollerSettings(SQSPollerSettings pollerSettings)
   {
      this.pollerSettings = pollerSettings;
   }



   /*******************************************************************************
    ** Fluent setter for pollerSettings
    *******************************************************************************/
   public SQSQueueProviderMetaData withPollerSettings(SQSPollerSettings pollerSettings)
   {
      this.pollerSettings = pollerSettings;
      return (this);
   }

}
