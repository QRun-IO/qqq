/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.messaging;


/*******************************************************************************
 **
 *******************************************************************************/
public class Party
{
   private String    label;
   private String    address;
   private PartyRole role;



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
   public Party withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for address
    *******************************************************************************/
   public String getAddress()
   {
      return (this.address);
   }



   /*******************************************************************************
    ** Setter for address
    *******************************************************************************/
   public void setAddress(String address)
   {
      this.address = address;
   }



   /*******************************************************************************
    ** Fluent setter for address
    *******************************************************************************/
   public Party withAddress(String address)
   {
      this.address = address;
      return (this);
   }



   /*******************************************************************************
    ** Getter for role
    *******************************************************************************/
   public PartyRole getRole()
   {
      return (this.role);
   }



   /*******************************************************************************
    ** Setter for role
    *******************************************************************************/
   public void setRole(PartyRole role)
   {
      this.role = role;
   }



   /*******************************************************************************
    ** Fluent setter for role
    *******************************************************************************/
   public Party withRole(PartyRole role)
   {
      this.role = role;
      return (this);
   }

}
