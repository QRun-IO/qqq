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
 ** Model containing datastructure expected by frontend alert widget
 **
 *******************************************************************************/
public class AlertData extends QWidgetData
{
   /***************************************************************************
    **
    ***************************************************************************/
   public enum AlertType
   {
      ERROR,
      SUCCESS,
      WARNING
   }



   private String       html;
   private AlertType    alertType;
   private Boolean      hideWidget = false;
   private List<String> bulletList;



   /*******************************************************************************
    **
    *******************************************************************************/
   public AlertData()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public AlertData(AlertType alertType, String html)
   {
      setHtml(html);
      setAlertType(alertType);
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public String getType()
   {
      return WidgetType.ALERT.getType();
   }



   /*******************************************************************************
    ** Getter for html
    **
    *******************************************************************************/
   public String getHtml()
   {
      return html;
   }



   /*******************************************************************************
    ** Setter for html
    **
    *******************************************************************************/
   public void setHtml(String html)
   {
      this.html = html;
   }



   /*******************************************************************************
    ** Fluent setter for html
    **
    *******************************************************************************/
   public AlertData withHtml(String html)
   {
      this.html = html;
      return (this);
   }



   /*******************************************************************************
    ** Getter for alertType
    *******************************************************************************/
   public AlertType getAlertType()
   {
      return (this.alertType);
   }



   /*******************************************************************************
    ** Setter for alertType
    *******************************************************************************/
   public void setAlertType(AlertType alertType)
   {
      this.alertType = alertType;
   }



   /*******************************************************************************
    ** Fluent setter for alertType
    *******************************************************************************/
   public AlertData withAlertType(AlertType alertType)
   {
      this.alertType = alertType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hideWidget
    *******************************************************************************/
   public boolean getHideWidget()
   {
      return (this.hideWidget);
   }



   /*******************************************************************************
    ** Setter for hideWidget
    *******************************************************************************/
   public void setHideWidget(boolean hideWidget)
   {
      this.hideWidget = hideWidget;
   }



   /*******************************************************************************
    ** Fluent setter for hideWidget
    *******************************************************************************/
   public AlertData withHideWidget(boolean hideWidget)
   {
      this.hideWidget = hideWidget;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bulletList
    *******************************************************************************/
   public List<String> getBulletList()
   {
      return (this.bulletList);
   }



   /*******************************************************************************
    ** Setter for bulletList
    *******************************************************************************/
   public void setBulletList(List<String> bulletList)
   {
      this.bulletList = bulletList;
   }



   /*******************************************************************************
    ** Fluent setter for bulletList
    *******************************************************************************/
   public AlertData withBulletList(List<String> bulletList)
   {
      this.bulletList = bulletList;
      return (this);
   }

}
