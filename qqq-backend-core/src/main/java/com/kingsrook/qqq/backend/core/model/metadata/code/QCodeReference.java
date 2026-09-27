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

package com.kingsrook.qqq.backend.core.model.metadata.code;


import java.io.Serializable;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;


/*******************************************************************************
 ** Pointer to code to be ran by the qqq framework, e.g., for custom behavior -
 ** maybe process steps, maybe customization to a table, etc.
 *******************************************************************************/
public class QCodeReference implements Serializable, Cloneable, QMetaDataObject
{
   private String    name;
   private QCodeType codeType;

   private String inlineCode;



   /*******************************************************************************
    ** Default empty constructor
    *******************************************************************************/
   public QCodeReference()
   {
   }



   /*******************************************************************************
    ** Constructor that takes all args
    *******************************************************************************/
   public QCodeReference(String name, QCodeType codeType)
   {
      this.name = name;
      this.codeType = codeType;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QCodeReference clone()
   {
      try
      {
         QCodeReference clone = (QCodeReference) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "QCodeReference{name='" + name + "'}";
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      QCodeReference that = (QCodeReference) o;
      return Objects.equals(name, that.name) && codeType == that.codeType && Objects.equals(inlineCode, that.inlineCode);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(name, codeType, inlineCode);
   }



   /*******************************************************************************
    ** Constructor that just takes a java class, and infers the other fields.
    *******************************************************************************/
   public QCodeReference(Class<?> javaClass)
   {
      this.name = javaClass.getName();
      this.codeType = QCodeType.JAVA;
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public QCodeReference withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for codeType
    **
    *******************************************************************************/
   public QCodeType getCodeType()
   {
      return codeType;
   }



   /*******************************************************************************
    ** Setter for codeType
    **
    *******************************************************************************/
   public void setCodeType(QCodeType codeType)
   {
      this.codeType = codeType;
   }



   /*******************************************************************************
    ** Setter for codeType
    **
    *******************************************************************************/
   public QCodeReference withCodeType(QCodeType codeType)
   {
      this.codeType = codeType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for inlineCode
    **
    *******************************************************************************/
   public String getInlineCode()
   {
      return inlineCode;
   }



   /*******************************************************************************
    ** Setter for inlineCode
    **
    *******************************************************************************/
   public void setInlineCode(String inlineCode)
   {
      this.inlineCode = inlineCode;
   }



   /*******************************************************************************
    ** Fluent setter for inlineCode
    **
    *******************************************************************************/
   public QCodeReference withInlineCode(String inlineCode)
   {
      this.inlineCode = inlineCode;
      return (this);
   }
}
