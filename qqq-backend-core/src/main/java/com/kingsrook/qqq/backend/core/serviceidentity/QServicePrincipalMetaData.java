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

package com.kingsrook.qqq.backend.core.serviceidentity;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/*******************************************************************************
 ** A named identity that background work runs as (docs/SERVICE-IDENTITY.md).
 **
 ** Under OAuth2 its authority comes only from the identity provider: the
 ** principal's service account and that account's groups and policy. The
 ** credentials are read from the environment (never from meta-data, the
 ** database or the filesystem): QQQ_SERVICE_{NAME}_USERNAME and
 ** QQQ_SERVICE_{NAME}_SECRET, NAME upper-cased with non-alphanumerics as _.
 **
 ** localPermissions and localSecurityKeyValues apply ONLY to deployments whose
 ** authentication module supports local principals (TABLE_BASED, MOCK, fully
 ** anonymous). OAuth2 and Auth0 ignore them.
 *******************************************************************************/
public class QServicePrincipalMetaData implements Serializable
{
   private String                           name;
   private String                           label;
   private String                           description;
   private boolean                          mayActOnBehalfOfUsers  = false;
   private Set<String>                      localPermissions       = new TreeSet<>();
   private Map<String, List<Serializable>> localSecurityKeyValues = new LinkedHashMap<>();



   /*******************************************************************************
    ** The id written to audits and logs: service:{name}.
    *******************************************************************************/
   public String getPrincipalId()
   {
      return (ServiceIdentities.PRINCIPAL_ID_PREFIX + name);
   }



   /*******************************************************************************
    ** Name of the environment variable holding the service account's username.
    *******************************************************************************/
   public String getUsernameEnvironmentVariable()
   {
      return ("QQQ_SERVICE_" + environmentName() + "_USERNAME");
   }



   /*******************************************************************************
    ** Name of the environment variable holding the service account's secret.
    *******************************************************************************/
   public String getSecretEnvironmentVariable()
   {
      return ("QQQ_SERVICE_" + environmentName() + "_SECRET");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String environmentName()
   {
      return (name == null ? "" : name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_"));
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
    ** Fluent setter for name - lowercase letters, digits and dashes (e.g. scheduler).
    *******************************************************************************/
   public QServicePrincipalMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label == null ? this.name : this.label);
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
   public QServicePrincipalMetaData withLabel(String label)
   {
      this.label = label;
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
   public QServicePrincipalMetaData withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mayActOnBehalfOfUsers
    *******************************************************************************/
   public boolean getMayActOnBehalfOfUsers()
   {
      return (this.mayActOnBehalfOfUsers);
   }



   /*******************************************************************************
    ** Setter for mayActOnBehalfOfUsers
    *******************************************************************************/
   public void setMayActOnBehalfOfUsers(boolean mayActOnBehalfOfUsers)
   {
      this.mayActOnBehalfOfUsers = mayActOnBehalfOfUsers;
   }



   /*******************************************************************************
    ** Fluent setter for mayActOnBehalfOfUsers - whether this principal may run
    ** work on behalf of a user (it must also hold qqq.service.actOnBehalf).
    *******************************************************************************/
   public QServicePrincipalMetaData withMayActOnBehalfOfUsers(boolean mayActOnBehalfOfUsers)
   {
      this.mayActOnBehalfOfUsers = mayActOnBehalfOfUsers;
      return (this);
   }



   /*******************************************************************************
    ** Getter for localPermissions
    *******************************************************************************/
   public Set<String> getLocalPermissions()
   {
      return (this.localPermissions);
   }



   /*******************************************************************************
    ** Setter for localPermissions
    *******************************************************************************/
   public void setLocalPermissions(Set<String> localPermissions)
   {
      this.localPermissions = localPermissions == null ? new TreeSet<>() : new TreeSet<>(localPermissions);
   }



   /*******************************************************************************
    ** Fluent setter for localPermissions - session permission names (e.g.
    ** reports.hasAccess) for local deployments only.
    *******************************************************************************/
   public QServicePrincipalMetaData withLocalPermissions(String... localPermissions)
   {
      this.localPermissions.addAll(List.of(localPermissions));
      return (this);
   }



   /*******************************************************************************
    ** Getter for localSecurityKeyValues
    *******************************************************************************/
   public Map<String, List<Serializable>> getLocalSecurityKeyValues()
   {
      return (this.localSecurityKeyValues);
   }



   /*******************************************************************************
    ** Setter for localSecurityKeyValues
    *******************************************************************************/
   public void setLocalSecurityKeyValues(Map<String, List<Serializable>> localSecurityKeyValues)
   {
      this.localSecurityKeyValues = localSecurityKeyValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(localSecurityKeyValues);
   }



   /*******************************************************************************
    ** Fluent setter for one local security key value (local deployments only).
    *******************************************************************************/
   public QServicePrincipalMetaData withLocalSecurityKeyValue(String keyName, Serializable value)
   {
      this.localSecurityKeyValues.computeIfAbsent(keyName, k -> new ArrayList<>()).add(value);
      return (this);
   }
}
