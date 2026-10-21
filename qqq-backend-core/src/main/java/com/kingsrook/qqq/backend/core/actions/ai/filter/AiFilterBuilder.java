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

package com.kingsrook.qqq.backend.core.actions.ai.filter;


import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiJsonSchema;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolException;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.Now;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.ThisOrLastPeriod;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** The strict filter schema of the AI tools (spec Appendix A: a boolean
 ** operator, criteria, and at most one level of sub-filters - strict schemas
 ** can't be recursive), and its translation to a {@link QQueryFilter}.
 **
 ** Translation is strict too: every criterion's field must be one of the
 ** filterable fields given (exposure other than NONE - so METADATA_ONLY fields
 ** may be filtered on, but unknown or NONE fields are rejected, with the same
 ** message, so a caller can't probe for unexposed fields).  Values are
 ** converted to the field's type.  For DATE and DATE_TIME fields, relative
 ** expressions are evaluated by QQQ (the model never computes dates):
 **    NOW, NOW-7 DAYS, NOW+2 HOURS  (MINUTES|HOURS|DAYS|WEEKS|MONTHS|YEARS)
 **    THIS_DAY, LAST_WEEK, THIS_MONTH, ... (HOUR|DAY|WEEK|MONTH|YEAR)
 *******************************************************************************/
public class AiFilterBuilder
{
   public static final String DEF_QUERY_FILTER = "QueryFilter";
   public static final String DEF_SUB_FILTER   = "SubFilter";
   public static final String DEF_CRITERION    = "Criterion";

   public static final int MAX_CRITERIA    = 25;
   public static final int MAX_SUB_FILTERS = 10;
   public static final int MAX_IN_VALUES   = 100;

   public static final List<String> OPERATORS = List.of("EQUALS", "NOT_EQUALS", "IN", "NOT_IN", "IS_BLANK", "IS_NOT_BLANK",
      "CONTAINS", "STARTS_WITH", "LESS_THAN", "LESS_THAN_OR_EQUALS", "GREATER_THAN", "GREATER_THAN_OR_EQUALS", "BETWEEN");

   private static final Set<String> NO_VALUE_OPERATORS   = Set.of("IS_BLANK", "IS_NOT_BLANK");
   private static final Set<String> MULTI_VALUE_OPERATORS = Set.of("IN", "NOT_IN");

   private static final Pattern NOW_OFFSET_PATTERN     = Pattern.compile("NOW\\s*([+-])\\s*(\\d{1,6})\\s*(MINUTE|HOUR|DAY|WEEK|MONTH|YEAR)S?", Pattern.CASE_INSENSITIVE);
   private static final Pattern THIS_OR_LAST_PATTERN   = Pattern.compile("(THIS|LAST)_(HOUR|DAY|WEEK|MONTH|YEAR)", Pattern.CASE_INSENSITIVE);
   private static final Set<String> DATE_ONLY_UNITS    = Set.of("DAY", "WEEK", "MONTH", "YEAR");

   private static final String VALUES_DESCRIPTION = "Values to compare to: none for IS_BLANK / IS_NOT_BLANK, two for BETWEEN, one or more for IN / NOT_IN, else one.  "
      + "Use raw values (e.g., ids for fields with possible values).  For date and date-time fields, relative expressions are allowed: "
      + "NOW, NOW-7 DAYS, NOW+2 HOURS (units MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS), or THIS_DAY, LAST_WEEK, THIS_MONTH, LAST_YEAR "
      + "(periods HOUR, DAY, WEEK, MONTH, YEAR) - never compute dates yourself.";



   /*******************************************************************************
    ** The $defs for the filter schema (QueryFilter, SubFilter, Criterion), to
    ** put at the root of a tool's input schema.
    *******************************************************************************/
   public static Map<String, Object> buildSchemaDefinitions()
   {
      Map<String, Object> valueItem = new LinkedHashMap<>();
      valueItem.put("type", List.of("string", "number", "boolean"));

      Map<String, Object> criterion = AiJsonSchema.object(AiJsonSchema.properties(
         "field", AiJsonSchema.string("A field name from describe_table."),
         "operator", AiJsonSchema.enumOf(OPERATORS, null),
         "values", AiJsonSchema.array(valueItem, VALUES_DESCRIPTION)
      ), List.of("field", "operator", "values"));

      Map<String, Object> booleanOperator = AiJsonSchema.enumOf(List.of("AND", "OR"), "How the criteria (and sub-filters) combine.");

      Map<String, Object> subFilter = AiJsonSchema.object(AiJsonSchema.properties(
         "booleanOperator", booleanOperator,
         "criteria", AiJsonSchema.array(AiJsonSchema.ref(DEF_CRITERION), null)
      ), List.of("booleanOperator", "criteria"));

      Map<String, Object> queryFilter = AiJsonSchema.object(AiJsonSchema.properties(
         "booleanOperator", booleanOperator,
         "criteria", AiJsonSchema.array(AiJsonSchema.ref(DEF_CRITERION), null),
         "subFilters", AiJsonSchema.array(AiJsonSchema.ref(DEF_SUB_FILTER), "Optional nested groups (one level), each combined with the criteria by booleanOperator.")
      ), List.of("booleanOperator", "criteria"));

      Map<String, Object> definitions = new LinkedHashMap<>();
      definitions.put(DEF_QUERY_FILTER, queryFilter);
      definitions.put(DEF_SUB_FILTER, subFilter);
      definitions.put(DEF_CRITERION, criterion);
      return (definitions);
   }



   /*******************************************************************************
    ** Build a QQueryFilter from a tool's filter input (null input gives an empty
    ** filter), allowing criteria only on the given filterable fields.
    *******************************************************************************/
   public static QQueryFilter build(Object filterInput, Map<String, QFieldMetaData> filterableFields) throws AiToolException
   {
      Map<String, Object> filterMap = AiToolInput.asMap(filterInput, "filter");
      if(filterMap == null)
      {
         return (new QQueryFilter());
      }

      QQueryFilter filter = buildLevel(filterMap, filterableFields, "filter", true);

      int criteriaCount = filter.getCriteria().size();
      for(QQueryFilter subFilter : filter.getSubFilters() == null ? List.<QQueryFilter>of() : filter.getSubFilters())
      {
         criteriaCount += subFilter.getCriteria().size();
      }

      if(criteriaCount > MAX_CRITERIA)
      {
         throw (AiToolException.invalidInput("A filter may have at most " + MAX_CRITERIA + " criteria."));
      }

      return (filter);
   }



   /*******************************************************************************
    ** Build one level of a filter (the top, which may have sub-filters, or a
    ** sub-filter, which may not).
    *******************************************************************************/
   private static QQueryFilter buildLevel(Map<String, Object> filterMap, Map<String, QFieldMetaData> filterableFields, String path, boolean allowSubFilters) throws AiToolException
   {
      Set<String> allowedKeys = allowSubFilters ? Set.of("booleanOperator", "criteria", "subFilters") : Set.of("booleanOperator", "criteria");
      for(String key : filterMap.keySet())
      {
         if(!allowedKeys.contains(key))
         {
            throw (AiToolException.invalidInput("Unknown property '" + key + "' in " + path + (allowSubFilters ? "" : " (sub-filters may not be nested)") + "."));
         }
      }

      QQueryFilter filter          = new QQueryFilter();
      String       booleanOperator = AiToolInput.asString(filterMap.get("booleanOperator"), path + ".booleanOperator");
      if(booleanOperator == null || "AND".equalsIgnoreCase(booleanOperator))
      {
         filter.setBooleanOperator(QQueryFilter.BooleanOperator.AND);
      }
      else if("OR".equalsIgnoreCase(booleanOperator))
      {
         filter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);
      }
      else
      {
         throw (AiToolException.invalidInput("The " + path + ".booleanOperator must be AND or OR."));
      }

      Object criteriaInput = filterMap.get("criteria");
      if(criteriaInput != null)
      {
         if(!(criteriaInput instanceof List<?> criteriaList))
         {
            throw (AiToolException.invalidInput("The " + path + ".criteria must be a list."));
         }

         for(Object criterionInput : criteriaList)
         {
            filter.addCriteria(buildCriterion(AiToolInput.asMap(criterionInput, path + ".criteria"), filterableFields, path));
         }
      }

      Object subFiltersInput = filterMap.get("subFilters");
      if(subFiltersInput != null)
      {
         if(!(subFiltersInput instanceof List<?> subFilterList))
         {
            throw (AiToolException.invalidInput("The " + path + ".subFilters must be a list."));
         }

         if(subFilterList.size() > MAX_SUB_FILTERS)
         {
            throw (AiToolException.invalidInput("A filter may have at most " + MAX_SUB_FILTERS + " sub-filters."));
         }

         for(Object subFilterInput : subFilterList)
         {
            Map<String, Object> subFilterMap = AiToolInput.asMap(subFilterInput, path + ".subFilters");
            if(subFilterMap == null)
            {
               throw (AiToolException.invalidInput("The " + path + ".subFilters may not contain nulls."));
            }
            filter.addSubFilter(buildLevel(subFilterMap, filterableFields, path + ".subFilters", false));
         }
      }

      return (filter);
   }



   /*******************************************************************************
    ** Build one criterion.
    *******************************************************************************/
   private static QFilterCriteria buildCriterion(Map<String, Object> criterionMap, Map<String, QFieldMetaData> filterableFields, String path) throws AiToolException
   {
      if(criterionMap == null)
      {
         throw (AiToolException.invalidInput("The " + path + ".criteria may not contain nulls."));
      }

      for(String key : criterionMap.keySet())
      {
         if(!Set.of("field", "operator", "values").contains(key))
         {
            throw (AiToolException.invalidInput("Unknown property '" + key + "' in a criterion."));
         }
      }

      String         fieldName = AiToolInput.asString(criterionMap.get("field"), "field");
      QFieldMetaData field     = fieldName == null ? null : filterableFields.get(fieldName);
      if(field == null)
      {
         throw (AiToolException.invalidInput("Unknown field '" + fieldName + "' for this table.  Use describe_table to see the available fields."));
      }

      String operatorName = AiToolInput.asString(criterionMap.get("operator"), "operator");
      if(operatorName == null || !OPERATORS.contains(operatorName.toUpperCase(Locale.ROOT)))
      {
         throw (AiToolException.invalidInput("Unknown operator '" + operatorName + "'.  Allowed operators are: " + String.join(", ", OPERATORS) + "."));
      }
      operatorName = operatorName.toUpperCase(Locale.ROOT);

      Object valuesInput = criterionMap.get("values");
      if(valuesInput != null && !(valuesInput instanceof List))
      {
         throw (AiToolException.invalidInput("The values of a criterion must be a list."));
      }
      List<?> valueList = valuesInput == null ? List.of() : (List<?>) valuesInput;

      List<Serializable> values = new ArrayList<>();
      if(!NO_VALUE_OPERATORS.contains(operatorName))
      {
         int expected = "BETWEEN".equals(operatorName) ? 2 : 1;
         if(MULTI_VALUE_OPERATORS.contains(operatorName))
         {
            if(valueList.isEmpty() || valueList.size() > MAX_IN_VALUES)
            {
               throw (AiToolException.invalidInput("The " + operatorName + " operator needs between 1 and " + MAX_IN_VALUES + " values (for field '" + fieldName + "')."));
            }
         }
         else if(valueList.size() != expected)
         {
            throw (AiToolException.invalidInput("The " + operatorName + " operator needs exactly " + expected + " value" + (expected == 1 ? "" : "s") + " (for field '" + fieldName + "')."));
         }

         for(Object value : valueList)
         {
            values.add(convertValue(field, value));
         }
      }

      return (new QFilterCriteria(fieldName, QCriteriaOperator.valueOf(operatorName), values));
   }



   /*******************************************************************************
    ** Convert one criterion value to the field's type - or to a relative date
    ** expression, for a date or date-time field.
    *******************************************************************************/
   static Serializable convertValue(QFieldMetaData field, Object value) throws AiToolException
   {
      if(value == null)
      {
         throw (AiToolException.invalidInput("Criterion values may not be null (use IS_BLANK)."));
      }

      if(!(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean))
      {
         throw (AiToolException.invalidInput("Criterion values must be strings, numbers or booleans."));
      }

      QFieldType type = field.getType() == null ? QFieldType.STRING : field.getType();
      if(value instanceof String string && (type == QFieldType.DATE || type == QFieldType.DATE_TIME))
      {
         Serializable expression = parseRelativeDateExpression(string.trim(), type);
         if(expression != null)
         {
            return (expression);
         }
      }

      try
      {
         Serializable converted = ValueUtils.getValueAsFieldType(type, (Serializable) value);
         if(converted == null)
         {
            throw (new IllegalArgumentException("null conversion"));
         }
         return (converted);
      }
      catch(Exception e)
      {
         throw (AiToolException.invalidInput("The value '" + value + "' is not valid for field '" + field.getName() + "' (type " + type.name() + ")."));
      }
   }



   /*******************************************************************************
    ** Parse a relative date expression - or return null if the value isn't one.
    *******************************************************************************/
   public static Serializable parseRelativeDateExpression(String value, QFieldType type) throws AiToolException
   {
      if("NOW".equalsIgnoreCase(value))
      {
         return (new Now());
      }

      Matcher offsetMatcher = NOW_OFFSET_PATTERN.matcher(value);
      if(offsetMatcher.matches())
      {
         String unit = offsetMatcher.group(3).toUpperCase(Locale.ROOT);
         checkUnitForType(unit, type, value);
         int        amount     = Integer.parseInt(offsetMatcher.group(2));
         ChronoUnit chronoUnit = toChronoUnit(unit);
         return ("+".equals(offsetMatcher.group(1)) ? NowWithOffset.plus(amount, chronoUnit) : NowWithOffset.minus(amount, chronoUnit));
      }

      Matcher periodMatcher = THIS_OR_LAST_PATTERN.matcher(value);
      if(periodMatcher.matches())
      {
         String unit = periodMatcher.group(2).toUpperCase(Locale.ROOT);
         checkUnitForType(unit, type, value);
         ChronoUnit chronoUnit = toChronoUnit(unit);
         return ("THIS".equalsIgnoreCase(periodMatcher.group(1)) ? ThisOrLastPeriod.this_(chronoUnit) : ThisOrLastPeriod.last(chronoUnit));
      }

      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void checkUnitForType(String unit, QFieldType type, String value) throws AiToolException
   {
      if(type == QFieldType.DATE && !DATE_ONLY_UNITS.contains(unit))
      {
         throw (AiToolException.invalidInput("The expression '" + value + "' is not valid for a date field (use DAYS, WEEKS, MONTHS or YEARS)."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ChronoUnit toChronoUnit(String unit)
   {
      return switch(unit)
      {
         case "MINUTE" -> ChronoUnit.MINUTES;
         case "HOUR" -> ChronoUnit.HOURS;
         case "DAY" -> ChronoUnit.DAYS;
         case "WEEK" -> ChronoUnit.WEEKS;
         case "MONTH" -> ChronoUnit.MONTHS;
         default -> ChronoUnit.YEARS;
      };
   }
}
