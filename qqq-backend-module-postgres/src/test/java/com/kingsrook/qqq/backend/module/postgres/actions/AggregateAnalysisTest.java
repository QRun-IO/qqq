/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.actions;


import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.NumericBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.aggregates.AggregateAnalysis;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSAggregateAction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;


/*******************************************************************************
 ** Aggregate analysis contract, using the same six-row fixture in each backend.
 *******************************************************************************/
class AggregateAnalysisTest extends BaseTest
{
   private static final String TABLE = "analysisFixture";
   private static final String MEMORY = "analysisMemory";



   /*******************************************************************************
    ** Identical hand-computable data in every backend.
    *******************************************************************************/
   private List<QRecord> fixture()
   {
      Integer[] numbers = { -1, 0, 5, 10, 11, null };
      String[] strings = { null, "", " ", "abc", new String(Character.toChars(0x1F600)), "abcdef" };
      List<QRecord> records = new ArrayList<>();
      for(int i = 0; i < numbers.length; i++)
      {
         records.add(new QRecord().withValue("id", i + 1).withValue("x", numbers[i]).withValue("y", numbers[i] == null ? 1000 : numbers[i] * 2)
            .withValue("textValue", strings[i]).withValue("eventTime", i == 5 ? null : Instant.parse("2024-04-01T00:30:00Z").plusSeconds(i * 86400L))
            .withValue("eventDate", i == 5 ? null : LocalDate.of(2024, 4, 1).plusDays(i)));
      }
      return (records);
   }



   /*******************************************************************************
    ** Fixture schema with explicit permissions and stable primary keys.
    *******************************************************************************/
   private QTableMetaData table(String name, String backend)
   {
      return (new QTableMetaData().withName(name).withLabel(name).withBackendName(backend).withPrimaryKeyField("id")
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("x", QFieldType.INTEGER))
         .withField(new QFieldMetaData("y", QFieldType.INTEGER))
         .withField(new QFieldMetaData("textValue", QFieldType.STRING))
         .withField(new QFieldMetaData("eventTime", QFieldType.DATE_TIME))
         .withField(new QFieldMetaData("eventDate", QFieldType.DATE)));
   }



   /*******************************************************************************
    ** Every new operator plus legacy operators in one request.
    *******************************************************************************/
   private AggregateInput operators(String table)
   {
      AggregateInput input = new AggregateInput(table);
      for(AggregateOperator operator : AggregateOperator.values())
      {
         String field = switch(operator)
         {
            case COUNT_BLANK, MIN_LENGTH, MAX_LENGTH, AVG_LENGTH -> "textValue";
            default -> "x";
         };
         input.withAggregate(new Aggregate(field, operator).withPercentile(new BigDecimal("0.3")).withOtherFieldName(operator == AggregateOperator.CORR ? "y" : null));
      }
      return (input);
   }



   /*******************************************************************************
    ** Compare numbers with a tolerance and other values exactly.
    *******************************************************************************/
   private void equivalent(AggregateOutput actual, AggregateOutput expected)
   {
      assertThat(actual.getResults()).hasSize(expected.getResults().size());
      for(AggregateResult result : actual.getResults())
      {
         AggregateResult reference = expected.getResults().stream().filter(row -> row.getGroupByValues().equals(result.getGroupByValues())).findFirst().orElseThrow();
         result.getAggregateValues().forEach((aggregate, value) ->
         {
            Serializable wanted = reference.getAggregateValue(aggregate);
            if(value instanceof Number && wanted instanceof Number)
            {
               assertThat(ValueUtils.getValueAsBigDecimal(value)).as(aggregate.getOperator().name()).isCloseTo(ValueUtils.getValueAsBigDecimal(wanted), within(new BigDecimal("0.000000001")));
            }
            else
            {
               assertThat(value).as(aggregate.getOperator().name()).isEqualTo(wanted);
            }
         });
      }
   }



   /*******************************************************************************
    ** Install the fixture in the active test instance.
    *******************************************************************************/
   @BeforeEach
   void fixtureSetup() throws Exception
   {
      MemoryRecordStore.getInstance().reset();
      QContext.getQInstance().addBackend(new QBackendMetaData().withName("analysisBackend").withBackendType(MemoryBackendModule.class));
      QContext.getQInstance().addTable(table(MEMORY, "analysisBackend"));
      QContext.getQInstance().addTable(table(TABLE, TestUtils.DEFAULT_BACKEND_NAME).withBackendDetails(new PostgreSQLTableBackendDetails().withTableName("analysis_fixture")));
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         connection.createStatement().execute("DROP TABLE IF EXISTS analysis_fixture");
         connection.createStatement().execute("CREATE TABLE analysis_fixture (id INTEGER PRIMARY KEY, x INTEGER, y INTEGER, \"textValue\" VARCHAR(100), \"eventTime\" TIMESTAMP, \"eventDate\" DATE)");
      }
      new InsertAction().execute(new InsertInput(MEMORY).withRecords(fixture()));
      new InsertAction().execute(new InsertInput(TABLE).withRecords(fixture()));
   }



   /*******************************************************************************
    ** All advertised operators execute in SQL and agree with the memory reference.
    *******************************************************************************/
   @Test
   void allOperators() throws Exception
   {
      AggregateInput input = operators(TABLE);
      assertThat(new RDBMSAggregateAction().getSupportedCapabilities(input)).containsAll(AggregateAnalysis.requiredCapabilities(input));
      equivalent(new AggregateAction().execute(input), new AggregateAction().execute(operators(MEMORY)));
   }



   /*******************************************************************************
    ** Every date unit, numeric edges and session zones agree across backends.
    *******************************************************************************/
   @Test
   void allBuckets() throws Exception
   {
      List<GroupBy> groups = new ArrayList<>();
      groups.add(new GroupBy(QFieldType.INTEGER, "x").withBucket(new NumericBucket(BigDecimal.ZERO, BigDecimal.TEN, 2)));
      for(DateBucketUnit unit : DateBucketUnit.values())
      {
         groups.add(new GroupBy(QFieldType.DATE_TIME, "eventTime").withBucket(new DateBucket(unit)));
         groups.add(new GroupBy(QFieldType.DATE, "eventDate").withBucket(new DateBucket(unit)));
      }
      for(String zone : List.of("UTC", "Asia/Kolkata", "America/Chicago"))
      {
         QContext.getQSession().withValue(QSession.VALUE_KEY_USER_TIMEZONE, zone);
         for(GroupBy group : groups)
         {
            AggregateInput input = new AggregateInput(TABLE).withGroupBy(group).withAggregate(AggregateOperator.COUNT.of("id"));
            AggregateOutput actual = new AggregateAction().execute(input);
            AggregateOutput expected = new AggregateAction().execute(input.withTableName(MEMORY));
            assertThat(actual.getResults()).extracting(row -> row.getGroupByValue(group))
               .as(zone + " " + group.getFieldName() + " " + (group.getBucket() instanceof DateBucket date ? date.getUnit() : "numeric"))
               .containsExactlyInAnyOrderElementsOf(expected.getResults().stream().map(row -> row.getGroupByValue(group)).toList());
            equivalent(actual, expected);
         }
      }
   }



   /*******************************************************************************
    ** A 100-percent sample is deterministic even for PostgreSQL block sampling on a tiny fixture.
    *******************************************************************************/
   @Test
   void samplingAndTopN() throws Exception
   {
      Aggregate count = AggregateOperator.COUNT.of("id");
      AggregateInput input = new AggregateInput(TABLE).withAggregate(count).withSamplePercent(new BigDecimal("100"));
      AggregateOutput output = new AggregateAction().execute(input);
      assertThat(output.getSampled()).isTrue();
      assertThat(output.getSamplePercent()).isEqualByComparingTo("100");
      assertThat((Integer) output.getResults().getFirst().getAggregateValue(count)).isBetween(1, 6);
      GroupBy group = new GroupBy(QFieldType.INTEGER, "x").withBucket(new NumericBucket(BigDecimal.ZERO, BigDecimal.TEN, 2));
      input.withSamplePercent(null).withGroupBy(group).withLimit(1)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(count).withIsAscending(false)));
      assertThat(new AggregateAction().execute(input).getResults()).hasSize(1).first()
         .satisfies(row -> assertThat(row.getAggregateValue(count)).isEqualTo(2));
   }
}
