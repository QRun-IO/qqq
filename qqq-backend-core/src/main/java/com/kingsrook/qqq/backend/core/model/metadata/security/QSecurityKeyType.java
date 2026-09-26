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


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;


/*******************************************************************************
 ** Define a type of security key (e.g., a field associated with values), that
 ** can be used to control access to records and/or fields
 *******************************************************************************/
public class QSecurityKeyType implements TopLevelMetaDataInterface
{
   private String name;
   private String allAccessKeyName;
   private String nullValueBehaviorKeyName;
   private String possibleValueSourceName;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QSecurityKeyType()
   {
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public QSecurityKeyType withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allAccessKeyName
    *******************************************************************************/
   public String getAllAccessKeyName()
   {
      return (this.allAccessKeyName);
   }



   /*******************************************************************************
    ** Setter for allAccessKeyName
    *******************************************************************************/
   public void setAllAccessKeyName(String allAccessKeyName)
   {
      this.allAccessKeyName = allAccessKeyName;
   }



   /*******************************************************************************
    ** Fluent setter for allAccessKeyName
    *******************************************************************************/
   public QSecurityKeyType withAllAccessKeyName(String allAccessKeyName)
   {
      this.allAccessKeyName = allAccessKeyName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for possibleValueSourceName
    *******************************************************************************/
   public String getPossibleValueSourceName()
   {
      return (this.possibleValueSourceName);
   }



   /*******************************************************************************
    ** Setter for possibleValueSourceName
    *******************************************************************************/
   public void setPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
   }



   /*******************************************************************************
    ** Fluent setter for possibleValueSourceName
    *******************************************************************************/
   public QSecurityKeyType withPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addSecurityKeyType(this);
   }


   /*******************************************************************************
    ** Getter for nullValueBehaviorKeyName
    *******************************************************************************/
   public String getNullValueBehaviorKeyName()
   {
      return (this.nullValueBehaviorKeyName);
   }



   /*******************************************************************************
    ** Setter for nullValueBehaviorKeyName
    *******************************************************************************/
   public void setNullValueBehaviorKeyName(String nullValueBehaviorKeyName)
   {
      this.nullValueBehaviorKeyName = nullValueBehaviorKeyName;
   }



   /*******************************************************************************
    ** Fluent setter for nullValueBehaviorKeyName
    *******************************************************************************/
   public QSecurityKeyType withNullValueBehaviorKeyName(String nullValueBehaviorKeyName)
   {
      this.nullValueBehaviorKeyName = nullValueBehaviorKeyName;
      return (this);
   }


}
