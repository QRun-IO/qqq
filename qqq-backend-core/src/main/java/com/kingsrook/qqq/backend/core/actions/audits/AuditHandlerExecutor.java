/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.audits;


import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.ProcessedAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.ReadAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditHandlerFailurePolicy;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditHandlerType;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditHandlerMetaData;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeVoidVoidMethod;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Executor for audit handlers.
 ** Handles sync and async execution of DML, Processed, and Read audit handlers.
 *******************************************************************************/
public class AuditHandlerExecutor
{
   private static final QLogger LOG = QLogger.getLogger(AuditHandlerExecutor.class);

   private static final Integer         CORE_THREADS    = 4;
   private static final Integer         MAX_THREADS     = 100;
   private static final ExecutorService executorService = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new PrefixedDefaultThreadFactory(AuditHandlerExecutor.class));



   /*******************************************************************************
    ** Get the shared executor service for async audit operations. Used by
    ** ReadAuditAction to submit fire-and-forget read audit work.
    *******************************************************************************/
   static ExecutorService getExecutorService()
   {
      return (executorService);
   }



   /*******************************************************************************
    ** Execute all DML audit handlers for the given table.
    **
    ** @param tableName the table name to get handlers for
    ** @param input the DML audit handler input
    ** @throws QException if a sync handler with FAIL_OPERATION policy throws
    *******************************************************************************/
   public void executeDMLHandlers(String tableName, DMLAuditHandlerInput input) throws QException
   {
      List<QAuditHandlerMetaData> handlers = QContext.getQInstance().getAuditHandlersForTable(tableName, AuditHandlerType.DML);

      for(QAuditHandlerMetaData handlerMetaData : handlers)
      {
         executeDMLHandler(handlerMetaData, input);
      }
   }



   /*******************************************************************************
    ** Execute a single DML audit handler.
    *******************************************************************************/
   private void executeDMLHandler(QAuditHandlerMetaData handlerMetaData, DMLAuditHandlerInput input) throws QException
   {
      DMLAuditHandlerInterface handler = QCodeLoader.getAdHoc(DMLAuditHandlerInterface.class, handlerMetaData.getHandlerCode());
      if(handler == null)
      {
         LOG.warn("Could not load DML audit handler", logPair("handlerName", handlerMetaData.getName()));
         return;
      }

      Boolean isAsync = handlerMetaData.getIsAsync();
      if(isAsync != null && isAsync)
      {
         executeAsync(handlerMetaData, () -> handler.handleDMLAudit(input));
      }
      else
      {
         executeSync(handlerMetaData, () -> handler.handleDMLAudit(input));
      }
   }



   /*******************************************************************************
    ** Execute all processed audit handlers for the given table.
    **
    ** @param tableName the table name to get handlers for (from the first audit single input)
    ** @param input the processed audit handler input
    ** @throws QException if a sync handler with FAIL_OPERATION policy throws
    *******************************************************************************/
   public void executeProcessedHandlers(String tableName, ProcessedAuditHandlerInput input) throws QException
   {
      List<QAuditHandlerMetaData> handlers = QContext.getQInstance().getAuditHandlersForTable(tableName, AuditHandlerType.PROCESSED);

      for(QAuditHandlerMetaData handlerMetaData : handlers)
      {
         executeProcessedHandler(handlerMetaData, input);
      }
   }



   /*******************************************************************************
    ** Execute a single processed audit handler.
    *******************************************************************************/
   private void executeProcessedHandler(QAuditHandlerMetaData handlerMetaData, ProcessedAuditHandlerInput input) throws QException
   {
      ProcessedAuditHandlerInterface handler = QCodeLoader.getAdHoc(ProcessedAuditHandlerInterface.class, handlerMetaData.getHandlerCode());
      if(handler == null)
      {
         LOG.warn("Could not load processed audit handler", logPair("handlerName", handlerMetaData.getName()));
         return;
      }

      Boolean isAsync = handlerMetaData.getIsAsync();
      if(isAsync != null && isAsync)
      {
         executeAsync(handlerMetaData, () -> handler.handleAudit(input));
      }
      else
      {
         executeSync(handlerMetaData, () -> handler.handleAudit(input));
      }
   }



   /*******************************************************************************
    ** Execute all read audit handlers for the given table.
    **
    ** @param tableName the table name to get handlers for
    ** @param input the read audit handler input
    ** @throws QException if a sync handler with FAIL_OPERATION policy throws
    *******************************************************************************/
   public void executeReadHandlers(String tableName, ReadAuditHandlerInput input) throws QException
   {
      List<QAuditHandlerMetaData> handlers = QContext.getQInstance().getAuditHandlersForTable(tableName, AuditHandlerType.READ);

      for(QAuditHandlerMetaData handlerMetaData : handlers)
      {
         executeReadHandler(handlerMetaData, input);
      }
   }



   /*******************************************************************************
    ** Execute a single read audit handler.
    *******************************************************************************/
   private void executeReadHandler(QAuditHandlerMetaData handlerMetaData, ReadAuditHandlerInput input) throws QException
   {
      ReadAuditHandlerInterface handler = QCodeLoader.getAdHoc(ReadAuditHandlerInterface.class, handlerMetaData.getHandlerCode());
      if(handler == null)
      {
         LOG.warn("Could not load read audit handler", logPair("handlerName", handlerMetaData.getName()));
         return;
      }

      Boolean isAsync = handlerMetaData.getIsAsync();
      if(isAsync != null && isAsync)
      {
         executeAsync(handlerMetaData, () -> handler.handleReadAudit(input));
      }
      else
      {
         executeSync(handlerMetaData, () -> handler.handleReadAudit(input));
      }
   }



   /*******************************************************************************
    ** Execute a handler synchronously, respecting failure policy.
    *******************************************************************************/
   private void executeSync(QAuditHandlerMetaData handlerMetaData, UnsafeVoidVoidMethod<Exception> runnable) throws QException
   {
      try
      {
         runnable.run();
      }
      catch(Exception e)
      {
         handleFailure(handlerMetaData, e);
      }
   }



   /*******************************************************************************
    ** Execute a handler asynchronously.
    ** Note: FAIL_OPERATION policy cannot work with async handlers since the
    ** original operation has already completed.
    *******************************************************************************/
   private void executeAsync(QAuditHandlerMetaData handlerMetaData, UnsafeVoidVoidMethod<Exception> runnable)
   {
      CapturedContext capturedContext = QContext.capture();

      executorService.submit(() ->
      {
         try
         {
            QContext.init(capturedContext);
            runnable.run();
         }
         catch(Exception e)
         {
            LOG.warn("Async audit handler failed", e,
               logPair("handlerName", handlerMetaData.getName()));
         }
         finally
         {
            QContext.clear();
         }
      });
   }



   /*******************************************************************************
    ** Handle a failure according to the handler's failure policy.
    *******************************************************************************/
   private void handleFailure(QAuditHandlerMetaData handlerMetaData, Exception e) throws QException
   {
      AuditHandlerFailurePolicy failurePolicy = handlerMetaData.getFailurePolicy();
      if(failurePolicy == null)
      {
         failurePolicy = AuditHandlerFailurePolicy.LOG_AND_CONTINUE;
      }

      switch(failurePolicy)
      {
         case LOG_AND_CONTINUE ->
         {
            LOG.warn("Audit handler failed, continuing", e,
               logPair("handlerName", handlerMetaData.getName()));
         }
         case FAIL_OPERATION ->
         {
            LOG.error("Audit handler failed, failing operation", e,
               logPair("handlerName", handlerMetaData.getName()));
            throw new QException("Audit handler [" + handlerMetaData.getName() + "] failed", e);
         }
         default ->
         {
            LOG.warn("Unknown failure policy, defaulting to LOG_AND_CONTINUE", e,
               logPair("handlerName", handlerMetaData.getName()),
               logPair("failurePolicy", failurePolicy));
         }
      }
   }

}
