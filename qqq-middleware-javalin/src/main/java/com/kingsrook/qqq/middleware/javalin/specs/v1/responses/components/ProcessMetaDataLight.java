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


import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendProcessMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapKnownEntries;


/***************************************************************************
 **
 ***************************************************************************/
public class ProcessMetaDataLight implements ToSchema
{
   @OpenAPIExclude()
   protected QFrontendProcessMetaData wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ProcessMetaDataLight(QFrontendProcessMetaData wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ProcessMetaDataLight()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Unique name for this process within the QQQ Instance")
   public String getName()
   {
      return (this.wrapped.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing name for this process")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("If this process is associated with a table, the table name is given here")
   public String getTableName()
   {
      return (this.wrapped.getTableName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Boolean indicator of whether the process should be shown to users or not")
   public Boolean getIsHidden()
   {
      return (this.wrapped.getIsHidden());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Indicator of the Step Flow used by the process.  Possible values are: LINEAR, STATE_MACHINE.")
   public String getStepFlow()
   {
      return (this.wrapped.getStepFlow());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Icon to display for the process.")
   @OpenAPIMapKnownEntries(value = Icon.class, useRef = true)
   public Icon getIcon()
   {
      return (this.wrapped.getIcon() == null ? null : new Icon(this.wrapped.getIcon()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Boolean to indicate if the user has permission for the process.")
   public Boolean getHasPermission()
   {
      return (this.wrapped.getHasPermission());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of the process's icon.")
   public String getIconName()
   {
      return (this.wrapped.getIconName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Fewest input records the process runs with (for processes on a table's records).")
   public Integer getMinInputRecords()
   {
      return (this.wrapped.getMinInputRecords());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Most input records the process runs with, when it has a maximum.")
   public Integer getMaxInputRecords()
   {
      return (this.wrapped.getMaxInputRecords());
   }

}
