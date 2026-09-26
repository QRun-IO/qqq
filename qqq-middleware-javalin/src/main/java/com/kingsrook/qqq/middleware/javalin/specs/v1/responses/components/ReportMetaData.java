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
