/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles;


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareScopePossibleValueMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableAudienceType;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles.DeleteSavedBulkLoadProfileProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles.QuerySavedBulkLoadProfileProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles.StoreSavedBulkLoadProfileProcess;


/*******************************************************************************
 **
 *******************************************************************************/
public class SavedBulkLoadProfileMetaDataProvider
{
   public static final String SHARED_SAVED_BULK_LOAD_PROFILE_JOIN_SAVED_BULK_LOAD_PROFILE = "sharedSavedBulkLoadProfileJoinSavedBulkLoadProfile";



   /*******************************************************************************
    **
    *******************************************************************************/
   public void defineAll(QInstance instance, String recordTablesBackendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      instance.addTable(defineSavedBulkLoadProfileTable(recordTablesBackendName, backendDetailEnricher));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable(SavedBulkLoadProfile.TABLE_NAME));

      /////////////////////////////////////
      // todo - param to enable sharing? //
      /////////////////////////////////////
      instance.addTable(defineSharedSavedBulkLoadProfileTable(recordTablesBackendName, backendDetailEnricher));
      instance.addJoin(defineSharedSavedBulkLoadProfileJoinSavedBulkLoadProfile());
      if(instance.getPossibleValueSource(ShareScopePossibleValueMetaDataProducer.NAME) == null)
      {
         instance.addPossibleValueSource(new ShareScopePossibleValueMetaDataProducer().produce(new QInstance()));
      }

      ////////////////////////////////////
      // processes for working with 'em //
      ////////////////////////////////////
      instance.add(StoreSavedBulkLoadProfileProcess.getProcessMetaData());
      instance.add(QuerySavedBulkLoadProfileProcess.getProcessMetaData());
      instance.add(DeleteSavedBulkLoadProfileProcess.getProcessMetaData());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QJoinMetaData defineSharedSavedBulkLoadProfileJoinSavedBulkLoadProfile()
   {
      return (new QJoinMetaData()
         .withName(SHARED_SAVED_BULK_LOAD_PROFILE_JOIN_SAVED_BULK_LOAD_PROFILE)
         .withLeftTable(SharedSavedBulkLoadProfile.TABLE_NAME)
         .withRightTable(SavedBulkLoadProfile.TABLE_NAME)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("savedBulkLoadProfileId", "id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineSavedBulkLoadProfileTable(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(SavedBulkLoadProfile.TABLE_NAME)
         .withLabel("Bulk Load Profile")
         .withIcon(new QIcon().withName("drive_folder_upload"))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("label")
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(SavedBulkLoadProfile.class)
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD))
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "label", "tableName")))
         .withSection(new QFieldSection("details", new QIcon().withName("text_snippet"), Tier.T2, List.of("userId", "mappingJson", "isBulkEdit")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      table.getField("mappingJson").withBehavior(SavedBulkLoadProfileJsonFieldDisplayValueFormatter.getInstance());
      table.getField("mappingJson").setLabel("Mapping");

      table.withShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(SharedSavedBulkLoadProfile.TABLE_NAME)
         .withAssetIdFieldName("savedBulkLoadProfileId")
         .withScopeFieldName("scope")
         .withThisTableOwnerIdFieldName("userId")
         .withAudienceType(new ShareableAudienceType().withName("user").withFieldName("userId")));

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData defineSharedSavedBulkLoadProfileTable(String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(SharedSavedBulkLoadProfile.TABLE_NAME)
         .withLabel("Shared Bulk Load Profile")
         .withIcon(new QIcon().withName("share"))
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("savedBulkLoadProfileId")
         .withBackendName(backendName)
         .withUniqueKey(new UniqueKey("savedBulkLoadProfileId", "userId"))
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(SharedSavedBulkLoadProfile.class)
         // todo - security key
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD))
         .withSection(new QFieldSection("identity", new QIcon().withName("badge"), Tier.T1, List.of("id", "savedBulkLoadProfileId", "userId")))
         .withSection(new QFieldSection("data", new QIcon().withName("text_snippet"), Tier.T2, List.of("scope")))
         .withSection(new QFieldSection("dates", new QIcon().withName("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }

      return (table);
   }

}
