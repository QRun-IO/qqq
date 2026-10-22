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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.utils.lambdas.VoidVoidMethod;


/*******************************************************************************
 ** Object to wrap a List, so that in case a caller provided an immutable List,
 ** you can safely perform mutating operations on it (in which case, it'll get
 ** replaced by an actual mutable list).
 *******************************************************************************/
public class MutableList<T> implements List<T>
{
   private List<T>           sourceList;
   private Supplier<List<T>> supplierIfNeeded;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public MutableList(List<T> sourceList)
   {
      this(sourceList, ArrayList::new);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public MutableList(List<T> sourceList, Supplier<List<T>> supplierIfNeeded)
   {
      this.sourceList = Objects.requireNonNullElseGet(sourceList, supplierIfNeeded);
      this.supplierIfNeeded = supplierIfNeeded;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   List<T> getUnderlyingList()
   {
      return (sourceList);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void replaceSourceListWithMutableCopy()
   {
      try
      {
         List<T> replacementList = supplierIfNeeded.get();
         replacementList.addAll(sourceList);
         sourceList = replacementList;
      }
      catch(Exception e)
      {
         throw (new IllegalStateException("Error getting from the supplier provided for this MutableList.", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private <V> V doMutableOperationForValue(Supplier<V> supplier)
   {
      try
      {
         return (supplier.get());
      }
      catch(UnsupportedOperationException uoe)
      {
         replaceSourceListWithMutableCopy();
         return (supplier.get());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void doMutableOperationForVoid(VoidVoidMethod method)
   {
      try
      {
         method.run();
      }
      catch(UnsupportedOperationException uoe)
      {
         replaceSourceListWithMutableCopy();
         method.run();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int size()
   {
      return (sourceList.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isEmpty()
   {
      return (sourceList.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean contains(Object o)
   {
      return (sourceList.contains(o));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Iterator<T> iterator()
   {
      return (sourceList.iterator());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Object[] toArray()
   {
      return (sourceList.toArray());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public <T1> T1[] toArray(T1[] a)
   {
      return (sourceList.toArray(a));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean add(T t)
   {
      return (doMutableOperationForValue(() -> sourceList.add(t)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean remove(Object o)
   {
      return (doMutableOperationForValue(() -> sourceList.remove(o)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean containsAll(Collection<?> c)
   {
      return (sourceList.containsAll(c));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean addAll(Collection<? extends T> c)
   {
      return (doMutableOperationForValue(() -> sourceList.addAll(c)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean addAll(int index, Collection<? extends T> c)
   {
      return (doMutableOperationForValue(() -> sourceList.addAll(index, c)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean removeAll(Collection<?> c)
   {
      return (doMutableOperationForValue(() -> sourceList.removeAll(c)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean retainAll(Collection<?> c)
   {
      return (doMutableOperationForValue(() -> sourceList.retainAll(c)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void clear()
   {
      doMutableOperationForVoid(() -> sourceList.clear());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public T get(int index)
   {
      return (sourceList.get(index));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public T set(int index, T element)
   {
      return (doMutableOperationForValue(() -> sourceList.set(index, element)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void add(int index, T element)
   {
      doMutableOperationForVoid(() -> sourceList.add(index, element));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public T remove(int index)
   {
      return (doMutableOperationForValue(() -> sourceList.remove(index)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int indexOf(Object o)
   {
      return (sourceList.indexOf(o));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int lastIndexOf(Object o)
   {
      return (sourceList.lastIndexOf(o));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ListIterator<T> listIterator()
   {
      return (sourceList.listIterator());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ListIterator<T> listIterator(int index)
   {
      return (sourceList.listIterator(index));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<T> subList(int fromIndex, int toIndex)
   {
      return (sourceList.subList(fromIndex, toIndex));
   }
}
