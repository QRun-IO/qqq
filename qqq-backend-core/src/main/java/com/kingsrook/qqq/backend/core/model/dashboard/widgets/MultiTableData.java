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
public class MultiTableData extends QWidgetData
{
   List<TableData> tableDataList;



   /*******************************************************************************
    **
    *******************************************************************************/
   public MultiTableData()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public MultiTableData(List<TableData> tableDataList)
   {
      setTableDataList(tableDataList);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.MULTI_TABLE.getType();
   }



   /*******************************************************************************
    ** Getter for tableDataList
    *******************************************************************************/
   public List<TableData> getTableDataList()
   {
      return (this.tableDataList);
   }



   /*******************************************************************************
    ** Setter for tableDataList
    *******************************************************************************/
   public void setTableDataList(List<TableData> tableDataList)
   {
      this.tableDataList = tableDataList;
   }



   /*******************************************************************************
    ** Fluent setter for tableDataList
    *******************************************************************************/
   public MultiTableData withTableDataList(List<TableData> tableDataList)
   {
      this.tableDataList = tableDataList;
      return (this);
   }

}
