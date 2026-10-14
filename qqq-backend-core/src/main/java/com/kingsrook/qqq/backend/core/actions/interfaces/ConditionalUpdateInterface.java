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


package com.kingsrook.qqq.backend.core.actions.interfaces;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;


/*******************************************************************************
 ** Backend capability CONDITIONAL_UPDATE: an atomic compare-and-set update.
 **
 ** Sets the given values on every row of one table that matches the filter, as
 ** ONE atomic statement (for an RDBMS: UPDATE t SET ... WHERE ...), and returns
 ** how many rows changed. Callers use it as a compare-and-set: put the expected
 ** current values (e.g., id and a version counter) in the filter, and check the
 ** count. Two concurrent callers with the same expectation can never both win.
 **
 ** This is a low-level store operation for kernels such as durable jobs: it does
 ** not run audits, automations, record security or field validation. Only the
 ** table's own fields may appear in the filter (no joins).
 **
 ** The backend also exposes its clock, so callers that compare times across pods
 ** (leases) use one clock instead of each pod's own.
 *******************************************************************************/
public interface ConditionalUpdateInterface
{

   /*******************************************************************************
    ** Run the update; return the number of rows updated.
    *******************************************************************************/
   int execute(ConditionalUpdateInput input) throws QException;


   /*******************************************************************************
    ** The backend's current time (for an RDBMS, the database clock).
    *******************************************************************************/
   Instant databaseNow(QBackendMetaData backend) throws QException;

}
