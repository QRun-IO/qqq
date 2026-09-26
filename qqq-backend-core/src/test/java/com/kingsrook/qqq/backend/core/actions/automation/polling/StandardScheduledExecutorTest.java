/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.automation.polling;


import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.scheduler.simple.StandardScheduledExecutor;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for StandardScheduledExecutor
 *******************************************************************************/
class StandardScheduledExecutorTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInsert() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      /////////////////////////////////////////////////////////////////////////////
      // insert 2 people - one who should be updated by the check-age automation //
      /////////////////////////////////////////////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "John").withValue("birthDate", LocalDate.of(1970, Month.JANUARY, 1)),
         new QRecord().withValue("id", 2).withValue("firstName", "Jim").withValue("birthDate", LocalDate.now().minusDays(30))
      ));
      new InsertAction().execute(insertInput);

      ////////////////////////////////////////////////
      // have the polling executor run "for awhile" //
      ////////////////////////////////////////////////
      runPollingAutomationExecutorForAwhile(qInstance, QSession::new);

      /////////////////////////////////////////////////
      // query for the records - assert their status //
      /////////////////////////////////////////////////
      List<QRecord> records = TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals(2, records.size());

      Optional<QRecord> optionalPerson1 = records.stream().filter(r -> r.getValueInteger("id") == 1).findFirst();
      assertThat(optionalPerson1).isPresent();
      QRecord person1 = optionalPerson1.get();
      assertThat(person1.getValueString("firstName")).isEqualTo("John");
      assertThat(person1.getValueInteger(TestUtils.standardQqqAutomationStatusField().getName())).isEqualTo(AutomationStatus.OK.getId());

      Optional<QRecord> optionalPerson2 = records.stream().filter(r -> r.getValueInteger("id") == 2).findFirst();
      assertThat(optionalPerson2).isPresent();
      QRecord person2 = optionalPerson2.get();
      assertThat(person2.getValueString("firstName")).isEqualTo("Jim" + TestUtils.CheckAge.SUFFIX_FOR_MINORS);
      assertThat(person2.getValueInteger(TestUtils.standardQqqAutomationStatusField().getName())).isEqualTo(AutomationStatus.OK.getId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSessionSupplier() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      //////////////////////////////////////////////////////////////////////
      // make the person-memory table's insert-action run a class in here //
      //////////////////////////////////////////////////////////////////////
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .getAutomationDetails().getActions().get(0)
         .withCodeReference(new QCodeReference(CaptureSessionIdAutomationHandler.class))
         .withName("captureSessionId");

      ////////////////////////////////////////////////////////////
      // insert a person that will trigger the on-insert action //
      ////////////////////////////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Tim").withValue("birthDate", LocalDate.now())
      ));
      new InsertAction().execute(insertInput);

      String   uuid    = UUID.randomUUID().toString();
      QSession session = new QSession();
      session.setIdReference(uuid);

      ////////////////////////////////////////////////
      // have the polling executor run "for awhile" //
      ////////////////////////////////////////////////
      runPollingAutomationExecutorForAwhile(qInstance, () -> session);

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert that the uuid we put in our session was present in the CaptureSessionIdAutomationHandler //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(CaptureSessionIdAutomationHandler.sessionId).isEqualTo(uuid);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CaptureSessionIdAutomationHandler implements RecordAutomationHandlerInterface
   {
      static String sessionId;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void execute(RecordAutomationInput recordAutomationInput) throws QException
      {
         sessionId = QContext.getQSession().getIdReference();
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runPollingAutomationExecutorForAwhile(QInstance qInstance, Supplier<QSession> sessionSupplier)
   {
      List<PollingAutomationPerTableRunner.TableActionsInterface> tableActions = PollingAutomationPerTableRunner.getTableActions(qInstance, TestUtils.POLLING_AUTOMATION);
      List<StandardScheduledExecutor>                    executors    = new ArrayList<>();
      for(PollingAutomationPerTableRunner.TableActionsInterface tableAction : tableActions)
      {
         PollingAutomationPerTableRunner pollingAutomationPerTableRunner = new PollingAutomationPerTableRunner(qInstance, TestUtils.POLLING_AUTOMATION, sessionSupplier, tableAction);
         StandardScheduledExecutor       pollingAutomationExecutor       = new StandardScheduledExecutor(pollingAutomationPerTableRunner);
         pollingAutomationExecutor.setInitialDelayMillis(0);
         pollingAutomationExecutor.setDelayMillis(100);
         pollingAutomationExecutor.setQInstance(qInstance);
         pollingAutomationExecutor.setName(TestUtils.POLLING_AUTOMATION);
         pollingAutomationExecutor.setSessionSupplier(sessionSupplier);
         pollingAutomationExecutor.start();
         executors.add(pollingAutomationExecutor);
      }

      SleepUtils.sleep(1, TimeUnit.SECONDS);

      executors.forEach(StandardScheduledExecutor::stop);
   }

}