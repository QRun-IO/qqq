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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordDeveloperModeOutputInterface;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils.AssociatedScriptDetails;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OutputRecord;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordDeveloperModeAssociatedScript;


/*******************************************************************************
 ** Response for the record developer-mode route.
 *******************************************************************************/
public class RecordDeveloperModeResponseV1 implements RecordDeveloperModeOutputInterface, ToSchema
{
   @OpenAPIDescription("The record, with display values (and possible values translated).")
   private OutputRecord record;

   @OpenAPIDescription("Each of the table's associated scripts, with the script the record references (if any).  Empty when the table has no associated scripts (or the instance has no script tables).")
   @OpenAPIListItems(value = RecordDeveloperModeAssociatedScript.class, useRef = true)
   private List<RecordDeveloperModeAssociatedScript> associatedScripts;



   /*******************************************************************************
    ** Setter for record
    *******************************************************************************/
   @Override
   public void setRecord(QRecord record)
   {
      this.record = record == null ? null : new OutputRecord(record, true);
   }



   /*******************************************************************************
    ** Fluent setter for record
    *******************************************************************************/
   public RecordDeveloperModeResponseV1 withRecord(QRecord record)
   {
      setRecord(record);
      return (this);
   }



   /*******************************************************************************
    ** Getter for record
    *******************************************************************************/
   public OutputRecord getRecord()
   {
      return (this.record);
   }



   /*******************************************************************************
    ** Setter for associatedScripts
    *******************************************************************************/
   @Override
   public void setAssociatedScripts(List<AssociatedScriptDetails> associatedScripts)
   {
      this.associatedScripts = associatedScripts == null ? null : associatedScripts.stream().map(RecordDeveloperModeAssociatedScript::new).toList();
   }



   /*******************************************************************************
    ** Fluent setter for associatedScripts
    *******************************************************************************/
   public RecordDeveloperModeResponseV1 withAssociatedScripts(List<AssociatedScriptDetails> associatedScripts)
   {
      setAssociatedScripts(associatedScripts);
      return (this);
   }



   /*******************************************************************************
    ** Getter for associatedScripts - an empty list is published as such.
    *******************************************************************************/
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public List<RecordDeveloperModeAssociatedScript> getAssociatedScripts()
   {
      return (this.associatedScripts);
   }

}
