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


import java.util.LinkedList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.core.utils.collections.MutableList
 *******************************************************************************/
class MutableListTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      List<Integer> list = new MutableList<>(List.of(1));
      list.add(2);
      list.clear();

      list = new MutableList<>(List.of(3));
      list.add(0, 4);
      list.remove(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNullInput()
   {
      List<Integer> list = new MutableList<>(null);
      list.add(1);
      assertEquals(1, list.size());

      MutableList<Integer> mutableList = new MutableList<>(null, LinkedList::new);
      mutableList.add(1);
      assertEquals(1, mutableList.size());
      assertEquals(LinkedList.class, mutableList.getUnderlyingList().getClass());
   }

}