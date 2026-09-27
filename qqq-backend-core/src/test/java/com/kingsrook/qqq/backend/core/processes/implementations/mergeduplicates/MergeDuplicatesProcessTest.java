/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.mergeduplicates;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.processes.utils.GeneralProcessUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for MergeDuplicatesProcess
 *******************************************************************************/
class MergeDuplicatesProcessTest extends BaseTest
{
   private static final String PROCESS_NAME = "testMergeProcess";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();

      //////////////////////////////////////////////////
      // add a field to the shape table for this test //
      //////////////////////////////////////////////////
      qInstance.getTable(TestUtils.TABLE_NAME_SHAPE)
         .addField(new QFieldMetaData("favoredByNoOfPeople", QFieldType.INTEGER));

      addProcessToInstance();

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("noOfShoes", 1).withValue("favoriteShapeId", 11),
         new QRecord().withValue("id", 2).withValue("firstName", "Tim").withValue("noOfShoes", 2).withValue("favoriteShapeId", 12),
         new QRecord().withValue("id", 3).withValue("firstName", "Tyler").withValue("noOfShoes", 1).withValue("favoriteShapeId", 13),
         new QRecord().withValue("id", 4).withValue("firstName", "Darin").withValue("noOfShoes", 1).withValue("favoriteShapeId", 14),
         new QRecord().withValue("id", 5).withValue("firstName", "Darin").withValue("noOfShoes", 1).withValue("favoriteShapeId", 15),
         new QRecord().withValue("id", 6).withValue("firstName", "James").withValue("noOfShoes", 1).withValue("favoriteShapeId", 16),
         new QRecord().withValue("id", 7).withValue("firstName", "James").withValue("noOfShoes", 1).withValue("favoriteShapeId", 17)
      ));

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_SHAPE), List.of(
         new QRecord().withValue("id", 11).withValue("favoredByNoOfPeople", 1),
         new QRecord().withValue("id", 12).withValue("favoredByNoOfPeople", 1),
         new QRecord().withValue("id", 13).withValue("favoredByNoOfPeople", 1),
         new QRecord().withValue("id", 14).withValue("favoredByNoOfPeople", 1),
         new QRecord().withValue("id", 15).withValue("favoredByNoOfPeople", 1),
         new QRecord().withValue("id", 16).withValue("favoredByNoOfPeople", 1),
         new QRecord().withValue("id", 17).withValue("favoredByNoOfPeople", 1)
      ));

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(PROCESS_NAME);
      runProcessInput.addValue("recordIds", "1,2,3,4,5,6,7");
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);

      RunProcessAction runProcessAction = new RunProcessAction();
      RunProcessOutput runProcessOutput = runProcessAction.execute(runProcessInput);

      @SuppressWarnings("unchecked")
      ArrayList<ProcessSummaryLineInterface> processResults = (ArrayList<ProcessSummaryLineInterface>) runProcessOutput.getValues().get("processResults");

      assertThat(processResults.get(0))
         .hasFieldOrPropertyWithValue("message", "was updated")
         .hasFieldOrPropertyWithValue("count", 1);

      assertThat(processResults.get(1))
         .hasFieldOrPropertyWithValue("message", "were deleted")
         .hasFieldOrPropertyWithValue("count", 2);

      assertThat(processResults.get(2))
         .hasFieldOrPropertyWithValue("message", "did not have any duplicates.")
         .hasFieldOrPropertyWithValue("count", 2);

      assertThat(processResults.get(3))
         .hasFieldOrPropertyWithValue("message", "were skipped, because it was not clear how they should have been processed.")
         .hasFieldOrPropertyWithValue("count", 2);

      /////////////////////////////////////////////////
      // make sure records 4 and 5 have been deleted //
      /////////////////////////////////////////////////
      Map<Serializable, QRecord> personMap = GeneralProcessUtils.loadTableToMap(TestUtils.TABLE_NAME_PERSON_MEMORY, "id");
      assertEquals(5, personMap.size());
      assertNull(personMap.get(4));
      assertNull(personMap.get(5));

      ////////////////////////////////////////////////
      // make sure person 1's noOfShoes was updated //
      ////////////////////////////////////////////////
      assertEquals(3, personMap.get(1).getValueInteger("noOfShoes"));

      /////////////////////////////////////////////////////////////////////////////
      // make sure the shapes corresponding to records 4 and 5 have been deleted //
      /////////////////////////////////////////////////////////////////////////////
      Map<Serializable, QRecord> shapesMap = GeneralProcessUtils.loadTableToMap(TestUtils.TABLE_NAME_SHAPE, "id");
      assertEquals(5, shapesMap.size());
      assertNull(shapesMap.get(4));
      assertNull(shapesMap.get(5));

      ///////////////////////////////////////////////
      // make sure the 'other table' got an update //
      ///////////////////////////////////////////////
      assertEquals(3, shapesMap.get(11).getValueInteger("favoredByNoOfPeople"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addProcessToInstance()
   {
      QInstance qInstance = QContext.getQInstance();

      qInstance.addProcess(MergeDuplicatesProcess.processMetaDataBuilder()
         .withName(PROCESS_NAME)
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withMergeDuplicatesTransformStepClass(PersonMergeDuplicatesStep.class)
         .getProcessMetaData());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonMergeDuplicatesStep extends AbstractMergeDuplicatesTransformStep
   {


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected MergeProcessConfig getMergeProcessConfig()
      {
         return (new MergeProcessConfig(TestUtils.TABLE_NAME_PERSON_MEMORY, List.of("firstName"), true));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QRecord buildRecordToKeep(RunBackendStepInput runBackendStepInput, List<QRecord> duplicateRecords) throws SkipTheseRecordsException
      {
         /////////////////////////////////////
         // keep the one with the lowest id //
         // add the other one's shoes to it //
         /////////////////////////////////////
         QRecord recordToKeep   = duplicateRecords.get(0);
         int     totalNoOfShoes = 0;
         for(QRecord duplicateRecord : duplicateRecords)
         {
            totalNoOfShoes += duplicateRecord.getValueInteger("noOfShoes");
            if(duplicateRecord.getValueInteger("id") < recordToKeep.getValueInteger("id"))
            {
               recordToKeep = duplicateRecord;
            }

            if(duplicateRecord.getValueString("firstName").equals("James"))
            {
               throw (new SkipTheseRecordsException("We don't want to mess with a James record..."));
            }
         }

         //////////////////////////////////////////////////////////////////////////
         // for ones that we aren't keeping, set to delete their favorite shapes //
         //////////////////////////////////////////////////////////////////////////
         for(QRecord duplicateRecord : duplicateRecords)
         {
            if(duplicateRecord != recordToKeep)
            {
               addOtherTableIdsToDelete(TestUtils.TABLE_NAME_SHAPE, List.of(duplicateRecord.getValueInteger("favoriteShapeId")));
            }
         }

         ////////////////////////////////////////////////////////////////
         // for the one that we are keeping, update its favorite shape //
         ////////////////////////////////////////////////////////////////
         addOtherTableRecordsToStore(TestUtils.TABLE_NAME_SHAPE, List.of(new QRecord()
            .withValue("id", recordToKeep.getValue("favoriteShapeId"))
            .withValue("favoredByNoOfPeople", duplicateRecords.size())
         ));

         recordToKeep.setValue("noOfShoes", totalNoOfShoes);

         return (recordToKeep);
      }

   }

}
