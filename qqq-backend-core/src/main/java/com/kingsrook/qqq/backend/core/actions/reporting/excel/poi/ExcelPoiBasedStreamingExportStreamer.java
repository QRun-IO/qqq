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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportStreamerInterface;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportStyleCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.reporting.ReportUtils;
import com.kingsrook.qqq.backend.core.exceptions.QReportingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Excel export format implementation using POI library, but with modifications
 ** to actually stream output rather than use any temp files.
 **
 ** For a rough outline:
 ** - create a basically empty Excel workbook using POI - empty meaning, without
 ** data rows.
 ** - have POI write that workbook out into a byte[] - which will be a zip full
 ** of xml (e.g., xlsx).
 ** - then open a new ZipOutputStream wrapper around the OutputStream we took in
 ** as the report destination (e.g., streamed into storage or http output)
 ** - Copy over all entries from the xlsx into our new zip-output-stream, other than
 ** ones that are the actual sheets that we want to put data into.
 ** - For the sheet entries, use the StreamedPoiSheetWriter class to write the
 ** report's data directly out as Excel XML (not using POI).
 ** - Pivot tables require a bit of an additional hack - to write a "pivot cache
 ** definition", which, while we won't put all the data in it (because we'll tell
 ** it refreshOnLoad="true"), we will at least need to know how many cols & rows
 ** are in the data-sheet (which we wouldn't know until we streamed that sheet!)
 *******************************************************************************/
public class ExcelPoiBasedStreamingExportStreamer implements ExportStreamerInterface
{
   private static final QLogger LOG = QLogger.getLogger(ExcelPoiBasedStreamingExportStreamer.class);

   private List<QReportView>    views;
   private ExportInput          exportInput;
   private List<QFieldMetaData> fields;
   private OutputStream         outputStream;
   private ZipOutputStream      zipOutputStream;

   public static final String EXCEL_DATE_FORMAT      = "yyyy-MM-dd";
   public static final String EXCEL_DATE_TIME_FORMAT = "yyyy-MM-dd H:mm:ss";

   private ExcelPoiBasedStreamingStyleCustomizerInterface styleCustomizerInterface;

   private Map<String, String>        excelCellFormats;
   private Map<String, XSSFCellStyle> styles = new HashMap<>();

   private int rowNo      = 0;
   private int sheetIndex = 1;

   private Map<String, String> pivotViewToCacheDefinitionReferenceMap = new HashMap<>();

   private Writer              activeSheetWriter = null;
   private StreamedSheetWriter sheetWriter       = null;

   private QReportView                       currentView              = null;
   private Map<String, List<QFieldMetaData>> fieldsPerView            = new HashMap<>();
   private Map<String, Integer>              rowsPerView              = new HashMap<>();
   private Map<String, String>               labelViewsByName         = new HashMap<>();
   private Map<String, String>               sheetReferenceByViewName = new HashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public ExcelPoiBasedStreamingExportStreamer()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void preRun(ReportDestination reportDestination, List<QReportView> views) throws QReportingException
   {
      try
      {
         this.outputStream = reportDestination.getReportOutputStream();
         this.views = views;

         ///////////////////////////////////////////////////////////////////////////////
         // create 'template' workbook through poi - with sheets corresponding to our //
         // actual file this will be a zip file (stream), with entries for all of the //
         // files in the final xlsx but without any data, so it'll be small           //
         ///////////////////////////////////////////////////////////////////////////////
         XSSFWorkbook workbook = new XSSFWorkbook();
         createStyles(workbook);

         //////////////////////////////////////////////////////////////////////////////////////////////////
         // for each of the sheets, create it in the workbook, and put a reference to it in the sheetMap //
         //////////////////////////////////////////////////////////////////////////////////////////////////
         Map<String, XSSFSheet> sheetMapByExcelReference = new HashMap<>();
         Map<String, XSSFSheet> sheetMapByViewName       = new HashMap<>();

         int sheetCounter = 1;
         for(QReportView view : views)
         {
            String label = Objects.requireNonNullElse(view.getLabel(), "Sheet " + sheetCounter);
            label = WorkbookUtil.createSafeSheetName(label);

            /////////////////////////////////////////////////////////////////////////////////////////////
            // track the actually-used sheet labels (needed for referencing in pivot table generation) //
            /////////////////////////////////////////////////////////////////////////////////////////////
            labelViewsByName.put(view.getName(), label);

            XSSFSheet sheet          = workbook.createSheet(label);
            String    sheetReference = sheet.getPackagePart().getPartName().getName().substring(1);
            sheetMapByExcelReference.put(sheetReference, sheet);
            sheetMapByViewName.put(view.getName(), sheet);
            sheetReferenceByViewName.put(view.getName(), sheetReference);
            sheetCounter++;
         }

         ////////////////////////////////////////////////////
         // if any views are pivot tables, create them now //
         ////////////////////////////////////////////////////
         List<String> pivotViewNames = new ArrayList<>();
         for(QReportView view : views)
         {
            if(ReportType.PIVOT.equals(view.getType()))
            {
               pivotViewNames.add(view.getName());

               XSSFSheet   pivotTableSheet = Objects.requireNonNull(sheetMapByViewName.get(view.getName()), "Could not get pivot table sheet view by name: " + view.getName());
               XSSFSheet   dataSheet       = Objects.requireNonNull(sheetMapByViewName.get(view.getPivotTableSourceViewName()), "Could not get pivot table source sheet by view name: " + view.getPivotTableSourceViewName());
               QReportView dataView        = ReportUtils.getSourceViewForPivotTableView(views, view);
               createPivotTableTemplate(pivotTableSheet, view, dataSheet, dataView);
            }
         }
         Iterator<String> pivotViewNameIterator = pivotViewNames.iterator();

         /////////////////////////////////////////////////////////
         // write that template worksheet zip out to byte array //
         /////////////////////////////////////////////////////////
         ByteArrayOutputStream templateBAOS = new ByteArrayOutputStream();
         workbook.write(templateBAOS);
         templateBAOS.close();
         byte[] templateBytes = templateBAOS.toByteArray();

         /////////////////////////////////////////////////////////////////////////////////////////////
         // open up a zipOutputStream around the output stream that the report is to be written to. //
         // note:  this stream is closed in the finish method - see more comments there.            //
         /////////////////////////////////////////////////////////////////////////////////////////////
         this.zipOutputStream = new ZipOutputStream(this.outputStream);

         /////////////////////////////////////////////////////////////////////////////////////////////////
         // copy over all the entries in the template zip that aren't the sheets into the output stream //
         /////////////////////////////////////////////////////////////////////////////////////////////////
         ZipInputStream zipInputStream   = new ZipInputStream(new ByteArrayInputStream(templateBytes));
         ZipEntry       zipTemplateEntry = null;
         byte[]         buffer           = new byte[2048];
         while((zipTemplateEntry = zipInputStream.getNextEntry()) != null)
         {
            if(zipTemplateEntry.getName().matches(".*/pivotCacheDefinition.*.xml"))
            {
               //////////////////////////////////////////////////////////////////////////////////////////////////////
               // if this zip entry is a pivotCacheDefinition, then don't write it to the output stream right now. //
               // instead, just map the pivot view's name to the zipTemplateEntry name                             //
               //////////////////////////////////////////////////////////////////////////////////////////////////////
               if(!pivotViewNameIterator.hasNext())
               {
                  throw new QReportingException("Found a pivot cache definition [" + zipTemplateEntry.getName() + "] in the template ZIP, but no (more) corresponding pivot view names");
               }

               String pivotViewName = pivotViewNameIterator.next();
               LOG.info("Holding on a pivot cache definition zip template entry [" + pivotViewName + "] [" + zipTemplateEntry.getName() + "]...");
               pivotViewToCacheDefinitionReferenceMap.put(pivotViewName, zipTemplateEntry.getName());
            }
            else if(!sheetMapByExcelReference.containsKey(zipTemplateEntry.getName()))
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // else, if we don't have this zipTemplateEntry name in our map of sheets, then this is a kinda "meta"    //
               // file that we don't really care about (e.g., not our sheet data), so just copy it to the output stream. //
               ////////////////////////////////////////////////////////////////////////////////////////////////////////////
               LOG.info("Copying zip template entry [" + zipTemplateEntry.getName() + "] to output stream");
               zipOutputStream.putNextEntry(new ZipEntry(zipTemplateEntry.getName()));

               int length;
               while((length = zipInputStream.read(buffer)) > 0)
               {
                  zipOutputStream.write(buffer, 0, length);
               }

               zipInputStream.closeEntry();
            }
            else
            {
               ////////////////////////////////////////////////////////////////////////////////////
               // else - this is a sheet - so again, don't write it yet - stream its data below. //
               ////////////////////////////////////////////////////////////////////////////////////
               LOG.info("Skipping presumed sheet zip template entry [" + zipTemplateEntry.getName() + "] to output stream");
            }
         }

         zipInputStream.close();
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error preparing to generate spreadsheet", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void createPivotTableTemplate(XSSFSheet pivotTableSheet, QReportView pivotView, XSSFSheet dataSheet, QReportView dataView) throws QReportingException
   {
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // write just enough data to the dataView's sheet so that we can refer to it for creating the pivot table. //
      // we need to do this, because POI will try to create the pivotCache referring to the data sheet, and if   //
      // there isn't any data there, it'll crash.                                                                //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      XSSFRow headerRow = dataSheet.createRow(0);
      int     columnNo  = 0;
      for(QReportField column : dataView.getColumns())
      {
         XSSFCell cell = headerRow.createCell(columnNo++);
         cell.setCellValue(QInstanceEnricher.nameToLabel(column.getName()));
      }

      XSSFRow valuesRow = dataSheet.createRow(1);
      columnNo = 0;
      for(QReportField column : dataView.getColumns())
      {
         XSSFCell cell = valuesRow.createCell(columnNo++);
         cell.setCellValue("Value " + columnNo);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // for this template version of the pivot table, tell it there are only 2 rows in the source sheet //
      // as that's all that we wrote above (a header and 1 fake value row)                               //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      int           rows       = 2;
      String        colsLetter = CellReference.convertNumToColString(dataView.getColumns().size() - 1);
      AreaReference source     = new AreaReference("A1:" + colsLetter + rows, SpreadsheetVersion.EXCEL2007);
      CellReference position   = new CellReference("A1");

      //////////////////////////////////////////////////////////////////
      // tell poi all about our pivot table - rows, cols, and columns //
      //////////////////////////////////////////////////////////////////
      XSSFPivotTable pivotTable = pivotTableSheet.createPivotTable(source, position, dataSheet);

      for(PivotTableGroupBy row : CollectionUtils.nonNullList(pivotView.getPivotTableDefinition().getRows()))
      {
         int rowLabelColumnIndex = getColumnIndex(dataView.getColumns(), row.getFieldName());
         pivotTable.addRowLabel(rowLabelColumnIndex);
      }

      for(PivotTableGroupBy column : CollectionUtils.nonNullList(pivotView.getPivotTableDefinition().getColumns()))
      {
         int colLabelColumnIndex = getColumnIndex(dataView.getColumns(), column.getFieldName());
         pivotTable.addColLabel(colLabelColumnIndex);
      }

      for(PivotTableValue value : CollectionUtils.nonNullList(pivotView.getPivotTableDefinition().getValues()))
      {
         int columnLabelColumnIndex = getColumnIndex(dataView.getColumns(), value.getFieldName());

         String labelPrefix = value.getFunction().name() + " of ";
         String label       = labelPrefix + QInstanceEnricher.nameToLabel(value.getFieldName());
         String valueFormat = null;

         Optional<QReportField> optSourceField = dataView.getColumns().stream().filter(c -> c.getName().equals(value.getFieldName())).findFirst();
         if(optSourceField.isPresent())
         {
            QReportField sourceField = optSourceField.get();

            if(StringUtils.hasContent(sourceField.getLabel()))
            {
               label = labelPrefix + sourceField.getLabel();
            }

            if(StringUtils.hasContent(sourceField.getDisplayFormat()))
            {
               valueFormat = DisplayFormat.getExcelFormat(sourceField.getDisplayFormat());
            }
            else
            {
               if(QFieldType.DATE.equals(sourceField.getType()))
               {
                  valueFormat = EXCEL_DATE_FORMAT;
               }
               else if(QFieldType.DATE_TIME.equals(sourceField.getType()))
               {
                  valueFormat = EXCEL_DATE_TIME_FORMAT;
               }
            }
         }

         pivotTable.addColumnLabel(DataConsolidateFunction.valueOf(value.getFunction().name()), columnLabelColumnIndex, label, valueFormat);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int getColumnIndex(List<QReportField> columns, String fieldName) throws QReportingException
   {
      for(int i = 0; i < columns.size(); i++)
      {
         if(columns.get(i).getName().equals(fieldName))
         {
            return (i);
         }
      }

      throw (new QReportingException("Could not find column by name [" + fieldName + "]"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void createStyles(XSSFWorkbook workbook)
   {
      CreationHelper createHelper = workbook.getCreationHelper();

      XSSFCellStyle dateStyle = workbook.createCellStyle();
      dateStyle.setDataFormat(createHelper.createDataFormat().getFormat(EXCEL_DATE_FORMAT));
      styles.put("date", dateStyle);

      XSSFCellStyle dateTimeStyle = workbook.createCellStyle();
      dateTimeStyle.setDataFormat(createHelper.createDataFormat().getFormat(EXCEL_DATE_TIME_FORMAT));
      styles.put("datetime", dateTimeStyle);

      PoiExcelStylerInterface poiExcelStylerInterface = getStylerInterface();
      styles.put("title", poiExcelStylerInterface.createStyleForTitle(workbook, createHelper));
      styles.put("header", poiExcelStylerInterface.createStyleForHeader(workbook, createHelper));
      styles.put("footer", poiExcelStylerInterface.createStyleForFooter(workbook, createHelper));

      XSSFCellStyle footerDateStyle = poiExcelStylerInterface.createStyleForFooter(workbook, createHelper);
      footerDateStyle.setDataFormat(createHelper.createDataFormat().getFormat(EXCEL_DATE_FORMAT));
      styles.put("footer-date", footerDateStyle);

      XSSFCellStyle footerDateTimeStyle = poiExcelStylerInterface.createStyleForFooter(workbook, createHelper);
      footerDateTimeStyle.setDataFormat(createHelper.createDataFormat().getFormat(EXCEL_DATE_TIME_FORMAT));
      styles.put("footer-datetime", footerDateTimeStyle);

      if(styleCustomizerInterface != null)
      {
         styleCustomizerInterface.customizeStyles(styles, workbook, createHelper);
      }
   }



   /*******************************************************************************
    ** Starts a new worksheet in the current workbook.  Can be called multiple times.
    *******************************************************************************/
   @Override
   public void start(ExportInput exportInput, List<QFieldMetaData> fields, String label, QReportView view) throws QReportingException
   {
      try
      {
         /////////////////////////////////////////
         // close previous sheet if one is open //
         /////////////////////////////////////////
         closeLastSheetIfOpen();

         if(currentView != null)
         {
            this.rowsPerView.put(currentView.getName(), rowNo);
         }

         this.currentView = view;
         this.exportInput = exportInput;
         this.fields = fields;
         this.rowNo = 0;

         this.fieldsPerView.put(view.getName(), fields);

         //////////////////////////////////////////
         // start the new sheet as:              //
         // - a new entry in the zipOutputStream //
         // - with a new output stream writer    //
         // - and with a SpreadsheetWriter       //
         //////////////////////////////////////////
         zipOutputStream.putNextEntry(new ZipEntry(sheetReferenceByViewName.get(view.getName())));
         activeSheetWriter = new OutputStreamWriter(zipOutputStream);
         sheetWriter = new StreamedSheetWriter(activeSheetWriter);

         if(ReportType.PIVOT.equals(view.getType()))
         {
            writePivotTable(view, ReportUtils.getSourceViewForPivotTableView(views, view));
         }
         else
         {
            sheetWriter.beginSheet(view, styleCustomizerInterface);

            ////////////////////////////////////////////////
            // put the title and header rows in the sheet //
            ////////////////////////////////////////////////
            writeTitleAndHeader();
         }
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error starting worksheet", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeTitleAndHeader() throws QReportingException
   {
      try
      {
         ///////////////
         // title row //
         ///////////////
         if(StringUtils.hasContent(exportInput.getTitleRow()))
         {
            sheetWriter.insertRow(rowNo++);
            sheetWriter.createCell(0, exportInput.getTitleRow(), styles.get("title").getIndex());
            sheetWriter.endRow();
         }

         ////////////////
         // header row //
         ////////////////
         if(exportInput.getIncludeHeaderRow())
         {
            sheetWriter.insertRow(rowNo++);
            XSSFCellStyle headerStyle = styles.get("header");

            int col = 0;
            for(QFieldMetaData column : fields)
            {
               sheetWriter.createCell(col, column.getLabel(), headerStyle.getIndex());
               col++;
            }

            sheetWriter.endRow();
         }
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error starting Excel report"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addRecords(List<QRecord> qRecords) throws QReportingException
   {
      LOG.info("Consuming [" + qRecords.size() + "] records from the pipe");

      try
      {
         for(QRecord qRecord : qRecords)
         {
            writeRecord(qRecord);
         }
      }
      catch(Exception e)
      {
         LOG.error("Exception generating excel file", e);
         try
         {
            outputStream.close();
         }
         catch(IOException ex)
         {
            LOG.warn("Secondary error closing excel output stream", e);
         }

         throw (new QReportingException("Error generating Excel report", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeRecord(QRecord qRecord) throws IOException
   {
      writeRecord(qRecord, false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void setStyleForField(QRecord record, String fieldName, String styleName)
   {
      record.setDisplayValue(fieldName + ":excelStyle", styleName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeRecord(QRecord qRecord, boolean isFooter) throws IOException
   {
      sheetWriter.insertRow(rowNo++);

      int baseStyleIndex     = -1;
      int dateStyleIndex     = styles.get("date").getIndex();
      int dateTimeStyleIndex = styles.get("datetime").getIndex();
      if(isFooter)
      {
         baseStyleIndex = styles.get("footer").getIndex();
         dateStyleIndex = styles.get("footer-date").getIndex();
         dateTimeStyleIndex = styles.get("footer-datetime").getIndex();
      }

      int col = 0;
      for(QFieldMetaData field : fields)
      {
         Serializable value = qRecord.getValue(field.getName());

         String overrideStyleName = qRecord.getDisplayValue(field.getName() + ":excelStyle");
         int    styleIndex        = baseStyleIndex;
         if(overrideStyleName != null)
         {
            styleIndex = styles.get(overrideStyleName).getIndex();
         }

         if(value != null)
         {
            if(value instanceof String s)
            {
               sheetWriter.createCell(col, s, styleIndex);
            }
            else if(value instanceof Number n)
            {
               sheetWriter.createCell(col, n.doubleValue(), styleIndex);

               if(excelCellFormats != null)
               {
                  String format = excelCellFormats.get(field.getName());
                  if(format != null)
                  {
                     ////////////////////////////////////////////////////////////////////////////////////////////
                     // todo - so - for this streamed/zip approach, we need to know all styles before we start //
                     // any sheets.  but, right now Report action only calls us with per-sheet styles when     //
                     // it's starting individual sheets.  so, we can't quite support this at this time.        //
                     // "just" need to change GenerateReportAction to look up all cell formats for all sheets  //
                     // before preRun is called... and change all existing streamer classes to handle that too //
                     ////////////////////////////////////////////////////////////////////////////////////////////
                     // worksheet.style(rowNo, col).format(format).set();
                  }
               }
            }
            else if(value instanceof Boolean b)
            {
               sheetWriter.createCell(col, b, styleIndex);
            }
            else if(value instanceof Date d)
            {
               sheetWriter.createCell(col, DateUtil.getExcelDate(d), dateStyleIndex);
            }
            else if(value instanceof LocalDate d)
            {
               sheetWriter.createCell(col, DateUtil.getExcelDate(d), dateStyleIndex);
            }
            else if(value instanceof LocalDateTime d)
            {
               sheetWriter.createCell(col, DateUtil.getExcelDate(d), dateStyleIndex);
            }
            else if(value instanceof ZonedDateTime d)
            {
               sheetWriter.createCell(col, DateUtil.getExcelDate(d.toLocalDateTime()), dateTimeStyleIndex);
            }
            else if(value instanceof Instant i)
            {
               sheetWriter.createCell(col, DateUtil.getExcelDate(i.atZone(ZoneId.systemDefault()).toLocalDateTime()), dateTimeStyleIndex);
            }
            else
            {
               sheetWriter.createCell(col, ValueUtils.getValueAsString(value), styleIndex);
            }
         }

         col++;
      }

      sheetWriter.endRow();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addTotalsRow(QRecord record) throws QReportingException
   {
      try
      {
         writeRecord(record, true);
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error adding totals row", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void finish() throws QReportingException
   {
      try
      {
         //////////////////////////////////////////////
         // close the last open sheet if one is open //
         //////////////////////////////////////////////
         closeLastSheetIfOpen();

         /////////////////////////////////////////////////////////////////////////////////////
         // so, we DO need a close here, on the zipOutputStream, to finish its "zippiness". //
         // even though, doing so also closes the outputStream from the caller that this    //
         // zipOutputStream is wrapping (and the caller will likely call close on too)...   //
         /////////////////////////////////////////////////////////////////////////////////////
         zipOutputStream.close();
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error finishing Excel report", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void closeLastSheetIfOpen() throws IOException
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // if we have an active sheet writer:                                                               //
      // - end the current sheet in the spreadsheet writer (write some closing xml, unless it's a pivot!) //
      // - flush the contents through the activeSheetWriter                                               //
      // - close the zip entry in the output stream.                                                      //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      if(activeSheetWriter != null)
      {
         if(!ReportType.PIVOT.equals(currentView.getType()))
         {
            sheetWriter.endSheet(currentView, styleCustomizerInterface);
         }

         activeSheetWriter.flush();
         zipOutputStream.closeEntry();
      }
   }



   /*******************************************************************************
    ** display formats is a map of field name to Excel format strings (e.g., $#,##0.00)
    *******************************************************************************/
   @Override
   public void setDisplayFormats(Map<String, String> displayFormats)
   {
      this.excelCellFormats = new HashMap<>();
      for(Map.Entry<String, String> entry : displayFormats.entrySet())
      {
         String excelFormat = DisplayFormat.getExcelFormat(entry.getValue());
         if(excelFormat != null)
         {
            excelCellFormats.put(entry.getKey(), excelFormat);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writePivotTable(QReportView pivotTableView, QReportView dataView) throws QReportingException
   {
      try
      {
         //////////////////////////////////////////////////////////////////////////////////
         // write the xml file that is the pivot table sheet.                            //
         // note that the ZipEntry here will have been started above in the start method //
         //////////////////////////////////////////////////////////////////////////////////
         activeSheetWriter.write("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
              <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:mx="http://schemas.microsoft.com/office/mac/excel/2008/main" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" xmlns:mv="urn:schemas-microsoft-com:mac:vml" xmlns:x14="http://schemas.microsoft.com/office/spreadsheetml/2009/9/main" xmlns:x15="http://schemas.microsoft.com/office/spreadsheetml/2010/11/main" xmlns:x14ac="http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac" xmlns:xm="http://schemas.microsoft.com/office/excel/2006/main">
                 <sheetPr>
                   <outlinePr summaryBelow="0" summaryRight="0"/>
                 </sheetPr>
                 <sheetViews>
                   <sheetView workbookViewId="0"/>
                 </sheetViews>
                 <sheetFormatPr customHeight="1" defaultColWidth="14.43" defaultRowHeight="15.0"/>
                 <sheetData>
                   <row r="1"/>
                 </sheetData>
              </worksheet>
            """);
         activeSheetWriter.flush();

         ////////////////////////////////////////////////////////////////////////////
         // start a new zip entry, for this pivot view's cacheDefinition reference //
         ////////////////////////////////////////////////////////////////////////////
         zipOutputStream.putNextEntry(new ZipEntry(pivotViewToCacheDefinitionReferenceMap.get(pivotTableView.getName())));

         /////////////////////////////////////////////////////////
         // prepare the xml for each field (e.g., w/ its label) //
         /////////////////////////////////////////////////////////
         List<String> cachedFieldElements = new ArrayList<>();
         for(QFieldMetaData column : this.fieldsPerView.get(dataView.getName()))
         {
            cachedFieldElements.add(String.format("""
               <cacheField numFmtId="0" name="%s">
                  <sharedItems/>
               </cacheField>
               """, column.getLabel()));
         }

         /////////////////////////////////////////////////////////////////////////////////////
         // write the xml file that is the pivot cache definition (structure only, no data) //
         /////////////////////////////////////////////////////////////////////////////////////
         activeSheetWriter = new OutputStreamWriter(zipOutputStream);
         activeSheetWriter.write(String.format("""
               <?xml version="1.0" encoding="UTF-8"?>
               <pivotCacheDefinition xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" createdVersion="3" minRefreshableVersion="3" refreshedVersion="3" refreshedBy="Apache POI" refreshedDate="1.711395767702E12" refreshOnLoad="true" r:id="rId1">
                 <cacheSource type="worksheet">
                   <worksheetSource sheet="%s" ref="A1:%s%d"/>
                 </cacheSource>
                 <cacheFields count="%d">
                   %s
                 </cacheFields>
               </pivotCacheDefinition>
               """,
            StreamedSheetWriter.cleanseValue(labelViewsByName.get(dataView.getName())),
            CellReference.convertNumToColString(dataView.getColumns().size() - 1),
            rowsPerView.get(dataView.getName()),
            dataView.getColumns().size(),
            StringUtils.join("\n", cachedFieldElements)));
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error writing pivot table", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected PoiExcelStylerInterface getStylerInterface()
   {
      if(styleCustomizerInterface != null)
      {
         return styleCustomizerInterface.getExcelStyler();
      }

      return (new PlainPoiExcelStyler());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setExportStyleCustomizer(ExportStyleCustomizerInterface exportStyleCustomizer)
   {
      if(exportStyleCustomizer instanceof ExcelPoiBasedStreamingStyleCustomizerInterface poiExcelStylerInterface)
      {
         this.styleCustomizerInterface = poiExcelStylerInterface;
      }
      else
      {
         LOG.debug("Supplied export style customizer is not an instance of ExcelPoiStyleCustomizerInterface, so will not be used for an excel export", logPair("exportStyleCustomizerClass", exportStyleCustomizer.getClass().getSimpleName()));
      }
   }
}
