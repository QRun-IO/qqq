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


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Aggregate cardinality, active field availability and READ authorization
 ** controls against canonical Field Lab / Person / Pet rows.
 *******************************************************************************/
class SampleAggregatePrivacyContractTest
{
   private QInstance instance;
   private Map<String, List<List<String>>> before;



   /*******************************************************************************
    ** Canonical physical rows distinguish private values, NULL and cardinality.
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
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,decimal_value,password_value) VALUES "
            + "(1,'Privacy Alpha',10,1.25,'private-zebra'),(2,'Privacy Beta',20,2.50,'private-apple'),(3,'Privacy Gamma',NULL,NULL,NULL)"));
      }
      assertEquals(QFieldType.PASSWORD, instance.getTable("fieldLab").getField("passwordValue").getType());
      instance.getTable("fieldLab").getField("longValue").setIsHidden(true);
      before = snapshot();
   }



   /*******************************************************************************
    ** All native columns remain unchanged, including unrelated canonical tables.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         assertEquals(before, snapshot());
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Heavy-only fields are an explicit computation choice, not a private value.
    *******************************************************************************/
   @Test
   void testHeavyOnlyExplicitValuesAndGroupsRemainSupported() throws Exception
   {
      instance.getTable("fieldLab").getField("decimalValue").setIsHeavy(true);
      assertNative(input(op("decimalValue", AggregateOperator.SUM), op("decimalValue", AggregateOperator.MIN), op("decimalValue", AggregateOperator.MAX)),
         "SELECT SUM(decimal_value),MIN(decimal_value),MAX(decimal_value) FROM field_lab");
      assertNative(grouped("decimalValue", QFieldType.DECIMAL), "SELECT decimal_value,COUNT(id) FROM field_lab GROUP BY decimal_value ORDER BY decimal_value");
   }



   /*******************************************************************************
    ** Available private counts still honor native READ locks and their NULL rules.
    *******************************************************************************/
   @Test
   void testPrivateCountsRetainReadAuthorization() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("aggregatePrivacyOwner"));
      instance.getTable("fieldLab").withRecordSecurityLock(new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("aggregatePrivacyOwner")
         .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      QContext.setQSession(new QSession().withSecurityKeyValue("aggregatePrivacyOwner", 10L));
      assertNative(input(op("longValue", AggregateOperator.COUNT), op("passwordValue", AggregateOperator.COUNT_DISTINCT)),
         "SELECT COUNT(long_value),COUNT(DISTINCT password_value) FROM field_lab WHERE long_value=10");
   }



   /*******************************************************************************
    ** COUNT is not a way to address removed or nonselectable fields, regardless
    ** of their original privacy flags or the restored native structural metadata.
    *******************************************************************************/
   @Test
   void testUnavailableFieldsRemainRejectedForCounts() throws Exception
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(RemovePrivateFields.class));
      instance.getTable("fieldLab").withVirtualField(virtual("unavailableLength", QFieldType.INTEGER).withIsQuerySelectable(false));
      assertAll(
         () -> assertDenied(input(op("longValue", AggregateOperator.COUNT))),
         () -> assertDenied(input(op("passwordValue", AggregateOperator.COUNT_DISTINCT))),
         () -> assertDenied(input(op("unavailableLength", AggregateOperator.COUNT))),
         () -> assertDenied(people(op("animal.name", AggregateOperator.COUNT))),
         () -> assertNative(input(op("longValue", AggregateOperator.COUNT), op("passwordValue", AggregateOperator.COUNT_DISTINCT)).withInputSource(QInputSource.SYSTEM),
            "SELECT COUNT(long_value),COUNT(DISTINCT password_value) FROM field_lab"));
   }



   /*******************************************************************************
    ** Native SQL errors must not masquerade as active-field validation failures.
    *******************************************************************************/
   private void assertDenied(AggregateInput input)
   {
      String canonicalBefore = metadata();
      String context = input.getTableName() + " aggregates=" + input.getAggregates().stream().map(aggregate -> aggregate.getFieldName() + "/" + aggregate.getOperator()).toList()
         + " groups=" + input.getGroupBys().stream().map(group -> group.getFieldName() + "/" + group.getType()).toList();
      QException failure = assertThrows(QException.class, () -> new AggregateAction().execute(input), context);
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         assertFalse(cause instanceof SQLException, "Privacy rejection must precede native SQL execution errors");
         assertFalse(cause instanceof NullPointerException, "Malformed native execution must not stand in for policy validation");
         assertFalse(String.valueOf(cause.getMessage()).contains("private-zebra"));
         assertFalse(String.valueOf(cause.getMessage()).contains("private-apple"));
      }
      assertEquals(canonicalBefore, metadata());
   }



   /*******************************************************************************
    ** Compare exact returned descriptors and all values to independent native SQL.
    *******************************************************************************/
   private AggregateOutput assertNative(AggregateInput input, String sql) throws Exception
   {
      String canonicalBefore = metadata();
      AggregateOutput output = new AggregateAction().execute(input);
      List<List<String>> actual = new ArrayList<>();
      for(AggregateResult result : output.getResults())
      {
         assertEquals(new LinkedHashSet<>(input.getAggregates()), result.getAggregateValues().keySet());
         assertEquals(new LinkedHashSet<>(input.getGroupBys()), result.getGroupByValues().keySet());
         List<String> row = new ArrayList<>();
         input.getGroupBys().forEach(group -> row.add(value(result.getGroupByValue(group))));
         input.getAggregates().forEach(aggregate -> row.add(value(result.getAggregateValue(aggregate))));
         actual.add(row);
      }
      List<List<String>> expected = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(value(result.getObject(column)));
            }
            expected.add(row);
         }
      }
      assertEquals(expected, actual);
      assertEquals(canonicalBefore, metadata());
      return output;
   }



   /*******************************************************************************
    ** Exact native rows include unrelated canonical records and every column.
    *******************************************************************************/
   private Map<String, List<List<String>>> snapshot() throws Exception
   {
      Map<String, List<List<String>>> result = new LinkedHashMap<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         for(String table : List.of("field_lab", "person", "pet", "pet_note"))
         {
            List<List<String>> rows = new ArrayList<>();
            try(ResultSet values = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id"))
            {
               while(values.next())
               {
                  List<String> row = new ArrayList<>();
                  for(int column = 1; column <= values.getMetaData().getColumnCount(); column++)
                  {
                     row.add(values.getString(column));
                  }
                  rows.add(row);
               }
            }
            result.put(table, rows);
         }
      }
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String metadata()
   {
      return JsonUtils.toJson(List.of(instance.getTable("fieldLab"), instance.getTable("person"), instance.getTable("pet"), instance.getJoins()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput input(Aggregate... aggregates)
   {
      return new AggregateInput("fieldLab").withAggregates(List.of(aggregates)).withInputSource(QInputSource.USER);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput grouped(String field, QFieldType type)
   {
      GroupBy group = new GroupBy(type, field);
      return input(op("id", AggregateOperator.COUNT)).withGroupBy(group).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(group)));
   }



   /*******************************************************************************
    ** The registered Person/Pet query join is independent of association metadata.
    *******************************************************************************/
   private AggregateInput people(Aggregate... aggregates)
   {
      return new AggregateInput("person").withAggregates(List.of(aggregates)).withInputSource(QInputSource.USER)
         .withQueryJoin(new QueryJoin("pet").withAlias("animal").withSelect(false).withType(QueryJoin.Type.INNER));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Aggregate op(String field, AggregateOperator operator)
   {
      return new Aggregate(field, operator);
   }



   /*******************************************************************************
    ** Native function expressions are actual computed operands, not injected data.
    *******************************************************************************/
   private static QVirtualFieldMetaData virtual(String name, QFieldType type)
   {
      return new QVirtualFieldMetaData(name, type).withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction().withFieldName("name").withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER));
   }



   /*******************************************************************************
    ** Numeric scale is normalized; NULL, empty text and actual private text differ.
    *******************************************************************************/
   private static String value(Object value)
   {
      return value instanceof Number ? new BigDecimal(value.toString()).stripTrailingZeros().toPlainString() : value == null ? null : value.toString();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws SQLException
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    ** USER can count available fields without receiving their private values.
    *******************************************************************************/
   public static class PrivateJoinedFields implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !"pet".equals(input.getTableName()))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getField("name").setType(QFieldType.PASSWORD);
         table.getField("speciesId").setIsHidden(true);
         return table;
      }
   }



   /*******************************************************************************
    ** No field privacy exception overrides public metadata availability.
    *******************************************************************************/
   public static class RemovePrivateFields implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !("fieldLab".equals(input.getTableName()) || "pet".equals(input.getTableName())))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         if("fieldLab".equals(input.getTableName()))
         {
            table.getFields().remove("longValue");
            table.getFields().remove("passwordValue");
         }
         else
         {
            table.getFields().remove("name");
         }
         return table;
      }
   }
}
