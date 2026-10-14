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

package com.kingsrook.qqq.backend.core.processes.implementations.reports;


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportActionTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.processes.ProcessOutputStorageMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumns;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryStorageAction;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.RenderSavedReportExecuteStep;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;


/*******************************************************************************
 ** Both report steps must publish storage downloads and clean up failed writes.
 *******************************************************************************/
class ReportStorageOutputTest extends BaseTest
{
   private static final String REPORT_NAME = "storageReportTest";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      QContext.getQInstance().withSupplementalMetaData(new ProcessOutputStorageMetaData().withTableName(TestUtils.TABLE_NAME_MEMORY_STORAGE));
      new SavedReportsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      QReportMetaData report = GenerateReportActionTest.definePersonShoesSummaryReport(true);
      report.setName(REPORT_NAME);
      report.setLabel(REPORT_NAME);
      QContext.getQInstance().addReport(report);
      GenerateReportActionTest.insertPersonRecords(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testCsvStoredWithoutTempFile(boolean saved) throws Exception
   {
      Set<String> before = temporaryReports();
      RunBackendStepOutput output = new RunBackendStepOutput();
      step(saved).run(input(saved), output);
      assertThat(temporaryReports()).isEqualTo(before);
      assertThat(output.getValues()).containsKeys("downloadFileName", "storageTableName", "storageReference").doesNotContainKey("serverFilePath");
      assertThat(output.getValueString("downloadFileName")).endsWith(".csv");
      assertThat(output.getValueString("storageTableName")).isEqualTo(TestUtils.TABLE_NAME_MEMORY_STORAGE);
      if(!saved)
      {
         assertThat(output.getValueString("storageReference")).matches("reports/\\d{4}/\\d{2}/\\d{2}/[0-9a-f-]{36}/" + REPORT_NAME + "\\.csv");
      }
      StorageInput storage = new StorageInput(output.getValueString("storageTableName")).withReference(output.getValueString("storageReference"));
      assertThat(ProcessFileDownload.isStorageRegistered(storage)).isTrue();
      try(InputStream stream = new StorageAction().getInputStream(storage))
      {
         String csv = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         assertThat(csv).contains("Last Name", "Kelkhoff");
      }
   }



   /*******************************************************************************
    ** Generation failure still closes storage, then removes the partial object.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testGenerationFailureDeletesPartialObject(boolean saved) throws Exception
   {
      QException original = new QException("generation failed");
      Set<String> before = temporaryReports();
      try(MockedConstruction<GenerateReportAction> ignored = mockConstruction(GenerateReportAction.class, (mock, context) ->
         when(mock.execute(any())).thenAnswer(invocation ->
         {
            ReportInput report = invocation.getArgument(0);
            report.getReportDestination().getReportOutputStream().write("partial report".getBytes(StandardCharsets.UTF_8));
            throw original;
         })))
      {
         QException failure = assertThrows(QException.class, () -> step(saved).run(input(saved), new RunBackendStepOutput()));
         assertThat(saved ? failure : failure.getCause()).isSameAs(original);
      }
      assertThat(new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_MEMORY_STORAGE)).getRecords()).isEmpty();
      assertThat(temporaryReports()).isEqualTo(before);
   }



   /*******************************************************************************
    ** Closing can publish an object and then fail; cleanup must still remove it.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testCloseFailureDeletesObject(boolean saved) throws Exception
   {
      IOException original = new IOException("close failed");
      try(MockedConstruction<StorageAction> ignored = mockConstruction(StorageAction.class, withSettings().defaultAnswer(CALLS_REAL_METHODS), (mock, context) ->
         doAnswer(invocation -> new FilterOutputStream(new MemoryStorageAction().createOutputStream(invocation.getArgument(0)))
         {
            @Override
            public void close() throws IOException
            {
               super.close();
               throw original;
            }
         }).when(mock).createOutputStream(any())))
      {
         QException failure = assertThrows(QException.class, () -> step(saved).run(input(saved), new RunBackendStepOutput()));
         assertThat(failure.getCause()).isSameAs(original);
      }
      assertThat(new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_MEMORY_STORAGE)).getRecords()).isEmpty();
   }



   /*******************************************************************************
    ** Cleanup errors must not replace the generation error.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testCleanupFailureSuppressed(boolean saved) throws Exception
   {
      QException original = new QException("generation failed");
      QException cleanup = new QException("delete failed");
      try(MockedConstruction<GenerateReportAction> reports = mockConstruction(GenerateReportAction.class, (mock, context) -> when(mock.execute(any())).thenThrow(original));
         MockedConstruction<StorageAction> storage = mockConstruction(StorageAction.class, withSettings().defaultAnswer(CALLS_REAL_METHODS), (mock, context) -> doThrow(cleanup).when(mock).deleteObject(any())))
      {
         QException failure = assertThrows(QException.class, () -> step(saved).run(input(saved), new RunBackendStepOutput()));
         assertThat(saved ? failure : failure.getCause()).isSameAs(original);
         assertThat(original.getSuppressed()).containsExactly(cleanup);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private BackendStep step(boolean saved)
   {
      return saved ? new RenderSavedReportExecuteStep() : new ExecuteReportStep();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunBackendStepInput input(boolean saved)
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("reportName", REPORT_NAME);
      input.addValue("reportFormat", "CSV");
      input.addValue("startDate", LocalDate.of(1980, 1, 1));
      input.addValue("endDate", LocalDate.of(2099, 12, 31));
      if(saved)
      {
         input.addValue("storageTableName", TestUtils.TABLE_NAME_MEMORY_STORAGE);
         input.setRecords(List.of(new SavedReport().withLabel(REPORT_NAME).withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY).withQueryFilterJson("{}")
            .withColumnsJson(JsonUtils.toJson(new ReportColumns().withColumn("id").withColumn("firstName").withColumn("lastName"))).toQRecord().withValue("id", 1)));
      }
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Set<String> temporaryReports() throws IOException
   {
      try(Stream<Path> files = Files.list(Path.of(System.getProperty("java.io.tmpdir"))))
      {
         return files.map(path -> path.getFileName().toString()).filter(name -> name.startsWith(REPORT_NAME) || name.startsWith("qqq-saved-report-")).collect(Collectors.toSet());
      }
   }
}
