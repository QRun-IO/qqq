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


import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.columnstats.ColumnStatsStep;
import com.kingsrook.qqq.backend.core.processes.implementations.columnstats.ColumnStatsTableConfig;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native canonical ColumnStats caller probes. These use existing public APIs
 ** and can run before source propagation changes are installed.
 *******************************************************************************/
class SampleAggregateColumnStatsContractTest
{
   private QInstance instance;
   private Map<String, List<List<String>>> before;



   /*******************************************************************************
    ** Exactly three FieldLab rows, one USER-visible owner, and canonical joins.
    ** Existing Person/Pet rows remain independent joined read controls.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addProcess(ColumnStatsStep.getProcessMetaData());
      String joinName = instance.getJoins().values().stream()
         .filter(join -> "person".equals(join.getLeftTable()) && "pet".equals(join.getRightTable())).findFirst().orElseThrow().getName();
      instance.getTable("person").withExposedJoin(new ExposedJoin().withJoinTable("pet").withJoinPath(List.of(joinName)));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("columnOwner"));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("columnSpecies"));
      instance.getTable("pet").getField("id").setBackendName("pet_id");
      QContext.init(instance, new QSession().withSecurityKeyValue("columnOwner", "visible").withSecurityKeyValue("columnSpecies", 2));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         // Distinct physical key names isolate READ checks from aggregate-sort ambiguity.
         statement.execute("ALTER TABLE pet RENAME COLUMN id TO pet_id");
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab(id,name,text_value,long_value,decimal_value,password_value) VALUES "
            + "(1,'Visible Alpha','visible',10,1.25,'column-secret-a'),"
            + "(2,'Hidden Beta','other',20,2.50,'column-secret-b'),"
            + "(3,'Hidden Gamma','other',20,2.50,'column-secret-b')"));
         try(PreparedStatement bytes = connection.prepareStatement("UPDATE field_lab SET blob_value=? WHERE id=1"))
         {
            bytes.setBytes(1, new byte[] { 0, 32, -1 });
            assertEquals(1, bytes.executeUpdate());
         }
      }
      before = snapshot();
   }



   /*******************************************************************************
    ** All native rows and bytes survive each read or refusal. Every call helper
    ** separately checks metadata against the current owned fixture variant.
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
    ** The actual registered single-step process publishes counts and numeric
    ** stats in ProcessState, and its public serialized values preserve them.
    *******************************************************************************/
   @Test
   void testRegisteredProcessPublishesNativeCountsAndStats() throws Exception
   {
      String metadataBefore = metadata();
      UUID processId = UUID.randomUUID();
      RunProcessInput input = new RunProcessInput().withProcessName("columnStats");
      input.setProcessUUID(processId.toString());
      input.addValue("tableName", "fieldLab");
      input.addValue("fieldName", "longValue");
      input.addValue("orderBy", "count.desc");
      try
      {
         RunProcessOutput output = new RunProcessAction().execute(input);
         assertTrue(output.getException().isEmpty());
         assertStats(output.getValues(), "longValue", numericGroups("long_value", "field_lab", ""),
            numericStats("long_value", "field_lab", ""), List.of("count", "countDistinct", "sum", "average", "min", "max"));
         assertEquals(metadataBefore, metadata());
      }
      finally
      {
         RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(processId, StateType.PROCESS_STATUS));
      }
   }



   /*******************************************************************************
    ** Preliminary USER personalization must reach both grouped values and scalar
    ** stats. A direct SYSTEM Aggregate proves the other two rows still exist.
    *******************************************************************************/
   @Test
   void testUserBaseReadScopeReachesBothAggregates() throws Exception
   {
      personalize(UserBaseRead.class);
      assertStep(request("fieldLab", "longValue"), numericGroups("long_value", "field_lab", " WHERE text_value='visible'"),
         numericStats("long_value", "field_lab", " WHERE text_value='visible'"), List.of("count", "countDistinct", "sum", "average", "min", "max"));
      assertSystemAggregate(new AggregateInput("fieldLab").withAggregate(new Aggregate("longValue", AggregateOperator.SUM)),
         "SELECT SUM(long_value) FROM field_lab");
   }



   /*******************************************************************************
    ** Native Count is tested independently: one visible row is below limit two,
    ** although three committed rows exist. Limit zero still refuses that one row.
    *******************************************************************************/
   @Test
   void testVisibleCountLimitUsesUserNativeCountAndStep() throws Exception
   {
      personalize(UserBaseRead.class);
      assertEquals(List.of(List.of("1")), sql("SELECT COUNT(id) FROM field_lab WHERE text_value='visible'"));
      assertEquals(List.of(List.of("3")), sql("SELECT COUNT(id) FROM field_lab"));
      String metadataBefore = metadata();
      RunBackendStepInput allowed = request("fieldLab", "longValue");
      allowed.addValue("ColumnStatsTableConfig", new ColumnStatsTableConfig().withFailIfCountOverLimit(2));
      assertAll(
         () -> assertEquals(1, new CountAction().execute(new CountInput("fieldLab").withInputSource(QInputSource.USER)).getCount()),
         () -> assertEquals(3, new CountAction().execute(new CountInput("fieldLab").withInputSource(QInputSource.SYSTEM)).getCount()),
         () -> assertStep(allowed, numericGroups("long_value", "field_lab", " WHERE text_value='visible'"),
            numericStats("long_value", "field_lab", " WHERE text_value='visible'"), List.of("count", "countDistinct", "sum", "average", "min", "max")),
         () ->
         {
            RunBackendStepInput denied = request("fieldLab", "longValue");
            denied.addValue("ColumnStatsTableConfig", new ColumnStatsTableConfig().withFailIfCountOverLimit(0));
            QException failure = assertRefused(denied);
            assertTrue(messages(failure).contains("too many rows (1)"), "The refusal reports the visible count, not the canonical total");
         },
         () -> assertEquals(metadataBefore, metadata()));
   }



   /*******************************************************************************
    ** The exposed canonical join's USER-only READ lock limits the actual child
    ** rows counted and summarized, with unrestricted SYSTEM native control.
    *******************************************************************************/
   @Test
   void testJoinedUserReadScopeReachesColumnStats() throws Exception
   {
      personalize(UserJoinedRead.class);
      String from = "person p INNER JOIN pet a ON p.id=a.person_id";
      assertStep(request("person", "pet.name"), stringGroups("a.name", from, " WHERE a.species_id=2"),
         stringStats("a.name", from, " WHERE a.species_id=2"), List.of("count", "countDistinct", "min", "max"));
      assertSystemAggregate(new AggregateInput("person").withAggregate(new Aggregate("pet.name", AggregateOperator.COUNT))
         .withQueryJoin(new QueryJoin("pet").withSelect(false).withType(QueryJoin.Type.INNER)),
         "SELECT COUNT(a.name) FROM " + from);
   }



   /*******************************************************************************
    ** A readable base does not grant exposed/filter-only joined table READ.
    ** Ordinary public fields and unchanged metadata isolate table permissions.
    *******************************************************************************/
   @Test
   void testJoinedTableReadPermissionForTargetAndFilter() throws Exception
   {
      instance.getTable("person").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("pet").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      QContext.getQSession().setPermissions(new LinkedHashSet<>(Set.of("person.read")));
      QueryInput permissionInput = new QueryInput().withTableName("person").withInputSource(QInputSource.USER);
      assertTrue(PermissionsHelper.hasTablePermission(permissionInput, "person", TablePermissionSubType.READ));
      assertFalse(PermissionsHelper.hasTablePermission(permissionInput, "pet", TablePermissionSubType.READ));
      assertStep(request("person", "firstName"), stringGroups("first_name", "person", ""),
         stringStats("first_name", "person", ""), List.of("count", "countDistinct", "min", "max"));

      RunBackendStepInput joinedFilter = request("person", "firstName");
      joinedFilter.addValue("filterJSON", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("pet.name", QCriteriaOperator.EQUALS, "Charlie"))));
      assertAll(
         () -> assertPermissionRefused(request("person", "pet.name")),
         () -> assertPermissionRefused(joinedFilter));

      QContext.getQSession().withPermission("pet.read");
      assertTrue(PermissionsHelper.hasTablePermission(permissionInput, "pet", TablePermissionSubType.READ));
      String from = "person p INNER JOIN pet a ON p.id=a.person_id";
      assertStep(request("person", "pet.name"), stringGroups("a.name", from, ""),
         stringStats("a.name", from, ""), List.of("count", "countDistinct", "min", "max"));
      assertStep(joinedFilter, stringGroups("p.first_name", from, " WHERE a.name='Charlie'"),
         stringStats("p.first_name", from, " WHERE a.name='Charlie'"), List.of("count", "countDistinct", "min", "max"));
   }



   /*******************************************************************************
    ** USER-removed base, exposed-join and filter fields reject before values are
    ** published; a fresh SYSTEM input still resolves canonical operands.
    *******************************************************************************/
   @Test
   void testRemovedBaseJoinAndFilterOperandsReject() throws Exception
   {
      personalize(RemoveOperands.class);
      RunBackendStepInput filter = request("fieldLab", "decimalValue");
      filter.addValue("filterJSON", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("textValue", QCriteriaOperator.EQUALS, "visible"))));
      assertAll(
         () -> assertRefused(request("fieldLab", "longValue")),
         () -> assertRefused(request("person", "pet.name")),
         () -> assertRefused(filter),
         () -> assertSystemAggregate(new AggregateInput("fieldLab").withAggregate(new Aggregate("longValue", AggregateOperator.SUM)),
            "SELECT SUM(long_value) FROM field_lab"),
         () -> assertSystemAggregate(new AggregateInput("person").withAggregate(new Aggregate("pet.name", AggregateOperator.COUNT))
            .withQueryJoin(new QueryJoin("pet").withSelect(false).withType(QueryJoin.Type.INNER)),
            "SELECT COUNT(a.name) FROM person p INNER JOIN pet a ON p.id=a.person_id"));
   }



   /*******************************************************************************
    ** A personalized-away base or exposed table is a checked refusal, not a null
    ** dereference, and must not leave partially populated process values.
    *******************************************************************************/
   @Test
   void testRemovedTablesRefuseWithoutPublishingValues()
   {
      personalize(RemoveTables.class);
      assertAll(
         () -> assertRefused(request("fieldLab", "longValue")),
         () -> assertRefused(request("person", "pet.name")),
         () -> assertSystemAggregate(new AggregateInput("fieldLab").withAggregate(new Aggregate("longValue", AggregateOperator.COUNT)),
            "SELECT COUNT(long_value) FROM field_lab"));
   }



   /*******************************************************************************
    ** A hidden primary key remains usable as the internal cardinality operand
    ** without appearing in the public statistics records.
    *******************************************************************************/
   @Test
   void testPrivatePrimaryKeyCountsRemainPrivate() throws Exception
   {
      instance.getTable("fieldLab").getField("id").setIsHidden(true);
      Map<String, Serializable> output = assertStep(request("fieldLab", "decimalValue"), numericGroups("decimal_value", "field_lab", ""),
         numericStats("decimal_value", "field_lab", ""), List.of("count", "countDistinct", "sum", "average", "min", "max"));
      for(QRecord value : records(output.get("valueCounts")))
      {
         assertFalse(value.getValues().containsKey("id"));
      }
      assertFalse(assertInstanceOf(QRecord.class, output.get("statsRecord")).getValues().containsKey("id"));
   }



   /*******************************************************************************
    ** Explicit REVEAL and heavy-only fields keep real values available through
    ** ColumnStats, not just through its lower-level Aggregate dependency.
    *******************************************************************************/
   @Test
   void testRevealAndHeavyOnlyValuesRemainAvailable() throws Exception
   {
      instance.getTable("fieldLab").getField("passwordValue").withFieldAdornment(new FieldAdornment().withType(AdornmentType.REVEAL));
      assertStep(request("fieldLab", "passwordValue"), stringGroups("password_value", "field_lab", ""),
         stringStats("password_value", "field_lab", ""), List.of("count", "countDistinct", "min", "max"));
      instance.getTable("fieldLab").getField("decimalValue").setIsHeavy(true);
      assertStep(request("fieldLab", "decimalValue"), numericGroups("decimal_value", "field_lab", ""),
         numericStats("decimal_value", "field_lab", ""), List.of("count", "countDistinct", "sum", "average", "min", "max"));
   }



   /*******************************************************************************
    ** Run the actual backend step and retain only its real returned value map.
    *******************************************************************************/
   private Map<String, Serializable> assertStep(RunBackendStepInput input, String groupsSql, String statsSql, List<String> statsFields) throws Exception
   {
      String metadataBefore = metadata();
      String inputBefore = JsonUtils.toJson(input.getValues());
      RunBackendStepOutput output = new RunBackendStepOutput();
      new ColumnStatsStep().run(input, output);
      assertStats(output.getValues(), input.getValueString("fieldName"), groupsSql, statsSql, statsFields);
      assertEquals(inputBefore, JsonUtils.toJson(input.getValues()));
      assertEquals(metadataBefore, metadata());
      return output.getValues();
   }



   /*******************************************************************************
    ** Independent native SQL supplies group values, counts, percentages and all
    ** scalar stats; exact raw and serialized output projections are checked.
    *******************************************************************************/
   private void assertStats(Map<String, Serializable> values, String fieldName, String groupsSql, String statsSql, List<String> statsFields) throws Exception
   {
      assertTrue(values.keySet().containsAll(Set.of("valueCounts", "statsRecord", "statsFields")));
      List<QRecord> counts = records(values.get("valueCounts"));
      List<List<String>> actualGroups = new ArrayList<>();
      for(QRecord record : counts)
      {
         assertEquals(Set.of(fieldName, "count", "percent"), record.getValues().keySet());
         actualGroups.add(List.of(value(record.getValue(fieldName)), value(record.getValue("count")), value(record.getValue("percent"))));
      }
      List<List<String>> expectedGroups = sql(groupsSql);
      assertEquals(expectedGroups, actualGroups);
      QRecord stats = assertInstanceOf(QRecord.class, values.get("statsRecord"));
      assertEquals(new LinkedHashSet<>(statsFields), stats.getValues().keySet());
      List<String> actualStats = statsFields.stream().map(field -> value(stats.getValue(field))).toList();
      List<List<String>> expectedStats = sql(statsSql);
      assertEquals(List.of(actualStats), expectedStats);
      List<?> fields = assertInstanceOf(List.class, values.get("statsFields"));
      assertEquals(statsFields, fields.stream().map(field -> assertInstanceOf(QFieldMetaData.class, field).getName()).toList());
      assertEquals("Rows with a value", assertInstanceOf(QFieldMetaData.class, fields.get(0)).getLabel());
      assertEquals("Distinct values", assertInstanceOf(QFieldMetaData.class, fields.get(1)).getLabel());

      JsonNode wire = JsonUtils.toObject(JsonUtils.toJson(values), JsonNode.class);
      assertEquals(expectedGroups.size(), wire.get("valueCounts").size());
      for(int row = 0; row < expectedGroups.size(); row++)
      {
         JsonNode recordValues = wire.get("valueCounts").get(row).get("values");
         assertEquals(Set.of(fieldName, "count", "percent"), names(recordValues));
         assertEquals(expectedGroups.get(row), List.of(jsonValue(recordValues.get(fieldName)), jsonValue(recordValues.get("count")), jsonValue(recordValues.get("percent"))));
      }
      JsonNode wireStats = wire.get("statsRecord").get("values");
      assertEquals(new LinkedHashSet<>(statsFields), names(wireStats));
      assertEquals(expectedStats.get(0), statsFields.stream().map(field -> jsonValue(wireStats.get(field))).toList());
      assertEquals(statsFields.size(), wire.get("statsFields").size());
   }



   /*******************************************************************************
    ** Output stays empty on refusal. Native errors cannot stand in for policy.
    *******************************************************************************/
   private QException assertRefused(RunBackendStepInput input) throws Exception
   {
      String metadataBefore = metadata();
      RunBackendStepOutput output = new RunBackendStepOutput();
      QException failure = assertThrows(QException.class, () -> new ColumnStatsStep().run(input, output));
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         assertFalse(cause instanceof SQLException, "Native SQL failure is not an authorization/limit refusal");
         assertFalse(cause instanceof NullPointerException, "Missing active metadata must fail deliberately");
      }
      assertFalse(output.getValues().containsKey("valueCounts"));
      assertFalse(output.getValues().containsKey("statsRecord"));
      assertFalse(output.getValues().containsKey("statsFields"));
      assertFalse(JsonUtils.toJson(output.getValues()).contains("column-secret"));
      assertEquals(metadataBefore, metadata());
      return failure;
   }



   /*******************************************************************************
    ** The denied child grant must produce permission failure, not another refusal.
    *******************************************************************************/
   private void assertPermissionRefused(RunBackendStepInput input) throws Exception
   {
      QException failure = assertRefused(input);
      boolean permissionFailure = false;
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         permissionFailure |= cause instanceof QPermissionDeniedException;
      }
      assertTrue(permissionFailure, "An unavailable child READ grant must cause the refusal");
   }



   /*******************************************************************************
    ** Direct trusted execution is only a control; ColumnStats itself stays USER.
    *******************************************************************************/
   private void assertSystemAggregate(AggregateInput input, String expectedSql) throws Exception
   {
      String metadataBefore = metadata();
      input.setInputSource(QInputSource.SYSTEM);
      Aggregate aggregate = input.getAggregates().get(0);
      Serializable actual = new AggregateAction().execute(input).getResults().get(0).getAggregateValue(aggregate);
      assertEquals(sql(expectedSql), List.of(List.of(value(actual))));
      assertEquals(metadataBefore, metadata());
   }



   /*******************************************************************************
    ** Canonical native numeric values are all nonnull, so ColumnStats's distinct
    ** presentation statistic equals SQL COUNT DISTINCT in these specific cases.
    *******************************************************************************/
   private String numericStats(String field, String from, String where)
   {
      return "SELECT COUNT(" + field + "),COUNT(DISTINCT " + field + "),SUM(" + field + "),AVG(" + field + "),MIN(" + field + "),MAX(" + field + ") FROM " + from + where;
   }



   /*******************************************************************************
    ** String stats deliberately omit unsupported SUM/AVG, matching the public UI.
    *******************************************************************************/
   private String stringStats(String field, String from, String where)
   {
      return "SELECT COUNT(" + field + "),COUNT(DISTINCT " + field + "),MIN(" + field + "),MAX(" + field + ") FROM " + from + where;
   }



   /*******************************************************************************
    ** H2 computes group percentages independently through the native grouped row
    ** count window; the caller's configured order is count descending/value ascending.
    *******************************************************************************/
   private String numericGroups(String field, String from, String where)
   {
      return "SELECT " + field + ",COUNT(*),ROUND(100.0*COUNT(*)/SUM(COUNT(*)) OVER(),2) FROM " + from + where
         + " GROUP BY " + field + " ORDER BY COUNT(*) DESC," + field;
   }



   /*******************************************************************************
    ** String groups share SQL ordering; no empty/null rollup policy is asserted.
    *******************************************************************************/
   private String stringGroups(String field, String from, String where)
   {
      return numericGroups(field, from, where);
   }



   /*******************************************************************************
    ** A new request prevents internal filter/order mutation from leaking between calls.
    *******************************************************************************/
   private RunBackendStepInput request(String table, String field)
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", table);
      input.addValue("fieldName", field);
      input.addValue("orderBy", "count.desc");
      return input;
   }



   /*******************************************************************************
    ** Only supported supplemental customization is used for USER-specific metadata.
    *******************************************************************************/
   private void personalize(Class<? extends TableMetaDataPersonalizerInterface> customizer)
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(customizer));
   }



   /*******************************************************************************
    ** The step's public value list contains QRecords, not AggregateResult adapters.
    *******************************************************************************/
   private List<QRecord> records(Serializable value)
   {
      List<?> list = assertInstanceOf(List.class, value);
      return list.stream().map(record -> assertInstanceOf(QRecord.class, record)).toList();
   }



   /*******************************************************************************
    ** Exact JSON keysets are inspected without assuming property iteration order.
    *******************************************************************************/
   private Set<String> names(JsonNode object)
   {
      Set<String> names = new LinkedHashSet<>();
      object.fieldNames().forEachRemaining(names::add);
      return names;
   }



   /*******************************************************************************
    ** Native numeric scale is normalized only for comparison.
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
    ** Preserve actual JSON numbers/text rather than comparing JSON formatting.
    *******************************************************************************/
   private String jsonValue(JsonNode node)
   {
      return node == null || node.isNull() ? null : node.isNumber() ? value(node.decimalValue()) : node.asText();
   }



   /*******************************************************************************
    ** Cause text is used only for the visible limit count, never private values.
    *******************************************************************************/
   private String messages(Throwable failure)
   {
      StringBuilder result = new StringBuilder();
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         result.append(cause.getMessage()).append('\n');
      }
      return result.toString();
   }



   /*******************************************************************************
    ** Fixed native SQL reads actual persisted values independently of QQQ output.
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
    ** Native snapshots preserve every row/column and actual heavy bytes.
    *******************************************************************************/
   private Map<String, List<List<String>>> snapshot() throws Exception
   {
      Map<String, List<List<String>>> tables = new LinkedHashMap<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         for(String table : List.of("field_lab", "person", "pet", "pet_note"))
         {
            List<List<String>> rows = new ArrayList<>();
            try(ResultSet result = statement.executeQuery("SELECT * FROM " + table + " ORDER BY " + (table.equals("pet") ? "pet_id" : "id")))
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
    ** Include registered query/exposed joins and process metadata in preservation.
    *******************************************************************************/
   private String metadata()
   {
      return JsonUtils.toJson(List.of(instance.getTable("fieldLab"), instance.getTable("person"), instance.getTable("pet"),
         instance.getTable("petNote"), instance.getJoins(), instance.getProcess("columnStats")));
   }



   /*******************************************************************************
    ** Connections are owned by this fixture, with no transaction claims.
    *******************************************************************************/
   private Connection connection() throws SQLException
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    ** The raw owner field is only a USER READ predicate, not the requested column.
    *******************************************************************************/
   public static class UserBaseRead implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !"fieldLab".equals(input.getTableName()))
         {
            return input.getTable();
         }
         return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withFieldName("textValue").withSecurityKeyType("columnOwner")
            .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      }
   }



   /*******************************************************************************
    ** One existing canonical pet species is selected only for USER execution.
    *******************************************************************************/
   public static class UserJoinedRead implements TableMetaDataPersonalizerInterface
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
         return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withFieldName("speciesId").withSecurityKeyType("columnSpecies")
            .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      }
   }



   /*******************************************************************************
    ** Removal is USER-only; original metadata and SYSTEM controls retain fields.
    *******************************************************************************/
   public static class RemoveOperands implements TableMetaDataPersonalizerInterface
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
         if("fieldLab".equals(table.getName()))
         {
            table.getFields().remove("longValue");
            table.getFields().remove("textValue");
         }
         else
         {
            table.getFields().remove("name");
         }
         return table;
      }
   }



   /*******************************************************************************
    ** The root and exposed tables may independently disappear for USER metadata.
    *******************************************************************************/
   public static class RemoveTables implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         return input.getInputSource() == QInputSource.USER && ("fieldLab".equals(input.getTableName()) || "pet".equals(input.getTableName())) ? null : input.getTable();
      }
   }
}
