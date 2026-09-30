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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard;


import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 ** AWS Quicksite specific meta data for frontend dashboard widget
 **
 *******************************************************************************/
public class QuickSightChartMetaData extends QWidgetMetaData
{
   private String             accessKey;
   private String             secretKey;
   private String             dashboardId;
   private String             accountId;
   private String             userArn;
   private String             region;
   private Collection<String> allowedDomains;

   protected Map<String, Serializable> defaultValues = new LinkedHashMap<>();



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public QuickSightChartMetaData withName(String name)
   {
      this.name = name;
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
   public QuickSightChartMetaData withAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {

      return label;
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    **
    *******************************************************************************/
   public QuickSightChartMetaData withLabel(String label)
   {
      this.label = label;
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
   public QuickSightChartMetaData withSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dashboardId
    **
    *******************************************************************************/
   public String getDashboardId()
   {
      return dashboardId;
   }



   /*******************************************************************************
    ** Setter for dashboardId
    **
    *******************************************************************************/
   public void setDashboardId(String dashboardId)
   {
      this.dashboardId = dashboardId;
   }



   /*******************************************************************************
    ** Fluent setter for dashboardId
    **
    *******************************************************************************/
   public QuickSightChartMetaData withDashboardId(String dashboardId)
   {
      this.dashboardId = dashboardId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for accountId
    **
    *******************************************************************************/
   public String getAccountId()
   {
      return accountId;
   }



   /*******************************************************************************
    ** Setter for accountId
    **
    *******************************************************************************/
   public void setAccountId(String accountId)
   {
      this.accountId = accountId;
   }



   /*******************************************************************************
    ** Fluent setter for accountId
    **
    *******************************************************************************/
   public QuickSightChartMetaData withAccountId(String accountId)
   {
      this.accountId = accountId;
      return this;
   }



   /*******************************************************************************
    ** Getter for userArn
    **
    *******************************************************************************/
   public String getUserArn()
   {
      return userArn;
   }



   /*******************************************************************************
    ** Setter for userArn
    **
    *******************************************************************************/
   public void setUserArn(String userArn)
   {
      this.userArn = userArn;
   }



   /*******************************************************************************
    ** Fluent setter for userArn
    **
    *******************************************************************************/
   public QuickSightChartMetaData withUserArn(String userArn)
   {
      this.userArn = userArn;
      return this;
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
   public QuickSightChartMetaData withRegion(String region)
   {
      this.region = region;
      return this;
   }



   /*******************************************************************************
    ** Getter for allowedDomains
    **
    *******************************************************************************/
   public Collection<String> getAllowedDomains()
   {
      return allowedDomains;
   }



   /*******************************************************************************
    ** Setter for allowedDomains
    **
    *******************************************************************************/
   public void setAllowedDomains(Collection<String> allowedDomains)
   {
      this.allowedDomains = allowedDomains;
   }



   /*******************************************************************************
    ** Fluent setter for allowedDomains
    **
    *******************************************************************************/
   public QuickSightChartMetaData withAllowedDomains(Collection<String> allowedDomains)
   {
      this.allowedDomains = allowedDomains;
      return this;
   }



   /*******************************************************************************
    ** Getter for defaultValues
    **
    *******************************************************************************/
   public Map<String, Serializable> getDefaultValues()
   {
      return defaultValues;
   }



   /*******************************************************************************
    ** Setter for defaultValues
    **
    *******************************************************************************/
   public void setDefaultValues(Map<String, Serializable> defaultValues)
   {
      this.defaultValues = defaultValues;
   }



   /*******************************************************************************
    ** Fluent setter for defaultValues
    **
    *******************************************************************************/
   public QWidgetMetaData withDefaultValues(Map<String, Serializable> defaultValues)
   {
      this.defaultValues = defaultValues;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for a single defaultValue
    **
    *******************************************************************************/
   public QWidgetMetaData withDefaultValue(String key, Serializable value)
   {
      if(this.defaultValues == null)
      {
         this.defaultValues = new LinkedHashMap<>();
      }

      this.defaultValues.put(key, value);

      return (this);
   }
}
