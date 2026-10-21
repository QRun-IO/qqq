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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.enumeration;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecordEnum;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for EnumerationQueryAction
 *******************************************************************************/
class EnumerationQueryActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnfilteredQuery() throws QException
   {
      QInstance instance = defineQInstance();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName("statesEnum");
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size());

      assertEquals(1, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals("Missouri", queryOutput.getRecords().get(0).getValueString("name"));
      assertEquals("MO", queryOutput.getRecords().get(0).getValueString("postalCode"));
      assertEquals(15_000_000, queryOutput.getRecords().get(0).getValueInteger("population"));

      assertEquals(2, queryOutput.getRecords().get(1).getValueInteger("id"));
      assertEquals("Illinois", queryOutput.getRecords().get(1).getValueString("name"));
      assertEquals("IL", queryOutput.getRecords().get(1).getValueString("postalCode"));
      assertEquals(25_000_000, queryOutput.getRecords().get(1).getValueInteger("population"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilteredQuery() throws QException
   {
      QInstance instance = defineQInstance();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName("statesEnum");
      queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria("population", QCriteriaOperator.GREATER_THAN, List.of(20_000_000))));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size());

      assertEquals(2, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals("IL", queryOutput.getRecords().get(0).getValueString("postalCode"));
      assertEquals(25_000_000, queryOutput.getRecords().get(0).getValueInteger("population"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryOrderBy() throws QException
   {
      QInstance instance = defineQInstance();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName("statesEnum");

      queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("name")));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of("Illinois", "Missouri"), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());

      queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("name", false)));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of("Missouri", "Illinois"), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuerySkipLimit() throws QException
   {
      QInstance instance = defineQInstance();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName("statesEnum");
      queryInput.setFilter(new QQueryFilter().withSkip(0).withLimit(null));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of("Missouri", "Illinois"), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());

      queryInput.setFilter(new QQueryFilter().withSkip(1).withLimit(null));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of("Illinois"), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());

      queryInput.setFilter(new QQueryFilter().withSkip(2).withLimit(null));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of(), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());

      queryInput.setFilter(new QQueryFilter().withSkip(null).withLimit(1));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of("Missouri"), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());

      queryInput.setFilter(new QQueryFilter().withSkip(null).withLimit(2));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of("Missouri", "Illinois"), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());

      queryInput.setFilter(new QQueryFilter().withSkip(null).withLimit(3));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of("Missouri", "Illinois"), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());

      queryInput.setFilter(new QQueryFilter().withSkip(null).withLimit(0));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(List.of(), queryOutput.getRecords().stream().map(r -> r.getValueString("name")).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QInstance defineQInstance()
   {
      QInstance instance = QContext.getQInstance();
      instance.addBackend(new QBackendMetaData()
         .withName("enum")
         .withBackendType(EnumerationBackendModule.class)
      );

      instance.addTable(new QTableMetaData()
         .withName("statesEnum")
         .withBackendName("enum")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("postalCode", QFieldType.STRING))
         .withField(new QFieldMetaData("population", QFieldType.INTEGER))
         .withBackendDetails(new EnumerationTableBackendDetails().withEnumClass(States.class))
      );
      return instance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static enum States implements QRecordEnum
   {
      MO(1, "Missouri", "MO", 15_000_000),
      IL(2, "Illinois", "IL", 25_000_000);


      private final Integer id;
      private final String  name;
      private final String  postalCode;
      private final Integer population;



      /*******************************************************************************
       **
       *******************************************************************************/
      States(int id, String name, String postalCode, int population)
      {
         this.id = id;
         this.name = name;
         this.postalCode = postalCode;
         this.population = population;
      }



      /*******************************************************************************
       ** Getter for id
       **
       *******************************************************************************/
      public Integer getId()
      {
         return id;
      }



      /*******************************************************************************
       ** Getter for name
       **
       *******************************************************************************/
      public String getName()
      {
         return name;
      }



      /*******************************************************************************
       ** Getter for postalCode
       **
       *******************************************************************************/
      public String getPostalCode()
      {
         return postalCode;
      }



      /*******************************************************************************
       ** Getter for population
       **
       *******************************************************************************/
      public Integer getPopulation()
      {
         return population;
      }
   }

}