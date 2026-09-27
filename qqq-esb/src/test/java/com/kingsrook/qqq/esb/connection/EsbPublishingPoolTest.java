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


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.publish.EsbPublisher;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.core.postoffice.RoutingStatus;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerMessagePlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.core.transaction.TransactionOperationAbstract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Native broker evidence for publishing-session reuse and transaction isolation.
 *******************************************************************************/
class EsbPublishingPoolTest extends EsbTestBase
{
   /*******************************************************************************
    ** Release all owned clients even when an assertion fails.
    *******************************************************************************/
   @AfterEach
   void closeClients() throws Exception
   {
      EsbConnectionManager.getInstance().closeAll();
      startEmbeddedBroker();
   }



   /*******************************************************************************
    ** Distinct public publish calls must reuse a broker-observed session while
    ** committing each call, and never reusing its producer or consumer session.
    *******************************************************************************/
   @Test
   void repeatedPublicationsReuseOneSession() throws Exception
   {
      String destination = "pool-" + UUID.randomUUID();
      EsbInstanceMetaData.of(QContext.getQInstance()).withDestination(new QEsbDestinationMetaData()
         .withName(destination).withProviderName(PROVIDER_NAME).withType(EsbDestinationType.QUEUE));
      Set<String> sendingSessions = ConcurrentHashMap.newKeySet();
      Set<ServerSession> brokerSessions = ConcurrentHashMap.newKeySet();
      ActiveMQServerMessagePlugin recorder = new ActiveMQServerMessagePlugin()
      {
         @Override
         public void afterSend(ServerSession session, Transaction tx, org.apache.activemq.artemis.api.core.Message message, boolean direct, boolean noAutoCreateQueue, RoutingStatus result)
         {
            if(destination.equals(message.getAddress()))
            {
               sendingSessions.add(session.getName());
               brokerSessions.add(session);
            }
         }
      };
      getEmbeddedBrokerServer().registerBrokerPlugin(recorder);
      try(Session consumerSession = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false);
         MessageConsumer consumer = consumerSession.createConsumer(consumerSession.createQueue(destination)))
      {
         for(int i = 0; i < 12; i++)
         {
            EsbEvent event = new EsbEvent().withId("event-" + i).withType("pool.test").withSource("qqq://pool/test");
            assertThat(EsbPublisher.getInstance().publish(destination, List.of(event)).getSuccess()).isTrue();
            Message message = consumer.receive(5000);
            assertThat(message).isNotNull();
            assertThat(message.getJMSDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
            assertThat(message.getStringProperty("ce_id")).isEqualTo("event-" + i);
         }
         assertThat(consumer.receive(100)).isNull();
         assertThat(sendingSessions).hasSize(1);
         assertThat(brokerSessions).allSatisfy(session -> assertThat(session.getProducerCount()).isZero());
      }
      finally
      {
         getEmbeddedBrokerServer().unRegisterBrokerPlugin(recorder);
      }
   }



   /*******************************************************************************
    ** Exhaustion follows the existing public failure/output contract and returns
    ** capacity after release, without preventing independent consumer sessions.
    *******************************************************************************/
   @Test
   void exhaustionReportsFailureAndReleaseRestoresPublishing() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();
      String destination = "capacity-" + UUID.randomUUID();
      EsbInstanceMetaData.of(QContext.getQInstance()).withDestination(new QEsbDestinationMetaData()
         .withName(destination).withProviderName(PROVIDER_NAME).withType(EsbDestinationType.QUEUE));
      List<EsbConnectionManager.PublishingSessionLease> held = new ArrayList<>();
      try
      {
         for(int i = 0; i < 8; i++)
         {
            held.add(manager.borrowPublishingSession(PROVIDER_NAME));
         }
         var output = EsbPublisher.getInstance().publish(destination, List.of(event("rejected")));
         assertThat(output.getSuccess()).isFalse();
         assertThat(output.getSent()).isZero();
         assertThat(output.getError()).contains("exhausted");
         assertThat(EsbStats.getInstance().destination(destination).publishFailures()).isEqualTo(1);
         try(Session consumer = manager.openSession(PROVIDER_NAME, false))
         {
            assertThat(consumer.getTransacted()).isFalse();
         }
      }
      finally
      {
         held.forEach(EsbConnectionManager.PublishingSessionLease::close);
      }
      assertThat(EsbPublisher.getInstance().publish(destination, List.of(event("restored"))).getSuccess()).isTrue();
      assertThat(EsbStats.getInstance().destination(destination).published()).isEqualTo(1);
   }



   /*******************************************************************************
    ** A broker rejecting the second send cannot leak the first uncommitted event
    ** into a later publication on a reused session.
    *******************************************************************************/
   @Test
   void brokerSendFailureDiscardsBatchAndSession() throws Exception
   {
      assertBrokerFailureDoesNotContaminateNextPublication(false);
   }



   /*******************************************************************************
    ** A native transaction failure must discard the lease just like a send error.
    *******************************************************************************/
   @Test
   void brokerCommitFailureDiscardsBatchAndSession() throws Exception
   {
      assertBrokerFailureDoesNotContaminateNextPublication(true);
   }



   /*******************************************************************************
    ** Native restart destroys the old generation, even when a successful lease
    ** returns late. Consumer sessions still remain independently owned.
    *******************************************************************************/
   @Test
   void nativeReconnectAndManagerResetFenceOutstandingLeases() throws Exception
   {
      EsbConnectionManager manager = EsbConnectionManager.getInstance();
      CountDownLatch reconnected = new CountDownLatch(1);
      manager.addConnectionListener(PROVIDER_NAME, reconnected::countDown);
      var late = manager.borrowPublishingSession(PROVIDER_NAME);
      Session old = late.getSession();
      late.commit();
      try(var idle = manager.borrowPublishingSession(PROVIDER_NAME))
      {
         idle.commit();
      }
      stopEmbeddedBroker();
      startEmbeddedBroker();
      assertThat(reconnected.await(20, TimeUnit.SECONDS)).isTrue();
      late.close();
      try(var fresh = manager.borrowPublishingSession(PROVIDER_NAME))
      {
         assertThat(fresh.getSession()).isNotSameAs(old);
         fresh.commit();
      }
      var outstanding = manager.borrowPublishingSession(PROVIDER_NAME);
      Session beforeReset = outstanding.getSession();
      manager.closeAll();
      assertThatThrownBy(outstanding::commit).isInstanceOf(JMSException.class);
      outstanding.close();
      assertThatThrownBy(() -> beforeReset.createTextMessage("closed")).isInstanceOf(JMSException.class);
      try(var afterReset = manager.borrowPublishingSession(PROVIDER_NAME))
      {
         assertThat(afterReset.getSession()).isNotSameAs(beforeReset);
         afterReset.commit();
      }
   }



   /*******************************************************************************
    ** Inject failures at real broker boundaries; independently receive the queue
    ** contents and record native session identities, not manager implementation.
    *******************************************************************************/
   private void assertBrokerFailureDoesNotContaminateNextPublication(boolean failCommit) throws Exception
   {
      String destination = "failed-pool-" + UUID.randomUUID();
      EsbInstanceMetaData.of(QContext.getQInstance()).withDestination(new QEsbDestinationMetaData()
         .withName(destination).withProviderName(PROVIDER_NAME).withType(EsbDestinationType.QUEUE));
      Set<String> sendingSessions = ConcurrentHashMap.newKeySet();
      AtomicBoolean armed = new AtomicBoolean(true);
      AtomicBoolean failureReached = new AtomicBoolean();
      ActiveMQServerMessagePlugin fault = new ActiveMQServerMessagePlugin()
      {
         @Override
         public void beforeSend(ServerSession session, Transaction tx, org.apache.activemq.artemis.api.core.Message message, boolean direct, boolean noAutoCreateQueue) throws ActiveMQException
         {
            if(!destination.equals(message.getAddress()))
            {
               return;
            }
            sendingSessions.add(session.getName());
            if(failCommit && armed.compareAndSet(true, false))
            {
               tx.addOperation(new TransactionOperationAbstract()
               {
                  @Override
                  public void beforeCommit(Transaction transaction) throws Exception
                  {
                     failureReached.set(true);
                     throw (new ActiveMQException("owned commit rejection"));
                  }
               });
            }
            else if(!failCommit && "reject".equals(message.getStringProperty("ce_id")) && armed.compareAndSet(true, false))
            {
               failureReached.set(true);
               ActiveMQException rejection = new ActiveMQException("owned send rejection");
               tx.markAsRollbackOnly(rejection);
               throw (rejection);
            }
         }
      };
      getEmbeddedBrokerServer().registerBrokerPlugin(fault);
      try(Session receiver = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false);
         MessageConsumer consumer = receiver.createConsumer(receiver.createQueue(destination)))
      {
         var failed = EsbPublisher.getInstance().publish(destination, List.of(event("first"), event("reject")));
         assertThat(failureReached).isTrue();
         assertThat(failed.getSuccess()).isFalse();
         assertThat(failed.getSent()).isZero();
         assertThat(consumer.receive(100)).isNull();
         assertThat(EsbPublisher.getInstance().publish(destination, List.of(event("healthy"))).getSuccess()).isTrue();
         Message message = consumer.receive(5000);
         assertThat(message).isNotNull();
         assertThat(message.getStringProperty("ce_id")).isEqualTo("healthy");
         assertThat(consumer.receive(100)).isNull();
         assertThat(sendingSessions).hasSize(2);
      }
      finally
      {
         getEmbeddedBrokerServer().unRegisterBrokerPlugin(fault);
      }
   }



   /*******************************************************************************
    ** A valid, individually identifiable event for native transaction assertions.
    *******************************************************************************/
   private EsbEvent event(String id)
   {
      return (new EsbEvent().withId(id).withType("pool.test").withSource("qqq://pool/test"));
   }
}
