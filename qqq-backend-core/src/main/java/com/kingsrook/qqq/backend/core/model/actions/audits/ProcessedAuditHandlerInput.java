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

package com.kingsrook.qqq.backend.core.model.actions.audits;


import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Input for processed audit handlers containing AuditSingleInput objects.
 ** This provides the same data that gets stored in the audit tables.
 *******************************************************************************/
public class ProcessedAuditHandlerInput implements Serializable
{
   private List<AuditSingleInput> auditSingleInputs;
   private Instant                timestamp;
   private QSession               session;
   private String                 sourceType;



   /*******************************************************************************
    ** Getter for auditSingleInputs
    *******************************************************************************/
   public List<AuditSingleInput> getAuditSingleInputs()
   {
      return (this.auditSingleInputs);
   }



   /*******************************************************************************
    ** Setter for auditSingleInputs
    *******************************************************************************/
   public void setAuditSingleInputs(List<AuditSingleInput> auditSingleInputs)
   {
      this.auditSingleInputs = auditSingleInputs;
   }



   /*******************************************************************************
    ** Fluent setter for auditSingleInputs
    **
    ** @param auditSingleInputs list of individual audit records to be processed
    *******************************************************************************/
   public ProcessedAuditHandlerInput withAuditSingleInputs(List<AuditSingleInput> auditSingleInputs)
   {
      this.auditSingleInputs = auditSingleInputs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timestamp
    *******************************************************************************/
   public Instant getTimestamp()
   {
      return (this.timestamp);
   }



   /*******************************************************************************
    ** Setter for timestamp
    *******************************************************************************/
   public void setTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
   }



   /*******************************************************************************
    ** Fluent setter for timestamp
    **
    ** @param timestamp when the audit event occurred
    *******************************************************************************/
   public ProcessedAuditHandlerInput withTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for session
    *******************************************************************************/
   public QSession getSession()
   {
      return (this.session);
   }



   /*******************************************************************************
    ** Setter for session
    *******************************************************************************/
   public void setSession(QSession session)
   {
      this.session = session;
   }



   /*******************************************************************************
    ** Fluent setter for session
    **
    ** @param session the user session associated with the audit
    *******************************************************************************/
   public ProcessedAuditHandlerInput withSession(QSession session)
   {
      this.session = session;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceType
    *******************************************************************************/
   public String getSourceType()
   {
      return (this.sourceType);
   }



   /*******************************************************************************
    ** Setter for sourceType
    *******************************************************************************/
   public void setSourceType(String sourceType)
   {
      this.sourceType = sourceType;
   }



   /*******************************************************************************
    ** Fluent setter for sourceType
    **
    ** @param sourceType identifier for the type of source that triggered the audit
    *******************************************************************************/
   public ProcessedAuditHandlerInput withSourceType(String sourceType)
   {
      this.sourceType = sourceType;
      return (this);
   }

}
