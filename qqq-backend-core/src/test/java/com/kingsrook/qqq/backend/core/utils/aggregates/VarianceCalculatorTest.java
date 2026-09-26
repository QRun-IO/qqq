/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils.aggregates;


import java.math.BigDecimal;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for VarianceCalculator (Welford's online algorithm).
 **
 ** Reference: https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm
 *******************************************************************************/
class VarianceCalculatorTest
{

   private static final Offset<Double> TOLERANCE = Offset.offset(0.0001);


   /*******************************************************************************
    ** With zero inputs, all statistics should be null.
    *******************************************************************************/
   @Test
   void testGetVariance_noInputs_returnsNull()
   {
      VarianceCalculator calc = new VarianceCalculator();

      assertNull(calc.getVariance());
      assertNull(calc.getVarP());
      assertNull(calc.getStandardDeviation());
      assertNull(calc.getStdDevP());
   }



   /*******************************************************************************
    ** With a single input, sample variance is undefined — must return null.
    *******************************************************************************/
   @Test
   void testGetVariance_singleInput_returnsNull()
   {
      VarianceCalculator calc = new VarianceCalculator();
      calc.updateVariance(new BigDecimal("42"));

      assertNull(calc.getVariance());
      assertNull(calc.getVarP());
      assertNull(calc.getStandardDeviation());
      assertNull(calc.getStdDevP());
   }



   /*******************************************************************************
    ** Identical values should yield variance = 0.
    *******************************************************************************/
   @Test
   void testGetVariance_identicalValues_returnsZero()
   {
      VarianceCalculator calc = new VarianceCalculator();
      calc.updateVariance(new BigDecimal("10"));
      calc.updateVariance(new BigDecimal("10"));
      calc.updateVariance(new BigDecimal("10"));

      assertThat(calc.getVariance().doubleValue()).isCloseTo(0.0, TOLERANCE);
      assertThat(calc.getVarP().doubleValue()).isCloseTo(0.0, TOLERANCE);
      assertThat(calc.getStandardDeviation().doubleValue()).isCloseTo(0.0, TOLERANCE);
      assertThat(calc.getStdDevP().doubleValue()).isCloseTo(0.0, TOLERANCE);
   }



   /*******************************************************************************
    ** Two values — sample variance = ((x1-mean)^2 + (x2-mean)^2) / (n-1).
    ** For values [2, 4]: mean=3, sample var=((2-3)^2+(4-3)^2)/1 = 2.0
    *******************************************************************************/
   @Test
   void testGetVariance_twoValues_correctSampleVariance()
   {
      VarianceCalculator calc = new VarianceCalculator();
      calc.updateVariance(new BigDecimal("2"));
      calc.updateVariance(new BigDecimal("4"));

      assertThat(calc.getVariance().doubleValue()).isCloseTo(2.0, TOLERANCE);
   }



   /*******************************************************************************
    ** Population variance divides by n, not n-1.
    ** For values [2, 4]: mean=3, pop var=((2-3)^2+(4-3)^2)/2 = 1.0
    *******************************************************************************/
   @Test
   void testGetVarP_twoValues_correctPopulationVariance()
   {
      VarianceCalculator calc = new VarianceCalculator();
      calc.updateVariance(new BigDecimal("2"));
      calc.updateVariance(new BigDecimal("4"));

      assertThat(calc.getVarP().doubleValue()).isCloseTo(1.0, TOLERANCE);
   }



   /*******************************************************************************
    ** Standard deviation should equal sqrt(variance).
    ** For sample var=2.0, stdDev = sqrt(2) ≈ 1.4142
    *******************************************************************************/
   @Test
   void testGetStandardDeviation_twoValues_equalsSqrtVariance()
   {
      VarianceCalculator calc = new VarianceCalculator();
      calc.updateVariance(new BigDecimal("2"));
      calc.updateVariance(new BigDecimal("4"));

      double expectedStdDev = Math.sqrt(2.0);
      assertThat(calc.getStandardDeviation().doubleValue()).isCloseTo(expectedStdDev, TOLERANCE);
   }



   /*******************************************************************************
    ** Population std dev should equal sqrt(varP).
    ** For pop var=1.0, stdDevP = 1.0
    *******************************************************************************/
   @Test
   void testGetStdDevP_twoValues_equalsOne()
   {
      VarianceCalculator calc = new VarianceCalculator();
      calc.updateVariance(new BigDecimal("2"));
      calc.updateVariance(new BigDecimal("4"));

      assertThat(calc.getStdDevP().doubleValue()).isCloseTo(1.0, TOLERANCE);
   }



   /*******************************************************************************
    ** Larger dataset — Welford's should stay numerically stable.
    ** Values [1, 2, 3, 4, 5]: mean=3, sample var=2.5
    *******************************************************************************/
   @Test
   void testGetVariance_fiveSequentialValues_correctSampleVariance()
   {
      VarianceCalculator calc = new VarianceCalculator();
      for(int i = 1; i <= 5; i++)
      {
         calc.updateVariance(BigDecimal.valueOf(i));
      }

      assertThat(calc.getVariance().doubleValue()).isCloseTo(2.5, TOLERANCE);
   }



   /*******************************************************************************
    ** Negative values — algorithm should still produce correct results.
    *******************************************************************************/
   @Test
   void testGetVariance_negativeValues_correctVariance()
   {
      VarianceCalculator calc = new VarianceCalculator();
      calc.updateVariance(new BigDecimal("-2"));
      calc.updateVariance(new BigDecimal("2"));

      // mean=0, sample var = ((-2)^2 + 2^2) / 1 = 8.0
      assertThat(calc.getVariance().doubleValue()).isCloseTo(8.0, TOLERANCE);
   }

}
