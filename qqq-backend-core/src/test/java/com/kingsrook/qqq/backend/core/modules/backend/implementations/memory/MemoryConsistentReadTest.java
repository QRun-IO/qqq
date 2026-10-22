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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for the memory backend's consistent-read transaction, and for the
 ** QBackendModuleInterface default (unsupported).
 *******************************************************************************/
class MemoryConsistentReadTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfter()
   {
      MemoryRecordStore.getInstance().reset();
      MemoryRecordStore.resetStatistics();
   }



   /*******************************************************************************
    ** Changes (insert, update, delete) made to any table after the transaction is
    ** opened are invisible to every table read through it - but visible to reads
    ** outside of it.
    *******************************************************************************/
   @Test
   void testWritesAfterOpenAreInvisibleToAllTables() throws QException
   {
      insert(TestUtils.TABLE_NAME_PERSON_MEMORY, new QRecord().withValue("id", 1).withValue("firstName", "Homer"), new QRecord().withValue("id", 2).withValue("firstName", "Marge"));
      insert(TestUtils.TABLE_NAME_SHAPE, new QRecord().withValue("id", 1).withValue("name", "Triangle"));

      try(QBackendTransaction transaction = QBackendTransaction.openConsistentReadFor(new ConsistentReadInput(TestUtils.MEMORY_BACKEND_NAME)))
      {
         assertEquals(ConsistencyGuarantee.ONE_INSTANT, transaction.getConsistencyGuarantee());
         assertThat(transaction.getExportedSnapshotId()).isEmpty();

         ///////////////////////////////////////////////////////////////
         // a concurrent writer: inserts to both tables, update, delete //
         ///////////////////////////////////////////////////////////////
         insert(TestUtils.TABLE_NAME_PERSON_MEMORY, new QRecord().withValue("id", 3).withValue("firstName", "Bart"));
         insert(TestUtils.TABLE_NAME_SHAPE, new QRecord().withValue("id", 2).withValue("name", "Square"));
         new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("id", 1).withValue("firstName", "Homer J")));
         new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withPrimaryKey(2));

         ///////////////////////////////////////////////////////////
         // through the transaction:  exactly what was there then //
         ///////////////////////////////////////////////////////////
         List<QRecord> people = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withTransaction(transaction)).getRecords();
         assertThat(people).extracting(r -> r.getValueString("firstName")).containsExactlyInAnyOrder("Homer", "Marge");
         assertEquals(1, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_SHAPE).withTransaction(transaction)).getRecords().size());
         assertEquals(2, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withTransaction(transaction)).getCount());
         assertEquals(1, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_SHAPE).withTransaction(transaction)).getCount());

         ///////////////////////////////////////////////
         // outside of the transaction: the new state //
         ///////////////////////////////////////////////
         List<QRecord> livePeople = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getRecords();
         assertThat(livePeople).extracting(r -> r.getValueString("firstName")).containsExactlyInAnyOrder("Homer J", "Bart");
         assertEquals(2, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_SHAPE)).getRecords().size());
      }

      ////////////////////////////////////////////////////////////////////
      // and reads on this thread aren't left stuck on the old snapshot //
      ////////////////////////////////////////////////////////////////////
      assertEquals(2, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_SHAPE)).getRecords().size());
   }



   /*******************************************************************************
    ** An unknown backend name is an error, not an NPE.
    *******************************************************************************/
   @Test
   void testUnknownBackend()
   {
      ConsistentReadInput input = new ConsistentReadInput("noSuchBackend");
      assertThatThrownBy(() -> QBackendTransaction.openConsistentReadFor(input)).isInstanceOf(QException.class).hasMessageContaining("noSuchBackend");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void insert(String tableName, QRecord... records) throws QException
   {
      new InsertAction().execute(new InsertInput(tableName).withRecords(List.of(records)));
   }

}
