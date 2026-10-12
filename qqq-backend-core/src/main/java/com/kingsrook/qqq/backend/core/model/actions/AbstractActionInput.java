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

package com.kingsrook.qqq.backend.core.model.actions;


import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.async.NonPersistedAsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Base input class for all Q actions.
 **
 *******************************************************************************/
public class AbstractActionInput
{
   private static final QLogger LOG = QLogger.getLogger(AbstractActionInput.class);

   private AsyncJobCallback asyncJobCallback;

   private PermissionEnforcement permissionEnforcement  = PermissionEnforcement.DEFAULT;
   private String                permissionBypassReason = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   public AbstractActionInput()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getActionIdentity()
   {
      return (getClass().getSimpleName());
   }



   /*******************************************************************************
    ** performance instance validation (if not previously done).
    * // todo - verify this is happening (e.g., when context is set i guess)
    *******************************************************************************/
   private void validateInstance(QInstance instance)
   {
      ////////////////////////////////////////////////////////////
      // if this instance hasn't been validated yet, do so now  //
      // noting that this will also enrich any missing metaData //
      ////////////////////////////////////////////////////////////
      if(!instance.getHasBeenValidated())
      {
         try
         {
            new QInstanceValidator().validate(instance);
         }
         catch(QInstanceValidationException e)
         {
            LOG.warn(e);
            throw (new IllegalArgumentException("QInstance failed validation" + e.getMessage(), e));
         }
      }
   }



   /*******************************************************************************
    ** Getter for asyncJobCallback
    **
    *******************************************************************************/
   @JsonIgnore
   public AsyncJobCallback getAsyncJobCallback()
   {
      if(asyncJobCallback == null)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // don't return null here (too easy to NPE).  instead, if someone wants one of these, create one and give it to them. //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         asyncJobCallback = new NonPersistedAsyncJobCallback(UUID.randomUUID(), new AsyncJobStatus().withJobName(getClass().getSimpleName()));
      }
      return asyncJobCallback;
   }



   /*******************************************************************************
    ** Setter for asyncJobCallback
    **
    *******************************************************************************/
   public void setAsyncJobCallback(AsyncJobCallback asyncJobCallback)
   {
      this.asyncJobCallback = asyncJobCallback;
   }


   /*******************************************************************************
    ** Getter for permissionEnforcement - how core actions should treat permissions
    ** for this input.  See PermissionEnforcement and CorePermissionEnforcer.
    *******************************************************************************/
   @JsonIgnore
   public PermissionEnforcement getPermissionEnforcement()
   {
      return (this.permissionEnforcement == null ? PermissionEnforcement.DEFAULT : this.permissionEnforcement);
   }



   /*******************************************************************************
    ** Setter for permissionEnforcement.  Accepts DEFAULT or ENFORCE.  SKIP must be
    ** requested through withPermissionBypass(reason), so that every bypass of
    ** permission checks carries an explicit justification.
    *******************************************************************************/
   public void setPermissionEnforcement(PermissionEnforcement permissionEnforcement)
   {
      if(PermissionEnforcement.SKIP.equals(permissionEnforcement))
      {
         throw (new IllegalArgumentException("PermissionEnforcement.SKIP may only be set via withPermissionBypass(reason)"));
      }
      this.permissionEnforcement = permissionEnforcement;
      this.permissionBypassReason = null;
   }



   /*******************************************************************************
    ** Fluent setter for permissionEnforcement (DEFAULT or ENFORCE).
    *******************************************************************************/
   public AbstractActionInput withPermissionEnforcement(PermissionEnforcement permissionEnforcement)
   {
      setPermissionEnforcement(permissionEnforcement);
      return (this);
   }



   /*******************************************************************************
    ** Mark this input as a trusted, framework- or system-internal call, which the
    ** core actions will not subject to table/process permission checks or field
    ** security locks - regardless of the instance setting.  The reason is required,
    ** is logged when the bypass is applied, and makes bypasses easy to audit (grep
    ** for withPermissionBypass).  Record security locks are NOT affected by this.
    *******************************************************************************/
   public AbstractActionInput withPermissionBypass(String reason)
   {
      if(!StringUtils.hasContent(reason))
      {
         throw (new IllegalArgumentException("A reason is required to bypass permission enforcement"));
      }
      this.permissionEnforcement = PermissionEnforcement.SKIP;
      this.permissionBypassReason = reason;
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionBypassReason (only set when permissionEnforcement is SKIP)
    *******************************************************************************/
   @JsonIgnore
   public String getPermissionBypassReason()
   {
      return (this.permissionBypassReason);
   }
}
