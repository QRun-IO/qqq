/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.AbstractFilterExpression;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.NullValueBehaviorUtil;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLockFilters;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.mongodb.fieldfunctions.MongoDBFieldFunctionAdapterInterface;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBTableBackendDetails;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for all mongoDB module actions.
 *******************************************************************************/
public class AbstractMongoDBAction
{
   private static final QLogger LOG = QLogger.getLogger(AbstractMongoDBAction.class);

   protected QueryStat queryStat;



   /*******************************************************************************
    ** Open a MongoDB Client / session -- re-using the one in the input transaction
    ** if it is present.
    *******************************************************************************/
   public MongoClientContainer openClient(MongoDBBackendMetaData backend, QBackendTransaction transaction)
   {
      if(transaction instanceof MongoDBTransaction mongoDBTransaction)
      {
         //////////////////////////////////////////////////////////////////////////////////////////
         // re-use the connection from the transaction (indicating false in last parameter here) //
         //////////////////////////////////////////////////////////////////////////////////////////
         return (new MongoClientContainer(mongoDBTransaction.getMongoClient(), mongoDBTransaction.getClientSession(), false));
      }

      String           suffix           = StringUtils.hasContent(backend.getUrlSuffix()) ? "?" + backend.getUrlSuffix() : "";
      ConnectionString connectionString = new ConnectionString("mongodb://" + backend.getHost() + ":" + backend.getPort() + "/" + suffix);

      MongoCredential credential = MongoCredential.createCredential(backend.getUsername(), backend.getAuthSourceDatabase(), backend.getPassword().toCharArray());

      MongoClientSettings settings = MongoClientSettings.builder()

         ////////////////////////////////////////////////
         // is this needed, what, for a cluster maybe? //
         ////////////////////////////////////////////////
         // .applyToClusterSettings(builder -> builder.hosts(seeds))

         .applyConnectionString(connectionString)
         .credential(credential)
         .build();

      MongoClient mongoClient = MongoClients.create(settings);

      ////////////////////////////////////////////////////////////////////////////
      // indicate that this connection was newly opened via the true param here //
      ////////////////////////////////////////////////////////////////////////////
      return (new MongoClientContainer(mongoClient, mongoClient.startSession(), true));
   }



   /*******************************************************************************
    ** Get the name to use for a field in the mongoDB, from the fieldMetaData.
    **
    ** That is, field.backendName if set -- else, field.name
    *******************************************************************************/
   protected String getFieldBackendName(QFieldMetaData field)
   {
      if(field.getBackendName() != null)
      {
         return (field.getBackendName());
      }
      return (field.getName());
   }



   /*******************************************************************************
    ** Get the name to use for a table in the mongoDB, from the table's backendDetails.
    **
    ** else, the table's name.
    *******************************************************************************/
   protected String getBackendTableName(QTableMetaData table)
   {
      if(table == null)
      {
         return (null);
      }

      if(table.getBackendDetails() != null)
      {
         String backendTableName = ((MongoDBTableBackendDetails) table.getBackendDetails()).getTableName();
         if(StringUtils.hasContent(backendTableName))
         {
            return (backendTableName);
         }
      }
      return table.getName();
   }



   /*******************************************************************************
    ** Get a MongoDB aggregation field reference for a field in the table.
    ** Returns "$" + the field's backend name.
    *******************************************************************************/
   protected String getFieldReference(QTableMetaData table, String fieldName)
   {
      return "$" + getFieldBackendName(table.getField(fieldName));
   }



   /*******************************************************************************
    ** Build an $addFields document for virtual fields on a table.
    ** Each virtual field that is querySelectable or queryCriteria will have an
    ** entry mapping its name to the appropriate MongoDB aggregation expression.
    **
    ** Also handles fieldFunction on criteria for non-virtual fields.
    *******************************************************************************/
   protected Document buildAddFieldsDocument(QTableMetaData table, MongoDBBackendMetaData backend, QQueryFilter filter)
   {
      Document addFieldsDocument = new Document();

      //////////////////////////////////////////
      // add expressions for virtual fields  //
      //////////////////////////////////////////
      for(QVirtualFieldMetaData virtualField : CollectionUtils.nonNullMap(table.getVirtualFields()).values())
      {
         if(virtualField.getIsQuerySelectable() || virtualField.getIsQueryCriteria())
         {
            FieldFunction fieldFunction = virtualField.getFieldFunction();
            if(fieldFunction != null)
            {
               MongoDBFieldFunctionAdapterInterface adapter = backend.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
               if(adapter != null)
               {
                  String fieldReference = getFieldReference(table, fieldFunction.getFieldName());
                  addFieldsDocument.append(virtualField.getName(), adapter.getExpression(fieldReference, fieldFunction, fieldName -> getFieldReference(table, fieldName)));
               }
            }
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////
      // handle criteria-level fieldFunction on non-virtual fields (inline computed fields) //
      /////////////////////////////////////////////////////////////////////////////////////
      addFieldFunctionFieldsFromFilter(addFieldsDocument, table, backend, filter);

      return addFieldsDocument;
   }



   /*******************************************************************************
    ** Keep computed predicates separate from stored fields, including READ locks.
    ** Restore the complete original document before later query/aggregate stages.
    *******************************************************************************/
   protected List<Bson> makeFilterPipeline(QTableMetaData table, MongoDBBackendMetaData backend, QQueryFilter filter) throws QException
   {
      QQueryFilter effectiveFilter = new QQueryFilter();
      if(filter != null && filter.hasAnyCriteria())
      {
         effectiveFilter.addSubFilter(filter);
      }
      QQueryFilter securityFilter = makeSecurityQueryFilter(table);
      if(securityFilter.hasAnyCriteria())
      {
         effectiveFilter.addSubFilter(securityFilter);
      }

      Map<QFilterCriteria, String> computedNames = new IdentityHashMap<>();
      Document wrapper = new Document("source", "$$ROOT");
      addRequiredFilterFields(wrapper, computedNames, table, backend, effectiveFilter);

      List<Bson> pipeline = new ArrayList<>();
      Boolean hasComputedFields = !computedNames.isEmpty();
      if(hasComputedFields)
      {
         pipeline.add(new Document("$replaceRoot", new Document("newRoot", wrapper)));
      }
      Bson match = makeSearchQueryDocumentWithoutSecurity(table, effectiveFilter, computedNames, hasComputedFields ? "source." : "");
      if(!match.toBsonDocument().isEmpty())
      {
         pipeline.add(Aggregates.match(match));
      }
      if(hasComputedFields)
      {
         pipeline.add(new Document("$replaceRoot", new Document("newRoot", "$source")));
      }
      return pipeline;
   }



   /*******************************************************************************
    ** Evaluate only actual predicate expressions. Each criterion gets its own
    ** private alias, so different arguments and native field names cannot collide.
    *******************************************************************************/
   private void addRequiredFilterFields(Document wrapper, Map<QFilterCriteria, String> computedNames, QTableMetaData table, MongoDBBackendMetaData backend, QQueryFilter filter) throws QException
   {
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         if(computedNames.containsKey(criteria))
         {
            continue;
         }
         QFieldMetaData field = table.getFieldOrVirtualField(criteria.getFieldName());
         FieldFunction fieldFunction = criteria.getFieldFunction();
         String sourceFieldName;
         if(field instanceof QVirtualFieldMetaData virtualField)
         {
            if(fieldFunction != null)
            {
               throw (new QException("Inline field functions on virtual fields are not supported for MongoDB tables"));
            }
            fieldFunction = virtualField.getFieldFunction();
            sourceFieldName = fieldFunction == null ? null : fieldFunction.getFieldName();
         }
         else if(fieldFunction != null)
         {
            sourceFieldName = criteria.getFieldName();
         }
         else
         {
            continue;
         }

         MongoDBFieldFunctionAdapterInterface adapter = requireFieldFunctionAdapter(backend, fieldFunction);
         String computedName = "f" + computedNames.size();
         wrapper.append(computedName, adapter.getExpression(getFieldReference(table, sourceFieldName), fieldFunction, fn -> getFieldReference(table, fn)));
         computedNames.put(criteria, computedName);
      }
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         addRequiredFilterFields(wrapper, computedNames, table, backend, subFilter);
      }
   }



   /*******************************************************************************
    ** Native computation requires both a function and its MongoDB adapter.
    *******************************************************************************/
   protected MongoDBFieldFunctionAdapterInterface requireFieldFunctionAdapter(MongoDBBackendMetaData backend, FieldFunction fieldFunction) throws QException
   {
      if(fieldFunction == null || fieldFunction.getFunctionTypeIdentifier() == null)
      {
         throw (new QException("Required virtual field has no native field function"));
      }
      MongoDBFieldFunctionAdapterInterface adapter = backend.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
      if(adapter == null)
      {
         throw (new QException("Required field function has no MongoDB adapter"));
      }
      return adapter;
   }



   /*******************************************************************************
    ** Recursively scan a filter for criteria that have a fieldFunction,
    ** and add corresponding computed fields to the addFields document.
    *******************************************************************************/
   private void addFieldFunctionFieldsFromFilter(Document addFieldsDocument, QTableMetaData table, MongoDBBackendMetaData backend, QQueryFilter filter)
   {
      if(filter == null)
      {
         return;
      }

      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         if(criteria.getFieldFunction() != null)
         {
            FieldFunction fieldFunction = criteria.getFieldFunction();
            MongoDBFieldFunctionAdapterInterface adapter = backend.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
            if(adapter != null)
            {
               String computedFieldName = criteria.getFieldName() + "_" + fieldFunction.getFunctionTypeIdentifierName();
               String fieldReference    = getFieldReference(table, criteria.getFieldName());
               addFieldsDocument.append(computedFieldName, adapter.getExpression(fieldReference, fieldFunction, fieldName -> getFieldReference(table, fieldName)));
            }
         }
      }

      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         addFieldFunctionFieldsFromFilter(addFieldsDocument, table, backend, subFilter);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected int getPageSize()
   {
      return (1000);
   }



   /*******************************************************************************
    ** Convert a mongodb document to a QRecord.
    *******************************************************************************/
   protected QRecord documentToRecord(QueryInput queryInput, Document document)
   {
      QTableMetaData table  = queryInput.getTable();
      QRecord        record = new QRecord();

      record.setTableName(table.getName());

      /////////////////////////////////////////////
      // build the set of field names to include //
      /////////////////////////////////////////////
      Set<String> fieldNamesToInclude = queryInput.getFieldNamesToInclude();
      List<QFieldMetaData> selectedFields = table.getFields().values()
         .stream().filter(field -> fieldNamesToInclude == null || fieldNamesToInclude.contains(field.getName()))
         .toList();

      //////////////////////////////////////////////////////////////////////////////////////////////
      // first iterate over the table's fields, looking for them (at their backend name (path,    //
      // if it has dots) inside the document note that we'll remove values from the document      //
      // as we go - then after this loop, will handle all remaining values as unstructured fields //
      //////////////////////////////////////////////////////////////////////////////////////////////
      Map<String, Serializable> values = record.getValues();
      for(QFieldMetaData field : selectedFields)
      {
         String fieldName        = field.getName();
         String fieldBackendName = getFieldBackendName(field);

         if(fieldBackendName.contains("."))
         {
            /////////////////////////////////////////////////////////////
            // process backend-names with dots as hierarchical objects //
            /////////////////////////////////////////////////////////////
            String[] parts       = fieldBackendName.split("\\.");
            Document tmpDocument = document;
            for(int i = 0; i < parts.length - 1; i++)
            {
               Object nestedValue = tmpDocument.get(parts[i]);
               if(nestedValue instanceof Document subDocument)
               {
                  tmpDocument = subDocument;
               }
               else
               {
                  if(nestedValue != null)
                  {
                     LOG.warn("Unexpected - In table [" + table.getName() + "] found a non-document at sub-key [" + parts[i] + "] for field [" + field.getName() + "]");
                  }
                  ///////////////////////////////////////////////////////////////////////////
                  // A missing path must not consume a same-named leaf from an ancestor.   //
                  ///////////////////////////////////////////////////////////////////////////
                  tmpDocument = null;
                  break;
               }
            }

            Object value = tmpDocument == null ? null : tmpDocument.remove(parts[parts.length - 1]);
            setValue(values, fieldName, value);
         }
         else
         {
            Object value = document.remove(fieldBackendName);
            setValue(values, fieldName, value);
         }
      }

      ////////////////////////////////////////////////////////////
      // extract virtual field values computed by $addFields  //
      ////////////////////////////////////////////////////////////
      for(QVirtualFieldMetaData virtualField : CollectionUtils.nonNullMap(table.getVirtualFields()).values())
      {
         if(virtualField.getIsQuerySelectable())
         {
            Object value = document.remove(virtualField.getName());
            setValue(values, virtualField.getName(), value);
         }
      }

      //////////////////////////////////////////////////////////////
      // handle remaining values in the document as un-structured //
      //////////////////////////////////////////////////////////////
      for(String subFieldName : document.keySet())
      {
         Object subValue = document.get(subFieldName);
         setValue(values, subFieldName, subValue);
      }

      return (record);
   }



   /*******************************************************************************
    ** Recursive helper method to put a value in a map - where mongodb documents
    ** are recursively expanded, and types are mapped to QQQ expectations.
    *******************************************************************************/
   private void setValue(Map<String, Serializable> values, String fieldName, Object value)
   {
      if(value instanceof ObjectId objectId)
      {
         values.put(fieldName, objectId.toString());
      }
      else if(value instanceof java.util.Date date)
      {
         values.put(fieldName, date.toInstant());
      }
      else if(value instanceof Document document)
      {
         LinkedHashMap<String, Serializable> subValues = new LinkedHashMap<>();
         values.put(fieldName, subValues);

         for(String subFieldName : document.keySet())
         {
            Object subValue = document.get(subFieldName);
            setValue(subValues, subFieldName, subValue);
         }
      }
      else if(value instanceof Serializable s)
      {
         values.put(fieldName, s);
      }
      else if(value != null)
      {
         values.put(fieldName, String.valueOf(value));
      }
      else
      {
         values.put(fieldName, null);
      }
   }



   /*******************************************************************************
    ** Convert a QRecord to a mongodb document.
    *******************************************************************************/
   protected Document recordToDocument(QTableMetaData table, QRecord record) throws QException
   {
      Document document = new Document();

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // first iterate over fields defined in the table - put them in the document for mongo first. //
      // track the names that we've processed in a set. then later we'll go over all values in the  //
      // record and send them all to mongo (skipping ones we knew about from the table definition)  //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      Set<String> processedFields = new HashSet<>();

      for(QFieldMetaData field : table.getFields().values())
      {
         Serializable value = record.getValue(field.getName());
         processedFields.add(field.getName());

         if(field.getName().equals(table.getPrimaryKeyField()) && value == null)
         {
            ////////////////////////////////////
            // let mongodb client generate id //
            ////////////////////////////////////
            continue;
         }

         String fieldBackendName = getFieldBackendName(field);
         if(fieldBackendName.contains("."))
         {
            /////////////////////////////////////////////////////////////
            // process backend-names with dots as hierarchical objects //
            /////////////////////////////////////////////////////////////
            String[] parts       = fieldBackendName.split("\\.");
            Document tmpDocument = document;
            for(int i = 0; i < parts.length - 1; i++)
            {
               if(!tmpDocument.containsKey(parts[i]))
               {
                  Document subDocument = new Document();
                  tmpDocument.put(parts[i], subDocument);
                  tmpDocument = subDocument;
               }
               else
               {
                  if(tmpDocument.get(parts[i]) instanceof Document subDocument)
                  {
                     tmpDocument = subDocument;
                  }
                  else
                  {
                     throw (new QException("Fields in table [" + table.getName() + "] specify both a sub-object and a field at the key: " + parts[i]));
                  }
               }
            }
            tmpDocument.append(parts[parts.length - 1], value);
         }
         else
         {
            document.append(fieldBackendName, value);
         }
      }

      /////////////////////////
      // do remaining values //
      /////////////////////////
      for(Map.Entry<String, Serializable> entry : record.getValues().entrySet())
      {
         if(!processedFields.contains(entry.getKey()))
         {
            document.append(entry.getKey(), entry.getValue());
         }
      }

      return (document);
   }



   /*******************************************************************************
    ** Convert QQueryFilter to Bson search query document - including security
    ** for the table if needed.
    *******************************************************************************/
   protected Bson makeSearchQueryDocument(QTableMetaData table, QQueryFilter filter) throws QException
   {
      Bson         searchQueryWithoutSecurity = makeSearchQueryDocumentWithoutSecurity(table, filter);
      QQueryFilter securityFilter             = makeSecurityQueryFilter(table);
      if(!securityFilter.hasAnyCriteria())
      {
         return (searchQueryWithoutSecurity);
      }

      Bson searchQueryForSecurity = makeSearchQueryDocumentWithoutSecurity(table, securityFilter);

      if(searchQueryWithoutSecurity.toBsonDocument().isEmpty())
      {
         return (searchQueryForSecurity);
      }
      else
      {
         return (Filters.and(searchQueryWithoutSecurity, searchQueryForSecurity));
      }
   }



   /*******************************************************************************
    ** Build a filter from the READ portion of the table's security-lock tree.
    *******************************************************************************/
   private QQueryFilter makeSecurityQueryFilter(QTableMetaData table) throws QException
   {
      MultiRecordSecurityLock locks = RecordSecurityLockFilters.filterForReadLockTree(CollectionUtils.nonNullList(table.getRecordSecurityLocks()));
      return makeFilterForRecordSecurityLock(QContext.getQInstance(), QContext.getQSession(), table, locks);
   }



   /*******************************************************************************
    ** An empty filter represents true. Preserve that identity within an OR even
    ** when an all-access leaf contributes no criteria; AND can omit true children.
    *******************************************************************************/
   private static QQueryFilter makeFilterForRecordSecurityLock(QInstance instance, QSession session, QTableMetaData table, RecordSecurityLock recordSecurityLock) throws QException
   {
      if(recordSecurityLock instanceof MultiRecordSecurityLock multiRecordSecurityLock)
      {
         QQueryFilter securityFilter = new QQueryFilter();
         securityFilter.setBooleanOperator(multiRecordSecurityLock.getOperator().toFilterOperator());
         boolean hasTrueBranch = false;
         for(RecordSecurityLock child : CollectionUtils.nonNullList(multiRecordSecurityLock.getLocks()))
         {
            QQueryFilter childFilter = makeFilterForRecordSecurityLock(instance, session, table, child);
            if(childFilter.hasAnyCriteria())
            {
               securityFilter.addSubFilter(childFilter);
            }
            else
            {
               hasTrueBranch = true;
            }
         }
         if(hasTrueBranch && QQueryFilter.BooleanOperator.OR.equals(securityFilter.getBooleanOperator()))
         {
            return new QQueryFilter();
         }
         return securityFilter;
      }

      QSecurityKeyType securityKeyType = instance.getSecurityKeyType(recordSecurityLock.getSecurityKeyType());
      if(StringUtils.hasContent(securityKeyType.getAllAccessKeyName())
         && session.hasSecurityKeyValue(securityKeyType.getAllAccessKeyName(), true, QFieldType.BOOLEAN))
      {
         return new QQueryFilter();
      }

      String fieldName = recordSecurityLock.getFieldName();
      if(CollectionUtils.nullSafeHasContents(recordSecurityLock.getJoinNameChain()))
      {
         throw (new QException("Security locks in mongodb with joinNameChain is not yet supported"));
      }

      QQueryFilter          lockFilter   = new QQueryFilter();
      List<QFilterCriteria> lockCriteria = new ArrayList<>();
      lockFilter.setCriteria(lockCriteria);

      QFieldType type = QFieldType.INTEGER;
      try
      {
         type = table.getField(fieldName).getType();
      }
      catch(Exception e)
      {
         LOG.debug("Error getting field type...  Trying Integer", e);
      }

      List<Serializable> securityKeyValues = session.getSecurityKeyValues(recordSecurityLock.getSecurityKeyType(), type);
      securityKeyValues.removeIf(value -> value == null);
      if(CollectionUtils.nullSafeIsEmpty(securityKeyValues))
      {
         if(RecordSecurityLock.NullValueBehavior.ALLOW.equals(NullValueBehaviorUtil.getEffectiveNullValueBehavior(recordSecurityLock)))
         {
            lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.IS_BLANK));
         }
         else
         {
            lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.IN, Collections.emptyList()));
         }
      }
      else
      {
         if(RecordSecurityLock.NullValueBehavior.ALLOW.equals(NullValueBehaviorUtil.getEffectiveNullValueBehavior(recordSecurityLock)))
         {
            lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.IS_NULL_OR_IN, securityKeyValues));
         }
         else
         {
            lockCriteria.add(new QFilterCriteria(fieldName, QCriteriaOperator.IN, securityKeyValues));
         }
      }

      return lockFilter;
   }



   /*******************************************************************************
    ** w/o considering security, just map a QQueryFilter to a Bson searchQuery.
    *******************************************************************************/
   private Bson makeSearchQueryDocumentWithoutSecurity(QTableMetaData table, QQueryFilter filter)
   {
      return makeSearchQueryDocumentWithoutSecurity(table, filter, Map.of(), "");
   }



   /*******************************************************************************
    ** Resolve private computed aliases or physical fields within the source root.
    *******************************************************************************/
   private Bson makeSearchQueryDocumentWithoutSecurity(QTableMetaData table, QQueryFilter filter, Map<QFilterCriteria, String> computedNames, String physicalPrefix)
   {
      if(filter == null || !filter.hasAnyCriteria())
      {
         return (new Document());
      }

      List<Bson> criteriaFilters = new ArrayList<>();

      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         List<Serializable> values = criteria.getValues() == null ? new ArrayList<>() : new ArrayList<>(criteria.getValues());
         QFieldMetaData     field  = table.getFieldOrVirtualField(criteria.getFieldName());

         ///////////////////////////////////////////////////////////////////////////////
         // Read pipelines supply private aliases. Retain legacy names for callers of //
         // the protected document-only filter helper, including native Delete.       //
         ///////////////////////////////////////////////////////////////////////////////
         String computedName = computedNames.get(criteria);
         String fieldBackendName;
         if(computedName != null)
         {
            fieldBackendName = computedName;
         }
         else if(field instanceof QVirtualFieldMetaData)
         {
            fieldBackendName = physicalPrefix + field.getName();
         }
         else if(criteria.getFieldFunction() != null)
         {
            fieldBackendName = physicalPrefix + criteria.getFieldName() + "_" + criteria.getFieldFunction().getFunctionTypeIdentifierName();
         }
         else
         {
            fieldBackendName = physicalPrefix + getFieldBackendName(field);
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // replace any expression-type values with their evaluation                                                                         //
         // also, "scrub" non-expression values, which type-converts them (e.g., strings in various supported date formats become LocalDate) //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         ListIterator<Serializable> valueListIterator = values.listIterator();
         while(valueListIterator.hasNext())
         {
            Serializable value = valueListIterator.next();
            if(value instanceof AbstractFilterExpression<?> expression)
            {
               try
               {
                  valueListIterator.set(expression.evaluate(field));
               }
               catch(QException qe)
               {
                  LOG.warn("Unexpected exception caught evaluating expression", qe);
               }
            }
            /*
            todo - is this needed??
            else
            {
               Serializable scrubbedValue = scrubValue(field, value);
               valueListIterator.set(scrubbedValue);
            }
            */
         }

         /////////////////////////////////////////////////////////////////////////////////////////
         // make sure any values we're going to run against the primary key (_id) are ObjectIds //
         /////////////////////////////////////////////////////////////////////////////////////////
         if(computedName == null && field.getName().equals(table.getPrimaryKeyField()))
         {
            ListIterator<Serializable> iterator = values.listIterator();
            while(iterator.hasNext())
            {
               Serializable value = iterator.next();
               iterator.set(new ObjectId(String.valueOf(value)));
            }
         }

         ////////
         // :( //
         ////////
         if(StringUtils.hasContent(criteria.getOtherFieldName()))
         {
            throw (new IllegalArgumentException("A mongodb query with an 'otherFieldName' specified is not currently supported."));
         }

         criteriaFilters.add(switch(criteria.getOperator())
         {
            case EQUALS -> Filters.eq(fieldBackendName, getValue(values, 0));

            case NOT_EQUALS -> Filters.and(
               Filters.ne(fieldBackendName, getValue(values, 0)),

               ////////////////////////////////////////////////////////////////////////////////////////////
               // to match RDBMS and other QQQ backends, consider a null to not match a not-equals query //
               ////////////////////////////////////////////////////////////////////////////////////////////
               Filters.not(Filters.eq(fieldBackendName, null))
            );

            case NOT_EQUALS_OR_IS_NULL -> Filters.or(
               Filters.eq(fieldBackendName, null),
               Filters.ne(fieldBackendName, getValue(values, 0))
            );
            case IN -> filterIn(fieldBackendName, values);
            case NOT_IN -> Filters.nor(filterIn(fieldBackendName, values));
            case IS_NULL_OR_IN -> Filters.or(
               Filters.eq(fieldBackendName, null),
               filterIn(fieldBackendName, values)
            );
            case LIKE -> filterRegex(fieldBackendName, null, ValueUtils.getValueAsString(getValue(values, 0)).replaceAll("%", ".*"), null);
            case NOT_LIKE -> Filters.nor(filterRegex(fieldBackendName, null, ValueUtils.getValueAsString(getValue(values, 0)).replaceAll("%", ".*"), null));
            case STARTS_WITH -> filterRegex(fieldBackendName, null, getValue(values, 0), ".*");
            case ENDS_WITH -> filterRegex(fieldBackendName, ".*", getValue(values, 0), null);
            case CONTAINS -> filterRegex(fieldBackendName, ".*", getValue(values, 0), ".*");
            case NOT_STARTS_WITH -> Filters.nor(filterRegex(fieldBackendName, null, getValue(values, 0), ".*"));
            case NOT_ENDS_WITH -> Filters.nor(filterRegex(fieldBackendName, ".*", getValue(values, 0), null));
            case NOT_CONTAINS -> Filters.nor(filterRegex(fieldBackendName, ".*", getValue(values, 0), ".*"));
            case LESS_THAN -> Filters.lt(fieldBackendName, getValue(values, 0));
            case LESS_THAN_OR_EQUALS -> Filters.lte(fieldBackendName, getValue(values, 0));
            case GREATER_THAN -> Filters.gt(fieldBackendName, getValue(values, 0));
            case GREATER_THAN_OR_EQUALS -> Filters.gte(fieldBackendName, getValue(values, 0));
            case IS_BLANK -> filterIsBlank(fieldBackendName);
            case IS_NOT_BLANK -> Filters.nor(filterIsBlank(fieldBackendName));
            case BETWEEN -> filterBetween(fieldBackendName, values);
            case NOT_BETWEEN -> Filters.nor(filterBetween(fieldBackendName, values));
            case TRUE -> Filters.or(Filters.eq(fieldBackendName, "true"), Filters.ne(fieldBackendName, "true"), Filters.eq(fieldBackendName, null)); // todo test!!
            case FALSE -> Filters.and(Filters.eq(fieldBackendName, "true"), Filters.ne(fieldBackendName, "true"), Filters.eq(fieldBackendName, null));
         });
      }

      /////////////////////////////////////
      // recursively process sub-filters //
      /////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(filter.getSubFilters()))
      {
         for(QQueryFilter subFilter : filter.getSubFilters())
         {
            criteriaFilters.add(makeSearchQueryDocumentWithoutSecurity(table, subFilter, computedNames, physicalPrefix));
         }
      }

      Bson bson = QQueryFilter.BooleanOperator.AND.equals(filter.getBooleanOperator()) ? Filters.and(criteriaFilters) : Filters.or(criteriaFilters);
      return bson;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Serializable getValue(List<Serializable> values, int i)
   {
      if(values == null || values.size() <= i)
      {
         throw new IllegalArgumentException("Incorrect number of values given for criteria");
      }

      return (values.get(i));
   }



   /*******************************************************************************
    ** build a bson filter doing a regex (e.g., for LIKE, STARTS_WITH, etc)
    *******************************************************************************/
   private Bson filterRegex(String fieldBackendName, String prefix, Serializable mainRegex, String suffix)
   {
      if(prefix == null)
      {
         prefix = "";
      }

      if(suffix == null)
      {
         suffix = "";
      }

      String fullRegex = prefix + ValueUtils.getValueAsString(mainRegex + suffix);
      return (Filters.regex(fieldBackendName, Pattern.compile(fullRegex)));
   }



   /*******************************************************************************
    ** build a bson filter doing IN
    *******************************************************************************/
   private static Bson filterIn(String fieldBackendName, List<Serializable> values)
   {
      return Filters.in(fieldBackendName, values);
   }



   /*******************************************************************************
    ** build a bson filter doing BETWEEN
    *******************************************************************************/
   private static Bson filterBetween(String fieldBackendName, List<Serializable> values)
   {
      return Filters.and(
         Filters.gte(fieldBackendName, getValue(values, 0)),
         Filters.lte(fieldBackendName, getValue(values, 1))
      );
   }



   /*******************************************************************************
    ** build a bson filter doing BLANK (null or == "")
    *******************************************************************************/
   private static Bson filterIsBlank(String fieldBackendName)
   {
      return Filters.or(
         Filters.eq(fieldBackendName, null),
         Filters.eq(fieldBackendName, "")
      );
   }



   /*******************************************************************************
    ** Getter for queryStat
    *******************************************************************************/
   public QueryStat getQueryStat()
   {
      return (this.queryStat);
   }



   /*******************************************************************************
    ** Setter for queryStat
    *******************************************************************************/
   public void setQueryStat(QueryStat queryStat)
   {
      this.queryStat = queryStat;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void setQueryInQueryStat(Bson query)
   {
      if(queryStat != null && query != null)
      {
         queryStat.setQueryText(query.toString());

         ////////////////////////////////////////////////////////////////
         // todo - if we support joins in the future, do them here too //
         ////////////////////////////////////////////////////////////////
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void logQuery(String tableName, String actionName, List<Bson> query, Long queryStartTime)
   {

      if(System.getProperty("qqq.mongodb.logQueries", "false").equals("true"))
      {
         try
         {
            if(System.getProperty("qqq.mongodb.logQueries.output", "logger").equalsIgnoreCase("system.out"))
            {
               System.out.println("Table: " + tableName + ", Action: " + actionName + ", Query: " + query);

               if(queryStartTime != null)
               {
                  System.out.println("Query Took [" + QValueFormatter.formatValue(DisplayFormat.COMMAS, (System.currentTimeMillis() - queryStartTime)) + "] ms");
               }
            }
            else
            {
               LOG.debug("Running Query", logPair("table", tableName), logPair("action", actionName), logPair("query", query), logPair("millis", queryStartTime == null ? null : (System.currentTimeMillis() - queryStartTime)));
            }
         }
         catch(Exception e)
         {
            LOG.debug("Error logging query...", e);
         }
      }
   }

}
