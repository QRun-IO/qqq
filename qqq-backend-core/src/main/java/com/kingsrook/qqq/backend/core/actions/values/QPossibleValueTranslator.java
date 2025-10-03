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

package com.kingsrook.qqq.backend.core.actions.values;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Class responsible for looking up possible-values for fields/records and
 ** make them into display values.
 *******************************************************************************/
public class QPossibleValueTranslator
{
   private static final QLogger LOG = QLogger.getLogger(QPossibleValueTranslator.class);

   ///////////////////////////////////////////////////////
   // top-level keys are pvsNames (not table names)     //
   // 2nd-level keys are pkey values from the PVS table //
   ///////////////////////////////////////////////////////
   private Map<String, Map<Serializable, String>> possibleValueCache = new HashMap<>();

   private int maxSizePerPvsCache = 50_000;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QPossibleValueTranslator()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueTranslator(QInstance qInstance, QSession session)
   {
   }



   /*******************************************************************************
    ** For a list of records, translate their possible values (populating their display values)
    *******************************************************************************/
   public void translatePossibleValuesInRecords(QTableMetaData table, List<QRecord> records)
   {
      translatePossibleValuesInRecords(table, records, Collections.emptyList(), null);
   }



   /*******************************************************************************
    ** For a list of records, translate their possible values (populating their display values)
    *******************************************************************************/
   public void translatePossibleValuesInRecords(QTableMetaData table, List<QRecord> records, List<QueryJoin> queryJoins, Set<String> limitedToFieldNames)
   {
      if(records == null || table == null)
      {
         return;
      }

      if(limitedToFieldNames != null && limitedToFieldNames.isEmpty())
      {
         LOG.debug("We were asked to translate possible values, but then given an empty set of fields to translate, so noop.");
         return;
      }

      LOG.trace("Translating possible values in [" + records.size() + "] records from the [" + table.getName() + "] table.");
      primePvsCache(table, records, queryJoins, limitedToFieldNames);

      for(QRecord record : records)
      {
         for(QFieldMetaData field : table.getFields().values())
         {
            if(field.getPossibleValueSourceName() != null)
            {
               if(limitedToFieldNames == null || limitedToFieldNames.contains(field.getName()))
               {
                  record.setDisplayValue(field.getName(), translatePossibleValue(field, record.getValue(field.getName())));
               }
            }
         }

         for(QueryJoin queryJoin : CollectionUtils.nonNullList(queryJoins))
         {
            if(queryJoin.getSelect())
            {
               try
               {
                  QTableMetaData joinTable = QContext.getQInstance().getTable(queryJoin.getJoinTable());
                  for(QFieldMetaData field : joinTable.getFields().values())
                  {
                     String joinFieldName = Objects.requireNonNullElse(queryJoin.getAlias(), joinTable.getName()) + "." + field.getName();
                     if(field.getPossibleValueSourceName() != null)
                     {
                        if(limitedToFieldNames == null || limitedToFieldNames.contains(joinFieldName))
                        {
                           ///////////////////////////////////////////////
                           // avoid circling-back upon the source table //
                           ///////////////////////////////////////////////
                           QPossibleValueSource possibleValueSource = QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
                           if(QPossibleValueSourceType.TABLE.equals(possibleValueSource.getType()) && table.getName().equals(possibleValueSource.getTableName()))
                           {
                              continue;
                           }

                           record.setDisplayValue(joinFieldName, translatePossibleValue(field, record.getValue(joinFieldName)));
                        }
                     }
                  }
               }
               catch(Exception e)
               {
                  LOG.warn("Error translating join table possible values", e);
               }
            }
         }

      }
   }



   /*******************************************************************************
    ** Translate a list of ids to a list of possible values (e.g., w/ rendered values)
    *******************************************************************************/
   public List<QPossibleValue<?>> buildTranslatedPossibleValueList(QPossibleValueSource possibleValueSource, List<Serializable> ids)
   {
      if(ids == null)
      {
         return (null);
      }

      if(ids.isEmpty())
      {
         return (new ArrayList<>());
      }

      List<QPossibleValue<?>> rs = new ArrayList<>();
      if(possibleValueSource.getType().equals(QPossibleValueSourceType.TABLE))
      {
         primePvsCache(possibleValueSource.getTableName(), List.of(possibleValueSource), ids);
      }

      for(Serializable id : ids)
      {
         String translated = translatePossibleValue(possibleValueSource, id);
         rs.add(new QPossibleValue<>(id, translated));
      }

      return (rs);
   }



   /*******************************************************************************
    ** For a given field and (raw/id) value, get the translated (string) value.
    *******************************************************************************/
   public String translatePossibleValue(QFieldMetaData field, Serializable value)
   {
      QPossibleValueSource possibleValueSource = QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
      if(possibleValueSource == null)
      {
         LOG.error("Missing possible value source named [" + field.getPossibleValueSourceName() + "] when formatting value for field [" + field.getName() + "]");
         return (null);
      }

      try
      {
         if(field.getType().equals(QFieldType.INTEGER) && !(value instanceof Integer))
         {
            value = ValueUtils.getValueAsInteger(value);
         }
         if(field.getType().equals(QFieldType.LONG) && !(value instanceof Long))
         {
            value = ValueUtils.getValueAsLong(value);
         }
      }
      catch(QValueException e)
      {
         LOG.info("Error translating possible value raw value...");
         ///////////////////////////
         // leave value as it was //
         ///////////////////////////
      }

      return translatePossibleValue(possibleValueSource, value);
   }



   /*******************************************************************************
    ** For a given PossibleValueSource and (raw/id) value, get the translated (string) value.
    *******************************************************************************/
   String translatePossibleValue(QPossibleValueSource possibleValueSource, Serializable value)
   {
      String resultValue = null;
      if(possibleValueSource.getType().equals(QPossibleValueSourceType.ENUM))
      {
         resultValue = translatePossibleValueEnum(value, possibleValueSource);
      }
      else if(possibleValueSource.getType().equals(QPossibleValueSourceType.TABLE))
      {
         resultValue = translatePossibleValueTable(value, possibleValueSource);
      }
      else if(possibleValueSource.getType().equals(QPossibleValueSourceType.CUSTOM))
      {
         resultValue = translatePossibleValueCustom(value, possibleValueSource);
      }
      else
      {
         LOG.error("Unrecognized possibleValueSourceType [" + possibleValueSource.getType() + "] in PVS named [" + possibleValueSource.getName() + "]");
      }

      if(resultValue == null)
      {
         resultValue = getDefaultForPossibleValue(possibleValueSource, value);
      }

      return (resultValue);
   }



   /*******************************************************************************
    ** do translation for an enum-type PVS
    *******************************************************************************/
   private String translatePossibleValueEnum(Serializable value, QPossibleValueSource possibleValueSource)
   {
      for(QPossibleValue<?> possibleValue : possibleValueSource.getEnumValues())
      {
         if(possibleValue.getId().equals(value))
         {
            return (formatPossibleValue(possibleValueSource, possibleValue));
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** do translation for a table-type PVS
    *******************************************************************************/
   String translatePossibleValueTable(Serializable value, QPossibleValueSource possibleValueSource)
   {
      /////////////////////////////////
      // null input gets null output //
      /////////////////////////////////
      if(value == null)
      {
         return (null);
      }

      //////////////////////////////////////////////////////////////
      // look for cached value - if it's missing, call the primer //
      //////////////////////////////////////////////////////////////
      Map<Serializable, String> cacheForPvs = possibleValueCache.computeIfAbsent(possibleValueSource.getName(), x -> new HashMap<>());
      if(!cacheForPvs.containsKey(value))
      {
         primePvsCache(possibleValueSource.getTableName(), List.of(possibleValueSource), List.of(value));
      }

      return (cacheForPvs.get(value));
   }



   /*******************************************************************************
    ** do translation for a custom-type PVS
    *******************************************************************************/
   private String translatePossibleValueCustom(Serializable value, QPossibleValueSource possibleValueSource)
   {
      /////////////////////////////////
      // null input gets null output //
      /////////////////////////////////
      if(value == null)
      {
         return (null);
      }

      try
      {
         QCustomPossibleValueProvider<?> customPossibleValueProvider = QCodeLoader.getAdHoc(QCustomPossibleValueProvider.class, possibleValueSource.getCustomCodeReference());
         QPossibleValue<?>               possibleValue               = customPossibleValueProvider.getPossibleValue(value);
         if(possibleValue == null)
         {
            return (null);
         }

         return (formatPossibleValue(possibleValueSource, possibleValue));
      }
      catch(Exception e)
      {
         LOG.warn("Error sending [" + value + "] for through custom code for PVS [" + possibleValueSource.getName() + "]", e);
      }

      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String formatPossibleValue(QPossibleValueSource possibleValueSource, QPossibleValue<?> possibleValue)
   {
      return (doFormatPossibleValue(possibleValueSource.getValueFormat(), possibleValueSource.getValueFields(), possibleValue.getId(), possibleValue.getLabel()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getDefaultForPossibleValue(QPossibleValueSource possibleValueSource, Serializable value)
   {
      if(possibleValueSource.getValueFormatIfNotFound() == null)
      {
         return (null);
      }

      return (doFormatPossibleValue(possibleValueSource.getValueFormatIfNotFound(), possibleValueSource.getValueFieldsIfNotFound(), value, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String doFormatPossibleValue(String formatString, List<String> valueFields, Object id, String label)
   {
      List<Object> values = new ArrayList<>();
      if(valueFields != null)
      {
         for(String valueField : valueFields)
         {
            Object value = switch(valueField)
            {
               case "id" -> id;
               case "label" -> label;
               default -> throw new IllegalArgumentException("Unexpected value field: " + valueField);
            };
            values.add(Objects.requireNonNullElse(value, ""));
         }
      }

      return (formatString.formatted(values.toArray()));
   }



   /*******************************************************************************
    ** prime the cache (e.g., by doing bulk-queries) for table-based PVS's
    *
    * @param table the table that the records are from
    ** @param records the records that have the possible value id's (e.g., foreign keys)
    * @param queryJoins joins that were used as part of the query that led to the records.
    * @param limitedToFieldNames set of names that are the only fields that get translated (null means all fields).
    *******************************************************************************/
   void primePvsCache(QTableMetaData table, List<QRecord> records, List<QueryJoin> queryJoins, Set<String> limitedToFieldNames)
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // this is a map of String(tableName - the PVS table) to Pair(String (either "" for main table in a query, or join-table + "."), field (from the table being selected from)) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      ListingHash<String, Pair<String, QFieldMetaData>> fieldsByPvsTable = new ListingHash<>();

      ///////////////////////////////////////////////////////////////////////////////////////
      // this is a map of String(tableName - the PVS table) to PossibleValueSource objects //
      ///////////////////////////////////////////////////////////////////////////////////////
      ListingHash<String, QPossibleValueSource> pvsesByTable = new ListingHash<>();

      primePvsCacheTableListingHashLoader(table, fieldsByPvsTable, pvsesByTable, "", table.getName(), limitedToFieldNames);
      for(QueryJoin queryJoin : CollectionUtils.nonNullList(queryJoins))
      {
         if(queryJoin.getSelect())
         {
            String aliasOrTableName = Objects.requireNonNullElse(queryJoin.getAlias(), queryJoin.getJoinTable());
            primePvsCacheTableListingHashLoader(QContext.getQInstance().getTable(queryJoin.getJoinTable()), fieldsByPvsTable, pvsesByTable, aliasOrTableName + ".", queryJoin.getJoinTable(), limitedToFieldNames);
         }
      }

      for(Map.Entry<String, Map<Serializable, String>> entry : possibleValueCache.entrySet())
      {
         int size = entry.getValue().size();
         if(size > maxSizePerPvsCache)
         {
            LOG.info("Found a big PVS cache - clearing it.", logPair("name", entry.getKey()), logPair("size", size));
            entry.getValue().clear();
         }
      }

      for(String tableName : fieldsByPvsTable.keySet())
      {
         Set<Serializable> values = new HashSet<>();
         for(QRecord record : records)
         {
            for(Pair<String, QFieldMetaData> fieldPair : fieldsByPvsTable.get(tableName))
            {
               String       fieldName  = fieldPair.getA() + fieldPair.getB().getName();
               Serializable fieldValue = record.getValue(fieldName);

               /////////////////////////////////////////
               // ignore null and empty-string values //
               /////////////////////////////////////////
               if(!StringUtils.hasContent(ValueUtils.getValueAsString(fieldValue)))
               {
                  continue;
               }

               //////////////////////////////////////
               // check if value is already cached //
               //////////////////////////////////////
               QPossibleValueSource      possibleValueSource = pvsesByTable.get(tableName).get(0);
               Map<Serializable, String> cacheForPvs         = possibleValueCache.computeIfAbsent(possibleValueSource.getName(), x -> new HashMap<>());

               if(!cacheForPvs.containsKey(fieldValue))
               {
                  values.add(fieldValue);
               }
            }
         }

         if(!values.isEmpty())
         {
            primePvsCache(tableName, pvsesByTable.get(tableName), values);
         }
      }
   }



   /*******************************************************************************
    ** Helper for the primePvsCache method
    *******************************************************************************/
   private void primePvsCacheTableListingHashLoader(QTableMetaData table, ListingHash<String, Pair<String, QFieldMetaData>> fieldsByPvsTable, ListingHash<String, QPossibleValueSource> pvsesByTable, String fieldNamePrefix, String tableName, Set<String> limitedToFieldNames)
   {
      for(QFieldMetaData field : table.getFields().values())
      {
         QPossibleValueSource possibleValueSource = QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
         if(possibleValueSource != null && possibleValueSource.getType().equals(QPossibleValueSourceType.TABLE))
         {
            if(limitedToFieldNames != null && !limitedToFieldNames.contains(fieldNamePrefix + field.getName()))
            {
               LOG.trace("Skipping cache priming for translation of possible value field [" + fieldNamePrefix + field.getName() + "] - it's not in the limitedToFieldNames set.");
               continue;
            }

            fieldsByPvsTable.add(possibleValueSource.getTableName(), Pair.of(fieldNamePrefix, field));

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // todo - optimization we can put the same PVS in this listing hash multiple times... either check for dupes, or change to a set, or something smarter. //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            pvsesByTable.add(possibleValueSource.getTableName(), possibleValueSource);
         }
      }
   }



   /*******************************************************************************
    ** For a given table, and a list of pkey-values in that table, AND a list of
    ** possible value sources based on that table (maybe usually 1, but could be more,
    ** e.g., if they had different formatting, or different filters (todo, would that work?)
    ** - query for the values in the table, and populate the possibleValueCache.
    *******************************************************************************/
   private void primePvsCache(String tableName, List<QPossibleValueSource> possibleValueSources, Collection<Serializable> values)
   {
      String idField = null;
      for(QPossibleValueSource possibleValueSource : possibleValueSources)
      {
         possibleValueCache.putIfAbsent(possibleValueSource.getName(), new HashMap<>());
         String thisPvsIdField;
         if(StringUtils.hasContent(possibleValueSource.getOverrideIdField()))
         {
            thisPvsIdField = possibleValueSource.getOverrideIdField();
         }
         else
         {
            thisPvsIdField = QContext.getQInstance().getTable(tableName).getPrimaryKeyField();
         }

         if(idField == null)
         {
            idField = thisPvsIdField;
         }
         else
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // does this ever happen?  maybe not... because, like, the list of values probably wouldn't make sense for //
            // more than one field in the table...                                                                     //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!idField.equals(thisPvsIdField))
            {
               for(QPossibleValueSource valueSource : possibleValueSources)
               {
                  primePvsCache(tableName, List.of(valueSource), values);
               }
            }
         }
      }

      try
      {
         for(List<Serializable> page : CollectionUtils.getPages(values, 1000))
         {
            QueryInput queryInput = new QueryInput();
            queryInput.setTableName(tableName);
            queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria(idField, QCriteriaOperator.IN, page)));
            queryInput.hasQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // when querying for possible values, we do want to generate their display values, which makes record labels, which are usually used as PVS labels //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            queryInput.setShouldGenerateDisplayValues(true);

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // also, if this table uses any possible value fields as part of its own record label, then THOSE possible values need translated. //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Set<String> possibleValueFieldsToTranslate = new HashSet<>();
            for(QPossibleValueSource possibleValueSource : possibleValueSources)
            {
               if(possibleValueSource.getType().equals(QPossibleValueSourceType.TABLE))
               {
                  QTableMetaData table = QContext.getQInstance().getTable(possibleValueSource.getTableName());
                  for(String recordLabelField : CollectionUtils.nonNullList(table.getRecordLabelFields()))
                  {
                     QFieldMetaData field = table.getField(recordLabelField);
                     if(field.getPossibleValueSourceName() != null)
                     {
                        possibleValueFieldsToTranslate.add(field.getName());
                     }
                  }
               }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // an earlier version of this code got into stack overflows, so do a "cheap" check for recursion depth too... //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!possibleValueFieldsToTranslate.isEmpty() && notTooDeep())
            {
               queryInput.setShouldTranslatePossibleValues(true);
               queryInput.setFieldsToTranslatePossibleValues(possibleValueFieldsToTranslate);
            }

            LOG.trace("Priming PVS cache for [" + page.size() + "] ids from [" + tableName + "] table.");
            QueryOutput queryOutput = new QueryAction().execute(queryInput);

            ///////////////////////////////////////////////////////////////////////////////////
            // for all records that were found, put a formatted value into cache foreach PVS //
            ///////////////////////////////////////////////////////////////////////////////////
            for(QRecord record : queryOutput.getRecords())
            {
               Serializable pkeyValue = record.getValue(idField);
               for(QPossibleValueSource possibleValueSource : possibleValueSources)
               {
                  QPossibleValue<?> possibleValue = new QPossibleValue<>(pkeyValue, record.getRecordLabel());
                  possibleValueCache.get(possibleValueSource.getName()).put(pkeyValue, formatPossibleValue(possibleValueSource, possibleValue));
               }
            }

            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            // for all pkeys that were NOT found, put a null value into cache foreach PVS (to avoid re-looking up) //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            for(Serializable pkey : page)
            {
               for(QPossibleValueSource possibleValueSource : possibleValueSources)
               {
                  if(!possibleValueCache.get(possibleValueSource.getName()).containsKey(pkey))
                  {
                     possibleValueCache.get(possibleValueSource.getName()).put(pkey, null);
                  }
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error looking up possible values for table [" + tableName + "]", e);
      }
   }



   /*******************************************************************************
    ** Avoid infinite recursion, for where one field's PVS depends on another's...
    ** not too smart, just breaks at 5...
    *******************************************************************************/
   private boolean notTooDeep()
   {
      int count = 0;
      for(StackTraceElement stackTraceElement : new Throwable().getStackTrace())
      {
         if(stackTraceElement.getMethodName().equals("translatePossibleValuesInRecords"))
         {
            count++;
         }
      }

      return (count < 5);
   }



   /*******************************************************************************
    ** Getter for maxSizePerPvsCache
    *******************************************************************************/
   public int getMaxSizePerPvsCache()
   {
      return (this.maxSizePerPvsCache);
   }



   /*******************************************************************************
    ** Setter for maxSizePerPvsCache
    *******************************************************************************/
   public void setMaxSizePerPvsCache(int maxSizePerPvsCache)
   {
      this.maxSizePerPvsCache = maxSizePerPvsCache;
   }

}
