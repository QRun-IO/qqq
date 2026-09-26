/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIHasAdditionalProperties;


/***************************************************************************
 ** The weekday criteria settings from an instance's `materialDashboard`
 ** supplemental meta-data:  whether query screens offer "day is any of" and
 ** "day is none of" on date and date-time fields, and the arguments to send
 ** with the WeekdayOfDateTime field function (e.g., a time zone).
 ***************************************************************************/
public class MaterialDashboardWeekdayCriteriaSettings implements ToSchema
{
   @OpenAPIDescription("Whether query screens offer the weekday criteria (day is any of, day is none of) on date and date-time fields.")
   private Boolean enabled;

   @OpenAPIDescription("Arguments for the WeekdayOfDateTime field function that a frontend sends with weekday criteria on date-time fields (for example timeZoneId or useSessionZoneId).")
   @OpenAPIHasAdditionalProperties()
   private Map<String, Serializable> dateTimeFieldFunctionArguments;



   /*******************************************************************************
    ** Getter for enabled
    *******************************************************************************/
   public Boolean getEnabled()
   {
      return (this.enabled);
   }



   /*******************************************************************************
    ** Setter for enabled
    *******************************************************************************/
   public void setEnabled(Boolean enabled)
   {
      this.enabled = enabled;
   }



   /*******************************************************************************
    ** Fluent setter for enabled
    *******************************************************************************/
   public MaterialDashboardWeekdayCriteriaSettings withEnabled(Boolean enabled)
   {
      this.enabled = enabled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dateTimeFieldFunctionArguments
    *******************************************************************************/
   public Map<String, Serializable> getDateTimeFieldFunctionArguments()
   {
      return (this.dateTimeFieldFunctionArguments);
   }



   /*******************************************************************************
    ** Setter for dateTimeFieldFunctionArguments
    *******************************************************************************/
   public void setDateTimeFieldFunctionArguments(Map<String, Serializable> dateTimeFieldFunctionArguments)
   {
      this.dateTimeFieldFunctionArguments = dateTimeFieldFunctionArguments;
   }



   /*******************************************************************************
    ** Fluent setter for dateTimeFieldFunctionArguments
    *******************************************************************************/
   public MaterialDashboardWeekdayCriteriaSettings withDateTimeFieldFunctionArguments(Map<String, Serializable> dateTimeFieldFunctionArguments)
   {
      this.dateTimeFieldFunctionArguments = dateTimeFieldFunctionArguments;
      return (this);
   }

}
