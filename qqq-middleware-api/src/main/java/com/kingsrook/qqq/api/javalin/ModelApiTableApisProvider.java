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

package com.kingsrook.qqq.api.javalin;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.api.actions.GenerateOpenApiSpecAction;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecInput;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.TableApisProviderInterface;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 ** The model APIs that expose a table, for the UI API's
 ** GET /qqq/v1/table/{tableName}/apis (registered through
 ** META-INF/services).  Same selection as /apis.json?tableName=: every API
 ** instance whose table metadata includes the table and doesn't exclude it.
 *******************************************************************************/
public class ModelApiTableApisProvider implements TableApisProviderInterface
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<TableApi> getTableApis(QInstance qInstance, String tableName)
   {
      List<TableApi>               rs        = new ArrayList<>();
      ApiInstanceMetaDataContainer container = ApiInstanceMetaDataContainer.of(qInstance);
      QTableMetaData               table     = qInstance.getTable(tableName);
      if(container == null || table == null)
      {
         return (rs);
      }

      ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
      if(apiTableMetaDataContainer == null)
      {
         return (rs);
      }

      for(ApiInstanceMetaData api : container.getApis().values())
      {
         ApiTableMetaData apiTableMetaData = apiTableMetaDataContainer.getApiTableMetaData(api.getName());
         if(apiTableMetaData == null || BooleanUtils.isTrue(apiTableMetaData.getIsExcluded()))
         {
            continue;
         }

         List<TableApiVersion> versions = new ArrayList<>();
         if(api.getSupportedVersions() != null)
         {
            for(APIVersion version : api.getSupportedVersions())
            {
               String versionString   = version.toString();
               String semanticVersion = CollectionUtils.nullSafeHasContents(api.getSemanticVersions()) ? api.getSemanticVersion(versionString) : null;
               versions.add(new TableApiVersion(versionString, semanticVersion, api.getPath() + versionString + "/" + tableName + "/openapi.json"));
            }
         }

         String currentVersion = api.getCurrentVersion() == null ? null : api.getCurrentVersion().toString();
         rs.add(new TableApi(api.getName(), api.getLabel(), api.getPath(), currentVersion, versions));
      }

      return (rs);
   }



   /***************************************************************************
    ** The table's document, as {api.path}{version}/{table}/openapi.json serves
    ** it - only for an API and version that getTableApis lists for the table.
    ***************************************************************************/
   @Override
   public Optional<String> getTableSpecJson(QInstance qInstance, String apiName, String version, String tableName) throws Exception
   {
      boolean listed = getTableApis(qInstance, tableName).stream()
         .anyMatch(api -> api.name().equals(apiName) && api.versions().stream().anyMatch(v -> v.version().equals(version)));
      if(!listed)
      {
         return (Optional.empty());
      }

      GenerateOpenApiSpecInput input = new GenerateOpenApiSpecInput().withVersion(version);
      input.setApiName(apiName);
      input.setTableName(tableName);
      return (Optional.ofNullable(new GenerateOpenApiSpecAction().execute(input).getJson()));
   }

}
