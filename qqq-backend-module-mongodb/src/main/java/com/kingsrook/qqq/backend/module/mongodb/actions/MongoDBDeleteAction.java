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
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class MongoDBDeleteAction extends AbstractMongoDBAction implements DeleteInterface
{
   private static final QLogger LOG = QLogger.getLogger(MongoDBDeleteAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean supportsQueryFilterInput()
   {
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public DeleteOutput execute(DeleteInput deleteInput) throws QException
   {
      MongoClientContainer mongoClientContainer = null;

      Long       queryStartTime = System.currentTimeMillis();
      List<Bson> queryToLog     = new ArrayList<>();

      try
      {
         DeleteOutput           deleteOutput     = new DeleteOutput();
         QTableMetaData         table            = deleteInput.getTable();
         String                 backendTableName = getBackendTableName(table);
         MongoDBBackendMetaData backend          = (MongoDBBackendMetaData) deleteInput.getBackend();

         mongoClientContainer = openClient(backend, deleteInput.getTransaction());
         MongoDatabase             database   = mongoClientContainer.getMongoClient().getDatabase(backend.getDatabaseName());
         MongoCollection<Document> collection = database.getCollection(backendTableName);

         QQueryFilter queryFilter = deleteInput.getQueryFilter();
         Bson         searchQuery;
         if(CollectionUtils.nullSafeHasContents(deleteInput.getPrimaryKeys()))
         {
            searchQuery = Filters.in("_id", deleteInput.getPrimaryKeys().stream().map(id -> new ObjectId(ValueUtils.getValueAsString(id))).toList());
         }
         else if(queryFilter != null && queryFilter.hasAnyCriteria())
         {
            QQueryFilter filter = queryFilter;
            searchQuery = makeSearchQueryDocument(table, filter);
         }
         else
         {
            LOG.info("Missing both primary keys and a search filter in delete request - exiting with noop", logPair("tableName", table.getName()));
            return (deleteOutput);
         }

         queryToLog.add(searchQuery);

         ////////////////////////////////////////////////////////
         // todo - system property to control (like print-sql) //
         ////////////////////////////////////////////////////////
         // LOG.debug(searchQuery);

         DeleteResult deleteResult = collection.deleteMany(mongoClientContainer.getMongoSession(), searchQuery);
         deleteOutput.setDeletedRecordCount((int) deleteResult.getDeletedCount());

         //////////////////////////////////////////////////////////////////////////
         // todo any way to get records with errors or warnings for deleteOutput //
         //////////////////////////////////////////////////////////////////////////

         return (deleteOutput);
      }
      catch(Exception e)
      {
         LOG.warn("Error executing delete", e);
         throw new QException("Error executing delete", e);
      }
      finally
      {
         logQuery(getBackendTableName(deleteInput.getTable()), "delete", queryToLog, queryStartTime);

         if(mongoClientContainer != null)
         {
            mongoClientContainer.closeIfNeeded();
         }
      }
   }

}
