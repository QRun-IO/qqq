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

package com.kingsrook.qqq.backend.core.actions.reporting.excel.poi;


import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/*******************************************************************************
 ** Version of POI excel styler that does bold headers and footers, with basic borders.
 *******************************************************************************/
public class BoldHeaderAndFooterPoiExcelStyler implements PoiExcelStylerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public XSSFCellStyle createStyleForTitle(XSSFWorkbook workbook, CreationHelper createHelper)
   {
      Font font = workbook.createFont();
      font.setFontHeightInPoints((short) 14);
      font.setBold(true);

      XSSFCellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setFont(font);
      cellStyle.setAlignment(HorizontalAlignment.CENTER);

      return (cellStyle);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public XSSFCellStyle createStyleForHeader(XSSFWorkbook workbook, CreationHelper createHelper)
   {
      Font font = workbook.createFont();
      font.setBold(true);

      XSSFCellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setFont(font);
      cellStyle.setBorderBottom(BorderStyle.THIN);

      return (cellStyle);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public XSSFCellStyle createStyleForFooter(XSSFWorkbook workbook, CreationHelper createHelper)
   {
      Font font = workbook.createFont();
      font.setBold(true);

      XSSFCellStyle cellStyle = workbook.createCellStyle();
      cellStyle.setFont(font);
      cellStyle.setBorderTop(BorderStyle.THIN);
      cellStyle.setBorderBottom(BorderStyle.DOUBLE);

      return (cellStyle);
   }

}
