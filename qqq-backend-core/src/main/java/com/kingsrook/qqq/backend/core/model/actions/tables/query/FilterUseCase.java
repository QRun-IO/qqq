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


/*******************************************************************************
 ** Interface where we can associate behaviors with various use cases for
 ** QQueryFilters - the original being, how to handle (in the interpretValues
 ** method) how to handle missing input values.
 **
 ** Includes a default implementation, with a default behavior - which is to
 ** throw an exception upon missing criteria variable values.
 *******************************************************************************/
public interface FilterUseCase
{
   FilterUseCase DEFAULT = new DefaultFilterUseCase();

   /***************************************************************************
    **
    ***************************************************************************/
   CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior();


   /***************************************************************************
    **
    ***************************************************************************/
   class DefaultFilterUseCase implements FilterUseCase
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior()
      {
         return CriteriaMissingInputValueBehavior.THROW_EXCEPTION;
      }
   }
}
