/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.savedreports;


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.savedreports.RenderedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumns;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility for verifying that the RenderReports process works for all fields,
 ** on all tables, and all exposed joins.
 **
 ** Meant for use within a unit test, or maybe as part of an instance's boot-up/
 ** validation.
 *******************************************************************************/
public class ReportsFullInstanceVerifier
{
   private static final QLogger LOG = QLogger.getLogger(ReportsFullInstanceVerifier.class);

   private boolean removeRenderedReports          = true;
   private boolean filterForAtMostOneRowPerReport = true;



   /*******************************************************************************
    **
    *******************************************************************************/
   public void verify(Collection<QTableMetaData> tables, String storageTableName) throws QException
   {
      Map<Pair<String, String>, Exception> caughtExceptions = new LinkedHashMap<>();
      for(QTableMetaData table : tables)
      {
         if(table.isCapabilityEnabled(QContext.getQInstance().getBackendForTable(table.getName()), Capability.TABLE_QUERY))
         {
            LOG.info("Verifying Reports on table", logPair("tableName", table.getName()));

            //////////////////////////////////////////////
            // run the table by itself (no join fields) //
            //////////////////////////////////////////////
            runReport(table.getName(), Collections.emptyList(), "main-table-only", caughtExceptions, storageTableName);

            ///////////////////////////////////////////////////
            // run once w/ the fields from each exposed join //
            ///////////////////////////////////////////////////
            for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
            {
               runReport(table.getName(), List.of(exposedJoin), "join-" + exposedJoin.getLabel(), caughtExceptions, storageTableName);
            }

            /////////////////////////////////////////////////
            // run w/ all exposed joins (if there are any) //
            /////////////////////////////////////////////////
            if(CollectionUtils.nullSafeHasContents(table.getExposedJoins()))
            {
               runReport(table.getName(), table.getExposedJoins(), "all-joins", caughtExceptions, storageTableName);
            }
         }
      }

      //////////////////////////////////
      // log out an exceptions caught //
      //////////////////////////////////
      if(!caughtExceptions.isEmpty())
      {
         for(Map.Entry<Pair<String, String>, Exception> entry : caughtExceptions.entrySet())
         {
            LOG.info("Caught an exception verifying reports", entry.getValue(), logPair("tableName", entry.getKey().getA()), logPair("fieldName", entry.getKey().getB()));
         }
         throw (new QException("Reports Verification failed with " + caughtExceptions.size() + " exception" + StringUtils.plural(caughtExceptions.size())));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runReport(String tableName, List<ExposedJoin> exposedJoinList, String description, Map<Pair<String, String>, Exception> caughtExceptions, String storageTableName)
   {
      try
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////
         // build the list of reports to include in the column - starting with all fields in the table //
         ////////////////////////////////////////////////////////////////////////////////////////////////
         ReportColumns reportColumns = new ReportColumns();
         for(QFieldMetaData field : QContext.getQInstance().getTable(tableName).getFields().values())
         {
            reportColumns.withColumn(field.getName());
         }

         ///////////////////////////////////////////////////
         // add all fields from all exposed joins as well //
         ///////////////////////////////////////////////////
         for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(exposedJoinList))
         {
            QTableMetaData joinTable = QContext.getQInstance().getTable(exposedJoin.getJoinTable());
            for(QFieldMetaData field : joinTable.getFields().values())
            {
               reportColumns.withColumn(joinTable.getName() + "." + field.getName());
            }
         }

         QQueryFilter queryFilter = new QQueryFilter();

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if caller is okay with a filter that should limit the report to a small number of rows (could be more than 1 for to-many joins), then do so //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(filterForAtMostOneRowPerReport)
         {
            queryFilter.withCriteria(QContext.getQInstance().getTable(tableName).getPrimaryKeyField(), QCriteriaOperator.EQUALS, 1);
         }

         //////////////////////////////////
         // insert a saved report record //
         //////////////////////////////////
         SavedReport savedReport = new SavedReport();
         savedReport.setTableName(tableName);
         savedReport.setLabel("Test " + tableName + " " + description);
         savedReport.setColumnsJson(JsonUtils.toJson(reportColumns));
         savedReport.setQueryFilterJson(JsonUtils.toJson(queryFilter));
         List<QRecord> reportRecordList = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(savedReport)).getRecords();

         ///////////////////////
         // render the report //
         ///////////////////////
         RunBackendStepInput  input  = new RunBackendStepInput();
         RunBackendStepOutput output = new RunBackendStepOutput();

         input.addValue(RenderSavedReportMetaDataProducer.FIELD_NAME_REPORT_FORMAT, ReportFormat.CSV.name());
         input.addValue(RenderSavedReportMetaDataProducer.FIELD_NAME_STORAGE_TABLE_NAME, storageTableName);
         input.setRecords(reportRecordList);

         new RenderSavedReportExecuteStep().run(input, output);

         //////////////////////////////////////////
         // clean up the report, if so requested //
         //////////////////////////////////////////
         if(removeRenderedReports)
         {
            new DeleteAction().execute(new DeleteInput(RenderedReport.TABLE_NAME).withPrimaryKey(output.getValue("renderedReportId")));
         }
      }
      catch(QException e)
      {
         caughtExceptions.put(Pair.of(tableName, description), e);
      }
   }

   /*******************************************************************************
    ** Getter for removeRenderedReports
    *******************************************************************************/
   public boolean getRemoveRenderedReports()
   {
      return (this.removeRenderedReports);
   }



   /*******************************************************************************
    ** Setter for removeRenderedReports
    *******************************************************************************/
   public void setRemoveRenderedReports(boolean removeRenderedReports)
   {
      this.removeRenderedReports = removeRenderedReports;
   }



   /*******************************************************************************
    ** Fluent setter for removeRenderedReports
    *******************************************************************************/
   public ReportsFullInstanceVerifier withRemoveRenderedReports(boolean removeRenderedReports)
   {
      this.removeRenderedReports = removeRenderedReports;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filterForAtMostOneRowPerReport
    *******************************************************************************/
   public boolean getFilterForAtMostOneRowPerReport()
   {
      return (this.filterForAtMostOneRowPerReport);
   }



   /*******************************************************************************
    ** Setter for filterForAtMostOneRowPerReport
    *******************************************************************************/
   public void setFilterForAtMostOneRowPerReport(boolean filterForAtMostOneRowPerReport)
   {
      this.filterForAtMostOneRowPerReport = filterForAtMostOneRowPerReport;
   }



   /*******************************************************************************
    ** Fluent setter for filterForAtMostOneRowPerReport
    *******************************************************************************/
   public ReportsFullInstanceVerifier withFilterForAtMostOneRowPerReport(boolean filterForAtMostOneRowPerReport)
   {
      this.filterForAtMostOneRowPerReport = filterForAtMostOneRowPerReport;
      return (this);
   }


}
