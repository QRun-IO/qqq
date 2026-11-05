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

package com.kingsrook.qqq.backend.core.actions.tables.listeners;


import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Host opt-in to a listener whose successful same-transaction work is required
 ** for the selected native mutations. The obligation exists before code loading.
 ** This does not make arbitrary listener side effects transactional.
 *******************************************************************************/
public final class TransactionalRecordChangeMetaData extends QSupplementalTableMetaData
{
   public static final String TYPE = "transactionalRecordChanges";
   private final QCodeReference listener;
   private final Set<RecordChangeType> events;



   /*******************************************************************************
    ** Define one host-owned listener and its required event phases.
    *******************************************************************************/
   public TransactionalRecordChangeMetaData(QCodeReference listener, Set<RecordChangeType> events)
   {
      this.listener = Objects.requireNonNull(listener);
      this.events = Set.copyOf(events);
      if(this.events.isEmpty())
      {
         throw (new IllegalArgumentException("Required record listener events must not be empty"));
      }
   }



   /*******************************************************************************
    ** Read a typed declaration, refusing an unrecognized marker rather than
    ** silently disabling a required listener.
    *******************************************************************************/
   public static TransactionalRecordChangeMetaData of(QTableMetaData table) throws QException
   {
      QSupplementalTableMetaData value = table == null ? null : table.getSupplementalMetaData(TYPE);
      if(value == null)
      {
         return (null);
      }
      if(!(value instanceof TransactionalRecordChangeMetaData required))
      {
         throw (new QException("Unrecognized required transactional record listener metadata"));
      }
      return (required);
   }



   /*******************************************************************************
    ** Supplemental metadata identity.
    *******************************************************************************/
   @Override
   public String getType()
   {
      return (TYPE);
   }



   /*******************************************************************************
    ** The fixed host code reference, never selected from a request payload.
    *******************************************************************************/
   public QCodeReference getListener()
   {
      return (listener);
   }



   /*******************************************************************************
    ** Whether this mutation requires the listener.
    *******************************************************************************/
   public boolean requires(RecordChangeType event)
   {
      return (events.contains(event));
   }
}
