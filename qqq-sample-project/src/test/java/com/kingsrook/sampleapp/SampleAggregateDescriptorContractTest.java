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


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Canonical H2 group-only and native descriptor/sorting controls.
 *******************************************************************************/
class SampleAggregateDescriptorContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private Map<String, List<List<String>>> before;
   private String metadataBefore;



   /*******************************************************************************
    ** Native rows deliberately distinguish null, empty text, zero, duplicate
    ** values, group counts and string-expression totals.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(7, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,unchanged_value) VALUES "
            + "(1,'Alpha',10,'red'),(2,'Beta',10,'red'),(3,'Gamma',30,'blue'),(4,'Pi',40,'blue'),"
            + "(5,'E',0,''),(6,'Zeta',NULL,NULL),(7,'Eta',NULL,NULL)"));
      }
      before = snapshot();
      metadataBefore = metadata();
   }



   /*******************************************************************************
    ** Every native column and original table definition survives reads/errors.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         assertAll(() -> assertEquals(before, snapshot()), () -> assertEquals(metadataBefore, metadata()));
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Sort descriptors use value equality rather than object identity. Explicit
    ** result conversion remains independent of the native ordering expression.
    *******************************************************************************/
   @Test
   void testEquivalentTypedSortDescriptorsAndExplicitResultTypes() throws Exception
   {
      Aggregate count = count().withFieldType(QFieldType.LONG);
      Aggregate sameCount = count().withFieldType(QFieldType.LONG);
      GroupBy group = new GroupBy(QFieldType.STRING, "unchangedValue", "COALESCE(%s, '<null>')");
      GroupBy sameGroup = new GroupBy(QFieldType.STRING, "unchangedValue", "COALESCE(%s, '<null>')");
      assertNotSame(count, sameCount);
      assertEquals(count, sameCount);
      assertNotSame(group, sameGroup);
      assertEquals(group, sameGroup);
      AggregateInput request = input(count).withGroupBy(group).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(sameCount, false))
         .withOrderBy(new QFilterOrderByGroupBy(sameGroup)));
      AggregateOutput output = assertNative(request,
         "SELECT COALESCE(unchanged_value,'<null>'),COUNT(id) FROM field_lab GROUP BY COALESCE(unchanged_value,'<null>') ORDER BY COUNT(id) DESC,COALESCE(unchanged_value,'<null>')");
      for(AggregateResult result : output.getResults())
      {
         assertInstanceOf(Long.class, result.getAggregateValue(count));
         assertInstanceOf(String.class, result.getGroupByValue(group));
      }
   }



   /*******************************************************************************
    ** Native H2 accepts ordinary grouped-field ordering and an aggregate used
    ** only for sorting. These are positive controls, not provider-wide promises.
    *******************************************************************************/
   @Test
   void testNativePlainGroupAndUnselectedAggregateSortControls() throws Exception
   {
      assertNative(input(count()).withGroupBy(group()).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("unchangedValue", false))),
         "SELECT unchanged_value,COUNT(id) FROM field_lab GROUP BY unchanged_value ORDER BY unchanged_value DESC");
      Aggregate unselectedSum = new Aggregate("longValue", AggregateOperator.SUM);
      assertNative(input(count()).withGroupBy(group()).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(unselectedSum, false))
         .withOrderBy(new QFilterOrderByGroupBy(group()))),
         "SELECT unchanged_value,COUNT(id) FROM field_lab GROUP BY unchanged_value ORDER BY SUM(long_value) DESC,unchanged_value");
   }



   /*******************************************************************************
    ** Rejection must occur at the action boundary without a published output.
    *******************************************************************************/
   private void assertRejected(AggregateInput input)
   {
      QException exception = assertThrows(QException.class, () -> new AggregateAction().execute(input), "Invalid Aggregate descriptor");
      assertNull(exception.getCause(), "Malformed descriptors must fail common validation, not a wrapped native error");
   }



   /*******************************************************************************
    ** Exact group/aggregate keysets and native scalar values are checked even for
    ** group-only requests whose aggregate list was not supplied.
    *******************************************************************************/
   private AggregateOutput assertNative(AggregateInput input, String query) throws Exception
   {
      List<Aggregate> aggregates = input.getAggregates() == null ? List.of() : new ArrayList<>(input.getAggregates());
      List<GroupBy> groups = input.getGroupBys() == null ? List.of() : new ArrayList<>(input.getGroupBys());
      AggregateOutput output = new AggregateAction().execute(input);
      List<List<String>> actual = new ArrayList<>();
      for(AggregateResult result : output.getResults())
      {
         assertEquals(new LinkedHashSet<>(aggregates), result.getAggregateValues().keySet());
         assertEquals(new LinkedHashSet<>(groups), result.getGroupByValues().keySet());
         List<String> row = new ArrayList<>();
         groups.forEach(group -> row.add(value(result.getGroupByValue(group))));
         aggregates.forEach(aggregate -> row.add(value(result.getAggregateValue(aggregate))));
         actual.add(row);
      }
      assertEquals(sql(query), actual);
      return output;
   }



   /*******************************************************************************
    ** Fixed test SQL reads the independently seeded native values directly.
    *******************************************************************************/
   private List<List<String>> sql(String query) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
            {
               row.add(value(result.getObject(index)));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** All four canonical native tables are compared independently of Aggregate.
    *******************************************************************************/
   private Map<String, List<List<String>>> snapshot() throws Exception
   {
      Map<String, List<List<String>>> tables = new LinkedHashMap<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         for(String table : List.of("field_lab", "person", "pet", "pet_note"))
         {
            List<List<String>> rows = new ArrayList<>();
            try(ResultSet result = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id"))
            {
               while(result.next())
               {
                  List<String> row = new ArrayList<>();
                  for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
                  {
                     row.add("BLOB_VALUE".equals(result.getMetaData().getColumnLabel(index)) && result.getBytes(index) != null
                        ? Base64.getEncoder().encodeToString(result.getBytes(index)) : result.getString(index));
                  }
                  rows.add(row);
               }
            }
            tables.put(table, rows);
         }
      }
      return tables;
   }



   /*******************************************************************************
    ** Numeric scale normalization never collapses null, zero and empty text.
    *******************************************************************************/
   private static String value(Object value)
   {
      if(value == null)
      {
         return null;
      }
      return value instanceof Number ? new BigDecimal(value.toString()).stripTrailingZeros().toPlainString() : value.toString();
   }



   /*******************************************************************************
    ** Public USER input is explicit, including group-only empty-list requests.
    *******************************************************************************/
   private AggregateInput input(Aggregate... aggregates)
   {
      return new AggregateInput(TABLE).withAggregates(List.of(aggregates)).withInputSource(QInputSource.USER);
   }



   /*******************************************************************************
    ** Nullable physical text is the canonical group field.
    *******************************************************************************/
   private GroupBy group()
   {
      return new GroupBy(QFieldType.STRING, "unchangedValue");
   }



   /*******************************************************************************
    ** COUNT's result type is intentionally left unspecified by default.
    *******************************************************************************/
   private Aggregate count()
   {
      return new Aggregate("id", AggregateOperator.COUNT);
   }



   /*******************************************************************************
    ** A real empty native source exercises validation independently of row count.
    *******************************************************************************/
   private QQueryFilter emptyFilter()
   {
      return new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -1));
   }



   /*******************************************************************************
    ** Only canonical tables used by these probes are included in metadata checks.
    *******************************************************************************/
   private String metadata()
   {
      return JsonUtils.toJson(List.of(instance.getTable(TABLE), instance.getTable("person"), instance.getTable("pet"), instance.getTable("petNote")));
   }



   /*******************************************************************************
    ** Opened native connections remain fixture-owned and are always closed.
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }
}
