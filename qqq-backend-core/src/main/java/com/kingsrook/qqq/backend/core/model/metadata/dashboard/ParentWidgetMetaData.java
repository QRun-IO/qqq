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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard;


import java.util.List;


/*******************************************************************************
 ** Specific meta data for frontend parent widget
 **
 *******************************************************************************/
public class ParentWidgetMetaData extends QWidgetMetaData
{
   private String       title;
   private List<String> childWidgetNameList;
   private List<String> childProcessNameList;

   private LayoutType layoutType = LayoutType.GRID;



   /***************************************************************************
    **
    ***************************************************************************/
   public enum LayoutType
   {
      GRID,
      TABS
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
   public ParentWidgetMetaData withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for childWidgetNameList
    **
    *******************************************************************************/
   public List<String> getChildWidgetNameList()
   {
      return childWidgetNameList;
   }



   /*******************************************************************************
    ** Setter for childWidgetNameList
    **
    *******************************************************************************/
   public void setChildWidgetNameList(List<String> childWidgetNameList)
   {
      this.childWidgetNameList = childWidgetNameList;
   }



   /*******************************************************************************
    ** Fluent setter for childWidgetNameList
    **
    *******************************************************************************/
   public ParentWidgetMetaData withChildWidgetNameList(List<String> childWidgetNameList)
   {
      this.childWidgetNameList = childWidgetNameList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for childProcessNameList
    **
    *******************************************************************************/
   public List<String> getChildProcessNameList()
   {
      return childProcessNameList;
   }



   /*******************************************************************************
    ** Setter for childProcessNameList
    **
    *******************************************************************************/
   public void setChildProcessNameList(List<String> childProcessNameList)
   {
      this.childProcessNameList = childProcessNameList;
   }



   /*******************************************************************************
    ** Fluent setter for childProcessNameList
    **
    *******************************************************************************/
   public ParentWidgetMetaData withChildProcessNameList(List<String> childProcessNameList)
   {
      this.childProcessNameList = childProcessNameList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for layoutType
    *******************************************************************************/
   public LayoutType getLayoutType()
   {
      return (this.layoutType);
   }



   /*******************************************************************************
    ** Setter for layoutType
    *******************************************************************************/
   public void setLayoutType(LayoutType layoutType)
   {
      this.layoutType = layoutType;
   }



   /*******************************************************************************
    ** Fluent setter for layoutType
    *******************************************************************************/
   public ParentWidgetMetaData withLayoutType(LayoutType layoutType)
   {
      this.layoutType = layoutType;
      return (this);
   }


}
