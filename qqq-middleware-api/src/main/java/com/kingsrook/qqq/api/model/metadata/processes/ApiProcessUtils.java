/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata.processes;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiProcessUtils
{
   private static final QLogger LOG = QLogger.getLogger(ApiProcessUtils.class);

   private static Map<Pair<String, String>, Map<String, QProcessMetaData>> processApiNameMap = new HashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Pair<ApiProcessMetaData, QProcessMetaData> getProcessMetaDataPair(ApiInstanceMetaData apiInstanceMetaData, String version, String processApiName) throws QNotFoundException
   {
      QProcessMetaData process  = getProcessByApiName(apiInstanceMetaData.getName(), version, processApiName);
      LogPair[]        logPairs = new LogPair[] { logPair("apiName", apiInstanceMetaData.getName()), logPair("version", version), logPair("processApiName", processApiName) };

      if(process == null)
      {
         LOG.info("404 because process is null (processApiName=" + processApiName + ")", logPairs);
         throw (new QNotFoundException("Could not find a process named " + processApiName + " in this api."));
      }

      ApiProcessMetaDataContainer apiProcessMetaDataContainer = ApiProcessMetaDataContainer.of(process);
      if(apiProcessMetaDataContainer == null)
      {
         LOG.info("404 because process apiProcessMetaDataContainer is null", logPairs);
         throw (new QNotFoundException("Could not find a process named " + processApiName + " in this api."));
      }

      ApiProcessMetaData apiProcessMetaData = apiProcessMetaDataContainer.getApiProcessMetaData(apiInstanceMetaData.getName());
      if(apiProcessMetaData == null)
      {
         LOG.info("404 because process apiProcessMetaData is null", logPairs);
         throw (new QNotFoundException("Could not find a process named " + processApiName + " in this api."));
      }

      if(BooleanUtils.isTrue(apiProcessMetaData.getIsExcluded()))
      {
         LOG.info("404 because process is excluded", logPairs);
         throw (new QNotFoundException("Could not find a process named " + processApiName + " in this api."));
      }

      if(BooleanUtils.isTrue(process.getIsHidden()))
      {
         if(!BooleanUtils.isTrue(apiProcessMetaData.getOverrideProcessIsHidden()))
         {
            LOG.info("404 because process isHidden", logPairs);
            throw (new QNotFoundException("Could not find a process named " + processApiName + " in this api."));
         }
      }

      APIVersion       requestApiVersion = new APIVersion(version);
      List<APIVersion> supportedVersions = apiInstanceMetaData.getSupportedVersions();
      if(CollectionUtils.nullSafeIsEmpty(supportedVersions) || !supportedVersions.contains(requestApiVersion))
      {
         LOG.info("404 because requested version is not supported", logPairs);
         throw (new QNotFoundException(version + " is not a supported version in this api."));
      }

      if(!apiProcessMetaData.getApiVersionRange().includes(requestApiVersion))
      {
         LOG.info("404 because process version range does not include requested version", logPairs);
         throw (new QNotFoundException(version + " is not a supported version for process " + processApiName + " in this api."));
      }

      return (Pair.of(apiProcessMetaData, process));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QProcessMetaData getProcessByApiName(String apiName, String version, String processApiName)
   {
      /////////////////////////////////////////////////////////////////////////////////////////////
      // processApiNameMap is a map of (apiName,apiVersion) => Map<String, QProcessMetaData>.    //
      // that is to say, a 2-level map.  The first level is keyed by (apiName,apiVersion) pairs. //
      // the second level is keyed by processApiNames.                                           //
      /////////////////////////////////////////////////////////////////////////////////////////////
      Pair<String, String> key = new Pair<>(apiName, version);
      if(processApiNameMap.get(key) == null)
      {
         Map<String, QProcessMetaData> map = new HashMap<>();

         for(QProcessMetaData process : QContext.getQInstance().getProcesses().values())
         {
            ApiProcessMetaDataContainer apiProcessMetaDataContainer = ApiProcessMetaDataContainer.of(process);
            if(apiProcessMetaDataContainer != null)
            {
               ApiProcessMetaData apiProcessMetaData = apiProcessMetaDataContainer.getApiProcessMetaData(apiName);
               if(apiProcessMetaData != null)
               {
                  String name = process.getName();
                  if(StringUtils.hasContent(apiProcessMetaData.getApiProcessName()))
                  {
                     name = apiProcessMetaData.getApiProcessName();
                  }
                  map.put(name, process);
               }
            }
         }

         processApiNameMap.put(key, map);
      }

      return (processApiNameMap.get(key).get(processApiName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getProcessApiPath(QInstance qInstance, QProcessMetaData process, ApiProcessMetaData apiProcessMetaData, ApiInstanceMetaData apiInstanceMetaData)
   {
      StringBuilder pathParams = new StringBuilder();
      if(ObjectUtils.ifCan(() -> CollectionUtils.nullSafeHasContents(apiProcessMetaData.getInput().getPathParams().getFields())))
      {
         for(QFieldMetaData field : apiProcessMetaData.getInput().getPathParams().getFields())
         {
            pathParams.append("/{").append(field.getName()).append("}");
         }
      }

      if(StringUtils.hasContent(apiProcessMetaData.getPath()))
      {
         return apiProcessMetaData.getPath() + "/" + apiProcessMetaData.getApiProcessName() + pathParams;
      }
      else if(StringUtils.hasContent(process.getTableName()))
      {
         QTableMetaData            table                     = qInstance.getTable(process.getTableName());
         String                    tablePathPart             = table.getName();
         ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
         if(apiTableMetaDataContainer != null)
         {
            ApiTableMetaData apiTableMetaData = apiTableMetaDataContainer.getApis().get(apiInstanceMetaData.getName());
            if(apiTableMetaData != null)
            {
               if(StringUtils.hasContent(apiTableMetaData.getApiTableName()))
               {
                  tablePathPart = apiTableMetaData.getApiTableName();
               }
            }
         }
         return tablePathPart + "/" + apiProcessMetaData.getApiProcessName() + pathParams;
      }
      else
      {
         return apiProcessMetaData.getApiProcessName() + pathParams;
      }
   }

}
