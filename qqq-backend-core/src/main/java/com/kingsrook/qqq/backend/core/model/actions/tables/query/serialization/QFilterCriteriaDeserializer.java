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

package com.kingsrook.qqq.backend.core.model.actions.tables.query.serialization;


import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaOption;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaOptionInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.AbstractFilterExpression;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Custom jackson deserializer, to deal w/ abstract expression field
 *******************************************************************************/
public class QFilterCriteriaDeserializer extends StdDeserializer<QFilterCriteria>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public QFilterCriteriaDeserializer()
   {
      this(null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFilterCriteriaDeserializer(Class<?> vc)
   {
      super(vc);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QFilterCriteria deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
   {
      JsonNode     node         = jsonParser.getCodec().readTree(jsonParser);
      ObjectMapper objectMapper = new ObjectMapper();

      /////////////////////////////////
      // get values out of json node //
      /////////////////////////////////
      @SuppressWarnings("unchecked")
      List<Serializable> values = objectMapper.treeToValue(node.get("values"), List.class);
      String            fieldName      = objectMapper.treeToValue(node.get("fieldName"), String.class);
      QCriteriaOperator operator       = objectMapper.treeToValue(node.get("operator"), QCriteriaOperator.class);
      String            otherFieldName = objectMapper.treeToValue(node.get("otherFieldName"), String.class);
      FieldFunction     fieldFunction  = objectMapper.treeToValue(node.get("fieldFunction"), FieldFunction.class);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // look at all the values - if any of them are actually meant to be an Expression (instance of subclass of AbstractFilterExpression)     //
      // they'll have deserialized as a Map, with a "type" key.  If that's the case, then re/de serialize them into the proper expression type //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      ListIterator<Serializable> valuesIterator = CollectionUtils.nonNullList(values).listIterator();
      while(valuesIterator.hasNext())
      {
         Object value = valuesIterator.next();
         if(value instanceof Map<?, ?> map && map.containsKey("type"))
         {
            String expressionType = ValueUtils.getValueAsString(map.get("type"));

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // right now, we'll assume that all expression subclasses are in the same package as AbstractFilterExpression //
            // so, we can just do a Class.forName on that name, and use JsonUtils.toObject requesting that class.         //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            try
            {
               String assumedExpressionJSON = JsonUtils.toJson(map);

               String       className        = AbstractFilterExpression.class.getName().replace(AbstractFilterExpression.class.getSimpleName(), expressionType);
               Serializable replacementValue = (Serializable) JsonUtils.toObject(assumedExpressionJSON, Class.forName(className));
               valuesIterator.set(replacementValue);
            }
            catch(Exception e)
            {
               throw (new IOException("Error deserializing criteria value which appeared to be an expression of type [" + expressionType + "] inside QFilterCriteria", e));
            }
         }
      }

      ///////////////////////////////////
      // put fields into return object //
      ///////////////////////////////////
      QFilterCriteria criteria = new QFilterCriteria();
      criteria.setFieldName(fieldName);
      criteria.setOperator(operator);
      criteria.setValues(values);
      criteria.setOtherFieldName(otherFieldName);
      criteria.setFieldFunction(fieldFunction);
      criteria.setOptions(readOptions(node.get("options")));

      return (criteria);
   }



   /*******************************************************************************
    ** Read a criteria's options:  an array of CriteriaOption names (null when
    ** absent or empty).  An unknown name is a bad request (a QBadRequestException
    ** cause on a JsonMappingException - jackson drops the cause of a plain
    ** IOException thrown from a deserializer).
    *******************************************************************************/
   private static Set<CriteriaOptionInterface> readOptions(JsonNode optionsNode) throws IOException
   {
      if(optionsNode == null || optionsNode.isNull())
      {
         return (null);
      }

      if(!optionsNode.isArray())
      {
         throw (new JsonMappingException(null, "Criteria options must be an array.", new QBadRequestException("Criteria options must be an array of option names.").withFieldError("options", "Must be an array of option names.")));
      }

      Set<CriteriaOptionInterface> options = new HashSet<>();
      for(JsonNode optionNode : optionsNode)
      {
         String name = optionNode.asText();
         try
         {
            options.add(CriteriaOption.valueOf(name));
         }
         catch(IllegalArgumentException e)
         {
            String message = "Unknown criteria option [" + name + "].  Known options: " + Arrays.toString(CriteriaOption.values()) + ".";
            throw (new JsonMappingException(null, message, new QBadRequestException(message).withFieldError("options", message)));
         }
      }
      return (options.isEmpty() ? null : options);
   }
}
