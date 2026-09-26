/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager.getJdbcUrl;


/*******************************************************************************
 **
 *******************************************************************************/
public class C3P0PooledConnectionProvider implements ConnectionProviderInterface
{
   private RDBMSBackendMetaData  backend;
   private ComboPooledDataSource connectionPool;

   private long usageCount = 0;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void init(RDBMSBackendMetaData backend) throws QException
   {
      this.backend = backend;

      try
      {
         ComboPooledDataSource pool = new ComboPooledDataSource();
         pool.setDriverClass(ConnectionManager.getJdbcDriverClassName(backend));
         pool.setJdbcUrl(getJdbcUrl(backend));
         pool.setUser(backend.getUsername());
         pool.setPassword(backend.getPassword());

         ConnectionPoolSettings poolSettings = backend.getConnectionPoolSettings();
         if(poolSettings != null)
         {
            if(poolSettings.getInitialPoolSize() != null)
            {
               pool.setInitialPoolSize(poolSettings.getInitialPoolSize());
            }

            if(poolSettings.getMinPoolSize() != null)
            {
               pool.setMinPoolSize(poolSettings.getMinPoolSize());
            }

            if(poolSettings.getMaxPoolSize() != null)
            {
               pool.setMaxPoolSize(poolSettings.getMaxPoolSize());
            }

            if(poolSettings.getAcquireIncrement() != null)
            {
               pool.setAcquireIncrement(poolSettings.getAcquireIncrement());
            }

            if(poolSettings.getMaxConnectionAgeSeconds() != null)
            {
               pool.setMaxConnectionAge(poolSettings.getMaxConnectionAgeSeconds());
            }

            if(poolSettings.getMaxIdleTimeSeconds() != null)
            {
               pool.setMaxIdleTime(poolSettings.getMaxIdleTimeSeconds());
            }

            if(poolSettings.getMaxIdleTimeExcessConnectionsSeconds() != null)
            {
               pool.setMaxIdleTimeExcessConnections(poolSettings.getMaxIdleTimeExcessConnectionsSeconds());
            }

            if(poolSettings.getCheckoutTimeoutSeconds() != null)
            {
               pool.setCheckoutTimeout(poolSettings.getCheckoutTimeoutSeconds() * 1000);
            }

            if(poolSettings.getTestConnectionOnCheckout() != null)
            {
               pool.setTestConnectionOnCheckout(poolSettings.getTestConnectionOnCheckout());
            }
         }

         pool.setIdentityToken(backend.getName());

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the backend specifies queries to run for new connections, then set up a connection customizer to run them //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(CollectionUtils.nullSafeHasContents(backend.getQueriesForNewConnections()))
         {
            BaseC3P0ConnectionCustomizer.setQueriesForNewConnections(backend.getName(), backend.getQueriesForNewConnections());
            pool.setConnectionCustomizerClassName(BaseC3P0ConnectionCustomizer.class.getName());
         }

         customizePool(pool);

         this.connectionPool = pool;
      }
      catch(Exception e)
      {
         throw (new QException("Error Initializing C3P0PooledConnectionProvider for backend [" + backend.getName() + "]", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void customizePool(ComboPooledDataSource pool)
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Connection getConnection() throws SQLException
   {
      usageCount++;
      return (this.connectionPool.getConnection());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public JSONObject dumpDebug() throws SQLException
   {
      JSONObject rs = new JSONObject(new LinkedHashMap<>());

      JSONObject settings = new JSONObject(new LinkedHashMap<>());
      rs.put("settings", settings);
      settings.put("initialPoolSize", connectionPool.getInitialPoolSize());
      settings.put("minPoolSize", connectionPool.getMinPoolSize());
      settings.put("maxPoolSize", connectionPool.getMaxPoolSize());
      settings.put("acquireIncrement", connectionPool.getAcquireIncrement());
      settings.put("maxConnectionAge", connectionPool.getMaxConnectionAge());
      settings.put("maxIdleTime", connectionPool.getMaxIdleTime());
      settings.put("maxIdleTimeExcessConnections", connectionPool.getMaxIdleTimeExcessConnections());
      settings.put("checkoutTimeout", connectionPool.getCheckoutTimeout());
      settings.put("testConnectionOnCheckout", connectionPool.isTestConnectionOnCheckout());

      JSONObject state = new JSONObject(new LinkedHashMap<>());
      rs.put("state", state);
      state.put("numUsages", usageCount);
      state.put("numConnections", connectionPool.getNumConnections());
      state.put("numBusyConnections", connectionPool.getNumBusyConnections());
      state.put("numIdleConnections", connectionPool.getNumIdleConnections());
      state.put("numFailedCheckins", connectionPool.getNumFailedCheckinsDefaultUser());
      state.put("numFailedCheckouts", connectionPool.getNumFailedCheckoutsDefaultUser());
      state.put("numFailedIdleTests", connectionPool.getNumFailedIdleTestsDefaultUser());
      state.put("numThreadsAwaitingCheckout", connectionPool.getNumThreadsAwaitingCheckoutDefaultUser());
      return (rs);
   }

}
