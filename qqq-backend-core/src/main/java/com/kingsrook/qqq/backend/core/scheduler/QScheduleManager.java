/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler;


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.automation.polling.PollingAutomationPerTableRunner;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.VariantRunStrategy;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.BasicSchedulableIdentity;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.SchedulableIdentity;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.SchedulableIdentityFactory;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableProcessRunner;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableRunner;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableSQSQueueRunner;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableTableAutomationsRunner;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** QQQ service to manage scheduled jobs, using 1 or more Schedulers - implementations
 ** of the QSchedulerInterface
 *******************************************************************************/
public class QScheduleManager
{
   private static final QLogger LOG = QLogger.getLogger(QScheduleManager.class);

   private static QScheduleManager   qScheduleManager = null;
   private final  QInstance          qInstance;
   private final  Supplier<QSession> systemUserSessionSupplier;

   private Map<String, QSchedulerInterface> schedulers = new HashMap<>();



   /*******************************************************************************
    ** Singleton constructor
    *******************************************************************************/
   private QScheduleManager(QInstance qInstance, Supplier<QSession> systemUserSessionSupplier)
   {
      this.qInstance = qInstance;
      this.systemUserSessionSupplier = systemUserSessionSupplier;
   }



   /*******************************************************************************
    ** Singleton initiator - e.g., must be called to initially initialize the singleton
    ** before anyone else calls getInstance (they'll get an error if they call that first).
    *******************************************************************************/
   public static QScheduleManager initInstance(QInstance qInstance, Supplier<QSession> systemUserSessionSupplier) throws QException
   {
      if(qScheduleManager == null)
      {
         qScheduleManager = new QScheduleManager(qInstance, systemUserSessionSupplier);

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // initialize the scheduler(s) we're configured to use                                                                     //
         // do this, even if we won't start them - so, for example, a web server can still be aware of schedules in the application //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         for(QSchedulerMetaData schedulerMetaData : CollectionUtils.nonNullMap(qInstance.getSchedulers()).values())
         {
            QSchedulerInterface scheduler = schedulerMetaData.initSchedulerInstance(qInstance, systemUserSessionSupplier);
            qScheduleManager.schedulers.put(schedulerMetaData.getName(), scheduler);
         }
      }
      return (qScheduleManager);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void defineDefaultSchedulableTypesInInstance(QInstance qInstance)
   {
      qInstance.addSchedulableType(new SchedulableType().withName(ScheduledJobType.PROCESS.name()).withRunner(new QCodeReference(SchedulableProcessRunner.class)));
      qInstance.addSchedulableType(new SchedulableType().withName(ScheduledJobType.QUEUE_PROCESSOR.name()).withRunner(new QCodeReference(SchedulableSQSQueueRunner.class)));
      qInstance.addSchedulableType(new SchedulableType().withName(ScheduledJobType.TABLE_AUTOMATIONS.name()).withRunner(new QCodeReference(SchedulableTableAutomationsRunner.class)));
   }



   /*******************************************************************************
    ** Singleton accessor
    *******************************************************************************/
   public static QScheduleManager getInstance()
   {
      if(qScheduleManager == null)
      {
         throw (new IllegalStateException("QScheduleManager singleton has not been init'ed (call initInstance)."));
      }
      return (qScheduleManager);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void start() throws QException
   {
      //////////////////////////////////////////////////////////////////////////
      // exit w/o starting schedulers, if schedule manager isn't enabled here //
      //////////////////////////////////////////////////////////////////////////
      if(!new QMetaDataVariableInterpreter().getBooleanFromPropertyOrEnvironment("qqq.scheduleManager.enabled", "QQQ_SCHEDULE_MANAGER_ENABLED", true))
      {
         LOG.info("Not starting ScheduleManager per settings.");
         schedulers.values().forEach(s -> s.doNotStart());
         return;
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // ensure that everything which should be scheduled is scheduled, in the appropriate scheduler //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.withTemporaryContext(new CapturedContext(qInstance, systemUserSessionSupplier.get()), () -> setupAllSchedules());

      //////////////////////////
      // start each scheduler //
      //////////////////////////
      schedulers.values().forEach(s -> s.start());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void stop()
   {
      schedulers.values().forEach(s -> s.stop());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void stopAsync()
   {
      schedulers.values().forEach(s -> s.stopAsync());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setupAllNewSchedules() throws QException
   {
      if(QContext.getQInstance().getTables().containsKey(ScheduledJob.TABLE_NAME))
      {
         List<ScheduledJob> scheduledJobList = new QueryAction()
            .execute(new QueryInput(ScheduledJob.TABLE_NAME)
               .withIncludeAssociations(true))
            .getRecordEntities(ScheduledJob.class);

         for(ScheduledJob scheduledJob : scheduledJobList)
         {
            try
            {
               QSchedulerInterface      scheduler           = getScheduler(scheduledJob.getSchedulerName());
               BasicSchedulableIdentity schedulableIdentity = SchedulableIdentityFactory.of(scheduledJob);
               SchedulableType          schedulableType     = qInstance.getSchedulableType(scheduledJob.getType());

               if(!scheduler.isScheduled(schedulableIdentity, schedulableType))
               {
                  setupScheduledJob(scheduledJob);
               }
            }
            catch(Exception e)
            {
               LOG.warn("Error evaluating scheduled job", logPair("id", scheduledJob.getId()));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setupAllSchedules() throws QException
   {
      /////////////////////////////////////////////
      // read dynamic schedules                  //
      // e.g., user-scheduled processes, reports //
      /////////////////////////////////////////////
      List<ScheduledJob> scheduledJobList = null;
      try
      {
         if(QContext.getQInstance().getTables().containsKey(ScheduledJob.TABLE_NAME))
         {
            scheduledJobList = new QueryAction()
               .execute(new QueryInput(ScheduledJob.TABLE_NAME)
                  .withIncludeAssociations(true))
               .getRecordEntities(ScheduledJob.class);
         }
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Failed to query for scheduled jobs - will not set up scheduler!", e));
      }

      /////////////////////////////////////////////////////////
      // let the schedulers know we're starting this process //
      /////////////////////////////////////////////////////////
      schedulers.values().forEach(s -> s.startOfSetupSchedules());

      /////////////////////////
      // schedule all queues //
      /////////////////////////
      for(QQueueMetaData queue : qInstance.getQueues().values())
      {
         if(queue.getSchedule() != null)
         {
            setupQueue(queue);
         }
      }

      ////////////////////////////////////////
      // schedule all tables w/ automations //
      ////////////////////////////////////////
      for(QTableMetaData table : qInstance.getTables().values())
      {
         QTableAutomationDetails automationDetails = table.getAutomationDetails();
         if(automationDetails != null && automationDetails.getSchedule() != null)
         {
            setupTableAutomations(table);
         }
      }

      /////////////////////////////////////////
      // schedule all processes that need it //
      /////////////////////////////////////////
      for(QProcessMetaData process : qInstance.getProcesses().values())
      {
         if(process.getSchedule() != null)
         {
            setupProcess(process);
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////////
      // todo- before, or after meta-datas?                                                      //
      // like quartz, it'd just re-schedule if a dupe - but, should we do our own dupe checking? //
      /////////////////////////////////////////////////////////////////////////////////////////////
      for(ScheduledJob scheduledJob : CollectionUtils.nonNullList(scheduledJobList))
      {
         try
         {
            setupScheduledJob(scheduledJob);
         }
         catch(Exception e)
         {
            LOG.info("Caught exception while scheduling a job", e, logPair("id", scheduledJob.getId()));
         }
      }

      //////////////////////////////////////////////////////////
      // let the schedulers know we're done with this process //
      //////////////////////////////////////////////////////////
      schedulers.values().forEach(s -> s.endOfSetupSchedules());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setupScheduledJob(ScheduledJob scheduledJob) throws QException
   {
      BasicSchedulableIdentity schedulableIdentity = SchedulableIdentityFactory.of(scheduledJob);

      ////////////////////////////////////////////////////////////////////////////////
      // non-active jobs should be deleted from the scheduler.  they get re-added   //
      // if they get re-activated.  but we don't want to rely on (e.g., for quartz) //
      // the paused state to be drive by is-active.  else, devops-pause & unpause   //
      // operations would clobber scheduled-job record facts                        //
      ////////////////////////////////////////////////////////////////////////////////
      if(!scheduledJob.getIsActive())
      {
         unscheduleScheduledJob(scheduledJob);
         return;
      }

      String exceptionSuffix = "in scheduledJob [" + scheduledJob.getId() + "]";

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // setup schedule meta-data object based on schedule data in the scheduled job - throwing if not well populated //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(scheduledJob.getRepeatSeconds() == null && !StringUtils.hasContent(scheduledJob.getCronExpression()))
      {
         throw (new QException("Missing a schedule (cronString or repeatSeconds) " + exceptionSuffix));
      }

      QScheduleMetaData scheduleMetaData = new QScheduleMetaData();
      scheduleMetaData.setCronExpression(scheduledJob.getCronExpression());
      scheduleMetaData.setCronTimeZoneId(scheduledJob.getCronTimeZoneId());
      scheduleMetaData.setRepeatSeconds(scheduledJob.getRepeatSeconds());

      /////////////////////////////////
      // get & validate the job type //
      /////////////////////////////////
      if(!StringUtils.hasContent(scheduledJob.getType()))
      {
         throw (new QException("Missing a type " + exceptionSuffix));
      }

      SchedulableType schedulableType = qInstance.getSchedulableType(scheduledJob.getType());
      if(schedulableType == null)
      {
         throw (new QException("Unrecognized type [" + scheduledJob.getType() + "] " + exceptionSuffix));
      }

      QSchedulerInterface       scheduler = getScheduler(scheduledJob.getSchedulerName());
      Map<String, Serializable> paramMap  = new HashMap<>(scheduledJob.getJobParametersMap());

      SchedulableRunner runner = QCodeLoader.getAdHoc(SchedulableRunner.class, schedulableType.getRunner());
      runner.validateParams(schedulableIdentity, new HashMap<>(paramMap));

      scheduler.setupSchedulable(schedulableIdentity, schedulableType, paramMap, scheduleMetaData, true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void unscheduleAll()
   {
      schedulers.values().forEach(s ->
      {
         try
         {
            s.unscheduleAll();
         }
         catch(Exception e)
         {
            LOG.warn("Error unscheduling everything in scheduler " + s, e);
         }
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void unscheduleScheduledJob(ScheduledJob scheduledJob) throws QException
   {
      QSchedulerInterface scheduler = getScheduler(scheduledJob.getSchedulerName());

      BasicSchedulableIdentity schedulableIdentity = SchedulableIdentityFactory.of(scheduledJob);
      SchedulableType          schedulableType     = qInstance.getSchedulableType(scheduledJob.getType());

      scheduler.unscheduleSchedulable(schedulableIdentity, schedulableType);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setupProcess(QProcessMetaData process) throws QException
   {
      BasicSchedulableIdentity schedulableIdentity = SchedulableIdentityFactory.of(process);
      QSchedulerInterface      scheduler           = getScheduler(process.getSchedule().getSchedulerName());
      boolean                  allowedToStart      = SchedulerUtils.allowedToStart(process.getName());

      Map<String, String> paramMap = new HashMap<>();
      paramMap.put("processName", process.getName());

      SchedulableType schedulableType = qInstance.getSchedulableType(ScheduledJobType.PROCESS.name());

      if(process.getVariantBackend() == null || VariantRunStrategy.SERIAL.equals(process.getVariantRunStrategy()))
      {
         ///////////////////////////////////////////////
         // if no variants, or variant is serial mode //
         ///////////////////////////////////////////////
         scheduler.setupSchedulable(schedulableIdentity, schedulableType, new HashMap<>(paramMap), process.getSchedule(), allowedToStart);
      }
      else if(VariantRunStrategy.PARALLEL.equals(process.getVariantRunStrategy()))
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // if this a "parallel", which for example means we want to have a thread for each backend variant //
         // running at the same time, get the variant records and schedule each separately                  //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         QBackendMetaData backendMetaData = qInstance.getBackend(process.getVariantBackend());
         for(QRecord qRecord : CollectionUtils.nonNullList(SchedulerUtils.getBackendVariantFilteredRecords(process)))
         {
            try
            {
               HashMap<String, Serializable> parameters = new HashMap<>(paramMap);

               String variantTypeKey                 = backendMetaData.getBackendVariantsConfig().getVariantTypeKey();
               String variantOptionsTableName        = backendMetaData.getBackendVariantsConfig().getOptionsTableName();
               String variantOptionsTableIdFieldName = QContext.getQInstance().getTable(variantOptionsTableName).getPrimaryKeyField();

               HashMap<String, Serializable> variantMap = new HashMap<>(Map.of(variantTypeKey, qRecord.getValue(variantOptionsTableIdFieldName)));
               parameters.put("backendVariantData", variantMap);

               String identity    = schedulableIdentity.getIdentity() + ";" + variantTypeKey + "=" + qRecord.getValue(variantOptionsTableIdFieldName);
               String description = schedulableIdentity.getDescription() + " for variant: " + variantTypeKey + "=" + qRecord.getValue(variantOptionsTableIdFieldName);

               BasicSchedulableIdentity variantIdentity = new BasicSchedulableIdentity(identity, description);

               scheduler.setupSchedulable(variantIdentity, schedulableType, parameters, process.getSchedule(), allowedToStart);
            }
            catch(Exception e)
            {
               LOG.error("An error starting process [" + process.getLabel() + "], with backend variant data.", e, new LogPair("variantQRecord", qRecord));
            }
         }
      }
      else
      {
         LOG.error("Unsupported Schedule Run Strategy [" + process.getVariantRunStrategy() + "] was provided.");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setupTableAutomations(QTableMetaData table) throws QException
   {
      SchedulableType         schedulableType   = qInstance.getSchedulableType(ScheduledJobType.TABLE_AUTOMATIONS.name());
      QTableAutomationDetails automationDetails = table.getAutomationDetails();
      QSchedulerInterface     scheduler         = getScheduler(automationDetails.getSchedule().getSchedulerName());

      List<PollingAutomationPerTableRunner.TableActionsInterface> tableActionList = PollingAutomationPerTableRunner.getTableActions(qInstance, automationDetails.getProviderName())
         .stream().filter(ta -> ta.tableName().equals(table.getName()))
         .toList();

      for(PollingAutomationPerTableRunner.TableActionsInterface tableActions : tableActionList)
      {
         SchedulableIdentity schedulableIdentity = SchedulableIdentityFactory.of(tableActions);
         boolean             allowedToStart      = SchedulerUtils.allowedToStart(table.getName());

         Map<String, String> paramMap = new HashMap<>();
         paramMap.put("tableName", tableActions.tableName());
         paramMap.put("automationStatus", tableActions.status().name());
         scheduler.setupSchedulable(schedulableIdentity, schedulableType, new HashMap<>(paramMap), automationDetails.getSchedule(), allowedToStart);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setupQueue(QQueueMetaData queue) throws QException
   {
      SchedulableIdentity schedulableIdentity = SchedulableIdentityFactory.of(queue);
      QSchedulerInterface scheduler           = getScheduler(queue.getSchedule().getSchedulerName());
      SchedulableType     schedulableType     = qInstance.getSchedulableType(ScheduledJobType.QUEUE_PROCESSOR.name());
      boolean             allowedToStart      = SchedulerUtils.allowedToStart(queue.getName());

      Map<String, String> paramMap = new HashMap<>();
      paramMap.put("queueName", queue.getName());
      scheduler.setupSchedulable(schedulableIdentity, schedulableType, new HashMap<>(paramMap), queue.getSchedule(), allowedToStart);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QSchedulerInterface getScheduler(String schedulerName) throws QException
   {
      if(!StringUtils.hasContent(schedulerName))
      {
         throw (new QException("Scheduler name was not given (and the concept of a default scheduler does not exist at this time)."));
      }

      QSchedulerInterface scheduler = schedulers.get(schedulerName);
      if(scheduler == null)
      {
         throw (new QException("Unrecognized schedulerName [" + schedulerName + "]"));
      }

      return (scheduler);
   }



   /*******************************************************************************
    ** reset the singleton instance (to null); clear the map of schedulers.
    ** Not clear it's ever useful to call in main-code - but can be used for tests.
    *******************************************************************************/
   public void unInit()
   {
      qScheduleManager = null;
      schedulers.values().forEach(s -> s.unInit());
      schedulers.clear();
   }
}
