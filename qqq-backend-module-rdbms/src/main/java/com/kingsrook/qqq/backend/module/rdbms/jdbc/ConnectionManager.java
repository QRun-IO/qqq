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


import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.ConnectionPoolSettings;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Class to manage access to JDBC Connections.
 **
 ** Relies heavily on RDBMSBackendMetaData.
 **
 ** Connection providers (which may be pools) are cached - but not by backend name
 ** alone, as two QInstances (or two configurations) can use the same name for
 ** different databases.  A provider is found by:
 ** - scope:  the QInstance in the QContext of the calling thread.  Each QInstance
 **   has its own providers (closed if the QInstance is garbage collected).  A
 **   thread with no QInstance in its context uses the shared "standalone" scope.
 ** - then, within the scope, the backend's name plus a fingerprint of its
 **   connection configuration (vendor, host, url, driver, credentials, provider
 **   class, queries for new connections, pool settings).  If any of those differ,
 **   it's a different provider - so the first provider isn't re-used for the new
 **   configuration, nor is it closed out from under its users.
 **
 ** The fingerprint is a one-way hash, salted with a random value that's unique to
 ** the JVM - so it never exposes (and can't be used to guess) a credential, and
 ** isn't ever logged or put in the debug dump.  Credentials aren't kept anywhere
 ** that they weren't already - the cache holds the provider (which holds the
 ** backend), as before, until a provider is closed or reset.
 *******************************************************************************/
public class ConnectionManager
{
   private static final QLogger LOG = QLogger.getLogger(ConnectionManager.class);
   private static final Map<ConnectionProviderInterface, PoolPressureEvaluator> poolEvaluators = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

   ///////////////////////////////////////////////////////////////////////////////////////
   // SCOPES_LOCK guards ONLY the (weak, non-thread-safe) instanceScopes map - for a     //
   // get or put, never for a provider init, close or debug dump.  The usual checkout   //
   // doesn't take it either: a thread remembers the scope it last used (see            //
   // THREAD_SCOPE).  Provider init is serialized per cache key, on the entry itself.   //
   ///////////////////////////////////////////////////////////////////////////////////////
   private static final Object SCOPES_LOCK = new Object();

   private static final Map<QInstance, Scope> instanceScopes   = new WeakHashMap<>();
   private static final Scope                 standaloneScope = new Scope();

   private static final ThreadLocal<ThreadScope> THREAD_SCOPE = new ThreadLocal<>();

   private static final Cleaner CLEANER = Cleaner.create();

   private static final byte[] FINGERPRINT_SALT = new byte[16];

   static
   {
      new SecureRandom().nextBytes(FINGERPRINT_SALT);
   }



   /*******************************************************************************
    ** The providers of one scope (a QInstance, or the standalone scope).  Never
    ** refers to its QInstance.  Retired when closed/reset, so that a thread that
    ** remembered it doesn't keep using it.
    *******************************************************************************/
   private static final class Scope
   {
      private final Map<String, ProviderEntry> entries = new ConcurrentHashMap<>();
      private volatile boolean retired = false;
   }



   /*******************************************************************************
    ** What a thread remembers of the scope it last used: weakly, the QInstance (so
    ** a thread never keeps one alive), and its scope.
    *******************************************************************************/
   private record ThreadScope(WeakReference<QInstance> instance, Scope scope)
   {
   }



   /*******************************************************************************
    ** A cached provider - created empty (cheaply) and initialized lazily, once,
    ** under its own monitor - so a slow init blocks only callers that want this
    ** very provider.  The backend name is non-secret, for debug.
    **
    ** A closed entry has been claimed (removed from its scope) by a closer or by
    ** a failed init, and is never used again.
    *******************************************************************************/
   private static final class ProviderEntry
   {
      private final String backendName;

      private volatile ConnectionProviderInterface provider;
      private boolean closed = false;



      /*******************************************************************************
       **
       *******************************************************************************/
      private ProviderEntry(String backendName)
      {
         this.backendName = backendName;
      }



      /*******************************************************************************
       ** The initialized provider, initializing it if this is the first call.
       ** Returns null if the entry was closed (the caller should look it up again).
       *******************************************************************************/
      private ConnectionProviderInterface getOrInit(RDBMSBackendMetaData backend) throws QException
      {
         ConnectionProviderInterface existing = provider;
         if(existing != null)
         {
            return (existing);
         }

         synchronized(this)
         {
            if(closed)
            {
               return (null);
            }

            if(provider == null)
            {
               QCodeReference connectionProviderReference = backend.getConnectionProvider();
               boolean        usingDefaultSimpleProvider  = false;
               if(connectionProviderReference == null)
               {
                  connectionProviderReference = new QCodeReference(SimpleConnectionProvider.class);
                  usingDefaultSimpleProvider = true;
               }

               LOG.info("Initializing connection provider for RDBMS backend", logPair("backendName", backendName), logPair("connectionProvider", connectionProviderReference.getName()), logPair("usingDefaultSimpleProvider", usingDefaultSimpleProvider));
               ConnectionProviderInterface connectionProvider = QCodeLoader.getAdHoc(ConnectionProviderInterface.class, connectionProviderReference);
               try
               {
                  connectionProvider.init(backend);
               }
               catch(QException | RuntimeException e)
               {
                  ///////////////////////////////////////////////////////////////////////
                  // a partly-initialized provider is never published - release it now //
                  ///////////////////////////////////////////////////////////////////////
                  try
                  {
                     connectionProvider.close();
                  }
                  catch(RuntimeException closeError)
                  {
                     e.addSuppressed(closeError);
                  }
                  throw (e);
               }
               provider = connectionProvider;
            }

            return (provider);
         }
      }



      /*******************************************************************************
       ** Mark closed - and get the provider (if one was initialized), to close it.
       ** Waits for an init in progress.
       *******************************************************************************/
      private synchronized ConnectionProviderInterface markClosed()
      {
         closed = true;
         return (provider);
      }
   }



   /*******************************************************************************
    ** Closes the providers of a QInstance's scope, when the QInstance is garbage
    ** collected.  (Must not refer to the QInstance - only to its scope.)
    *******************************************************************************/
   private record ScopeCloser(Scope scope) implements Runnable
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run()
      {
         closeAll(scope);
      }
   }



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
      String name = Objects.requireNonNullElse(backend.getName(), "");
      if(!StringUtils.hasContent(name))
      {
         throw (new QException("RDBMSBackendMetaData is missing a name"));
      }

      String key = name + "#" + getConnectionFingerprint(backend);

      while(true)
      {
         Scope         scope = getScope(true);
         ProviderEntry entry = scope.entries.computeIfAbsent(key, k -> new ProviderEntry(name));

         ///////////////////////////////////////////////////////////////////////////////////
         // a closer retires its scope BEFORE it sweeps the entries - so an entry that we //
         // put into a retired scope may have been missed by the sweep.  Claim it back    //
         // (if no one else has), and go around to a fresh scope.                         //
         ///////////////////////////////////////////////////////////////////////////////////
         if(scope.retired)
         {
            if(scope.entries.remove(key, entry))
            {
               closeEntry(entry);
            }
            continue;
         }

         ConnectionProviderInterface connectionProvider;
         try
         {
            connectionProvider = entry.getOrInit(backend);
         }
         catch(QException | RuntimeException e)
         {
            ///////////////////////////////////////////////////////////////////////////
            // only a provider that initialized is cached - so a failure is retried by //
            // the next call, instead of being remembered.  (mark the entry closed so   //
            // that callers waiting on it look again, rather than init into a dead one) //
            ///////////////////////////////////////////////////////////////////////////
            entry.markClosed();
            scope.entries.remove(key, entry);
            throw (e);
         }

         if(connectionProvider != null)
         {
            return (connectionProvider);
         }

         ////////////////////////////////////////////////////////////////////////////
         // the entry was closed (by a closer, or a failed init) while we waited -  //
         // go around, and find (or create) the current one                         //
         ////////////////////////////////////////////////////////////////////////////
      }
   }



   /*******************************************************************************
    ** Get the providers for the QInstance in the context (or the standalone scope,
    ** if there's no QInstance).  If there isn't one yet, make it (when create is
    ** true), or return null.
    **
    ** The usual call takes no lock: a thread remembers the scope that it last
    ** used (and the QInstance weakly), unless that scope has since been retired.
    *******************************************************************************/
   private static Scope getScope(boolean create)
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null)
      {
         return (standaloneScope);
      }

      ThreadScope threadScope = THREAD_SCOPE.get();
      if(threadScope != null && threadScope.instance().get() == qInstance && !threadScope.scope().retired)
      {
         return (threadScope.scope());
      }

      Scope scope;
      synchronized(SCOPES_LOCK)
      {
         scope = instanceScopes.get(qInstance);
         if(scope == null && create)
         {
            scope = new Scope();
            instanceScopes.put(qInstance, scope);
            CLEANER.register(qInstance, new ScopeCloser(scope));
         }
      }

      if(scope != null)
      {
         THREAD_SCOPE.set(new ThreadScope(new WeakReference<>(qInstance), scope));
      }
      return (scope);
   }



   /*******************************************************************************
    ** One-way, salted fingerprint of everything in a backend that decides which
    ** database a connection goes to, and how - see the class comment.
    *******************************************************************************/
   private static String getConnectionFingerprint(RDBMSBackendMetaData backend) throws QException
   {
      try
      {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         digest.update(FINGERPRINT_SALT);

         List<Object> parts = new ArrayList<>();
         parts.add(backend.getClass().getName());
         parts.add(backend.getVendor());
         parts.add(backend.getHostName());
         parts.add(backend.getPort());
         parts.add(backend.getDatabaseName());
         parts.add(backend.getUsername());
         parts.add(backend.getPassword());
         parts.add(backend.getJdbcUrl());
         parts.add(backend.getJdbcDriverClassName());

         ///////////////////////////////////////////////////////////////////////////////////
         // the url as the providers will really use it (which covers vendor-built urls, //
         // and backends that build their own connection string)                         //
         ///////////////////////////////////////////////////////////////////////////////////
         try
         {
            parts.add(getJdbcUrl(backend));
         }
         catch(RuntimeException e)
         {
            parts.add(null);
         }

         QCodeReference connectionProvider = backend.getConnectionProvider();
         parts.add(connectionProvider == null ? null : connectionProvider.getName());
         parts.add(connectionProvider == null ? null : connectionProvider.getCodeType());
         parts.add(connectionProvider == null ? null : connectionProvider.getInlineCode());

         List<String> queriesForNewConnections = backend.getQueriesForNewConnections();
         parts.add(queriesForNewConnections == null ? null : queriesForNewConnections.size());
         if(queriesForNewConnections != null)
         {
            parts.addAll(queriesForNewConnections);
         }

         ConnectionPoolSettings poolSettings = backend.getConnectionPoolSettings();
         parts.add(poolSettings == null ? null : "poolSettings");
         if(poolSettings != null)
         {
            parts.add(poolSettings.getInitialPoolSize());
            parts.add(poolSettings.getMinPoolSize());
            parts.add(poolSettings.getMaxPoolSize());
            parts.add(poolSettings.getAcquireIncrement());
            parts.add(poolSettings.getMaxConnectionAgeSeconds());
            parts.add(poolSettings.getMaxIdleTimeSeconds());
            parts.add(poolSettings.getMaxIdleTimeExcessConnectionsSeconds());
            parts.add(poolSettings.getCheckoutTimeoutSeconds());
            parts.add(poolSettings.getTestConnectionOnCheckout());
            parts.add(poolSettings.getLeakDetectionThresholdSeconds());
            parts.add(poolSettings.getUnreturnedConnectionTimeoutSeconds());
         }

         for(Object part : parts)
         {
            //////////////////////////////////////////////////////////////////////////////
            // length-prefixed, so that ("ab", "c") and ("a", "bc") are not the same    //
            //////////////////////////////////////////////////////////////////////////////
            byte[] bytes = part == null ? new byte[0] : String.valueOf(part).getBytes(StandardCharsets.UTF_8);
            digest.update((byte) (part == null ? 0 : 1));
            digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(bytes);
         }

         return (HexFormat.of().formatHex(digest.digest()));
      }
      catch(NoSuchAlgorithmException e)
      {
         throw (new QException("Error fingerprinting RDBMS backend connection configuration", e));
      }
   }



   /*******************************************************************************
    ** Retire a scope (unless it's the standalone one, which lives forever), then
    ** close every provider in it and forget them.  Each entry is CLAIMED by an
    ** atomic remove(key, entry) - so whoever wins (this, the Cleaner, a concurrent
    ** close, a racing caller) is the only one to close it: never twice.
    *******************************************************************************/
   private static void closeAll(Scope scope)
   {
      if(scope != standaloneScope)
      {
         scope.retired = true;
      }

      for(Map.Entry<String, ProviderEntry> mapEntry : new ArrayList<>(scope.entries.entrySet()))
      {
         if(scope.entries.remove(mapEntry.getKey(), mapEntry.getValue()))
         {
            closeEntry(mapEntry.getValue());
         }
      }
   }



   /*******************************************************************************
    ** Close an entry that the caller has claimed (removed from its scope).
    *******************************************************************************/
   private static void closeEntry(ProviderEntry entry)
   {
      ConnectionProviderInterface provider = entry.markClosed();
      if(provider != null)
      {
         try
         {
            provider.close();
         }
         catch(Exception e)
         {
            LOG.warn("Error closing connection provider", e, logPair("backendName", entry.backendName));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static JSONArray dumpConnectionProviderDebug()
   {
      try
      {
         JSONArray rs = new JSONArray();

         //////////////////////////////////////////////////////////////////////////////
         // only the (quick) snapshot of the scopes is under the lock - the providers //
         // are asked for their debug values outside of it                           //
         //////////////////////////////////////////////////////////////////////////////
         List<Scope> instanceScopeList;
         synchronized(SCOPES_LOCK)
         {
            instanceScopeList = new ArrayList<>(instanceScopes.values());
         }

         for(ProviderEntry entry : standaloneScope.entries.values())
         {
            dumpEntry(rs, entry, "standalone");
         }

         for(Scope scope : instanceScopeList)
         {
            for(ProviderEntry entry : scope.entries.values())
            {
               dumpEntry(rs, entry, "instance");
            }
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
    ** The debug dump of one provider - which names its backend, class and scope,
    ** and whatever the provider itself reports; but never the cache key.
    *******************************************************************************/
   private static void dumpEntry(JSONArray rs, ProviderEntry entry, String scope) throws SQLException
   {
      ConnectionProviderInterface provider = entry.provider;
      if(provider == null)
      {
         /////////////////////////////////////////////////////////////
         // still initializing - nothing to report (and don't wait) //
         /////////////////////////////////////////////////////////////
         return;
      }

      JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
      jsonObject.put("backendName", entry.backendName);
      jsonObject.put("scope", scope);
      jsonObject.put("connectionProviderClass", provider.getClass().getName());
      jsonObject.put("values", provider.dumpDebug());
      rs.put(jsonObject);
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
      if(backend == null || backend.getName() == null)
      {
         return (Map.of());
      }

      try
      {
         Scope         scope    = getScope(false);
         ProviderEntry entry    = scope == null ? null : scope.entries.get(backend.getName() + "#" + getConnectionFingerprint(backend));
         ConnectionProviderInterface provider = entry == null ? null : entry.provider;
         return (provider == null ? Map.of() : provider.getPoolMetrics());
      }
      catch(QException e)
      {
         LOG.warn("Error looking up connection provider for pool metrics", e, logPair("backendName", backend.getName()));
         return (Map.of());
      }
   }



   /*******************************************************************************
    ** History follows the provider lifetime, not just its reusable backend name.
    ** Looks the provider up in the current instance scope; never initializes one.
    *******************************************************************************/
   public static com.kingsrook.qqq.backend.core.health.HealthStatus getPoolPressureStatus(RDBMSBackendMetaData backend, Map<String, Number> metrics)
   {
      if(backend == null || backend.getName() == null)
      {
         return (com.kingsrook.qqq.backend.core.health.HealthStatus.UNKNOWN);
      }

      try
      {
         Scope         scope    = getScope(false);
         ProviderEntry entry    = scope == null ? null : scope.entries.get(backend.getName() + "#" + getConnectionFingerprint(backend));
         ConnectionProviderInterface provider = entry == null ? null : entry.provider;
         if(provider == null)
         {
            return (com.kingsrook.qqq.backend.core.health.HealthStatus.UNKNOWN);
         }
         return (poolEvaluators.computeIfAbsent(provider, ignored -> new PoolPressureEvaluator()).evaluate(metrics, System.nanoTime()));
      }
      catch(QException e)
      {
         LOG.warn("Error looking up connection provider for pool pressure", e, logPair("backendName", backend.getName()));
         return (com.kingsrook.qqq.backend.core.health.HealthStatus.UNKNOWN);
      }
   }



   /*******************************************************************************
    ** Close every connection provider (pools release their connections) and
    ** forget them. Call at the end of an application's shutdown, after the work
    ** that uses the database (requests, jobs, the scheduler) has stopped; a later
    ** getConnection initializes a new provider.
    *******************************************************************************/
   public static void closeConnectionProviders()
   {
      ///////////////////////////////////////////////////////////////////////////
      // take the scopes out under the lock; close them (slowly) outside of it //
      ///////////////////////////////////////////////////////////////////////////
      List<Scope> scopes;
      synchronized(SCOPES_LOCK)
      {
         scopes = new ArrayList<>(instanceScopes.values());
         instanceScopes.clear();
      }

      for(Scope scope : scopes)
      {
         closeAll(scope);
      }
      closeAll(standaloneScope);
   }



   /*******************************************************************************
    ** reset the connection providers of every scope - closing what is forgotten, so
    ** that no pool outlives the reset - not necessarily meant to be useful in
    ** production code - written for use in qqq tests.
    *******************************************************************************/
   public static void resetConnectionProviders()
   {
      List<Scope> scopes;
      synchronized(SCOPES_LOCK)
      {
         scopes = new ArrayList<>(instanceScopes.values());
         instanceScopes.clear();
      }

      for(Scope scope : scopes)
      {
         closeAll(scope);
      }
      closeAll(standaloneScope);
   }



   /*******************************************************************************
    ** Run a lookup in the scope of an explicitly selected QInstance (or the
    ** standalone scope, for null), regardless of the calling thread's context.
    *******************************************************************************/
   private static <T> T inScopeOf(QInstance instance, java.util.function.Supplier<T> supplier)
   {
      Object[] result = new Object[1];
      QContext.withTemporaryContext(new CapturedContext(instance, null), () -> result[0] = supplier.get());
      @SuppressWarnings("unchecked")
      T typed = (T) result[0];
      return (typed);
   }



   /*******************************************************************************
    ** Explicit ownership for health probes and hosts outside the request context.
    *******************************************************************************/
   public static Connection getConnection(QInstance instance, RDBMSBackendMetaData backend) throws SQLException
   {
      Connection[] connection = new Connection[1];
      QContext.withTemporaryContext(new CapturedContext(instance, null), () -> connection[0] = getConnection(backend));
      return (connection[0]);
   }



   /*******************************************************************************
    ** Inspect only the explicitly selected application's providers.
    *******************************************************************************/
   public static JSONArray dumpConnectionProviderDebug(QInstance instance)
   {
      try
      {
         JSONArray rs    = new JSONArray();
         Scope     scope = inScopeOf(instance, () -> getScope(false));
         if(scope != null)
         {
            for(ProviderEntry entry : scope.entries.values())
            {
               dumpEntry(rs, entry, instance == null ? "standalone" : "instance");
            }
         }
         return (rs);
      }
      catch(Exception e)
      {
         String message = "Error dumping debug data for connection providers";
         LOG.warn(message, e);
         return (new JSONArray(new JSONObject(Map.of("error", String.valueOf(e.getMessage())))));
      }
   }



   /*******************************************************************************
    ** Read an explicitly selected application's metrics without initializing a provider.
    *******************************************************************************/
   public static Map<String, Number> getPoolMetrics(QInstance instance, RDBMSBackendMetaData backend)
   {
      return (inScopeOf(instance, () -> getPoolMetrics(backend)));
   }



   /*******************************************************************************
    ** Pressure history follows the selected application's provider lifetime.
    *******************************************************************************/
   public static com.kingsrook.qqq.backend.core.health.HealthStatus getPoolPressureStatus(QInstance instance, RDBMSBackendMetaData backend, Map<String, Number> metrics)
   {
      return (inScopeOf(instance, () -> getPoolPressureStatus(backend, metrics)));
   }



   /*******************************************************************************
    ** Close an owner's pools after its work stops, even from a foreign context.
    *******************************************************************************/
   public static void closeConnectionProviders(QInstance instance)
   {
      ///////////////////////////////////////////////////////////////////////////////
      // only the selected owner's scope - the other applications' pools stay open //
      ///////////////////////////////////////////////////////////////////////////////
      if(instance == null)
      {
         closeAll(standaloneScope);
         return;
      }

      Scope scope;
      synchronized(SCOPES_LOCK)
      {
         scope = instanceScopes.remove(instance);
      }

      if(scope != null)
      {
         closeAll(scope);
      }
   }

}
