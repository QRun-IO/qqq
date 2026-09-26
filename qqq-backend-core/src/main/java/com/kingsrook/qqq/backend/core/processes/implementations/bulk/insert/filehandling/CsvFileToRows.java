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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class CsvFileToRows extends AbstractIteratorBasedFileToRows<CSVRecord> implements FileToRowsInterface
{
   private CSVParser csvParser;



   /***************************************************************************
    **
    ***************************************************************************/
   public static CsvFileToRows forString(String csv) throws QException
   {
      CsvFileToRows csvFileToRows = new CsvFileToRows();

      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(csv.getBytes());
      csvFileToRows.init(byteArrayInputStream);

      return (csvFileToRows);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void init(InputStream inputStream) throws QException
   {
      try
      {
         csvParser = new CSVParser(new InputStreamReader(inputStream), CSVFormat.DEFAULT
            .withIgnoreSurroundingSpaces()
         );
         setIterator(csvParser.iterator());
      }
      catch(IOException e)
      {
         throw new QException("Error opening CSV Parser", e);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BulkLoadFileRow makeRow(CSVRecord csvRecord)
   {
      Serializable[] values = new Serializable[csvRecord.size()];
      int            i      = 0;
      for(String s : csvRecord)
      {
         values[i++] = s;
      }

      return (new BulkLoadFileRow(values, getRowNo()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void close() throws Exception
   {
      if(csvParser != null)
      {
         csvParser.close();
      }
   }

}
