/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.instances.loaders;


import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import org.apache.commons.io.IOUtils;
import static com.kingsrook.qqq.backend.core.utils.ValueUtils.getValueAsInteger;
import static com.kingsrook.qqq.backend.core.utils.ValueUtils.getValueAsString;


/*******************************************************************************
 ** Abstract base class in hierarchy of classes that know how to construct &
 ** populate QMetaDataObject instances, based on input streams (e.g., from files).
 *******************************************************************************/
public abstract class AbstractMetaDataLoader<T extends QMetaDataObject>
{
   private static final QLogger LOG = QLogger.getLogger(AbstractMetaDataLoader.class);

   private String fileName;

   private List<LoadingProblem> problems = new ArrayList<>();



   /***************************************************************************
    **
    ***************************************************************************/
   public T fileToMetaDataObject(QInstance qInstance, InputStream inputStream, String fileName) throws QMetaDataLoaderException
   {
      this.fileName = fileName;
      Map<String, Object> map = fileToMap(inputStream, fileName);
      LoadingContext loadingContext = new LoadingContext(fileName, "/");
      return (mapToMetaDataObject(qInstance, map, loadingContext));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public abstract T mapToMetaDataObject(QInstance qInstance, Map<String, Object> map, LoadingContext context) throws QMetaDataLoaderException;



   /***************************************************************************
    **
    ***************************************************************************/
   protected Map<String, Object> fileToMap(InputStream inputStream, String fileName) throws QMetaDataLoaderException
   {
      try
      {
         String string = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
         string = StringUtils.ltrim(string);
         if(fileName.toLowerCase().endsWith(".json"))
         {
            return JsonUtils.toObject(string, new TypeReference<>() {});
         }
         else if(fileName.toLowerCase().endsWith(".yaml") || fileName.toLowerCase().endsWith(".yml"))
         {
            return YamlUtils.toMap(string);
         }

         throw (new QMetaDataLoaderException("Unsupported file format (based on file name: " + fileName + ")"));
      }
      catch(IOException e)
      {
         throw new QMetaDataLoaderException("Error building map from file: " + fileName, e);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected void reflectivelyMap(QInstance qInstance, QMetaDataObject targetObject, Map<String, Object> map, LoadingContext context)
   {
      Class<? extends QMetaDataObject> targetClass    = targetObject.getClass();
      Set<String>                      usedFieldNames = new HashSet<>();

      for(Method method : targetClass.getMethods())
      {
         try
         {
            if(method.getName().startsWith("set") && method.getParameterTypes().length == 1)
            {
               String propertyName = StringUtils.lcFirst(method.getName().substring(3));

               if(map.containsKey(propertyName))
               {
                  usedFieldNames.add(propertyName);
                  Class<?> parameterType = method.getParameterTypes()[0];
                  Object   rawValue      = map.get(propertyName);

                  try
                  {
                     Object mappedValue = reflectivelyMapValue(qInstance, method, parameterType, rawValue, context.descendToProperty(propertyName));
                     method.invoke(targetObject, mappedValue);
                  }
                  catch(NoValueException nve)
                  {
                     ///////////////////////
                     // don't call setter //
                     ///////////////////////
                     LOG.debug("at " + context + ": No value was mapped for property [" + propertyName + "] on " + targetClass.getSimpleName() + "." + method.getName() + ", raw value: [" + rawValue + "]");
                  }
               }
            }
         }
         catch(Exception e)
         {
            addProblem(new LoadingProblem(context, "Error reflectively mapping on " + targetClass.getName() + "." + method.getName(), e));
         }
      }

      //////////////////////////
      // mmm, slightly sus... //
      //////////////////////////
      map.remove("class");
      map.remove("version");

      Set<String> unrecognizedKeys = new HashSet<>(map.keySet());
      unrecognizedKeys.removeAll(usedFieldNames);

      if(!unrecognizedKeys.isEmpty())
      {
         addProblem(new LoadingProblem(context, unrecognizedKeys.size() + " Unrecognized " + StringUtils.plural(unrecognizedKeys, "property", "properties") + ": " + unrecognizedKeys));
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public Object reflectivelyMapValue(QInstance qInstance, Method method, Class<?> parameterType, Object rawValue, LoadingContext context) throws Exception
   {
      int initialProblemCount = problems.size();
      if(rawValue instanceof String s && s.matches("^\\$\\{.+\\..+}"))
      {
         rawValue = new QMetaDataVariableInterpreter().interpret(s);
         LOG.debug("Interpreted raw value [" + s + "] as [" + StringUtils.maskAndTruncate(ValueUtils.getValueAsString(rawValue) + "]"));
      }

      if(parameterType.equals(String.class))
      {
         return (getValueAsString(rawValue));
      }
      else if(parameterType.equals(Integer.class))
      {
         try
         {
            return (getValueAsInteger(rawValue));
         }
         catch(Exception e)
         {
            addProblem(new LoadingProblem(context, "[" + rawValue + "] is not an Integer value."));
         }
      }
      else if(parameterType.equals(Boolean.class))
      {
         if("true".equals(rawValue) || Boolean.TRUE.equals(rawValue))
         {
            return (true);
         }
         else if("false".equals(rawValue) || Boolean.FALSE.equals(rawValue))
         {
            return (false);
         }
         else if(rawValue == null)
         {
            return (null);
         }
         else
         {
            addProblem(new LoadingProblem(context, "[" + rawValue + "] is not a boolean value (must be 'true' or 'false')."));
            return (null);
         }
      }
      else if(parameterType.equals(boolean.class))
      {
         if("true".equals(rawValue) || Boolean.TRUE.equals(rawValue))
         {
            return (true);
         }
         else if("false".equals(rawValue) || Boolean.FALSE.equals(rawValue))
         {
            return (false);
         }
         else
         {
            addProblem(new LoadingProblem(context, rawValue + " is not a boolean value (must be 'true' or 'false')."));
            throw (new NoValueException());
         }
      }
      else if(parameterType.equals(List.class))
      {
         Type     actualTypeArgument = ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
         Class<?> actualTypeClass    = Class.forName(actualTypeArgument.getTypeName());

         if(rawValue instanceof @SuppressWarnings("rawtypes")List valueList)
         {
            List<Object> mappedValueList = new ArrayList<>();
            for(Object o : valueList)
            {
               try
               {
                  Object mappedValue = reflectivelyMapValue(qInstance, null, actualTypeClass, o, context);
                  mappedValueList.add(mappedValue);
               }
               catch(NoValueException nve)
               {
                  // leave off list
               }
            }
            return (mappedValueList);
         }
      }
      else if(parameterType.equals(Set.class))
      {
         Type     actualTypeArgument = ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
         Class<?> actualTypeClass    = Class.forName(actualTypeArgument.getTypeName());

         if(rawValue instanceof @SuppressWarnings("rawtypes")List valueList)
         {
            Set<Object> mappedValueSet = new LinkedHashSet<>();
            for(Object o : valueList)
            {
               try
               {
                  Object mappedValue = reflectivelyMapValue(qInstance, null, actualTypeClass, o, context);
                  mappedValueSet.add(mappedValue);
               }
               catch(NoValueException nve)
               {
                  // leave off list
               }
            }
            return (mappedValueSet);
         }
      }
      else if(parameterType.equals(Map.class))
      {
         Type keyType = ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
         if(!keyType.equals(String.class))
         {
            addProblem(new LoadingProblem(context, "Unsupported key type for " + method + " got [" + keyType + "], expected [String]"));
            throw new NoValueException();
         }
         // todo make sure string

         Type     actualTypeArgument = ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[1];
         Class<?> actualTypeClass    = Class.forName(actualTypeArgument.getTypeName());

         if(rawValue instanceof @SuppressWarnings("rawtypes")Map valueMap)
         {
            Map<String, Object> mappedValueMap = new LinkedHashMap<>();
            for(Object o : valueMap.entrySet())
            {
               try
               {
                  @SuppressWarnings("unchecked")
                  Map.Entry<String, Object> entry = (Map.Entry<String, Object>) o;
                  Object mappedValue = reflectivelyMapValue(qInstance, null, actualTypeClass, entry.getValue(), context);
                  mappedValueMap.put(entry.getKey(), mappedValue);
               }
               catch(NoValueException nve)
               {
                  // leave out of map
               }
            }
            return (mappedValueMap);
         }
      }
      else if(parameterType.isEnum())
      {
         String value = getValueAsString(rawValue);
         for(Object enumConstant : parameterType.getEnumConstants())
         {
            if(((Enum<?>) enumConstant).name().equals(value))
            {
               return (enumConstant);
            }
         }

         addProblem(new LoadingProblem(context, "Unrecognized value [" + rawValue + "].  Expected one of: " + Arrays.toString(parameterType.getEnumConstants())));
      }
      else if(MetaDataLoaderRegistry.hasLoaderForClass(parameterType))
      {
         if(rawValue instanceof @SuppressWarnings("rawtypes")Map valueMap)
         {
            Class<? extends AbstractMetaDataLoader<?>> loaderClass = MetaDataLoaderRegistry.getLoaderForClass(parameterType);
            AbstractMetaDataLoader<?>                  loader      = loaderClass.getConstructor().newInstance();
            //noinspection unchecked
            QMetaDataObject metadata = loader.mapToMetaDataObject(qInstance, valueMap, context);
            problems.addAll(loader.getProblems());
            return metadata;
         }
      }
      else if(QMetaDataObject.class.isAssignableFrom(parameterType))
      {
         if(rawValue instanceof @SuppressWarnings("rawtypes")Map valueMap)
         {
            QMetaDataObject childObject = (QMetaDataObject) parameterType.getConstructor().newInstance();
            //noinspection unchecked
            reflectivelyMap(qInstance, childObject, valueMap, context);
            return (childObject);
         }
      }
      else if(parameterType.equals(Serializable.class))
      {
         if(rawValue instanceof String
            || rawValue instanceof Integer
            || rawValue instanceof BigDecimal
            || rawValue instanceof Boolean
         )
         {
            return rawValue;
         }
      }
      else
      {
         // todo clean up this message/level
         addProblem(new LoadingProblem(context, "No case for " + parameterType + " (arg to: " + method + ")"));
      }

      if(rawValue != null && problems.size() == initialProblemCount)
      {
         addProblem(new LoadingProblem(context, "Cannot map " + rawValue.getClass().getSimpleName() + " to " + parameterType.getSimpleName()));
      }
      throw new NoValueException();
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // unclear if the below is needed.  if so, useful to not re-write, but is hurting test coverage, so zombie until used //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   ///***************************************************************************
   // *
   // ***************************************************************************/
   //protected ListOfMapOrMapOfMap getListOfMapOrMapOfMap(Map<String, Object> map, String key)
   //{
   //   if(map.containsKey(key))
   //   {
   //      if(map.get(key) instanceof List)
   //      {
   //         return (new ListOfMapOrMapOfMap((List<Map<String, Object>>) map.get(key)));
   //      }
   //      else if(map.get(key) instanceof Map)
   //      {
   //         return (new ListOfMapOrMapOfMap((Map<String, Map<String, Object>>) map.get(key)));
   //      }
   //      else
   //      {
   //         LOG.warn("Expected list or map under key [" + key + "] while processing [" + getClass().getSimpleName() + "] from [" + fileName + "], but found: " + (map.get(key) == null ? "null" : map.get(key).getClass().getSimpleName()));
   //      }
   //   }

   //   return (null);
   //}

   ///***************************************************************************
   // *
   // ***************************************************************************/
   //protected List<Map<String, Object>> getListOfMap(Map<String, Object> map, String key)
   //{
   //   if(map.containsKey(key))
   //   {
   //      if(map.get(key) instanceof List)
   //      {
   //         return (List<Map<String, Object>>) map.get(key);
   //      }
   //      else
   //      {
   //         LOG.warn("Expected list under key [" + key + "] while processing [" + getClass().getSimpleName() + "] from [" + fileName + "], but found: " + (map.get(key) == null ? "null" : map.get(key).getClass().getSimpleName()));
   //      }
   //   }

   //   return (null);
   //}

   ///***************************************************************************
   // *
   // ***************************************************************************/
   //protected Map<String, Map<String, Object>> getMapOfMap(Map<String, Object> map, String key)
   //{
   //   if(map.containsKey(key))
   //   {
   //      if(map.get(key) instanceof Map)
   //      {
   //         return (Map<String, Map<String, Object>>) map.get(key);
   //      }
   //      else
   //      {
   //         LOG.warn("Expected map under key [" + key + "] while processing [" + getClass().getSimpleName() + "] from [" + fileName + "], but found: " + (map.get(key) == null ? "null" : map.get(key).getClass().getSimpleName()));
   //      }
   //   }

   //   return (null);
   //}

   ///***************************************************************************
   // **
   // ***************************************************************************/
   //protected record ListOfMapOrMapOfMap(List<Map<String, Object>> listOf, Map<String, Map<String, Object>> mapOf)
   //{
   //   /*******************************************************************************
   //    ** Constructor
   //    **
   //    *******************************************************************************/
   //   public ListOfMapOrMapOfMap(List<Map<String, Object>> listOf)
   //   {
   //      this(listOf, null);
   //   }

   //   /*******************************************************************************
   //    ** Constructor
   //    **
   //    *******************************************************************************/
   //   public ListOfMapOrMapOfMap(Map<String, Map<String, Object>> mapOf)
   //   {
   //      this(null, mapOf);
   //   }
   //}



   /*******************************************************************************
    ** Getter for fileName
    **
    *******************************************************************************/
   public String getFileName()
   {
      return fileName;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class NoValueException extends Exception
   {
      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public NoValueException()
      {
         super("No value");
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void addProblem(LoadingProblem problem)
   {
      problems.add(problem);
   }



   /*******************************************************************************
    ** Getter for problems
    **
    *******************************************************************************/
   public List<LoadingProblem> getProblems()
   {
      return (problems);
   }
}
