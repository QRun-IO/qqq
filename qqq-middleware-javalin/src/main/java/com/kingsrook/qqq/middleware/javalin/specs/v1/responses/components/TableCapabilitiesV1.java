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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.audits.GetAuditsForRecordProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.DeleteSharedRecordProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.EditSharedRecordProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.GetSharedRecordsProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.sharing.InsertSharedRecordProcess;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** What a client may offer on a table beyond plain CRUD, as typed capabilities - so\nno client hard-codes process names.  Each is computed for the current user.\nSince UI API 1.6.0.
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableCapabilitiesV1 implements ToSchema
{
   public static final String SAVED_VIEW_QUERY_PROCESS  = "querySavedView";
   public static final String SAVED_VIEW_STORE_PROCESS  = "storeSavedView";
   public static final String SAVED_VIEW_DELETE_PROCESS = "deleteSavedView";

   @OpenAPIDescription("Saved views of this table.")
   private SavedViewsCapabilityV1 savedViews;

   @OpenAPIDescription("Audit history of this table's records.")
   private AuditsCapabilityV1     audits;

   @OpenAPIDescription("Record sharing, for a shareable table.")
   private SharingCapabilityV1    sharing;

   @OpenAPIDescription("Bulk insert (load a file into the table).")
   private ProcessCapabilityV1    bulkInsert;

   @OpenAPIDescription("Bulk edit of selected records.")
   private ProcessCapabilityV1    bulkEdit;

   @OpenAPIDescription("Bulk edit from a file.")
   private ProcessCapabilityV1    bulkEditWithFile;

   @OpenAPIDescription("Bulk delete of selected records.")
   private ProcessCapabilityV1    bulkDelete;



   /*******************************************************************************
    ** Getter for savedViews
    *******************************************************************************/
   public SavedViewsCapabilityV1 getSavedViews()
   {
      return (this.savedViews);
   }



   /*******************************************************************************
    ** Fluent setter for savedViews
    *******************************************************************************/
   public TableCapabilitiesV1 withSavedViews(SavedViewsCapabilityV1 savedViews)
   {
      this.savedViews = savedViews;
      return (this);
   }



   /*******************************************************************************
    ** Getter for audits
    *******************************************************************************/
   public AuditsCapabilityV1 getAudits()
   {
      return (this.audits);
   }



   /*******************************************************************************
    ** Fluent setter for audits
    *******************************************************************************/
   public TableCapabilitiesV1 withAudits(AuditsCapabilityV1 audits)
   {
      this.audits = audits;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sharing
    *******************************************************************************/
   public SharingCapabilityV1 getSharing()
   {
      return (this.sharing);
   }



   /*******************************************************************************
    ** Fluent setter for sharing
    *******************************************************************************/
   public TableCapabilitiesV1 withSharing(SharingCapabilityV1 sharing)
   {
      this.sharing = sharing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bulkInsert
    *******************************************************************************/
   public ProcessCapabilityV1 getBulkInsert()
   {
      return (this.bulkInsert);
   }



   /*******************************************************************************
    ** Fluent setter for bulkInsert
    *******************************************************************************/
   public TableCapabilitiesV1 withBulkInsert(ProcessCapabilityV1 bulkInsert)
   {
      this.bulkInsert = bulkInsert;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bulkEdit
    *******************************************************************************/
   public ProcessCapabilityV1 getBulkEdit()
   {
      return (this.bulkEdit);
   }



   /*******************************************************************************
    ** Fluent setter for bulkEdit
    *******************************************************************************/
   public TableCapabilitiesV1 withBulkEdit(ProcessCapabilityV1 bulkEdit)
   {
      this.bulkEdit = bulkEdit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bulkEditWithFile
    *******************************************************************************/
   public ProcessCapabilityV1 getBulkEditWithFile()
   {
      return (this.bulkEditWithFile);
   }



   /*******************************************************************************
    ** Fluent setter for bulkEditWithFile
    *******************************************************************************/
   public TableCapabilitiesV1 withBulkEditWithFile(ProcessCapabilityV1 bulkEditWithFile)
   {
      this.bulkEditWithFile = bulkEditWithFile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bulkDelete
    *******************************************************************************/
   public ProcessCapabilityV1 getBulkDelete()
   {
      return (this.bulkDelete);
   }



   /*******************************************************************************
    ** Fluent setter for bulkDelete
    *******************************************************************************/
   public TableCapabilitiesV1 withBulkDelete(ProcessCapabilityV1 bulkDelete)
   {
      this.bulkDelete = bulkDelete;
      return (this);
   }



   /*******************************************************************************
    ** The capabilities of a table for the session in QContext.
    *******************************************************************************/
   public static TableCapabilitiesV1 of(QTableMetaData table)
   {
      QInstance           instance = QContext.getQInstance();
      AbstractActionInput input    = new AbstractActionInput();
      if(instance == null || table == null)
      {
         return (null);
      }

      TableCapabilitiesV1 rs = new TableCapabilitiesV1();

      ////////////////////////////////////////////////////////////////////////
      // saved views: listing needs the query process; store and delete are //
      // offered only when the user may run them                            //
      ////////////////////////////////////////////////////////////////////////
      boolean canQueryViews = canRun(instance, input, SAVED_VIEW_QUERY_PROCESS);
      rs.withSavedViews(new SavedViewsCapabilityV1()
         .withAvailable(canQueryViews)
         .withQueryProcessName(canQueryViews ? SAVED_VIEW_QUERY_PROCESS : null)
         .withStoreProcessName(canQueryViews && canRun(instance, input, SAVED_VIEW_STORE_PROCESS) ? SAVED_VIEW_STORE_PROCESS : null)
         .withDeleteProcessName(canQueryViews && canRun(instance, input, SAVED_VIEW_DELETE_PROCESS) ? SAVED_VIEW_DELETE_PROCESS : null));

      ////////////////////////////////////////////////////////////////////////////
      // audits: the user must read this table (the audit process enforces it  //
      // too), then run the audit process or read the audit table.  The level  //
      // is informational: a table with level NONE gets no automatic audits,   //
      // but code can still write audits for its records explicitly            //
      ////////////////////////////////////////////////////////////////////////////
      AuditLevel level        = table.getAuditRules() == null || table.getAuditRules().getAuditLevel() == null ? AuditLevel.NONE : table.getAuditRules().getAuditLevel();
      boolean    canReadTable = PermissionsHelper.hasTablePermission(input, table.getName(), TablePermissionSubType.READ);
      boolean    auditProcess = canReadTable && canRun(instance, input, GetAuditsForRecordProcess.NAME);
      boolean    auditTable   = canReadTable && instance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT) != null
         && PermissionsHelper.hasTablePermission(input, AuditsMetaDataProvider.TABLE_NAME_AUDIT, TablePermissionSubType.READ);
      rs.withAudits(new AuditsCapabilityV1()
         .withAvailable(auditProcess || auditTable)
         .withLevel(level.name())
         .withProcessName(auditProcess ? GetAuditsForRecordProcess.NAME : null)
         .withTableName(auditTable ? AuditsMetaDataProvider.TABLE_NAME_AUDIT : null)
         .withDetailTableName(auditTable && instance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL) != null ? AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL : null));

      /////////////////////////////////////
      // sharing: only shareable tables  //
      /////////////////////////////////////
      if(table.getShareableTableMetaData() != null)
      {
         boolean canList = canRun(instance, input, GetSharedRecordsProcess.NAME);
         rs.withSharing(new SharingCapabilityV1()
            .withAvailable(canList)
            .withListProcessName(canList ? GetSharedRecordsProcess.NAME : null)
            .withInsertProcessName(canList && canRun(instance, input, InsertSharedRecordProcess.NAME) ? InsertSharedRecordProcess.NAME : null)
            .withEditProcessName(canList && canRun(instance, input, EditSharedRecordProcess.NAME) ? EditSharedRecordProcess.NAME : null)
            .withDeleteProcessName(canList && canRun(instance, input, DeleteSharedRecordProcess.NAME) ? DeleteSharedRecordProcess.NAME : null));
      }

      rs.withBulkInsert(processCapability(instance, input, table.getName() + ".bulkInsert"));
      rs.withBulkEdit(processCapability(instance, input, table.getName() + ".bulkEdit"));
      rs.withBulkEditWithFile(processCapability(instance, input, table.getName() + ".bulkEditWithFile"));
      rs.withBulkDelete(processCapability(instance, input, table.getName() + ".bulkDelete"));
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ProcessCapabilityV1 processCapability(QInstance instance, AbstractActionInput input, String processName)
   {
      boolean available = canRun(instance, input, processName);
      return (new ProcessCapabilityV1().withAvailable(available).withProcessName(available ? processName : null));
   }



   /*******************************************************************************
    ** The process exists, and the session may run it.
    *******************************************************************************/
   private static boolean canRun(QInstance instance, AbstractActionInput input, String processName)
   {
      try
      {
         return (instance.getProcess(processName) != null && PermissionsHelper.hasProcessPermission(input, processName));
      }
      catch(Exception e)
      {
         return (false);
      }
   }
}
