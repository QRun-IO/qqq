/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.adapters;


import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for JsonQQueryFilterAdapter 
 *******************************************************************************/
class QQueryFilterJsonAdapterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QQueryFilterJsonAdapter adapter = new QQueryFilterJsonAdapter();

      QQueryFilter expected = new QQueryFilter();
      assertThat(adapter.jsonToQQueryFilter(null)).usingRecursiveComparison().isEqualTo(expected);
      assertThat(adapter.jsonToQQueryFilter("")).usingRecursiveComparison().isEqualTo(expected);
      assertThat(adapter.jsonToQQueryFilter(" ")).usingRecursiveComparison().isEqualTo(expected);
      assertThat(adapter.jsonToQQueryFilter("{}")).usingRecursiveComparison().isEqualTo(expected);

      expected.withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, "1"));
      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "EQUALS", "values": ["1"]}]}
         """)).usingRecursiveComparison().isEqualTo(expected);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJsonErrors()
   {
      QQueryFilterJsonAdapter adapter = new QQueryFilterJsonAdapter();
      assertThatThrownBy(() -> adapter.jsonToQQueryFilter("not json")).isInstanceOf(QException.class).hasMessageContaining("Error converting JSON String to QQueryFilter");
      assertThatThrownBy(() -> adapter.jsonToQQueryFilter("""
         {"criteria": {"fieldName": "id", "operator": "EQUALS", "values": ["1"]}}
         """)).isInstanceOf(QException.class);

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDiscardCriteriaWithEmptyValueListsForOperatorsThatExpectValues() throws QException
   {
      QQueryFilterJsonAdapter adapter = new QQueryFilterJsonAdapter();

      //////////////////////////////////////////////////////////////////////////////////////
      // these cases should all return an empty filter - the only criteria gets discarded //
      //////////////////////////////////////////////////////////////////////////////////////
      QQueryFilter expected = new QQueryFilter();
      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "EQUALS", "values": [""]}]}
         """)).usingRecursiveComparison().isEqualTo(expected);

      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "EQUALS", "values": []}]}
         """)).usingRecursiveComparison().isEqualTo(expected);

      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "EQUALS"}]}
         """)).usingRecursiveComparison().isEqualTo(expected);

      ///////////////////////////////////
      // cases that should not discard //
      ///////////////////////////////////
      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "EQUALS", "values": ["1"]}]}
         """)).usingRecursiveComparison().isEqualTo(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, "1")));

      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "IN", "values": ["1", "2"]}]}
         """)).usingRecursiveComparison().isEqualTo(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, "1", "2")));

      /////////////////////////////////////////////////////////////////////////////////////
      // for these operator, values aren't required, so the criteria should come through //
      /////////////////////////////////////////////////////////////////////////////////////
      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "IS_BLANK", "values": [""]}]}
         """)).usingRecursiveComparison().isEqualTo(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IS_BLANK, "")));

      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "IS_NOT_BLANK", "values": []}]}
         """)).usingRecursiveComparison().isEqualTo(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IS_NOT_BLANK).withValues(new ArrayList<>())));

      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "TRUE"}]}
         """)).usingRecursiveComparison().isEqualTo(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.TRUE).withValues(null)));

      /////////////////////////////////////////////////////////////
      // set the adapter to not discard such criteria and re-run //
      /////////////////////////////////////////////////////////////
      adapter.setDiscardCriteriaWithEmptyValueListsForOperatorsThatExpectValues(false);
      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "EQUALS", "values": [""]}]}
         """)).usingRecursiveComparison().isEqualTo(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, "")));

      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "EQUALS", "values": []}]}
         """)).usingRecursiveComparison().isEqualTo(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS).withValues(new ArrayList<>())));

      assertThat(adapter.jsonToQQueryFilter("""
         {"criteria": [{"fieldName": "id", "operator": "EQUALS"}]}
         """)).usingRecursiveComparison().isEqualTo(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS).withValues(null)));
   }

}