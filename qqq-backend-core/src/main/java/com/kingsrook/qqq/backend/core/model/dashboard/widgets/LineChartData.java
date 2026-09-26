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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.util.List;


/*******************************************************************************
 ** Model containing datastructure expected by frontend bar chart widget
 **
 *******************************************************************************/
public class LineChartData extends QWidgetData
{
   /*
      export interface DefaultLineChartData
      {
         labels: string[];  // monday, tues...
         lineLabels: string[]; // axle, cdl...
         datasets: {
            label: string; // axle, cdl...
            color?: "primary" | "secondary" | "info" | "success" | "warning" | "error" | "light" | "dark";
            data: number[];
         }[];
      };
    */

   private String  title;
   private Data    chartData;
   private boolean isYAxisCurrency = false;



   /*******************************************************************************
    **
    *******************************************************************************/
   public LineChartData()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public LineChartData(String title, List<String> labels, List<String> lineLabels, List<Data.Dataset> datasets)
   {
      setTitle(title);
      setChartData(new LineChartData.Data()
         .withLabels(labels)
         .withLineLabels(lineLabels)
         .withDatasets(datasets));
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.LINE_CHART.getType();
   }



   /*******************************************************************************
    ** Getter for title
    **
    *******************************************************************************/
   public String getTitle()
   {
      return title;
   }



   /*******************************************************************************
    ** Setter for title
    **
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title
    **
    *******************************************************************************/
   public LineChartData withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for chartData
    **
    *******************************************************************************/
   public Data getChartData()
   {
      return chartData;
   }



   /*******************************************************************************
    ** Setter for chartData
    **
    *******************************************************************************/
   public void setChartData(Data chartData)
   {
      this.chartData = chartData;
   }



   /*******************************************************************************
    ** Fluent setter for chartData
    **
    *******************************************************************************/
   public LineChartData withChartData(Data chartData)
   {
      this.chartData = chartData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isYAxisCurrency
    **
    *******************************************************************************/
   public boolean getIsYAxisCurrency()
   {
      return isYAxisCurrency;
   }



   /*******************************************************************************
    ** Setter for isYAxisCurrency
    **
    *******************************************************************************/
   public void setIsYAxisCurrency(boolean isYAxisCurrency)
   {
      this.isYAxisCurrency = isYAxisCurrency;
   }



   /*******************************************************************************
    ** Fluent setter for isYAxisCurrency
    **
    *******************************************************************************/
   public LineChartData withIsYAxisCurrency(boolean isYAxisCurrency)
   {
      this.isYAxisCurrency = isYAxisCurrency;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Data
   {
      private List<String>  labels;
      private List<String>  lineLabels;
      private List<Dataset> datasets;



      /*******************************************************************************
       ** Getter for labels
       **
       *******************************************************************************/
      public List<String> getLabels()
      {
         return labels;
      }



      /*******************************************************************************
       ** Setter for labels
       **
       *******************************************************************************/
      public void setLabels(List<String> labels)
      {
         this.labels = labels;
      }



      /*******************************************************************************
       ** Fluent setter for labels
       **
       *******************************************************************************/
      public Data withLabels(List<String> labels)
      {
         this.labels = labels;
         return (this);
      }



      /*******************************************************************************
       ** Getter for lineLabels
       **
       *******************************************************************************/
      public List<String> getLineLabels()
      {
         return lineLabels;
      }



      /*******************************************************************************
       ** Setter for lineLabels
       **
       *******************************************************************************/
      public void setLineLabels(List<String> lineLabels)
      {
         this.lineLabels = lineLabels;
      }



      /*******************************************************************************
       ** Fluent setter for lineLabels
       **
       *******************************************************************************/
      public Data withLineLabels(List<String> lineLabels)
      {
         this.lineLabels = lineLabels;
         return (this);
      }



      /*******************************************************************************
       ** Getter for datasets
       **
       *******************************************************************************/
      public List<Dataset> getDatasets()
      {
         return datasets;
      }



      /*******************************************************************************
       ** Setter for datasets
       **
       *******************************************************************************/
      public void setDatasets(List<Dataset> datasets)
      {
         this.datasets = datasets;
      }



      /*******************************************************************************
       ** Fluent setter for datasets
       **
       *******************************************************************************/
      public Data withDatasets(List<Dataset> datasets)
      {
         this.datasets = datasets;
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static class Dataset
      {
         private String       label;
         private List<Number> data;



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
         public Dataset withLabel(String label)
         {
            this.label = label;
            return (this);
         }



         /*******************************************************************************
          ** Getter for data
          **
          *******************************************************************************/
         public List<Number> getData()
         {
            return data;
         }



         /*******************************************************************************
          ** Setter for data
          **
          *******************************************************************************/
         public void setData(List<Number> data)
         {
            this.data = data;
         }



         /*******************************************************************************
          ** Fluent setter for data
          **
          *******************************************************************************/
         public Dataset withData(List<Number> data)
         {
            this.data = data;
            return (this);
         }
      }
   }
}
