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


package com.kingsrook.qqq.backend.core.jobs.store;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryLockMode;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;


/*******************************************************************************
 ** Claims with SELECT ... FOR UPDATE SKIP LOCKED (spec section 2.13, item 5):
 ** in one short transaction, lock candidate rows other claimers have not
 ** locked, then claim them (the fenced conditional update still bumps
 ** leaseVersion) and commit. Competing pods skip each other's rows instead of
 ** racing for the same ones. Backends without it use conditional updates only.
 *******************************************************************************/
public final class SkipLockedClaims
{
   /*******************************************************************************
    ** Claims the locked candidates inside the transaction.
    *******************************************************************************/
   @FunctionalInterface
   public interface Claimer<T>
   {
      /*******************************************************************************
       ** Claim (conditionally update) the locked rows, in the transaction.
       *******************************************************************************/
      T claim(List<QRecord> lockedCandidates, QBackendTransaction transaction) throws QException;
   }



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private SkipLockedClaims()
   {
   }



   /*******************************************************************************
    ** Whether the table's backend supports FOR UPDATE SKIP LOCKED.
    *******************************************************************************/
   public static boolean isSupported(String tableName)
   {
      try
      {
         QBackendMetaData backend = QContext.getQInstance().getBackendForTable(tableName);
         return (backend != null && new QBackendModuleDispatcher().getQBackendModule(backend).supportsQueryLockMode(backend, QueryLockMode.FOR_UPDATE_SKIP_LOCKED));
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** Lock the filter's rows (skipping locked ones) and claim them, in one
    ** transaction (as the system user).
    *******************************************************************************/
   public static <T> T claim(String tableName, QQueryFilter filter, Claimer<T> claimer) throws QException
   {
      return (SystemScope.call(() ->
      {
         QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(tableName));
         try
         {
            List<QRecord> locked = new QueryAction().execute(new QueryInput(tableName).withTransaction(transaction)
               .withLockMode(QueryLockMode.FOR_UPDATE_SKIP_LOCKED).withFilter(filter)).getRecords();
            T result = claimer.claim(locked, transaction);
            transaction.commit();
            return (result);
         }
         catch(Exception e)
         {
            try
            {
               transaction.rollback();
            }
            catch(Exception rollbackException)
            {
               e.addSuppressed(rollbackException);
            }
            throw (e instanceof QException qe ? qe : new QException("Could not claim from " + tableName, e));
         }
         finally
         {
            transaction.close();
         }
      }));
   }
}
