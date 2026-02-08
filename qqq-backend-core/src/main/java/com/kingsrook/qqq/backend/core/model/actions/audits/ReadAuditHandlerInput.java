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
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Input for read audit handlers, providing context about viewed records.
 *******************************************************************************/
public class ReadAuditHandlerInput implements Serializable
{
   private String        tableName;
   private ReadAuditInput.ReadType readType;
   private List<QRecord> records;
   private Integer       resultCount;
   private QQueryFilter  queryFilter;
   private Instant       timestamp;
   private QSession      session;



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public ReadAuditHandlerInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for readType
    *******************************************************************************/
   public ReadAuditInput.ReadType getReadType()
   {
      return (this.readType);
   }



   /*******************************************************************************
    ** Setter for readType
    *******************************************************************************/
   public void setReadType(ReadAuditInput.ReadType readType)
   {
      this.readType = readType;
   }



   /*******************************************************************************
    ** Fluent setter for readType
    *******************************************************************************/
   public ReadAuditHandlerInput withReadType(ReadAuditInput.ReadType readType)
   {
      this.readType = readType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<QRecord> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    ** Setter for records
    *******************************************************************************/
   public void setRecords(List<QRecord> records)
   {
      this.records = records;
   }



   /*******************************************************************************
    ** Fluent setter for records
    *******************************************************************************/
   public ReadAuditHandlerInput withRecords(List<QRecord> records)
   {
      this.records = records;
      return (this);
   }



   /*******************************************************************************
    ** Getter for resultCount
    *******************************************************************************/
   public Integer getResultCount()
   {
      return (this.resultCount);
   }



   /*******************************************************************************
    ** Setter for resultCount
    *******************************************************************************/
   public void setResultCount(Integer resultCount)
   {
      this.resultCount = resultCount;
   }



   /*******************************************************************************
    ** Fluent setter for resultCount
    *******************************************************************************/
   public ReadAuditHandlerInput withResultCount(Integer resultCount)
   {
      this.resultCount = resultCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryFilter
    *******************************************************************************/
   public QQueryFilter getQueryFilter()
   {
      return (this.queryFilter);
   }



   /*******************************************************************************
    ** Setter for queryFilter
    *******************************************************************************/
   public void setQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
   }



   /*******************************************************************************
    ** Fluent setter for queryFilter
    *******************************************************************************/
   public ReadAuditHandlerInput withQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
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
    *******************************************************************************/
   public ReadAuditHandlerInput withTimestamp(Instant timestamp)
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
    *******************************************************************************/
   public ReadAuditHandlerInput withSession(QSession session)
   {
      this.session = session;
      return (this);
   }

}
