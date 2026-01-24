/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.middleware.javalin.executors.io.BackChannelLogoutInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.BackChannelLogoutOutputInterface;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Executor for the OIDC back-channel logout endpoint.
 **
 ** Handles logout tokens sent by the IdP when a user logs out from the IdP
 ** or another application in the SSO ecosystem.
 **
 ** Per OIDC Back-Channel Logout spec:
 ** - The logout_token JWT contains either 'sub' (subject) or 'sid' (session ID)
 ** - We find and delete matching QQQ sessions
 ** - Return HTTP 200 on success (even if no sessions found)
 *******************************************************************************/
public class BackChannelLogoutExecutor extends AbstractMiddlewareExecutor<BackChannelLogoutInput, BackChannelLogoutOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(BackChannelLogoutExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(BackChannelLogoutInput input, BackChannelLogoutOutputInterface output) throws QException
   {
      String logoutToken = input.getLogoutToken();
      if(logoutToken == null || logoutToken.isBlank())
      {
         LOG.warn("Back-channel logout received with empty logout_token");
         return;
      }

      try
      {
         //////////////////////////////////////////////////////////////////////////
         // Parse the logout_token JWT payload (middle section)                   //
         // Note: Full signature validation requires JWKS fetch - for now we      //
         // trust the token since it comes over HTTPS from configured IdP         //
         //////////////////////////////////////////////////////////////////////////
         JSONObject payload = parseJwtPayload(logoutToken);

         String sub = payload.optString("sub", null);
         String sid = payload.optString("sid", null);

         if(sub == null && sid == null)
         {
            LOG.warn("Back-channel logout token missing both 'sub' and 'sid' claims");
            return;
         }

         LOG.info("Processing back-channel logout", logPair("sub", sub), logPair("sid", sid));

         QInstance qInstance     = QContext.getQInstance();
         int       deletedCount  = 0;

         ///////////////////////////////////////////////////////////////////////////
         // If 'sub' is present, we can efficiently query by userId               //
         // (UserSession.userId stores the 'sub' claim from the access token)     //
         ///////////////////////////////////////////////////////////////////////////
         if(sub != null)
         {
            deletedCount = deleteSessionsByUserId(qInstance, sub);
         }

         ///////////////////////////////////////////////////////////////////////////
         // If only 'sid' is present, we need to scan sessions and check their    //
         // access tokens for matching session IDs                                //
         ///////////////////////////////////////////////////////////////////////////
         if(sid != null && deletedCount == 0)
         {
            deletedCount = deleteSessionsByOidcSessionId(qInstance, sid);
         }

         LOG.info("Back-channel logout completed", logPair("deletedSessions", deletedCount));
      }
      catch(Exception e)
      {
         LOG.warn("Error processing back-channel logout", e);
      }
   }



   /*******************************************************************************
    ** Parse JWT payload (middle section) to JSONObject.
    *******************************************************************************/
   private JSONObject parseJwtPayload(String jwt)
   {
      String[] parts = jwt.split("\\.");
      if(parts.length < 2)
      {
         return new JSONObject();
      }
      String payload = new String(Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
      return new JSONObject(payload);
   }



   /*******************************************************************************
    ** Delete sessions by userId (the 'sub' claim).
    *******************************************************************************/
   private int deleteSessionsByUserId(QInstance qInstance, String userId) throws QException
   {
      if(qInstance.getTable(UserSession.TABLE_NAME) == null)
      {
         LOG.debug("UserSession table not found in QInstance, skipping back-channel logout");
         return 0;
      }

      var beforeSession = QContext.getQSession();
      try
      {
         QContext.setQSession(new QSystemUserSession());

         ///////////////////////////////////////
         // Query for sessions with this userId //
         ///////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(UserSession.TABLE_NAME);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("userId", QCriteriaOperator.EQUALS, userId)));
         queryInput.setShouldOmitHiddenFields(false);

         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         List<QRecord> sessions = queryOutput.getRecords();

         if(sessions.isEmpty())
         {
            LOG.debug("No sessions found for userId", logPair("userId", userId));
            return 0;
         }

         ///////////////////////////////////////
         // Delete the matching sessions       //
         ///////////////////////////////////////
         List<String> uuidsToDelete = new ArrayList<>();
         for(QRecord session : sessions)
         {
            uuidsToDelete.add(session.getValueString("uuid"));
         }

         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(UserSession.TABLE_NAME);
         deleteInput.setQueryFilter(new QQueryFilter(new QFilterCriteria("uuid", QCriteriaOperator.IN, uuidsToDelete)));
         new DeleteAction().execute(deleteInput);

         LOG.debug("Deleted sessions by userId", logPair("userId", userId), logPair("count", uuidsToDelete.size()));
         return uuidsToDelete.size();
      }
      finally
      {
         QContext.setQSession(beforeSession);
      }
   }



   /*******************************************************************************
    ** Delete sessions by OIDC session ID ('sid' claim).
    **
    ** This requires scanning sessions and checking each access token for the
    ** matching 'sid' claim. Less efficient than userId lookup but necessary
    ** when only 'sid' is provided in the logout token.
    *******************************************************************************/
   private int deleteSessionsByOidcSessionId(QInstance qInstance, String targetSid) throws QException
   {
      if(qInstance.getTable(UserSession.TABLE_NAME) == null)
      {
         LOG.debug("UserSession table not found in QInstance, skipping back-channel logout");
         return 0;
      }

      var beforeSession = QContext.getQSession();
      try
      {
         QContext.setQSession(new QSystemUserSession());

         ///////////////////////////////////////
         // Query all sessions                 //
         ///////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(UserSession.TABLE_NAME);
         queryInput.setShouldOmitHiddenFields(false);
         queryInput.setShouldMaskPasswords(false);

         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         List<QRecord> sessions = queryOutput.getRecords();

         ///////////////////////////////////////
         // Find sessions with matching 'sid'  //
         ///////////////////////////////////////
         List<String> uuidsToDelete = new ArrayList<>();
         for(QRecord session : sessions)
         {
            String accessToken = session.getValueString("accessToken");
            if(accessToken != null)
            {
               try
               {
                  JSONObject tokenPayload = parseJwtPayload(accessToken);
                  String sessionSid = tokenPayload.optString("sid", null);
                  if(targetSid.equals(sessionSid))
                  {
                     uuidsToDelete.add(session.getValueString("uuid"));
                  }
               }
               catch(Exception e)
               {
                  LOG.debug("Error parsing access token during sid lookup", e);
               }
            }
         }

         if(uuidsToDelete.isEmpty())
         {
            return 0;
         }

         ///////////////////////////////////////
         // Delete the matching sessions       //
         ///////////////////////////////////////
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(UserSession.TABLE_NAME);
         deleteInput.setQueryFilter(new QQueryFilter(new QFilterCriteria("uuid", QCriteriaOperator.IN, uuidsToDelete)));
         new DeleteAction().execute(deleteInput);

         LOG.debug("Deleted sessions by sid", logPair("sid", targetSid), logPair("count", uuidsToDelete.size()));
         return uuidsToDelete.size();
      }
      finally
      {
         QContext.setQSession(beforeSession);
      }
   }

}
