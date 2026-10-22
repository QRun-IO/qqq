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

package com.kingsrook.qqq.backend.core.model.session;


import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/*******************************************************************************
 ** The scope of the credential a session was created from - e.g. an access
 ** token (Authentik app-password / OAuth client) limited to some tables, or to
 ** reading.  Set on the QSession by the authentication module's customizer
 ** (e.g. qbit-authentik from its token mapping); enforced by core:
 **
 ** - PermissionsHelper intersects it with the user's live permissions: an
 **   object outside a non-null set is denied (hidden from metadata, refused);
 **   readOnly denies table writes and every process not explicitly listed.
 **   A scope only narrows - revoking the user's permission narrows the token.
 ** - The UI API refuses operations outside operationIds (`METHOD /template`,
 **   e.g. `POST /table/{tableName}/query`).
 ** - The session's client channel is `token:<credentialId>` (audited); the
 **   request cannot relabel it.
 **
 ** Every set is null for "unrestricted".  See docs/UI-API-CLIENT-CHANNELS.md.
 *******************************************************************************/
public class QCredentialScope implements Serializable, Cloneable
{
   @Serial
   private static final long serialVersionUID = 1L;

   private String      credentialId;
   private boolean     readOnly;
   private Set<String> apps;
   private Set<String> tables;
   private Set<String> processes;
   private Set<String> reports;
   private Set<String> widgets;
   private Set<String> operationIds;
   private Map<String, Map<String, Set<String>>> operationPathParameters;



   /*******************************************************************************
    ** Whether a set allows a name (null set: unrestricted).
    *******************************************************************************/
   public static boolean allows(Set<String> set, String name)
   {
      return (set == null || (name != null && set.contains(name)));
   }



   /*******************************************************************************
    ** The client channel of sessions under this scope: token:<credentialId>.
    *******************************************************************************/
   public String getClientChannel()
   {
      return ("token:" + (credentialId == null ? "unknown" : credentialId));
   }



   /*******************************************************************************
    ** A deep copy.
    *******************************************************************************/
   @Override
   public QCredentialScope clone()
   {
      try
      {
         QCredentialScope clone = (QCredentialScope) super.clone();
         clone.apps = copy(apps);
         clone.tables = copy(tables);
         clone.processes = copy(processes);
         clone.reports = copy(reports);
         clone.widgets = copy(widgets);
         clone.operationIds = copy(operationIds);
         clone.withOperationPathParameters(operationPathParameters);
         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw (new AssertionError(e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Set<String> view(Set<String> set)
   {
      return (set == null ? null : Collections.unmodifiableSet(set));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Set<String> copy(Set<String> set)
   {
      return (set == null ? null : new LinkedHashSet<>(set));
   }



   /*******************************************************************************
    ** Getter for credentialId
    *******************************************************************************/
   public String getCredentialId()
   {
      return (this.credentialId);
   }



   /*******************************************************************************
    ** Fluent setter for credentialId - the token identifier (for audit).
    *******************************************************************************/
   public QCredentialScope withCredentialId(String credentialId)
   {
      this.credentialId = credentialId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for readOnly
    *******************************************************************************/
   public boolean getReadOnly()
   {
      return (this.readOnly);
   }



   /*******************************************************************************
    ** Fluent setter for readOnly - no table writes, no unlisted processes.
    *******************************************************************************/
   public QCredentialScope withReadOnly(boolean readOnly)
   {
      this.readOnly = readOnly;
      return (this);
   }



   /*******************************************************************************
    ** Getter for apps
    *******************************************************************************/
   public Set<String> getApps()
   {
      return (view(this.apps));
   }



   /*******************************************************************************
    ** Fluent setter for apps (null: unrestricted)
    *******************************************************************************/
   public QCredentialScope withApps(Set<String> apps)
   {
      this.apps = copy(apps);
      return (this);
   }



   /*******************************************************************************
    ** Getter for tables
    *******************************************************************************/
   public Set<String> getTables()
   {
      return (view(this.tables));
   }



   /*******************************************************************************
    ** Fluent setter for tables (null: unrestricted)
    *******************************************************************************/
   public QCredentialScope withTables(Set<String> tables)
   {
      this.tables = copy(tables);
      return (this);
   }



   /*******************************************************************************
    ** Getter for processes
    *******************************************************************************/
   public Set<String> getProcesses()
   {
      return (view(this.processes));
   }



   /*******************************************************************************
    ** Fluent setter for processes (null: unrestricted, unless readOnly)
    *******************************************************************************/
   public QCredentialScope withProcesses(Set<String> processes)
   {
      this.processes = copy(processes);
      return (this);
   }



   /*******************************************************************************
    ** Getter for reports
    *******************************************************************************/
   public Set<String> getReports()
   {
      return (view(this.reports));
   }



   /*******************************************************************************
    ** Fluent setter for reports (null: unrestricted)
    *******************************************************************************/
   public QCredentialScope withReports(Set<String> reports)
   {
      this.reports = copy(reports);
      return (this);
   }



   /*******************************************************************************
    ** Getter for widgets
    *******************************************************************************/
   public Set<String> getWidgets()
   {
      return (view(this.widgets));
   }



   /*******************************************************************************
    ** Fluent setter for widgets (null: unrestricted)
    *******************************************************************************/
   public QCredentialScope withWidgets(Set<String> widgets)
   {
      this.widgets = copy(widgets);
      return (this);
   }



   /*******************************************************************************
    ** Getter for operationIds
    *******************************************************************************/
   public Set<String> getOperationIds()
   {
      return (view(this.operationIds));
   }



   /*******************************************************************************
    ** Fluent setter for operationIds - UI API operations as `METHOD /template`
    ** (null: unrestricted).
    *******************************************************************************/
   public QCredentialScope withOperationIds(Set<String> operationIds)
   {
      this.operationIds = copy(operationIds);
      return (this);
   }



   /*******************************************************************************
    ** HTTP boundary allowlist keyed by canonical METHOD /template. A null map
    ** adds no restriction; an empty map denies every operation. Each operation
    ** maps parameter names to allowed decoded values; an empty parameter map
    ** allows that operation without object restrictions. Internal actions retain
    ** normal user permissions and the existing global object scope.
    *******************************************************************************/
   public QCredentialScope withOperationPathParameters(Map<String, Map<String, Set<String>>> operations)
   {
      if(operations == null)
      {
         operationPathParameters = null;
         return (this);
      }
      Map<String, Map<String, Set<String>>> copied = new LinkedHashMap<>();
      operations.forEach((operation, parameters) ->
      {
         Objects.requireNonNull(operation, "operation");
         Map<String, Set<String>> copiedParameters = new LinkedHashMap<>();
         Objects.requireNonNull(parameters, "parameters").forEach((name, values) ->
            copiedParameters.put(Objects.requireNonNull(name, "parameter"), Set.copyOf(values)));
         copied.put(operation, Collections.unmodifiableMap(copiedParameters));
      });
      operationPathParameters = Collections.unmodifiableMap(copied);
      return (this);
   }



   /*******************************************************************************
    ** Immutable operation and parameter allowlists, or null when unrestricted.
    *******************************************************************************/
   public Map<String, Map<String, Set<String>>> getOperationPathParameters()
   {
      return (operationPathParameters);
   }



   /*******************************************************************************
    ** Match exact route parameters supplied by the router; missing names fail
    ** closed. This is additional to operationIds, never an alternate grant.
    *******************************************************************************/
   public boolean allowsOperationPathParameters(String operation, Map<String, String> parameters)
   {
      if(operationPathParameters == null)
      {
         return (true);
      }
      Map<String, Set<String>> allowed = operationPathParameters.get(operation);
      return (allowed != null && parameters != null && allowed.entrySet().stream().allMatch(entry -> parameters.get(entry.getKey()) != null
         && entry.getValue().contains(parameters.get(entry.getKey()))));
   }
}
