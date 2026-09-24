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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.Aggregate2DTableWidgetRenderer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Column identities must preserve native groups through configured widget JSON.
 *******************************************************************************/
class SampleAggregateWidgetCollisionTest
{
   private QInstance instance;
   private List<List<String>> before;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(before != null)
         {
            assertEquals(before, snapshot());
         }
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** An ordinary default-SYSTEM rendering remains the compatibility control.
    *******************************************************************************/
   @Test
   void testOrdinaryColumnValuesAndSystemControl() throws Exception
   {
      assertNativeCells("first", "second");
   }



   /*******************************************************************************
    ** Compare column cell profiles, avoiding a prescribed encoding or display label.
    ** Asymmetric counts identify each column even if visible labels match.
    *******************************************************************************/
   private void assertNativeCells(String... columnValues) throws Exception
   {
      try(Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
         "INSERT INTO field_lab(name,long_value,truncate_value) VALUES(?,?,?)"))
      {
         int id = 0;
         for(int row : List.of(10, 20))
         {
            for(int column = 0; column < columnValues.length; column++)
            {
               int count = row == 10 ? column + 1 : columnValues.length + 1 - column;
               for(int record = 0; record < count; record++)
               {
                  statement.setString(1, "Widget cell " + ++id);
                  statement.setInt(2, row);
                  statement.setString(3, columnValues[column]);
                  assertEquals(1, statement.executeUpdate());
               }
            }
         }
      }
      before = snapshot();
      QWidgetMetaData widget = new QWidgetMetaData().withName("collisionGrid").withLabel("Collision grid").withType(WidgetType.TABLE.getType())
         .withCodeReference(new QCodeReference(Aggregate2DTableWidgetRenderer.class))
         .withDefaultValue("tableName", "fieldLab").withDefaultValue("valueField", "id")
         .withDefaultValue("rowField", "longValue").withDefaultValue("columnField", "truncateValue").withDefaultValue("orderBys", "row,value");
      instance.addWidget(widget);
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(widget);
      JSONObject json = JsonUtils.toJSONObject(JsonUtils.toJson(new RenderWidgetAction().execute(input).getWidgetData()));
      assertCells(json, columnValues.length);
   }



   /*******************************************************************************
    ** Derive every cell and total directly from SQL, independently of AggregateAction.
    *******************************************************************************/
   private void assertCells(JSONObject json, int expectedColumnCount) throws Exception
   {
      Map<String, Map<Integer, Integer>> nativeColumns = new HashMap<>();
      Map<Integer, Integer> rowTotals = new HashMap<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
         "SELECT long_value,truncate_value,COUNT(id) FROM field_lab GROUP BY long_value,truncate_value ORDER BY long_value,truncate_value"))
      {
         while(result.next())
         {
            int row = result.getInt(1);
            int count = result.getInt(3);
            nativeColumns.computeIfAbsent(result.getString(2), ignored -> new HashMap<>()).put(row, count);
            rowTotals.merge(row, count, Integer::sum);
         }
      }
      assertEquals(expectedColumnCount, nativeColumns.size(), "The native fixture must contain every distinct column group");
      JSONArray columns = json.getJSONArray("columns");
      assertEquals(nativeColumns.size() + 2, columns.length());
      Set<String> accessors = new HashSet<>();
      List<String> dataAccessors = new ArrayList<>();
      Map<String, String> headersByAccessor = new HashMap<>();
      for(int index = 0; index < columns.length(); index++)
      {
         String accessor = columns.getJSONObject(index).getString("accessor");
         assertTrue(accessors.add(accessor), "Serialized columns must have distinct accessors: " + json);
         if(index > 0 && index < columns.length() - 1)
         {
            assertFalse(Set.of("_row", "_total").contains(accessor), "Data column collides with a structural slot");
            dataAccessors.add(accessor);
            headersByAccessor.put(accessor, columns.getJSONObject(index).getString("header"));
         }
      }
      assertEquals("_row", columns.getJSONObject(0).getString("accessor"));
      assertEquals("_total", columns.getJSONObject(columns.length() - 1).getString("accessor"));
      JSONArray rows = json.getJSONArray("rows");
      assertEquals(rowTotals.size() + 1, rows.length());
      Map<Integer, JSONObject> renderedRows = new HashMap<>();
      for(int index = 0; index < rows.length() - 1; index++)
      {
         JSONObject row = rows.getJSONObject(index);
         int identity = Integer.parseInt(String.valueOf(row.get("_row")));
         assertFalse(renderedRows.containsKey(identity), "The renderer must preserve both row identities");
         renderedRows.put(identity, row);
         assertEquals(rowTotals.get(identity), row.getInt("_total"));
      }
      assertEquals(rowTotals.keySet(), renderedRows.keySet());
      JSONObject total = rows.getJSONObject(rows.length() - 1);
      assertEquals("Total", total.getString("_row"));
      assertEquals(rowTotals.values().stream().mapToInt(Integer::intValue).sum(), total.getInt("_total"));
      List<Map<Integer, Integer>> expectedProfiles = new ArrayList<>(nativeColumns.values());
      for(String accessor : dataAccessors)
      {
         Map<Integer, Integer> actual = new HashMap<>();
         for(Map.Entry<Integer, JSONObject> row : renderedRows.entrySet())
         {
            actual.put(row.getKey(), row.getValue().getInt(accessor));
         }
         assertTrue(expectedProfiles.remove(actual), "A native column's cells were lost or changed: " + actual);
         Map.Entry<String, Map<Integer, Integer>> nativeColumn = nativeColumns.entrySet().stream().filter(entry -> entry.getValue().equals(actual)).findFirst().orElseThrow();
         String nativeHeader = String.valueOf(nativeColumn.getKey());
         assertEquals(nativeHeader, headersByAccessor.get(accessor), "Existing headers must remain attached to the same native cells");
         if(!Set.of("_row", "_total").contains(nativeHeader)
            && nativeColumns.keySet().stream().filter(value -> String.valueOf(value).equals(nativeHeader)).count() == 1)
         {
            assertEquals(nativeHeader, accessor, "An ordinary raw column accessor must be preserved");
         }
         assertEquals(actual.values().stream().mapToInt(Integer::intValue).sum(), total.getInt(accessor));
      }
      assertTrue(expectedProfiles.isEmpty(), "Every native column must survive JSON serialization");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> snapshot() throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
            {
               row.add(result.getString(index));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
      assertEquals("jdbc:h2:mem:test_database", connection.getMetaData().getURL());
      return connection;
   }
}
