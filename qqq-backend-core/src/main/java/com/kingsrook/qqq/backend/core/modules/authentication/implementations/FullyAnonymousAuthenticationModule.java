/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.SimpleStateKey;
import com.kingsrook.qqq.backend.core.state.StateProviderInterface;


/*******************************************************************************
 ** An authentication module with no actual backing system - all users are treated
 ** as anonymous, and all sessions are always valid.
 **
 ** <p>Sessions are persisted using StateProvider, allowing session values to
 ** be stored and retrieved across requests. The session UUID is stored in a
 ** cookie and used to restore session data.</p>
 *******************************************************************************/
public class FullyAnonymousAuthenticationModule implements QAuthenticationModuleInterface
{
   public static final String TEST_ACCESS_TOKEN = "b0a88d00-8439-48e8-8b48-e0ef40c40ed9";
   public static final String SESSION_UUID_KEY  = "sessionUUID";

   private static final String SESSION_STATE_KEY_PREFIX = "FullyAnonymousSession:";



   /*******************************************************************************
    ** Get the state provider for storing session data.
    **
    ** @return The state provider instance
    *******************************************************************************/
   private static StateProviderInterface getStateProvider()
   {
      // TODO: Could be configurable via metadata in the future
      return InMemoryStateProvider.getInstance();
   }



   /*******************************************************************************
    ** Persist session data to StateProvider.
    **
    ** <p>This should be called after modifying session values to ensure
    ** they persist across requests.</p>
    **
    ** @param session The session to persist
    *******************************************************************************/
   public static void persistSession(QSession session)
   {
      if(session == null || session.getUuid() == null)
      {
         return;
      }

      SessionData sessionData = new SessionData();
      sessionData.values = session.getValues();
      sessionData.valuesForFrontend = session.getValuesForFrontend();

      SimpleStateKey<String> stateKey = new SimpleStateKey<>(SESSION_STATE_KEY_PREFIX + session.getUuid());
      getStateProvider().put(stateKey, sessionData);
   }



   /*******************************************************************************
    ** Create or restore a session.
    **
    ** <p>If a session UUID is provided in the context, attempts to restore
    ** the session from StateProvider. Otherwise, creates a new session.</p>
    **
    ** @param qInstance The QInstance
    ** @param context The authentication context (may contain sessionUUID)
    ** @return The created or restored session
    *******************************************************************************/
   @Override
   public QSession createSession(QInstance qInstance, Map<String, String> context)
   {
      QUser qUser = new QUser();
      qUser.setIdReference("anonymous");
      qUser.setFullName("Anonymous");

      QSession qSession    = new QSession();
      String   sessionUuid = null;

      ///////////////////////////////////////////////////////////////////////////
      // Check for existing session UUID in context                          //
      ///////////////////////////////////////////////////////////////////////////
      if(context != null)
      {
         // Check for sessionUUID (used by QJavalinImplementation)
         if(context.containsKey(SESSION_UUID_KEY))
         {
            sessionUuid = context.get(SESSION_UUID_KEY);
         }
         // Check for sessionId (legacy/alternative key)
         else if(context.containsKey("sessionId"))
         {
            sessionUuid = context.get("sessionId");
         }
      }

      ///////////////////////////////////////////////////////////////////////////
      // If we have a session UUID, try to restore the session              //
      ///////////////////////////////////////////////////////////////////////////
      if(sessionUuid != null && !sessionUuid.isEmpty())
      {
         qSession.setUuid(sessionUuid);
         qSession.setIdReference(sessionUuid);

         // Try to restore session data from StateProvider
         SimpleStateKey<String> stateKey    = new SimpleStateKey<>(SESSION_STATE_KEY_PREFIX + sessionUuid);
         Optional<SessionData>  sessionData = getStateProvider().get(SessionData.class, stateKey);
         if(sessionData.isPresent())
         {
            // Restore session values
            if(sessionData.get().values != null)
            {
               qSession.setValues(sessionData.get().values);
            }
            if(sessionData.get().valuesForFrontend != null)
            {
               qSession.setValuesForFrontend(sessionData.get().valuesForFrontend);
            }
         }
      }
      else
      {
         ///////////////////////////////////////////////////////////////////////////
         // Create new session with new UUID                                    //
         ///////////////////////////////////////////////////////////////////////////
         sessionUuid = UUID.randomUUID().toString();
         qSession.setUuid(sessionUuid);
         qSession.setIdReference("Session:" + sessionUuid);
      }

      qSession.setUser(qUser);

      return (qSession);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isSessionValid(QInstance instance, QSession session)
   {
      return session != null;
   }



   /*******************************************************************************
    ** Indicates that this module uses session ID cookies for session management.
    **
    ** @return true to enable session cookie management
    *******************************************************************************/
   @Override
   public boolean usesSessionIdCookie()
   {
      return true;
   }



   /*******************************************************************************
    ** Data class for storing session state in StateProvider.
    *******************************************************************************/
   private static class SessionData implements Serializable
   {
      private static final long serialVersionUID = 1L;
      Map<String, String>       values;
      Map<String, Serializable> valuesForFrontend;
   }

}
