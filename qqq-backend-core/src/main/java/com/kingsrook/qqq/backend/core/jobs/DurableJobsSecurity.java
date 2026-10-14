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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Puts the durable-jobs security keys into a session: jobs_user = the user's id,
 ** plus jobs_user_all_access for sessions holding the jobs_admin permission.
 **
 ** A host calls applyToSession from its authentication customizer
 ** (QAuthenticationModuleCustomizerInterface.finalCustomizeSession), the same as
 ** for the alerts keys. The kernel also ensures the keys on the sessions it
 ** builds for workers and on launch/control calls, so a host that forgets still
 ** gets correct (owner-only) behaviour there; a session without the keys sees no
 ** jobs through the standard table screens (the lock fails closed).
 *******************************************************************************/
public final class DurableJobsSecurity
{

   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private DurableJobsSecurity()
   {
   }



   /*******************************************************************************
    ** Replace any jobs keys on the session with ones derived from its user and
    ** permissions. No-op for null and system sessions.
    *******************************************************************************/
   public static void applyToSession(QSession session)
   {
      if(session == null || session instanceof QSystemUserSession)
      {
         return;
      }

      Map<String, List<Serializable>> keys = session.getSecurityKeyValues();
      if(keys != null)
      {
         keys.remove(DurableJobsTables.SECURITY_KEY_USER);
         keys.remove(DurableJobsTables.SECURITY_KEY_USER_ALL_ACCESS);
      }

      String userId = userId(session);
      if(StringUtils.hasContent(userId))
      {
         session.withSecurityKeyValue(DurableJobsTables.SECURITY_KEY_USER, userId);
      }
      if(session.hasPermission(DurableJobsTables.PERMISSION_ADMIN))
      {
         session.withSecurityKeyValue(DurableJobsTables.SECURITY_KEY_USER_ALL_ACCESS, true);
      }
   }



   /*******************************************************************************
    ** Apply the keys only when the session has none yet.
    *******************************************************************************/
   public static void ensureSessionKeys(QSession session)
   {
      if(session == null || session instanceof QSystemUserSession)
      {
         return;
      }
      if(session.getSecurityKeyValues(DurableJobsTables.SECURITY_KEY_USER).isEmpty())
      {
         applyToSession(session);
      }
   }



   /*******************************************************************************
    ** The id the jobs tables record as a job's owner.
    *******************************************************************************/
   public static String userId(QSession session)
   {
      if(session == null || session.getUser() == null)
      {
         return (null);
      }
      return (session.getUser().getIdReference());
   }

}
