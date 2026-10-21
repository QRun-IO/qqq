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

package com.kingsrook.qqq.backend.core.modules.backend;


/*******************************************************************************
 ** Result of resyncing one table's key sequence, as returned by
 ** QBackendModuleInterface.resyncKeySequences.
 **
 ** The status says what happened, and newValue is the sequence's next-value
 ** position after a resync (the value the next generated key will have), when
 ** a resync took place.
 *******************************************************************************/
public class SequenceResyncResult
{
   private final String tableName;
   private final Status status;
   private final Long   newValue;



   /*******************************************************************************
    ** What happened for a table.
    *******************************************************************************/
   public enum Status
   {
      /////////////////////////////////////////////////////////////////////////////
      // the sequence was moved so that the next generated key is max(key) + 1   //
      /////////////////////////////////////////////////////////////////////////////
      RESYNCED,

      ///////////////////////////////////////////////////////////////////////////
      // the backend keeps its own key counter in step with explicit inserts   //
      ///////////////////////////////////////////////////////////////////////////
      NOT_NEEDED,

      ///////////////////////////////////////////////////////////////////////////
      // the table (or backend) has no key sequence that could be resynced     //
      ///////////////////////////////////////////////////////////////////////////
      NOT_APPLICABLE
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public SequenceResyncResult(String tableName, Status status, Long newValue)
   {
      this.tableName = tableName;
      this.status = status;
      this.newValue = newValue;
   }



   /*******************************************************************************
    ** Getter for tableName (the QQQ table name)
    *******************************************************************************/
   public String getTableName()
   {
      return (tableName);
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public Status getStatus()
   {
      return (status);
   }



   /*******************************************************************************
    ** Getter for newValue - the value the next generated key will have; null
    ** unless the status is RESYNCED.
    *******************************************************************************/
   public Long getNewValue()
   {
      return (newValue);
   }



   /*******************************************************************************
    ** toString
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("SequenceResyncResult{table=" + tableName + ", status=" + status + ", newValue=" + newValue + "}");
   }
}
