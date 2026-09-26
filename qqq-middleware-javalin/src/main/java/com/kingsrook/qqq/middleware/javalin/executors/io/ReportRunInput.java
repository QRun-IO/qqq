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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.util.Map;


/*******************************************************************************
 ** Input for running a report and streaming the generated file
 ** (GET /reports/{reportName}).
 *******************************************************************************/
public class ReportRunInput extends AbstractMiddlewareInput
{
   private String              reportName;
   private String              format;
   private Map<String, String> inputValues;



   /*******************************************************************************
    ** Getter for reportName
    *******************************************************************************/
   public String getReportName()
   {
      return (this.reportName);
   }



   /*******************************************************************************
    ** Setter for reportName
    *******************************************************************************/
   public void setReportName(String reportName)
   {
      this.reportName = reportName;
   }



   /*******************************************************************************
    ** Fluent setter for reportName
    *******************************************************************************/
   public ReportRunInput withReportName(String reportName)
   {
      this.reportName = reportName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   public String getFormat()
   {
      return (this.format);
   }



   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(String format)
   {
      this.format = format;
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public ReportRunInput withFormat(String format)
   {
      this.format = format;
      return (this);
   }



   /*******************************************************************************
    ** Getter for inputValues - the raw text of the report's input fields
    *******************************************************************************/
   public Map<String, String> getInputValues()
   {
      return (this.inputValues);
   }



   /*******************************************************************************
    ** Setter for inputValues
    *******************************************************************************/
   public void setInputValues(Map<String, String> inputValues)
   {
      this.inputValues = inputValues;
   }



   /*******************************************************************************
    ** Fluent setter for inputValues
    *******************************************************************************/
   public ReportRunInput withInputValues(Map<String, String> inputValues)
   {
      this.inputValues = inputValues;
      return (this);
   }

}
