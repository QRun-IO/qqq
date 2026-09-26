/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendReportMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 ** A report in the instance's meta-data: what a frontend needs to list it and
 ** to run it (through its process, or the report route when it has none).
 *******************************************************************************/
public class ReportMetaData implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendReportMetaData wrapped;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ReportMetaData(QFrontendReportMetaData wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public ReportMetaData()
   {
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   @OpenAPIDescription("Unique name for this report within the QQQ Instance.")
   public String getName()
   {
      return (wrapped.getName());
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   @OpenAPIDescription("User-facing name for this report.")
   public String getLabel()
   {
      return (wrapped.getLabel());
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   @OpenAPIDescription("Process that runs this report, when it has one; otherwise it runs from GET /reports/{reportName}.")
   public String getProcessName()
   {
      return (wrapped.getProcessName());
   }



   /*******************************************************************************
    ** Getter for iconName
    *******************************************************************************/
   @OpenAPIDescription("Name of the report's icon.")
   public String getIconName()
   {
      return (wrapped.getIconName());
   }



   /*******************************************************************************
    ** Getter for hasPermission
    *******************************************************************************/
   @OpenAPIDescription("Whether the user may run the report.")
   public boolean getHasPermission()
   {
      return (wrapped.getHasPermission());
   }

}
