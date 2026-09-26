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

package com.kingsrook.qqq.backend.core.actions.processes.listeners;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.CollectedLogMessage;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.tracing.ProcessTracerInterface;
import com.kingsrook.qqq.backend.core.processes.tracing.ProcessTracerMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for process lifecycle listeners, as fired by RunProcessAction
 ** through ProcessLifecycleListenerHelper.
 *******************************************************************************/
class ProcessLifecycleListenerTest extends BaseTest
{
   private static final String PROCESS_BACKEND_ONLY  = "lifecycleBackendOnly";
   private static final String PROCESS_WITH_FRONTEND = "lifecycleWithFrontend";
   private static final String PROCESS_FAILING       = "lifecycleFailing";
   private static final String PROCESS_FAILING_LATER = "lifecycleFailingAfterFrontend";
   private static final String PROCESS_NOT_LISTENED  = "lifecycleNotListened";

   private static final String FAILURE_MESSAGE = "step failed on purpose";

   ////////////////////////////////////////////////////////////////////////////////
   // listeners, steps, and tracers are instantiated by QCodeLoader, so they     //
   // record into this static list, in the order that their callbacks are called //
   ////////////////////////////////////////////////////////////////////////////////
   private static final List<String> events = new ArrayList<>();

   private static RunProcessOutput lastCompletedOutput = null;
   private static String           lastStartedUUID     = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      events.clear();
      lastCompletedOutput = null;
      lastStartedUUID = null;

      QInstance qInstance = QContext.getQInstance();

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_BACKEND_ONLY)
         .withStep(recordingStep("first"))
         .withStep(recordingStep("second")));

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_WITH_FRONTEND)
         .withStep(recordingStep("prepare"))
         .withStep(new QFrontendStepMetaData().withName("confirm"))
         .withStep(recordingStep("execute"))
         .withStep(new QFrontendStepMetaData().withName("result")));

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_FAILING)
         .withStep(recordingStep("prepare"))
         .withStep(new QBackendStepMetaData().withName("explode").withCode(new QCodeReference(FailingStep.class))));

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_FAILING_LATER)
         .withStep(recordingStep("prepare"))
         .withStep(new QFrontendStepMetaData().withName("confirm"))
         .withStep(new QBackendStepMetaData().withName("explode").withCode(new QCodeReference(FailingStep.class)))
         .withStep(new QFrontendStepMetaData().withName("result")));

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_NOT_LISTENED)
         .withStep(recordingStep("only")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QLogger.deactivateCollectingLoggerForClass(ProcessLifecycleListenerHelper.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBackendOnlyProcess_firesStartedThenCompleted() throws QException
   {
      QContext.getQInstance().withProcessLifecycleListener(new QCodeReference(RecordingListener.class));

      RunProcessOutput output = runProcess(new RunProcessInput().withProcessName(PROCESS_BACKEND_ONLY));

      assertEquals(List.of(
         "started:" + PROCESS_BACKEND_ONLY,
         "step:first",
         "step:second",
         "completed:" + PROCESS_BACKEND_ONLY), events);

      assertThat(lastCompletedOutput).isNotNull();
      assertEquals(output.getProcessUUID(), lastCompletedOutput.getProcessUUID());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessWithFrontendStep_firesStartedOnceAndCompletedAfterLastStep() throws QException
   {
      QContext.getQInstance().withProcessLifecycleListener(new QCodeReference(RecordingListener.class));

      //////////////////////////////////////////////////////////////////////
      // first request runs up to the (non-final) confirm frontend step - //
      // the run has started, but it is not complete                      //
      //////////////////////////////////////////////////////////////////////
      RunProcessInput  input   = new RunProcessInput().withProcessName(PROCESS_WITH_FRONTEND);
      RunProcessOutput output0 = runProcess(input);
      assertEquals("confirm", output0.getProcessState().getNextStepName().orElseThrow());
      assertEquals(List.of("started:" + PROCESS_WITH_FRONTEND, "step:prepare"), events);

      /////////////////////////////////////////////////////////////////////////
      // second request resumes after confirm, runs the last backend step,   //
      // and stops at the final (result) frontend step - so the run is done. //
      // started must not fire again for the resume.                         //
      /////////////////////////////////////////////////////////////////////////
      input.setStartAfterStep("confirm");
      RunProcessOutput output1 = runProcess(input);
      assertEquals("result", output1.getProcessState().getNextStepName().orElseThrow());
      assertEquals(List.of(
         "started:" + PROCESS_WITH_FRONTEND,
         "step:prepare",
         "step:execute",
         "completed:" + PROCESS_WITH_FRONTEND), events);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testThrowingStep_firesFailed()
   {
      QContext.getQInstance().withProcessLifecycleListener(new QCodeReference(RecordingListener.class));

      assertThrows(QException.class, () -> runProcess(new RunProcessInput().withProcessName(PROCESS_FAILING)));

      assertEquals(List.of(
         "started:" + PROCESS_FAILING,
         "step:prepare",
         "failed:" + PROCESS_FAILING + ":" + FAILURE_MESSAGE), events);
   }



   /*******************************************************************************
    ** The input given to onProcessStarted already carries the run's processUUID
    ** (which ESB started events include), the same one the run's output has.
    *******************************************************************************/
   @Test
   void testStarted_receivesProcessUUID() throws QException
   {
      QContext.getQInstance().withProcessLifecycleListener(new QCodeReference(RecordingListener.class));

      RunProcessOutput output = runProcess(new RunProcessInput().withProcessName(PROCESS_BACKEND_ONLY));

      assertThat(lastStartedUUID).isNotNull();
      assertEquals(output.getProcessUUID(), lastStartedUUID);
   }



   /*******************************************************************************
    ** Going back to re-run a step (startAtStep) continues the same run, so it
    ** does not fire started again.
    *******************************************************************************/
   @Test
   void testStartAtStep_doesNotFireStartedAgain() throws QException
   {
      QContext.getQInstance().withProcessLifecycleListener(new QCodeReference(RecordingListener.class));

      RunProcessInput input = new RunProcessInput().withProcessName(PROCESS_WITH_FRONTEND);
      runProcess(input);
      assertEquals(List.of("started:" + PROCESS_WITH_FRONTEND, "step:prepare"), events);

      input.setStartAtStep("prepare");
      RunProcessOutput output = runProcess(input);
      assertEquals("confirm", output.getProcessState().getNextStepName().orElseThrow());
      assertEquals(List.of(
         "started:" + PROCESS_WITH_FRONTEND,
         "step:prepare",
         "step:prepare"), events);
   }



   /*******************************************************************************
    ** A step that throws on a resumed request (after a frontend step) fires
    ** failed - without a second started.
    *******************************************************************************/
   @Test
   void testFailureOnResumedRequest_firesFailed() throws QException
   {
      QContext.getQInstance().withProcessLifecycleListener(new QCodeReference(RecordingListener.class));

      RunProcessInput input = new RunProcessInput().withProcessName(PROCESS_FAILING_LATER);
      runProcess(input);
      assertEquals(List.of("started:" + PROCESS_FAILING_LATER, "step:prepare"), events);

      input.setStartAfterStep("confirm");
      assertThrows(QException.class, () -> runProcess(input));
      assertEquals(List.of(
         "started:" + PROCESS_FAILING_LATER,
         "step:prepare",
         "failed:" + PROCESS_FAILING_LATER + ":" + FAILURE_MESSAGE), events);
   }



   /*******************************************************************************
    ** A listener that throws a LinkageError (e.g., a NoClassDefFoundError from
    ** an optional client jar that isn't on the classpath) is caught like an
    ** exception: the run's outcome, and its exception, are unchanged.
    *******************************************************************************/
   @Test
   void testLinkageErrorListener_doesNotAffectProcessOrOtherListeners() throws QException
   {
      QContext.getQInstance().withProcessLifecycleListeners(new ArrayList<>(List.of(
         new QCodeReference(LinkageErrorListener.class),
         new QCodeReference(RecordingListener.class))));

      runProcess(new RunProcessInput().withProcessName(PROCESS_BACKEND_ONLY));
      assertEquals(List.of(
         "started:" + PROCESS_BACKEND_ONLY,
         "step:first",
         "step:second",
         "completed:" + PROCESS_BACKEND_ONLY), events);

      events.clear();
      QException exception = assertThrows(QException.class, () -> runProcess(new RunProcessInput().withProcessName(PROCESS_FAILING)));
      assertEquals(FAILURE_MESSAGE, exception.getMessage());
      assertEquals(List.of(
         "started:" + PROCESS_FAILING,
         "step:prepare",
         "failed:" + PROCESS_FAILING + ":" + FAILURE_MESSAGE), events);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessTracer_stillReceivesCallsAlongsideListener() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.withProcessLifecycleListener(new QCodeReference(RecordingListener.class));
      qInstance.getProcess(PROCESS_BACKEND_ONLY).withProcessTracerCodeReference(new QCodeReference(RecordingTracer.class));

      runProcess(new RunProcessInput().withProcessName(PROCESS_BACKEND_ONLY));

      assertEquals(List.of(
         "trace:start",
         "trace:stepStart:first",
         "trace:stepFinish:first",
         "trace:stepStart:second",
         "trace:stepFinish:second",
         "trace:finish"), events.stream().filter(e -> e.startsWith("trace:")).toList());

      assertEquals(List.of(
         "started:" + PROCESS_BACKEND_ONLY,
         "completed:" + PROCESS_BACKEND_ONLY), events.stream().filter(e -> e.startsWith("started:") || e.startsWith("completed:")).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testListenerNotCalled_whenItDoesNotApply() throws QException
   {
      QContext.getQInstance().withProcessLifecycleListener(new QCodeReference(RecordingListener.class));

      runProcess(new RunProcessInput().withProcessName(PROCESS_NOT_LISTENED));

      assertEquals(List.of("step:only"), events);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBrokenListeners_areLoggedAndDoNotAffectProcessOrOtherListeners() throws QException
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(ProcessLifecycleListenerHelper.class);

      QContext.getQInstance().withProcessLifecycleListeners(new ArrayList<>(List.of(
         new QCodeReference(ThrowingListener.class),
         new QCodeReference(NotAListener.class),
         new QCodeReference(UnloadableListener.class),
         new QCodeReference(RecordingListener.class))));

      runProcess(new RunProcessInput().withProcessName(PROCESS_BACKEND_ONLY));
      assertEquals(List.of(
         "started:" + PROCESS_BACKEND_ONLY,
         "step:first",
         "step:second",
         "completed:" + PROCESS_BACKEND_ONLY), events);

      events.clear();
      assertThrows(QException.class, () -> runProcess(new RunProcessInput().withProcessName(PROCESS_FAILING)));
      assertEquals(List.of(
         "started:" + PROCESS_FAILING,
         "step:prepare",
         "failed:" + PROCESS_FAILING + ":" + FAILURE_MESSAGE), events);

      ///////////////////////////////////////////////////////////////////////////
      // each broken listener logs once per event: started + completed, then   //
      // started + failed.  the throwing and wrong-type ones fail when called; //
      // the unloadable one is reported as not loaded.                         //
      ///////////////////////////////////////////////////////////////////////////
      List<String> loggedMessages = collectingLogger.getCollectedMessages().stream().map(CollectedLogMessage::getMessage).toList();
      assertThat(loggedMessages).hasSize(12);
      assertThat(loggedMessages).filteredOn(m -> m.contains("Error calling process lifecycle listener")).hasSize(8);
      assertThat(loggedMessages).filteredOn(m -> m.contains("Could not load process lifecycle listener")).hasSize(4);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoListeners_processRunsNormally() throws QException
   {
      QContext.getQInstance().withProcessLifecycleListeners(null);

      runProcess(new RunProcessInput().withProcessName(PROCESS_BACKEND_ONLY));
      assertEquals(List.of("step:first", "step:second"), events);

      ProcessLifecycleListenerHelper.fireStarted(null, new RunProcessInput().withProcessName(PROCESS_BACKEND_ONLY));
      assertEquals(List.of("step:first", "step:second"), events);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RunProcessOutput runProcess(RunProcessInput input) throws QException
   {
      return (new RunProcessAction().execute(input));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QBackendStepMetaData recordingStep(String name)
   {
      return (new QBackendStepMetaData()
         .withName(name)
         .withCode(new QCodeReference(RecordingStep.class)));
   }



   /*******************************************************************************
    ** backend step that records that it ran
    *******************************************************************************/
   public static class RecordingStep implements BackendStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         events.add("step:" + runBackendStepInput.getStepName());
      }
   }



   /*******************************************************************************
    ** backend step that always throws
    *******************************************************************************/
   public static class FailingStep implements BackendStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         throw (new QException(FAILURE_MESSAGE));
      }
   }



   /*******************************************************************************
    ** listener that records each event it receives
    *******************************************************************************/
   public static class RecordingListener implements ProcessLifecycleListenerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public boolean appliesTo(String processName)
      {
         return (!PROCESS_NOT_LISTENED.equals(processName));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessStarted(RunProcessInput input)
      {
         events.add("started:" + input.getProcessName());
         lastStartedUUID = input.getProcessUUID();
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessCompleted(RunProcessInput input, RunProcessOutput output)
      {
         events.add("completed:" + input.getProcessName());
         lastCompletedOutput = output;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessFailed(RunProcessInput input, Exception exception)
      {
         events.add("failed:" + input.getProcessName() + ":" + exception.getMessage());
      }
   }



   /*******************************************************************************
    ** listener that throws from every callback
    *******************************************************************************/
   public static class ThrowingListener implements ProcessLifecycleListenerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public boolean appliesTo(String processName)
      {
         return (true);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessStarted(RunProcessInput input)
      {
         throw (new IllegalStateException("started listener failure"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessCompleted(RunProcessInput input, RunProcessOutput output)
      {
         throw (new IllegalStateException("completed listener failure"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessFailed(RunProcessInput input, Exception exception)
      {
         throw (new IllegalStateException("failed listener failure"));
      }
   }



   /*******************************************************************************
    ** listener that throws a LinkageError from every callback
    *******************************************************************************/
   public static class LinkageErrorListener implements ProcessLifecycleListenerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public boolean appliesTo(String processName)
      {
         return (true);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessStarted(RunProcessInput input)
      {
         throw (new NoClassDefFoundError("com/example/MissingBrokerClient"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessCompleted(RunProcessInput input, RunProcessOutput output)
      {
         throw (new NoClassDefFoundError("com/example/MissingBrokerClient"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void onProcessFailed(RunProcessInput input, Exception exception)
      {
         throw (new NoClassDefFoundError("com/example/MissingBrokerClient"));
      }
   }



   /*******************************************************************************
    ** class registered as a listener, that does not implement the interface
    *******************************************************************************/
   public static class NotAListener
   {
   }



   /*******************************************************************************
    ** listener that QCodeLoader cannot instantiate (no no-arg constructor)
    *******************************************************************************/
   public static class UnloadableListener extends RecordingListener
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public UnloadableListener(String unused)
      {
      }
   }



   /*******************************************************************************
    ** process tracer that records each call it receives
    *******************************************************************************/
   public static class RecordingTracer implements ProcessTracerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void handleProcessStart(RunProcessInput runProcessInput)
      {
         events.add("trace:start");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void handleProcessResume(RunProcessInput runProcessInput)
      {
         events.add("trace:resume");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void handleStepStart(RunBackendStepInput runBackendStepInput)
      {
         events.add("trace:stepStart:" + runBackendStepInput.getStepName());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void handleMessage(RunBackendStepInput runBackendStepInput, ProcessTracerMessage message)
      {
         events.add("trace:message");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void handleStepFinish(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
      {
         events.add("trace:stepFinish:" + runBackendStepInput.getStepName());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void handleProcessBreak(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException)
      {
         events.add("trace:break");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void handleProcessFinish(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException)
      {
         events.add("trace:finish");
      }
   }

}
