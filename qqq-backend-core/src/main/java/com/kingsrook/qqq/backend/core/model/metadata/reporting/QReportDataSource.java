/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.reporting;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Meta-data definition of a source of data for a report (e.g., a table and query
 ** filter or custom-code reference).
 **
 ** Runs in 3 modes:
 **
 ** - If a customRecordSource is specified, then that code is executed to get the records.
 ** - else, if a sourceTable is specified, then the corresponding queryFilter
 ** (optionally along with queryJoins and queryInputCustomizer) is used.
 ** - else a staticDataSupplier is used.
 *******************************************************************************/
public class QReportDataSource implements Cloneable
{
   private String name;

   private String          sourceTable;
   private QQueryFilter    queryFilter;
   private List<QueryJoin> queryJoins = null;


   private QCodeReference queryInputCustomizer;
   private QCodeReference staticDataSupplier;
   private QCodeReference customRecordSource;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QReportDataSource clone()
   {
      try
      {
         QReportDataSource clone = (QReportDataSource) super.clone();
         if(queryFilter != null)
         {
            clone.queryFilter = queryFilter.clone();
         }

         if(queryJoins != null)
         {
            clone.queryJoins = new ArrayList<>();
            for(QueryJoin join : queryJoins)
            {
               queryJoins.add(join.clone());
            }
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public QReportDataSource withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceTable
    **
    *******************************************************************************/
   public String getSourceTable()
   {
      return sourceTable;
   }



   /*******************************************************************************
    ** Setter for sourceTable
    **
    *******************************************************************************/
   public void setSourceTable(String sourceTable)
   {
      this.sourceTable = sourceTable;
   }



   /*******************************************************************************
    ** Fluent setter for sourceTable
    **
    *******************************************************************************/
   public QReportDataSource withSourceTable(String sourceTable)
   {
      this.sourceTable = sourceTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryFilter
    **
    *******************************************************************************/
   public QQueryFilter getQueryFilter()
   {
      return queryFilter;
   }



   /*******************************************************************************
    ** Setter for queryFilter
    **
    *******************************************************************************/
   public void setQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
   }



   /*******************************************************************************
    ** Fluent setter for queryFilter
    **
    *******************************************************************************/
   public QReportDataSource withQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for staticDataSupplier
    **
    *******************************************************************************/
   public QCodeReference getStaticDataSupplier()
   {
      return staticDataSupplier;
   }



   /*******************************************************************************
    ** Setter for staticDataSupplier
    **
    *******************************************************************************/
   public void setStaticDataSupplier(QCodeReference staticDataSupplier)
   {
      this.staticDataSupplier = staticDataSupplier;
   }



   /*******************************************************************************
    ** Fluent setter for staticDataSupplier
    **
    *******************************************************************************/
   public QReportDataSource withStaticDataSupplier(QCodeReference staticDataSupplier)
   {
      this.staticDataSupplier = staticDataSupplier;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryJoins
    **
    *******************************************************************************/
   public List<QueryJoin> getQueryJoins()
   {
      return queryJoins;
   }



   /*******************************************************************************
    ** Setter for queryJoins
    **
    *******************************************************************************/
   public void setQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    **
    *******************************************************************************/
   public QReportDataSource withQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    **
    *******************************************************************************/
   public QReportDataSource withQueryJoin(QueryJoin queryJoin)
   {
      if(this.queryJoins == null)
      {
         this.queryJoins = new ArrayList<>();
      }
      this.queryJoins.add(queryJoin);
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryInputCustomizer
    **
    *******************************************************************************/
   public QCodeReference getQueryInputCustomizer()
   {
      return queryInputCustomizer;
   }



   /*******************************************************************************
    ** Setter for queryInputCustomizer
    **
    *******************************************************************************/
   public void setQueryInputCustomizer(QCodeReference queryInputCustomizer)
   {
      this.queryInputCustomizer = queryInputCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for queryInputCustomizer
    **
    *******************************************************************************/
   public QReportDataSource withQueryInputCustomizer(QCodeReference queryInputCustomizer)
   {
      this.queryInputCustomizer = queryInputCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for customRecordSource
    *******************************************************************************/
   public QCodeReference getCustomRecordSource()
   {
      return (this.customRecordSource);
   }



   /*******************************************************************************
    ** Setter for customRecordSource
    *******************************************************************************/
   public void setCustomRecordSource(QCodeReference customRecordSource)
   {
      this.customRecordSource = customRecordSource;
   }



   /*******************************************************************************
    ** Fluent setter for customRecordSource
    *******************************************************************************/
   public QReportDataSource withCustomRecordSource(QCodeReference customRecordSource)
   {
      this.customRecordSource = customRecordSource;
      return (this);
   }

}
