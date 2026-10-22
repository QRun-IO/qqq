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

package com.kingsrook.qqq.backend.core.actions.help;


import java.util.List;


/*******************************************************************************
    ** HelpCoverageEntry data.
    *******************************************************************************/
public class HelpCoverageEntry
{
   private String kind;
   private String key;
   private String label;
   private String parentKey;
   private boolean hidden;
   private boolean hasSummary;
   private boolean hasContent;
   private List<String> roles;



   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   public String getKind()
   {
      return (this.kind);
   }



   /*******************************************************************************
    ** Setter for kind
    *******************************************************************************/
   public void setKind(String kind)
   {
      this.kind = kind;
   }



   /*******************************************************************************
    ** Fluent setter for kind
    *******************************************************************************/
   public HelpCoverageEntry withKind(String kind)
   {
      this.kind = kind;
      return (this);
   }



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (this.key);
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
   public HelpCoverageEntry withKey(String key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public HelpCoverageEntry withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for parentKey
    *******************************************************************************/
   public String getParentKey()
   {
      return (this.parentKey);
   }



   /*******************************************************************************
    ** Setter for parentKey
    *******************************************************************************/
   public void setParentKey(String parentKey)
   {
      this.parentKey = parentKey;
   }



   /*******************************************************************************
    ** Fluent setter for parentKey
    *******************************************************************************/
   public HelpCoverageEntry withParentKey(String parentKey)
   {
      this.parentKey = parentKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hidden
    *******************************************************************************/
   public boolean getHidden()
   {
      return (this.hidden);
   }



   /*******************************************************************************
    ** Setter for hidden
    *******************************************************************************/
   public void setHidden(boolean hidden)
   {
      this.hidden = hidden;
   }



   /*******************************************************************************
    ** Fluent setter for hidden
    *******************************************************************************/
   public HelpCoverageEntry withHidden(boolean hidden)
   {
      this.hidden = hidden;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hasSummary
    *******************************************************************************/
   public boolean getHasSummary()
   {
      return (this.hasSummary);
   }



   /*******************************************************************************
    ** Setter for hasSummary
    *******************************************************************************/
   public void setHasSummary(boolean hasSummary)
   {
      this.hasSummary = hasSummary;
   }



   /*******************************************************************************
    ** Fluent setter for hasSummary
    *******************************************************************************/
   public HelpCoverageEntry withHasSummary(boolean hasSummary)
   {
      this.hasSummary = hasSummary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hasContent
    *******************************************************************************/
   public boolean getHasContent()
   {
      return (this.hasContent);
   }



   /*******************************************************************************
    ** Setter for hasContent
    *******************************************************************************/
   public void setHasContent(boolean hasContent)
   {
      this.hasContent = hasContent;
   }



   /*******************************************************************************
    ** Fluent setter for hasContent
    *******************************************************************************/
   public HelpCoverageEntry withHasContent(boolean hasContent)
   {
      this.hasContent = hasContent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for roles
    *******************************************************************************/
   public List<String> getRoles()
   {
      return (this.roles);
   }



   /*******************************************************************************
    ** Setter for roles
    *******************************************************************************/
   public void setRoles(List<String> roles)
   {
      this.roles = roles;
   }



   /*******************************************************************************
    ** Fluent setter for roles
    *******************************************************************************/
   public HelpCoverageEntry withRoles(List<String> roles)
   {
      this.roles = roles;
      return (this);
   }
}
