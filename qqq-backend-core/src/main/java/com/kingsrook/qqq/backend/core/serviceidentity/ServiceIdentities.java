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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeVoidVoidMethod;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Run background work as a service principal (docs/SERVICE-IDENTITY.md).
 **
 ** <pre>
 ** ServiceIdentities.run("scheduler", () -> ...);                    // as the principal
 ** ServiceIdentities.runOnBehalfOf("scheduler", userId, () -> ...);  // as the user, via the principal
 ** </pre>
 **
 ** The instance's authentication module supplies the session: OAuth2 obtains a
 ** real access token and builds a bearer session; local modules mint a
 ** QServiceSession with the principal's declared local permissions. A missing,
 ** unconfigured or refused principal throws - there is never a fallback to the
 ** system user. Call per unit of work (a report, a delivery, a chunk), so the
 ** work always holds a fresh session.
 *******************************************************************************/
public final class ServiceIdentities
{
   private static final QLogger LOG = QLogger.getLogger(ServiceIdentities.class);

   public static final String PRINCIPAL_ID_PREFIX = "service:";
   public static final String VALUE_PRINCIPAL     = "qqq.service.principal";
   public static final String VALUE_ON_BEHALF_OF  = "qqq.service.onBehalfOf";
   public static final String CLIENT_CHANNEL      = "service";

   /////////////////////////////////////////////////////////////////////////////
   // the principal core's scheduled work uses when declared (scheduled       //
   // reports and output deliveries run on behalf of their owners through it) //
   /////////////////////////////////////////////////////////////////////////////
   public static final String SCHEDULER = "scheduler";



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private ServiceIdentities()
   {
   }



   /*******************************************************************************
    ** Whether the instance declares this principal (callers keep their previous
    ** behavior when it does not, so single-node defaults are unchanged).
    *******************************************************************************/
   public static boolean isDeclared(QInstance qInstance, String principalName)
   {
      return (ServiceIdentitiesMetaData.getPrincipal(qInstance, principalName) != null);
   }



   /*******************************************************************************
    ** The principal name from a principal id (service:{name}), or null.
    *******************************************************************************/
   public static String principalNameFromId(String principalId)
   {
      return (principalId != null && principalId.startsWith(PRINCIPAL_ID_PREFIX) ? principalId.substring(PRINCIPAL_ID_PREFIX.length()) : null);
   }



   /*******************************************************************************
    ** A fresh session for the principal, in the current instance.
    *******************************************************************************/
   public static QSession session(String principalName) throws QException
   {
      return (session(QContext.getQInstance(), principalName));
   }



   /*******************************************************************************
    ** A fresh session for the principal.
    *******************************************************************************/
   public static QSession session(QInstance qInstance, String principalName) throws QException
   {
      QServicePrincipalMetaData principal = ServiceIdentitiesMetaData.getPrincipal(qInstance, principalName);
      if(principal == null)
      {
         throw (new QAuthenticationException("Service principal " + principalName + " is not declared."));
      }

      QAuthenticationModuleInterface     module   = module(qInstance);
      QServiceIdentityProviderInterface provider = module.getServiceIdentityProvider();
      if(provider == null)
      {
         throw (new QAuthenticationException("The authentication module does not support service principals."));
      }

      QSession session = provider.sessionFor(qInstance, principal);
      if(session == null || session instanceof QSystemUserSession)
      {
         throw (new QAuthenticationException("Service principal " + principalName + " did not get a session."));
      }

      session.setUser(new QUser().withIdReference(principal.getPrincipalId()).withFullName(principal.getPrincipalId()));
      session.setValue(VALUE_PRINCIPAL, principal.getName());
      session.setClientChannel(CLIENT_CHANNEL);
      QSession current = QContext.getQSession();
      if(current != null && current.getBackendVariants() != null && session.getBackendVariants() == null)
      {
         session.setBackendVariants(new HashMap<>(current.getBackendVariants()));
      }
      return (session);
   }



   /*******************************************************************************
    ** Run work as the principal; the caller's context is restored afterwards.
    *******************************************************************************/
   public static void run(String principalName, UnsafeVoidVoidMethod<? extends Exception> body) throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QSession  session   = session(qInstance, principalName);
      runIn(qInstance, session, body);
   }



   /*******************************************************************************
    ** Run work as the principal and return its result.
    *******************************************************************************/
   public static <T> T call(String principalName, UnsafeSupplier<T, ? extends Exception> body) throws QException
   {
      List<T> result = new ArrayList<>(1);
      run(principalName, () -> result.add(body.get()));
      return (result.get(0));
   }



   /*******************************************************************************
    ** A session for work the principal does on behalf of a user: the user's
    ** CURRENT access (rebuilt by the authentication module now, so a removed or
    ** deactivated user fails) intersected with the principal's permissions
    ** (owner decision D2), attributed to the principal. Valid only while the
    ** principal's own session is.
    *******************************************************************************/
   public static QSession sessionOnBehalfOf(QInstance qInstance, String principalName, String userId) throws QException
   {
      QServicePrincipalMetaData principal = ServiceIdentitiesMetaData.getPrincipal(qInstance, principalName);
      if(principal == null)
      {
         throw (new QAuthenticationException("Service principal " + principalName + " is not declared."));
      }
      if(!principal.getMayActOnBehalfOfUsers())
      {
         throw (new QPermissionDeniedException("Service principal " + principalName + " may not act on behalf of users."));
      }
      if(!StringUtils.hasContent(userId) || userId.startsWith(PRINCIPAL_ID_PREFIX))
      {
         throw (new QPermissionDeniedException("Work on behalf of a user needs a user."));
      }

      QSession                       parent = session(qInstance, principalName);
      QAuthenticationModuleInterface module = module(qInstance);

      QSession[] user = new QSession[1];
      try
      {
         QContext.withTemporaryContext(new CapturedContext(qInstance, parent), () -> user[0] = module.createAutomatedSessionForUser(qInstance, userId));
      }
      catch(QAuthenticationException e)
      {
         LOG.info("Could not establish a user's session for delegated work", logPair("principal", principalName), logPair("userId", userId), logPair("reason", e.getMessage()));
         throw (new QPermissionDeniedException("Could not establish the session of the user this work is for."));
      }
      if(user[0] == null || user[0] instanceof QSystemUserSession || user[0].getUser() == null || !Objects.equals(userId, user[0].getUser().getIdReference()))
      {
         throw (new QPermissionDeniedException("Could not establish the session of the user this work is for."));
      }

      QDelegatedSession delegated = new QDelegatedSession(parent, principal.getName());
      delegated.setIdReference(delegated.getUuid());
      delegated.setUser(new QUser().withIdReference(userId).withFullName(user[0].getUser().getFullName()).withEmail(user[0].getUser().getEmail()));
      delegated.setPermissions(intersect(user[0].getPermissions(), parent));
      delegated.setSecurityKeyValues(keysWithinPrincipal(qInstance, user[0].getSecurityKeyValues(), parent));
      if(user[0].getBackendVariants() != null)
      {
         delegated.setBackendVariants(new HashMap<>(user[0].getBackendVariants()));
      }
      if(user[0].getValues() != null)
      {
         user[0].getValues().forEach(delegated::setValue);
      }
      delegated.setCredentialScope(user[0].getCredentialScope());
      delegated.setValue(VALUE_PRINCIPAL, principal.getName());
      delegated.setValue(VALUE_ON_BEHALF_OF, userId);
      delegated.setClientChannel(CLIENT_CHANNEL);
      return (delegated);
   }



   /*******************************************************************************
    ** Run work on behalf of a user, via the principal.
    *******************************************************************************/
   public static void runOnBehalfOf(String principalName, String userId, UnsafeVoidVoidMethod<? extends Exception> body) throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      runIn(qInstance, sessionOnBehalfOf(qInstance, principalName, userId), body);
   }



   /*******************************************************************************
    ** Whether a session is valid for the module - the single place service and
    ** delegated sessions are judged (ActionHelper.validateSession):
    ** - a delegated session is valid while its parent is;
    ** - a QServiceSession only when the module mints local principals and this
    **   JVM minted it (never under OAuth2 or Auth0);
    ** - anything else is up to the module, unchanged.
    *******************************************************************************/
   public static boolean isSessionValid(QInstance qInstance, QAuthenticationModuleInterface module, QSession session)
   {
      if(session instanceof QDelegatedSession delegated)
      {
         QSession parent = delegated.getParent();
         return (parent != null && !(parent instanceof QDelegatedSession) && isSessionValid(qInstance, module, parent));
      }
      if(session instanceof QServiceSession)
      {
         return (module.getServiceIdentityProvider() instanceof LocalServiceIdentityProvider && LocalServiceIdentityProvider.isMinted(session));
      }
      return (module.isSessionValid(qInstance, session));
   }



   /*******************************************************************************
    ** Whether this is a principal's own session minted in this JVM (an OAuth2
    ** service bearer session or a local QServiceSession) - as opposed to a
    ** request's bearer token or a hand-made session. Delegated sessions are not.
    *******************************************************************************/
   public static boolean isMintedServiceSession(QSession session)
   {
      if(session == null || session instanceof QDelegatedSession || principalOf(session) == null)
      {
         return (false);
      }
      return (LocalServiceIdentityProvider.isMinted(session) || OAuth2ServiceIdentityProvider.accessTokenFor(session).isPresent());
   }



   /*******************************************************************************
    ** The principal a session runs as (service or delegated), or null.
    *******************************************************************************/
   public static String principalOf(QSession session)
   {
      return (session == null ? null : session.getValue(VALUE_PRINCIPAL));
   }



   /*******************************************************************************
    ** Audit/log name: service:{name}, or {user} via service:{name}; null for
    ** other sessions.
    *******************************************************************************/
   public static String attributionName(QSession session)
   {
      String principal = principalOf(session);
      if(principal == null)
      {
         return (null);
      }
      if(session instanceof QDelegatedSession)
      {
         String who = session.getUser() == null ? null : session.getUser().getFullName();
         if(!StringUtils.hasContent(who))
         {
            who = session.getUser() == null ? "unknown" : session.getUser().getIdReference();
         }
         return (who + " via " + PRINCIPAL_ID_PREFIX + principal);
      }
      return (PRINCIPAL_ID_PREFIX + principal);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void runIn(QInstance qInstance, QSession session, UnsafeVoidVoidMethod<? extends Exception> body) throws QException
   {
      UnsafeVoidVoidMethod<Exception> work = body::run;
      try
      {
         QContext.withTemporaryContext(new CapturedContext(qInstance, session), work);
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException(e));
      }
   }



   /*******************************************************************************
    ** The user's permissions that the principal also holds.
    *******************************************************************************/
   private static Set<String> intersect(Set<String> userPermissions, QSession parent)
   {
      Set<String> result = new TreeSet<>();
      if(userPermissions != null)
      {
         for(String permission : userPermissions)
         {
            if(parent.hasPermission(permission))
            {
               result.add(permission);
            }
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** The user's record-lock keys, except all-access keys the principal lacks:
    ** delegation never widens what the principal itself could see.
    *******************************************************************************/
   private static Map<String, List<Serializable>> keysWithinPrincipal(QInstance qInstance, Map<String, List<Serializable>> userKeys, QSession parent)
   {
      Set<String> allAccessKeys = new HashSet<>();
      if(qInstance.getSecurityKeyTypes() != null)
      {
         for(QSecurityKeyType type : qInstance.getSecurityKeyTypes().values())
         {
            if(StringUtils.hasContent(type.getAllAccessKeyName()))
            {
               allAccessKeys.add(type.getAllAccessKeyName());
            }
         }
      }

      Map<String, List<Serializable>> result = new HashMap<>();
      if(userKeys != null)
      {
         for(Map.Entry<String, List<Serializable>> entry : userKeys.entrySet())
         {
            if(allAccessKeys.contains(entry.getKey()) && !parent.hasSecurityKeyValue(entry.getKey(), true))
            {
               LOG.debug("Delegated session drops an all-access key the principal lacks", logPair("key", entry.getKey()));
               continue;
            }
            result.put(entry.getKey(), entry.getValue() == null ? null : new ArrayList<>(entry.getValue()));
         }
      }
      return (result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QAuthenticationModuleInterface module(QInstance qInstance) throws QException
   {
      return (new QAuthenticationModuleDispatcher().getQModule(qInstance.getAuthentication()));
   }
}
