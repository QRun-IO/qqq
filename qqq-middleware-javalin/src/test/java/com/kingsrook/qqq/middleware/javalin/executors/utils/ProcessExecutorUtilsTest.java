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

package com.kingsrook.qqq.middleware.javalin.executors.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepOrStatusOutputInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Unit test for ProcessExecutorUtils
 *******************************************************************************/
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
      ProcessInitOrStepOrStatusOutputInterface output = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      RunProcessOutput runProcessOutput = new RunProcessOutput();

      java.util.Map<String, java.io.Serializable> values = new HashMap<>();
      values.put("result", "success");
      runProcessOutput.setValues(values);

      ProcessExecutorUtils.serializeRunProcessResultForCaller(output, "testProcess", runProcessOutput);

      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);
      verify(output).setValues(java.util.Map.class.cast(values));
   }


   /*******************************************************************************
    ** Test serializeRunProcessResultForCaller with exception
    *******************************************************************************/
   @Test
   void testSerializeRunProcessResultForCaller_WithException()
   {
      ProcessInitOrStepOrStatusOutputInterface output = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      RunProcessOutput runProcessOutput = new RunProcessOutput();
      Exception exception = new RuntimeException("Test error");
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
      ProcessInitOrStepOrStatusOutputInterface output = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      QUserFacingException userException = new QUserFacingException("User-friendly error message");
      Exception exception = new RuntimeException("Wrapped", userException);

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
      ProcessInitOrStepOrStatusOutputInterface output = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      Exception exception = new RuntimeException("Technical error details");

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
      ProcessInitOrStepOrStatusOutputInterface output = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      RunProcessOutput runProcessOutput = new RunProcessOutput();

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
      ProcessInitOrStepOrStatusOutputInterface output = mock(ProcessInitOrStepOrStatusOutputInterface.class);
      RunProcessOutput runProcessOutput = new RunProcessOutput();
      runProcessOutput.getProcessState().setNextStepName("nextStepName");

      ProcessExecutorUtils.serializeRunProcessResultForCaller(output, "testProcess", runProcessOutput);

      verify(output).setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);
      verify(output).setNextStep("nextStepName");
   }

}
