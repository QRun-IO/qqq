/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** Reads bulk-load rows from JSON Lines (.jsonl): one {"rowNo": n, "values":
 ** [...]} per line. Durable jobs split an uploaded file into chunk objects in
 ** this form, so each chunk keeps the ORIGINAL file row numbers (row errors and
 ** "retry failed rows" refer to the rows the user sees in the file).
 *******************************************************************************/
public class JsonlFileToRows extends AbstractIteratorBasedFileToRows<String> implements FileToRowsInterface
{
   private BufferedReader reader;
   private int            lastRowNo = 0;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void init(InputStream inputStream) throws QException
   {
      reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
      Iterator<String> lines = reader.lines().filter(line -> !line.isBlank()).iterator();
      setIterator(lines);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BulkLoadFileRow makeRow(String line)
   {
      JSONObject     object = new JSONObject(line);
      JSONArray      array  = object.getJSONArray("values");
      Serializable[] values = new Serializable[array.length()];
      for(int i = 0; i < array.length(); i++)
      {
         values[i] = array.isNull(i) ? null : String.valueOf(array.get(i));
      }
      lastRowNo = object.optInt("rowNo", lastRowNo + 1);
      return (new BulkLoadFileRow(values, lastRowNo));
   }



   /*******************************************************************************
    ** The original row number of the last row read.
    *******************************************************************************/
   @Override
   public int getRowNo()
   {
      return (lastRowNo);
   }



   /*******************************************************************************
    ** Write one row as a JSON line.
    *******************************************************************************/
   public static void writeRow(OutputStream outputStream, BulkLoadFileRow row) throws IOException
   {
      JSONArray values = new JSONArray();
      for(int i = 0; i < row.size(); i++)
      {
         Serializable value = row.getValueElseNull(i);
         values.put(value == null ? JSONObject.NULL : String.valueOf(value));
      }
      JSONObject object = new JSONObject();
      object.put("rowNo", row.getRowNo());
      object.put("values", values);
      outputStream.write(object.toString().getBytes(StandardCharsets.UTF_8));
      outputStream.write('\n');
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void close() throws Exception
   {
      if(reader != null)
      {
         reader.close();
      }
   }

}
