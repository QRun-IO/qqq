/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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
// SPDX-License-Identifier: Apache-2.0

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** Optional, private native logout material. Never part of QSession serialization.
 *******************************************************************************/
public class NativeSessionLogoutSupport
{
   public static final String FIELD = "nativeLogoutContext";
   public static final String NATIVE_FLOW_KEY = "nativeSessionFlow";
   private static final QLogger LOG = QLogger.getLogger(NativeSessionLogoutSupport.class);



   /*******************************************************************************
    ** Capability requires explicit host migration, native policy and a usable key.
    *******************************************************************************/
   public static boolean isConfigured(QInstance instance)
   {
      try
      {
         return (instance.getAuthentication() instanceof OAuth2AuthenticationMetaData auth
            && auth.getName() != null && !auth.getName().isBlank()
            && auth.getNativeSessionLogin() != null && auth.getNativeSessionLogin().isValid() && auth.getNativeSessionLogin().getRequireNonceValidation()
            && auth.getNativeSessionLogout() != null && auth.getNativeSessionLogout().isValid()
            && instance.getTable(UserSession.TABLE_NAME) != null && instance.getTable(UserSession.TABLE_NAME).getField(FIELD) != null
            && RefreshTokenCipher.fromMetaData(auth) != null);
      }
      catch(Exception ignored)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** Preserve refresh AAD exactly; a JSON tuple prevents ambiguous delimiters.
    *******************************************************************************/
   static String associatedData(String provider, String uuid, String userId)
   {
      return (new JSONArray(List.of("qqq.oauth2.nativeLogout.v1", provider, uuid, userId)).toString());
   }



   /*******************************************************************************
    ** Store only after a server-validated native exchange; never fail login here.
    *******************************************************************************/
   public void retain(QSession session, String rawIdToken, String callback)
   {
      QInstance instance = QContext.getQInstance();
      if(!isConfigured(instance) || rawIdToken == null || rawIdToken.isBlank() || rawIdToken.length() > 65536)
      {
         return;
      }
      OAuth2AuthenticationMetaData auth = (OAuth2AuthenticationMetaData) instance.getAuthentication();
      if(!auth.getNativeSessionLogin().getRedirectUris().contains(callback))
      {
         return;
      }
      try
      {
         QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance)), () ->
         {
            QRecord row = row(session.getUuid());
            if(row == null || !Objects.equals(row.getValueString("userId"), session.getUser().getIdReference()))
            {
               return;
            }
            String envelope = new JSONObject().put("idToken", rawIdToken).put("provider", auth.getName())
               .put("issuer", auth.getNativeSessionLogin().getIssuer()).put("clientId", auth.getClientId()).put("callback", callback).toString();
            String encrypted = RefreshTokenCipher.fromMetaData(auth).encrypt(envelope,
               associatedData(auth.getName(), session.getUuid(), row.getValueString("userId")));
            var updated = new UpdateAction().execute(new UpdateInput(UserSession.TABLE_NAME)
               .withRecord(new QRecord().withValue("id", row.getValue("id")).withValue(FIELD, encrypted)));
            if(updated.getRecords() == null || updated.getRecords().size() != 1
               || CollectionUtils.nullSafeHasContents(updated.getRecords().get(0).getErrors()))
            {
               throw (new QException("Could not retain native logout context"));
            }
         });
      }
      catch(Exception ignored)
      {
         LOG.warn("Native provider logout context was not retained");
      }
   }



   /*******************************************************************************
    ** Capture only for the current authenticated cookie session, before deletion.
    *******************************************************************************/
   public NativeSessionLogoutData prepare(String sessionUuid, String callback)
   {
      QInstance instance = QContext.getQInstance();
      QSession session = QContext.getQSession();
      if(!isConfigured(instance) || session == null || session.getUser() == null || session instanceof QSystemUserSession
         || !Objects.equals(sessionUuid, session.getUuid()) || "true".equals(session.getValue(OAuth2AuthenticationModule.BEARER_SESSION_VALUE_KEY)))
      {
         return (null);
      }
      OAuth2AuthenticationMetaData auth = (OAuth2AuthenticationMetaData) instance.getAuthentication();
      if(!auth.getNativeSessionLogout().getPostLogoutRedirectUris().contains(callback))
      {
         return (null);
      }
      try
      {
         AtomicReference<NativeSessionLogoutData> result = new AtomicReference<>();
         QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance)), () ->
         {
            QRecord row = row(sessionUuid);
            if(row == null || row.getValueString(FIELD) == null || !Objects.equals(row.getValueString("userId"), session.getUser().getIdReference()))
            {
               return;
            }
            String decoded = RefreshTokenCipher.fromMetaData(auth).decrypt(row.getValueString(FIELD),
               associatedData(auth.getName(), sessionUuid, row.getValueString("userId")));
            JSONObject envelope = new JSONObject(decoded);
            if(!auth.getName().equals(envelope.getString("provider")) || !auth.getClientId().equals(envelope.getString("clientId"))
               || !auth.getNativeSessionLogin().getIssuer().equals(envelope.getString("issuer"))
               || !auth.getNativeSessionLogin().getRedirectUris().contains(envelope.getString("callback")))
            {
               return;
            }
            String endpoint = resolveEndSessionEndpoint(auth);
            if(endpoint != null)
            {
               result.set(new NativeSessionLogoutData(envelope.getString("issuer"), endpoint, callback, envelope.getString("idToken")));
            }
         });
         return (result.get());
      }
      catch(Exception ignored)
      {
         LOG.info("Native provider logout is unavailable");
         return (null);
      }
   }



   /*******************************************************************************
    ** Fetch public discovery with bounded I/O and no redirect or credentials.
    *******************************************************************************/
   protected String resolveEndSessionEndpoint(OAuth2AuthenticationMetaData auth) throws Exception
   {
      HttpsURLConnection connection = (HttpsURLConnection) URI.create(auth.getNativeSessionLogin().getDiscoveryUrl()).toURL().openConnection();
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      try
      {
         if(connection.getResponseCode() != 200)
         {
            return (null);
         }
         try(InputStream body = connection.getInputStream())
         {
            byte[] bytes = body.readNBytes(65537);
            if(bytes.length > 65536)
            {
               return (null);
            }
            JSONObject discovery = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if(!auth.getNativeSessionLogin().getIssuer().equals(discovery.optString("issuer")))
            {
               return (null);
            }
            URI endpoint = URI.create(discovery.getString("end_session_endpoint"));
            return ("https".equals(endpoint.getScheme()) && endpoint.getHost() != null && endpoint.getRawUserInfo() == null
               && endpoint.getRawQuery() == null && endpoint.getRawFragment() == null ? endpoint.toString() : null);
         }
      }
      finally
      {
         connection.disconnect();
      }
   }



   /*******************************************************************************
    ** Internal session lookup; callers have already entered a system context.
    *******************************************************************************/
   private static QRecord row(String uuid) throws QException
   {
      return (new GetAction().executeForRecord(new GetInput(UserSession.TABLE_NAME).withUniqueKey(Map.of("uuid", uuid))
         .withShouldMaskPasswords(false).withShouldOmitHiddenFields(false)));
   }
}
