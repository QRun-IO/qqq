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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.time.Duration;
import java.time.Instant;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** MongoDB implementation of backend transaction.
 **
 ** Stores a mongoClient and clientSession.
 **
 ** Also keeps track of if the specific mongo backend being used supports transactions,
 ** as, it appears that single-node instances do not, and they throw errors if
 ** you try to do transaction operations in them...  This is configured by the
 ** corresponding field in the backend metaData.
 *******************************************************************************/
public class MongoDBTransaction extends QBackendTransaction
{
   private static final QLogger LOG = QLogger.getLogger(MongoDBTransaction.class);

   private boolean       transactionsSupported;
   private MongoClient   mongoClient;
   private ClientSession clientSession;

   private Instant openedAt                  = Instant.now();
   private Integer logSlowTransactionSeconds = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   public MongoDBTransaction(MongoDBBackendMetaData backend, MongoClient mongoClient)
   {
      this.transactionsSupported = backend.getTransactionsSupported();
      ClientSession clientSession = mongoClient.startSession();

      if(transactionsSupported)
      {
         clientSession.startTransaction();
      }

      String propertyName = "qqq.mongodb.logSlowTransactionSeconds";
      try
      {
         logSlowTransactionSeconds = Integer.parseInt(System.getProperty(propertyName, "10"));
      }
      catch(Exception e)
      {
         LOG.debug("Error reading property [" + propertyName + "] value as integer", e);
      }

      this.mongoClient = mongoClient;
      this.clientSession = clientSession;
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

         if(transactionsSupported)
         {
            this.clientSession.commitTransaction();
            LOG.debug("Commit complete");
         }
         else
         {
            LOG.debug("Request to commit, but transactions not supported in this mongodb backend");
         }
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
         if(transactionsSupported)
         {
            this.clientSession.startTransaction();
         }
      }

      ////////////////////////////////////////////////////////////////////////
      // only reached if the commit succeeded (else the catch threw above). //
      // without transaction support, writes were already durable, so the   //
      // callbacks run then too.                                            //
      ////////////////////////////////////////////////////////////////////////
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
         if(transactionsSupported)
         {
            LOG.info("Rolling back transaction");
            this.clientSession.abortTransaction();
            LOG.info("Rollback complete");
         }
         else
         {
            LOG.debug("Request to rollback, but transactions not supported in this mongodb backend");
         }
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
         // failed (or isn't supported), they must not run on a later commit  //
         ///////////////////////////////////////////////////////////////////////
         super.rollback();

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // reset this - as after one commit, the transaction is essentially re-opened for any future statements that run on it //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         openedAt = Instant.now();
         if(transactionsSupported)
         {
            this.clientSession.startTransaction();
         }
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
         this.clientSession.close();
         this.mongoClient.close();
      }
      catch(Exception e)
      {
         LOG.error("Error closing connection - possible mongo connection leak", e);
      }
   }



   /*******************************************************************************
    ** Getter for mongoClient
    **
    *******************************************************************************/
   public MongoClient getMongoClient()
   {
      return mongoClient;
   }



   /*******************************************************************************
    ** Getter for clientSession
    **
    *******************************************************************************/
   public ClientSession getClientSession()
   {
      return clientSession;
   }
}
