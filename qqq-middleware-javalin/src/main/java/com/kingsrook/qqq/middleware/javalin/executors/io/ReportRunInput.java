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
