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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;


/*******************************************************************************
 ** Bean representing an element of a query order-by clause.
 **
 *******************************************************************************/
public class QFilterOrderBy implements Serializable, Cloneable, QMetaDataObject
{
   private String  fieldName;
   private boolean isAscending = true;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QFilterOrderBy clone()
   {
      try
      {
         return (QFilterOrderBy) super.clone();
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    ** Default no-arg constructor
    *******************************************************************************/
   public QFilterOrderBy()
   {

   }



   /*******************************************************************************
    ** Constructor that sets field name, but leaves default for isAscending (true)
    *******************************************************************************/
   public QFilterOrderBy(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Constructor that takes field name and isAscending.
    *******************************************************************************/
   public QFilterOrderBy(String fieldName, boolean isAscending)
   {
      this.fieldName = fieldName;
      this.isAscending = isAscending;
   }



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
    ** Fluent Setter for fieldName
    **
    *******************************************************************************/
   public QFilterOrderBy withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isAscending
    **
    *******************************************************************************/
   public boolean getIsAscending()
   {
      return isAscending;
   }



   /*******************************************************************************
    ** Setter for isAscending
    **
    *******************************************************************************/
   public void setIsAscending(boolean ascending)
   {
      isAscending = ascending;
   }



   /*******************************************************************************
    ** Fluent Setter for isAscending
    **
    *******************************************************************************/
   public QFilterOrderBy withIsAscending(boolean ascending)
   {
      this.isAscending = ascending;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (fieldName + " " + (isAscending ? "ASC" : "DESC"));
   }
}
