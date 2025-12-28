/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 * Widget data for the row builder widget.  e.g., returned by a widget renderer
 * for rowBuidler type widget.
 *
 * <p>Primarily, a list of QRecords given to the constructor or {@link #withRecords(List)}.</p>
 *
 * Additional data for more complex use cases can be set in:
 * @see #withHiddenValues(Map)
 * @see #withDropdownDefaultValueList(List)
 *******************************************************************************/
public class RowBuilderData extends QWidgetData
{
   private List<QRecord> records;

   private Map<String, Serializable> hiddenValues;
   private Map<String, Serializable> defaultValuesForNewRecords;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RowBuilderData(List<QRecord> records)
   {
      this.records = records;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.ROW_BUILDER.getType();
   }



   /*******************************************************************************
    * Getter for records
    * @see #withRecords(List)
    *******************************************************************************/
   public List<QRecord> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    * Setter for records
    * @see #withRecords(List)
    *******************************************************************************/
   public void setRecords(List<QRecord> records)
   {
      this.records = records;
   }



   /*******************************************************************************
    * Fluent setter for records
    *
    * @param records
    * The records to be rendered in the widget
    * @return this
    *******************************************************************************/
   public RowBuilderData withRecords(List<QRecord> records)
   {
      this.records = records;
      return (this);
   }



   /*******************************************************************************
    * Getter for hiddenValues
    * @see #withHiddenValues(Map)
    *******************************************************************************/
   public Map<String, Serializable> getHiddenValues()
   {
      return (this.hiddenValues);
   }



   /*******************************************************************************
    * Setter for hiddenValues
    * @see #withHiddenValues(Map)
    *******************************************************************************/
   public void setHiddenValues(Map<String, Serializable> hiddenValues)
   {
      this.hiddenValues = hiddenValues;
   }



   /*******************************************************************************
    * Fluent setter for hiddenValues
    *
    * @param hiddenValues
    * map of fieldName → value, aren't for putting values into the widget, but are
    * in the overall form.  Useful, for example, for inputs to possible value source
    * filters.
    * @return this
    *******************************************************************************/
   public RowBuilderData withHiddenValues(Map<String, Serializable> hiddenValues)
   {
      this.hiddenValues = hiddenValues;
      return (this);
   }



   /*******************************************************************************
    * Getter for defaultValuesForNewRecords
    * @see #withDefaultValuesForNewRecords(Map)
    *******************************************************************************/
   public Map<String, Serializable> getDefaultValuesForNewRecords()
   {
      return (this.defaultValuesForNewRecords);
   }



   /*******************************************************************************
    * Setter for defaultValuesForNewRecords
    * @see #withDefaultValuesForNewRecords(Map)
    *******************************************************************************/
   public void setDefaultValuesForNewRecords(Map<String, Serializable> defaultValuesForNewRecords)
   {
      this.defaultValuesForNewRecords = defaultValuesForNewRecords;
   }



   /*******************************************************************************
    * Fluent setter for defaultValuesForNewRecords
    *
    * @param defaultValuesForNewRecords
    * map of fieldName → value, which will be put in all new rows created within the
    * widget.
    * @return this
    *******************************************************************************/
   public RowBuilderData withDefaultValuesForNewRecords(Map<String, Serializable> defaultValuesForNewRecords)
   {
      this.defaultValuesForNewRecords = defaultValuesForNewRecords;
      return (this);
   }

}



