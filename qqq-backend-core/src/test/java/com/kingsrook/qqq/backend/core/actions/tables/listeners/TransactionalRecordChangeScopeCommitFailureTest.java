/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.listeners;


import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** A required-listener write whose commit fails with an unchecked exception (a
 ** transaction implementation that does not wrap its commit failures) must end
 ** like any other commit failure: the caller sees the failure (never a success),
 ** the transaction is rolled back and poisoned, and the ambient context and the
 ** thread-local scope are restored so the next write starts clean. The commit
 ** outcome is not guessed and nothing is retried.
 *******************************************************************************/
class TransactionalRecordChangeScopeCommitFailureTest extends BaseTest
{
   private static final AtomicReference<FaultTransaction> LAST = new AtomicReference<>();
   private static boolean failCommit;



   /*******************************************************************************
    ** A transaction whose commit can throw an unchecked exception.
    *******************************************************************************/
   static class FaultTransaction extends QBackendTransaction
   {
      boolean rolledBack;
      boolean closed;
      boolean committed;



      @Override
      public void requireAtomicWrites(QTableMetaData table)
      {
      }



      @Override
      public void commit() throws QException
      {
         requireCommitAllowed();
         if(failCommit)
         {
            throw (new IllegalStateException("injected commit failure"));
         }
         committed = true;
      }



      @Override
      public void rollback() throws QException
      {
         rolledBack = true;
         super.rollback();
      }



      @Override
      public void close()
      {
         closed = true;
      }
   }



   /*******************************************************************************
    ** The memory backend, handing out the fault transaction.
    *******************************************************************************/
   public static class FaultModule extends MemoryBackendModule
   {
      @Override
      public String getBackendType()
      {
         return ("faultCommitTest");
      }



      @Override
      public QBackendTransaction openTransaction(AbstractTableActionInput input)
      {
         FaultTransaction transaction = new FaultTransaction();
         LAST.set(transaction);
         return (transaction);
      }
   }



   /*******************************************************************************
    ** A required listener that does nothing.
    *******************************************************************************/
   public static class NoopRequiredListener implements RecordChangeListenerInterface
   {
      @Override
      public boolean isRequired()
      {
         return (true);
      }



      @Override
      public boolean appliesTo(String tableName, RecordChangeType type)
      {
         return (true);
      }



      @Override
      public void onRecordsChanged(RecordChangeEvent event)
      {
      }
   }



   /*******************************************************************************
    ** A table with a required insert listener on the fault backend.
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      QBackendModuleDispatcher.registerBackendModule(new FaultModule());
      QContext.getQInstance().addBackend(new QBackendMetaData().withName("faultBackend").withBackendType(FaultModule.class));
      QContext.getQInstance().addTable(new QTableMetaData().withName("faultTable").withBackendName("faultBackend").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("name", QFieldType.STRING))
         .withSupplementalMetaData(new TransactionalRecordChangeMetaData(new QCodeReference(NoopRequiredListener.class), Set.of(RecordChangeType.INSERT))));
      LAST.set(null);
      failCommit = false;
   }



   /*******************************************************************************
    ** Negative control: with a commit that works the write commits.
    *******************************************************************************/
   @Test
   void commitThatWorksCommits() throws Exception
   {
      new InsertAction().execute(new InsertInput("faultTable").withRecords(List.of(new QRecord().withValue("name", "ok"))));
      assertThat(LAST.get().committed).isTrue();
      assertThat(LAST.get().rolledBack).isFalse();
      assertThat(LAST.get().closed).isTrue();
   }



   /*******************************************************************************
    ** An unchecked commit failure is a failure, rolled back, with the context restored.
    *******************************************************************************/
   @Test
   void uncheckedCommitFailureRollsBackAndRestoresContext() throws Exception
   {
      failCommit = true;
      Throwable thrown = null;
      try
      {
         new InsertAction().execute(new InsertInput("faultTable").withRecords(List.of(new QRecord().withValue("name", "boom"))));
      }
      catch(Throwable t)
      {
         thrown = t;
      }
      assertThat(thrown).as("the caller must see the commit failure, not a success").isNotNull();
      assertThat(LAST.get().committed).isFalse();
      assertThat(LAST.get().rolledBack).as("the failed commit is rolled back").isTrue();
      assertThat(LAST.get().isRollbackOnly()).isTrue();
      assertThat(LAST.get().closed).isTrue();
      assertThat(QContext.getQBackendTransaction()).isNull();

      failCommit = false;
      new InsertAction().execute(new InsertInput("faultTable").withRecords(List.of(new QRecord().withValue("name", "next"))));
      assertThat(LAST.get().committed).as("the next write starts a fresh scope and transaction").isTrue();
   }
}
