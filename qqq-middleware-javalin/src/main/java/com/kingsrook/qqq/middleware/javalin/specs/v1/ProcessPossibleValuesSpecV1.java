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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
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
 ** Spec for searching possible values for a field on a process.
 ** POST /processes/{processName}/possibleValues/{fieldName}
 *******************************************************************************/
public class ProcessPossibleValuesSpecV1 extends AbstractEndpointSpec<PossibleValuesInput, PossibleValuesResponseV1, PossibleValuesExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/processes/{processName}/possibleValues/{fieldName}")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Search possible values for a process field")
         .withLongDescription("""
            Search for possible values associated with an input field on a process.
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
            .withName("processName")
            .withDescription("Name of the process containing the field.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("greetInteractive")
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
      String processName = getRequestParam(context, "processName");
      String fieldName   = getRequestParam(context, "fieldName");

      QProcessMetaData process = QContext.getQInstance().getProcess(processName);
      if(process == null)
      {
         throw (new QNotFoundException("Could not find process named " + processName + " in this instance."));
      }

      Optional<QFieldMetaData> optField = process.getInputFields().stream()
         .filter(f -> f.getName().equals(fieldName))
         .findFirst();

      QFieldMetaData field = optField.orElseThrow(
         () -> new QNotFoundException("Could not find field named " + fieldName + " in process " + processName + "."));

      if(!StringUtils.hasContent(field.getPossibleValueSourceName()))
      {
         throw (new QNotFoundException("Field " + fieldName + " in process " + processName + " is not associated with a possible value source."));
      }

      PossibleValuesInput input = new PossibleValuesInput();
      input.setPossibleValueSourceName(field.getPossibleValueSourceName());
      input.setProcessName(processName);
      input.setFieldName(fieldName);

      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      Map<String, Serializable> otherValues = PossibleValuesSpecUtils.extractOtherValues(requestBody);
      input.setOtherValues(otherValues);

      ///////////////////////////////////////////////////////////////////////////////////
      // extract processValues from the request body, for use in filter interpolation  //
      ///////////////////////////////////////////////////////////////////////////////////
      Map<String, Serializable> processValues = PossibleValuesSpecUtils.extractProcessValues(requestBody);
      if(processValues == null)
      {
         processValues = new HashMap<>();
      }

      QQueryFilter defaultFilter = null;
      if(field.getPossibleValueSourceFilter() != null)
      {
         defaultFilter = field.getPossibleValueSourceFilter().clone();

         String useCase = PossibleValuesSpecUtils.extractStringField(requestBody, "useCase");
         PossibleValueSearchFilterUseCase filterUseCase = (useCase != null)
            ? ObjectUtils.tryElse(() -> PossibleValueSearchFilterUseCase.valueOf(useCase.toUpperCase()), PossibleValueSearchFilterUseCase.FORM)
            : PossibleValueSearchFilterUseCase.FORM;

         defaultFilter.interpretValues(MapBuilder.of("input", otherValues, "processValues", processValues), filterUseCase);
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
