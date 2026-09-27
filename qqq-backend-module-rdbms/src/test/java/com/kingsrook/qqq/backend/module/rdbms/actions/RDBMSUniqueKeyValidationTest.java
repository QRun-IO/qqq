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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.statusmessages.DuplicateKeyBadInputStatusMessage;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Framework uniqueness must reject writes before SQL, without a physical UNIQUE
 ** constraint masking a missing validation or using public reads to verify storage.
 *******************************************************************************/
class RDBMSUniqueKeyValidationTest extends RDBMSActionTest
{
   private static final String TABLE_NAME = "uniqueUpdateRecord";
   private QTableMetaData table;



   /*******************************************************************************
    ** Deliberately permit duplicate business keys in storage; only the PK is unique.
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      primeTestDatabase();
      executeSql("CREATE TABLE unique_update_record (record_id INT PRIMARY KEY, carrier_code VARCHAR(20), service_code VARCHAR(20), tenant_id INT, amount DECIMAL(12,2), marker VARCHAR(30))");
      executeSql("INSERT INTO unique_update_record VALUES (1,'UPS','2',1,1.00,'Target'),(2,'UPS','G',2,2.00,'Owner'),(3,'DHL','2',1,3.00,'Other')");
      table = new QTableMetaData().withName(TABLE_NAME).withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("unique_update_record"))
         .withPrimaryKeyField("recordId")
         .withField(new QFieldMetaData("recordId", QFieldType.INTEGER).withBackendName("record_id"))
         .withField(new QFieldMetaData("carrierCode", QFieldType.STRING).withBackendName("carrier_code"))
         .withField(new QFieldMetaData("serviceCode", QFieldType.STRING).withBackendName("service_code"))
         .withField(new QFieldMetaData("tenantId", QFieldType.INTEGER).withBackendName("tenant_id"))
         .withField(new QFieldMetaData("amount", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("marker", QFieldType.STRING))
         .withUniqueKey(new UniqueKey("carrierCode", "serviceCode"));
      QContext.getQInstance().addTable(table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void removeFixture() throws Exception
   {
      executeSql("DROP TABLE IF EXISTS unique_update_record");
   }



   /*******************************************************************************
    ** An absent component is the stored value, not an explicit null component.
    *******************************************************************************/
   @Test
   void testSparseCompositeDuplicateIsRejectedBeforeStorageChanges() throws Exception
   {
      List<List<String>> before = snapshot();
      UpdateOutput output = update(patch("1").withValue("serviceCode", "G"));
      assertDuplicateAndUnchanged(output, before);
   }



   /*******************************************************************************
    ** The target remains writable while a different tenant's conflicting owner is hidden.
    *******************************************************************************/
   @Test
   void testHiddenOwnerStillConflictsWithoutChangingReadVisibility() throws Exception
   {
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("uniqueTenant"));
      table.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("uniqueTenant").withFieldName("tenantId"));
      QContext.getQSession().withSecurityKeyValue("uniqueTenant", 1);
      var session = QContext.getQSession();
      var locks = table.getRecordSecurityLocks();
      assertThat(new QueryAction().execute(new QueryInput(TABLE_NAME)).getRecords()).extracting(record -> record.getValueInteger("recordId"))
         .containsExactlyInAnyOrder(1, 3);
      List<List<String>> before = snapshot();
      UpdateOutput output = update(patch(1).withValue("serviceCode", "G"));
      assertAll(() -> assertDuplicateAndUnchanged(output, before),
         () -> assertThat(new QueryAction().execute(new QueryInput(TABLE_NAME)).getRecords()).extracting(record -> record.getValueInteger("recordId"))
            .containsExactlyInAnyOrder(1, 3),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertSame(locks, table.getRecordSecurityLocks()));
   }



   /*******************************************************************************
    ** A sole self match is valid; an additional historical owner cannot be overwritten.
    *******************************************************************************/
   @Test
   void testUnchangedSelfCannotHideASecondExistingOwner() throws Exception
   {
      UpdateOutput allowed = update(patch("1").withValue("serviceCode", "2"));
      assertThat(allowed.getRecords().get(0).getErrors()).isEmpty();
      executeSql("UPDATE unique_update_record SET service_code='2' WHERE record_id=2");
      List<List<String>> before = snapshot();
      UpdateOutput output = update(patch(1).withValue("serviceCode", "2").withValue("marker", "Changed again"));
      assertDuplicateAndUnchanged(output, before);
   }



   /*******************************************************************************
    ** Sparse validation needs raw hidden/heavy components but must not hydrate the patch.
    *******************************************************************************/
   @Test
   void testHiddenHeavyStoredComponentIsAvailableOnlyToValidation() throws Exception
   {
      table.getField("carrierCode").withIsHidden(true).withIsHeavy(true);
      assertThat(new QueryAction().execute(new QueryInput(TABLE_NAME)).getRecords()).allSatisfy(record -> assertFalse(record.getValues().containsKey("carrierCode")));
      QRecord record = patch(1).withValue("serviceCode", "G");
      List<List<String>> before = snapshot();
      UpdateOutput output = update(record);
      assertAll(() -> assertDuplicateAndUnchanged(output, before),
         () -> assertFalse(record.getValues().containsKey("carrierCode"), "Privileged stored values must not be added to the patch"),
         () -> assertFalse(output.getRecords().get(0).getValues().containsKey("carrierCode"), "Privileged stored values must not be returned"));
   }



   /*******************************************************************************
    ** Conflict lookup must see the caller's uncommitted row, retain its connection,
    ** and allow the key again after the caller rolls that row back.
    *******************************************************************************/
   @Test
   void testCallerTransactionConflictAndRollback() throws Exception
   {
      UpdateOutput output;
      List<List<String>> before;
      List<List<String>> after;
      boolean connectionWasClosed;
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())))
      {
         try(Statement statement = transaction.getConnection().createStatement())
         {
            statement.executeUpdate("INSERT INTO unique_update_record VALUES (4,'UPS','LOCAL',1,4.00,'Uncommitted owner')");
         }
         assertEquals(3, snapshot().size(), "The other connection must not see the uncommitted owner");
         before = snapshot(transaction.getConnection());
         output = new UpdateAction().execute(new UpdateInput(TABLE_NAME).withInputSource(QInputSource.USER)
            .withTransaction(transaction).withRecord(patch(1).withValue("serviceCode", "LOCAL")));
         connectionWasClosed = transaction.getConnection().isClosed();
         after = snapshot(transaction.getConnection());
         transaction.rollback();
      }
      UpdateOutput recovered = update(patch(1).withValue("serviceCode", "LOCAL"));
      assertAll(() -> assertDuplicate(output),
         () -> assertEquals(before, after, "Denied update must not mutate inside the caller's transaction"),
         () -> assertFalse(connectionWasClosed),
         () -> assertThat(recovered.getRecords().get(0).getErrors()).isEmpty(),
         () -> assertEquals(3, snapshot().size()),
         () -> assertEquals("LOCAL", snapshot().get(0).get(2)));
   }



   /*******************************************************************************
    ** Separate fragments can form an unchecked composite key in backend write order.
    *******************************************************************************/
   @Test
   void testRepeatedTargetPrimaryKeysRejectBothSparseFragments() throws Exception
   {
      executeSql("UPDATE unique_update_record SET carrier_code='FEDEX' WHERE record_id=1");
      List<List<String>> before = snapshot();
      UpdateOutput output = update(patch(1).withValue("carrierCode", "UPS"), patch("1").withValue("serviceCode", "G"));
      assertAll(() -> assertThat(output.getRecords()).hasSize(2).allSatisfy(record -> assertThat(record.getErrors()).isNotEmpty()),
         () -> assertEquals(before, snapshot(), "Both fragments must be rejected before either reaches SQL"));
   }



   /*******************************************************************************
    ** Native DECIMAL equality sees 2.0 and stored 2.00 as the same key.
    *******************************************************************************/
   @Test
   void testDecimalScaleDoesNotHideANativeConflict() throws Exception
   {
      table.setUniqueKeys(List.of(new UniqueKey("amount")));
      List<List<String>> before = snapshot();
      UpdateOutput output = update(patch(1).withValue("amount", new BigDecimal("2.0")));
      assertDuplicateAndUnchanged(output, before);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord patch(Serializable primaryKey)
   {
      return new QRecord().withValue("recordId", primaryKey).withValue("marker", "Attempted update");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private UpdateOutput update(QRecord... records) throws Exception
   {
      return new UpdateAction().execute(new UpdateInput(TABLE_NAME).withInputSource(QInputSource.USER).withRecords(List.of(records)));
   }



   /*******************************************************************************
    ** A not-found/access error cannot masquerade as correct unique-key validation.
    *******************************************************************************/
   private void assertDuplicate(UpdateOutput output)
   {
      assertThat(output.getRecords()).hasSize(1);
      assertThat(output.getRecords().get(0).getErrors()).anyMatch(error -> error instanceof DuplicateKeyBadInputStatusMessage);
   }



   /*******************************************************************************
    ** Check independent SQL even when the expected validation error is absent.
    *******************************************************************************/
   private void assertDuplicateAndUnchanged(UpdateOutput output, List<List<String>> before) throws Exception
   {
      List<List<String>> after = snapshot();
      assertAll(() -> assertDuplicate(output), () -> assertEquals(before, after, "Denied update must not reach storage"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> snapshot() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         return snapshot(connection);
      }
   }



   /*******************************************************************************
    ** All columns are compared, independently of query masking and row security.
    *******************************************************************************/
   private List<List<String>> snapshot(Connection connection) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM unique_update_record ORDER BY record_id"))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(result.getString(column));
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
