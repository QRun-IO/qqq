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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Identity for durable jobs (spec section 2.10, R5).
 **
 ** At launch the kernel captures the launching session's security key values.
 ** At every claim the worker builds a fresh session for the job's user with the
 ** app's createAutomatedSessionForUser (so CURRENT permissions apply - a user who
 ** lost access fails the job), then INTERSECTS its security keys with the
 ** captured ones, so a job never has more access than when it was submitted, and
 ** sets the job's client channel (so audits carry e.g. assistant/turnId).
 *******************************************************************************/
public final class JobSessions
{
   static final String SCOPE_USER_FULL_NAME = "__userFullName";

   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private JobSessions()
   {
   }



   /*******************************************************************************
    ** What a launch captures from the session: its security key values (minus the
    ** jobs keys, which are derived) as a JSON-able map.
    *******************************************************************************/
   public static Map<String, List<Serializable>> captureScope(QSession session)
   {
      Map<String, List<Serializable>> scope = new LinkedHashMap<>();
      if(session != null && session.getSecurityKeyValues() != null)
      {
         session.getSecurityKeyValues().forEach((key, values) ->
         {
            if(!DurableJobsTables.SECURITY_KEY_USER.equals(key) && !DurableJobsTables.SECURITY_KEY_USER_ALL_ACCESS.equals(key))
            {
               scope.put(key, new ArrayList<>(Objects.requireNonNullElse(values, List.of())));
            }
         });
      }
      if(session != null && session.getUser() != null && session.getUser().getFullName() != null)
      {
         scope.put(SCOPE_USER_FULL_NAME, new ArrayList<>(List.of(session.getUser().getFullName())));
      }
      return (scope);
   }



   /*******************************************************************************
    ** Build the session a job's chunk runs under. Throws QPermissionDeniedException
    ** when the user's session cannot be established.
    *******************************************************************************/
   public static QSession forJob(QInstance instance, QRecord job) throws QException
   {
      String userId = job.getValueString("userId");
      if(!StringUtils.hasContent(userId))
      {
         throw (new QPermissionDeniedException("The job has no owner."));
      }

      QAuthenticationModuleInterface auth    = new QAuthenticationModuleDispatcher().getQModule(instance.getAuthentication());
      QSession                       session = auth.createAutomatedSessionForUser(instance, userId);
      if(session == null || session instanceof QSystemUserSession || session.getUser() == null || !userId.equals(session.getUser().getIdReference()))
      {
         throw (new QPermissionDeniedException("Could not establish the session of the job's owner."));
      }

      Map<String, List<Serializable>> scope = readScope(job.getValueString("credentialScopeJson"));
      List<Serializable>              name  = scope.remove(SCOPE_USER_FULL_NAME);
      if(session.getUser().getFullName() == null && name != null && !name.isEmpty())
      {
         //////////////////////////////////////////////////////////////////////
         // audits name the user by full name: keep the launching user's if //
         // the auth module's automated session does not set one            //
         //////////////////////////////////////////////////////////////////////
         session.getUser().setFullName(String.valueOf(name.get(0)));
      }
      intersect(session, scope);
      session.setClientChannel(job.getValueString("clientChannel"));
      DurableJobsSecurity.applyToSession(session);
      return (session);
   }



   /*******************************************************************************
    ** Keep only the security key values the launch also had. A key the launch did
    ** not have at all is removed.
    *******************************************************************************/
   static void intersect(QSession session, Map<String, List<Serializable>> captured)
   {
      Map<String, List<Serializable>> current = session.getSecurityKeyValues();
      if(current == null)
      {
         return;
      }

      Map<String, List<Serializable>> result = new HashMap<>();
      for(Map.Entry<String, List<Serializable>> entry : current.entrySet())
      {
         List<Serializable> allowed = captured.get(entry.getKey());
         if(allowed == null)
         {
            continue;
         }
         List<String>       allowedStrings = allowed.stream().map(String::valueOf).toList();
         List<Serializable> kept           = new ArrayList<>();
         for(Serializable value : Objects.requireNonNullElse(entry.getValue(), List.<Serializable>of()))
         {
            if(allowedStrings.contains(String.valueOf(value)))
            {
               kept.add(value);
            }
         }
         if(!kept.isEmpty())
         {
            result.put(entry.getKey(), kept);
         }
      }
      session.setSecurityKeyValues(result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, List<Serializable>> readScope(String json) throws QException
   {
      if(!StringUtils.hasContent(json))
      {
         return (new HashMap<>());
      }
      try
      {
         return (JsonUtils.toObject(json, new TypeReference<>() {}));
      }
      catch(Exception e)
      {
         throw (new QException("Could not read the job's credential scope", e));
      }
   }

}
