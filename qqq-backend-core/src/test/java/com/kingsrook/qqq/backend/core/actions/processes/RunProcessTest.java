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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunProcessTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(RunProcessTest.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testBasepull() throws QException
   {
      TestCallback    callback = new TestCallback();
      RunProcessInput request  = new RunProcessInput();
      request.setProcessName(TestUtils.PROCESS_NAME_BASEPULL);
      request.setCallback(callback);
      RunProcessOutput result = new RunProcessAction().execute(request);
      assertNotNull(result);

      //////////////////////////////////////////////////////////////////////////////////////////
      // get the last run time and 'this' run time - because the definition states that if no //
      // rows found, the last runtime timestamp should be for 24 hours ago                    //
      //////////////////////////////////////////////////////////////////////////////////////////
      Instant lastRunTime = (Instant) result.getValues().get(RunProcessAction.BASEPULL_LAST_RUNTIME_KEY);
      Instant thisRunTime = (Instant) result.getValues().get(RunProcessAction.BASEPULL_THIS_RUNTIME_KEY);
      assertTrue(thisRunTime.isAfter(lastRunTime), "new run time should be after last run time.");

      DayOfWeek lastRunTimeDayOfWeek = lastRunTime.atZone(ZoneId.systemDefault()).getDayOfWeek();
      DayOfWeek thisRunTimeDayOfWeek = thisRunTime.atZone(ZoneId.systemDefault()).getDayOfWeek();
      thisRunTimeDayOfWeek = thisRunTimeDayOfWeek.minus(1);
      assertEquals(lastRunTimeDayOfWeek.getValue(), thisRunTimeDayOfWeek.getValue(), "last and this run times should be the same day after subtracting a day");

      ///////////////////////////////////////////////
      // make sure new stamp stored in backend too //
      ///////////////////////////////////////////////
      assertEquals(thisRunTime, getBasepullLastRunTime(), "last run time should be properly stored in backend");

      ////////////////////////////////////////////////////
      // run the process one more time and check values //
      ////////////////////////////////////////////////////
      result = new RunProcessAction().execute(request);
      assertNotNull(result);

      ////////////////////////////////
      // this should still be after //
      ////////////////////////////////
      lastRunTime = (Instant) result.getValues().get(RunProcessAction.BASEPULL_LAST_RUNTIME_KEY);
      thisRunTime = (Instant) result.getValues().get(RunProcessAction.BASEPULL_THIS_RUNTIME_KEY);
      assertTrue(thisRunTime.isAfter(lastRunTime), "new run time should be after last run time.");

      ///////////////////////////////////////////////
      // make sure new stamp stored in backend too //
      ///////////////////////////////////////////////
      assertEquals(thisRunTime, getBasepullLastRunTime(), "last run time should be properly stored in backend");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Instant getBasepullLastRunTime() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria()
         .withFieldName(TestUtils.BASEPULL_KEY_FIELD_NAME)
         .withOperator(QCriteriaOperator.EQUALS)
         .withValues(List.of(TestUtils.PROCESS_NAME_BASEPULL))));
      queryInput.setTableName(TestUtils.TABLE_NAME_BASEPULL);

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertNotNull(queryOutput);
      assertEquals(1, queryOutput.getRecords().size(), "Should have one record");
      return (ValueUtils.getValueAsInstant(queryOutput.getRecords().get(0).getValue(TestUtils.BASEPULL_LAST_RUN_TIME_FIELD_NAME)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      TestCallback    callback = new TestCallback();
      RunProcessInput request  = new RunProcessInput();
      request.setProcessName("addToPeoplesAge");
      request.setCallback(callback);
      RunProcessOutput result = new RunProcessAction().execute(request);
      assertNotNull(result);
      assertTrue(result.getRecords().stream().allMatch(r -> r.getValues().containsKey("age")), "records should have a value set by the process");
      assertTrue(result.getValues().containsKey("maxAge"), "process result object should have a value set by the first function in the process");
      assertTrue(result.getValues().containsKey("totalYearsAdded"), "process result object should have a value set by the second function in the process");
      assertTrue(callback.wasCalledForQueryFilter, "callback was used for query filter");
      assertTrue(callback.wasCalledForFieldValues, "callback was used for field values");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testBreakOnFrontendSteps() throws QException
   {
      TestCallback    callback    = new TestCallback();
      QInstance       instance    = QContext.getQInstance();
      RunProcessInput request     = new RunProcessInput();
      String          processName = TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE;

      request.setProcessName(processName);
      request.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      request.setCallback(callback);
      RunProcessOutput result0 = new RunProcessAction().execute(request);

      assertNotNull(result0);

      ///////////////////////////////////////////////////////////////
      // make sure we were told that we broke at a (frontend) step //
      ///////////////////////////////////////////////////////////////
      Optional<String> breakingAtStep0 = result0.getProcessState().getNextStepName();
      assertTrue(breakingAtStep0.isPresent());
      assertInstanceOf(QFrontendStepMetaData.class, instance.getProcessStep(processName, breakingAtStep0.get()));
      assertNull(result0.getValues().get(MockBackendStep.FIELD_MOCK_VALUE));

      //////////////////////////////////////////////
      // now run again, proceeding from this step //
      //////////////////////////////////////////////
      request.setStartAfterStep(breakingAtStep0.get());
      RunProcessOutput result1 = new RunProcessAction().execute(request);

      ////////////////////////////////////////////////////////////////////
      // make sure we were told that we broke at the next frontend step //
      ////////////////////////////////////////////////////////////////////
      Optional<String> breakingAtStep1 = result1.getProcessState().getNextStepName();
      assertTrue(breakingAtStep1.isPresent());
      assertInstanceOf(QFrontendStepMetaData.class, instance.getProcessStep(processName, breakingAtStep1.get()));
      assertNotEquals(breakingAtStep0.get(), breakingAtStep1.get());
      assertEquals(MockBackendStep.MOCK_VALUE, result1.getValues().get(MockBackendStep.FIELD_MOCK_VALUE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSkipFrontendSteps() throws QException
   {
      TestCallback    callback    = new TestCallback();
      RunProcessInput request     = new RunProcessInput();
      String          processName = TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE;

      request.setProcessName(processName);
      request.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      request.setCallback(callback);

      RunProcessOutput runProcessOutput = new RunProcessAction().execute(request);
      assertTrue(runProcessOutput.getException().isEmpty());
      assertEquals(MockBackendStep.MOCK_VALUE, runProcessOutput.getValues().get(MockBackendStep.FIELD_MOCK_VALUE));
      assertTrue(runProcessOutput.getProcessState().getNextStepName().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testFailOnFrontendSteps()
   {
      TestCallback    callback    = new TestCallback();
      RunProcessInput request     = new RunProcessInput();
      String          processName = TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE;

      request.setProcessName(processName);
      request.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.FAIL);
      request.setCallback(callback);

      try
      {
         new RunProcessAction().execute(request);
         fail("This should have thrown...");
      }
      catch(Exception e)
      {
         assertTrue(e.getMessage().contains("fail on frontend steps"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPrimeProcessRequestNewProcess() throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////
      // this is a flow where it's a new process - so, we should create a new state //
      ////////////////////////////////////////////////////////////////////////////////
      RunProcessInput     runProcessInput = new RunProcessInput();
      UUIDAndTypeStateKey stateKey        = new UUIDAndTypeStateKey(UUID.randomUUID(), StateType.PROCESS_STATUS);
      QProcessMetaData    process         = QContext.getQInstance().getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE);
      ProcessState        processState    = new RunProcessAction().primeProcessState(runProcessInput, stateKey, process);
      assertNotNull(processState);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPrimeProcessRequestAttemptToContinueButStateNotFound()
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////
      // this is a flow where it's a continue, but we don't have a state stored, so it should throw //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setStartAfterStep("setupStep");
      UUIDAndTypeStateKey stateKey = new UUIDAndTypeStateKey(UUID.randomUUID(), StateType.PROCESS_STATUS);
      QProcessMetaData    process  = QContext.getQInstance().getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE);

      assertThrows(QException.class, () ->
      {
         new RunProcessAction().primeProcessState(runProcessInput, stateKey, process);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPrimeProcessRequestAttemptToContinueAndStateIsFound() throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////
      // this is a flow where it's a continue, but we don't have a state stored, so it should throw //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setStartAfterStep("setupStep");
      runProcessInput.addValue("foo", "bar");
      runProcessInput.addValue("alpha", "beta");
      UUIDAndTypeStateKey stateKey = new UUIDAndTypeStateKey(UUID.randomUUID(), StateType.PROCESS_STATUS);

      ////////////////////////////////////////////////
      // simulate the state being previously stored //
      ////////////////////////////////////////////////
      ProcessState oldProcessState = new ProcessState();
      oldProcessState.getValues().put("key", "myValue");
      oldProcessState.getValues().put("foo", "fubu");
      RunProcessAction.getStateProvider().put(stateKey, oldProcessState);

      QProcessMetaData process            = QContext.getQInstance().getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE);
      ProcessState     primedProcessState = new RunProcessAction().primeProcessState(runProcessInput, stateKey, process);
      assertEquals("myValue", primedProcessState.getValues().get("key"));

      /////////////////////////////////////////////////////////////////////////////////////////////
      // make sure values that were in the original request trump values that had been in state. //
      /////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals("bar", primedProcessState.getValues().get("foo"));
      assertEquals("beta", primedProcessState.getValues().get("alpha"));
   }



   /*******************************************************************************
    ** Test a simple version of custom routing, where we just add a frontend step.
    *******************************************************************************/
   @Test
   void testCustomRoutingAddFrontendStep() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      QStepMetaData back1 = new QBackendStepMetaData()
         .withName("back1")
         .withCode(new QCodeReference(BackendStepThatMayAddFrontendStep.class));

      QStepMetaData front1 = new QFrontendStepMetaData()
         .withName("front1");

      String processName = "customRouting";
      qInstance.addProcess(new QProcessMetaData()
         .withName(processName)
         .withStepList(List.of(
            back1
            //////////////////////////////////////
            // only put back1 in the step list. //
            //////////////////////////////////////
         ))
         .withOptionalStep(front1)
      );

      ////////////////////////////////////////////////////////////
      // make sure that if we run by default, we get to the end //
      ////////////////////////////////////////////////////////////
      RunProcessInput request = new RunProcessInput();
      request.setProcessName(processName);
      request.setCallback(new TestCallback());
      request.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      RunProcessOutput result = new RunProcessAction().execute(request);
      assertThat(result.getProcessState().getNextStepName()).isEmpty();

      /////////////////////////////////////////////////////////////////////////////////////////////
      // now run again, with the field set to cause the front1 step to be added to the step list //
      /////////////////////////////////////////////////////////////////////////////////////////////
      request.addValue("shouldAddFrontendStep", true);
      result = new RunProcessAction().execute(request);
      assertThat(result.getProcessState().getNextStepName()).hasValue("front1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class BackendStepThatMayAddFrontendStep implements BackendStep
   {

      /*******************************************************************************
       ** Execute the backend step - using the request as input, and the result as output.
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         LOG.info("Running " + getClass().getSimpleName());
         if(runBackendStepInput.getValue("shouldAddFrontendStep") != null)
         {
            List<String> stepList = new ArrayList<>(runBackendStepOutput.getProcessState().getStepList());
            stepList.add("front1");
            runBackendStepOutput.getProcessState().setStepList(stepList);
         }
      }
   }



   /*******************************************************************************
    ** Test a version of custom routing, where we remove steps
    *******************************************************************************/
   @Test
   void testCustomRoutingRemoveSteps() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      QStepMetaData back1 = new QBackendStepMetaData()
         .withName("back1")
         .withCode(new QCodeReference(BackendStepThatMayRemoveFrontendStep.class));

      QStepMetaData front1 = new QFrontendStepMetaData()
         .withName("front1");

      QStepMetaData back2 = new QBackendStepMetaData()
         .withName("back2")
         .withCode(new QCodeReference(NoopBackendStep.class));

      QStepMetaData front2 = new QFrontendStepMetaData()
         .withName("front2");

      String processName = "customRouting";
      qInstance.addProcess(new QProcessMetaData()
         .withName(processName)
         .withStepList(List.of(
            back1,
            front1,
            back2,
            front2
         ))
      );

      ////////////////////////////////////////////////////////////////////////////////
      // make sure that if we run by default, we get stopped on both frontend steps //
      ////////////////////////////////////////////////////////////////////////////////
      RunProcessInput request = new RunProcessInput();
      request.setProcessName(processName);
      request.setCallback(new TestCallback());
      request.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      RunProcessOutput result = new RunProcessAction().execute(request);
      assertThat(result.getProcessState().getNextStepName()).hasValue("front1");

      request.setStartAfterStep("front1");
      result = new RunProcessAction().execute(request);
      assertThat(result.getProcessState().getNextStepName()).hasValue("front2");

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // now run again, with the field set to cause the front1 step to be removed from the step list //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      request.setStartAfterStep(null);
      request.addValue("shouldRemoveFrontendStep", true);
      result = new RunProcessAction().execute(request);
      assertThat(result.getProcessState().getNextStepName()).hasValue("front2");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class BackendStepThatMayRemoveFrontendStep implements BackendStep
   {

      /*******************************************************************************
       ** Execute the backend step - using the request as input, and the result as output.
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         LOG.info("Running " + getClass().getSimpleName());
         if(runBackendStepInput.getValue("shouldRemoveFrontendStep") != null)
         {
            List<String> stepList = new ArrayList<>(runBackendStepOutput.getProcessState().getStepList());
            stepList.removeIf(s -> s.equals("front1"));
            runBackendStepOutput.getProcessState().setStepList(stepList);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class TestCallback implements QProcessCallback
   {
      private boolean wasCalledForQueryFilter = false;
      private boolean wasCalledForFieldValues = false;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QQueryFilter getQueryFilter()
      {
         wasCalledForQueryFilter = true;
         return (new QQueryFilter());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
      {
         wasCalledForFieldValues = true;
         Map<String, Serializable> rs = new HashMap<>();
         if(fields.stream().anyMatch(f -> f.getName().equals("yearsToAdd")))
         {
            rs.put("yearsToAdd", 42);
         }
         return (rs);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class NoopBackendStep implements BackendStep
   {

      /*******************************************************************************
       ** Execute the backend step - using the request as input, and the result as output.
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         LOG.info("Running " + getClass().getSimpleName());
      }
   }

}
