/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.executors.io.VersionOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.UiApiExtensionInfo;


/*******************************************************************************
 ** Response for GET /version.
 *******************************************************************************/
public class VersionResponseV1 implements VersionOutputInterface, ToSchema
{
   @OpenAPIDescription("Semantic version (MAJOR.MINOR.PATCH) of the UI API this server implements. MAJOR matches the URL version (v1). A client built against 1.x works with any server 1.y where y >= x.")
   private String apiVersion;

   @OpenAPIDescription("Base path of this API version, e.g. /qqq/v1.")
   private String basePath;

   @OpenAPIDescription("The major versions of the UI API this server serves.")
   @OpenAPIListItems(value = Integer.class)
   private List<Integer> supportedMajorVersions;

   @OpenAPIDescription("Names of the optional core features this server supports, so a client can show or hide an area without probing.")
   @OpenAPIListItems(value = String.class)
   private List<String> features;

   @OpenAPIDescription("Extensions (modules and qbits) registered on this server, each with its own path prefix and semantic version.")
   @OpenAPIListItems(value = UiApiExtensionInfo.class, useRef = true)
   private List<UiApiExtensionInfo> extensions;

   @OpenAPIDescription("Client channel names the server accepts in the X-QQQ-Client request header (`channel[/detail]`, e.g. next-ui, assistant/<turnId>). A client names itself on every request; it is recorded in audits and some objects are denied to some channels. The actor is always the signed-in user; a channel never grants anything.")
   @OpenAPIListItems(value = String.class)
   private List<String> clientChannels;



   /*******************************************************************************
    ** Getter for apiVersion
    *******************************************************************************/
   public String getApiVersion()
   {
      return (this.apiVersion);
   }



   /*******************************************************************************
    ** Setter for apiVersion
    *******************************************************************************/
   @Override
   public void setApiVersion(String apiVersion)
   {
      this.apiVersion = apiVersion;
   }



   /*******************************************************************************
    ** Getter for basePath
    *******************************************************************************/
   public String getBasePath()
   {
      return (this.basePath);
   }



   /*******************************************************************************
    ** Setter for basePath
    *******************************************************************************/
   @Override
   public void setBasePath(String basePath)
   {
      this.basePath = basePath;
   }



   /*******************************************************************************
    ** Getter for supportedMajorVersions
    *******************************************************************************/
   public List<Integer> getSupportedMajorVersions()
   {
      return (this.supportedMajorVersions);
   }



   /*******************************************************************************
    ** Setter for supportedMajorVersions
    *******************************************************************************/
   @Override
   public void setSupportedMajorVersions(List<Integer> supportedMajorVersions)
   {
      this.supportedMajorVersions = supportedMajorVersions;
   }



   /*******************************************************************************
    ** Getter for features
    *******************************************************************************/
   public List<String> getFeatures()
   {
      return (this.features);
   }



   /*******************************************************************************
    ** Setter for features
    *******************************************************************************/
   @Override
   public void setFeatures(List<String> features)
   {
      this.features = features;
   }



   /*******************************************************************************
    ** Getter for extensions
    *******************************************************************************/
   public List<UiApiExtensionInfo> getExtensions()
   {
      return (this.extensions);
   }



   /*******************************************************************************
    ** Setter for extensions
    *******************************************************************************/
   @Override
   public void setExtensions(List<UiApiExtensionInfo> extensions)
   {
      this.extensions = extensions;
   }



   /*******************************************************************************
    ** Getter for clientChannels
    *******************************************************************************/
   public List<String> getClientChannels()
   {
      return (this.clientChannels);
   }



   /*******************************************************************************
    ** Setter for clientChannels
    *******************************************************************************/
   @Override
   public void setClientChannels(List<String> clientChannels)
   {
      this.clientChannels = clientChannels;
   }
}
