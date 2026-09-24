/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.async;


import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessStateAccess;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.StateProviderInterface;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.logging.log4j.Level;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Class to manage running asynchronous actions, and working with their statuses.
 *******************************************************************************/
public class AsyncJobManager
{
   private static final QLogger LOG = QLogger.getLogger(AsyncJobManager.class);

   /////////////////////////////////////////////////////////////////////////////
   // we would probably use Executors.newCachedThreadPool() - but - it has no //
   // maxPoolSize...  we think some limit is good, so that at a large number  //
   // of attempted concurrent jobs we'll have new jobs block, rather than     //
   // exhausting all server resources and locking up "everything"             //
   // also, it seems like keeping a handful of core-threads around is very    //
   // little actual waste, and better than ever wasting time starting a new   //
   // one, which we know we'll often be doing.                                //
   /////////////////////////////////////////////////////////////////////////////
   private static Integer         CORE_THREADS    = 8;
   private static Integer         MAX_THREADS     = 500;
   private static ExecutorService executorService = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new PrefixedDefaultThreadFactory(AsyncJobManager.class));


   private String forcedJobUUID = null;
   private String processUUID;



   /*******************************************************************************
    ** Start a job - if it finishes within the specified timeout, get its results,
    ** else, get back an exception with the job id.
    *******************************************************************************/
   public <T extends Serializable> T startJob(long timeout, TimeUnit timeUnit, AsyncJob<T> asyncJob) throws JobGoingAsyncException, QException
   {
      return (startJob("Anonymous", timeout, timeUnit, asyncJob));
   }



   /*******************************************************************************
    ** Start a job - if it finishes within the specified timeout, get its results,
    ** else, get back an exception with the job id.
    *******************************************************************************/
   public <T extends Serializable> T startJob(String jobName, long timeout, TimeUnit timeUnit, AsyncJob<T> asyncJob) throws JobGoingAsyncException, QException
   {
      UUID                jobUUID             = StringUtils.hasContent(forcedJobUUID) ? UUID.fromString(forcedJobUUID) : UUID.randomUUID();
      UUIDAndTypeStateKey uuidAndTypeStateKey = new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS);
      AsyncJobStatus      asyncJobStatus      = new AsyncJobStatus();
      asyncJobStatus.setState(AsyncJobState.RUNNING);
      if(processUUID != null)
      {
         asyncJobStatus.setStateAccess(ProcessStateAccess.capture(jobName, processUUID));
      }
      getStateProvider().put(uuidAndTypeStateKey, asyncJobStatus);

      try
      {
         CapturedContext capturedContext = QContext.capture();
         CompletableFuture<T> future = CompletableFuture.supplyAsync(() ->
         {
            QContext.init(capturedContext);
            return (runAsyncJob(jobName, asyncJob, uuidAndTypeStateKey, asyncJobStatus));
         }, executorService);

         if(timeout == 0)
         {
            throw (new JobGoingAsyncException(uuidAndTypeStateKey.getUuid().toString()));
         }

         T result = future.get(timeout, timeUnit);
         return (result);
      }
      catch(InterruptedException | ExecutionException e)
      {
         throw (new QException("Error running job", e));
      }
      catch(TimeoutException e)
      {
         LOG.debug("Job going async " + uuidAndTypeStateKey.getUuid());
         throw (new JobGoingAsyncException(uuidAndTypeStateKey.getUuid().toString()));
      }
   }



   /*******************************************************************************
    ** Start a job, and always, just get back the job UUID.
    *******************************************************************************/
   public <T extends Serializable> String startJob(AsyncJob<T> asyncJob) throws QException
   {
      return (startJob("Anonymous", asyncJob));
   }



   /*******************************************************************************
    ** Start a job, and always, just get back the job UUID.
    *******************************************************************************/
   public <T extends Serializable> String startJob(String jobName, AsyncJob<T> asyncJob) throws QException
   {
      try
      {
         startJob(jobName, 0, TimeUnit.MILLISECONDS, asyncJob);
         throw (new QException("Job was expected to go asynchronous, but did not"));
      }
      catch(JobGoingAsyncException jgae)
      {
         return (jgae.getJobUUID());
      }
   }



   /*******************************************************************************
    ** run the job.
    *******************************************************************************/
   private <T extends Serializable> T runAsyncJob(String jobName, AsyncJob<T> asyncJob, UUIDAndTypeStateKey uuidAndTypeStateKey, AsyncJobStatus asyncJobStatus)
   {
      String originalThreadName = Thread.currentThread().getName();
      Thread.currentThread().setName("Job:" + jobName);
      try
      {
         LOG.debug("Starting job " + uuidAndTypeStateKey.getUuid());
         T result = asyncJob.run(new AsyncJobCallback(uuidAndTypeStateKey.getUuid(), asyncJobStatus));
         asyncJobStatus.setState(AsyncJobState.COMPLETE);
         getStateProvider().put(uuidAndTypeStateKey, asyncJobStatus);
         LOG.debug("Completed job " + uuidAndTypeStateKey.getUuid());
         return (result);
      }
      catch(Throwable t)
      {
         asyncJobStatus.setState(AsyncJobState.ERROR);
         if(t instanceof Exception e)
         {
            asyncJobStatus.setCaughtException(e);
         }
         else
         {
            asyncJobStatus.setCaughtException(new QException("Caught throwable", t));
         }
         getStateProvider().put(uuidAndTypeStateKey, asyncJobStatus);

         //////////////////////////////////////////////////////
         // if user facing, just log an info, warn otherwise //
         //////////////////////////////////////////////////////
         LOG.log((t instanceof QUserFacingException) ? Level.INFO : Level.WARN, "Job ended with an exception", t, logPair("jobId", uuidAndTypeStateKey.getUuid()));
         throw (new CompletionException(t));
      }
      finally
      {
         Thread.currentThread().setName(originalThreadName);
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Get the status of the job identified by the given UUID.
    **
    *******************************************************************************/
   public Optional<AsyncJobStatus> getJobStatus(String uuid)
   {
      UUIDAndTypeStateKey uuidAndTypeStateKey = new UUIDAndTypeStateKey(UUID.fromString(uuid), StateType.ASYNC_JOB_STATUS);
      return (getStateProvider().get(AsyncJobStatus.class, uuidAndTypeStateKey));
   }



   /*******************************************************************************
    ** Bind a user-facing job to the process whose state it produces.
    *******************************************************************************/
   public AsyncJobManager withProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
      return this;
   }



   /*******************************************************************************
    ** Check ownership even while the process has not produced any state yet.
    *******************************************************************************/
   public Optional<AsyncJobStatus> getJobStatusForUser(String uuid, String processName, String processUUID) throws QException
   {
      Optional<AsyncJobStatus> status = getJobStatus(uuid);
      if(status.isPresent())
      {
         ProcessStateAccess.requireAccess(status.get().getStateAccess(), processName, processUUID);
      }
      return status;
   }



   /*******************************************************************************
    ** Load an instance of the appropriate state provider
    **
    *******************************************************************************/
   static StateProviderInterface getStateProvider()
   {
      // TODO - read this from somewhere in meta data eh?
      return InMemoryStateProvider.getInstance();

      // todo - by using JSON serialization internally, this makes stupidly large payloads and crashes things.
      // return TempFileStateProvider.getInstance();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void cancelJob(String jobUUID)
   {
      Optional<AsyncJobStatus> jobStatus = getJobStatus(jobUUID);
      jobStatus.ifPresent(asyncJobStatus -> asyncJobStatus.setCancelRequested(true));
   }



   /*******************************************************************************
    ** Getter for forcedJobUUID
    *******************************************************************************/
   public String getForcedJobUUID()
   {
      return (this.forcedJobUUID);
   }



   /*******************************************************************************
    ** Setter for forcedJobUUID
    *******************************************************************************/
   public void setForcedJobUUID(String forcedJobUUID)
   {
      this.forcedJobUUID = forcedJobUUID;
   }



   /*******************************************************************************
    ** Fluent setter for forcedJobUUID
    *******************************************************************************/
   public AsyncJobManager withForcedJobUUID(String forcedJobUUID)
   {
      this.forcedJobUUID = forcedJobUUID;
      return (this);
   }

}
