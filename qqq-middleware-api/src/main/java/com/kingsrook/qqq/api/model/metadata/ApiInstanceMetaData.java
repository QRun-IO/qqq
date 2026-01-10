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

package com.kingsrook.qqq.api.model.metadata;


import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.Server;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiInstanceMetaData implements ApiOperation.EnabledOperationsProvider
{
   private String name;
   private String label;
   private String path;
   private String description;
   private String contactEmail;

   private APIVersion       currentVersion;
   private List<APIVersion> supportedVersions;
   private List<APIVersion> pastVersions;
   private List<APIVersion> futureVersions;

   private List<Server> servers;

   private Set<ApiOperation> enabledOperations  = new HashSet<>();
   private Set<ApiOperation> disabledOperations = new HashSet<>();

   private Map<String, SecurityScheme> securitySchemes = new LinkedHashMap<>();

   private boolean includeErrorTooManyRequests = true;



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(String apiName, QInstance qInstance, QInstanceValidator validator)
   {
      validator.assertCondition(Objects.equals(apiName, name), "Name mismatch for instance api (" + apiName + " != " + name + ")");
      validator.assertCondition(StringUtils.hasContent(name), "Missing name for api " + apiName);

      if(validator.assertCondition(StringUtils.hasContent(path), "Missing path for api " + apiName))
      {
         validator.assertCondition(path.startsWith("/"), "Path for api " + apiName + " does not start with '/' (but it needs to).");
         validator.assertCondition(path.endsWith("/"), "Path for api " + apiName + " does not end with '/' (but it needs to).");
      }

      validator.assertCondition(StringUtils.hasContent(label), "Missing label for api " + apiName);
      validator.assertCondition(StringUtils.hasContent(description), "Missing description for api " + apiName);
      validator.assertCondition(StringUtils.hasContent(contactEmail), "Missing contactEmail for api " + apiName);

      Set<APIVersion> allVersions = new HashSet<>();

      if(validator.assertCondition(currentVersion != null, "Missing currentVersion for api " + apiName))
      {
         allVersions.add(currentVersion);
      }

      if(validator.assertCondition(supportedVersions != null, "Missing supportedVersions for api " + apiName))
      {
         validator.assertCondition(supportedVersions.contains(currentVersion), "supportedVersions [" + supportedVersions + "] does not contain currentVersion [" + currentVersion + "] for api " + apiName);
         allVersions.addAll(supportedVersions);
      }

      for(APIVersion pastVersion : CollectionUtils.nonNullList(pastVersions))
      {
         validator.assertCondition(pastVersion.compareTo(currentVersion) < 0, "pastVersion [" + pastVersion + "] is not lexicographically before currentVersion [" + currentVersion + "] for api " + apiName);
         allVersions.add(pastVersion);
      }

      for(APIVersion futureVersion : CollectionUtils.nonNullList(futureVersions))
      {
         validator.assertCondition(futureVersion.compareTo(currentVersion) > 0, "futureVersion [" + futureVersion + "] is not lexicographically after currentVersion [" + currentVersion + "] for api " + apiName);
         allVersions.add(futureVersion);
      }

      /////////////////////////////////
      // validate all table versions //
      /////////////////////////////////
      for(QTableMetaData table : qInstance.getTables().values())
      {
         ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
         if(apiTableMetaDataContainer != null)
         {
            ApiTableMetaData apiTableMetaData = apiTableMetaDataContainer.getApiTableMetaData(apiName);
            if(apiTableMetaData != null)
            {
               if(BooleanUtils.isNotTrue(apiTableMetaData.getIsExcluded()))
               {
                  if(StringUtils.hasContent(apiTableMetaData.getInitialVersion()))
                  {
                     validator.assertCondition(allVersions.contains(new APIVersion(apiTableMetaData.getInitialVersion())), "Table " + table.getName() + "'s initial API version is not a recognized version for api " + apiName);
                  }
               }
            }
         }
      }

      // todo - find duplicate tableApiNames!!
   }



   /*******************************************************************************
    ** Getter for currentVersion
    *******************************************************************************/
   public APIVersion getCurrentVersion()
   {
      return (this.currentVersion);
   }



   /*******************************************************************************
    ** Setter for currentVersion
    *******************************************************************************/
   public void setCurrentVersion(APIVersion currentVersion)
   {
      this.currentVersion = currentVersion;
   }



   /*******************************************************************************
    ** Fluent setter for currentVersion
    *******************************************************************************/
   public ApiInstanceMetaData withCurrentVersion(APIVersion currentVersion)
   {
      this.currentVersion = currentVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pastVersions
    *******************************************************************************/
   public List<APIVersion> getPastVersions()
   {
      return (this.pastVersions);
   }



   /*******************************************************************************
    ** Setter for pastVersions
    *******************************************************************************/
   public void setPastVersions(List<APIVersion> pastVersions)
   {
      this.pastVersions = pastVersions;
   }



   /*******************************************************************************
    ** Fluent setter for pastVersions
    *******************************************************************************/
   public ApiInstanceMetaData withPastVersions(List<APIVersion> pastVersions)
   {
      this.pastVersions = pastVersions;
      return (this);
   }



   /*******************************************************************************
    ** Getter for futureVersions
    *******************************************************************************/
   public List<APIVersion> getFutureVersions()
   {
      return (this.futureVersions);
   }



   /*******************************************************************************
    ** Setter for futureVersions
    *******************************************************************************/
   public void setFutureVersions(List<APIVersion> futureVersions)
   {
      this.futureVersions = futureVersions;
   }



   /*******************************************************************************
    ** Fluent setter for futureVersions
    *******************************************************************************/
   public ApiInstanceMetaData withFutureVersions(List<APIVersion> futureVersions)
   {
      this.futureVersions = futureVersions;
      return (this);
   }



   /*******************************************************************************
    ** Getter for supportedVersions
    *******************************************************************************/
   public List<APIVersion> getSupportedVersions()
   {
      return (this.supportedVersions);
   }



   /*******************************************************************************
    ** Setter for supportedVersions
    *******************************************************************************/
   public void setSupportedVersions(List<APIVersion> supportedVersions)
   {
      this.supportedVersions = supportedVersions;
   }



   /*******************************************************************************
    ** Fluent setter for supportedVersions
    *******************************************************************************/
   public ApiInstanceMetaData withSupportedVersions(List<APIVersion> supportedVersions)
   {
      this.supportedVersions = supportedVersions;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public ApiInstanceMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public ApiInstanceMetaData withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contactEmail
    *******************************************************************************/
   public String getContactEmail()
   {
      return (this.contactEmail);
   }



   /*******************************************************************************
    ** Setter for contactEmail
    *******************************************************************************/
   public void setContactEmail(String contactEmail)
   {
      this.contactEmail = contactEmail;
   }



   /*******************************************************************************
    ** Fluent setter for contactEmail
    *******************************************************************************/
   public ApiInstanceMetaData withContactEmail(String contactEmail)
   {
      this.contactEmail = contactEmail;
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeErrorTooManyRequests
    *******************************************************************************/
   public boolean getIncludeErrorTooManyRequests()
   {
      return (this.includeErrorTooManyRequests);
   }



   /*******************************************************************************
    ** Setter for includeErrorTooManyRequests
    *******************************************************************************/
   public void setIncludeErrorTooManyRequests(boolean includeErrorTooManyRequests)
   {
      this.includeErrorTooManyRequests = includeErrorTooManyRequests;
   }



   /*******************************************************************************
    ** Fluent setter for includeErrorTooManyRequests
    *******************************************************************************/
   public ApiInstanceMetaData withIncludeErrorTooManyRequests(boolean includeErrorTooManyRequests)
   {
      this.includeErrorTooManyRequests = includeErrorTooManyRequests;
      return (this);
   }



   /*******************************************************************************
    ** Getter for servers
    *******************************************************************************/
   public List<Server> getServers()
   {
      return (this.servers);
   }



   /*******************************************************************************
    ** Setter for servers
    *******************************************************************************/
   public void setServers(List<Server> servers)
   {
      this.servers = servers;
   }



   /*******************************************************************************
    ** Fluent setter for servers
    *******************************************************************************/
   public ApiInstanceMetaData withServers(List<Server> servers)
   {
      this.servers = servers;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public ApiInstanceMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for path
    *******************************************************************************/
   public String getPath()
   {
      return (this.path);
   }



   /*******************************************************************************
    ** Setter for path
    *******************************************************************************/
   public void setPath(String path)
   {
      this.path = path;
   }



   /*******************************************************************************
    ** Fluent setter for path
    *******************************************************************************/
   public ApiInstanceMetaData withPath(String path)
   {
      this.path = path;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Set<ApiOperation> getEnabledOperations()
   {
      return (enabledOperations);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Set<ApiOperation> getDisabledOperations()
   {
      return (disabledOperations);
   }



   /*******************************************************************************
    ** Setter for enabledOperations
    *******************************************************************************/
   public void setEnabledOperations(Set<ApiOperation> enabledOperations)
   {
      this.enabledOperations = enabledOperations;
   }



   /*******************************************************************************
    ** Fluent setter for enabledOperations
    *******************************************************************************/
   public ApiInstanceMetaData withEnabledOperations(Set<ApiOperation> enabledOperations)
   {
      this.enabledOperations = enabledOperations;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for enabledOperations
    *******************************************************************************/
   public ApiInstanceMetaData withEnabledOperation(ApiOperation operation)
   {
      return withEnabledOperations(operation);
   }



   /*******************************************************************************
    ** Fluent setter for enabledOperations
    *******************************************************************************/
   public ApiInstanceMetaData withEnabledOperations(ApiOperation... operations)
   {
      if(this.enabledOperations == null)
      {
         this.enabledOperations = new HashSet<>();
      }
      if(operations != null)
      {
         Collections.addAll(this.enabledOperations, operations);
      }
      return (this);
   }



   /*******************************************************************************
    ** Setter for disabledOperations
    *******************************************************************************/
   public void setDisabledOperations(Set<ApiOperation> disabledOperations)
   {
      this.disabledOperations = disabledOperations;
   }



   /*******************************************************************************
    ** Fluent setter for disabledOperations
    *******************************************************************************/
   public ApiInstanceMetaData withDisabledOperations(Set<ApiOperation> disabledOperations)
   {
      this.disabledOperations = disabledOperations;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for disabledOperations
    *******************************************************************************/
   public ApiInstanceMetaData withDisabledOperation(ApiOperation operation)
   {
      return withDisabledOperations(operation);
   }



   /*******************************************************************************
    ** Fluent setter for disabledOperations
    *******************************************************************************/
   public ApiInstanceMetaData withDisabledOperations(ApiOperation... operations)
   {
      if(this.disabledOperations == null)
      {
         this.disabledOperations = new HashSet<>();
      }
      if(operations != null)
      {
         Collections.addAll(this.disabledOperations, operations);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for securitySchemes
    *******************************************************************************/
   public Map<String, SecurityScheme> getSecuritySchemes()
   {
      return (this.securitySchemes);
   }



   /*******************************************************************************
    ** Setter for securitySchemes
    *******************************************************************************/
   public void setSecuritySchemes(Map<String, SecurityScheme> securitySchemes)
   {
      this.securitySchemes = securitySchemes;
   }



   /*******************************************************************************
    ** Fluent setter for securitySchemes
    *******************************************************************************/
   public ApiInstanceMetaData withSecuritySchemes(Map<String, SecurityScheme> securitySchemes)
   {
      this.securitySchemes = securitySchemes;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for securitySchemes
    *******************************************************************************/
   public ApiInstanceMetaData withSecurityScheme(String label, SecurityScheme securityScheme)
   {
      if(this.securitySchemes == null)
      {
         this.securitySchemes = new LinkedHashMap<>();
      }
      this.securitySchemes.put(label, securityScheme);
      return (this);
   }

}
