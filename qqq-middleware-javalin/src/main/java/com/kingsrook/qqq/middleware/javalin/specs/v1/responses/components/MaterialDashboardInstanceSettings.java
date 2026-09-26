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

}
