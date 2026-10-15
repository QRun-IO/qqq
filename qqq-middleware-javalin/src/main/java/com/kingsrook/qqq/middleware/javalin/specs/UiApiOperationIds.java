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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qqq.openapi.model.HttpMethod;


/*******************************************************************************
 ** OpenAPI operationIds of the UI API (`/qqq/v1`).  Generated clients name
 ** their methods after these, so they are part of the contract: once released,
 ** an operationId never changes (UiApiContract fails the build if one does).
 **
 ** Core operations have hand-chosen names (CORE).  Any other operation (an
 ** extension's, or a new core one not yet named here) gets a name derived from
 ** its method and path (derive()), unless its spec sets one explicitly with
 ** BasicOperation.withOperationId.  Do not rename an entry of CORE: add new
 ** ones only.
 *******************************************************************************/
public class UiApiOperationIds
{
   private static final Map<String, String> CORE = new LinkedHashMap<>();

   static
   {
      core("GET /version", "getVersion");
      core("GET /metaData/authentication", "getAuthenticationMetaData");
      core("POST /manageSession", "manageSession");
      core("POST /logout", "logout");
      core("POST /oidc/backchannel-logout", "oidcBackChannelLogout");
      core("GET /metaData", "getMetaData");
      core("GET /metaData/table/{tableName}", "getTableMetaData");
      core("GET /metaData/process/{processName}", "getProcessMetaData");

      core("GET /realtime/stream", "openRealtimeStream");
      core("POST /realtime/subscriptions", "updateRealtimeSubscriptions");

      core("GET /preferences", "listPreferenceSections");
      core("GET /preferences/{sectionName}", "getPreferenceSection");
      core("PATCH /preferences/{sectionName}", "updatePreferenceSection");
      core("POST /preferences/{sectionName}/actions/{actionName}", "runPreferenceSectionAction");

      core("GET /delivery/destinations", "listDeliveryDestinations");
      core("POST /delivery/destinations/{name}/targets", "chooseDeliveryTargets");
      core("GET /delivery/output/{logId}", "getDeliveryOutput");

      core("POST /table/{tableName}/query", "queryTable");
      core("POST /table/{tableName}/count", "countTable");
      core("POST /table/{tableName}", "insertRecord");
      core("GET /table/{tableName}/{primaryKey}", "getRecord");
      core("PATCH /table/{tableName}/{primaryKey}", "updateRecord");
      core("DELETE /table/{tableName}/{primaryKey}", "deleteRecord");
      core("GET /table/{tableName}/{primaryKey}/features", "getRecordFeatures");
      core("GET /table/{tableName}/variants", "listTableVariants");
      core("GET /table/{tableName}/apis", "listTableApis");
      core("GET /table/{tableName}/apis/{apiName}/{version}/openapi", "getTableApiDocument");
      core("POST /table/{tableName}/formAdjuster/{identifier}/{event}", "runFormAdjuster");
      core("POST /table/{tableName}/export", "exportTable");
      core("GET /table/{tableName}/{primaryKey}/{fieldName}/{filename}", "downloadRecordFieldFile");
      core("POST /table/{tableName}/possibleValues/{fieldName}", "searchTablePossibleValues");
      core("POST /table/{tableName}/relatedRecordPicker/{pickerName}/query", "queryRelatedRecordPicker");
      core("POST /table/{tableName}/relatedRecordPicker/{pickerName}/prepare", "prepareRelatedRecordPicker");
      core("POST /table/{tableName}/relatedRecordPicker/{pickerName}/add", "addRelatedRecords");
      core("POST /table/{tableName}/relatedRecordPicker/{pickerName}/remove", "removeRelatedRecords");
      core("GET /table/{tableName}/{primaryKey}/developer", "getRecordDeveloperMode");
      core("GET /table/{tableName}/{primaryKey}/developer/panels", "listRecordDeveloperPanels");
      core("POST /table/{tableName}/{primaryKey}/developer/associatedScript/{fieldName}", "storeAssociatedScript");
      core("GET /table/{tableName}/{primaryKey}/developer/associatedScript/{fieldName}/{scriptRevisionId}/logs", "getAssociatedScriptLogs");

      core("POST /search", "searchRecords");
      core("GET /help/search", "searchHelp");
      core("GET /help/coverage", "getHelpCoverage");

      core("POST /processes/{processName}/possibleValues/{fieldName}", "searchProcessPossibleValues");
      core("POST /possibleValues/{possibleValueSourceName}", "searchPossibleValues");
      core("POST /widget/{widgetName}", "getWidgetData");

      core("POST /processes/{processName}/init", "initProcess");
      core("POST /processes/{processName}/{processUUID}/step/{stepName}", "stepProcess");
      core("GET /processes/{processName}/{processUUID}/status/{jobUUID}", "getProcessStatus");
      core("GET /processes/{processName}/{processUUID}/records", "getProcessRecords");
      core("POST /processes/{processName}/{processUUID}/cancel", "cancelProcess");
      core("POST /processes/{processName}/jobs", "launchProcessJob");

      core("GET /jobs", "listJobs");
      core("GET /jobs/{jobId}", "getJob");
      core("GET /jobs/{jobId}/progress", "getJobProgress");
      core("POST /jobs/{jobId}/cancel", "cancelJob");
      core("POST /jobs/{jobId}/retry", "retryJob");
      core("POST /jobs/track", "trackJob");

      core("POST /uploads", "upload");
      core("GET /download/{file}", "download");
      core("GET /reports/{reportName}", "runReport");

      core("GET /bulkLoad/uploads/{uploadRef}/profile", "getBulkLoadUploadProfile");
      core("POST /bulkLoad/{tableName}/suggestMapping", "suggestBulkLoadMapping");
      core("POST /bulkLoad/{tableName}/validate", "validateBulkLoad");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void core(String key, String operationId)
   {
      if(CORE.containsValue(operationId))
      {
         throw (new IllegalStateException("Duplicate core operationId: " + operationId));
      }
      CORE.put(key, operationId);
   }



   /***************************************************************************
    ** The operationId for an operation (path relative to the version base,
    ** e.g. `/table/{tableName}/query`).
    ***************************************************************************/
   public static String of(HttpMethod httpMethod, String path)
   {
      String core = CORE.get(httpMethod.name() + " " + path);
      return (core != null ? core : derive(httpMethod, path));
   }



   /***************************************************************************
    ** Derive an operationId: the lower-case method, then each path segment in
    ** camel case, with `{param}` segments as `By{Param}`.  e.g.
    ** `GET /qbit/alerts/inbox/{id}` gives `getQbitAlertsInboxById`.
    ***************************************************************************/
   public static String derive(HttpMethod httpMethod, String path)
   {
      StringBuilder rs = new StringBuilder(httpMethod.name().toLowerCase(Locale.ROOT));
      for(String segment : path.split("/"))
      {
         if(segment.isEmpty())
         {
            continue;
         }

         boolean param = segment.startsWith("{") && segment.endsWith("}");
         if(param)
         {
            rs.append("By");
            segment = segment.substring(1, segment.length() - 1);
         }

         for(String word : segment.split("[^A-Za-z0-9]+"))
         {
            if(!word.isEmpty())
            {
               rs.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
         }
      }
      return (rs.toString());
   }



   /***************************************************************************
    ** The named core operations (`METHOD /path` -> operationId).
    ***************************************************************************/
   public static Map<String, String> getCore()
   {
      return (Map.copyOf(CORE));
   }
}
