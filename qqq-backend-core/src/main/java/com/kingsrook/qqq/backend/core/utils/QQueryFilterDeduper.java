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

package com.kingsrook.qqq.backend.core.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IN;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IS_NOT_BLANK;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.NOT_EQUALS;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.NOT_IN;


/*******************************************************************************
 ** Class to help deduplicate redundant criteria in filters.
 **
 ** Original use-case is for making more clean url links out of filters.
 **
 ** Does not (at this time) look into sub-filters at all, or support any "OR"
 ** filters other than the most basic (a=1 OR a=1).
 **
 ** Also, other than for completely redundant criteria (e.g., a>1 and a>1) only
 * works on a limited subset of criteria operators (EQUALS, NOT_EQUALS, IN, and NOT_IN)
 *******************************************************************************/
public class QQueryFilterDeduper
{
   private static final QLogger LOG = QLogger.getLogger(QQueryFilterDeduper.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QQueryFilter dedupeFilter(QQueryFilter filter)
   {
      if(filter == null)
      {
         return (null);
      }

      try
      {
         /////////////////////////////////////////////////////////////////
         // track (just for logging) if we failed or if we did any good //
         /////////////////////////////////////////////////////////////////
         List<String> log        = new ArrayList<>();
         boolean      fail       = false;
         boolean      didAnyGood = false;

         //////////////////////////////////////////////////////////////////////////////////////////
         // always create a clone to be returned.  this is especially useful because,            //
         // the clone's lists will be ArrayLists, which are mutable - since some of the deduping //
         // involves manipulating value lists.                                                   //
         //////////////////////////////////////////////////////////////////////////////////////////
         QQueryFilter rs = filter.clone();

         ////////////////////////////////////////////////////////////////////////////////////
         // general strategy is:                                                           //
         // iterate over criteria, possibly removing the one the iterator is pointing at,  //
         // if we are able to somehow merge it into other criteria we've already seen.     //
         // the others-we've-seen will be tracked in the criteriaByFieldName listing hash. //
         ////////////////////////////////////////////////////////////////////////////////////
         ListingHash<String, QFilterCriteria> criteriaByFieldName = new ListingHash<>();
         Iterator<QFilterCriteria>            iterator            = rs.getCriteria().iterator();
         while(iterator.hasNext())
         {
            QFilterCriteria criteria = iterator.next();

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // first thing to check is, have we seen any other criteria for this field - if so - try to do some de-duping. //
            // note that, any time we do a remove, we'll need to do a continue - to avoid adding the now-removed criteria  //
            // to the listing hash                                                                                         //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(criteriaByFieldName.containsKey(criteria.getFieldName()))
            {
               List<QFilterCriteria> others = criteriaByFieldName.get(criteria.getFieldName());
               QFilterCriteria       other  = others.get(0);

               if(others.size() == 1 && other.equals(criteria))
               {
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // if we've only see 1 other criteria for this field so far, and this one is an exact match, then remove this one. //
                  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  log.add(String.format("Remove duplicate criteria [%s]", criteria));
                  iterator.remove();
                  didAnyGood = true;
                  continue;
               }
               else
               {
                  /////////////////////////////////////////////////////////////////////////////////////////
                  // else - if there's still just 1 other, and it's an AND query - then apply some basic //
                  // logic-merging operations, based on the pair of criteria operators                   //
                  /////////////////////////////////////////////////////////////////////////////////////////
                  if(others.size() == 1 && QQueryFilter.BooleanOperator.AND.equals(filter.getBooleanOperator()))
                  {
                     if((NOT_EQUALS.equals(other.getOperator()) || NOT_IN.equals(other.getOperator())) && EQUALS.equals(criteria.getOperator()))
                     {
                        ///////////////////////////////////////////////////////////////////////////
                        // if we previously saw a not-equals or not-in, and now we see an equals //
                        // and the value from the EQUALS isn't in the not-in list                //
                        // then replace the not-equals with the equals                           //
                        // then just discard this equals                                         //
                        ///////////////////////////////////////////////////////////////////////////
                        if(other.getValues().contains(criteria.getValues().get(0)))
                        {
                           log.add("Contradicting NOT_EQUALS/NOT_IN and EQUALS");
                           fail = true;
                        }
                        else
                        {
                           other.setOperator(criteria.getOperator());
                           other.setValues(criteria.getValues());
                           iterator.remove();
                           didAnyGood = true;
                           log.add("Replace a not-equals or not-in superseded by an equals");
                           continue;
                        }
                     }
                     else if(EQUALS.equals(other.getOperator()) && (NOT_EQUALS.equals(criteria.getOperator()) || NOT_IN.equals(criteria.getOperator())))
                     {
                        /////////////////////////////////////////////////////////////////////////////
                        // if we previously saw an equals, and now we see a not-equals or a not-in //
                        // and the value from the EQUALS isn't in the not-in list                  //
                        // then just discard this not-equals                                       //
                        /////////////////////////////////////////////////////////////////////////////
                        if(criteria.getValues().contains(other.getValues().get(0)))
                        {
                           log.add("Contradicting NOT_EQUALS/NOT_IN and EQUALS");
                           fail = true;
                        }
                        else
                        {
                           iterator.remove();
                           didAnyGood = true;
                           log.add("Remove a redundant not-equals");
                           continue;
                        }
                     }
                     else if(NOT_EQUALS.equals(other.getOperator()) && IN.equals(criteria.getOperator()))
                     {
                        /////////////////////////////////////////////////////////////////////////////////////////////////////
                        // if we previously saw a not-equals, and now we see an IN                                         //
                        // then replace the not-equals with the IN (making sure the not-equals value isn't in the in-list) //
                        // then just discard this equals                                                                   //
                        /////////////////////////////////////////////////////////////////////////////////////////////////////
                        Serializable       notEqualsValue = other.getValues().get(0);
                        List<Serializable> inValues       = new ArrayList<>(criteria.getValues());
                        inValues.remove(notEqualsValue);
                        if(inValues.isEmpty())
                        {
                           ///////////////////////////////////////////////////////////////////////////////////
                           // if the only in-value was the not-equal value, then... i don't know, don't try //
                           ///////////////////////////////////////////////////////////////////////////////////
                           log.add("Contradicting IN and NOT_EQUAL");
                           fail = true;
                        }
                        else
                        {
                           //////////////////////////////////////////////////////////////////
                           // else, we can proceed by replacing the not-equals with the in //
                           //////////////////////////////////////////////////////////////////
                           other.setOperator(criteria.getOperator());
                           other.setValues(criteria.getValues());
                           iterator.remove();
                           didAnyGood = true;
                           log.add("Replace superseded not-equals (removing its value from in-list)");
                           continue;
                        }
                     }
                     else if(IN.equals(other.getOperator()) && NOT_EQUALS.equals(criteria.getOperator()))
                     {
                        //////////////////////////////////////////////////////////////////
                        // if we previously saw an in, and now we see a not-equals      //
                        // discard the not-equals (removing its value from the in-list) //
                        // then just discard this not-equals                            //
                        //////////////////////////////////////////////////////////////////
                        Serializable       notEqualsValue = criteria.getValues().get(0);
                        List<Serializable> inValues       = new ArrayList<>(other.getValues());
                        inValues.remove(notEqualsValue);
                        if(inValues.isEmpty())
                        {
                           ///////////////////////////////////////////////////////////////////////////////////
                           // if the only in-value was the not-equal value, then... i don't know, don't try //
                           ///////////////////////////////////////////////////////////////////////////////////
                           log.add("Contradicting IN and NOT_EQUAL");
                           fail = true;
                        }
                        else
                        {
                           //////////////////////////////////////////////////////////////////
                           // else, we can proceed by replacing the not-equals with the in //
                           //////////////////////////////////////////////////////////////////
                           iterator.remove();
                           didAnyGood = true;
                           log.add("Remove redundant not-equals (removing its value from in-list)");
                           continue;
                        }
                     }
                     else if(NOT_EQUALS.equals(other.getOperator()) && NOT_IN.equals(criteria.getOperator()))
                     {
                        /////////////////////////////////////////////////////////////////////////////////////////
                        // if we previously saw a not-equals, and now we see a not-in                          //
                        // we can change the not-equals to the not-in, and make sure it's value is in the list //
                        // then just discard this not-in                                                       //
                        /////////////////////////////////////////////////////////////////////////////////////////
                        Serializable originalNotEqualsValue = other.getValues().get(0);
                        other.setOperator(criteria.getOperator());
                        other.setValues(criteria.getValues());
                        if(!other.getValues().contains(originalNotEqualsValue))
                        {
                           other.getValues().add(originalNotEqualsValue);
                        }
                        iterator.remove();
                        didAnyGood = true;
                        log.add("Replace superseded not-equals with not-in");
                        continue;
                     }
                     else if(NOT_IN.equals(other.getOperator()) && NOT_EQUALS.equals(criteria.getOperator()))
                     {
                        ////////////////////////////////////////////////////////////////////////////////////////
                        // if we previously saw a not-in, and now we see a not-equals                         //
                        // we can discard this not-equals, and just make sure its value is in the not-in list //
                        ////////////////////////////////////////////////////////////////////////////////////////
                        Serializable originalNotEqualsValue = criteria.getValues().get(0);
                        if(!other.getValues().contains(originalNotEqualsValue))
                        {
                           other.getValues().add(originalNotEqualsValue);
                        }
                        iterator.remove();
                        didAnyGood = true;
                        log.add("Remove not-equals, absorbing into not-in");
                        continue;
                     }
                     else if(NOT_IN.equals(other.getOperator()) && NOT_IN.equals(criteria.getOperator()))
                     {
                        ////////////////////////////////////////////////////////////////
                        // for multiple not-ins, just merge their values (as a union) //
                        ////////////////////////////////////////////////////////////////
                        for(Serializable value : criteria.getValues())
                        {
                           if(!other.getValues().contains(value))
                           {
                              other.getValues().add(value);
                           }
                        }
                        iterator.remove();
                        didAnyGood = true;
                        log.add("Merging not-ins");
                        continue;
                     }
                     else if(IN.equals(other.getOperator()) && IN.equals(criteria.getOperator()))
                     {
                        ////////////////////////////////////////////////////////////////////////
                        // for multiple not-ins, just merge their values (as an intersection) //
                        ////////////////////////////////////////////////////////////////////////
                        Set<Serializable> otherValues    = new HashSet<>(other.getValues());
                        Set<Serializable> criteriaValues = new HashSet<>(criteria.getValues());
                        otherValues.retainAll(criteriaValues);
                        if(otherValues.isEmpty())
                        {
                           log.add("Contradicting IN lists (no values)");
                           fail = true;
                        }
                        else
                        {
                           other.setValues(new ArrayList<>(otherValues));
                           iterator.remove();
                           didAnyGood = true;
                           log.add("Merging not-ins");
                           continue;
                        }
                     }
                     else if(NOT_EQUALS.equals(other.getOperator()) && NOT_EQUALS.equals(criteria.getOperator()))
                     {
                        /////////////////////////////////////////////////////////////////////////////////////
                        // if we have 2 not-equals, we can merge them in a not-in                          //
                        // we can assume their values are different, else they'd have been equals up above //
                        /////////////////////////////////////////////////////////////////////////////////////
                        other.setOperator(NOT_IN);
                        other.setValues(new ArrayList<>(List.of(other.getValues().get(0), criteria.getValues().get(0))));
                        iterator.remove();
                        didAnyGood = true;
                        log.add("Merge two not-equals as not-in");
                        continue;
                     }
                     else if(IN.equals(other.getOperator()) && IS_NOT_BLANK.equals(criteria.getOperator()))
                     {
                        //////////////////////////////////////////////////////////////////////////
                        // for an IN and IS_NOT_BLANK, remove the IS_NOT_BLANK - it's redundant //
                        //////////////////////////////////////////////////////////////////////////
                        iterator.remove();
                        didAnyGood = true;
                        log.add("Removing redundant is-not-blank");
                        continue;
                     }
                     else if(IS_NOT_BLANK.equals(other.getOperator()) && IN.equals(criteria.getOperator()))
                     {
                        //////////////////////////////////////////////////////////////////////////
                        // for an IN and IS_NOT_BLANK, remove the IS_NOT_BLANK - it's redundant //
                        //////////////////////////////////////////////////////////////////////////
                        other.setOperator(IN);
                        other.setValues(new ArrayList<>(criteria.getValues()));
                        iterator.remove();
                        didAnyGood = true;
                        log.add("Removing redundant is-not-blank");
                        continue;
                     }
                     else
                     {
                        log.add("Fail because unhandled operator pair");
                        fail = true;
                     }
                  }
                  else
                  {
                     log.add("Fail because > 1 other or operator: OR");
                     fail = true;
                  }
               }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if we reach here (e.g., no continue), then assuming we didn't remove the criteria, add it to the listing hash. //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            criteriaByFieldName.add(criteria.getFieldName(), criteria);
         }

         ///////////////////////////
         // log based on booleans //
         ///////////////////////////
         if(fail && didAnyGood)
         {
            LOG.info("Partially unsuccessful dedupe of filter", logPair("original", filter), logPair("deduped", rs), logPair("log", log));
         }
         else if(fail)
         {
            LOG.info("Unsuccessful dedupe of filter", logPair("filter", filter), logPair("log", log));
         }
         else if(didAnyGood)
         {
            LOG.debug("Successful dedupe of filter", logPair("original", filter), logPair("deduped", rs), logPair("log", log));
         }
         else
         {
            LOG.debug("No duplicates in filter, so nothing to dedupe", logPair("original", filter));
         }

         return rs;
      }
      catch(Exception e)
      {
         LOG.warn("Error de-duping filter", e, logPair("filter", filter));
         return (filter.clone());
      }
   }

}
