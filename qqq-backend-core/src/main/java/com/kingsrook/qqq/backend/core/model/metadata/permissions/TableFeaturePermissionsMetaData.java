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
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import org.apache.commons.lang3.SerializationUtils;


/*******************************************************************************
 ** Shared registry for grantable table features from any module.
 *******************************************************************************/
public class TableFeaturePermissionsMetaData implements QSupplementalInstanceMetaData, Cloneable
{
   public static final String NAME = "tableFeaturePermissions";
   private List<TableFeaturePermission> permissions = new ArrayList<>();



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
   public static TableFeaturePermissionsMetaData of(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.of(instance, NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static TableFeaturePermissionsMetaData ofOrWithNew(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, TableFeaturePermissionsMetaData::new));
   }



   /*******************************************************************************
    ** Replace a previous registration of the same suffix, preserving list order.
    *******************************************************************************/
   public TableFeaturePermissionsMetaData withPermission(TableFeaturePermission permission)
   {
      for(int i = 0; i < permissions.size(); i++)
      {
         if(Objects.equals(permissions.get(i).getSuffix(), permission.getSuffix()))
         {
            permissions.set(i, permission);
            return (this);
         }
      }
      permissions.add(permission);
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissions
    *******************************************************************************/
   public List<TableFeaturePermission> getPermissions()
   {
      return (permissions);
   }



   /*******************************************************************************
    ** Copy entries and serializable callbacks without sharing mutable state.
    *******************************************************************************/
   @Override
   public TableFeaturePermissionsMetaData clone()
   {
      return (SerializationUtils.clone(this));
   }

}
