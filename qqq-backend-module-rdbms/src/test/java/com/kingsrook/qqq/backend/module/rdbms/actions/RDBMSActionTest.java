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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;
import org.junit.jupiter.api.AfterEach;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEachRDBMSActionTest()
   {
      BaseRDBMSActionStrategy actionStrategy = getBaseRDBMSActionStrategy();
      actionStrategy.setPageSize(BaseRDBMSActionStrategy.DEFAULT_PAGE_SIZE);
      actionStrategy.resetStatistics();
      actionStrategy.setCollectStatistics(false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void primeTestDatabase() throws Exception
   {
      TestUtils.primeTestDatabase("prime-test-database.sql");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected static BaseRDBMSActionStrategy getBaseRDBMSActionStrategy()
   {
      RDBMSBackendMetaData    backend        = (RDBMSBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      BaseRDBMSActionStrategy actionStrategy = (BaseRDBMSActionStrategy) backend.getActionStrategy();
      return actionStrategy;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected static BaseRDBMSActionStrategy getBaseRDBMSActionStrategyAndActivateCollectingStatistics()
   {
      BaseRDBMSActionStrategy actionStrategy = getBaseRDBMSActionStrategy();
      actionStrategy.setCollectStatistics(true);
      actionStrategy.resetStatistics();
      return actionStrategy;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void runTestSql(String sql, QueryManager.ResultSetProcessor resultSetProcessor) throws Exception
   {
      ConnectionManager connectionManager = new ConnectionManager();
      Connection        connection        = connectionManager.getConnection(TestUtils.defineBackend());
      QueryManager.executeStatement(connection, sql, resultSetProcessor);
      connection.close();
   }
}
