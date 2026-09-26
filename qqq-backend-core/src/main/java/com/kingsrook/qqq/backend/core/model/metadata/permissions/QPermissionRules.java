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

package com.kingsrook.qqq.backend.core.model.metadata.permissions;


import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 **
 *******************************************************************************/
public class QPermissionRules implements Cloneable, QMetaDataObject
{
   private PermissionLevel level;
   private DenyBehavior    denyBehavior;
   private String          permissionBaseName;

   private QCodeReference customPermissionChecker;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QPermissionRules defaultInstance()
   {
      return new QPermissionRules()
         .withLevel(PermissionLevel.NOT_PROTECTED)
         .withDenyBehavior(DenyBehavior.HIDDEN);
   }



   /*******************************************************************************
    ** Getter for level
    *******************************************************************************/
   public PermissionLevel getLevel()
   {
      return (this.level);
   }



   /*******************************************************************************
    ** Setter for level
    *******************************************************************************/
   public void setLevel(PermissionLevel level)
   {
      this.level = level;
   }



   /*******************************************************************************
    ** Fluent setter for level
    *******************************************************************************/
   public QPermissionRules withLevel(PermissionLevel level)
   {
      this.level = level;
      return (this);
   }



   /*******************************************************************************
    ** Getter for denyBehavior
    *******************************************************************************/
   public DenyBehavior getDenyBehavior()
   {
      return (this.denyBehavior);
   }



   /*******************************************************************************
    ** Setter for denyBehavior
    *******************************************************************************/
   public void setDenyBehavior(DenyBehavior denyBehavior)
   {
      this.denyBehavior = denyBehavior;
   }



   /*******************************************************************************
    ** Fluent setter for denyBehavior
    *******************************************************************************/
   public QPermissionRules withDenyBehavior(DenyBehavior denyBehavior)
   {
      this.denyBehavior = denyBehavior;
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionBaseName
    *******************************************************************************/
   public String getPermissionBaseName()
   {
      return (this.permissionBaseName);
   }



   /*******************************************************************************
    ** Setter for permissionBaseName
    *******************************************************************************/
   public void setPermissionBaseName(String permissionBaseName)
   {
      this.permissionBaseName = permissionBaseName;
   }



   /*******************************************************************************
    ** Fluent setter for permissionBaseName
    *******************************************************************************/
   public QPermissionRules withPermissionBaseName(String permissionBaseName)
   {
      this.permissionBaseName = permissionBaseName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QPermissionRules clone()
   {
      try
      {
         QPermissionRules clone = (QPermissionRules) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    ** Getter for customPermissionChecker
    *******************************************************************************/
   public QCodeReference getCustomPermissionChecker()
   {
      return (this.customPermissionChecker);
   }



   /*******************************************************************************
    ** Setter for customPermissionChecker
    *******************************************************************************/
   public void setCustomPermissionChecker(QCodeReference customPermissionChecker)
   {
      this.customPermissionChecker = customPermissionChecker;
   }



   /*******************************************************************************
    ** Fluent setter for customPermissionChecker
    *******************************************************************************/
   public QPermissionRules withCustomPermissionChecker(QCodeReference customPermissionChecker)
   {
      this.customPermissionChecker = customPermissionChecker;
      return (this);
   }

}
