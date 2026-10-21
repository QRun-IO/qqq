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
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecordEnum;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for EnumerationCountAction
 *******************************************************************************/
class EnumerationCountActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnfilteredCount() throws QException
   {
      QInstance instance = defineQInstance();

      CountInput countInput = new CountInput();
      countInput.setTableName("statesEnum");
      CountOutput countOutput = new CountAction().execute(countInput);
      assertEquals(2, countOutput.getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilteredCount() throws QException
   {
      QInstance instance = defineQInstance();

      CountInput countInput = new CountInput();
      countInput.setTableName("statesEnum");
      countInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria("population", QCriteriaOperator.GREATER_THAN, List.of(20_000_000))));
      CountOutput countOutput = new CountAction().execute(countInput);
      assertEquals(1, countOutput.getCount());
   }



   /*******************************************************************************
    ** Count evaluates all matching enum records without changing a reusable page.
    *******************************************************************************/
   @Test
   void testCountIgnoresPaginationAndPreservesFilter() throws QException
   {
      defineQInstance();
      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("population", QCriteriaOperator.GREATER_THAN, 20_000_000))
         .withSkip(5).withLimit(0);
      assertEquals(1, CountAction.execute("statesEnum", filter));
      assertEquals(5, filter.getSkip());
      assertEquals(0, filter.getLimit());
      assertEquals(List.of(20_000_000), filter.getCriteria().get(0).getValues());
      assertEquals(2, CountAction.execute("statesEnum", new QQueryFilter().withLimit(1)));
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
