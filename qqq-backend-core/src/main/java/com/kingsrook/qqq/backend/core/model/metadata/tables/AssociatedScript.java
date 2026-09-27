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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 **
 *******************************************************************************/
public class AssociatedScript implements Serializable, Cloneable
{
   private String         fieldName;
   private Serializable   scriptTypeId;
   private QCodeReference scriptTester;



   /*******************************************************************************
    ** Getter for fieldName
    **
    *******************************************************************************/
   public String getFieldName()
   {
      return fieldName;
   }



   /*******************************************************************************
    ** Setter for fieldName
    **
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    **
    *******************************************************************************/
   public AssociatedScript withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptTypeId
    **
    *******************************************************************************/
   public Serializable getScriptTypeId()
   {
      return scriptTypeId;
   }



   /*******************************************************************************
    ** Setter for scriptTypeId
    **
    *******************************************************************************/
   public void setScriptTypeId(Serializable scriptTypeId)
   {
      this.scriptTypeId = scriptTypeId;
   }



   /*******************************************************************************
    ** Fluent setter for scriptTypeId
    **
    *******************************************************************************/
   public AssociatedScript withScriptTypeId(Serializable scriptTypeId)
   {
      this.scriptTypeId = scriptTypeId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptTester
    **
    *******************************************************************************/
   public QCodeReference getScriptTester()
   {
      return scriptTester;
   }



   /*******************************************************************************
    ** Setter for scriptTester
    **
    *******************************************************************************/
   public void setScriptTester(QCodeReference scriptTester)
   {
      this.scriptTester = scriptTester;
   }



   /*******************************************************************************
    ** Fluent setter for scriptTester
    **
    *******************************************************************************/
   public AssociatedScript withScriptTester(QCodeReference scriptTester)
   {
      this.scriptTester = scriptTester;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public AssociatedScript clone()
   {
      try
      {
         AssociatedScript clone = (AssociatedScript) super.clone();
         if(scriptTester != null)
         {
            scriptTester = scriptTester.clone();
         }
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
