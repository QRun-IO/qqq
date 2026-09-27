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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.util.List;
import java.util.Locale;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** String parsing accepts known formats and rejects invalid names.
 *******************************************************************************/
class ReportFormatTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFileFormatsAreCaseInsensitive() throws Exception
   {
      for(ReportFormat format : List.of(ReportFormat.XLSX, ReportFormat.CSV, ReportFormat.TSV, ReportFormat.JSON))
      {
         assertEquals(format, ReportFormat.fromString(format.name().toLowerCase(Locale.ROOT)));
         assertEquals(format, ReportFormat.fromString(format.name()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRejectsMissingAndUnknownFormats()
   {
      for(String value : new String[] {null, "", " ", "docx"})
      {
         assertThrows(QUserFacingException.class, () -> ReportFormat.fromString(value));
      }
   }
}
