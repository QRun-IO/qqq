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

package com.kingsrook.qqq.backend.module.rdbms.fieldfunctions;


import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;


/*******************************************************************************
 * RDBMS adapter for the {@link SubStringFunction} that generates SQL
 * {@code SUBSTRING(col FROM ? [FOR ?])} expressions.
 *
 * <p>The fromIndex bind parameter is always supplied; the length bind parameter
 * is included only when the {@code length} argument is provided.</p>
 *******************************************************************************/
public class RDBMSSubStringFunction implements RDBMSFieldFunctionAdapterInterface
{

   /***************************************************************************
    * Returns a SUBSTRING SQL expression, using {@code FROM ?} form without a
    * length, or {@code FROM ? FOR ?} form when a length argument is present.
    ***************************************************************************/
   @Override
   public String wrapColumnName(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
   {
      Integer length = fieldFunction.getArgumentValueOrDefault(Integer.class, SubStringFunction.LENGTH_PARAM);
      if(length == null)
      {
         return "SUBSTRING(" + escapedColumnName + " FROM ?)";
      }
      else
      {
         return "SUBSTRING(" + escapedColumnName + " FROM ? FOR ?)";
      }
   }



   /***************************************************************************
    * Returns the SQL bind parameters for the SUBSTRING expression: [fromIndex]
    * or [fromIndex, length] depending on whether length was specified.
    ***************************************************************************/
   @Override
   public List<Serializable> getParams(FieldFunction fieldFunction)
   {
      Integer fromIndex = fieldFunction.getArgumentValueOrDefault(Integer.class, SubStringFunction.FROM_INDEX_PARAM);
      Integer length    = fieldFunction.getArgumentValueOrDefault(Integer.class, SubStringFunction.LENGTH_PARAM);

      if(length == null)
      {
         return ListBuilder.of(fromIndex);
      }
      else
      {
         return ListBuilder.of(fromIndex, length);
      }
   }

}
