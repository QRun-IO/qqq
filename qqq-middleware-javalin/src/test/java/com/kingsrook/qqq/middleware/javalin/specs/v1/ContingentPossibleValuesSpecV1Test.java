/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueDependencyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FieldMetaData;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Contract-level tests of contingent metadata, options and user writes.
 *******************************************************************************/
class ContingentPossibleValuesSpecV1Test extends SpecTestBase
{
   /*******************************************************************************
    ** Main route.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new TablePossibleValuesSpecV1());
   }



   /*******************************************************************************
    ** Related routes share exactly the same instance.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new ProcessPossibleValuesSpecV1(), new TableMetaDataSpecV1(), new TableInsertSpecV1()));
   }



   /*******************************************************************************
    ** UI API version.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** A person's partner must have the selected first name.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = TestUtils.defineInstance();
      QFieldMetaData field = instance.getTable("person").getField("partnerPersonId");
      field.withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "${input.firstName}")));
      instance.addProcess(new QProcessMetaData().withName("contingentProcess")
         .withStep(new QFrontendStepMetaData().withName("setup").withFormField(field.clone())
            .withFormField(new QFieldMetaData("firstName", QFieldType.STRING)))
         .withStep(new QBackendStepMetaData().withName("work").withCode(new QCodeReference(MockBackendStep.class))
            .withInputData(new QFunctionInputMetaData().withField(field.clone()))));
      return (instance);
   }



   /*******************************************************************************
    ** Lowercase use cases, other values and SHOW_ALL behave alike on both routes.
    *******************************************************************************/
   @Test
   void searches()
   {
      for(String path : List.of("/table/person/possibleValues/partnerPersonId", "/processes/contingentProcess/possibleValues/partnerPersonId"))
      {
         assertThat(optionCount(post(path, Map.of("useCase", "filter")))).isGreaterThan(0);
         assertThat(optionCount(post(path, Map.of("useCase", "form")))).isZero();
         JSONObject narrowed = post(path, Map.of("values", Map.of("firstName", "Darin"), "useCase", "form"));
         assertThat(optionCount(narrowed)).isEqualTo(1);
         assertThat(narrowed.getJSONArray("options").getJSONObject(0).getInt("id")).isEqualTo(1);
         JSONObject all = post(path, Map.of("useCase", "filter"));
         int firstId = all.getJSONArray("options").getJSONObject(0).getInt("id");
         assertThat(optionCount(post(path, Map.of("values", Map.of("firstName", "NoSuchFirstName"))))).isZero();
         JSONObject selected = post(path, Map.of("useCase", "filter", "ids", List.of(firstId)));
         assertThat(optionCount(selected)).isGreaterThan(0);
      }
      QFieldMetaData field = serverQInstance.getTable("person").getField("partnerPersonId");
      field.withPossibleValueSourceDependencyBehavior(PossibleValueDependencyBehavior.SHOW_ALL_UNTIL_SET);
      serverQInstance.getProcess("contingentProcess").getInputFields().stream().filter(f -> f.getName().equals("partnerPersonId"))
         .forEach(f -> f.withPossibleValueSourceDependencyBehavior(PossibleValueDependencyBehavior.SHOW_ALL_UNTIL_SET));
      assertThat(optionCount(post("/table/person/possibleValues/partnerPersonId", Map.of("useCase", "form")))).isGreaterThan(0);
      assertThat(optionCount(post("/processes/contingentProcess/possibleValues/partnerPersonId", Map.of("useCase", "form")))).isGreaterThan(0);
   }



   /*******************************************************************************
    ** Dependencies are typed and omitted entirely for ordinary fields.
    *******************************************************************************/
   @Test
   void metadata()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/table/person").asString();
      assertThat(response.getStatus()).isEqualTo(200);
      JSONObject fields = new JSONObject(response.getBody()).getJSONObject("fields");
      assertThat(fields.getJSONObject("partnerPersonId").getJSONArray("possibleValueSourceDependsOn").toList()).containsExactly("firstName");
      assertThat(fields.getJSONObject("partnerPersonId").getString("possibleValueSourceDependencyBehavior")).isEqualTo("DISABLE_UNTIL_SET");
      assertThat(fields.getJSONObject("firstName").has("possibleValueSourceDependsOn")).isFalse();
      assertThat(fields.getJSONObject("firstName").has("possibleValueSourceDependencyBehavior")).isFalse();
      QFieldMetaData field = serverQInstance.getTable("person").getField("partnerPersonId");
      FieldMetaData legacyWrapped = new FieldMetaData(new QFrontendFieldMetaData(field));
      assertThat(legacyWrapped.getPossibleValueSourceDependsOn()).containsExactly("firstName");
      assertThat(legacyWrapped.getPossibleValueSourceDependencyBehavior()).isEqualTo("DISABLE_UNTIL_SET");
   }



   /*******************************************************************************
    ** An invalid dependent choice returns the existing API validation error.
    *******************************************************************************/
   @Test
   void invalidInsert()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/table/person")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("firstName", "NoSuchFirstName", "lastName", "Test", "email", "test@example.invalid", "partnerPersonId", 1))).asString();
      assertThat(response.getStatus()).isEqualTo(400);
      assertThat(response.getBody()).contains("is not a valid choice for the selected First Name");
   }



   /*******************************************************************************
    ** Send a JSON possible-value request.
    *******************************************************************************/
   private JSONObject post(String path, Map<String, ?> body)
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + path).contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(body)).asString();
      assertThat(response.getStatus()).isEqualTo(200);
      return (new JSONObject(response.getBody()));
   }



   /*******************************************************************************
    ** Empty option lists may be omitted by NON_EMPTY serialization.
    *******************************************************************************/
   private int optionCount(JSONObject response)
   {
      return (response.has("options") ? response.getJSONArray("options").length() : 0);
   }
}
