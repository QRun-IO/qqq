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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.AbstractJsonEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** POST /table/{tableName}/formAdjuster/{identifier}/{event}: run a table
 ** form's adjuster (the table's on-load adjuster, identifier `table:{name}`,
 ** or a field's on-load / on-change adjuster, by its formAdjusterIdentifier).
 **
 ** Adjusters are registered by the Material Dashboard module
 ** (FormAdjusterRegistry), which also adds the process that runs them
 ** (FORM_ADJUSTER_PROCESS_NAME).  This spec runs that process in the caller's
 ** session (the legacy /material-dashboard-backend/form-adjuster route runs it
 ** unauthenticated, as the system user) after checking that the caller may
 ** insert or edit records in the table.  Without the module there are no
 ** adjusters and the operation answers 404.
 *******************************************************************************/
public class TableFormAdjusterSpecV1 extends AbstractJsonEndpointSpec
{
   /***************************************************************************
    ** the process the Material Dashboard module registers to run adjusters
    ***************************************************************************/
   public static final String FORM_ADJUSTER_PROCESS_NAME = "MaterialDashboardRunFormAdjusterProcess";

   public static final String EVENT_ON_LOAD   = "onLoad";
   public static final String EVENT_ON_CHANGE = "onChange";

   private static final String RESPONSE_SCHEMA = "FormAdjusterOutputV1";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation()
         .withPath("/table/{tableName}/formAdjuster/{identifier}/{event}")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Run a form adjuster")
         .withLongDescription("""
            Run one of a table form's adjusters and return the changes to apply to the form.  The \
            identifier is `table:{tableName}` for the table's on-load adjuster, or a field's \
            `formAdjusterIdentifier` (from its `materialDashboard` supplemental metadata) for that \
            field's adjusters.  The caller must be allowed to insert or edit records in the table.  \
            Answers 404 when the application registers no form adjusters."""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(
         new Parameter().withName("tableName").withIn(In.PATH).withRequired(true)
            .withDescription("Name of the table whose form is being adjusted.")
            .withSchema(new Schema().withType(Type.STRING)),
         new Parameter().withName("identifier").withIn(In.PATH).withRequired(true)
            .withDescription("`table:{tableName}`, or a field's `formAdjusterIdentifier`.")
            .withSchema(new Schema().withType(Type.STRING)),
         new Parameter().withName("event").withIn(In.PATH).withRequired(true)
            .withDescription("Which adjuster to run.")
            .withSchema(new Schema().withType(Type.STRING).withEnumValues(List.of(EVENT_ON_LOAD, EVENT_ON_CHANGE)))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected Schema defineRequestBodySchema()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("fieldName", new Schema().withType(Type.STRING).withDescription("The field the event is for (field adjusters)."));
      properties.put("newValue", new Schema().withDescription("The field's new value (field adjusters); sent to the adjuster as text."));
      properties.put("allValues", new Schema().withType(Type.OBJECT).withAdditionalProperties(true).withDescription("Every value of the form, by field name."));
      return (new Schema().withType(Type.OBJECT)
         .withDescription("The form's state when the event fired.")
         .withProperties(properties));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getResponseSchemaName()
   {
      return (RESPONSE_SCHEMA);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("updatedFieldMetaData", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withRefToSchema("FieldMetaData"))
         .withDescription("Replacement definitions for fields, by field name."));
      properties.put("updatedFieldValues", new Schema().withType(Type.OBJECT).withAdditionalProperties(true)
         .withDescription("New values for fields, by field name."));
      properties.put("updatedFieldDisplayValues", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withType(Type.STRING))
         .withDescription("Display values for possible-value fields whose values changed, by field name."));
      properties.put("fieldsToClear", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING))
         .withDescription("Fields whose values should be cleared."));
      properties.put("updatedSectionMetaData", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withRefToSchema("TableSection"))
         .withDescription("Replacement definitions for form sections, by section name."));
      properties.put("isFormDisabled", new Schema().withType(Type.BOOLEAN).withDescription("Whether the form should be disabled."));
      properties.put("formDisabledMessage", new Schema().withType(Type.STRING).withDescription("Why the form is disabled."));

      return (Map.of(RESPONSE_SCHEMA, new Schema().withType(Type.OBJECT)
         .withDescription("Changes a form adjuster asks the form to apply.  Absent or null properties mean no change.")
         .withProperties(properties)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected Object handle(JsonInput input) throws Exception
   {
      String tableName  = input.getPathParam("tableName");
      String identifier = input.getPathParam("identifier");
      String event      = input.getPathParam("event");

      if(!Set.of(EVENT_ON_LOAD, EVENT_ON_CHANGE).contains(event))
      {
         throw (new QBadRequestException("Unknown form adjuster event: " + event + " (expected onLoad or onChange)."));
      }

      ////////////////////////////////////////////////////////////////////////
      // an unknown table answers 403, the same as one the user can't see. //
      ////////////////////////////////////////////////////////////////////////
      QInstance qInstance = QContext.getQInstance();
      if(qInstance.getTable(tableName) == null)
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }

      QueryInput permissionInput = new QueryInput(tableName);
      if(!PermissionsHelper.hasTablePermission(permissionInput, tableName, TablePermissionSubType.INSERT)
         && !PermissionsHelper.hasTablePermission(permissionInput, tableName, TablePermissionSubType.EDIT))
      {
         throw (new QPermissionDeniedException("You do not have permission to insert or edit records in this table."));
      }

      if(qInstance.getProcess(FORM_ADJUSTER_PROCESS_NAME) == null)
      {
         throw (new QNotFoundException("This application has no form adjusters."));
      }

      Map<String, Object>       body       = input.getBody();
      Map<String, List<String>> formParams = new HashMap<>();
      putFormParam(formParams, "event", event);
      putFormParam(formParams, "fieldName", textOf(body.get("fieldName")));
      if(body.containsKey("newValue"))
      {
         putFormParam(formParams, "newValue", textOf(body.get("newValue")));
      }
      Object allValues = body.get("allValues");
      putFormParam(formParams, "allValues", allValues == null ? "{}" : JsonUtils.toJson(allValues));

      RunProcessInput processInput = new RunProcessInput();
      processInput.setProcessName(FORM_ADJUSTER_PROCESS_NAME);
      processInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      processInput.addValue("pathParams", new HashMap<>(Map.of("identifier", identifier, "event", event)));
      processInput.addValue("formParams", (Serializable) formParams);

      RunProcessOutput processOutput = new RunProcessAction().execute(processInput);
      if(processOutput.getException().isPresent())
      {
         throw (processOutput.getException().get());
      }

      String responseString = processOutput.getValueString("responseString");
      if(!StringUtils.hasContent(responseString))
      {
         throw (new QException("The form adjuster returned no output."));
      }
      return (JsonUtils.toObject(responseString, LinkedHashMap.class));
   }



   /***************************************************************************
    ** text form of a JSON value, as the multipart route sends it
    ***************************************************************************/
   private static String textOf(Object value)
   {
      if(value == null)
      {
         return (null);
      }
      if(value instanceof Map<?, ?> || value instanceof List<?>)
      {
         return (JsonUtils.toJson(value));
      }
      return (String.valueOf(value));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void putFormParam(Map<String, List<String>> formParams, String name, String value)
   {
      if(value != null)
      {
         formParams.put(name, new ArrayList<>(List.of(value)));
      }
   }

}
