/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
