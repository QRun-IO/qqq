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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for AlphaNumericComparator 
 *******************************************************************************/
class AlphaNumericComparatorTest extends BaseTest
{

   /*******************************************************************************
    ** test odd-balls
    **
    *******************************************************************************/
   @Test
   public void testFringeCases()
   {
      test(sort("", null, "foo", " ", "", "1", null),
         null, null, "", "", "1", " ", "foo");
   }



   /*******************************************************************************
    ** test alpha-strings only
    **
    *******************************************************************************/
   @Test
   public void testAlphasOnly()
   {
      test(sort("F", "G", "A", "AB", "BB", "BA", "BD"),
         "A", "AB", "BA", "BB", "BD", "F", "G");
   }



   /*******************************************************************************
    ** test numbers only
    **
    *******************************************************************************/
   @Test
   public void testNumbersOnly()
   {
      test(sort("1", "273", "271", "102", "101", "10", "13", "2", "22", "273"),
         "1", "2", "10", "13", "22", "101", "102", "271", "273", "273");
   }



   /*******************************************************************************
    ** test mixed
    **
    *******************************************************************************/
   @Test
   public void testMixed1()
   {
      test(sort("1", "A", "A1", "1A", "10", "10AA", "11", "A11", "11B", "1B", "A10B2", "A10B10", "D1", "D10", "D2", "F20G11H10", "F3", "F20G11H2", "A1", "A10", "A2", "01", "001"),
         "001", "01", "1", "1A", "1B", "10", "10AA", "11", "11B", "A", "A1", "A1", "A2", "A10", "A10B2", "A10B10", "A11", "D1", "D2", "D10", "F3", "F20G11H2", "F20G11H10");
   }



   /*******************************************************************************
    ** test mixed
    **
    *******************************************************************************/
   @Test
   public void testMixed2()
   {
      test(sort("A", "A001", "A1", "A0000", "A00001", "000023", "023", "000023", "023A", "23", "2", "0002", "02"),
         "0002", "02", "2", "000023", "000023", "023", "23", "023A", "A", "A0000", "A00001", "A001", "A1");
   }



   /*******************************************************************************
    **
    **
    *******************************************************************************/
   private void test(List<String> a, String... b)
   {
      System.out.println("Expecting: " + Arrays.asList(b));

      assertEquals(a.size(), b.length);

      for(int i = 0; i < a.size(); i++)
      {
         String aString = a.get(i);
         String bString = b[i];

         assertEquals(aString, bString);
      }
   }



   /*******************************************************************************
    **
    **
    *******************************************************************************/
   private List<String> sort(String... input)
   {
      List<String> inputList = Arrays.asList(input);
      System.out.println("Sorting:   " + inputList);

      try
      {
         List<String> naturalSortList = Arrays.asList(input);
         Collections.sort(naturalSortList);
         System.out.println("Natural:   " + naturalSortList);
      }
      catch(Exception e)
      {
         System.out.println("Natural:   FAILED");
      }

      inputList.sort(new AlphaNumericComparator());
      System.out.println("Produced:  " + inputList);
      return (inputList);
   }
}