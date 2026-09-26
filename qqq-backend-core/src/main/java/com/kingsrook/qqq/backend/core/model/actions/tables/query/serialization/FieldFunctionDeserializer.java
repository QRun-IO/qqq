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
import java.util.Map;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionIdentifierRegistry;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;


/*******************************************************************************
 * Custom jackson deserializer, to deal w/ mapping a functionTypeIdentity
 * to a FieldFunctionType object
 *******************************************************************************/
public class FieldFunctionDeserializer extends StdDeserializer<FieldFunction>
{
   private static final QLogger LOG = QLogger.getLogger(FieldFunctionDeserializer.class);

   private static final ObjectMapper objectMapper = new ObjectMapper();



   /*******************************************************************************
    * Default no-arg constructor required by Jackson.
    *******************************************************************************/
   public FieldFunctionDeserializer()
   {
      this(null);
   }



   /*******************************************************************************
    * Constructor accepting the value class; delegates to parent {@link StdDeserializer}.
    *******************************************************************************/
   public FieldFunctionDeserializer(Class<?> vc)
   {
      super(vc);
   }



   /*******************************************************************************
    * Deserializes a FieldFunction JSON node by extracting fieldName, arguments,
    * and functionTypeIdentifierName, resolving the identifier via
    * {@link FieldFunctionIdentifierRegistry}, and constructing the FieldFunction.
    *******************************************************************************/
   @Override
   public FieldFunction deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException
   {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);

      /////////////////////////////////
      // get values out of json node //
      /////////////////////////////////
      @SuppressWarnings("unchecked")
      Map<String, Serializable> arguments = objectMapper.treeToValue(node.get("arguments"), Map.class);
      String fieldName                  = objectMapper.treeToValue(node.get("fieldName"), String.class);
      String functionTypeIdentifierName = objectMapper.treeToValue(node.get("functionTypeIdentifierName"), String.class);

      FieldFunctionTypeIdentifier fieldFunctionTypeIdentifier = FieldFunctionIdentifierRegistry.getInstance().getFieldFunctionTypeIdentifier(functionTypeIdentifierName);
      if(fieldFunctionTypeIdentifier == null)
      {
         throw new IOException("Field function type identifier not found for name [" + functionTypeIdentifierName + "]");
      }

      ///////////////////////////////////
      // put fields into return object //
      ///////////////////////////////////
      FieldFunction fieldFunction = new FieldFunction();
      fieldFunction.setFieldName(fieldName);
      fieldFunction.setArguments(arguments);
      fieldFunction.setFunctionTypeIdentifier(fieldFunctionTypeIdentifier);

      return (fieldFunction);
   }
}
