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

package com.kingsrook.qqq.backend.core.processes.tracing;


/*******************************************************************************
 ** Specialization of process tracer message, to indicate a 'key record' that was
 ** used as an input or trigger to a process.
 *******************************************************************************/
public class ProcessTracerKeyRecordMessage extends ProcessTracerMessage
{
   private final String  tableName;
   private final Integer recordId;



   /***************************************************************************
    **
    ***************************************************************************/
   public ProcessTracerKeyRecordMessage(String tableName, Integer recordId)
   {
      super("Process Key Record is " + tableName + " " + recordId);
      this.tableName = tableName;
      this.recordId = recordId;
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Getter for recordId
    *******************************************************************************/
   public Integer getRecordId()
   {
      return (this.recordId);
   }

}
