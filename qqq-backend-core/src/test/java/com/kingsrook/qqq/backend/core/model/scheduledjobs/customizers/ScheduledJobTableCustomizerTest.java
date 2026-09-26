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

package com.kingsrook.qqq.backend.core.model.scheduledjobs.customizers;


import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobParameter;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzJobAndTriggerWrapper;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzTestUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ScheduledJobTableCustomizer 
 *******************************************************************************/
class ScheduledJobTableCustomizerTest extends BaseTest
{
   private static final String GOOD_CRON   = "0 * * * * ?";
   private static final String GOOD_CRON_2 = "* * * * * ?";
   private static final String BAD_CRON    = "* * * * * *";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QuartzTestUtils.setupInstanceForQuartzTests();

      QSession         qSession         = QContext.getQSession();
      QScheduleManager qScheduleManager = QScheduleManager.initInstance(qInstance, () -> qSession);
      qScheduleManager.start();

      new ScheduledJobsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      SchedulerTestUtils.afterEach();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPreInsertAssertValidationErrors() throws QException
   {
      UnsafeFunction<Consumer<ScheduledJob>, QRecord, QException> tryToInsert = consumer ->
      {
         ScheduledJob scheduledJob = new ScheduledJob()
            .withLabel("Test")
            .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
            .withType(ScheduledJobType.PROCESS.name())
            .withIsActive(true);
         consumer.accept(scheduledJob);
         InsertOutput insertOutput = new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(scheduledJob));
         return (insertOutput.getRecords().get(0));
      };

      /////////////////////////////////////////////////////////
      // lambdas to run a test and assert about no of errors //
      /////////////////////////////////////////////////////////
      Function<QRecord, AbstractStringAssert<?>> assertOneErrorExtractingMessage = qRecord -> assertThat(qRecord.getErrors()).hasSize(1).first().extracting("message").asString();
      Consumer<QRecord>                          assertNoErrors                  = qRecord -> assertThat(qRecord.getErrors()).hasSize(0);

      assertOneErrorExtractingMessage.apply(tryToInsert.apply(sj -> sj.setId(null)))
         .contains("Either Cron Expression or Repeat Seconds must be given");

      assertOneErrorExtractingMessage.apply(tryToInsert.apply(sj -> sj.withRepeatSeconds(1).withCronExpression(GOOD_CRON).withCronTimeZoneId("UTC")))
         .contains("Cron Expression and Repeat Seconds may not both be given");

      assertOneErrorExtractingMessage.apply(tryToInsert.apply(sj -> sj.withRepeatSeconds(null).withCronExpression(GOOD_CRON)))
         .contains("If a Cron Expression is given, then a Cron Time Zone is required");

      assertOneErrorExtractingMessage.apply(tryToInsert.apply(sj -> sj.withRepeatSeconds(null).withCronExpression(BAD_CRON).withCronTimeZoneId("UTC")))
         .contains("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented");

      ///////////////////
      // success cases //
      ///////////////////
      assertNoErrors.accept(tryToInsert.apply(sj -> sj.withCronExpression(GOOD_CRON).withCronTimeZoneId("UTC")));
      assertNoErrors.accept(tryToInsert.apply(sj -> sj.withRepeatSeconds(1)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostInsertActionSchedulesJob() throws QException, SchedulerException
   {
      List<QuartzJobAndTriggerWrapper> wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(0, wrappers.size());

      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(new ScheduledJob()
         .withLabel("Test")
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
         .withType(ScheduledJobType.PROCESS.name())
         .withIsActive(true)
         .withRepeatSeconds(1)
         .withJobParameters(List.of(
            new ScheduledJobParameter().withKey("processName").withValue(TestUtils.PROCESS_NAME_BASEPULL)))));

      assertTrue(CollectionUtils.nullSafeIsEmpty(insertOutput.getRecords().get(0).getErrors()));
      assertTrue(CollectionUtils.nullSafeIsEmpty(insertOutput.getRecords().get(0).getWarnings()));

      wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(1, wrappers.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostInsertActionIssuesWarnings() throws QException, SchedulerException
   {
      List<QuartzJobAndTriggerWrapper> wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(0, wrappers.size());

      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(new ScheduledJob()
         .withLabel("Test")
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
         .withType(ScheduledJobType.PROCESS.name())
         .withIsActive(true)
         .withRepeatSeconds(1)
         .withJobParameters(List.of(
            new ScheduledJobParameter().withKey("processName").withValue("notAProcess")))));

      assertTrue(CollectionUtils.nullSafeIsEmpty(insertOutput.getRecords().get(0).getErrors()));
      assertThat(insertOutput.getRecords().get(0).getWarnings())
         .hasSize(1).first().extracting("message").asString()
         .contains("Error scheduling job: Unrecognized processName");

      wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(0, wrappers.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPreUpdateAssertValidationErrors() throws QException
   {
      new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(new ScheduledJob()
         .withLabel("Test")
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
         .withType(ScheduledJobType.PROCESS.name())
         .withIsActive(true)
         .withRepeatSeconds(1)));

      UnsafeFunction<Consumer<QRecord>, QRecord, QException> tryToUpdate = consumer ->
      {
         QRecord record = new QRecord().withValue("id", 1);
         consumer.accept(record);
         UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(ScheduledJob.TABLE_NAME).withRecord(record));
         return (updateOutput.getRecords().get(0));
      };

      /////////////////////////////////////////////////////////
      // lambdas to run a test and assert about no of errors //
      /////////////////////////////////////////////////////////
      Function<QRecord, AbstractStringAssert<?>> assertOneErrorExtractingMessage = qRecord -> assertThat(qRecord.getErrors()).hasSize(1).first().extracting("message").asString();
      Consumer<QRecord>                          assertNoErrors                  = qRecord -> assertThat(qRecord.getErrors()).hasSize(0);

      assertOneErrorExtractingMessage.apply(tryToUpdate.apply(r -> r.withValue("repeatSeconds", null)))
         .contains("Either Cron Expression or Repeat Seconds must be given");

      assertOneErrorExtractingMessage.apply(tryToUpdate.apply(r -> r.withValue("cronExpression", GOOD_CRON).withValue("cronTimeZoneId", "UTC")))
         .contains("Cron Expression and Repeat Seconds may not both be given");

      assertOneErrorExtractingMessage.apply(tryToUpdate.apply(r -> r.withValue("repeatSeconds", null).withValue("cronExpression", GOOD_CRON)))
         .contains("If a Cron Expression is given, then a Cron Time Zone is required");

      assertOneErrorExtractingMessage.apply(tryToUpdate.apply(r -> r.withValue("repeatSeconds", null).withValue("cronExpression", BAD_CRON).withValue("cronTimeZoneId", "UTC")))
         .contains("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented");

      ///////////////////
      // success cases //
      ///////////////////
      assertNoErrors.accept(tryToUpdate.apply(r -> r.withValue("modifyDate", Instant.now())));
      assertNoErrors.accept(tryToUpdate.apply(r -> r.withValue("repeatSeconds", null).withValue("cronExpression", GOOD_CRON).withValue("cronTimeZoneId", "UTC")));
      assertNoErrors.accept(tryToUpdate.apply(r -> r.withValue("repeatSeconds", null).withValue("cronExpression", GOOD_CRON_2).withValue("cronTimeZoneId", "UTC")));
      assertNoErrors.accept(tryToUpdate.apply(r -> r.withValue("repeatSeconds", null).withValue("cronExpression", GOOD_CRON).withValue("cronTimeZoneId", "America/Chicago")));
      assertNoErrors.accept(tryToUpdate.apply(r -> r.withValue("repeatSeconds", 1).withValue("cronExpression", null).withValue("cronTimeZoneId", null)));
      assertNoErrors.accept(tryToUpdate.apply(r -> r.withValue("repeatSeconds", 2)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostUpdateActionReSchedulesJob() throws QException, SchedulerException
   {
      List<QuartzJobAndTriggerWrapper> wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(0, wrappers.size());

      //////////////////////////////////////////////////
      // do an insert - this will originally schedule //
      //////////////////////////////////////////////////
      new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(new ScheduledJob()
         .withLabel("Test")
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
         .withType(ScheduledJobType.PROCESS.name())
         .withIsActive(true)
         .withRepeatSeconds(1)
         .withJobParameters(List.of(new ScheduledJobParameter().withKey("processName").withValue(TestUtils.PROCESS_NAME_BASEPULL)))));

      wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(1, wrappers.size());
      assertThat(wrappers.get(0).trigger()).isInstanceOf(SimpleTrigger.class);

      //////////////////////////////////////
      // now do an update, to re-schedule //
      //////////////////////////////////////
      new UpdateAction().execute(new UpdateInput(ScheduledJob.TABLE_NAME).withRecordEntity(new ScheduledJob()
         .withId(1)
         .withLabel("Test")
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
         .withType(ScheduledJobType.PROCESS.name())
         .withIsActive(true)
         .withRepeatSeconds(null)
         .withCronExpression(GOOD_CRON)
         .withCronTimeZoneId("UTC")));

      wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(1, wrappers.size());
      assertThat(wrappers.get(0).trigger()).isInstanceOf(CronTrigger.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostUpdateActionIssuesWarnings() throws QException, SchedulerException
   {
      List<QuartzJobAndTriggerWrapper> wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(0, wrappers.size());

      //////////////////////////////////////////////////
      // do an insert - this will originally schedule //
      //////////////////////////////////////////////////
      new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(new ScheduledJob()
         .withLabel("Test")
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
         .withType(ScheduledJobType.PROCESS.name())
         .withIsActive(true)
         .withRepeatSeconds(1)
         .withJobParameters(List.of(new ScheduledJobParameter().withKey("processName").withValue(TestUtils.PROCESS_NAME_BASEPULL)))));

      wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(1, wrappers.size());
      assertThat(wrappers.get(0).trigger()).isInstanceOf(SimpleTrigger.class);

      //////////////////////////////////////
      // now do an update, to re-schedule //
      //////////////////////////////////////
      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(ScheduledJob.TABLE_NAME).withRecordEntity(new ScheduledJob()
         .withId(1)
         .withLabel("Test")
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
         .withType(ScheduledJobType.PROCESS.name())
         .withIsActive(true)
         .withRepeatSeconds(null)
         .withCronExpression(GOOD_CRON)
         .withCronTimeZoneId("UTC")
         .withJobParameters(List.of(new ScheduledJobParameter().withKey("process").withValue("not")))));

      assertTrue(CollectionUtils.nullSafeIsEmpty(updateOutput.getRecords().get(0).getErrors()));
      assertThat(updateOutput.getRecords().get(0).getWarnings())
         .hasSize(1).first().extracting("message").asString()
         .contains("Missing scheduledJobParameter with key [processName]");

      wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(0, wrappers.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostDeleteUnschedules() throws QException, SchedulerException
   {
      List<QuartzJobAndTriggerWrapper> wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(0, wrappers.size());

      //////////////////////////////////////////////////
      // do an insert - this will originally schedule //
      //////////////////////////////////////////////////
      new InsertAction().execute(new InsertInput(ScheduledJob.TABLE_NAME).withRecordEntity(new ScheduledJob()
         .withLabel("Test")
         .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
         .withType(ScheduledJobType.PROCESS.name())
         .withIsActive(true)
         .withRepeatSeconds(1)
         .withJobParameters(List.of(new ScheduledJobParameter().withKey("processName").withValue(TestUtils.PROCESS_NAME_BASEPULL)))));

      wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(1, wrappers.size());

      ////////////////////////////////////
      // now do a delete, to unschedule //
      ////////////////////////////////////
      new DeleteAction().execute(new DeleteInput(ScheduledJob.TABLE_NAME).withPrimaryKeys(List.of(1)));

      wrappers = QuartzTestUtils.queryQuartz();
      assertEquals(0, wrappers.size());
   }

}