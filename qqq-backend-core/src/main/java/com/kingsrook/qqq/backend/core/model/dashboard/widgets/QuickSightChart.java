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
 ** Model containing datastructure expected by frontend AWS quick sight widget
 ** TODO: this might just be an IFrameChart widget in the future
 **
 *******************************************************************************/
public class QuickSightChart extends QWidgetData
{
   private String label;
   private String name;
   private String url;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QuickSightChart(String name, String label, String url)
   {
      this.url = url;
      this.name = name;
      this.label = label;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.QUICK_SIGHT_CHART.getType();
   }



   /*******************************************************************************
    ** Getter for url
    **
    *******************************************************************************/
   public String getUrl()
   {
      return url;
   }



   /*******************************************************************************
    ** Setter for url
    **
    *******************************************************************************/
   public void setUrl(String url)
   {
      this.url = url;
   }



   /*******************************************************************************
    ** Fluent setter for url
    **
    *******************************************************************************/
   public QuickSightChart withUrl(String url)
   {
      this.url = url;
      return (this);
   }



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
   public QuickSightChart withName(String name)
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
   public QuickSightChart withLabel(String label)
   {
      this.label = label;
      return (this);
   }

}
