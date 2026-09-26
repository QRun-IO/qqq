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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** RDBMS implementation of backend transaction.
 **
 ** Stores a jdbc connection, which is set to autoCommit(false).
 *******************************************************************************/
public class RDBMSTransaction extends QBackendTransaction
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSTransaction.class);

   private Connection connection;

   private Instant openedAt                  = Instant.now();
   private Integer logSlowTransactionSeconds = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RDBMSTransaction(Connection connection) throws SQLException
   {
      connection.setAutoCommit(false);

      String propertyName = "qqq.rdbms.logSlowTransactionSeconds";
      try
      {
         logSlowTransactionSeconds = Integer.parseInt(System.getProperty(propertyName, "10"));
      }
      catch(Exception e)
      {
         LOG.debug("Error reading property [" + propertyName + "] value as integer", e);
      }

      this.connection = connection;
   }



   /*******************************************************************************
    ** Getter for connection
    **
    *******************************************************************************/
   public Connection getConnection()
   {
      return connection;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void commit() throws QException
   {
      try
      {
         Instant commitAt = Instant.now();

         Duration duration = Duration.between(openedAt, commitAt);
         if(logSlowTransactionSeconds != null && duration.compareTo(Duration.ofSeconds(logSlowTransactionSeconds)) > 0)
         {
            LOG.info("Committing long-running transaction", logPair("durationSeconds", duration.getSeconds()));
         }
         else
         {
            LOG.debug("Committing transaction");
         }

         connection.commit();
         LOG.debug("Commit complete");
      }
      catch(Exception e)
      {
         LOG.error("Error committing transaction", e);

         ///////////////////////////////////////////////////////////////////////////
         // the callbacks' work was not committed - so drop them now, rather than //
         // running them on a later commit of this (re-opened) transaction        //
         ///////////////////////////////////////////////////////////////////////////
         discardAfterCommitCallbacks();
         throw new QException("Error committing transaction: " + e.getMessage(), e);
      }
      finally
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // reset this - as after one commit, the transaction is essentially re-opened for any future statements that run on it //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         openedAt = Instant.now();
      }

      ///////////////////////////////////////////////////////////////////////
      // only reached if the commit succeeded (else the catch threw above) //
      ///////////////////////////////////////////////////////////////////////
      runAfterCommitCallbacks();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void rollback() throws QException
   {
      try
      {
         LOG.info("Rolling back transaction");
         connection.rollback();
         LOG.info("Rollback complete");
      }
      catch(Exception e)
      {
         LOG.error("Error rolling back transaction", e);
         throw new QException("Error rolling back transaction: " + e.getMessage(), e);
      }
      finally
      {
         ///////////////////////////////////////////////////////////////////////
         // base class discards after-commit callbacks - even if the rollback //
         // failed, their work was never committed                            //
         ///////////////////////////////////////////////////////////////////////
         super.rollback();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void close()
   {
      try
      {
         if(connection.isClosed())
         {
            return;
         }

         connection.close();
      }
      catch(Exception e)
      {
         LOG.error("Error closing connection - possible jdbc connection leak", e);
      }
   }
}
