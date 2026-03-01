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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.middleware.javalin.executors.io.WidgetOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** V1 response object for widget rendering.
 *******************************************************************************/
public class WidgetResponseV1 implements WidgetOutputInterface, ToSchema
{
   @OpenAPIDescription("Widget data object, whose shape varies by widget type.")
   private Object widgetData;



   /*******************************************************************************
    ** Setter for widgetData
    *******************************************************************************/
   @Override
   public void setWidgetData(Object widgetData)
   {
      this.widgetData = widgetData;
   }



   /*******************************************************************************
    ** Getter for widgetData
    *******************************************************************************/
   public Object getWidgetData()
   {
      return (this.widgetData);
   }



   /*******************************************************************************
    ** Fluent setter for widgetData
    *******************************************************************************/
   public WidgetResponseV1 withWidgetData(Object widgetData)
   {
      this.widgetData = widgetData;
      return (this);
   }

}
