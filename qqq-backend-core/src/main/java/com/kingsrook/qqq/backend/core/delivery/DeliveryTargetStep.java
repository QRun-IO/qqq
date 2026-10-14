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

package com.kingsrook.qqq.backend.core.delivery;


import java.util.List;


/*******************************************************************************
 ** DeliveryTargetStep.
 *******************************************************************************/
public class DeliveryTargetStep implements java.io.Serializable
{
   private String name;
   private String label;
   private List<DeliveryTargetOption> options = List.of();
   private boolean freeText;
   private boolean complete;
   private String summary;



   /*******************************************************************************
    ** Get name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Set name.
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** With name.
    *******************************************************************************/
   public DeliveryTargetStep withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Get label.
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** Set label.
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** With label.
    *******************************************************************************/
   public DeliveryTargetStep withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Get options.
    *******************************************************************************/
   public List<DeliveryTargetOption> getOptions()
   {
      return (options);
   }



   /*******************************************************************************
    ** Set options.
    *******************************************************************************/
   public void setOptions(List<DeliveryTargetOption> options)
   {
      this.options = options;
   }



   /*******************************************************************************
    ** With options.
    *******************************************************************************/
   public DeliveryTargetStep withOptions(List<DeliveryTargetOption> options)
   {
      this.options = options;
      return (this);
   }



   /*******************************************************************************
    ** Get freeText.
    *******************************************************************************/
   public boolean getFreeText()
   {
      return (freeText);
   }



   /*******************************************************************************
    ** Set freeText.
    *******************************************************************************/
   public void setFreeText(boolean freeText)
   {
      this.freeText = freeText;
   }



   /*******************************************************************************
    ** With freeText.
    *******************************************************************************/
   public DeliveryTargetStep withFreeText(boolean freeText)
   {
      this.freeText = freeText;
      return (this);
   }



   /*******************************************************************************
    ** Get complete.
    *******************************************************************************/
   public boolean getComplete()
   {
      return (complete);
   }



   /*******************************************************************************
    ** Set complete.
    *******************************************************************************/
   public void setComplete(boolean complete)
   {
      this.complete = complete;
   }



   /*******************************************************************************
    ** With complete.
    *******************************************************************************/
   public DeliveryTargetStep withComplete(boolean complete)
   {
      this.complete = complete;
      return (this);
   }



   /*******************************************************************************
    ** Get summary.
    *******************************************************************************/
   public String getSummary()
   {
      return (summary);
   }



   /*******************************************************************************
    ** Set summary.
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** With summary.
    *******************************************************************************/
   public DeliveryTargetStep withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }
}
