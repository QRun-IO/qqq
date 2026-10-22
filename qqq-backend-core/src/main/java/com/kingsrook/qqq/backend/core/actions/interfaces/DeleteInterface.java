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

package com.kingsrook.qqq.backend.core.actions.interfaces;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;


/*******************************************************************************
 ** Interface for the Delete action.
 **
 *******************************************************************************/
public interface DeleteInterface
{
   /*******************************************************************************
    **
    *******************************************************************************/
   DeleteOutput execute(DeleteInput deleteInput) throws QException;

   /*******************************************************************************
    ** Specify whether this particular module's delete action natively supports
    ** receiving a queryFilter as input (e.g., SQL does).  If the module doesn't
    ** support a query filter, then the qqq framework (DeleteAction) will, if it
    ** receives a queryFilter in its input, it will execute the query, and pass
    ** the list of primary keys down into the module's delete implementation.
    *******************************************************************************/
   default boolean supportsQueryFilterInput()
   {
      return (false);
   }

   /*******************************************************************************
    ** Specify whether this particular module's delete action can & should fetch
    ** records before deleting them, e.g., for audits or "not-found-checks"
    *******************************************************************************/
   default boolean supportsPreFetchQuery()
   {
      return (true);
   }

}
