/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.util.LinkedList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for FluentList 
 *******************************************************************************/
class FluentListTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      assertEquals(ListBuilder.of("A", "B", "C", null, "D", "E", "F"), new FluentList<String>()
         .with("A")
         .with("B", "C")
         .with()
         .with((String) null)
         .with((List<String>) null)
         .with(List.of("D", "E"))
         .with(List.of("F")));

      assertEquals(List.of(1, 2, 3, 4), new FluentList<>(List.of(1))
         .with(2)
         .with(List.of(3, 4)));

      assertEquals(List.of(true, true, false), new FluentList<>(new LinkedList<Boolean>())
         .with(true)
         .with(List.of(true, false)));
   }

}