/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
