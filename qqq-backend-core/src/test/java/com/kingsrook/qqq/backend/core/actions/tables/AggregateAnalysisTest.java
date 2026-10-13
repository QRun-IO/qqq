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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.StreamedAggregateEvaluator;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
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
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.aggregates.AggregateAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
      QContext.getQInstance().addTable(table(TABLE, "analysisBackend"));
      new InsertAction().execute(new InsertInput(TABLE).withRecords(fixture()));
   }



   /*******************************************************************************
    ** Operator results are hand-computed, including PostgreSQL percentile definitions.
    *******************************************************************************/
   @Test
   void operatorsAndFallback() throws Exception
   {
      AggregateInput input = operators(TABLE);
      AggregateOutput output = new AggregateAction().execute(input);
      Map<AggregateOperator, Serializable> values = output.getResults().getFirst().getAggregateValues().entrySet().stream()
         .collect(Collectors.toMap(entry -> entry.getKey().getOperator(), Map.Entry::getValue));
      Map<AggregateOperator, String> expected = Map.ofEntries(
         Map.entry(AggregateOperator.STDDEV_SAMP, "5.522680508593630"), Map.entry(AggregateOperator.STDDEV_POP, "4.939635614091388"),
         Map.entry(AggregateOperator.VAR_SAMP, "30.5"), Map.entry(AggregateOperator.VAR_POP, "24.4"),
         Map.entry(AggregateOperator.PERCENTILE_CONT, "1"), Map.entry(AggregateOperator.PERCENTILE_DISC, "0"),
         Map.entry(AggregateOperator.CORR, "1"), Map.entry(AggregateOperator.COUNT_NULL, "1"),
         Map.entry(AggregateOperator.COUNT_BLANK, "3"), Map.entry(AggregateOperator.MIN_LENGTH, "0"),
         Map.entry(AggregateOperator.MAX_LENGTH, "6"), Map.entry(AggregateOperator.AVG_LENGTH, "2.2"));
      expected.forEach((operator, value) -> assertThat(ValueUtils.getValueAsBigDecimal(values.get(operator))).as(operator.name())
         .isCloseTo(new BigDecimal(value), within(new BigDecimal("0.000000001"))));
      equivalent(new StreamedAggregateEvaluator().execute(input), output);
      AggregateOutput limited = new StreamedAggregateEvaluator().execute(input.withStreamedRowLimit(3));
      assertThat(limited.getTruncated()).isTrue();
      assertThat(limited.getRowsScanned()).isEqualTo(3);
   }



   /*******************************************************************************
    ** Numeric edge semantics and each date unit are shared with fallback.
    *******************************************************************************/
   @Test
   void buckets() throws Exception
   {
      GroupBy numeric = new GroupBy(QFieldType.INTEGER, "x").withBucket(new NumericBucket(BigDecimal.ZERO, BigDecimal.TEN, 2));
      AggregateInput input = new AggregateInput(TABLE).withGroupBy(numeric).withAggregate(AggregateOperator.COUNT.of("id"));
      AggregateOutput output = new AggregateAction().execute(input);
      assertThat(output.getResults()).extracting(result -> result.getGroupByValue(numeric)).containsExactlyInAnyOrder(0, 1, 2, 3, null);
      assertThat(output.getResults().stream().filter(row -> Integer.valueOf(2).equals(row.getGroupByValue(numeric))).findFirst().orElseThrow()
         .getAggregateValue(AggregateOperator.COUNT.of("id"))).isEqualTo(2);
      equivalent(new StreamedAggregateEvaluator().execute(input), output);
      for(String zone : List.of("UTC", "Asia/Kolkata", "America/Chicago"))
      {
         QContext.getQSession().withValue(QSession.VALUE_KEY_USER_TIMEZONE, zone);
         for(DateBucketUnit unit : DateBucketUnit.values())
         {
            GroupBy date = new GroupBy(QFieldType.DATE_TIME, "eventTime").withBucket(new DateBucket(unit));
            AggregateInput request = new AggregateInput(TABLE).withGroupBy(date).withAggregate(AggregateOperator.COUNT.of("id"));
            equivalent(new StreamedAggregateEvaluator().execute(request), new AggregateAction().execute(request));
         }
      }
      Instant instant = Instant.parse("2024-04-01T00:30:00Z");
      ZoneId zone = ZoneId.of("America/Chicago");
      Map<DateBucketUnit, Serializable> starts = Map.of(
         DateBucketUnit.YEAR, LocalDate.of(2024, 1, 1), DateBucketUnit.QUARTER, LocalDate.of(2024, 1, 1),
         DateBucketUnit.MONTH, LocalDate.of(2024, 3, 1), DateBucketUnit.WEEK, LocalDate.of(2024, 3, 25),
         DateBucketUnit.DAY, LocalDate.of(2024, 3, 31), DateBucketUnit.HOUR, Instant.parse("2024-04-01T00:00:00Z"),
         DateBucketUnit.DAY_OF_WEEK, 7, DateBucketUnit.HOUR_OF_DAY, 19);
      starts.forEach((unit, expected) -> assertThat(AggregateAnalysis.bucket(instant, new DateBucket(unit), zone)).isEqualTo(expected));
   }



   /*******************************************************************************
    ** String-only operators reject non-string operands before choosing a backend.
    *******************************************************************************/
   @Test
   void stringOperatorTypes() throws Exception
   {
      for(AggregateOperator operator : List.of(AggregateOperator.COUNT_BLANK, AggregateOperator.MIN_LENGTH, AggregateOperator.MAX_LENGTH, AggregateOperator.AVG_LENGTH))
      {
         for(QFieldType type : QFieldType.values())
         {
            QContext.getQInstance().getTable(TABLE).getField("textValue").setType(type);
            AggregateInput input = new AggregateInput(TABLE).withAggregate(operator.of("textValue"));
            if(type == QFieldType.STRING || type == QFieldType.TEXT || type == QFieldType.HTML)
            {
               AggregateAnalysis.validate(input);
            }
            else
            {
               assertThatThrownBy(() -> new AggregateAction().execute(input)).isInstanceOf(QException.class)
                  .hasMessageContaining(operator.name()).hasMessageContaining("textValue").hasMessageContaining("STRING, TEXT or HTML");
            }
         }
      }
   }



   /*******************************************************************************
    ** Invalid parameters fail before backend evaluation; deterministic samples are labelled.
    *******************************************************************************/
   @Test
   void validationAndSampling() throws Exception
   {
      AggregateInput input = operators(TABLE);
      input.withGroupBy(new GroupBy(QFieldType.INTEGER, "x", "%s").withBucket(new NumericBucket(BigDecimal.ZERO, BigDecimal.TEN, 2)));
      assertThatThrownBy(() -> new AggregateAction().execute(input)).isInstanceOf(QException.class).hasMessageContaining("mutually exclusive");
      input.setGroupBys(List.of());
      input.withSamplePercent(new BigDecimal("50"));
      AggregateOutput sampled = new AggregateAction().execute(input);
      assertThat(sampled.getSampled()).isTrue();
      assertThat(sampled.getSamplePercent()).isEqualByComparingTo("50");
      assertThat(sampled.getRowsScanned()).isEqualTo(6);
      assertThat(sampled.getResults().getFirst().getAggregateValue(input.getAggregates().getFirst())).isEqualTo(3);
      equivalent(sampled, new AggregateAction().execute(input));
      input.withSamplePercent(BigDecimal.ZERO);
      assertThatThrownBy(() -> new AggregateAction().execute(input)).hasMessageContaining("samplePercent");
      input.withSamplePercent(null).withStreamedRowLimit(0);
      assertThatThrownBy(() -> new AggregateAction().execute(input)).hasMessageContaining("streamedRowLimit");
      input.withStreamedRowLimit(100).getAggregates().get(10).withPercentile(new BigDecimal("2"));
      assertThatThrownBy(() -> new AggregateAction().execute(input)).hasMessageContaining("percentile");
   }



   /*******************************************************************************
    ** Unknown capabilities reject by default, or evaluate the whole request through QueryAction.
    *******************************************************************************/
   @Test
   void unsupportedBackend() throws Exception
   {
      QBackendModuleDispatcher.registerBackendModule(new LimitedMemoryBackend());
      QContext.getQInstance().getBackend("analysisBackend").setBackendType("limitedAnalysisMemory");
      AggregateInput input = operators(TABLE);
      assertThatThrownBy(() -> new AggregateAction().execute(input)).isInstanceOf(QUserFacingException.class).hasMessageContaining("PERCENTILE");
      input.withAllowStreamedFallback(true).withSamplePercent(new BigDecimal("50"));
      AggregateOutput output = new AggregateAction().execute(input);
      equivalent(output, new StreamedAggregateEvaluator().execute(input));
      assertThat(output.getSampled()).isFalse();
      assertThat(output.getSamplePercent()).isNull();
      assertThat(output.getTruncated()).isFalse();
   }



   /*******************************************************************************
    ** Top-N by count uses the existing order-by descriptor and limit.
    *******************************************************************************/
   @Test
   void topN() throws Exception
   {
      Aggregate count = AggregateOperator.COUNT.of("id");
      AggregateInput input = new AggregateInput(TABLE).withGroupBy(new GroupBy(QFieldType.INTEGER, "x")
         .withBucket(new NumericBucket(BigDecimal.ZERO, BigDecimal.TEN, 2))).withAggregate(count)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(count).withIsAscending(false))).withLimit(1);
      assertThat(new AggregateAction().execute(input).getResults()).hasSize(1).first()
         .satisfies(result -> assertThat(result.getAggregateValue(count)).isEqualTo(2));
   }



   /*******************************************************************************
    ** Test backend with Query support and no advertised aggregate extensions.
    *******************************************************************************/
   public static class LimitedMemoryBackend extends MemoryBackendModule
   {
      /*******************************************************************************
       ** Unique test backend name.
       *******************************************************************************/
      @Override
      public String getBackendType()
      {
         return ("limitedAnalysisMemory");
      }



      /*******************************************************************************
       ** Use the default empty capability contract.
       *******************************************************************************/
      @Override
      public AggregateInterface getAggregateInterface()
      {
         return (new AggregateInterface()
         {
            /*******************************************************************************
             ** Native execution is used only when no extension requires fallback.
             *******************************************************************************/
            @Override
            public AggregateOutput execute(AggregateInput input) throws QException
            {
               return (MemoryRecordStore.getInstance().aggregate(input));
            }
         });
      }
   }



   /*******************************************************************************
    ** Percentile boundaries, singleton variance, and null-only inputs match SQL definitions.
    *******************************************************************************/
   @Test
   void percentileAndSmallGroups() throws Exception
   {
      for(double p : new double[] { 0, 0.25, 0.3, 0.5, 1 })
      {
         Aggregate continuous = Aggregate.percentile("x", p);
         Aggregate discrete = new Aggregate("x", AggregateOperator.PERCENTILE_DISC).withPercentile(BigDecimal.valueOf(p));
         AggregateResult result = new AggregateAction().execute(new AggregateInput(TABLE).withAggregate(continuous).withAggregate(discrete)).getResults().getFirst();
         double expected = p == 0 ? -1 : p == 0.25 ? 0 : p == 0.3 ? 1 : p == 0.5 ? 5 : 11;
         assertThat((BigDecimal) result.getAggregateValue(continuous)).isCloseTo(BigDecimal.valueOf(expected), within(new BigDecimal("0.000000001")));
         assertThat(result.getAggregateValue(discrete)).isInstanceOf(Integer.class);
      }
      Aggregate population = AggregateOperator.VAR_POP.of("x");
      Aggregate sample = AggregateOperator.STDDEV_SAMP.of("x");
      Aggregate percentile = Aggregate.percentile("x", 0.5);
      AggregateInput input = new AggregateInput(TABLE).withAggregate(population).withAggregate(sample).withAggregate(percentile)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)));
      AggregateResult singleton = new AggregateAction().execute(input).getResults().getFirst();
      assertThat((BigDecimal) singleton.getAggregateValue(population)).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(singleton.getAggregateValue(sample)).isNull();
      input.withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 6)));
      assertThat(new AggregateAction().execute(input).getResults().getFirst().getAggregateValues().values()).containsOnlyNulls();
      input.withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 99)));
      assertThat(new AggregateAction().execute(input).getResults().getFirst().getAggregateValues().values()).containsOnlyNulls();
   }



   /*******************************************************************************
    ** The row budget crosses a page boundary without dropping or double-counting rows.
    *******************************************************************************/
   @Test
   void pagedBudget() throws Exception
   {
      List<QRecord> records = new ArrayList<>();
      for(int id = 7; id <= 1010; id++)
      {
         records.add(new QRecord().withValue("id", id).withValue("x", id));
      }
      new InsertAction().execute(new InsertInput(TABLE).withRecords(records));
      Aggregate count = AggregateOperator.COUNT.of("id");
      AggregateInput input = new AggregateInput(TABLE).withAggregate(count).withStreamedRowLimit(1002);
      AggregateOutput output = new StreamedAggregateEvaluator().execute(input);
      assertThat(output.getRowsScanned()).isEqualTo(1002);
      assertThat(output.getTruncated()).isTrue();
      assertThat(output.getResults().getFirst().getAggregateValue(count)).isEqualTo(1002);
      input.withStreamedRowLimit(2000);
      output = new StreamedAggregateEvaluator().execute(input);
      assertThat(output.getRowsScanned()).isEqualTo(1010);
      assertThat(output.getTruncated()).isFalse();
   }



   /*******************************************************************************
    ** Joined operands and main-row sampling preserve join multiplicity.
    *******************************************************************************/
   @Test
   void joinedOperandsAndSampling() throws Exception
   {
      String joined = "analysisJoined";
      QContext.getQInstance().addTable(table(joined, "analysisBackend").withField(new QFieldMetaData("fixtureId", QFieldType.INTEGER)));
      QContext.getQInstance().addJoin(new QJoinMetaData().withName("analysisJoin").withLeftTable(TABLE).withRightTable(joined)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "fixtureId")));
      new InsertAction().execute(new InsertInput(joined).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("fixtureId", 1).withValue("x", 1).withValue("y", 2),
         new QRecord().withValue("id", 2).withValue("fixtureId", 1).withValue("x", 3).withValue("y", 6),
         new QRecord().withValue("id", 3).withValue("fixtureId", 2).withValue("x", 8).withValue("y", 16))));
      Aggregate percentile = Aggregate.percentile("j.x", 0.5);
      Aggregate correlation = AggregateOperator.CORR.of("j.x").withOtherFieldName("j.y");
      Aggregate count = AggregateOperator.COUNT.of("id");
      AggregateInput input = new AggregateInput(TABLE).withQueryJoin(new QueryJoin(TABLE, joined).withAlias("j").withType(QueryJoin.Type.INNER))
         .withAggregate(percentile).withAggregate(correlation).withAggregate(count);
      AggregateOutput output = new AggregateAction().execute(input);
      assertThat((BigDecimal) output.getResults().getFirst().getAggregateValue(percentile)).isEqualByComparingTo("3");
      assertThat((BigDecimal) output.getResults().getFirst().getAggregateValue(correlation)).isEqualByComparingTo("1");
      equivalent(new StreamedAggregateEvaluator().execute(input), output);
      output = new AggregateAction().execute(input.withSamplePercent(new BigDecimal("50")));
      assertThat(output.getResults().getFirst().getAggregateValue(count)).isEqualTo(2);
   }



   /*******************************************************************************
    ** Record locks and caller filters also restrict the streamed request.
    *******************************************************************************/
   @Test
   void recordLocks() throws Exception
   {
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("analysisRows"));
      QContext.getQInstance().getTable(TABLE).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("analysisRows").withFieldName("id"));
      QContext.getQSession().withSecurityKeyValue("analysisRows", 3);
      Aggregate count = AggregateOperator.COUNT.of("id");
      AggregateInput input = new AggregateInput(TABLE).withAggregate(count).withAggregate(Aggregate.percentile("x", 0.5))
         .withFilter(new QQueryFilter(new QFilterCriteria("x", QCriteriaOperator.GREATER_THAN, 0)));
      input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      AggregateOutput output = new AggregateAction().execute(input);
      assertThat(output.getResults().getFirst().getAggregateValue(count)).isEqualTo(1);
      equivalent(new StreamedAggregateEvaluator().execute(input), output);
   }
}
