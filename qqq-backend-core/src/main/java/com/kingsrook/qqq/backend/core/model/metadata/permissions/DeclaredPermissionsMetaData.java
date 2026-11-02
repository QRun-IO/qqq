/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.permissions.AvailablePermission;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;


/*******************************************************************************
 ** Registry for permissions a module checks by name that belong to no table,
 ** process, app, report or widget (e.g., jobs_admin), so that they are listed by
 ** PermissionsHelper.getAllAvailablePermissions like every other permission.
 *******************************************************************************/
public class DeclaredPermissionsMetaData implements QSupplementalInstanceMetaData, Cloneable
{
   public static final String NAME = "declaredPermissions";

   private List<AvailablePermission> permissions = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static DeclaredPermissionsMetaData of(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.of(instance, NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static DeclaredPermissionsMetaData ofOrWithNew(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, DeclaredPermissionsMetaData::new));
   }



   /*******************************************************************************
    ** Declare a permission, replacing a previous declaration of the same name.
    *******************************************************************************/
   public DeclaredPermissionsMetaData withPermission(AvailablePermission permission)
   {
      permissions.removeIf(existing -> Objects.equals(existing.getName(), permission.getName()));
      permissions.add(permission);
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissions
    *******************************************************************************/
   public List<AvailablePermission> getPermissions()
   {
      return (permissions);
   }



   /*******************************************************************************
    ** Copy entries without sharing mutable state.
    *******************************************************************************/
   @Override
   public DeclaredPermissionsMetaData clone()
   {
      DeclaredPermissionsMetaData clone = new DeclaredPermissionsMetaData();
      for(AvailablePermission permission : permissions)
      {
         clone.permissions.add(new AvailablePermission()
            .withName(permission.getName())
            .withObjectName(permission.getObjectName())
            .withObjectType(permission.getObjectType())
            .withPermissionType(permission.getPermissionType()));
      }
      return (clone);
   }
}
