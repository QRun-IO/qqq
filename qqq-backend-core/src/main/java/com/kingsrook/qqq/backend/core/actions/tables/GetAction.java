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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.interfaces.GetInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.GetActionCacheHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldFilterBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 ** Action to run a get against a table.
 **
 *******************************************************************************/
public class GetAction
{
   private static final QLogger LOG = QLogger.getLogger(GetAction.class);

   private Optional<TableCustomizerInterface> postGetRecordCustomizer;

   private GetInput                 getInput;
   private QPossibleValueTranslator qPossibleValueTranslator;

   private Memoization<Pair<String, String>, List<FieldFilterBehavior<?>>> getFieldFilterBehaviorMemoization = new Memoization<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public GetOutput execute(GetInput getInput) throws QException
   {
      ActionHelper.validateSession(getInput);

      QTableMetaData table = getInput.getTable();
      if(table == null)
      {
         throw (new QException("Requested to Get a record from an unrecognized table: " + getInput.getTableName()));
      }
      table = TableMetaDataPersonalizerAction.execute(getInput);
      getInput.setTableMetaData(table);

      postGetRecordCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.POST_QUERY_RECORD.getRole());
      this.getInput = getInput;

      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(getInput.getBackend());
      // todo pre-customization - just get to modify the request?

      GetInterface getInterface = null;
      try
      {
         getInterface = qModule.getGetInterface();
      }
      catch(IllegalStateException ise)
      {
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if a module doesn't implement Get directly - try to do a Get by a Query in the DefaultGetInterface (inner class) //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      }

      GetOutput getOutput;
      boolean   usingDefaultGetInterface = false;
      if(getInterface == null)
      {
         getInterface = new DefaultGetInterface();
         usingDefaultGetInterface = true;
      }

      getInput = applyFieldBehaviors(getInput);

      getInterface.validateInput(getInput);

      QueryStat queryStat = QueryStatManager.newQueryStat(getInput.getBackend(), table, null, GetAction.class.getSimpleName());
      getOutput = getInterface.execute(getInput);

      if(!usingDefaultGetInterface && queryStat != null)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////
         // if we did the get ourselves here, then record a query stat                             //
         // (vs, the default GetInterface, it does a query, which would have stored its own stats) //
         ////////////////////////////////////////////////////////////////////////////////////////////
         queryStat.setRecordCount(getOutput.getRecord() == null ? 0 : 1);
         QueryStatManager.getInstance().add(queryStat);
      }

      ////////////////////////////
      // handle cache use-cases //
      ////////////////////////////
      if(table.getCacheOf() != null)
      {
         new GetActionCacheHelper().handleCaching(getInput, getOutput);
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if the record is found, perform post-actions on it                                                         //
      // unless the defaultGetInterface was used - as it just does a query, and the query will do the post-actions. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(getOutput.getRecord() != null && !usingDefaultGetInterface)
      {
         getOutput.setRecord(postRecordActions(getOutput.getRecord()));
      }

      return getOutput;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private GetInput applyFieldBehaviors(GetInput getInput)
   {
      QTableMetaData table = getInput.getTable();

      try
      {
         if(getInput.getPrimaryKey() != null)
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if the input has a primary key, get its behaviors, then apply, and update the pkey in the input if the value is different //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            List<FieldFilterBehavior<?>> fieldFilterBehaviors = getFieldFilterBehaviors(table, table.getPrimaryKeyField());
            for(FieldFilterBehavior<?> fieldFilterBehavior : CollectionUtils.nonNullList(fieldFilterBehaviors))
            {
               QFilterCriteria pkeyCriteria    = new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.EQUALS, getInput.getPrimaryKey());
               QFilterCriteria updatedCriteria = ValueBehaviorApplier.apply(pkeyCriteria, QContext.getQInstance(), table, table.getField(table.getPrimaryKeyField()), fieldFilterBehavior);
               if(updatedCriteria != pkeyCriteria)
               {
                  getInput.setPrimaryKey(updatedCriteria.getValues().get(0));
               }
            }
         }
         else if(getInput.getUniqueKey() != null)
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if the input has a unique key, get its behaviors, then apply, and update the ukey values in the input if any are different //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Map<String, Serializable> updatedUniqueKey = new HashMap<>(getInput.getUniqueKey());
            for(String fieldName : getInput.getUniqueKey().keySet())
            {
               List<FieldFilterBehavior<?>> fieldFilterBehaviors = getFieldFilterBehaviors(table, fieldName);
               for(FieldFilterBehavior<?> fieldFilterBehavior : CollectionUtils.nonNullList(fieldFilterBehaviors))
               {
                  QFilterCriteria ukeyCriteria    = new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, updatedUniqueKey.get(fieldName));
                  QFilterCriteria updatedCriteria = ValueBehaviorApplier.apply(ukeyCriteria, QContext.getQInstance(), table, table.getField(table.getPrimaryKeyField()), fieldFilterBehavior);
                  updatedUniqueKey.put(fieldName, updatedCriteria.getValues().get(0));
               }
            }
            getInput.setUniqueKey(updatedUniqueKey);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error applying field behaviors to get input - will run with original inputs", e);
      }

      return (getInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<FieldFilterBehavior<?>> getFieldFilterBehaviors(QTableMetaData tableMetaData, String fieldName)
   {
      Pair<String, String> key = new Pair<>(tableMetaData.getName(), fieldName);
      return getFieldFilterBehaviorMemoization.getResult(key, (p) ->
      {
         List<FieldFilterBehavior<?>> rs = new ArrayList<>();
         for(FieldBehavior<?> fieldBehavior : CollectionUtils.nonNullCollection(tableMetaData.getFields().get(fieldName).getBehaviors()))
         {
            if(fieldBehavior instanceof FieldFilterBehavior<?> fieldFilterBehavior)
            {
               rs.add(fieldFilterBehavior);
            }
         }
         return (rs);
      }).orElse(null);
   }



   /*******************************************************************************
    ** shorthand way to call for the most common use-case, when you just want the
    ** output record to be returned.
    *******************************************************************************/
   public QRecord executeForRecord(GetInput getInput) throws QException
   {
      return (execute(getInput).getRecord());
   }



   /*******************************************************************************
    ** more shorthand way to call for the most common use-case, when you just want the
    ** output record to be returned, and you just want to pass in a table name and primary key.
    *******************************************************************************/
   public static QRecord execute(String tableName, Serializable primaryKey) throws QException
   {
      if(primaryKey instanceof QQueryFilter)
      {
         LOG.warn("Unexpected use of QQueryFilter instead of primary key in GetAction call");
      }

      GetAction getAction = new GetAction();
      GetInput  getInput  = new GetInput(tableName).withPrimaryKey(primaryKey);
      return getAction.executeForRecord(getInput);
   }



   /*******************************************************************************
    ** more shorthand way to call for the most common use-case, when you just want the
    ** output record to be returned, and you just want to pass in a table name and unique key
    *******************************************************************************/
   public static QRecord execute(String tableName, Map<String, Serializable> uniqueKey) throws QException
   {
      GetAction getAction = new GetAction();
      GetInput  getInput  = new GetInput(tableName).withUniqueKey(uniqueKey);
      return getAction.executeForRecord(getInput);
   }



   /*******************************************************************************
    ** Run a GetAction by using the QueryAction instead (e.g., with a filter made
    ** from the pkey/ukey, and returning the single record if found).
    *******************************************************************************/
   public GetOutput executeViaQuery(GetInput getInput) throws QException
   {
      return (new DefaultGetInterface().execute(getInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class DefaultGetInterface implements GetInterface
   {
      @Override
      public GetOutput execute(GetInput getInput) throws QException
      {
         QueryInput queryInput = convertGetInputToQueryInput(getInput);

         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         GetOutput getOutput = new GetOutput();
         if(!queryOutput.getRecords().isEmpty())
         {
            getOutput.setRecord(queryOutput.getRecords().get(0));
         }
         return (getOutput);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QueryInput convertGetInputToQueryInput(GetInput getInput) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(getInput.getTableName());

      //////////////////////////////////////////////////
      // build filter using either pkey or unique key //
      //////////////////////////////////////////////////
      QQueryFilter filter = new QQueryFilter();
      if(getInput.getPrimaryKey() != null)
      {
         filter.addCriteria(new QFilterCriteria(getInput.getTable().getPrimaryKeyField(), QCriteriaOperator.EQUALS, getInput.getPrimaryKey()));
      }
      else if(getInput.getUniqueKey() != null)
      {
         for(Map.Entry<String, Serializable> entry : getInput.getUniqueKey().entrySet())
         {
            if(entry.getValue() == null)
            {
               filter.addCriteria(new QFilterCriteria(entry.getKey(), QCriteriaOperator.IS_BLANK));
            }
            else
            {
               filter.addCriteria(new QFilterCriteria(entry.getKey(), QCriteriaOperator.EQUALS, entry.getValue()));
            }
         }
      }
      else
      {
         throw (new QException("Unable to get " + ObjectUtils.tryElse(() -> queryInput.getTable().getLabel(), queryInput.getTableName()) + ".  Missing required input."));
      }

      queryInput.setFilter(filter);
      queryInput.setCommonParamsFrom(getInput);
      queryInput.setInputSource(getInput.getInputSource());
      return queryInput;
   }



   /*******************************************************************************
    ** Run the necessary actions on a record.  This may include setting display values,
    ** translating possible values, and running post-record customizations.
    *******************************************************************************/
   public QRecord postRecordActions(QRecord record) throws QException
   {
      QRecord returnRecord = record;
      if(this.postGetRecordCustomizer.isPresent())
      {
         returnRecord = postGetRecordCustomizer.get().postQuery(getInput, List.of(record)).get(0);
      }

      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.READ, QContext.getQInstance(), getInput.getTable(), List.of(record), null);

      if(getInput.getShouldTranslatePossibleValues())
      {
         if(qPossibleValueTranslator == null)
         {
            qPossibleValueTranslator = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession());
         }
         qPossibleValueTranslator.translatePossibleValuesInRecords(getInput.getTable(), List.of(returnRecord));
      }

      if(getInput.getShouldGenerateDisplayValues())
      {
         QValueFormatter.setDisplayValuesInRecords(getInput.getTable(), List.of(returnRecord));
      }

      if(getInput.getShouldOmitHiddenFields() || getInput.getShouldMaskPasswords())
      {
         Map<String, QFieldMetaData> fields = getInput.getTable().getFields();
         for(String fieldName : fields.keySet())
         {
            QFieldMetaData field = fields.get(fieldName);
            if(getInput.getShouldOmitHiddenFields() && field.getIsHidden())
            {
               returnRecord.removeValue(fieldName);
            }
            else if(getInput.getShouldMaskPasswords() && field.getType() != null && field.getType().needsMasked() && !field.hasAdornmentType(AdornmentType.REVEAL))
            {
               //////////////////////////////////////////////////////////////////////
               // empty out the value completely first (which will remove from     //
               // display fields as well) then update display value if flag is set //
               //////////////////////////////////////////////////////////////////////
               returnRecord.removeValue(fieldName);
               returnRecord.setValue(fieldName, "************");
               if(getInput.getShouldGenerateDisplayValues())
               {
                  returnRecord.setDisplayValue(fieldName, record.getValueString(fieldName));
               }
            }
         }
      }

      //////////////////////////////////////////////////////////////////////////////
      // note - shouldFetchHeavyFields should be handled by the underlying action //
      //////////////////////////////////////////////////////////////////////////////

      return (returnRecord);
   }
}
