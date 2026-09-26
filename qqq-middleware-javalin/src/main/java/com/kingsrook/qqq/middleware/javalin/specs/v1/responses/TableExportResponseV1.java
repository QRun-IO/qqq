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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.io.InputStream;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableExportOutputInterface;


/*******************************************************************************
 ** Response wrapper for the table export endpoint. Carries the piped input
 ** stream and metadata needed to produce the binary HTTP response.
 *******************************************************************************/
public class TableExportResponseV1 implements TableExportOutputInterface
{
   private ReportFormat reportFormat;
   private String       filename;
   private InputStream  inputStream;



   /*******************************************************************************
    ** Setter for reportFormat
    *******************************************************************************/
   @Override
   public void setReportFormat(ReportFormat format)
   {
      this.reportFormat = format;
   }



   /*******************************************************************************
    ** Setter for filename
    *******************************************************************************/
   @Override
   public void setFilename(String filename)
   {
      this.filename = filename;
   }



   /*******************************************************************************
    ** Setter for inputStream
    *******************************************************************************/
   @Override
   public void setInputStream(InputStream inputStream)
   {
      this.inputStream = inputStream;
   }



   /*******************************************************************************
    ** Getter for reportFormat
    *******************************************************************************/
   public ReportFormat getReportFormat()
   {
      return (this.reportFormat);
   }



   /*******************************************************************************
    ** Getter for filename
    *******************************************************************************/
   public String getFilename()
   {
      return (this.filename);
   }



   /*******************************************************************************
    ** Getter for inputStream
    *******************************************************************************/
   public InputStream getInputStream()
   {
      return (this.inputStream);
   }

}
