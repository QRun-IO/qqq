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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** Input for an Export action
 *******************************************************************************/
public class ExportInput extends AbstractTableActionInput
{
   private QQueryFilter queryFilter;
   private Integer      limit;
   private List<String> fieldNames;

   private ReportDestination reportDestination;

   private String       titleRow;
   private boolean      includeHeaderRow = true;



   /*******************************************************************************
    **
    *******************************************************************************/
   public ExportInput()
   {
   }



   /*******************************************************************************
    ** Getter for queryFilter
    **
    *******************************************************************************/
   public QQueryFilter getQueryFilter()
   {
      return queryFilter;
   }



   /*******************************************************************************
    ** Setter for queryFilter
    **
    *******************************************************************************/
   public void setQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
   }



   /*******************************************************************************
    ** Getter for limit
    **
    *******************************************************************************/
   public Integer getLimit()
   {
      return limit;
   }



   /*******************************************************************************
    ** Setter for limit
    **
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Getter for fieldNames
    **
    *******************************************************************************/
   public List<String> getFieldNames()
   {
      return fieldNames;
   }



   /*******************************************************************************
    ** Setter for fieldNames
    **
    *******************************************************************************/
   public void setFieldNames(List<String> fieldNames)
   {
      this.fieldNames = fieldNames;
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   public String getTitleRow()
   {
      return titleRow;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setTitleRow(String titleRow)
   {
      this.titleRow = titleRow;
   }



   /*******************************************************************************
    ** Getter for includeHeaderRow
    **
    *******************************************************************************/
   public boolean getIncludeHeaderRow()
   {
      return includeHeaderRow;
   }



   /*******************************************************************************
    ** Setter for includeHeaderRow
    **
    *******************************************************************************/
   public void setIncludeHeaderRow(boolean includeHeaderRow)
   {
      this.includeHeaderRow = includeHeaderRow;
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
   public ExportInput withReportDestination(ReportDestination reportDestination)
   {
      this.reportDestination = reportDestination;
      return (this);
   }


}
