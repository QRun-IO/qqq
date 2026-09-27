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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for MySQLFullTextIndexFieldStrategy 
 *******************************************************************************/
class MySQLFullTextIndexFieldStrategyTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      ///////////////////////////////////////////////////
      // test an operator that uses the fulltext index //
      ///////////////////////////////////////////////////
      QFieldMetaData field = new QFieldMetaData("myText", QFieldType.TEXT);
      QFilterCriteria criterion = new QFilterCriteria(field.getName(), QCriteriaOperator.LIKE, "hello");
      StringBuilder clause = new StringBuilder();
      Integer expectedNoOfParams = new MySQLFullTextIndexFieldStrategy().appendCriterionToWhereClause(criterion, clause, "my_text", List.of("hello"), field);
      assertEquals(1, expectedNoOfParams);
      assertEquals(" MATCH (my_text) AGAINST (?) ", clause.toString());

      ////////////////////////////////////////////
      // test a negated fulltext index operator //
      ////////////////////////////////////////////
      criterion.setOperator(QCriteriaOperator.NOT_CONTAINS);
      clause.delete(0, clause.length());
      expectedNoOfParams = new MySQLFullTextIndexFieldStrategy().appendCriterionToWhereClause(criterion, clause, "my_text", List.of("hello"), field);
      assertEquals(1, expectedNoOfParams);
      assertEquals(" NOT MATCH (my_text) AGAINST (?) ", clause.toString());

      ////////////////////////////////////////////
      // an operator that should defer to super //
      ////////////////////////////////////////////
      criterion.setOperator(QCriteriaOperator.IS_BLANK);
      clause.delete(0, clause.length());
      expectedNoOfParams = new MySQLFullTextIndexFieldStrategy().appendCriterionToWhereClause(criterion, clause, "my_text", List.of("hello"), field);
      assertEquals(0, expectedNoOfParams);
      assertEquals("my_text IS NULL OR my_text = ''", clause.toString());
   }

}