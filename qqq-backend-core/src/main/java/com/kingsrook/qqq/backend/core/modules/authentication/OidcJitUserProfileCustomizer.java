/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.authentication;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.json.JSONObject;


/*******************************************************************************
 ** Reusable OIDC session customizer that just-in-time provisions a local user
 ** profile and maps its primary key into the QQQ security context.
 *******************************************************************************/
public class OidcJitUserProfileCustomizer implements QAuthenticationModuleCustomizerInterface
{
   public static final String DEFAULT_TABLE_NAME        = "userProfile";
   public static final String DEFAULT_SUBJECT_FIELD     = "oidcSubject";
   public static final String DEFAULT_SECURITY_KEY_NAME = "userProfileId";

   private final String tableName;
   private final String subjectField;
   private final String securityKeyName;



   /***************************************************************************
    ** Construct a customizer using the conventional Business Platform names.
    ***************************************************************************/
   public OidcJitUserProfileCustomizer()
   {
      this(DEFAULT_TABLE_NAME, DEFAULT_SUBJECT_FIELD, DEFAULT_SECURITY_KEY_NAME);
   }



   /***************************************************************************
    ** Construct a customizer for application-specific table/key names.
    ***************************************************************************/
   public OidcJitUserProfileCustomizer(String tableName, String subjectField, String securityKeyName)
   {
      this.tableName = tableName;
      this.subjectField = subjectField;
      this.securityKeyName = securityKeyName;
   }



   /***************************************************************************
    ** Resolve or create the profile identified by the immutable OIDC subject.
    ***************************************************************************/
   @Override
   public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
   {
      JSONObject claims = getClaims(context);
      String subject = claims.optString("sub", null);
      if(subject == null || subject.isBlank())
      {
         throw new IllegalArgumentException("OIDC sub claim is required for JIT user provisioning");
      }

      try
      {
         QRecord profile = new GetAction().executeForRecord(new GetInput(tableName)
            .withUniqueKey(Map.of(subjectField, subject)));
         if(profile == null)
         {
            profile = new InsertAction().execute(new InsertInput(tableName).withRecord(new QRecord()
               .withValue(subjectField, subject)
               .withValue("email", claims.optString("email", null))
               .withValue("fullName", claims.optString("name", null)))).getRecords().get(0);
         }

         Serializable profileId = profile.getValue(qInstance.getTable(tableName).getPrimaryKeyField());
         qSession.withSecurityKeyValue(securityKeyName, profileId);
      }
      catch(QException e)
      {
         throw new IllegalStateException("Failed to provision OIDC user profile", e);
      }
   }



   /***************************************************************************
    ** Prefer ID-token claims but support access-token claims for providers
    ** (including Keycloak configurations) that duplicate identity claims.
    ***************************************************************************/
   private JSONObject getClaims(Map<String, Object> context)
   {
      Object claims = context.get("idToken");
      if(!(claims instanceof JSONObject))
      {
         claims = context.get("jwtPayloadJsonObject");
      }
      if(claims instanceof JSONObject jsonObject)
      {
         return jsonObject;
      }
      throw new IllegalArgumentException("OIDC claims are required for JIT user provisioning");
   }
}
