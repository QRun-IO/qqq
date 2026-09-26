/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit tests for QBackendTransaction
 **
 ** The base class has no backend work in commit/rollback/close, beyond running
 ** or discarding after-commit callbacks - these tests verify the base class
 ** contract and that openFor() delegates to the correct backend module.
 *******************************************************************************/
class QBackendTransactionTest extends BaseTest
{

   /*******************************************************************************
    ** Base class commit() should be a no-op (no exception).
    *******************************************************************************/
   @Test
   void testCommit_baseClass_noOp() throws QException
   {
      QBackendTransaction tx = new QBackendTransaction();
      assertThatCode(tx::commit).doesNotThrowAnyException();
   }



   /*******************************************************************************
    ** Base class rollback() should be a no-op (no exception).
    *******************************************************************************/
   @Test
   void testRollback_baseClass_noOp() throws QException
   {
      QBackendTransaction tx = new QBackendTransaction();
      assertThatCode(tx::rollback).doesNotThrowAnyException();
   }



   /*******************************************************************************
    ** Base class close() should be a no-op (no exception) and usable in
    ** try-with-resources.
    *******************************************************************************/
   @Test
   void testClose_baseClass_noOp()
   {
      assertThatCode(() ->
      {
         try(QBackendTransaction tx = new QBackendTransaction())
         {
            // intentionally empty — verifying AutoCloseable contract
         }
      }).doesNotThrowAnyException();
   }



   /*******************************************************************************
    ** openFor() against a memory backend should return a non-null transaction.
    *******************************************************************************/
   @Test
   void testOpenFor_memoryBackend_returnsTransaction() throws QException
   {
      InsertInput input = new InsertInput();
      input.setTableName(TestUtils.TABLE_NAME_PERSON);

      QBackendTransaction tx = QBackendTransaction.openFor(input);

      assertThat(tx).isNotNull();
   }



   /*******************************************************************************
    ** Verify the full commit-after-open lifecycle does not throw for memory backend.
    *******************************************************************************/
   @Test
   void testOpenFor_memoryBackend_commitLifecycle() throws QException
   {
      InsertInput input = new InsertInput();
      input.setTableName(TestUtils.TABLE_NAME_PERSON);

      try(QBackendTransaction tx = QBackendTransaction.openFor(input))
      {
         tx.commit();
      }
   }



   /*******************************************************************************
    ** Verify the full rollback-after-open lifecycle does not throw for memory backend.
    *******************************************************************************/
   @Test
   void testOpenFor_memoryBackend_rollbackLifecycle() throws QException
   {
      InsertInput input = new InsertInput();
      input.setTableName(TestUtils.TABLE_NAME_PERSON);

      try(QBackendTransaction tx = QBackendTransaction.openFor(input))
      {
         tx.rollback();
      }
   }



   /*******************************************************************************
    ** Callbacks should not run until commit, and then run in the order added.
    *******************************************************************************/
   @Test
   void callbacksRunAfterCommitInOrder() throws QException
   {
      List<String>        ran = new ArrayList<>();
      QBackendTransaction tx  = new QBackendTransaction();
      tx.addAfterCommitCallback(() -> ran.add("a"));
      tx.addAfterCommitCallback(() -> ran.add("b"));
      tx.addAfterCommitCallback(() -> ran.add("c"));
      assertThat(ran).isEmpty();

      tx.commit();
      assertThat(ran).containsExactly("a", "b", "c");
   }



   /*******************************************************************************
    ** Rollback should discard callbacks without running them - so a later commit
    ** must not run them either.
    *******************************************************************************/
   @Test
   void callbacksDiscardedOnRollback() throws QException
   {
      List<String>        ran = new ArrayList<>();
      QBackendTransaction tx  = new QBackendTransaction();
      tx.addAfterCommitCallback(() -> ran.add("a"));

      tx.rollback();
      assertThat(ran).isEmpty();

      tx.commit();
      assertThat(ran).isEmpty();
   }



   /*******************************************************************************
    ** A callback that throws must not fail the commit, nor stop later callbacks.
    *******************************************************************************/
   @Test
   void callbackExceptionDoesNotFailCommitOrLaterCallbacks() throws QException
   {
      List<String>        ran = new ArrayList<>();
      QBackendTransaction tx  = new QBackendTransaction();
      tx.addAfterCommitCallback(() -> ran.add("a"));
      tx.addAfterCommitCallback(() ->
      {
         throw (new IllegalStateException("callback failure"));
      });
      tx.addAfterCommitCallback(() -> ran.add("c"));

      assertThatCode(tx::commit).doesNotThrowAnyException();
      assertThat(ran).containsExactly("a", "c");
   }



   /*******************************************************************************
    ** Committing a second time must not re-run callbacks from the first commit.
    *******************************************************************************/
   @Test
   void callbacksRunOnlyOnce() throws QException
   {
      AtomicInteger       runCount = new AtomicInteger(0);
      QBackendTransaction tx       = new QBackendTransaction();
      tx.addAfterCommitCallback(runCount::incrementAndGet);

      tx.commit();
      tx.commit();
      assertThat(runCount.get()).isEqualTo(1);
   }



   /*******************************************************************************
    ** A callback added while callbacks are running belongs to the next commit, as
    ** the transaction is re-opened once a commit completes.
    *******************************************************************************/
   @Test
   void callbackAddedDuringCallbacksRunsOnNextCommit() throws QException
   {
      List<String>        ran = new ArrayList<>();
      QBackendTransaction tx  = new QBackendTransaction();
      tx.addAfterCommitCallback(() ->
      {
         ran.add("first");
         tx.addAfterCommitCallback(() -> ran.add("second"));
      });

      tx.commit();
      assertThat(ran).containsExactly("first");

      tx.commit();
      assertThat(ran).containsExactly("first", "second");
   }



   /*******************************************************************************
    ** A callback that throws a LinkageError (e.g., a NoClassDefFoundError from an
    ** optional client jar that isn't on the classpath) must not fail the commit -
    ** whose data is already committed - nor stop later callbacks.
    *******************************************************************************/
   @Test
   void callbackLinkageErrorDoesNotFailCommitOrLaterCallbacks() throws QException
   {
      List<String>        ran = new ArrayList<>();
      QBackendTransaction tx  = new QBackendTransaction();
      tx.addAfterCommitCallback(() -> ran.add("a"));
      tx.addAfterCommitCallback(() ->
      {
         throw (new NoClassDefFoundError("com/example/MissingBrokerClient"));
      });
      tx.addAfterCommitCallback(() -> ran.add("c"));

      assertThatCode(tx::commit).doesNotThrowAnyException();
      assertThat(ran).containsExactly("a", "c");
   }



   /*******************************************************************************
    ** A null callback is a caller bug - reject it when added, not at commit time.
    *******************************************************************************/
   @Test
   void nullCallbackIsRejected()
   {
      QBackendTransaction tx = new QBackendTransaction();
      assertThatThrownBy(() -> tx.addAfterCommitCallback(null))
         .isInstanceOf(NullPointerException.class);
   }

}
