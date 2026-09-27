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

package com.kingsrook.qqq.backend.core.utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import org.assertj.core.api.AbstractAssert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** assertions against collections
 *******************************************************************************/
public class CollectionAssert<A> extends AbstractAssert<CollectionAssert<A>, Collection<A>>
{
   /***************************************************************************
    **
    ***************************************************************************/
   protected CollectionAssert(Collection<A> actual, Class<?> selfType)
   {
      super(actual, selfType);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static <A> CollectionAssert<A> assertThat(Collection<A> actualCollection)
   {
      return (new CollectionAssert<>(actualCollection, CollectionAssert.class));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public <E> CollectionAssert<A> matchesAllowingExpectedToHaveMore(Collection<E> expected, BiFunction<E, A, Boolean> predicate)
   {
      if(actual == null && expected != null)
      {
         fail("Actual collection was null, but expected collection was not-null");
         return (this);
      }
      else if(actual != null && expected == null)
      {
         fail("Actual collection was not null, but expected collection was null");
         return (this);
      }
      else if(actual == null && expected == null)
      {
         return (this);
      }

      assertTrue(actual.size() >= expected.size(), "Actual collection size [" + actual.size() + "] should be >= expected collection size [" + expected.size() + "]");

      matchElements(expected, predicate);

      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private <E> void matchElements(Collection<E> expected, BiFunction<E, A, Boolean> predicate)
   {
      List<Integer> nonMatchingExpectedIndexes = new ArrayList<>();
      Set<Integer>  matchedActualIndexes       = new HashSet<>();

      List<E> expectedList = new ArrayList<>(expected);
      List<A> actualList   = new ArrayList<>(actual);

      for(int eIndex = 0; eIndex < expectedList.size(); eIndex++)
      {
         E e = expectedList.get(eIndex);

         boolean matchedThieE = false;

         for(int aIndex = 0; aIndex < actualList.size(); aIndex++)
         {
            A a = actualList.get(aIndex);
            if(!matchedThieE && !matchedActualIndexes.contains(aIndex)) // don't re-check an already-matched item
            {
               if(predicate.apply(e, a))
               {
                  matchedActualIndexes.add(aIndex);
                  matchedThieE = true;
               }
            }
         }

         if(!matchedThieE)
         {
            nonMatchingExpectedIndexes.add(eIndex);
         }
      }

      assertTrue(nonMatchingExpectedIndexes.isEmpty(), "Did not find a match for indexes " + nonMatchingExpectedIndexes + "\n from expected collection: " + expected + "\n in actual collection: " + actual);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public <E> CollectionAssert<A> matchesAll(Collection<E> expected, BiFunction<E, A, Boolean> predicate)
   {
      if(actual == null && expected != null)
      {
         fail("Actual collection was null, but expected collection was not-null");
         return (this);
      }
      else if(actual != null && expected == null)
      {
         fail("Actual collection was not null, but expected collection was null");
         return (this);
      }
      else if(actual == null && expected == null)
      {
         return (this);
      }

      assertEquals(expected.size(), actual.size(), "Expected size of collections");

      matchElements(expected, predicate);

      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public CollectionAssert<A> isNullOrEmpty()
   {
      if(actual != null)
      {
         assertEquals(0, actual.size(), "Expected collection to be null or empty");
      }
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public CollectionAssert<A> isEmpty()
   {
      assertEquals(0, actual.size(), "Expected collection to be empty");
      return (this);
   }

}
