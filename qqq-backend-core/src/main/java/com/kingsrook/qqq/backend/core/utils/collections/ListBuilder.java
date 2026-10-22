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
import java.util.List;


/*******************************************************************************
 ** List.of is "great", but annoying because it makes unmodifiable lists...
 ** So, replace it with this, which returns ArrayLists, which "don't suck"
 **
 ** Can use it 3 ways:
 ** ListBuilder.of(value, value2, ...) => List (an ArrayList)
 ** ListBuilder.<ElementType>of(SomeList::new).with(value).with(value2)...build() => SomeList (the type you specify)
 ** new ListBuilder.<ElementType>.with(value).with(value2)...build() => List (an ArrayList - for when you have more than 10 values...)
 *******************************************************************************/
public class ListBuilder<E>
{
   private List<E> list;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ListBuilder()
   {
      this.list = new ArrayList<>();
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ListBuilder(List<E> list)
   {
      this.list = list;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ListBuilder<E> with(E value)
   {
      list.add(value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<E> build()
   {
      return (this.list);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2, E e3)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      rs.add(e3);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2, E e3, E e4)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      rs.add(e3);
      rs.add(e4);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      rs.add(e3);
      rs.add(e4);
      rs.add(e5);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      rs.add(e3);
      rs.add(e4);
      rs.add(e5);
      rs.add(e6);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      rs.add(e3);
      rs.add(e4);
      rs.add(e5);
      rs.add(e6);
      rs.add(e7);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      rs.add(e3);
      rs.add(e4);
      rs.add(e5);
      rs.add(e6);
      rs.add(e7);
      rs.add(e8);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      rs.add(e3);
      rs.add(e4);
      rs.add(e5);
      rs.add(e6);
      rs.add(e7);
      rs.add(e8);
      rs.add(e9);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10)
   {
      List<E> rs = new ArrayList<>();
      rs.add(e1);
      rs.add(e2);
      rs.add(e3);
      rs.add(e4);
      rs.add(e5);
      rs.add(e6);
      rs.add(e7);
      rs.add(e8);
      rs.add(e9);
      rs.add(e10);
      return (rs);
   }

}
