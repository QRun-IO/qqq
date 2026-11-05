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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordReferenceSecurityHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.reporting.BufferedRecordPipe;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipeBufferedWrapper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordUpdate;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociationJoin;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.CurrentUserFilterHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryActionCacheHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.SelectionValidationHelper;
import com.kingsrook.qqq.backend.core.actions.tables.query.QueryFilterPreprocessor;
import com.kingsrook.qqq.backend.core.actions.tables.query.VirtualFieldValueProviders;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.RawReadReason;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Action to run a query against a table.
 **
 *******************************************************************************/
public class QueryAction
{
   private int fieldAccessRecordCount;
   private String fieldAccessActionName = "query";

   private static final QLogger LOG = QLogger.getLogger(QueryAction.class);

   private Optional<TableCustomizerInterface> postQueryRecordCustomizer;
   private int associationDepth;

   private QueryInput               queryInput;
   private QueryInterface           queryInterface;
   private QPossibleValueTranslator qPossibleValueTranslator;
   private boolean preservePrimaryKeys;
   private final Set<String> providerIdentitiesToOmit = new HashSet<>();
   private boolean enforceProviderFieldAccess;
   private boolean enforcingPermissions = false;
   private final Set<Object> nativePrimaryKeys = new HashSet<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutput execute(QueryInput queryInput) throws QException
   {
      fieldAccessActionName = "query";
      return execute(queryInput, false);
   }



   /*******************************************************************************
    ** DML bookkeeping needs row identity independently of its public fields.
    *******************************************************************************/
   QueryOutput executeForDml(QueryInput queryInput) throws QException
   {
      if(queryInput.getRecordPipe() != null)
      {
         throw new QException("DML prefetch requires a materialized record list");
      }
      return execute(queryInput, true);
   }



   /*******************************************************************************
    ** Public invocations always restore ordinary Query behavior on reuse.
    *******************************************************************************/
   private QueryOutput execute(QueryInput queryInput, boolean preservePrimaryKeys) throws QException
   {
      if(queryInput.getRawReadReason() != null)
      {
         authorizeAndAuditRawRead(queryInput);
      }

      FieldAccessHelper.PendingExactMatch exactMatch = null;
      boolean enforce = false;
      if(queryInput.getTableName() != null && queryInput.getTable() != null)
      {
         CorePermissionEnforcer.checkTablePermissions(queryInput, TablePermissionSubType.READ);
         enforce = CorePermissionEnforcer.shouldEnforceFieldAccess(queryInput);
         if(enforce)
         {
            FieldAccessHelper.assertSelectedFieldsAllowed(queryInput.getTableName(), queryInput.getQueryJoins(), queryInput.getFieldNamesToInclude());
            exactMatch = CorePermissionEnforcer.checkReadCriteria(queryInput, queryInput.getQueryJoins(), queryInput.getFilter(), null);
         }
      }

      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         QueryOutput output = executeInAuthorizedScope(queryInput, preservePrimaryKeys, enforce, enforce);
         if(exactMatch != null)
         {
            exactMatch.complete(fieldAccessRecordCount, fieldAccessActionName);
         }
         return (output);
      }
   }



   /*******************************************************************************
    ** The owning GET already authorized the table and filters. Preserve its provider
    ** policy without repeating the final masking performed by that GET.
    *******************************************************************************/
   QueryOutput executeForGet(QueryInput input, boolean enforceProviders) throws QException
   {
      if(QContext.getAuthorizedActionScopeDepth() == 0)
      {
         throw new QException("A nested GET query requires an authorized action scope");
      }
      fieldAccessActionName = "get";
      return (executeInAuthorizedScope(input, false, false, enforceProviders));
   }



   /*******************************************************************************
    ** A raw read (see QueryInput.withRawRead) must pass its reason's permission
    ** check, is audited, and has its flags (re)applied.  This always applies, not
    ** only when core permission enforcement is turned on, since a raw read is an
    ** explicit opt-in to see more than a normal query does.  Record locks and
    ** field security locks are not touched here.
    *******************************************************************************/
   private void authorizeAndAuditRawRead(QueryInput queryInput) throws QException
   {
      QTableMetaData table = queryInput.getTable();
      if(table == null)
      {
         throw (new QException("A table named [" + queryInput.getTableName() + "] was not found in the active QInstance"));
      }

      RawReadReason rawReadReason = queryInput.getRawReadReason();
      rawReadReason.checkPermission(queryInput, table);
      queryInput.applyRawReadFlags();

      String userName = QContext.getQSession() == null || QContext.getQSession().getUser() == null ? null : QContext.getQSession().getUser().getIdReference();
      LOG.info("Raw read of table", logPair("tableName", table.getName()), logPair("reason", rawReadReason.getReason()), logPair("user", userName));
      CorePermissionEnforcer.runTrusted("audit of a raw read", () ->
      {
         AuditAction.execute(table.getName(), null, null, "Raw read of table (hidden, password, and heavy fields included): " + rawReadReason.getReason());
         return (null);
      });
   }



   /*******************************************************************************
    ** The query itself - after any permission enforcement owed to the caller.
    *******************************************************************************/
   private QueryOutput executeInAuthorizedScope(QueryInput queryInput, boolean preservePrimaryKeys, boolean enforce, boolean enforceProviders) throws QException
   {
      this.preservePrimaryKeys = preservePrimaryKeys;
      this.providerIdentitiesToOmit.clear();
      this.enforceProviderFieldAccess = enforceProviders;
      this.enforcingPermissions = enforce;
      this.fieldAccessRecordCount = 0;
      nativePrimaryKeys.clear();
      ActionHelper.validateSession(queryInput);

      if(queryInput.getTableName() == null)
      {
         throw (new QException("Table name was not specified in query input"));
      }

      QTableMetaData table = queryInput.getTable();
      if(table == null)
      {
         throw (new QException("A table named [" + queryInput.getTableName() + "] was not found in the active QInstance"));
      }
      table = TableMetaDataPersonalizerAction.execute(queryInput, enforce);
      if(table == null)
      {
         throw (new QException("Query table is not available"));
      }
      queryInput.setTableMetaData(table);
      queryInput.setFilter(RecordReferenceSecurityHelper.secureFilter(table, queryInput.getFilter(), queryInput.getTransaction()));

      QueryFilterPreprocessor.prepare(queryInput);
      validateFieldNamesToInclude(queryInput);
      QBackendMetaData backend = queryInput.getBackend();
      postQueryRecordCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.POST_QUERY_RECORD.getRole());
      this.queryInput = queryInput;

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // apply any available field behaviors to the filter (noting that, if anything changes, a new filter is returned) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      queryInput.setFilter(CurrentUserFilterHelper.resolve(queryInput, queryInput.getFilter()));
      queryInput.setFilter(ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), table, queryInput.getFilter(), Collections.emptySet()));

      QueryStat queryStat = QueryStatManager.newQueryStat(backend, table, queryInput.getFilter(), QueryAction.class.getSimpleName());

      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(backend);

      queryInterface = qModule.getQueryInterface();
      queryInterface.setQueryStat(queryStat);
      QueryInput backendInput = prepareProviderProjection(queryInput);
      if(queryInput.getRecordPipe() != null)
      {
         queryInput.getRecordPipe().setPostRecordActions(this::postRecordActions);
         if(!(queryInput.getRecordPipe() instanceof BufferedRecordPipe)
            && (queryInput.getIncludeAssociations() || !VirtualFieldValueProviders.selected(queryInput, enforceProviderFieldAccess).isEmpty()))
         {
            try
            {
               if(backendInput == queryInput)
               {
                  backendInput = queryInput.clone();
               }
               backendInput.setRecordPipe(new RecordPipeBufferedWrapper(queryInput.getRecordPipe()));
            }
            catch(CloneNotSupportedException e)
            {
               throw new QException("Unable to prepare buffered query input", e);
            }
         }
      }
      QueryOutput queryOutput = preservePrimaryKeys ? executeDmlQuery(backendInput) : queryInterface.execute(backendInput);

      if(queryStat != null)
      {
         setRecordCountInQueryStat(backendInput, queryOutput, queryStat);
         QueryStatManager.getInstance().add(queryStat);
      }

      ////////////////////////////
      // handle cache use-cases //
      ////////////////////////////
      if(table.getCacheOf() != null)
      {
         new QueryActionCacheHelper().handleCaching(queryInput, queryOutput);
      }

      if(backendInput.getRecordPipe() instanceof BufferedRecordPipe bufferedRecordPipe)
      {
         bufferedRecordPipe.finalFlush();
      }

      if(queryInput.getRecordPipe() == null)
      {
         postRecordActions(queryOutput.getRecords());
      }

      return queryOutput;
   }



   /*******************************************************************************
    ** Fetch provider identity on a private input, leaving the caller's projection intact.
    ** DISTINCT cannot be evaluated correctly before a post-query value is available.
    *******************************************************************************/
   private QueryInput prepareProviderProjection(QueryInput input) throws QException
   {
      var providers = VirtualFieldValueProviders.selected(input, enforceProviderFieldAccess);
      if(providers.isEmpty())
      {
         return (input);
      }
      if(input.getSelectDistinct())
      {
         throw new QException("DISTINCT is not supported for post-query virtual-field values");
      }
      Set<String> selection = input.getFieldNamesToInclude() == null ? null : new HashSet<>(input.getFieldNamesToInclude());
      boolean fetchHeavyBaseKey = false;
      for(var provider : providers)
      {
         String primaryKey = provider.table().getPrimaryKeyField();
         if(primaryKey == null || provider.table().getField(primaryKey) == null)
         {
            throw new QException("Virtual-field value providers require a primary key");
         }
         String outputKey = provider.prefix() + primaryKey;
         boolean omitted = selection != null && !input.getFieldNamesToInclude().contains(outputKey);
         boolean heavy = provider.table().getField(primaryKey).getIsHeavy() && !input.getShouldFetchHeavyFields();
         if(heavy && !provider.prefix().isEmpty())
         {
            throw new QException("Joined virtual-field providers require fetching their heavy primary key");
         }
         if(omitted || heavy)
         {
            providerIdentitiesToOmit.add(outputKey);
            if(selection != null)
            {
               selection.add(outputKey);
            }
            fetchHeavyBaseKey |= heavy;
         }
      }
      if(providerIdentitiesToOmit.isEmpty())
      {
         return (input);
      }
      try
      {
         QueryInput backendInput = input.clone();
         backendInput.setFieldNamesToInclude(selection);
         if(fetchHeavyBaseKey)
         {
            QTableMetaData backendTable = input.getTable().clone();
            backendTable.getField(input.getTable().getPrimaryKeyField()).setIsHeavy(false);
            backendInput.setTableMetaData(backendTable);
         }
         return (backendInput);
      }
      catch(CloneNotSupportedException e)
      {
         throw new QException("Unable to prepare virtual-field projection", e);
      }
   }



   /*******************************************************************************
    ** Fetch a heavy key only for identity, leaving other field-loading choices
    ** and the original personalized metadata available to presentation code.
    *******************************************************************************/
   private QueryOutput executeDmlQuery(QueryInput input) throws QException
   {
      QTableMetaData table = input.getTable();
      String primaryKey = table.getPrimaryKeyField();
      boolean omitHeavyKey = table.getField(primaryKey).getIsHeavy() && !input.getShouldFetchHeavyFields();
      QueryOutput output;
      try
      {
         if(omitHeavyKey)
         {
            QTableMetaData nativeTable = table.clone();
            nativeTable.getField(primaryKey).setIsHeavy(false);
            input.setTableMetaData(nativeTable);
         }
         output = queryInterface.executeForDml(input);
      }
      finally
      {
         input.setTableMetaData(table);
      }
      for(QRecord record : CollectionUtils.nonNullList(output.getRecords()))
      {
         Serializable key = record.getValue(primaryKey);
         if(omitHeavyKey)
         {
            record.removeValue(primaryKey);
         }
         record.capturePrimaryKey(table, key);
         nativePrimaryKeys.add(AssociatedRecordUpdate.primaryKey(table, record));
      }
      return output;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static void setRecordCountInQueryStat(QueryInput queryInput, QueryOutput queryOutput, QueryStat queryStat)
   {
      if(queryStat == null)
      {
         return;
      }

      int recordCount;
      if(queryInput.getRecordPipe() != null)
      {
         recordCount = queryInput.getRecordPipe().getTotalRecordCount();
      }
      else
      {
         recordCount = queryOutput.getRecords() == null ? 0 : queryOutput.getRecords().size();
      }

      queryStat.setRecordCount(recordCount);
   }



   /***************************************************************************
    ** if QueryInput contains a set of FieldNamesToInclude, then validate that
    ** those are known field names in the table being queried, or a selected
    ** queryJoin.
    ***************************************************************************/
   static void validateFieldNamesToInclude(QueryInput queryInput) throws QException
   {
      Set<String> fieldNamesToInclude = queryInput.getFieldNamesToInclude();
      if(fieldNamesToInclude == null)
      {
         ////////////////////////////////
         // null set means select all. //
         ////////////////////////////////
         return;
      }

      if(fieldNamesToInclude.isEmpty())
      {
         /////////////////////////////////////
         // empty set, however, is an error //
         /////////////////////////////////////
         throw (new QException("An empty set of fieldNamesToInclude was given as queryInput, which is not allowed."));
      }

      List<String> unrecognizedFieldNames = SelectionValidationHelper.getUnrecognizedFieldNames(queryInput, fieldNamesToInclude);

      if(!unrecognizedFieldNames.isEmpty())
      {
         throw (new QException("QueryInput contained " + unrecognizedFieldNames.size() + " unrecognized field name" + StringUtils.plural(unrecognizedFieldNames) + ": " + StringUtils.join(",", unrecognizedFieldNames)));
      }
   }




   /*******************************************************************************
    ** shorthand way to call for the most common use-case, when you just want the
    ** entities to be returned, and you just want to pass in a table name and filter.
    *******************************************************************************/
   public static <T extends QRecordEntity> List<T> execute(String tableName, Class<T> entityClass, QQueryFilter filter) throws QException
   {
      QueryAction queryAction = new QueryAction();
      QueryInput  queryInput  = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(filter);
      QueryOutput queryOutput = queryAction.execute(queryInput);
      return (queryOutput.getRecordEntities(entityClass));
   }



   /*******************************************************************************
    ** shorthand way to call for the most common use-case, when you just want the
    ** records to be returned, and you just want to pass in a table name and filter.
    *******************************************************************************/
   public static List<QRecord> execute(String tableName, QQueryFilter filter) throws QException
   {
      QueryAction queryAction = new QueryAction();
      QueryInput  queryInput  = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(filter);
      QueryOutput queryOutput = queryAction.execute(queryInput);
      return (queryOutput.getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void manageAssociations(QueryInput queryInput, List<QRecord> queryOutputRecords) throws QException
   {
      if(associationDepth > 64 && !queryOutputRecords.isEmpty())
      {
         throw new QException("Associated records exceed the maximum depth of 64");
      }
      QTableMetaData table = queryInput.getTable();
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         if(queryInput.getAssociationNamesToInclude() == null || queryInput.getAssociationNamesToInclude().contains(association.getName()))
         {
            AssociationJoin join = AssociationJoin.resolve(table, association);

            QueryInput nextLevelQueryInput = new QueryInput();
            nextLevelQueryInput.setTableName(association.getAssociatedTableName());
            nextLevelQueryInput.setInputSource(queryInput.getInputSource());
            nextLevelQueryInput.setIncludeAssociations(!queryOutputRecords.isEmpty());
            nextLevelQueryInput.setShouldFetchHeavyFields(queryInput.getShouldFetchHeavyFields());
            nextLevelQueryInput.setShouldOmitHiddenFields(queryInput.getShouldOmitHiddenFields());
            nextLevelQueryInput.setShouldMaskPasswords(queryInput.getShouldMaskPasswords());
            nextLevelQueryInput.setShouldTranslatePossibleValues(queryInput.getShouldTranslatePossibleValues());
            nextLevelQueryInput.setShouldGenerateDisplayValues(queryInput.getShouldGenerateDisplayValues());
            nextLevelQueryInput.setAssociationNamesToInclude(buildNextLevelAssociationNamesToInclude(association.getName(), queryInput.getAssociationNamesToInclude()));
            nextLevelQueryInput.setTransaction(queryInput.getTransaction());

            QQueryFilter filter = new QQueryFilter();
            nextLevelQueryInput.setFilter(filter);

            ListingHash<List<Serializable>, QRecord> outerResultMap = new ListingHash<>();

            Set<Serializable> singleValues = new HashSet<>();
            filter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);
            for(QRecord record : queryOutputRecords)
            {
               if(join.getJoinOns().stream().anyMatch(joinOn -> !record.getValues().containsKey(joinOn.getLeftField())))
               {
                  throw new QException("Association parent relationship fields were not returned by the query");
               }
               List<Serializable> values = join.parentValues(record);
               if(values.contains(null))
               {
                  continue;
               }
               outerResultMap.add(values, record);
               if(join.getJoinOns().size() == 1)
               {
                  singleValues.add(values.get(0));
               }
               else
               {
                  QQueryFilter subFilter = new QQueryFilter();
                  for(int i = 0; i < join.getJoinOns().size(); i++)
                  {
                     subFilter.addCriteria(new QFilterCriteria(join.getJoinOns().get(i).getRightField(), QCriteriaOperator.EQUALS, Collections.singletonList(values.get(i))));
                  }
                  filter.addSubFilter(subFilter);
               }
            }
            if(join.getJoinOns().size() == 1 || outerResultMap.isEmpty())
            {
               ///////////////////////////////////////////////////////////////////////////////////
               // An empty tuple set must still run child authorization, using a no-match query. //
               // NULL components never mean membership in the set of unassigned child records. //
               ///////////////////////////////////////////////////////////////////////////////////
               filter.addCriteria(new QFilterCriteria(join.getJoinOns().get(0).getRightField(), QCriteriaOperator.IN, new ArrayList<>(singleValues)));
            }

            QueryAction nextLevelAction = new QueryAction();
            nextLevelAction.associationDepth = associationDepth + 1;
            QueryOutput nextLevelQueryOutput = nextLevelAction.execute(nextLevelQueryInput);
            ListingHash<List<Serializable>, QRecord> childResultMap = new ListingHash<>();
            for(QRecord record : nextLevelQueryOutput.getRecords())
            {
               if(join.getJoinOns().stream().anyMatch(joinOn -> !record.getValues().containsKey(joinOn.getRightField())))
               {
                  throw new QException("Association child relationship fields were not returned by the query");
               }
               childResultMap.add(join.childValues(record), record);
            }

            for(QRecord outerRecord : queryOutputRecords)
            {
               outerRecord.withAssociatedRecords(association.getName(), new ArrayList<>());
            }
            for(List<Serializable> values : outerResultMap.keySet())
            {
               if(childResultMap.containsKey(values))
               {
                  for(QRecord outerRecord : outerResultMap.get(values))
                  {
                     outerRecord.withAssociatedRecords(association.getName(), new ArrayList<>(childResultMap.get(values)));
                  }
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Collection<String> buildNextLevelAssociationNamesToInclude(String name, Collection<String> associationNamesToInclude)
   {
      if(associationNamesToInclude == null)
      {
         return (associationNamesToInclude);
      }

      Set<String> rs = new HashSet<>();
      for(String nextLevelCandidateName : associationNamesToInclude)
      {
         if(nextLevelCandidateName.startsWith(name + "."))
         {
            rs.add(nextLevelCandidateName.substring(name.length() + 1));
         }
      }

      return (rs);
   }



   /*******************************************************************************
    ** Run the necessary actions on a list of records (which must be a mutable list - e.g.,
    ** not one created via List.of()).  This may include setting display values,
    ** translating possible values, and running post-record customizations.
    *******************************************************************************/
   public void postRecordActions(List<QRecord> records) throws QException
   {
      if(this.postQueryRecordCustomizer.isPresent())
      {
         List<QRecord> customizedRecords = postQueryRecordCustomizer.get().postQuery(queryInput, records);
         if(customizedRecords == null)
         {
            throw new QException("Post-query customizer returned null records");
         }
         if(customizedRecords != records)
         {
            ///////////////////////////////////////////////////////////////////////
            // A replacement may be a view backed by the published record list. //
            ///////////////////////////////////////////////////////////////////////
            customizedRecords = new ArrayList<>(customizedRecords);
            records.clear();
            records.addAll(customizedRecords);
         }
      }

      VirtualFieldValueProviders.fill(queryInput, records, enforceProviderFieldAccess);

      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.READ, QContext.getQInstance(), queryInput.getTable(), records, null);

      if(queryInput.getShouldTranslatePossibleValues())
      {
         if(qPossibleValueTranslator == null)
         {
            qPossibleValueTranslator = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession()).withFieldAccessEnforcement(enforcingPermissions);
         }
         qPossibleValueTranslator.translatePossibleValuesInRecords(queryInput.getTable(), records, queryInput.getQueryJoins(), queryInput.getFieldsToTranslatePossibleValues());
      }

      if(queryInput.getShouldGenerateDisplayValues())
      {
         QValueFormatter.setDisplayValuesInRecords(queryInput.getTable(), records);
      }

      if(queryInput.getIncludeAssociations())
      {
         manageAssociations(queryInput, records);
      }

      Map<QRecord, Serializable> primaryKeys = preservePrimaryKeys ? new IdentityHashMap<>() : Collections.emptyMap();
      if(preservePrimaryKeys)
      {
         for(QRecord record : records)
         {
            if(!nativePrimaryKeys.contains(AssociatedRecordUpdate.primaryKey(queryInput.getTable(), record)))
            {
               throw new QException("DML prefetch customizer returned a record outside the native result");
            }
            primaryKeys.put(record, record.resolvePrimaryKey(queryInput.getTable()));
         }
      }

      ////////////////////////////////////////////////////////////////////////////
      // when enforcing permissions, remove values of fields (incl. joined and  //
      // associated tables' fields) that a FieldSecurityLock denies the session //
      ////////////////////////////////////////////////////////////////////////////
      if(enforcingPermissions)
      {
         fieldAccessRecordCount += records.size();
         FieldAccessHelper.applyToRecords(queryInput.getTableName(), queryInput.getQueryJoins(), records, fieldAccessActionName);
      }

      //////////////////////////////
      // mask any password fields //
      //////////////////////////////
      if(queryInput.getShouldOmitHiddenFields() || queryInput.getShouldMaskPasswords())
      {
         Set<String> maskedFields = new HashSet<>();
         Set<String> hiddenFields = new HashSet<>();

         //////////////////////////////////////////////////
         // build up sets of passwords and hidden fields //
         //////////////////////////////////////////////////
         Map<String, QFieldMetaData> fields = new HashMap<>(queryInput.getTable().getFields());
         fields.putAll(CollectionUtils.nonNullMap(queryInput.getTable().getVirtualFields()));
         for(QueryJoin queryJoin : CollectionUtils.nonNullList(queryInput.getQueryJoins()))
         {
            if(!queryJoin.getSelect())
            {
               continue;
            }
            QTableMetaData joinTable = QContext.getQInstance().getTable(queryJoin.getJoinTable());
            if(joinTable == null)
            {
               throw new QException("Requested join table [" + queryJoin.getJoinTable() + "] is not a defined table.");
            }
            joinTable = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput().withTableMetaData(joinTable).withInputSource(queryInput.getInputSource()));
            Map<String, QFieldMetaData> joinFields = new HashMap<>(joinTable.getFields());
            joinFields.putAll(CollectionUtils.nonNullMap(joinTable.getVirtualFields()));
            for(QFieldMetaData field : joinFields.values())
            {
               fields.put(queryJoin.getJoinTableOrItsAlias() + "." + field.getName(), field);
            }
         }
         for(String fieldName : fields.keySet())
         {
            QFieldMetaData field = fields.get(fieldName);
            if(queryInput.getShouldOmitHiddenFields() && field.getIsHidden())
            {
               hiddenFields.add(fieldName);
            }
            else if(queryInput.getShouldMaskPasswords() && field.getType() != null && field.getType().needsMasked() && !field.hasAdornmentType(AdornmentType.REVEAL))
            {
               maskedFields.add(fieldName);
            }
         }

         /////////////////////////////////////////////////////
         // iterate over records replacing values with mask //
         /////////////////////////////////////////////////////
         boolean privateLabel = hiddenFields.contains(queryInput.getTable().getPrimaryKeyField()) || maskedFields.contains(queryInput.getTable().getPrimaryKeyField())
            || CollectionUtils.nonNullList(queryInput.getTable().getRecordLabelFields()).stream().anyMatch(field -> hiddenFields.contains(field) || maskedFields.contains(field));
         for(QRecord record : records)
         {
            /////////////////////////
            // clear hidden fields //
            /////////////////////////
            for(String hiddenFieldName : hiddenFields)
            {
               record.removeValue(hiddenFieldName);
            }

            for(String maskedFieldName : maskedFields)
            {
               //////////////////////////////////////////////////////////////////////
               // empty out the value completely first (which will remove from     //
               // display fields as well) then update display value if flag is set //
               //////////////////////////////////////////////////////////////////////
               boolean hasValue = record.getValues().containsKey(maskedFieldName);
               record.removeValue(maskedFieldName);
               if(hasValue)
               {
                  record.setValue(maskedFieldName, "************");
                  if(queryInput.getShouldGenerateDisplayValues())
                  {
                     record.setDisplayValue(maskedFieldName, record.getValueString(maskedFieldName));
                  }
               }
            }
            if(privateLabel && record.getRecordLabel() != null)
            {
               ///////////////////////////////////////////////////////////////////////
               // A failed label format must not fall back to the old private label. //
               ///////////////////////////////////////////////////////////////////////
               record.setRecordLabel(null);
               record.setRecordLabel(QValueFormatter.formatRecordLabel(queryInput.getTable(), record));
            }
         }
      }
      if(!providerIdentitiesToOmit.isEmpty())
      {
         for(QRecord record : records)
         {
            for(String identity : providerIdentitiesToOmit)
            {
               record.removeValue(identity);
               if(record.getBackendDetails() != null
                  && record.getBackendDetails().get(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS) instanceof Map<?, ?> lengths)
               {
                  lengths.remove(identity);
               }
            }
            record.setRecordLabel(null);
         }
      }
      if(preservePrimaryKeys)
      {
         for(QRecord record : records)
         {
            record.capturePrimaryKey(queryInput.getTable(), primaryKeys.get(record));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void cancel()
   {
      if(queryInterface == null)
      {
         LOG.warn("queryInterface object was null when requested to cancel");
         return;
      }

      queryInterface.cancelAction();
   }



   /*******************************************************************************
    ** Execute with the owning egress action name, including export reveal suppression.
    *******************************************************************************/
   public QueryOutput execute(QueryInput input, String actionName) throws QException
   {
      fieldAccessActionName = actionName;
      return (execute(input, false));
   }
}
