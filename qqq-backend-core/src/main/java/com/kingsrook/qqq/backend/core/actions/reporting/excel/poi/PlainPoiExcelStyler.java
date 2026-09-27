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


import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


/*******************************************************************************
 ** Excel styler that does nothing - just takes defaults (which are all no-op) from the interface.
 *******************************************************************************/
public class PlainPoiExcelStyler implements PoiExcelStylerInterface
{

   /*******************************************************************************
    ** ... sorry, but adding this gives us test coverage on this class, even though
    ** we're just deferring to super...
    *******************************************************************************/
   @Override
   public XSSFCellStyle createStyleForHeader(XSSFWorkbook workbook, CreationHelper createHelper)
   {
      return PoiExcelStylerInterface.super.createStyleForHeader(workbook, createHelper);
   }

}
