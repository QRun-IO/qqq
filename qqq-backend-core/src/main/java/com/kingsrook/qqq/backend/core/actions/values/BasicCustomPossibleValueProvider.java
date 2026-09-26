/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;


/*******************************************************************************
 ** Basic implementation of a possible value provider, for where there's a limited
 ** set of possible source objects - so you just have to define how to make one
 ** PV from a source object, how to list all of the source objects, and how to
 ** look up a PV from an id.
 *******************************************************************************/
public abstract class BasicCustomPossibleValueProvider<S, ID extends Serializable> implements QCustomPossibleValueProvider<ID>
{

   /***************************************************************************
    **
    ***************************************************************************/
   protected abstract QPossibleValue<ID> makePossibleValue(S sourceObject);

   /***************************************************************************
    **
    ***************************************************************************/
   protected abstract S getSourceObject(Serializable id) throws QException;

   /***************************************************************************
    **
    ***************************************************************************/
   protected abstract List<S> getAllSourceObjects() throws QException;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QPossibleValue<ID> getPossibleValue(Serializable idValue) throws QException
   {
      S sourceObject = getSourceObject(idValue);
      if(sourceObject == null)
      {
         return (null);
      }

      return makePossibleValue(sourceObject);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<QPossibleValue<ID>> search(SearchPossibleValueSourceInput input) throws QException
   {
      List<QPossibleValue<ID>> allPossibleValues = new ArrayList<>();
      List<S>                  allSourceObjects  = getAllSourceObjects();
      for(S sourceObject : allSourceObjects)
      {
         allPossibleValues.add(makePossibleValue(sourceObject));
      }

      return completeCustomPVSSearch(input, allPossibleValues);
   }
}
