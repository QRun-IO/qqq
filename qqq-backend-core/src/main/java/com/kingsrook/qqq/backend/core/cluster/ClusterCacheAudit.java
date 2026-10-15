/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.cluster;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 ** Test helper: finds static Memoization fields that did not register with
 ** ClusterCaches (shared or node-local), so a new static cache cannot silently
 ** go stale on other nodes. In a test:
 ** <pre>assertEquals(List.of(), ClusterCacheAudit.unregisteredStaticCaches("com.mycompany"));</pre>
 *******************************************************************************/
public final class ClusterCacheAudit
{

   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private ClusterCacheAudit()
   {
   }



   /*******************************************************************************
    ** "Class.field" for each unregistered static Memoization in the packages
    ** (and their sub-packages).
    *******************************************************************************/
   public static List<String> unregisteredStaticCaches(String... packageNames) throws Exception
   {
      List<String> result = new ArrayList<>();
      for(String packageName : packageNames)
      {
         for(Class<?> clazz : ClassPathUtils.getClassesInPackage(packageName))
         {
            Field[] fields;
            try
            {
               fields = clazz.getDeclaredFields();
            }
            catch(Throwable t)
            {
               continue;
            }

            for(Field field : fields)
            {
               if(!Modifier.isStatic(field.getModifiers()) || !Memoization.class.isAssignableFrom(field.getType()))
               {
                  continue;
               }
               try
               {
                  field.setAccessible(true);
                  Object value = field.get(null);
                  if(value != null && !ClusterCaches.isRegistered(value))
                  {
                     result.add(clazz.getName() + "." + field.getName());
                  }
               }
               catch(Throwable t)
               {
                  result.add(clazz.getName() + "." + field.getName() + " (could not be read: " + t.getClass().getSimpleName() + ")");
               }
            }
         }
      }
      return (result);
   }
}
