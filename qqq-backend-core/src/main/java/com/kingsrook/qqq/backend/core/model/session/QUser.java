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

package com.kingsrook.qqq.backend.core.model.session;


import java.io.Serializable;


/*******************************************************************************
 **
 *******************************************************************************/
public class QUser implements Cloneable, Serializable
{
   private String idReference;
   private String fullName;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QUser clone() throws CloneNotSupportedException
   {
      return (QUser) super.clone();
   }



   /*******************************************************************************
    ** Getter for idReference
    **
    *******************************************************************************/
   public String getIdReference()
   {
      return idReference;
   }



   /*******************************************************************************
    ** Setter for idReference
    **
    *******************************************************************************/
   public void setIdReference(String idReference)
   {
      this.idReference = idReference;
   }



   /*******************************************************************************
    ** Getter for fullName
    **
    *******************************************************************************/
   public String getFullName()
   {
      return fullName;
   }



   /*******************************************************************************
    ** Setter for fullName
    **
    *******************************************************************************/
   public void setFullName(String fullName)
   {
      this.fullName = fullName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("QUser{" + idReference + "," + fullName + "}");
   }

   /*******************************************************************************
    ** Fluent setter for idReference
    *******************************************************************************/
   public QUser withIdReference(String idReference)
   {
      this.idReference = idReference;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for fullName
    *******************************************************************************/
   public QUser withFullName(String fullName)
   {
      this.fullName = fullName;
      return (this);
   }


}
