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


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.InitializableViaCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithName;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 * Simple implementation of a Custom Permission Checker, that takes tableName
 * and TablePermissionSubType name properties, and checks if the session has
 * that table-permission-sub-type for the specified table name.
 *
 * Should handle correctly the permission rules applied to the table in question,
 * e.g., NOT_PROTECTED (effectively making the object checked by this class
 * NOT_PROTECTED), HAS_ACCESS - to tie this object to ${tableName}.hasAccess,
 * READ_WRITE, or READ_INSERT_UPDATE_DELETE, which then bases the permission name
 * on the tablePermissionSubType.
 *******************************************************************************/
public class UseTablePermissionCustomPermissionChecker implements CustomPermissionChecker, InitializableViaCodeReference
{
   private static final QLogger LOG = QLogger.getLogger(UseTablePermissionCustomPermissionChecker.class);

   public static final String TABLE_NAME_PROPERTY                     = "tableName";
   public static final String TABLE_PERMISSION_SUB_TYPE_NAME_PROPERTY = "tablePermissionSubTypeName";

   private String                 tableName;
   private TablePermissionSubType tablePermissionSubType;



   /***************************************************************************
    * Factory method to build a code reference to this class, with the given
    * table name and tablePermissionSubType.
    *
    * @param tableName Name of the table to be checked by instances of this class
    * @param tablePermissionSubType table-permission type to check for.
    * @return code reference that can be passed to, e.g.,
    * {@link com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules#withCustomPermissionChecker(QCodeReference)}
    ***************************************************************************/
   public static QCodeReferenceWithProperties build(String tableName, TablePermissionSubType tablePermissionSubType)
   {
      return (new QCodeReferenceWithProperties(UseTablePermissionCustomPermissionChecker.class, MapBuilder.of(
         TABLE_NAME_PROPERTY, tableName,
         TABLE_PERMISSION_SUB_TYPE_NAME_PROPERTY, tablePermissionSubType.name())));
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
         Map<String, Serializable> properties = codeReferenceWithProperties.getProperties();
         this.tableName = ValueUtils.getValueAsString(properties.get(TABLE_NAME_PROPERTY));
         this.tablePermissionSubType = TablePermissionSubType.valueOf(ValueUtils.getValueAsString(properties.get(TABLE_PERMISSION_SUB_TYPE_NAME_PROPERTY)));
      }
   }



   /***************************************************************************
    * Per {@link CustomPermissionChecker}, check if the active session has the
    * permission specified as this class's permissionName property.
    ***************************************************************************/
   @Override
   public void checkPermissionsThrowing(AbstractActionInput actionInput, MetaDataWithPermissionRules metaDataWithPermissionRules) throws QPermissionDeniedException
   {
      if(!StringUtils.hasContent(tableName))
      {
         LOG.warn("Missing tableName in a custom permission checker");
      }

      if(tablePermissionSubType == null)
      {
         LOG.warn("Missing tablePermissionSubType in a custom permission checker");
      }

      AbstractTableActionInput tableActionInput = new AbstractTableActionInput();
      tableActionInput.setTableName(tableName);

      PermissionsHelper.checkTablePermissionThrowing(tableActionInput, tablePermissionSubType);
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

      String tablePermissionName = PermissionsHelper.getTablePermissionName(getTableName(), getTablePermissionSubType());
      return availablePermission.withName(tablePermissionName);
   }



   /*******************************************************************************
    * Getter for tableName
    * @see #withTableName(String)
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    * Setter for tableName
    * @see #withTableName(String)
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    * Fluent setter for tableName
    *
    * @param tableName
    * The name of the table that is checked by this checker.  Note that this value
    * is not expected to be directly set via this setter - but rather, via the
    * initialization of this class through a {@link QCodeReferenceWithProperties}.
    * @return this
    *******************************************************************************/
   public UseTablePermissionCustomPermissionChecker withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    * Getter for tablePermissionSubType
    * @see #withTablePermissionSubType(TablePermissionSubType)
    *******************************************************************************/
   public TablePermissionSubType getTablePermissionSubType()
   {
      return (this.tablePermissionSubType);
   }



   /*******************************************************************************
    * Setter for tablePermissionSubType
    * @see #withTablePermissionSubType(TablePermissionSubType)
    *******************************************************************************/
   public void setTablePermissionSubType(TablePermissionSubType tablePermissionSubType)
   {
      this.tablePermissionSubType = tablePermissionSubType;
   }



   /*******************************************************************************
    * Fluent setter for tablePermissionSubType
    *
    * @param tablePermissionSubType
    * The table permission sub type that is checked by this checker.  Note that this value
    * is not expected to be directly set via this setter - but rather, via the
    * initialization of this class through a {@link QCodeReferenceWithProperties}.
    * @return this
    *******************************************************************************/
   public UseTablePermissionCustomPermissionChecker withTablePermissionSubType(TablePermissionSubType tablePermissionSubType)
   {
      this.tablePermissionSubType = tablePermissionSubType;
      return (this);
   }

}
