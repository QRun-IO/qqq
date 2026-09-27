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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.math.BigDecimal;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QFormulaException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.reporting.FormulaInterpreter.interpretFormula;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for FormulaInterpreter
 *******************************************************************************/
class FormulaInterpreterTest extends BaseTest
{
   public static final Offset<BigDecimal> ZERO_OFFSET = Offset.offset(BigDecimal.ZERO);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInterpretFormulaSimpleSuccess() throws QFormulaException
   {
      QMetaDataVariableInterpreter vi = new QMetaDataVariableInterpreter();

      assertEquals(new BigDecimal("7"), interpretFormula(vi, "7"));
      assertEquals(new BigDecimal("8"), interpretFormula(vi, "ADD(3,5)"));
      assertEquals(new BigDecimal("9"), interpretFormula(vi, "ADD(2,ADD(3,4))"));
      assertEquals(new BigDecimal("10"), interpretFormula(vi, "ADD(ADD(1,5),4)"));
      assertEquals(new BigDecimal("11"), interpretFormula(vi, "ADD(ADD(1,5),ADD(2,3))"));
      assertEquals(new BigDecimal("15"), interpretFormula(vi, "ADD(1,ADD(2,ADD(3,ADD(4,5))))"));
      assertEquals(new BigDecimal("15"), interpretFormula(vi, "ADD(1,ADD(ADD(2,ADD(3,4)),5))"));
      assertEquals(new BigDecimal("15"), interpretFormula(vi, "ADD(ADD(ADD(ADD(1,2),3),4),5)"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInterpretFormulaWithVariables() throws QFormulaException
   {
      QMetaDataVariableInterpreter vi = new QMetaDataVariableInterpreter();
      vi.addValueMap("input", Map.of("i", 5, "j", 6, "f", new BigDecimal("0.1")));

      assertEquals("5", interpretFormula(vi, "${input.i}"));
      assertEquals(new BigDecimal("8"), interpretFormula(vi, "ADD(3,${input.i})"));
      assertEquals(new BigDecimal("11"), interpretFormula(vi, "ADD(${input.i},${input.j})"));
      assertEquals(new BigDecimal("11.1"), interpretFormula(vi, "ADD(${input.f},ADD(${input.i},${input.j}))"));
      assertEquals(new BigDecimal("11.2"), interpretFormula(vi, "ADD(ADD(${input.f},ADD(${input.i},${input.j})),${input.f})"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInterpretFormulaRecursiveExceptions()
   {
      QMetaDataVariableInterpreter vi = new QMetaDataVariableInterpreter();
      vi.addValueMap("input", Map.of("i", 5, "c", 'c'));

      assertThatThrownBy(() -> interpretFormula(vi, "")).hasRootCauseMessage("No results from formula");
      assertThatThrownBy(() -> interpretFormula(vi, "NOT-A-FUN(1,2)")).hasRootCauseMessage("Unable to evaluate unrecognized expression: NOT-A-FUN");
      assertThatThrownBy(() -> interpretFormula(vi, "ADD(1)")).hasRootCauseMessage("Wrong number of arguments (required: 2, received: 1)");
      assertThatThrownBy(() -> interpretFormula(vi, "ADD(1,2,3)")).hasRootCauseMessage("Wrong number of arguments (required: 2, received: 3)");
      assertThatThrownBy(() -> interpretFormula(vi, "ADD(1,A)")).hasRootCauseMessage("Could not process [A] as a number");
      assertThatThrownBy(() -> interpretFormula(vi, "ADD(1,${input.c})")).hasRootCauseMessage("Could not process [c] as a number");
      // todo - bad syntax (e.g., missing ')'
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFunctions() throws QFormulaException
   {
      QMetaDataVariableInterpreter vi = new QMetaDataVariableInterpreter();

      assertEquals(new BigDecimal("3"), interpretFormula(vi, "ADD(1,2)"));
      assertEquals(new BigDecimal("2"), interpretFormula(vi, "MINUS(4,2)"));
      assertEquals(new BigDecimal("34.500"), interpretFormula(vi, "MULTIPLY(100,0.345)"));

      assertThat((BigDecimal) interpretFormula(vi, "DIVIDE(1,2)")).isCloseTo(new BigDecimal("0.5"), ZERO_OFFSET);
      assertThat((BigDecimal) interpretFormula(vi, "DIVIDE_SCALE(1,2,0)")).isEqualTo(new BigDecimal("1"));
      assertThat((BigDecimal) interpretFormula(vi, "DIVIDE_SCALE(1,2,0)")).isEqualTo(new BigDecimal("1"));
      assertThat((BigDecimal) interpretFormula(vi, "DIVIDE_SCALE(1,2,3)")).isEqualTo(new BigDecimal("0.500"));
      assertThat((BigDecimal) interpretFormula(vi, "DIVIDE_SCALE(1,2,4)")).isEqualTo(new BigDecimal("0.5000"));
      assertNull(interpretFormula(vi, "DIVIDE(1,0)"));
      assertNull(interpretFormula(vi, "DIVIDE(1,0.0)"));
      assertNull(interpretFormula(vi, "DIVIDE_SCALE(1,0,0)"));
      assertNull(interpretFormula(vi, "DIVIDE_SCALE(1,0.0,1)"));

      assertEquals(new BigDecimal("0.5"), interpretFormula(vi, "ROUND(0.510,1)"));
      assertEquals(new BigDecimal("5.0"), interpretFormula(vi, "ROUND(5.010,2)"));
      assertEquals(new BigDecimal("5"), interpretFormula(vi, "ROUND(5.010,1)"));

      assertEquals(new BigDecimal("0.5100"), interpretFormula(vi, "SCALE(0.510,4)"));
      assertEquals(new BigDecimal("5.01"), interpretFormula(vi, "SCALE(5.010,2)"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QFormulaException
   {
      QMetaDataVariableInterpreter vi = new QMetaDataVariableInterpreter();
      vi.addValueMap("pivot", Map.of("sum.noOfShoes", 5));
      vi.addValueMap("total", Map.of("sum.noOfShoes", 18));

      assertEquals(new BigDecimal("27.78"), interpretFormula(vi, "SCALE(MULTIPLY(100,DIVIDE_SCALE(${pivot.sum.noOfShoes},${total.sum.noOfShoes},6)),2)"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testComparisons() throws QFormulaException
   {
      QMetaDataVariableInterpreter vi = new QMetaDataVariableInterpreter();
      vi.addValueMap("input", Map.of("one", 1, "two", 2, "foo", "bar"));

      assertTrue((Boolean) interpretFormula(vi, "LT(${input.one},${input.two})"));
      assertFalse((Boolean) interpretFormula(vi, "LT(${input.two},${input.one})"));

      assertFalse((Boolean) interpretFormula(vi, "GT(${input.one},${input.two})"));
      assertTrue((Boolean) interpretFormula(vi, "GT(${input.two},${input.one})"));

      assertTrue((Boolean) interpretFormula(vi, "LTE(${input.one},${input.two})"));
      assertTrue((Boolean) interpretFormula(vi, "LTE(${input.one},${input.one})"));
      assertFalse((Boolean) interpretFormula(vi, "LTE(${input.two},${input.one})"));

      assertFalse((Boolean) interpretFormula(vi, "GTE(${input.one},${input.two})"));
      assertTrue((Boolean) interpretFormula(vi, "GTE(${input.one},${input.one})"));
      assertTrue((Boolean) interpretFormula(vi, "GTE(${input.two},${input.one})"));

      // todo - google sheets compares strings differently...
      assertThatThrownBy(() -> interpretFormula(vi, "LT(${input.foo},${input.one})")).hasRootCauseMessage("Could not process [bar] as a number");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testConditionals() throws QFormulaException
   {
      QMetaDataVariableInterpreter vi = new QMetaDataVariableInterpreter();
      vi.addValueMap("input", Map.of("one", 1, "two", 2, "three", 3, "foo", "bar"));

      assertEquals("A", interpretFormula(vi, "IF(LT(${input.one},${input.two}),A,B)"));
      assertEquals("B", interpretFormula(vi, "IF(GT(${input.one},${input.two}),A,B)"));

      assertEquals("C", interpretFormula(vi, "IF(GT(${input.one},${input.two}),A,IF(GT(${input.two},${input.three}),B,C))"));
      assertEquals("B", interpretFormula(vi, "IF(GT(${input.one},${input.two}),A,IF(LT(${input.two},${input.three}),B,C))"));
      assertEquals("A", interpretFormula(vi, "IF(GT(${input.two},${input.one}),A,IF(LT(${input.two},${input.three}),B,C))"));

      assertEquals("Yes", interpretFormula(vi, "IF(GT(${input.one},0),Yes,No)"));
      assertEquals("No", interpretFormula(vi, "IF(LT(${input.one},0),Yes,No)"));
      assertEquals("Yes", interpretFormula(vi, "IF(true,Yes,No)"));
      assertEquals("Yes", interpretFormula(vi, "IF(True,Yes,No)"));
      assertEquals("No", interpretFormula(vi, "IF(false,Yes,No)"));
      assertEquals("No", interpretFormula(vi, "IF(False,Yes,No)"));

      assertThatThrownBy(() -> interpretFormula(vi, "IF(foo,Yes,No)")).hasRootCauseMessage("Could not evaluate string 'foo' as a boolean.");
   }

}