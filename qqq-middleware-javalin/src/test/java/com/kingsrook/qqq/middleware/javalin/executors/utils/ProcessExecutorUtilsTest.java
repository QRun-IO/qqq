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

package com.kingsrook.qqq.middleware.javalin.executors.utils;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepOrStatusOutputInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/*******************************************************************************
 * Unit test for ProcessExecutorUtils
 */
class ProcessExecutorUtilsTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
   }



   /*******************************************************************************
    ** Test serializeRunProcessResultForCaller with successful output
    *******************************************************************************/
   @Test
   void testSerializeRunProcessResultForCaller_Success()
   {
      ProcessInitOrStepOrStatusOutputInterface output           = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      RunProcessOutput                         runProcessOutput = new RunProcessOutput();

      Map<String, java.io.Serializable> values = new HashMap<>();
      values.put("result", "success");
      runProcessOutput.setValues(values);

      ProcessExecutorUtils.serializeRunProcessResultForCaller(output, "testProcess", runProcessOutput);

      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);
      verify(output).setValues(Map.class.cast(values));
   }



   /*******************************************************************************
    ** Test serializeRunProcessResultForCaller with exception
    *******************************************************************************/
   @Test
   void testSerializeRunProcessResultForCaller_WithException()
   {
      ProcessInitOrStepOrStatusOutputInterface output           = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      RunProcessOutput                         runProcessOutput = new RunProcessOutput();
      Exception                                exception        = new RuntimeException("Test error");
      runProcessOutput.setException(exception);

      ProcessExecutorUtils.serializeRunProcessResultForCaller(output, "testProcess", runProcessOutput);

      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);
      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.ERROR);
   }



   /*******************************************************************************
    ** Test serializeRunProcessExceptionForCaller with user-facing exception
    *******************************************************************************/
   @Test
   void testSerializeRunProcessExceptionForCaller_UserFacingException()
   {
      ProcessInitOrStepOrStatusOutputInterface output        = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      QUserFacingException                     userException = new QUserFacingException("User-friendly error message");
      Exception                                exception     = new RuntimeException("Wrapped", userException);

      ProcessExecutorUtils.serializeRunProcessExceptionForCaller(output, exception);

      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.ERROR);
      verify(output).setError("User-friendly error message");
      verify(output).setUserFacingError("User-friendly error message");
   }



   /*******************************************************************************
    ** Test serializeRunProcessExceptionForCaller with non-user-facing exception
    *******************************************************************************/
   @Test
   void testSerializeRunProcessExceptionForCaller_NonUserFacingException()
   {
      ProcessInitOrStepOrStatusOutputInterface output    = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      Exception                                exception = new RuntimeException("Technical error details");

      ProcessExecutorUtils.serializeRunProcessExceptionForCaller(output, exception);

      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.ERROR);
      verify(output).setError("Error message: Technical error details");
   }



   /*******************************************************************************
    ** Test serializeRunProcessResultForCaller with processMetaDataAdjustment
    *******************************************************************************/
   @Test
   void testSerializeRunProcessResultForCaller_WithProcessMetaDataAdjustment()
   {
      ProcessInitOrStepOrStatusOutputInterface output           = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      RunProcessOutput                         runProcessOutput = new RunProcessOutput();

      // Create a ProcessMetaDataAdjustment - it's stored in ProcessState
      com.kingsrook.qqq.backend.core.model.actions.processes.ProcessMetaDataAdjustment adjustment =
         new com.kingsrook.qqq.backend.core.model.actions.processes.ProcessMetaDataAdjustment();
      runProcessOutput.getProcessState().setProcessMetaDataAdjustment(adjustment);

      ProcessExecutorUtils.serializeRunProcessResultForCaller(output, "testProcess", runProcessOutput);

      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);
      verify(output).setProcessMetaDataAdjustment(adjustment);
   }



   /*******************************************************************************
    ** Test serializeRunProcessResultForCaller with next step
    *******************************************************************************/
   @Test
   void testSerializeRunProcessResultForCaller_WithNextStep()
   {
      ProcessInitOrStepOrStatusOutputInterface output           = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      RunProcessOutput                         runProcessOutput = new RunProcessOutput();
      runProcessOutput.getProcessState().setNextStepName("nextStepName");

      ProcessExecutorUtils.serializeRunProcessResultForCaller(output, "testProcess", runProcessOutput);

      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);
      verify(output).setNextStep("nextStepName");
   }

}
