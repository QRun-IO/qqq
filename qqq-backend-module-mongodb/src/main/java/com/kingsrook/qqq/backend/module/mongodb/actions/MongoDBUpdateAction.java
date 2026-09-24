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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UpdateActionRecordSplitHelper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;


/*******************************************************************************
 **
 *******************************************************************************/
public class MongoDBUpdateAction extends AbstractMongoDBAction implements UpdateInterface
{
   private static final QLogger LOG = QLogger.getLogger(MongoDBUpdateAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public UpdateOutput execute(UpdateInput updateInput) throws QException
   {
      MongoClientContainer   mongoClientContainer = null;
      QTableMetaData         table                = updateInput.getTable();
      String                 backendTableName     = getBackendTableName(table);
      MongoDBBackendMetaData backend              = (MongoDBBackendMetaData) updateInput.getBackend();

      UpdateActionRecordSplitHelper updateActionRecordSplitHelper = new UpdateActionRecordSplitHelper();
      updateActionRecordSplitHelper.init(updateInput);

      UpdateOutput rs = new UpdateOutput();
      rs.setRecords(updateActionRecordSplitHelper.getOutputRecords());

      if(!updateActionRecordSplitHelper.getHaveAnyWithoutErrors())
      {
         LOG.info("Exiting early - all records have some error.");
         return (rs);
      }

      try
      {
         mongoClientContainer = openClient(backend, updateInput.getTransaction());
         MongoDatabase             database   = mongoClientContainer.getMongoClient().getDatabase(backend.getDatabaseName());
         MongoCollection<Document> collection = database.getCollection(backendTableName);

         /////////////////////////////////////////////////////////////////////////////////////////////
         // process each distinct list of fields being updated (e.g., each different SQL statement) //
         /////////////////////////////////////////////////////////////////////////////////////////////
         ListingHash<List<String>, QRecord> recordsByFieldBeingUpdated = updateActionRecordSplitHelper.getRecordsByFieldBeingUpdated();
         for(Map.Entry<List<String>, List<QRecord>> entry : recordsByFieldBeingUpdated.entrySet())
         {
            updateRecordsWithMatchingListOfFields(updateInput, mongoClientContainer, collection, table, entry.getValue(), entry.getKey());
         }
      }
      catch(Exception e)
      {
         throw new QException("Error executing update: " + e.getMessage(), e);
      }
      finally
      {
         if(mongoClientContainer != null)
         {
            mongoClientContainer.closeIfNeeded();
         }
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void updateRecordsWithMatchingListOfFields(UpdateInput updateInput, MongoClientContainer mongoClientContainer, MongoCollection<Document> collection, QTableMetaData table, List<QRecord> recordList, List<String> fieldsBeingUpdated)
   {
      boolean allAreTheSame = UpdateActionRecordSplitHelper.areAllValuesBeingUpdatedTheSame(updateInput, recordList, fieldsBeingUpdated);
      if(allAreTheSame)
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if all records w/ this set of fields have the same values, we can do 1 big updateMany on the whole list //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         updateRecordsWithMatchingValuesAndFields(mongoClientContainer, collection, table, recordList, fieldsBeingUpdated);
      }
      else
      {
         /////////////////////////////////////////////////////////////////////////
         // else, if not all are being updated the same, then update one-by-one //
         /////////////////////////////////////////////////////////////////////////
         for(QRecord record : recordList)
         {
            updateRecordsWithMatchingValuesAndFields(mongoClientContainer, collection, table, List.of(record), fieldsBeingUpdated);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void updateRecordsWithMatchingValuesAndFields(MongoClientContainer mongoClientContainer, MongoCollection<Document> collection, QTableMetaData table, List<QRecord> recordList, List<String> fieldsBeingUpdated)
   {
      Long       queryStartTime = System.currentTimeMillis();
      List<Bson> queryToLog     = new ArrayList<>();

      QRecord        firstRecord = recordList.get(0);
      List<ObjectId> ids         = recordList.stream().map(r -> new ObjectId(r.getValueString(table.getPrimaryKeyField()))).toList();
      Bson           filter      = Filters.in("_id", ids);
      queryToLog.add(filter);

      List<Bson> updates = new ArrayList<>();
      for(String fieldName : fieldsBeingUpdated)
      {
         QFieldMetaData field            = table.getField(fieldName);
         String         fieldBackendName = getFieldBackendName(field);
         Bson set = Updates.set(fieldBackendName, firstRecord.getValue(fieldName));
         updates.add(set);
         queryToLog.add(set);
      }
      Bson changes = Updates.combine(updates);

      UpdateResult updateResult = collection.updateMany(mongoClientContainer.getMongoSession(), filter, changes);
      // todo - anything with the output??

      logQuery(getBackendTableName(table), "update", queryToLog, queryStartTime);
   }

}
