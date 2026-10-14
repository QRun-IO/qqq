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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.listeners.ProcessLifecycleListenerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;
import com.kingsrook.qqq.backend.core.processes.tracing.LoggingProcessTracer;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Regression coverage for the transient process secret channel.
 *******************************************************************************/
public class RunProcessActionOneTimeSecretsTest extends BaseTest
{
   private static final String SECRET = "one-time-access-token-12345";
   private static final List<String> capturedJson = new ArrayList<>();



   /*******************************************************************************
    ** Observers and stored state must be safe to serialize, while callers receive
    ** each secret in backend execution order, once per producing invocation.
    *******************************************************************************/
   @Test
   void testSecretsStayOutOfStateObserversAndLogs() throws Exception
   {
      capturedJson.clear();
      QCollectingLogger logger = QLogger.activateCollectingLoggerForClass(CapturingTracer.class);
      try
      {
         QContext.getQInstance().withProcessLifecycleListener(new QCodeReference(CapturingListener.class));
         QContext.getQInstance().addProcess(new QProcessMetaData().withName("secrets")
            .withProcessTracerCodeReference(new QCodeReference(CapturingTracer.class))
            .withStep(secretStep("first"))
            .withStep(secretStep("second"))
            .withStep(new QFrontendStepMetaData().withName("shown"))
            .withStep(new QBackendStepMetaData().withName("finish").withCode(new QCodeReferenceLambda<BackendStep>((input, output) -> output.addValue("finished", true)))));

         RunProcessOutput output = new RunProcessAction().execute(new RunProcessInput().withProcessName("secrets")
            .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
         assertThat(output.getOneTimeSecrets()).extracting(OneTimeSecret::getLabel).containsExactly("first", "second");
         assertThat(output.getOneTimeSecrets()).extracting(OneTimeSecret::getValue).containsExactly(SECRET + "first", SECRET + "second");
         assertThat(output.getValues().get(RunProcessAction.ONE_TIME_SECRETS_MASKED)).isEqualTo(List.of("****first", "****second"));
         ProcessState stored = RunProcessAction.getState(output.getProcessUUID()).orElseThrow();
         assertThat(json(stored)).doesNotContain(SECRET);
         assertThat(json(output)).doesNotContain(SECRET, "\"oneTimeSecrets\"");
         assertThat(output.toString()).doesNotContain(SECRET);
         assertThat(capturedJson).hasSize(5).allSatisfy(json -> assertThat(json).doesNotContain(SECRET));
         assertThat(logger.getCollectedMessages()).isNotEmpty().allSatisfy(message -> assertThat(message.toString()).doesNotContain(SECRET));
         assertThat(new RunProcessOutput(stored).getOneTimeSecrets()).isEmpty();

         RunProcessOutput resumed = new RunProcessAction().execute(new RunProcessInput().withProcessName("secrets")
            .withProcessUUID(output.getProcessUUID()).withStartAfterStep("shown"));
         assertThat(resumed.getOneTimeSecrets()).isEmpty();
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(CapturingTracer.class);
      }
   }



   /*******************************************************************************
    ** Both output fields must be transient, independent of their Jackson exclusions.
    *******************************************************************************/
   @Test
   void testTransientFields() throws Exception
   {
      RunBackendStepOutput stepOutput = new RunBackendStepOutput().addOneTimeSecret(new OneTimeSecret().withValue(SECRET));
      RunProcessOutput processOutput = new RunProcessOutput();
      processOutput.getOneTimeSecrets().addAll(stepOutput.getOneTimeSecrets());
      assertThat(json(stepOutput)).doesNotContain(SECRET, "oneTimeSecrets");
      assertThat(stepOutput.toString()).doesNotContain(SECRET);
      assertThat(stepOutput.getOneTimeSecrets().get(0).toString()).doesNotContain(SECRET);
      assertThat(java.lang.reflect.Modifier.isTransient(RunProcessOutput.class.getDeclaredField("oneTimeSecrets").getModifiers())).isTrue();
      assertThat(java.lang.reflect.Modifier.isTransient(RunBackendStepOutput.class.getDeclaredField("oneTimeSecrets").getModifiers())).isTrue();
   }



   /*******************************************************************************
    ** Make a backend step with an independently identifiable secret.
    *******************************************************************************/
   private QBackendStepMetaData secretStep(String name)
   {
      return (new QBackendStepMetaData().withName(name).withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
         output.addOneTimeSecret(new OneTimeSecret().withLabel(name).withValue(SECRET + name).withMasked("****" + name)))));
   }



   /*******************************************************************************
    ** Capture the same JSON that lifecycle integrations may publish.
    *******************************************************************************/
   public static class CapturingListener implements ProcessLifecycleListenerInterface
   {
      /***************************************************************************
       ** Only observe the test process.
       ***************************************************************************/
      @Override
      public boolean appliesTo(String processName)
      {
         return ("secrets".equals(processName));
      }



      /***************************************************************************
       ** No output exists at start.
       ***************************************************************************/
      @Override
      public void onProcessStarted(RunProcessInput input)
      {
      }



      /***************************************************************************
       ** Serialize output as an ESB listener would.
       ***************************************************************************/
      @Override
      public void onProcessCompleted(RunProcessInput input, RunProcessOutput output)
      {
         capturedJson.add(json(output));
      }



      /***************************************************************************
       ** Unexpected failures are surfaced by the calling test.
       ***************************************************************************/
      @Override
      public void onProcessFailed(RunProcessInput input, Exception exception)
      {
      }
   }



   /*******************************************************************************
    ** Exercise JSON serialization and logging at both output boundaries.
    *******************************************************************************/
   public static class CapturingTracer extends LoggingProcessTracer
   {
      private static final QLogger LOG = QLogger.getLogger(CapturingTracer.class);



      /***************************************************************************
       ** Capture step output without revealing its secret list.
       ***************************************************************************/
      @Override
      public void handleStepFinish(RunBackendStepInput input, RunBackendStepOutput output)
      {
         String json = json(output);
         capturedJson.add(json);
         LOG.info(json);
      }



      /***************************************************************************
       ** Capture final output without revealing its secret list.
       ***************************************************************************/
      @Override
      public void handleProcessFinish(RunProcessInput input, RunProcessOutput output, Exception exception)
      {
         String json = json(output);
         capturedJson.add(json);
         LOG.info(json);
      }
   }



   /*******************************************************************************
    ** Serialize as an observer with Optional support would (process state and
    ** outputs hold Optionals; the default mapper does not register Jdk8Module).
    *******************************************************************************/
   public static String json(Object object)
   {
      return (JsonUtils.toJsonWithMapper(object, mapper -> mapper.registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module())));
   }
}
