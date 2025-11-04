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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatCriteriaField;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatJoinTable;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatOrderByField;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.tables.QQQTableTableManager;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Singleton, which starts a thread, to store query stats into a table.
 **
 ** Supports these systemProperties or ENV_VARS:
 ** qqq.queryStatManager.enabled / QQQ_QUERY_STAT_MANAGER_ENABLED
 ** qqq.queryStatManager.minMillisToStore / QQQ_QUERY_STAT_MANAGER_MIN_MILLIS_TO_STORE
 ** qqq.queryStatManager.jobPeriodSeconds / QQQ_QUERY_STAT_MANAGER_JOB_PERIOD_SECONDS
 ** qqq.queryStatManager.jobInitialDelay / QQQ_QUERY_STAT_MANAGER_JOB_INITIAL_DELAY
 *******************************************************************************/
public class QueryStatManager
{
   private static final QLogger LOG = QLogger.getLogger(QueryStatManager.class);

   private static QueryStatManager queryStatManager = null;

   // todo - support multiple qInstances?
   private QInstance          qInstance;
   private Supplier<QSession> sessionSupplier;

   private boolean         active     = false;
   private List<QueryStat> queryStats = new ArrayList<>();

   private ScheduledExecutorService executorService;

   private int jobPeriodSeconds = 60;
   private int jobInitialDelay  = 60;
   private int minMillisToStore = 0;

   private List<QueryStatConsumerInterface> queryStatConsumers = new ArrayList<>();


   /*******************************************************************************
    ** Singleton constructor
    *******************************************************************************/
   private QueryStatManager()
   {

   }



   /*******************************************************************************
    ** Singleton accessor
    *******************************************************************************/
   public static QueryStatManager getInstance()
   {
      if(queryStatManager == null)
      {
         queryStatManager = new QueryStatManager();

         QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();

         Integer propertyMinMillisToStore = interpreter.getIntegerFromPropertyOrEnvironment("qqq.queryStatManager.minMillisToStore", "QQQ_QUERY_STAT_MANAGER_MIN_MILLIS_TO_STORE", null);
         if(propertyMinMillisToStore != null)
         {
            queryStatManager.setMinMillisToStore(propertyMinMillisToStore);
         }

         Integer propertyJobPeriodSeconds = interpreter.getIntegerFromPropertyOrEnvironment("qqq.queryStatManager.jobPeriodSeconds", "QQQ_QUERY_STAT_MANAGER_JOB_PERIOD_SECONDS", null);
         if(propertyJobPeriodSeconds != null)
         {
            queryStatManager.setJobPeriodSeconds(propertyJobPeriodSeconds);
         }

         Integer propertyJobInitialDelay = interpreter.getIntegerFromPropertyOrEnvironment("qqq.queryStatManager.jobInitialDelay", "QQQ_QUERY_STAT_MANAGER_JOB_INITIAL_DELAY", null);
         if(propertyJobInitialDelay != null)
         {
            queryStatManager.setJobInitialDelay(propertyJobInitialDelay);
         }

      }
      return (queryStatManager);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QueryStat newQueryStat(QBackendMetaData backend, QTableMetaData table, QQueryFilter filter, String backendActionName)
   {
      QueryStat queryStat = null;

      if(table.isCapabilityEnabled(backend, Capability.QUERY_STATS))
      {
         queryStat = new QueryStat();
         queryStat.setTableName(table.getName());
         queryStat.setQueryFilter(Objects.requireNonNullElse(filter, new QQueryFilter()));
         queryStat.setStartTimestamp(Instant.now());
         queryStat.setBackendAction(backendActionName);
      }

      return (queryStat);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void start(QInstance qInstance, Supplier<QSession> sessionSupplier)
   {
      if(!isEnabled())
      {
         LOG.info("Not starting QueryStatManager per settings.");
         return;
      }

      LOG.info("Starting QueryStatManager");

      this.qInstance = qInstance;
      this.sessionSupplier = sessionSupplier;

      active = true;
      queryStats = new ArrayList<>();

      executorService = Executors.newSingleThreadScheduledExecutor(new PrefixedDefaultThreadFactory(this));
      executorService.scheduleAtFixedRate(new QueryStatManagerInsertJob(), jobInitialDelay, jobPeriodSeconds, TimeUnit.SECONDS);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isEnabled()
   {
      return new QMetaDataVariableInterpreter().getBooleanFromPropertyOrEnvironment("qqq.queryStatManager.enabled", "QQQ_QUERY_STAT_MANAGER_ENABLED", true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void stop()
   {
      active = false;
      queryStats.clear();

      if(executorService != null)
      {
         executorService.shutdown();
         executorService = null;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void add(QueryStat queryStat)
   {
      try
      {
         if(queryStat == null)
         {
            return;
         }

         if(active)
         {
            ////////////////////////////////////////////////////////////////////////////////////////
            // set fields that we need to capture now (rather than when the thread to store runs) //
            ////////////////////////////////////////////////////////////////////////////////////////
            if(queryStat.getFirstResultTimestamp() == null)
            {
               queryStat.setFirstResultTimestamp(Instant.now());
            }

            if(queryStat.getStartTimestamp() != null && queryStat.getFirstResultTimestamp() != null && queryStat.getFirstResultMillis() == null)
            {
               long millis = queryStat.getFirstResultTimestamp().toEpochMilli() - queryStat.getStartTimestamp().toEpochMilli();
               queryStat.setFirstResultMillis((int) millis);
            }

            if(queryStat.getSessionId() == null && QContext.getQSession() != null)
            {
               queryStat.setSessionId(QContext.getQSession().getUuid());
            }

            if(queryStat.getAction() == null)
            {
               if(QContext.getActionStack() != null && !QContext.getActionStack().isEmpty())
               {
                  queryStat.setAction(QContext.getActionStack().peek().getActionIdentity());
               }
               else
               {
                  boolean   expected = false;
                  Exception e        = new Exception("Unexpected empty action stack");
                  for(StackTraceElement stackTraceElement : e.getStackTrace())
                  {
                     String className = stackTraceElement.getClassName();
                     if(className.contains(QueryStatManagerInsertJob.class.getName()))
                     {
                        expected = true;
                        break;
                     }
                  }

                  if(!expected)
                  {
                     LOG.debug(e);
                  }
               }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // send the query stat through any consumers that exist                                                                       //
            // note we do that regardless of the minMillisToStore - which only applies to this class's own storage of query stat records. //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(CollectionUtils.nullSafeHasContents(queryStatConsumers))
            {
               processConsumers(queryStat);
            }

            if(queryStat.getFirstResultMillis() != null && queryStat.getFirstResultMillis() < minMillisToStore)
            {
               //////////////////////////////////////////////////////////////
               // discard this record if it's under the min millis setting //
               //////////////////////////////////////////////////////////////
               return;
            }

            synchronized(this)
            {
               queryStats.add(queryStat);
            }
         }
      }
      catch(Exception e)
      {
         LOG.debug("Error adding query stat", e);
      }
   }



   /***************************************************************************
    * send the query stat through any consumers that we have.
    ***************************************************************************/
   private void processConsumers(QueryStat queryStat)
   {
      for(QueryStatConsumerInterface queryStatConsumer : queryStatConsumers)
      {
         try
         {
            queryStatConsumer.accept(queryStat);
         }
         catch(Exception e)
         {
            LOG.warn("Error processing query stat consumer", e, logPair("consumerClass", queryStatConsumer.getClass().getName()));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QueryStat> getListAndReset()
   {
      if(queryStats.isEmpty())
      {
         return Collections.emptyList();
      }

      synchronized(this)
      {
         List<QueryStat> returnList = queryStats;
         queryStats = new ArrayList<>();
         return (returnList);
      }
   }



   /*******************************************************************************
    ** force stats to be stored right now (rather than letting the scheduled job do it)
    *******************************************************************************/
   public void storeStatsNow()
   {
      new QueryStatManagerInsertJob().run();
   }



   /*******************************************************************************
    ** Runnable that gets scheduled to periodically reset and store the list of collected stats
    *******************************************************************************/
   private static class QueryStatManagerInsertJob implements Runnable
   {
      private static final QLogger LOG = QLogger.getLogger(QueryStatManagerInsertJob.class);



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run()
      {
         try
         {
            QContext.init(getInstance().qInstance, getInstance().sessionSupplier.get());

            /////////////////////////////////////////////////////////////////////////////////////
            // every time we re-run, check if we've been turned off - if so, stop the service. //
            /////////////////////////////////////////////////////////////////////////////////////
            if(!isEnabled())
            {
               LOG.info("Stopping QueryStatManager.");
               getInstance().stop();
               return;
            }

            List<QueryStat> list = getInstance().getListAndReset();

            LOG.info(logPair("queryStatListSize", list.size()));

            if(list.isEmpty())
            {
               return;
            }

            ////////////////////////////////////
            // prime the entities for storing //
            ////////////////////////////////////
            List<QRecord> queryStatQRecordsToInsert = new ArrayList<>();
            for(QueryStat queryStat : list)
            {
               try
               {
                  //////////////////////
                  // set the table id //
                  //////////////////////
                  Integer qqqTableId = QQQTableTableManager.getQQQTableId(getInstance().qInstance, queryStat.getTableName());
                  queryStat.setQqqTableId(qqqTableId);

                  //////////////////////////////
                  // build join-table records //
                  //////////////////////////////
                  if(CollectionUtils.nullSafeHasContents(queryStat.getJoinTableNames()))
                  {
                     List<QueryStatJoinTable> queryStatJoinTableList = new ArrayList<>();
                     for(String joinTableName : queryStat.getJoinTableNames())
                     {
                        queryStatJoinTableList.add(new QueryStatJoinTable().withQqqTableId(QQQTableTableManager.getQQQTableId(getInstance().qInstance, joinTableName)));
                     }
                     queryStat.setQueryStatJoinTableList(queryStatJoinTableList);
                  }

                  ////////////////////////////
                  // build criteria records //
                  ////////////////////////////
                  if(queryStat.getQueryFilter() != null && queryStat.getQueryFilter().hasAnyCriteria())
                  {
                     List<QueryStatCriteriaField> queryStatCriteriaFieldList = new ArrayList<>();
                     processCriteriaFromFilter(qqqTableId, queryStatCriteriaFieldList, queryStat.getQueryFilter());
                     queryStat.setQueryStatCriteriaFieldList(queryStatCriteriaFieldList);
                  }

                  if(CollectionUtils.nullSafeHasContents(queryStat.getQueryFilter().getOrderBys()))
                  {
                     List<QueryStatOrderByField> queryStatOrderByFieldList = new ArrayList<>();
                     processOrderByFromFilter(qqqTableId, queryStatOrderByFieldList, queryStat.getQueryFilter());
                     queryStat.setQueryStatOrderByFieldList(queryStatOrderByFieldList);
                  }

                  queryStatQRecordsToInsert.add(queryStat.toQRecord());
               }
               catch(Exception e)
               {
                  //////////////////////
                  // skip this record //
                  //////////////////////
                  LOG.warn("Error priming a query stat for storing", e);
               }
            }

            try
            {
               InsertInput insertInput = new InsertInput();
               insertInput.setTableName(QueryStat.TABLE_NAME);
               insertInput.setRecords(queryStatQRecordsToInsert);
               new InsertAction().execute(insertInput);
            }
            catch(Exception e)
            {
               LOG.error("Error inserting query stats", e);
            }
         }
         catch(Exception e)
         {
            LOG.warn("Error storing query stats", e);
         }
         finally
         {
            QContext.clear();
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static void processCriteriaFromFilter(Integer qqqTableId, List<QueryStatCriteriaField> queryStatCriteriaFieldList, QQueryFilter queryFilter) throws QException
      {
         for(QFilterCriteria criteria : CollectionUtils.nonNullList(queryFilter.getCriteria()))
         {
            String                 fieldName              = criteria.getFieldName();
            QueryStatCriteriaField queryStatCriteriaField = new QueryStatCriteriaField();
            queryStatCriteriaField.setOperator(String.valueOf(criteria.getOperator()));

            if(criteria.getValues() != null)
            {
               queryStatCriteriaField.setValues(StringUtils.join(",", criteria.getValues()));
            }

            if(fieldName.contains("."))
            {
               String[] parts = fieldName.split("\\.");
               if(parts.length > 1)
               {
                  queryStatCriteriaField.setQqqTableId(QQQTableTableManager.getQQQTableId(getInstance().qInstance, parts[0]));
                  queryStatCriteriaField.setName(parts[1]);
               }
            }
            else
            {
               queryStatCriteriaField.setQqqTableId(qqqTableId);
               queryStatCriteriaField.setName(fieldName);
            }

            queryStatCriteriaFieldList.add(queryStatCriteriaField);
         }

         for(QQueryFilter subFilter : CollectionUtils.nonNullList(queryFilter.getSubFilters()))
         {
            processCriteriaFromFilter(qqqTableId, queryStatCriteriaFieldList, subFilter);
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static void processOrderByFromFilter(Integer qqqTableId, List<QueryStatOrderByField> queryStatOrderByFieldList, QQueryFilter queryFilter) throws QException
      {
         for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(queryFilter.getOrderBys()))
         {
            String                fieldName             = orderBy.getFieldName();
            QueryStatOrderByField queryStatOrderByField = new QueryStatOrderByField();

            if(fieldName != null)
            {
               if(fieldName.contains("."))
               {
                  String[] parts = fieldName.split("\\.");
                  if(parts.length > 1)
                  {
                     queryStatOrderByField.setQqqTableId(QQQTableTableManager.getQQQTableId(getInstance().qInstance, parts[0]));
                     queryStatOrderByField.setName(parts[1]);
                  }
               }
               else
               {
                  queryStatOrderByField.setQqqTableId(qqqTableId);
                  queryStatOrderByField.setName(fieldName);
               }

               queryStatOrderByFieldList.add(queryStatOrderByField);
            }
         }
      }
   }



   /*******************************************************************************
    ** Getter for jobPeriodSeconds
    *******************************************************************************/
   public int getJobPeriodSeconds()
   {
      return (this.jobPeriodSeconds);
   }



   /*******************************************************************************
    ** Setter for jobPeriodSeconds
    *******************************************************************************/
   public void setJobPeriodSeconds(int jobPeriodSeconds)
   {
      this.jobPeriodSeconds = jobPeriodSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for jobPeriodSeconds
    *******************************************************************************/
   public QueryStatManager withJobPeriodSeconds(int jobPeriodSeconds)
   {
      this.jobPeriodSeconds = jobPeriodSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for jobInitialDelay
    *******************************************************************************/
   public int getJobInitialDelay()
   {
      return (this.jobInitialDelay);
   }



   /*******************************************************************************
    ** Setter for jobInitialDelay
    *******************************************************************************/
   public void setJobInitialDelay(int jobInitialDelay)
   {
      this.jobInitialDelay = jobInitialDelay;
   }



   /*******************************************************************************
    ** Fluent setter for jobInitialDelay
    *******************************************************************************/
   public QueryStatManager withJobInitialDelay(int jobInitialDelay)
   {
      this.jobInitialDelay = jobInitialDelay;
      return (this);
   }



   /*******************************************************************************
    ** Getter for minMillisToStore
    *******************************************************************************/
   public int getMinMillisToStore()
   {
      return (this.minMillisToStore);
   }



   /*******************************************************************************
    ** Setter for minMillisToStore
    *******************************************************************************/
   public void setMinMillisToStore(int minMillisToStore)
   {
      this.minMillisToStore = minMillisToStore;
   }



   /*******************************************************************************
    ** Fluent setter for minMillisToStore
    *******************************************************************************/
   public QueryStatManager withMinMillisToStore(int minMillisToStore)
   {
      this.minMillisToStore = minMillisToStore;
      return (this);
   }


   /***************************************************************************
    * package-private accessor to current list of query stats.
    * originally adding for tests to access.
    ***************************************************************************/
   List<QueryStat> getQueryStats()
   {
      return (queryStats);
   }


   /*******************************************************************************
    * Getter for queryStatConsumers
    * @see #withQueryStatConsumers(List)
    *******************************************************************************/
   public List<QueryStatConsumerInterface> getQueryStatConsumers()
   {
      return (this.queryStatConsumers);
   }



   /*******************************************************************************
    * Setter for queryStatConsumers
    * @see #withQueryStatConsumers(List)
    *******************************************************************************/
   public void setQueryStatConsumers(List<QueryStatConsumerInterface> queryStatConsumers)
   {
      this.queryStatConsumers = queryStatConsumers;
   }



   /*******************************************************************************
    * Fluent setter for queryStatConsumers
    *
    * @param queryStatConsumers
    * List of instances of QueryStateConsumerInterface - e.g., classes that want to
    * be invoked when a query stat object is being added to the manager here (e.g.,
    * after its query is complete).
    * @return this
    *******************************************************************************/
   public QueryStatManager withQueryStatConsumers(List<QueryStatConsumerInterface> queryStatConsumers)
   {
      this.queryStatConsumers = queryStatConsumers;
      return (this);
   }

}
