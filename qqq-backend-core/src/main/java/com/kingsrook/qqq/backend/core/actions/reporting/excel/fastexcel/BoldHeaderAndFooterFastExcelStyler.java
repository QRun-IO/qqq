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


import org.dhatim.fastexcel.BorderSide;
import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.StyleSetter;


/*******************************************************************************
 ** Version of excel styler that does bold headers and footers, with basic borders.
 *******************************************************************************/
public class BoldHeaderAndFooterFastExcelStyler implements FastExcelStylerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void styleTitleRow(StyleSetter titleRowStyle)
   {
      titleRowStyle
         .bold()
         .fontSize(14)
         .horizontalAlignment("center");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void styleHeaderRow(StyleSetter headerRowStyle)
   {
      headerRowStyle
         .bold()
         .borderStyle(BorderSide.BOTTOM, BorderStyle.THIN);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void styleTotalsRow(StyleSetter totalsRowStyle)
   {
      totalsRowStyle
         .bold()
         .borderStyle(BorderSide.TOP, BorderStyle.THIN)
         .borderStyle(BorderSide.BOTTOM, BorderStyle.DOUBLE);
   }
}
