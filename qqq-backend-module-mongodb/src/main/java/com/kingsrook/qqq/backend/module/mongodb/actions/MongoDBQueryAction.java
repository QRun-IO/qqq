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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ActionTimeoutHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.module.mongodb.fieldfunctions.MongoDBFieldFunctionAdapterInterface;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;


/*******************************************************************************
 * Execute a Query action against a MongoDB backend.
 * 
 * <p>Note that this a {@code collection.aggregate()} even if it could maybe do
 * a {@code collection.find()}, to keep the code paths simpler in here, and to
 * support {@link FieldFunction}s (which require {@code aggregate}).  The performance
 * difference is hopefully not so bad, but this could potentially be a future
 * optimization if it was found to be worthwhile.</p>
 *******************************************************************************/
public class MongoDBQueryAction extends AbstractMongoDBAction implements QueryInterface
{
   private static final QLogger LOG = QLogger.getLogger(MongoDBQueryAction.class);

   private ActionTimeoutHelper actionTimeoutHelper;



   /*******************************************************************************
    ** Display locks and unrelated computed fields cannot hide stored key conflicts.
    *******************************************************************************/
   @Override
   public List<QRecord> lookupUniqueKey(UniqueKeyLookup.Input input) throws QException
   {
      QueryInput queryInput = input.newQueryInput();
      QTableMetaData table = queryInput.getTable().clone();
      table.setRecordSecurityLocks(List.of());
      table.setVirtualFields(Map.of());
      queryInput.setTableMetaData(table);
      return execute(queryInput).getRecords();
   }



   /*******************************************************************************
    ** Stored relationship values are independent of display locks and computations.
    *******************************************************************************/
   @Override
   public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
   {
      QueryInput queryInput = input.newQueryInput();
      QTableMetaData table = queryInput.getTable().clone();
      table.setRecordSecurityLocks(List.of());
      table.setVirtualFields(Map.of());
      queryInput.setTableMetaData(table);
      return executeForDml(queryInput).getRecords();
   }



   /*******************************************************************************
    ** Discover relationship membership without changing the caller's read visibility.
    *******************************************************************************/
   @Override
   public List<Serializable> findAssociatedPrimaryKeys(AssociatedRecordDiscovery.Input input) throws QException
   {
      QueryInput queryInput = input.newQueryInput();
      QTableMetaData table = queryInput.getTable().clone();
      table.setRecordSecurityLocks(List.of());
      table.setVirtualFields(Map.of());
      queryInput.setTableMetaData(table);
      return executeForDml(queryInput).getRecords().stream().map(record -> record.getValue(table.getPrimaryKeyField())).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutput execute(QueryInput queryInput) throws QException
   {
      return execute(queryInput, false);
   }



   /*******************************************************************************
    ** Reject native identities that cannot safely select a later MongoDB write.
    *******************************************************************************/
   @Override
   public QueryOutput executeForDml(QueryInput queryInput) throws QException
   {
      if(queryInput.getRecordPipe() != null)
      {
         throw new QException("DML prefetch requires a materialized record list");
      }
      return execute(queryInput, true);
   }



   /*******************************************************************************
    ** Ordinary reads retain their existing native identity and projection behavior.
    *******************************************************************************/
   private QueryOutput execute(QueryInput queryInput, Boolean requireWriteIdentity) throws QException
   {
      MongoClientContainer mongoClientContainer = null;

      Long       queryStartTime = System.currentTimeMillis();
      List<Bson> queryToLog     = new ArrayList<>();

      try
      {
         QueryOutput            queryOutput      = new QueryOutput(queryInput);
         QTableMetaData         table            = queryInput.getTable();
         String                 backendTableName = getBackendTableName(table);
         MongoDBBackendMetaData backend          = (MongoDBBackendMetaData) queryInput.getBackend();

         mongoClientContainer = openClient(backend, queryInput.getTransaction());
         MongoDatabase             database   = mongoClientContainer.getMongoClient().getDatabase(backend.getDatabaseName());
         MongoCollection<Document> collection = database.getCollection(backendTableName);

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // set up & start an actionTimeoutHelper (note, internally it'll deal with the time being null or negative as meaning not to timeout) //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         actionTimeoutHelper = new ActionTimeoutHelper(queryInput.getTimeoutSeconds(), TimeUnit.SECONDS, new TimeoutCanceller(mongoClientContainer));
         actionTimeoutHelper.start();

         /////////////////////////
         // set up filter/query //
         /////////////////////////
         QQueryFilter filter = queryInput.getFilter();

         /////////////////////////////////////
         // build the aggregation pipeline  //
         /////////////////////////////////////
         List<Bson> pipeline = makeFilterPipeline(table, backend, filter);

         ///////////////////////////////////////////////////////////////////////
         // Preserve named virtual projection after filtering original values. //
         // Inline predicate aliases have already been removed with the wrapper. //
         ///////////////////////////////////////////////////////////////////////
         Document addFieldsDocument = buildAddFieldsDocument(table, backend, null);
         if(requireWriteIdentity && addFieldsDocument.keySet().stream().anyMatch(name -> name.equals("_id") || name.startsWith("_id.")))
         {
            throw new QException("MongoDB write prefetch cannot replace the native primary key _id");
         }
         if(!addFieldsDocument.isEmpty())
         {
            pipeline.add(new Document("$addFields", addFieldsDocument));
         }

         ///////////////////////////////////
         // add a sort operator if needed //
         ///////////////////////////////////
         Set<String> tempSortFieldNames = new HashSet<>();
         if(filter != null && CollectionUtils.nullSafeHasContents(filter.getOrderBys()))
         {
            Document sortDocument      = new Document();
            Document extraAddFields    = new Document();

            for(QFilterOrderBy orderBy : filter.getOrderBys())
            {
               QFieldMetaData field = table.getFieldOrVirtualField(orderBy.getFieldName());

               if(field instanceof QVirtualFieldMetaData virtualField)
               {
                  FieldFunction fieldFunction = virtualField.getFieldFunction();
                  MongoDBFieldFunctionAdapterInterface adapter = backend.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
                  if(adapter != null)
                  {
                     Object orderByExpression = adapter.getExpressionForOrderBy(getFieldReference(table, fieldFunction.getFieldName()), fieldFunction, fieldName -> getFieldReference(table, fieldName));
                     Object selectExpression  = adapter.getExpression(getFieldReference(table, fieldFunction.getFieldName()), fieldFunction, fieldName -> getFieldReference(table, fieldName));

                     if(!orderByExpression.equals(selectExpression))
                     {
                        //////////////////////////////////////////////////////////////////////////////////
                        // the order-by expression differs from select — add a temp field to sort on   //
                        //////////////////////////////////////////////////////////////////////////////////
                        String tempFieldName = "__sort_" + virtualField.getName();
                        tempSortFieldNames.add(tempFieldName);
                        extraAddFields.append(tempFieldName, orderByExpression);
                        sortDocument.put(tempFieldName, orderBy.getIsAscending() ? 1 : -1);
                     }
                     else
                     {
                        sortDocument.put(virtualField.getName(), orderBy.getIsAscending() ? 1 : -1);
                     }
                  }
               }
               else
               {
                  String fieldBackendName = getFieldBackendName(field);
                  sortDocument.put(fieldBackendName, orderBy.getIsAscending() ? 1 : -1);
               }
            }

            if(!extraAddFields.isEmpty())
            {
               pipeline.add(new Document("$addFields", extraAddFields));
            }

            if(!sortDocument.isEmpty())
            {
               pipeline.add(new Document("$sort", sortDocument));
            }
         }

         ////////////////////////
         // apply skip & limit //
         ////////////////////////
         if(filter != null)
         {
            if(filter.getSkip() != null)
            {
               pipeline.add(new Document("$skip", filter.getSkip()));
            }

            if(filter.getLimit() != null)
            {
               pipeline.add(new Document("$limit", filter.getLimit()));
            }
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////
         // if input specifies a set of field names to include, then add a $project stage              //
         // also remove any temp sort fields                                                           //
         ///////////////////////////////////////////////////////////////////////////////////////////////
         if(queryInput.getFieldNamesToInclude() != null || !tempSortFieldNames.isEmpty())
         {
            Document projectDocument = new Document();

            if(queryInput.getFieldNamesToInclude() != null)
            {
               for(String fieldName : queryInput.getFieldNamesToInclude())
               {
                  String backendFieldName = getFieldBackendName(table.getField(fieldName));
                  projectDocument.append(backendFieldName, 1);
               }

               /////////////////////////////////////////////////////
               // also include virtual fields in the projection   //
               /////////////////////////////////////////////////////
               for(QVirtualFieldMetaData virtualField : CollectionUtils.nonNullMap(table.getVirtualFields()).values())
               {
                  if(virtualField.getIsQuerySelectable())
                  {
                     projectDocument.append(virtualField.getName(), 1);
                  }
               }
            }

            for(String tempFieldName : tempSortFieldNames)
            {
               projectDocument.append(tempFieldName, 0);
            }

            if(!projectDocument.isEmpty())
            {
               pipeline.add(new Document("$project", projectDocument));
            }
         }

         queryToLog = pipeline;
         setQueryInQueryStat(new Document("pipeline", pipeline));

         ////////////////////////////////////////////
         // iterate over results, building records //
         ////////////////////////////////////////////
         try(MongoCursor<Document> cursor = collection.aggregate(mongoClientContainer.getMongoSession(), pipeline).iterator())
         {
            while(cursor.hasNext())
            {
               Document document = cursor.next();
               /////////////////////////////////////////////////////////////////////////
               // once we've started getting results, go ahead and cancel the timeout //
               /////////////////////////////////////////////////////////////////////////
               actionTimeoutHelper.cancel();
               setQueryStatFirstResultTime();

               String nativePrimaryKey = null;
               if(requireWriteIdentity)
               {
                  if(document.get("_id") instanceof ObjectId objectId)
                  {
                     nativePrimaryKey = objectId.toHexString();
                  }
                  else
                  {
                     throw new QException("MongoDB writes require native ObjectId primary keys");
                  }
               }

               QRecord record = documentToRecord(queryInput, document);
               if(requireWriteIdentity && !nativePrimaryKey.equals(record.getValue(table.getPrimaryKeyField())))
               {
                  throw new QException("MongoDB primary key changed during write prefetch");
               }
               queryOutput.addRecord(record);

               if(queryInput.getAsyncJobCallback().wasCancelRequested())
               {
                  LOG.info("Breaking query job, as requested.");
                  break;
               }
            }
         }

         return (queryOutput);
      }
      catch(Exception e)
      {
         if(actionTimeoutHelper != null && actionTimeoutHelper.getDidTimeout())
         {
            setQueryStatFirstResultTime();
            throw (new QUserFacingException("Query timed out."));
         }

         LOG.warn("Error executing query", e);
         throw new QException("Error executing query", e);
      }
      finally
      {
         if(actionTimeoutHelper != null)
         {
            actionTimeoutHelper.cancel();
         }

         logQuery(getBackendTableName(queryInput.getTable()), "query", queryToLog, queryStartTime);

         if(mongoClientContainer != null)
         {
            mongoClientContainer.closeIfNeeded();
         }
      }
   }

}
