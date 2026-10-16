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
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


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
   private static final QLogger LOG = QLogger.getLogger(JobSessions.class);

   /** createSession context key: build a fresh session (do not use a session-store copy) */
   public static final String BYPASS_SESSION_STORE = "bypassSessionStore";

   static final String SCOPE_USER_FULL_NAME = "__userFullName";

   /** the audit channel of work done under a job grant */
   public static final String CHANNEL_JOB = "job";

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

      QAuthenticationModuleInterface  auth  = new QAuthenticationModuleDispatcher().getQModule(instance.getAuthentication());
      Map<String, List<Serializable>> scope = readScope(job.getValueString("credentialScopeJson"));
      List<Serializable>              grant = scope.get(JobGrants.SCOPE_GRANT);

      QSession session;
      boolean  granted = grant != null && !grant.isEmpty();
      if(granted)
      {
         session = sessionFromGrant(instance, auth, job, userId, String.valueOf(grant.get(0)), scope);
      }
      else
      {
         session = auth.createAutomatedSessionForUser(instance, userId);
      }
      scope.remove(JobGrants.SCOPE_GRANT);

      if(session == null || session instanceof QSystemUserSession || session.getUser() == null || !userId.equals(session.getUser().getIdReference()))
      {
         throw (new QPermissionDeniedException("Could not establish the session of the job's owner."));
      }

      List<Serializable> name = scope.remove(SCOPE_USER_FULL_NAME);
      if(session.getUser().getFullName() == null && name != null && !name.isEmpty())
      {
         //////////////////////////////////////////////////////////////////////
         // audits name the user by full name: keep the launching user's if //
         // the auth module's automated session does not set one            //
         //////////////////////////////////////////////////////////////////////
         session.getUser().setFullName(String.valueOf(name.get(0)));
      }
      intersect(session, scope);

      ///////////////////////////////////////////////////////////////////////
      // audits attribute the work to the launching user, through channel  //
      // "job" (keeping the launch channel, e.g. job:assistant/turn-12)    //
      ///////////////////////////////////////////////////////////////////////
      String launchChannel = job.getValueString("clientChannel");
      session.setClientChannel(granted ? (StringUtils.hasContent(launchChannel) ? CHANNEL_JOB + ":" + launchChannel : CHANNEL_JOB) : launchChannel);
      DurableJobsSecurity.applyToSession(session);
      com.kingsrook.qqq.backend.core.actions.profile.UserProfiles.applyIfInstalled(instance, session);
      return (session);
   }



   /*******************************************************************************
    ** Whether a session comes from an access token rather than an interactive
    ** sign-in: a scoped credential (PAT, QCredentialScope) or a bearer request.
    ** Such sessions may not launch durable jobs - a worker could not hold the job
    ** to the token's scope, expiry or revocation.
    *******************************************************************************/
   public static boolean isTokenSession(QSession session)
   {
      return (session != null && (session.getCredentialScope() != null || StringUtils.hasContent(session.getValue("qqq.oauth2.bearer"))));
   }



   /*******************************************************************************
    ** Bind a just-launched job to the launching sign-in (JobGrants), when grants
    ** are configured and the session is a real sign-in. Never fails the launch:
    ** a job without a grant falls back to the auth module's automated session.
    *******************************************************************************/
   public static void attachGrant(QInstance instance, QSession session, Integer jobId, String userId, Map<String, List<Serializable>> launchScope)
   {
      if(!JobGrants.isEnabled(instance) || session == null || session instanceof QSystemUserSession || !StringUtils.hasContent(session.getUuid()))
      {
         return;
      }

      ///////////////////////////////////////////////////////////////////
      // token sessions never reach here (DurableJobs.launch refuses  //
      // them); defensively, never bind a grant to one                //
      ///////////////////////////////////////////////////////////////////
      if(isTokenSession(session))
      {
         return;
      }
      try
      {
         Map<String, List<Serializable>> withGrant = new LinkedHashMap<>(launchScope);
         withGrant.put(JobGrants.SCOPE_GRANT, new ArrayList<>(List.of(JobGrants.issue(instance, jobId, userId, session.getUuid(), launchScope))));
         SystemScope.call(() -> new UpdateAction().execute(new UpdateInput(DurableJobsTables.TABLE_JOB)
            .withRecord(new QRecord().withValue("id", jobId).withValue("credentialScopeJson", JsonUtils.toJson(withGrant)))));
      }
      catch(Exception e)
      {
         LOG.warn("Could not attach a grant to the job; a worker may not be able to run it as its owner", e, logPair("jobId", jobId));
      }
   }



   /*******************************************************************************
    ** Rebuild the launching user's session from the job's grant: verified, then
    ** re-established by the auth module from the launching sign-in (so current
    ** permissions apply, and an ended sign-in fails the job).
    *******************************************************************************/
   private static QSession sessionFromGrant(QInstance instance, QAuthenticationModuleInterface auth, QRecord job, String userId, String grant, Map<String, List<Serializable>> scope) throws QException
   {
      Map<String, List<Serializable>> signedScope = new LinkedHashMap<>(scope);
      signedScope.remove(JobGrants.SCOPE_GRANT);
      String sessionUUID = JobGrants.verify(instance, job.getValueInteger("id"), userId, grant, signedScope);

      QSession session;
      try
      {
         session = auth.createSession(instance, Map.of("sessionUUID", sessionUUID, BYPASS_SESSION_STORE, "true"));
      }
      catch(Exception e)
      {
         throw (new QPermissionDeniedException("The job's owner is no longer signed in (signed out, or the sign-in expired or was revoked), so the job cannot continue as them. Run it again."));
      }
      if(session == null || session.getUser() == null || !userId.equals(session.getUser().getIdReference()))
      {
         throw (new QPermissionDeniedException("The job's sign-in belongs to another user."));
      }
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
