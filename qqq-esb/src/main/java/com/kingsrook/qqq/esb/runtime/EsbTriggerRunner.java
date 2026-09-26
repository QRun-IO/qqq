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

package com.kingsrook.qqq.esb.runtime;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbUnparseableMessageException;
import com.kingsrook.qqq.esb.model.EsbDeadLetterAction;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.EsbTriggerMode;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Runs one trigger's consumers on this node (spec section 6).
 *
 * Consumers: the runner starts `concurrency` workers, each a virtual thread
 * with its own transacted JMS session and consumer, receiving with a 1 s
 * timeout.  Queue triggers use createConsumer (the workers, and other nodes,
 * compete for messages); topic triggers use createSharedDurableConsumer on the
 * trigger's effective subscription name (so each message is processed once
 * across all nodes, and is kept while they are down).  Names come from the
 * two-argument EsbTrigger methods, which don't need a QContext.
 *
 * Each message (or, in BATCH mode, each batch: up to batchSize messages, or
 * what arrives within batchWaitMs of the first) is run through
 * EsbTriggerHandler.runProcess on a new virtual thread, so each run starts with
 * a clean QContext, and can be timed out (the run thread is interrupted, and
 * the attempt counts as failed).  Then, in the same transacted session:
 * - success: commit (acknowledge);
 * - failure below maxAttempts (the attempt number being the broker's
 *   JMSXDeliveryCount - the highest in a batch): wait the backoff, then roll
 *   back, so the broker redelivers to this queue or subscription only (while
 *   waiting, the worker is busy);
 * - failure at maxAttempts: send each message to the dead-letter queue (or
 *   discard it, per onDeadLetter), with qqqError, qqqFailedTrigger,
 *   qqqAttempts and qqqFailedAt properties, then commit;
 * - a message that isn't a CloudEvent: dead-letter it at once (qqqError
 *   "unparseable message"), without retries.  In a batch, the other messages
 *   still run.
 * Dead letters copy the body of a text, bytes, map, or stream message.  An
 * object message's body is not deserialized, so its dead letter has no body,
 * and a qqqBodyDropped property of true.
 * A batch fails as a unit, so every message in it is dead-lettered when the
 * batch reaches maxAttempts, each with its own attempt count.  A session
 * commits or rolls back everything it received, so when a batch's run fails
 * and is retried, the dead letters of its unparseable messages roll back too:
 * those messages are redelivered as well, and dead-lettered by a later commit.
 *
 * Connecting never blocks the caller: until a worker has a consumer, the
 * trigger is CONNECTING, and the worker retries every second, or at once when
 * EsbConnectionManager reports a reconnect (QEsbRuntime passes that on).  When
 * the connection is lost, workers close their consumers and connect again.
 *
 * pauseLocal, resumeLocal and restartLocal act on this node only (see
 * EsbTriggerControl for every node).  Pausing closes the consumers - each after
 * the run it is in, if any, and at most one receive timeout (1 s) after the
 * pause - so messages stay on the broker.  A batch being collected is cut short
 * by a pause, and runs with what it has.
 * Restarting makes each worker build a new session and consumer.
 *
 * Workers block in the broker client's receive, which (on Java 21) holds its
 * virtual thread's carrier; the JDK adds carrier threads to make up for that,
 * up to its limit (jdk.virtualThreadScheduler.maxPoolSize, 256 by default) - so
 * a node's total concurrency, across all triggers, should stay well below it.
 *
 * Stopping (QEsbRuntime.stop) interrupts runs in progress, and rolls their
 * messages back, so they are redelivered after the next start - not lost, and
 * not counted as failures.  A run that ignores the interrupt is waited for up
 * to 2 s; after that its message is rolled back anyway, and the run is left to
 * finish on its own (with at-least-once delivery, the message is then
 * processed again).
 *******************************************************************************/
public class EsbTriggerRunner
{
   private static final QLogger LOG = QLogger.getLogger(EsbTriggerRunner.class);

   static final Long RECEIVE_TIMEOUT_MS     = 1000L;
   static final Long RECONNECT_POLL_MS      = 1000L;
   static final Long RUN_POLL_MS            = 100L;
   static final Long RUN_INTERRUPT_GRACE_MS = 2000L;

   private final QInstance               qInstance;
   private final String                  processName;
   private final EsbTrigger              trigger;
   private final QEsbDestinationMetaData destination;
   private final String                  triggerName;
   private final String                  providerName;
   private final String                  subscriptionName;
   private final String                  deadLetterDestinationName;
   private final EsbTriggerHandler       handler;

   ///////////////////////////////////////////////////////////////////////
   // state below is guarded by lock; changed is signalled (and the     //
   // changeCount bumped) on every change, to wake waiting workers      //
   ///////////////////////////////////////////////////////////////////////
   private final ReentrantLock lock    = new ReentrantLock();
   private final Condition     changed = lock.newCondition();

   private Boolean      active      = false;
   private Boolean      paused      = false;
   private Long         changeCount = 0L;
   private Long         generation  = 0L;
   private List<Worker> workers     = List.of();



   /*******************************************************************************
    ** Constructor - for the trigger on the named process, whose destination is
    ** given.  Not started.
    *******************************************************************************/
   EsbTriggerRunner(QInstance qInstance, String processName, EsbTrigger trigger, QEsbDestinationMetaData destination)
   {
      this.qInstance = qInstance;
      this.processName = processName;
      this.trigger = trigger;
      this.destination = destination;
      this.triggerName = trigger.getName(processName);
      this.providerName = destination.getProviderName();
      this.subscriptionName = destination.getType() == EsbDestinationType.TOPIC ? trigger.getEffectiveSubscriptionName(processName, destination) : null;
      this.deadLetterDestinationName = trigger.getEffectiveDeadLetterDestinationName(processName, destination);
      this.handler = new EsbTriggerHandler(qInstance);
   }



   /*******************************************************************************
    ** This trigger's state on this node.  RUNNING needs every worker to have a
    ** consumer built since the last restart or reconnect - so right after
    ** restartLocal, the state is CONNECTING until the consumers are rebuilt.
    *******************************************************************************/
   public EsbTriggerState getState()
   {
      lock.lock();
      try
      {
         if(!active)
         {
            return (EsbTriggerState.STOPPED);
         }

         if(paused)
         {
            return (EsbTriggerState.PAUSED);
         }

         for(Worker worker : workers)
         {
            if(!worker.hasConsumerFor(generation))
            {
               return (EsbTriggerState.CONNECTING);
            }
         }

         return (EsbTriggerState.RUNNING);
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    ** Pause this trigger on this node: its consumers close (see the class
    ** comment), and messages wait on the broker.  No-op if stopped or paused.
    *******************************************************************************/
   public void pauseLocal()
   {
      if(changeState(true, false))
      {
         LOG.info("Paused ESB trigger", logPair("triggerName", triggerName));
      }
   }



   /*******************************************************************************
    ** Resume this trigger on this node.  No-op if stopped or not paused.
    *******************************************************************************/
   public void resumeLocal()
   {
      if(changeState(false, false))
      {
         LOG.info("Resumed ESB trigger", logPair("triggerName", triggerName));
      }
   }



   /*******************************************************************************
    ** Restart this trigger's consumers on this node: each worker closes its
    ** session and consumer (after its current run, if any) and opens new ones.
    ** A paused trigger stays paused.  No-op if stopped.
    *******************************************************************************/
   public void restartLocal()
   {
      if(changeState(null, true))
      {
         LOG.info("Restarting ESB trigger consumers", logPair("triggerName", triggerName));
      }
   }



   /*******************************************************************************
    ** The trigger's name: processName.destinationName.
    *******************************************************************************/
   public String getTriggerName()
   {
      return (triggerName);
   }



   /*******************************************************************************
    ** The name of the process the trigger runs.
    *******************************************************************************/
   public String getProcessName()
   {
      return (processName);
   }



   /*******************************************************************************
    ** The trigger's meta-data.
    *******************************************************************************/
   public EsbTrigger getTrigger()
   {
      return (trigger);
   }



   /*******************************************************************************
    ** The trigger's destination (queue or topic).
    *******************************************************************************/
   public QEsbDestinationMetaData getDestination()
   {
      return (destination);
   }



   /*******************************************************************************
    ** Start the workers (at most once).  Doesn't block: the workers connect on
    ** their own threads.
    *******************************************************************************/
   void start()
   {
      lock.lock();
      try
      {
         if(active || !workers.isEmpty())
         {
            return;
         }

         active = true;
         paused = Boolean.TRUE.equals(trigger.getStartPaused());
         signalChange();

         Integer      concurrency = Math.max(1, Objects.requireNonNullElse(trigger.getConcurrency(), 1));
         List<Worker> newWorkers  = new ArrayList<>();
         for(int i = 0; i < concurrency; i++)
         {
            newWorkers.add(new Worker(i));
         }
         workers = List.copyOf(newWorkers);
         workers.forEach(Worker::start);

         LOG.info("Started ESB trigger", logPair("triggerName", triggerName), logPair("concurrency", concurrency), logPair("paused", paused));
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    ** Tell the workers to stop (see the class comment); awaitStopped waits for
    ** them.
    *******************************************************************************/
   void requestStop()
   {
      lock.lock();
      try
      {
         active = false;
         signalChange();
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    ** Wait, until the deadline at most, for the workers to finish.  Returns
    ** whether they all did.
    *******************************************************************************/
   Boolean awaitStopped(Instant deadline)
   {
      List<Worker> workersToAwait;
      lock.lock();
      try
      {
         workersToAwait = workers;
      }
      finally
      {
         lock.unlock();
      }

      Boolean allStopped = true;
      for(Worker worker : workersToAwait)
      {
         allStopped = worker.awaitStopped(deadline) && allStopped;
      }

      if(!allStopped)
      {
         LOG.warn("ESB trigger consumers did not stop in time; they will stop when their runs end", logPair("triggerName", triggerName));
      }
      return (allStopped);
   }



   /*******************************************************************************
    ** The provider reconnected: have every worker build a new consumer (the old
    ** ones died with the old connection).
    *******************************************************************************/
   void onReconnect()
   {
      changeState(null, true);
   }



   /*******************************************************************************
    ** Change the pause state (null: leave it) and/or bump the generation (so
    ** workers rebuild their consumers) - only while active.  Returns whether
    ** anything changed.
    *******************************************************************************/
   private Boolean changeState(Boolean newPaused, Boolean newGeneration)
   {
      lock.lock();
      try
      {
         if(!active)
         {
            return (false);
         }

         Boolean pauseChanged = newPaused != null && !newPaused.equals(paused);
         if(!pauseChanged && !newGeneration)
         {
            return (false);
         }

         if(pauseChanged)
         {
            paused = newPaused;
         }

         if(newGeneration)
         {
            generation++;
         }

         signalChange();
         return (true);
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    ** Wake waiting workers.  Called while holding the lock.
    *******************************************************************************/
   private void signalChange()
   {
      changeCount++;
      changed.signalAll();
   }



   /*******************************************************************************
    ** Wait until there's been a change since seenChangeCount (from
    ** getChangeCount), or maxWaitMs pass.
    *******************************************************************************/
   private void awaitChange(Long seenChangeCount, Long maxWaitMs)
   {
      lock.lock();
      try
      {
         long nanos = TimeUnit.MILLISECONDS.toNanos(maxWaitMs);
         while(changeCount.equals(seenChangeCount) && nanos > 0)
         {
            nanos = changed.awaitNanos(nanos);
         }
      }
      catch(InterruptedException e)
      {
         ////////////////////////////////////////////////////////////////////
         // workers are stopped through the active flag, never interrupts, //
         // so an interrupt here just ends this wait                       //
         ////////////////////////////////////////////////////////////////////
         LOG.debug("ESB trigger worker interrupted while waiting", logPair("triggerName", triggerName));
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Long getChangeCount()
   {
      lock.lock();
      try
      {
         return (changeCount);
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Boolean isActive()
   {
      lock.lock();
      try
      {
         return (active);
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Boolean isPaused()
   {
      lock.lock();
      try
      {
         return (paused);
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    ** The consumers' generation: it goes up with each restart and reconnect, and
    ** a worker's consumer is current only if built for the current generation.
    *******************************************************************************/
   Long getGeneration()
   {
      lock.lock();
      try
      {
         return (generation);
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void closeQuietly(AutoCloseable closeable)
   {
      if(closeable == null)
      {
         return;
      }

      try
      {
         closeable.close();
      }
      catch(Exception e)
      {
         LOG.debug("Error closing an ESB trigger session", e);
      }
   }



   /*******************************************************************************
    * One consumer: a virtual thread with its own transacted session.  Its JMS
    * objects are used only on its own thread.
    *******************************************************************************/
   private class Worker
   {
      private final Integer index;

      private Thread          thread;
      private Session         session;
      private MessageConsumer consumer;
      private MessageProducer deadLetterProducer;
      private Boolean         lastConnectFailed = false;

      /////////////////////////////////////////////////////////////////////
      // the generation the open consumer was built in; null when none   //
      // (read by getState on other threads)                             //
      /////////////////////////////////////////////////////////////////////
      private volatile Long consumerGeneration;



      /*******************************************************************************
       ** Constructor
       *******************************************************************************/
      Worker(Integer index)
      {
         this.index = index;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      void start()
      {
         thread = Thread.ofVirtual()
            .name("qqq-esb-" + triggerName + "-" + index)
            .start(this::run);
      }



      /*******************************************************************************
       ** Whether this worker has a consumer built in the given generation.
       *******************************************************************************/
      Boolean hasConsumerFor(Long currentGeneration)
      {
         return (currentGeneration.equals(consumerGeneration));
      }



      /*******************************************************************************
       ** Wait (until the deadline at most) for the thread to end.
       *******************************************************************************/
      Boolean awaitStopped(Instant deadline)
      {
         try
         {
            Duration remaining = Duration.between(Instant.now(), deadline);
            return (remaining.isNegative() ? !thread.isAlive() : thread.join(remaining));
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            return (!thread.isAlive());
         }
      }



      /*******************************************************************************
       ** The worker's loop: (re)connect as needed, then receive and handle - until
       ** stopped.  The QContext (just the instance) is for the connection
       ** manager, which reads the provider's meta-data from it.
       *******************************************************************************/
      private void run()
      {
         try
         {
            QContext.init(qInstance, null);
            while(true)
            {
               Long seenChangeCount = getChangeCount();
               if(!isActive())
               {
                  break;
               }

               try
               {
                  if(isPaused())
                  {
                     closeConsumer();
                     awaitChange(seenChangeCount, RECONNECT_POLL_MS);
                     continue;
                  }

                  if(consumer != null && (!consumerGeneration.equals(getGeneration()) || !EsbConnectionManager.getInstance().isConnected(providerName)))
                  {
                     closeConsumer();
                  }

                  if(consumer == null && !openConsumer())
                  {
                     awaitChange(seenChangeCount, RECONNECT_POLL_MS);
                     continue;
                  }

                  receiveAndHandle();
               }
               catch(Exception | LinkageError e)
               {
                  if(isActive())
                  {
                     LOG.warn("Error in ESB trigger consumer; reconnecting", e, logPair("triggerName", triggerName));
                  }
                  closeConsumer();
                  awaitChange(seenChangeCount, RECONNECT_POLL_MS);
               }
            }
         }
         catch(Exception | LinkageError e)
         {
            LOG.error("ESB trigger consumer ended with an error", e, logPair("triggerName", triggerName));
         }
         finally
         {
            closeConsumer();
            QContext.clear();
         }
      }



      /*******************************************************************************
       ** Open a transacted session and this trigger's consumer on it.  Returns
       ** false (and logs, once per outage) if that can't be done now.
       *******************************************************************************/
      private Boolean openConsumer()
      {
         Long    openingGeneration = getGeneration();
         Session newSession        = null;
         try
         {
            EsbConnectionManager manager = EsbConnectionManager.getInstance();
            newSession = manager.openSession(providerName, true);

            Destination jmsDestination = manager.resolve(newSession, destination);
            consumer = (destination.getType() == EsbDestinationType.TOPIC)
               ? newSession.createSharedDurableConsumer((Topic) jmsDestination, subscriptionName)
               : newSession.createConsumer(jmsDestination);
            session = newSession;
            consumerGeneration = openingGeneration;

            if(lastConnectFailed)
            {
               LOG.info("ESB trigger consumer connected", logPair("triggerName", triggerName), logPair("worker", index));
               lastConnectFailed = false;
            }
            return (true);
         }
         catch(QException | JMSException | RuntimeException e)
         {
            consumer = null;
            closeQuietly(newSession);
            if(!lastConnectFailed)
            {
               ////////////////////////////////////////////////////////////////////
               // with the provider connected, this isn't an outage that a       //
               // reconnect will end (e.g., the broker refused the subscription) //
               ////////////////////////////////////////////////////////////////////
               if(EsbConnectionManager.getInstance().isConnected(providerName))
               {
                  LOG.warn("ESB trigger consumer could not be set up though its provider is connected; will keep trying", e, logPair("triggerName", triggerName), logPair("worker", index));
               }
               else
               {
                  LOG.info("ESB trigger consumer could not connect; will keep trying", logPair("triggerName", triggerName), logPair("worker", index), logPair("error", e.getMessage()));
               }
               lastConnectFailed = true;
            }
            return (false);
         }
      }



      /*******************************************************************************
       ** Close the session (which closes the consumer and producer, and rolls
       ** back anything not committed).
       *******************************************************************************/
      private void closeConsumer()
      {
         consumerGeneration = null;
         Session sessionToClose = session;
         session = null;
         consumer = null;
         deadLetterProducer = null;
         closeQuietly(sessionToClose);
      }



      /*******************************************************************************
       ** Receive a message (waiting up to the receive timeout) - or, in BATCH
       ** mode, a batch - and handle it.
       *******************************************************************************/
      private void receiveAndHandle() throws JMSException
      {
         Message first = consumer.receive(RECEIVE_TIMEOUT_MS);
         if(first == null)
         {
            return;
         }

         List<Message> messages = new ArrayList<>();
         messages.add(first);
         if(trigger.getMode() == EsbTriggerMode.BATCH)
         {
            collectBatch(messages);
         }

         if(!isActive())
         {
            session.rollback();
            return;
         }

         handleMessages(messages);
      }



      /*******************************************************************************
       ** Add to a batch until it has batchSize messages, or batchWaitMs have
       ** passed since its first one (or the runner is stopping or pausing).
       *******************************************************************************/
      private void collectBatch(List<Message> messages) throws JMSException
      {
         Integer batchSize  = trigger.getEffectiveBatchSize();
         long    deadlineMs = System.currentTimeMillis() + trigger.getEffectiveBatchWaitMs();
         while(messages.size() < batchSize && isActive() && !isPaused())
         {
            long    remainingMs = deadlineMs - System.currentTimeMillis();
            Message next        = (remainingMs > 0) ? consumer.receive(Math.min(remainingMs, RECEIVE_TIMEOUT_MS)) : consumer.receiveNoWait();
            if(next != null)
            {
               messages.add(next);
            }
            else if(remainingMs <= 0)
            {
               break;
            }
         }
      }



      /*******************************************************************************
       ** Dead-letter unparseable messages, run the process for the rest, then
       ** commit, retry, or dead-letter (see the class comment).
       *******************************************************************************/
      private void handleMessages(List<Message> messages) throws JMSException
      {
         EsbStats       stats        = EsbStats.getInstance();
         List<Message>  runMessages  = new ArrayList<>();
         List<EsbEvent> events       = new ArrayList<>();
         Integer        deadLettered = 0;

         for(Message message : messages)
         {
            stats.consumed(triggerName);
            try
            {
               events.add(EsbEventCodec.fromMessage(message));
               runMessages.add(message);
            }
            catch(EsbUnparseableMessageException e)
            {
               LOG.warn("Dead-lettering an unparseable message from an ESB trigger", logPair("triggerName", triggerName), logPair("messageId", message.getJMSMessageID()), logPair("error", e.getMessage()));
               stats.failed(triggerName, e);
               deadLetter(message, EsbTriggerHandler.ERROR_UNPARSEABLE, EsbTriggerHandler.getAttempt(message));
               deadLettered++;
            }
         }

         if(events.isEmpty())
         {
            commit(deadLettered);
            return;
         }

         RunResult result = runProcess(events);
         if(result.stopped())
         {
            session.rollback();
            return;
         }

         if(result.error() == null)
         {
            commit(deadLettered);
            stats.succeeded(triggerName, result.ms());
            return;
         }

         stats.failed(triggerName, result.error());
         String  errorText   = EsbTriggerHandler.getErrorText(result.error());
         Integer maxAttempts = Objects.requireNonNullElse(trigger.getMaxAttempts(), 1);
         Integer attempt     = runMessages.stream().map(EsbTriggerHandler::getAttempt).max(Integer::compare).orElse(1);
         if(attempt < maxAttempts)
         {
            Long delayMs = EsbTriggerHandler.getRetryDelayMs(trigger, attempt);
            LOG.info("ESB trigger run failed; it will be retried", logPair("triggerName", triggerName), logPair("attempt", attempt), logPair("maxAttempts", maxAttempts), logPair("retryDelayMs", delayMs), logPair("error", errorText));
            waitForBackoff(delayMs);
            session.rollback();
            runMessages.forEach(message -> stats.retried(triggerName));
            return;
         }

         LOG.warn("ESB trigger run failed on its last attempt", logPair("triggerName", triggerName), logPair("attempt", attempt), logPair("messageCount", runMessages.size()), logPair("onDeadLetter", trigger.getOnDeadLetter()), logPair("error", errorText));
         for(Message message : runMessages)
         {
            deadLetter(message, errorText, EsbTriggerHandler.getAttempt(message));
            deadLettered++;
         }
         commit(deadLettered);
      }



      /*******************************************************************************
       ** Commit the session, then count what it dead-lettered.
       *******************************************************************************/
      private void commit(Integer deadLettered) throws JMSException
      {
         session.commit();
         for(int i = 0; i < deadLettered; i++)
         {
            EsbStats.getInstance().deadLettered(triggerName);
         }
      }



      /*******************************************************************************
       ** Send a copy of the message to the dead-letter queue (in this session's
       ** transaction) - or, with onDeadLetter DISCARD, just log that it is
       ** dropped.
       *******************************************************************************/
      private void deadLetter(Message message, String error, Integer attempts) throws JMSException
      {
         if(trigger.getOnDeadLetter() == EsbDeadLetterAction.DISCARD)
         {
            LOG.warn("Discarding a message from an ESB trigger", logPair("triggerName", triggerName), logPair("messageId", message.getJMSMessageID()), logPair("attempts", attempts));
            return;
         }

         if(deadLetterProducer == null)
         {
            deadLetterProducer = session.createProducer(EsbConnectionManager.getInstance().resolveQueue(session, providerName, deadLetterDestinationName));
            deadLetterProducer.setDeliveryMode(DeliveryMode.PERSISTENT);
         }

         deadLetterProducer.send(EsbTriggerHandler.buildDeadLetter(session, message, triggerName, error, attempts));
      }



      /*******************************************************************************
       ** Wait out a retry backoff - unless the runner stops first.
       *******************************************************************************/
      private void waitForBackoff(Long delayMs)
      {
         long deadlineMs = System.currentTimeMillis() + delayMs;
         while(isActive())
         {
            long remainingMs = deadlineMs - System.currentTimeMillis();
            if(remainingMs <= 0)
            {
               return;
            }
            awaitChange(getChangeCount(), remainingMs);
         }
      }



      /*******************************************************************************
       ** Run the process for the events on a new virtual thread, waiting for it -
       ** up to timeoutMs, if the trigger has one - and watching for a stop.  A
       ** run that times out, or is stopped, is interrupted (and given a grace
       ** period to end); a run that finished meanwhile still counts as done.
       *******************************************************************************/
      private RunResult runProcess(List<EsbEvent> events)
      {
         AtomicBoolean              finished = new AtomicBoolean(false);
         AtomicReference<Exception> failure  = new AtomicReference<>();
         long                       startMs  = System.currentTimeMillis();

         EsbStats.getInstance().inFlight(triggerName, events.size());
         Thread runThread = Thread.ofVirtual()
            .name("qqq-esb-run-" + triggerName + "-" + index)
            .start(() ->
            {
               try
               {
                  handler.runProcess(trigger, processName, events);
                  finished.set(true);
               }
               catch(Throwable t)
               {
                  //////////////////////////////////////////////////////////////
                  // an Error fails the run too, keeping the Error as the cause //
                  //////////////////////////////////////////////////////////////
                  failure.set(t instanceof Exception exception ? exception : new QException(t.toString(), t));
               }
            });

         try
         {
            Integer timeoutMs = trigger.getTimeoutMs();
            while(runThread.isAlive())
            {
               long elapsedMs = System.currentTimeMillis() - startMs;
               if(!isActive())
               {
                  interruptAndWait(runThread);
                  return (finished.get() ? RunResult.succeeded(elapsedMs) : RunResult.STOPPED);
               }

               if(timeoutMs != null && elapsedMs >= timeoutMs)
               {
                  interruptAndWait(runThread);
                  return (finished.get() ? RunResult.succeeded(elapsedMs) : RunResult.failed(new QException("ESB trigger run timed out after " + timeoutMs + " ms"), elapsedMs));
               }

               long waitMs = (timeoutMs == null) ? RUN_POLL_MS : Math.min(RUN_POLL_MS, timeoutMs - elapsedMs);
               runThread.join(Math.max(1, waitMs));
            }
         }
         catch(InterruptedException e)
         {
            interruptAndWait(runThread);
            return (RunResult.STOPPED);
         }
         finally
         {
            EsbStats.getInstance().inFlight(triggerName, -events.size());
         }

         long elapsedMs = System.currentTimeMillis() - startMs;
         if(finished.get())
         {
            return (RunResult.succeeded(elapsedMs));
         }

         return (RunResult.failed(Objects.requireNonNullElseGet(failure.get(), () -> new QException("ESB trigger run ended without finishing")), elapsedMs));
      }



      /*******************************************************************************
       ** Interrupt a run thread, and give it a grace period to end.
       *******************************************************************************/
      private void interruptAndWait(Thread runThread)
      {
         runThread.interrupt();
         try
         {
            if(!runThread.join(Duration.ofMillis(RUN_INTERRUPT_GRACE_MS)))
            {
               LOG.warn("An interrupted ESB trigger run did not end; leaving it to finish on its own", logPair("triggerName", triggerName));
            }
         }
         catch(InterruptedException e)
         {
            LOG.debug("Interrupted while waiting for an ESB trigger run to end", logPair("triggerName", triggerName));
         }
      }
   }



   /*******************************************************************************
    * How a run went: stopped (by the runner stopping), or done - succeeded (no
    * error) or failed - taking ms milliseconds.
    *******************************************************************************/
   private record RunResult(Boolean stopped, Exception error, Long ms)
   {
      private static final RunResult STOPPED = new RunResult(true, null, 0L);



      /*******************************************************************************
       **
       *******************************************************************************/
      static RunResult succeeded(Long ms)
      {
         return (new RunResult(false, null, ms));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      static RunResult failed(Exception error, Long ms)
      {
         return (new RunResult(false, error, ms));
      }
   }

}
