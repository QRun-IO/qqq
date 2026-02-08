/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.audits;


import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;


/*******************************************************************************
 **
 *******************************************************************************/
public class QAuditRules implements QMetaDataObject, Cloneable
{
   private AuditLevel     auditLevel;
   private ReadAuditLevel readAuditLevel;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QAuditRules defaultInstanceLevelNone()
   {
      return (new QAuditRules()
         .withAuditLevel(AuditLevel.NONE));
   }



   /*******************************************************************************
    ** Getter for auditLevel
    *******************************************************************************/
   public AuditLevel getAuditLevel()
   {
      return (this.auditLevel);
   }



   /*******************************************************************************
    ** Setter for auditLevel
    *******************************************************************************/
   public void setAuditLevel(AuditLevel auditLevel)
   {
      this.auditLevel = auditLevel;
   }



   /*******************************************************************************
    ** Fluent setter for auditLevel
    *******************************************************************************/
   public QAuditRules withAuditLevel(AuditLevel auditLevel)
   {
      this.auditLevel = auditLevel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for readAuditLevel
    *******************************************************************************/
   public ReadAuditLevel getReadAuditLevel()
   {
      return (this.readAuditLevel);
   }



   /*******************************************************************************
    ** Setter for readAuditLevel
    *******************************************************************************/
   public void setReadAuditLevel(ReadAuditLevel readAuditLevel)
   {
      this.readAuditLevel = readAuditLevel;
   }



   /*******************************************************************************
    ** Fluent setter for readAuditLevel
    *******************************************************************************/
   public QAuditRules withReadAuditLevel(ReadAuditLevel readAuditLevel)
   {
      this.readAuditLevel = readAuditLevel;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public QAuditRules clone()
   {
      try
      {
         QAuditRules clone = (QAuditRules) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
