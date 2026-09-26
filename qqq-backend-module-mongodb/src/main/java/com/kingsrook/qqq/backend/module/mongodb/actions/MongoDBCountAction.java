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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ActionTimeoutHelper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.conversions.Bson;


/*******************************************************************************
 **
 *******************************************************************************/
public class MongoDBCountAction extends AbstractMongoDBAction implements CountInterface
{
   private static final QLogger LOG = QLogger.getLogger(MongoDBCountAction.class);

   private ActionTimeoutHelper actionTimeoutHelper;



   /*******************************************************************************
    **
    *******************************************************************************/
   public CountOutput execute(CountInput countInput) throws QException
   {
      MongoClientContainer mongoClientContainer = null;

      Long       queryStartTime = System.currentTimeMillis();
      List<Bson> queryToLog     = new ArrayList<>();

      try
      {
         CountOutput            countOutput      = new CountOutput();
         QTableMetaData         table            = countInput.getTable();
         String                 backendTableName = getBackendTableName(table);
         MongoDBBackendMetaData backend          = (MongoDBBackendMetaData) countInput.getBackend();

         mongoClientContainer = openClient(backend, countInput.getTransaction());
         MongoDatabase             database   = mongoClientContainer.getMongoClient().getDatabase(backend.getDatabaseName());
         MongoCollection<Document> collection = database.getCollection(backendTableName);

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // set up & start an actionTimeoutHelper (note, internally it'll deal with the time being null or negative as meaning not to timeout) //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         actionTimeoutHelper = new ActionTimeoutHelper(countInput.getTimeoutSeconds(), TimeUnit.SECONDS, new TimeoutCanceller(mongoClientContainer));
         actionTimeoutHelper.start();

         QQueryFilter filter = countInput.getFilter();
         List<Bson> bsonList = makeFilterPipeline(table, backend, filter);
         bsonList.add(Aggregates.group("_id", Accumulators.sum("count", 1)));
         queryToLog = bsonList;
         setQueryInQueryStat(new Document("pipeline", bsonList));

         AggregateIterable<Document> aggregate = collection.aggregate(mongoClientContainer.getMongoSession(), bsonList);

         Document document = aggregate.first();
         countOutput.setCount(document == null ? 0 : document.get("count", Integer.class));

         actionTimeoutHelper.cancel();
         setQueryStatFirstResultTime();

         return (countOutput);
      }
      catch(Exception e)
      {
         if(actionTimeoutHelper != null && actionTimeoutHelper.getDidTimeout())
         {
            setQueryStatFirstResultTime();
            throw (new QUserFacingException("Count timed out."));
         }

         /*
         /////////////////////////////////////////////////////////////////////////////////////
         // this was copied from RDBMS - not sure where/how/if it's being used there though //
         /////////////////////////////////////////////////////////////////////////////////////
         if(isCancelled)
         {
            throw (new QUserFacingException("Count was cancelled."));
         }
         */

         LOG.warn("Error executing count", e);
         throw new QException("Error executing count", e);
      }
      finally
      {
         logQuery(getBackendTableName(countInput.getTable()), "count", queryToLog, queryStartTime);

         if(mongoClientContainer != null)
         {
            mongoClientContainer.closeIfNeeded();
         }
      }
   }

}
