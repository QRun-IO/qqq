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

package com.kingsrook.qqq.backend.core.adapters;


import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MutableList;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Adapter class to convert back and forth between JSON strings and QQueryFilters
 *
 * <p>At a basic level, we just us Jackson (de)serialization for this, but in case
 * any additional extra custom controls are needed (besides what we have in the
 * Jackson custom mapper on classes such as {@link QFilterCriteria}, then those
 * can be built into here.</p>
 *
 * <p>For example, the initial use case is:</p>
 * <ul>
 *    <li><code>discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues</code>
 *    an option to, as it says, discard criteria with an operator that expects a value
 *    (like EQUALS or IN), but whose values array is null, empty, or size 1 with an empty
 *    string as its own value (as has been seen to come from QFMD in some cases).
 *    </li>
 * </ul>
 *******************************************************************************/
public class QQueryFilterJsonAdapter
{
   private static final QLogger LOG = QLogger.getLogger(QQueryFilterJsonAdapter.class);

   private boolean discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues = true;



   /*******************************************************************************
    * convert a JSON String into a QQueryFilter
    *
    * @param json string to be converted to a QQueryFilter
    * @return QQueryFilter - never null;  new and blank if input json is null or empty.
    *******************************************************************************/
   public QQueryFilter jsonToQQueryFilter(String json) throws QException
   {
      if(!StringUtils.hasContent(json))
      {
         return (new QQueryFilter());
      }

      try
      {
         QQueryFilter queryFilter = JsonUtils.toObject(json, QQueryFilter.class);

         if(discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues)
         {
            discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues(queryFilter);
         }

         return queryFilter;
      }
      catch(Exception e)
      {
         throw (new QException("Error converting JSON String to QQueryFilter", e));
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues(QQueryFilter queryFilter)
   {
      if(CollectionUtils.nullSafeHasContents(queryFilter.getCriteria()))
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // wrap the criteria list in a MutableList, in case we need to remove anything from it (and it isn't mutable) //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         List<QFilterCriteria> criteria = new MutableList<>(queryFilter.getCriteria());
         boolean removedAny = false;

         Iterator<QFilterCriteria> iterator = criteria.iterator();
         while(iterator.hasNext())
         {
            QFilterCriteria next = iterator.next();

            ////////////////////////////////////////////////////////////////////////////
            // check if the operator requires values - e.g., don't discard a criteria //
            // for an operator that doesn't use or need values                        //
            ////////////////////////////////////////////////////////////////////////////
            boolean requireValues = true;
            if(QCriteriaOperator.TRUE.equals(next.getOperator()) || QCriteriaOperator.FALSE.equals(next.getOperator()) || QCriteriaOperator.IS_BLANK.equals(next.getOperator()) || QCriteriaOperator.IS_NOT_BLANK.equals(next.getOperator()))
            {
               requireValues = false;
            }

            /////////////////////////////////////////////////////////////////////////////////////////
            // if values are required, but not present (null or empty list, or a list with only an //
            // empty string in it (a bug from frontend!)) then return null to discard the criteria //
            /////////////////////////////////////////////////////////////////////////////////////////
            if(requireValues)
            {
               List<Serializable> values = next.getValues();
               if(CollectionUtils.nullSafeIsEmpty(values) || values.size() == 1 && "".equals(values.get(0)))
               {
                  LOG.info("Discarding criteria with empty values", logPair("criteria", next));
                  iterator.remove();
                  removedAny = true;
               }
            }
         };

         ////////////////////////////////////////////////////////////////////////////////////////
         // replace the list in the filter with the mutable list that we removed elements from //
         // just in case the original list wasn't mutable, so the MutableList had to construct //
         // a new list inside itself.                                                          //
         ////////////////////////////////////////////////////////////////////////////////////////
         if(removedAny)
         {
            queryFilter.setCriteria(criteria);
         }
      }

      /////////////////////////////////////
      // recursively process sub filters //
      /////////////////////////////////////
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(queryFilter.getSubFilters()))
      {
         discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues(subFilter);
      }
   }



   /*******************************************************************************
    * Getter for discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues
    * @see #withDiscardCriteriaWithEmptyValueListsForOperatorsThatExpectValues(boolean)
    *******************************************************************************/
   public boolean getDiscardCriteriaWithEmptyValueListsForOperatorsThatExpectValues()
   {
      return (this.discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues);
   }



   /*******************************************************************************
    * Setter for discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues
    * @see #withDiscardCriteriaWithEmptyValueListsForOperatorsThatExpectValues(boolean)
    *******************************************************************************/
   public void setDiscardCriteriaWithEmptyValueListsForOperatorsThatExpectValues(boolean discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues)
   {
      this.discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues = discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues;
   }



   /*******************************************************************************
    * Fluent setter for discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues
    *
    * @param discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues
    * set this adapter to discard criteria with an operator that expects a value
    * (like EQUALS or IN), but whose values array is null, empty, or size 1 with an empty
    * string as its own value (as has been seen to come from QFMD in some cases).
    * The default value for this property is <b>true</b>.
    * @return this
    *******************************************************************************/
   public QQueryFilterJsonAdapter withDiscardCriteriaWithEmptyValueListsForOperatorsThatExpectValues(boolean discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues)
   {
      this.discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues = discardCriteriaWithEmptyValueListsForOperatorsThatExpectValues;
      return (this);
   }

}
