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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Class to manage access to JDBC Connections.
 **
 ** Relies heavily on RDBMSBackendMetaData.
 *******************************************************************************/
public class ConnectionManager
{
   private static final QLogger LOG = QLogger.getLogger(ConnectionManager.class);

   private static final Map<String, ConnectionProviderInterface> standaloneProviders = new ConcurrentHashMap<>();
   private static final Map<QInstance, Map<String, ConnectionProviderInterface>> instanceProviders = new WeakHashMap<>();
   private static final Map<ConnectionProviderInterface, PoolPressureEvaluator> poolEvaluators = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Connection getConnection(RDBMSBackendMetaData backend) throws SQLException
   {
      try
      {
         ConnectionProviderInterface connectionProvider = getConnectionProvider(backend);
         return connectionProvider.getConnection();
      }
      catch(QException qe)
      {
         throw (new SQLException("Error getting connection", qe));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ConnectionProviderInterface getConnectionProvider(RDBMSBackendMetaData backend) throws QException
   {
      Map<String, ConnectionProviderInterface> connectionProviderMap = providers(QContext.getQInstance());
      String name = Objects.requireNonNullElse(backend.getName(), "");
      if(!StringUtils.hasContent(name))
      {
         throw (new QException("RDBMSBackendMetaData is missing a name"));
      }

      if(!connectionProviderMap.containsKey(name))
      {
         synchronized(connectionProviderMap)
         {
            if(!connectionProviderMap.containsKey(name))
            {
               QCodeReference connectionProviderReference = backend.getConnectionProvider();
               boolean        usingDefaultSimpleProvider  = false;
               if(connectionProviderReference == null)
               {
                  connectionProviderReference = new QCodeReference(SimpleConnectionProvider.class);
                  usingDefaultSimpleProvider = true;
               }

               LOG.info("Initializing connection provider for RDBMS backend", logPair("backendName", name), logPair("connectionProvider", connectionProviderReference.getName()), logPair("usingDefaultSimpleProvider", usingDefaultSimpleProvider));
               ConnectionProviderInterface connectionProvider = QCodeLoader.getAdHoc(ConnectionProviderInterface.class, connectionProviderReference);
               try
               {
                  connectionProvider.init(backend);
               }
               catch(QException | RuntimeException e)
               {
                  try
                  {
                     connectionProvider.close();
                  }
                  catch(RuntimeException closeError)
                  {
                     e.addSuppressed(closeError);
                  }
                  throw e;
               }

               connectionProviderMap.put(name, connectionProvider);
            }
         }
      }

      return (connectionProviderMap.get(name));
   }



   /*******************************************************************************
    ** An application's registry never falls back to the standalone registry.
    *******************************************************************************/
   private static Map<String, ConnectionProviderInterface> providers(QInstance instance)
   {
      if(instance == null)
      {
         return standaloneProviders;
      }
      synchronized(instanceProviders)
      {
         return instanceProviders.computeIfAbsent(instance, ignored -> new ConcurrentHashMap<>());
      }
   }



   /*******************************************************************************
    ** Explicit ownership for health probes and hosts outside the request context.
    *******************************************************************************/
   public static Connection getConnection(QInstance instance, RDBMSBackendMetaData backend) throws SQLException
   {
      Connection[] connection = new Connection[1];
      QContext.withTemporaryContext(new CapturedContext(instance, null), () -> connection[0] = getConnection(backend));
      return connection[0];
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static JSONArray dumpConnectionProviderDebug()
   {
      return dumpConnectionProviderDebug(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Inspect only the explicitly selected application's providers.
    *******************************************************************************/
   public static JSONArray dumpConnectionProviderDebug(QInstance instance)
   {
      try
      {
         JSONArray rs = new JSONArray();
         for(Map.Entry<String, ConnectionProviderInterface> entry : providers(instance).entrySet())
         {
            JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
            jsonObject.put("backendName", entry.getKey());
            jsonObject.put("connectionProviderClass", entry.getValue().getClass().getName());
            jsonObject.put("values", entry.getValue().dumpDebug());
            rs.put(jsonObject);
         }

         return (rs);
      }
      catch(Exception e)
      {
         String message = "Error dumping debug data for connection providers";
         LOG.warn(message, e);
         return (new JSONArray(new JSONObject(Map.of("error", e.getMessage()))));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getJdbcDriverClassName(RDBMSBackendMetaData backend)
   {
      if(StringUtils.hasContent(backend.getJdbcDriverClassName()))
      {
         return backend.getJdbcDriverClassName();
      }

      return switch(backend.getVendor())
      {
         case RDBMSBackendMetaData.VENDOR_MYSQL, RDBMSBackendMetaData.VENDOR_AURORA_MYSQL -> "com.mysql.cj.jdbc.Driver";
         case RDBMSBackendMetaData.VENDOR_H2 -> "org.h2.Driver";
         default -> throw (new IllegalStateException("We do not know what jdbc driver to use for vendor name [" + backend.getVendor() + "].  Try setting jdbcDriverClassName in your backend meta data."));
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getJdbcUrl(RDBMSBackendMetaData backend)
   {
      if(StringUtils.hasContent(backend.getJdbcUrl()))
      {
         return backend.getJdbcUrl();
      }

      return switch(backend.getVendor())
      {
         ////////////////////////////////////////////////////////////////
         // TODO aws-mysql-jdbc driver not working when running on AWS //
         ////////////////////////////////////////////////////////////////
         // jdbcURL = "jdbc:mysql:aws://" + backend.getHostName() + ":" + backend.getPort() + "/" + backend.getDatabaseName() + "?rewriteBatchedStatements=true&zeroDateTimeBehavior=CONVERT_TO_NULL";

         case RDBMSBackendMetaData.VENDOR_AURORA_MYSQL -> "jdbc:mysql://" + backend.getHostName() + ":" + backend.getPort() + "/" + backend.getDatabaseName() + "?rewriteBatchedStatements=true&zeroDateTimeBehavior=convertToNull&useSSL=false";
         case RDBMSBackendMetaData.VENDOR_MYSQL -> "jdbc:mysql://" + backend.getHostName() + ":" + backend.getPort() + "/" + backend.getDatabaseName() + "?rewriteBatchedStatements=true&zeroDateTimeBehavior=convertToNull";
         case RDBMSBackendMetaData.VENDOR_H2 -> "jdbc:h2:" + backend.getHostName() + ":" + backend.getDatabaseName() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
         default ->
         {
            String connectionString = backend.buildConnectionString();
            if(connectionString == null)
            {
               throw new IllegalArgumentException("Unsupported rdbms backend vendor: " + backend.getVendor() + " (and null returned by backendMetaData.buildConnectionString())");
            }
            else
            {
               yield (connectionString);
            }
         }
      };
   }



   /*******************************************************************************
    ** Pool gauges for a backend's connection provider, or an empty map when its
    ** provider has no pool or has not been initialized yet (never initializes it).
    *******************************************************************************/
   public static Map<String, Number> getPoolMetrics(RDBMSBackendMetaData backend)
   {
      return getPoolMetrics(QContext.getQInstance(), backend);
   }



   /*******************************************************************************
    ** Read owner-specific metrics without initializing a provider.
    *******************************************************************************/
   public static Map<String, Number> getPoolMetrics(QInstance instance, RDBMSBackendMetaData backend)
   {
      ConnectionProviderInterface connectionProvider = backend == null || backend.getName() == null ? null : providers(instance).get(backend.getName());
      return (connectionProvider == null ? Map.of() : connectionProvider.getPoolMetrics());
   }



   /*******************************************************************************
    ** Close the current application's providers (or standalone providers without a context) and
    ** forget them. Call at the end of an application's shutdown, after the work
    ** that uses the database (requests, jobs, the scheduler) has stopped; a later
    ** getConnection initializes a new provider.
    *******************************************************************************/
   public static void closeConnectionProviders()
   {
      closeConnectionProviders(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Close an owner's pools after its work stops, even from a foreign context.
    *******************************************************************************/
   public static void closeConnectionProviders(QInstance instance)
   {
      closeProviders(providers(instance));
   }



   /*******************************************************************************
    ** Close before forgetting providers; other applications remain untouched.
    *******************************************************************************/
   private static void closeProviders(Map<String, ConnectionProviderInterface> connectionProviderMap)
   {
      synchronized(connectionProviderMap)
      {
         for(Map.Entry<String, ConnectionProviderInterface> entry : connectionProviderMap.entrySet())
         {
            try
            {
               entry.getValue().close();
            }
            catch(Exception e)
            {
               LOG.warn("Error closing connection provider", e, logPair("backendName", entry.getKey()));
            }
         }
         connectionProviderMap.clear();
      }
   }



   /*******************************************************************************
    ** reset the map of connection providers - not necessarily meant to be useful
    ** in production code - written for use in qqq tests.
    *******************************************************************************/
   public static void resetConnectionProviders()
   {
      ArrayList<Map<String, ConnectionProviderInterface>> registries;
      synchronized(instanceProviders)
      {
         registries = new ArrayList<>(instanceProviders.values());
         instanceProviders.clear();
      }
      registries.add(standaloneProviders);
      registries.forEach(ConnectionManager::closeProviders);
   }



   /*******************************************************************************
    ** History follows the provider lifetime, not just its reusable backend name.
    *******************************************************************************/
   public static com.kingsrook.qqq.backend.core.health.HealthStatus getPoolPressureStatus(RDBMSBackendMetaData backend, Map<String, Number> metrics)
   {
      return getPoolPressureStatus(QContext.getQInstance(), backend, metrics);
   }



   /*******************************************************************************
    ** Pressure history follows the selected application's provider lifetime.
    *******************************************************************************/
   public static com.kingsrook.qqq.backend.core.health.HealthStatus getPoolPressureStatus(QInstance instance, RDBMSBackendMetaData backend, Map<String, Number> metrics)
   {
      ConnectionProviderInterface provider = providers(instance).get(backend.getName());
      if(provider == null)
      {
         return (com.kingsrook.qqq.backend.core.health.HealthStatus.UNKNOWN);
      }
      return (poolEvaluators.computeIfAbsent(provider, ignored -> new PoolPressureEvaluator()).evaluate(metrics, System.nanoTime()));
   }
}
