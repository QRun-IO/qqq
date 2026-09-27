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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeVoidVoidMethod;


/*******************************************************************************
 ** Utility class for working with exceptions.
 **
 *******************************************************************************/
public class ExceptionUtils
{

   /*******************************************************************************
    ** Find a specific exception class in an exception's caused-by chain.  Returns
    ** null if not found.  Be aware, uses class.isInstance (so sub-classes get found).
    **
    *******************************************************************************/
   public static <T extends Throwable> T findClassInRootChain(Throwable e, Class<T> targetClass)
   {
      if(e == null)
      {
         return (null);
      }

      if(targetClass.isInstance(e))
      {
         return targetClass.cast(e);
      }

      if(e.getCause() == null)
      {
         return (null);
      }

      return findClassInRootChain(e.getCause(), targetClass);
   }



   /*******************************************************************************
    ** Find a list of exceptions of the given class in an exception's caused-by chain.
    ** Returns empty list if none found.
    **
    *******************************************************************************/
   public static <T extends Throwable> List<T> getClassListFromRootChain(Throwable e, Class<T> targetClass)
   {
      List<T> throwableList = new ArrayList<>();
      if(targetClass.isInstance(e))
      {
         throwableList.add(targetClass.cast(e));
      }

      ///////////////////////////////////////////////////
      // iterate through the chain with a limit of 100 //
      ///////////////////////////////////////////////////
      int counter = 0;
      while(counter++ < 100)
      {
         ////////////////////////////////////////////////////////////////////////
         // look for the same class from the last throwable found of that type //
         ////////////////////////////////////////////////////////////////////////
         e = findClassInRootChain(e.getCause(), targetClass);
         if(e == null)
         {
            break;
         }

         ////////////////////////////////////////////////////////////////////////
         // if we did not break, higher one must have been found, keep looking //
         ////////////////////////////////////////////////////////////////////////
         throwableList.add(targetClass.cast(e));
      }

      return (throwableList);
   }



   /*******************************************************************************
    ** Get the root exception in a caused-by-chain.
    **
    *******************************************************************************/
   public static Throwable getRootException(Exception exception)
   {
      if(exception == null)
      {
         return (null);
      }

      Throwable      root = exception;
      Set<Throwable> seen = new HashSet<>();
      while(root.getCause() != null)
      {
         if(seen.contains(root))
         {
            //////////////////////////
            // avoid infinite loops //
            //////////////////////////
            break;
         }
         seen.add(root);
         root = root.getCause();
      }

      return (root);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String concatenateMessagesFromChain(Exception exception)
   {
      if(exception == null)
      {
         return (null);
      }

      List<String>   messages = new ArrayList<>();
      Throwable      root     = exception;
      Set<Throwable> seen     = new HashSet<>();

      do
      {
         if(StringUtils.hasContent(root.getMessage()))
         {
            messages.add(root.getMessage());
         }
         else
         {
            messages.add(root.getClass().getSimpleName());
         }

         seen.add(root);
         root = root.getCause();
      }
      while(root != null && !seen.contains(root));

      return (StringUtils.join("; ", messages));
   }



   /*******************************************************************************
    ** Get the messages from the top & bottoms (root) of an exception.
    **
    ** If there's no root, just return the top (e.g., parameter)'s message.
    ** If they are both found, put ": " between them.
    *******************************************************************************/
   public static String getTopAndBottomMessages(Exception e)
   {
      String    rs            = e.getMessage();
      Throwable rootException = getRootException(e);
      if(rootException != e)
      {
         rs += ": " + rootException.getMessage();
      }
      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void tryIgnore(UnsafeVoidVoidMethod<?> method)
   {
      try
      {
         method.run();
      }
      catch(Throwable t)
      {
         ///////////////////////////
         // ignore, as name says! //
         ///////////////////////////
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void tryCatch(UnsafeVoidVoidMethod<?> method, Consumer<Throwable> catcher)
   {
      try
      {
         method.run();
      }
      catch(Throwable t)
      {
         catcher.accept(t);
      }
   }

}
