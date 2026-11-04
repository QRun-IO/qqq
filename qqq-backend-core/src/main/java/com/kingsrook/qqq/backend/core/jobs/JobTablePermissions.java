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

package com.kingsrook.qqq.backend.core.jobs;


import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** Only the jobs kernel (system sessions) may write job records. A job's process,
 ** status, owner scope, source and parameters are decisions made by launch and
 ** control, which check the caller's permissions, eligibility and caps; a generic
 ** table write by the owner (the record lock only keeps them to their own rows,
 ** and field editability is a UI hint, not enforced) would let them skip all of
 ** that. Reads are unchanged: the owner record lock still decides what is seen.
 *******************************************************************************/
public class JobTablePermissions implements CustomPermissionChecker
{
   /*******************************************************************************
    ** Deny writes from anything but a kernel system session.
    *******************************************************************************/
   @Override
   public void checkPermissionsThrowing(AbstractActionInput input, MetaDataWithPermissionRules metadata) throws QPermissionDeniedException
   {
      if(QContext.getQSession() instanceof QSystemUserSession)
      {
         return;
      }

      if(input instanceof InsertInput || input instanceof UpdateInput || input instanceof DeleteInput || input instanceof ReplaceInput || input instanceof ConditionalUpdateInput)
      {
         throw (new QPermissionDeniedException("Job records are managed by the jobs kernel; start or control a job with its actions."));
      }
   }
}
