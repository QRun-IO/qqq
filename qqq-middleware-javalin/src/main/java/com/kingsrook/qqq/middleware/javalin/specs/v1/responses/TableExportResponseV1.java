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
