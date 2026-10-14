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

package com.kingsrook.qqq.backend.core.health;


import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Slot-leased health execution. Probe threads are bounded even if interruption
 ** is ignored. Every worker installs and clears its own automated QContext.
 *******************************************************************************/
public class HealthRunner implements AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(HealthRunner.class);
   private static final Map<QInstance, HealthRunner> RUNNERS = new ConcurrentHashMap<>();
   private final QInstance qInstance;
   private final String podId;
   private final Clock clock;
   private final HealthMonitoringMetaData settings;
   private final ExecutorService pool;
   private final Semaphore capacity;
   private final Map<String, HealthCheckOutcome> lastOutcomes = new ConcurrentHashMap<>();
   private final Map<String, Instant> lastOutcomeTimes = new ConcurrentHashMap<>();
   private volatile Map<String, HealthCheckMetaData> definitions = Map.of();
   private Random random = new Random();
   private ScheduledExecutorService scheduler;
   private Instant lastSync;
   private Instant lastHeartbeat;
   private Instant lastStorageWarning;
   private volatile boolean stopped;
   private final AtomicBoolean ticking = new AtomicBoolean();



   /*******************************************************************************
    ** Construction does not schedule work. Clock and randomness are injectable.
    *******************************************************************************/
   public HealthRunner(QInstance qInstance, String podId, Clock clock)
   {
      this.qInstance = Objects.requireNonNull(qInstance);
      this.podId = Objects.requireNonNull(podId);
      this.clock = Objects.requireNonNull(clock);
      settings = HealthMonitoringMetaData.ofOrWithNew(qInstance);
      capacity = new Semaphore(settings.getMaxConcurrentChecks());
      pool = Executors.newFixedThreadPool(settings.getMaxConcurrentChecks(), Thread.ofPlatform().daemon().name("health-check-", 0).factory());
   }



   /*******************************************************************************
    ** Configure deterministic jitter before starting this runner.
    *******************************************************************************/
   public HealthRunner withRandom(Random random)
   {
      this.random = Objects.requireNonNull(random);
      return (this);
   }



   /*******************************************************************************
    ** Find the runner started for this instance in this JVM.
    *******************************************************************************/
   public static HealthRunner getForInstance(QInstance qInstance)
   {
      return (RUNNERS.get(qInstance));
   }



   /*******************************************************************************
    ** Last sanitized outcome, including probes made during a storage outage.
    *******************************************************************************/
   public HealthCheckOutcome getLastOutcome(String name)
   {
      return (lastOutcomes.get(name));
   }



   /*******************************************************************************
    ** Completion time used by readiness to reject stale cached results.
    *******************************************************************************/
   public Instant getLastOutcomeAt(String name)
   {
      return (lastOutcomeTimes.get(name));
   }



   /*******************************************************************************
    ** Publish the result before its completion timestamp.
    *******************************************************************************/
   private void cacheOutcome(String name, HealthCheckOutcome outcome)
   {
      lastOutcomes.put(name, outcome);
      lastOutcomeTimes.put(name, clock.instant());
   }



   /*******************************************************************************
    ** Synchronize definitions before beginning fixed-delay ticks.
    *******************************************************************************/
   public synchronized void start() throws QException
   {
      if(!settings.getRunnerEnabled() || scheduler != null)
      {
         return;
      }
      if(stopped)
      {
         throw new QException("A stopped health runner cannot be restarted");
      }
      QContext.withTemporaryContext(context(), () ->
      {
         try
         {
            sync();
         }
         catch(QException e)
         {
            storageWarning(e);
         }
      });
      if(RUNNERS.putIfAbsent(qInstance, this) != null)
      {
         throw new QException("A health runner is already started for this instance");
      }
      scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("health-tick-", 0).factory());
      scheduler.scheduleWithFixedDelay(this::tick, 0, settings.getTickSeconds(), TimeUnit.SECONDS);
   }



   /*******************************************************************************
    ** Stop scheduling and interrupt probes; daemon threads cannot hold shutdown.
    *******************************************************************************/
   public synchronized void stop()
   {
      stopped = true;
      if(scheduler != null)
      {
         scheduler.shutdownNow();
      }
      pool.shutdownNow();
      RUNNERS.remove(qInstance, this);
   }



   /*******************************************************************************
    ** Close transient runners after record actions.
    *******************************************************************************/
   @Override
   public void close()
   {
      stop();
   }



   /*******************************************************************************
    ** Public deterministic tick. Launch all available probes before awaiting any.
    *******************************************************************************/
   public void tick()
   {
      if(!ticking.compareAndSet(false, true))
      {
         return;
      }
      try
      {
         if(stopped || !settings.getRunnerEnabled())
         {
            return;
         }
         QContext.withTemporaryContext(context(), () ->
         {
            try
            {
               Instant now = clock.instant();
               if(lastSync == null || !now.isBefore(lastSync.plusSeconds(300)))
               {
                  sync();
               }
               closeAbandoned(now);
               List<Execution> executions = new ArrayList<>();
               for(QRecord check : HealthStore.query(HealthMetaDataProvider.CHECK_TABLE, new QQueryFilter()))
               {
                  if(!Boolean.TRUE.equals(check.getValueBoolean("registered")) || Boolean.FALSE.equals(check.getValueBoolean("enabled")))
                  {
                     continue;
                  }
                  Instant due = check.getValueInstant("nextDueAt");
                  if(due != null && due.isAfter(now))
                  {
                     continue;
                  }
                  Execution execution = launch(check, false);
                  if(execution != null)
                  {
                     executions.add(execution);
                  }
               }
               for(Execution execution : executions)
               {
                  finish(execution);
               }
               if(lastHeartbeat == null || !now.isBefore(lastHeartbeat.plusSeconds(60)))
               {
                  QAlerts.heartbeat("health.runner");
                  lastHeartbeat = now;
               }
            }
            catch(Exception e)
            {
               storageWarning(e);
               probeDatabasesWithoutStorage();
            }
         });
      }
      finally
      {
         ticking.set(false);
      }
   }



   /*******************************************************************************
    ** On-demand uses a unique request id and deliberately ignores enabled.
    *******************************************************************************/
   public HealthCheckOutcome runNow(String checkName) throws QException
   {
      AtomicReference<HealthCheckOutcome> result = new AtomicReference<>();
      QContext.withTemporaryContext(context(), () ->
      {
         if(definitions.isEmpty())
         {
            sync();
         }
         QRecord check = HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", checkName);
         if(check == null || !definitions.containsKey(checkName))
         {
            throw new QException("Health check is not registered");
         }
         Execution execution = launch(check, true);
         result.set(execution == null ? HealthCheckOutcome.unknown("Health check capacity is busy") : finish(execution));
      });
      return (result.get());
   }



   /*******************************************************************************
    ** Automated identity has health permissions only and no record security keys.
    ** Like scheduler workers, it installs a separate session on each thread.
    *******************************************************************************/
   private CapturedContext context()
   {
      return (new CapturedContext(qInstance, new QSession()
         .withUser(new QUser().withIdReference(settings.getRunnerUserName()).withFullName(settings.getRunnerUserName()))
         .withSecurityKeyValues(Map.of()).withPermissions("health.read", "health.write")));
   }



   /*******************************************************************************
    ** Replace the entire registry snapshot only after successful synchronization.
    *******************************************************************************/
   private void sync() throws QException
   {
      Map<String, HealthCheckMetaData> resolved = new LinkedHashMap<>();
      for(HealthCheckMetaData check : HealthChecksMetaData.ofOrWithNew(qInstance).resolveAll(qInstance))
      {
         resolved.put(check.getName(), check);
      }
      definitions = Map.copyOf(resolved);
      new HealthCheckSync().sync(qInstance, List.copyOf(resolved.values()));
      lastSync = clock.instant();
   }



   /*******************************************************************************
    ** Claim only when a worker is available. The lock supplies database-like
    ** uniqueness for the in-process memory backend; real backends use the key.
    *******************************************************************************/
   private Execution launch(QRecord check, boolean onDemand) throws QException
   {
      HealthCheckMetaData definition = definitions.get(check.getValueString("name"));
      if(definition == null || !capacity.tryAcquire())
      {
         return (null);
      }
      boolean submitted = false;
      try
      {
         int interval = effective(check, "intervalSeconds", definition.getIntervalSeconds());
         int timeout = effective(check, "timeoutMs", definition.getTimeoutMs());
         if(interval < 10 || timeout <= 0 || timeout >= interval * 1000L || (definition.getKind() == HealthCheckKind.LIVENESS && timeout > 1000))
         {
            throw new QException("Invalid health scheduling override");
         }
         Instant now = clock.instant();
         long slot = Math.floorDiv(now.getEpochSecond(), interval);
         String requestId = onDemand ? UUID.randomUUID().toString() : "";
         QRecord result;
         synchronized(HealthStore.lockFor(qInstance))
         {
            if(!onDemand && !HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter()
               .withCriteria(new QFilterCriteria("healthCheckId", QCriteriaOperator.EQUALS, check.getValue("id")))
               .withCriteria(new QFilterCriteria("slot", QCriteriaOperator.EQUALS, slot))
               .withCriteria(new QFilterCriteria("onDemandRequestId", QCriteriaOperator.EQUALS, ""))).isEmpty())
            {
               return (null);
            }
            try
            {
               result = HealthStore.insert(HealthMetaDataProvider.RESULT_TABLE, new QRecord()
                  .withValue("healthCheckId", check.getValue("id")).withValue("slot", slot).withValue("onDemandRequestId", requestId)
                  .withValue("status", HealthResultStatus.RUNNING.name()).withValue("runnerPod", podId)
                  .withValue("startedAt", now).withValue("leaseExpiresAt", now.plusMillis(timeout + 5000L))
                  .withValue("isTransition", false).withValue("triggeredBy", onDemand ? "ON_DEMAND" : "SCHEDULE"));
            }
            catch(QException e)
            {
               LOG.debug("Health slot insert lost or unavailable", logPair("check", definition.getName()));
               return (null);
            }
         }
         long started = System.nanoTime();
         FutureTask<HealthCheckOutcome> future = submitProbe(definition, new HealthCheckInput().withQInstance(qInstance)
            .withDeadline(now.plusMillis(timeout)).withOnDemand(onDemand).withAttempt(1));
         submitted = true;
         return (new Execution(check, result, definition, future, started, interval, timeout, slot));
      }
      finally
      {
         if(!submitted)
         {
            capacity.release();
         }
      }
   }



   /*******************************************************************************
    ** Submit a probe after capacity has been acquired, releasing the permit only
    ** when the underlying thread actually exits, even after cancel(true).
    *******************************************************************************/
   private FutureTask<HealthCheckOutcome> submitProbe(HealthCheckMetaData definition, HealthCheckInput input)
   {
      long submittedAt = System.nanoTime();
      long timeoutMs = Math.max(0, Duration.between(clock.instant(), input.getDeadline()).toMillis());
      FutureTask<HealthCheckOutcome> future = new FutureTask<>(() ->
      {
         QContext.init(context());
         QContext.setObject("healthCheckName", definition.getName());
         try
         {
            HealthCheck implementation = definition.getCheckInstance() == null ? QCodeLoader.getAdHoc(HealthCheck.class, definition.getCheck()) : definition.getCheckInstance();
            long probeStarted = System.nanoTime();
            HealthCheckOutcome outcome = implementation.run(input);
            if(System.nanoTime() - submittedAt > TimeUnit.MILLISECONDS.toNanos(timeoutMs))
            {
               return (HealthCheckOutcome.down("timed out after " + timeoutMs + "ms"));
            }
            if(outcome != null && outcome.getLatencyMs() == null)
            {
               outcome.setLatencyMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - probeStarted));
            }
            return (outcome);
         }
         finally
         {
            QContext.clear();
         }
      });
      pool.execute(() ->
      {
         try
         {
            future.run();
         }
         finally
         {
            capacity.release();
         }
      });
      return (future);
   }



   /*******************************************************************************
    ** Wait against a monotonic deadline, sanitize, persist, then send alert intents.
    *******************************************************************************/
   private HealthCheckOutcome finish(Execution execution) throws QException
   {
      HealthCheckOutcome outcome;
      try
      {
         long remaining = TimeUnit.MILLISECONDS.toNanos(execution.timeout()) - (System.nanoTime() - execution.started());
         outcome = execution.future().get(Math.max(0, remaining), TimeUnit.NANOSECONDS);
         if(outcome == null)
         {
            outcome = HealthCheckOutcome.unknown("Check returned no outcome");
         }
      }
      catch(TimeoutException e)
      {
         execution.future().cancel(true);
         outcome = HealthCheckOutcome.down("timed out after " + execution.timeout() + "ms");
      }
      catch(InterruptedException e)
      {
         execution.future().cancel(true);
         Thread.currentThread().interrupt();
         throw new QException("Health runner interrupted", e);
      }
      catch(ExecutionException e)
      {
         outcome = HealthCheckOutcome.down(HealthRedactor.redactThrowable(e.getCause()));
         Exception redactedException = new Exception(outcome.getMessage());
         redactedException.setStackTrace(e.getCause().getStackTrace());
         LOG.debug("Health check threw an exception", redactedException, logPair("check", execution.definition().getName()));
      }
      outcome = sanitize(outcome, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - execution.started()));
      cacheOutcome(execution.definition().getName(), outcome);
      synchronized(HealthStore.lockFor(qInstance))
      {
         QRecord result = HealthStore.find(HealthMetaDataProvider.RESULT_TABLE, "id", execution.result().getValue("id"));
         if(result == null || !HealthResultStatus.RUNNING.name().equals(result.getValueString("status")))
         {
            return (outcome);
         }
         QRecord check = HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "id", execution.check().getValue("id"));
         Instant now = clock.instant();
         HealthTransitionEvaluator.Decision decision = new HealthTransitionEvaluator().evaluate(check, outcome,
            effective(check, "failureThreshold", execution.definition().getFailureThreshold()),
            effective(check, "recoveryThreshold", execution.definition().getRecoveryThreshold()), now);
         HealthStore.update(HealthMetaDataProvider.RESULT_TABLE, new QRecord().withValue("id", result.getValue("id"))
            .withValue("status", outcome.getStatus().name()).withValue("message", outcome.getMessage())
            .withValue("detailsJson", outcome.getDetails().toJson()).withValue("metricsJson", JsonUtils.toJson(outcome.getMetrics()))
            .withValue("latencyMs", outcome.getLatencyMs()).withValue("finishedAt", now).withValue("isTransition", decision.transition()));
         int jitterBound = (int) Math.min(30_000L, execution.interval() * 100L);
         Instant next = Instant.ofEpochSecond((execution.slot() + 1) * execution.interval()).plusMillis(random.nextInt(jitterBound + 1));
         HealthStore.update(HealthMetaDataProvider.CHECK_TABLE, decision.state().withValue("currentMessage", outcome.getMessage())
            .withValue("lastRunAt", now).withValue("lastLatencyMs", outcome.getLatencyMs()).withValue("nextDueAt", next));
         rollup(check.getValueString("component"), now);
         decision.raises().forEach(QAlerts::raise);
         decision.resolutions().forEach(r -> QAlerts.resolve(r.getAlertTypeName(), r.getDedupKey(), r.getNote()));
      }
      return (outcome);
   }



   /*******************************************************************************
    ** A fresh sanitized outcome prevents a check from mutating persisted values.
    *******************************************************************************/
   static HealthCheckOutcome sanitize(HealthCheckOutcome raw, long elapsed)
   {
      HealthDetails details = new HealthDetails();
      if(raw.getDetails() != null)
      {
         raw.getDetails().getValues().forEach(details::withValue);
      }
      String message = HealthRedactor.redact(raw.getMessage());
      if(message != null)
      {
         message = message.split("\\R", 2)[0];
         message = message.substring(0, Math.min(500, message.length()));
      }
      List<HealthMetric> metrics = new ArrayList<>();
      if(raw.getMetrics() != null)
      {
         for(HealthMetric metric : raw.getMetrics())
         {
            if(metric != null && metric.getValue() != null && Double.isFinite(metric.getValue().doubleValue()))
            {
               metrics.add(new HealthMetric().withName(HealthRedactor.redact(metric.getName())).withValue(metric.getValue())
                  .withUnit(HealthRedactor.redact(metric.getUnit())));
            }
         }
      }
      return (new HealthCheckOutcome().withStatus(raw.getStatus() == null ? HealthStatus.UNKNOWN : raw.getStatus())
         .withMessage(message).withDetails(details).withMetrics(metrics).withLatencyMs(raw.getLatencyMs() == null ? elapsed : raw.getLatencyMs()));
   }



   /*******************************************************************************
    ** Reaping never changes counters, rollups or alerts.
    *******************************************************************************/
   private void closeAbandoned(Instant now) throws QException
   {
      synchronized(HealthStore.lockFor(qInstance))
      {
         for(QRecord result : HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter()
            .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, "RUNNING"))
            .withCriteria(new QFilterCriteria("leaseExpiresAt", QCriteriaOperator.LESS_THAN, now))))
         {
            HealthStore.update(HealthMetaDataProvider.RESULT_TABLE, new QRecord().withValue("id", result.getValue("id"))
               .withValue("status", "UNKNOWN").withValue("message", "abandoned by pod " + HealthRedactor.redact(result.getValueString("runnerPod")))
               .withValue("finishedAt", now).withValue("isTransition", false));
         }
      }
   }



   /*******************************************************************************
    ** Recompute from current registered checks; INFO DOWN caps at DEGRADED.
    *******************************************************************************/
   private void rollup(String component, Instant now) throws QException
   {
      List<QRecord> checks = HealthStore.query(HealthMetaDataProvider.CHECK_TABLE, new QQueryFilter()
         .withCriteria(new QFilterCriteria("component", QCriteriaOperator.EQUALS, component))
         .withCriteria(new QFilterCriteria("registered", QCriteriaOperator.EQUALS, true)));
      Map<HealthStatus, Integer> counts = new EnumMap<>(HealthStatus.class);
      HealthStatus status = null;
      Serializable worstId = null;
      for(QRecord check : checks)
      {
         HealthStatus checkStatus = HealthTransitionEvaluator.status(check.getValueString("currentStatus"));
         if(checkStatus == HealthStatus.DOWN && "INFO".equals(check.getValueString("severity")))
         {
            checkStatus = HealthStatus.DEGRADED;
         }
         counts.merge(checkStatus, 1, Integer::sum);
         HealthStatus aggregate = status == null ? checkStatus : HealthStatus.aggregate(status, checkStatus);
         if(aggregate != status)
         {
            worstId = check.getValue("id");
         }
         status = aggregate;
      }
      QRecord existing = HealthStore.find(HealthMetaDataProvider.COMPONENT_TABLE, "name", component);
      QRecord row = new QRecord().withValue("name", component).withValue("label", component)
         .withValue("status", (status == null ? HealthStatus.UNKNOWN : status).name()).withValue("worstCheckId", worstId).withValue("updatedAt", now);
      for(HealthStatus value : HealthStatus.values())
      {
         String field = "checks" + value.name().charAt(0) + value.name().substring(1).toLowerCase(Locale.ROOT);
         row.withValue(field, counts.getOrDefault(value, 0));
      }
      if(existing == null)
      {
         try
         {
            HealthStore.insert(HealthMetaDataProvider.COMPONENT_TABLE, row.withValue("since", now));
         }
         catch(QException e)
         {
            if(HealthStore.find(HealthMetaDataProvider.COMPONENT_TABLE, "name", component) == null)
            {
               throw e;
            }
            // Another pod created this component. Recompute against fresh check rows.
            rollup(component, now);
         }
      }
      else
      {
         if(!Objects.equals(existing.getValueString("status"), row.getValueString("status")))
         {
            row.withValue("since", now);
         }
         HealthStore.update(HealthMetaDataProvider.COMPONENT_TABLE, row.withValue("id", existing.getValue("id")));
      }
   }



   /*******************************************************************************
    ** Null editable values inherit metadata defaults.
    *******************************************************************************/
   private int effective(QRecord check, String field, int fallback)
   {
      Integer value = check.getValueInteger(field);
      return (value == null ? fallback : value);
   }



   /*******************************************************************************
    ** Health storage failures are rate-limited and never log raw provider data.
    *******************************************************************************/
   private void storageWarning(Exception exception)
   {
      Instant now = clock.instant();
      if(lastStorageWarning == null || !now.isBefore(lastStorageWarning.plusSeconds(60)))
      {
         LOG.warn("Health storage is unavailable", logPair("failure", HealthRedactor.redactThrowable(exception)));
         lastStorageWarning = now;
      }
   }



   /*******************************************************************************
    ** A failed health database must not leave its readiness cache falsely UP.
    *******************************************************************************/
   private void probeDatabasesWithoutStorage()
   {
      for(HealthCheckMetaData definition : definitions.values())
      {
         if(definition.getCategory() != HealthCategory.DATABASE || !capacity.tryAcquire())
         {
            continue;
         }
         FutureTask<HealthCheckOutcome> future = null;
         try
         {
            future = submitProbe(definition, new HealthCheckInput().withQInstance(qInstance)
               .withDeadline(clock.instant().plusMillis(definition.getTimeoutMs())).withAttempt(1));
            HealthCheckOutcome outcome = future.get(definition.getTimeoutMs(), TimeUnit.MILLISECONDS);
            cacheOutcome(definition.getName(), sanitize(outcome == null ? HealthCheckOutcome.unknown("Check returned no outcome") : outcome, 0));
         }
         catch(TimeoutException e)
         {
            future.cancel(true);
            cacheOutcome(definition.getName(), HealthCheckOutcome.down("timed out after " + definition.getTimeoutMs() + "ms"));
         }
         catch(InterruptedException e)
         {
            if(future != null)
            {
               future.cancel(true);
            }
            Thread.currentThread().interrupt();
            return;
         }
         catch(Exception e)
         {
            cacheOutcome(definition.getName(), HealthCheckOutcome.down(HealthRedactor.redactThrowable(e instanceof ExecutionException ? e.getCause() : e)));
         }
         finally
         {
            if(future == null)
            {
               capacity.release();
            }
         }
      }
   }



   /*******************************************************************************
    ** Execution state is private to one claim and never persisted as configuration.
    *******************************************************************************/
   private record Execution(QRecord check, QRecord result, HealthCheckMetaData definition, FutureTask<HealthCheckOutcome> future,
      long started, int interval, int timeout, long slot)
   {
   }
}
