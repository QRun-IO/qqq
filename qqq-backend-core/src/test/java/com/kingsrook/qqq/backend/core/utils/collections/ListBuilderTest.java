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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ListBuilder
 *******************************************************************************/
class ListBuilderTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSameAsListOf()
   {
      assertEquals(List.of(1), ListBuilder.of(1));
      assertEquals(List.of(1, 2), ListBuilder.of(1, 2));
      assertEquals(List.of(1, 2, 3), ListBuilder.of(1, 2, 3));
      assertEquals(List.of(1, 2, 3, 4), ListBuilder.of(1, 2, 3, 4));
      assertEquals(List.of(1, 2, 3, 4, 5), ListBuilder.of(1, 2, 3, 4, 5));
      assertEquals(List.of(1, 2, 3, 4, 5, 6), ListBuilder.of(1, 2, 3, 4, 5, 6));
      assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), ListBuilder.of(1, 2, 3, 4, 5, 6, 7));
      assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), ListBuilder.of(1, 2, 3, 4, 5, 6, 7, 8));
      assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), ListBuilder.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
      assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), ListBuilder.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBetterThanListMapOf()
   {
      ///////////////////////////////
      // assert this doesn't throw //
      ///////////////////////////////
      List<Integer> list = ListBuilder.of(1, null, 3);

      ///////////////////////////////////////
      // this too, doesn't freaking throw. //
      ///////////////////////////////////////
      list.add(4);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBuilderMode()
   {
      List<String> builtList = new ListBuilder<String>().with("A").with("B").build();
      assertEquals(List.of("A", "B"), builtList);
      assertEquals(ArrayList.class, builtList.getClass());

      List<String> builtLinkedList = new ListBuilder<String>(new LinkedList<>()).with("A").with("B").build();
      assertEquals(List.of("A", "B"), builtLinkedList);
      assertEquals(LinkedList.class, builtLinkedList.getClass());
   }

}