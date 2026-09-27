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

package com.kingsrook.qqq.esb.connection;


import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Deterministic JMS-boundary tests: lease exclusivity, failures and generations.
 ** The fake driver rejects unexpected calls rather than silently returning null.
 *******************************************************************************/
class EsbPublishingLeaseTest
{
   private final List<DriverConnection> connections = new CopyOnWriteArrayList<>();
   private final CountDownLatch reconnected = new CountDownLatch(1);
   private final ConnectionFactory factory = proxy(ConnectionFactory.class, (method, args) ->
   {
      if(method.equals("createConnection"))
      {
         DriverConnection connection = new DriverConnection();
         connections.add(connection);
         return (connection.jms);
      }
      throw (new UnsupportedOperationException(method));
   });
   private boolean failConfigure;
   private boolean loseDuringConfigure;
   private final EsbConnectionFactoryBuilder builder = new EsbConnectionFactoryBuilder()
   {
      @Override
      public ConnectionFactory buildConnectionFactory(com.kingsrook.qqq.esb.model.QEsbProviderMetaData provider)
      {
         return (factory);
      }

      @Override
      public void configureSession(Session session) throws JMSException
      {
         if(failConfigure)
         {
            throw (new JMSException("owned configure failure"));
         }
         if(loseDuringConfigure)
         {
            connections.getLast().listener.onException(new JMSException("owned connection loss during configuration"));
         }
      }
   };
   private final EsbConnectionManager.ProviderConnection provider = new EsbConnectionManager.ProviderConnection("owned", builder, factory, reconnected::countDown);



   /*******************************************************************************
    ** Always stop owned reconnect workers and close fake sessions.
    *******************************************************************************/
   @AfterEach
   void closeProvider()
   {
      provider.close();
   }



   /*******************************************************************************
    ** A failed commit cannot be retried into a reusable session, even if a driver
    ** would accept that second commit. The next publication must get a fresh one.
    *******************************************************************************/
   @Test
   void failedCommitPermanentlyPoisonsLease() throws Exception
   {
      EsbConnectionManager.PublishingSessionLease lease = provider.borrowPublishingSession();
      Session failed = lease.getSession();
      DriverSession driver = connections.getFirst().sessions.getFirst();
      driver.failCommit = true;
      assertThatThrownBy(lease::commit).isInstanceOf(JMSException.class).hasMessageContaining("owned commit failure");
      driver.failCommit = false;
      assertThatThrownBy(lease::commit).isInstanceOf(IllegalStateException.class);
      lease.close();
      assertThat(driver.rollbacks).isEqualTo(1);
      assertThat(driver.closes).isEqualTo(1);
      try(var replacement = provider.borrowPublishingSession())
      {
         assertThat(replacement.getSession()).isNotSameAs(failed);
         replacement.commit();
      }
   }



   /*******************************************************************************
    ** Sequential reuse, idempotent close, consumer ownership and provider scope.
    *******************************************************************************/
   @Test
   void successfulLeaseReusesSessionButConsumersAndProvidersDoNot() throws Exception
   {
      Session first;
      var lease = provider.borrowPublishingSession();
      first = lease.getSession();
      lease.commit();
      lease.close();
      lease.close();
      assertThatThrownBy(lease::getSession).isInstanceOf(IllegalStateException.class);
      try(var next = provider.borrowPublishingSession(); Session consumer = provider.openSession(false))
      {
         assertThat(next.getSession()).isSameAs(first).isNotSameAs(consumer);
         assertThat(connections.getFirst().sessions.getLast().transacted).isFalse();
         next.commit();
      }
      EsbConnectionManager.ProviderConnection other = new EsbConnectionManager.ProviderConnection("other", builder, factory, () -> {});
      try(var separate = other.borrowPublishingSession())
      {
         assertThat(separate.getSession()).isNotSameAs(first);
         separate.commit();
      }
      finally
      {
         other.close();
      }
      assertThat(connections.getFirst().sessions.getFirst().commits).isEqualTo(2);
      assertThat(connections.getFirst().sessions.getFirst().closes).isZero();
   }



   /*******************************************************************************
    ** Eight truly concurrent borrowers hold distinct sessions. The ninth fails
    ** while all are held; release restores capacity without growing the pool.
    *******************************************************************************/
   @Test
   void concurrentLeasesAreExclusiveAndCapacityIsBounded() throws Exception
   {
      try(var warm = provider.borrowPublishingSession())
      {
         warm.commit();
      }
      CountDownLatch borrowed = new CountDownLatch(8);
      CountDownLatch release = new CountDownLatch(1);
      Set<Session> active = ConcurrentHashMap.newKeySet();
      List<Future<?>> work = new ArrayList<>();
      try(var executor = Executors.newFixedThreadPool(8))
      {
         try
         {
            for(int i = 0; i < 8; i++)
            {
               work.add(executor.submit(() ->
               {
                  try(var lease = provider.borrowPublishingSession())
                  {
                     assertThat(active.add(lease.getSession())).isTrue();
                     borrowed.countDown();
                     assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                     lease.commit();
                  }
                  return (null);
               }));
            }
            assertThat(borrowed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(active).hasSize(8);
            assertThatThrownBy(provider::borrowPublishingSession).isInstanceOf(QException.class).hasMessageContaining("exhausted");
            assertThat(connections.getFirst().sessions).hasSize(8);
         }
         finally
         {
            release.countDown();
         }
         for(Future<?> future : work)
         {
            future.get(5, TimeUnit.SECONDS);
         }
      }
      try(var next = provider.borrowPublishingSession())
      {
         assertThat(active).contains(next.getSession());
         next.commit();
      }
      assertThat(connections.getFirst().sessions).hasSize(8);
   }



   /*******************************************************************************
    ** Abandonment and rollback failure both discard instead of recycling.
    *******************************************************************************/
   @Test
   void abandonedLeaseDiscardsEvenWhenRollbackFails() throws Exception
   {
      var abandoned = provider.borrowPublishingSession();
      DriverSession driver = connections.getFirst().sessions.getFirst();
      driver.failRollback = true;
      abandoned.close();
      assertThat(driver.rollbacks).isEqualTo(1);
      assertThat(driver.closes).isEqualTo(1);
      try(var next = provider.borrowPublishingSession())
      {
         assertThat(next.getSession()).isNotSameAs(driver.jms);
         next.commit();
      }
   }



   /*******************************************************************************
    ** A failed session close cannot leave unaccounted broker resources behind as
    ** capacity is replenished. Retire the connection that owns that resource.
    *******************************************************************************/
   @Test
   void failedSessionCleanupRetiresItsConnection() throws Exception
   {
      var lease = provider.borrowPublishingSession();
      DriverConnection old = connections.getFirst();
      old.sessions.getFirst().failClose = true;
      lease.close();
      assertThat(reconnected.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(old.closed).isTrue();
      try(var next = provider.borrowPublishingSession())
      {
         assertThat(next.getSession()).isNotSameAs(old.sessions.getFirst().jms);
         next.commit();
      }
   }



   /*******************************************************************************
    ** Connection loss invalidates both idle and borrowed sessions, including a
    ** successful lease whose late close occurs after the replacement connects.
    *******************************************************************************/
   @Test
   void reconnectFencesIdleAndLateSuccessfulReturn() throws Exception
   {
      var late = provider.borrowPublishingSession();
      Session oldBorrowed = late.getSession();
      late.commit();
      Session oldIdle;
      try(var idle = provider.borrowPublishingSession())
      {
         oldIdle = idle.getSession();
         idle.commit();
      }
      DriverConnection oldConnection = connections.getFirst();
      oldConnection.listener.onException(new JMSException("owned loss"));
      assertThatThrownBy(provider::borrowPublishingSession).isInstanceOf(QException.class).hasMessageContaining("reconnecting");
      assertThat(reconnected.await(5, TimeUnit.SECONDS)).isTrue();
      late.close();
      try(var fresh = provider.borrowPublishingSession())
      {
         assertThat(fresh.getSession()).isNotSameAs(oldBorrowed).isNotSameAs(oldIdle);
         fresh.commit();
      }
      assertThat(oldConnection.closed).isTrue();
      assertThat(oldConnection.sessions).allSatisfy(session -> assertThat(session.closed).isTrue());
      assertThat(connections.getLast().sessions).hasSize(1);
   }



   /*******************************************************************************
    ** A commit already inside the driver can fail after a replacement connection
    ** is live. Its subsequent close must not return that failed borrowed resource.
    *******************************************************************************/
   @Test
   void inFlightCommitFailureAfterReconnectCannotReturnOldSession() throws Exception
   {
      var lease = provider.borrowPublishingSession();
      DriverConnection oldConnection = connections.getFirst();
      DriverSession oldSession = oldConnection.sessions.getFirst();
      oldSession.commitEntered = new CountDownLatch(1);
      oldSession.releaseCommit = new CountDownLatch(1);
      oldSession.failCommit = true;
      try(var executor = Executors.newSingleThreadExecutor())
      {
         Future<?> failedPublication = executor.submit(() ->
         {
            try(lease)
            {
               assertThatThrownBy(lease::commit).isInstanceOf(JMSException.class);
            }
         });
         try
         {
            assertThat(oldSession.commitEntered.await(5, TimeUnit.SECONDS)).isTrue();
            oldConnection.listener.onException(new JMSException("owned loss during commit"));
            assertThat(reconnected.await(5, TimeUnit.SECONDS)).isTrue();
         }
         finally
         {
            oldSession.releaseCommit.countDown();
         }
         failedPublication.get(5, TimeUnit.SECONDS);
      }
      try(var replacement = provider.borrowPublishingSession())
      {
         assertThat(replacement.getSession()).isNotSameAs(oldSession.jms);
         replacement.commit();
      }
      assertThat(connections.getLast().sessions).hasSize(1);
      assertThat(oldSession.closed).isTrue();
   }



   /*******************************************************************************
    ** Shutdown fences idle and borrowed sessions; a late commit is forbidden.
    *******************************************************************************/
   @Test
   void closeInvalidatesBorrowedAndIdleSessions() throws Exception
   {
      var borrowed = provider.borrowPublishingSession();
      try(var idle = provider.borrowPublishingSession())
      {
         idle.commit();
      }
      provider.close();
      assertThatThrownBy(borrowed::commit).isInstanceOf(JMSException.class).hasMessageContaining("no longer current");
      borrowed.close();
      assertThatThrownBy(provider::borrowPublishingSession).isInstanceOf(QException.class).hasMessageContaining("closed");
      assertThat(connections.getFirst().sessions).allSatisfy(session -> assertThat(session.closed).isTrue());
   }



   /*******************************************************************************
    ** A session that fails setup consumes no capacity and is closed immediately.
    *******************************************************************************/
   @Test
   void configurationFailureDoesNotLeakPoolCapacity() throws Exception
   {
      failConfigure = true;
      for(int i = 0; i < 10; i++)
      {
         assertThatThrownBy(provider::borrowPublishingSession).isInstanceOf(QException.class).hasMessageContaining("configure");
      }
      assertThat(connections.getFirst().sessions).allSatisfy(session -> assertThat(session.closed).isTrue());
      failConfigure = false;
      try(var lease = provider.borrowPublishingSession())
      {
         lease.commit();
      }
   }



   /*******************************************************************************
    ** Synchronous driver loss during setup cannot return a session of the failed
    ** generation, even before the background reconnect worker has run.
    *******************************************************************************/
   @Test
   void connectionLossDuringSessionCreationRejectsLease() throws Exception
   {
      loseDuringConfigure = true;
      assertThatThrownBy(provider::borrowPublishingSession).isInstanceOf(QException.class).hasMessageContaining("changed");
      assertThat(connections.getFirst().sessions.getFirst().closed).isTrue();
   }



   /*******************************************************************************
    ** Minimal JMS factory/connection/session stand-ins, with identity semantics.
    *******************************************************************************/
   private static <T> T proxy(Class<T> type, Invocation invocation)
   {
      return (type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) ->
      {
         return switch(method.getName())
         {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> type.getSimpleName() + "@" + System.identityHashCode(proxy);
            default -> invocation.call(method.getName(), args);
         };
      })));
   }



   /*******************************************************************************
    ** Driver calls may throw the same checked exceptions as JMS.
    *******************************************************************************/
   private interface Invocation
   {
      /*******************************************************************************
       ** Dispatch one driver call.
       *******************************************************************************/
      Object call(String method, Object[] args) throws Exception;
   }



   /*******************************************************************************
    ** Closing the connection closes all its child sessions, as JMS requires.
    *******************************************************************************/
   private static class DriverConnection
   {
      private final List<DriverSession> sessions = new CopyOnWriteArrayList<>();
      private ExceptionListener listener;
      private boolean closed;
      private final Connection jms = proxy(Connection.class, (method, args) ->
      {
         switch(method)
         {
            case "setExceptionListener" -> listener = (ExceptionListener) args[0];
            case "start" ->
            {
            }
            case "createSession" ->
            {
               DriverSession session = new DriverSession((Boolean) args[0]);
               sessions.add(session);
               return (session.jms);
            }
            case "close" ->
            {
               closed = true;
               sessions.forEach(session -> session.closed = true);
            }
            default -> throw (new UnsupportedOperationException(method));
         }
         return (null);
      });
   }



   /*******************************************************************************
    ** Count cleanup and deliberately permit a second commit after an injected
    ** failure, so the test detects accidental reuse rather than relying on JMS.
    *******************************************************************************/
   private static class DriverSession
   {
      private final boolean transacted;
      private CountDownLatch commitEntered;
      private CountDownLatch releaseCommit;
      private boolean failCommit;
      private boolean failRollback;
      private boolean failClose;
      private boolean closed;
      private int commits;
      private int rollbacks;
      private int closes;
      private final Session jms = proxy(Session.class, (method, args) ->
      {
         switch(method)
         {
            case "commit" ->
            {
               if(commitEntered != null)
               {
                  commitEntered.countDown();
                  assertThat(releaseCommit.await(5, TimeUnit.SECONDS)).isTrue();
               }
               if(failCommit)
               {
                  throw (new JMSException("owned commit failure"));
               }
               commits++;
            }
            case "rollback" ->
            {
               rollbacks++;
               if(failRollback)
               {
                  throw (new JMSException("owned rollback failure"));
               }
            }
            case "close" ->
            {
               closes++;
               if(failClose)
               {
                  throw (new JMSException("owned close failure"));
               }
               closed = true;
            }
            default -> throw (new UnsupportedOperationException(method));
         }
         return (null);
      });



      /*******************************************************************************
       ** Record the transaction mode requested by the manager.
       *******************************************************************************/
      DriverSession(boolean transacted)
      {
         this.transacted = transacted;
      }
   }
}
