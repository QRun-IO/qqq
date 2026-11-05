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


package com.kingsrook.qqq.backend.core.jobs;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Backend transaction capability for the Jobs-owned atomic admission boundary.
 ** Implementations must qualify the actual connection, synchronous durable storage
 ** and owned schema, then lock the pre-seeded admission row on that same connection.
 ** No separate connection, pooled-setting mutation, or runtime DDL is allowed.
 *******************************************************************************/
public interface TransactionalJobAdmission
{
   /*******************************************************************************
    ** Lock admission for this transaction; return its stable opaque backend identity.
    ** The lock is retained until the source transaction commits or rolls back.
    *******************************************************************************/
   String lockJobAdmission(QTableMetaData sourceTable) throws QException;



   /*******************************************************************************
    ** Read database time using this exact transaction's connection.
    *******************************************************************************/
   Instant jobDatabaseNow() throws QException;



   /*******************************************************************************
    ** Lock a job's existing chunks before its job row, after admission is held.
    ** Control operations use the same chunk-before-job order as progress writers.
    *******************************************************************************/
   void lockJobControlRows(Integer jobId) throws QException;
}
