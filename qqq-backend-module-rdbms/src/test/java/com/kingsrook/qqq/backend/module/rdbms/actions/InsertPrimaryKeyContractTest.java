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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Declared primary-key names and physical key generation are independent.
 *******************************************************************************/
class InsertPrimaryKeyContractTest extends RDBMSActionTest
{
   private static final String TABLE = "insertKeyContract";



   /*******************************************************************************
    ** Remove only this class's native fixture.
    *******************************************************************************/
   @AfterEach
   void removeFixture() throws Exception
   {
      executeSql("DROP TABLE IF EXISTS insert_key_contract");
   }



   /*******************************************************************************
    ** A supplied natural key named id is data, including its backend column name.
    *******************************************************************************/
   @Test
   void testNaturalPrimaryKeyNamedIdIsInsertedAndReturned() throws Exception
   {
      defineManualTable("id");
      QRecord inserted = insert(new QRecord().withValue("id", "ORDER/A#1").withValue("payload", "Original owner"));
      assertThat(inserted.getErrors()).isEmpty();
      assertEquals("ORDER/A#1", inserted.getValueString("id"));
      assertEquals(List.of(List.of("ORDER/A#1", "Original owner")), rows());
      List<List<String>> before = rows();
      assertThrows(QException.class, () -> insert(new QRecord().withValue("id", "ORDER/A#1").withValue("payload", "Overwrite attempt")));
      assertEquals(before, rows());
      QRecord emptyKey = insert(new QRecord().withValue("id", "").withValue("payload", "Empty natural key"));
      assertThat(emptyKey.getErrors()).isEmpty();
      assertEquals("", emptyKey.getValueString("id"));
      assertEquals(List.of(List.of("", "Empty natural key"), List.of("ORDER/A#1", "Original owner")), rows());
   }



   /*******************************************************************************
    ** A renamed manual key is the existing native control for identical storage.
    *******************************************************************************/
   @Test
   void testRenamedNaturalPrimaryKeyIsInsertedAndReturned() throws Exception
   {
      defineManualTable("businessKey");
      QRecord inserted = insert(new QRecord().withValue("businessKey", "CUSTOM/2").withValue("payload", "Manual owner"));
      assertThat(inserted.getErrors()).isEmpty();
      assertEquals("CUSTOM/2", inserted.getValueString("businessKey"));
      assertEquals(List.of(List.of("CUSTOM/2", "Manual owner")), rows());
   }



   /*******************************************************************************
    ** Native identity generation is declared by SQL, not the Java field name.
    ** An ordinary non-key field named id must still be inserted as supplied.
    *******************************************************************************/
   @Test
   void testRenamedGeneratedPrimaryKeyAndOrdinaryIdField() throws Exception
   {
      primeTestDatabase();
      executeSql("CREATE TABLE insert_key_contract(record_key INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, id VARCHAR(40), payload VARCHAR(40))");
      QTableMetaData table = new QTableMetaData().withName(TABLE).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField("recordKey")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("insert_key_contract"))
         .withField(new QFieldMetaData("recordKey", QFieldType.INTEGER).withBackendName("record_key"))
         .withField(new QFieldMetaData("id", QFieldType.STRING))
         .withField(new QFieldMetaData("payload", QFieldType.STRING));
      QContext.getQInstance().addTable(table);
      new QInstanceValidator().revalidate(QContext.getQInstance());
      QRecord first = insert(new QRecord().withValue("id", "external-1").withValue("payload", "First owner"));
      QRecord second = insert(new QRecord().withValue("recordKey", "").withValue("id", "external-2").withValue("payload", "Second owner"));
      QRecord third = insert(new QRecord().withValue("recordKey", " \t ").withValue("id", "external-3").withValue("payload", "Third owner"));
      assertThat(first.getErrors()).isEmpty();
      assertThat(second.getErrors()).isEmpty();
      assertThat(third.getErrors()).isEmpty();
      assertNotNull(first.getValueInteger("recordKey"));
      assertNotNull(second.getValueInteger("recordKey"));
      assertNotNull(third.getValueInteger("recordKey"));
      assertEquals(3, List.of(first, second, third).stream().map(record -> record.getValueInteger("recordKey")).distinct().count());
      assertThat(second.getValueInteger("recordKey")).isNotEqualTo(first.getValueInteger("recordKey"));
      assertEquals(List.of(List.of(first.getValueString("recordKey"), "external-1", "First owner"),
         List.of(second.getValueString("recordKey"), "external-2", "Second owner"), List.of(third.getValueString("recordKey"), "external-3", "Third owner")), rows());
   }



   /*******************************************************************************
    ** Mixed key modes and rejected records retain input order across native pages.
    *******************************************************************************/
   @Test
   void testMixedKeyModesAndExistingErrorsRetainOutputAlignment() throws Exception
   {
      primeTestDatabase();
      executeSql("CREATE TABLE insert_key_contract(record_key INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, payload VARCHAR(40))");
      QContext.getQInstance().addTable(new QTableMetaData().withName(TABLE).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField("recordKey")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("insert_key_contract"))
         .withField(new QFieldMetaData("recordKey", QFieldType.INTEGER).withBackendName("record_key"))
         .withField(new QFieldMetaData("payload", QFieldType.STRING)));
      new QInstanceValidator().revalidate(QContext.getQInstance());
      getBaseRDBMSActionStrategy().setPageSize(3);
      List<QRecord> records = List.of(
         new QRecord().withValue("recordKey", 90).withValue("payload", "Rejected head").withError(new BadInputStatusMessage("Rejected head")),
         new QRecord().withValue("payload", "Generated first"),
         new QRecord().withValue("recordKey", 41).withValue("payload", "Manual first"),
         new QRecord().withValue("payload", "Rejected middle").withError(new BadInputStatusMessage("Rejected middle")),
         new QRecord().withValue("recordKey", 42).withValue("payload", "Manual second"),
         new QRecord().withValue("payload", "Generated second"),
         new QRecord().withValue("payload", "Rejected tail").withError(new BadInputStatusMessage("Rejected tail")),
         new QRecord().withValue("recordKey", 0).withValue("payload", "Zero key"),
         new QRecord().withValue("payload", "Generated after zero"));
      InsertOutput output = new InsertAction().execute(new InsertInput(TABLE).withRecords(records));
      assertThat(output.getRecords()).extracting(record -> record.getValueString("payload")).containsExactly(
         "Rejected head", "Generated first", "Manual first", "Rejected middle", "Manual second", "Generated second", "Rejected tail", "Zero key", "Generated after zero");
      for(int index : List.of(0, 3, 6))
      {
         assertThat(output.getRecords().get(index).getErrors()).singleElement().isInstanceOf(BadInputStatusMessage.class);
         assertEquals(records.get(index).getValue("recordKey"), output.getRecords().get(index).getValue("recordKey"));
      }
      for(int index : List.of(1, 2, 4, 5, 7, 8))
      {
         assertThat(output.getRecords().get(index).getErrors()).isEmpty();
      }
      assertEquals(41, output.getRecords().get(2).getValueInteger("recordKey"));
      assertEquals(42, output.getRecords().get(4).getValueInteger("recordKey"));
      assertEquals(0, output.getRecords().get(7).getValueInteger("recordKey"));
      Integer firstGenerated = output.getRecords().get(1).getValueInteger("recordKey");
      Integer secondGenerated = output.getRecords().get(5).getValueInteger("recordKey");
      Integer thirdGenerated = output.getRecords().get(8).getValueInteger("recordKey");
      assertNotNull(firstGenerated);
      assertNotNull(secondGenerated);
      assertNotNull(thirdGenerated);
      assertThat(List.of(firstGenerated, secondGenerated, thirdGenerated, 0, 41, 42)).doesNotHaveDuplicates();
      assertThat(rows()).containsExactlyInAnyOrder(
         List.of(firstGenerated.toString(), "Generated first"), List.of("41", "Manual first"),
         List.of("42", "Manual second"), List.of(secondGenerated.toString(), "Generated second"),
         List.of("0", "Zero key"), List.of(thirdGenerated.toString(), "Generated after zero"));
   }



   /*******************************************************************************
    ** A primary-key-only table remains valid with supplied or generated identity.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testPrimaryKeyOnlyTable(boolean generated) throws Exception
   {
      primeTestDatabase();
      executeSql("CREATE TABLE insert_key_contract(record_key " + (generated ? "INTEGER GENERATED ALWAYS AS IDENTITY" : "VARCHAR(40)") + " PRIMARY KEY)");
      QContext.getQInstance().addTable(new QTableMetaData().withName(TABLE).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("insert_key_contract"))
         .withField(new QFieldMetaData("id", generated ? QFieldType.INTEGER : QFieldType.STRING).withBackendName("record_key")));
      new QInstanceValidator().revalidate(QContext.getQInstance());
      QRecord record = generated ? new QRecord() : new QRecord().withValue("id", "MANUAL/only");
      QRecord result = insert(record);
      assertThat(result.getErrors()).isEmpty();
      assertNotNull(result.getValue("id"));
      if(!generated)
      {
         assertEquals("MANUAL/only", result.getValueString("id"));
      }
      assertEquals(List.of(List.of(result.getValueString("id"))), rows());
   }



   /*******************************************************************************
    ** Required manual identity fails USER validation before any native row exists.
    *******************************************************************************/
   @Test
   void testMissingRequiredManualKeyIsRejectedWithoutMutation() throws Exception
   {
      defineManualTable("id");
      QContext.getQInstance().getTable(TABLE).getField("id").setIsRequired(true);
      InsertInput input = new InsertInput(TABLE).withRecord(new QRecord().withValue("payload", "Missing key"));
      input.setInputSource(QInputSource.USER);
      InsertOutput output = new InsertAction().execute(input);
      assertThat(output.getRecords()).singleElement().satisfies(record ->
         assertThat(record.getErrors()).anyMatch(error -> error instanceof BadInputStatusMessage));
      assertThat(rows()).isEmpty();
   }



   /*******************************************************************************
    ** No generation flag exists in current QQQ field metadata.
    *******************************************************************************/
   private void defineManualTable(String keyName) throws Exception
   {
      primeTestDatabase();
      executeSql("CREATE TABLE insert_key_contract(business_key VARCHAR(40) PRIMARY KEY, payload VARCHAR(40))");
      QContext.getQInstance().addTable(new QTableMetaData().withName(TABLE).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField(keyName)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("insert_key_contract"))
         .withField(new QFieldMetaData(keyName, QFieldType.STRING).withBackendName("business_key"))
         .withField(new QFieldMetaData("payload", QFieldType.STRING)));
      new QInstanceValidator().revalidate(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insert(QRecord record) throws Exception
   {
      return new InsertAction().execute(new InsertInput(TABLE).withRecord(record)).getRecords().get(0);
   }



   /*******************************************************************************
    ** Verify native values independently of returned QRecords.
    *******************************************************************************/
   private List<List<String>> rows() throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM insert_key_contract ORDER BY 1"))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int i = 1; i <= result.getMetaData().getColumnCount(); i++)
            {
               row.add(result.getString(i));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void executeSql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }
}
