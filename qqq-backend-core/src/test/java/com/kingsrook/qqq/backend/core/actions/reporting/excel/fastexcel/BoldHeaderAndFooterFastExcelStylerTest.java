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

package com.kingsrook.qqq.backend.core.actions.reporting.excel.fastexcel;


import java.io.ByteArrayOutputStream;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.dhatim.fastexcel.StyleSetter;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for BoldHeaderAndFooterFastExcelStyler 
 *******************************************************************************/
class BoldHeaderAndFooterFastExcelStylerTest extends BaseTest
{

   /*******************************************************************************
    ** ... kinda just here to add test coverage to the class.  I suppose, it
    ** makes sure there's not an NPE inside that method at least...?
    *******************************************************************************/
   @Test
   void test()
   {
      Workbook    workbook    = new Workbook(new ByteArrayOutputStream(), "Test", null);
      Worksheet   worksheet   = workbook.newWorksheet("Sheet 1");
      StyleSetter headerStyle = worksheet.range(0, 0, 1, 1).style();
      new BoldHeaderAndFooterFastExcelStyler().styleHeaderRow(headerStyle);
   }

}