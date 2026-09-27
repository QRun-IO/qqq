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


import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.apache.poi.ss.util.CellReference;


/*******************************************************************************
 ** Write excel formatted XML to a Writer.
 ** Originally from https://coderanch.com/t/548897/java/Generate-large-excel-POI
 *******************************************************************************/
public class StreamedSheetWriter
{
   private final Writer writer;
   private       int    rowNo;



   /*******************************************************************************
    **
    *******************************************************************************/
   public StreamedSheetWriter(Writer writer)
   {
      this.writer = writer;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void beginSheet(QReportView view, ExcelPoiBasedStreamingStyleCustomizerInterface styleCustomizerInterface) throws IOException
   {
      writer.write("""
         <?xml version="1.0" encoding="UTF-8"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""");

      if(styleCustomizerInterface != null && view != null)
      {
         List<Integer> columnWidths = styleCustomizerInterface.getColumnWidthsForView(view);
         if(CollectionUtils.nullSafeHasContents(columnWidths))
         {
            writer.write("<cols>");
            for(int i = 0; i < columnWidths.size(); i++)
            {
               Integer width = columnWidths.get(i);
               if(width != null)
               {
                  writer.write("""
                     <col min="%d" max="%d" width="%d" customWidth="1"/>
                     """.formatted(i + 1, i + 1, width));
               }
            }
            writer.write("</cols>");
         }
      }

      writer.write("<sheetData>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void endSheet(QReportView view, ExcelPoiBasedStreamingStyleCustomizerInterface styleCustomizerInterface) throws IOException
   {
      writer.write("</sheetData>");

      if(styleCustomizerInterface != null && view != null)
      {
         List<String> mergedRanges = styleCustomizerInterface.getMergedRangesForView(view);
         if(CollectionUtils.nullSafeHasContents(mergedRanges))
         {
            writer.write(String.format("<mergeCells count=\"%d\">", mergedRanges.size()));
            for(String range : mergedRanges)
            {
               writer.write(String.format("<mergeCell ref=\"%s\"/>", range));
            }
            writer.write("</mergeCells>");
         }
      }

      writer.write("</worksheet>");
   }



   /*******************************************************************************
    ** Insert a new row
    **
    ** @param rowNo 0-based row number
    *******************************************************************************/
   public void insertRow(int rowNo) throws IOException
   {
      writer.write("<row r=\"" + (rowNo + 1) + "\">\n");
      this.rowNo = rowNo;
   }



   /*******************************************************************************
    ** Insert row end marker
    *******************************************************************************/
   public void endRow() throws IOException
   {
      writer.write("</row>\n");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void createCell(int columnIndex, String value, int styleIndex) throws IOException
   {
      String ref = new CellReference(rowNo, columnIndex).formatAsString();
      writer.write("<c r=\"" + ref + "\" t=\"inlineStr\"");
      if(styleIndex != -1)
      {
         writer.write(" s=\"" + styleIndex + "\"");
      }

      String cleanValue = cleanseValue(value);

      writer.write(">");
      writer.write("<is><t>" + cleanValue + "</t></is>");
      writer.write("</c>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String cleanseValue(String value)
   {
      if(value != null)
      {
         StringBuilder rs = new StringBuilder();
         for(int i = 0; i < value.length(); i++)
         {
            char c = value.charAt(i);
            if(c == '&')
            {
               rs.append("&amp;");
            }
            else if(c == '<')
            {
               rs.append("&lt;");
            }
            else if(c == '>')
            {
               rs.append("&gt;");
            }
            else if(c == '\'')
            {
               rs.append("&apos;");
            }
            else if(c == '"')
            {
               rs.append("&quot;");
            }
            else if(c < 32 && c != '\t' && c != '\n')
            {
               rs.append(' ');
            }
            else
            {
               rs.append(c);
            }
         }

         Map<String, Integer> m = new HashMap<>();
         m.computeIfAbsent("s", (s) -> 3);

         value = rs.toString();
      }

      return (value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void createCell(int columnIndex, String value) throws IOException
   {
      createCell(columnIndex, value, -1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void createCell(int columnIndex, double value, int styleIndex) throws IOException
   {
      String ref = new CellReference(rowNo, columnIndex).formatAsString();
      writer.write("<c r=\"" + ref + "\" t=\"n\"");
      if(styleIndex != -1)
      {
         writer.write(" s=\"" + styleIndex + "\"");
      }
      writer.write(">");
      writer.write("<v>" + value + "</v>");
      writer.write("</c>");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void createCell(int columnIndex, double value) throws IOException
   {
      createCell(columnIndex, value, -1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void createCell(int columnIndex, Boolean value) throws IOException
   {
      createCell(columnIndex, value, -1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void createCell(int columnIndex, Boolean value, int styleIndex) throws IOException
   {
      String ref = new CellReference(rowNo, columnIndex).formatAsString();
      writer.write("<c r=\"" + ref + "\" t=\"b\"");
      if(styleIndex != -1)
      {
         writer.write(" s=\"" + styleIndex + "\"");
      }
      writer.write(">");
      if(value != null)
      {
         writer.write("<v>" + (value ? 1 : 0) + "</v>");
      }
      writer.write("</c>");
   }

}
