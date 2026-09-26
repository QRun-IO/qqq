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


/*******************************************************************************
 ** Possible values for widget type
 *******************************************************************************/
public enum WidgetType
{
   ///////////////////////////////////
   // (generally) dashboard widgets //
   ///////////////////////////////////
   ALERT("alert"),
   BAR_CHART("barChart"),
   CHART("chart"),
   DIVIDER("divider"),
   FIELD_VALUE_LIST("fieldValueList"),
   GENERIC("generic"),
   HORIZONTAL_BAR_CHART("horizontalBarChart"),
   HTML("html"),
   LINE_CHART("lineChart"),
   SMALL_LINE_CHART("smallLineChart"),
   LOCATION("location"),
   MULTI_STATISTICS("multiStatistics"),
   MULTI_TABLE("multiTable"),
   PIE_CHART("pieChart"),
   QUICK_SIGHT_CHART("quickSightChart"),
   STATISTICS("statistics"),
   STACKED_BAR_CHART("stackedBarChart"),
   STEPPER("stepper"),
   TABLE("table"),
   USA_MAP("usaMap"),

   ///////////////////////////////
   // widget to house a process //
   ///////////////////////////////
   PROCESS("process"),

   ///////////////////////
   // container widgets //
   ///////////////////////
   PARENT_WIDGET("parentWidget"),
   COMPOSITE("composite"),

   //////////////////////////////
   // record view/edit widgets //
   //////////////////////////////
   CHILD_RECORD_LIST("childRecordList"),
   CUSTOM_COMPONENT("customComponent"),
   CRON_UI("cronUI"),
   DYNAMIC_FORM("dynamicForm"),
   DATA_BAG_VIEWER("dataBagViewer"),
   PIVOT_TABLE_SETUP("pivotTableSetup"),
   FILTER_AND_COLUMNS_SETUP("filterAndColumnsSetup"),
   ROW_BUILDER("rowBuilder"),
   SCRIPT_VIEWER("scriptViewer");


   private final String type;



   /*******************************************************************************
    **
    *******************************************************************************/
   WidgetType(String type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return type;
   }

}
