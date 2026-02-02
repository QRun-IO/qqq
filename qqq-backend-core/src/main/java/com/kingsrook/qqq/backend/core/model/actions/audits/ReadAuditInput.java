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
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Input for the ReadAuditAction, describing which records were read and how.
 *******************************************************************************/
public class ReadAuditInput implements Serializable
{
   private String        tableName;
   private ReadType      readType;
   private List<QRecord> records;
   private Integer       resultCount;
   private QQueryFilter  queryFilter;



   /*******************************************************************************
    ** Type of read operation being audited.
    *******************************************************************************/
   public enum ReadType
   {
      GET,
      QUERY
   }



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
   public ReadAuditInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for readType
    *******************************************************************************/
   public ReadType getReadType()
   {
      return (this.readType);
   }



   /*******************************************************************************
    ** Setter for readType
    *******************************************************************************/
   public void setReadType(ReadType readType)
   {
      this.readType = readType;
   }



   /*******************************************************************************
    ** Fluent setter for readType
    *******************************************************************************/
   public ReadAuditInput withReadType(ReadType readType)
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
   public ReadAuditInput withRecords(List<QRecord> records)
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
   public ReadAuditInput withResultCount(Integer resultCount)
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
   public ReadAuditInput withQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
      return (this);
   }

}
