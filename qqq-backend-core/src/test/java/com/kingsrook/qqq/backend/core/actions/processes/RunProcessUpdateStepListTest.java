/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessTest.NoopBackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunProcessUpdateStepListTest extends BaseTest
{
   private static final String PROCESS_NAME = RunProcessUpdateStepListTest.class.getSimpleName();

   private static final String STEP_START = "start";
   private static final String STEP_A     = "a";
   private static final String STEP_B     = "b";
   private static final String STEP_C     = "c";
   private static final String STEP_1     = "1";
   private static final String STEP_2     = "2";
   private static final String STEP_3     = "3";
   private static final String STEP_END   = "end";

   private static final List<String> LETTERS_STEP_LIST = List.of(
      STEP_START,
      STEP_A,
      STEP_B,
      STEP_C,
      STEP_END
   );

   private static final List<String> NUMBERS_STEP_LIST = List.of(
      STEP_START,
      STEP_1,
      STEP_2,
      STEP_3,
      STEP_END
   );



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGoingLettersPath() throws QException
   {
      QContext.getQInstance().addProcess(defineProcess());

      ////////////////////////////////////////////////////////////
      // start the process, telling it to go the "letters" path //
      ////////////////////////////////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(PROCESS_NAME);
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      runProcessInput.setValues(MapBuilder.of("which", "letters"));
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert that we got back the next-step name of A, and the updated list of frontend steps (A, C) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      Optional<String> nextStepName = runProcessOutput.getProcessState().getNextStepName();
      assertTrue(nextStepName.isPresent());
      assertEquals(STEP_A, nextStepName.get());
      assertEquals(List.of(STEP_A, STEP_C, STEP_END), runProcessOutput.getUpdatedFrontendStepList().stream().map(s -> s.getName()).toList());

      /////////////////////////////////////////////////
      // resume the process after that frontend step //
      /////////////////////////////////////////////////
      runProcessInput.setProcessUUID(runProcessOutput.getProcessUUID());
      runProcessInput.setStartAfterStep(nextStepName.get());
      runProcessOutput = new RunProcessAction().execute(runProcessInput);

      ///////////////////////////////////////////////////////////////////////////////////////
      // assert we got back C as the next-step now, and no updated frontend list this time //
      ///////////////////////////////////////////////////////////////////////////////////////
      nextStepName = runProcessOutput.getProcessState().getNextStepName();
      assertTrue(nextStepName.isPresent());
      assertEquals(STEP_C, nextStepName.get());
      assertNull(runProcessOutput.getUpdatedFrontendStepList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGoingNumbersPathAndSkippingAhead() throws QException
   {
      QContext.getQInstance().addProcess(defineProcess());

      ////////////////////////////////////////////////////////////////////////////////////
      // start the process, telling it to go the "numbers" path, and to skip ahead some //
      ////////////////////////////////////////////////////////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(PROCESS_NAME);
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      runProcessInput.setValues(MapBuilder.of("which", "numbers", "skipSomeSteps", true));
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert that we got back the next-step name of 2, and the updated list of frontend steps (1, 3) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      Optional<String> nextStepName = runProcessOutput.getProcessState().getNextStepName();
      assertTrue(nextStepName.isPresent());
      assertEquals(STEP_END, nextStepName.get());
      assertEquals(List.of(STEP_2, STEP_END), runProcessOutput.getUpdatedFrontendStepList().stream().map(s -> s.getName()).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QProcessMetaData defineProcess()
   {
      QProcessMetaData process = new QProcessMetaData()
         .withName(PROCESS_NAME)
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName(STEP_START)
               .withCode(new QCodeReference(StartStep.class)),
            new QFrontendStepMetaData()
               .withName(STEP_END)
         ));

      process.withOptionalStep(new QFrontendStepMetaData().withName(STEP_A));
      process.withOptionalStep(new QBackendStepMetaData().withName(STEP_B).withCode(new QCodeReference(NoopBackendStep.class)));
      process.withOptionalStep(new QFrontendStepMetaData().withName(STEP_C));

      process.withOptionalStep(new QBackendStepMetaData().withName(STEP_1).withCode(new QCodeReference(NoopBackendStep.class)));
      process.withOptionalStep(new QFrontendStepMetaData().withName(STEP_2));
      process.withOptionalStep(new QBackendStepMetaData().withName(STEP_3).withCode(new QCodeReference(NoopBackendStep.class)));

      return (process);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class StartStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         boolean skipSomeSteps = BooleanUtils.isTrue(runBackendStepInput.getValueBoolean("skipSomeSteps"));

         if(runBackendStepInput.getValueString("which").equals("letters"))
         {
            runBackendStepOutput.updateStepList(LETTERS_STEP_LIST);
            if(skipSomeSteps)
            {
               runBackendStepOutput.setOverrideLastStepName(STEP_C);
            }
         }
         else
         {
            runBackendStepOutput.updateStepList(NUMBERS_STEP_LIST);
            if(skipSomeSteps)
            {
               runBackendStepOutput.setOverrideLastStepName(STEP_2);
            }
         }
      }
   }
}
