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

package com.kingsrook.qqq.backend.core.actions.ai.filter;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.Now;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.ThisOrLastPeriod;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for AiFilterBuilder.
 *******************************************************************************/
class AiFilterBuilderTest
{
   private static final Map<String, QFieldMetaData> FIELDS = new LinkedHashMap<>();

   static
   {
      FIELDS.put("id", new QFieldMetaData("id", QFieldType.INTEGER));
      FIELDS.put("name", new QFieldMetaData("name", QFieldType.STRING));
      FIELDS.put("price", new QFieldMetaData("price", QFieldType.DECIMAL));
      FIELDS.put("active", new QFieldMetaData("active", QFieldType.BOOLEAN));
      FIELDS.put("orderDate", new QFieldMetaData("orderDate", QFieldType.DATE));
      FIELDS.put("createDate", new QFieldMetaData("createDate", QFieldType.DATE_TIME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Object> criterion(String field, String operator, Object... values)
   {
      Map<String, Object> criterion = new LinkedHashMap<>();
      criterion.put("field", field);
      criterion.put("operator", operator);
      criterion.put("values", new ArrayList<>(List.of(values)));
      return (criterion);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBuild() throws Exception
   {
      assertThat(AiFilterBuilder.build(null, FIELDS).getCriteria()).isEmpty();

      Map<String, Object> input = new LinkedHashMap<>();
      input.put("booleanOperator", "or");
      input.put("criteria", List.of(criterion("id", "in", 1, "2"), criterion("price", "GREATER_THAN", 9.5), criterion("active", "EQUALS", true), criterion("name", "IS_BLANK")));
      input.put("subFilters", List.of(Map.of("booleanOperator", "AND", "criteria", List.of(criterion("createDate", "GREATER_THAN", "NOW-7 DAYS"), criterion("orderDate", "EQUALS", "THIS_MONTH")))));

      QQueryFilter filter = AiFilterBuilder.build(input, FIELDS);
      assertEquals(QQueryFilter.BooleanOperator.OR, filter.getBooleanOperator());
      assertEquals(4, filter.getCriteria().size());

      QFilterCriteria inCriterion = filter.getCriteria().get(0);
      assertEquals(QCriteriaOperator.IN, inCriterion.getOperator());
      assertEquals(List.of(1, 2), inCriterion.getValues());
      assertEquals(new BigDecimal("9.5"), filter.getCriteria().get(1).getValues().get(0));
      assertEquals(true, filter.getCriteria().get(2).getValues().get(0));
      assertThat(filter.getCriteria().get(3).getValues()).isEmpty();

      QQueryFilter subFilter = filter.getSubFilters().get(0);
      assertEquals(QQueryFilter.BooleanOperator.AND, subFilter.getBooleanOperator());
      assertInstanceOf(NowWithOffset.class, subFilter.getCriteria().get(0).getValues().get(0));
      assertInstanceOf(ThisOrLastPeriod.class, subFilter.getCriteria().get(1).getValues().get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRelativeDateExpressions() throws Exception
   {
      assertInstanceOf(Now.class, AiFilterBuilder.parseRelativeDateExpression("now", QFieldType.DATE_TIME));
      assertInstanceOf(NowWithOffset.class, AiFilterBuilder.parseRelativeDateExpression("NOW+2 HOURS", QFieldType.DATE_TIME));
      assertInstanceOf(NowWithOffset.class, AiFilterBuilder.parseRelativeDateExpression("NOW-1MINUTE", QFieldType.DATE_TIME));
      assertInstanceOf(NowWithOffset.class, AiFilterBuilder.parseRelativeDateExpression("NOW-3 WEEKS", QFieldType.DATE));
      assertInstanceOf(NowWithOffset.class, AiFilterBuilder.parseRelativeDateExpression("NOW+1 YEAR", QFieldType.DATE));
      assertInstanceOf(NowWithOffset.class, AiFilterBuilder.parseRelativeDateExpression("NOW-1 MONTHS", QFieldType.DATE));
      assertInstanceOf(ThisOrLastPeriod.class, AiFilterBuilder.parseRelativeDateExpression("last_week", QFieldType.DATE));
      assertInstanceOf(ThisOrLastPeriod.class, AiFilterBuilder.parseRelativeDateExpression("THIS_HOUR", QFieldType.DATE_TIME));
      assertInstanceOf(ThisOrLastPeriod.class, AiFilterBuilder.parseRelativeDateExpression("LAST_YEAR", QFieldType.DATE_TIME));
      assertInstanceOf(ThisOrLastPeriod.class, AiFilterBuilder.parseRelativeDateExpression("THIS_DAY", QFieldType.DATE_TIME));
      assertNull(AiFilterBuilder.parseRelativeDateExpression("2026-01-01", QFieldType.DATE));

      assertThatThrownBy(() -> AiFilterBuilder.parseRelativeDateExpression("NOW-2 HOURS", QFieldType.DATE)).isInstanceOf(AiToolException.class).hasMessageContaining("not valid for a date field");
      assertThatThrownBy(() -> AiFilterBuilder.parseRelativeDateExpression("THIS_HOUR", QFieldType.DATE)).isInstanceOf(AiToolException.class);

      assertEquals("2026-01-01", String.valueOf(AiFilterBuilder.convertValue(FIELDS.get("orderDate"), "2026-01-01")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalid()
   {
      assertInvalid(Map.of("criteria", List.of(criterion("nope", "EQUALS", 1))), "Unknown field 'nope'");
      assertInvalid(Map.of("criteria", List.of(Map.of("field", "id", "operator", "EQUALS", "values", List.of(1), "extra", 1))), "Unknown property 'extra'");
      assertInvalid(Map.of("bogus", 1), "Unknown property 'bogus'");
      assertInvalid(Map.of("criteria", List.of(Map.of("field", "id", "operator", "EQUALS", "values", 1))), "must be a list");
      assertInvalid(Map.of("criteria", new ArrayList<>(java.util.Arrays.asList((Object) null))), "may not contain nulls");
      assertInvalid(Map.of("criteria", List.of(criterion("id", "EQUALS", Map.of()))), "strings, numbers or booleans");
      assertInvalid(Map.of("subFilters", "x"), "must be a list");
      assertInvalid(Map.of("subFilters", new ArrayList<>(java.util.Arrays.asList((Object) null))), "may not contain nulls");
      assertInvalid(Map.of("subFilters", java.util.Collections.nCopies(11, Map.of())), "at most 10 sub-filters");

      List<Map<String, Object>> many = new ArrayList<>();
      for(int i = 0; i < 26; i++)
      {
         many.add(criterion("id", "EQUALS", i));
      }
      assertInvalid(Map.of("criteria", many), "at most 25 criteria");

      List<Object> tooManyValues = new ArrayList<>();
      for(int i = 0; i < 101; i++)
      {
         tooManyValues.add(i);
      }
      assertInvalid(Map.of("criteria", List.of(Map.of("field", "id", "operator", "IN", "values", tooManyValues))), "between 1 and 100");

      Map<String, Object> nullValue = new LinkedHashMap<>();
      nullValue.put("field", "id");
      nullValue.put("operator", "EQUALS");
      nullValue.put("values", new ArrayList<>(java.util.Arrays.asList((Object) null)));
      assertInvalid(Map.of("criteria", List.of(nullValue)), "may not be null");

      assertThatThrownBy(() -> AiFilterBuilder.build("not a map", FIELDS)).isInstanceOf(AiToolException.class).satisfies(e -> assertEquals(AiToolOutcome.INVALID_INPUT, ((AiToolException) e).getOutcome()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertInvalid(Map<String, Object> filterInput, String messagePart)
   {
      assertThatThrownBy(() -> AiFilterBuilder.build(filterInput, FIELDS))
         .isInstanceOf(AiToolException.class)
         .hasMessageContaining(messagePart);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSchemaDefinitions()
   {
      Map<String, Object> definitions = AiFilterBuilder.buildSchemaDefinitions();
      assertEquals(List.of("QueryFilter", "SubFilter", "Criterion"), new ArrayList<>(definitions.keySet()));
   }
}
