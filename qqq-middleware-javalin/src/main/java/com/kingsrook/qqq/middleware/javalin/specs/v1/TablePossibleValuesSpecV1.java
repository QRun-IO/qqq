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
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
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
 ** Spec for searching possible values for a field on a table.
 ** POST /table/{tableName}/possibleValues/{fieldName}
 *******************************************************************************/
public class TablePossibleValuesSpecV1 extends AbstractEndpointSpec<PossibleValuesInput, PossibleValuesResponseV1, PossibleValuesExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/possibleValues/{fieldName}")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Search possible values for a table field")
         .withLongDescription("""
            Search for possible values associated with a field on a table.
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
            .withName("tableName")
            .withDescription("Name of the table containing the field.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH),
         new Parameter()
            .withName("fieldName")
            .withDescription("Name of the field whose possible values should be searched.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("partnerPersonId")
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
      String tableName = getRequestParam(context, "tableName");
      String fieldName = getRequestParam(context, "fieldName");

      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         throw (new QNotFoundException("Could not find table named " + tableName + " in this instance."));
      }

      QFieldMetaData field;
      try
      {
         field = table.getField(fieldName);
      }
      catch(Exception e)
      {
         throw (new QNotFoundException("Could not find field named " + fieldName + " in table " + tableName + "."));
      }

      if(!StringUtils.hasContent(field.getPossibleValueSourceName()))
      {
         throw (new QNotFoundException("Field " + fieldName + " in table " + tableName + " is not associated with a possible value source."));
      }

      PossibleValuesInput input = new PossibleValuesInput();
      input.setPossibleValueSourceName(field.getPossibleValueSourceName());
      input.setTableName(tableName);
      input.setFieldName(fieldName);

      ///////////////////////////////////////////////////////////////////////////////////
      // if the field has a possible value source filter, clone it and set as default.  //
      // also handle use case for filter interpretation, matching the legacy behavior.  //
      ///////////////////////////////////////////////////////////////////////////////////
      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      Map<String, Serializable> otherValues = PossibleValuesSpecUtils.extractOtherValues(requestBody);
      input.setOtherValues(otherValues);

      QQueryFilter defaultFilter = null;
      if(field.getPossibleValueSourceFilter() != null)
      {
         defaultFilter = field.getPossibleValueSourceFilter().clone();

         String useCase = PossibleValuesSpecUtils.extractStringField(requestBody, "useCase");
         PossibleValueSearchFilterUseCase filterUseCase = (useCase != null)
            ? ObjectUtils.tryElse(() -> PossibleValueSearchFilterUseCase.valueOf(useCase.toUpperCase()), PossibleValueSearchFilterUseCase.FORM)
            : PossibleValueSearchFilterUseCase.FORM;

         defaultFilter.interpretValues(MapBuilder.of("input", otherValues), filterUseCase);
      }
      input.setDefaultFilter(defaultFilter);

      if(requestBody != null)
      {
         input.setSearchTerm(PossibleValuesSpecUtils.extractStringField(requestBody, "searchTerm"));
         input.setIdList(PossibleValuesSpecUtils.extractIdList(requestBody));
         input.setLabelList(PossibleValuesSpecUtils.extractLabelList(requestBody));
      }

      input.setPathParams(context.pathParamMap());
      input.setQueryParams(context.queryParamMap());

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
