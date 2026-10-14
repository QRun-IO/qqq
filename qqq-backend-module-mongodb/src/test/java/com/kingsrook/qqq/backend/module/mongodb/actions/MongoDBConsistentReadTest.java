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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Test for MongoDB's consistent-read transaction.  Our testcontainer is a single
 ** standalone mongo - which can't do snapshot reads - so this covers the
 ** documented fallback:  a plain session, and a PER_TABLE guarantee - and that
 ** reads through the transaction work.
 *******************************************************************************/
class MongoDBConsistentReadTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStandaloneFallsBackToPerTable() throws QException
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecords(List.of(new QRecord().withValue("firstName", "Darin"))));

      try(QBackendTransaction transaction = QBackendTransaction.openConsistentReadFor(new ConsistentReadInput(TestUtils.DEFAULT_BACKEND_NAME)))
      {
         assertThat(transaction).isInstanceOf(MongoDBConsistentReadTransaction.class);
         assertEquals(ConsistencyGuarantee.PER_TABLE, transaction.getConsistencyGuarantee());
         assertThat(transaction.getExportedSnapshotId()).isEmpty();

         List<QRecord> records = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON).withTransaction(transaction)).getRecords();
         assertEquals(1, records.size());
         assertEquals("Darin", records.get(0).getValueString("firstName"));
      }
   }



   /*******************************************************************************
    ** Importing a snapshot isn't something mongo can do.
    *******************************************************************************/
   @Test
   void testImportNotSupported()
   {
      ConsistentReadInput input = new ConsistentReadInput(TestUtils.DEFAULT_BACKEND_NAME).withImportSnapshotId("abc");
      assertThatThrownBy(() -> QBackendTransaction.openConsistentReadFor(input)).isInstanceOf(QException.class).hasMessageContaining("not supported");
   }

}
