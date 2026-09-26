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
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Unit test for CsvFileToRows 
 *******************************************************************************/
class CsvFileToRowsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      byte[] csvBytes = """
         one,two,three
         1,2,3,4
         """.getBytes();
      FileToRowsInterface fileToRowsInterface = FileToRowsInterface.forFile("someFile.csv", new ByteArrayInputStream(csvBytes));

      BulkLoadFileRow headerRow = fileToRowsInterface.next();
      BulkLoadFileRow bodyRow   = fileToRowsInterface.next();

      assertEquals(new BulkLoadFileRow(new String[] { "one", "two", "three" }, 1), headerRow);
      assertEquals(new BulkLoadFileRow(new String[] { "1", "2", "3", "4" }, 2), bodyRow);
      assertFalse(fileToRowsInterface.hasNext());
   }

}