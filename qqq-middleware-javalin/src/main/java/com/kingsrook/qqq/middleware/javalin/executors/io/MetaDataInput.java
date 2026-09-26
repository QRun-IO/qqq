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

package com.kingsrook.qqq.middleware.javalin.executors.io;


/*******************************************************************************
 **
 *******************************************************************************/
public class MetaDataInput extends AbstractMiddlewareInput
{
   private String frontendName;
   private String frontendVersion;

   private String middlewareName;
   private String middlewareVersion;

   private String applicationName;
   private String applicationVersion;



   /*******************************************************************************
    ** Getter for frontendName
    *******************************************************************************/
   public String getFrontendName()
   {
      return (this.frontendName);
   }



   /*******************************************************************************
    ** Setter for frontendName
    *******************************************************************************/
   public void setFrontendName(String frontendName)
   {
      this.frontendName = frontendName;
   }



   /*******************************************************************************
    ** Fluent setter for frontendName
    *******************************************************************************/
   public MetaDataInput withFrontendName(String frontendName)
   {
      this.frontendName = frontendName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for frontendVersion
    *******************************************************************************/
   public String getFrontendVersion()
   {
      return (this.frontendVersion);
   }



   /*******************************************************************************
    ** Setter for frontendVersion
    *******************************************************************************/
   public void setFrontendVersion(String frontendVersion)
   {
      this.frontendVersion = frontendVersion;
   }



   /*******************************************************************************
    ** Fluent setter for frontendVersion
    *******************************************************************************/
   public MetaDataInput withFrontendVersion(String frontendVersion)
   {
      this.frontendVersion = frontendVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for middlewareName
    *******************************************************************************/
   public String getMiddlewareName()
   {
      return (this.middlewareName);
   }



   /*******************************************************************************
    ** Setter for middlewareName
    *******************************************************************************/
   public void setMiddlewareName(String middlewareName)
   {
      this.middlewareName = middlewareName;
   }



   /*******************************************************************************
    ** Fluent setter for middlewareName
    *******************************************************************************/
   public MetaDataInput withMiddlewareName(String middlewareName)
   {
      this.middlewareName = middlewareName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for middlewareVersion
    *******************************************************************************/
   public String getMiddlewareVersion()
   {
      return (this.middlewareVersion);
   }



   /*******************************************************************************
    ** Setter for middlewareVersion
    *******************************************************************************/
   public void setMiddlewareVersion(String middlewareVersion)
   {
      this.middlewareVersion = middlewareVersion;
   }



   /*******************************************************************************
    ** Fluent setter for middlewareVersion
    *******************************************************************************/
   public MetaDataInput withMiddlewareVersion(String middlewareVersion)
   {
      this.middlewareVersion = middlewareVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for applicationName
    *******************************************************************************/
   public String getApplicationName()
   {
      return (this.applicationName);
   }



   /*******************************************************************************
    ** Setter for applicationName
    *******************************************************************************/
   public void setApplicationName(String applicationName)
   {
      this.applicationName = applicationName;
   }



   /*******************************************************************************
    ** Fluent setter for applicationName
    *******************************************************************************/
   public MetaDataInput withApplicationName(String applicationName)
   {
      this.applicationName = applicationName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for applicationVersion
    *******************************************************************************/
   public String getApplicationVersion()
   {
      return (this.applicationVersion);
   }



   /*******************************************************************************
    ** Setter for applicationVersion
    *******************************************************************************/
   public void setApplicationVersion(String applicationVersion)
   {
      this.applicationVersion = applicationVersion;
   }



   /*******************************************************************************
    ** Fluent setter for applicationVersion
    *******************************************************************************/
   public MetaDataInput withApplicationVersion(String applicationVersion)
   {
      this.applicationVersion = applicationVersion;
      return (this);
   }

}
