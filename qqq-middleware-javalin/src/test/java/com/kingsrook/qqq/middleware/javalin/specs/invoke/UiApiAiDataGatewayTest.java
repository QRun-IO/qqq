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

package com.kingsrook.qqq.middleware.javalin.specs.invoke;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** The AI tool kernel's UI API data gateway: every read is a /qqq/v1 call as
 ** the user, through the assistant channel - so the assistant wall applies.
 *******************************************************************************/
class UiApiAiDataGatewayTest
{
   private static final int    PORT          = 6273;
   private static final String NOTES         = "aiNote";
   private static final String WALLED_TABLE  = "assistant_profile";

   private QApplicationJavalinServer javalinServer;



   /***************************************************************************
    ** an instance with an open notes table and a walled table
    ***************************************************************************/
   public static class GatewayApplication extends AbstractQQQApplication
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QInstance defineQInstance()
      {
         QInstance qInstance = TestUtils.defineInstance();
         for(String name : List.of(NOTES, WALLED_TABLE))
         {
            qInstance.addTable(new QTableMetaData()
               .withName(name)
               .withBackendName(TestUtils.BACKEND_NAME_MEMORY)
               .withPrimaryKeyField("id")
               .withField(new QFieldMetaData("id", QFieldType.INTEGER))
               .withField(new QFieldMetaData("title", QFieldType.STRING)));
         }
         return (qInstance);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      if(javalinServer != null)
      {
         javalinServer.stop();
      }
      QJavalinImplementation.setSessionCookieHttpOnly(false);
      MemoryRecordStore.fullReset();
      QContext.clear();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testReadsGoThroughTheUiApiAsTheAssistant() throws Exception
   {
      javalinServer = new QApplicationJavalinServer(new GatewayApplication())
         .withPort(PORT)
         .withServeFrontendNext(false)
         .withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false);
      javalinServer.start();

      ///////////////////////////////////////////////////////////////
      // seed rows (memory backend, shared with the server's JVM) //
      ///////////////////////////////////////////////////////////////
      QContext.init(javalinServer.getQInstance(), new QSession());
      for(String table : List.of(NOTES, WALLED_TABLE))
      {
         new InsertAction().execute(new InsertInput(table).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("title", "First"),
            new QRecord().withValue("id", 2).withValue("title", "Second"))));
      }

      UiApiAiDataGateway gateway = new UiApiAiDataGateway(UiApiInvoker.forServer(javalinServer, "assistant"), UiApiCredentials.ofSessionUUID("user-session"), "turn-1");

      QueryInput query = new QueryInput(NOTES);
      query.setFilter(new QQueryFilter(new QFilterCriteria("title", QCriteriaOperator.EQUALS, "Second")));
      List<QRecord> records = gateway.query(query);
      assertEquals(1, records.size());
      assertEquals(2, records.get(0).getValueInteger("id"));
      assertEquals("Second", records.get(0).getValueString("title"));

      CountInput count = new CountInput(NOTES);
      assertEquals(2, gateway.count(count));

      GetInput get = new GetInput(NOTES);
      get.setPrimaryKey(1);
      assertEquals("First", gateway.get(get).getValueString("title"));
      GetInput missing = new GetInput(NOTES);
      missing.setPrimaryKey(99);
      assertNull(gateway.get(missing));

      ///////////////////////////////////////////////////////////////////////
      // the assistant wall holds: the same user's walled table is refused //
      ///////////////////////////////////////////////////////////////////////
      assertThrows(QPermissionDeniedException.class, () -> gateway.query(new QueryInput(WALLED_TABLE)));
      assertThrows(QPermissionDeniedException.class, () -> gateway.count(new CountInput(WALLED_TABLE)));

      //////////////////////////////////////////////////
      // no credentials: refused before any request   //
      //////////////////////////////////////////////////
      UiApiAiDataGateway anonymous = new UiApiAiDataGateway(UiApiInvoker.forServer(javalinServer, "assistant"), null, null);
      QException e = assertThrows(QException.class, () -> anonymous.query(new QueryInput(NOTES)));
      assertThat(e.getMessage()).contains("credentials");
   }
}
