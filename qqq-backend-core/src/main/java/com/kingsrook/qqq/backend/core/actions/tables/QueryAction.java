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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.reporting.BufferedRecordPipe;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipeBufferedWrapper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.FilterValidationHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryActionCacheHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.SelectionValidationHelper;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Action to run a query against a table.
 **
 *******************************************************************************/
public class QueryAction
{
   private static final QLogger LOG = QLogger.getLogger(QueryAction.class);

   private Optional<TableCustomizerInterface> postQueryRecordCustomizer;

   private QueryInput               queryInput;
   private QueryInterface           queryInterface;
   private QPossibleValueTranslator qPossibleValueTranslator;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutput execute(QueryInput queryInput) throws QException
   {
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
      table = TableMetaDataPersonalizerAction.execute(queryInput);
      queryInput.setTableMetaData(table);

      validateFieldNamesToInclude(queryInput);
      FilterValidationHelper.validateFieldNamesInFilter(queryInput);

      QBackendMetaData backend = queryInput.getBackend();
      postQueryRecordCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.POST_QUERY_RECORD.getRole());
      this.queryInput = queryInput;

      if(queryInput.getRecordPipe() != null)
      {
         queryInput.getRecordPipe().setPostRecordActions(this::postRecordActions);

         if(queryInput.getIncludeAssociations())
         {
            //////////////////////////////////////////////////////////////////////////////////////////
            // if the user requested to include associations, it's important that that is buffered, //
            // (for performance reasons), so, wrap the user's pipe with a buffer                    //
            //////////////////////////////////////////////////////////////////////////////////////////
            queryInput.setRecordPipe(new RecordPipeBufferedWrapper(queryInput.getRecordPipe()));
         }
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // apply any available field behaviors to the filter (noting that, if anything changes, a new filter is returned) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      queryInput.setFilter(ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), table, queryInput.getFilter(), Collections.emptySet()));

      QueryStat queryStat = QueryStatManager.newQueryStat(backend, table, queryInput.getFilter(), QueryAction.class.getSimpleName());

      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(backend);

      queryInterface = qModule.getQueryInterface();
      queryInterface.setQueryStat(queryStat);
      QueryOutput queryOutput = queryInterface.execute(queryInput);

      if(queryStat != null)
      {
         setRecordCountInQueryStat(queryInput, queryOutput, queryStat);
         QueryStatManager.getInstance().add(queryStat);
      }

      ////////////////////////////
      // handle cache use-cases //
      ////////////////////////////
      if(table.getCacheOf() != null)
      {
         new QueryActionCacheHelper().handleCaching(queryInput, queryOutput);
      }

      if(queryInput.getRecordPipe() instanceof BufferedRecordPipe bufferedRecordPipe)
      {
         bufferedRecordPipe.finalFlush();
      }

      if(queryInput.getRecordPipe() == null)
      {
         postRecordActions(queryOutput.getRecords());
      }

      return queryOutput;
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
         recordCount = queryOutput.getRecords().size();
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
      QTableMetaData table = queryInput.getTable();
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         if(queryInput.getAssociationNamesToInclude() == null || queryInput.getAssociationNamesToInclude().contains(association.getName()))
         {
            // e.g., order -> orderLine
            QJoinMetaData join = QContext.getQInstance().getJoin(association.getJoinName()); // todo ... ever need to flip?
            // just assume this, at least for now... if(BooleanUtils.isTrue(association.getDoInserts()))

            QueryInput nextLevelQueryInput = new QueryInput();
            nextLevelQueryInput.setTableName(association.getAssociatedTableName());
            nextLevelQueryInput.setIncludeAssociations(true);
            nextLevelQueryInput.setAssociationNamesToInclude(buildNextLevelAssociationNamesToInclude(association.getName(), queryInput.getAssociationNamesToInclude()));
            nextLevelQueryInput.setTransaction(queryInput.getTransaction());

            QQueryFilter filter = new QQueryFilter();
            nextLevelQueryInput.setFilter(filter);

            ListingHash<List<Serializable>, QRecord> outerResultMap = new ListingHash<>();

            if(join.getJoinOns().size() == 1)
            {
               JoinOn            joinOn = join.getJoinOns().get(0);
               Set<Serializable> values = new HashSet<>();
               for(QRecord record : queryOutputRecords)
               {
                  Serializable value       = record.getValue(joinOn.getLeftField());
                  Serializable valueAsType = ValueUtils.getValueAsFieldType(table.getField(joinOn.getLeftField()).getType(), value);
                  values.add(valueAsType);
                  outerResultMap.add(List.of(valueAsType), record);
               }
               filter.addCriteria(new QFilterCriteria(joinOn.getRightField(), QCriteriaOperator.IN, new ArrayList<>(values)));
            }
            else
            {
               filter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);

               for(QRecord record : queryOutputRecords)
               {
                  QQueryFilter subFilter = new QQueryFilter();
                  filter.addSubFilter(subFilter);
                  List<Serializable> values = new ArrayList<>();
                  for(JoinOn joinOn : join.getJoinOns())
                  {
                     Serializable value = record.getValue(joinOn.getLeftField());
                     values.add(value);
                     subFilter.addCriteria(new QFilterCriteria(joinOn.getRightField(), QCriteriaOperator.EQUALS, value));
                  }
                  outerResultMap.add(values, record);
               }
            }

            QueryOutput nextLevelQueryOutput = new QueryAction().execute(nextLevelQueryInput);
            for(QRecord record : nextLevelQueryOutput.getRecords())
            {
               List<Serializable> values = new ArrayList<>();
               for(JoinOn joinOn : join.getJoinOns())
               {
                  Serializable value = record.getValue(joinOn.getRightField());
                  values.add(value);
               }

               if(outerResultMap.containsKey(values))
               {
                  for(QRecord outerRecord : outerResultMap.get(values))
                  {
                     outerRecord.withAssociatedRecord(association.getName(), record);
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
            rs.add(nextLevelCandidateName.replaceFirst(name + ".", ""));
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
         records = postQueryRecordCustomizer.get().postQuery(queryInput, records);
      }

      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.READ, QContext.getQInstance(), queryInput.getTable(), records, null);

      if(queryInput.getShouldTranslatePossibleValues())
      {
         if(qPossibleValueTranslator == null)
         {
            qPossibleValueTranslator = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession());
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
         Map<String, QFieldMetaData> fields = queryInput.getTable().getFields();
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
               record.removeValue(maskedFieldName);
               record.setValue(maskedFieldName, "************");
               if(queryInput.getShouldGenerateDisplayValues())
               {
                  record.setDisplayValue(maskedFieldName, record.getValueString(maskedFieldName));
               }
            }
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
}
