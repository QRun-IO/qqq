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


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzTestUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for QScheduleManager 
 *******************************************************************************/
class QScheduleManagerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QLogger.deactivateCollectingLoggerForClass(QuartzScheduler.class);
      SchedulerTestUtils.afterEach();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetupScheduledJobErrorCases() throws QException
   {
      QScheduleManager qScheduleManager = QScheduleManager.initInstance(QContext.getQInstance(), () -> QContext.getQSession());

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.PROCESS, Map.of()).withRepeatSeconds(null)))
         .hasMessageContaining("Missing a schedule");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.PROCESS, Map.of()).withType(null)))
         .hasMessageContaining("Missing a type");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.PROCESS, Map.of()).withType("notAType")))
         .hasMessageContaining("Unrecognized type");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.PROCESS, Map.of())))
         .hasMessageContaining("Missing scheduledJobParameter with key [processName]");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.PROCESS, Map.of("processName", "notAProcess"))))
         .hasMessageContaining("Unrecognized processName");

      QContext.getQInstance().getProcess(TestUtils.PROCESS_NAME_BASEPULL).withSchedule(new QScheduleMetaData());
      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.PROCESS, Map.of("processName", TestUtils.PROCESS_NAME_BASEPULL))))
         .hasMessageContaining("has a schedule in its metaData - so it should not be dynamically scheduled");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.QUEUE_PROCESSOR, Map.of())))
         .hasMessageContaining("Missing scheduledJobParameter with key [queueName]");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.QUEUE_PROCESSOR, Map.of("queueName", "notAQueue"))))
         .hasMessageContaining("Unrecognized queueName");

      QContext.getQInstance().getQueue(TestUtils.TEST_SQS_QUEUE).withSchedule(new QScheduleMetaData());
      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.QUEUE_PROCESSOR, Map.of("queueName", TestUtils.TEST_SQS_QUEUE))))
         .hasMessageContaining("has a schedule in its metaData - so it should not be dynamically scheduled");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.TABLE_AUTOMATIONS, Map.of())))
         .hasMessageContaining("Missing scheduledJobParameter with key [tableName]");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.TABLE_AUTOMATIONS, Map.of("tableName", "notATable"))))
         .hasMessageContaining("Missing scheduledJobParameter with key [automationStatus]");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.TABLE_AUTOMATIONS, Map.of("tableName", "notATable", "automationStatus", AutomationStatus.PENDING_INSERT_AUTOMATIONS.name()))))
         .hasMessageContaining("Unrecognized tableName");

      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.TABLE_AUTOMATIONS, Map.of("tableName", TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC, "automationStatus", AutomationStatus.PENDING_INSERT_AUTOMATIONS.name()))))
         .hasMessageContaining("does not have automationDetails");

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().withSchedule(null);
      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.TABLE_AUTOMATIONS, Map.of("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY, "automationStatus", "foobar"))))
         .hasMessageContaining("Did not find table automation actions matching automationStatus")
         .hasMessageContaining("Found: PENDING_INSERT_AUTOMATIONS,PENDING_UPDATE_AUTOMATIONS");

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().withSchedule(new QScheduleMetaData());
      assertThatThrownBy(() -> qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.TABLE_AUTOMATIONS, Map.of("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY, "automationStatus", AutomationStatus.PENDING_INSERT_AUTOMATIONS.name()))))
         .hasMessageContaining("has a schedule in its metaData - so it should not be dynamically scheduled");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccessfulScheduleWithQuartz() throws QException
   {
      QCollectingLogger quartzLogger = QLogger.activateCollectingLoggerForClass(QuartzScheduler.class);

      QInstance qInstance = QContext.getQInstance();
      QuartzTestUtils.setupInstanceForQuartzTests();

      QScheduleManager qScheduleManager = QScheduleManager.initInstance(qInstance, () -> QContext.getQSession());
      qScheduleManager.start();

      qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.PROCESS,
         Map.of("processName", TestUtils.PROCESS_NAME_GREET_PEOPLE))
         .withId(2)
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME));

      qInstance.getQueue(TestUtils.TEST_SQS_QUEUE).setSchedule(null);
      qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.QUEUE_PROCESSOR,
         Map.of("queueName", TestUtils.TEST_SQS_QUEUE))
         .withId(3)
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME));

      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().setSchedule(null);
      qScheduleManager.setupScheduledJob(SchedulerTestUtils.newScheduledJob(ScheduledJobType.TABLE_AUTOMATIONS,
         Map.of("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY, "automationStatus", AutomationStatus.PENDING_UPDATE_AUTOMATIONS.name()))
         .withId(4)
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME));

      assertThat(quartzLogger.getCollectedMessages())
         .anyMatch(l -> l.getMessage().matches(".*Scheduled new job.*PROCESS.scheduledJob:2.*"))
         .anyMatch(l -> l.getMessage().matches(".*Scheduled new job.*QUEUE_PROCESSOR.scheduledJob:3.*"))
         .anyMatch(l -> l.getMessage().matches(".*Scheduled new job.*TABLE_AUTOMATIONS.scheduledJob:4.*"));
   }

}
