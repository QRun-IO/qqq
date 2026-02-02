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


import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.ReadAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.ReadAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.audits.ReadAuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.QQueryFilterFormatter;
import static com.kingsrook.qqq.backend.core.actions.audits.AuditAction.getRecordSecurityKeyValues;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Audit action for read (view) operations. Fires asynchronously so that read
 ** performance is not impacted by audit I/O.
 *******************************************************************************/
public class ReadAuditAction
{
   private static final QLogger LOG = QLogger.getLogger(ReadAuditAction.class);

   private static final String GET_AUDIT_MESSAGE   = "Record was Viewed";
   private static final String QUERY_AUDIT_MESSAGE = "Record was included in Query result";

   /////////////////////////////////////////////////////////////////////////////////////
   // thread-local flag to suppress duplicate read audits when GetAction delegates to //
   // QueryAction via DefaultGetInterface. GetAction sets this before the inner query //
   // and clears it after, so QueryAction's read audit call is suppressed.            //
   /////////////////////////////////////////////////////////////////////////////////////
   private static final ThreadLocal<Boolean> suppressReadAuditThreadLocal = new ThreadLocal<>();



   /*******************************************************************************
    ** Suppress read audit on the current thread. Used by GetAction to prevent
    ** its inner DefaultGetInterface query from firing a duplicate read audit.
    *******************************************************************************/
   public static void suppressOnCurrentThread()
   {
      suppressReadAuditThreadLocal.set(Boolean.TRUE);
   }



   /*******************************************************************************
    ** Clear the suppression flag on the current thread.
    *******************************************************************************/
   public static void unsuppressOnCurrentThread()
   {
      suppressReadAuditThreadLocal.remove();
   }



   /*******************************************************************************
    ** Fire-and-forget async read audit. Checks table's readAuditLevel and
    ** InputSource before submitting to the thread pool. Extracts only primary
    ** keys and security key values from records on the calling thread to minimize
    ** memory held by the async closure.
    **
    ** @param table the table that was read
    ** @param actionInput the original get/query input (for InputSource check)
    ** @param records the records that were returned by the read
    ** @param readType GET or QUERY
    ** @param queryFilter the query filter used (null for GET operations)
    *******************************************************************************/
   public static void executeAsync(QTableMetaData table, AbstractTableActionInput actionInput, List<QRecord> records, ReadAuditInput.ReadType readType, QQueryFilter queryFilter)
   {
      try
      {
         if(CollectionUtils.nullSafeIsEmpty(records))
         {
            return;
         }

         ////////////////////////////////////
         // check thread-local suppression //
         ////////////////////////////////////
         if(Boolean.TRUE.equals(suppressReadAuditThreadLocal.get()))
         {
            return;
         }

         ////////////////////////////////////////////////////////
         // only audit user-facing reads, not system internals //
         ////////////////////////////////////////////////////////
         if(!isUserSourced(actionInput))
         {
            return;
         }

         ReadAuditLevel readAuditLevel = getReadAuditLevel(table);
         if(readAuditLevel == ReadAuditLevel.NONE)
         {
            return;
         }

         if(readAuditLevel == ReadAuditLevel.GET && readType == ReadAuditInput.ReadType.QUERY)
         {
            return;
         }

         /////////////////////////////////////////////////////////////////////////////////
         // extract only primary keys and security key values from records on the        //
         // calling thread, so the async closure does not hold full QRecord references. //
         /////////////////////////////////////////////////////////////////////////////////
         String primaryKeyField = table.getPrimaryKeyField();
         List<Serializable> primaryKeys = new ArrayList<>(records.size());
         List<Map<String, Serializable>> securityKeyValuesList = new ArrayList<>(records.size());

         for(QRecord record : records)
         {
            primaryKeys.add(record.getValue(primaryKeyField));
            securityKeyValuesList.add(getRecordSecurityKeyValues(table, record, Optional.empty()));
         }

         CapturedContext capturedContext = QContext.capture();

         AuditHandlerExecutor.getExecutorService().submit(() ->
         {
            try
            {
               QContext.init(capturedContext);
               executeInBackground(table, primaryKeys, securityKeyValuesList, readType, queryFilter);
            }
            catch(Exception e)
            {
               LOG.warn("Error performing async read audit", e, logPair("table", table.getName()), logPair("readType", readType));
            }
            finally
            {
               QContext.clear();
            }
         });
      }
      catch(Exception e)
      {
         LOG.warn("Error submitting read audit", e, logPair("table", table.getName()));
      }
   }



   /*******************************************************************************
    ** Background execution - builds audit records and inserts them in bulk.
    **
    ** @param table the table metadata
    ** @param primaryKeys the primary key values extracted from the read records
    ** @param securityKeyValuesList the security key values for each record
    ** @param readType GET or QUERY
    ** @param queryFilter the query filter used (null for GET operations)
    *******************************************************************************/
   static void executeInBackground(QTableMetaData table, List<Serializable> primaryKeys, List<Map<String, Serializable>> securityKeyValuesList, ReadAuditInput.ReadType readType, QQueryFilter queryFilter)
   {
      try
      {
         String message = readType == ReadAuditInput.ReadType.QUERY ? QUERY_AUDIT_MESSAGE : GET_AUDIT_MESSAGE;

         /////////////////////////////////////////////////////////////////////////////////
         // for QUERY operations with a filter, format the filter once and include it   //
         // as an auditDetail record on each audit entry. Fresh QRecord instances are   //
         // required per entry because AuditAction.execute() mutates detail records.    //
         /////////////////////////////////////////////////////////////////////////////////
         String filterSummary = null;
         if(readType == ReadAuditInput.ReadType.QUERY && queryFilter != null)
         {
            filterSummary = QQueryFilterFormatter.formatQueryFilter(table.getName(), queryFilter);
         }

         AuditInput auditInput = new AuditInput();

         for(int i = 0; i < primaryKeys.size(); i++)
         {
            if(filterSummary != null)
            {
               List<QRecord> details = List.of(new QRecord().withValue("message", "Query Filter: " + filterSummary));
               AuditAction.appendToInput(auditInput, table.getName(), primaryKeys.get(i), securityKeyValuesList.get(i), message, details);
            }
            else
            {
               AuditAction.appendToInput(auditInput, table.getName(), primaryKeys.get(i), securityKeyValuesList.get(i), message);
            }
         }

         new AuditAction().execute(auditInput);

         ///////////////////////////
         // execute read handlers //
         ///////////////////////////
         executeReadHandlers(table, primaryKeys, readType, queryFilter);

         LOG.trace("Read audit completed", logPair("table", table.getName()), logPair("readType", readType), logPair("recordCount", primaryKeys.size()));
      }
      catch(Exception e)
      {
         LOG.warn("Error performing read audit", e, logPair("table", table.getName()));
      }
   }



   /*******************************************************************************
    ** Execute read audit handlers for the table.
    *******************************************************************************/
   private static void executeReadHandlers(QTableMetaData table, List<Serializable> primaryKeys, ReadAuditInput.ReadType readType, QQueryFilter queryFilter)
   {
      try
      {
         //////////////////////////////////////////////////////////////////////////////////////
         // build lightweight QRecords with only the primary key set for handler consumption //
         //////////////////////////////////////////////////////////////////////////////////////
         String primaryKeyField = table.getPrimaryKeyField();
         List<QRecord> records = new ArrayList<>(primaryKeys.size());
         for(Serializable primaryKey : primaryKeys)
         {
            records.add(new QRecord().withValue(primaryKeyField, primaryKey));
         }

         ReadAuditHandlerInput handlerInput = new ReadAuditHandlerInput()
            .withTableName(table.getName())
            .withReadType(readType)
            .withRecords(records)
            .withResultCount(primaryKeys.size())
            .withQueryFilter(queryFilter)
            .withTimestamp(Instant.now())
            .withSession(QContext.getQSession());

         new AuditHandlerExecutor().executeReadHandlers(table.getName(), handlerInput);
      }
      catch(Exception e)
      {
         LOG.warn("Error executing read audit handlers", e, logPair("table", table.getName()));
      }
   }



   /*******************************************************************************
    ** Get the read audit level for a table, defaulting to NONE.
    *******************************************************************************/
   static ReadAuditLevel getReadAuditLevel(QTableMetaData table)
   {
      if(table.getAuditRules() == null)
      {
         return (ReadAuditLevel.NONE);
      }

      ReadAuditLevel readAuditLevel = table.getAuditRules().getReadAuditLevel();
      return (readAuditLevel == null ? ReadAuditLevel.NONE : readAuditLevel);
   }



   /*******************************************************************************
    ** Check if the action's InputSource is USER (not system/internal).
    *******************************************************************************/
   private static boolean isUserSourced(AbstractTableActionInput actionInput)
   {
      if(actionInput == null || actionInput.getInputSource() == null)
      {
         return (false);
      }

      return (actionInput.getInputSource().equals(QInputSource.USER));
   }

}
