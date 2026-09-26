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

package com.kingsrook.qqq.backend.module.mongodb.fieldfunctions;


import java.util.List;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import org.bson.Document;


/*******************************************************************************
 ** MongoDB adapter for SubStringFunction.
 ** Generates {$substrCP: ["$field", fromIndex, length]}.
 ** MongoDB $substrCP is 0-based; QQQ SubStringFunction uses 1-based (like SQL).
 ** When no length is specified, uses Integer.MAX_VALUE as a sentinel.
 *******************************************************************************/
public class MongoDBSubStringFunction implements MongoDBFieldFunctionAdapterInterface
{

   @Override
   public Object getExpression(String fieldReference, FieldFunction fieldFunction, Function<String, String> fieldNameToFieldReference)
   {
      Integer fromIndex = fieldFunction.getArgumentValueOrDefault(Integer.class, SubStringFunction.FROM_INDEX_PARAM);
      Integer length    = fieldFunction.getArgumentValueOrDefault(Integer.class, SubStringFunction.LENGTH_PARAM);

      int mongoFromIndex = (fromIndex != null ? fromIndex : 1) - 1; // convert 1-based to 0-based
      int mongoLength    = (length != null ? length : Integer.MAX_VALUE);

      return new Document("$substrCP", List.of(fieldReference, mongoFromIndex, mongoLength));
   }

}
