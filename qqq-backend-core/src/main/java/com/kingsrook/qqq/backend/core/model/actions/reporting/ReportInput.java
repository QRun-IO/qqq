/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportStreamerInterface;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;


/*******************************************************************************
 ** Input for a Report action
 *******************************************************************************/
public class ReportInput extends AbstractTableActionInput
{
   private String          reportName;
   private QReportMetaData reportMetaData;

   private Map<String, Serializable> inputValues;

   private ReportDestination reportDestination;

   private Supplier<? extends ExportStreamerInterface> overrideExportStreamerSupplier;
   private QCodeReference                              exportStyleCustomizer;



   /*******************************************************************************
    **
    *******************************************************************************/
   public ReportInput()
   {
   }



   /*******************************************************************************
    ** Getter for reportName
    **
    *******************************************************************************/
   public String getReportName()
   {
      return reportName;
   }



   /*******************************************************************************
    ** Setter for reportName
    **
    *******************************************************************************/
   public void setReportName(String reportName)
   {
      this.reportName = reportName;
   }



   /*******************************************************************************
    ** Getter for inputValues
    **
    *******************************************************************************/
   public Map<String, Serializable> getInputValues()
   {
      return inputValues;
   }



   /*******************************************************************************
    ** Setter for inputValues
    **
    *******************************************************************************/
   public void setInputValues(Map<String, Serializable> inputValues)
   {
      this.inputValues = inputValues;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addInputValue(String key, Serializable value)
   {
      if(this.inputValues == null)
      {
         this.inputValues = new HashMap<>();
      }
      this.inputValues.put(key, value);
   }



   /*******************************************************************************
    ** Getter for reportDestination
    *******************************************************************************/
   public ReportDestination getReportDestination()
   {
      return (this.reportDestination);
   }



   /*******************************************************************************
    ** Setter for reportDestination
    *******************************************************************************/
   public void setReportDestination(ReportDestination reportDestination)
   {
      this.reportDestination = reportDestination;
   }



   /*******************************************************************************
    ** Fluent setter for reportDestination
    *******************************************************************************/
   public ReportInput withReportDestination(ReportDestination reportDestination)
   {
      this.reportDestination = reportDestination;
      return (this);
   }



   /*******************************************************************************
    ** Getter for reportMetaData
    *******************************************************************************/
   public QReportMetaData getReportMetaData()
   {
      return (this.reportMetaData);
   }



   /*******************************************************************************
    ** Setter for reportMetaData
    *******************************************************************************/
   public void setReportMetaData(QReportMetaData reportMetaData)
   {
      this.reportMetaData = reportMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for reportMetaData
    *******************************************************************************/
   public ReportInput withReportMetaData(QReportMetaData reportMetaData)
   {
      this.reportMetaData = reportMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for overrideExportStreamerSupplier
    **
    *******************************************************************************/
   public Supplier<? extends ExportStreamerInterface> getOverrideExportStreamerSupplier()
   {
      return overrideExportStreamerSupplier;
   }



   /*******************************************************************************
    ** Setter for overrideExportStreamerSupplier
    **
    *******************************************************************************/
   public void setOverrideExportStreamerSupplier(Supplier<? extends ExportStreamerInterface> overrideExportStreamerSupplier)
   {
      this.overrideExportStreamerSupplier = overrideExportStreamerSupplier;
   }



   /*******************************************************************************
    ** Fluent setter for overrideExportStreamerSupplier
    **
    *******************************************************************************/
   public ReportInput withOverrideExportStreamerSupplier(Supplier<? extends ExportStreamerInterface> overrideExportStreamerSupplier)
   {
      this.overrideExportStreamerSupplier = overrideExportStreamerSupplier;
      return (this);
   }



   /*******************************************************************************
    ** Getter for exportStyleCustomizer
    *******************************************************************************/
   public QCodeReference getExportStyleCustomizer()
   {
      return (this.exportStyleCustomizer);
   }



   /*******************************************************************************
    ** Setter for exportStyleCustomizer
    *******************************************************************************/
   public void setExportStyleCustomizer(QCodeReference exportStyleCustomizer)
   {
      this.exportStyleCustomizer = exportStyleCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for exportStyleCustomizer
    *******************************************************************************/
   public ReportInput withExportStyleCustomizer(QCodeReference exportStyleCustomizer)
   {
      this.exportStyleCustomizer = exportStyleCustomizer;
      return (this);
   }

}
