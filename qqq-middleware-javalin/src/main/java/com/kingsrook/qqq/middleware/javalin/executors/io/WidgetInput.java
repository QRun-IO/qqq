/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
