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
