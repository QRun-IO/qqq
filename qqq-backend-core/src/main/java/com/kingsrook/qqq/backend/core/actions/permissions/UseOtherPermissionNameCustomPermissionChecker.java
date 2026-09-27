/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.InitializableViaCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithName;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 * Simple implementation of a Custom Permission Checker, that takes a permissionName
 * property, and just checks if the session has that permission.
 *******************************************************************************/
public class UseOtherPermissionNameCustomPermissionChecker implements CustomPermissionChecker, InitializableViaCodeReference
{
   private static final QLogger LOG = QLogger.getLogger(UseOtherPermissionNameCustomPermissionChecker.class);

   public static final String PERMISSION_NAME_PROPERTY = "permissionName";

   private String permissionName;



   /***************************************************************************
    * Factory method to build a code reference to this class, with the given
    * permission name.
    * @param permissionName Full name of the permission to be checked by instances
    *                       of this class
    * @return code reference that can be passed to, e.g.,
    * {@link com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules#withCustomPermissionChecker(QCodeReference)}
    ***************************************************************************/
   public static QCodeReferenceWithProperties build(String permissionName)
   {
      return (new QCodeReferenceWithProperties(UseOtherPermissionNameCustomPermissionChecker.class, MapBuilder.of(PERMISSION_NAME_PROPERTY, permissionName)));
   }



   /***************************************************************************
    * As per {@link InitializableViaCodeReference}, initialize an instance of this
    * class using the supplied {@link QCodeReference}, which is expected to be a
    * {@link QCodeReferenceWithProperties}.
    ***************************************************************************/
   @Override
   public void initialize(QCodeReference codeReference)
   {
      if(codeReference instanceof QCodeReferenceWithProperties codeReferenceWithProperties)
      {
         this.permissionName = ValueUtils.getValueAsString(codeReferenceWithProperties.getProperties().get(PERMISSION_NAME_PROPERTY));
      }
   }



   /***************************************************************************
    * Per {@link CustomPermissionChecker}, check if the active session has the
    * permission specified as this class's permissionName property.
    ***************************************************************************/
   @Override
   public void checkPermissionsThrowing(AbstractActionInput actionInput, MetaDataWithPermissionRules metaDataWithPermissionRules) throws QPermissionDeniedException
   {
      if(!StringUtils.hasContent(permissionName))
      {
         LOG.warn("Missing permissionName in a custom permission checker");
      }

      QSession session = QContext.getQSession();
      if(session == null || !session.hasPermission(permissionName))
      {
         throw (new QPermissionDeniedException("Permission denied"));
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean handlesBuildAvailablePermission()
   {
      return (true);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public AvailablePermission buildAvailablePermission(QPermissionRules rules, PermissionSubType permissionSubType, String baseName, MetaDataWithName metaDataWithName, String objectType)
   {
      AvailablePermission availablePermission = buildBaseAvailablePermission(rules, permissionSubType, metaDataWithName, objectType);
      if(availablePermission == null)
      {
         return (null);
      }

      return availablePermission.withName(permissionName);
   }



   /*******************************************************************************
    * Getter for permissionName
    * @see #withPermissionName(String)
    *******************************************************************************/
   public String getPermissionName()
   {
      return (this.permissionName);
   }



   /*******************************************************************************
    * Setter for permissionName
    * @see #withPermissionName(String)
    *******************************************************************************/
   public void setPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
   }



   /*******************************************************************************
    * Fluent setter for permissionName
    *
    * @param permissionName
    * The full permission name that is checked by this checker.  note that this value
    * is not expected to be directly set via this setter - but rather, via the
    * initialization of this class through a {@link QCodeReferenceWithProperties}.
    * @return this
    *******************************************************************************/
   public UseOtherPermissionNameCustomPermissionChecker withPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
      return (this);
   }
}
