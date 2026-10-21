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


import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** One-time secret. Shown once; clients must not store secret values.
 *******************************************************************************/
public class PreferenceSecretV1 implements ToSchema
{
   private String label;
   private String value;
   private String masked;
   private String usageText;



   /*******************************************************************************
    ** Describe the wire contract.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT)
         .withProperty("label", new Schema().withType(Type.STRING).withDescription("Human-readable secret label."))
         .withProperty("value", new Schema().withType(Type.STRING).withDescription("Shown once. Clients must not store, log, cache, or send this secret to analytics."))
         .withProperty("masked", new Schema().withType(Type.STRING).withDescription("Masked representation, for example qqq_…7f3a."))
         .withProperty("usageText", new Schema().withType(Type.STRING).withDescription("Usage guidance such as a curl example; may contain newlines.")));
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
   public PreferenceSecretV1 withLabel(String label)
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
   public PreferenceSecretV1 withValue(String value)
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
   public PreferenceSecretV1 withUsageText(String usageText)
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
   public PreferenceSecretV1 withMasked(String masked)
   {
      this.masked = masked;
      return (this);
   }
}
