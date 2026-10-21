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

package com.kingsrook.qqq.backend.core.model.data;


import java.lang.reflect.Method;


/*******************************************************************************
 ** Reflective information about an association in a QRecordEntity
 *******************************************************************************/
public class QRecordEntityAssociation
{
   private final String fieldName;
   private final Method getter;
   private final Method setter;

   private final Class<? extends QRecordEntity> associatedType;

   private final QAssociation associationAnnotation;



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public QRecordEntityAssociation(String fieldName, Method getter, Method setter, Class<? extends QRecordEntity> associatedType, QAssociation associationAnnotation)
   {
      this.fieldName = fieldName;
      this.getter = getter;
      this.setter = setter;
      this.associatedType = associatedType;
      this.associationAnnotation = associationAnnotation;
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
    ** Getter for getter
    **
    *******************************************************************************/
   public Method getGetter()
   {
      return getter;
   }



   /*******************************************************************************
    ** Getter for setter
    **
    *******************************************************************************/
   public Method getSetter()
   {
      return setter;
   }



   /*******************************************************************************
    ** Getter for associatedType
    **
    *******************************************************************************/
   public Class<? extends QRecordEntity> getAssociatedType()
   {
      return associatedType;
   }



   /*******************************************************************************
    ** Getter for associationAnnotation
    **
    *******************************************************************************/
   public QAssociation getAssociationAnnotation()
   {
      return associationAnnotation;
   }

}
