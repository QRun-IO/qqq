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

package com.kingsrook.qqq.backend.core.model.metadata.possiblevalues;


import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaMissingInputValueBehavior;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.FilterUseCase;


/*******************************************************************************
 ** FilterUseCase implementation for the ways that possible value searches
 ** are performed, and where we want to have different behaviors for criteria
 ** that are missing an input value.  That is, either for a:
 **
 ** - FORM - e.g., creating a new record, or in a process - where we want a
 ** missing filter value to basically block you from selecting a value in the
 ** PVS field - e.g., you must enter some other foreign-key value before choosing
 ** from this possible value - at least that's the use-case we know of now.
 **
 ** - FILTER - e.g., a query screen - where there isn't really quite the same
 ** scenario of choosing that foreign-key value first - so, such a PVS should
 ** list all its values (e.g., a criteria missing an input value should be
 ** removed from the filter).
 *******************************************************************************/
public enum PossibleValueSearchFilterUseCase implements FilterUseCase
{
   FORM(CriteriaMissingInputValueBehavior.MAKE_NO_MATCHES),
   FILTER(CriteriaMissingInputValueBehavior.REMOVE_FROM_FILTER);


   private final CriteriaMissingInputValueBehavior defaultCriteriaMissingInputValueBehavior;



   /***************************************************************************
    **
    ***************************************************************************/
   PossibleValueSearchFilterUseCase(CriteriaMissingInputValueBehavior defaultCriteriaMissingInputValueBehavior)
   {
      this.defaultCriteriaMissingInputValueBehavior = defaultCriteriaMissingInputValueBehavior;
   }



   /*******************************************************************************
    ** Getter for defaultCriteriaMissingInputValueBehavior
    **
    *******************************************************************************/
   public CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior()
   {
      return defaultCriteriaMissingInputValueBehavior;
   }
}
