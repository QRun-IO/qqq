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


/*******************************************************************************
 ** HelpBundleEntry.
 *******************************************************************************/
public class HelpBundleEntry implements Serializable
{
   private String key;
   private String role = "ALL_SCREENS";
   private String summary;
   private String content;
   private String format;
   private boolean retired;



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Fluent setter for key
    *******************************************************************************/
   public HelpBundleEntry withKey(String key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Getter for role
    *******************************************************************************/
   public String getRole()
   {
      return (role);
   }



   /*******************************************************************************
    ** Setter for role
    *******************************************************************************/
   public void setRole(String role)
   {
      this.role = role;
   }



   /*******************************************************************************
    ** Fluent setter for role
    *******************************************************************************/
   public HelpBundleEntry withRole(String role)
   {
      this.role = role;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public HelpBundleEntry withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for content
    *******************************************************************************/
   public String getContent()
   {
      return (content);
   }



   /*******************************************************************************
    ** Setter for content
    *******************************************************************************/
   public void setContent(String content)
   {
      this.content = content;
   }



   /*******************************************************************************
    ** Fluent setter for content
    *******************************************************************************/
   public HelpBundleEntry withContent(String content)
   {
      this.content = content;
      return (this);
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   public String getFormat()
   {
      return (format == null && content != null ? "MARKDOWN" : format);
   }



   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(String format)
   {
      this.format = format;
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public HelpBundleEntry withFormat(String format)
   {
      this.format = format;
      return (this);
   }



   /*******************************************************************************
    ** Getter for retired
    *******************************************************************************/
   public boolean getRetired()
   {
      return (retired);
   }



   /*******************************************************************************
    ** Setter for retired
    *******************************************************************************/
   public void setRetired(boolean retired)
   {
      this.retired = retired;
   }



   /*******************************************************************************
    ** Fluent setter for retired
    *******************************************************************************/
   public HelpBundleEntry withRetired(boolean retired)
   {
      this.retired = retired;
      return (this);
   }
}
