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

package com.kingsrook.qqq.backend.core.model.metadata.serialization;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Utility methods to help with deserializing JSON streams into QQQ models.
 ** Specifically meant to be used within a jackson custom deserializer (e.g.,
 ** an implementation of JsonDeserializer).
 *******************************************************************************/
public class DeserializerUtils
{
   private static final QLogger LOG = QLogger.getLogger(DeserializerUtils.class);



   /*******************************************************************************
    ** Read a string value, identified by key, from a jackson treeNode.
    *******************************************************************************/
   public static String readTextValue(TreeNode treeNode, String key) throws IOException
   {
      TreeNode valueNode = treeNode.get(key);
      if(valueNode == null || valueNode instanceof NullNode)
      {
         throw new IOException("Missing node named [" + key + "]");
      }

      if(!(valueNode instanceof TextNode textNode))
      {
         throw new IOException(key + "is not a string value (is: " + valueNode.getClass().getSimpleName() + ")");
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // get the value of the backendType json node, and use it to look up the qBackendModule object //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      return (textNode.asText());
   }



   /*******************************************************************************
    ** For a given (jackson, JSON) treeNode, look at its backendType property,
    ** and return an instance of the corresponding QBackendModule.
    *******************************************************************************/
   public static QBackendModuleInterface getBackendModule(TreeNode treeNode) throws IOException
   {
      try
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////
         // get the value of the backendType json node, and use it to look up the qBackendModule object //
         /////////////////////////////////////////////////////////////////////////////////////////////////
         String backendType = readTextValue(treeNode, "backendType");
         return new QBackendModuleDispatcher().getQBackendModule(backendType);
      }
      catch(QModuleDispatchException e)
      {
         throw (new IOException(e));
      }
   }



   /*******************************************************************************
    ** Using reflection, create & populate an instance of a class, based on the
    ** properties in a jackson/json treeNode.
    **
    *******************************************************************************/
   public static <T> T reflectivelyDeserialize(Class<T> outputClass, TreeNode treeNode) throws IOException
   {
      try
      {
         /////////////////////////////////
         // construct the output object //
         /////////////////////////////////
         T output = outputClass.getConstructor().newInstance();

         /////////////////////////////////////////////////////////////////////////////////////////////////
         // set up a mapping between field names, and lambdas which will take a String (from the json), //
         // and set it in the output object, doing type conversion as needed.                           //
         // do this by iterating over methods on the output class that look like setters.               //
         /////////////////////////////////////////////////////////////////////////////////////////////////
         Map<String, Consumer<Object>> setterMap = new HashMap<>();
         for(Method method : outputClass.getMethods())
         {
            /////////////////////////////////////////////////////////////
            // setters start with the word "set", and have 1 parameter //
            /////////////////////////////////////////////////////////////
            if(method.getName().startsWith("set") && method.getParameterTypes().length == 1)
            {
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // get the parameter type, and the name of the field (remove set from the method name, and downshift the first letter) //
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               Class<?> parameterType = method.getParameterTypes()[0];
               String   fieldName     = method.getName().substring(3, 4).toLowerCase(Locale.ROOT) + method.getName().substring(4);

               ///////////////////////////////////////////////////////////////////////////////////
               // put the entry in the map - where the value here is a consumer lambda function //
               ///////////////////////////////////////////////////////////////////////////////////
               setterMap.put(fieldName, (Object value) ->
               {
                  try
                  {
                     if(value == null)
                     {
                        Object[] args = new Object[] { null };
                        method.invoke(output, args);
                        return;
                     }

                     //////////////////////////////////////////////////////////////////////////////////////////////////
                     // based on the parameter type, handle it differently - either type-converting (e.g., parseInt) //
                     // or gracefully ignoring, or failing.                                                          //
                     //////////////////////////////////////////////////////////////////////////////////////////////////
                     String valueString = String.valueOf(value);
                     if(parameterType.equals(String.class))
                     {
                        method.invoke(output, String.valueOf(value));
                     }
                     else if(parameterType.equals(Integer.class))
                     {
                        method.invoke(output, ValueUtils.getValueAsInteger(value));
                     }
                     else if(parameterType.equals(Long.class))
                     {
                        method.invoke(output, StringUtils.hasContent(valueString) ? Long.parseLong(valueString) : null);
                     }
                     else if(parameterType.equals(BigDecimal.class))
                     {
                        method.invoke(output, StringUtils.hasContent(valueString) ? new BigDecimal(valueString) : null);
                     }
                     else if(parameterType.equals(Boolean.class))
                     {
                        method.invoke(output, StringUtils.hasContent(valueString) ? Boolean.parseBoolean(valueString) : null);
                     }
                     else if(parameterType.isEnum())
                     {
                        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // if the param is an enum, look up the static 'valueOf' method that all enums have.                            //
                        // call that method, passing it the string value from the json (the null there is because it's a static method) //
                        // then pass the num value into the output object, via our method.invoke.                                       //
                        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        if(StringUtils.hasContent(valueString))
                        {
                           Method valueOfMethod = parameterType.getMethod("valueOf", String.class);
                           Object enumValue     = valueOfMethod.invoke(null, value);
                           method.invoke(output, enumValue);
                        }
                        else
                        {
                           method.invoke(output, (Object) null);
                        }
                     }
                     else if(parameterType.equals(Class.class))
                     {
                        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // specifically do NOT try to handle Class type arguments                                                    //
                        // we hit this when trying to de-serialize a QBackendMetaData, and we found its setBackendType(Class) method //
                        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     }
                     else if(parameterType.isAssignableFrom(List.class))
                     {
                        if(value instanceof List<?>)
                        {
                           method.invoke(output, value);
                        }
                     }
                     else if(parameterType.isAssignableFrom(Map.class))
                     {
                        // TypeVariable<? extends Class<?>> keyType   = parameterType.getTypeParameters()[0];
                        // TypeVariable<? extends Class<?>> valueType = parameterType.getTypeParameters()[1];
                        Map<?, ?> map = new LinkedHashMap<>();
                        // todo - recursively process
                        method.invoke(output, map);
                     }
                     else
                     {
                        /////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // if we hit this, we might want to add an else-if to handle the type.                                 //
                        // otherwise, either find some jackson annotation that makes sense, and apply it to the setter method, //
                        // or if no jackson annotation is right, then come up with annotation of our own.                      //
                        /////////////////////////////////////////////////////////////////////////////////////////////////////////
                        method.invoke(output, reflectivelyDeserialize((Class<?>) parameterType, (TreeNode) value));
                     }
                  }
                  catch(IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException e)
                  {
                     throw new RuntimeException(e);
                  }
               });
            }
         }

         DeserializerUtils.deserializeBean(treeNode, setterMap);

         return output;
      }
      catch(Exception e)
      {
         LOG.warn(e);
         throw (new IOException("Error deserializing json object into instance of " + outputClass.getName(), e));
      }
   }



   /*******************************************************************************
    ** Helper for custom jackson serializers - allows the caller to specify a map
    ** of field names to setter methods.
    **
    ** Note, the consumers in the map all work on strings, so you may need to do
    ** Integer.parseInt, for example, in a lambda in the map.
    *******************************************************************************/
   private static void deserializeBean(TreeNode treeNode, Map<String, Consumer<Object>> setterMap) throws IOException
   {
      ///////////////////////////////////////////////////////
      // iterate over fields in the json object (treeNode) //
      ///////////////////////////////////////////////////////
      Iterator<String> fieldNamesIterator = treeNode.fieldNames();
      while(fieldNamesIterator.hasNext())
      {
         String fieldName = fieldNamesIterator.next();

         //////////////////////////////////////////////////////////////////////////
         // error if we find a field in the json that we don't have a setter for //
         //////////////////////////////////////////////////////////////////////////
         if(!setterMap.containsKey(fieldName))
         {
            throw (new IOException("Unexpected field (no corresponding setter): " + fieldName));
         }

         // call the setter -
         TreeNode fieldNode = treeNode.get(fieldName);
         if(fieldNode instanceof NullNode)
         {
            setterMap.get(fieldName).accept(null);
         }
         else if(fieldNode instanceof TextNode textNode)
         {
            setterMap.get(fieldName).accept(textNode.asText());
         }
         else if(fieldNode instanceof BooleanNode booleanNode)
         {
            setterMap.get(fieldName).accept(booleanNode);
         }
         else if(fieldNode instanceof ObjectNode)
         {
            setterMap.get(fieldName).accept(fieldNode);
         }
         else if(fieldNode instanceof ArrayNode arrayNode)
         {
            List<Object> list = new ArrayList<>();
            for(JsonNode jsonNode : arrayNode)
            {
               // todo - actually build the objects...
            }
            setterMap.get(fieldName).accept(list);
         }
         else
         {
            throw (new IOException("Unexpected node type (" + fieldNode.getClass() + ") for field: " + fieldName));
         }
      }
   }
}
