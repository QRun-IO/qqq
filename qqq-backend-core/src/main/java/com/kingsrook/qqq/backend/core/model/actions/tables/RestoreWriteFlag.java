/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables;


import java.util.Set;


/*******************************************************************************
 ** ActionFlag for "restore-mode" writes - data operations such as restoring a
 ** backup, which must put records back exactly as they were, rather than treating
 ** the write as a business event.
 **
 ** May be added to InsertInput, UpdateInput and ReplaceInput.  Only a session
 ** that has the permission named by {@link #REQUIRED_PERMISSION} may use it - the
 ** actions reject the input otherwise (see CorePermissionEnforcer.checkRestoreWriteFlag),
 ** whether or not the instance enforces permissions in core actions.
 **
 ** When present:
 ** <ul>
 **   <li>supplied create-date and modify-date values are kept (the
 **       DynamicDefaultValueBehavior only fills nulls)</li>
 **   <li>if suppressAutomations: records are not put in a PENDING automation
 **       status - an insert gets OK (unless it supplied a status), and an update
 **       leaves the status alone</li>
 **   <li>table customizers (pre and post) only run if runCustomizers</li>
 **   <li>the per-record DML audit is replaced by one summary audit per action
 **       call, unless perRecordAudit</li>
 **   <li>record-change listeners (e.g., ESB publication) are skipped, unless
 **       publishEvents</li>
 ** </ul>
 **
 ** Record-security locks, field-security write checks, unique-key checks and
 ** required-field and type validation always still run.
 **
 ** Instances are used as plain values:  find the one on an input via {@link #from(Set)}.
 *******************************************************************************/
public class RestoreWriteFlag implements ActionFlag
{
   /////////////////////////////////////////////////////////////////////////
   // the permission name a session must have to use this flag on a write //
   /////////////////////////////////////////////////////////////////////////
   public static final String REQUIRED_PERMISSION = "backups.restore";

   private boolean suppressAutomations = true;
   private boolean runCustomizers      = false;
   private boolean perRecordAudit      = false;
   private boolean publishEvents       = false;



   /*******************************************************************************
    ** find the RestoreWriteFlag (if any) in a set of flags (which may be null).
    *******************************************************************************/
   public static RestoreWriteFlag from(Set<ActionFlag> flags)
   {
      if(flags != null)
      {
         for(ActionFlag flag : flags)
         {
            if(flag instanceof RestoreWriteFlag restoreWriteFlag)
            {
               return (restoreWriteFlag);
            }
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** Getter for suppressAutomations
    *******************************************************************************/
   public boolean getSuppressAutomations()
   {
      return (this.suppressAutomations);
   }



   /*******************************************************************************
    ** Fluent setter for suppressAutomations (default true)
    *******************************************************************************/
   public RestoreWriteFlag withSuppressAutomations(boolean suppressAutomations)
   {
      this.suppressAutomations = suppressAutomations;
      return (this);
   }



   /*******************************************************************************
    ** Getter for runCustomizers
    *******************************************************************************/
   public boolean getRunCustomizers()
   {
      return (this.runCustomizers);
   }



   /*******************************************************************************
    ** Fluent setter for runCustomizers (default false)
    *******************************************************************************/
   public RestoreWriteFlag withRunCustomizers(boolean runCustomizers)
   {
      this.runCustomizers = runCustomizers;
      return (this);
   }



   /*******************************************************************************
    ** Getter for perRecordAudit
    *******************************************************************************/
   public boolean getPerRecordAudit()
   {
      return (this.perRecordAudit);
   }



   /*******************************************************************************
    ** Fluent setter for perRecordAudit (default false)
    *******************************************************************************/
   public RestoreWriteFlag withPerRecordAudit(boolean perRecordAudit)
   {
      this.perRecordAudit = perRecordAudit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for publishEvents
    *******************************************************************************/
   public boolean getPublishEvents()
   {
      return (this.publishEvents);
   }



   /*******************************************************************************
    ** Fluent setter for publishEvents (default false)
    *******************************************************************************/
   public RestoreWriteFlag withPublishEvents(boolean publishEvents)
   {
      this.publishEvents = publishEvents;
      return (this);
   }

}
