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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.executors.TableUpdateExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableUpdateInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableUpdateResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableUpdateSpecV1 extends AbstractEndpointSpec<TableUpdateInput, TableUpdateResponseV1, TableUpdateExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/{primaryKey}")
         .withHttpMethod(HttpMethod.PATCH)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Update a record in a table")
         .withLongDescription("""
            Update an existing record in a table, identified by its primary key. The request body should contain the field values to update."""
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(
         new Parameter()
            .withName("tableName")
            .withDescription("Name of the table containing the record to update.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH),
         new Parameter()
            .withName("primaryKey")
            .withDescription("Primary key value of the record to update.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("42")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("fieldName", new Schema()
         .withDescription("Value for a field in the record. Repeat for each field to update.")
         .withType(Type.STRING));

      Schema bodySchema = new Schema()
         .withType(Type.OBJECT)
         .withDescription("JSON object with field names as keys and field values as values.")
         .withProperties(properties);

      Schema multipartSchema = new Schema()
         .withType(Type.OBJECT)
         .withDescription("""
            Form fields named for the record's fields (an empty field clears the value), a file for each blob field to set, and \
            optionally an `associations` field: JSON of association name to a list of associated records.  With the request header \
            `X-QQQ-Association-Format: record-v1`, each associated record is `{"values": {...}, "associations": {...}}`, nested to any \
            depth, and exactly one `associations` field is required.""")
         .withProperties(new LinkedHashMap<>(Map.of(
            "fieldName", new Schema()
               .withDescription("Value for a field in the record. Repeat for each field to set.")
               .withType(Type.STRING))));
      multipartSchema.getProperties().put("associations", new Schema()
         .withDescription("JSON object of association name to a list of associated records.")
         .withType(Type.STRING));

      //////////////////////////////////////////////////////////////
      // ordered, so the published document is stable between runs //
      //////////////////////////////////////////////////////////////
      Map<String, Content> content = new LinkedHashMap<>();
      content.put(ContentType.APPLICATION_JSON.getMimeType(), new Content().withSchema(bodySchema));
      content.put(ContentType.MULTIPART_FORM_DATA.getMimeType(), new Content().withSchema(multipartSchema));
      return new RequestBody().withContent(content);
   }



   /***************************************************************************
    ** Read the changed values from a JSON object body, or from multipart form
    ** fields (uploaded files for blob fields, and an `associations` field), the
    ** same way the legacy update route does - after the edit permission check,
    ** so a refused request is not read (an unknown table is refused the same way).
    ***************************************************************************/
   @Override
   public TableUpdateInput buildInput(Context context) throws Exception
   {
      TableUpdateInput input     = new TableUpdateInput();
      String           tableName = getRequestParam(context, "tableName");
      input.setTableName(tableName);
      input.setPrimaryKey(getRequestParam(context, "primaryKey"));

      UpdateInput updateInput = new UpdateInput(tableName).withInputSource(QInputSource.USER);
      PermissionsHelper.checkTablePermissionThrowing(updateInput, TablePermissionSubType.EDIT);

      QRecord record = new QRecord();
      record.setTableName(tableName);
      QJavalinImplementation.setRecordValuesForInsertOrUpdate(context, QContext.getQInstance().getTable(tableName), record, updateInput);
      input.setRecord(record);
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(TableUpdateResponseV1.class.getSimpleName(), new TableUpdateResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new TableUpdateResponseV1().withRecord(new QRecord()
            .withRecordLabel("Darin Kelkhoff")
            .withTableName("person")
            .withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff")
            .withDisplayValue("id", "1").withDisplayValue("firstName", "Darin").withDisplayValue("lastName", "Kelkhoff")
         )));

      return new BasicResponse("""
         The record that was updated""",
         TableUpdateResponseV1.class.getSimpleName(),
         examples
      );
   }

}
