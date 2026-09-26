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
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.AbstractFilterExpression;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.Now;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.ThisOrLastPeriod;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.BETWEEN;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.GREATER_THAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QFilterCriteriaDeserializer 
 *******************************************************************************/
class QFilterCriteriaDeserializerTest extends BaseTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeserialize() throws IOException
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // just put a reference to this class here, so it's a tad easier to find this class via navigation in IDE... //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      new QFilterCriteriaDeserializer();

      {
         QFilterCriteria criteria = JsonUtils.toObject("""
            {"fieldName": "id", "operator": "EQUALS", "values": [1]}
            """, QFilterCriteria.class);
         assertEquals("id", criteria.getFieldName());
         assertEquals(EQUALS, criteria.getOperator());
         assertEquals(List.of(1), criteria.getValues());
      }

      {
         QFilterCriteria criteria = JsonUtils.toObject("""
            {"fieldName": "createDate", "operator": "GREATER_THAN", "values":
               [{"type": "NowWithOffset", "operator": "PLUS", "amount": 5, "timeUnit": "MINUTES"}]
            }
            """, QFilterCriteria.class);
         assertEquals("createDate", criteria.getFieldName());
         assertEquals(GREATER_THAN, criteria.getOperator());
         AbstractFilterExpression<?> expression = (AbstractFilterExpression<?>) criteria.getValues().get(0);
         assertThat(expression).isInstanceOf(NowWithOffset.class);
         NowWithOffset nowWithOffset = (NowWithOffset) expression;
         assertEquals(5, nowWithOffset.getAmount());
         assertEquals(NowWithOffset.Operator.PLUS, nowWithOffset.getOperator());
         assertEquals(ChronoUnit.MINUTES, nowWithOffset.getTimeUnit());
      }

      {
         QFilterCriteria criteria = JsonUtils.toObject("""
            {"fieldName": "orderDate", "operator": "EQUALS", "values": [{"type": "Now"}] }
            """, QFilterCriteria.class);
         assertEquals("orderDate", criteria.getFieldName());
         assertEquals(EQUALS, criteria.getOperator());
         AbstractFilterExpression<?> expression = (AbstractFilterExpression<?>) criteria.getValues().get(0);
         assertThat(expression).isInstanceOf(Now.class);
      }

      {
         QFilterCriteria criteria = JsonUtils.toObject("""
            {"fieldName": "orderDate", "operator": "BETWEEN", "values": [{"type": "Now"}, {"type": "ThisOrLastPeriod"}] }
            """, QFilterCriteria.class);
         assertEquals("orderDate", criteria.getFieldName());
         assertEquals(BETWEEN, criteria.getOperator());
         AbstractFilterExpression<?> expression0 = (AbstractFilterExpression<?>) criteria.getValues().get(0);
         assertThat(expression0).isInstanceOf(Now.class);
         AbstractFilterExpression<?> expression1 = (AbstractFilterExpression<?>) criteria.getValues().get(1);
         assertThat(expression1).isInstanceOf(ThisOrLastPeriod.class);
      }

      {
         assertThatThrownBy(() -> JsonUtils.toObject("""
            {"fieldName": "orderDate", "operator": "BETWEEN", "values": [{"type": "NotAnExpressionType"}] }
            """, QFilterCriteria.class)).hasMessageContaining("Error deserializing criteria value which appeared to be an expression");
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeserializeWithFieldFunction() throws IOException
   {
      QFilterCriteria criteria = JsonUtils.toObject("""
         {"fieldName": "firstName", "operator": "EQUALS", "values": [5],
          "fieldFunction": {"fieldName": "firstName", "functionTypeIdentifierName": "StringLength", "arguments": null}}
         """, QFilterCriteria.class);
      assertEquals("firstName", criteria.getFieldName());
      assertEquals(EQUALS, criteria.getOperator());
      assertEquals(List.of(5), criteria.getValues());

      FieldFunction fieldFunction = criteria.getFieldFunction();
      assertNotNull(fieldFunction);
      assertEquals("firstName", fieldFunction.getFieldName());
      assertEquals("StringLength", fieldFunction.getFunctionTypeIdentifier().getName());
      assertNull(fieldFunction.getArguments());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeserializeWithFieldFunctionWithArguments() throws IOException
   {
      QFilterCriteria criteria = JsonUtils.toObject("""
         {"fieldName": "firstName", "operator": "EQUALS", "values": ["ell"],
          "fieldFunction": {"fieldName": "firstName", "functionTypeIdentifierName": "SubString",
            "arguments": {"fromIndex": 2, "length": 3}}}
         """, QFilterCriteria.class);
      assertEquals("firstName", criteria.getFieldName());

      FieldFunction fieldFunction = criteria.getFieldFunction();
      assertNotNull(fieldFunction);
      assertEquals("SubString", fieldFunction.getFunctionTypeIdentifier().getName());
      assertNotNull(fieldFunction.getArguments());
      assertEquals(2, fieldFunction.getArguments().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeserializeWithoutFieldFunction() throws IOException
   {
      QFilterCriteria criteria = JsonUtils.toObject("""
         {"fieldName": "id", "operator": "EQUALS", "values": [1]}
         """, QFilterCriteria.class);
      assertNull(criteria.getFieldFunction());
   }

}