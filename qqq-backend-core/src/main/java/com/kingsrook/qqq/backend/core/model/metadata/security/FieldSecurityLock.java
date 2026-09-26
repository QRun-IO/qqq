/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.security;


import java.io.Serializable;
import java.util.List;


/*******************************************************************************
 ** Define, for a field, a lock that controls if users can or cannot see the field.
 *******************************************************************************/
public class FieldSecurityLock
{
   private String             securityKeyType;
   private Behavior           defaultBehavior = Behavior.DENY;
   private List<Serializable> overrideValues;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FieldSecurityLock()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public enum Behavior
   {
      ALLOW,
      DENY
   }



   /*******************************************************************************
    ** Getter for securityKeyType
    *******************************************************************************/
   public String getSecurityKeyType()
   {
      return (this.securityKeyType);
   }



   /*******************************************************************************
    ** Setter for securityKeyType
    *******************************************************************************/
   public void setSecurityKeyType(String securityKeyType)
   {
      this.securityKeyType = securityKeyType;
   }



   /*******************************************************************************
    ** Fluent setter for securityKeyType
    *******************************************************************************/
   public FieldSecurityLock withSecurityKeyType(String securityKeyType)
   {
      this.securityKeyType = securityKeyType;
      return (this);
   }




   /*******************************************************************************
    ** Getter for defaultBehavior
    *******************************************************************************/
   public Behavior getDefaultBehavior()
   {
      return (this.defaultBehavior);
   }



   /*******************************************************************************
    ** Setter for defaultBehavior
    *******************************************************************************/
   public void setDefaultBehavior(Behavior defaultBehavior)
   {
      this.defaultBehavior = defaultBehavior;
   }



   /*******************************************************************************
    ** Fluent setter for defaultBehavior
    *******************************************************************************/
   public FieldSecurityLock withDefaultBehavior(Behavior defaultBehavior)
   {
      this.defaultBehavior = defaultBehavior;
      return (this);
   }



   /*******************************************************************************
    ** Getter for overrideValues
    *******************************************************************************/
   public List<Serializable> getOverrideValues()
   {
      return (this.overrideValues);
   }



   /*******************************************************************************
    ** Setter for overrideValues
    *******************************************************************************/
   public void setOverrideValues(List<Serializable> overrideValues)
   {
      this.overrideValues = overrideValues;
   }



   /*******************************************************************************
    ** Fluent setter for overrideValues
    *******************************************************************************/
   public FieldSecurityLock withOverrideValues(List<Serializable> overrideValues)
   {
      this.overrideValues = overrideValues;
      return (this);
   }


}
