/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.reporting.excel;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.reporting.excel.poi.ExcelPoiBasedStreamingStyleCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/*******************************************************************************
 **
 *******************************************************************************/
public class TestExcelStyler implements ExcelPoiBasedStreamingStyleCustomizerInterface
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Integer> getColumnWidthsForView(QReportView view)
   {
      return List.of(60, 50, 40);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<String> getMergedRangesForView(QReportView view)
   {
      return List.of("A1:B1");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void customizeStyles(Map<String, XSSFCellStyle> styles, XSSFWorkbook workbook, CreationHelper createHelper)
   {
      Font font = workbook.createFont();
      font.setFontHeightInPoints((short) 16);
      font.setBold(true);
      XSSFCellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setFont(font);
      styles.put("header", cellStyle);
   }
}
