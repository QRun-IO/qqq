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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.BackendFieldFunctionAdapterInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;


/*******************************************************************************
 * RDBMS-specific extension of {@link BackendFieldFunctionAdapterInterface}
 * for adapting a {@link FieldFunction} into SQL expressions.
 *
 * <p>Implementations wrap a raw escaped column name in the appropriate SQL
 * function call (e.g., {@code CHAR_LENGTH(`col`)}) for use in SELECT and WHERE
 * clauses, and optionally supply bind-parameter values (e.g., for {@code
 * SUBSTR(`col`, ?, ?)}</p>
 *
 * <p>By default, the same expression is used for ORDER-BY, but, an alternative
 * can be used instead if needed (e.g., to change weekday sorting)</p>
 *******************************************************************************/
public interface RDBMSFieldFunctionAdapterInterface extends BackendFieldFunctionAdapterInterface
{
   /***************************************************************************
    * Wraps the given escaped SQL column reference in the appropriate SQL
    * function expression for use in SELECT, WHERE, ORDER-BY or GROUP-BY clauses.
    *
    * @param escapedColumnName a fully qualified and escaped SQL column name
    *                          for the 'main' field that this function works with
    *                          (e.g., {@code FieldFunction#withFieldName(String)}.
    *                          Looks like:  {@code `my_table`.`my_field`}.
    * @param fieldFunction the function with arguments, being used.
    * @param fieldNameToColumnReference resolves a QQQ field name to its fully-
    *        qualified escaped SQL column reference (e.g., {@code "timeZone"} →
    *        {@code `t1`.`time_zone`}).  Useful when the function expression
    *        needs to reference additional columns on the same row.
    ***************************************************************************/
   String wrapColumnName(String escapedColumnName, FieldFunction fieldFunction,
                         Function<String, String> fieldNameToColumnReference);


   /***************************************************************************
    * Returns any SQL bind-parameter values needed by the wrapped expression
    * (e.g., SUBSTRING start index). Returns an empty list by default.
    ***************************************************************************/
   default List<Serializable> getParams(FieldFunction fieldFunction)
   {
      return Collections.emptyList();
   }


   /***************************************************************************
    * Wraps the column reference for use specifically in an ORDER BY clause.
    * Defaults to calling {@link #wrapColumnName}; may be overridden when the
    * ORDER BY form differs (e.g., a modulo for Sunday-first sorting).
    ***************************************************************************/
   default String wrapColumnNameForOrderBy(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
   {
      return wrapColumnName(escapedColumnName, fieldFunction, fieldNameToColumnReference);
   }
}
