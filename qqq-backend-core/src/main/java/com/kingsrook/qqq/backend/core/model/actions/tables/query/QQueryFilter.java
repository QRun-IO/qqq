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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.AbstractFilterExpression;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.FilterVariableExpression;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 * Full "filter" for a query - a list of criteria and order-bys
 *
 *******************************************************************************/
public class QQueryFilter implements Serializable, Cloneable, QMetaDataObject
{
   private static final QLogger LOG = QLogger.getLogger(QQueryFilter.class);

   private List<QFilterCriteria> criteria = new ArrayList<>();
   private List<QFilterOrderBy>  orderBys = new ArrayList<>();

   private BooleanOperator    booleanOperator = BooleanOperator.AND;
   private List<QQueryFilter> subFilters      = new ArrayList<>();

   //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // initial intent here was - put, e.g., UNION between multiple SELECT (with the individual selects being defined in subFilters) //
   // but, actually SQL would let us do, e.g., SELECT UNION SELECT INTERSECT SELECT                                                //
   // so - we could see a future implementation where we:                                                                          //
   // - used the top-level subFilterSetOperator to indicate hat we are doing a multi-query set-operation query.                    //
   // - looked within the subFilter, to see if it specified a subFilterSetOperator - and use that operator before that query       //
   // but - in v0, just using the one at the top-level works                                                                       //
   //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private SubFilterSetOperator subFilterSetOperator = null;

   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // skip & limit are meant to only apply to QueryAction (at least at the initial time they are added here) //
   // e.g., they are ignored in CountAction, AggregateAction, etc, where their meanings may be less obvious  //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private Integer skip;
   private Integer limit;



   /*******************************************************************************
    **
    *******************************************************************************/
   public enum BooleanOperator
   {
      AND,
      OR
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public enum SubFilterSetOperator
   {
      UNION,
      UNION_ALL,
      INTERSECT,
      EXCEPT
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QQueryFilter()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QQueryFilter(QFilterCriteria... criteria)
   {
      this.criteria = new ArrayList<>(Arrays.stream(criteria).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QQueryFilter clone()
   {
      try
      {
         QQueryFilter clone = (QQueryFilter) super.clone();

         if(criteria != null)
         {
            clone.criteria = new ArrayList<>();
            for(QFilterCriteria criterion : criteria)
            {
               clone.criteria.add(criterion.clone());
            }
         }

         if(orderBys != null)
         {
            clone.orderBys = new ArrayList<>();
            for(QFilterOrderBy orderBy : orderBys)
            {
               clone.orderBys.add(orderBy.clone());
            }
         }

         if(subFilters != null)
         {
            clone.subFilters = new ArrayList<>();
            for(QQueryFilter subFilter : subFilters)
            {
               clone.subFilters.add(subFilter.clone());
            }
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    ** recursively look at both this filter, and any sub-filters it may have.
    *******************************************************************************/
   public boolean hasAnyCriteria()
   {
      if(CollectionUtils.nullSafeHasContents(criteria))
      {
         return (true);
      }

      if(CollectionUtils.nullSafeHasContents(subFilters))
      {
         for(QQueryFilter subFilter : subFilters)
         {
            if(subFilter != null && subFilter.hasAnyCriteria())
            {
               return (true);
            }
         }
      }

      return (false);
   }



   /*******************************************************************************
    ** Getter for criteria
    **
    *******************************************************************************/
   public List<QFilterCriteria> getCriteria()
   {
      return criteria;
   }



   /*******************************************************************************
    ** Setter for criteria
    **
    *******************************************************************************/
   public void setCriteria(List<QFilterCriteria> criteria)
   {
      this.criteria = criteria;
   }



   /*******************************************************************************
    ** Getter for order
    **
    *******************************************************************************/
   public List<QFilterOrderBy> getOrderBys()
   {
      return orderBys;
   }



   /*******************************************************************************
    ** Setter for order
    **
    *******************************************************************************/
   public void setOrderBys(List<QFilterOrderBy> orderBys)
   {
      this.orderBys = orderBys;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addCriteria(QFilterCriteria qFilterCriteria)
   {
      if(criteria == null)
      {
         criteria = new ArrayList<>();
      }
      criteria.add(qFilterCriteria);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QQueryFilter withCriteria(QFilterCriteria qFilterCriteria)
   {
      addCriteria(qFilterCriteria);
      return (this);
   }



   /*******************************************************************************
    ** fluent method to add a new criteria
    *******************************************************************************/
   public QQueryFilter withCriteria(String fieldName, QCriteriaOperator operator, Collection<? extends Serializable> values)
   {
      addCriteria(new QFilterCriteria(fieldName, operator, values));
      return (this);
   }



   /*******************************************************************************
    ** fluent method to add a new criteria
    *******************************************************************************/
   public QQueryFilter withCriteria(String fieldName, QCriteriaOperator operator, Serializable... values)
   {
      addCriteria(new QFilterCriteria(fieldName, operator, values));
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addOrderBy(QFilterOrderBy qFilterOrderBy)
   {
      if(orderBys == null)
      {
         orderBys = new ArrayList<>();
      }
      orderBys.add(qFilterOrderBy);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QQueryFilter withOrderBy(QFilterOrderBy qFilterOrderBy)
   {
      addOrderBy(qFilterOrderBy);
      return (this);
   }



   /*******************************************************************************
    ** Getter for booleanOperator
    **
    *******************************************************************************/
   public BooleanOperator getBooleanOperator()
   {
      return booleanOperator;
   }



   /*******************************************************************************
    ** Setter for booleanOperator
    **
    *******************************************************************************/
   public void setBooleanOperator(BooleanOperator booleanOperator)
   {
      this.booleanOperator = booleanOperator;
   }



   /*******************************************************************************
    ** Fluent setter for booleanOperator
    **
    *******************************************************************************/
   public QQueryFilter withBooleanOperator(BooleanOperator booleanOperator)
   {
      this.booleanOperator = booleanOperator;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subFilters
    **
    *******************************************************************************/
   public List<QQueryFilter> getSubFilters()
   {
      return subFilters;
   }



   /*******************************************************************************
    ** Setter for subFilters
    **
    *******************************************************************************/
   public void setSubFilters(List<QQueryFilter> subFilters)
   {
      this.subFilters = subFilters;
   }



   /*******************************************************************************
    ** Fluent setter for subFilters
    **
    *******************************************************************************/
   public QQueryFilter withSubFilters(List<QQueryFilter> subFilters)
   {
      this.subFilters = subFilters;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for adding a single subFilter
    **
    *******************************************************************************/
   public QQueryFilter withSubFilter(QQueryFilter subFilter)
   {
      addSubFilter(subFilter);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addSubFilter(QQueryFilter subFilter)
   {
      if(this.subFilters == null)
      {
         subFilters = new ArrayList<>();
      }

      subFilters.add(subFilter);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      StringBuilder rs = new StringBuilder("(");
      try
      {
         int criteriaIndex = 0;
         for(QFilterCriteria criterion : CollectionUtils.nonNullList(criteria))
         {
            if(criteriaIndex > 0)
            {
               rs.append(" ").append(getBooleanOperator()).append(" ");
            }
            rs.append(criterion);
            criteriaIndex++;
         }

         if(CollectionUtils.nullSafeHasContents(subFilters))
         {
            rs.append("Sub:{");
            int subIndex = 0;
            for(QQueryFilter subFilter : CollectionUtils.nonNullList(subFilters))
            {
               if(subIndex > 0)
               {
                  rs.append(" ").append(getBooleanOperator()).append(" ");
               }
               rs.append(subFilter);
               subIndex++;
            }
            rs.append("}");
         }

         rs.append(")");

         if(CollectionUtils.nullSafeHasContents(orderBys))
         {
            rs.append("OrderBy[");
            for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(orderBys))
            {
               rs.append(orderBy).append(",");
            }
            rs.append("]");
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error in toString", e);
         rs.append("Error generating toString...");
      }

      return (rs.toString());
   }



   /*******************************************************************************
    ** Replaces any FilterVariables' variableNames with one constructed from the field
    ** name, criteria, and index, camel style
    **
    *******************************************************************************/
   public void prepForBackend()
   {
      Map<String, Integer> fieldOperatorMap = new HashMap<>();
      for(QFilterCriteria criterion : getCriteria())
      {
         if(criterion.getValues() != null)
         {
            int criteriaIndex = 1;
            int valueIndex    = 0;
            for(Serializable value : criterion.getValues())
            {
               ///////////////////////////////////////////////////////////////////////////////
               // keep track of what the index is for this criterion, this way if there are //
               // more than one with the same id/operator values, we can differentiate      //
               ///////////////////////////////////////////////////////////////////////////////
               String backendName = getBackendName(criterion, valueIndex);
               if(!fieldOperatorMap.containsKey(backendName))
               {
                  fieldOperatorMap.put(backendName, criteriaIndex);
               }
               else
               {
                  criteriaIndex = fieldOperatorMap.get(backendName) + 1;
                  fieldOperatorMap.put(backendName, criteriaIndex);
               }

               if(value instanceof FilterVariableExpression fve)
               {
                  if(criteriaIndex > 1)
                  {
                     backendName += criteriaIndex;
                  }
                  fve.setVariableName(backendName);
               }

               valueIndex++;
            }
         }
      }
   }



   /*******************************************************************************
    ** builds up a backend name for a field variable expression
    **
    *******************************************************************************/
   private String getBackendName(QFilterCriteria criterion, int valueIndex)
   {
      StringBuilder backendName = new StringBuilder();
      for(String fieldNameParts : criterion.getFieldName().split("\\."))
      {
         backendName.append(StringUtils.ucFirst(fieldNameParts));
      }

      for(String operatorParts : criterion.getOperator().name().split("_"))
      {
         backendName.append(StringUtils.ucFirst(operatorParts.toLowerCase()));
      }

      if(criterion.getOperator().equals(QCriteriaOperator.BETWEEN) || criterion.getOperator().equals(QCriteriaOperator.NOT_BETWEEN))
      {
         if(valueIndex == 0)
         {
            backendName.append("From");
         }
         else
         {
            backendName.append("To");
         }
      }

      return (StringUtils.lcFirst(backendName.toString()));
   }



   /*******************************************************************************
    ** Replace any criteria values that look like ${input.XXX} with the value of XXX
    ** from the supplied inputValues map.
    **
    ** Note - it may be very important that you call this method on a clone of a
    ** QQueryFilter - e.g., if it's one that defined in metaData, and that we don't
    ** want to be (permanently) changed!!
    **
    ** This overload does not take in a FilterUseCase - it uses FilterUseCase.DEFAULT
    ******************************************************************************/
   public void interpretValues(Map<String, Serializable> inputValues) throws QException
   {
      interpretValues(MapBuilder.of("input", inputValues), FilterUseCase.DEFAULT);
   }



   /*******************************************************************************
    ** Replace any criteria values that look like ${input.XXX} with the value of XXX
    ** from the supplied valueMap map - where the handling of missing values
    ** is specified in the inputted FilterUseCase parameter
    **
    ** input values is a map of maps, so that more than one 'inputValue' map can be used
    ** e.g. MapBuilder.of("input", valueMap, "processValues", processValues)
    **
    ** Note - it may be very important that you call this method on a clone of a
    ** QQueryFilter - e.g., if it's one that defined in metaData, and that we don't
    ** want to be (permanently) changed!!
    **
    *******************************************************************************/
   public void interpretValues(Map<String, Map<String, Serializable>> valueMap, FilterUseCase useCase) throws QException
   {
      List<Exception>           caughtExceptions = new ArrayList<>();
      Map<String, Serializable> inputValues      = valueMap.getOrDefault("input", new HashMap<>());

      ////////////////////////////////////////////////////////////////////////////
      // if process values were passed in, add those to the interpreter as well //
      ////////////////////////////////////////////////////////////////////////////
      QMetaDataVariableInterpreter variableInterpreter = new QMetaDataVariableInterpreter();
      for(String mapName : valueMap.keySet())
      {
         variableInterpreter.addValueMap(mapName, valueMap.get(mapName));
      }

      for(QFilterCriteria criterion : getCriteria())
      {
         if(criterion.getValues() != null)
         {
            List<Serializable> newValues = new ArrayList<>();

            for(Serializable value : criterion.getValues())
            {
               try
               {
                  Serializable interpretedValue = value;
                  Exception    caughtException  = null;

                  if(value instanceof AbstractFilterExpression<?>)
                  {
                     ///////////////////////////////////////////////////////////////////////
                     // if a filter variable expression, evaluate the input values, which //
                     // will replace the variables with the corresponding actual values   //
                     ///////////////////////////////////////////////////////////////////////
                     if(value instanceof FilterVariableExpression filterVariableExpression)
                     {
                        try
                        {
                           interpretedValue = filterVariableExpression.evaluateInputValues(inputValues);
                        }
                        catch(Exception e)
                        {
                           caughtException = e;
                           interpretedValue = InputNotFound.instance;
                        }
                     }
                  }
                  else
                  {
                     /////////////////////////////////////////////////////////////////////////////////////////////////////////
                     // for non-expressions, cast the value to a string, and see if it can be resolved a variable.          //
                     // there are 3 possible cases here:                                                                    //
                     // 1: it doesn't look like a variable, so it just comes back as a string version of whatever went in.  //
                     // 2: it was resolved from a variable to a value, e.g., ${input.someVar} => someValue                  //
                     // 3: it looked like a variable, but no value for that variable was present in the interpreter's value //
                     // map - so we'll get back the InputNotFound.instance.                                                 //
                     /////////////////////////////////////////////////////////////////////////////////////////////////////////
                     String valueAsString = ValueUtils.getValueAsString(value);
                     interpretedValue = variableInterpreter.interpretForObject(valueAsString, InputNotFound.instance);
                  }

                  /////////////////////////////////////////////////////////////////////////////////
                  // if interpreting a value returned the not-found value, or an empty string,   //
                  // then decide how to handle the missing value, based on the use-case input    //
                  // Note: questionable, using "" here, but that's what reality is passing a lot //
                  // for cases we want to treat as missing...                                    //
                  /////////////////////////////////////////////////////////////////////////////////
                  if(interpretedValue == InputNotFound.instance || "".equals(interpretedValue))
                  {
                     CriteriaMissingInputValueBehavior missingInputValueBehavior = getMissingInputValueBehavior(useCase);

                     switch(missingInputValueBehavior)
                     {
                        case REMOVE_FROM_FILTER -> criterion.setOperator(QCriteriaOperator.TRUE);
                        case MAKE_NO_MATCHES -> criterion.setOperator(QCriteriaOperator.FALSE);
                        case INTERPRET_AS_NULL_VALUE -> newValues.add(null);

                        /////////////////////////////////////////////////
                        // handle case in the default: THROW_EXCEPTION //
                        /////////////////////////////////////////////////
                        default -> throw (Objects.requireNonNullElseGet(caughtException, () -> new QUserFacingException("Missing value for criteria on field: " + criterion.getFieldName())));
                     }
                  }
                  else
                  {
                     newValues.add(interpretedValue);
                  }
               }
               catch(Exception e)
               {
                  caughtExceptions.add(e);
               }
            }
            criterion.setValues(newValues);
         }
      }

      //////////////////////////////////////
      // recursively process sub filters! //
      //////////////////////////////////////
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(getSubFilters()))
      {
         try
         {
            subFilter.interpretValues(valueMap, useCase);
         }
         catch(Exception e)
         {
            caughtExceptions.add(e);
         }
      }

      if(!caughtExceptions.isEmpty())
      {
         String  message       = "Error interpreting filter values: " + StringUtils.joinWithCommasAndAnd(caughtExceptions.stream().map(e -> e.getMessage()).toList());
         boolean allUserFacing = caughtExceptions.stream().allMatch(QUserFacingException.class::isInstance);
         throw (allUserFacing ? new QUserFacingException(message) : new QException(message));
      }
   }



   /***************************************************************************
    ** Note:  in the original build of this, it felt like we *might* want to be
    ** able to specify these behaviors at the individual criteria level, where
    ** the implementation would be to add to QFilterCriteria:
    ** - Map<FilterUseCase, CriteriaMissingInputValueBehavior> missingInputValueBehaviors;
    ** - CriteriaMissingInputValueBehavior getMissingInputValueBehaviorForUseCase(FilterUseCase useCase) {}
    *
    ** (and maybe do that in a sub-class of QFilterCriteria, so it isn't always
    ** there?  idk...) and then here we'd call:
    ** - CriteriaMissingInputValueBehavior missingInputValueBehavior = criterion.getMissingInputValueBehaviorForUseCase(useCase);
    *
    ** But, we don't actually have that use-case at hand now, so - let's keep it
    ** just at the level we need for now.
    **
    ***************************************************************************/
   private CriteriaMissingInputValueBehavior getMissingInputValueBehavior(FilterUseCase useCase)
   {
      if(useCase == null)
      {
         useCase = FilterUseCase.DEFAULT;
      }

      CriteriaMissingInputValueBehavior missingInputValueBehavior = useCase.getDefaultCriteriaMissingInputValueBehavior();
      if(missingInputValueBehavior == null)
      {
         missingInputValueBehavior = useCase.getDefaultCriteriaMissingInputValueBehavior();
      }

      if(missingInputValueBehavior == null)
      {
         missingInputValueBehavior = FilterUseCase.DEFAULT.getDefaultCriteriaMissingInputValueBehavior();
      }

      return (missingInputValueBehavior);
   }



   /*******************************************************************************
    ** Getter for skip
    *******************************************************************************/
   public Integer getSkip()
   {
      return (this.skip);
   }



   /*******************************************************************************
    ** Setter for skip
    *******************************************************************************/
   public void setSkip(Integer skip)
   {
      this.skip = skip;
   }



   /*******************************************************************************
    ** Fluent setter for skip
    *******************************************************************************/
   public QQueryFilter withSkip(Integer skip)
   {
      this.skip = skip;
      return (this);
   }



   /*******************************************************************************
    ** Getter for limit
    *******************************************************************************/
   public Integer getLimit()
   {
      return (this.limit);
   }



   /*******************************************************************************
    ** Setter for limit
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Fluent setter for limit
    *******************************************************************************/
   public QQueryFilter withLimit(Integer limit)
   {
      this.limit = limit;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return true;
      }

      if(o == null || getClass() != o.getClass())
      {
         return false;
      }

      QQueryFilter that = (QQueryFilter) o;
      return Objects.equals(criteria, that.criteria) && Objects.equals(orderBys, that.orderBys) && booleanOperator == that.booleanOperator && Objects.equals(subFilters, that.subFilters) && Objects.equals(skip, that.skip) && Objects.equals(limit, that.limit);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(criteria, orderBys, booleanOperator, subFilters, skip, limit);
   }



   /***************************************************************************
    ** "Token" object to be used as the defaultIfLooksLikeVariableButNotFound
    ** parameter to variableInterpreter.interpretForObject, so we can be
    ** very clear that we got this default back (e.g., instead of a null,
    ** which could maybe mean something else?)
    ***************************************************************************/
   private static final class InputNotFound implements Serializable
   {
      private static InputNotFound instance = new InputNotFound();



      /*******************************************************************************
       ** private singleton constructor
       *******************************************************************************/
      private InputNotFound()
      {

      }
   }



   /*******************************************************************************
    ** Getter for subFilterSetOperator
    *******************************************************************************/
   public SubFilterSetOperator getSubFilterSetOperator()
   {
      return (this.subFilterSetOperator);
   }



   /*******************************************************************************
    ** Setter for subFilterSetOperator
    *******************************************************************************/
   public void setSubFilterSetOperator(SubFilterSetOperator subFilterSetOperator)
   {
      this.subFilterSetOperator = subFilterSetOperator;
   }



   /*******************************************************************************
    ** Fluent setter for subFilterSetOperator
    *******************************************************************************/
   public QQueryFilter withSubFilterSetOperator(SubFilterSetOperator subFilterSetOperator)
   {
      this.subFilterSetOperator = subFilterSetOperator;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void applyCriteriaOptionToAllCriteria(CriteriaOptionInterface criteriaOption)
   {
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(this.criteria))
      {
         criteria.withOption(criteriaOption);
      }

      for(QQueryFilter subFilter : CollectionUtils.nonNullList(subFilters))
      {
         subFilter.applyCriteriaOptionToAllCriteria(criteriaOption);
      }
   }

}
