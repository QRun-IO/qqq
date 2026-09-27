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
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QKeyBasedFieldMapping;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.basic.BasicETLProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed.StreamedETLBackendStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed.StreamedETLProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaDeleteStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaInsertStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaUpdateStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLExecuteStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.general.StandardProcessSummaryLineProducer;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native H2 commit controls for ordinary streamed ETL execution.
 *******************************************************************************/
class SampleEtlCancellationContractTest
{
   private static final String TARGET = "ownedEtlTarget";

   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         statement.execute("DROP TABLE IF EXISTS owned_etl_target");
         statement.execute("CREATE TABLE owned_etl_target (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(120) NOT NULL)");
      }
      instance.addTable(new QTableMetaData().withName(TARGET).withLabel("Owned ETL target")
         .withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME).withBackendDetails(new RDBMSTableBackendDetails().withTableName("owned_etl_target"))
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING)));
      instance.addProcess(StreamedETLWithFrontendProcess.defineProcessMetaData("person", TARGET, ExtractViaQueryStep.class, PeopleToTarget.class, LoadViaInsertStep.class)
         .withName("ownedEtl").withTableName("person"));
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("owned-etl-user")).withPermissions(Set.of()));
   }



   /*******************************************************************************
    ** Release the owned context and native connection provider.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccessfulStreamedEtlCommitsNativeRows() throws Exception
   {
      RunBackendStepInput input = input();
      new StreamedETLBackendStep().run(input, output(input));
      assertEquals(5, targetCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccessfulExecuteCommitsNativeRows() throws Exception
   {
      RunBackendStepInput input = input();
      new StreamedETLExecuteStep().run(input, output(input));
      assertEquals(5, targetCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunBackendStepInput input()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.setProcessName("ownedEtl");
      input.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_EXECUTE);
      input.setTableName("person");
      input.setInputSource(QInputSource.USER);
      input.addValue("recordIds", "1,2,3,4,5");
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      input.addValue(BasicETLProcess.FIELD_SOURCE_TABLE, "person");
      input.addValue(BasicETLProcess.FIELD_DESTINATION_TABLE, TARGET);
      input.addValue(BasicETLProcess.FIELD_MAPPING_JSON, JsonUtils.toJson(new QKeyBasedFieldMapping().withMapping("name", "firstName")));
      input.addValue(StreamedETLWithFrontendProcess.FIELD_EXTRACT_CODE, new QCodeReference(ExtractViaQueryStep.class));
      input.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSFORM_CODE, new QCodeReference(PeopleToTarget.class));
      input.addValue(StreamedETLWithFrontendProcess.FIELD_LOAD_CODE, new QCodeReference(LoadViaInsertStep.class));
      input.addValue(StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION, true);
      input.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL, StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_PROCESS);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunBackendStepOutput output(RunBackendStepInput input)
   {
      RunBackendStepOutput output = new RunBackendStepOutput();
      output.seedFromRequest(input);
      output.getProcessState().setStepList(List.of(StreamedETLWithFrontendProcess.STEP_NAME_PREVIEW, StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE,
         StreamedETLWithFrontendProcess.STEP_NAME_REVIEW, StreamedETLWithFrontendProcess.STEP_NAME_EXECUTE));
      return output;
   }



   /*******************************************************************************
    ** Read through a separate native connection after the step has unwound.
    *******************************************************************************/
   private int targetCount() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM owned_etl_target"))
      {
         assertTrue(result.next());
         return result.getInt(1);
      }
   }



   /*******************************************************************************
    ** Map canonical native people into the owned transaction target.
    *******************************************************************************/
   public static class PeopleToTarget extends AbstractTransformStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         return new ArrayList<>();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output)
      {
         input.getRecords().forEach(record -> output.getRecords().add(new QRecord().withValue("name", record.getValue("firstName"))));

      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBasicAndStreamedMappingUsesNativeRows() throws Exception
   {
      instance.addProcess(new BasicETLProcess().defineProcessMetaData());
      instance.addProcess(new StreamedETLProcess().defineProcessMetaData());
      for(String process : List.of(BasicETLProcess.PROCESS_NAME, StreamedETLProcess.PROCESS_NAME))
      {
         sql("DELETE FROM owned_etl_target");
         RunProcessInput input = new RunProcessInput();
         input.setProcessName(process);
         input.setInputSource(QInputSource.USER);
         input.addValue(BasicETLProcess.FIELD_SOURCE_TABLE, "person");
         input.addValue(BasicETLProcess.FIELD_DESTINATION_TABLE, TARGET);
         input.addValue(BasicETLProcess.FIELD_MAPPING_JSON, JsonUtils.toJson(new QKeyBasedFieldMapping().withMapping("name", "firstName")));
         new RunProcessAction().execute(input);
         assertEquals(sourceNames(), targetNames());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBoundedPagesCommitInEveryTransactionMode() throws Exception
   {
      for(String level : List.of(StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_AUTO_COMMIT,
         StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_PAGE, StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_PROCESS))
      {
         sql("DELETE FROM owned_etl_target");
         MappingTransform.pageSizes.clear();
         RunBackendStepInput input = input();
         input.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL, level);
         input.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSFORM_CODE, new QCodeReference(MappingTransform.class));
         input.addValue("recordPipeCapacity", 2);
         RunBackendStepOutput output = output(input);
         new StreamedETLExecuteStep().run(input, output);
         assertEquals(sourceNames(), targetNames());
         assertTrue(MappingTransform.pageSizes.size() >= 3, MappingTransform.pageSizes.toString());
         assertTrue(MappingTransform.pageSizes.stream().allMatch(size -> size > 0 && size <= 2), MappingTransform.pageSizes.toString());
         List<?> summary = (List<?>) output.getValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
         assertEquals(5, ((ProcessSummaryLine) summary.get(0)).getCount());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPreviewAndFullValidationDoNotWriteMixedRows() throws Exception
   {
      instance.addProcess(StreamedETLWithFrontendProcess.defineProcessMetaData("person", TARGET,
         ExtractViaQueryStep.class, MappingTransform.class, LoadViaInsertStep.class).withName("validatedEtl").withTableName("person"));
      RunProcessInput input = new RunProcessInput();
      input.setProcessName("validatedEtl");
      input.setInputSource(QInputSource.USER);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      input.addValue("recordIds", "1,2,3,4,5");
      input.addValue("rejectId", 2);
      input.addValue("recordPipeCapacity", 2);
      RunProcessOutput preview = new RunProcessAction().execute(input);
      assertEquals("review", preview.getProcessState().getNextStepName().orElseThrow());
      assertEquals(0, targetCount());
      input.setStartAfterStep("review");
      input.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      RunProcessOutput validated = new RunProcessAction().execute(input);
      assertEquals("review", validated.getProcessState().getNextStepName().orElseThrow());
      List<?> summary = (List<?>) validated.getValue(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY);
      assertEquals(4, ((ProcessSummaryLine) summary.get(0)).getCount());
      assertEquals(1, ((ProcessSummaryLine) summary.get(1)).getCount());
      assertEquals(Status.ERROR, ((ProcessSummaryLine) summary.get(1)).getStatus());
      assertEquals(0, targetCount());
      RunProcessOutput loaded = new RunProcessAction().execute(input);
      assertEquals("result", loaded.getProcessState().getNextStepName().orElseThrow());
      assertEquals(sourceNames().stream().filter(name -> !name.equals("Blair")).toList(), targetNames());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateAndDeleteLoadPersistTheirResults() throws Exception
   {
      RunBackendStepInput input = input();
      new StreamedETLExecuteStep().run(input, output(input));
      input = input();
      input.addValue(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, TARGET);
      input.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSFORM_CODE, new QCodeReference(TargetTransform.class));
      input.addValue(StreamedETLWithFrontendProcess.FIELD_LOAD_CODE, new QCodeReference(LoadViaUpdateStep.class));
      new StreamedETLExecuteStep().run(input, output(input));
      assertEquals(sourceNames().stream().map(name -> "Updated: " + name).toList(), targetNames());
      RunBackendStepInput delete = input();
      delete.addValue(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, TARGET);
      delete.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSFORM_CODE, new QCodeReference(TargetTransform.class));
      delete.addValue(StreamedETLWithFrontendProcess.FIELD_LOAD_CODE, new QCodeReference(LoadViaDeleteStep.class));
      new StreamedETLExecuteStep().run(delete, output(delete));
      assertEquals(0, targetCount());
      assertEquals(5, sourceNames().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLoaderFailureRespectsTransactionBoundary() throws Exception
   {
      for(String level : List.of(StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_AUTO_COMMIT,
         StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_PAGE, StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_PROCESS))
      {
         sql("DELETE FROM owned_etl_target");
         RunBackendStepInput input = input();
         input.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL, level);
         input.addValue(StreamedETLWithFrontendProcess.FIELD_LOAD_CODE, new QCodeReference(FailingLoader.class));
         input.addValue("recordPipeCapacity", 2);
         assertThrows(QException.class, () -> new StreamedETLExecuteStep().run(input, output(input)));
         assertEquals(level.equals(StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_AUTO_COMMIT) ? 2 : 0, targetCount());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExtractorAndLaterTransformFailureLeaveNoCommittedRows() throws Exception
   {
      RunBackendStepInput extract = input();
      extract.addValue(StreamedETLWithFrontendProcess.FIELD_EXTRACT_CODE, new QCodeReference(FailingExtractor.class));
      assertThrows(QException.class, () -> new StreamedETLExecuteStep().run(extract, output(extract)));
      assertEquals(0, targetCount());
      RunBackendStepInput transform = input();
      transform.addValue(StreamedETLWithFrontendProcess.FIELD_TRANSFORM_CODE, new QCodeReference(FailingTransform.class));
      transform.addValue("recordPipeCapacity", 2);
      assertThrows(QException.class, () -> new StreamedETLExecuteStep().run(transform, output(transform)));
      assertEquals(0, targetCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void sql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> targetNames() throws Exception
   {
      return names("SELECT name FROM owned_etl_target ORDER BY name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> sourceNames() throws Exception
   {
      return names("SELECT first_name FROM person ORDER BY first_name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> names(String sql) throws Exception
   {
      List<String> names = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            names.add(result.getString(1));
         }
      }
      return names;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class MappingTransform extends AbstractTransformStep
   {
      private static final List<Integer> pageSizes = new ArrayList<>();
      private final ProcessSummaryLine accepted = StandardProcessSummaryLineProducer.getOkToInsertLine();
      private final ProcessSummaryLine rejected = new ProcessSummaryLine(Status.ERROR)
         .withSingularFutureMessage("will be rejected").withPluralFutureMessage("will be rejected")
         .withSingularPastMessage("was rejected").withPluralPastMessage("were rejected");



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         return StandardProcessSummaryLineProducer.toArrayList(accepted, rejected);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         pageSizes.add(input.getRecords().size());
         for(QRecord record : input.getRecords())
         {
            if(record.getValueInteger("id").equals(input.getValueInteger("rejectId")))
            {
               rejected.incrementCountAndAddPrimaryKey(record.getValue("id"));
               continue;
            }
            output.addRecord(new QRecord().withValue("name", record.getValue("firstName")));
            accepted.incrementCountAndAddPrimaryKey(record.getValue("id"));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class TargetTransform extends PeopleToTarget
   {



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output)
      {
         input.getRecords().forEach(record -> output.addRecord(new QRecord().withValue("id", record.getValue("id"))
            .withValue("name", "Updated: " + record.getValueString("name"))));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailingLoader extends LoadViaInsertStep
   {



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         super.runOnePage(input, output);
         throw new QException("Owned loader failure after native writes");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailingExtractor extends AbstractExtractStep
   {



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         throw new QException("Owned source failure before extraction");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailingTransform extends MappingTransform
   {
      private Integer page = 0;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         page++;
         if(page == 2)
         {
            throw new QException("Owned transform failure after first loaded page");
         }
         super.runOnePage(input, output);
      }
   }
}
