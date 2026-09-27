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

package com.kingsrook.qqq.backend.core.utils.memoization;


import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for Memoization 
 *******************************************************************************/
class MemoizationTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      Memoization<String, Integer> memoization = new Memoization<>();
      memoization.setMaxSize(3);
      memoization.setTimeout(Duration.ofMillis(100));

      assertThat(memoization.getMemoizedResult("one")).isEmpty();
      memoization.storeResult("one", 1);
      assertThat(memoization.getMemoizedResult("one")).isPresent().get().extracting("result").isEqualTo(1);

      ////////////////////////////////////////////////////
      // store 3 more results - this should force 1 out //
      ////////////////////////////////////////////////////
      memoization.storeResult("two", 2);
      memoization.storeResult("three", 3);
      memoization.storeResult("four", 4);
      assertThat(memoization.getMemoizedResult("one")).isEmpty();

      //////////////////////////////////
      // make sure others are present //
      //////////////////////////////////
      assertThat(memoization.getMemoizedResult("two")).isPresent().get().extracting("result").isEqualTo(2);
      assertThat(memoization.getMemoizedResult("three")).isPresent().get().extracting("result").isEqualTo(3);
      assertThat(memoization.getMemoizedResult("four")).isPresent().get().extracting("result").isEqualTo(4);

      /////////////////////////////////////////////////////////////
      // wait more than the timeout, then make sure all are gone //
      /////////////////////////////////////////////////////////////
      SleepUtils.sleep(150, TimeUnit.MILLISECONDS);
      assertThat(memoization.getMemoizedResult("two")).isEmpty();
      assertThat(memoization.getMemoizedResult("three")).isEmpty();
      assertThat(memoization.getMemoizedResult("four")).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCanStoreNull()
   {
      Memoization<String, Integer> memoization = new Memoization<>();
      memoization.storeResult("null", null);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // the memoizedResult should never be null, and should be present if we memoized/stored a null value //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      Optional<MemoizedResult<Integer>> optionalMemoizedResult = memoization.getMemoizedResult("null");
      assertNotNull(optionalMemoizedResult);
      assertTrue(optionalMemoizedResult.isPresent());
      assertNull(optionalMemoizedResult.get().getResult());

      //////////////////////////////////////////////////////////////////////////////
      // make sure getMemoizedResult returns non-null and empty for an un-set key //
      //////////////////////////////////////////////////////////////////////////////
      optionalMemoizedResult = memoization.getMemoizedResult("never-stored");
      assertNotNull(optionalMemoizedResult);
      assertTrue(optionalMemoizedResult.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMayNotStoreNull()
   {
      Memoization<String, String> memoization = new Memoization<>();
      memoization.setMayStoreNullValues(false);

      AtomicInteger callCounter = new AtomicInteger();
      callCounter.set(0);
      UnsafeFunction<String, String, Exception> supplier = name ->
      {
         callCounter.getAndIncrement();
         if(name.equals("throw"))
         {
            throw (new Exception("You asked me to throw"));
         }
         else if(name.equals("null"))
         {
            return (null);
         }
         else
         {
            return (name);
         }
      };

      assertThat(memoization.getResult("null", supplier)).isEmpty();
      assertEquals(1, callCounter.get());

      assertThat(memoization.getResult("null", supplier)).isEmpty();
      assertEquals(2, callCounter.get()); // should re-run the supplier, incrementing the counter

      assertThat(memoization.getResult("throw", supplier)).isEmpty();
      assertEquals(3, callCounter.get());

      assertThat(memoization.getResult("throw", supplier)).isEmpty();
      assertEquals(4, callCounter.get()); // should re-run the supplier, incrementing the counter

      //noinspection AssertBetweenInconvertibleTypes
      assertThat(memoization.getResult("foo", supplier)).isPresent().get().isEqualTo("foo");
      assertEquals(5, callCounter.get());

      //noinspection AssertBetweenInconvertibleTypes
      assertThat(memoization.getResult("foo", supplier)).isPresent().get().isEqualTo("foo");
      assertEquals(5, callCounter.get()); // should NOT re-run the supplier, NOT incrementing the counter
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLookupFunction()
   {
      AtomicInteger lookupFunctionCallCounter = new AtomicInteger(0);

      Memoization<String, Integer> memoization = new Memoization<>();

      UnsafeFunction<String, Integer, Exception> lookupFunction = numberString ->
      {
         lookupFunctionCallCounter.getAndIncrement();

         if(numberString.equals("null"))
         {
            return (null);
         }

         return Integer.parseInt(numberString);
      };

      //////////////////////////////////////////////////////////////////////////////////////////
      // get "1" twice - should return 1 each time, and call the lookup function exactly once //
      //////////////////////////////////////////////////////////////////////////////////////////
      assertThat(memoization.getResult("1", lookupFunction)).isPresent().contains(1);
      assertEquals(1, lookupFunctionCallCounter.get());

      assertThat(memoization.getResult("1", lookupFunction)).isPresent().contains(1);
      assertEquals(1, lookupFunctionCallCounter.get());

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now get "null" twice - should return null each time, and call the lookup function exactly once more //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(memoization.getResult("null", lookupFunction)).isEmpty();
      assertEquals(2, lookupFunctionCallCounter.get());

      assertThat(memoization.getResult("null", lookupFunction)).isEmpty();
      assertEquals(2, lookupFunctionCallCounter.get());

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now make a call that throws twice - again, should return null each time, and only do one more loookup call //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(memoization.getResult(null, lookupFunction)).isEmpty();
      assertEquals(3, lookupFunctionCallCounter.get());

      assertThat(memoization.getResult(null, lookupFunction)).isEmpty();
      assertEquals(3, lookupFunctionCallCounter.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetResultThrowing() throws Exception
   {
      Memoization<String, Integer> memoization = new Memoization<>();

      UnsafeFunction<String, Integer, Exception> lookupFunction = Integer::parseInt;

      assertEquals(Optional.of(1), memoization.getResultThrowing("1", lookupFunction));
      assertThatThrownBy(() -> memoization.getResultThrowing(null, lookupFunction)).hasMessageContaining("null");
      assertThatThrownBy(() -> memoization.getResultThrowing("two", lookupFunction)).hasMessageContaining("For input string:");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @Disabled("Slow, so not for CI - but good to demonstrate thread-safety during dev")
   void testMultiThread() throws InterruptedException, ExecutionException
   {
      Memoization<String, Integer> memoization     = new Memoization<>();
      ExecutorService              executorService = Executors.newFixedThreadPool(20);

      List<Future<?>> futures = new ArrayList<>();

      for(int i = 0; i < 20; i++)
      {
         int finalI = i;
         futures.add(executorService.submit(() ->
         {
            System.out.println("Start " + finalI);
            for(int n = 0; n < 1_000_000; n++)
            {
               memoization.storeResult(String.valueOf(n), n);
               memoization.getMemoizedResult(String.valueOf(n));

               if(n % 100_000 == 0)
               {
                  System.out.format("Thread %d at %,d\n", finalI, +n);
               }
            }
            System.out.println("End " + finalI);
         }));
      }

      while(!futures.isEmpty())
      {
         Iterator<Future<?>> iterator = futures.iterator();
         while(iterator.hasNext())
         {
            Future<?> next = iterator.next();
            if(next.isDone())
            {
               Object o = next.get();
               iterator.remove();
            }
         }
      }

      System.out.println("All Done");
   }

}