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




/*******************************************************************************
 ** DeliveryTargetOption.
 *******************************************************************************/
public class DeliveryTargetOption implements java.io.Serializable
{
   private String value;
   private String label;
   private String description;



   /*******************************************************************************
    ** Get value.
    *******************************************************************************/
   public String getValue()
   {
      return (value);
   }



   /*******************************************************************************
    ** Set value.
    *******************************************************************************/
   public void setValue(String value)
   {
      this.value = value;
   }



   /*******************************************************************************
    ** With value.
    *******************************************************************************/
   public DeliveryTargetOption withValue(String value)
   {
      this.value = value;
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
   public DeliveryTargetOption withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Get description.
    *******************************************************************************/
   public String getDescription()
   {
      return (description);
   }



   /*******************************************************************************
    ** Set description.
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** With description.
    *******************************************************************************/
   public DeliveryTargetOption withDescription(String description)
   {
      this.description = description;
      return (this);
   }
}
