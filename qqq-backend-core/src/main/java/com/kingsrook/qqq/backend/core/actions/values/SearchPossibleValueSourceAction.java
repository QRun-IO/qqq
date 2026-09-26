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

package com.kingsrook.qqq.backend.core.actions.values;


import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Class responsible for looking up possible-values for fields/records and
 ** make them into display values.
 *******************************************************************************/
public class SearchPossibleValueSourceAction
{
   private static final QLogger LOG = QLogger.getLogger(SearchPossibleValueSourceAction.class);

   private static final Set<String> warnedAboutUnexpectedValueField                = Collections.synchronizedSet(new HashSet<>());
   private static final Set<String> warnedAboutUnexpectedNoOfFieldsToSearchByLabel = Collections.synchronizedSet(new HashSet<>());

   private QPossibleValueTranslator possibleValueTranslator;



   /*******************************************************************************
    **
    *******************************************************************************/
   public SearchPossibleValueSourceOutput execute(SearchPossibleValueSourceInput input) throws QException
   {
      QInstance            qInstance           = QContext.getQInstance();
      QPossibleValueSource possibleValueSource = qInstance.getPossibleValueSource(input.getPossibleValueSourceName());
      if(possibleValueSource == null)
      {
         throw new QException("Missing possible value source named [" + input.getPossibleValueSourceName() + "]");
      }

      possibleValueTranslator = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession());
      SearchPossibleValueSourceOutput output = null;
      if(possibleValueSource.getType().equals(QPossibleValueSourceType.ENUM))
      {
         output = searchPossibleValueEnum(input, possibleValueSource);
      }
      else if(possibleValueSource.getType().equals(QPossibleValueSourceType.TABLE))
      {
         output = searchPossibleValueTable(input, possibleValueSource);
      }
      else if(possibleValueSource.getType().equals(QPossibleValueSourceType.CUSTOM))
      {
         output = searchPossibleValueCustom(input, possibleValueSource);
      }
      else
      {
         LOG.error("Unrecognized possibleValueSourceType [" + possibleValueSource.getType() + "] in PVS named [" + possibleValueSource.getName() + "]");
      }

      return (output);
   }



   /***************************************************************************
    ** record to store "computed" values as part of a possible-value search -
    ** e.g., ids type-convered, and lower-cased labels.
    ***************************************************************************/
   public record PreparedSearchPossibleValueSourceInput(Collection<?> inputIdsAsCorrectType, Collection<String> lowerCaseLabels, String searchTerm) {}



   /***************************************************************************
    **
    ***************************************************************************/
   public static PreparedSearchPossibleValueSourceInput prepareSearchPossibleValueSourceInput(SearchPossibleValueSourceInput input)
   {
      QPossibleValueSource possibleValueSource   = QContext.getQInstance().getPossibleValueSource(input.getPossibleValueSourceName());
      List<?>              inputIdsAsCorrectType = convertInputIdsToPossibleValueSourceIdType(possibleValueSource, input.getIdList());

      Set<String> lowerCaseLabels = null;
      if(input.getLabelList() != null)
      {
         lowerCaseLabels = input.getLabelList().stream()
            .filter(Objects::nonNull)
            .map(l -> l.toLowerCase())
            .collect(Collectors.toSet());
      }

      return (new PreparedSearchPossibleValueSourceInput(inputIdsAsCorrectType, lowerCaseLabels, input.getSearchTerm()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SearchPossibleValueSourceOutput searchPossibleValueEnum(SearchPossibleValueSourceInput input, QPossibleValueSource possibleValueSource)
   {
      PreparedSearchPossibleValueSourceInput preparedSearchPossibleValueSourceInput = prepareSearchPossibleValueSourceInput(input);

      SearchPossibleValueSourceOutput output      = new SearchPossibleValueSourceOutput();
      List<Serializable>              matchingIds = new ArrayList<>();

      for(QPossibleValue<?> possibleValue : possibleValueSource.getEnumValues())
      {
         boolean match = doesPossibleValueMatchSearchInput(possibleValue, preparedSearchPossibleValueSourceInput);

         if(match)
         {
            matchingIds.add(possibleValue.getId());
         }
      }

      List<QPossibleValue<?>> qPossibleValues = possibleValueTranslator.buildTranslatedPossibleValueList(possibleValueSource, matchingIds);
      output.setResults(qPossibleValues);

      return (output);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static boolean doesPossibleValueMatchSearchInput(QPossibleValue<?> possibleValue, PreparedSearchPossibleValueSourceInput input)
   {
      boolean match = false;

      if(input.inputIdsAsCorrectType() != null)
      {
         if(input.inputIdsAsCorrectType().contains(possibleValue.getId()))
         {
            match = true;
         }
      }
      else if(input.lowerCaseLabels() != null)
      {
         if(input.lowerCaseLabels().contains(possibleValue.getLabel().toLowerCase()))
         {
            match = true;
         }
      }
      else
      {
         if(StringUtils.hasContent(input.searchTerm()))
         {
            match = (Objects.equals(ValueUtils.getValueAsString(possibleValue.getId()).toLowerCase(), input.searchTerm().toLowerCase())
               || possibleValue.getLabel().toLowerCase().startsWith(input.searchTerm().toLowerCase()));
         }
         else
         {
            match = true;
         }
      }
      return match;
   }



   /*******************************************************************************
    ** The input list of ids might come through as a type that isn't the same as
    ** the type of the ids in the enum (e.g., strings from a frontend, integers
    ** in an enum).  So, this method type-converts them.
    *******************************************************************************/
   private static List<Object> convertInputIdsToPossibleValueSourceIdType(QPossibleValueSource possibleValueSource, List<Serializable> inputIdList)
   {
      List<Object> rs = new ArrayList<>();

      if(inputIdList == null)
      {
         return (null);
      }
      else if(inputIdList.isEmpty())
      {
         return (rs);
      }

      QFieldType type = possibleValueSource.getIdType();

      for(Serializable inputId : inputIdList)
      {
         Object properlyTypedId = null;
         try
         {
            if(type.equals(QFieldType.INTEGER))
            {
               properlyTypedId = ValueUtils.getValueAsInteger(inputId);
            }
            else if(type.isStringLike())
            {
               properlyTypedId = ValueUtils.getValueAsString(inputId);
            }
            else if(type.equals(QFieldType.BOOLEAN))
            {
               properlyTypedId = ValueUtils.getValueAsBoolean(inputId);
            }
            else
            {
               LOG.warn("Unexpected type [" + type + "] for ids in enum: " + possibleValueSource.getName());
            }
         }
         catch(Exception e)
         {
            LOG.debug("Error converting possible value id to expected id type", e, logPair("value", inputId));
         }

         if(properlyTypedId != null)
         {
            rs.add(properlyTypedId);
         }
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SearchPossibleValueSourceOutput searchPossibleValueTable(SearchPossibleValueSourceInput input, QPossibleValueSource possibleValueSource) throws QException
   {
      SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceOutput();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(possibleValueSource.getTableName());

      QTableMetaData table = QContext.getQInstance().getTable(possibleValueSource.getTableName());

      QQueryFilter queryFilter = new QQueryFilter();
      queryFilter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);

      if(input.getIdList() != null)
      {
         queryFilter.addCriteria(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, input.getIdList()));
      }
      else if(input.getLabelList() != null)
      {
         List<String> fieldNames = new ArrayList<>();

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // the 'value fields' will either be 'id' or 'label' (which means, use the fields from the tableMetaData's label fields) //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         for(String valueField : possibleValueSource.getValueFields())
         {
            if("id".equals(valueField))
            {
               fieldNames.add(table.getPrimaryKeyField());
            }
            else if("label".equals(valueField))
            {
               if(table.getRecordLabelFields() != null)
               {
                  fieldNames.addAll(table.getRecordLabelFields());
               }
            }
            else
            {
               String message = "Unexpected valueField defined in possibleValueSource when searching possibleValueSource by label (required: 'id' or 'label')";
               if(!warnedAboutUnexpectedValueField.contains(possibleValueSource.getName()))
               {
                  LOG.warn(message, logPair("valueField", valueField), logPair("possibleValueSource", possibleValueSource.getName()));
                  warnedAboutUnexpectedValueField.add(possibleValueSource.getName());
               }
               output.setWarning(message);
            }
         }

         if(fieldNames.size() == 1)
         {
            queryFilter.addCriteria(new QFilterCriteria(fieldNames.get(0), QCriteriaOperator.IN, input.getLabelList()));
         }
         else
         {
            String message = "Unexpected number of fields found for searching possibleValueSource by label (required: 1, found: " + fieldNames.size() + ")";
            if(!warnedAboutUnexpectedNoOfFieldsToSearchByLabel.contains(possibleValueSource.getName()))
            {
               LOG.warn(message);
               warnedAboutUnexpectedNoOfFieldsToSearchByLabel.add(possibleValueSource.getName());
            }
            output.setWarning(message);
         }
      }
      else
      {
         String searchTerm = input.getSearchTerm();
         if(StringUtils.hasContent(searchTerm))
         {
            for(String valueField : possibleValueSource.getSearchFields())
            {
               try
               {
                  QFieldMetaData field = table.getField(valueField);
                  if(field.getType().equals(QFieldType.STRING))
                  {
                     queryFilter.addCriteria(new QFilterCriteria(valueField, QCriteriaOperator.STARTS_WITH, List.of(searchTerm)));
                  }
                  else if(field.getType().equals(QFieldType.DATE))
                  {
                     LocalDate searchDate = ValueUtils.getValueAsLocalDate(searchTerm);
                     if(searchDate != null)
                     {
                        queryFilter.addCriteria(new QFilterCriteria(valueField, QCriteriaOperator.EQUALS, searchDate));
                     }
                  }
                  else if(field.getType().equals(QFieldType.DATE_TIME))
                  {
                     Instant searchDate = ValueUtils.getValueAsInstant(searchTerm);
                     if(searchDate != null)
                     {
                        queryFilter.addCriteria(new QFilterCriteria(valueField, QCriteriaOperator.EQUALS, searchDate));
                     }
                  }
                  else
                  {
                     Integer valueAsInteger = ValueUtils.getValueAsInteger(searchTerm);
                     if(valueAsInteger != null)
                     {
                        queryFilter.addCriteria(new QFilterCriteria(valueField, QCriteriaOperator.EQUALS, List.of(valueAsInteger)));
                     }
                  }
               }
               catch(Exception e)
               {
                  //////////////////////////////////////////////////////////////////////////////////////////
                  // write a FALSE criteria upon exceptions (e.g., type conversion fails)                 //
                  // Why are we doing this?  so a single-field query finds nothing instead of everything. //
                  //////////////////////////////////////////////////////////////////////////////////////////
                  queryFilter.addCriteria(new QFilterCriteria(valueField, QCriteriaOperator.IN, List.of()));
               }
            }
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // if given a default filter, make it the 'top level' filter and the one we just created a subfilter //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      if(input.getDefaultQueryFilter() != null)
      {
         input.getDefaultQueryFilter().addSubFilter(queryFilter);
         queryFilter = input.getDefaultQueryFilter();
      }

      queryFilter.setLimit(input.getLimit());
      queryFilter.setSkip(input.getSkip());

      queryFilter.setOrderBys(possibleValueSource.getOrderByFields());

      queryInput.setFilter(queryFilter);

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      String fieldName;
      if(StringUtils.hasContent(possibleValueSource.getOverrideIdField()))
      {
         fieldName = possibleValueSource.getOverrideIdField();
      }
      else
      {
         fieldName = table.getPrimaryKeyField();
      }

      List<Serializable>      ids             = queryOutput.getRecords().stream().map(r -> r.getValue(fieldName)).toList();
      List<QPossibleValue<?>> qPossibleValues = possibleValueTranslator.buildTranslatedPossibleValueList(possibleValueSource, ids);
      output.setResults(qPossibleValues);

      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings({ "rawtypes", "unchecked" })
   private SearchPossibleValueSourceOutput searchPossibleValueCustom(SearchPossibleValueSourceInput input, QPossibleValueSource possibleValueSource) throws QException
   {
      try
      {
         QCustomPossibleValueProvider customPossibleValueProvider = QCodeLoader.getAdHoc(QCustomPossibleValueProvider.class, possibleValueSource.getCustomCodeReference());
         List<QPossibleValue<?>>      possibleValues              = customPossibleValueProvider.search(input);

         SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceOutput();
         output.setResults(possibleValues);
         return (output);
      }
      catch(Exception e)
      {
         String message = "Error searching custom possible value source [" + input.getPossibleValueSourceName() + "]";
         LOG.warn(message, e);
         throw (new QException(message, e));
      }
   }

}
