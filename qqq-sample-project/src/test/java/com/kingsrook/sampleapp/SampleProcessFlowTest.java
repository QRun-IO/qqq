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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.ProcessStepFlow;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStateMachineStep;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Process flow controls exercised with owned sample data and caller sessions.
 *******************************************************************************/
class SampleProcessFlowTest
{
   private final List<String> trace = new ArrayList<>();
   private QInstance instance;
   private QSession owner;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      owner = new QSession().withUser(new QUser().withIdReference("sample-process-owner")).withPermissions();
      QContext.init(instance, owner);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLinearSelectedRecordsValuesAndStepOverride() throws Exception
   {
      addLinearProcess();
      List<String> before = people();
      RunProcessInput input = input("ownedLinear");
      input.addValue("recordIds", "1,3");
      input.addValue("label", "Selected people");
      RunProcessOutput output = new RunProcessAction().execute(input);
      assertEquals(List.of("read", "finish"), trace);
      assertEquals("Selected people: Avery,Casey", output.getValueString("summary"));
      assertEquals(2, output.getValueInteger("selectedCount"));
      assertTrue(output.getProcessState().getNextStepName().isEmpty());
      assertEquals(before, people());
      assertSame(instance, QContext.getQInstance());
      assertSame(owner, QContext.getQSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingRequiredFieldOrRecordsCannotRunStep() throws Exception
   {
      addLinearProcess();
      List<String> before = people();
      RunProcessInput missingField = input("ownedLinear");
      missingField.addValue("recordIds", "1");
      assertThrows(QException.class, () -> new RunProcessAction().execute(missingField));
      RunProcessInput missingRecords = input("ownedLinear");
      missingRecords.addValue("label", "No selection");
      assertThrows(QException.class, () -> new RunProcessAction().execute(missingRecords));
      assertEquals(List.of(), trace);
      assertEquals(before, people());
      assertSame(owner, QContext.getQSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCheckedAndRuntimeFailuresPreserveCallerContext() throws Exception
   {
      for(Boolean checked : List.of(true, false))
      {
         String name = checked ? "ownedCheckedFailure" : "ownedRuntimeFailure";
         instance.addProcess(new QProcessMetaData().withName(name).withTableName("person")
            .withStep(step("fail", (in, out) ->
            {
               assertEquals(5, CountAction.execute("person", null));
               if(checked)
               {
                  throw new QException("Owned checked failure");
               }
               throw new IllegalStateException("Owned runtime failure");
            })));
         assertThrows(QException.class, () -> new RunProcessAction().execute(input(name)));
         assertSame(instance, QContext.getQInstance());
         assertSame(owner, QContext.getQSession());
         assertEquals(5, people().size());
      }
      addLinearProcess();
      RunProcessInput recovery = input("ownedLinear");
      recovery.addValue("recordIds", "2");
      recovery.addValue("label", "Recovery");
      assertEquals("Recovery: Blair", new RunProcessAction().execute(recovery).getValueString("summary"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStateMachineFrontendBackendAndOptionalTransitions() throws Exception
   {
      QProcessMetaData process = new QProcessMetaData().withName("ownedStates").withTableName("person").withStepFlow(ProcessStepFlow.STATE_MACHINE)
         .withStep(QStateMachineStep.frontendOnly("start", new QFrontendStepMetaData().withName("prompt")).withDefaultNextStepName("work"))
         .withStep(QStateMachineStep.backendOnly("work", step("readPeople", (in, out) ->
         {
            trace.add("work");
            out.addValue("count", CountAction.execute("person", null));
            out.getProcessState().setNextStepName(Boolean.TRUE.equals(in.getValue("includeOptional")) ? "optional" : "review");
         })))
         .withStep(QStateMachineStep.frontendThenBackend("review", new QFrontendStepMetaData().withName("reviewScreen"), step("confirm", (in, out) ->
         {
            trace.add("confirmed");
            out.getProcessState().setNextStepName("done");
         })))
         .withStep(QStateMachineStep.frontendOnly("done", new QFrontendStepMetaData().withName("results")))
         .withOptionalStep(QStateMachineStep.backendOnly("optional", step("optionalWork", (in, out) ->
         {
            trace.add("optional");
            out.getProcessState().setNextStepName("review");
         })));
      instance.addProcess(process);
      for(Boolean optional : List.of(false, true))
      {
         trace.clear();
         RunProcessInput input = input("ownedStates");
         input.addValue("includeOptional", optional);
         assertEquals("prompt", next(new RunProcessAction().execute(input)));
         input.setStartAfterStep("prompt");
         RunProcessOutput review = new RunProcessAction().execute(input);
         assertEquals("reviewScreen", next(review));
         assertEquals(5, review.getValueInteger("count"));
         assertEquals(optional ? List.of("work", "optional") : List.of("work"), trace);
         input.setStartAfterStep("reviewScreen");
         assertEquals("results", next(new RunProcessAction().execute(input)));
         input.setStartAfterStep("results");
         assertTrue(new RunProcessAction().execute(input).getProcessState().getNextStepName().isEmpty());
         assertEquals(optional ? List.of("work", "optional", "confirmed") : List.of("work", "confirmed"), trace);
      }
      assertEquals(5, people().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBackNavigationAndUnknownBackTarget() throws Exception
   {
      instance.addProcess(new QProcessMetaData().withName("ownedBack").withTableName("person").withStepFlow(ProcessStepFlow.LINEAR)
         .withStep(step("read", (in, out) ->
         {
            trace.add(Boolean.TRUE.equals(in.getIsStepBack()) ? "back" : "read");
            out.addValue("count", CountAction.execute("person", null));
         }))
         .withStep(new QFrontendStepMetaData().withName("review").withBackStepName("read"))
         .withStep(step("finish", (in, out) -> trace.add("finish"))));
      RunProcessInput input = input("ownedBack");
      assertEquals("review", next(new RunProcessAction().execute(input)));
      input.setStartAtStep("read");
      assertEquals("review", next(new RunProcessAction().execute(input)));
      assertEquals(List.of("read", "back"), trace);
      input.setStartAtStep("missingBackTarget");
      new RunProcessAction().execute(input);
      assertEquals(List.of("read", "back"), trace);
      input.setStartAtStep(null);
      input.setStartAfterStep("review");
      assertTrue(new RunProcessAction().execute(input).getProcessState().getNextStepName().isEmpty());
      assertEquals(List.of("read", "back", "finish"), trace);
      assertEquals(5, people().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidStateAndLoopBound() throws Exception
   {
      instance.addProcess(new QProcessMetaData().withName("ownedBadState").withStepFlow(ProcessStepFlow.STATE_MACHINE)
         .withStep(QStateMachineStep.backendOnly("start", step("requestMissing", (in, out) -> out.getProcessState().setNextStepName("missingState")))));
      QException invalid = assertThrows(QException.class, () -> new RunProcessAction().execute(input("ownedBadState")));
      assertTrue(invalid.getMessage().contains("missingState"), invalid.getMessage());
      instance.addProcess(new QProcessMetaData().withName("ownedLoop").withStepFlow(ProcessStepFlow.STATE_MACHINE)
         .withStep(QStateMachineStep.backendOnly("again", step("repeat", (in, out) ->
         {
            trace.add("repeat");
            out.getProcessState().setNextStepName("again");
         }))));
      RunProcessInput loop = input("ownedLoop");
      loop.addValue("maxStateMachineProcessStepFlowStackDepth", 3);
      QException bounded = assertThrows(QException.class, () -> new RunProcessAction().execute(loop));
      assertTrue(bounded.getMessage().contains("maxStateMachineProcessStepFlowStackDepth of 3"), bounded.getMessage());
      assertTrue(trace.size() <= 4, trace.toString());
      assertSame(owner, QContext.getQSession());
      assertEquals(5, people().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidBackendCodeReferenceIsRejected() throws Exception
   {
      QInstance invalid = SampleMetaDataProvider.defineTestInstance();
      invalid.addProcess(new QProcessMetaData().withName("ownedInvalidCode")
         .withStep(new QBackendStepMetaData().withName("invalid").withCode(new QCodeReference(String.class))));
      assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(invalid));
      assertSame(instance, QContext.getQInstance());
      assertSame(owner, QContext.getQSession());
      assertEquals(5, people().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownStateResumeTargetIsRejected() throws Exception
   {
      instance.addProcess(new QProcessMetaData().withName("ownedResumeState").withStepFlow(ProcessStepFlow.STATE_MACHINE)
         .withStep(QStateMachineStep.frontendOnly("start", new QFrontendStepMetaData().withName("prompt"))));
      RunProcessInput input = input("ownedResumeState");
      assertEquals("prompt", next(new RunProcessAction().execute(input)));
      input.setStartAfterStep("missingBackTarget");
      QException invalid = assertThrows(QException.class, () -> new RunProcessAction().execute(input));
      assertTrue(invalid.getMessage().contains("missingBackTarget"), invalid.getMessage());
      assertSame(owner, QContext.getQSession());
      assertEquals(5, people().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addLinearProcess()
   {
      instance.addProcess(new QProcessMetaData().withName("ownedLinear").withTableName("person").withStepFlow(ProcessStepFlow.LINEAR)
         .withStep(step("read", (in, out) ->
         {
            trace.add("read");
            String names = String.join(",", in.getRecords().stream().map(record -> record.getValueString("firstName")).sorted().toList());
            out.addValue("names", names);
            out.setRecords(in.getRecords());
            out.setOverrideLastStepName("skip");
         }).withInputData(new QFunctionInputMetaData().withRecordListMetaData(new QRecordListMetaData().withTableName("person"))
            .withField(new QFieldMetaData("label", QFieldType.STRING).withIsRequired(true))))
         .withStep(step("skip", (in, out) -> trace.add("unexpected")))
         .withStep(step("finish", (in, out) ->
         {
            trace.add("finish");
            out.addValue("selectedCount", in.getRecords().size());
            out.addValue("summary", in.getValueString("label") + ": " + in.getValueString("names"));
         })));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QBackendStepMetaData step(String name, BackendStep backend)
   {
      return new QBackendStepMetaData().withName(name).withCode(new QCodeReferenceLambda<>(backend));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput input(String name)
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(name);
      input.setInputSource(QInputSource.USER);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String next(RunProcessOutput output)
   {
      return output.getProcessState().getNextStepName().orElseThrow();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> people() throws Exception
   {
      List<String> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT id,first_name FROM person ORDER BY id"))
      {
         while(result.next())
         {
            rows.add(result.getInt(1) + ":" + result.getString(2));
         }
      }
      return rows;
   }
}
