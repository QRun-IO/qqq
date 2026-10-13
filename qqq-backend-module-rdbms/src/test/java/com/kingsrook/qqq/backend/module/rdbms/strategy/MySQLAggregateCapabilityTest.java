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

package com.kingsrook.qqq.backend.module.rdbms.strategy;


import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Exercise the MySQL probe through JDBC with an H2 stand-in for CONVERT_TZ.
 *******************************************************************************/
public class MySQLAggregateCapabilityTest extends BaseTest
{
   private static int probes;



   /*******************************************************************************
    ** Do not leave the simulated MySQL function in the shared H2 database.
    *******************************************************************************/
   @AfterEach
   void cleanup() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         connection.createStatement().execute("DROP ALIAS IF EXISTS CONVERT_TZ");
      }
   }



   /*******************************************************************************
    ** Simulate populated and missing named zones without requiring a MySQL server.
    *******************************************************************************/
   public static String convertTimeZone(String timestamp, String from, String zone)
   {
      probes++;
      assertThat(from).isEqualTo("+00:00");
      return (zone.equals("Asia/Kolkata") || zone.startsWith("+") || zone.startsWith("-") ? timestamp : null);
   }



   /*******************************************************************************
    ** Probe only date requests and cache positive and negative results per backend and zone.
    *******************************************************************************/
   @Test
   void cachedZoneCapabilities() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         connection.createStatement().execute("CREATE ALIAS IF NOT EXISTS CONVERT_TZ FOR '" + getClass().getName() + ".convertTimeZone'");
      }
      probes = 0;
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setVendor(RDBMSBackendMetaData.VENDOR_MYSQL);
      AggregateInput input = new AggregateInput(TestUtils.TABLE_NAME_PERSON);
      QContext.getQSession().withValue(QSession.VALUE_KEY_USER_TIMEZONE, "America/Chicago");
      new BaseRDBMSActionStrategy().getSupportedAggregateCapabilities(input);
      assertThat(probes).isZero();
      input.withGroupBy(new GroupBy(QFieldType.DATE_TIME, "createDate").withBucket(new DateBucket(DateBucketUnit.DAY)));
      for(int i = 0; i < 2; i++)
      {
         assertThat(new BaseRDBMSActionStrategy().getSupportedAggregateCapabilities(input)).doesNotContain(AggregateCapability.DATE_BUCKET);
      }
      assertThat(probes).isEqualTo(1);
      QContext.getQSession().withValue(QSession.VALUE_KEY_USER_TIMEZONE, "Asia/Kolkata");
      for(int i = 0; i < 2; i++)
      {
         assertThat(new BaseRDBMSActionStrategy().getSupportedAggregateCapabilities(input)).contains(AggregateCapability.DATE_BUCKET);
      }
      assertThat(probes).isEqualTo(2);
      QContext.getQSession().withValue(QSession.VALUE_KEY_USER_TIMEZONE, null)
         .withValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES, "-300");
      assertThat(new BaseRDBMSActionStrategy().getSupportedAggregateCapabilities(input)).contains(AggregateCapability.DATE_BUCKET);
      assertThat(probes).isEqualTo(3);
      RDBMSBackendMetaData anotherBackend = TestUtils.defineBackend();
      anotherBackend.setVendor(RDBMSBackendMetaData.VENDOR_MYSQL);
      QContext.getQInstance().getBackends().put(anotherBackend.getName(), anotherBackend);
      assertThat(new BaseRDBMSActionStrategy().getSupportedAggregateCapabilities(input)).contains(AggregateCapability.DATE_BUCKET);
      assertThat(probes).isEqualTo(4);
   }



   /*******************************************************************************
    ** MySQL accepts signed offsets, including explicit UTC, without named-zone tables.
    *******************************************************************************/
   @Test
   void offsetParameters()
   {
      RDBMSBackendMetaData backend = TestUtils.defineBackend();
      backend.setVendor(RDBMSBackendMetaData.VENDOR_MYSQL);
      for(String minutes : List.of("-300", "0", "330"))
      {
         QContext.getQSession().withValue(QSession.VALUE_KEY_USER_TIMEZONE, null)
            .withValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES, minutes);
         GroupBy group = new GroupBy(QFieldType.DATE_TIME, "x").withBucket(new DateBucket(DateBucketUnit.HOUR));
         List<Serializable> params = new ArrayList<>();
         String sql = new BaseRDBMSActionStrategy().getGroupByBucketExpression(group, "x", QFieldType.DATE_TIME, backend, params);
         assertThat(sql).contains("CONVERT_TZ(x, '+00:00', ?)");
         String zone = minutes.equals("-300") ? "-05:00" : minutes.equals("0") ? "+00:00" : "+05:30";
         assertThat(params).containsExactly(zone, zone);
      }
   }
}
