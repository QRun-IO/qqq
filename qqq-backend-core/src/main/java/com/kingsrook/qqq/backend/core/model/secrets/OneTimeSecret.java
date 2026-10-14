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

package com.kingsrook.qqq.backend.core.model.secrets;




/*******************************************************************************
 ** Returned once to the requesting client; never stored, logged, audited or put in process state.
 *******************************************************************************/
public class OneTimeSecret
{
   private String label;
   private String value;
   private String masked;
   private String usageText;



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
   public OneTimeSecret withLabel(String label)
   {
      this.label = label;
      return (this);
   }



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
   public OneTimeSecret withValue(String value)
   {
      this.value = value;
      return (this);
   }



   /*******************************************************************************
    ** Get usageText.
    *******************************************************************************/
   public String getUsageText()
   {
      return (usageText);
   }



   /*******************************************************************************
    ** Set usageText.
    *******************************************************************************/
   public void setUsageText(String usageText)
   {
      this.usageText = usageText;
   }



   /*******************************************************************************
    ** With usageText.
    *******************************************************************************/
   public OneTimeSecret withUsageText(String usageText)
   {
      this.usageText = usageText;
      return (this);
   }



   /*******************************************************************************
    ** Get masked.
    *******************************************************************************/
   public String getMasked()
   {
      return (masked);
   }



   /*******************************************************************************
    ** Set masked.
    *******************************************************************************/
   public void setMasked(String masked)
   {
      this.masked = masked;
   }



   /*******************************************************************************
    ** With masked.
    *******************************************************************************/
   public OneTimeSecret withMasked(String masked)
   {
      this.masked = masked;
      return (this);
   }



   /*******************************************************************************
    ** Describe only the label and masked representation, never the secret or usage text.
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("OneTimeSecret{label='" + label + "', masked='" + masked + "'}");
   }
}
