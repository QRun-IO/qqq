/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaMissingInputValueBehavior;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.FilterUseCase;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.AbstractFilterExpression;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.FilterVariableExpression;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.BETWEEN;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.FALSE;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IS_BLANK;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.TRUE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QQueryFilter
 **
 *******************************************************************************/
class QQueryFilterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInterpretValues() throws QException
   {
      Map<String, Serializable> inputValues = new HashMap<>();
      inputValues.put("clientIdEquals1", "value");

      AbstractFilterExpression<Serializable> expression = new FilterVariableExpression()
         .withVariableName("clientIdEquals1");

      QQueryFilter qQueryFilter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
      qQueryFilter.interpretValues(inputValues);

      assertEquals("value", qQueryFilter.getCriteria().get(0).getValues().get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInterpretValuesSubFilter() throws QException
   {
      Map<String, Serializable> inputValues = new HashMap<>();
      inputValues.put("clientIdEquals1", "value");

      AbstractFilterExpression<Serializable> expression = new FilterVariableExpression()
         .withVariableName("clientIdEquals1");

      QQueryFilter qQueryFilter = new QQueryFilter()
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", EQUALS, expression)));
      qQueryFilter.interpretValues(inputValues);

      assertEquals("value", qQueryFilter.getSubFilters().get(0).getCriteria().get(0).getValues().get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInterpretValuesNotInMap() throws QException
   {
      AbstractFilterExpression<Serializable> expression = new FilterVariableExpression()
         .withVariableName("clientIdEquals1");

      QQueryFilter qQueryFilter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
      assertThatThrownBy(() -> qQueryFilter.interpretValues(Collections.emptyMap()))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("Missing value for variable: clientIdEquals1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInterpretValuesEmptyString() throws QException
   {
      Map<String, Serializable> inputValues = new HashMap<>();
      inputValues.put("clientIdEquals1", "");

      AbstractFilterExpression<Serializable> expression = new FilterVariableExpression()
         .withVariableName("clientIdEquals1");

      QQueryFilter qQueryFilter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
      assertThatThrownBy(() -> qQueryFilter.interpretValues(inputValues))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("Missing value for variable: clientIdEquals1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testPrepForBackend() throws QException
   {
      FilterVariableExpression fve0 = new FilterVariableExpression();
      FilterVariableExpression fve1 = new FilterVariableExpression();
      FilterVariableExpression fve2 = new FilterVariableExpression();
      FilterVariableExpression fve3 = new FilterVariableExpression();
      FilterVariableExpression fve4 = new FilterVariableExpression();
      FilterVariableExpression fve5 = new FilterVariableExpression();
      FilterVariableExpression fve6 = new FilterVariableExpression();
      FilterVariableExpression fve7 = new FilterVariableExpression();

      QQueryFilter qQueryFilter = new QQueryFilter(
         new QFilterCriteria("id", EQUALS, fve0),
         new QFilterCriteria("value", IS_BLANK, fve1),
         new QFilterCriteria("id", EQUALS, fve2),
         new QFilterCriteria("id", BETWEEN, fve3, fve4),
         new QFilterCriteria("id", BETWEEN, fve5, fve6),
         new QFilterCriteria("joinTable.someFieldId", EQUALS, fve7)
      );
      qQueryFilter.prepForBackend();

      assertEquals("idEquals", fve0.getVariableName());
      assertEquals("valueIsBlank", fve1.getVariableName());
      assertEquals("idEquals2", fve2.getVariableName());
      assertEquals("idBetweenFrom", fve3.getVariableName());
      assertEquals("idBetweenTo", fve4.getVariableName());
      assertEquals("idBetweenFrom2", fve5.getVariableName());
      assertEquals("idBetweenTo2", fve6.getVariableName());
      assertEquals("joinTableSomeFieldIdEquals", fve7.getVariableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInterpretValueVariableExpressionNotFoundUseCases() throws QException
   {
      Map<String, Serializable> inputValues = new HashMap<>();

      AbstractFilterExpression<Serializable> expression = new FilterVariableExpression()
         .withVariableName("clientId");

      ////////////////////////////////////////
      // Control - where the value IS found //
      ////////////////////////////////////////
      inputValues.put("clientId", 47);
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
         filter.interpretValues(inputValues);
         assertEquals(47, filter.getCriteria().get(0).getValues().get(0));
         assertEquals(EQUALS, filter.getCriteria().get(0).getOperator());
      }

      //////////////////////////////////////////////////////
      // now - remove the value for the next set of cases //
      //////////////////////////////////////////////////////
      inputValues.remove("clientId");

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // a use-case that says to remove-from-filter, which, means translate to a criteria of "TRUE" //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
         filter.interpretValues(new RemoveFromFilterUseCase(), inputValues);
         assertEquals(0, filter.getCriteria().get(0).getValues().size());
         assertEquals(TRUE, filter.getCriteria().get(0).getOperator());
      }

      //////////////////////////////////////////////////////////////////////////////////////////////
      // a use-case that says to make-no-matches, which, means translate to a criteria of "FALSE" //
      //////////////////////////////////////////////////////////////////////////////////////////////
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
         filter.interpretValues(new MakeNoMatchesUseCase(), inputValues);
         assertEquals(0, filter.getCriteria().get(0).getValues().size());
         assertEquals(FALSE, filter.getCriteria().get(0).getOperator());
      }

      ///////////////////////////////////////////
      // a use-case that says to treat as null //
      ///////////////////////////////////////////
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
         filter.interpretValues(new InterpretAsNullValueUseCase(), inputValues);
         assertNull(filter.getCriteria().get(0).getValues().get(0));
         assertEquals(EQUALS, filter.getCriteria().get(0).getOperator());
      }

      ///////////////////////////////////
      // a use-case that says to throw //
      ///////////////////////////////////
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
         assertThatThrownBy(() -> filter.interpretValues(new ThrowExceptionUseCase(), inputValues))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("Missing value for variable: clientId");
      }

      //////////////////////////////////////////////////////////
      // verify that empty-string is treated as not-found too //
      //////////////////////////////////////////////////////////
      inputValues.put("clientId", "");
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, expression));
         assertThatThrownBy(() -> filter.interpretValues(new ThrowExceptionUseCase(), inputValues))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("Missing value for variable: clientId");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInterpretValueStringStyleNotFoundUseCases() throws QException
   {
      Map<String, Serializable> inputValues = new HashMap<>();

      ////////////////////////////////////////
      // Control - where the value IS found //
      ////////////////////////////////////////
      inputValues.put("clientId", 47);
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}"));
         filter.interpretValues(inputValues);
         assertEquals(47, filter.getCriteria().get(0).getValues().get(0));
         assertEquals(EQUALS, filter.getCriteria().get(0).getOperator());
      }

      //////////////////////////////////////////////////////
      // now - remove the value for the next set of cases //
      //////////////////////////////////////////////////////
      inputValues.remove("clientId");

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // a use-case that says to remove-from-filter, which, means translate to a criteria of "TRUE" //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}"));
         filter.interpretValues(new RemoveFromFilterUseCase(), inputValues);
         assertEquals(0, filter.getCriteria().get(0).getValues().size());
         assertEquals(TRUE, filter.getCriteria().get(0).getOperator());
      }

      //////////////////////////////////////////////////////////////////////////////////////////////
      // a use-case that says to make-no-matches, which, means translate to a criteria of "FALSE" //
      //////////////////////////////////////////////////////////////////////////////////////////////
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}"));
         filter.interpretValues(new MakeNoMatchesUseCase(), inputValues);
         assertEquals(0, filter.getCriteria().get(0).getValues().size());
         assertEquals(FALSE, filter.getCriteria().get(0).getOperator());
      }

      ///////////////////////////////////////////
      // a use-case that says to treat as null //
      ///////////////////////////////////////////
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}"));
         filter.interpretValues(new InterpretAsNullValueUseCase(), inputValues);
         assertNull(filter.getCriteria().get(0).getValues().get(0));
         assertEquals(EQUALS, filter.getCriteria().get(0).getOperator());
      }

      ///////////////////////////////////
      // a use-case that says to throw //
      ///////////////////////////////////
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}"));
         assertThatThrownBy(() -> filter.interpretValues(new ThrowExceptionUseCase(), inputValues))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("Missing value for criteria on field: id");
      }

      //////////////////////////////////////////////////////////
      // verify that empty-string is treated as not-found too //
      //////////////////////////////////////////////////////////
      inputValues.put("clientId", "");
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}"));
         assertThatThrownBy(() -> filter.interpretValues(new ThrowExceptionUseCase(), inputValues))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("Missing value for criteria on field: id");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInterpretValueStringStyleCoalescingMultipleInputMaps() throws QException
   {
      int inputValue   = 47;
      int processValue = 42;

      Map<String, Serializable> inputValuesMap   = Map.of("clientId", inputValue);
      Map<String, Serializable> processValuesMap = Map.of("clientId", processValue);

      String inputThenProcess = "${input.clientId}??${processValues.clientId}";

      {
         /////////////////////////////////////////////
         // value in both maps - uses the first one //
         /////////////////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, inputThenProcess));
         filter.interpretValues(Map.of("input", inputValuesMap, "processValues", processValuesMap), FilterUseCase.DEFAULT);
         assertEquals(inputValue, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         ////////////////////////////////////////////////////////////
         // key in first map, but empty string - so use second one //
         ////////////////////////////////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}??${processValues.commonClientId}"));
         filter.interpretValues(Map.of("input", Map.of("clientId", ""), "processValues", Map.of("commonClientId", processValue)), FilterUseCase.DEFAULT);
         assertEquals(processValue, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         ////////////////////////////////////////////////////
         // key in first map, but null - so use second one //
         ////////////////////////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}??${processValues.commonClientId}"));
         filter.interpretValues(Map.of("input", MapBuilder.of("clientId", null), "processValues", Map.of("commonClientId", processValue)), FilterUseCase.DEFAULT);
         assertEquals(processValue, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         //////////////////////////////////////////
         // key not in first - so use second one //
         //////////////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}??${processValues.commonClientId}"));
         filter.interpretValues(Map.of("input", Map.of("foo", "bar"), "processValues", Map.of("commonClientId", processValue)), FilterUseCase.DEFAULT);
         assertEquals(processValue, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         ///////////////////////////////////////
         // 2nd map not given - use first one //
         ///////////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, inputThenProcess));
         filter.interpretValues(Map.of("input", inputValuesMap), FilterUseCase.DEFAULT);
         assertEquals(inputValue, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         ////////////////////////////////////////
         // 1st map not given - use second one //
         ////////////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, inputThenProcess));
         filter.interpretValues(Map.of("processValues", processValuesMap), FilterUseCase.DEFAULT);
         assertEquals(processValue, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         ////////////////////////////////////
         // 1st map empty - use second one //
         ////////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, inputThenProcess));
         filter.interpretValues(Map.of("input", Map.of(), "processValues", processValuesMap), FilterUseCase.DEFAULT);
         assertEquals(processValue, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         ///////////////////////////////////
         // 1st map null - use second one //
         ///////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, inputThenProcess));
         filter.interpretValues(MapBuilder.of("input", null, "processValues", processValuesMap), FilterUseCase.DEFAULT);
         assertEquals(processValue, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         ////////////////////////////////////////////////////////////
         // not found in either map - throw (for default use case) //
         ////////////////////////////////////////////////////////////
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, inputThenProcess));
         assertThatThrownBy(() -> filter.interpretValues(Map.of("input", Map.of()), FilterUseCase.DEFAULT))
            .hasMessageContaining("Missing value for criteria on field: id");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInterpretValueStringStyleCoalescingDotsInKey() throws QException
   {
      Map<String, Serializable> inputValues = new HashMap<>();

      inputValues.put("subObject.clientId", 47);
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}??${input.subObject.clientId}"));
         filter.interpretValues(inputValues);
         assertEquals(47, filter.getCriteria().get(0).getValues().get(0));
      }

      inputValues.put("clientId", 42);
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.clientId}??${input.subObject.clientId}"));
         filter.interpretValues(inputValues);
         assertEquals(42, filter.getCriteria().get(0).getValues().get(0));
      }

      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", EQUALS, "${input.userId}??${input.subObject.userId}"));
         assertThatThrownBy(() -> filter.interpretValues(inputValues))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("Missing value for criteria on field: id");
      }

   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class RemoveFromFilterUseCase implements FilterUseCase
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior()
      {
         return CriteriaMissingInputValueBehavior.REMOVE_FROM_FILTER;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class MakeNoMatchesUseCase implements FilterUseCase
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior()
      {
         return CriteriaMissingInputValueBehavior.MAKE_NO_MATCHES;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class InterpretAsNullValueUseCase implements FilterUseCase
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior()
      {
         return CriteriaMissingInputValueBehavior.INTERPRET_AS_NULL_VALUE;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class ThrowExceptionUseCase implements FilterUseCase
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior()
      {
         return CriteriaMissingInputValueBehavior.THROW_EXCEPTION;
      }
   }
}
