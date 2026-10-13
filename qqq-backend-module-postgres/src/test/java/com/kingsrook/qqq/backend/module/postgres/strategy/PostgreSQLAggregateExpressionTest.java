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

package com.kingsrook.qqq.backend.module.postgres.strategy;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.NumericBucket;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Parameterized PostgreSQL expression contracts; these also run without Docker.
 *******************************************************************************/
class PostgreSQLAggregateExpressionTest
{
   /*******************************************************************************
    ** Clear the session used only for timezone lookup.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Ordered-set and paired aggregates bind values without interpolating client input.
    *******************************************************************************/
   @Test
   void operators()
   {
      PostgreSQLRDBMSActionStrategy strategy = new PostgreSQLRDBMSActionStrategy();
      assertThat(strategy.getSupportedAggregateCapabilities(null)).containsExactlyInAnyOrder(AggregateCapability.values());
      for(AggregateOperator operator : List.of(AggregateOperator.PERCENTILE_CONT, AggregateOperator.PERCENTILE_DISC))
      {
         List<Serializable> params = new ArrayList<>();
         Aggregate aggregate = new Aggregate("x", operator).withPercentile(new BigDecimal("0.3"));
         assertThat(strategy.getAggregateExpression(operator, "x", "y", aggregate, new PostgreSQLBackendMetaData(), params))
            .isEqualTo(operator.name() + "(?) WITHIN GROUP (ORDER BY x)");
         assertThat(params).containsExactly(new BigDecimal("0.3"));
      }
      assertThat(strategy.getAggregateExpression(AggregateOperator.CORR, "x", "y", AggregateOperator.CORR.of("x"), new PostgreSQLBackendMetaData(), new ArrayList<>())).isEqualTo("CORR(y, x)");
      assertThat(strategy.getAggregateExpression(AggregateOperator.AVG_LENGTH, "x", "", AggregateOperator.AVG_LENGTH.of("x"), new PostgreSQLBackendMetaData(), new ArrayList<>())).isEqualTo("AVG(CHAR_LENGTH(x))");
   }



   /*******************************************************************************
    ** Date bucketing binds the session timezone; width_bucket includes the explicit max clamp.
    *******************************************************************************/
   @Test
   void bucketsAndSampling()
   {
      QContext.setQSession(new QSession().withValue(QSession.VALUE_KEY_USER_TIMEZONE, "Asia/Kolkata"));
      PostgreSQLRDBMSActionStrategy strategy = new PostgreSQLRDBMSActionStrategy();
      PostgreSQLBackendMetaData backend = new PostgreSQLBackendMetaData();
      List<Serializable> params = new ArrayList<>();
      GroupBy month = new GroupBy(QFieldType.DATE_TIME, "x").withBucket(new DateBucket(DateBucketUnit.MONTH));
      assertThat(strategy.getGroupByBucketExpression(month, "x", QFieldType.DATE_TIME, backend, params))
         .isEqualTo("CAST(date_trunc('month', ((x AT TIME ZONE 'UTC') AT TIME ZONE ?)) AS DATE)");
      assertThat(params).containsExactly("Asia/Kolkata");
      params.clear();
      strategy.getGroupByBucketExpression(month, "x", QFieldType.DATE, backend, params);
      assertThat(params).isEmpty();
      GroupBy numeric = new GroupBy(QFieldType.DECIMAL, "x").withBucket(new NumericBucket(BigDecimal.ZERO, BigDecimal.TEN, 2));
      assertThat(strategy.getGroupByBucketExpression(numeric, "x", QFieldType.DECIMAL, backend, params))
         .isEqualTo("CASE WHEN x = ? THEN ? ELSE width_bucket(x, ?, ?, ?) END");
      assertThat(params).containsExactly(BigDecimal.TEN, 2, BigDecimal.ZERO, BigDecimal.TEN, 2);
      params.clear();
      assertThat(strategy.getSampledTableExpression("orders", "o", new BigDecimal("25"), params)).isEqualTo("orders AS o TABLESAMPLE SYSTEM (?)");
      assertThat(params).containsExactly(new BigDecimal("25"));
      assertThat(strategy.getSampledTableExpression("orders", "o", null, new ArrayList<>())).isEqualTo("orders AS o");
   }
}
