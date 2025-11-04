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

package com.kingsrook.qqq.backend.core.model.querystats;


import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QAssociation;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QIgnore;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.tables.QQQTable;


/*******************************************************************************
 ** QRecord Entity for QueryStat table
 *******************************************************************************/
public class QueryStat extends QRecordEntity
{
   public static final String TABLE_NAME = "queryStat";

   @QField(isEditable = false)
   private Integer id;

   @QField()
   private Instant startTimestamp;

   @QField()
   private Instant firstResultTimestamp;

   @QField()
   private Integer firstResultMillis;

   @QField(label = "Table", possibleValueSourceName = QQQTable.TABLE_NAME)
   private Integer qqqTableId;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String action;

   @QField(maxLength = 36, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String sessionId;

   @QField(maxLength = 64 * 1024 - 1, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String queryText;

   @QAssociation(name = "queryStatJoinTables")
   private List<QueryStatJoinTable> queryStatJoinTableList;

   @QAssociation(name = "queryStatCriteriaFields")
   private List<QueryStatCriteriaField> queryStatCriteriaFieldList;

   @QAssociation(name = "queryStatOrderByFields")
   private List<QueryStatOrderByField> queryStatOrderByFieldList;

   ///////////////////////////////////////////////////////////
   // non-persistent fields - used to help build the record //
   // and available to QueryStatConsumers                   //
   ///////////////////////////////////////////////////////////
   @QIgnore
   private String tableName;

   @QIgnore
   private String backendAction;

   @QIgnore
   private Integer recordCount;

   private Set<String>  joinTableNames;
   private QQueryFilter queryFilter;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public QueryStat()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public QueryStat(QRecord record)
   {
      populateFromQRecord(record);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public QueryStat withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startTimestamp
    *******************************************************************************/
   public Instant getStartTimestamp()
   {
      return (this.startTimestamp);
   }



   /*******************************************************************************
    ** Setter for startTimestamp
    *******************************************************************************/
   public void setStartTimestamp(Instant startTimestamp)
   {
      this.startTimestamp = startTimestamp;
   }



   /*******************************************************************************
    ** Fluent setter for startTimestamp
    *******************************************************************************/
   public QueryStat withStartTimestamp(Instant startTimestamp)
   {
      this.startTimestamp = startTimestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for firstResultTimestamp
    *******************************************************************************/
   public Instant getFirstResultTimestamp()
   {
      return (this.firstResultTimestamp);
   }



   /*******************************************************************************
    ** Setter for firstResultTimestamp
    *******************************************************************************/
   public void setFirstResultTimestamp(Instant firstResultTimestamp)
   {
      this.firstResultTimestamp = firstResultTimestamp;
   }



   /*******************************************************************************
    ** Fluent setter for firstResultTimestamp
    *******************************************************************************/
   public QueryStat withFirstResultTimestamp(Instant firstResultTimestamp)
   {
      this.firstResultTimestamp = firstResultTimestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for firstResultMillis
    *******************************************************************************/
   public Integer getFirstResultMillis()
   {
      return (this.firstResultMillis);
   }



   /*******************************************************************************
    ** Setter for firstResultMillis
    *******************************************************************************/
   public void setFirstResultMillis(Integer firstResultMillis)
   {
      this.firstResultMillis = firstResultMillis;
   }



   /*******************************************************************************
    ** Fluent setter for firstResultMillis
    *******************************************************************************/
   public QueryStat withFirstResultMillis(Integer firstResultMillis)
   {
      this.firstResultMillis = firstResultMillis;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryText
    *******************************************************************************/
   public String getQueryText()
   {
      return (this.queryText);
   }



   /*******************************************************************************
    ** Setter for queryText
    *******************************************************************************/
   public void setQueryText(String queryText)
   {
      this.queryText = queryText;
   }



   /*******************************************************************************
    ** Fluent setter for queryText
    *******************************************************************************/
   public QueryStat withQueryText(String queryText)
   {
      this.queryText = queryText;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryStatJoinTableList
    *******************************************************************************/
   public List<QueryStatJoinTable> getQueryStatJoinTableList()
   {
      return (this.queryStatJoinTableList);
   }



   /*******************************************************************************
    ** Setter for queryStatJoinTableList
    *******************************************************************************/
   public void setQueryStatJoinTableList(List<QueryStatJoinTable> queryStatJoinTableList)
   {
      this.queryStatJoinTableList = queryStatJoinTableList;
   }



   /*******************************************************************************
    ** Fluent setter for queryStatJoinTableList
    *******************************************************************************/
   public QueryStat withQueryStatJoinTableList(List<QueryStatJoinTable> queryStatJoinTableList)
   {
      this.queryStatJoinTableList = queryStatJoinTableList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryStatCriteriaFieldList
    *******************************************************************************/
   public List<QueryStatCriteriaField> getQueryStatCriteriaFieldList()
   {
      return (this.queryStatCriteriaFieldList);
   }



   /*******************************************************************************
    ** Setter for queryStatCriteriaFieldList
    *******************************************************************************/
   public void setQueryStatCriteriaFieldList(List<QueryStatCriteriaField> queryStatCriteriaFieldList)
   {
      this.queryStatCriteriaFieldList = queryStatCriteriaFieldList;
   }



   /*******************************************************************************
    ** Fluent setter for queryStatCriteriaFieldList
    *******************************************************************************/
   public QueryStat withQueryStatCriteriaFieldList(List<QueryStatCriteriaField> queryStatCriteriaFieldList)
   {
      this.queryStatCriteriaFieldList = queryStatCriteriaFieldList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryStatOrderByFieldList
    *******************************************************************************/
   public List<QueryStatOrderByField> getQueryStatOrderByFieldList()
   {
      return (this.queryStatOrderByFieldList);
   }



   /*******************************************************************************
    ** Setter for queryStatOrderByFieldList
    *******************************************************************************/
   public void setQueryStatOrderByFieldList(List<QueryStatOrderByField> queryStatOrderByFieldList)
   {
      this.queryStatOrderByFieldList = queryStatOrderByFieldList;
   }



   /*******************************************************************************
    ** Fluent setter for queryStatOrderByFieldList
    *******************************************************************************/
   public QueryStat withQueryStatOrderByFieldList(List<QueryStatOrderByField> queryStatOrderByFieldList)
   {
      this.queryStatOrderByFieldList = queryStatOrderByFieldList;
      return (this);
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
   public QueryStat withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryFilter
    *******************************************************************************/
   @QIgnore
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
   public QueryStat withQueryFilter(QQueryFilter queryFilter)
   {
      this.queryFilter = queryFilter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for qqqTableId
    *******************************************************************************/
   public Integer getQqqTableId()
   {
      return (this.qqqTableId);
   }



   /*******************************************************************************
    ** Setter for qqqTableId
    *******************************************************************************/
   public void setQqqTableId(Integer qqqTableId)
   {
      this.qqqTableId = qqqTableId;
   }



   /*******************************************************************************
    ** Fluent setter for qqqTableId
    *******************************************************************************/
   public QueryStat withQqqTableId(Integer qqqTableId)
   {
      this.qqqTableId = qqqTableId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for joinTableNames
    *******************************************************************************/
   @QIgnore
   public Set<String> getJoinTableNames()
   {
      return (this.joinTableNames);
   }



   /*******************************************************************************
    ** Setter for joinTableNames
    *******************************************************************************/
   public void setJoinTableNames(Set<String> joinTableNames)
   {
      this.joinTableNames = joinTableNames;
   }



   /*******************************************************************************
    ** Fluent setter for joinTableNames
    *******************************************************************************/
   public QueryStat withJoinTableNames(Set<String> joinTableNames)
   {
      this.joinTableNames = joinTableNames;
      return (this);
   }



   /*******************************************************************************
    * Getter for action
    * @see #withAction(String)
    *******************************************************************************/
   public String getAction()
   {
      return (this.action);
   }



   /*******************************************************************************
    * Setter for action
    * @see #withAction(String)
    *******************************************************************************/
   public void setAction(String action)
   {
      this.action = action;
   }



   /*******************************************************************************
    * Fluent setter for action
    *
    * @param action
    * This is the top-level action that was being performed when the query was
    * executed - so that may often be a process (or, it could be a lower-level
    * action, like a query or insert from middleware).
    * <p>Contrasted with {@link #withBackendAction(String)}, which is the low-level
    * "backend" action (always an insert/update/delete table action)</p>
    * @return this
    *******************************************************************************/
   public QueryStat withAction(String action)
   {
      this.action = action;
      return (this);
   }




   /*******************************************************************************
    ** Getter for sessionId
    *******************************************************************************/
   public String getSessionId()
   {
      return (this.sessionId);
   }



   /*******************************************************************************
    ** Setter for sessionId
    *******************************************************************************/
   public void setSessionId(String sessionId)
   {
      this.sessionId = sessionId;
   }



   /*******************************************************************************
    ** Fluent setter for sessionId
    *******************************************************************************/
   public QueryStat withSessionId(String sessionId)
   {
      this.sessionId = sessionId;
      return (this);
   }



   /*******************************************************************************
    * Getter for backendAction
    * @see #withBackendAction(String)
    *******************************************************************************/
   public String getBackendAction()
   {
      return (this.backendAction);
   }



   /*******************************************************************************
    * Setter for backendAction
    * @see #withBackendAction(String)
    *******************************************************************************/
   public void setBackendAction(String backendAction)
   {
      this.backendAction = backendAction;
   }



   /*******************************************************************************
    * Fluent setter for backendAction
    *
    * @param backendAction
    * e.g., "InsertAction" or "QueryAction" - the low-level table action that generated
    * the query.
    * <p>Contrast with action above, which is the initiating action, e.g., a process
    * or middleware/user initiated query</p>
    * @return this
    *******************************************************************************/
   public QueryStat withBackendAction(String backendAction)
   {
      this.backendAction = backendAction;
      return (this);
   }



   /*******************************************************************************
    * Getter for recordCount
    * @see #withRecordCount(Integer)
    *******************************************************************************/
   public Integer getRecordCount()
   {
      return (this.recordCount);
   }



   /*******************************************************************************
    * Setter for recordCount
    * @see #withRecordCount(Integer)
    *******************************************************************************/
   public void setRecordCount(Integer recordCount)
   {
      this.recordCount = recordCount;
   }



   /*******************************************************************************
    * Fluent setter for recordCount
    *
    * @param recordCount
    * number of records impacted by this query (e.g., returned from a select, or
    * given as input to a DML statement).
    * @return this
    *******************************************************************************/
   public QueryStat withRecordCount(Integer recordCount)
   {
      this.recordCount = recordCount;
      return (this);
   }

}
