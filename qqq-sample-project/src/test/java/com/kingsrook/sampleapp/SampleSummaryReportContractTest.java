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


import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Grouped summaries retain native SQL totals and keep subtotal rows with groups.
 *******************************************************************************/
class SampleSummaryReportContractTest
{
   /*******************************************************************************
    ** 
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QSession session = new QSession();
      session.setUser(new QUser());
      session.setPermissions(Set.of());
      QContext.init(SampleMetaDataProvider.defineTestInstance(), session);
      executeSql("UPDATE person SET first_name = CASE WHEN id IN (1, 3, 5) THEN 'North' ELSE 'South' END, last_name = CASE WHEN id IN (1, 2, 3) THEN 'Alpha' ELSE 'Beta' END");
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testThreeLevelSubtotalsRollUpOnce() throws Exception
   {
      QReportView view = summaryView().withSummaryFields(List.of("firstName", "lastName", "id"))
         .withOrderByFields(List.of(new QFilterOrderBy("firstName"), new QFilterOrderBy("lastName"), new QFilterOrderBy("id")));
      JSONArray rows = run(view);
      assertEquals(12, rows.length());
      assertEquals(List.of("North|Alpha|1|1", "North|Alpha|1|3", "North|Alpha Total|2|4", "North|Beta|1|5", "North|Beta Total|1|5", "North Total||3|9", "South|Alpha|1|2", "South|Alpha Total|1|2", "South|Beta|1|4", "South|Beta Total|1|4", "South Total||2|6", "Totals||5|15"), signatures(rows));
      assertEquals(1, rows.getJSONObject(0).getInt("id"));
      assertEquals(3, rows.getJSONObject(1).getInt("id"));
      assertTrue(rows.getJSONObject(2).isNull("id"));
      assertNativeTotals(rows);
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testFlatOrderingWithoutSubtotals() throws Exception
   {
      QReportView view = summaryView().withIncludeSummarySubTotals(false)
         .withOrderByFields(List.of(new QFilterOrderBy("count"), new QFilterOrderBy("sum", false)));
      JSONArray rows = run(view);
      assertEquals(List.of("North|Beta|1|5", "South|Beta|1|4", "South|Alpha|1|2", "North|Alpha|2|4", "Totals||5|15"), signatures(rows));
      assertNativeTotals(rows);
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testSubtotalFormulasUseOwnAggregates() throws Exception
   {
      executeSql("UPDATE person SET first_name = 'South' WHERE id = 5");
      QReportView view = summaryView().withOrderByFields(List.of(new QFilterOrderBy("firstName"), new QFilterOrderBy("lastName")))
         .withColumn(new QReportField("mean").withLabel("Mean").withFormula("=DIVIDE(${pivot.sum.id},${pivot.count.id})"));
      JSONArray rows = run(view);
      Map<String, Double> means = new HashMap<>();
      for(int i = 0; i < rows.length(); i++)
      {
         JSONObject row = rows.getJSONObject(i);
         if(row.isNull("lastName"))
         {
            means.put(row.getString("firstName"), row.getDouble("mean"));
         }
      }
      assertEquals(2.0, means.get("North Total"), 0.000001);
      assertEquals(3.6667, means.get("South Total"), 0.000001);
      assertEquals(3.0, means.get("Totals"), 0.000001);
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private QReportView summaryView()
   {
      return new QReportView().withName("summary").withLabel("Summary").withType(ReportType.SUMMARY).withDataSourceName("people")
         .withSummaryFields(List.of("firstName", "lastName")).withIncludeSummarySubTotals(true).withIncludeTotalRow(true)
         .withColumns(new ArrayList<>(List.of(new QReportField("count").withLabel("Count").withFormula("${pivot.count.id}"),
            new QReportField("sum").withLabel("Sum").withFormula("${pivot.sum.id}"))));
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private JSONArray run(QReportView view) throws Exception
   {
      QReportMetaData report = new QReportMetaData().withName("grouped")
         .withDataSources(List.of(new QReportDataSource().withName("people").withSourceTable("person"))).withViews(List.of(view));
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ReportInput input = new ReportInput();
      input.setInputSource(QInputSource.USER);
      input.setReportMetaData(report);
      input.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.JSON).withReportOutputStream(bytes));
      assertEquals(0, new GenerateReportAction().execute(input).getTotalRecordCount());
      return new JSONArray(bytes.toString(StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private List<String> signatures(JSONArray rows)
   {
      List<String> result = new ArrayList<>();
      for(int i = 0; i < rows.length(); i++)
      {
         JSONObject row = rows.getJSONObject(i);
         result.add(row.optString("firstName", "") + "|" + row.optString("lastName", "") + "|" + row.getInt("count") + "|" + row.getInt("sum"));
      }
      return result;
   }



   /*******************************************************************************
    ** Validate subtotal arithmetic against an independent native SQL aggregation.
    *******************************************************************************/
   private void assertNativeTotals(JSONArray rows) throws Exception
   {
      Map<String, Integer> counts = new HashMap<>();
      Map<String, Integer> sums = new HashMap<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT first_name, COUNT(*) AS row_count, SUM(id) AS id_sum FROM person GROUP BY first_name"))
      {
         while(result.next())
         {
            counts.put(result.getString("first_name") + " Total", result.getInt("row_count"));
            sums.put(result.getString("first_name") + " Total", result.getInt("id_sum"));
         }
      }
      for(int i = 0; i < rows.length(); i++)
      {
         JSONObject row = rows.getJSONObject(i);
         if(row.isNull("lastName") && !"Totals".equals(row.optString("firstName")))
         {
            assertEquals(counts.get(row.getString("firstName")), row.getInt("count"));
            assertEquals(sums.get(row.getString("firstName")), row.getInt("sum"));
         }
      }
      JSONObject total = rows.getJSONObject(rows.length() - 1);
      assertEquals(5, total.getInt("count"));
      assertEquals(15, total.getInt("sum"));
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private void executeSql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         statement.executeUpdate(sql);
      }
   }



}
