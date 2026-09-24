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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QReportingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for ReportUtils
 *******************************************************************************/
@TestMethodOrder(MethodOrderer.MethodName.class)
class ReportUtilsTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(ReportUtilsTest.class);



   /*******************************************************************************
    ** When a pivot table view references an existing data view, the matching view
    ** must be returned.
    *******************************************************************************/
   @Test
   void testGetSourceViewForPivotTableView_matchingViewExists_returnsIt() throws QReportingException
   {
      QReportView dataView = new QReportView()
         .withName("dataView")
         .withType(ReportType.TABLE);

      QReportView pivotView = new QReportView()
         .withName("pivotView")
         .withType(ReportType.PIVOT)
         .withPivotTableSourceViewName("dataView");

      QReportView result = ReportUtils.getSourceViewForPivotTableView(List.of(dataView, pivotView), pivotView);

      assertThat(result).isEqualTo(dataView);
   }



   /*******************************************************************************
    ** When the referenced source view is not in the list, a QReportingException
    ** must be thrown with a useful message.
    *******************************************************************************/
   @Test
   void testGetSourceViewForPivotTableView_missingSourceView_throwsException()
   {
      QReportView pivotView = new QReportView()
         .withName("pivotView")
         .withType(ReportType.PIVOT)
         .withPivotTableSourceViewName("missingDataView");

      assertThatThrownBy(() ->
         ReportUtils.getSourceViewForPivotTableView(List.of(pivotView), pivotView))
         .isInstanceOf(QReportingException.class)
         .hasMessageContaining("missingDataView")
         .hasMessageContaining("pivotView");
   }



   /*******************************************************************************
    ** When multiple views exist, the correct one is matched by name only.
    *******************************************************************************/
   @Test
   void testGetSourceViewForPivotTableView_multipleViews_correctOneReturned() throws QReportingException
   {
      QReportView dataView1 = new QReportView().withName("dataViewA").withType(ReportType.TABLE);
      QReportView dataView2 = new QReportView().withName("dataViewB").withType(ReportType.TABLE);

      QReportView pivotView = new QReportView()
         .withName("pivotView")
         .withType(ReportType.PIVOT)
         .withPivotTableSourceViewName("dataViewB");

      QReportView result = ReportUtils.getSourceViewForPivotTableView(
         List.of(dataView1, dataView2, pivotView), pivotView);

      assertThat(result).isEqualTo(dataView2);
   }



   /*******************************************************************************
    ** An empty view list always throws, since no source can be found.
    *******************************************************************************/
   @Test
   void testGetSourceViewForPivotTableView_emptyViewList_throwsException()
   {
      QReportView pivotView = new QReportView()
         .withName("pivotView")
         .withPivotTableSourceViewName("dataView");

      assertThatThrownBy(() -> ReportUtils.getSourceViewForPivotTableView(List.of(), pivotView))
         .isInstanceOf(QReportingException.class);
   }

}
