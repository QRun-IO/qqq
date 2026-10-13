/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.query;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordReferenceSecurityHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.FilterValidationHelper;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QSubQuery;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Shared query/count/aggregate expansion and bounded sub-query execution.
 ** Works on filter copies so reusable filters never retain session-specific ids.
 *******************************************************************************/
public class QueryFilterPreprocessor
{
   private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
   private static int maxMaterialized = QSubQuery.MAX_MATERIALIZED;



   /*******************************************************************************
    ** Package-visible limit override for bounded materialization tests.
    *******************************************************************************/
   static void setMaxMaterialized(int limit)
   {
      maxMaterialized = limit;
   }



   /*******************************************************************************
    ** Expand before validation, then prepare each sub-query for the backend.
    *******************************************************************************/
   public static void prepare(QueryOrCountInputInterface input) throws QException
   {
      int depth = DEPTH.get();
      if(depth >= 32)
      {
         throw (new QUserFacingException("Sub-query nesting is too deep."));
      }
      DEPTH.set(depth + 1);
      try
      {
         if(input.getFilter() == null)
         {
            return;
         }
         CriteriaExpansionContext context = new CriteriaExpansionContext(QContext.getQInstance(), QContext.getQSession(), input.getTransaction());
         input.setFilter(expand(input.getTable(), input.getFilter(), context, true));
         FilterValidationHelper.validateFieldNamesInFilter(input);
         prepareSubQueries(input, input.getFilter());
      }
      finally
      {
         if(depth == 0)
         {
            DEPTH.remove();
         }
         else
         {
            DEPTH.set(depth);
         }
      }
   }



   /*******************************************************************************
    ** A replacement is nested in the parent's operator without flattening it.
    ** Reject an expander returning another expander criterion rather than looping.
    *******************************************************************************/
   private static QQueryFilter expand(QTableMetaData table, QQueryFilter source, CriteriaExpansionContext context, boolean allowExpansion) throws QException
   {
      if(source == null)
      {
         throw (new QUserFacingException("Query Filter contained a null subfilter"));
      }
      QQueryFilter result = new QQueryFilter()
         .withBooleanOperator(source.getBooleanOperator())
         .withLimit(source.getLimit()).withSkip(source.getSkip())
         .withSubFilterSetOperator(source.getSubFilterSetOperator());
      result.setOrderBys(source.getOrderBys() == null ? null : new ArrayList<>(source.getOrderBys().stream().map(orderBy -> orderBy == null ? null : orderBy.clone()).toList()));
      result.setCriteria(new ArrayList<>());
      result.setSubFilters(new ArrayList<>());
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(source.getCriteria()))
      {
         if(criteria == null || criteria.getFieldName() == null || criteria.getOperator() == null)
         {
            throw (new QUserFacingException("Query Filter criteria must specify a field name and operator"));
         }
         String fieldName = criteria.getFieldName();
         if(fieldName.startsWith(table.getName() + "."))
         {
            fieldName = fieldName.substring(table.getName().length() + 1);
         }
         QVirtualFieldMetaData field = CollectionUtils.nonNullMap(table.getVirtualFields()).get(fieldName);
         if(field != null && field.getCriteriaExpander() != null)
         {
            if(!allowExpansion)
            {
               throw (new QUserFacingException("Virtual field criteria expansion must return ordinary criteria."));
            }
            QQueryFilter replacement = QCodeLoader.getAdHoc(VirtualFieldCriteriaExpanderInterface.class, field.getCriteriaExpander()).expand(table, criteria.clone(), context);
            if(replacement == null)
            {
               throw (new QUserFacingException("Virtual field criteria expansion returned no filter."));
            }
            result.addSubFilter(expand(table, replacement, context, false));
         }
         else
         {
            result.addCriteria(criteria.clone());
         }
      }
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(source.getSubFilters()))
      {
         result.addSubFilter(expand(table, subFilter, context, allowExpansion));
      }
      return (result);
   }



   /*******************************************************************************
    ** Native sub-queries are authorized and personalized here; their backend uses
    ** JoinsContext to apply record locks. Other sub-queries run through QueryAction.
    *******************************************************************************/
   private static void prepareSubQueries(QueryOrCountInputInterface input, QQueryFilter filter) throws QException
   {
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         if(criteria.getValues() == null || criteria.getValues().size() != 1 || !(criteria.getValues().get(0) instanceof QSubQuery subQuery))
         {
            continue;
         }
         QueryInput inner = new QueryInput(subQuery.getTableName());
         inner.setInputSource(input.getInputSource());
         inner.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         inner.setFieldNamesToInclude(Set.of(subQuery.getFieldName()));
         inner.setFilter(subQuery.getFilter() == null ? new QQueryFilter() : subQuery.getFilter().clone());
         boolean sameBackend = Objects.equals(input.getTable().getBackendName(), inner.getTable().getBackendName());
         if(sameBackend)
         {
            inner.setTransaction(input.getTransaction());
         }
         QBackendMetaData backend = QContext.getQInstance().getBackend(input.getTable().getBackendName());
         if(sameBackend && new QBackendModuleDispatcher().getQBackendModule(backend).supportsSubQueries(backend))
         {
            CorePermissionEnforcer.checkTablePermissions(inner, TablePermissionSubType.READ);
            CorePermissionEnforcer.checkReadCriteria(inner, inner.getQueryJoins(), inner.getFilter(), inner.getFieldNamesToInclude());
            QTableMetaData table = TableMetaDataPersonalizerAction.execute(inner, true);
            if(table == null || !table.getFields().containsKey(subQuery.getFieldName()))
            {
               throw (new QUserFacingException("Sub-query selected field is not available."));
            }
            inner.setTableMetaData(table);
            inner.setFilter(RecordReferenceSecurityHelper.secureFilter(table, inner.getFilter(), inner.getTransaction()));
            prepare(inner);
            CorePermissionEnforcer.checkReadCriteria(inner, inner.getQueryJoins(), inner.getFilter(), inner.getFieldNamesToInclude());
            inner.setFilter(ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), table, inner.getFilter(), Collections.emptySet()));
            criteria.setValues(List.of(new QSubQuery(subQuery.getTableName(), subQuery.getFieldName(), inner.getFilter())));
         }
         else
         {
            inner.getFilter().setLimit(maxMaterialized + 1);
            inner.getFilter().setSkip(null);
            List<QRecord> records = new QueryAction().execute(inner).getRecords();
            if(records.size() > maxMaterialized)
            {
               throw (new QUserFacingException("This filter matches too many records to combine across databases."));
            }
            List<Serializable> values = new ArrayList<>();
            for(QRecord record : records)
            {
               values.add(record.getValue(subQuery.getFieldName()));
            }
            criteria.setValues(values);
         }
      }
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         prepareSubQueries(input, subFilter);
      }
   }
}
