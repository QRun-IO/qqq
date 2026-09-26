/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for APIVersionRange
 *******************************************************************************/
class APIVersionRangeTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      assertTrue(APIVersionRange.beforeAndIncluding("2023.Q2").includes(new APIVersion("2023.Q1")));
      assertTrue(APIVersionRange.beforeAndIncluding("2023.Q2").includes(new APIVersion("2023.Q2")));
      assertFalse(APIVersionRange.beforeAndIncluding("2023.Q2").includes(new APIVersion("2023.Q3")));

      assertFalse(APIVersionRange.afterAndIncluding("2023.Q2").includes(new APIVersion("2023.Q1")));
      assertTrue(APIVersionRange.afterAndIncluding("2023.Q2").includes(new APIVersion("2023.Q2")));
      assertTrue(APIVersionRange.afterAndIncluding("2023.Q2").includes(new APIVersion("2023.Q3")));

      assertTrue(APIVersionRange.beforeButExcluding("2023.Q2").includes(new APIVersion("2023.Q1")));
      assertFalse(APIVersionRange.beforeButExcluding("2023.Q2").includes(new APIVersion("2023.Q2")));
      assertFalse(APIVersionRange.beforeButExcluding("2023.Q2").includes(new APIVersion("2023.Q3")));

      assertFalse(APIVersionRange.afterButExcluding("2023.Q2").includes(new APIVersion("2023.Q1")));
      assertFalse(APIVersionRange.afterButExcluding("2023.Q2").includes(new APIVersion("2023.Q2")));
      assertTrue(APIVersionRange.afterButExcluding("2023.Q2").includes(new APIVersion("2023.Q3")));

      assertFalse(APIVersionRange.betweenAndIncluding("2023.Q2", "2023.Q4").includes(new APIVersion("2023.Q1")));
      assertTrue(APIVersionRange.betweenAndIncluding("2023.Q2", "2023.Q4").includes(new APIVersion("2023.Q2")));
      assertTrue(APIVersionRange.betweenAndIncluding("2023.Q2", "2023.Q4").includes(new APIVersion("2023.Q3")));
      assertTrue(APIVersionRange.betweenAndIncluding("2023.Q2", "2023.Q4").includes(new APIVersion("2023.Q4")));
      assertFalse(APIVersionRange.betweenAndIncluding("2023.Q2", "2023.Q4").includes(new APIVersion("2024.Q1")));

      assertFalse(APIVersionRange.betweenButExcluding("2023.Q2", "2023.Q4").includes(new APIVersion("2023.Q1")));
      assertFalse(APIVersionRange.betweenButExcluding("2023.Q2", "2023.Q4").includes(new APIVersion("2023.Q2")));
      assertTrue(APIVersionRange.betweenButExcluding("2023.Q2", "2023.Q4").includes(new APIVersion("2023.Q3")));
      assertFalse(APIVersionRange.betweenButExcluding("2023.Q2", "2023.Q4").includes(new APIVersion("2023.Q4")));
      assertFalse(APIVersionRange.betweenButExcluding("2023.Q2", "2023.Q4").includes(new APIVersion("2024.Q1")));
   }
}