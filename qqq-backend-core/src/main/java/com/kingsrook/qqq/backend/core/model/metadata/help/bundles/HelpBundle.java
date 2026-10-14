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

package com.kingsrook.qqq.backend.core.model.metadata.help.bundles;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*******************************************************************************
 ** HelpBundle.
 *******************************************************************************/
public class HelpBundle implements Serializable
{
   private String id;
   private String version;
   private HelpBundleLayer layer;
   private List<HelpBundleEntry> entries = new ArrayList<>();



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public String getId()
   {
      return (id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(String id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public HelpBundle withId(String id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for version
    *******************************************************************************/
   public String getVersion()
   {
      return (version);
   }



   /*******************************************************************************
    ** Setter for version
    *******************************************************************************/
   public void setVersion(String version)
   {
      this.version = version;
   }



   /*******************************************************************************
    ** Fluent setter for version
    *******************************************************************************/
   public HelpBundle withVersion(String version)
   {
      this.version = version;
      return (this);
   }



   /*******************************************************************************
    ** Getter for layer
    *******************************************************************************/
   public HelpBundleLayer getLayer()
   {
      return (layer);
   }



   /*******************************************************************************
    ** Setter for layer
    *******************************************************************************/
   public void setLayer(HelpBundleLayer layer)
   {
      this.layer = layer;
   }



   /*******************************************************************************
    ** Fluent setter for layer
    *******************************************************************************/
   public HelpBundle withLayer(HelpBundleLayer layer)
   {
      this.layer = layer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for entries
    *******************************************************************************/
   public List<HelpBundleEntry> getEntries()
   {
      return (entries);
   }



   /*******************************************************************************
    ** Setter for entries
    *******************************************************************************/
   public void setEntries(List<HelpBundleEntry> entries)
   {
      this.entries = entries;
   }



   /*******************************************************************************
    ** Fluent setter for entries
    *******************************************************************************/
   public HelpBundle withEntries(List<HelpBundleEntry> entries)
   {
      this.entries = entries;
      return (this);
   }
}
