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

package com.kingsrook.sampleapp.metadata;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 ** A country, state and city cascade for table and process forms.
 *******************************************************************************/
public final class ContingentPlaceMetaData
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private ContingentPlaceMetaData()
   {
   }



   /*******************************************************************************
    ** Add isolated sample tables and a navigation section.
    *******************************************************************************/
   public static void addTo(QInstance instance)
   {
      instance.addTable(placeTable("country", "Countries"));
      instance.addTable(placeTable("countryState", "States and provinces")
         .withField(new QFieldMetaData("countryId", QFieldType.INTEGER).withPossibleValueSourceName("country")));
      instance.addTable(placeTable("stateCity", "Cities by state")
         .withField(new QFieldMetaData("stateId", QFieldType.INTEGER).withPossibleValueSourceName("countryState")));
      for(String name : List.of("country", "countryState", "stateCity"))
      {
         instance.addPossibleValueSource(QPossibleValueSource.newForTable(name));
      }
      QTableMetaData address = new QTableMetaData().withName("placeAddress").withLabel("Address book")
         .withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME).withPrimaryKeyField("id")
         .withRecordLabelFormat("%s").withRecordLabelFields("label")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("label", QFieldType.STRING));
      for(QFieldMetaData field : placeFields())
      {
         address.withField(field);
      }
      instance.addTable(address);
      for(String tableName : List.of("country", "countryState", "stateCity", "placeAddress"))
      {
         QTableMetaData table = instance.getTable(tableName);
         table.withBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(tableName)));
         QInstanceEnricher.setInferredFieldBackendNames(table);
      }
      instance.addProcess(new QProcessMetaData().withName("placeAddressPick").withLabel("Pick a place")
         .withStep(new QFrontendStepMetaData().withName("pickPlace")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM))
            .withFormField(placeFields().get(0)).withFormField(placeFields().get(1)).withFormField(placeFields().get(2)))
         .withStep(new QBackendStepMetaData().withName("echoCity").withCode(new QCodeReference(EchoCityStep.class))
            .withInputData(new QFunctionInputMetaData().withFieldList(placeFields()))
            .withOutputMetaData(new QFunctionOutputMetaData().withField(new QFieldMetaData("chosenCityName", QFieldType.STRING))))
         .withStep(new QFrontendStepMetaData().withName("result")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.VIEW_FORM))
            .withViewField(new QFieldMetaData("chosenCityName", QFieldType.STRING))));
      instance.addApp(new QAppMetaData().withName("places").withLabel("Places").withIcon(new QIcon("location_city"))
         .withChild(address).withChild(instance.getTable("country")).withChild(instance.getTable("countryState"))
         .withChild(instance.getTable("stateCity")).withChild(instance.getProcess("placeAddressPick")));
   }



   /*******************************************************************************
    ** Shared fields with independent metadata instances for each form.
    *******************************************************************************/
   private static List<QFieldMetaData> placeFields()
   {
      return (List.of(
         new QFieldMetaData("countryId", QFieldType.INTEGER).withLabel("Country").withPossibleValueSourceName("country"),
         new QFieldMetaData("stateId", QFieldType.INTEGER).withLabel("State / province").withPossibleValueSourceName("countryState")
            .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("countryId", QCriteriaOperator.EQUALS, "${input.countryId}"))),
         new QFieldMetaData("cityId", QFieldType.INTEGER).withLabel("City").withPossibleValueSourceName("stateCity")
            .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("stateId", QCriteriaOperator.EQUALS, "${input.stateId}")))));
   }



   /*******************************************************************************
    ** All lookup tables use the sample server's seeded H2 backend.
    *******************************************************************************/
   private static QTableMetaData placeTable(String name, String label)
   {
      return (new QTableMetaData().withName(name).withLabel(label).withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withPrimaryKeyField("id").withRecordLabelFormat("%s").withRecordLabelFields("name")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("name", QFieldType.STRING)));
   }



   /*******************************************************************************
    ** Echo the chosen city's label on the result screen.
    *******************************************************************************/
   public static class EchoCityStep implements BackendStep
   {
      /*******************************************************************************
       ** Resolve the chosen city through the normal table action.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         output.addValue("chosenCityName", new GetAction().execute(new GetInput("stateCity")
            .withPrimaryKey(input.getValue("cityId"))).getRecord().getValueString("name"));
      }
   }
}
