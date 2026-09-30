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

package com.kingsrook.qqq.backend.core.model.actions.processes;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** AssertJ assert class for ProcessSummary - that is - a list of ProcessSummaryLineInterface's
 *******************************************************************************/
public class ProcessSummaryAssert extends AbstractAssert<ProcessSummaryAssert, List<ProcessSummaryLineInterface>>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   protected ProcessSummaryAssert(List<ProcessSummaryLineInterface> actual, Class<?> selfType)
   {
      super(actual, selfType);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static ProcessSummaryAssert assertThat(RunProcessOutput runProcessOutput)
   {
      List<ProcessSummaryLineInterface> processResults = (List<ProcessSummaryLineInterface>) runProcessOutput.getValue("processResults");
      if(processResults == null)
      {
         processResults = (List<ProcessSummaryLineInterface>) runProcessOutput.getValue("validationSummary");
      }

      return (new ProcessSummaryAssert(processResults, ProcessSummaryAssert.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static ProcessSummaryAssert assertThat(RunBackendStepOutput runBackendStepOutput)
   {
      List<ProcessSummaryLineInterface> processResults = (List<ProcessSummaryLineInterface>) runBackendStepOutput.getValue("processResults");
      if(processResults == null)
      {
         processResults = (List<ProcessSummaryLineInterface>) runBackendStepOutput.getValue("validationSummary");
      }

      if(processResults == null)
      {
         fail("Could not find process results in backend step output.");
      }

      return (new ProcessSummaryAssert(processResults, ProcessSummaryAssert.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ProcessSummaryAssert assertThat(List<ProcessSummaryLineInterface> actual)
   {
      return (new ProcessSummaryAssert(actual, ProcessSummaryAssert.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryAssert hasSize(int expectedSize)
   {
      Assertions.assertThat(actual).hasSize(expectedSize);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert hasLineWithMessageMatching(String regExp)
   {
      List<String> foundMessages = new ArrayList<>();
      for(ProcessSummaryLineInterface processSummaryLineInterface : actual)
      {
         if(processSummaryLineInterface.getMessage() == null)
         {
            processSummaryLineInterface.prepareForFrontend(false);
         }

         if(processSummaryLineInterface.getMessage() != null && processSummaryLineInterface.getMessage().matches(regExp))
         {
            return (new ProcessSummaryLineInterfaceAssert(processSummaryLineInterface, ProcessSummaryLineInterfaceAssert.class));
         }
         else
         {
            foundMessages.add(processSummaryLineInterface.getMessage());
         }
      }

      failWithMessage("Failed to find a ProcessSummaryLine with message matching [" + regExp + "].\nFound messages were:\n" + StringUtils.join("\n", foundMessages));
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert hasLineWithMessageContaining(String substr)
   {
      List<String> foundMessages = new ArrayList<>();
      for(ProcessSummaryLineInterface processSummaryLineInterface : actual)
      {
         if(processSummaryLineInterface.getMessage() == null)
         {
            processSummaryLineInterface.prepareForFrontend(false);
         }

         if(processSummaryLineInterface.getMessage() != null && processSummaryLineInterface.getMessage().contains(substr))
         {
            return (new ProcessSummaryLineInterfaceAssert(processSummaryLineInterface, ProcessSummaryLineInterfaceAssert.class));
         }
         else
         {
            foundMessages.add(processSummaryLineInterface.getMessage());
         }
      }

      failWithMessage("Failed to find a ProcessSummaryLine with message containing [" + substr + "].\nFound messages were:\n" + StringUtils.join("\n", foundMessages));
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryLineInterfaceAssert hasLineWithStatus(Status status)
   {
      List<String> foundStatuses = new ArrayList<>();
      for(ProcessSummaryLineInterface processSummaryLineInterface : actual)
      {
         if(status.equals(processSummaryLineInterface.getStatus()))
         {
            return (new ProcessSummaryLineInterfaceAssert(processSummaryLineInterface, ProcessSummaryLineInterfaceAssert.class));
         }
         else
         {
            foundStatuses.add(String.valueOf(processSummaryLineInterface.getStatus()));
         }
      }

      failWithMessage("Failed to find a ProcessSummaryLine with status [" + status + "].\nFound statuses were:\n" + StringUtils.join("\n", foundStatuses));
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessSummaryAssert hasNoLineWithStatus(Status status)
   {
      for(ProcessSummaryLineInterface processSummaryLineInterface : actual)
      {
         if(status.equals(processSummaryLineInterface.getStatus()))
         {
            failWithMessage("Found a ProcessSummaryLine with status [" + status + "], which was not supposed to happen.");
            return (null);
         }
      }

      return (this);
   }

}
