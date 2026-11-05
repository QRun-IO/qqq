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
import java.time.Duration;
import java.time.Instant;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.TransactionalJobAdmission;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** RDBMS implementation of backend transaction.
 **
 ** Stores a jdbc connection, which is set to autoCommit(false).
 *******************************************************************************/
public class RDBMSTransaction extends QBackendTransaction implements TransactionalJobAdmission
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSTransaction.class);

   private Connection connection;
   private String atomicBackendName;
   private String jobAdmissionIdentity;

   private Instant openedAt                  = Instant.now();
   private boolean rolledBackSinceCommit     = false;
   private Integer logSlowTransactionSeconds = null;



   /*******************************************************************************
    ** JDBC rollback provides a confirmed boundary for external resource compensation.
    *******************************************************************************/
   @Override
   public boolean supportsRollbackCallbacks()
   {
      return (true);
   }



   /*******************************************************************************
    ** New compensation means the next close must still roll back, even if an earlier
    ** rollback already happened.
    *******************************************************************************/
   @Override
   public void addAfterRollbackCallback(Runnable callback)
   {
      super.addAfterRollbackCallback(callback);
      rolledBackSinceCommit = false;
   }



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
    ** Verify atomic participation on this connection before required native work.
    *******************************************************************************/
   @Override
   public void requireAtomicWrites(QTableMetaData table) throws QException
   {
      try
      {
         requireCommitAllowed();
         if(this instanceof RDBMSConsistentReadTransaction)
         {
            throw (new QException("A consistent-read transaction cannot own required record mutations"));
         }
         String backend = RDBMSAtomicWrites.require(connection, table);
         if(atomicBackendName != null && !atomicBackendName.equals(backend))
         {
            throw (new QException("Required record mutations must share one native backend"));
         }
         atomicBackendName = backend;
      }
      catch(QException error)
      {
         markRollbackOnly();
         throw (error);
      }
   }



   /*******************************************************************************
    ** Admit durable Jobs on this exact source transaction, never another checkout.
    *******************************************************************************/
   @Override
   public String lockJobAdmission(QTableMetaData sourceTable) throws QException
   {
      try
      {
         requireAtomicWrites(sourceTable);
         if(jobAdmissionIdentity == null)
         {
            jobAdmissionIdentity = RDBMSJobAdmission.lock(connection, sourceTable);
         }
         return (jobAdmissionIdentity);
      }
      catch(QException | RuntimeException error)
      {
         markRollbackOnly();
         throw (error);
      }
   }



   /*******************************************************************************
    ** Read the database clock on the current transaction.
    *******************************************************************************/
   @Override
   public Instant jobDatabaseNow() throws QException
   {
      requireCommitAllowed();
      return (RDBMSJobAdmission.now(connection));
   }



   /*******************************************************************************
    ** Lock existing chunks before their job row for atomic control operations.
    *******************************************************************************/
   @Override
   public void lockJobControlRows(Integer jobId) throws QException
   {
      try
      {
         requireCommitAllowed();
         if(jobAdmissionIdentity == null)
         {
            throw (new QException("Jobs control locks require prior admission"));
         }
         RDBMSJobAdmission.lockControlRows(connection, jobId);
      }
      catch(QException error)
      {
         markRollbackOnly();
         throw (error);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void commit() throws QException
   {
      requireCommitAllowed();
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
         rolledBackSinceCommit = false;
         jobAdmissionIdentity = null;
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
      rollbackConnection();
   }



   /*******************************************************************************
    ** Release pending JDBC work and compensate resources. Close uses this directly
    ** so cleanup does not invoke an application's explicit rollback override after
    ** a successful commit.
    *******************************************************************************/
   private void rollbackConnection() throws QException
   {
      try
      {
         LOG.info("Rolling back transaction");
         connection.rollback();
         rolledBackSinceCommit = true;
         runAfterRollbackCallbacks();
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
         jobAdmissionIdentity = null;
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

         try
         {
            /////////////////////////////////////////////////////////////////////////////
            // release pending work and run compensation - unless a rollback (e.g., by //
            // a subclass's close) already did, which must not be repeated             //
            /////////////////////////////////////////////////////////////////////////////
            if(!rolledBackSinceCommit)
            {
               rollbackConnection();
            }
         }
         finally
         {
            connection.close();
         }
      }
      catch(Exception e)
      {
         LOG.error("Error closing connection - possible jdbc connection leak", e);
      }
   }
}
