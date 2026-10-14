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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Session-aware policy, reveal grants and value-free access notifications.
 *******************************************************************************/
public interface FieldAccessResolverInterface
{
   /*******************************************************************************
    ** Resolve using the current QContext session.
    *******************************************************************************/
   FieldAccessDecision resolve(QTableMetaData table, QFieldMetaData field);



   /*******************************************************************************
    ** Whether this record and field have an active reveal grant.
    *******************************************************************************/
   default boolean isRevealActive(String tableName, String fieldName, Serializable primaryKey)
   {
      return (false);
   }



   /*******************************************************************************
    ** Stable identity of effective access, excluding reveal grants.
    *******************************************************************************/
   default String getAccessFingerprint()
   {
      return ("");
   }



   /*******************************************************************************
    ** Receive an audit notification; failures never prevent enforcement.
    *******************************************************************************/
   default void onAccess(FieldAccessEvent event)
   {

   }



   /*******************************************************************************
    ** Refuse an exact-match search before execution; the exception message is user-facing.
    *******************************************************************************/
   default void checkExactMatch(FieldAccessEvent event) throws QPermissionDeniedException
   {

   }



   /*******************************************************************************
    ** One-way fingerprint of a search literal. Raw values never enter access events.
    *******************************************************************************/
   default String fingerprintSearchValue(Serializable value)
   {
      return (null);
   }
}
