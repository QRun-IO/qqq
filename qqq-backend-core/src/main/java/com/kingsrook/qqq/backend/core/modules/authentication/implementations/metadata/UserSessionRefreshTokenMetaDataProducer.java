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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSessionRefreshToken;


/*******************************************************************************
 ** Meta Data Producer for UserSessionRefreshToken.  Adding this table to an
 ** instance that uses OAuth2 authentication opts the app in to server-side
 ** refresh-token storage (#1041).  The table is hidden and not audited, as its
 ** contents are credentials (encrypted at rest - #1048).
 **
 ** It is also locked down (#1048): its permission rules require a dedicated
 ** permission (NO_ACCESS_PERMISSION_BASE_NAME + ".hasAccess") that no role
 ** should ever be granted - and, as the table is hidden, that permission is not
 ** offered among the instance's available permissions - so the table API,
 ** query, export, reports and UI all deny it, even to admins.  The
 ** authentication module's own reads and writes run as the system user, which
 ** isn't subject to this.
 *******************************************************************************/
public class UserSessionRefreshTokenMetaDataProducer extends MetaDataProducer<QTableMetaData>
{
   public static final String NO_ACCESS_PERMISSION_BASE_NAME = "qqq.internal.userSessionRefreshToken.noAccess";

   private final String backendName;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public UserSessionRefreshTokenMetaDataProducer(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QTableMetaData produce(QInstance qInstance) throws QException
   {
      QTableMetaData tableMetaData = new QTableMetaData()
         .withName(UserSessionRefreshToken.TABLE_NAME)
         .withBackendName(backendName)
         .withIsHidden(true)
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("id")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("sessionUuid"))
         .withFieldsFromEntity(UserSessionRefreshToken.class)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE))
         .withPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withPermissionBaseName(NO_ACCESS_PERMISSION_BASE_NAME)
            .withDenyBehavior(DenyBehavior.HIDDEN));
      return tableMetaData;
   }

}
