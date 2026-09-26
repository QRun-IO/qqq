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


import java.util.List;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.EsbTestBase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;


/*******************************************************************************
 ** Unit test for validation of ESB meta-data (via QInstanceValidator and the
 ** supplemental meta-data validate hooks).
 *******************************************************************************/
class EsbMetaDataValidationTest extends EsbTestBase
{
   private static final String RABBIT_PROVIDER = "rabbit";
   private static final String ORDER_EVENTS    = "orderEvents";
   private static final String ORDER_QUEUE     = "orderQueue";



   /*******************************************************************************
    ** An instance that uses every ESB meta-data field, validly.
    *******************************************************************************/
   private QInstance defineFullInstance()
   {
      QInstance qInstance = defineInstance();

      EsbInstanceMetaData.of(qInstance)
         .withProvider(new QEsbProviderMetaData()
            .withName(RABBIT_PROVIDER)
            .withType(EsbProviderType.RABBITMQ)
            .withUrl("amqp://localhost:5672/%2F")
            .withUsername("guest")
            .withPassword("guest")
            .withManagementUrl("http://localhost:15672")
            .withManagementUsername("guest")
            .withManagementPassword("guest"))
         .withDestination(new QEsbDestinationMetaData()
            .withName(ORDER_EVENTS)
            .withType(EsbDestinationType.TOPIC)
            .withProviderName(PROVIDER_NAME))
         .withDestination(new QEsbDestinationMetaData()
            .withName(ORDER_QUEUE)
            .withType(EsbDestinationType.QUEUE)
            .withProviderName(RABBIT_PROVIDER)
            .withDestinationName("orders.v1"));

      EsbTableMetaData.ofOrWithNew(qInstance.getTable(TABLE_NAME_ORDER))
         .withPublication(new EsbTablePublication()
            .withDestinationName(ORDER_EVENTS)
            .withEvents(List.of(EsbTableEvent.INSERT, EsbTableEvent.UPDATE, EsbTableEvent.DELETE)));

      EsbProcessMetaData.ofOrWithNew(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER))
         .withPublication(new EsbProcessPublication()
            .withDestinationName(ORDER_QUEUE)
            .withEvents(List.of(EsbProcessEvent.STARTED, EsbProcessEvent.COMPLETED, EsbProcessEvent.FAILED)))
         .withTrigger(new EsbTrigger()
            .withDestinationName(ORDER_EVENTS)
            .withSubscriptionName("syncOrderSubscription")
            .withConcurrency(2)
            .withMaxAttempts(5)
            .withRetryDelayMs(100)
            .withRetryMultiplier(3.0)
            .withRetryMaxDelayMs(5000)
            .withOnDeadLetter(EsbDeadLetterAction.DISCARD)
            .withDeadLetterDestinationName("orderEvents.syncOrder.dead")
            .withRunAsSessionSupplier(new QCodeReference(TestSessionSupplier.class))
            .withTimeoutMs(30_000)
            .withStartPaused(true))
         .withTrigger(new EsbTrigger()
            .withDestinationName(ORDER_QUEUE)
            .withMode(EsbTriggerMode.BATCH)
            .withBatchSize(10)
            .withBatchWaitMs(500));

      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidFullConfig()
   {
      QInstance qInstance = defineFullInstance();
      assertThatCode(() -> new QInstanceValidator().validate(qInstance)).doesNotThrowAnyException();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownProviderOnDestination()
   {
      QInstance qInstance = defineFullInstance();
      EsbInstanceMetaData.of(qInstance).getDestination(ORDER_EVENTS).setProviderName("noSuchProvider");
      assertValidationError(qInstance, "ESB destination orderEvents references an unknown provider: noSuchProvider");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownDestinationOnTablePublication()
   {
      QInstance qInstance = defineFullInstance();
      EsbTableMetaData.of(qInstance.getTable(TABLE_NAME_ORDER)).getPublications().get(0).setDestinationName("noSuchDestination");
      assertValidationError(qInstance, "ESB publication on table order references an unknown destination: noSuchDestination");
   }



   /*******************************************************************************
    ** A table publication, but no ESB instance meta-data at all.
    *******************************************************************************/
   @Test
   void testTablePublicationWithoutEsbInstanceMetaData()
   {
      QInstance qInstance = defineInstance();
      qInstance.getSupplementalMetaData().remove(EsbInstanceMetaData.NAME);
      EsbTableMetaData.ofOrWithNew(qInstance.getTable(TABLE_NAME_ORDER))
         .withPublication(new EsbTablePublication().withDestinationName(ORDER_EVENTS).withEvents(List.of(EsbTableEvent.INSERT)));
      assertValidationError(qInstance, "ESB publication on table order references an unknown destination: orderEvents");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownDestinationOnProcessPublication()
   {
      QInstance qInstance = defineFullInstance();
      EsbProcessMetaData.of(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER)).getPublications().get(0).setDestinationName("noSuchDestination");
      assertValidationError(qInstance, "ESB publication on process syncOrder references an unknown destination: noSuchDestination");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownDestinationOnTrigger()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance).setDestinationName("noSuchDestination");
      assertValidationError(qInstance, "ESB trigger syncOrder.noSuchDestination references an unknown destination: noSuchDestination");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDuplicateTriggerOnSameDestination()
   {
      QInstance qInstance = defineFullInstance();
      EsbProcessMetaData.of(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER)).withTrigger(new EsbTrigger().withDestinationName(ORDER_EVENTS));
      assertValidationError(qInstance, "Process syncOrder has more than one ESB trigger on destination orderEvents");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSubscriptionNameOnQueueTrigger()
   {
      QInstance qInstance = defineFullInstance();
      secondTrigger(qInstance).setSubscriptionName("notForQueues");
      assertValidationError(qInstance, "ESB trigger syncOrder.orderQueue has a subscriptionName, but destination orderQueue is a QUEUE");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testConcurrencyLessThanOne()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance).setConcurrency(0);
      assertValidationError(qInstance, "ESB trigger syncOrder.orderEvents concurrency must be at least 1");

      QInstance nullConcurrencyInstance = defineFullInstance();
      firstTrigger(nullConcurrencyInstance).setConcurrency(null);
      assertValidationError(nullConcurrencyInstance, "ESB trigger syncOrder.orderEvents concurrency must be at least 1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMaxAttemptsLessThanOne()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance).setMaxAttempts(0);
      assertValidationError(qInstance, "ESB trigger syncOrder.orderEvents maxAttempts must be at least 1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBatchSizeLessThanOne()
   {
      QInstance qInstance = defineFullInstance();
      secondTrigger(qInstance).setBatchSize(0);
      assertValidationError(qInstance, "ESB trigger syncOrder.orderQueue batchSize must be at least 1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBatchFieldsInSingleMode()
   {
      String expected = "ESB trigger syncOrder.orderEvents sets batchSize or batchWaitMs, but its mode is SINGLE";

      QInstance batchSizeInstance = defineFullInstance();
      firstTrigger(batchSizeInstance).setBatchSize(10);
      assertValidationError(batchSizeInstance, expected);

      QInstance batchWaitInstance = defineFullInstance();
      firstTrigger(batchWaitInstance).setBatchWaitMs(250);
      assertValidationError(batchWaitInstance, expected);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProviderWithoutUrl()
   {
      QInstance qInstance = defineFullInstance();
      EsbInstanceMetaData.of(qInstance).getProvider(RABBIT_PROVIDER).setUrl(null);
      assertValidationError(qInstance, "ESB provider rabbit is missing a url");

      QInstance artemisInstance = defineFullInstance();
      EsbInstanceMetaData.of(artemisInstance).getProvider(PROVIDER_NAME).setUrl("");
      assertValidationError(artemisInstance, "ESB provider artemis is missing a url");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProviderAndDestinationWithoutType()
   {
      QInstance providerInstance = defineFullInstance();
      EsbInstanceMetaData.of(providerInstance).getProvider(RABBIT_PROVIDER).setType(null);
      assertValidationError(providerInstance, "ESB provider rabbit is missing a type");

      QInstance destinationInstance = defineFullInstance();
      EsbInstanceMetaData.of(destinationInstance).getDestination(ORDER_QUEUE).setType(null);
      assertValidationError(destinationInstance, "ESB destination orderQueue is missing a type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRunAsSessionSupplierNotASupplier()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance).setRunAsSessionSupplier(new QCodeReference(NotASupplier.class));
      assertValidationError(qInstance, "ESB trigger syncOrder.orderEvents runAsSessionSupplier: CodeReference is not of the expected type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRunAsSessionSupplierOfWrongType()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance).setRunAsSessionSupplier(new QCodeReference(StringSupplier.class));
      assertValidationError(qInstance, "ESB trigger syncOrder.orderEvents runAsSessionSupplier must implement Supplier<QSession>");
   }



   /*******************************************************************************
    ** Two topic triggers on one provider can't use the same subscription name
    ** (both brokers name the subscription's queue after it) - though triggers
    ** on different providers can.
    *******************************************************************************/
   @Test
   void testDuplicateSubscriptionNameOnProvider()
   {
      QInstance qInstance = defineFullInstance();
      addProcessWithTrigger(qInstance, "auditOrder", new EsbTrigger().withDestinationName(ORDER_EVENTS).withSubscriptionName("syncOrderSubscription"));
      assertThat(validationReasons(qInstance)).anySatisfy(reason -> assertThat(reason)
         .startsWith("ESB subscription name syncOrderSubscription is used by more than one topic trigger on provider artemis")
         .contains("syncOrder.orderEvents")
         .contains("auditOrder.orderEvents"));

      QInstance otherProviderInstance = defineFullInstance();
      EsbInstanceMetaData.of(otherProviderInstance).withDestination(new QEsbDestinationMetaData()
         .withName("rabbitOrderEvents")
         .withType(EsbDestinationType.TOPIC)
         .withProviderName(RABBIT_PROVIDER));
      addProcessWithTrigger(otherProviderInstance, "auditOrder", new EsbTrigger().withDestinationName("rabbitOrderEvents").withSubscriptionName("syncOrderSubscription"));
      assertThatCode(() -> new QInstanceValidator().validate(otherProviderInstance)).doesNotThrowAnyException();
   }



   /*******************************************************************************
    ** Default subscription names are built from the broker-side destination
    ** name - so two QQQ destinations for the same broker topic give one process
    ** colliding default subscription names.
    *******************************************************************************/
   @Test
   void testDefaultSubscriptionNamesCollideOnBrokerSideName()
   {
      QInstance qInstance = defineFullInstance();
      EsbInstanceMetaData.of(qInstance).withDestination(new QEsbDestinationMetaData()
         .withName("orderEventsAlias")
         .withType(EsbDestinationType.TOPIC)
         .withProviderName(PROVIDER_NAME)
         .withDestinationName(ORDER_EVENTS));
      firstTrigger(qInstance).setSubscriptionName(null);
      EsbProcessMetaData.of(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER)).withTrigger(new EsbTrigger().withDestinationName("orderEventsAlias"));

      assertThat(validationReasons(qInstance)).anySatisfy(reason -> assertThat(reason)
         .isEqualTo("ESB subscription name syncOrder.orderEvents is used by more than one topic trigger on provider artemis: syncOrder.orderEvents, syncOrder.orderEventsAlias."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRetryDelayMsNegative()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance).setRetryDelayMs(-1);
      assertValidationError(qInstance, "ESB trigger syncOrder.orderEvents retryDelayMs must be at least 0");

      QInstance nullInstance = defineFullInstance();
      firstTrigger(nullInstance).setRetryDelayMs(null);
      assertValidationError(nullInstance, "ESB trigger syncOrder.orderEvents retryDelayMs must be at least 0");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRetryMultiplierBelowOne()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance).setRetryMultiplier(0.5);
      assertValidationError(qInstance, "ESB trigger syncOrder.orderEvents retryMultiplier must be at least 1");

      QInstance nullInstance = defineFullInstance();
      firstTrigger(nullInstance).setRetryMultiplier(null);
      assertValidationError(nullInstance, "ESB trigger syncOrder.orderEvents retryMultiplier must be at least 1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRetryMaxDelayBelowRetryDelay()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance).withRetryDelayMs(100).setRetryMaxDelayMs(99);
      assertValidationError(qInstance, "ESB trigger syncOrder.orderEvents retryMaxDelayMs must be at least retryDelayMs");

      QInstance nullInstance = defineFullInstance();
      firstTrigger(nullInstance).setRetryMaxDelayMs(null);
      assertValidationError(nullInstance, "ESB trigger syncOrder.orderEvents retryMaxDelayMs must be at least retryDelayMs");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTimeoutMsNotPositive()
   {
      QInstance zeroInstance = defineFullInstance();
      firstTrigger(zeroInstance).setTimeoutMs(0);
      assertValidationError(zeroInstance, "ESB trigger syncOrder.orderEvents timeoutMs must be greater than 0");

      QInstance negativeInstance = defineFullInstance();
      firstTrigger(negativeInstance).setTimeoutMs(-1);
      assertValidationError(negativeInstance, "ESB trigger syncOrder.orderEvents timeoutMs must be greater than 0");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBatchWaitMsNegative()
   {
      QInstance qInstance = defineFullInstance();
      secondTrigger(qInstance).setBatchWaitMs(-1);
      assertValidationError(qInstance, "ESB trigger syncOrder.orderQueue batchWaitMs must be at least 0");
   }



   /*******************************************************************************
    ** The lowest allowed values (and an unset timeout) validate.
    *******************************************************************************/
   @Test
   void testBoundaryValuesValidate()
   {
      QInstance qInstance = defineFullInstance();
      firstTrigger(qInstance)
         .withRetryDelayMs(0)
         .withRetryMultiplier(1.0)
         .withRetryMaxDelayMs(0)
         .withTimeoutMs(null);
      secondTrigger(qInstance)
         .withBatchWaitMs(0)
         .withTimeoutMs(1);
      assertThatCode(() -> new QInstanceValidator().validate(qInstance)).doesNotThrowAnyException();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingModeOrOnDeadLetter()
   {
      QInstance modeInstance = defineFullInstance();
      firstTrigger(modeInstance).setMode(null);
      List<String> modeReasons = validationReasons(modeInstance);
      assertThat(modeReasons).anySatisfy(reason -> assertThat(reason).startsWith("ESB trigger syncOrder.orderEvents is missing a mode"));
      assertThat(modeReasons).noneSatisfy(reason -> assertThat(reason).contains("batch fields"));

      QInstance onDeadLetterInstance = defineFullInstance();
      firstTrigger(onDeadLetterInstance).setOnDeadLetter(null);
      assertValidationError(onDeadLetterInstance, "ESB trigger syncOrder.orderEvents is missing an onDeadLetter action");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProviderAndDestinationWithoutName()
   {
      QInstance providerInstance = defineFullInstance();
      EsbInstanceMetaData.of(providerInstance).withProvider(new QEsbProviderMetaData()
         .withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
         .withUrl(getBrokerUrl()));
      assertValidationError(providerInstance, "An ESB provider is missing a name");

      QInstance destinationInstance = defineFullInstance();
      EsbInstanceMetaData.of(destinationInstance).withDestination(new QEsbDestinationMetaData()
         .withType(EsbDestinationType.QUEUE)
         .withProviderName(PROVIDER_NAME));
      assertValidationError(destinationInstance, "An ESB destination is missing a name");
   }



   /*******************************************************************************
    ** The instance name goes in event sources as the URI authority
    ** (qqq://instanceName/...), so it may only use URI-unreserved characters.
    *******************************************************************************/
   @Test
   void testInstanceName()
   {
      QInstance validInstance = defineFullInstance();
      EsbInstanceMetaData.of(validInstance).withInstanceName("order-service_2.prod~a");
      assertThatCode(() -> new QInstanceValidator().validate(validInstance)).doesNotThrowAnyException();

      QInstance invalidInstance = defineFullInstance();
      EsbInstanceMetaData.of(invalidInstance).withInstanceName("order service/prod");
      assertValidationError(invalidInstance, "ESB instanceName order service/prod may only contain letters, digits, and . _ ~ -");
   }



   /*******************************************************************************
    ** Validate, asserting it fails, with (at least) the expected reason.
    *******************************************************************************/
   private void assertValidationError(QInstance qInstance, String expectedReasonPrefix)
   {
      QInstanceValidationException exception = catchThrowableOfType(QInstanceValidationException.class, () -> new QInstanceValidator().validate(qInstance));
      assertThat(exception).as("validation should fail").isNotNull();
      assertThat(exception.getReasons()).anySatisfy(reason -> assertThat(reason).startsWith(expectedReasonPrefix));
   }



   /*******************************************************************************
    ** Validate, asserting it fails, and return all of its reasons.
    *******************************************************************************/
   private List<String> validationReasons(QInstance qInstance)
   {
      QInstanceValidationException exception = catchThrowableOfType(QInstanceValidationException.class, () -> new QInstanceValidator().validate(qInstance));
      assertThat(exception).as("validation should fail").isNotNull();
      return (exception.getReasons());
   }



   /*******************************************************************************
    ** Add another process (running the syncOrder step) with one ESB trigger.
    *******************************************************************************/
   private static void addProcessWithTrigger(QInstance qInstance, String processName, EsbTrigger trigger)
   {
      QProcessMetaData process = new QProcessMetaData()
         .withName(processName)
         .withStep(new QBackendStepMetaData()
            .withName("sync")
            .withCode(new QCodeReference(SyncOrderStep.class)));
      EsbProcessMetaData.ofOrWithNew(process).withTrigger(trigger);
      qInstance.addProcess(process);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static EsbTrigger firstTrigger(QInstance qInstance)
   {
      return (EsbProcessMetaData.of(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER)).getTriggers().get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static EsbTrigger secondTrigger(QInstance qInstance)
   {
      return (EsbProcessMetaData.of(qInstance.getProcess(PROCESS_NAME_SYNC_ORDER)).getTriggers().get(1));
   }



   /*******************************************************************************
    ** A valid run-as session supplier.
    *******************************************************************************/
   public static class TestSessionSupplier implements Supplier<QSession>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QSession get()
      {
         return (new QSession());
      }
   }



   /*******************************************************************************
    ** Not a Supplier at all.
    *******************************************************************************/
   public static class NotASupplier
   {
   }



   /*******************************************************************************
    ** A Supplier - but not of QSession.
    *******************************************************************************/
   public static class StringSupplier implements Supplier<String>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String get()
      {
         return ("not a session");
      }
   }

}
