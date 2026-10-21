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
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager.getJdbcUrl;


/*******************************************************************************
 ** C3P0 connection pool for an RDBMS backend, configured from the backend's
 ** ConnectionPoolSettings.
 **
 ** Checkout waits are bounded: without a checkoutTimeoutSeconds setting the
 ** wait is DEFAULT_CHECKOUT_TIMEOUT_SECONDS (C3P0's own default is to wait
 ** forever), and a timed-out checkout throws an SQLException naming the backend
 ** and the pool's state. Connections returned to the pool are reset by C3P0
 ** (see BaseC3P0ConnectionCustomizer).
 *******************************************************************************/
public class C3P0PooledConnectionProvider implements ConnectionProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(C3P0PooledConnectionProvider.class);

   public static final int DEFAULT_CHECKOUT_TIMEOUT_SECONDS = 30;

   private RDBMSBackendMetaData  backend;
   private ComboPooledDataSource connectionPool;

   private final AtomicLong usageCount         = new AtomicLong();
   private final AtomicLong checkoutTimeouts   = new AtomicLong();
   private volatile long    lastLeakScanMillis = 0;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void init(RDBMSBackendMetaData backend) throws QException
   {
      this.backend = backend;

      ComboPooledDataSource pool = null;
      try
      {
         pool = new ComboPooledDataSource();
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

         ////////////////////////////////////////////////////////////////////////////
         // bounded checkout wait: C3P0's default (0) waits forever for a free one //
         ////////////////////////////////////////////////////////////////////////////
         if(poolSettings == null || poolSettings.getCheckoutTimeoutSeconds() == null)
         {
            pool.setCheckoutTimeout(DEFAULT_CHECKOUT_TIMEOUT_SECONDS * 1000);
         }

         ///////////////////////////////////////////////////////////////////////////
         // hard reclaim of connections held too long (off unless configured), with //
         // the stack trace of the checkout logged when one is reclaimed            //
         ///////////////////////////////////////////////////////////////////////////
         if(poolSettings != null && poolSettings.getUnreturnedConnectionTimeoutSeconds() != null && poolSettings.getUnreturnedConnectionTimeoutSeconds() > 0)
         {
            pool.setUnreturnedConnectionTimeout(poolSettings.getUnreturnedConnectionTimeoutSeconds());
            pool.setDebugUnreturnedConnectionStackTraces(true);
         }

         ///////////////////////////////////////////////////////////////////////////
         // the customizer runs queries for new connections and tracks checkouts  //
         // for leak detection (detect-and-log only; nothing is reclaimed)        //
         // use C3P0's unique pool identity; backend names repeat across apps     //
         ///////////////////////////////////////////////////////////////////////////
         if(CollectionUtils.nullSafeHasContents(backend.getQueriesForNewConnections()))
         {
            BaseC3P0ConnectionCustomizer.setQueriesForNewConnections(pool.getIdentityToken(), backend.getQueriesForNewConnections());
         }
         Integer leakSeconds = poolSettings == null ? null : poolSettings.getLeakDetectionThresholdSeconds();
         BaseC3P0ConnectionCustomizer.setLeakDetectionThreshold(pool.getIdentityToken(), leakSeconds == null ? null : Duration.ofSeconds(leakSeconds));
         pool.setConnectionCustomizerClassName(BaseC3P0ConnectionCustomizer.class.getName());

         customizePool(pool);

         this.connectionPool = pool;
      }
      catch(Exception e)
      {
         if(pool != null)
         {
            try
            {
               pool.close();
            }
            catch(RuntimeException closeError)
            {
               e.addSuppressed(closeError);
            }
            finally
            {
               BaseC3P0ConnectionCustomizer.forget(pool.getIdentityToken());
            }
         }
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
    ** Check a connection out of the pool. When none frees up within the checkout
    ** timeout, throws an SQLException that names the backend and the pool state.
    *******************************************************************************/
   @Override
   public Connection getConnection() throws SQLException
   {
      usageCount.incrementAndGet();
      scanForLeaksAtMostEverySecond();
      try
      {
         return (this.connectionPool.getConnection());
      }
      catch(SQLException e)
      {
         if(isCheckoutTimeout(e))
         {
            checkoutTimeouts.incrementAndGet();
            String message = "Timed out after " + connectionPool.getCheckoutTimeout() + "ms waiting for a pooled connection to backend [" + backend.getName() + "]: "
               + "pool exhausted (" + describeState() + "). Raise maxPoolSize, or look for long transactions or leaked connections. (" + e.getMessage() + ")";
            LOG.warn(message, logPair("backendName", backend.getName()));
            throw (new SQLException(message, "08001", e));
         }
         throw (e);
      }
   }



   /*******************************************************************************
    ** C3P0 reports a checkout timeout as an SQLException caused by its resource
    ** pool's TimeoutException.
    *******************************************************************************/
   private static boolean isCheckoutTimeout(SQLException e)
   {
      for(Throwable t = e; t != null; t = t.getCause())
      {
         if(t instanceof com.mchange.v2.resourcepool.TimeoutException)
         {
            return (true);
         }
      }
      return (e.getMessage() != null && e.getMessage().contains("checkout a Connection has timed out"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String describeState()
   {
      try
      {
         return ("max " + connectionPool.getMaxPoolSize() + ", busy " + connectionPool.getNumBusyConnectionsDefaultUser() + ", idle " + connectionPool.getNumIdleConnectionsDefaultUser()
            + ", waiting " + connectionPool.getNumThreadsAwaitingCheckoutDefaultUser());
      }
      catch(Exception e)
      {
         return ("max " + connectionPool.getMaxPoolSize() + ", state unavailable");
      }
   }



   /*******************************************************************************
    ** Leak detection runs on checkouts (and from getPoolMetrics), throttled so
    ** a busy pool pays for one scan a second at most.
    *******************************************************************************/
   private void scanForLeaksAtMostEverySecond()
   {
      long now = System.currentTimeMillis();
      if(now - lastLeakScanMillis >= 1000)
      {
         lastLeakScanMillis = now;
         BaseC3P0ConnectionCustomizer.findLeaks(connectionPool.getIdentityToken());
      }
   }



   /*******************************************************************************
    ** Pool gauges: connections total/busy/idle, threads waiting for a checkout,
    ** the max size, checkout timeouts and suspected leaks so far.
    *******************************************************************************/
   @Override
   public Map<String, Number> getPoolMetrics()
   {
      Map<String, Number> metrics = new LinkedHashMap<>();
      try
      {
         metrics.put("connectionsTotal", connectionPool.getNumConnectionsDefaultUser());
         metrics.put("connectionsBusy", connectionPool.getNumBusyConnectionsDefaultUser());
         metrics.put("connectionsIdle", connectionPool.getNumIdleConnectionsDefaultUser());
         metrics.put("threadsAwaitingCheckout", connectionPool.getNumThreadsAwaitingCheckoutDefaultUser());
      }
      catch(SQLException e)
      {
         LOG.debug("Could not read connection pool state", e, logPair("backendName", backend.getName()));
      }
      metrics.put("maxPoolSize", connectionPool.getMaxPoolSize());
      metrics.put("checkoutTimeouts", checkoutTimeouts.get());
      metrics.put("suspectedLeaks", BaseC3P0ConnectionCustomizer.findLeaks(connectionPool.getIdentityToken()).size());
      return (metrics);
   }



   /*******************************************************************************
    ** Close the pool and its connections.
    *******************************************************************************/
   @Override
   public void close()
   {
      if(connectionPool == null)
      {
         return;
      }
      LOG.info("Closing connection pool", logPair("backendName", backend.getName()), logPair("state", describeState()));
      try
      {
         connectionPool.close();
      }
      finally
      {
         BaseC3P0ConnectionCustomizer.forget(connectionPool.getIdentityToken());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public JSONObject dumpDebug() throws SQLException
   {
      JSONObject rs = new JSONObject(new LinkedHashMap<>());
      rs.put("poolIdentityToken", connectionPool.getIdentityToken());

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
      state.put("numUsages", usageCount.get());
      state.put("numCheckoutTimeouts", checkoutTimeouts.get());
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
