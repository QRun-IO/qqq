/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.reporting;


import java.util.Locale;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.reporting.CsvExportStreamer;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportStreamerInterface;
import com.kingsrook.qqq.backend.core.actions.reporting.JsonExportStreamer;
import com.kingsrook.qqq.backend.core.actions.reporting.ListOfMapsExportStreamer;
import com.kingsrook.qqq.backend.core.actions.reporting.TsvExportStreamer;
import com.kingsrook.qqq.backend.core.actions.reporting.excel.poi.ExcelPoiBasedStreamingExportStreamer;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.poi.ss.SpreadsheetVersion;


/*******************************************************************************
 ** QQQ Report/export file formats
 *******************************************************************************/
public enum ReportFormat
{
   /////////////////////////////////////////////////////////////////////////
   // if we need to fall back to Fastexcel, this was its version of this. //
   /////////////////////////////////////////////////////////////////////////
   // XLSX(Worksheet.MAX_ROWS, Worksheet.MAX_COLS, ExcelFastexcelExportStreamer::new, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", true, false, true),

   XLSX(SpreadsheetVersion.EXCEL2007.getMaxRows(), SpreadsheetVersion.EXCEL2007.getMaxColumns(), ExcelPoiBasedStreamingExportStreamer::new, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", true, true, true),
   JSON(null, null, JsonExportStreamer::new, "application/json", "json", false, false, true),
   CSV(null, null, CsvExportStreamer::new, "text/csv", "csv", false, false, false),
   TSV(null, null, TsvExportStreamer::new, "text/tab-separated-values", "tsv", false, false, false),
   LIST_OF_MAPS(null, null, ListOfMapsExportStreamer::new, null, null, false, false, true);


   private final Integer maxRows;
   private final Integer maxCols;
   private final String  mimeType;
   private final String  extension;
   private final boolean isBinary;
   private final boolean supportsNativePivotTables;
   private final boolean supportsMultipleViews;

   private final Supplier<? extends ExportStreamerInterface> streamerConstructor;



   /*******************************************************************************
    **
    *******************************************************************************/
   ReportFormat(Integer maxRows, Integer maxCols, Supplier<? extends ExportStreamerInterface> streamerConstructor, String mimeType, String extension, boolean isBinary, boolean supportsNativePivotTables, boolean supportsMultipleViews)
   {
      this.maxRows = maxRows;
      this.maxCols = maxCols;
      this.mimeType = mimeType;
      this.streamerConstructor = streamerConstructor;
      this.extension = extension;
      this.isBinary = isBinary;
      this.supportsNativePivotTables = supportsNativePivotTables;
      this.supportsMultipleViews = supportsMultipleViews;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ReportFormat fromString(String format) throws QUserFacingException
   {
      if(!StringUtils.hasContent(format))
      {
         throw (new QUserFacingException("Report format was not specified."));
      }

      try
      {
         return (ReportFormat.valueOf(format.toUpperCase(Locale.ROOT)));
      }
      catch(IllegalArgumentException iae)
      {
         throw (new QUserFacingException("Unsupported report format: " + format + "."));
      }
   }



   /*******************************************************************************
    ** Getter for maxRows
    **
    *******************************************************************************/
   public Integer getMaxRows()
   {
      return maxRows;
   }



   /*******************************************************************************
    ** Getter for maxCols
    **
    *******************************************************************************/
   public Integer getMaxCols()
   {
      return maxCols;
   }



   /*******************************************************************************
    ** Getter for mimeType
    **
    *******************************************************************************/
   public String getMimeType()
   {
      return mimeType;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ExportStreamerInterface newReportStreamer()
   {
      return (streamerConstructor.get());
   }



   /*******************************************************************************
    ** Getter for extension
    **
    *******************************************************************************/
   public String getExtension()
   {
      return extension;
   }



   /*******************************************************************************
    ** Getter for isBinary
    **
    *******************************************************************************/
   public boolean getIsBinary()
   {
      return isBinary;
   }



   /*******************************************************************************
    ** Getter for supportsNativePivotTables
    **
    *******************************************************************************/
   public boolean getSupportsNativePivotTables()
   {
      return supportsNativePivotTables;
   }



   /*******************************************************************************
    ** Getter for supportsMultipleViews
    **
    *******************************************************************************/
   public boolean getSupportsMultipleViews()
   {
      return supportsMultipleViews;
   }
}
