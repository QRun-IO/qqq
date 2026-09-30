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
 **
 *******************************************************************************/
public class MiddlewareVersionV1 extends AbstractMiddlewareVersion
{
   private static List<AbstractEndpointSpec<?, ?, ?>> list = new ArrayList<>();

   static
   {
      list.add(new AuthenticationMetaDataSpecV1());
      list.add(new ManageSessionSpecV1());
      list.add(new LogoutSpecV1());
      list.add(new BackChannelLogoutSpecV1());

      list.add(new MetaDataSpecV1());

      list.add(new TableMetaDataSpecV1());
      list.add(new TableQuerySpecV1());
      list.add(new TableCountSpecV1());
      list.add(new RecordSearchSpecV1());

      ///////////////////////////////////////////////////////////////////////////
      // before TableGetSpecV1: GET /table/{t}/variants would otherwise match //
      // GET /table/{t}/{primaryKey} (routes match in registration order)     //
      ///////////////////////////////////////////////////////////////////////////
      list.add(new TableVariantsSpecV1());
      list.add(new TableGetSpecV1());
      list.add(new TableInsertSpecV1());
      list.add(new TableUpdateSpecV1());
      list.add(new TableDeleteSpecV1());

      list.add(new RecordDeveloperModeSpecV1());
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

      list.add(new TableExportSpecV1());

      list.add(new RecordFieldDownloadSpecV1());
      list.add(new GeneralDownloadSpecV1());
      list.add(new ReportRunSpecV1());
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
   public List<AbstractEndpointSpec<?, ?, ?>> getEndpointSpecs()
   {
      return (list);
   }

}
