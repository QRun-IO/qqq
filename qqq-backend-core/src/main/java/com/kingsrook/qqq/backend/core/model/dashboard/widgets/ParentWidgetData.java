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
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.ParentWidgetMetaData;


/*******************************************************************************
 ** Model containing datastructure expected by frontend parent widget
 **
 *******************************************************************************/
public class ParentWidgetData extends QWidgetData
{
   private List<String> childWidgetNameList;
   private ParentWidgetMetaData.LayoutType layoutType = ParentWidgetMetaData.LayoutType.GRID;

   private boolean isLabelPageTitle = false;



   /*******************************************************************************
    **
    *******************************************************************************/
   public ParentWidgetData()
   {
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.PARENT_WIDGET.getType();
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
   public ParentWidgetData withChildWidgetNameList(List<String> childWidgetNameList)
   {
      this.childWidgetNameList = childWidgetNameList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for layoutType
    *******************************************************************************/
   public ParentWidgetMetaData.LayoutType getLayoutType()
   {
      return (this.layoutType);
   }



   /*******************************************************************************
    ** Setter for layoutType
    *******************************************************************************/
   public void setLayoutType(ParentWidgetMetaData.LayoutType layoutType)
   {
      this.layoutType = layoutType;
   }



   /*******************************************************************************
    ** Fluent setter for layoutType
    *******************************************************************************/
   public ParentWidgetData withLayoutType(ParentWidgetMetaData.LayoutType layoutType)
   {
      this.layoutType = layoutType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isLabelPageTitle
    *******************************************************************************/
   public boolean getIsLabelPageTitle()
   {
      return (this.isLabelPageTitle);
   }



   /*******************************************************************************
    ** Setter for isLabelPageTitle
    *******************************************************************************/
   public void setIsLabelPageTitle(boolean isLabelPageTitle)
   {
      this.isLabelPageTitle = isLabelPageTitle;
   }



   /*******************************************************************************
    ** Fluent setter for isLabelPageTitle
    *******************************************************************************/
   public ParentWidgetData withIsLabelPageTitle(boolean isLabelPageTitle)
   {
      this.isLabelPageTitle = isLabelPageTitle;
      return (this);
   }

}
