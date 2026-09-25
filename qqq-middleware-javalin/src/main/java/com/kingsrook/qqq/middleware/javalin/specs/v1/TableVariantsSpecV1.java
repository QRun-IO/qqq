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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.TableVariantsExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableVariantsInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableVariantsResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** Spec for listing the variants of a table whose backend uses variants
 ** (GET /table/{tableName}/variants) - the v1 form of the legacy variants route.
 *******************************************************************************/
public class TableVariantsSpecV1 extends AbstractEndpointSpec<TableVariantsInput, TableVariantsResponseV1, TableVariantsExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/variants")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("List a table's backend variants")
         .withLongDescription("""
            List the variants a table's backend can be scoped to (for example one data set per store).  Requires read
            permission on the table and on the backend's variant options table.""");
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
            .withDescription("Name of the table whose variants to list.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public TableVariantsInput buildInput(Context context) throws Exception
   {
      return (new TableVariantsInput().withTableName(getRequestParam(context, "tableName")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(TableVariantsResponseV1.class.getSimpleName(), new TableVariantsResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      TableVariantsResponseV1 example = new TableVariantsResponseV1();
      example.setVariants(List.of(new TableVariant().withType("store").withId("1").withName("Main Street")));

      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("A table with variants", new Example().withValue(example));

      return new BasicResponse("""
         The variants the table can be scoped to.""",
         TableVariantsResponseV1.class.getSimpleName(),
         examples
      );
   }



   /***************************************************************************
    ** Always include the list, so a table without variants answers an empty one.
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, TableVariantsResponseV1 output) throws Exception
   {
      context.result(JsonUtils.toJsonCustomized(output, builder -> builder.serializationInclusion(JsonInclude.Include.ALWAYS)));
   }

}
