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

package com.kingsrook.qqq.backend.module.postgres;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** On PostgreSQL, a pooled connection (one-connection pool, so every checkout
 ** is the same server session) carries no state from one checkout to the next
 ** along the paths QQQ uses: transactions closed without commit, SET LOCAL /
 ** set_config(..., true), and SET TRANSACTION isolation / read-only.
 *******************************************************************************/
class PostgreSQLPooledConnectionStateTest extends BaseTest
{
   private RDBMSBackendMetaData backend;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      backend = (RDBMSBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setConnectionPoolSettings(new ConnectionPoolSettings().withInitialPoolSize(1).withMinPoolSize(1).withMaxPoolSize(1).withAcquireIncrement(1).withCheckoutTimeoutSeconds(2));
      ConnectionManager.closeConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      ConnectionManager.closeConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String single(Connection connection, String sql) throws SQLException
   {
      try(Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql))
      {
         resultSet.next();
         return (resultSet.getString(1));
      }
   }



   /*******************************************************************************
    ** A transaction closed without commit is rolled back and autoCommit is back
    ** on for the next checkout of the same server session.
    *******************************************************************************/
   @Test
   void uncommittedTransactionIsRolledBack() throws Exception
   {
      String pid;
      RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(backend));
      pid = single(transaction.getConnection(), "SELECT pg_backend_pid()");
      try(Statement statement = transaction.getConnection().createStatement())
      {
         statement.executeUpdate("INSERT INTO person (first_name, last_name, email) VALUES ('Never', 'Committed', 'never@committed.com')");
      }
      transaction.close();

      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         assertThat(single(connection, "SELECT pg_backend_pid()")).isEqualTo(pid);
         assertThat(connection.getAutoCommit()).isTrue();
         assertThat(single(connection, "SELECT COUNT(*) FROM person WHERE email = 'never@committed.com'")).isEqualTo("0");
      }
   }



   /*******************************************************************************
    ** SET LOCAL and set_config(..., true) end with the transaction.
    *******************************************************************************/
   @Test
   void transactionLocalSettingsDoNotLeak() throws Exception
   {
      String defaultTimeout;
      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         defaultTimeout = single(connection, "SHOW statement_timeout");
         connection.setAutoCommit(false);
         try(Statement statement = connection.createStatement())
         {
            statement.execute("SET LOCAL statement_timeout = 1234");
            statement.execute("SET LOCAL search_path = pg_catalog");
            statement.execute("SELECT set_config('qqq_test.flag', 'on', true)");
            statement.execute("SELECT set_config('TimeZone', 'Asia/Tokyo', true)");
         }
         assertThat(single(connection, "SHOW statement_timeout")).isEqualTo("1234ms");
         connection.commit();
      }

      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         assertThat(single(connection, "SHOW statement_timeout")).isEqualTo(defaultTimeout);
         assertThat(single(connection, "SHOW search_path")).contains("public");
         assertThat(single(connection, "SELECT current_setting('qqq_test.flag', true)")).isNullOrEmpty();
         assertThat(single(connection, "SHOW TimeZone")).isNotEqualTo("Asia/Tokyo");
      }
   }



   /*******************************************************************************
    ** The consistent-read path's SET TRANSACTION (isolation, read only) and the
    ** JDBC read-only flag do not carry over to the next checkout.
    *******************************************************************************/
   @Test
   void readOnlyAndIsolationDoNotLeak() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         connection.setAutoCommit(false);
         try(Statement statement = connection.createStatement())
         {
            statement.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY");
         }
         assertThat(single(connection, "SHOW transaction_read_only")).isEqualTo("on");
         connection.commit();
         connection.setReadOnly(true);
         connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      }

      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         assertThat(connection.isReadOnly()).isFalse();
         assertThat(single(connection, "SHOW transaction_read_only")).isEqualTo("off");
         assertThat(single(connection, "SHOW transaction_isolation")).isEqualTo("read committed");
         try(Statement statement = connection.createStatement())
         {
            assertThat(statement.executeUpdate("INSERT INTO person (first_name, last_name, email) VALUES ('Can', 'Write', 'can@write.com')")).isEqualTo(1);
         }
      }
   }



   /*******************************************************************************
    ** An exhausted pool fails the checkout after the timeout with a clear error.
    *******************************************************************************/
   @Test
   void exhaustedPoolFailsClearly() throws Exception
   {
      try(Connection held = ConnectionManager.getConnection(backend))
      {
         assertThatThrownBy(() -> ConnectionManager.getConnection(backend))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("pool exhausted (max 1, busy 1");
      }
   }

}
