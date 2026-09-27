/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for SortedPair 
 *******************************************************************************/
class SortedPairTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      assertEquals(1, new SortedPair<>(1, 2).getA());
      assertEquals(2, new SortedPair<>(1, 2).getB());

      assertEquals(1, new SortedPair<>(2, 1).getA());
      assertEquals(2, new SortedPair<>(2, 1).getB());

      assertEquals(3, new SortedPair<>(3, 3).getA());
      assertEquals(3, new SortedPair<>(3, 3).getB());

      assertNull(new SortedPair<>(null, null).getA());
      assertNull(new SortedPair<>(null, null).getB());

      ///////////////////////////////////
      // null compares before non-null //
      ///////////////////////////////////
      assertNull(new SortedPair<>(null, 1).getA());
      assertEquals(1, new SortedPair<>(null, 1).getB());
      assertNull(new SortedPair<>(1, null).getA());
      assertEquals(1, new SortedPair<>(1, null).getB());
   }

}