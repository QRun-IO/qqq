/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.mchange.v2.c3p0.ConnectionCustomizer;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Basic version of a C3P0 Connection Customizer used by QQQ - that does things
 ** expected for an RDBMS backend - specifically:
 ** - runs queriesForNewConnections, if they are set.
 ** - tracks checked-out connections for leak detection, when the backend's
 **   pool settings have a leakDetectionThresholdSeconds.
 **
 ** Connection state is reset by C3P0 itself on check-in: uncommitted work is
 ** rolled back (autoCommitOnClose is false) and autoCommit, readOnly,
 ** transaction isolation, catalog and holdability return to their defaults.
 ** Session-level SET statements are not reset; QQQ only uses SET TRANSACTION /
 ** SET LOCAL, which end with the transaction.
 *******************************************************************************/
public class BaseC3P0ConnectionCustomizer implements ConnectionCustomizer
{
   private static final QLogger LOG = QLogger.getLogger(BaseC3P0ConnectionCustomizer.class);

   private static final Map<String, List<String>>              queriesForNewConnections = new ConcurrentHashMap<>();
   private static final Map<String, Duration>                  leakThresholds           = new ConcurrentHashMap<>();
   private static final Map<String, Map<Connection, Checkout>> checkouts                = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** One checked-out connection: when, by which thread, and where (a stack
    ** trace captured at checkout). Its leak is logged once.
    *******************************************************************************/
   public static final class Checkout
   {
      private final Instant   checkedOutAt;
      private final String    threadName;
      private final Throwable checkoutSite;
      private volatile boolean reported = false;



      /*******************************************************************************
       ** Constructor
       *******************************************************************************/
      Checkout(Instant checkedOutAt, String threadName, Throwable checkoutSite)
      {
         this.checkedOutAt = checkedOutAt;
         this.threadName = threadName;
         this.checkoutSite = checkoutSite;
      }



      /*******************************************************************************
       ** Getter for checkedOutAt
       *******************************************************************************/
      public Instant getCheckedOutAt()
      {
         return (checkedOutAt);
      }



      /*******************************************************************************
       ** Getter for threadName
       *******************************************************************************/
      public String getThreadName()
      {
         return (threadName);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void onAcquire(Connection connection, String dataSourceIdentityToken) throws Exception
   {
      try
      {
         List<String> queries = queriesForNewConnections.get(dataSourceIdentityToken);
         if(CollectionUtils.nullSafeHasContents(queries))
         {
            for(String sql : queries)
            {
               try(Statement statement = connection.createStatement())
               {
                  statement.execute(sql);
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Exception on a query-for-new-connection", e);
         throw (e);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void onDestroy(Connection connection, String dataSourceIdentityToken) throws Exception
   {
      untrack(connection, dataSourceIdentityToken);
   }



   /***************************************************************************
    * Track the checkout, only when leak detection is on for this pool.
    ***************************************************************************/
   @Override
   public void onCheckOut(Connection connection, String dataSourceIdentityToken) throws Exception
   {
      if(leakThresholds.containsKey(dataSourceIdentityToken))
      {
         Map<Connection, Checkout> map = checkouts.computeIfAbsent(dataSourceIdentityToken, k -> Collections.synchronizedMap(new IdentityHashMap<>()));
         map.put(connection, new Checkout(Instant.now(), Thread.currentThread().getName(), new Throwable("Connection checked out here")));
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void onCheckIn(Connection connection, String dataSourceIdentityToken) throws Exception
   {
      untrack(connection, dataSourceIdentityToken);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static void untrack(Connection connection, String dataSourceIdentityToken)
   {
      Map<Connection, Checkout> map = checkouts.get(dataSourceIdentityToken);
      if(map != null)
      {
         map.remove(connection);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static void setQueriesForNewConnections(String poolIdentityToken, List<String> queriesForNewConnections)
   {
      BaseC3P0ConnectionCustomizer.queriesForNewConnections.put(poolIdentityToken, queriesForNewConnections);
   }



   /***************************************************************************
    * Turn leak detection on (a positive threshold) or off (null) for a pool.
    ***************************************************************************/
   public static void setLeakDetectionThreshold(String poolIdentityToken, Duration threshold)
   {
      if(threshold == null || threshold.isZero() || threshold.isNegative())
      {
         leakThresholds.remove(poolIdentityToken);
         checkouts.remove(poolIdentityToken);
      }
      else
      {
         leakThresholds.put(poolIdentityToken, threshold);
      }
   }



   /***************************************************************************
    * The pool's connections held longer than its leak threshold. Each one is
    * logged as a warning (with its checkout stack trace) the first time it is
    * found, and stays in the result until it is returned.
    ***************************************************************************/
   public static List<Checkout> findLeaks(String poolIdentityToken)
   {
      Duration                  threshold = leakThresholds.get(poolIdentityToken);
      Map<Connection, Checkout> map       = checkouts.get(poolIdentityToken);
      if(threshold == null || map == null)
      {
         return (List.of());
      }

      Instant        cutoff = Instant.now().minus(threshold);
      List<Checkout> leaks  = new ArrayList<>();
      synchronized(map)
      {
         for(Checkout checkout : map.values())
         {
            if(checkout.checkedOutAt.isBefore(cutoff))
            {
               leaks.add(checkout);
            }
         }
      }

      for(Checkout leak : leaks)
      {
         if(!leak.reported)
         {
            leak.reported = true;
            LOG.warn("Possible connection leak: a pooled connection has been checked out longer than the leak detection threshold", leak.checkoutSite,
               logPair("poolIdentityToken", poolIdentityToken), logPair("thread", leak.threadName), logPair("checkedOutAt", leak.checkedOutAt), logPair("thresholdSeconds", threshold.toSeconds()));
         }
      }
      return (leaks);
   }



   /***************************************************************************
    * Forget a pool's settings and checkouts (when the pool is closed).
    ***************************************************************************/
   static void forget(String poolIdentityToken)
   {
      queriesForNewConnections.remove(poolIdentityToken);
      leakThresholds.remove(poolIdentityToken);
      checkouts.remove(poolIdentityToken);
   }

}
