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
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSessionRefreshToken;


/*******************************************************************************
 ** Meta Data Producer for UserSessionRefreshToken.  Adding this table to an
 ** instance that uses OAuth2 authentication opts the app in to server-side
 ** refresh-token storage (#1041).  The table is hidden and not audited, as its
 ** contents are credentials.
 *******************************************************************************/
public class UserSessionRefreshTokenMetaDataProducer extends MetaDataProducer<QTableMetaData>
{
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
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE));
      return tableMetaData;
   }

}
