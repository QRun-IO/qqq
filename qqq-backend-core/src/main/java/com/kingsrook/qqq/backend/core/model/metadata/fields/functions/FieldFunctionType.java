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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions;


import java.io.Serializable;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/***************************************************************************
 * Interface defining a type of function that can be applied to a field in a
 * QQQ query or virtual field expression.
 *
 * <p>Each implementation defines which field types it accepts, what parameters
 * it requires, what type it returns, and how to evaluate it both in-memory
 * (via {@link #apply}) and for sorting (via {@link #applyForSorting}).</p>
 *
 * <p>Backend-specific adapters (e.g., RDBMS SQL generation) are registered
 * separately via {@link BackendFieldFunctionAdapterRegistry}.</p>
 ***************************************************************************/
public interface FieldFunctionType
{
   /***************************************************************************
    * Returns the unique identifier for this function type.
    ***************************************************************************/
   FieldFunctionTypeIdentifier getIdentifier();

   /***************************************************************************
    * Returns the set of QFieldTypes that this function may be applied to.
    ***************************************************************************/
   Set<QFieldType> getAllowedFieldTypes();

   /***************************************************************************
    * Returns the list of parameters this function accepts, including names, types, and defaults.
    ***************************************************************************/
   List<FieldFunctionParameter> getParameters();

   /***************************************************************************
    * Returns the QFieldType that this function produces as output.
    ***************************************************************************/
   QFieldType getReturnType();

   /***************************************************************************
    * Applies this function to the given record's field value and returns the result.
    ***************************************************************************/
   Serializable apply(FieldFunction fieldFunction, QRecord record) throws QValueException, QException;

   /***************************************************************************
    * Applies this function for sort-order purposes. Defaults to the same result
    * as {@link #apply}, but may be overridden to produce a different ordering
    * value (e.g., shifting Sunday to position 0).
    ***************************************************************************/
   default Serializable applyForSorting(FieldFunction fieldFunction, QRecord record) throws QValueException, QException
   {
      return (apply(fieldFunction, record));
   }

}
