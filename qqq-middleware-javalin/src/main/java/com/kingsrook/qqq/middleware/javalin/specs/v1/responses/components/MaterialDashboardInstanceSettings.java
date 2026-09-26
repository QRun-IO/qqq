/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/***************************************************************************
 ** The settings a frontend may read from an instance's `materialDashboard`
 ** supplemental meta-data.  Only the settings listed here are published -
 ** never the supplemental meta-data object itself.
 ***************************************************************************/
public class MaterialDashboardInstanceSettings implements ToSchema
{
   @OpenAPIDescription("Names of processes to offer on the query and record-view screens of every table, in their configured order.  Only processes that the user may see (i.e., that are in the meta-data's `processes` map) are listed, so the list may be empty.")
   @OpenAPIListItems(value = String.class)
   private List<String> processNamesToAddToAllQueryAndViewScreens;

   @OpenAPIDescription("Weekday criteria settings for the query screens (day is any of, day is none of).  Omitted when the material dashboard meta-data does not define them.")
   private MaterialDashboardWeekdayCriteriaSettings weekdayCriteriaSettings;



   /*******************************************************************************
    ** Getter for processNamesToAddToAllQueryAndViewScreens - an empty list is
    ** published as such (not omitted), so a client can tell "none visible" from
    ** "not configured".
    *******************************************************************************/
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public List<String> getProcessNamesToAddToAllQueryAndViewScreens()
   {
      return (this.processNamesToAddToAllQueryAndViewScreens);
   }



   /*******************************************************************************
    ** Setter for processNamesToAddToAllQueryAndViewScreens
    *******************************************************************************/
   public void setProcessNamesToAddToAllQueryAndViewScreens(List<String> processNamesToAddToAllQueryAndViewScreens)
   {
      this.processNamesToAddToAllQueryAndViewScreens = processNamesToAddToAllQueryAndViewScreens;
   }



   /*******************************************************************************
    ** Fluent setter for processNamesToAddToAllQueryAndViewScreens
    *******************************************************************************/
   public MaterialDashboardInstanceSettings withProcessNamesToAddToAllQueryAndViewScreens(List<String> processNamesToAddToAllQueryAndViewScreens)
   {
      this.processNamesToAddToAllQueryAndViewScreens = processNamesToAddToAllQueryAndViewScreens;
      return (this);
   }



   /*******************************************************************************
    ** Getter for weekdayCriteriaSettings
    *******************************************************************************/
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public MaterialDashboardWeekdayCriteriaSettings getWeekdayCriteriaSettings()
   {
      return (this.weekdayCriteriaSettings);
   }



   /*******************************************************************************
    ** Setter for weekdayCriteriaSettings
    *******************************************************************************/
   public void setWeekdayCriteriaSettings(MaterialDashboardWeekdayCriteriaSettings weekdayCriteriaSettings)
   {
      this.weekdayCriteriaSettings = weekdayCriteriaSettings;
   }



   /*******************************************************************************
    ** Fluent setter for weekdayCriteriaSettings
    *******************************************************************************/
   public MaterialDashboardInstanceSettings withWeekdayCriteriaSettings(MaterialDashboardWeekdayCriteriaSettings weekdayCriteriaSettings)
   {
      this.weekdayCriteriaSettings = weekdayCriteriaSettings;
      return (this);
   }

}
