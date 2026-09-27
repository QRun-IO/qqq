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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


/***************************************************************************
 ** Possible behaviors for doing interpretValues on a filter, and a criteria
 ** has a variable value (either as a string-that-looks-like-a-variable,
 ** as in ${input.foreignId} for a PVS filter, or a FilterVariableExpression),
 ** and a value for that variable isn't available.
 **
 ** Used in conjunction with FilterUseCase and its implementations, e.g.,
 ** PossibleValueSearchFilterUseCase.
 ***************************************************************************/
public enum CriteriaMissingInputValueBehavior
{
   //////////////////////////////////////////////////////////////////////
   // this was the original behavior, before we added this enum.  but, //
   // it doesn't ever seem entirely valid, and isn't currently used.   //
   //////////////////////////////////////////////////////////////////////
   INTERPRET_AS_NULL_VALUE,

   //////////////////////////////////////////////////////////////////////////
   // make the criteria behave as though it's not in the filter at all.    //
   // effectively by changing its operator to TRUE, so it always matches.  //
   // original intended use is for possible-values on  query screens,      //
   // where a foreign-id isn't present, so we want to show all PV options. //
   //////////////////////////////////////////////////////////////////////////
   REMOVE_FROM_FILTER,

   //////////////////////////////////////////////////////////////////////////////////////
   // make the criteria such that it makes no rows ever match.                         //
   // e.g., changes it to a FALSE.  I suppose, within an OR, that might                //
   // not be powerful enough...  but, it solves the immediate use-case in              //
   // front of us, which is forms, where a PV field should show no values              //
   // until a foreign key field has a value.                                           //
   // Note that this use-case used to have the same end-effect by such                 //
   // variables being interpreted as nulls - but this approach feels more intentional. //
   //////////////////////////////////////////////////////////////////////////////////////
   MAKE_NO_MATCHES,

   ///////////////////////////////////////////////////////////////////////////////////////////
   // throw an exception if a value isn't available.  This is the overall default,          //
   // and originally was what we did for FilterVariableExpressions, e.g., for saved reports //
   ///////////////////////////////////////////////////////////////////////////////////////////
   THROW_EXCEPTION
}
