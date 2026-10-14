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
 ** A label/value pair in a KEY_VALUES {@link DeveloperPanelBlock}.
 *******************************************************************************/
public class DeveloperPanelKeyValue implements Serializable
{
   /** The label. */
   private String label;

   /** The value, as display text (null shows as empty). */
   private String value;

   /** Show the value in a monospace font (identifiers, keys, topics). */
   private Boolean monospace;

   /** Optional link for the value (app-relative, e.g., /app/table/1, or absolute). */
   private String href;



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
   public DeveloperPanelKeyValue withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for value
    *******************************************************************************/
   public String getValue()
   {
      return (this.value);
   }



   /*******************************************************************************
    ** Setter for value
    *******************************************************************************/
   public void setValue(String value)
   {
      this.value = value;
   }



   /*******************************************************************************
    ** Fluent setter for value
    *******************************************************************************/
   public DeveloperPanelKeyValue withValue(String value)
   {
      this.value = value;
      return (this);
   }



   /*******************************************************************************
    ** Getter for monospace
    *******************************************************************************/
   public Boolean getMonospace()
   {
      return (this.monospace);
   }



   /*******************************************************************************
    ** Setter for monospace
    *******************************************************************************/
   public void setMonospace(Boolean monospace)
   {
      this.monospace = monospace;
   }



   /*******************************************************************************
    ** Fluent setter for monospace
    *******************************************************************************/
   public DeveloperPanelKeyValue withMonospace(Boolean monospace)
   {
      this.monospace = monospace;
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
   public DeveloperPanelKeyValue withHref(String href)
   {
      this.href = href;
      return (this);
   }

}
