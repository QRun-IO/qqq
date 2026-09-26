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

package com.kingsrook.qqq.backend.core.processes.implementations.savedreports;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Unit tests for RenderSavedReportExecuteStep
 *******************************************************************************/
class RenderSavedReportExecuteStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetDownloadFileBaseName_customName_usesCustomName()
   {
      RunBackendStepInput input  = new RunBackendStepInput();
      SavedReport         report = new SavedReport().withLabel("My Report");
      input.addValue("downloadFileBaseName", "Custom Export");

      String result = RenderSavedReportExecuteStep.getDownloadFileBaseName(input, report);

      assertThat(result).startsWith("Custom Export - ");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetDownloadFileBaseName_noCustomName_fallsBackToReportLabel()
   {
      RunBackendStepInput input  = new RunBackendStepInput();
      SavedReport         report = new SavedReport().withLabel("Sales Summary");
      // downloadFileBaseName not set

      String result = RenderSavedReportExecuteStep.getDownloadFileBaseName(input, report);

      assertThat(result).startsWith("Sales Summary - ");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetDownloadFileBaseName_nameWithSlashes_replacedWithDashes()
   {
      RunBackendStepInput input  = new RunBackendStepInput();
      SavedReport         report = new SavedReport().withLabel("Q1/Q2 Sales");

      String result = RenderSavedReportExecuteStep.getDownloadFileBaseName(input, report);

      assertThat(result).startsWith("Q1-Q2 Sales - ");
      assertFalse(result.contains("/"), "Forward slashes must be replaced");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetDownloadFileBaseName_nameWithCommas_replacedWithUnderscores()
   {
      RunBackendStepInput input  = new RunBackendStepInput();
      SavedReport         report = new SavedReport().withLabel("Sales, Orders, Returns");

      String result = RenderSavedReportExecuteStep.getDownloadFileBaseName(input, report);

      assertThat(result).startsWith("Sales_ Orders_ Returns - ");
      assertFalse(result.contains(","), "Commas must be replaced");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetDownloadFileBaseName_includesDateSuffix()
   {
      RunBackendStepInput input  = new RunBackendStepInput();
      SavedReport         report = new SavedReport().withLabel("Report");

      String result = RenderSavedReportExecuteStep.getDownloadFileBaseName(input, report);

      // date suffix format: yyyy-MM-dd-HHmm
      assertThat(result).matches("Report - \\d{4}-\\d{2}-\\d{2}-\\d{4}");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetDownloadFileBaseName_emptyCustomName_fallsBackToLabel()
   {
      RunBackendStepInput input  = new RunBackendStepInput();
      SavedReport         report = new SavedReport().withLabel("Fallback Label");
      input.addValue("downloadFileBaseName", "");

      String result = RenderSavedReportExecuteStep.getDownloadFileBaseName(input, report);

      assertThat(result).startsWith("Fallback Label - ");
   }

}
