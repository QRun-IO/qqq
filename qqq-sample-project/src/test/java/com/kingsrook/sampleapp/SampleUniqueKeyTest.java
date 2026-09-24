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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.DuplicateKeyBadInputStatusMessage;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical Carrier fixtures exercise declared uniqueness without a SQL constraint.
 *******************************************************************************/
class SampleUniqueKeyTest
{
   private QInstance instance;
   private SampleJavalinServer server;
   private HttpClient client;
   private URI base;



   /*******************************************************************************
    ** Carrier supplies composite values; only metadata enforces this declared key.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getTable("carrier").withUniqueKey(new UniqueKey("company_code", "service_level"));
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(client != null)
      {
         client.close();
      }
      if(server != null)
      {
         server.stop();
      }
      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSparseUpdateRejectsExistingCompositeKey() throws Exception
   {
      List<List<String>> before = rows();
      QRecord patch = new QRecord().withValue("id", "2").withValue("service_level", "G");
      QRecord result = update(patch).get(0);
      assertAll(() -> assertTrue(result.getErrors().stream().anyMatch(error -> error instanceof DuplicateKeyBadInputStatusMessage)),
         () -> assertEquals(before, rows()),
         () -> assertFalse(patch.getValues().containsKey("company_code")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSelfAndNonKeyUpdatesRemainAllowed() throws Exception
   {
      assertTrue(update(new QRecord().withValue("id", 2).withValue("service_level", "2")).get(0).getErrors().isEmpty());
      assertTrue(update(new QRecord().withValue("id", 2).withValue("name", "Renamed service")).get(0).getErrors().isEmpty());
      assertEquals(List.of("2", "Renamed service", "UPS", "2"), rows().get(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBatchRejectsSecondProposedCompositeKey() throws Exception
   {
      List<QRecord> results = update(new QRecord().withValue("id", 2).withValue("service_level", "NEW"),
         new QRecord().withValue("id", 3).withValue("service_level", "NEW"));
      assertAll(() -> assertTrue(results.get(0).getErrors().isEmpty()),
         () -> assertTrue(results.get(1).getErrors().stream().anyMatch(error -> error instanceof DuplicateKeyBadInputStatusMessage)),
         () -> assertEquals("NEW", rows().get(1).get(3)),
         () -> assertEquals("I", rows().get(2).get(3)));
   }



   /*******************************************************************************
    ** Individually sparse fragments must not combine into an unchecked final key.
    *******************************************************************************/
   @Test
   void testRepeatedTargetCannotBypassCompositeValidation() throws Exception
   {
      List<List<String>> before = rows();
      List<QRecord> results = update(new QRecord().withValue("id", 6).withValue("company_code", "FEDEX"),
         new QRecord().withValue("id", "6").withValue("service_level", "G"));
      assertAll(() -> assertFalse(results.get(0).getErrors().isEmpty()),
         () -> assertFalse(results.get(1).getErrors().isEmpty()),
         () -> assertEquals(before, rows()));
   }



   /*******************************************************************************
    ** Hidden conflicts cannot grant reads or be disclosed in returned write fields.
    *******************************************************************************/
   @Test
   void testHiddenOwnerBlocksUpdate() throws Exception
   {
      hideOtherCarriers();
      List<List<String>> before = rows();
      QRecord result = update(new QRecord().withValue("id", 2).withValue("service_level", "G")).get(0);
      assertAll(() -> assertTrue(result.getErrors().stream().anyMatch(error -> error instanceof DuplicateKeyBadInputStatusMessage)),
         () -> assertEquals(before, rows()),
         () -> assertEquals(List.of(2), visibleIds()),
         () -> assertFalse(result.getValues().containsKey("company_code")));
   }



   /*******************************************************************************
    ** The original hidden owner is the only conflicting row before this INSERT.
    *******************************************************************************/
   @Test
   void testHiddenOwnerBlocksInsert() throws Exception
   {
      hideOtherCarriers();
      List<List<String>> before = rows();
      QRecord result = new InsertAction().execute(new InsertInput("carrier").withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", 77).withValue("name", "Duplicate service")
            .withValue("company_code", "UPS").withValue("service_level", "G"))).getRecords().get(0);
      assertAll(() -> assertTrue(result.getErrors().stream().anyMatch(error -> error instanceof DuplicateKeyBadInputStatusMessage)),
         () -> assertEquals(before, rows()),
         () -> assertEquals(List.of(2), visibleIds()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void hideOtherCarriers() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("carrierId"));
      instance.getTable("carrier").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("carrierId").withFieldName("id"));
      QContext.getQSession().withSecurityKeyValue("carrierId", 2).withSecurityKeyValue("carrierId", 77);
      assertEquals(List.of(2), visibleIds());
   }



   /*******************************************************************************
    ** The ordinary frontend route must surface validation failure without writing.
    *******************************************************************************/
   @Test
   void testHttpSparseDuplicateLeavesDatabaseUnchanged() throws Exception
   {
      startHttp();
      List<List<String>> before = rows();
      HttpResponse<String> response = patch("service_level=G");
      assertAll(() -> assertEquals(400, response.statusCode()),
         () -> assertTrue(response.body().contains("already exists"), response.body()),
         () -> assertEquals(before, rows()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHttpValidSparseUpdatePersistsAndPreservesOtherFields() throws Exception
   {
      startHttp();
      HttpResponse<String> response = patch("service_level=EXPRESS");
      assertEquals(200, response.statusCode(), response.body());
      assertEquals(List.of("2", "UPS 2Day", "UPS", "EXPRESS"), rows().get(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> update(QRecord... records) throws Exception
   {
      return new UpdateAction().execute(new UpdateInput("carrier").withInputSource(QInputSource.USER)
         .withRecords(new ArrayList<>(List.of(records)))).getRecords();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Integer> visibleIds() throws Exception
   {
      return new QueryAction().execute(new QueryInput("carrier")).getRecords().stream().map(record -> record.getValueInteger("id")).sorted().toList();
   }



   /*******************************************************************************
    ** Independent full-row SQL readback cannot inherit the action's visibility.
    *******************************************************************************/
   private List<List<String>> rows() throws Exception
   {
      List<List<String>> result = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement();
          ResultSet rows = statement.executeQuery("SELECT id, name, company_code, service_level FROM carrier ORDER BY id"))
      {
         while(rows.next())
         {
            result.add(List.of(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4)));
         }
      }
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void startHttp() throws Exception
   {
      server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      base = URI.create("http://localhost:" + service.get().port());
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> patch(String body) throws Exception
   {
      return client.send(HttpRequest.newBuilder(base.resolve("/data/carrier/2")).timeout(Duration.ofSeconds(15))
         .header("Content-Type", "application/x-www-form-urlencoded")
         .method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
   }
}
