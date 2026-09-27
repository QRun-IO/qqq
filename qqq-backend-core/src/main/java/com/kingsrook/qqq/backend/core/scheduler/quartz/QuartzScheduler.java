/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler.quartz;


import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.QSchedulerInterface;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.BasicSchedulableIdentity;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.SchedulableIdentity;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.AnyKey;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Singleton to provide access between QQQ and the quartz Scheduler system.
 *******************************************************************************/
public class QuartzScheduler implements QSchedulerInterface
{
   private static final QLogger LOG = QLogger.getLogger(QuartzScheduler.class);

   private static QuartzScheduler quartzScheduler = null;

   private final QInstance          qInstance;
   private       String             schedulerName;
   private       Supplier<QSession> sessionSupplier;

   private Scheduler scheduler;


   /////////////////////////////////////////////////////////////////////////////////////////
   // create memoization objects for some quartz-query functions, that we'll only want to //
   // use during our setup routine, when we'd query it many times over and over again.    //
   // So default to a timeout of 0 (effectively disabling memoization).  then in the      //
   // start-of-setup and end-of-setup methods, temporarily increase, then re-decrease     //
   /////////////////////////////////////////////////////////////////////////////////////////
   private Memoization<AnyKey, List<String>> jobGroupNamesMemoization = new Memoization<AnyKey, List<String>>()
      .withTimeout(Duration.of(0, ChronoUnit.SECONDS));

   private Memoization<String, Set<JobKey>> jobKeyNamesMemoization = new Memoization<String, Set<JobKey>>()
      .withTimeout(Duration.of(0, ChronoUnit.SECONDS));

   private Memoization<AnyKey, List<QuartzJobAndTriggerWrapper>> queryQuartzMemoization = new Memoization<AnyKey, List<QuartzJobAndTriggerWrapper>>()
      .withTimeout(Duration.of(0, ChronoUnit.SECONDS));

   private List<Memoization<?, ?>> allMemoizations = List.of(jobGroupNamesMemoization, jobKeyNamesMemoization, queryQuartzMemoization);

   ///////////////////////////////////////////////////////////////////////////////
   // vars used during the setup routine, to figure out what jobs need deleted. //
   ///////////////////////////////////////////////////////////////////////////////
   private boolean                          insideSetup                 = false;
   private List<QuartzJobAndTriggerWrapper> scheduledJobsAtStartOfSetup = new ArrayList<>();
   private List<QuartzJobAndTriggerWrapper> scheduledJobsAtEndOfSetup   = new ArrayList<>();

   /////////////////////////////////////////////////////////////////////////////////
   // track if the instance is past the server's startup routine.                 //
   // for quartz - we'll use this to know if we're allowed to schedule jobs.      //
   // that is - during server startup, we don't want to the schedule & unschedule //
   // routine, which could potentially have serve concurrency problems            //
   /////////////////////////////////////////////////////////////////////////////////
   private boolean pastStartup = false;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   private QuartzScheduler(QInstance qInstance, String schedulerName, Supplier<QSession> sessionSupplier)
   {
      this.qInstance = qInstance;
      this.schedulerName = schedulerName;
      this.sessionSupplier = sessionSupplier;
   }



   /*******************************************************************************
    ** Singleton initiator - e.g., must be called to initially initialize the singleton
    ** before anyone else calls getInstance (they'll get an error if they call that first).
    *******************************************************************************/
   public static QuartzScheduler initInstance(QInstance qInstance, String schedulerName, Properties quartzProperties, Supplier<QSession> sessionSupplier) throws SchedulerException
   {
      if(quartzScheduler == null)
      {
         quartzScheduler = new QuartzScheduler(qInstance, schedulerName, sessionSupplier);

         ///////////////////////////////////////////////////////////
         // Grab the Scheduler instance from the Factory          //
         // initialize it with the properties we took in as input //
         ///////////////////////////////////////////////////////////
         StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
         schedulerFactory.initialize(quartzProperties);
         quartzScheduler.scheduler = schedulerFactory.getScheduler();
      }
      return (quartzScheduler);
   }



   /*******************************************************************************
    ** Singleton accessor
    *******************************************************************************/
   public static QuartzScheduler getInstance()
   {
      if(quartzScheduler == null)
      {
         throw (new IllegalStateException("QuartzScheduler singleton has not been init'ed (call initInstance)."));
      }
      return (quartzScheduler);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getSchedulerName()
   {
      return (schedulerName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void start()
   {
      this.pastStartup = true;

      try
      {
         //////////////////////
         // and start it off //
         //////////////////////
         scheduler.start();
      }
      catch(Exception e)
      {
         LOG.error("Error starting quartz scheduler", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void doNotStart()
   {
      this.pastStartup = true;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void stop()
   {
      try
      {
         scheduler.shutdown(true);
      }
      catch(SchedulerException e)
      {
         LOG.error("Error shutting down (stopping) quartz scheduler", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void stopAsync()
   {
      try
      {
         scheduler.shutdown(false);
      }
      catch(SchedulerException e)
      {
         LOG.error("Error shutting down (stopping) quartz scheduler", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setupSchedulable(SchedulableIdentity schedulableIdentity, SchedulableType schedulableType, Map<String, Serializable> parameters, QScheduleMetaData schedule, boolean allowedToStart)
   {
      ////////////////////////////////////////////////////////////////////////////
      // only actually schedule things if we're past the server startup routine //
      ////////////////////////////////////////////////////////////////////////////
      if(!pastStartup)
      {
         return;
      }

      Map<String, Object> jobData = new HashMap<>();
      jobData.put("params", parameters);
      jobData.put("type", schedulableType.getName());

      scheduleJob(schedulableIdentity, schedulableType.getName(), QuartzJobRunner.class, jobData, schedule, allowedToStart);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void startOfSetupSchedules()
   {
      ////////////////////////////////////////////////////////////////////////////
      // only actually schedule things if we're past the server startup routine //
      ////////////////////////////////////////////////////////////////////////////
      if(!pastStartup)
      {
         return;
      }

      this.insideSetup = true;
      this.allMemoizations.forEach(m -> m.setTimeout(Duration.ofSeconds(5)));

      try
      {
         this.scheduledJobsAtStartOfSetup = queryQuartz();
         this.scheduledJobsAtEndOfSetup = new ArrayList<>();
      }
      catch(Exception e)
      {
         LOG.warn("Error querying quartz for the currently scheduled jobs during startup - will not be able to delete no-longer-needed jobs!", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void endOfSetupSchedules()
   {
      ////////////////////////////////////////////////////////////////////////////
      // only actually schedule things if we're past the server startup routine //
      ////////////////////////////////////////////////////////////////////////////
      if(!pastStartup)
      {
         return;
      }

      this.insideSetup = false;
      this.allMemoizations.forEach(m -> m.setTimeout(Duration.ofSeconds(0)));

      if(this.scheduledJobsAtStartOfSetup == null)
      {
         return;
      }

      try
      {
         Set<JobKey> startJobKeys = this.scheduledJobsAtStartOfSetup.stream().map(w -> w.jobDetail().getKey()).collect(Collectors.toSet());
         Set<JobKey> endJobKeys   = this.scheduledJobsAtEndOfSetup.stream().map(w -> w.jobDetail().getKey()).collect(Collectors.toSet());

         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // remove all 'end' keys from the set of start keys.  any left-over start-keys need to be deleted. //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         startJobKeys.removeAll(endJobKeys);
         for(JobKey jobKey : startJobKeys)
         {
            LOG.info("Deleting job that had previously been scheduled, but doesn't appear to be any more", logPair("jobKey", jobKey));
            deleteJob(jobKey);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error trying to clean up no-longer-needed jobs at end of scheduler setup", e);
      }

      ////////////////////////////////////////////////////
      // reset these lists, no need to keep them around //
      ////////////////////////////////////////////////////
      this.scheduledJobsAtStartOfSetup = null;
      this.scheduledJobsAtEndOfSetup = null;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean scheduleJob(SchedulableIdentity schedulableIdentity, String groupName, Class<? extends Job> jobClass, Map<String, Object> jobData, QScheduleMetaData scheduleMetaData, boolean allowedToStart)
   {
      try
      {
         /////////////////////////
         // Define job instance //
         /////////////////////////
         JobKey jobKey = new JobKey(schedulableIdentity.getIdentity(), groupName);
         JobDetail jobDetail = JobBuilder.newJob(jobClass)
            .withIdentity(jobKey)
            .withDescription(schedulableIdentity.getDescription())
            .storeDurably()
            .requestRecovery() // todo - our frequent repeaters, maybe nice to say false here
            .build();

         jobDetail.getJobDataMap().putAll(jobData);

         /////////////////////////////////////////////////////////
         // map the qqq schedule meta data to a quartz schedule //
         /////////////////////////////////////////////////////////
         ScheduleBuilder<?> scheduleBuilder;
         if(scheduleMetaData.isCron())
         {
            CronExpression cronExpression = new CronExpression(scheduleMetaData.getCronExpression());
            cronExpression.setTimeZone(TimeZone.getTimeZone(scheduleMetaData.getCronTimeZoneId()));
            scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
         }
         else
         {
            long intervalMillis = Objects.requireNonNullElseGet(scheduleMetaData.getRepeatMillis(), () -> scheduleMetaData.getRepeatSeconds() * 1000);
            scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
               .withIntervalInMilliseconds(intervalMillis)
               .repeatForever();
         }

         Date startAt = new Date();
         if(scheduleMetaData.getInitialDelayMillis() != null)
         {
            startAt.setTime(startAt.getTime() + scheduleMetaData.getInitialDelayMillis());
         }
         else if(scheduleMetaData.getInitialDelaySeconds() != null)
         {
            startAt.setTime(startAt.getTime() + scheduleMetaData.getInitialDelaySeconds() * 1000);
         }
         else
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            // by default, put a 3-second delay on everything we schedule                                       //
            // this gives us a chance to re-pause if the job was previously paused, but then we re-schedule it. //
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            startAt.setTime(startAt.getTime() + 3000);
         }

         ///////////////////////////////////////
         // Define a Trigger for the schedule //
         ///////////////////////////////////////
         Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(new TriggerKey(schedulableIdentity.getIdentity(), groupName))
            .withDescription(schedulableIdentity.getDescription() + " - " + getScheduleDescriptionForTrigger(scheduleMetaData))
            .forJob(jobKey)
            .withSchedule(scheduleBuilder)
            // .startAt(startAt)
            .build();

         ///////////////////////////////////////
         // Schedule the job with the trigger //
         ///////////////////////////////////////
         addOrReplaceJobAndTrigger(jobKey, jobDetail, trigger);

         ///////////////////////////////////////////////////////////////////////////
         // if we're inside the setup event (e.g., initial startup), then capture //
         // this job as one that is currently active and should be kept.          //
         ///////////////////////////////////////////////////////////////////////////
         if(insideSetup)
         {
            scheduledJobsAtEndOfSetup.add(new QuartzJobAndTriggerWrapper(jobDetail, trigger, null));
         }

         return (true);
      }
      catch(Exception e)
      {
         LOG.warn("Error scheduling job", e, logPair("name", schedulableIdentity.getIdentity()), logPair("group", groupName));
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getScheduleDescriptionForTrigger(QScheduleMetaData scheduleMetaData)
   {
      if(StringUtils.hasContent(scheduleMetaData.getDescription()))
      {
         return scheduleMetaData.getDescription();
      }

      if(StringUtils.hasContent(scheduleMetaData.getCronExpression()))
      {
         return "cron expression: " + scheduleMetaData.getCronExpression() + (StringUtils.hasContent(scheduleMetaData.getCronTimeZoneId()) ? " time zone: " + scheduleMetaData.getCronTimeZoneId() : "");
      }

      if(scheduleMetaData.getRepeatSeconds() != null)
      {
         return "repeat seconds: " + scheduleMetaData.getRepeatSeconds();
      }

      return "";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void unscheduleSchedulable(SchedulableIdentity schedulableIdentity, SchedulableType schedulableType)
   {
      ////////////////////////////////////////////////////////////////////////////
      // only actually schedule things if we're past the server startup routine //
      ////////////////////////////////////////////////////////////////////////////
      if(!pastStartup)
      {
         return;
      }

      deleteJob(new JobKey(schedulableIdentity.getIdentity(), schedulableType.getName()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isScheduled(BasicSchedulableIdentity schedulableIdentity, SchedulableType schedulableType)
   {
      try
      {
         JobKey jobKey = new JobKey(schedulableIdentity.getIdentity(), schedulableType.getName());
         return (isJobAlreadyScheduled(jobKey));
      }
      catch(Exception e)
      {
         LOG.warn("Error checking if job is scheduled", logPair("identity", schedulableIdentity));
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void unscheduleAll() throws QException
   {
      try
      {
         for(QuartzJobAndTriggerWrapper wrapper : queryQuartz())
         {
            deleteJob(new JobKey(wrapper.jobDetail().getKey().getName(), wrapper.jobDetail().getKey().getGroup()));
         }
      }
      catch(Exception e)
      {
         throw (new QException("Error unscheduling all quartz jobs", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addOrReplaceJobAndTrigger(JobKey jobKey, JobDetail jobDetail, Trigger trigger) throws SchedulerException
   {
      boolean isJobAlreadyScheduled = isJobAlreadyScheduled(jobKey);
      if(isJobAlreadyScheduled)
      {
         boolean wasPaused = wasExistingJobPaused(jobKey);

         this.scheduler.scheduleJob(jobDetail, Set.of(trigger), true); // note, true flag here replaces if already present.
         LOG.info("Re-scheduled job", logPair("jobKey", jobKey));

         if(wasPaused)
         {
            LOG.info("Re-pausing job", logPair("jobKey", jobKey));
            pauseJob(jobKey.getName(), jobKey.getGroup());
         }
      }
      else
      {
         this.scheduler.scheduleJob(jobDetail, trigger);
         LOG.info("Scheduled new job", logPair("jobKey", jobKey));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean wasExistingJobPaused(JobKey jobKey) throws SchedulerException
   {
      List<QuartzJobAndTriggerWrapper>     quartzJobAndTriggerWrappers = queryQuartz();
      Optional<QuartzJobAndTriggerWrapper> existingWrapper             = quartzJobAndTriggerWrappers.stream().filter(w -> w.jobDetail().getKey().equals(jobKey)).findFirst();
      if(existingWrapper.isPresent())
      {
         if(Trigger.TriggerState.PAUSED.equals(existingWrapper.get().triggerState()))
         {
            return (true);
         }
      }

      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean isJobAlreadyScheduled(JobKey jobKey) throws SchedulerException
   {
      Optional<List<String>> jobGroupNames = jobGroupNamesMemoization.getResult(AnyKey.getInstance(), (x) -> scheduler.getJobGroupNames());
      if(jobGroupNames.isEmpty())
      {
         throw (new SchedulerException("Error getting job group names"));
      }

      for(String group : jobGroupNames.get())
      {
         Optional<Set<JobKey>> jobKeys = jobKeyNamesMemoization.getResult(group, (x) -> scheduler.getJobKeys(GroupMatcher.groupEquals(group)));
         if(jobKeys.isEmpty())
         {
            throw (new SchedulerException("Error getting job keys"));
         }

         if(jobKeys.get().contains(jobKey))
         {
            return (true);
         }
      }

      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean deleteJob(JobKey jobKey)
   {
      try
      {
         /////////////////////////////////////////////////////////////////////////////////////////////
         // https://www.quartz-scheduler.org/documentation/quartz-2.3.0/cookbook/UnscheduleJob.html //
         // Deleting a Job and Unscheduling All of Its Triggers                                     //
         /////////////////////////////////////////////////////////////////////////////////////////////
         if(isJobAlreadyScheduled(jobKey))
         {
            boolean result = scheduler.deleteJob(jobKey);
            LOG.info("Attempted to delete quartz job", logPair("jobKey", jobKey), logPair("deleteJobResult", result));
            return (result);
         }

         /////////////////////////////////////////
         // return true to indicate, we're good //
         /////////////////////////////////////////
         LOG.info("Request to delete quartz job, but it is not already scheduled.", logPair("jobKey", jobKey));
         return (true);
      }
      catch(Exception e)
      {
         LOG.warn("Error deleting job", e, logPair("jobKey", jobKey));
         return false;
      }
   }



   /*******************************************************************************
    ** Getter for qInstance
    **
    *******************************************************************************/
   public QInstance getQInstance()
   {
      return qInstance;
   }



   /*******************************************************************************
    ** Getter for sessionSupplier
    **
    *******************************************************************************/
   public Supplier<QSession> getSessionSupplier()
   {
      return sessionSupplier;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void pauseAll() throws SchedulerException
   {
      ///////////////////////////////////////////////////////////////////////////////
      // lesson from past self to future self:                                     //
      // pauseAll creates paused-group entries for all jobs -                      //
      // and so they can only really be resumed by a resumeAll call...             //
      // even newly scheduled things become paused.  Which can be quite confusing. //
      // so, we don't want pause all.                                              //
      ///////////////////////////////////////////////////////////////////////////////
      // this.scheduler.pauseAll();

      List<QuartzJobAndTriggerWrapper> quartzJobAndTriggerWrappers = queryQuartz();
      for(QuartzJobAndTriggerWrapper wrapper : quartzJobAndTriggerWrappers)
      {
         this.pauseJob(wrapper.jobDetail().getKey().getName(), wrapper.jobDetail().getKey().getGroup());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void resumeAll() throws SchedulerException
   {
      //////////////////////////////////////////////////
      // this seems okay, even though pauseAll isn't. //
      //////////////////////////////////////////////////
      this.scheduler.resumeAll();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void pauseJob(String jobName, String groupName) throws SchedulerException
   {
      LOG.info("Request to pause job", logPair("jobName", jobName));
      this.scheduler.pauseJob(new JobKey(jobName, groupName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void resumeJob(String jobName, String groupName) throws SchedulerException
   {
      LOG.info("Request to resume job", logPair("jobName", jobName));
      this.scheduler.resumeJob(new JobKey(jobName, groupName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QuartzJobAndTriggerWrapper> queryQuartz() throws SchedulerException
   {
      return queryQuartzMemoization.getResultThrowing(AnyKey.getInstance(), (x) ->
      {
         List<QuartzJobAndTriggerWrapper> rs = new ArrayList<>();

         for(String group : scheduler.getJobGroupNames())
         {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.groupEquals(group));
            for(JobKey jobKey : jobKeys)
            {
               JobDetail               jobDetail     = scheduler.getJobDetail(jobKey);
               List<? extends Trigger> triggersOfJob = scheduler.getTriggersOfJob(jobKey);
               for(Trigger trigger : triggersOfJob)
               {
                  Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                  rs.add(new QuartzJobAndTriggerWrapper(jobDetail, trigger, triggerState));
               }
            }
         }

         return (rs);
      }).orElse(null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void unInit()
   {
      ///////////////////////////////////////////////////
      // resetting the singleton should be sufficient! //
      ///////////////////////////////////////////////////
      quartzScheduler = null;
   }
}
