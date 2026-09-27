/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.util.Objects;


/*******************************************************************************
 ** Simple container for two objects
 *******************************************************************************/
public class Pair<A, B> implements Cloneable
{
   private A a;
   private B b;



   /*******************************************************************************
    **
    *******************************************************************************/
   public Pair(A a, B b)
   {
      this.a = a;
      this.b = b;
   }



   /*******************************************************************************
    ** static constructor (factory)
    *******************************************************************************/
   public static <A, B> Pair<A, B> of(A a, B b)
   {
      return (new Pair<>(a, b));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (a + ":" + b);
   }



   /*******************************************************************************
    ** Getter for a
    **
    *******************************************************************************/
   public A getA()
   {
      return a;
   }



   /*******************************************************************************
    ** Getter for b
    **
    *******************************************************************************/
   public B getB()
   {
      return b;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return true;
      }
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      Pair<?, ?> pair = (Pair<?, ?>) o;
      return Objects.equals(a, pair.a) && Objects.equals(b, pair.b);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(a, b);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   @Override
   public Pair<A, B> clone()
   {
      try
      {
         return (Pair<A, B>) super.clone();
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
