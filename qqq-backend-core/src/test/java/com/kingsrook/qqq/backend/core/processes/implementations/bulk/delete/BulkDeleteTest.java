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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.delete;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for full bulk edit process
 *******************************************************************************/
class BulkDeleteTest extends BaseTest
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
   void test() throws QException
   {
      //////////////////////////////
      // insert some test records //
      //////////////////////////////
      QInstance qInstance = QContext.getQInstance();
      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff").withValue("email", "darin.kelkhoff@kingsrook.com"),
         new QRecord().withValue("id", 2).withValue("firstName", "Tim").withValue("lastName", "Chamberlain").withValue("email", "tim.chamberlain@kingsrook.com"),
         new QRecord().withValue("id", 3).withValue("firstName", "James").withValue("lastName", "Maes").withValue("email", "james.maes@kingsrook.com")
      ));

      //////////////////////////////////
      // set up the run-process input //
      //////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(TestUtils.TABLE_NAME_PERSON_MEMORY + ".bulkDelete");
      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER,
         new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(1, 3))));

      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();

      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      runProcessInput.addValue("processUUID", processUUID);
      runProcessInput.setStartAfterStep("review");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(2);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("review");
      assertThat(runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY)).isNotNull().isInstanceOf(List.class);

      runProcessInput.setStartAfterStep("review");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("result");
      assertThat(runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY)).isNotNull().isInstanceOf(List.class);
      assertThat(runProcessOutput.getException()).isEmpty();

      @SuppressWarnings("unchecked")
      List<ProcessSummaryLine> processSummaryLines = (List<ProcessSummaryLine>) runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
      assertThat(processSummaryLines).hasSize(1);
      assertThat(processSummaryLines.stream().filter(psl -> psl.getStatus().equals(Status.OK))).hasSize(1);

      //////////////////////////////////////////////////////////////
      // query for the records - assert that only id=2 exists now //
      //////////////////////////////////////////////////////////////
      List<QRecord> records = TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals(2, records.get(0).getValueInteger("id"));
   }

}