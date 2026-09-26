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

package com.kingsrook.qqq.backend.core.actions.audits;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditHandlerInput;


/*******************************************************************************
 ** Handler interface for receiving raw DML audit events with full record snapshots.
 ** This is called by DMLAuditAction after audits are processed.
 ** Receives complete old and new QRecords for HIPAA/WORM compliance.
 *******************************************************************************/
public interface DMLAuditHandlerInterface extends AuditHandlerInterface
{

   /***************************************************************************
    ** Handle a DML audit event with full record data.
    **
    ** @param input contains the table, DML type, new records, and old records
    ** @throws QException if processing fails
    ***************************************************************************/
   void handleDMLAudit(DMLAuditHandlerInput input) throws QException;

}
