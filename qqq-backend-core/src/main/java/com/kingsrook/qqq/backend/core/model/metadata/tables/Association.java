/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;


/*******************************************************************************
 ** definition of a qqq table that is "associated" with another table, e.g.,
 ** managed along with it - such as child-records under a parent record.
 *******************************************************************************/
public class Association implements QMetaDataObject, Cloneable
{
   private String name;
   private String associatedTableName;
   private String joinName;



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
   public Association withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for associatedTableName
    *******************************************************************************/
   public String getAssociatedTableName()
   {
      return (this.associatedTableName);
   }



   /*******************************************************************************
    ** Setter for associatedTableName
    *******************************************************************************/
   public void setAssociatedTableName(String associatedTableName)
   {
      this.associatedTableName = associatedTableName;
   }



   /*******************************************************************************
    ** Fluent setter for associatedTableName
    *******************************************************************************/
   public Association withAssociatedTableName(String associatedTableName)
   {
      this.associatedTableName = associatedTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for joinName
    *******************************************************************************/
   public String getJoinName()
   {
      return (this.joinName);
   }



   /*******************************************************************************
    ** Setter for joinName
    *******************************************************************************/
   public void setJoinName(String joinName)
   {
      this.joinName = joinName;
   }



   /*******************************************************************************
    ** Fluent setter for joinName
    *******************************************************************************/
   public Association withJoinName(String joinName)
   {
      this.joinName = joinName;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public Association clone()
   {
      try
      {
         Association clone = (Association) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}