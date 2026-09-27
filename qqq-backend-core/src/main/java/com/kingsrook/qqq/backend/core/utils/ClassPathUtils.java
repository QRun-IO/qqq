/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;


/*******************************************************************************
 ** Utilities for reading classes - e.g., finding all in a package
 *******************************************************************************/
@SuppressWarnings("ALL") // the api we're using here, from google, is marked Beta
public class ClassPathUtils
{
   private static ImmutableSet<ClassPath.ClassInfo> topLevelClasses;

   private static final Map<String, Boolean> cache = new ConcurrentHashMap<>();



   /***************************************************************************
    **
    ***************************************************************************/
   public static boolean isClassAvailable(String className)
   {
      return cache.computeIfAbsent(className, c ->
      {
         try
         {
            Class.forName(c, false, ClassPathUtils.class.getClassLoader());
            return true;
         }
         catch(ClassNotFoundException e)
         {
            return false;
         }
      });
   }



   /*******************************************************************************
    ** from https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection
    **
    *******************************************************************************/
   public static List<Class<?>> getClassesInPackage(String packageName) throws IOException
   {
      List<Class<?>> classes = new ArrayList<>();
      ClassLoader    loader  = Thread.currentThread().getContextClassLoader();

      for(ClassPath.ClassInfo info : getTopLevelClasses(loader))
      {
         if(info.getName().startsWith(packageName))
         {
            classes.add(info.load());
         }
      }

      return (classes);
   }



   /*******************************************************************************
    ** from https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection
    **
    *******************************************************************************/
   public static List<Class<?>> getClassesContainingNameAndOfType(String nameContains, Class<?> type) throws IOException
   {
      List<Class<?>> classes = new ArrayList<>();
      ClassLoader    loader  = Thread.currentThread().getContextClassLoader();

      for(ClassPath.ClassInfo info : getTopLevelClasses(loader))
      {
         try
         {
            if(info.getName().contains(nameContains))
            {
               Class<?> testClass = info.load();
               if(type.isAssignableFrom(testClass))
               {
                  classes.add(testClass);
               }
            }
         }
         catch(Throwable t)
         {
            // ignore - comes up for non-class entries, like module-info
         }
      }

      return (classes);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ImmutableSet<ClassPath.ClassInfo> getTopLevelClasses(ClassLoader loader) throws IOException
   {
      if(topLevelClasses == null)
      {
         topLevelClasses = ClassPath.from(loader).getTopLevelClasses();
      }

      return (topLevelClasses);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void clearTopLevelClassCache()
   {
      topLevelClasses = null;
   }

}
