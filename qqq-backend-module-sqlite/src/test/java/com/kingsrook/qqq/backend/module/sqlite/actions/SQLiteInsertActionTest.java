/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.sqlite.actions;


import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;
import com.kingsrook.qqq.backend.module.sqlite.BaseTest;
import com.kingsrook.qqq.backend.module.sqlite.TestUtils;
import com.kingsrook.qqq.backend.module.sqlite.model.metadata.SQLiteTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
public class SQLiteInsertActionTest extends BaseTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInsertNullList() throws QException
   {
      InsertInput insertInput = initInsertRequest();
      insertInput.setRecords(null);
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(0, insertOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInsertEmptyList() throws QException
   {
      InsertInput insertInput = initInsertRequest();
      insertInput.setRecords(Collections.emptyList());
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(0, insertOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInsertOne() throws Exception
   {
      InsertInput insertInput = initInsertRequest();
      QRecord record = new QRecord().withTableName("person")
         .withValue("firstName", "James")
         .withValue("lastName", "Kirk")
         .withValue("email", "jamestk@starfleet.net")
         .withValue("birthDate", "2210-05-20");
      insertInput.setRecords(List.of(record));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(1, insertOutput.getRecords().size(), "Should return 1 row");
      assertNotNull(insertOutput.getRecords().get(0).getValue("id"), "Should have an id in the row");
      // todo - add errors to QRecord? assertTrue(insertResult.getRecords().stream().noneMatch(qrs -> CollectionUtils.nullSafeHasContents(qrs.getErrors())), "There should be no errors");
      assertAnInsertedPersonRecord("James", "Kirk", 6);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInsertMany() throws Exception
   {
      getBaseRDBMSActionStrategyAndActivateCollectingStatistics()
         .setPageSize(2);

      InsertInput insertInput = initInsertRequest();
      QRecord record1 = new QRecord().withTableName("person")
         .withValue("firstName", "Jean-Luc")
         .withValue("lastName", "Picard")
         .withValue("email", "jl@starfleet.net")
         .withValue("birthDate", "2310-05-20");
      QRecord record2 = new QRecord().withTableName("person")
         .withValue("firstName", "William")
         .withValue("lastName", "Riker")
         .withValue("email", "notthomas@starfleet.net")
         .withValue("birthDate", "2320-05-20");
      QRecord record3 = new QRecord().withTableName("person")
         .withValue("firstName", "Beverly")
         .withValue("lastName", "Crusher")
         .withValue("email", "doctor@starfleet.net")
         .withValue("birthDate", "2320-06-26");
      insertInput.setRecords(List.of(record1, record2, record3));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(3, insertOutput.getRecords().size(), "Should return right # of rows");
      assertEquals(6, insertOutput.getRecords().get(0).getValue("id"), "Should have next id in the row");
      assertEquals(7, insertOutput.getRecords().get(1).getValue("id"), "Should have next id in the row");
      assertEquals(8, insertOutput.getRecords().get(2).getValue("id"), "Should have next id in the row");

      Map<String, Integer> statistics = getBaseRDBMSActionStrategy().getStatistics();
      assertEquals(2, statistics.get(BaseRDBMSActionStrategy.STAT_QUERIES_RAN));

      assertAnInsertedPersonRecord("Jean-Luc", "Picard", 6);
      assertAnInsertedPersonRecord("William", "Riker", 7);
      assertAnInsertedPersonRecord("Beverly", "Crusher", 8);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInsertAssociations() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1);

      int originalNoOfOrderLineExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).size();
      int originalNoOfOrderLines          = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_LINE).size();
      int originalNoOfOrders              = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).size();

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(
         new QRecord().withValue("storeId", 1).withValue("billToPersonId", 100).withValue("shipToPersonId", 200)

            .withAssociatedRecord("orderLine", new QRecord().withValue("storeId", 1).withValue("sku", "BASIC1").withValue("quantity", 1)
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-1.1").withValue("value", "LINE-VAL-1")))

            .withAssociatedRecord("orderLine", new QRecord().withValue("storeId", 1).withValue("sku", "BASIC2").withValue("quantity", 2)
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-2.1").withValue("value", "LINE-VAL-2"))
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-2.2").withValue("value", "LINE-VAL-3")))
      ));
      new InsertAction().execute(insertInput);

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(originalNoOfOrders + 1, orders.size());
      assertTrue(orders.stream().anyMatch(r -> Objects.equals(r.getValue("billToPersonId"), 100) && Objects.equals(r.getValue("shipToPersonId"), 200)));

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_LINE);
      assertEquals(originalNoOfOrderLines + 2, orderLines.size());
      assertTrue(orderLines.stream().anyMatch(r -> Objects.equals(r.getValue("sku"), "BASIC1") && Objects.equals(r.getValue("quantity"), 1)));
      assertTrue(orderLines.stream().anyMatch(r -> Objects.equals(r.getValue("sku"), "BASIC2") && Objects.equals(r.getValue("quantity"), 2)));

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(originalNoOfOrderLineExtrinsics + 3, lineItemExtrinsics.size());
      assertTrue(lineItemExtrinsics.stream().anyMatch(r -> Objects.equals(r.getValue("key"), "LINE-EXT-1.1") && Objects.equals(r.getValue("value"), "LINE-VAL-1")));
      assertTrue(lineItemExtrinsics.stream().anyMatch(r -> Objects.equals(r.getValue("key"), "LINE-EXT-2.1") && Objects.equals(r.getValue("value"), "LINE-VAL-2")));
      assertTrue(lineItemExtrinsics.stream().anyMatch(r -> Objects.equals(r.getValue("key"), "LINE-EXT-2.2") && Objects.equals(r.getValue("value"), "LINE-VAL-3")));
   }



   /*******************************************************************************
    ** Manual mapped keys and an ordinary id field retain their separate identities.
    *******************************************************************************/
   @Test
   void testNaturalPrimaryKeyWithOrdinaryIdPreservesOwner() throws Exception
   {
      defineKeyTable("insert_key_natural", "businessKey", QFieldType.STRING, "business_key",
         "CREATE TABLE insert_key_natural (business_key TEXT PRIMARY KEY NOT NULL, id TEXT, payload TEXT)",
         new QFieldMetaData("id", QFieldType.STRING), new QFieldMetaData("payload", QFieldType.STRING));
      QRecord owner = new QRecord().withValue("businessKey", "NATURAL/A#1").withValue("id", "ordinary-id").withValue("payload", "Owner");
      InsertOutput output = new InsertAction().execute(new InsertInput("insert_key_natural").withRecord(owner));
      assertEquals("NATURAL/A#1", output.getRecords().get(0).getValue("businessKey"));
      assertTrue(output.getRecords().get(0).getErrors().isEmpty());
      List<List<String>> before = List.of(List.of("NATURAL/A#1", "ordinary-id", "Owner"));
      assertEquals(before, nativeRows("SELECT business_key, id, payload FROM insert_key_natural ORDER BY business_key"));

      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("insert_key_natural").withRecord(new QRecord()
         .withValue("businessKey", "NATURAL/A#1").withValue("id", "replacement").withValue("payload", "Overwrite"))));
      assertEquals(before, nativeRows("SELECT business_key, id, payload FROM insert_key_natural ORDER BY business_key"));
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("insert_key_natural").withRecord(new QRecord()
         .withValue("id", "missing-key").withValue("payload", "Missing"))));
      assertEquals(before, nativeRows("SELECT business_key, id, payload FROM insert_key_natural ORDER BY business_key"));
   }



   /*******************************************************************************
    ** A primary-key field literally named id must not be omitted when it is supplied.
    *******************************************************************************/
   @Test
   void testNaturalPrimaryKeyNamedIdUsesMappedColumn() throws Exception
   {
      defineKeyTable("insert_key_literal", "id", QFieldType.STRING, "business_key",
         "CREATE TABLE insert_key_literal (business_key TEXT PRIMARY KEY NOT NULL, payload TEXT)",
         new QFieldMetaData("payload", QFieldType.STRING));
      InsertOutput output = new InsertAction().execute(new InsertInput("insert_key_literal").withRecord(new QRecord()
         .withValue("id", "MANUAL/B#2").withValue("payload", "Literal id")));
      assertEquals("MANUAL/B#2", output.getRecords().get(0).getValue("id"));
      assertTrue(output.getRecords().get(0).getErrors().isEmpty());
      assertEquals(List.of(List.of("MANUAL/B#2", "Literal id")), nativeRows("SELECT business_key, payload FROM insert_key_literal"));
   }



   /*******************************************************************************
    ** Homogeneous statement splits must retain the mapping across outer page boundaries.
    *******************************************************************************/
   @Test
   void testMixedPrimaryKeysAcrossPagesKeepReturnedAndStoredMapping() throws Exception
   {
      defineKeyTable("insert_key_generated", "recordKey", QFieldType.INTEGER, "record_key",
         "CREATE TABLE insert_key_generated (record_key INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT, payload TEXT)",
         new QFieldMetaData("id", QFieldType.STRING), new QFieldMetaData("payload", QFieldType.STRING));
      getBaseRDBMSActionStrategy().setPageSize(2);
      List<QRecord> records = new ArrayList<>();
      for(int index = 0; index < 7; index++)
      {
         records.add(new QRecord().withValue("id", "ordinary-" + index).withValue("payload", "Payload " + index));
      }
      records.get(2).setValue("recordKey", 41);
      records.get(4).setValue("recordKey", 70);
      records.get(5).setValue("recordKey", 71);
      InsertOutput output = new InsertAction().execute(new InsertInput("insert_key_generated").withRecords(records));
      List<Integer> expectedKeys = List.of(1, 2, 41, 42, 70, 71, 72);
      List<List<String>> expectedRows = new ArrayList<>();
      assertEquals(7, output.getRecords().size());
      for(int index = 0; index < 7; index++)
      {
         QRecord returned = output.getRecords().get(index);
         assertTrue(returned.getErrors().isEmpty());
         assertEquals(expectedKeys.get(index), returned.getValue("recordKey"));
         assertEquals("ordinary-" + index, returned.getValue("id"));
         assertEquals("Payload " + index, returned.getValue("payload"));
         expectedRows.add(List.of(expectedKeys.get(index).toString(), "ordinary-" + index, "Payload " + index));
      }
      assertEquals(expectedRows, nativeRows("SELECT record_key, id, payload FROM insert_key_generated ORDER BY record_key"));
   }



   /*******************************************************************************
    ** DEFAULT VALUES must return actual native keys for numeric and text-only rows.
    *******************************************************************************/
   @Test
   void testPrimaryKeyOnlyRowsUseNativeDefaults() throws Exception
   {
      defineKeyTable("insert_key_only", "key", QFieldType.INTEGER, "record_key",
         "CREATE TABLE insert_key_only (record_key INTEGER PRIMARY KEY AUTOINCREMENT)");
      getBaseRDBMSActionStrategy().setPageSize(2);
      InsertOutput generated = new InsertAction().execute(new InsertInput("insert_key_only")
         .withRecords(List.of(new QRecord(), new QRecord(), new QRecord())));
      assertEquals(3, generated.getRecords().size());
      for(int index = 0; index < 3; index++)
      {
         assertTrue(generated.getRecords().get(index).getErrors().isEmpty());
         assertEquals(index + 1, generated.getRecords().get(index).getValue("key"));
      }
      assertEquals(List.of(List.of("1"), List.of("2"), List.of("3")), nativeRows("SELECT record_key FROM insert_key_only ORDER BY record_key"));

      defineKeyTable("insert_key_text_default", "key", QFieldType.STRING, "business_key",
         "CREATE TABLE insert_key_text_default (business_key TEXT PRIMARY KEY NOT NULL DEFAULT 'NATIVE/TEXT')");
      InsertOutput text = new InsertAction().execute(new InsertInput("insert_key_text_default")
         .withRecords(List.of(new QRecord(), new QRecord().withValue("key", "MANUAL/TEXT"))));
      assertEquals(2, text.getRecords().size());
      assertTrue(text.getRecords().stream().allMatch(record -> record.getErrors().isEmpty()));
      assertEquals("NATIVE/TEXT", text.getRecords().get(0).getValue("key"));
      assertEquals("MANUAL/TEXT", text.getRecords().get(1).getValue("key"));
      assertEquals(List.of(List.of("MANUAL/TEXT"), List.of("NATIVE/TEXT")), nativeRows("SELECT business_key FROM insert_key_text_default ORDER BY business_key"));
   }



   /*******************************************************************************
    ** Each fixture owns only its additional tables in the existing test database.
    *******************************************************************************/
   @AfterEach
   void cleanUpKeyTables() throws Exception
   {
      for(String table : List.of("insert_key_natural", "insert_key_literal", "insert_key_generated", "insert_key_only", "insert_key_text_default"))
      {
         executeKeySql("DROP TABLE IF EXISTS " + table);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void defineKeyTable(String tableName, String primaryKey, QFieldType type, String column, String ddl, QFieldMetaData... otherFields) throws Exception
   {
      executeKeySql("DROP TABLE IF EXISTS " + tableName);
      executeKeySql(ddl);
      QTableMetaData table = new QTableMetaData().withName(tableName).withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new SQLiteTableBackendDetails().withTableName(tableName)).withPrimaryKeyField(primaryKey)
         .withField(new QFieldMetaData(primaryKey, type).withBackendName(column));
      for(QFieldMetaData field : otherFields)
      {
         table.withField(field);
      }
      QContext.getQInstance().addTable(table);
      new QInstanceValidator().revalidate(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void executeKeySql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()); Statement statement = connection.createStatement())
      {
         statement.executeUpdate(sql);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> nativeRows(String sql) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      runTestSql(sql, resultSet ->
      {
         while(resultSet.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= resultSet.getMetaData().getColumnCount(); column++)
            {
               row.add(resultSet.getString(column));
            }
            rows.add(row);
         }
      });
      return rows;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertAnInsertedPersonRecord(String firstName, String lastName, Integer id) throws Exception
   {
      runTestSql("SELECT * FROM person WHERE last_name = '" + lastName + "'", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals(id, rs.getInt("id"));
            assertEquals(firstName, rs.getString("first_name"));
            assertNotNull(rs.getString("create_date"));
            assertNotNull(rs.getString("modify_date"));
         }
         assertEquals(1, rowsFound);
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private InsertInput initInsertRequest()
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      return insertInput;
   }

}