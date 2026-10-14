/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.ApiOperation;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.actions.metadata.ProcessMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** Application-test guard for "API = admin parity". Uses the v1 core metadata
 ** actions with an all-permissions system session. Checks next-ui against api
 ** client-channel policy; it never executes table CRUD or process bodies.
 *******************************************************************************/
public class UiApiModelApiParityChecker
{
   private static final Map<Capability, ApiOperation> OPERATIONS = Map.of(
      Capability.TABLE_QUERY, ApiOperation.QUERY_BY_QUERY_STRING,
      Capability.TABLE_GET, ApiOperation.GET,
      Capability.TABLE_COUNT, ApiOperation.QUERY_BY_QUERY_STRING,
      Capability.TABLE_INSERT, ApiOperation.INSERT,
      Capability.TABLE_UPDATE, ApiOperation.UPDATE,
      Capability.TABLE_DELETE, ApiOperation.DELETE);



   /*******************************************************************************
    ** Check an already validated/enriched instance. Returns sorted drift lines,
    ** including stale exclusions. Invalid API selections throw rather than pass.
    ** Caller context is restored even if a metadata action/customizer throws.
    *******************************************************************************/
   public List<String> check(QInstance instance, String apiName, String version, ParityExclusions exclusions) throws QException
   {
      Objects.requireNonNull(instance);
      Objects.requireNonNull(exclusions);
      ApiInstanceMetaDataContainer container = ApiInstanceMetaDataContainer.of(instance);
      ApiInstanceMetaData api = container == null ? null : container.getApiInstanceMetaData(apiName);
      APIVersion apiVersion = new APIVersion(version);
      if(api == null || api.getSupportedVersions() == null || !api.getSupportedVersions().contains(apiVersion))
      {
         throw (new IllegalArgumentException("Unknown API or unsupported version: " + apiName + " " + version));
      }

      Check check = new Check(instance, api, version, exclusions);
      QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance)
         .withClientChannel(ClientChannelsMetaData.CHANNEL_NEXT_UI)), check::run);
      return (check.drift.stream().sorted().toList());
   }



   /*******************************************************************************
    ** Per-invocation state: no shared API field/name caches are populated or cleared.
    *******************************************************************************/
   private static class Check
   {
      private final QInstance instance;
      private final ApiInstanceMetaData api;
      private final String version;
      private final APIVersion apiVersion;
      private final Map<String, String> remainingExclusions;
      private final List<String> drift = new ArrayList<>();



      /*******************************************************************************
       ** Copy exclusions so the same fixture can be checked repeatedly.
       *******************************************************************************/
      private Check(QInstance instance, ApiInstanceMetaData api, String version, ParityExclusions exclusions)
      {
         this.instance = instance;
         this.api = api;
         this.version = version;
         this.apiVersion = new APIVersion(version);
         this.remainingExclusions = new HashMap<>(exclusions.getReasons());
      }



      /*******************************************************************************
       ** The top-level action determines visibility, including application customizers.
       *******************************************************************************/
      private void run() throws QException
      {
         MetaDataOutput ui = new MetaDataAction().execute(new MetaDataInput());
         for(var entry : ui.getTables().entrySet())
         {
            if(!entry.getValue().getIsHidden())
            {
               checkTable(entry.getKey());
            }
         }
         for(var entry : ui.getProcesses().entrySet())
         {
            if(!entry.getValue().getIsHidden())
            {
               checkProcess(entry.getKey());
            }
         }
         remainingExclusions.forEach((key, reason) -> drift.add(key + ": stale exclusion (" + reason + ")"));
      }



      /*******************************************************************************
       ** Table publication and operation precedence match the model API's metadata.
       *******************************************************************************/
      private void checkTable(String tableName) throws QException
      {
         String prefix = "table [" + tableName + "]";
         TableMetaDataInput input = new TableMetaDataInput();
         input.setTableName(tableName);
         input.setInputSource(QInputSource.USER);
         QFrontendTableMetaData uiTable = new TableMetaDataAction().execute(input).getTable();
         if(uiTable.getIsHidden() || excluded(prefix))
         {
            return;
         }
         QTableMetaData table = instance.getTable(tableName);
         ApiTableMetaDataContainer container = ApiTableMetaDataContainer.of(table);
         ApiTableMetaData apiTable = container == null ? null : container.getApiTableMetaData(api.getName());
         if(apiTable == null || Boolean.TRUE.equals(apiTable.getIsExcluded()) || table.getIsHidden() || !apiTable.getApiVersionRange().includes(apiVersion))
         {
            ///////////////////////////////////////////////////////////////////////////////////
            // These UI children still exist, even though their parent is missing from API. //
            // Do not misreport their existing exceptions as stale alongside table drift.   //
            ///////////////////////////////////////////////////////////////////////////////////
            uiTable.getFields().keySet().forEach(name -> excluded(prefix + " field [" + name + "]"));
            uiTable.getVirtualFields().keySet().forEach(name -> excluded(prefix + " field [" + name + "]"));
            uiTable.getCapabilities().forEach(name -> excluded(prefix + " capability [" + name + "]"));
            missing(prefix);
            return;
         }

         checkPermissions(prefix, table);
         checkFields(prefix, table, uiTable);
         for(var entry : OPERATIONS.entrySet())
         {
            Capability capability = entry.getKey();
            String capabilityPrefix = prefix + " capability [" + capability + "]";
            if(uiTable.getCapabilities().contains(capability.name()) && !excluded(capabilityPrefix))
            {
               ApiOperation operation = entry.getValue();
               if(!operation.isOperationEnabled(List.of(api, apiTable))
                  || !table.isCapabilityEnabled(instance.getBackendForTable(tableName), capability)
                  || !table.isCapabilityEnabled(instance.getBackendForTable(tableName), operation.getCapability()))
               {
                  missing(capabilityPrefix);
               }
            }
         }
      }



      /*******************************************************************************
       ** Include virtual fields and honor aliases, while rejecting alias collisions
       ** and unrelated historical fields that happen to reuse a current API name.
       *******************************************************************************/
      private void checkFields(String prefix, QTableMetaData table, QFrontendTableMetaData uiTable) throws QException
      {
         Map<String, QFieldMetaData> apiFields = new HashMap<>();
         CapturedContext apiContext = new CapturedContext(instance, new QSystemUserSession(instance)
            .withClientChannel(ClientChannelsMetaData.CHANNEL_API));
         QContext.withTemporaryContext(apiContext, () ->
         {
            List<QFieldMetaData> fields = new GetTableApiFieldsAction().execute(new GetTableApiFieldsInput()
               .withApiName(api.getName()).withVersion(version).withTableName(table.getName())
               .withInputSource(QInputSource.USER).withIncludeVirtualFields(true)).getFields();
            for(QFieldMetaData field : fields)
            {
               String name = ApiFieldMetaData.getEffectiveApiFieldName(api.getName(), field);
               if(apiFields.put(name, field) != null)
               {
                  drift.add(prefix + " API field [" + name + "]: duplicate API name");
               }
            }
         });

         Map<String, QFrontendFieldMetaData> uiFields = new HashMap<>(uiTable.getFields());
         uiFields.putAll(uiTable.getVirtualFields());
         for(var entry : uiFields.entrySet())
         {
            String fieldName = entry.getKey();
            String fieldPrefix = prefix + " field [" + fieldName + "]";
            if(excluded(fieldPrefix))
            {
               continue;
            }
            QFieldMetaData coreField = table.getFields().get(fieldName);
            if(coreField == null && table.getVirtualFields() != null)
            {
               coreField = table.getVirtualFields().get(fieldName);
            }
            String apiFieldName = coreField == null ? fieldName : ApiFieldMetaData.getEffectiveApiFieldName(api.getName(), coreField);
            QFieldMetaData apiField = apiFields.get(apiFieldName);
            if(apiField == null || !fieldName.equals(apiField.getName()))
            {
               missing(fieldPrefix);
            }
            else if(entry.getValue().getType() != apiField.getType())
            {
               drift.add(fieldPrefix + ": incompatible type (UI " + entry.getValue().getType() + ", API " + apiField.getType() + ")");
            }
         }
      }



      /*******************************************************************************
       ** Require an explicit exception even for processes with no API supplement;
       ** otherwise removing a supplement would silently remove the parity guard.
       *******************************************************************************/
      private void checkProcess(String processName) throws QException
      {
         String prefix = "process [" + processName + "]";
         if(excluded(prefix))
         {
            return;
         }
         ProcessMetaDataInput input = new ProcessMetaDataInput();
         input.setProcessName(processName);
         new ProcessMetaDataAction().execute(input);
         var process = instance.getProcess(processName);
         ApiProcessMetaDataContainer container = ApiProcessMetaDataContainer.of(process);
         ApiProcessMetaData apiProcess = container == null ? null : container.getApiProcessMetaData(api.getName());
         if(apiProcess == null || Boolean.TRUE.equals(apiProcess.getIsExcluded()) || !apiProcess.getApiVersionRange().includes(apiVersion))
         {
            missing(prefix);
            return;
         }
         checkPermissions(prefix, process);
      }



      /*******************************************************************************
       ** Both APIs enforce the same core object via PermissionsHelper. Compare the
       ** effective rule signature AND decisions in each channel, so client denies
       ** or channel-aware permission customizers cannot be hidden by all-access.
       *******************************************************************************/
      private void checkPermissions(String prefix, MetaDataWithPermissionRules object) throws QException
      {
         List<Object> uiRules = permissionSignature(object);
         CapturedContext apiContext = new CapturedContext(instance, new QSystemUserSession(instance)
            .withClientChannel(ClientChannelsMetaData.CHANNEL_API));
         QContext.withTemporaryContext(apiContext, () ->
         {
            if(!uiRules.equals(permissionSignature(object)))
            {
               drift.add(prefix + ": permission rules differ between next-ui and API " + api.getName() + " " + version);
            }
         });
      }



      /*******************************************************************************
       ** Include the resolved level/base/denies and the exact table permission names
       ** used by OpenAPI generation. Decisions use the same helper as API execution.
       *******************************************************************************/
      private List<Object> permissionSignature(MetaDataWithPermissionRules object)
      {
         QPermissionRules rules = PermissionsHelper.getEffectivePermissionRules(object, instance);
         String baseName = rules.getPermissionBaseName();
         if(baseName == null || baseName.isBlank())
         {
            baseName = object.getName();
         }
         List<Object> signature = new ArrayList<>();
         signature.add(rules.getLevel());
         signature.add(baseName);
         signature.add(rules.getDenyBehavior());
         signature.add(rules.getDeniedClients() == null ? Set.of() : new HashSet<>(rules.getDeniedClients()));
         signature.add(rules.getCustomPermissionChecker());
         if(object instanceof QTableMetaData)
         {
            TableMetaDataInput input = new TableMetaDataInput();
            input.setTableName(object.getName());
            input.setInputSource(QInputSource.USER);
            for(TablePermissionSubType subType : TablePermissionSubType.values())
            {
               signature.add(PermissionsHelper.getTablePermissionName(object.getName(), subType));
               signature.add(PermissionsHelper.hasTablePermission(input, object.getName(), subType));
            }
         }
         else
         {
            ProcessMetaDataInput input = new ProcessMetaDataInput();
            input.setProcessName(object.getName());
            signature.add(PermissionsHelper.hasProcessPermission(input, object.getName()));
         }
         return (signature);
      }



      /*******************************************************************************
       ** A matching exclusion acknowledges this UI surface for this invocation.
       *******************************************************************************/
      private boolean excluded(String key)
      {
         return (remainingExclusions.remove(key) != null);
      }



      /*******************************************************************************
       ** Consistent diagnostics keep application assertions and CI output readable.
       *******************************************************************************/
      private void missing(String prefix)
      {
         drift.add(prefix + ": not in API " + api.getName() + " " + version);
      }
   }
}
