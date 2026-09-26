/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 ** Middleware input for widget rendering.
 *******************************************************************************/
public class WidgetInput extends AbstractMiddlewareInput
{
   private String              widgetName;
   private Map<String, String> queryParams = new LinkedHashMap<>();



   /*******************************************************************************
    ** Getter for widgetName
    *******************************************************************************/
   public String getWidgetName()
   {
      return (this.widgetName);
   }



   /*******************************************************************************
    ** Setter for widgetName
    *******************************************************************************/
   public void setWidgetName(String widgetName)
   {
      this.widgetName = widgetName;
   }



   /*******************************************************************************
    ** Fluent setter for widgetName
    *******************************************************************************/
   public WidgetInput withWidgetName(String widgetName)
   {
      this.widgetName = widgetName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryParams
    *******************************************************************************/
   public Map<String, String> getQueryParams()
   {
      return (this.queryParams);
   }



   /*******************************************************************************
    ** Setter for queryParams
    *******************************************************************************/
   public void setQueryParams(Map<String, String> queryParams)
   {
      this.queryParams = queryParams;
   }



   /*******************************************************************************
    ** Fluent setter for queryParams
    *******************************************************************************/
   public WidgetInput withQueryParams(Map<String, String> queryParams)
   {
      this.queryParams = queryParams;
      return (this);
   }

}
