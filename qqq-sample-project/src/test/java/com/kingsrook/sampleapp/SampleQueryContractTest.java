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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Query examples use Field Lab's public metadata and real persisted SQL rows.
 *******************************************************************************/
class SampleQueryContractTest
{
   private static final String TABLE = FieldLabTableMetaDataProducer.NAME;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession());
      List<QRecord> inserted = InsertAction.executeForRecords(new InsertInput(TABLE).withRecords(List.of(
         new QRecord().withValue("name", "Alpha").withValue("textValue", "alpha").withValue("boundedValue", 10),
         new QRecord().withValue("name", "Alphabet").withValue("textValue", "alphabet").withValue("boundedValue", 20),
         new QRecord().withValue("name", "Beta").withValue("textValue", "beta").withValue("boundedValue", 30),
         new QRecord().withValue("name", "Empty").withValue("textValue", ""),
         new QRecord().withValue("name", "Null"))));
      for(QRecord record : inserted)
      {
         assertTrue(record.getErrors().isEmpty(), record.getErrors().toString());
      }
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
    ** Every declared operator has an independently enumerated expected result.
    *******************************************************************************/
   @Test
   void testEveryCriteriaOperator() throws Exception
   {
      Map<QFilterCriteria, Set<String>> cases = Map.ofEntries(
         entry(new QFilterCriteria("textValue", QCriteriaOperator.EQUALS, "alpha"), Set.of("Alpha")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.NOT_EQUALS, "alpha"), Set.of("Alphabet", "Beta", "Empty")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, "alpha"), Set.of("Alphabet", "Beta", "Empty", "Null")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.IN, "alpha", "beta"), Set.of("Alpha", "Beta")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.NOT_IN, "alpha", "beta"), Set.of("Alphabet", "Empty")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.IS_NULL_OR_IN, "alpha"), Set.of("Alpha", "Null")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.LIKE, "alp%"), Set.of("Alpha", "Alphabet")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.NOT_LIKE, "alp%"), Set.of("Beta", "Empty")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.STARTS_WITH, "alp"), Set.of("Alpha", "Alphabet")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.ENDS_WITH, "ta"), Set.of("Beta")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.CONTAINS, "pha"), Set.of("Alpha", "Alphabet")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.NOT_STARTS_WITH, "alp"), Set.of("Beta", "Empty")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.NOT_ENDS_WITH, "ta"), Set.of("Alpha", "Alphabet", "Empty")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.NOT_CONTAINS, "pha"), Set.of("Beta", "Empty")),
         entry(new QFilterCriteria("boundedValue", QCriteriaOperator.LESS_THAN, 20), Set.of("Alpha")),
         entry(new QFilterCriteria("boundedValue", QCriteriaOperator.LESS_THAN_OR_EQUALS, 20), Set.of("Alpha", "Alphabet")),
         entry(new QFilterCriteria("boundedValue", QCriteriaOperator.GREATER_THAN, 20), Set.of("Beta")),
         entry(new QFilterCriteria("boundedValue", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 20), Set.of("Alphabet", "Beta")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.IS_BLANK), Set.of("Empty", "Null")),
         entry(new QFilterCriteria("textValue", QCriteriaOperator.IS_NOT_BLANK), Set.of("Alpha", "Alphabet", "Beta")),
         entry(new QFilterCriteria("boundedValue", QCriteriaOperator.BETWEEN, 10, 20), Set.of("Alpha", "Alphabet")),
         entry(new QFilterCriteria("boundedValue", QCriteriaOperator.NOT_BETWEEN, 10, 20), Set.of("Beta")),
         entry(new QFilterCriteria("name", QCriteriaOperator.TRUE), Set.of("Alpha", "Alphabet", "Beta", "Empty", "Null")),
         entry(new QFilterCriteria("name", QCriteriaOperator.FALSE), Set.of()));
      assertEquals(Set.copyOf(Arrays.asList(QCriteriaOperator.values())), cases.keySet().stream().map(QFilterCriteria::getOperator).collect(Collectors.toSet()));
      for(Map.Entry<QFilterCriteria, Set<String>> testCase : cases.entrySet())
      {
         List<QRecord> records = QueryAction.execute(TABLE, new QQueryFilter(testCase.getKey()));
         assertEquals(testCase.getValue(), records.stream().map(record -> record.getValueString("name")).collect(Collectors.toSet()), testCase.getKey().getOperator().name());
         assertEquals(testCase.getValue().size(), records.size(), testCase.getKey().getOperator().name());
         assertEquals(testCase.getValue().size(), CountAction.execute(TABLE, new QQueryFilter(testCase.getKey())), testCase.getKey().getOperator().name());
      }
   }



   /*******************************************************************************
    ** Nested filters, ordering and pagination compose without changing selection.
    *******************************************************************************/
   @Test
   void testNestedFiltersOrderingAndPagination() throws Exception
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("boundedValue", QCriteriaOperator.GREATER_THAN, 15))
         .withSubFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withCriteria(new QFilterCriteria("textValue", QCriteriaOperator.STARTS_WITH, "alp"))
            .withCriteria(new QFilterCriteria("textValue", QCriteriaOperator.ENDS_WITH, "ta")))
         .withOrderBy(new QFilterOrderBy("name", false));
      assertEquals(List.of("Beta", "Alphabet"), QueryAction.execute(TABLE, filter).stream().map(record -> record.getValueString("name")).toList());
      QQueryFilter selective = new QQueryFilter(new QFilterCriteria("boundedValue", QCriteriaOperator.GREATER_THAN, 15))
         .withSubFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withCriteria(new QFilterCriteria("textValue", QCriteriaOperator.STARTS_WITH, "alp"))
            .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Null")));
      assertEquals(List.of("Alphabet"), QueryAction.execute(TABLE, selective).stream().map(record -> record.getValueString("name")).toList());
      assertEquals(2, CountAction.execute(TABLE, filter));
      assertEquals(1, CountAction.execute(TABLE, selective));
      filter.withLimit(1);
      assertEquals(List.of("Beta"), QueryAction.execute(TABLE, filter).stream().map(record -> record.getValueString("name")).toList());
      filter.withSkip(1);
      assertEquals(2, CountAction.execute(TABLE, filter));
      assertEquals(List.of("Alphabet"), QueryAction.execute(TABLE, filter).stream().map(record -> record.getValueString("name")).toList());
      filter.withSkip(10);
      assertTrue(QueryAction.execute(TABLE, filter).isEmpty());
      assertTrue(QueryAction.execute(TABLE, new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.IN))).isEmpty());
      assertEquals(5, QueryAction.execute(TABLE, new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.NOT_IN))).size());
      assertThrows(QException.class, () -> QueryAction.execute(TABLE, new QQueryFilter(new QFilterCriteria("missingField", QCriteriaOperator.EQUALS, "value"))));
      assertThrows(QException.class, () -> QueryAction.execute(TABLE, new QQueryFilter(new QFilterCriteria("boundedValue", QCriteriaOperator.EQUALS, "invalid-number"))));
   }



   /*******************************************************************************
    ** Zero, bounded and beyond-end windows preserve exact ordered populations.
    *******************************************************************************/
   @Test
   void testZeroAndBoundedLimitsMatchNativeOrderedRows() throws Exception
   {
      List<String> nativeNames = nativeOrderedNames();
      List<Executable> cases = new ArrayList<>();
      for(Integer limit : List.of(0, 1, 3, 10))
      {
         for(Integer skip : Arrays.asList(null, 0, 1, 3, 5, 10))
         {
            cases.add(() ->
            {
               int start = Math.min(skip == null ? 0 : skip, nativeNames.size());
               int end = Math.min(start + limit, nativeNames.size());
               QQueryFilter filter = new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withLimit(limit).withSkip(skip);
               assertEquals(nativeNames.subList(start, end), QueryAction.execute(TABLE, filter).stream()
                  .map(record -> record.getValueString("name")).toList(), "limit=" + limit + ", skip=" + skip);
            });
         }
      }
      assertAll(cases);
      assertEquals(nativeNames, nativeOrderedNames());
   }



   /*******************************************************************************
    ** Native SQL is independent of QueryAction's pagination and output processing.
    *******************************************************************************/
   private List<String> nativeOrderedNames() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement();
          ResultSet rows = statement.executeQuery("SELECT name FROM field_lab ORDER BY id"))
      {
         List<String> names = new ArrayList<>();
         while(rows.next())
         {
            names.add(rows.getString(1));
         }
         return names;
      }
   }
}
