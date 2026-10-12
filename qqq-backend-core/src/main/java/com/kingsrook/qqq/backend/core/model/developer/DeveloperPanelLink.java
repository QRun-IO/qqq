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

package com.kingsrook.qqq.backend.core.model.developer;


import java.io.Serializable;


/*******************************************************************************
 ** A link in a LINKS {@link DeveloperPanelBlock}.
 *******************************************************************************/
public class DeveloperPanelLink implements Serializable
{
   /** Link text. */
   private String label;

   /** Target: app-relative (e.g., /app/quickSearchIndex/8) or absolute. */
   private String href;

   /** Optional explanation shown with the link. */
   private String description;

   /** True to open in a new tab (absolute links to other sites). */
   private Boolean external;



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
   public DeveloperPanelLink withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for href
    *******************************************************************************/
   public String getHref()
   {
      return (this.href);
   }



   /*******************************************************************************
    ** Setter for href
    *******************************************************************************/
   public void setHref(String href)
   {
      this.href = href;
   }



   /*******************************************************************************
    ** Fluent setter for href
    *******************************************************************************/
   public DeveloperPanelLink withHref(String href)
   {
      this.href = href;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public DeveloperPanelLink withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for external
    *******************************************************************************/
   public Boolean getExternal()
   {
      return (this.external);
   }



   /*******************************************************************************
    ** Setter for external
    *******************************************************************************/
   public void setExternal(Boolean external)
   {
      this.external = external;
   }



   /*******************************************************************************
    ** Fluent setter for external
    *******************************************************************************/
   public DeveloperPanelLink withExternal(Boolean external)
   {
      this.external = external;
      return (this);
   }

}
