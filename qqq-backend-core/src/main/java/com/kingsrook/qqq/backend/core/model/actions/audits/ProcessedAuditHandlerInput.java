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
