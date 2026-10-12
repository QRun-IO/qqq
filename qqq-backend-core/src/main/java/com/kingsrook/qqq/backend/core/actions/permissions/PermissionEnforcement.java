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


package com.kingsrook.qqq.backend.core.actions.permissions;


/*******************************************************************************
 ** How a core action (Query, Get, Count, Aggregate, Insert, Update, Delete,
 ** Replace, RunProcess, RenderWidget, GenerateReport, Export) should treat
 ** table/process/widget/report permissions and field security locks for the
 ** current session.  Set on an action input via
 ** AbstractActionInput.withPermissionEnforcement (ENFORCE) or
 ** AbstractActionInput.withPermissionBypass(reason) (SKIP).
 **
 ** See CorePermissionEnforcer for the full rules.
 *******************************************************************************/
public enum PermissionEnforcement
{
   ////////////////////////////////////////////////////////////////////////////
   // follow the instance setting (QInstance.enforcePermissionsInCoreActions) //
   // for outermost calls; trust calls nested inside another core action.     //
   ////////////////////////////////////////////////////////////////////////////
   DEFAULT,

   ///////////////////////////////////////////////////////////////////////////
   // always check, even when nested inside another action, and even if the //
   // instance setting is off.  Intended for MCP, AI assistants, and other  //
   // entry points that act on behalf of a user.                            //
   ///////////////////////////////////////////////////////////////////////////
   ENFORCE,

   ///////////////////////////////////////////////////////////////////////////////
   // never check.  Only settable through withPermissionBypass(reason), so that //
   // every bypass carries an explicit, greppable, logged justification.        //
   ///////////////////////////////////////////////////////////////////////////////
   SKIP
}
