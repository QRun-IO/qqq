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


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Interface for the Query action.
 **
 *******************************************************************************/
public interface QueryInterface extends BaseQueryInterface
{
   /*******************************************************************************
    **
    *******************************************************************************/
   QueryOutput execute(QueryInput queryInput) throws QException;



   /*******************************************************************************
    ** Native lookup whose returned identities will drive writes. Backends may
    ** reject identities that cannot be preserved through their public record form.
    *******************************************************************************/
   default QueryOutput executeForDml(QueryInput queryInput) throws QException
   {
      return execute(queryInput);
   }



   /*******************************************************************************
    ** Native structural lookup for associated-write permission preflight.
    *******************************************************************************/
   default List<Serializable> findAssociatedPrimaryKeys(AssociatedRecordDiscovery.Input input) throws QException
   {
      throw new QException("Backend does not implement structural association discovery");
   }



   /*******************************************************************************
    ** Native, exact-key projection of declared parent association values.
    *******************************************************************************/
   default List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
   {
      throw new QException("Backend does not implement stored association-value lookup");
   }



   /*******************************************************************************
    ** Native, schema-constrained unique-key validation, independent of read visibility.
    *******************************************************************************/
   default List<QRecord> lookupUniqueKey(UniqueKeyLookup.Input input) throws QException
   {
      throw new QException("Backend does not implement native unique-key validation");
   }

}
