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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.io.Serializable;


/*******************************************************************************
 ** Compatibility view of the core FieldAccessHelper decision and why: "hidden",
 ** "password", "field security lock", "metadata" (explicit AiFieldMetaData),
 ** "table" (capped by the table's exposure) or "default".  canPropose is the
 ** user's own edit permission on the table and exposure other than NONE
 ** (informational until write proposals exist).
 *******************************************************************************/
public class AiFieldAccess implements Serializable
{
   private String     fieldName;
   private String     label;
   private AiExposure exposure;
   private String     reason;
   private Boolean    canPropose;


   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Setter for fieldName
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    *******************************************************************************/
   public AiFieldAccess withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
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
   public AiFieldAccess withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for exposure
    *******************************************************************************/
   public AiExposure getExposure()
   {
      return (this.exposure);
   }



   /*******************************************************************************
    ** Setter for exposure
    *******************************************************************************/
   public void setExposure(AiExposure exposure)
   {
      this.exposure = exposure;
   }



   /*******************************************************************************
    ** Fluent setter for exposure
    *******************************************************************************/
   public AiFieldAccess withExposure(AiExposure exposure)
   {
      this.exposure = exposure;
      return (this);
   }



   /*******************************************************************************
    ** Getter for reason
    *******************************************************************************/
   public String getReason()
   {
      return (this.reason);
   }



   /*******************************************************************************
    ** Setter for reason
    *******************************************************************************/
   public void setReason(String reason)
   {
      this.reason = reason;
   }



   /*******************************************************************************
    ** Fluent setter for reason
    *******************************************************************************/
   public AiFieldAccess withReason(String reason)
   {
      this.reason = reason;
      return (this);
   }



   /*******************************************************************************
    ** Getter for canPropose
    *******************************************************************************/
   public Boolean getCanPropose()
   {
      return (this.canPropose);
   }



   /*******************************************************************************
    ** Setter for canPropose
    *******************************************************************************/
   public void setCanPropose(Boolean canPropose)
   {
      this.canPropose = canPropose;
   }



   /*******************************************************************************
    ** Fluent setter for canPropose
    *******************************************************************************/
   public AiFieldAccess withCanPropose(Boolean canPropose)
   {
      this.canPropose = canPropose;
      return (this);
   }
}
