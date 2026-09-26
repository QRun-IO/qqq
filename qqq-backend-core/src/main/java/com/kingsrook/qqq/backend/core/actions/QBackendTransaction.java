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

package com.kingsrook.qqq.backend.core.actions;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


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
 ** must call runAfterCommitCallbacks() once their commit succeeds, and
 ** discardAfterCommitCallbacks() if it fails.  Subclasses that override
 ** rollback() must call super.rollback(), which discards them.
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
      discardAfterCommitCallbacks();
   }



   /*******************************************************************************
    ** Register a callback to run after this transaction's next successful commit.
    ** Callbacks run in the order they were added, and each runs at most once.  If
    ** the transaction is rolled back instead, they are discarded without running.
    **
    ** A callback that throws (an Exception, or a LinkageError, such as a
    ** NoClassDefFoundError from an optional library that isn't on the classpath)
    ** is logged, and does not fail the commit - whose data is already committed -
    ** nor stop later callbacks from running.
    *******************************************************************************/
   public void addAfterCommitCallback(Runnable callback)
   {
      afterCommitCallbacks.add(Objects.requireNonNull(callback, "callback"));
   }



   /*******************************************************************************
    ** Run (and then forget) all after-commit callbacks, in the order they were
    ** added, catching and logging any Exception or LinkageError from each.  For
    ** subclasses to call once their commit has succeeded.
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
         catch(Exception | LinkageError e)
         {
            LOG.warn("Error running after-commit callback", e, logPair("callbackClass", callback.getClass().getName()));
         }
      }
   }



   /*******************************************************************************
    ** Forget all after-commit callbacks, without running them - their work will
    ** not be committed.  For rollback, and for subclasses to call when their
    ** commit fails (so a later commit on this transaction doesn't run them).
    *******************************************************************************/
   protected void discardAfterCommitCallbacks()
   {
      afterCommitCallbacks.clear();
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
