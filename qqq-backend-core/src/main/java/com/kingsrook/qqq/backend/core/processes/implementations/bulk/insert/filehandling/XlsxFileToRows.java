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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling;


import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class XlsxFileToRows extends AbstractIteratorBasedFileToRows<org.dhatim.fastexcel.reader.Row> implements FileToRowsInterface
{
   private static final QLogger LOG = QLogger.getLogger(XlsxFileToRows.class);

   private static final Pattern DAY_PATTERN = Pattern.compile(".*\\b(d|dd)\\b.*");

   private ReadableWorkbook                        workbook;
   private Stream<org.dhatim.fastexcel.reader.Row> rows;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void init(InputStream inputStream) throws QException
   {
      try
      {
         workbook = new ReadableWorkbook(inputStream, new ReadingOptions(true, true));
         Sheet sheet = workbook.getFirstSheet();

         rows = sheet.openStream();
         setIterator(rows.iterator());
      }
      catch(IOException e)
      {
         throw new QException("Error opening XLSX Parser", e);
      }
   }


   /***************************************************************************
    ** open/go-to a specific sheet (by 0-based index).  resets rows & iterator.
    ***************************************************************************/
   public void openSheet(int index) throws IOException
   {
      Optional<Sheet> sheet = workbook.getSheet(index);

      if(sheet.isEmpty())
      {
         throw (new IOException("No sheet found for index: " + index));
      }

      rows = sheet.get().openStream();
      setIterator(rows.iterator());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BulkLoadFileRow makeRow(org.dhatim.fastexcel.reader.Row readerRow)
   {
      Serializable[] values = new Serializable[readerRow.getCellCount()];

      for(int i = 0; i < readerRow.getCellCount(); i++)
      {
         values[i] = processCell(readerRow, i);
      }

      return new BulkLoadFileRow(values, getRowNo());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private Serializable processCell(Row readerRow, int columnIndex)
   {
      Cell cell = readerRow.getCell(columnIndex);
      if(cell == null)
      {
         return (null);
      }

      String dataFormatString = cell.getDataFormatString();
      switch(cell.getType())
      {
         case NUMBER ->
         {
            /////////////////////////////////////////////////////////////////////////////////////
            // dates, date-times, integers, and decimals are all identified as type = "number" //
            // so go through this process to try to identify what user means it as             //
            /////////////////////////////////////////////////////////////////////////////////////
            if(isDateTimeFormat(dataFormatString))
            {
               ////////////////////////////////////////////////////////////////////////////////////////
               // first - if it has a date-time looking format string, then treat it as a date-time. //
               ////////////////////////////////////////////////////////////////////////////////////////
               return (cell.asDate());
            }
            else if(isDateFormat(dataFormatString))
            {
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////
               // second, if it has a date looking format string (which is a sub-set of date-time), then treat as date. //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////
               return (cell.asDate().toLocalDate());
            }
            else
            {
               ////////////////////////////////////////////////////////////////////////////////////////
               // now assume it's a number - but in case this optional is empty (why?) return a null //
               ////////////////////////////////////////////////////////////////////////////////////////
               Optional<BigDecimal> bigDecimal = readerRow.getCellAsNumber(columnIndex);
               if(bigDecimal.isEmpty())
               {
                  return (null);
               }

               try
               {
                  ////////////////////////////////////////////////////////////
                  // now if the bigDecimal is an exact integer, return that //
                  ////////////////////////////////////////////////////////////
                  Integer i = bigDecimal.get().intValueExact();
                  return (i);
               }
               catch(ArithmeticException e)
               {
                  /////////////////////////////////
                  // else, end up with a decimal //
                  /////////////////////////////////
                  return (bigDecimal.get());
               }
            }
         }
         case STRING ->
         {
            return cell.asString();
         }
         case BOOLEAN ->
         {
            return cell.asBoolean();
         }
         case FORMULA ->
         {
            return (ValueUtils.getValueAsString(cell.getRawValue()));
         }
         case EMPTY, ERROR ->
         {
            LOG.debug("Empty or Error cell", logPair("type", cell.getType()), logPair("rawValue", () -> cell.getRawValue()));
            return (null);
         }
         default ->
         {
            return (null);
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static boolean isDateTimeFormat(String dataFormatString)
   {
      if(dataFormatString == null)
      {
         return (false);
      }

      if(hasDay(dataFormatString) && hasHour(dataFormatString))
      {
         return (true);
      }

      return false;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static boolean hasHour(String dataFormatString)
   {
      return dataFormatString.contains("h") || dataFormatString.contains("H");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static boolean hasDay(String dataFormatString)
   {
      return DAY_PATTERN.matcher(dataFormatString).matches();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static boolean isDateFormat(String dataFormatString)
   {
      if(dataFormatString == null)
      {
         return (false);
      }

      if(hasDay(dataFormatString))
      {
         return (true);
      }

      return false;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void close() throws Exception
   {
      if(workbook != null)
      {
         workbook.close();
      }

      if(rows != null)
      {
         rows.close();
      }
   }

}
