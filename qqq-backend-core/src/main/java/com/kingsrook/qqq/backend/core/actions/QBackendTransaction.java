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

package com.kingsrook.qqq.backend.core.actions;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;


/*******************************************************************************
 ** Container wherein backend modules can track data and/or objects that are
 ** part of a transaction.
 **
 ** Most obvious use-case would be a JDBC Connection.  See subclass in rdbms module.
 ** Ditto MongoDB.
 **
 ** Also holds after-commit callbacks: work (e.g., publishing events about
 ** changed records) that must only happen once the transaction's data is
 ** committed, and never if it is rolled back.  Subclasses that override commit()
 ** must call runAfterCommitCallbacks() once their commit succeeds, and subclasses
 ** that override rollback() must call super.rollback(), which discards them.
 **
 ** Note:  One would imagine that this class shouldn't ever implement Serializable...
 *******************************************************************************/
public class QBackendTransaction implements AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(QBackendTransaction.class);

   private List<Runnable> afterCommitCallbacks = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QBackendTransaction openFor(AbstractTableActionInput input) throws QException
   {
      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(input.getBackend());
      QBackendTransaction      transaction              = qModule.openTransaction(input);
      return (transaction);
   }



   /*******************************************************************************
    ** Commit the transaction.
    *******************************************************************************/
   public void commit() throws QException
   {
      //////////////////////////////////////////////////////////////////////
      // nothing to commit in base class - so just run any callbacks that //
      // were waiting on the commit                                       //
      //////////////////////////////////////////////////////////////////////
      runAfterCommitCallbacks();
   }



   /*******************************************************************************
    ** Rollback the transaction.
    *******************************************************************************/
   public void rollback() throws QException
   {
      //////////////////////////////////////////////////////////////////////
      // nothing to roll back in base class - but any after-commit        //
      // callbacks were for work that won't be committed, so discard them //
      //////////////////////////////////////////////////////////////////////
      afterCommitCallbacks.clear();
   }



   /*******************************************************************************
    ** Register a callback to run after this transaction's next successful commit.
    ** Callbacks run in the order they were added, and each runs at most once.  If
    ** the transaction is rolled back instead, they are discarded without running.
    **
    ** A callback that throws is logged, and does not fail the commit, nor stop
    ** later callbacks from running.
    *******************************************************************************/
   public void addAfterCommitCallback(Runnable callback)
   {
      afterCommitCallbacks.add(Objects.requireNonNull(callback, "callback"));
   }



   /*******************************************************************************
    ** Run (and then forget) all after-commit callbacks, in the order they were
    ** added, catching and logging any exception from each.  For subclasses to
    ** call once their commit has succeeded.
    *******************************************************************************/
   protected void runAfterCommitCallbacks()
   {
      if(afterCommitCallbacks.isEmpty())
      {
         return;
      }

      /////////////////////////////////////////////////////////////////////////////
      // swap the list out before running, so each callback runs at most once.   //
      // a callback added while these run (e.g., by more writes on this          //
      // transaction, which a commit re-opens) waits for the next commit, rather //
      // than running before its work is committed.                              //
      /////////////////////////////////////////////////////////////////////////////
      List<Runnable> callbacksToRun = afterCommitCallbacks;
      afterCommitCallbacks = new ArrayList<>();

      for(Runnable callback : callbacksToRun)
      {
         try
         {
            callback.run();
         }
         catch(Exception e)
         {
            LOG.warn("Error running after-commit callback", e);
         }
      }
   }



   /*******************************************************************************
    ** Close any resources associated with the transaction.  In theory, should only
    ** be called after a commit or rollback was done.
    *******************************************************************************/
   public void close()
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }
}
