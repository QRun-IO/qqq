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
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils.AssociatedScriptDetails;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/***************************************************************************
 ** One of a table's associated scripts, as shown in developer mode for a
 ** record:  its definition, script type, and the script the record references
 ** (if any) with its revisions, plus the fields for testing it.
 ***************************************************************************/
public class RecordDeveloperModeAssociatedScript implements ToSchema
{
   @OpenAPIExclude()
   private AssociatedScriptDetails wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RecordDeveloperModeAssociatedScript(AssociatedScriptDetails wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RecordDeveloperModeAssociatedScript()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Definition of the associated script, from the table's meta-data.")
   public AssociatedScriptMetaData getAssociatedScript()
   {
      return (this.wrapped.getAssociatedScript() == null ? null : new AssociatedScriptMetaData(this.wrapped.getAssociatedScript()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The scriptType record of the associated script.  Omitted if there is no such record.")
   public OutputRecord getScriptType()
   {
      return (toOutputRecord(this.wrapped.getScriptType()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The script record that the record references.  Omitted if the record does not reference an existing script.")
   public OutputRecord getScript()
   {
      return (toOutputRecord(this.wrapped.getScript()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The script's revisions (scriptRevision records), newest (highest id) first.  Present (possibly empty) when script is.")
   @OpenAPIListItems(value = OutputRecord.class, useRef = true)
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public List<OutputRecord> getScriptRevisions()
   {
      List<QRecord> scriptRevisions = this.wrapped.getScriptRevisions();
      return (scriptRevisions == null ? null : scriptRevisions.stream().map(RecordDeveloperModeAssociatedScript::toOutputRecord).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Input fields for testing the script.  Present (possibly empty) when the associated script has a script tester.")
   @OpenAPIListItems(value = FieldMetaData.class, useRef = true)
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public List<FieldMetaData> getTestInputFields()
   {
      return (toFieldMetaData(this.wrapped.getTestInputFields()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Output fields from testing the script.  Present (possibly empty) when the associated script has a script tester.")
   @OpenAPIListItems(value = FieldMetaData.class, useRef = true)
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public List<FieldMetaData> getTestOutputFields()
   {
      return (toFieldMetaData(this.wrapped.getTestOutputFields()));
   }



   /***************************************************************************
    ** Records here are single records (as on a record get), so they keep their
    ** empty values.
    ***************************************************************************/
   private static OutputRecord toOutputRecord(QRecord record)
   {
      return (record == null ? null : new OutputRecord(record, true));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static List<FieldMetaData> toFieldMetaData(List<QFieldMetaData> fields)
   {
      return (fields == null ? null : fields.stream().map(FieldMetaData::new).toList());
   }
}
