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

package com.kingsrook.qqq.backend.core.actions.preferences;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Core key-value preferences table metadata.
 *******************************************************************************/
public class UserPreferencesKeyValueStoreMetaDataProvider
{
   /*******************************************************************************
    ** Define the ordinary audited table; applications supply its database migration.
    *******************************************************************************/
   public void defineAll(QInstance qInstance, String backendName, String tableNameOrNullForDefault)
   {
      String tableName = tableNameOrNullForDefault == null ? UserPreferencesMetaData.DEFAULT_TABLE_NAME : tableNameOrNullForDefault;
      UserPreferencesMetaData.ofOrWithNew(qInstance).setKeyValueTableName(tableName);
      qInstance.addTable(new QTableMetaData()
         .withName(tableName).withLabel("User Preferences").withBackendName(backendName)
         .withIcon(new QIcon("settings"))
         .withPrimaryKeyField("id")
         .withRecordLabelFormatAndFields("%s / %s / %s", "userId", "sectionName", "preferenceName")
         .withUniqueKey(new UniqueKey("userId", "sectionName", "preferenceName"))
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("userId", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("sectionName", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("preferenceName", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("value", QFieldType.TEXT))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME)));
   }
}
