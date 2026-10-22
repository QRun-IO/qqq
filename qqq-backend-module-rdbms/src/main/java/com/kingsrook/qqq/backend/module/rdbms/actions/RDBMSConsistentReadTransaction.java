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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.module.rdbms.strategy.ConsistentReadStart;
import com.kingsrook.qqq.backend.module.rdbms.strategy.RDBMSActionStrategyInterface;


/*******************************************************************************
 ** RDBMS transaction opened by RDBMSBackendModule.openConsistentReadTransaction:
 ** a (read-only, by default) transaction on one connection, started such that
 ** every table read through it sees one snapshot.  Pass it as the transaction on
 ** query, count, etc. inputs.
 **
 ** The connection's isolation level and read-only flag are put back when this
 ** transaction is closed (as the connection may come from a pool).  Closing
 ** without commit rolls back - which, for a read-only transaction, is the same
 ** thing, and releases the snapshot.
 *******************************************************************************/
public class RDBMSConsistentReadTransaction extends RDBMSTransaction
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSConsistentReadTransaction.class);

   private int     originalIsolation;
   private boolean originalReadOnly;

   private ConsistentReadStart consistentReadStart;



   /*******************************************************************************
    ** Constructor - takes the snapshot (using the strategy for the database
    ** vendor), or closes the connection and throws if that fails.
    *******************************************************************************/
   public RDBMSConsistentReadTransaction(Connection connection, RDBMSActionStrategyInterface strategy, ConsistentReadInput input) throws SQLException
   {
      super(connection);

      try
      {
         originalIsolation = connection.getTransactionIsolation();
         originalReadOnly = connection.isReadOnly();

         consistentReadStart = strategy.beginConsistentRead(connection, input);
      }
      catch(SQLException | RuntimeException e)
      {
         close();
         throw (e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Optional<String> getExportedSnapshotId()
   {
      return (consistentReadStart.exportedSnapshotId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ConsistencyGuarantee getConsistencyGuarantee()
   {
      return (consistentReadStart.guarantee());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void close()
   {
      Connection connection = getConnection();
      try
      {
         if(!connection.isClosed())
         {
            ////////////////////////////////////////////////////////////////////////
            // end the transaction (releasing the snapshot) before the connection //
            // goes back to its pool, then put back what we changed on it         //
            ////////////////////////////////////////////////////////////////////////
            try
            {
               rollback();
            }
            catch(QException e)
            {
               LOG.warn("Error rolling back consistent-read transaction at close", e);
            }

            connection.setTransactionIsolation(originalIsolation);
            connection.setReadOnly(originalReadOnly);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error restoring connection settings after consistent-read transaction", e);
      }

      super.close();
   }

}
