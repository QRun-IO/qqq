/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatConsumerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.tables.QQQTablesMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.SimpleConnectionProvider;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertFalse;

/*******************************************************************************
 ** Public sample queries and metadata-backed statistics on an owned H2 database.
 ** Native JDBC is the oracle; snapshots copy values at consumer invocation time.
 ******************************************************************************/
@Timeout(20)
abstract class SampleQueryStatisticsAcceptanceFixture
{
   protected static final String ENABLED = "qqq.queryStatManager.enabled";
   protected final List<Snapshot> snapshots = new CopyOnWriteArrayList<>();
   protected final QueryStatManager manager = QueryStatManager.getInstance();
   protected Connection oracle;
   protected QInstance instance;
   protected QSession session;
   private String oldEnabled;
   private Integer oldDelay;
   private Integer oldPeriod;
   private Integer oldThreshold;
   private List<QueryStatConsumerInterface> oldConsumers;



   /*******************************************************************************
    ** Each case has a separate database, canonical sample metadata and session.
    ******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      oldEnabled = System.getProperty(ENABLED);
      oldDelay = manager.getJobInitialDelay();
      oldPeriod = manager.getJobPeriodSeconds();
      oldThreshold = manager.getMinMillisToStore();
      oldConsumers = manager.getQueryStatConsumers();
      manager.stop();
      System.setProperty(ENABLED, "true");
      manager.setJobInitialDelay(3600);
      manager.setJobPeriodSeconds(3600);
      manager.setMinMillisToStore(0);
      manager.setQueryStatConsumers(List.of(stat -> snapshots.add(new Snapshot(stat))));
      instance = SampleMetaDataProvider.defineTestInstance();
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME);
      backend.setDatabaseName("query_stats_" + UUID.randomUUID().toString().replace("-", ""));
      backend.setConnectionProvider(new QCodeReference(SimpleConnectionProvider.class));
      backend.withCapability(Capability.QUERY_STATS);
      backend.setJdbcUrl("jdbc:h2:mem:" + backend.getDatabaseName() + ";MODE=MySQL");
      oracle = DriverManager.getConnection(backend.getJdbcUrl(), "sa", "");
      try(var script = new InputStreamReader(getClass().getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(oracle, script);
      }
      new QQQTablesMetaDataProvider().defineAll(instance, backend.getName(), backend.getName(), this::mapStatisticsTable);
      new QueryStatMetaDataProvider().defineAll(instance, backend.getName(), this::mapStatisticsTable);
      try(var script = new InputStreamReader(getClass().getResourceAsStream("/SampleQueryStatisticsAcceptance.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(oracle, script);
      }
      session = new QSession();
      session.setUuid(UUID.randomUUID().toString());
      QContext.init(instance, session);
      instance.setHasBeenValidated(null);
      new QInstanceValidator().validate(instance);
      manager.start(instance, QSession::new);
   }



   /*******************************************************************************
    ** Stop the scheduler before closing the database; restore singleton settings.
    ******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         manager.stop();
         awaitWorkersStopped();
      }
      finally
      {
         manager.setQueryStatConsumers(oldConsumers);
         manager.setJobInitialDelay(oldDelay);
         manager.setJobPeriodSeconds(oldPeriod);
         manager.setMinMillisToStore(oldThreshold);
         if(oldEnabled == null)
         {
            System.clearProperty(ENABLED);
         }
         else
         {
            System.setProperty(ENABLED, oldEnabled);
         }
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
         if(oracle != null)
         {
            oracle.close();
         }
      }
   }



   /*******************************************************************************
    ** Table-level opt-out prevents statistics storage from observing itself.
    ******************************************************************************/
   protected void mapStatisticsTable(QTableMetaData table)
   {
      table.withBackendDetails(new RDBMSTableBackendDetails().withTableName(snake(table.getName())));
      table.getFields().values().forEach(field -> field.setBackendName(snake(field.getName())));
      if(table.getFields().containsKey("values"))
      {
         table.getField("values").setBackendName("criteria_values");
      }
      table.withoutCapability(Capability.QUERY_STATS);
   }



   /*******************************************************************************
    ** Translate fixture-only physical identifiers without generating the oracle.
    ******************************************************************************/
   private String snake(String value)
   {
      return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
   }



   /*******************************************************************************
    ** JDBC readback does not pass through QQQ or generate statistics recursively.
    ******************************************************************************/
   protected List<List<String>> rows(String sql) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Statement statement = oracle.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int i = 1; i <= result.getMetaData().getColumnCount(); i++)
            {
               row.add(result.getString(i));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Bounded worker termination is checked before database and context cleanup.
    ******************************************************************************/
   protected void awaitWorkersStopped() throws InterruptedException
   {
      for(Thread thread : Thread.getAllStackTraces().keySet())
      {
         if(thread.getName().startsWith("QueryStatManager-"))
         {
            thread.join(3000);
            assertFalse(thread.isAlive(), "Statistics worker survived stop: " + thread.getName());
         }
      }
   }



   /*******************************************************************************
    ** Copy consumer-visible values before the manager mutates storage entities.
    ******************************************************************************/
   protected record Snapshot(String table, String backendAction, String session, Integer count,
                           Instant start, Instant first, Integer millis, String sql, String action, String filter, Set<String> joins)
   {
      /*******************************************************************************
       ** Capture values synchronously at the public consumer boundary.
       ******************************************************************************/
      Snapshot(QueryStat stat)
      {
         this(stat.getTableName(), stat.getBackendAction(), stat.getSessionId(), stat.getRecordCount(),
            stat.getStartTimestamp(), stat.getFirstResultTimestamp(), stat.getFirstResultMillis(), stat.getQueryText(), stat.getAction(),
            JsonUtils.toJson(stat.getQueryFilter()),
            stat.getJoinTableNames() == null ? Set.of() : Set.copyOf(stat.getJoinTableNames()));
      }
   }
}
