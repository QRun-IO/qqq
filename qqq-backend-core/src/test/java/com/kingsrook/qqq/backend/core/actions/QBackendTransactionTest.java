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


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


/*******************************************************************************
 ** Unit tests for QBackendTransaction
 **
 ** The base class has no-op commit/rollback/close — these tests verify the base
 ** class contract and that openFor() delegates to the correct backend module.
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

}
