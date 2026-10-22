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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.ResolvedRecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Feature grants always require READ; the edit fallback also requires stored WRITE locks.
 *******************************************************************************/
public class RecordFeaturePermissions
{



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean hasTableCapability(QTableMetaData table, RecordFeature feature)
   {
      ResolvedRecordFeature resolved = RecordFeaturesResolver.resolve(QContext.getQInstance(), table).getFeatures().get(feature);
      if(!resolved.getEnabled())
      {
         return (false);
      }
      return (feature.getPermissionSuffix() == null
         ? PermissionsHelper.hasTablePermission(new GetInput(), table.getName(), TablePermissionSubType.READ)
         : PermissionsHelper.hasTableFeaturePermission(table, feature.getPermissionSuffix()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean explicitlyGranted(QTableMetaData table, RecordFeature feature)
   {
      QPermissionRules rules = PermissionsHelper.getEffectivePermissionRules(table, QContext.getQInstance());
      return (rules.getLevel() == PermissionLevel.NOT_PROTECTED
         || QContext.getQSession().hasPermission(PermissionsHelper.getEffectivePermissionBaseName(rules, table.getName()) + "." + feature.getPermissionSuffix()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean checkRecordCapability(QTableMetaData table, RecordFeature feature, QRecord parent) throws QException
   {
      return (checkRecordCapability(table, feature, parent, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean checkRecordCapability(QTableMetaData table, RecordFeature feature, QRecord parent, QBackendTransaction transaction) throws QException
   {
      if(parent == null || !hasTableCapability(table, feature))
      {
         return (false);
      }
      QRecord visibleParent = RecordReferenceSecurityHelper.visibleParent(table.getName(), parent.getValue(table.getPrimaryKeyField()), transaction);
      if(visibleParent == null)
      {
         return (false);
      }
      if(feature.getPermissionSuffix() == null || explicitlyGranted(table, feature))
      {
         return (true);
      }
      Map<Object, QRecord> records = ValidateRecordSecurityLockHelper.validateStoredWriteLocks(table,
         Map.of(visibleParent.getValue(table.getPrimaryKeyField()), visibleParent), ValidateRecordSecurityLockHelper.Action.UPDATE, transaction);
      return (records == null || (!records.isEmpty() && records.values().stream().allMatch(record -> CollectionUtils.nullSafeIsEmpty(record.getErrors()))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void requireTableCapability(QTableMetaData table, RecordFeature feature) throws QPermissionDeniedException
   {
      if(!hasTableCapability(table, feature))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void requireRecordCapability(QTableMetaData table, RecordFeature feature, QRecord parent) throws QException
   {
      if(!checkRecordCapability(table, feature, parent))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }
   }

}
