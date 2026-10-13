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

package com.kingsrook.qqq.backend.module.postgres.actions;


import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.SequenceResyncResult;
import com.kingsrook.qqq.backend.core.modules.backend.SequenceResyncResult.Status;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Tests for resyncKeySequences: after rows are inserted with explicit keys,
 ** the next generated key must be max+1.
 *******************************************************************************/
class PostgreSQLKeySequenceResyncTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void dropFixtures() throws Exception
   {
      executeSql("DROP TABLE IF EXISTS seq_resync_generated");
      executeSql("DROP TABLE IF EXISTS seq_resync_plain");
   }



   /*******************************************************************************
    ** serial / identity column: explicit inserts leave the sequence behind;
    ** after the resync the next generated insert gets max+1.
    *******************************************************************************/
   @Test
   void testNextInsertGetsMaxPlusOne() throws Exception
   {
      executeSql("CREATE TABLE seq_resync_generated (id SERIAL PRIMARY KEY, name VARCHAR(40))");
      executeSql("INSERT INTO seq_resync_generated (id, name) VALUES (10, 'a'), (20, 'b'), (35, 'c')");

      QTableMetaData table = defineTable("seqResyncGenerated", "seq_resync_generated");
      List<SequenceResyncResult> results = resync(table);

      assertEquals(1, results.size());
      assertEquals("seqResyncGenerated", results.get(0).getTableName());
      assertEquals(Status.RESYNCED, results.get(0).getStatus());
      assertEquals(36L, results.get(0).getNewValue());

      QContext.getQInstance().addTable(table);
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("seqResyncGenerated");
      insertInput.setRecords(List.of(new QRecord().withValue("name", "d")));
      QRecord inserted = new InsertAction().execute(insertInput).getRecords().get(0);
      assertEquals(36, inserted.getValueInteger("id"));
   }



   /*******************************************************************************
    ** an empty table starts from 1, and a second resync is harmless.
    *******************************************************************************/
   @Test
   void testEmptyTableAndRepeat() throws Exception
   {
      executeSql("CREATE TABLE seq_resync_generated (id SERIAL PRIMARY KEY, name VARCHAR(40))");
      QTableMetaData table = defineTable("seqResyncGenerated", "seq_resync_generated");

      assertEquals(1L, resync(table).get(0).getNewValue());
      assertEquals(1L, resync(table).get(0).getNewValue());
   }



   /*******************************************************************************
    ** a table whose key has no sequence is reported as not applicable.
    *******************************************************************************/
   @Test
   void testTableWithoutSequenceIsNotApplicable() throws Exception
   {
      executeSql("CREATE TABLE seq_resync_plain (id INTEGER PRIMARY KEY, name VARCHAR(40))");
      executeSql("INSERT INTO seq_resync_plain (id, name) VALUES (7, 'a')");

      List<SequenceResyncResult> results = resync(defineTable("seqResyncPlain", "seq_resync_plain"));
      assertEquals(Status.NOT_APPLICABLE, results.get(0).getStatus());
      assertEquals(null, results.get(0).getNewValue());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<SequenceResyncResult> resync(QTableMetaData table) throws Exception
   {
      QBackendMetaData backend = QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      return (new QBackendModuleDispatcher().getQBackendModule(backend).resyncKeySequences(backend, List.of(table)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData defineTable(String name, String sqlName)
   {
      return (new QTableMetaData()
         .withName(name)
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withBackendDetails(new PostgreSQLTableBackendDetails().withTableName(sqlName))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING)));
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
