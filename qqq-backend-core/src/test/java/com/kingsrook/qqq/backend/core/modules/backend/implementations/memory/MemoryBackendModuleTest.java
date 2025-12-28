/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostQueryCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for MemoryBackendModule
 *******************************************************************************/
class MemoryBackendModuleTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfter()
   {
      MemoryRecordStore.getInstance().reset();
      MemoryRecordStore.resetStatistics();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFullCRUD() throws QException
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);
      QSession       session   = new QSession();

      /////////////////////////
      // do an initial count //
      /////////////////////////
      CountInput countInput = new CountInput();
      countInput.setTableName(table.getName());
      assertEquals(0, new CountAction().execute(countInput).getCount());

      //////////////////
      // do an insert //
      //////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(table.getName());
      insertInput.setRecords(getTestRecords(table));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(3, insertOutput.getRecords().size());
      assertTrue(insertOutput.getRecords().stream().allMatch(r -> r.getValue("id") != null));
      assertTrue(insertOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(1)));
      assertTrue(insertOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(2)));
      assertTrue(insertOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(3)));

      ////////////////
      // do a query //
      ////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(table.getName());
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size());
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValue("id") != null));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(1)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(2)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(3)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueString("name").equals("My Triangle")));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueString("name").equals("Your Square")));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueString("name").equals("Some Circle")));

      assertEquals(3, new CountAction().execute(countInput).getCount());

      //////////////////
      // do an update //
      //////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(table.getName());
      updateInput.setRecords(List.of(
         new QRecord()
            .withTableName(table.getName())
            .withValue("id", 1)
            .withValue("name", "Not My Triangle any more"),
         new QRecord()
            .withTableName(table.getName())
            .withValue("id", 3)
            .withValue("type", "ellipse")
      ));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      assertEquals(2, updateOutput.getRecords().size());

      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size());
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("name").equals("My Triangle")));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueString("name").equals("Not My Triangle any more")));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueString("type").equals("ellipse")));
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("type").equals("circle")));

      assertEquals(3, new CountAction().execute(countInput).getCount());

      /////////////////////////
      // do a filtered query //
      /////////////////////////
      queryInput = new QueryInput();
      queryInput.setTableName(table.getName());
      queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(1, 3))));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size());
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(1)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(3)));

      /////////////////////////
      // do a filtered count //
      /////////////////////////
      countInput.setFilter(queryInput.getFilter());
      assertEquals(2, new CountAction().execute(countInput).getCount());

      /////////////////
      // do a delete //
      /////////////////
      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(table.getName());
      deleteInput.setPrimaryKeys(List.of(1, 2));
      DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);
      assertEquals(2, deleteOutput.getDeletedRecordCount());

      assertEquals(1, new CountAction().execute(countInput).getCount());

      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size());
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueInteger("id").equals(1)));
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueInteger("id").equals(2)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(3)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVariants() throws QException
   {
      ////////////////////////////////////
      // insert our two variant options //
      ////////////////////////////////////
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "People"),
         new QRecord().withValue("id", 2).withValue("name", "Planets")
      )));

      ///////////////////////////////////////////////////////////////////////////////////////////
      // assert we fail if no variant set in session when working with a table that needs them //
      ///////////////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "Tom")
      )))).hasMessageContaining("Could not find Backend Variant information");

      ///////////////////////////////////////////////////////////
      // set the variant in session, and assert we insert once //
      ///////////////////////////////////////////////////////////
      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, 1));
      Integer peopleId1 = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(List.of(
         new QRecord().withValue("name", "Tom")
      ))).getRecords().get(0).getValueInteger("id");
      assertEquals(1, peopleId1);

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // set the other variant - make sure we insert, and get the same serial (e.g., they differ per variant) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, 2));
      Integer planetId1 = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(List.of(
         new QRecord().withValue("name", "Mercury"),
         new QRecord().withValue("name", "Venus"),
         new QRecord().withValue("name", "Earth")
      ))).getRecords().get(0).getValueInteger("id");
      assertEquals(1, planetId1);

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure counts return what we expect per-variant                                         //
      // also, put the variant ids as strings, just to make sure we're a little type-flexible there //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, "2"));
      assertEquals(3, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA)).getCount());

      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, "1"));
      assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA)).getCount());

      /////////////////////////////////////////////
      // assert we fail if trying unknown variant //
      /////////////////////////////////////////////
      QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, 4));
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "England")
      )))).hasMessageContaining("Could not find Backend Variant in table memoryVariantOptions with id '4'");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryOperators() throws QException
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);

      /////////////////////////////////////////////
      // add a date field, for testing DATE type //
      /////////////////////////////////////////////
      table.addField(new QFieldMetaData("date", QFieldType.DATE));

      QSession       session   = new QSession();

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(table.getName());
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "Square").withValue("date", LocalDate.of(1980, Month.MAY, 31)).withValue("isPolygon", true),
         new QRecord().withValue("id", 2).withValue("name", "Triangle").withValue("date", LocalDate.of(1999, Month.DECEMBER, 31)).withValue("isPolygon", true),
         new QRecord().withValue("id", 3).withValue("name", "Circle").withValue("date", LocalDate.of(2022, Month.OCTOBER, 10)).withValue("isPolygon", false)
      ));
      new InsertAction().execute(insertInput);

      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.TRUE)).size());
      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.FALSE)).size());

      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.IN, List.of(1, 2))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.IN, List.of(3, 4))).size());

      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.NOT_IN, List.of(4, 5))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.NOT_IN, List.of(2, 3))).size());

      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.EQUALS, List.of("Square"))).size());
      assertEquals("Square", queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.EQUALS, List.of("Square"))).get(0).getValue("name"));
      assertThrows(QException.class, () -> queryShapes(qInstance, table, session, new QFilterCriteria("notAFieldSoNull", QCriteriaOperator.EQUALS, List.of("Square"))));

      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.NOT_EQUALS, List.of("notFound"))).size());
      assertEquals("Square", queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.NOT_EQUALS, List.of("Triangle", "Circle"))).get(0).getValue("name"));

      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.CONTAINS, List.of("ria"))).size());
      assertEquals("Triangle", queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.CONTAINS, List.of("ria"))).get(0).getValue("name"));

      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.NOT_CONTAINS, List.of("notFound"))).size());
      assertEquals("Square", queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.NOT_CONTAINS, List.of("le"))).get(0).getValue("name"));

      assertThrows(QException.class, () -> queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.CONTAINS, List.of("ria"))));
      assertThrows(QException.class, () -> queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.CONTAINS, List.of(1))));
      assertThrows(QException.class, () -> queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.CONTAINS, List.of())));

      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.GREATER_THAN, List.of(LocalDate.of(2035, Month.JANUARY, 1)))).size());
      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.GREATER_THAN, List.of(LocalDate.of(1990, Month.JANUARY, 1)))).size());
      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.GREATER_THAN, List.of(LocalDate.of(1970, Month.JANUARY, 1)))).size());
      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, List.of(3))).size());
      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, List.of(1))).size());
      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, List.of(0))).size());

      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of(LocalDate.of(2035, Month.JANUARY, 1)))).size());
      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of(LocalDate.of(1990, Month.JANUARY, 1)))).size());
      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of(LocalDate.of(1970, Month.JANUARY, 1)))).size());
      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of(LocalDate.of(1980, Month.MAY, 31)))).size());
      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of(4))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of(3))).size());
      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of(2))).size());
      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of(1))).size());

      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.LESS_THAN, List.of(LocalDate.of(2022, Month.SEPTEMBER, 1)))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.LESS_THAN, List.of(LocalDate.of(1990, Month.JANUARY, 1)))).size());
      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.LESS_THAN, List.of(LocalDate.of(1970, Month.JANUARY, 1)))).size());
      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN, List.of(3))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN, List.of(2))).size());
      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN, List.of(1))).size());

      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of(LocalDate.of(2022, Month.SEPTEMBER, 1)))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of(LocalDate.of(1990, Month.JANUARY, 1)))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of(LocalDate.of(1980, Month.MAY, 31)))).size());
      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("date", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of(LocalDate.of(1970, Month.JANUARY, 1)))).size());
      assertEquals(3, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of(3))).size());
      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of(2))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of(1))).size());
      assertEquals(0, queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of(0))).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("name", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of("Darin"))).size());

      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("isPolygon", QCriteriaOperator.EQUALS, true)).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("isPolygon", QCriteriaOperator.EQUALS, false)).size());
      assertEquals(2, queryShapes(qInstance, table, session, new QFilterCriteria("isPolygon", QCriteriaOperator.EQUALS, "true")).size());
      assertEquals(1, queryShapes(qInstance, table, session, new QFilterCriteria("isPolygon", QCriteriaOperator.EQUALS, "false")).size());

      assertThrows(QException.class, () -> queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, List.of())));
      assertThrows(QException.class, () -> queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, List.of())));
      assertThrows(QException.class, () -> queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN, List.of())));
      assertThrows(QException.class, () -> queryShapes(qInstance, table, session, new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, List.of())));

      {
         ////////////////////////////
         // test a simple OR query //
         ////////////////////////////
         QQueryFilter filter = new QQueryFilter()
            .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, List.of("Square")))
            .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, List.of("Triangle")));
         assertEquals(2, queryShapes(qInstance, table, session, filter).size());
         assertThat(queryShapes(qInstance, table, session, filter)).anyMatch(r -> r.getValueString("name").equals("Square"));
         assertThat(queryShapes(qInstance, table, session, filter)).anyMatch(r -> r.getValueString("name").equals("Triangle"));
      }

      ///////////////////////////////////////////////////
      // null or empty query - should find all records //
      ///////////////////////////////////////////////////
      assertEquals(3, queryShapes(qInstance, table, session, (QQueryFilter) null).size());
      assertEquals(3, queryShapes(qInstance, table, session, new QQueryFilter()).size());

      {
         /////////////////////////////////
         // test a complex nested query //
         /////////////////////////////////
         QQueryFilter filter = new QQueryFilter()
            .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withSubFilters(List.of(
               new QQueryFilter()
                  .withBooleanOperator(QQueryFilter.BooleanOperator.AND)
                  .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, List.of("Square")))
                  .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, List.of(1))),
               new QQueryFilter()
                  .withBooleanOperator(QQueryFilter.BooleanOperator.AND)
                  .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, List.of("Circle")))
                  .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, List.of(3)))
            ));
         assertEquals(2, queryShapes(qInstance, table, session, filter).size());
         assertThat(queryShapes(qInstance, table, session, filter)).anyMatch(r -> r.getValueString("name").equals("Square") && r.getValueInteger("id").equals(1));
         assertThat(queryShapes(qInstance, table, session, filter)).anyMatch(r -> r.getValueString("name").equals("Circle") && r.getValueInteger("id").equals(3));
      }

      {
         /////////////////////////////////
         // test a complex nested query //
         /////////////////////////////////
         QQueryFilter filter = new QQueryFilter()
            .withBooleanOperator(QQueryFilter.BooleanOperator.AND)
            .withSubFilters(List.of(
               new QQueryFilter()
                  .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
                  .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, List.of(1)))
                  .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, List.of(3))),
               new QQueryFilter()
                  .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
                  .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, List.of("Square")))
                  .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, List.of("Circle")))
            ));
         assertEquals(2, queryShapes(qInstance, table, session, filter).size());
         assertThat(queryShapes(qInstance, table, session, filter)).anyMatch(r -> r.getValueString("name").equals("Square") && r.getValueInteger("id").equals(1));
         assertThat(queryShapes(qInstance, table, session, filter)).anyMatch(r -> r.getValueString("name").equals("Circle") && r.getValueInteger("id").equals(3));
      }

      //////////////////
      // skip & limit //
      //////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(table.getName());
         queryInput.setFilter(new QQueryFilter().withLimit(2));
         assertEquals(2, new QueryAction().execute(queryInput).getRecords().size());

         queryInput.setFilter(new QQueryFilter().withLimit(1));
         assertEquals(1, new QueryAction().execute(queryInput).getRecords().size());

         queryInput.setFilter(new QQueryFilter().withSkip(4).withLimit(3));
         assertEquals(0, new QueryAction().execute(queryInput).getRecords().size());
      }

      ///////////
      // order //
      ///////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(table.getName());
         queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("name", true)));
         assertEquals(List.of("Circle", "Square", "Triangle"), new QueryAction().execute(queryInput).getRecords().stream().map(r -> r.getValueString("name")).toList());

         queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("name", false)));
         assertEquals(List.of("Triangle", "Square", "Circle"), new QueryAction().execute(queryInput).getRecords().stream().map(r -> r.getValueString("name")).toList());

         queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id", true)));
         assertEquals(List.of(1, 2, 3), new QueryAction().execute(queryInput).getRecords().stream().map(r -> r.getValueInteger("id")).toList());

         queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id", false)));
         assertEquals(List.of(3, 2, 1), new QueryAction().execute(queryInput).getRecords().stream().map(r -> r.getValueInteger("id")).toList());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> queryShapes(QInstance qInstance, QTableMetaData table, QSession session, QFilterCriteria criteria) throws QException
   {
      return queryShapes(qInstance, table, session, new QQueryFilter().withCriteria(criteria));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> queryShapes(QInstance qInstance, QTableMetaData table, QSession session, QQueryFilter filter) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(table.getName());
      queryInput.setFilter(filter);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return queryOutput.getRecords();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSerials() throws QException
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);
      QSession       session   = new QSession();

      //////////////////
      // do an insert //
      //////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(table.getName());
      insertInput.setRecords(List.of(new QRecord().withTableName(table.getName()).withValue("name", "Shape 1")));
      new InsertAction().execute(insertInput);

      insertInput.setRecords(List.of(new QRecord().withTableName(table.getName()).withValue("name", "Shape 2")));
      new InsertAction().execute(insertInput);

      insertInput.setRecords(List.of(new QRecord().withTableName(table.getName()).withValue("name", "Shape 3")));
      new InsertAction().execute(insertInput);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(table.getName());
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(1)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(2)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(3)));

      insertInput.setRecords(List.of(new QRecord().withTableName(table.getName()).withValue("id", 4).withValue("name", "Shape 4")));
      new InsertAction().execute(insertInput);
      queryOutput = new QueryAction().execute(queryInput);
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(4)));

      insertInput.setRecords(List.of(new QRecord().withTableName(table.getName()).withValue("id", 6).withValue("name", "Shape 6")));
      new InsertAction().execute(insertInput);
      queryOutput = new QueryAction().execute(queryInput);
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(6)));

      insertInput.setRecords(List.of(new QRecord().withTableName(table.getName()).withValue("name", "Shape 7")));
      new InsertAction().execute(insertInput);
      queryOutput = new QueryAction().execute(queryInput);
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(7)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> getTestRecords(QTableMetaData table)
   {
      return List.of(
         new QRecord()
            .withTableName(table.getName())
            .withValue("name", "My Triangle")
            .withValue("type", "triangle")
            .withValue("noOfSides", 3)
            .withValue("isPolygon", true),
         new QRecord()
            .withTableName(table.getName())
            .withValue("name", "Your Square")
            .withValue("type", "square")
            .withValue("noOfSides", 4)
            .withValue("isPolygon", true),
         new QRecord()
            .withTableName(table.getName())
            .withValue("name", "Some Circle")
            .withValue("type", "circle")
            .withValue("noOfSides", null)
            .withValue("isPolygon", false)
      );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCustomizer() throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_SHAPE);

      ///////////////////////////////////
      // add a customizer to the table //
      ///////////////////////////////////
      table.withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(ShapeTestCustomizer.class));

      //////////////////
      // do an insert //
      //////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(table.getName());
      insertInput.setRecords(getTestRecords(table));
      new InsertAction().execute(insertInput);

      ///////////////////////////////////////////////////////
      // do a query - assert that the customizer did stuff //
      ///////////////////////////////////////////////////////
      ShapeTestCustomizer.invocationCount = 0;
      ShapeTestCustomizer.recordsCustomizedCount = 0;
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(table.getName());
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size());
      assertEquals(1, ShapeTestCustomizer.invocationCount);
      assertEquals(3, ShapeTestCustomizer.recordsCustomizedCount);
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(1) && r.getValueInteger("tenTimesId").equals(10)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(2) && r.getValueInteger("tenTimesId").equals(20)));
      assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValueInteger("id").equals(3) && r.getValueInteger("tenTimesId").equals(30)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAggregate() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("noOfShoes", 1).withValue("lastName", "Simpson"),
         new QRecord().withValue("noOfShoes", 2).withValue("lastName", "Simpson"),
         new QRecord().withValue("noOfShoes", 2).withValue("lastName", "Flanders"),
         new QRecord().withValue("noOfShoes", 2).withValue("lastName", "Flanders"),
         new QRecord().withValue("noOfShoes", 3).withValue("lastName", "Flanders"),
         new QRecord().withValue("noOfShoes", null).withValue("lastName", "Flanders")
      ));
      new InsertAction().execute(insertInput);

      {
         ////////////////////////////////
         // do some integer aggregates //
         ////////////////////////////////
         AggregateInput aggregateInput = new AggregateInput();
         aggregateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         aggregateInput.withAggregate(new Aggregate("noOfShoes", AggregateOperator.SUM));
         aggregateInput.withAggregate(new Aggregate("noOfShoes", AggregateOperator.COUNT));
         aggregateInput.withAggregate(new Aggregate("noOfShoes", AggregateOperator.COUNT_DISTINCT));
         aggregateInput.withAggregate(new Aggregate("noOfShoes", AggregateOperator.MIN));
         aggregateInput.withAggregate(new Aggregate("noOfShoes", AggregateOperator.MAX));
         aggregateInput.withAggregate(new Aggregate("noOfShoes", AggregateOperator.AVG));
         AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
         assertEquals(1, aggregateOutput.getResults().size());
         AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
         assertEquals(10, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
         assertEquals(5, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.COUNT)));
         assertEquals(3, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.COUNT_DISTINCT)));
         assertEquals(1, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.MIN)));
         assertEquals(3, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.MAX)));
         assertEquals(new BigDecimal(2), aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.AVG)));
      }

      {
         ///////////////////////////////
         // do some string aggregates //
         ///////////////////////////////
         AggregateInput aggregateInput = new AggregateInput();
         aggregateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         aggregateInput.withAggregate(new Aggregate("lastName", AggregateOperator.COUNT));
         aggregateInput.withAggregate(new Aggregate("lastName", AggregateOperator.COUNT_DISTINCT));
         aggregateInput.withAggregate(new Aggregate("lastName", AggregateOperator.MIN));
         aggregateInput.withAggregate(new Aggregate("lastName", AggregateOperator.MAX));

         AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
         assertEquals(1, aggregateOutput.getResults().size());
         AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
         assertEquals(6, aggregateResult.getAggregateValue(new Aggregate("lastName", AggregateOperator.COUNT)));
         assertEquals(2, aggregateResult.getAggregateValue(new Aggregate("lastName", AggregateOperator.COUNT_DISTINCT)));
         assertEquals("Flanders", aggregateResult.getAggregateValue(new Aggregate("lastName", AggregateOperator.MIN)));
         assertEquals("Simpson", aggregateResult.getAggregateValue(new Aggregate("lastName", AggregateOperator.MAX)));

         assertThatThrownBy(() -> new AggregateAction().execute(aggregateInput.withAggregates(List.of(new Aggregate("lastName", AggregateOperator.SUM))))).hasStackTraceContaining("Cannot perform SUM");
         assertThatThrownBy(() -> new AggregateAction().execute(aggregateInput.withAggregates(List.of(new Aggregate("lastName", AggregateOperator.AVG))))).hasStackTraceContaining("Cannot perform AVG");
      }

      {
         ////////////////////
         // do a group-bys //
         ////////////////////
         AggregateInput aggregateInput = new AggregateInput();
         aggregateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         aggregateInput.withAggregate(new Aggregate("noOfShoes", AggregateOperator.SUM));
         aggregateInput.withGroupBy(new GroupBy(QFieldType.STRING, "lastName"));

         {
            aggregateInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(new Aggregate("noOfShoes", AggregateOperator.SUM))));

            AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
            assertEquals(2, aggregateOutput.getResults().size());
            AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
            assertEquals(3, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
            assertEquals("Simpson", aggregateResult.getGroupByValue(new GroupBy(QFieldType.STRING, "lastName")));

            aggregateResult = aggregateOutput.getResults().get(1);
            assertEquals(7, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
            assertEquals("Flanders", aggregateResult.getGroupByValue(new GroupBy(QFieldType.STRING, "lastName")));
         }
         {
            ///////////////////////////////////////////////////////////////////////////
            // with all different versions of order-by (agg or groupBy, asc or desc) //
            ///////////////////////////////////////////////////////////////////////////
            aggregateInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(new Aggregate("noOfShoes", AggregateOperator.SUM)).withIsAscending(false)));

            AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
            assertEquals(2, aggregateOutput.getResults().size());
            AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
            assertEquals(7, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
            assertEquals("Flanders", aggregateResult.getGroupByValue(new GroupBy(QFieldType.STRING, "lastName")));

            aggregateResult = aggregateOutput.getResults().get(1);
            assertEquals(3, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
            assertEquals("Simpson", aggregateResult.getGroupByValue(new GroupBy(QFieldType.STRING, "lastName")));
         }
         {
            aggregateInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(new GroupBy(QFieldType.STRING, "lastName"))));

            AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
            assertEquals(2, aggregateOutput.getResults().size());
            AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
            assertEquals(7, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
            assertEquals("Flanders", aggregateResult.getGroupByValue(new GroupBy(QFieldType.STRING, "lastName")));

            aggregateResult = aggregateOutput.getResults().get(1);
            assertEquals(3, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
            assertEquals("Simpson", aggregateResult.getGroupByValue(new GroupBy(QFieldType.STRING, "lastName")));
         }
         {
            aggregateInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(new GroupBy(QFieldType.STRING, "lastName")).withIsAscending(false)));

            AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
            assertEquals(2, aggregateOutput.getResults().size());
            AggregateResult aggregateResult = aggregateOutput.getResults().get(0);
            assertEquals(3, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
            assertEquals("Simpson", aggregateResult.getGroupByValue(new GroupBy(QFieldType.STRING, "lastName")));

            aggregateResult = aggregateOutput.getResults().get(1);
            assertEquals(7, aggregateResult.getAggregateValue(new Aggregate("noOfShoes", AggregateOperator.SUM)));
            assertEquals("Flanders", aggregateResult.getGroupByValue(new GroupBy(QFieldType.STRING, "lastName")));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoins() throws QException
   {
      QContext.getQSession().setSecurityKeyValues(Map.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, List.of(true)));

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(List.of(
         new QRecord().withValue("id", 1),
         new QRecord().withValue("id", 2)
      )));

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM).withRecords(List.of(
         new QRecord().withValue("sku", "A").withValue("orderId", 1),
         new QRecord().withValue("sku", "B").withValue("orderId", 1),
         new QRecord().withValue("sku", "A").withValue("orderId", 2)
      )));

      QueryOutput queryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_ORDER)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_LINE_ITEM)));
      assertEquals(3, queryOutput.getRecords().size());

      CountOutput countOutput = new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_ORDER)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_LINE_ITEM))
         .withIncludeDistinctCount(true));
      assertEquals(3, countOutput.getCount());
      assertEquals(2, countOutput.getDistinctCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ShapeTestCustomizer extends AbstractPostQueryCustomizer
   {
      static int invocationCount        = 0;
      static int recordsCustomizedCount = 0;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         invocationCount++;
         for(QRecord record : records)
         {
            recordsCustomizedCount++;
            record.setValue("tenTimesId", record.getValueInteger("id") * 10);
         }
         return (records);
      }
   }
}