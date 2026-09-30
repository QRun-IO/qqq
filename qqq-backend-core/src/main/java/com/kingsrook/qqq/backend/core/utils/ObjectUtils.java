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

package com.kingsrook.qqq.backend.core.utils;


import java.util.function.Consumer;
import java.util.function.Predicate;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeConsumer;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;


/*******************************************************************************
 **
 *******************************************************************************/
public class ObjectUtils
{

   /*******************************************************************************
    ** A varargs version of Objects.requireNonNullElse
    *******************************************************************************/
   @SafeVarargs
   public static <T> T requireNonNullElse(T... objects)
   {
      if(objects == null)
      {
         throw (new NullPointerException("null objects array as input"));
      }

      for(T object : objects)
      {
         if(object != null)
         {
            return (object);
         }
      }

      throw (new NullPointerException("all null objects"));
   }



   /*******************************************************************************
    ** Like Objects.requireNonNullElse, only use an (unsafe) supplier as the first
    ** arg, and only if it throws, return the 2nd arg
    *******************************************************************************/
   public static <T> T tryElse(UnsafeSupplier<T, ?> supplier, T defaultIfThrew)
   {
      try
      {
         return (supplier.get());
      }
      catch(Exception e)
      {
         return (defaultIfThrew);
      }
   }



   /*******************************************************************************
    ** Like Objects.requireNonNullElse, only use an (unsafe) supplier as the first
    ** arg, and if it throws or returns null, then return the 2nd arg
    *******************************************************************************/
   public static <T> T tryAndRequireNonNullElse(UnsafeSupplier<T, ?> supplier, T defaultIfThrew)
   {
      try
      {
         T t = supplier.get();
         if(t != null)
         {
            return (t);
         }
      }
      catch(Exception e)
      {
         //////////
         // noop //
         //////////
      }

      return (defaultIfThrew);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <T> void ifNotNull(T object, Consumer<T> consumer)
   {
      if(object != null)
      {
         consumer.accept(object);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <T, E extends Exception> void ifNotNullUnsafe(T object, UnsafeConsumer<T, E> consumer) throws E
   {
      if(object != null)
      {
         consumer.run(object);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <T> T requireConditionElse(T a, Predicate<T> condition, T b)
   {
      if(condition.test(a))
      {
         return (a);
      }
      return (b);
   }



   /*******************************************************************************
    ** Utility to test a chained unsafe expression CAN get to the end and return true.
    **
    ** e.g., instead of:
    **   if(a && a.b && a.b.c && a.b.c.d)
    ** we can do:
    **   if(ifCan(() -> a.b.c.d))
    **
    ** Note - if the supplier returns null, that counts as false!
    *******************************************************************************/
   public static boolean ifCan(UnsafeSupplier<Boolean, ? extends Throwable> supplier)
   {
      try
      {
         return supplier.get();
      }
      catch(Throwable t)
      {
         return (false);
      }
   }

}
