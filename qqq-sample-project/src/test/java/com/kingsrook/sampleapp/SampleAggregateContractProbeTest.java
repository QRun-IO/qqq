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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
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
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Common Aggregate invocation and native H2 result contracts on canonical tables.
 ** SQL formatting expressions and JDBC transaction recovery are H2-specific.
 *******************************************************************************/
class SampleAggregateContractProbeTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private Map<String, List<List<String>>> before;



   /*******************************************************************************
    ** Native NULL, empty string, zero, duplicate numeric values and distinct dates
    ** distinguish COUNT, DISTINCT and grouping without framework-written fixtures.
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
         assertEquals(6, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,decimal_value,boolean_value,unchanged_value,date_value,normalized_key,password_value) VALUES "
            + "(1,'Alpha',0,1.2500,TRUE,'red',DATE '2024-01-01','ALPHA','aggregate-secret-one'),"
            + "(2,'Beta',10,2.7500,TRUE,'red',DATE '2024-01-15','BETA','aggregate-secret-two'),"
            + "(3,'Gamma',10,2.7500,FALSE,'blue',DATE '2024-02-01','GAMMA','aggregate-secret-three'),"
            + "(4,'Delta',-4,-1.2500,FALSE,NULL,DATE '2024-02-28','DELTA',NULL),"
            + "(5,'Epsilon',NULL,NULL,NULL,'',NULL,'EPSILON',''),"
            + "(6,'Zeta',14,6.5000,NULL,NULL,DATE '2025-01-01','ZETA',NULL)"));
         try(PreparedStatement bytes = connection.prepareStatement("UPDATE field_lab SET blob_value=? WHERE id=?"))
         {
            bytes.setBytes(1, new byte[] { 0, 1, -1 });
            bytes.setInt(2, 1);
            assertEquals(1, bytes.executeUpdate());
            bytes.setBytes(1, new byte[0]);
            bytes.setInt(2, 2);
            assertEquals(1, bytes.executeUpdate());
         }
      }
      before = snapshot();
   }



   /*******************************************************************************
    ** Every probe, including rejected requests, must leave all canonical rows intact.
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
    ** All six operators, integer AVG promotion, decimals, strings and dates have
    ** independent SQL results. An explicit result type is also honored.
    *******************************************************************************/
   @Test
   void testScalarOperatorsAndExplicitResultType() throws Exception
   {
      AggregateInput input = input(
         op("id", AggregateOperator.COUNT), op("longValue", AggregateOperator.SUM), op("longValue", AggregateOperator.AVG),
         op("decimalValue", AggregateOperator.SUM), op("decimalValue", AggregateOperator.MIN), op("decimalValue", AggregateOperator.MAX),
         op("unchangedValue", AggregateOperator.MIN), op("dateValue", AggregateOperator.MAX), op("longValue", AggregateOperator.COUNT_DISTINCT),
         op("id", AggregateOperator.SUM).withFieldType(QFieldType.LONG));
      assertEquals(List.of(List.of("2025-01-01")), sql("SELECT CAST(MAX(date_value) AS VARCHAR) FROM field_lab"));
      AggregateOutput output = assertNative(input, "SELECT COUNT(id),SUM(long_value),AVG(long_value),SUM(decimal_value),MIN(decimal_value),MAX(decimal_value),MIN(unchanged_value),MAX(date_value),COUNT(DISTINCT long_value),SUM(id) FROM field_lab");
      assertEquals(1, output.getResults().size());
      assertInstanceOf(BigDecimal.class, output.getResults().get(0).getAggregateValue(input.getAggregates().get(2)));
      assertInstanceOf(Long.class, output.getResults().get(0).getAggregateValue(input.getAggregates().get(9)));
   }



   /*******************************************************************************
    ** Multiple groups preserve SQL NULL versus empty string and exact tuples.
    *******************************************************************************/
   @Test
   void testMultiGroupNullEmptyAndDuplicateValues() throws Exception
   {
      GroupBy text = new GroupBy(QFieldType.STRING, "unchangedValue");
      GroupBy bool = new GroupBy(QFieldType.BOOLEAN, "booleanValue");
      AggregateInput input = input(op("id", AggregateOperator.COUNT), op("longValue", AggregateOperator.COUNT_DISTINCT), op("decimalValue", AggregateOperator.SUM))
         .withGroupBy(text).withGroupBy(bool)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(text)).withOrderBy(new QFilterOrderByGroupBy(bool)));
      assertNative(input, "SELECT unchanged_value,boolean_value,COUNT(id),COUNT(DISTINCT long_value),SUM(decimal_value) FROM field_lab GROUP BY unchanged_value,boolean_value ORDER BY unchanged_value,boolean_value");
   }



   /*******************************************************************************
    ** No matching rows produce one scalar row with COUNT zero and other operators
    ** NULL; adding a GROUP BY produces no groups. All-null matched rows differ.
    *******************************************************************************/
   @Test
   void testEmptySelectionAndAllNullInputs() throws Exception
   {
      AggregateInput input = input(op("id", AggregateOperator.COUNT), op("longValue", AggregateOperator.SUM), op("longValue", AggregateOperator.AVG),
         op("longValue", AggregateOperator.MIN), op("longValue", AggregateOperator.MAX), op("longValue", AggregateOperator.COUNT_DISTINCT))
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -1)));
      assertEquals(1, assertNative(input, "SELECT COUNT(id),SUM(long_value),AVG(long_value),MIN(long_value),MAX(long_value),COUNT(DISTINCT long_value) FROM field_lab WHERE id=-1").getResults().size());
      input.withGroupBy(new GroupBy(QFieldType.STRING, "unchangedValue"));
      assertTrue(assertNative(input, "SELECT unchanged_value,COUNT(id),SUM(long_value),AVG(long_value),MIN(long_value),MAX(long_value),COUNT(DISTINCT long_value) FROM field_lab WHERE id=-1 GROUP BY unchanged_value").getResults().isEmpty());
      AggregateInput allNull = input(op("longValue", AggregateOperator.COUNT), op("longValue", AggregateOperator.SUM), op("longValue", AggregateOperator.AVG))
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 5)));
      assertNative(allNull, "SELECT COUNT(long_value),SUM(long_value),AVG(long_value) FROM field_lab WHERE id=5");
   }



   /*******************************************************************************
    ** Input filter values follow declared field behavior without changing the
    ** caller-owned filter or the canonical metadata.
    *******************************************************************************/
   @Test
   void testNormalizedFilterAndMetadataPreservation() throws Exception
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("normalizedKey", QCriteriaOperator.EQUALS, "  alpha  "));
      String filterJson = JsonUtils.toJson(filter);
      String tableJson = JsonUtils.toJson(instance.getTable(TABLE));
      assertNative(input(op("id", AggregateOperator.COUNT), op("decimalValue", AggregateOperator.SUM)).withFilter(filter),
         "SELECT COUNT(id),SUM(decimal_value) FROM field_lab WHERE normalized_key='ALPHA'");
      assertAll(() -> assertEquals(filterJson, JsonUtils.toJson(filter)), () -> assertEquals(tableJson, JsonUtils.toJson(instance.getTable(TABLE))));
   }



   /*******************************************************************************
    ** The caller-supplied trusted formatting expression uses native H2 syntax;
    ** this does not claim that arbitrary expression text is an HTTP-safe API.
    *******************************************************************************/
   @Test
   void testNativeDateGroupingExpressionAndOrdering() throws Exception
   {
      GroupBy month = new GroupBy(QFieldType.STRING, "dateValue", "FORMATDATETIME(%s, 'yyyy-MM')");
      AggregateInput input = input(op("id", AggregateOperator.COUNT), op("decimalValue", AggregateOperator.SUM)).withGroupBy(month)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(month)));
      assertNative(input, "SELECT FORMATDATETIME(date_value,'yyyy-MM'),COUNT(id),SUM(decimal_value) FROM field_lab GROUP BY FORMATDATETIME(date_value,'yyyy-MM') ORDER BY FORMATDATETIME(date_value,'yyyy-MM')");
   }



   /*******************************************************************************
    ** Existing native StringLength field functions work as groups and aggregate
    ** operands. Aggregate ordering and the aggregate-level limit act on groups.
    *******************************************************************************/
   @Test
   void testVirtualExpressionAggregateOrderingAndLimit() throws Exception
   {
      QVirtualFieldMetaData length = new QVirtualFieldMetaData("nameLength", QFieldType.INTEGER)
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction().withFieldName("name").withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER));
      instance.getTable(TABLE).withVirtualField(length);
      String tableJson = JsonUtils.toJson(instance.getTable(TABLE));
      GroupBy group = new GroupBy(length);
      Aggregate count = op("id", AggregateOperator.COUNT);
      AggregateInput grouped = input(count).withGroupBy(group)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(count, false)).withOrderBy(new QFilterOrderByGroupBy(group)))
         .withLimit(2);
      assertNative(grouped, "SELECT CHAR_LENGTH(name),COUNT(id) FROM field_lab GROUP BY CHAR_LENGTH(name) ORDER BY COUNT(id) DESC,CHAR_LENGTH(name) LIMIT 2");
      assertTrue(assertNative(grouped.withLimit(0), "SELECT CHAR_LENGTH(name),COUNT(id) FROM field_lab GROUP BY CHAR_LENGTH(name) ORDER BY COUNT(id) DESC,CHAR_LENGTH(name) LIMIT 0").getResults().isEmpty());
      assertNative(input(op("nameLength", AggregateOperator.SUM), op("nameLength", AggregateOperator.MIN), op("nameLength", AggregateOperator.MAX)),
         "SELECT SUM(CHAR_LENGTH(name)),MIN(CHAR_LENGTH(name)),MAX(CHAR_LENGTH(name)) FROM field_lab");
      assertEquals(tableJson, JsonUtils.toJson(instance.getTable(TABLE)));
   }



   /*******************************************************************************
    ** Native INNER/LEFT multiplicities and COUNT DISTINCT preserve the difference
    ** between parent count, non-null joined count and the number of parent owners.
    *******************************************************************************/
   @Test
   void testJoinedMultiplicityAndExactAliasGroups() throws Exception
   {
      for(QueryJoin.Type type : List.of(QueryJoin.Type.INNER, QueryJoin.Type.LEFT))
      {
         AggregateInput input = people(type, op("id", AggregateOperator.COUNT), op("animal.id", AggregateOperator.COUNT), op("id", AggregateOperator.COUNT_DISTINCT));
         assertNative(input, "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p " + type.name() + " JOIN pet a ON p.id=a.person_id");
      }
      GroupBy parent = new GroupBy(QFieldType.INTEGER, "id");
      GroupBy species = new GroupBy(QFieldType.INTEGER, "animal.speciesId");
      AggregateInput groups = people(QueryJoin.Type.LEFT, op("animal.id", AggregateOperator.COUNT)).withGroupBy(parent).withGroupBy(species)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(parent)).withOrderBy(new QFilterOrderByGroupBy(species)));
      assertNative(groups, "SELECT p.id,a.species_id,COUNT(a.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id GROUP BY p.id,a.species_id ORDER BY p.id,a.species_id");
   }



   /*******************************************************************************
    ** Unavailable operands reject without publishing an aggregate;
    ** valid calls still work afterward. Native unsupported type pairs are errors.
    *******************************************************************************/
   @Test
   void testInvalidAggregateAndGroupOperandsReject()
   {
      List<AggregateInput> invalid = List.of(
         input(op("missingField", AggregateOperator.SUM)), input(op("name", AggregateOperator.SUM)),
         input(op("id", AggregateOperator.COUNT)).withGroupBy(new GroupBy(QFieldType.STRING, "missingField")),
         input(op("id", AggregateOperator.COUNT)).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("missingField"))));
      List<Executable> checks = new ArrayList<>();
      invalid.forEach(input -> checks.add(() -> assertThrows(QException.class, () -> new AggregateAction().execute(input), JsonUtils.toJson(input))));
      checks.add(() -> assertNative(input(op("id", AggregateOperator.COUNT)), "SELECT COUNT(id) FROM field_lab"));
      assertAll(checks);
   }



   /*******************************************************************************
    ** Missing operator/field must not disappear and turn a selective request into
    ** an unrestricted scalar. A fresh valid call supplies the recovery control.
    *******************************************************************************/
   @Test
   void testMalformedFilterRejectedWithoutBroadResult()
   {
      List<QQueryFilter> invalid = List.of(
         new QQueryFilter(new QFilterCriteria("id", null, 1)),
         new QQueryFilter(new QFilterCriteria(null, QCriteriaOperator.EQUALS, 1)),
         new QQueryFilter().withCriteria((QFilterCriteria) null),
         new QQueryFilter().withSubFilters(Arrays.asList((QQueryFilter) null)));
      List<Executable> checks = new ArrayList<>();
      invalid.forEach(filter -> checks.add(() -> assertThrows(QException.class, () -> new AggregateAction().execute(input(op("id", AggregateOperator.COUNT)).withFilter(filter)))));
      checks.add(() -> assertNative(input(op("id", AggregateOperator.COUNT)).withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))), "SELECT COUNT(id) FROM field_lab WHERE id=1"));
      assertAll(checks);
   }



   /*******************************************************************************
    ** Native base and joined READ locks restrict aggregate input rows even though
    ** no QRecord materialization or post-query customizer is involved.
    *******************************************************************************/
   @Test
   void testBaseAndJoinedReadLocks() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("aggregateOwner"));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("aggregateSpecies"));
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock());
      QContext.setQSession(new QSession().withSecurityKeyValue("aggregateOwner", 10L));
      assertNative(input(op("id", AggregateOperator.COUNT), op("decimalValue", AggregateOperator.SUM)), "SELECT COUNT(id),SUM(decimal_value) FROM field_lab WHERE long_value=10");
      QContext.setQSession(new QSession());
      assertNative(input(op("id", AggregateOperator.COUNT), op("decimalValue", AggregateOperator.SUM)), "SELECT COUNT(id),SUM(decimal_value) FROM field_lab WHERE 1=0");
      instance.getTable("pet").withRecordSecurityLock(new RecordSecurityLock().withFieldName("speciesId").withSecurityKeyType("aggregateSpecies")
         .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      QContext.setQSession(new QSession().withSecurityKeyValue("aggregateSpecies", 1));
      assertNative(people(QueryJoin.Type.INNER, op("id", AggregateOperator.COUNT), op("animal.id", AggregateOperator.COUNT)),
         "SELECT COUNT(p.id),COUNT(a.id) FROM person p INNER JOIN pet a ON p.id=a.person_id WHERE a.species_id=1");
   }



   /*******************************************************************************
    ** USER field removal affects aggregate, group, filter and joined selections;
    ** SYSTEM retains access to the unchanged declared schema.
    *******************************************************************************/
   @Test
   void testUserPersonalizedFieldsAndSystemControl()
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(RemoveFields.class));
      String fieldJson = JsonUtils.toJson(instance.getTable(TABLE));
      String petJson = JsonUtils.toJson(instance.getTable("pet"));
      List<AggregateInput> invalid = List.of(
         input(op("longValue", AggregateOperator.SUM)),
         input(op("id", AggregateOperator.COUNT)).withGroupBy(new GroupBy(QFieldType.LONG, "longValue")),
         input(op("id", AggregateOperator.COUNT)).withFilter(new QQueryFilter(new QFilterCriteria("longValue", QCriteriaOperator.EQUALS, 10L))),
         people(QueryJoin.Type.INNER, op("animal.name", AggregateOperator.MIN)),
         people(QueryJoin.Type.INNER, op("id", AggregateOperator.COUNT)).withGroupBy(new GroupBy(QFieldType.STRING, "animal.name")));
      List<Executable> checks = new ArrayList<>();
      invalid.forEach(input -> checks.add(() -> assertThrows(QException.class, () -> new AggregateAction().execute(input))));
      checks.add(() -> assertNative(input(op("longValue", AggregateOperator.SUM)).withInputSource(QInputSource.SYSTEM), "SELECT SUM(long_value) FROM field_lab"));
      checks.add(() -> assertNative(people(QueryJoin.Type.INNER, op("animal.name", AggregateOperator.MIN)).withInputSource(QInputSource.SYSTEM), "SELECT MIN(a.name) FROM person p INNER JOIN pet a ON p.id=a.person_id"));
      checks.add(() -> assertEquals(fieldJson, JsonUtils.toJson(instance.getTable(TABLE))));
      checks.add(() -> assertEquals(petJson, JsonUtils.toJson(instance.getTable("pet"))));
      assertAll(checks);
   }



   /*******************************************************************************
    ** A USER-only personalized READ lock must participate in native aggregation;
    ** it must not mutate canonical metadata or constrain the SYSTEM control.
    *******************************************************************************/
   @Test
   void testPersonalizedReadLockReachesNativeAggregate() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("aggregateOwner"));
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(AddReadLock.class));
      QContext.setQSession(new QSession().withSecurityKeyValue("aggregateOwner", 10L));
      String tableJson = JsonUtils.toJson(instance.getTable(TABLE));
      assertAll(
         () -> assertNative(input(op("id", AggregateOperator.COUNT), op("decimalValue", AggregateOperator.SUM)), "SELECT COUNT(id),SUM(decimal_value) FROM field_lab WHERE long_value=10"),
         () -> assertNative(input(op("id", AggregateOperator.COUNT), op("decimalValue", AggregateOperator.SUM)).withInputSource(QInputSource.SYSTEM), "SELECT COUNT(id),SUM(decimal_value) FROM field_lab"),
         () -> assertEquals(tableJson, JsonUtils.toJson(instance.getTable(TABLE))));
   }



   /*******************************************************************************
    ** Invocation boundaries reject without requiring a backend query and recover
    ** once table/context/session is restored. No HTTP status is prescribed here.
    *******************************************************************************/
   @Test
   void testMissingInvocationStateAndRecovery() throws Exception
   {
      for(String table : Arrays.asList(null, "", "missingAggregateTable"))
      {
         assertThrows(QException.class, () -> new AggregateAction().execute(input(op("id", AggregateOperator.COUNT)).withTableName(table)));
      }
      QContext.setQSession(null);
      try
      {
         assertThrows(QException.class, () -> new AggregateAction().execute(input(op("id", AggregateOperator.COUNT))));
      }
      finally
      {
         QContext.setQSession(new QSession());
      }
      AggregateInput missingContextInput = input(op("id", AggregateOperator.COUNT));
      QContext.clear();
      try
      {
         assertThrows(QException.class, () -> new AggregateAction().execute(missingContextInput));
      }
      finally
      {
         QContext.init(instance, new QSession());
      }
      assertNative(input(op("id", AggregateOperator.COUNT)), "SELECT COUNT(id) FROM field_lab");
   }



   /*******************************************************************************
    ** The caller's uncommitted native values are read on the same connection.
    ** Success and a native SQL error leave ownership and explicit rollback intact.
    *******************************************************************************/
   @Test
   void testCallerTransactionVisibilityFailureRecoveryAndRollback() throws Exception
   {
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET long_value=1000,decimal_value=99.5000 WHERE id=1"));
         }
         AggregateInput input = input(op("longValue", AggregateOperator.SUM), op("decimalValue", AggregateOperator.MAX)).withTransaction(transaction);
         assertResult(input, new AggregateAction().execute(input), sql(connection, "SELECT SUM(long_value),MAX(decimal_value) FROM field_lab"));
         assertNative(input(op("longValue", AggregateOperator.SUM), op("decimalValue", AggregateOperator.MAX)), "SELECT SUM(long_value),MAX(decimal_value) FROM field_lab");
         assertEquals(before, snapshot());
         AggregateInput invalid = input(op("name", AggregateOperator.SUM)).withTransaction(transaction);
         QException failure = assertThrows(QException.class, () -> new AggregateAction().execute(invalid));
         Throwable cause = failure;
         while(cause != null && !(cause instanceof SQLException))
         {
            cause = cause.getCause();
         }
         assertInstanceOf(SQLException.class, cause);
         assertAll(() -> assertSame(transaction, input.getTransaction()), () -> assertSame(connection, transaction.getConnection()),
            () -> assertFalse(connection.isClosed()), () -> assertFalse(connection.getAutoCommit()));
         assertResult(input, new AggregateAction().execute(input), sql(connection, "SELECT SUM(long_value),MAX(decimal_value) FROM field_lab"));
         transaction.rollback();
         assertResult(input, new AggregateAction().execute(input), sql("SELECT SUM(long_value),MAX(decimal_value) FROM field_lab"));
         assertAll(() -> assertFalse(connection.isClosed()), () -> assertFalse(connection.getAutoCommit()));
      }
   }



   /*******************************************************************************
    ** The same Aggregate/GroupBy descriptors are returned as exact map keys; rows
    ** are checked against independently executed JDBC expressions in input order.
    *******************************************************************************/
   private AggregateOutput assertNative(AggregateInput input, String query) throws Exception
   {
      AggregateOutput output = new AggregateAction().execute(input);
      assertResult(input, output, sql(query));
      return output;
   }



   /*******************************************************************************
    ** Numeric scale is not part of SQL numeric equality, but NULL remains distinct
    ** from zero/empty text. Each individual count test separately checks its type.
    *******************************************************************************/
   private void assertResult(AggregateInput input, AggregateOutput output, List<List<String>> expected)
   {
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
      assertEquals(expected, actual);
   }



   /*******************************************************************************
    ** Only fixed test expressions and column names reach this native SQL helper.
    *******************************************************************************/
   private List<List<String>> sql(String query) throws Exception
   {
      try(Connection connection = connection())
      {
         return sql(connection, query);
      }
   }



   /*******************************************************************************
    ** Native result extraction is independent of QQQ Aggregate output conversion.
    *******************************************************************************/
   private List<List<String>> sql(Connection connection, String query) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
            {
               row.add(result.getMetaData().getColumnType(index) == Types.DATE ? result.getString(index) : value(result.getObject(index)));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Complete native rows include raw bytes and no framework display conversion.
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
    ** Numeric equivalence does not require a driver-specific decimal scale.
    *******************************************************************************/
   private static String value(Object value)
   {
      if(value == null)
      {
         return null;
      }
      if(value instanceof Number)
      {
         return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
      }
      if(value instanceof byte[] bytes)
      {
         return Base64.getEncoder().encodeToString(bytes);
      }
      return value.toString();
   }



   /*******************************************************************************
    ** Public USER input is explicit; SYSTEM controls opt in at their call sites.
    *******************************************************************************/
   private AggregateInput input(Aggregate... aggregates)
   {
      return new AggregateInput(TABLE).withAggregates(List.of(aggregates)).withInputSource(QInputSource.USER);
   }



   /*******************************************************************************
    ** Aggregate joins need not select QRecord fields to supply aggregate operands.
    *******************************************************************************/
   private AggregateInput people(QueryJoin.Type type, Aggregate... aggregates)
   {
      return input(aggregates).withTableName("person")
         .withQueryJoin(new QueryJoin("pet").withAlias("animal").withSelect(false).withType(type));
   }



   /*******************************************************************************
    ** Existing operator objects are used directly, including malformed null probes.
    *******************************************************************************/
   private static Aggregate op(String field, AggregateOperator operator)
   {
      return new Aggregate(field, operator);
   }



   /*******************************************************************************
    ** Canonical backend connection; every opened connection remains fixture-owned.
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    ** No NULL owner is permitted to become an unassigned aggregate member.
    *******************************************************************************/
   private static RecordSecurityLock ownerLock()
   {
      return new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("aggregateOwner")
         .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY);
   }



   /*******************************************************************************
    ** USER-only active field policy leaves canonical objects untouched.
    *******************************************************************************/
   public static class RemoveFields implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !(TABLE.equals(input.getTableName()) || "pet".equals(input.getTableName())))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getFields().remove(TABLE.equals(input.getTableName()) ? "longValue" : "name");
         return table;
      }
   }



   /*******************************************************************************
    ** A trusted personalizer can tighten a USER read without changing SYSTEM.
    *******************************************************************************/
   public static class AddReadLock implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !TABLE.equals(input.getTableName()))
         {
            return input.getTable();
         }
         return input.getTable().clone().withRecordSecurityLock(ownerLock());
      }
   }
}
