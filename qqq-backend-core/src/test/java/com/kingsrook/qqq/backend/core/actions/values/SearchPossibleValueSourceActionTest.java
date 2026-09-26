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

package com.kingsrook.qqq.backend.core.actions.values;


import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for SearchPossibleValueSourceAction
 *******************************************************************************/
class SearchPossibleValueSourceActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach() throws QException
   {
      MemoryRecordStore.getInstance().reset();
      TestUtils.insertDefaultShapes(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   @ValueSource(strings = { " " })
   void testSearchPvsAction_enumNullAndEmptySearchTerms(String searchTerm) throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput(searchTerm, TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
      assertEquals(2, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(1) && pv.getLabel().equals("IL"));
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("MO"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = { "I", "IL", "1", "i", "iL", "il" })
   void testSearchPvsAction_enumMatchesForIL(String searchTerm) throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput(searchTerm, TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(1) && pv.getLabel().equals("IL"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = { "3", "ILL" })
   void testSearchPvsAction_enumMatchesNothing(String searchTerm) throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput(searchTerm, TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
      assertEquals(0, output.getResults().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_enumById() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById(2, TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("MO"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_enumByIdWrongType() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById("2", TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("MO"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_enumByIdNotFound() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById(-1, TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
      assertEquals(0, output.getResults().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @ParameterizedTest
   @NullAndEmptySource
   @ValueSource(strings = { " " })
   void testSearchPvsAction_tableNullAndEmptySearchTerms(String searchTerm) throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput(searchTerm, TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
      assertEquals(3, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(1) && pv.getLabel().equals("Triangle"));
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("Square"));
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(3) && pv.getLabel().equals("Circle"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = { "1", "Triangle" })
   void testSearchPvsAction_tableMatchesOne(String searchTerm) throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput(searchTerm, TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(1) && pv.getLabel().equals("Triangle"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_tableById() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById(2, TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("Square"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_tableByIdWrongType() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById("2", TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("Square"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_tableByIds() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputByIds(List.of(2, 3), TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
      assertEquals(2, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("Square"));
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(3) && pv.getLabel().equals("Circle"));
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_tableByLabels() throws QException
   {
      {
         SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputByLabels(List.of("Square", "Circle"), TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
         assertEquals(2, output.getResults().size());
         assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("Square"));
         assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(3) && pv.getLabel().equals("Circle"));
      }

      {
         SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputByLabels(List.of(), TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
         assertEquals(0, output.getResults().size());
      }

      {
         SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputByLabels(List.of("notFound"), TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
         assertEquals(0, output.getResults().size());
      }
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_enumByLabel() throws QException
   {
      {
         SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputByLabels(List.of("IL", "MO", "XX"), TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
         assertEquals(2, output.getResults().size());
         assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(1) && pv.getLabel().equals("IL"));
         assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("MO"));
      }

      {
         SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputByLabels(List.of("Il", "mo", "XX"), TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
         assertEquals(2, output.getResults().size());
         assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(1) && pv.getLabel().equals("IL"));
         assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(2) && pv.getLabel().equals("MO"));
      }

      {
         SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputByLabels(List.of(), TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
         assertEquals(0, output.getResults().size());
      }

      {
         SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputByLabels(List.of("not-found"), TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
         assertEquals(0, output.getResults().size());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_tableByIdNotFound() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById(-1, TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
      assertEquals(0, output.getResults().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_tableByIdOnlyNonNumeric() throws QException
   {
      QContext.getQInstance().getPossibleValueSource(TestUtils.TABLE_NAME_SHAPE)
         .withSearchFields(List.of("id"));

      /////////////////////////////////////////////////////////////////////////////////////////////
      // a non-integer input should find nothing                                                 //
      // the catch { (IN, empty) } code makes this happen - without that, all records are found. //
      // (furthermore, i think that's only exposed if there's only 1 search field, maybe)        //
      /////////////////////////////////////////////////////////////////////////////////////////////
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput("A", TestUtils.TABLE_NAME_SHAPE);
      assertEquals(0, output.getResults().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_tableByLocalDate() throws QException
   {
      MemoryRecordStore.getInstance().reset();

      ////////////////////////////////////////////
      // make a PVS for the person-memory table //
      ////////////////////////////////////////////
      QContext.getQInstance().addPossibleValueSource(QPossibleValueSource.newForTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withSearchFields(List.of("id", "firstName", "birthDate"))
      );

      List<QRecord> shapeRecords = List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Homer").withValue("birthDate", LocalDate.of(1960, Month.JANUARY, 1)),
         new QRecord().withValue("id", 2).withValue("firstName", "Marge").withValue("birthDate", LocalDate.of(1961, Month.FEBRUARY, 2)),
         new QRecord().withValue("id", 3).withValue("firstName", "Bart").withValue("birthDate", LocalDate.of(1980, Month.MARCH, 3)));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(shapeRecords);
      new InsertAction().execute(insertInput);

      /////////////////////////////////////
      // a parseable date yields a match //
      /////////////////////////////////////
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput("1960-01-01", TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(1));

      ///////////////////////////////////////////////////////////////////////
      // alternative date format also works (thanks to ValueUtils parsing) //
      ///////////////////////////////////////////////////////////////////////
      output = getSearchPossibleValueSourceOutput("1/1/1960", TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals(1));

      ///////////////////////////////////
      // incomplete date finds nothing //
      ///////////////////////////////////
      output = getSearchPossibleValueSourceOutput("1960-01", TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals(0, output.getResults().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_customSearchTermFound() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput("Custom[3]", TestUtils.POSSIBLE_VALUE_SOURCE_CUSTOM);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals("3") && pv.getLabel().equals("Custom[3]"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_customSearchTermNotFound() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput("Foo", TestUtils.POSSIBLE_VALUE_SOURCE_CUSTOM);
      assertEquals(0, output.getResults().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_customSearchTermManyFound() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutput("Custom", TestUtils.POSSIBLE_VALUE_SOURCE_CUSTOM);
      assertEquals(10, output.getResults().size());
      assertThat(output.getResults()).allMatch(pv -> pv.getLabel().startsWith("Custom["));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_customIdFound() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById("4", TestUtils.POSSIBLE_VALUE_SOURCE_CUSTOM);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals("4") && pv.getLabel().equals("Custom[4]"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_customIdFoundDifferentType() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById(5, TestUtils.POSSIBLE_VALUE_SOURCE_CUSTOM);
      assertEquals(1, output.getResults().size());
      assertThat(output.getResults()).anyMatch(pv -> pv.getId().equals("5") && pv.getLabel().equals("Custom[5]"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSearchPvsAction_customIdNotFound() throws QException
   {
      SearchPossibleValueSourceOutput output = getSearchPossibleValueSourceOutputById(-1, TestUtils.POSSIBLE_VALUE_SOURCE_CUSTOM);
      assertEquals(0, output.getResults().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SearchPossibleValueSourceOutput getSearchPossibleValueSourceOutput(String searchTerm, String possibleValueSourceName) throws QException
   {
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput();
      input.setSearchTerm(searchTerm);
      input.setPossibleValueSourceName(possibleValueSourceName);
      SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceAction().execute(input);
      return output;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SearchPossibleValueSourceOutput getSearchPossibleValueSourceOutputById(Serializable id, String possibleValueSourceName) throws QException
   {
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput();
      input.setIdList(List.of(id));
      input.setPossibleValueSourceName(possibleValueSourceName);
      SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceAction().execute(input);
      return output;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SearchPossibleValueSourceOutput getSearchPossibleValueSourceOutputByIds(List<Serializable> ids, String possibleValueSourceName) throws QException
   {
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput();
      input.setIdList(ids);
      input.setPossibleValueSourceName(possibleValueSourceName);
      SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceAction().execute(input);
      return output;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SearchPossibleValueSourceOutput getSearchPossibleValueSourceOutputByLabels(List<String> labels, String possibleValueSourceName) throws QException
   {
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput();
      input.setLabelList(labels);
      input.setPossibleValueSourceName(possibleValueSourceName);
      SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceAction().execute(input);
      return output;
   }

}