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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;


/*******************************************************************************
 ** Version 1 of the QQQ UI API (`/qqq/v1`).  Its semantic version (see
 ** docs/UI-API-CHANGELOG.md) is published as the OpenAPI info.version and by
 ** GET /qqq/v1/version.
 *******************************************************************************/
public class MiddlewareVersionV1 extends AbstractMiddlewareVersion
{
   /***************************************************************************
    ** The semantic version of the v1 UI API.  Bump MINOR for additive changes
    ** and PATCH for fixes (UiApiContractTest enforces it); a breaking change
    ** needs a new URL version (/qqq/v2).
    ***************************************************************************/
   public static final String SEMANTIC_VERSION = "1.3.0";

   /***************************************************************************
    ** Optional core features, published by GET /version.
    ***************************************************************************/
   public static final List<String> FEATURES = List.of(
      "metaData", "tables", "recordSearch", "tableVariants", "recordDeveloper", "possibleValues",
      "widgets", "processes", "processCancel", "reports", "exports", "downloads", "version", "extensions", "recordFeatures", "realtime",
      "help", "preferences", "durableJobs");

   private final List<AbstractEndpointSpec<?, ?, ?>> list = new ArrayList<>();

   {
      list.add(new VersionSpecV1());
      RealtimeStreamSpecV1 realtime = new RealtimeStreamSpecV1();
      list.add(realtime);
      list.add(new RealtimeSubscriptionsSpecV1(realtime::getHub));
      list.add(new AuthenticationMetaDataSpecV1());
      list.add(new ManageSessionSpecV1());
      list.add(new LogoutSpecV1());
      list.add(new BackChannelLogoutSpecV1());

      list.add(new MetaDataSpecV1());
      list.add(new PreferencesSpecV1());
      list.add(new PreferenceSectionGetSpecV1());
      list.add(new PreferenceSectionPatchSpecV1());
      list.add(new PreferenceSectionActionSpecV1());

      list.add(new TableMetaDataSpecV1());
      list.add(new TableQuerySpecV1());
      list.add(new TableCountSpecV1());
      list.add(new RecordSearchSpecV1());
      list.add(new HelpSearchSpecV1());
      list.add(new HelpCoverageSpecV1());

      ///////////////////////////////////////////////////////////////////////////
      // before TableGetSpecV1: GET /table/{t}/variants would otherwise match //
      // GET /table/{t}/{primaryKey} (routes match in registration order)     //
      ///////////////////////////////////////////////////////////////////////////
      list.add(new TableVariantsSpecV1());
      list.add(new TableGetSpecV1());
      list.add(new TableRecordFeaturesSpecV1());
      list.add(new TableInsertSpecV1());
      list.add(new TableUpdateSpecV1());
      list.add(new TableDeleteSpecV1());

      list.add(new RelatedRecordPickerQuerySpecV1());
      list.add(new RelatedRecordPickerPrepareSpecV1());
      list.add(new RelatedRecordPickerAddSpecV1());
      list.add(new RelatedRecordPickerRemoveSpecV1());

      list.add(new RecordDeveloperModeSpecV1());
      list.add(new RecordDeveloperPanelsSpecV1());
      list.add(new RecordAssociatedScriptStoreSpecV1());
      list.add(new RecordAssociatedScriptLogsSpecV1());

      list.add(new TablePossibleValuesSpecV1());
      list.add(new ProcessPossibleValuesSpecV1());
      list.add(new StandalonePossibleValuesSpecV1());

      list.add(new WidgetSpecV1());

      list.add(new ProcessMetaDataSpecV1());
      list.add(new ProcessInitSpecV1());
      list.add(new ProcessStepSpecV1());
      list.add(new ProcessStatusSpecV1());
      list.add(new ProcessRecordsSpecV1());
      list.add(new ProcessCancelSpecV1());
      list.add(new ProcessJobLaunchSpecV1());
      list.add(new UploadsSpecV1());

      list.add(new TableExportSpecV1());

      list.add(new RecordFieldDownloadSpecV1());
      list.add(new GeneralDownloadSpecV1());
      list.add(new ReportRunSpecV1());

      list.add(new BulkLoadUploadProfileSpecV1());
      list.add(new BulkLoadSuggestMappingSpecV1());
      list.add(new BulkLoadValidateSpecV1());
   }

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getSemanticVersion()
   {
      return (SEMANTIC_VERSION);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<String> getFeatures()
   {
      return (FEATURES);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public List<AbstractEndpointSpec<?, ?, ?>> getEndpointSpecs()
   {
      return (list);
   }

}
