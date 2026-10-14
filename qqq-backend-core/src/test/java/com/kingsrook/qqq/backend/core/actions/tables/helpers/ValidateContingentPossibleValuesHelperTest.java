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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.values.QCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercise real user writes on a memory-backed cascade and custom provider.
 *******************************************************************************/
class ValidateContingentPossibleValuesHelperTest extends BaseTest
{
   private QTableMetaData address;



   /*******************************************************************************
    ** An isolated A -> B -> C fixture leaves shared TestUtils tables unchanged.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      for(String name : List.of("states", "cities", "addresses"))
      {
         instance.addTable(new QTableMetaData().withName(name).withBackendName(TestUtils.MEMORY_BACKEND_NAME)
            .withPrimaryKeyField("id").withRecordLabelFormat("%s").withRecordLabelFields("name")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withField(new QFieldMetaData("name", QFieldType.STRING))
            .withField(new QFieldMetaData("countryId", QFieldType.INTEGER).withLabel("Country"))
            .withField(new QFieldMetaData("stateId", QFieldType.INTEGER).withLabel("State"))
            .withField(new QFieldMetaData("cityId", QFieldType.INTEGER).withLabel("City")));
      }
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("states"));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("cities"));
      address = instance.getTable("addresses");
      address.getField("stateId").withPossibleValueSourceName("states")
         .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("countryId", QCriteriaOperator.EQUALS, "${input.countryId}")));
      address.getField("cityId").withPossibleValueSourceName("cities")
         .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("stateId", QCriteriaOperator.EQUALS, "${input.stateId}")));
      new QInstanceEnricher(instance).enrich();
      new InsertAction().execute(new InsertInput("states").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("countryId", 1),
         new QRecord().withValue("id", 2).withValue("countryId", 2))));
      new InsertAction().execute(new InsertInput("cities").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("stateId", 1),
         new QRecord().withValue("id", 2).withValue("stateId", 2))));
      CountingProvider.searches = 0;
   }



   /*******************************************************************************
    ** Valid cascade passes; an invalid choice produces a per-record input error.
    *******************************************************************************/
   @Test
   void insertAndCascade() throws Exception
   {
      assertThat(insert(new QRecord().withValue("countryId", 1).withValue("stateId", 1).withValue("cityId", 1)).getErrors()).isEmpty();
      QRecord invalid = insert(new QRecord().withValue("countryId", 2).withValue("stateId", 1).withValue("cityId", 1));
      assertThat(invalid.getErrors()).extracting("message").contains("State [1] is not a valid choice for the selected Country");
      assertThat(insert(new QRecord().withValue("countryId", 1).withValue("stateId", 1).withValue("cityId", 2)).getErrors()).hasSize(1);
      assertThat(insert(new QRecord().withValue("stateId", 1)).getErrors()).isEmpty();
      assertThat(insert(new QRecord().withValue("countryId", 1)).getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** Dependency-only partial updates validate the stored dependent choice.
    *******************************************************************************/
   @Test
   void partialUpdates() throws Exception
   {
      QRecord stored = insert(new QRecord().withValue("countryId", 1).withValue("stateId", 1).withValue("cityId", 1));
      Serializable id = stored.getValue("id");
      QRecord invalid = update(new QRecord().withValue("id", id).withValue("countryId", 2));
      assertThat(invalid.getErrors()).extracting("message").contains("State [1] is not a valid choice for the selected Country");
      assertThat(update(new QRecord().withValue("id", id).withValue("stateId", 2)).getErrors()).hasSize(2);
      assertThat(update(new QRecord().withValue("id", id).withValue("name", "untouched")).getErrors()).isEmpty();
      assertThat(update(new QRecord().withValue("id", id).withValue("countryId", 2).withValue("stateId", 2).withValue("cityId", 2)).getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** The fallback path fills missing values without expanding update records.
    *******************************************************************************/
   @Test
   void missingOldRecordList() throws Exception
   {
      QRecord stored = insert(new QRecord().withValue("countryId", 1).withValue("stateId", 1));
      QRecord change = new QRecord().withValue("id", stored.getValue("id")).withValue("countryId", 2);
      new UpdateAction().performValidations(new UpdateInput().withTableName("addresses").withInputSource(QInputSource.USER).withRecords(List.of(change)), Optional.empty(), false);
      assertThat(change.getErrors()).hasSize(1);
      assertThat(change.getValues()).doesNotContainKey("stateId");
   }



   /*******************************************************************************
    ** System writes and explicit opt-outs retain existing behavior.
    *******************************************************************************/
   @Test
   void bypasses() throws Exception
   {
      QRecord invalid = new QRecord().withValue("countryId", 2).withValue("stateId", 1);
      assertThat(new InsertAction().execute(new InsertInput("addresses").withInputSource(QInputSource.SYSTEM).withRecords(List.of(invalid))).getRecords().get(0).getErrors()).isEmpty();
      address.getField("stateId").withValidatePossibleValueSourceOnWrite(false);
      assertThat(insert(new QRecord().withValue("countryId", 2).withValue("stateId", 1)).getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** Custom providers receive dependency values once per group, beyond 250 ids.
    *******************************************************************************/
   @Test
   void customBatchAndUntouchedUpdate() throws Exception
   {
      customSource();
      List<QRecord> records = new ArrayList<>();
      for(int i = 1; i <= 600; i++)
      {
         records.add(new QRecord().withValue("countryId", 1).withValue("stateId", i));
      }
      records.add(new QRecord().withValue("countryId", 2).withValue("stateId", 1));
      List<QRecord> result = new InsertAction().execute(new InsertInput("addresses").withInputSource(QInputSource.USER).withRecords(records)).getRecords();
      assertThat(CountingProvider.searches).isEqualTo(2);
      assertThat(result.get(599).getErrors()).isEmpty();
      assertThat(result.get(600).getErrors()).hasSize(1);
      update(new QRecord().withValue("id", result.get(0).getValue("id")).withValue("name", "unrelated"));
      assertThat(CountingProvider.searches).isEqualTo(2);
   }



   /*******************************************************************************
    ** Every submitted collection member is checked.
    *******************************************************************************/
   @Test
   void multipleValues() throws Exception
   {
      QRecord record = new QRecord().withValue("countryId", 1).withValue("stateId", new ArrayList<>(List.of(1, 2)));
      ValidateContingentPossibleValuesHelper.validate(address, List.of(record), null, null);
      assertThat(record.getErrors()).extracting("message").containsExactly("State [2] is not a valid choice for the selected Country");
      address.getField("stateId").getPossibleValueSourceFilter().getCriteria().get(0).setOperator(QCriteriaOperator.IN);
      QRecord collectionDependency = new QRecord().withValue("countryId", new ArrayList<>(List.of(1, 2))).withValue("stateId", 2);
      ValidateContingentPossibleValuesHelper.validate(address, List.of(collectionDependency), null, null);
      assertThat(collectionDependency.getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** Configure explicit dependency metadata for a provider with no filter.
    *******************************************************************************/
   private void customSource()
   {
      QContext.getQInstance().addPossibleValueSource(new QPossibleValueSource().withName("customStates")
         .withType(QPossibleValueSourceType.CUSTOM).withCustomCodeReference(new QCodeReference(CountingProvider.class)));
      address.getField("stateId").withPossibleValueSourceName("customStates").withPossibleValueSourceFilter(null).withPossibleValueSourceDependsOn(List.of("countryId"));
   }



   /*******************************************************************************
    ** Execute a user insert through the same validation path as forms and APIs.
    *******************************************************************************/
   private QRecord insert(QRecord record) throws Exception
   {
      return (new InsertAction().execute(new InsertInput("addresses").withInputSource(QInputSource.USER).withRecords(List.of(record))).getRecords().get(0));
   }



   /*******************************************************************************
    ** Execute a partial user update.
    *******************************************************************************/
   private QRecord update(QRecord record) throws Exception
   {
      return (new UpdateAction().execute(new UpdateInput().withTableName("addresses").withInputSource(QInputSource.USER).withRecords(List.of(record))).getRecords().get(0));
   }



   /*******************************************************************************
    ** Instrumented custom provider: only country 1 permits the requested ids.
    *******************************************************************************/
   public static class CountingProvider implements QCustomPossibleValueProvider<Integer>
   {
      private static int searches;



      /*******************************************************************************
       ** Display lookup is independent of filtering.
       *******************************************************************************/
      public QPossibleValue<Integer> getPossibleValue(Serializable id)
      {
         return (new QPossibleValue<>((Integer) id, "State " + id));
      }



      /*******************************************************************************
       ** Search validates using the declared dependency, not a parsed filter.
       *******************************************************************************/
      public List<QPossibleValue<Integer>> search(SearchPossibleValueSourceInput input)
      {
         searches++;
         if(!Objects.equals(1, input.getOtherValues().get("countryId")))
         {
            return (List.of());
         }
         return (input.getIdList().stream().map(this::getPossibleValue).toList());
      }
   }



   /*******************************************************************************
    ** Explicit table dependencies are checked; contextual filter inputs are legal.
    *******************************************************************************/
   @Test
   void validateDependencyMetadata()
   {
      QFieldMetaData field = address.getField("stateId");
      field.withPossibleValueSourceDependsOn(List.of("notAField"));
      QInstanceValidator validator = new QInstanceValidator();
      validator.validateField(QContext.getQInstance(), "State ", Optional.of(address), field);
      assertThat(validator.getErrors()).anyMatch(error -> error.contains("possibleValueSourceDependsOn field notAField"));
      field.withPossibleValueSourceDependsOn(List.of("countryId"))
         .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("countryId", QCriteriaOperator.EQUALS, "${input.externalContext}")));
      validator = new QInstanceValidator();
      validator.validateField(QContext.getQInstance(), "State ", Optional.of(address), field);
      assertThat(validator.getErrors()).isEmpty();
   }
}
