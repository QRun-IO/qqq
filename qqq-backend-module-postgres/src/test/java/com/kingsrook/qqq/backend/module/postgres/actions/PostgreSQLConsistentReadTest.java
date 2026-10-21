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
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyScope;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Test class for the PostgreSQL consistent-read transaction (REPEATABLE READ,
 ** READ ONLY, with an exported snapshot other connections can import).
 *******************************************************************************/
class PostgreSQLConsistentReadTest extends BaseTest
{

   /*******************************************************************************
    ** Rows committed to any table by another connection after the transaction is
    ** opened are invisible to every table read through the transaction - even
    ** though none of the tables had been read when the writer ran.  And a second
    ** connection can attach to the exported snapshot, and sees the same thing.
    *******************************************************************************/
   @Test
   void testConcurrentWriterInvisibleAndSnapshotCanBeImported() throws Exception
   {
      int peopleBefore = count(TestUtils.TABLE_NAME_PERSON, null);
      int cardsBefore  = count(TestUtils.TABLE_NAME_PERSONAL_ID_CARD, null);

      try(QBackendTransaction exporter = QBackendTransaction.openConsistentReadFor(new ConsistentReadInput(TestUtils.DEFAULT_BACKEND_NAME)))
      {
         assertEquals(ConsistencyGuarantee.ONE_INSTANT, exporter.getConsistencyGuarantee());
         Optional<String> snapshotId = exporter.getExportedSnapshotId();
         assertThat(snapshotId).isPresent();

         ///////////////////////////////////////////////////////////////////////////
         // another connection inserts into both tables (and commits) mid-snapshot //
         ///////////////////////////////////////////////////////////////////////////
         try(Connection writer = ConnectionManager.getConnection(TestUtils.defineBackend()))
         {
            QueryManager.executeUpdate(writer, "INSERT INTO person (first_name, last_name, email) VALUES ('Mid', 'Snapshot', 'mid@snapshot.com')");
            QueryManager.executeUpdate(writer, "INSERT INTO personal_id_card (person_id, id_number) VALUES (1, 'MID-SNAPSHOT')");
         }

         assertEquals(peopleBefore, count(TestUtils.TABLE_NAME_PERSON, exporter));
         assertEquals(cardsBefore, count(TestUtils.TABLE_NAME_PERSONAL_ID_CARD, exporter));

         ///////////////////////////////////////////////////////////////////////
         // a second connection attaches to the exported snapshot - and sees  //
         // the same instant, not the rows committed since                    //
         ///////////////////////////////////////////////////////////////////////
         try(QBackendTransaction importer = QBackendTransaction.openConsistentReadFor(new ConsistentReadInput(TestUtils.DEFAULT_BACKEND_NAME).withImportSnapshotId(snapshotId.get())))
         {
            assertEquals(ConsistencyGuarantee.ONE_INSTANT, importer.getConsistencyGuarantee());
            assertEquals(peopleBefore, count(TestUtils.TABLE_NAME_PERSON, importer));
            assertEquals(cardsBefore, count(TestUtils.TABLE_NAME_PERSONAL_ID_CARD, importer));
         }

         ///////////////////////////////////////////////////////
         // outside of the snapshot, the new rows are visible //
         ///////////////////////////////////////////////////////
         assertEquals(peopleBefore + 1, count(TestUtils.TABLE_NAME_PERSON, null));
         assertEquals(cardsBefore + 1, count(TestUtils.TABLE_NAME_PERSONAL_ID_CARD, null));
      }
   }



   /*******************************************************************************
    ** The transaction is read only - writes through it fail - and TABLE scope
    ** doesn't export a snapshot.
    *******************************************************************************/
   @Test
   void testReadOnlyAndTableScope() throws Exception
   {
      try(QBackendTransaction transaction = QBackendTransaction.openConsistentReadFor(new ConsistentReadInput(TestUtils.DEFAULT_BACKEND_NAME).withScope(ConsistencyScope.TABLE)))
      {
         assertThat(transaction.getExportedSnapshotId()).isEmpty();

         Connection connection = ((RDBMSTransaction) transaction).getConnection();
         assertThatThrownBy(() -> QueryManager.executeUpdate(connection, "INSERT INTO person (first_name, last_name, email) VALUES ('No', 'Way', 'no@way.com')"))
            .hasMessageContaining("read-only");
      }
   }



   /*******************************************************************************
    ** A bad snapshot id is refused (before it gets near any SQL), and a well-formed
    ** id that doesn't exist is an error too.
    *******************************************************************************/
   @Test
   void testBadSnapshotIds()
   {
      ConsistentReadInput injection = new ConsistentReadInput(TestUtils.DEFAULT_BACKEND_NAME).withImportSnapshotId("1'; DROP TABLE person; --");
      assertThatThrownBy(() -> QBackendTransaction.openConsistentReadFor(injection)).isInstanceOf(QException.class).hasMessageContaining("Invalid snapshot id");

      ConsistentReadInput missing = new ConsistentReadInput(TestUtils.DEFAULT_BACKEND_NAME).withImportSnapshotId("00000000-00000000-1");
      assertThatThrownBy(() -> QBackendTransaction.openConsistentReadFor(missing)).isInstanceOf(QException.class);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private int count(String tableName, QBackendTransaction transaction) throws QException
   {
      CountInput countInput = new CountInput(tableName);
      countInput.setTransaction(transaction);
      return new CountAction().execute(countInput).getCount();
   }

}
