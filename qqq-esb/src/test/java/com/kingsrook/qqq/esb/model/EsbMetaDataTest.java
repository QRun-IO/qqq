/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.esb.model;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.EsbTestBase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for the ESB meta-data model - defaults, derived names, accessors.
 *******************************************************************************/
class EsbMetaDataTest extends EsbTestBase
{

   /*******************************************************************************
    ** Spec section 3 defaults for a trigger.
    *******************************************************************************/
   @Test
   void testTriggerDefaults()
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName("orderEvents");

      assertThat(trigger.getMode()).isEqualTo(EsbTriggerMode.SINGLE);
      assertThat(trigger.getConcurrency()).isEqualTo(1);
      assertThat(trigger.getMaxAttempts()).isEqualTo(3);
      assertThat(trigger.getRetryDelayMs()).isEqualTo(0);
      assertThat(trigger.getRetryMultiplier()).isEqualTo(2.0);
      assertThat(trigger.getRetryMaxDelayMs()).isEqualTo(60_000);
      assertThat(trigger.getOnDeadLetter()).isEqualTo(EsbDeadLetterAction.DEAD_LETTER_QUEUE);
      assertThat(trigger.getStartPaused()).isFalse();
      assertThat(trigger.getTimeoutMs()).isNull();
      assertThat(trigger.getRunAsSessionSupplier()).isNull();
      assertThat(trigger.getSubscriptionName()).isNull();
      assertThat(trigger.getDeadLetterDestinationName()).isNull();

      ////////////////////////////////////////////////////////////////////////////
      // batch fields stay unset (so SINGLE-mode validation can see if they are //
      // set), but have effective defaults of 100 messages and 1000 ms          //
      ////////////////////////////////////////////////////////////////////////////
      assertThat(trigger.getBatchSize()).isNull();
      assertThat(trigger.getBatchWaitMs()).isNull();
      assertThat(trigger.getEffectiveBatchSize()).isEqualTo(100);
      assertThat(trigger.getEffectiveBatchWaitMs()).isEqualTo(1000);

      trigger.withBatchSize(7).withBatchWaitMs(70);
      assertThat(trigger.getEffectiveBatchSize()).isEqualTo(7);
      assertThat(trigger.getEffectiveBatchWaitMs()).isEqualTo(70);
   }



   /*******************************************************************************
    ** Trigger name: <process>.<destination>, using the QQQ destination name.
    ** Default subscription name: <process>.<broker-side destination name>.
    *******************************************************************************/
   @Test
   void testTriggerNames()
   {
      QEsbDestinationMetaData orderEvents = new QEsbDestinationMetaData().withName("orderEvents").withType(EsbDestinationType.TOPIC).withProviderName(PROVIDER_NAME);
      QEsbDestinationMetaData renamed     = new QEsbDestinationMetaData().withName("orderEventsV2").withType(EsbDestinationType.TOPIC).withProviderName(PROVIDER_NAME).withDestinationName("prod.order.events");
      QInstance               qInstance   = defineInstance();
      EsbInstanceMetaData.of(qInstance).withDestination(orderEvents).withDestination(renamed);
      QContext.init(qInstance, new QSession());

      EsbTrigger trigger = new EsbTrigger().withDestinationName("orderEvents");
      assertThat(trigger.getName("syncOrder")).isEqualTo("syncOrder.orderEvents");
      assertThat(trigger.getEffectiveSubscriptionName("syncOrder")).isEqualTo("syncOrder.orderEvents");
      assertThat(trigger.getEffectiveSubscriptionName("syncOrder", orderEvents)).isEqualTo("syncOrder.orderEvents");

      /////////////////////////////////////////////////////////////////////
      // with a broker-side name, the trigger's name keeps the QQQ name, //
      // but the subscription (a queue on the broker) uses the broker's  //
      /////////////////////////////////////////////////////////////////////
      EsbTrigger renamedTrigger = new EsbTrigger().withDestinationName("orderEventsV2");
      assertThat(renamedTrigger.getName("syncOrder")).isEqualTo("syncOrder.orderEventsV2");
      assertThat(renamedTrigger.getEffectiveSubscriptionName("syncOrder")).isEqualTo("syncOrder.prod.order.events");
      assertThat(renamedTrigger.getEffectiveSubscriptionName("syncOrder", renamed)).isEqualTo("syncOrder.prod.order.events");

      trigger.withSubscriptionName("customSubscription");
      assertThat(trigger.getName("syncOrder")).isEqualTo("syncOrder.orderEvents");
      assertThat(trigger.getEffectiveSubscriptionName("syncOrder")).isEqualTo("customSubscription");
      assertThat(trigger.getEffectiveSubscriptionName("syncOrder", orderEvents)).isEqualTo("customSubscription");

      ///////////////////////////////////////////////////////////////////////
      // without an explicit name, the default needs the destination: the  //
      // one-arg version reads it from the QContext's instance, and fails  //
      // loudly if it isn't there                                          //
      ///////////////////////////////////////////////////////////////////////
      EsbTrigger unknownTrigger = new EsbTrigger().withDestinationName("noSuchDestination");
      assertThatThrownBy(() -> unknownTrigger.getEffectiveSubscriptionName("syncOrder"))
         .isInstanceOf(QRuntimeException.class)
         .hasMessageContaining("noSuchDestination");
      assertThatThrownBy(() -> unknownTrigger.getEffectiveSubscriptionName("syncOrder", null))
         .isInstanceOf(NullPointerException.class);

      ////////////////////////////////////////////////////////////////////
      // an explicit subscription name needs no lookup (nor a QContext) //
      ////////////////////////////////////////////////////////////////////
      QContext.clear();
      assertThat(trigger.getEffectiveSubscriptionName("syncOrder")).isEqualTo("customSubscription");
      assertThatThrownBy(() -> renamedTrigger.getEffectiveSubscriptionName("syncOrder"))
         .isInstanceOf(QRuntimeException.class)
         .hasMessageContaining("orderEventsV2");
   }



   /*******************************************************************************
    ** Dead-letter destination defaults, from the broker-side destination name:
    ** <destination>.dlq for queues, and <destination>.<subscription>.dlq for
    ** topics.
    *******************************************************************************/
   @Test
   void testDeadLetterDestinationNames()
   {
      QEsbDestinationMetaData orderEvents = new QEsbDestinationMetaData().withName("orderEvents").withType(EsbDestinationType.TOPIC).withProviderName(PROVIDER_NAME);
      QEsbDestinationMetaData orderQueue  = new QEsbDestinationMetaData().withName("orderQueue").withType(EsbDestinationType.QUEUE).withProviderName(PROVIDER_NAME);
      QEsbDestinationMetaData v1Queue     = new QEsbDestinationMetaData().withName("orderQueueV1").withType(EsbDestinationType.QUEUE).withProviderName(PROVIDER_NAME).withDestinationName("orders.v1");
      QEsbDestinationMetaData v1Topic     = new QEsbDestinationMetaData().withName("orderEventsV1").withType(EsbDestinationType.TOPIC).withProviderName(PROVIDER_NAME).withDestinationName("order.events.v1");
      QInstance               qInstance   = defineInstance();
      EsbInstanceMetaData.of(qInstance).withDestination(orderEvents).withDestination(orderQueue).withDestination(v1Queue).withDestination(v1Topic);
      QContext.init(qInstance, new QSession());

      EsbTrigger queueTrigger = new EsbTrigger().withDestinationName("orderQueue");
      assertThat(queueTrigger.getEffectiveDeadLetterDestinationName("syncOrder")).isEqualTo("orderQueue.dlq");
      assertThat(queueTrigger.getEffectiveDeadLetterDestinationName("syncOrder", orderQueue)).isEqualTo("orderQueue.dlq");

      EsbTrigger topicTrigger = new EsbTrigger().withDestinationName("orderEvents");
      assertThat(topicTrigger.getEffectiveDeadLetterDestinationName("syncOrder")).isEqualTo("orderEvents.syncOrder.orderEvents.dlq");
      assertThat(topicTrigger.getEffectiveDeadLetterDestinationName("syncOrder", orderEvents)).isEqualTo("orderEvents.syncOrder.orderEvents.dlq");

      /////////////////////////////////////////////////////////////////////
      // with broker-side names, the defaults are built from those names //
      /////////////////////////////////////////////////////////////////////
      EsbTrigger v1QueueTrigger = new EsbTrigger().withDestinationName("orderQueueV1");
      assertThat(v1QueueTrigger.getEffectiveDeadLetterDestinationName("syncOrder")).isEqualTo("orders.v1.dlq");
      assertThat(v1QueueTrigger.getEffectiveDeadLetterDestinationName("syncOrder", v1Queue)).isEqualTo("orders.v1.dlq");

      EsbTrigger v1TopicTrigger = new EsbTrigger().withDestinationName("orderEventsV1");
      assertThat(v1TopicTrigger.getEffectiveDeadLetterDestinationName("syncOrder")).isEqualTo("order.events.v1.syncOrder.order.events.v1.dlq");
      assertThat(v1TopicTrigger.getEffectiveDeadLetterDestinationName("syncOrder", v1Topic)).isEqualTo("order.events.v1.syncOrder.order.events.v1.dlq");

      topicTrigger.withSubscriptionName("mySubscription");
      assertThat(topicTrigger.getEffectiveDeadLetterDestinationName("syncOrder")).isEqualTo("orderEvents.mySubscription.dlq");
      v1TopicTrigger.withSubscriptionName("mySubscription");
      assertThat(v1TopicTrigger.getEffectiveDeadLetterDestinationName("syncOrder")).isEqualTo("order.events.v1.mySubscription.dlq");

      topicTrigger.withDeadLetterDestinationName("customDeadLetters");
      assertThat(topicTrigger.getEffectiveDeadLetterDestinationName("syncOrder")).isEqualTo("customDeadLetters");
      assertThat(topicTrigger.getEffectiveDeadLetterDestinationName("syncOrder", orderEvents)).isEqualTo("customDeadLetters");

      ///////////////////////////////////////////////////////////////////////////
      // without an explicit name, an unknown destination can't be resolved to //
      // a queue or topic, so the one-arg version fails loudly                 //
      ///////////////////////////////////////////////////////////////////////////
      EsbTrigger unknownTrigger = new EsbTrigger().withDestinationName("noSuchDestination");
      assertThatThrownBy(() -> unknownTrigger.getEffectiveDeadLetterDestinationName("syncOrder"))
         .isInstanceOf(QRuntimeException.class)
         .hasMessageContaining("noSuchDestination");
      assertThatThrownBy(() -> unknownTrigger.getEffectiveDeadLetterDestinationName("syncOrder", null))
         .isInstanceOf(NullPointerException.class);

      QContext.clear();
      assertThatThrownBy(() -> queueTrigger.getEffectiveDeadLetterDestinationName("syncOrder"))
         .isInstanceOf(QRuntimeException.class)
         .hasMessageContaining("orderQueue");
      assertThat(topicTrigger.getEffectiveDeadLetterDestinationName("syncOrder")).isEqualTo("customDeadLetters");
   }



   /*******************************************************************************
    ** Adding a second provider or destination with a name already in use throws
    ** (like QInstance does for a second table or process), rather than silently
    ** replacing the first.
    *******************************************************************************/
   @Test
   void testDuplicateProviderOrDestinationNameThrows()
   {
      EsbInstanceMetaData esbInstanceMetaData = new EsbInstanceMetaData()
         .withProvider(new QEsbProviderMetaData().withName("p1"))
         .withDestination(new QEsbDestinationMetaData().withName("d1"));

      assertThatThrownBy(() -> esbInstanceMetaData.withProvider(new QEsbProviderMetaData().withName("p1")))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("p1");
      assertThatThrownBy(() -> esbInstanceMetaData.withDestination(new QEsbDestinationMetaData().withName("d1")))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("d1");

      ///////////////////////////////////////////////////////////////////
      // including via addSelfToInstance (as a MetaDataProducer would) //
      ///////////////////////////////////////////////////////////////////
      QInstance qInstance = defineInstance();
      assertThatThrownBy(() -> new QEsbProviderMetaData().withName(PROVIDER_NAME).addSelfToInstance(qInstance))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining(PROVIDER_NAME);
      new QEsbDestinationMetaData().withName("orderEvents").addSelfToInstance(qInstance);
      assertThatThrownBy(() -> new QEsbDestinationMetaData().withName("orderEvents").addSelfToInstance(qInstance))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("orderEvents");

      /////////////////////////////////////////////////////////
      // the originals are kept, and other names still work  //
      /////////////////////////////////////////////////////////
      assertThat(esbInstanceMetaData.getProviders()).containsOnlyKeys("p1");
      assertThat(esbInstanceMetaData.getDestinations()).containsOnlyKeys("d1");
      esbInstanceMetaData.withProvider(new QEsbProviderMetaData().withName("p2")).withDestination(new QEsbDestinationMetaData().withName("d2"));
      assertThat(esbInstanceMetaData.getProviders()).containsOnlyKeys("p1", "p2");
      assertThat(esbInstanceMetaData.getDestinations()).containsOnlyKeys("d1", "d2");
   }



   /*******************************************************************************
    ** Destination broker-side name defaults to the destination's name.
    *******************************************************************************/
   @Test
   void testDestination()
   {
      QEsbDestinationMetaData destination = new QEsbDestinationMetaData()
         .withName("orderEvents")
         .withType(EsbDestinationType.TOPIC)
         .withProviderName(PROVIDER_NAME);

      assertThat(destination.getName()).isEqualTo("orderEvents");
      assertThat(destination.getType()).isEqualTo(EsbDestinationType.TOPIC);
      assertThat(destination.getProviderName()).isEqualTo(PROVIDER_NAME);
      assertThat(destination.getDestinationName()).isNull();
      assertThat(destination.getEffectiveDestinationName()).isEqualTo("orderEvents");

      destination.setDestinationName("prod.order.events");
      assertThat(destination.getEffectiveDestinationName()).isEqualTo("prod.order.events");

      destination.setName("renamed");
      destination.setType(EsbDestinationType.QUEUE);
      destination.setProviderName("other");
      assertThat(destination.getName()).isEqualTo("renamed");
      assertThat(destination.getType()).isEqualTo(EsbDestinationType.QUEUE);
      assertThat(destination.getProviderName()).isEqualTo("other");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProvider()
   {
      QEsbProviderMetaData provider = new QEsbProviderMetaData();
      provider.setName("rabbit");
      provider.setType(EsbProviderType.RABBITMQ);
      provider.setUrl("amqp://localhost:5672/%2F");
      provider.setUsername("user");
      provider.setPassword("pass");
      provider.setManagementUrl("http://localhost:15672");
      provider.setManagementUsername("managementUser");
      provider.setManagementPassword("managementPass");

      assertThat(provider.getName()).isEqualTo("rabbit");
      assertThat(provider.getType()).isEqualTo(EsbProviderType.RABBITMQ);
      assertThat(provider.getUrl()).isEqualTo("amqp://localhost:5672/%2F");
      assertThat(provider.getUsername()).isEqualTo("user");
      assertThat(provider.getPassword()).isEqualTo("pass");
      assertThat(provider.getManagementUrl()).isEqualTo("http://localhost:15672");
      assertThat(provider.getManagementUsername()).isEqualTo("managementUser");
      assertThat(provider.getManagementPassword()).isEqualTo("managementPass");
   }



   /*******************************************************************************
    ** Providers and destinations are top-level meta-data: adding one to an
    ** instance puts it in the instance's ESB meta-data.
    *******************************************************************************/
   @Test
   void testProviderAndDestinationAddSelfToInstance()
   {
      QInstance qInstance = new QInstance();
      new QEsbProviderMetaData().withName("p1").withType(EsbProviderType.ACTIVEMQ_ARTEMIS).addSelfToInstance(qInstance);
      new QEsbDestinationMetaData().withName("d1").withType(EsbDestinationType.QUEUE).withProviderName("p1").addSelfToInstance(qInstance);

      EsbInstanceMetaData esbInstanceMetaData = EsbInstanceMetaData.of(qInstance);
      assertThat(esbInstanceMetaData.getProvider("p1")).isNotNull();
      assertThat(esbInstanceMetaData.getDestination("d1")).isNotNull();
      assertThat(esbInstanceMetaData.getProviders()).containsOnlyKeys("p1");
      assertThat(esbInstanceMetaData.getDestinations()).containsOnlyKeys("d1");
   }



   /*******************************************************************************
    ** Instance meta-data: name "esb", get-or-create, lookups.
    *******************************************************************************/
   @Test
   void testInstanceMetaData()
   {
      QInstance qInstance = new QInstance();
      assertThat(qInstance.getSupplementalMetaData(EsbInstanceMetaData.NAME)).isNull();

      EsbInstanceMetaData esbInstanceMetaData = EsbInstanceMetaData.of(qInstance);
      assertThat(esbInstanceMetaData).isNotNull();
      assertThat(esbInstanceMetaData.getName()).isEqualTo("esb");
      assertThat(qInstance.getSupplementalMetaData("esb")).isSameAs(esbInstanceMetaData);
      assertThat(EsbInstanceMetaData.of(qInstance)).isSameAs(esbInstanceMetaData);

      assertThat(esbInstanceMetaData.getProvider("nope")).isNull();
      assertThat(esbInstanceMetaData.getDestination("nope")).isNull();
      assertThat(esbInstanceMetaData.getProvider(null)).isNull();

      esbInstanceMetaData.setProviders(null);
      esbInstanceMetaData.setDestinations(null);
      assertThat(esbInstanceMetaData.getProvider("nope")).isNull();
      assertThat(esbInstanceMetaData.getDestination("nope")).isNull();

      esbInstanceMetaData
         .withProvider(new QEsbProviderMetaData().withName("p1"))
         .withDestination(new QEsbDestinationMetaData().withName("d1"));
      assertThat(esbInstanceMetaData.getProvider("p1").getName()).isEqualTo("p1");
      assertThat(esbInstanceMetaData.getDestination("d1").getName()).isEqualTo("d1");

      ///////////////////////////////////////////////////////
      // adding the object to the instance is idempotent   //
      ///////////////////////////////////////////////////////
      esbInstanceMetaData.addSelfToInstance(qInstance);
      assertThat(EsbInstanceMetaData.of(qInstance)).isSameAs(esbInstanceMetaData);
   }



   /*******************************************************************************
    ** Table and process supplemental meta-data: type "esb", of / ofOrWithNew.
    *******************************************************************************/
   @Test
   void testTableAndProcessSupplementalMetaData()
   {
      QTableMetaData   table   = QContext.getQInstance().getTable(TABLE_NAME_ORDER);
      QProcessMetaData process = QContext.getQInstance().getProcess(PROCESS_NAME_SYNC_ORDER);

      assertThat(EsbTableMetaData.of(table)).isNull();
      assertThat(EsbProcessMetaData.of(process)).isNull();

      EsbTableMetaData   esbTable   = EsbTableMetaData.ofOrWithNew(table);
      EsbProcessMetaData esbProcess = EsbProcessMetaData.ofOrWithNew(process);
      assertThat(esbTable.getType()).isEqualTo("esb");
      assertThat(esbProcess.getType()).isEqualTo("esb");
      assertThat(EsbTableMetaData.ofOrWithNew(table)).isSameAs(esbTable);
      assertThat(EsbProcessMetaData.ofOrWithNew(process)).isSameAs(esbProcess);
      assertThat(table.getSupplementalMetaData("esb")).isSameAs(esbTable);
      assertThat(process.getSupplementalMetaData("esb")).isSameAs(esbProcess);

      assertThat(esbTable.includeInFullFrontendMetaData()).isFalse();
      assertThat(esbTable.includeInPartialFrontendMetaData()).isFalse();

      EsbTablePublication tablePublication = new EsbTablePublication().withDestinationName("orderEvents").withEvents(List.of(EsbTableEvent.INSERT));
      esbTable.setPublications(new ArrayList<>(List.of(tablePublication)));
      assertThat(esbTable.getPublications()).containsExactly(tablePublication);
      tablePublication.setEvents(List.of(EsbTableEvent.DELETE));
      assertThat(tablePublication.getEvents()).containsExactly(EsbTableEvent.DELETE);

      EsbProcessPublication processPublication = new EsbProcessPublication().withDestinationName("orderEvents").withEvents(List.of(EsbProcessEvent.COMPLETED));
      EsbTrigger            trigger            = new EsbTrigger().withDestinationName("orderEvents");
      esbProcess.setPublications(new ArrayList<>(List.of(processPublication)));
      esbProcess.setTriggers(new ArrayList<>(List.of(trigger)));
      assertThat(esbProcess.getPublications()).containsExactly(processPublication);
      assertThat(esbProcess.getTriggers()).containsExactly(trigger);
      processPublication.setEvents(List.of(EsbProcessEvent.FAILED));
      assertThat(processPublication.getDestinationName()).isEqualTo("orderEvents");
      assertThat(processPublication.getEvents()).containsExactly(EsbProcessEvent.FAILED);

      esbProcess.withPublications(null).withTriggers(null);
      assertThat(esbProcess.getPublications()).isNull();
      assertThat(esbProcess.getTriggers()).isNull();
      esbTable.withPublications(null);
      assertThat(esbTable.getPublications()).isNull();
   }



   /*******************************************************************************
    ** Cloning a table deep-copies its ESB publications.
    *******************************************************************************/
   @Test
   void testTableCloneCopiesPublications()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TABLE_NAME_ORDER);
      EsbTableMetaData.ofOrWithNew(table).withPublication(new EsbTablePublication()
         .withDestinationName("orderEvents")
         .withEvents(new ArrayList<>(List.of(EsbTableEvent.INSERT))));

      QTableMetaData   clone         = table.clone();
      EsbTableMetaData cloneEsbTable = EsbTableMetaData.of(clone);
      assertThat(cloneEsbTable).isNotSameAs(EsbTableMetaData.of(table));
      assertThat(cloneEsbTable.getPublications()).hasSize(1);

      cloneEsbTable.getPublications().get(0).setDestinationName("changed");
      cloneEsbTable.getPublications().get(0).getEvents().add(EsbTableEvent.UPDATE);
      cloneEsbTable.withPublication(new EsbTablePublication().withDestinationName("another"));

      EsbTableMetaData original = EsbTableMetaData.of(table);
      assertThat(original.getPublications()).hasSize(1);
      assertThat(original.getPublications().get(0).getDestinationName()).isEqualTo("orderEvents");
      assertThat(original.getPublications().get(0).getEvents()).containsExactly(EsbTableEvent.INSERT);

      /////////////////////////////////////////////
      // a table with no publications clones too //
      /////////////////////////////////////////////
      EsbTableMetaData.of(table).setPublications(null);
      assertThat(EsbTableMetaData.of(table.clone()).getPublications()).isNull();
   }



   /*******************************************************************************
    ** Every trigger field has a getter, setter, and fluent setter.
    *******************************************************************************/
   @Test
   void testTriggerAccessors()
   {
      QCodeReference supplier = new QCodeReference(EsbMetaDataValidationTest.TestSessionSupplier.class);
      EsbTrigger     trigger  = new EsbTrigger();
      trigger.setDestinationName("d");
      trigger.setSubscriptionName("s");
      trigger.setMode(EsbTriggerMode.BATCH);
      trigger.setBatchSize(5);
      trigger.setBatchWaitMs(50);
      trigger.setConcurrency(4);
      trigger.setMaxAttempts(6);
      trigger.setRetryDelayMs(10);
      trigger.setRetryMultiplier(1.5);
      trigger.setRetryMaxDelayMs(100);
      trigger.setOnDeadLetter(EsbDeadLetterAction.DISCARD);
      trigger.setDeadLetterDestinationName("dl");
      trigger.setRunAsSessionSupplier(supplier);
      trigger.setTimeoutMs(1000);
      trigger.setStartPaused(true);

      assertThat(trigger.getDestinationName()).isEqualTo("d");
      assertThat(trigger.getSubscriptionName()).isEqualTo("s");
      assertThat(trigger.getMode()).isEqualTo(EsbTriggerMode.BATCH);
      assertThat(trigger.getBatchSize()).isEqualTo(5);
      assertThat(trigger.getBatchWaitMs()).isEqualTo(50);
      assertThat(trigger.getConcurrency()).isEqualTo(4);
      assertThat(trigger.getMaxAttempts()).isEqualTo(6);
      assertThat(trigger.getRetryDelayMs()).isEqualTo(10);
      assertThat(trigger.getRetryMultiplier()).isEqualTo(1.5);
      assertThat(trigger.getRetryMaxDelayMs()).isEqualTo(100);
      assertThat(trigger.getOnDeadLetter()).isEqualTo(EsbDeadLetterAction.DISCARD);
      assertThat(trigger.getDeadLetterDestinationName()).isEqualTo("dl");
      assertThat(trigger.getRunAsSessionSupplier()).isSameAs(supplier);
      assertThat(trigger.getTimeoutMs()).isEqualTo(1000);
      assertThat(trigger.getStartPaused()).isTrue();
   }



   /*******************************************************************************
    ** Enum values from spec section 3.
    *******************************************************************************/
   @Test
   void testEnums()
   {
      assertThat(EsbProviderType.values()).containsExactly(EsbProviderType.ACTIVEMQ_ARTEMIS, EsbProviderType.RABBITMQ);
      assertThat(EsbDestinationType.values()).containsExactly(EsbDestinationType.QUEUE, EsbDestinationType.TOPIC);
      assertThat(EsbTriggerMode.values()).containsExactly(EsbTriggerMode.SINGLE, EsbTriggerMode.BATCH);
      assertThat(EsbDeadLetterAction.values()).containsExactly(EsbDeadLetterAction.DEAD_LETTER_QUEUE, EsbDeadLetterAction.DISCARD);
      assertThat(EsbProcessEvent.values()).containsExactly(EsbProcessEvent.STARTED, EsbProcessEvent.COMPLETED, EsbProcessEvent.FAILED);
      assertThat(EsbTableEvent.values()).containsExactly(EsbTableEvent.INSERT, EsbTableEvent.UPDATE, EsbTableEvent.DELETE);
   }

}
