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

import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Tests the reusable OIDC just-in-time user profile session customizer.
 *******************************************************************************/
class OidcJitUserProfileCustomizerTest extends BaseTest
{
   /***************************************************************************
    ** First login provisions one profile and repeat login reuses it.
    ***************************************************************************/
   @Test
   void firstLoginCreatesProfileAndMapsItsIdToSecurityKey() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData()
         .withName("userProfile")
         .withBackendName("memory")
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("oidcSubject"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("oidcSubject", QFieldType.STRING))
         .withField(new QFieldMetaData("email", QFieldType.STRING))
         .withField(new QFieldMetaData("fullName", QFieldType.STRING)));

      JSONObject claims = new JSONObject(Map.of(
         "sub", "keycloak-subject-42",
         "email", "person@example.test",
         "name", "Example Person"));
      QSession firstSession = new QSession();
      QSession secondSession = new QSession();
      OidcJitUserProfileCustomizer customizer = new OidcJitUserProfileCustomizer();

      customizer.customizeSession(qInstance, firstSession, Map.of("jwtPayloadJsonObject", claims));
      customizer.customizeSession(qInstance, secondSession, Map.of("jwtPayloadJsonObject", claims));

      var profiles = new QueryAction().execute(new QueryInput("userProfile")).getRecords();
      assertEquals(1, profiles.size(), "repeat login must not duplicate the profile");
      QRecord profile = profiles.get(0);
      assertEquals("keycloak-subject-42", profile.getValueString("oidcSubject"));
      assertEquals("person@example.test", profile.getValueString("email"));
      assertEquals(profile.getValue("id"), firstSession.getSecurityKeyValues("userProfileId").get(0));
      assertEquals(profile.getValue("id"), secondSession.getSecurityKeyValues("userProfileId").get(0));
   }
}
