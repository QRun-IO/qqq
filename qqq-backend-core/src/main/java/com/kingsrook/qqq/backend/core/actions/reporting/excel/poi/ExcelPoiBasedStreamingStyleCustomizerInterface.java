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

package com.kingsrook.qqq.backend.core.actions.reporting.excel.poi;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportStyleCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/*******************************************************************************
 ** style customization points for Excel files generated via our streaming POI.
 *******************************************************************************/
public interface ExcelPoiBasedStreamingStyleCustomizerInterface extends ExportStyleCustomizerInterface
{
   /***************************************************************************
    ** slightly legacy way we did excel styles - but get an instance of object
    ** that defaults "default" styles (header, footer, etc).
    ***************************************************************************/
   default PoiExcelStylerInterface getExcelStyler()
   {
      return (new PlainPoiExcelStyler());
   }


   /***************************************************************************
    ** either change "default" styles put in the styles map, or create new ones
    ** which can then be applied to row/field values (cells) via:
    ** ExcelPoiBasedStreamingExportStreamer.setStyleForField(row, fieldName, styleName);
    ***************************************************************************/
   default void customizeStyles(Map<String, XSSFCellStyle> styles, XSSFWorkbook workbook, CreationHelper createHelper)
   {
      //////////////////
      // noop default //
      //////////////////
   }


   /***************************************************************************
    ** for a given view (sheet), return a list of custom column widths.
    ** any nulls in the list are ignored (so default width is used).
    ***************************************************************************/
   default List<Integer> getColumnWidthsForView(QReportView view)
   {
      return (null);
   }


   /***************************************************************************
    ** for a given view (sheet), return a list of any ranges which should be
    ** merged, as in "A1:C1" (first three cells in first row).
    ***************************************************************************/
   default List<String> getMergedRangesForView(QReportView view)
   {
      return (null);
   }

}
