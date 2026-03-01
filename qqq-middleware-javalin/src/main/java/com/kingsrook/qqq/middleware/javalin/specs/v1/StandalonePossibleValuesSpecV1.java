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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.middleware.javalin.executors.PossibleValuesExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.PossibleValuesInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PossibleValuesResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.PossibleValueOption;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.PossibleValuesSpecUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;
import org.json.JSONObject;


/*******************************************************************************
 ** Spec for searching a standalone possible value source (not associated with
 ** a specific table or process field).
 ** POST /possibleValues/{possibleValueSourceName}
 *******************************************************************************/
public class StandalonePossibleValuesSpecV1 extends AbstractEndpointSpec<PossibleValuesInput, PossibleValuesResponseV1, PossibleValuesExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/possibleValues/{possibleValueSourceName}")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.GENERAL)
         .withShortSummary("Search a standalone possible value source")
         .withLongDescription("""
            Search for possible values from a standalone possible value source
            (not associated with a specific table or process field).
            Returns a list of id/label pairs that match the given search criteria.""");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(
         new Parameter()
            .withName("possibleValueSourceName")
            .withDescription("Name of the possible value source to search.")
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
   public RequestBody defineRequestBody()
   {
      return PossibleValuesSpecUtils.defineRequestBody();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public PossibleValuesInput buildInput(Context context) throws Exception
   {
      String possibleValueSourceName = getRequestParam(context, "possibleValueSourceName");

      QPossibleValueSource pvs = QContext.getQInstance().getPossibleValueSource(possibleValueSourceName);
      if(pvs == null)
      {
         throw (new QNotFoundException("Could not find possible value source " + possibleValueSourceName + " in this instance."));
      }

      PossibleValuesInput input = new PossibleValuesInput();
      input.setPossibleValueSourceName(possibleValueSourceName);

      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      Map<String, Serializable> otherValues = TablePossibleValuesSpecV1.extractOtherValues(requestBody);
      input.setOtherValues(otherValues);

      ///////////////////////////////////////
      // handle filter from the request    //
      ///////////////////////////////////////
      QQueryFilter defaultFilter = null;
      if(requestBody != null && requestBody.has("filter") && !requestBody.isNull("filter"))
      {
         Object filterFromJson = requestBody.get("filter");
         if(filterFromJson instanceof JSONObject filterJsonObject)
         {
            defaultFilter = JsonUtils.toObject(filterJsonObject.toString(), QQueryFilter.class);

            String useCase = TablePossibleValuesSpecV1.extractStringField(requestBody, "useCase");
            PossibleValueSearchFilterUseCase filterUseCase = ObjectUtils.tryElse(
               () -> PossibleValueSearchFilterUseCase.valueOf(useCase.toUpperCase()),
               PossibleValueSearchFilterUseCase.FORM);

            defaultFilter.interpretValues(filterUseCase, otherValues);
         }
      }
      input.setDefaultFilter(defaultFilter);

      if(requestBody != null)
      {
         input.setSearchTerm(TablePossibleValuesSpecV1.extractStringField(requestBody, "searchTerm"));
         input.setIdList(TablePossibleValuesSpecV1.extractIdList(requestBody));
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      Map<String, Schema> schemas = new LinkedHashMap<>();
      schemas.put(PossibleValuesResponseV1.class.getSimpleName(), new PossibleValuesResponseV1().toSchema());
      schemas.put(PossibleValueOption.class.getSimpleName(), new PossibleValueOption().toSchema());
      return schemas;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         List of possible value options matching the search criteria.""",
         PossibleValuesResponseV1.class.getSimpleName()
      );
   }

}
