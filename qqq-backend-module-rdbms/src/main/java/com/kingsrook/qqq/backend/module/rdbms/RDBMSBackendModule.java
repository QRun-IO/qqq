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

package com.kingsrook.qqq.backend.module.rdbms;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.ConditionalUpdateInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.InsertInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.modules.backend.SequenceResyncResult;
import com.kingsrook.qqq.backend.module.rdbms.actions.AbstractRDBMSAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSAggregateAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSConditionalUpdateAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSConsistentReadTransaction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSCountAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSDeleteAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSInsertAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSQueryAction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSUpdateAction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** QQQ Backend module for working with Relational Databases (RDBMS's).
 *******************************************************************************/
public class RDBMSBackendModule implements QBackendModuleInterface
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSBackendModule.class);

   public static final String NAME = "rdbms";

   static
   {
      QBackendModuleDispatcher.registerBackendModule(new RDBMSBackendModule());
   }

   /*******************************************************************************
    ** RDBMS actions translate same-backend sub-queries into SQL.
    *******************************************************************************/
   @Override
   public boolean supportsSubQueries(QBackendMetaData backend)
   {
      return (true);
   }



   /*******************************************************************************
    ** Method where a backend module must be able to provide its type (name).
    *******************************************************************************/
   public String getBackendType()
   {
      return NAME;
   }



   /*******************************************************************************
    ** Method to identify the class used for backend meta data for this module.
    *******************************************************************************/
   @Override
   public Class<? extends QBackendMetaData> getBackendMetaDataClass()
   {
      return (RDBMSBackendMetaData.class);
   }



   /*******************************************************************************
    ** Method to identify the class used for table-backend details for this module.
    *******************************************************************************/
   @Override
   public Class<? extends QTableBackendDetails> getTableBackendDetailsClass()
   {
      return (RDBMSTableBackendDetails.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public CountInterface getCountInterface()
   {
      return (new RDBMSCountAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueryInterface getQueryInterface()
   {
      return (new RDBMSQueryAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public InsertInterface getInsertInterface()
   {
      return (new RDBMSInsertAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public UpdateInterface getUpdateInterface()
   {
      return (new RDBMSUpdateAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public DeleteInterface getDeleteInterface()
   {
      return (new RDBMSDeleteAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public AggregateInterface getAggregateInterface()
   {
      return (new RDBMSAggregateAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QBackendTransaction openTransaction(AbstractTableActionInput input) throws QException
   {
      try
      {
         LOG.debug("Opening transaction");
         Connection connection = AbstractRDBMSAction.getConnection(input);
         return (new RDBMSTransaction(connection));
      }
      catch(Exception e)
      {
         throw new QException("Error opening transaction: " + e.getMessage(), e);
      }
   }

   /*******************************************************************************
    ** Open a transaction whose reads all see one snapshot of the database - in the
    ** vendor's way (see RDBMSActionStrategyInterface.beginConsistentRead).  With
    ** useReadReplica, and a read-only backend configured, it's on that backend.
    *******************************************************************************/
   @Override
   public Optional<QBackendTransaction> openConsistentReadTransaction(ConsistentReadInput input) throws QException
   {
      try
      {
         RDBMSBackendMetaData backend = (RDBMSBackendMetaData) QContext.getQInstance().getBackend(input.getBackendName());
         if(backend == null)
         {
            throw (new QException("Unrecognized backend name: " + input.getBackendName()));
         }

         if(input.getUseReadReplica() && backend.getReadOnlyBackendMetaData() != null)
         {
            backend = backend.getReadOnlyBackendMetaData();
         }

         LOG.debug("Opening consistent-read transaction", logPair("backendName", input.getBackendName()));
         Connection connection = ConnectionManager.getConnection(backend);
         return (Optional.of(new RDBMSConsistentReadTransaction(connection, backend.getActionStrategy(), input)));
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw new QException("Error opening consistent-read transaction: " + e.getMessage(), e);
      }
   }


   /*******************************************************************************
    ** Advance each table's key sequence past its largest key, per vendor:
    ** - postgres: setval on the column's serial/identity sequence.
    ** - h2: ALTER TABLE ... RESTART WITH max+1 on an identity column.
    ** - mysql, mariadb, sqlite: the engine does this itself on explicit inserts.
    ** - anything else (or no integer key, or no sequence): not applicable.
    *******************************************************************************/
   @Override
   public List<SequenceResyncResult> resyncKeySequences(QBackendMetaData backend, Collection<QTableMetaData> tables) throws QException
   {
      List<SequenceResyncResult> rs = new ArrayList<>();
      if(!(backend instanceof RDBMSBackendMetaData rdbmsBackend))
      {
         return (rs);
      }

      String vendor = rdbmsBackend.getVendor() == null ? "" : rdbmsBackend.getVendor().toLowerCase();
      if(Set.of("mysql", "aurora-mysql", "mariadb", "sqlite").contains(vendor))
      {
         ///////////////////////////////////////////////////////////////////////
         // these keep their counters in step themselves - no connection needed //
         ///////////////////////////////////////////////////////////////////////
         tables.forEach(table -> rs.add(new SequenceResyncResult(table.getName(), SequenceResyncResult.Status.NOT_NEEDED, null)));
         return (rs);
      }

      try(Connection connection = ConnectionManager.getConnection(rdbmsBackend))
      {
         String quote = rdbmsBackend.getActionStrategy().getIdentifierQuoteString();
         for(QTableMetaData table : tables)
         {
            QFieldMetaData pk = table.getFields() == null ? null : table.getFields().get(table.getPrimaryKeyField());
            if(pk == null || (pk.getType() != QFieldType.INTEGER && pk.getType() != QFieldType.LONG))
            {
               rs.add(new SequenceResyncResult(table.getName(), SequenceResyncResult.Status.NOT_APPLICABLE, null));
               continue;
            }

            String tableName = AbstractRDBMSAction.getTableName(table);
            String column    = AbstractRDBMSAction.getColumnName(pk);

            switch(vendor)
            {
               case "postgres" -> rs.add(resyncPostgres(connection, table.getName(), tableName, column));
               case "h2" -> rs.add(resyncH2(connection, quote, table.getName(), tableName, column));
               default -> rs.add(new SequenceResyncResult(table.getName(), SequenceResyncResult.Status.NOT_APPLICABLE, null));
            }
         }
      }
      catch(SQLException e)
      {
         throw new QException("Error resyncing key sequences: " + e.getMessage(), e);
      }

      return (rs);
   }



   /***************************************************************************
    ** pg_get_serial_sequence covers both serial and identity columns, and gives
    ** null (so setval gives null) when the column has no sequence.
    ***************************************************************************/
   private SequenceResyncResult resyncPostgres(Connection connection, String qqqTableName, String tableName, String column) throws SQLException
   {
      String quotedTable  = quoteQualified(tableName, "\"");
      String quotedColumn = "\"" + column.replace("\"", "\"\"") + "\"";
      String sql = "SELECT setval(pg_get_serial_sequence(?, ?), COALESCE(MAX(" + quotedColumn + "), 1), MAX(" + quotedColumn + ") IS NOT NULL) AS seq_value,"
         + " MAX(" + quotedColumn + ") IS NOT NULL AS has_rows FROM " + quotedTable;

      List<Map<String, Object>> rows     = QueryManager.executeStatementForRows(connection, sql, quotedTable, column);
      Object                    seqValue = rows.get(0).get("seq_value");
      if(seqValue == null)
      {
         return (new SequenceResyncResult(qqqTableName, SequenceResyncResult.Status.NOT_APPLICABLE, null));
      }

      long next = Boolean.TRUE.equals(rows.get(0).get("has_rows")) ? ((Number) seqValue).longValue() + 1 : 1L;
      return (new SequenceResyncResult(qqqTableName, SequenceResyncResult.Status.RESYNCED, next));
   }



   /***************************************************************************
    ** H2: only identity columns can be restarted.
    ***************************************************************************/
   private SequenceResyncResult resyncH2(Connection connection, String quote, String qqqTableName, String tableName, String column) throws SQLException
   {
      String isIdentity = QueryManager.executeStatementForSingleValue(connection, String.class,
         "SELECT IS_IDENTITY FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)", tableName, column);
      if(!"YES".equals(isIdentity))
      {
         return (new SequenceResyncResult(qqqTableName, SequenceResyncResult.Status.NOT_APPLICABLE, null));
      }

      String quotedColumn = quote + column.replace(quote, quote + quote) + quote;
      String quotedTable  = quoteQualified(tableName, quote);
      Number max          = QueryManager.executeStatementForSingleValue(connection, Number.class, "SELECT MAX(" + quotedColumn + ") FROM " + quotedTable);
      long   next         = max == null ? 1L : max.longValue() + 1;
      QueryManager.executeUpdate(connection, "ALTER TABLE " + quotedTable + " ALTER COLUMN " + quotedColumn + " RESTART WITH " + next);
      return (new SequenceResyncResult(qqqTableName, SequenceResyncResult.Status.RESYNCED, next));
   }



   /***************************************************************************
    ** Quote each dot-separated part of a (possibly schema-qualified) table name.
    ***************************************************************************/
   private static String quoteQualified(String name, String quote)
   {
      List<String> parts = new ArrayList<>();
      for(String part : name.split("\\."))
      {
         parts.add(quote + part.replace(quote, quote + quote) + quote);
      }
      return (String.join(".", parts));
   }




   /*******************************************************************************
    ** Atomic conditional updates (CONDITIONAL_UPDATE), plus the database clock.
    *******************************************************************************/
   @Override
   public ConditionalUpdateInterface getConditionalUpdateInterface()
   {
      return (new RDBMSConditionalUpdateAction());
   }
}
