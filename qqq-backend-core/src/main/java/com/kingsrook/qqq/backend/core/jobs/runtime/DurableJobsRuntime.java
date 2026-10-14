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


package com.kingsrook.qqq.backend.core.jobs.runtime;


import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.JobSessions;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkOutcome;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.plans.ChunkContext;
import com.kingsrook.qqq.backend.core.jobs.plans.JobInterruptedException;
import com.kingsrook.qqq.backend.core.jobs.plans.JobPlans;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.jobs.store.LeaseLostException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_WORKER;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The per-pod durable jobs runtime: worker threads that claim and run chunks,
 ** a heartbeat per running chunk (every ttl/3), the lease reaper (every pod,
 ** every reaperIntervalSeconds), stall detection, and graceful drain.
 **
 ** Chunks run on this runtime's own bounded pool (workerThreads), never on
 ** AsyncJobManager's pool. workerThreads = 0 makes an API-only pod: it still
 ** reaps, but claims nothing.
 **
 ** <pre>DurableJobsRuntime.start(qInstance);   // at server start
 ** DurableJobsRuntime.stop(qInstance, Duration.ofSeconds(30));   // on SIGTERM</pre>
 *******************************************************************************/
public class DurableJobsRuntime
{
   private static final QLogger LOG = QLogger.getLogger(DurableJobsRuntime.class);

   private static final Map<QInstance, DurableJobsRuntime> RUNTIMES = new WeakHashMap<>();

   private static final int DRAIN_ON_SHUTDOWN_SECONDS = 20;

   private final QInstance           instance;
   private final DurableJobsMetaData settings;
   private final DurableJobStore     store;
   private final String              podId;

   private final AtomicBoolean draining = new AtomicBoolean(false);
   private final AtomicInteger busy     = new AtomicInteger(0);

   private ExecutorService          workers;
   private Thread                   shutdownHook;
   private ScheduledExecutorService scheduler;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   DurableJobsRuntime(QInstance instance, DurableJobsMetaData settings)
   {
      this.instance = instance;
      this.settings = settings;
      this.store = new DurableJobStore(settings);
      this.podId = StringUtils.hasContent(settings.getPodId()) ? settings.getPodId() : defaultPodId();
   }



   /*******************************************************************************
    ** Start the runtime for an instance (no-op if durable jobs are off or it is
    ** already running). Returns the runtime, or null when off.
    *******************************************************************************/
   public static synchronized DurableJobsRuntime start(QInstance instance)
   {
      DurableJobsMetaData settings = DurableJobsMetaData.of(instance);
      if(settings == null)
      {
         return (null);
      }
      DurableJobsRuntime runtime = RUNTIMES.get(instance);
      if(runtime == null)
      {
         runtime = new DurableJobsRuntime(instance, settings);
         runtime.startThreads();
         RUNTIMES.put(instance, runtime);

         ///////////////////////////////////////////////////////////////////////
         // SIGTERM (a redeploy) drains: running chunks release their leases //
         // at their next safe point, so another pod continues them at once.  //
         ///////////////////////////////////////////////////////////////////////
         runtime.shutdownHook = new Thread(() -> stop(instance, Duration.ofSeconds(DRAIN_ON_SHUTDOWN_SECONDS)), "qqq-jobs-drain-" + runtime.podId);
         Runtime.getRuntime().addShutdownHook(runtime.shutdownHook);
      }
      return (runtime);
   }



   /*******************************************************************************
    ** Stop an instance's runtime: stop claiming, let running chunks reach a safe
    ** point and release their leases (no attempt counted), wait up to the given
    ** time, then stop. Chunks still running after that are reclaimed by another
    ** pod when their lease expires.
    *******************************************************************************/
   public static void stop(QInstance instance, Duration drainTimeout)
   {
      DurableJobsRuntime runtime;
      synchronized(DurableJobsRuntime.class)
      {
         runtime = RUNTIMES.remove(instance);
      }
      if(runtime != null)
      {
         runtime.drainAndStop(drainTimeout);
         try
         {
            if(runtime.shutdownHook != null && Thread.currentThread() != runtime.shutdownHook)
            {
               Runtime.getRuntime().removeShutdownHook(runtime.shutdownHook);
            }
         }
         catch(IllegalStateException e)
         {
            /////////////////////////////////////////////
            // the JVM is already shutting down - fine //
            /////////////////////////////////////////////
         }
      }
   }



   /*******************************************************************************
    ** The running runtime for an instance, or null.
    *******************************************************************************/
   public static synchronized DurableJobsRuntime get(QInstance instance)
   {
      return (RUNTIMES.get(instance));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void startThreads()
   {
      int threads = settings.getWorkerThreads() == null ? 0 : settings.getWorkerThreads();
      scheduler = Executors.newScheduledThreadPool(2, runnable -> daemon(runnable, "qqq-jobs-sched-" + podId));
      workers = Executors.newFixedThreadPool(Math.max(1, threads), runnable -> daemon(runnable, "qqq-jobs-worker-" + podId));

      withSystemContext(this::registerWorkerRow);
      scheduler.scheduleWithFixedDelay(() -> withSystemContext(this::reaperPass), jitterMillis(1000), settings.getReaperIntervalSeconds() * 1000L, TimeUnit.MILLISECONDS);
      scheduler.scheduleWithFixedDelay(() -> withSystemContext(this::workerRowHeartbeat), 15, 15, TimeUnit.SECONDS);

      for(int i = 0; i < threads; i++)
      {
         workers.submit(this::workerLoop);
      }
      LOG.info("Durable jobs runtime started", logPair("podId", podId), logPair("workerThreads", threads));
   }



   /*******************************************************************************
    ** One worker thread: claim, run, repeat; sleep (with jitter) when idle.
    *******************************************************************************/
   private void workerLoop()
   {
      QContext.init(instance, new QSystemUserSession());
      while(!draining.get() && !Thread.currentThread().isInterrupted())
      {
         try
         {
            Optional<JobLease> lease = new JobClaimer(store, podId).claimNext();
            if(lease.isPresent())
            {
               busy.incrementAndGet();
               try
               {
                  runLease(lease.get());
               }
               finally
               {
                  busy.decrementAndGet();
               }
            }
            else
            {
               Thread.sleep(settings.getPollIntervalMillis() + jitterMillis(settings.getPollIntervalMillis() / 2));
            }
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
         }
         catch(Exception e)
         {
            LOG.warn("Durable jobs worker error", e, logPair("podId", podId));
            sleepQuietly(settings.getPollIntervalMillis());
         }
      }
   }



   /*******************************************************************************
    ** Run one leased chunk as the job's user, then record its outcome.
    *******************************************************************************/
   void runLease(JobLease lease) throws QException
   {
      QRecord      job     = store.getJob(lease.jobId());
      QRecord      chunk   = store.getChunk(lease.chunkId());
      ChunkContext context = new ChunkContext(job, chunk, lease, store, draining::get);

      long                 beat      = Math.max(1, settings.getLeaseTtlSeconds() * 1000L / 3);
      ScheduledFuture<?>   heartbeat = scheduler.scheduleWithFixedDelay(() -> withSystemContext(() -> heartbeat(context)), beat, beat, TimeUnit.MILLISECONDS);
      try
      {
         QSession session;
         try
         {
            session = JobSessions.forJob(instance, job);
         }
         catch(QPermissionDeniedException | QAuthenticationException e)
         {
            fail(lease, "The job's owner can no longer run it: " + e.getMessage(), false);
            return;
         }

         ChunkOutcome[] outcome = new ChunkOutcome[1];
         QContext.withTemporaryContext(new CapturedContext(instance, session), () -> outcome[0] = JobPlans.forJob(job).runChunk(context));

         store.completeChunk(lease, outcome[0], null);
         if(outcome[0].outputsJson() != null)
         {
            store.updateJob(lease.jobId(), Map.of("outputStorageJson", outcome[0].outputsJson()));
         }
      }
      catch(Exception e)
      {
         handleChunkException(lease, context, e);
      }
      finally
      {
         heartbeat.cancel(false);
         try
         {
            new JobFinalizer(store).evaluate(lease.jobId());
         }
         catch(Exception e)
         {
            LOG.warn("Could not evaluate a job after its chunk", e, logPair("jobId", lease.jobId()));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void handleChunkException(JobLease lease, ChunkContext context, Exception e) throws QException
   {
      JobInterruptedException interrupted = JobInterruptedException.find(e);
      if(interrupted != null)
      {
         switch(interrupted.getReason())
         {
            case PAUSE -> store.releaseChunk(lease, ChunkStatus.PENDING);
            case CANCEL -> store.releaseChunk(lease, ChunkStatus.CANCELLED);
            case DRAIN ->
            {
               if(store.releaseChunk(lease, ChunkStatus.PENDING))
               {
                  store.recordEvent(lease.jobId(), lease.chunkId(), JobEventType.RELEASED, null, null, podId, "Chunk " + lease.seq() + " released by " + podId + " (draining); another pod continues it.");
               }
            }
            default -> LOG.info("Job chunk stopped after losing its lease", logPair("jobId", lease.jobId()), logPair("chunkId", lease.chunkId()));
         }
         return;
      }

      if(ExceptionUtils.findClassInRootChain(e, LeaseLostException.class) != null || context.isLeaseLost())
      {
         LOG.warn("Job chunk lost its lease; its work is rolled back where it shared the lease transaction", logPair("jobId", lease.jobId()), logPair("chunkId", lease.chunkId()));
         return;
      }

      boolean retryable = ExceptionUtils.findClassInRootChain(e, QPermissionDeniedException.class) == null;
      LOG.info("Job chunk failed", e, logPair("jobId", lease.jobId()), logPair("chunkId", lease.chunkId()), logPair("retryable", retryable));
      fail(lease, ExceptionUtils.concatenateMessagesFromChain(e), retryable);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void fail(JobLease lease, String message, boolean retryable) throws QException
   {
      ChunkStatus status = store.failChunk(lease, message, retryable);
      if(status == ChunkStatus.RETRY)
      {
         store.recordEvent(lease.jobId(), lease.chunkId(), JobEventType.RETRIED, null, null, podId, "Chunk " + lease.seq() + " attempt " + lease.attempt() + " failed; it will be retried: " + StringUtils.safeTruncate(message, 500));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void heartbeat(ChunkContext context)
   {
      try
      {
         if(!store.heartbeat(context.getLease(), settings.getLeaseTtlSeconds()))
         {
            context.markLeaseLost();
         }
      }
      catch(Exception e)
      {
         LOG.warn("Job chunk heartbeat failed", e, logPair("chunkId", context.getLease().chunkId()));
      }
   }



   /*******************************************************************************
    ** Reclaim expired leases, re-evaluate their jobs, check for stalls.
    *******************************************************************************/
   void reaperPass()
   {
      try
      {
         Set<Integer> jobIds = new HashSet<>();
         for(QRecord chunk : store.reapExpired(podId))
         {
            jobIds.add(chunk.getValueInteger("jobId"));
         }
         for(Integer jobId : jobIds)
         {
            new JobFinalizer(store).evaluate(jobId);
         }
         QAlerts.heartbeat(JobSignals.HEARTBEAT_REAPER);
         detectStalls();
      }
      catch(Exception e)
      {
         LOG.warn("Durable jobs reaper pass failed", e, logPair("podId", podId));
      }
   }



   /*******************************************************************************
    ** RUNNING with no progress for stallMinutes, or QUEUED past
    ** queueStallMinutes, raises jobs.job_stalled (deduped per job by QAlerts).
    *******************************************************************************/
   private void detectStalls() throws QException
   {
      Instant now = store.now();
      for(QRecord job : store.query(TABLE_JOB, new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, JobStatus.RUNNING.name()))
         .withCriteria(new QFilterCriteria("lastProgressAt", QCriteriaOperator.LESS_THAN, now.minus(Duration.ofMinutes(settings.getStallMinutes()))))
         .withLimit(50)))
      {
         JobSignals.stalled(job, "No progress for " + settings.getStallMinutes() + " minutes.");
      }
      for(QRecord job : store.query(TABLE_JOB, new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, JobStatus.QUEUED.name()))
         .withCriteria(new QFilterCriteria("queuedAt", QCriteriaOperator.LESS_THAN, now.minus(Duration.ofMinutes(settings.getQueueStallMinutes()))))
         .withLimit(50)))
      {
         JobSignals.stalled(job, "Queued for over " + settings.getQueueStallMinutes() + " minutes" + (job.getValueString("statusReason") == null ? "." : ": " + job.getValueString("statusReason")));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void drainAndStop(Duration drainTimeout)
   {
      draining.set(true);
      withSystemContext(() -> updateWorkerRow(true));
      workers.shutdown();
      try
      {
         if(!workers.awaitTermination(drainTimeout.toMillis(), TimeUnit.MILLISECONDS))
         {
            LOG.warn("Durable jobs runtime did not drain in time; leases will be reclaimed", logPair("podId", podId), logPair("busy", busy.get()));
            workers.shutdownNow();
         }
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      scheduler.shutdownNow();
      LOG.info("Durable jobs runtime stopped", logPair("podId", podId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void registerWorkerRow() throws QException
   {
      Instant now = Instant.now();
      QRecord existing = findWorkerRow();
      if(existing == null)
      {
         SystemScope.call(() -> new InsertAction().execute(new InsertInput(TABLE_WORKER).withRecord(new QRecord()
            .withValue("podId", podId).withValue("startedAt", now).withValue("heartbeatAt", now)
            .withValue("threads", settings.getWorkerThreads()).withValue("draining", false)
            .withValue("version", Objects.requireNonNullElse(DurableJobsRuntime.class.getPackage().getImplementationVersion(), "dev")))));
      }
      else
      {
         SystemScope.call(() -> new UpdateAction().execute(new UpdateInput(TABLE_WORKER).withRecord(new QRecord()
            .withValue("id", existing.getValue("id")).withValue("startedAt", now).withValue("heartbeatAt", now)
            .withValue("threads", settings.getWorkerThreads()).withValue("draining", false))));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void workerRowHeartbeat() throws QException
   {
      updateWorkerRow(draining.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void updateWorkerRow(boolean isDraining) throws QException
   {
      QRecord existing = findWorkerRow();
      if(existing != null)
      {
         SystemScope.call(() -> new UpdateAction().execute(new UpdateInput(TABLE_WORKER).withRecord(new QRecord()
            .withValue("id", existing.getValue("id")).withValue("heartbeatAt", Instant.now()).withValue("draining", isDraining))));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord findWorkerRow() throws QException
   {
      List<QRecord> rows = store.query(TABLE_WORKER, new QQueryFilter(new QFilterCriteria("podId", QCriteriaOperator.EQUALS, podId)));
      return (rows.isEmpty() ? null : rows.get(0));
   }



   /*******************************************************************************
    ** A body that may throw.
    *******************************************************************************/
   @FunctionalInterface
   private interface Body
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      void run() throws Exception;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void withSystemContext(Body body)
   {
      try
      {
         QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession()), body::run);
      }
      catch(Exception e)
      {
         LOG.warn("Durable jobs background task failed", e, logPair("podId", podId));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Thread daemon(Runnable runnable, String name)
   {
      Thread thread = new Thread(runnable, name);
      thread.setDaemon(true);
      return (thread);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static long jitterMillis(long max)
   {
      return (max <= 0 ? 0 : ThreadLocalRandom.current().nextLong(max));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void sleepQuietly(long millis)
   {
      try
      {
         Thread.sleep(millis);
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String defaultPodId()
   {
      String host;
      try
      {
         host = InetAddress.getLocalHost().getHostName();
      }
      catch(Exception e)
      {
         host = "pod";
      }
      return (host + "-" + UUID.randomUUID().toString().substring(0, 8));
   }



   /*******************************************************************************
    ** Getter for podId
    *******************************************************************************/
   public String getPodId()
   {
      return (this.podId);
   }



   /*******************************************************************************
    ** Getter for store
    *******************************************************************************/
   public DurableJobStore getStore()
   {
      return (this.store);
   }

}
