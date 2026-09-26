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

package com.kingsrook.qqq.esb;


import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbTestBase - the embedded broker and the test instance.
 *******************************************************************************/
class EsbTestBaseTest extends EsbTestBase
{

   /*******************************************************************************
    ** The broker is running for the test class; stopping it refuses connections;
    ** starting it again reuses the same URL.
    *******************************************************************************/
   @Test
   void testEmbeddedBrokerStartsAndStops() throws Exception
   {
      assertThat(isEmbeddedBrokerRunning()).isTrue();
      assertThat(sendAndReceive("esbTestBaseQueue", "hello")).isEqualTo("hello");

      String urlBeforeRestart = getBrokerUrl();
      stopEmbeddedBroker();
      assertThat(isEmbeddedBrokerRunning()).isFalse();
      assertThatThrownBy(() -> sendAndReceive("esbTestBaseQueue", "nobody home")).isInstanceOf(JMSException.class);

      ///////////////////////////////////////////////////
      // stop is idempotent; start again, on same port //
      ///////////////////////////////////////////////////
      stopEmbeddedBroker();
      startEmbeddedBroker();
      startEmbeddedBroker();
      assertThat(isEmbeddedBrokerRunning()).isTrue();
      assertThat(getBrokerUrl()).isEqualTo(urlBeforeRestart);
      assertThat(sendAndReceive("esbTestBaseQueue", "back again")).isEqualTo("back again");
   }



   /*******************************************************************************
    ** The test instance validates, has the order table, syncOrder process, and an
    ** ESB provider pointing at the embedded broker - and the process runs.
    *******************************************************************************/
   @Test
   void testDefineInstance() throws Exception
   {
      QInstance qInstance = defineInstance();
      new QInstanceValidator().validate(qInstance);

      assertThat(qInstance.getTable(TABLE_NAME_ORDER)).isNotNull();
      assertThat(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER)).isNotNull();
      assertThat(EsbInstanceMetaData.of(qInstance).getProvider(PROVIDER_NAME).getUrl()).isEqualTo(getBrokerUrl());

      QContext.init(qInstance, new QSession());
      new RunProcessAction().execute(new RunProcessInput().withProcessName(PROCESS_NAME_SYNC_ORDER));
      assertThat(SyncOrderStep.getRunCount()).isEqualTo(1);

      SyncOrderStep.reset();
      assertThat(SyncOrderStep.getRunCount()).isZero();
   }



   /*******************************************************************************
    ** Send a text message to a queue on the embedded broker and read it back.
    *******************************************************************************/
   private String sendAndReceive(String queueName, String text) throws JMSException
   {
      ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(getBrokerUrl());
      try(connectionFactory; Connection connection = connectionFactory.createConnection())
      {
         connection.start();
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         Queue   queue   = session.createQueue(queueName);
         session.createProducer(queue).send(session.createTextMessage(text));

         MessageConsumer consumer = session.createConsumer(queue);
         TextMessage     message  = (TextMessage) consumer.receive(5000);
         return (message == null ? null : message.getText());
      }
   }

}
