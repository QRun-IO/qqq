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

package com.kingsrook.qqq.backend.module.mongodb;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.InsertInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyScope;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.module.mongodb.actions.AbstractMongoDBAction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoClientContainer;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBAggregateAction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBConsistentReadTransaction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBCountAction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBDeleteAction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBInsertAction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBQueryAction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBTransaction;
import com.kingsrook.qqq.backend.module.mongodb.actions.MongoDBUpdateAction;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBTableBackendDetails;
import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.bson.Document;


/*******************************************************************************
 ** QQQ Backend module for working with MongoDB
 *******************************************************************************/
public class MongoDBBackendModule implements QBackendModuleInterface
{
   static
   {
      QBackendModuleDispatcher.registerBackendModule(new MongoDBBackendModule());
   }

   /*******************************************************************************
    ** Method where a backend module must be able to provide its type (name).
    *******************************************************************************/
   public String getBackendType()
   {
      return ("mongodb");
   }



   /*******************************************************************************
    ** Method to identify the class used for backend meta data for this module.
    *******************************************************************************/
   @Override
   public Class<? extends QBackendMetaData> getBackendMetaDataClass()
   {
      return (MongoDBBackendMetaData.class);
   }



   /*******************************************************************************
    ** Method to identify the class used for table-backend details for this module.
    *******************************************************************************/
   @Override
   public Class<? extends QTableBackendDetails> getTableBackendDetailsClass()
   {
      return (MongoDBTableBackendDetails.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public CountInterface getCountInterface()
   {
      return (new MongoDBCountAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueryInterface getQueryInterface()
   {
      return (new MongoDBQueryAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public InsertInterface getInsertInterface()
   {
      return (new MongoDBInsertAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public UpdateInterface getUpdateInterface()
   {
      return (new MongoDBUpdateAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public DeleteInterface getDeleteInterface()
   {
      return (new MongoDBDeleteAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public AggregateInterface getAggregateInterface()
   {
      return (new MongoDBAggregateAction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QBackendTransaction openTransaction(AbstractTableActionInput input)
   {
      MongoDBBackendMetaData backend              = (MongoDBBackendMetaData) input.getBackend();
      MongoClientContainer   mongoClientContainer = new AbstractMongoDBAction().openClient(backend, null);
      return (new MongoDBTransaction(backend, mongoClientContainer.getMongoClient(), mongoClientContainer.getMongoSession()));
   }


   /*******************************************************************************
    ** Open a session with snapshot read concern, so all reads through it see one
    ** point in time - best effort, as:
    ** - snapshot reads need a replica set (or sharded cluster).  On a standalone
    **   server, or for TABLE scope, we use a plain session, and report PER_TABLE.
    ** - the snapshot is only kept for minSnapshotHistoryWindowInSeconds (300 by
    **   default) - reads after that fail (SnapshotTooOld), and the caller should
    **   fall back (e.g., to per-table consistency).
    ** Sharing a snapshot with other sessions isn't supported - so no exported id.
    *******************************************************************************/
   @Override
   public Optional<QBackendTransaction> openConsistentReadTransaction(ConsistentReadInput input) throws QException
   {
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) QContext.getQInstance().getBackend(input.getBackendName());
      if(backend == null)
      {
         throw (new QException("Unrecognized backend name: " + input.getBackendName()));
      }

      if(input.getImportSnapshotId().isPresent())
      {
         throw (new QException("Importing a snapshot is not supported by the MongoDB backend"));
      }

      ///////////////////////////////////////////////////////////////////////////
      // openClient starts a (plain) session - which we replace with our own,  //
      // so close that one, and just keep the client                           //
      ///////////////////////////////////////////////////////////////////////////
      MongoClientContainer container   = new AbstractMongoDBAction().openClient(backend, null);
      MongoClient          mongoClient = container.getMongoClient();
      try
      {
         container.getMongoSession().close();

         boolean snapshotSupported = false;
         if(input.getScope() == ConsistencyScope.BACKEND)
         {
            Document hello = mongoClient.getDatabase(backend.getDatabaseName()).runCommand(new Document("hello", 1));
            snapshotSupported = hello.containsKey("setName") || "isdbgrid".equals(hello.getString("msg"));
         }

         ClientSession session = snapshotSupported
            ? mongoClient.startSession(ClientSessionOptions.builder().snapshot(true).build())
            : mongoClient.startSession();

         return (Optional.of(new MongoDBConsistentReadTransaction(backend, mongoClient, session, snapshotSupported ? ConsistencyGuarantee.ONE_INSTANT : ConsistencyGuarantee.PER_TABLE)));
      }
      catch(RuntimeException e)
      {
         mongoClient.close();
         throw (new QException("Error opening consistent-read transaction: " + e.getMessage(), e));
      }
   }

}
