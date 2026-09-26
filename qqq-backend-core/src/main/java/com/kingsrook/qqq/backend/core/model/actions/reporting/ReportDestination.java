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

package com.kingsrook.qqq.backend.core.model.actions.reporting;


import java.io.OutputStream;


/*******************************************************************************
 ** Member of report & export Inputs, that wraps details about the destination of
 ** where & how the report (or export) is being written.
 *******************************************************************************/
public class ReportDestination
{
   private String       filename;
   private ReportFormat reportFormat;
   private OutputStream reportOutputStream;



   /*******************************************************************************
    ** Getter for filename
    *******************************************************************************/
   public String getFilename()
   {
      return (this.filename);
   }



   /*******************************************************************************
    ** Setter for filename
    *******************************************************************************/
   public void setFilename(String filename)
   {
      this.filename = filename;
   }



   /*******************************************************************************
    ** Fluent setter for filename
    *******************************************************************************/
   public ReportDestination withFilename(String filename)
   {
      this.filename = filename;
      return (this);
   }



   /*******************************************************************************
    ** Getter for reportFormat
    *******************************************************************************/
   public ReportFormat getReportFormat()
   {
      return (this.reportFormat);
   }



   /*******************************************************************************
    ** Setter for reportFormat
    *******************************************************************************/
   public void setReportFormat(ReportFormat reportFormat)
   {
      this.reportFormat = reportFormat;
   }



   /*******************************************************************************
    ** Fluent setter for reportFormat
    *******************************************************************************/
   public ReportDestination withReportFormat(ReportFormat reportFormat)
   {
      this.reportFormat = reportFormat;
      return (this);
   }



   /*******************************************************************************
    ** Getter for reportOutputStream
    *******************************************************************************/
   public OutputStream getReportOutputStream()
   {
      return (this.reportOutputStream);
   }



   /*******************************************************************************
    ** Setter for reportOutputStream
    *******************************************************************************/
   public void setReportOutputStream(OutputStream reportOutputStream)
   {
      this.reportOutputStream = reportOutputStream;
   }



   /*******************************************************************************
    ** Fluent setter for reportOutputStream
    *******************************************************************************/
   public ReportDestination withReportOutputStream(OutputStream reportOutputStream)
   {
      this.reportOutputStream = reportOutputStream;
      return (this);
   }

}
