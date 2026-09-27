/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** The running sample exposes the same pet species enum as a countable table.
 *******************************************************************************/
class SampleEnumerationContractTest
{
   private static final String TABLE = SampleMetaDataProvider.PetSpecies.NAME;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Count ignores a query page while retaining selection and caller ownership.
    *******************************************************************************/
   @Test
   void testEnumerationCountIgnoresPagination() throws Exception
   {
      assertEquals(2, CountAction.execute(TABLE, null));
      QQueryFilter page = new QQueryFilter().withSkip(1).withLimit(1);
      assertEquals(2, CountAction.execute(TABLE, page));
      assertEquals(1, QueryAction.execute(TABLE, page).size());
      assertEquals(1, page.getSkip());
      assertEquals(1, page.getLimit());

      QQueryFilter selected = new QQueryFilter()
         .withCriteria(new QFilterCriteria("possibleValueLabel", QCriteriaOperator.EQUALS, "Dog"))
         .withSkip(10).withLimit(0);
      assertEquals(1, CountAction.execute(TABLE, selected));
      assertEquals(0, QueryAction.execute(TABLE, selected).size());
      assertEquals(10, selected.getSkip());
      assertEquals(0, selected.getLimit());
      assertEquals(List.of("Dog"), selected.getCriteria().get(0).getValues());
      assertEquals(0, CountAction.execute(TABLE, new QQueryFilter()
         .withCriteria(new QFilterCriteria("possibleValueLabel", QCriteriaOperator.EQUALS, "No such species"))));
   }



   /*******************************************************************************
    ** A valid request reaches the adapter's explicit unsupported-action boundary.
    *******************************************************************************/
   @Test
   void testEnumerationAggregateRefusesUnsupportedDispatch() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      QSession session = QContext.getQSession();
      String metadata = JsonUtils.toJson(instance.getTable(TABLE));
      String records = JsonUtils.toJson(QueryAction.execute(TABLE, null));
      assertEquals(2, CountAction.execute(TABLE, null));
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         QQueryFilter filter = new QQueryFilter()
            .withCriteria(new QFilterCriteria("possibleValueLabel", QCriteriaOperator.EQUALS, "Dog"));
         Aggregate aggregate = new Aggregate("possibleValueId", AggregateOperator.COUNT);
         AggregateInput input = new AggregateInput().withTableName(TABLE).withInputSource(source)
            .withFilter(filter).withAggregate(aggregate);
         assertEquals("Aggregate is not implemented in this module: EnumerationBackendModule",
            assertThrows(IllegalStateException.class, () -> new AggregateAction().execute(input)).getMessage());
         assertEquals(List.of("Dog"), filter.getCriteria().get(0).getValues());
         assertEquals(List.of(aggregate), input.getAggregates());
         assertSame(source, input.getInputSource());
         assertSame(instance, QContext.getQInstance());
         assertSame(session, QContext.getQSession());
         assertEquals(metadata, JsonUtils.toJson(instance.getTable(TABLE)));
         assertEquals(records, JsonUtils.toJson(QueryAction.execute(TABLE, null)));
         assertEquals(1, CountAction.execute(TABLE, filter));
      }
   }
}
