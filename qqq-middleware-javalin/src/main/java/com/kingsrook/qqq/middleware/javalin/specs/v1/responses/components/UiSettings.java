/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapValueType;


/*******************************************************************************
 ** Client-neutral settings copied only from the already published allow-list.
 *******************************************************************************/
public class UiSettings
{
   @OpenAPIExclude
   private final MaterialDashboardInstanceSettings settings;
   @OpenAPIExclude
   private final Map<String, Object> theme;



   /*******************************************************************************
    ** Client-neutral representation for non-web clients.
    *******************************************************************************/
   public UiSettings(MaterialDashboardInstanceSettings settings, Map<String, Object> theme)
   {
      this.settings = settings;
      this.theme = theme;
   }



   /*******************************************************************************
    ** Client-neutral representation for non-web clients.
    *******************************************************************************/
   @OpenAPIDescription("Visible processes offered on every query and record-view screen.")
   @OpenAPIListItems(String.class)
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public List<String> getProcessNamesToAddToAllQueryAndViewScreens()
   {
      return (settings == null ? null : settings.getProcessNamesToAddToAllQueryAndViewScreens());
   }



   /*******************************************************************************
    ** Client-neutral representation for non-web clients.
    *******************************************************************************/
   @OpenAPIDescription("Placement of record-view actions.")
   public String getRecordViewActionsPlacement()
   {
      return (settings == null ? null : settings.getRecordViewActionsPlacement());
   }



   /*******************************************************************************
    ** Client-neutral representation for non-web clients.
    *******************************************************************************/
   @OpenAPIDescription("Weekday query criteria configuration.")
   public MaterialDashboardWeekdayCriteriaSettings getWeekdayCriteriaSettings()
   {
      return (settings == null ? null : settings.getWeekdayCriteriaSettings());
   }



   /*******************************************************************************
    ** Client-neutral representation for non-web clients.
    *******************************************************************************/
   @OpenAPIDescription("Configured colors and font families; no web layout or CSS settings.")
   @OpenAPIMapValueType(String.class)
   public Map<String, String> getTheme()
   {
      if(theme == null)
      {
         return (null);
      }
      Map<String, String> result = new LinkedHashMap<>();
      theme.forEach((key, value) ->
      {
         if((key.endsWith("Color") || key.endsWith("FontFamily") || key.equals("fontFamily") || key.equals("textPrimary") || key.equals("textSecondary")) && value instanceof String text)
         {
            result.put(key, text);
         }
      });
      return (result);
   }
}
