/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.sampleapp.metadata;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableAudienceType;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.SharedSavedReport;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedviews.SharedSavedView;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.SharingMetaDataProvider;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 ** Self-contained sharing examples, enabled only with explicit local mock auth.
 *******************************************************************************/
public class SampleSharingMetaDataProvider
{
   public static final String USER_TABLE = "sampleSharingUser";
   public static final String USER_KEY = "sampleSharingUserId";
   public static final String AUDIENCE_SOURCE = "sampleSharingAudience";
   public static final String ALICE_SESSION = "11111111-1111-4111-8111-111111111111";
   public static final String BOB_SESSION = "22222222-2222-4222-8222-222222222222";
   public static final String CASEY_SESSION = "33333333-3333-4333-8333-333333333333";
   private static final String REPORT_OWNER_JOIN = "sampleSavedReportOwnerJoin";



   /*******************************************************************************
    ** Use the same native tables and generic processes as application integrations.
    *******************************************************************************/
   public void defineAll(QInstance instance) throws QException
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName(USER_KEY));
      instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
      QTableMetaData users = new QTableMetaData().withName(USER_TABLE).withLabel("Sample User")
         .withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.STRING))
         .withField(new QFieldMetaData("audienceId", QFieldType.STRING))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withRecordLabelFields("name").withRecordLabelFormat("%s");
      nativeTable(users);
      instance.addTable(users);
      instance.addPossibleValueSource(QPossibleValueSource.newForTable(USER_TABLE).withName(AUDIENCE_SOURCE).withOverrideIdField("audienceId"));
      new SavedViewsMetaDataProvider().withIsQuickSavedViewEnabled(true).withUserLevelRecordSecurityLock(userLock())
         .defineAll(instance, SampleMetaDataProvider.RDBMS_BACKEND_NAME, SampleSharingMetaDataProvider::nativeTable);
      instance.getTable(SavedView.TABLE_NAME).setShareableTableMetaData(new ShareableTableMetaData()
         .withSharedRecordTableName(SharedSavedView.TABLE_NAME)
         .withAssetIdFieldName("savedViewId").withScopeFieldName("scope").withThisTableOwnerIdFieldName("userId")
         .withAudienceType(new ShareableAudienceType().withName("user").withFieldName("userId")));
      new SavedReportsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME,
         SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME, table ->
         {
            if(SavedReportsMetaDataProvider.REPORT_STORAGE_TABLE_NAME.equals(table.getName()))
            {
               table.setBackendDetails(new FilesystemTableBackendDetails().withBasePath("sharing-reports")
                  .withCardinality(Cardinality.MANY).withRecordFormat(RecordFormat.CSV));
            }
            else if(List.of(SavedReport.TABLE_NAME, SharedSavedReport.TABLE_NAME).contains(table.getName()))
            {
               table.setBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME);
               nativeTable(table);
            }
         });
      String shareJoin = SavedReportsMetaDataProvider.SHARED_SAVED_REPORT_JOIN_SAVED_REPORT;
      instance.addJoin(instance.getJoin(shareJoin).flip().withName(REPORT_OWNER_JOIN));
      instance.getTable(SavedReport.TABLE_NAME).withRecordSecurityLock(new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.OR).withLock(userLock())
         .withLock(userLock().withLockScope(RecordSecurityLock.LockScope.READ).withFieldName("sharedSavedReport.userId")
            .withJoinNameChain(List.of(shareJoin))));
      instance.getTable(SharedSavedReport.TABLE_NAME).withRecordSecurityLock(new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
         .withLock(userLock().withFieldName("savedReport.userId").withJoinNameChain(List.of(REPORT_OWNER_JOIN)))
         .withLock(userLock().withLockScope(RecordSecurityLock.LockScope.READ)));
      for(String tableName : List.of(SavedView.TABLE_NAME, SavedReport.TABLE_NAME))
      {
         var sharing = instance.getTable(tableName).getShareableTableMetaData();
         sharing.setAudiencePossibleValueSourceName(AUDIENCE_SOURCE);
         sharing.getAudienceTypes().get("user").setSourceTableName(USER_TABLE);
      }
      new SharingMetaDataProvider().defineAll(instance, null);
      instance.getAuthentication().setCustomizer(new QCodeReference(SharingUser.class));
      instance.addApp(new QAppMetaData().withName("sharing").withLabel("Sharing Demo").withIcon(new QIcon("share"))
         .withChild(instance.getTable(SavedReport.TABLE_NAME)).withChild(instance.getTable(SavedView.TABLE_NAME)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RecordSecurityLock userLock()
   {
      return new RecordSecurityLock().withSecurityKeyType(USER_KEY).withFieldName("userId");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void nativeTable(QTableMetaData table)
   {
      table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
      QInstanceEnricher.setInferredFieldBackendNames(table);
   }



   /*******************************************************************************
    ** Demo identity selection belongs to mock authentication, never real accounts.
    *******************************************************************************/
   public static class SharingUser implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         String name = BOB_SESSION.equals(session.getUuid()) ? "bob" : CASEY_SESSION.equals(session.getUuid()) ? "casey" : "alice";
         String userId = "sample:" + name;
         session.getUser().setIdReference(userId);
         session.getUser().setFullName(Character.toUpperCase(name.charAt(0)) + name.substring(1) + " (sample)");
         session.withValueForFrontend("user", new LinkedHashMap<>(Map.of("name", session.getUser().getFullName(), "email", userId)));
         session.setPermissions(Set.of());
         session.setSecurityKeyValues(Map.of(USER_KEY, List.of(userId)));
      }
   }
}
