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

package com.kingsrook.qqq.backend.module.rdbms.strategy;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 ** RDBMS action strategy for a field with a FULLTEXT INDEX on it in a MySQL
 ** database.  Makes a LIKE or CONTAINS (or NOT those) query use the special
 ** syntax that hits the FULLTEXT INDEX.
 *******************************************************************************/
public class MySQLFullTextIndexFieldStrategy extends BaseRDBMSActionStrategy
{
   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public Integer appendCriterionToWhereClause(QFilterCriteria criterion, StringBuilder clause, String column, List<Serializable> values, QFieldMetaData field)
   {
      switch(criterion.getOperator())
      {
         case LIKE, CONTAINS ->
         {
            clause.append(" MATCH (").append(column).append(") AGAINST (?) ");
            return (1);
         }
         case NOT_LIKE, NOT_CONTAINS ->
         {
            clause.append(" NOT MATCH (").append(column).append(") AGAINST (?) ");
            return (1);
         }
         default ->
         {
            return super.appendCriterionToWhereClause(criterion, clause, column, values, field);
         }
      }
   }
}
