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

package com.kingsrook.qqq.backend.module.sqlite.actions;


import java.sql.Connection;
import java.sql.Statement;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.sqlite.BaseTest;
import com.kingsrook.qqq.backend.module.sqlite.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Test class for the SQLite consistent-read transaction (in WAL mode, where a
 ** writer can commit while a reader holds its snapshot).
 *******************************************************************************/
class SQLiteConsistentReadTest extends BaseTest
{

   /*******************************************************************************
    ** Rows committed to any table by another connection after the transaction is
    ** opened are invisible to every table read through the transaction - even
    ** though none of the tables had been read when the writer ran.
    *******************************************************************************/
   @Test
   void testConcurrentWriterIsInvisibleToEveryTable() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         try(Statement statement = connection.createStatement())
         {
            statement.execute("PRAGMA journal_mode=WAL");
         }
      }

      int peopleBefore = count(TestUtils.TABLE_NAME_PERSON, null);
      int cardsBefore  = count(TestUtils.TABLE_NAME_PERSONAL_ID_CARD, null);

      try(QBackendTransaction transaction = QBackendTransaction.openConsistentReadFor(new ConsistentReadInput(TestUtils.DEFAULT_BACKEND_NAME)))
      {
         assertEquals(ConsistencyGuarantee.ONE_INSTANT, transaction.getConsistencyGuarantee());
         assertThat(transaction.getExportedSnapshotId()).isEmpty();

         try(Connection writer = ConnectionManager.getConnection(TestUtils.defineBackend()))
         {
            QueryManager.executeUpdate(writer, "INSERT INTO person (first_name, last_name, email) VALUES ('Mid', 'Snapshot', 'mid@snapshot.com')");
            QueryManager.executeUpdate(writer, "INSERT INTO personal_id_card (person_id, id_number) VALUES (1, 'MID-SNAPSHOT')");
         }

         assertEquals(peopleBefore, count(TestUtils.TABLE_NAME_PERSON, transaction));
         assertEquals(cardsBefore, count(TestUtils.TABLE_NAME_PERSONAL_ID_CARD, transaction));
      }

      assertEquals(peopleBefore + 1, count(TestUtils.TABLE_NAME_PERSON, null));
      assertEquals(cardsBefore + 1, count(TestUtils.TABLE_NAME_PERSONAL_ID_CARD, null));
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
