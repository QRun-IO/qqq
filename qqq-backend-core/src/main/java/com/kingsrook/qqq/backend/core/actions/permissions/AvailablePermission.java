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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 **
 *******************************************************************************/
public class AvailablePermission extends QRecordEntity
{
   public static final String TABLE_NAME = "availablePermission";

   @QField(label = "Permission Name")
   private String name;

   @QField(label = "Object")
   private String objectName;

   @QField()
   private String objectType;

   @QField()
   private String permissionType;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return true;
      }
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      AvailablePermission that = (AvailablePermission) o;
      return Objects.equals(name, that.name) && Objects.equals(objectName, that.objectName) && Objects.equals(objectType, that.objectType) && Objects.equals(permissionType, that.permissionType);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(name, objectName, objectType, permissionType);
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
   public AvailablePermission withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for objectType
    *******************************************************************************/
   public String getObjectType()
   {
      return (this.objectType);
   }



   /*******************************************************************************
    ** Setter for objectType
    *******************************************************************************/
   public void setObjectType(String objectType)
   {
      this.objectType = objectType;
   }



   /*******************************************************************************
    ** Fluent setter for objectType
    *******************************************************************************/
   public AvailablePermission withObjectType(String objectType)
   {
      this.objectType = objectType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionType
    *******************************************************************************/
   public String getPermissionType()
   {
      return (this.permissionType);
   }



   /*******************************************************************************
    ** Setter for permissionType
    *******************************************************************************/
   public void setPermissionType(String permissionType)
   {
      this.permissionType = permissionType;
   }



   /*******************************************************************************
    ** Fluent setter for permissionType
    *******************************************************************************/
   public AvailablePermission withPermissionType(String permissionType)
   {
      this.permissionType = permissionType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for objectName
    *******************************************************************************/
   public String getObjectName()
   {
      return (this.objectName);
   }



   /*******************************************************************************
    ** Setter for objectName
    *******************************************************************************/
   public void setObjectName(String objectName)
   {
      this.objectName = objectName;
   }



   /*******************************************************************************
    ** Fluent setter for objectName
    *******************************************************************************/
   public AvailablePermission withObjectName(String objectName)
   {
      this.objectName = objectName;
      return (this);
   }

}
