/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.api.actions;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.module.api.BaseTest;
import com.kingsrook.qqq.backend.module.api.model.AuthorizationType;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;
import com.kingsrook.qqq.backend.module.api.model.metadata.APITableBackendDetails;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Only an explicit provider contract can assert complete native unique-key visibility.
 *******************************************************************************/
class APIUniqueKeyLookupTest extends BaseTest
{
   /*******************************************************************************
    ** The owned provider exposes every page; lookup failures cannot return partial owners.
    *******************************************************************************/
   @Test
   void testConfiguredCompleteProviderAndFailure() throws Exception
   {
      List<String> requests = Collections.synchronizedList(new ArrayList<>());
      AtomicBoolean fail = new AtomicBoolean();
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/children", exchange -> respond(exchange, requests, fail.get()));
      server.start();
      try
      {
         QContext.getQInstance().addBackend(new APIBackendMetaData().withName("discoveryApi")
            .withBaseUrl("http://127.0.0.1:" + server.getAddress().getPort()).withAuthorizationType(AuthorizationType.NONE)
            .withContentType("application/json").withActionUtil(new QCodeReference(PagedActionUtil.class)));
         QTableMetaData table = table();
         QContext.getQInstance().addTable(table);
         UniqueKey key = table.getUniqueKeys().get(0);
         assertThat(UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("parentCode", "ALPHA"), null))
            .extracting(record -> record.getValueInteger("id")).containsExactly(2, 3);
         assertThat(requests).containsExactly("limit=2&skip=0", "limit=2&skip=2", "limit=2&skip=4");
         requests.clear();
         Association association = storedAssociation(table);
         assertThat(AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of("3"), null))
            .singleElement().satisfies(record -> assertThat(record.getValues()).containsOnlyKeys("id", "parentCode")
               .containsEntry("id", 3).containsEntry("parentCode", "ALPHA"));
         assertThat(requests).containsExactly("limit=2&skip=0", "limit=2&skip=2", "limit=2&skip=4");
         requests.clear();
         assertThat(UniqueKeyLookup.readStoredComponents(table, List.of(3), null))
            .extracting(record -> record.getValueString("parentCode")).containsExactly("ALPHA");
         assertThat(requests).containsExactly("limit=2&skip=0", "limit=2&skip=2", "limit=2&skip=4");
         requests.clear();
         table.setPrimaryKeyField(null);
         assertThat(UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("parentCode", "ALPHA"), null)).hasSize(2);
         assertThat(requests).containsExactly("limit=2&skip=0", "limit=2&skip=2", "limit=2&skip=4");
         requests.clear();
         fail.set(true);
         assertThrows(QException.class, () -> UniqueKeyLookup.findConflicts(table, key, new QRecord().withValue("parentCode", "ALPHA"), null));
         assertThat(requests).containsExactly("limit=2&skip=0", "limit=2&skip=2");
         requests.clear();
         table.setPrimaryKeyField("id");
         assertThrows(QException.class, () -> AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of(3), null));
         assertThat(requests).containsExactly("limit=2&skip=0", "limit=2&skip=2");
      }
      finally
      {
         server.stop(0);
      }
   }



   /*******************************************************************************
    ** An ordinary API utility cannot silently claim complete provider visibility.
    *******************************************************************************/
   @Test
   void testUnconfiguredProviderFailsWithoutHttpRequest() throws Exception
   {
      List<String> requests = Collections.synchronizedList(new ArrayList<>());
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/children", exchange -> respond(exchange, requests, false));
      server.start();
      try
      {
         QContext.getQInstance().addBackend(new APIBackendMetaData().withName("discoveryApi")
            .withBaseUrl("http://127.0.0.1:" + server.getAddress().getPort()).withAuthorizationType(AuthorizationType.NONE));
         QTableMetaData table = table();
         QContext.getQInstance().addTable(table);
         QException exception = assertThrows(QException.class, () -> UniqueKeyLookup.findConflicts(table, table.getUniqueKeys().get(0),
            new QRecord().withValue("parentCode", "ALPHA"), null));
         assertThat(exception.getMessage()).contains("complete unique-key lookup");
         assertThat(requests).isEmpty();
         Association association = storedAssociation(table);
         QException refused = assertThrows(QException.class,
            () -> AssociatedRecordDiscovery.readParentValues(table, List.of(association), List.of(3), null));
         assertThat(refused.getMessage()).contains("complete association-value lookup");
         assertThat(requests).isEmpty();
      }
      finally
      {
         server.stop(0);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData table()
   {
      return new QTableMetaData().withName("discoveryChild").withBackendName("discoveryApi")
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER).withBackendName("child_key"))
         .withField(new QFieldMetaData("parentCode", QFieldType.STRING).withBackendName("parent_code").withIsHidden(true).withIsHeavy(true))
         .withUniqueKey(new UniqueKey("parentCode"))
         .withBackendDetails(new APITableBackendDetails().withTablePath("/children"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Association storedAssociation(QTableMetaData table)
   {
      Association association = new Association().withName("stored relationship").withAssociatedTableName(table.getName()).withJoinName("storedValuesJoin");
      table.withAssociation(association);
      QContext.getQInstance().addJoin(new QJoinMetaData().withName(association.getJoinName()).withLeftTable(table.getName()).withRightTable(table.getName())
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("parentCode", "parentCode")));
      return association;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void respond(HttpExchange exchange, List<String> requests, boolean fail) throws IOException
   {
      String query = exchange.getRequestURI().getQuery();
      requests.add(query);
      boolean failThisPage = fail && "limit=2&skip=2".equals(query);
      String response = failThisPage ? "{\"error\":\"local fixture failure\"}" : switch(query)
      {
         case "limit=2&skip=0" -> "[{\"child_key\":1,\"parent_code\":\"BETA\"},{\"child_key\":\"2\",\"parent_code\":\"ALPHA\"}]";
         case "limit=2&skip=2" -> "[{\"child_key\":3,\"parent_code\":\"ALPHA\"},{\"child_key\":4,\"parent_code\":\"BETA\"}]";
         default -> "[]";
      };
      byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(failThisPage ? 500 : 200, bytes.length);
      try(exchange)
      {
         exchange.getResponseBody().write(bytes);
      }
   }



   /*******************************************************************************
    ** This API supports paging only; relationship filtering must remain local.
    *******************************************************************************/
   public static class PagedActionUtil extends BaseAPIActionUtil
   {
      /*******************************************************************************
       ** Only this owned provider promises complete stored fields across every page.
       *******************************************************************************/
      @Override
      public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
      {
         QueryInput query = input.newQueryInput();
         query.setFilter(new QQueryFilter());
         query.setFieldNamesToInclude(null);
         List<QRecord> matches = new ArrayList<>();
         for(QRecord record : doQuery(query.getTable(), query).getRecords())
         {
            if(input.matches(record))
            {
               matches.add(record);
            }
         }
         return matches;
      }



      /*******************************************************************************
       ** This owned endpoint's query contract explicitly returns every stored row.
       *******************************************************************************/
      @Override
      public List<QRecord> lookupUniqueKey(UniqueKeyLookup.Input input) throws QException
      {
         QueryInput query = input.newQueryInput();
         query.setFilter(new QQueryFilter());
         query.setFieldNamesToInclude(null);
         List<QRecord> matches = new ArrayList<>();
         for(QRecord record : doQuery(query.getTable(), query).getRecords())
         {
            if(input.matches(record))
            {
               matches.add(record);
            }
         }
         return matches;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected Integer getApiStandardLimit()
      {
         return 2;
      }



      /*******************************************************************************
       ** Fail the fixture's second page immediately, without retry backoff.
       *******************************************************************************/
      @Override
      protected int getMaxAllowedServerErrors()
      {
         return 0;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected String buildQueryStringForGet(QQueryFilter filter, Integer limit, Integer skip, Map<String, QFieldMetaData> fields) throws QException
      {
         assertFalse(filter.hasAnyCriteria());
         return "?limit=" + limit + "&skip=" + (skip == null ? 0 : skip);
      }
   }
}
