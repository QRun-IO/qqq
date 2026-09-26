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

package com.kingsrook.qqq.backend.core.model.metadata.reporting;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 **
 *******************************************************************************/
public class QReportView implements Cloneable
{
   private String       name;
   private String       label;
   private String       dataSourceName;
   private String       varianceDataSourceName;
   private ReportType   type;
   private String       titleFormat;
   private List<String> titleFields;
   private List<String> summaryFields;

   private boolean includeHeaderRow        = true;
   private boolean includeTotalRow         = false;
   private boolean includeSummarySubTotals = false;

   private List<QReportField>   columns;
   private List<QFilterOrderBy> orderByFields;

   private QCodeReference recordTransformStep;
   private QCodeReference viewCustomizer;

   private String               pivotTableSourceViewName;
   private PivotTableDefinition pivotTableDefinition;

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // Note:  This class is Cloneable - think about if new fields added here need deep-copied in the clone method! //
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public QReportView withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    **
    *******************************************************************************/
   public QReportView withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dataSourceName
    **
    *******************************************************************************/
   public String getDataSourceName()
   {
      return dataSourceName;
   }



   /*******************************************************************************
    ** Setter for dataSourceName
    **
    *******************************************************************************/
   public void setDataSourceName(String dataSourceName)
   {
      this.dataSourceName = dataSourceName;
   }



   /*******************************************************************************
    ** Fluent setter for dataSourceName
    **
    *******************************************************************************/
   public QReportView withDataSourceName(String dataSourceName)
   {
      this.dataSourceName = dataSourceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for varianceDataSourceName
    **
    *******************************************************************************/
   public String getVarianceDataSourceName()
   {
      return varianceDataSourceName;
   }



   /*******************************************************************************
    ** Setter for varianceDataSourceName
    **
    *******************************************************************************/
   public void setVarianceDataSourceName(String varianceDataSourceName)
   {
      this.varianceDataSourceName = varianceDataSourceName;
   }



   /*******************************************************************************
    ** Fluent setter for varianceDataSourceName
    **
    *******************************************************************************/
   public QReportView withVarianceDataSourceName(String varianceDataSourceName)
   {
      this.varianceDataSourceName = varianceDataSourceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public ReportType getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(ReportType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public QReportView withType(ReportType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for titleFormat
    **
    *******************************************************************************/
   public String getTitleFormat()
   {
      return titleFormat;
   }



   /*******************************************************************************
    ** Setter for titleFormat
    **
    *******************************************************************************/
   public void setTitleFormat(String titleFormat)
   {
      this.titleFormat = titleFormat;
   }



   /*******************************************************************************
    ** Fluent setter for titleFormat
    **
    *******************************************************************************/
   public QReportView withTitleFormat(String titleFormat)
   {
      this.titleFormat = titleFormat;
      return (this);
   }



   /*******************************************************************************
    ** Getter for titleFields
    **
    *******************************************************************************/
   public List<String> getTitleFields()
   {
      return titleFields;
   }



   /*******************************************************************************
    ** Setter for titleFields
    **
    *******************************************************************************/
   public void setTitleFields(List<String> titleFields)
   {
      this.titleFields = titleFields;
   }



   /*******************************************************************************
    ** Fluent setter for titleFields
    **
    *******************************************************************************/
   public QReportView withTitleFields(List<String> titleFields)
   {
      this.titleFields = titleFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summaryFields
    **
    *******************************************************************************/
   public List<String> getSummaryFields()
   {
      return summaryFields;
   }



   /*******************************************************************************
    ** Setter for summaryFields
    **
    *******************************************************************************/
   public void setSummaryFields(List<String> summaryFields)
   {
      this.summaryFields = summaryFields;
   }



   /*******************************************************************************
    ** Fluent setter for summaryFields
    **
    *******************************************************************************/
   public QReportView withSummaryFields(List<String> summaryFields)
   {
      this.summaryFields = summaryFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for headerRow
    **
    *******************************************************************************/
   public boolean getIncludeHeaderRow()
   {
      return includeHeaderRow;
   }



   /*******************************************************************************
    ** Setter for headerRow
    **
    *******************************************************************************/
   public void setIncludeHeaderRow(boolean includeHeaderRow)
   {
      this.includeHeaderRow = includeHeaderRow;
   }



   /*******************************************************************************
    ** Fluent setter for headerRow
    **
    *******************************************************************************/
   public QReportView withIncludeHeaderRow(boolean headerRow)
   {
      this.includeHeaderRow = headerRow;
      return (this);
   }



   /*******************************************************************************
    ** Getter for totalRow
    **
    *******************************************************************************/
   public boolean getIncludeTotalRow()
   {
      return includeTotalRow;
   }



   /*******************************************************************************
    ** Setter for totalRow
    **
    *******************************************************************************/
   public void setIncludeTotalRow(boolean includeTotalRow)
   {
      this.includeTotalRow = includeTotalRow;
   }



   /*******************************************************************************
    ** Fluent setter for totalRow
    **
    *******************************************************************************/
   public QReportView withIncludeTotalRow(boolean totalRow)
   {
      this.includeTotalRow = totalRow;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summarySubTotals
    **
    *******************************************************************************/
   public boolean getIncludeSummarySubTotals()
   {
      return includeSummarySubTotals;
   }



   /*******************************************************************************
    ** Setter for summarySubTotals
    **
    *******************************************************************************/
   public void setIncludeSummarySubTotals(boolean includeSummarySubTotals)
   {
      this.includeSummarySubTotals = includeSummarySubTotals;
   }



   /*******************************************************************************
    ** Fluent setter for summarySubTotals
    **
    *******************************************************************************/
   public QReportView withIncludeSummarySubTotals(boolean summarySubTotals)
   {
      this.includeSummarySubTotals = summarySubTotals;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columns
    **
    *******************************************************************************/
   public List<QReportField> getColumns()
   {
      return columns;
   }



   /*******************************************************************************
    ** Setter for columns
    **
    *******************************************************************************/
   public void setColumns(List<QReportField> columns)
   {
      this.columns = columns;
   }



   /*******************************************************************************
    ** Fluent setter for columns
    **
    *******************************************************************************/
   public QReportView withColumns(List<QReportField> columns)
   {
      this.columns = columns;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single column
    **
    *******************************************************************************/
   public QReportView withColumn(QReportField column)
   {
      if(this.columns == null)
      {
         this.columns = new ArrayList<>();
      }
      this.columns.add(column);
      return (this);
   }



   /*******************************************************************************
    ** Getter for orderByFields
    **
    *******************************************************************************/
   public List<QFilterOrderBy> getOrderByFields()
   {
      return orderByFields;
   }



   /*******************************************************************************
    ** Setter for orderByFields
    **
    *******************************************************************************/
   public void setOrderByFields(List<QFilterOrderBy> orderByFields)
   {
      this.orderByFields = orderByFields;
   }



   /*******************************************************************************
    ** Fluent setter for orderByFields
    **
    *******************************************************************************/
   public QReportView withOrderByFields(List<QFilterOrderBy> orderByFields)
   {
      this.orderByFields = orderByFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordTransformerStep
    **
    *******************************************************************************/
   public QCodeReference getRecordTransformStep()
   {
      return recordTransformStep;
   }



   /*******************************************************************************
    ** Setter for recordTransformerStep
    **
    *******************************************************************************/
   public void setRecordTransformStep(QCodeReference recordTransformStep)
   {
      this.recordTransformStep = recordTransformStep;
   }



   /*******************************************************************************
    ** Fluent setter for recordTransformerStep
    **
    *******************************************************************************/
   public QReportView withRecordTransformStep(QCodeReference recordTransformerStep)
   {
      this.recordTransformStep = recordTransformerStep;
      return (this);
   }



   /*******************************************************************************
    ** Getter for viewCustomizer
    **
    *******************************************************************************/
   public QCodeReference getViewCustomizer()
   {
      return viewCustomizer;
   }



   /*******************************************************************************
    ** Setter for viewCustomizer
    **
    *******************************************************************************/
   public void setViewCustomizer(QCodeReference viewCustomizer)
   {
      this.viewCustomizer = viewCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for viewCustomizer
    **
    *******************************************************************************/
   public QReportView withViewCustomizer(QCodeReference viewCustomizer)
   {
      this.viewCustomizer = viewCustomizer;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QReportView clone()
   {
      try
      {
         QReportView clone = (QReportView) super.clone();

         /////////////////////////
         // copy any lists, etc //
         /////////////////////////
         if(titleFields != null)
         {
            clone.setTitleFields(new ArrayList<>(titleFields));
         }

         if(summaryFields != null)
         {
            clone.setSummaryFields(new ArrayList<>(summaryFields));
         }

         if(columns != null)
         {
            clone.setColumns(new ArrayList<>(columns));
         }

         if(orderByFields != null)
         {
            clone.setOrderByFields(new ArrayList<>(orderByFields));
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    ** Getter for pivotTableSourceViewName
    *******************************************************************************/
   public String getPivotTableSourceViewName()
   {
      return (this.pivotTableSourceViewName);
   }



   /*******************************************************************************
    ** Setter for pivotTableSourceViewName
    *******************************************************************************/
   public void setPivotTableSourceViewName(String pivotTableSourceViewName)
   {
      this.pivotTableSourceViewName = pivotTableSourceViewName;
   }



   /*******************************************************************************
    ** Fluent setter for pivotTableSourceViewName
    *******************************************************************************/
   public QReportView withPivotTableSourceViewName(String pivotTableSourceViewName)
   {
      this.pivotTableSourceViewName = pivotTableSourceViewName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pivotTableDefinition
    *******************************************************************************/
   public PivotTableDefinition getPivotTableDefinition()
   {
      return (this.pivotTableDefinition);
   }



   /*******************************************************************************
    ** Setter for pivotTableDefinition
    *******************************************************************************/
   public void setPivotTableDefinition(PivotTableDefinition pivotTableDefinition)
   {
      this.pivotTableDefinition = pivotTableDefinition;
   }



   /*******************************************************************************
    ** Fluent setter for pivotTableDefinition
    *******************************************************************************/
   public QReportView withPivotTableDefinition(PivotTableDefinition pivotTableDefinition)
   {
      this.pivotTableDefinition = pivotTableDefinition;
      return (this);
   }

}
