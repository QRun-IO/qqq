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


import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QIgnore;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Utilities for bean-like classes (e.g., QRecordEntity, QProcessPayload) that
 ** use reflection to understand their bean-fields
 *******************************************************************************/
public class ReflectiveBeanLikeClassUtils
{
   private static final QLogger LOG = QLogger.getLogger(ReflectiveBeanLikeClassUtils.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getFieldNameFromGetter(Method getter)
   {
      String nameWithoutGet = getter.getName().replaceFirst("^get", "");
      if(nameWithoutGet.length() == 1)
      {
         return (nameWithoutGet.toLowerCase(Locale.ROOT));
      }
      return (nameWithoutGet.substring(0, 1).toLowerCase(Locale.ROOT) + nameWithoutGet.substring(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean isGetter(Method method, boolean allowAssociations)
   {
      return isGetter(method, allowAssociations, defaultAllowedTypes());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean isGetter(Method method, boolean allowAssociations, Collection<Class<?>> allowedTypes)
   {
      if(method.getParameterTypes().length == 0 && method.getName().matches("^get[A-Z].*"))
      {
         if(allowedTypes.contains(method.getReturnType()) || (allowAssociations && isSupportedAssociation(method.getReturnType(), method.getAnnotatedReturnType())))
         {
            return (true);
         }
         else
         {
            if(!method.getName().equals("getClass") && method.getAnnotation(QIgnore.class) == null)
            {
               LOG.debug("Method [" + method.getName() + "] in [" + method.getDeclaringClass().getSimpleName() + "] looks like a getter, but its return type, [" + method.getReturnType().getSimpleName() + "], isn't supported.");
            }
         }
      }
      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<Method> getSetterForGetter(Class<?> c, Method getter)
   {
      String setterName = getter.getName().replaceFirst("^get", "set");
      for(Method method : c.getMethods())
      {
         if(method.getName().equals(setterName))
         {
            if(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(getter.getReturnType()))
            {
               return (Optional.of(method));
            }
            else
            {
               LOG.info("Method [" + method.getName() + "] looks like a setter for [" + getter.getName() + "], but its parameters, [" + Arrays.toString(method.getParameterTypes()) + "], don't match the getter's return type [" + getter.getReturnType() + "]");
            }
         }
      }
      return (Optional.empty());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static Collection<Class<?>> defaultAllowedTypes()
   {
      /////////////////////////////////////////////
      // note - this list has implications upon: //
      // - QFieldType.fromClass                  //
      // - QRecordEntityField.convertValueType   //
      /////////////////////////////////////////////
      return (Set.of(String.class,
         Integer.class,
         Long.class,
         int.class,
         Boolean.class,
         boolean.class,
         BigDecimal.class,
         Instant.class,
         LocalDate.class,
         LocalTime.class,
         byte[].class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isSupportedAssociation(Class<?> returnType, AnnotatedType annotatedType)
   {
      Class<?> listTypeParam = getListTypeParam(returnType, annotatedType);
      return (listTypeParam != null && QRecordEntity.class.isAssignableFrom(listTypeParam));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Class<?> getListTypeParam(Class<?> listType, AnnotatedType annotatedType)
   {
      if(listType.equals(List.class))
      {
         if(annotatedType instanceof AnnotatedParameterizedType apt)
         {
            AnnotatedType[] annotatedActualTypeArguments = apt.getAnnotatedActualTypeArguments();
            for(AnnotatedType annotatedActualTypeArgument : annotatedActualTypeArguments)
            {
               Type type = annotatedActualTypeArgument.getType();
               if(type instanceof Class<?> c)
               {
                  return (c);
               }
            }
         }
      }

      return (null);
   }

}
