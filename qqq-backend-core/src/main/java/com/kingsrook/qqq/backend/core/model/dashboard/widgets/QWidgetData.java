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


import java.io.Serializable;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** Base class for the data returned by rendering a Widget.
 **
 *******************************************************************************/
public abstract class QWidgetData implements Serializable
{
   private String       label;
   private String       sublabel;
   private String       footerHTML;
   private List<String> dropdownNameList;
   private List<String> dropdownLabelList;
   private List<String> dropdownDefaultValueList;
   private Boolean      hasPermission;

   /////////////////////////////////////////////////////////////////////////////////////////
   // this is a list of lists, the outer list corresponds to each dropdown (parallel list //
   // with the above dropdownLabelList) - the inner list is the list of actual dropdown   //
   // options                                                                             //
   /////////////////////////////////////////////////////////////////////////////////////////
   private List<List<Map<String, String>>> dropdownDataList;
   private String                          dropdownNeedsSelectedText;

   private List<List<Serializable>> csvData;



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public abstract String getType();



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
   public QWidgetData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for footerHTML
    **
    *******************************************************************************/
   public String getFooterHTML()
   {
      return footerHTML;
   }



   /*******************************************************************************
    ** Setter for footerHTML
    **
    *******************************************************************************/
   public void setFooterHTML(String footerHTML)
   {
      this.footerHTML = footerHTML;
   }



   /*******************************************************************************
    ** Fluent setter for footerHTML
    **
    *******************************************************************************/
   public QWidgetData withFooterHTML(String footerHTML)
   {
      this.footerHTML = footerHTML;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dropdownLabelList
    **
    *******************************************************************************/
   public List<String> getDropdownLabelList()
   {
      return dropdownLabelList;
   }



   /*******************************************************************************
    ** Setter for dropdownLabelList
    **
    *******************************************************************************/
   public void setDropdownLabelList(List<String> dropdownLabelList)
   {
      this.dropdownLabelList = dropdownLabelList;
   }



   /*******************************************************************************
    ** Fluent setter for dropdownLabelList
    **
    *******************************************************************************/
   public QWidgetData withDropdownLabelList(List<String> dropdownLabelList)
   {
      this.dropdownLabelList = dropdownLabelList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dropdownNameList
    **
    *******************************************************************************/
   public List<String> getDropdownNameList()
   {
      return dropdownNameList;
   }



   /*******************************************************************************
    ** Setter for dropdownNameList
    **
    *******************************************************************************/
   public void setDropdownNameList(List<String> dropdownNameList)
   {
      this.dropdownNameList = dropdownNameList;
   }



   /*******************************************************************************
    ** Fluent setter for dropdownNameList
    **
    *******************************************************************************/
   public QWidgetData withDropdownNameList(List<String> dropdownNameList)
   {
      this.dropdownNameList = dropdownNameList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dropdownDataList
    **
    *******************************************************************************/
   public List<List<Map<String, String>>> getDropdownDataList()
   {
      return dropdownDataList;
   }



   /*******************************************************************************
    ** Setter for dropdownDataList
    **
    *******************************************************************************/
   public void setDropdownDataList(List<List<Map<String, String>>> dropdownDataList)
   {
      this.dropdownDataList = dropdownDataList;
   }



   /*******************************************************************************
    ** Fluent setter for dropdownDataList
    **
    *******************************************************************************/
   public QWidgetData withDropdownDataList(List<List<Map<String, String>>> dropdownDataList)
   {
      this.dropdownDataList = dropdownDataList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dropdownNeedsSelectedText
    **
    *******************************************************************************/
   public String getDropdownNeedsSelectedText()
   {
      return dropdownNeedsSelectedText;
   }



   /*******************************************************************************
    ** Setter for dropdownNeedsSelectedText
    **
    *******************************************************************************/
   public void setDropdownNeedsSelectedText(String dropdownNeedsSelectedText)
   {
      this.dropdownNeedsSelectedText = dropdownNeedsSelectedText;
   }



   /*******************************************************************************
    ** Fluent setter for dropdownNeedsSelectedText
    **
    *******************************************************************************/
   public QWidgetData withDropdownNeedsSelectedText(String dropdownNeedsSelectedText)
   {
      this.dropdownNeedsSelectedText = dropdownNeedsSelectedText;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hasPermission
    **
    *******************************************************************************/
   public Boolean getHasPermission()
   {
      return hasPermission;
   }



   /*******************************************************************************
    ** Setter for hasPermission
    **
    *******************************************************************************/
   public void setHasPermission(Boolean hasPermission)
   {
      this.hasPermission = hasPermission;
   }



   /*******************************************************************************
    ** Fluent setter for hasPermission
    **
    *******************************************************************************/
   public QWidgetData withHasPermission(Boolean hasPermission)
   {
      this.hasPermission = hasPermission;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dropdownDefaultValueList
    *******************************************************************************/
   public List<String> getDropdownDefaultValueList()
   {
      return (this.dropdownDefaultValueList);
   }



   /*******************************************************************************
    ** Setter for dropdownDefaultValueList
    *******************************************************************************/
   public void setDropdownDefaultValueList(List<String> dropdownDefaultValueList)
   {
      this.dropdownDefaultValueList = dropdownDefaultValueList;
   }



   /*******************************************************************************
    ** Fluent setter for dropdownDefaultValueList
    *******************************************************************************/
   public QWidgetData withDropdownDefaultValueList(List<String> dropdownDefaultValueList)
   {
      this.dropdownDefaultValueList = dropdownDefaultValueList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for csvData
    *******************************************************************************/
   public List<List<Serializable>> getCsvData()
   {
      return (this.csvData);
   }



   /*******************************************************************************
    ** Setter for csvData
    *******************************************************************************/
   public void setCsvData(List<List<Serializable>> csvData)
   {
      this.csvData = csvData;
   }



   /*******************************************************************************
    ** Fluent setter for csvData
    *******************************************************************************/
   public QWidgetData withCsvData(List<List<Serializable>> csvData)
   {
      this.csvData = csvData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sublabel
    *******************************************************************************/
   public String getSublabel()
   {
      return (this.sublabel);
   }



   /*******************************************************************************
    ** Setter for sublabel
    *******************************************************************************/
   public void setSublabel(String sublabel)
   {
      this.sublabel = sublabel;
   }



   /*******************************************************************************
    ** Fluent setter for sublabel
    *******************************************************************************/
   public QWidgetData withSublabel(String sublabel)
   {
      this.sublabel = sublabel;
      return (this);
   }

}
