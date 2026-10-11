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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionRefreshTokenMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSessionRefreshToken;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** The userSessionRefreshToken table (#1048) must not be readable through the
 ** v1 table API - query, get, count or export - by an ordinary session, while
 ** an ordinary table in the same instance is.
 *******************************************************************************/
class UserSessionRefreshTokenTableLockdownSpecV1Test extends SpecTestBase
{
   private static final String STORED_VALUE = "v1:default:not-a-real-ciphertext-" + UUID.randomUUID();



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableQuerySpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new TableGetSpecV1(), new TableCountSpecV1(), new TableExportSpecV1()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** Add the refresh-token table to the standard test instance.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.addTable(new UserSessionRefreshTokenMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      return (qInstance);
   }



   /***************************************************************************
    ** Also store a refresh-token row.
    ***************************************************************************/
   @Override
   protected void primeTestData(QInstance qInstance) throws Exception
   {
      super.primeTestData(qInstance);
      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession()), () ->
         new InsertAction().execute(new InsertInput(UserSessionRefreshToken.TABLE_NAME).withRecordEntity(new UserSessionRefreshToken()
            .withSessionUuid(UUID.randomUUID().toString())
            .withRefreshToken(STORED_VALUE))));
   }



   /*******************************************************************************
    ** query, get, count and export of the refresh-token table are all denied,
    ** and never return the stored value; the same calls on an ordinary table
    ** succeed (so the denial is the table's permission rules at work).
    *******************************************************************************/
   @Test
   void testRefreshTokenTableIsNotReadable()
   {
      assertNotNull(serverQInstance.getTable(UserSessionRefreshToken.TABLE_NAME));

      String               tablePath = getBaseUrlAndPath() + "/table/" + UserSessionRefreshToken.TABLE_NAME;
      HttpResponse<String> query     = Unirest.post(tablePath + "/query").contentType(ContentType.APPLICATION_JSON.getMimeType()).body("{}").asString();
      HttpResponse<String> get       = Unirest.get(tablePath + "/1").asString();
      HttpResponse<String> count     = Unirest.post(tablePath + "/count").contentType(ContentType.APPLICATION_JSON.getMimeType()).body("{}").asString();
      HttpResponse<String> export    = Unirest.post(tablePath + "/export").contentType(ContentType.APPLICATION_JSON.getMimeType()).body(JsonUtils.toJson(Map.of("format", "csv"))).asString();

      for(HttpResponse<String> response : List.of(query, get, count, export))
      {
         assertEquals(403, response.getStatus());
         assertThat(response.getBody()).doesNotContain(STORED_VALUE);
      }

      HttpResponse<String> control = Unirest.post(getBaseUrlAndPath() + "/table/person/query").contentType(ContentType.APPLICATION_JSON.getMimeType()).body("{}").asString();
      assertEquals(200, control.getStatus());
   }

}
