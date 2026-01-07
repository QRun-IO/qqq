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

package com.kingsrook.qqq.backend.module.postgres;


import java.sql.Connection;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.postgresql.PostgreSQLContainer;


/*******************************************************************************
 ** Base test class for PostgreSQL module tests.
 ** 
 ** This class provides common test infrastructure including:
 ** - PostgreSQL test container lifecycle management
 ** - QContext initialization and cleanup
 ** - Test database setup and teardown
 ** - Utility methods for test execution
 *******************************************************************************/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest
{
   protected PostgreSQLContainer postgres;



   /*******************************************************************************
    ** Initializes and starts the PostgreSQL test container before all tests.
    ** 
    ** The container is configured with:
    ** - Database name: qqq_test
    ** - Username/password: test/test
    ** - 256MB shared memory
    ** - 128MB shared buffers
    ** - 200 max connections
    *******************************************************************************/
   @BeforeAll
   void baseBeforeAll()
   {
      postgres = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_test")
         .withUsername("test")
         .withPassword("test")
         .withSharedMemorySize(256 * 1024 * 1024L) // 256MB shared memory
         .withCommand("postgres", "-c", "shared_buffers=128MB", "-c", "max_connections=200");
      postgres.start();
      
      QLogger.getLogger(BaseTest.class).info("Started PostgreSQL container for test class: " + this.getClass().getName() + " on port: " + postgres.getFirstMappedPort());
   }



   /*******************************************************************************
    ** Stops the PostgreSQL test container and cleans up resources after all tests.
    ** 
    ** This includes clearing QContext, resetting connection pools, and stopping
    ** the container to prevent resource leaks.
    *******************************************************************************/
   @AfterAll
   void baseAfterAll()
   {
      if(postgres != null && postgres.isRunning())
      {
         // Clear QContext first
         QContext.clear();
         
         // Reset connection pools to prevent stale connections to old container
         ConnectionManager.resetConnectionProviders();
         
         postgres.stop();
         QLogger.getLogger(BaseTest.class).info("Stopped PostgreSQL container for test class: " + this.getClass().getName());
      }
   }



   /*******************************************************************************
    ** Sets up the test environment before each test method.
    ** 
    ** This includes:
    ** - Setting the PostgreSQL container in TestUtils
    ** - Initializing QContext with test instance and session
    ** - Priming the test database with schema and data
    ** 
    ** @throws Exception if setup fails
    *******************************************************************************/
   @BeforeEach
   void baseBeforeEach() throws Exception
   {
      TestUtils.setPostgresContainer(postgres);
      QContext.init(TestUtils.defineInstance(), new QSession());
      TestUtils.primeTestDatabase("prime-test-database.sql");
   }



   /*******************************************************************************
    ** Cleans up the test environment after each test method.
    ** 
    ** This includes:
    ** - Resetting action strategy settings (page size, statistics)
    ** - Clearing QContext to prevent state leakage between tests
    *******************************************************************************/
   @AfterEach
   void baseAfterEach()
   {
      BaseRDBMSActionStrategy actionStrategy = getBaseRDBMSActionStrategy();
      actionStrategy.setPageSize(BaseRDBMSActionStrategy.DEFAULT_PAGE_SIZE);
      actionStrategy.resetStatistics();
      actionStrategy.setCollectStatistics(false);

      QContext.clear();
   }



   /***************************************************************************
    ** Retrieves the RDBMS action strategy from the default backend.
    ** 
    ** @return the action strategy instance for the test backend
    ***************************************************************************/
   protected BaseRDBMSActionStrategy getBaseRDBMSActionStrategy()
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      BaseRDBMSActionStrategy actionStrategy = (BaseRDBMSActionStrategy) backend.getActionStrategy();
      return actionStrategy;
   }



   /***************************************************************************
    ** Retrieves the RDBMS action strategy and enables statistics collection.
    ** 
    ** This method is useful for tests that need to verify query execution counts
    ** or other performance metrics.
    ** 
    ** @return the action strategy instance with statistics collection enabled
    ***************************************************************************/
   protected BaseRDBMSActionStrategy getBaseRDBMSActionStrategyAndActivateCollectingStatistics()
   {
      BaseRDBMSActionStrategy actionStrategy = getBaseRDBMSActionStrategy();
      actionStrategy.setCollectStatistics(true);
      actionStrategy.resetStatistics();
      return actionStrategy;
   }



   /*******************************************************************************
    ** Reinitializes QContext with a new QInstance.
    ** 
    ** This is useful for tests that need to modify the metadata configuration
    ** and have those changes take effect.
    ** 
    ** @param qInstance the new QInstance to initialize in context
    *******************************************************************************/
   protected void reInitInstanceInContext(QInstance qInstance)
   {
      if(qInstance.equals(QContext.getQInstance()))
      {
         QLogger.getLogger(BaseTest.class).warn("Unexpected condition - the same qInstance that is already in the QContext was passed into reInit.  You probably want a new QInstance object instance.");
      }
      QContext.init(qInstance, new QSession());
   }



   /*******************************************************************************
    ** Executes a SQL statement directly against the test database.
    ** 
    ** This is useful for setting up specific test conditions or verifying
    ** database state that isn't easily accessible through the QQQ API.
    ** 
    ** @param sql the SQL statement to execute
    ** @param resultSetProcessor optional processor for handling result sets
    ** @throws Exception if SQL execution fails
    *******************************************************************************/
   protected void runTestSql(String sql, QueryManager.ResultSetProcessor resultSetProcessor) throws Exception
   {
      ConnectionManager connectionManager = new ConnectionManager();
      Connection        connection        = connectionManager.getConnection(TestUtils.defineBackend());
      QueryManager.executeStatement(connection, sql, resultSetProcessor);
      connection.close();
   }
}
