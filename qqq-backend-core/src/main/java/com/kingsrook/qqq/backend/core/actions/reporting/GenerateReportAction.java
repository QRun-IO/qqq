/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.actions.async.AsyncRecordPipeLoop;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.reporting.customizers.DataSourceQueryInputCustomizer;
import com.kingsrook.qqq.backend.core.actions.reporting.customizers.ReportCustomRecordSourceInterface;
import com.kingsrook.qqq.backend.core.actions.reporting.customizers.ReportViewCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QFormulaException;
import com.kingsrook.qqq.backend.core.exceptions.QReportingException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaMissingInputValueBehavior;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.FilterUseCase;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAndJoinTable;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.BackendStepPostRunInput;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.BackendStepPostRunOutput;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.aggregates.AggregatesInterface;
import com.kingsrook.qqq.backend.core.utils.aggregates.BigDecimalAggregates;
import com.kingsrook.qqq.backend.core.utils.aggregates.InstantAggregates;
import com.kingsrook.qqq.backend.core.utils.aggregates.IntegerAggregates;
import com.kingsrook.qqq.backend.core.utils.aggregates.LocalDateAggregates;
import com.kingsrook.qqq.backend.core.utils.aggregates.LongAggregates;
import com.kingsrook.qqq.backend.core.utils.aggregates.StringAggregates;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Action to generate a report.
 **
 ** A report can contain 1 or more Data Sources - e.g., tables + filters that define
 ** data that goes into the report, or simple data-supplier lambdas.
 **
 ** A report can also contain 1 or more Views - e.g., sheets in a spreadsheet workbook.
 ** (how do those work in non-XLSX formats??). Views can either be:
 ** - plain tables,
 ** - summaries (like pivot tables, but called summary to avoid confusion with "native" pivot tables),
 ** - native pivot tables (not initially supported, due to lack of support in fastexcel...).
 *******************************************************************************/
public class GenerateReportAction extends AbstractQActionFunction<ReportInput, ReportOutput>
{
   private static final QLogger LOG = QLogger.getLogger(GenerateReportAction.class);

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // summaryAggregates and varianceAggregates are multi-level maps, ala:                                     //
   // viewName > SummaryKey > fieldName > Aggregates                                                          //
   // e.g.:                                                                                                   //
   // viewName: salesSummaryReport                                                                            //
   // SummaryKey: [(state:MO),(city:St.Louis)]                                                                //
   // fieldName: salePrice                                                                                    //
   // Aggregates: (count:47;sum:10,000;max:2,000;min:15)                                                      //
   // salesSummaryReport > [(state:MO),(city:St.Louis)] > salePrice  > (count:47;sum:10,000;max:2,000;min:15) //
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////
   Map<String, Map<SummaryKey, Map<String, AggregatesInterface<?, ?>>>> summaryAggregates  = new HashMap<>();
   Map<String, Map<SummaryKey, Map<String, AggregatesInterface<?, ?>>>> varianceAggregates = new HashMap<>();

   Map<String, AggregatesInterface<?, ?>> totalAggregates         = new HashMap<>();
   Map<String, AggregatesInterface<?, ?>> varianceTotalAggregates = new HashMap<>();

   private ExportStreamerInterface reportStreamer;
   private List<QReportDataSource> dataSources;
   private List<QReportView>       views;

   private Map<String, Integer> countByDataSource = new HashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public ReportOutput execute(ReportInput reportInput) throws QException
   {
      ReportOutput    reportOutput = new ReportOutput();
      QReportMetaData report       = getReportMetaData(reportInput);

      this.views = report.getViews();
      this.dataSources = report.getDataSources();

      ReportFormat reportFormat = reportInput.getReportDestination().getReportFormat();
      if(reportFormat == null)
      {
         throw new QException("Report format was not specified.");
      }

      if(reportInput.getOverrideExportStreamerSupplier() != null)
      {
         reportStreamer = reportInput.getOverrideExportStreamerSupplier().get();
      }
      else
      {
         reportStreamer = reportFormat.newReportStreamer();
      }

      if(reportInput.getExportStyleCustomizer() != null)
      {
         ExportStyleCustomizerInterface styleCustomizer = QCodeLoader.getAdHoc(ExportStyleCustomizerInterface.class, reportInput.getExportStyleCustomizer());
         reportStreamer.setExportStyleCustomizer(styleCustomizer);
      }
      else if(report.getExportStyleCustomizer() != null)
      {
         ExportStyleCustomizerInterface styleCustomizer = QCodeLoader.getAdHoc(ExportStyleCustomizerInterface.class, report.getExportStyleCustomizer());
         reportStreamer.setExportStyleCustomizer(styleCustomizer);
      }

      reportStreamer.preRun(reportInput.getReportDestination(), views);

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // foreach data source, do a query (possibly more than 1, if it goes to multiple table views) //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      for(QReportDataSource dataSource : dataSources)
      {
         //////////////////////////////////////////////////////////////////////////////
         // make a list of the views that use this data source for various purposes. //
         //////////////////////////////////////////////////////////////////////////////
         List<QReportView> dataSourceTableViews = views.stream()
            .filter(v -> v.getType().equals(ReportType.TABLE))
            .filter(v -> v.getDataSourceName().equals(dataSource.getName()))
            .toList();

         List<QReportView> dataSourceSummaryViews = views.stream()
            .filter(v -> v.getType().equals(ReportType.SUMMARY))
            .filter(v -> v.getDataSourceName().equals(dataSource.getName()))
            .toList();

         List<QReportView> dataSourceVariantViews = views.stream()
            .filter(v -> v.getType().equals(ReportType.SUMMARY))
            .filter(v -> v.getVarianceDataSourceName() != null && v.getVarianceDataSourceName().equals(dataSource.getName()))
            .toList();

         /////////////////////////////////////////////////////////////////////////////////////////////
         // if this data source isn't used for any table views, but it is used for one or           //
         // more summary views (possibly as a variant), then run the query, gathering summary data. //
         /////////////////////////////////////////////////////////////////////////////////////////////
         if(dataSourceTableViews.isEmpty())
         {
            if(!dataSourceSummaryViews.isEmpty() || !dataSourceVariantViews.isEmpty())
            {
               gatherData(reportInput, dataSource, null, dataSourceSummaryViews, dataSourceVariantViews);
            }
         }
         else
         {
            ////////////////////////////////////////////////////////////////////////////////////////
            // else, foreach table view this data source is used for, run the data source's query //
            ////////////////////////////////////////////////////////////////////////////////////////
            for(QReportView dataSourceTableView : dataSourceTableViews)
            {
               /////////////////////////////////////////////////////////////////////////////////////////
               // if there's a view customizer, run it (e.g., to customize the columns in the report) //
               /////////////////////////////////////////////////////////////////////////////////////////
               if(dataSourceTableView.getViewCustomizer() != null)
               {
                  @SuppressWarnings("unchecked")
                  Function<QReportView, QReportView> viewCustomizerFunction = QCodeLoader.getAdHoc(Function.class, dataSourceTableView.getViewCustomizer());
                  if(viewCustomizerFunction instanceof ReportViewCustomizer reportViewCustomizer)
                  {
                     reportViewCustomizer.setReportInput(reportInput);
                  }
                  dataSourceTableView = viewCustomizerFunction.apply(dataSourceTableView.clone());
               }

               ////////////////////////////////////////////////////////////////////////////////////
               // start the table-view (e.g., open this tab in xlsx) and then run the query-loop //
               ////////////////////////////////////////////////////////////////////////////////////
               startTableView(reportInput, dataSource, dataSourceTableView, reportFormat);
               gatherData(reportInput, dataSource, dataSourceTableView, dataSourceSummaryViews, dataSourceVariantViews);
            }
         }
      }

      //////////////////////
      // add pivot sheets //
      //////////////////////
      for(QReportView view : views)
      {
         if(view.getType().equals(ReportType.PIVOT))
         {
            if(reportFormat.getSupportsNativePivotTables())
            {
               startTableView(reportInput, null, view, reportFormat);
            }
            else
            {
               LOG.warn("Request to render a report with a PIVOT type view, for a format that does not support native pivot tables", logPair("reportFormat", reportFormat));
            }

            //////////////////////////////////////////////////////////////////////////
            // there's no data to add to a pivot table, so nothing else to do here. //
            //////////////////////////////////////////////////////////////////////////
         }
      }

      outputSummaries(reportInput);

      reportOutput.setTotalRecordCount(countByDataSource.values().stream().mapToInt(Integer::intValue).sum());

      reportStreamer.finish();

      try
      {
         reportInput.getReportDestination().getReportOutputStream().close();
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error completing report", e));
      }

      return (reportOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QReportMetaData getReportMetaData(ReportInput reportInput) throws QException
   {
      if(reportInput.getReportMetaData() != null)
      {
         return reportInput.getReportMetaData();
      }

      if(StringUtils.hasContent(reportInput.getReportName()))
      {
         return QContext.getQInstance().getReport(reportInput.getReportName());
      }

      throw (new QReportingException("ReportInput did not contain required parameters to identify the report being generated"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void startTableView(ReportInput reportInput, QReportDataSource dataSource, QReportView reportView, ReportFormat reportFormat) throws QException
   {
      QMetaDataVariableInterpreter variableInterpreter = new QMetaDataVariableInterpreter();
      variableInterpreter.addValueMap("input", reportInput.getInputValues());

      ExportInput exportInput = new ExportInput();
      exportInput.setReportDestination(reportInput.getReportDestination());
      exportInput.setTitleRow(getTitle(reportView, variableInterpreter));
      exportInput.setIncludeHeaderRow(reportView.getIncludeHeaderRow());

      JoinsContext joinsContext = null;
      if(dataSource != null)
      {
         ///////////////////////////////////////////////////////////////////////////////////////
         // count records, if applicable, from the data source - for populating into the      //
         // countByDataSource map, as well as for checking if too many rows (e.g., for excel) //
         ///////////////////////////////////////////////////////////////////////////////////////
         countDataSourceRecords(reportInput, dataSource, reportFormat);

         ///////////////////////////////////////////////////////////////////////////////////////////
         // if there's a source table, set up a joins context, to use below for looking up fields //
         ///////////////////////////////////////////////////////////////////////////////////////////
         if(StringUtils.hasContent(dataSource.getSourceTable()))
         {
            QQueryFilter queryFilter = dataSource.getQueryFilter() == null ? new QQueryFilter() : dataSource.getQueryFilter().clone();
            joinsContext = new JoinsContext(QContext.getQInstance(), dataSource.getSourceTable(), dataSource.getQueryJoins(), queryFilter);
         }
      }

      List<QFieldMetaData> fields = new ArrayList<>();
      for(QReportField column : CollectionUtils.nonNullList(reportView.getColumns()))
      {
         if(column.getIsVirtual())
         {
            fields.add(column.toField());
         }
         else
         {
            String                                effectiveFieldName       = Objects.requireNonNullElse(column.getSourceFieldName(), column.getName());
            JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext == null ? null : joinsContext.getFieldAndTableNameOrAlias(effectiveFieldName);
            if(fieldAndTableNameOrAlias == null || fieldAndTableNameOrAlias.field() == null)
            {
               throw new QReportingException("Could not find field named [" + effectiveFieldName + "] in dataSource [" + (dataSource == null ? null : dataSource.getName()) + "]");
            }

            QFieldMetaData field = fieldAndTableNameOrAlias.field().clone();
            field.setName(column.getName());
            if(StringUtils.hasContent(column.getLabel()))
            {

               field.setLabel(column.getLabel());
            }
            fields.add(field);
         }
      }

      reportStreamer.setDisplayFormats(getDisplayFormatMap(fields));
      reportStreamer.start(exportInput, fields, reportView.getLabel(), reportView);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void countDataSourceRecords(ReportInput reportInput, QReportDataSource dataSource, ReportFormat reportFormat) throws QException
   {
      Integer count = null;
      if(dataSource.getCustomRecordSource() != null)
      {
         // todo - add `count` method to interface?
      }
      else if(StringUtils.hasContent(dataSource.getSourceTable()))
      {
         QQueryFilter queryFilter = dataSource.getQueryFilter() == null ? new QQueryFilter() : dataSource.getQueryFilter().clone();
         setInputValuesInQueryFilter(reportInput, queryFilter);

         CountInput countInput = new CountInput();
         countInput.setTableName(dataSource.getSourceTable());
         countInput.setFilter(queryFilter);
         countInput.setQueryJoins(cloneDataSourceQueryJoins(dataSource));
         CountOutput countOutput = new CountAction().execute(countInput);

         count = countOutput.getCount();
      }

      if(count != null)
      {
         countByDataSource.put(dataSource.getName(), count);

         if(reportFormat.getMaxRows() != null && count > reportFormat.getMaxRows())
         {
            throw (new QUserFacingException("The requested report would include more rows ("
               + String.format("%,d", count) + ") than the maximum allowed ("
               + String.format("%,d", reportFormat.getMaxRows()) + ") for the selected file format (" + reportFormat + ")."));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QueryJoin> cloneDataSourceQueryJoins(QReportDataSource dataSource)
   {
      if(dataSource == null || dataSource.getQueryJoins() == null)
      {
         return (null);
      }

      List<QueryJoin> rs = new ArrayList<>();
      for(QueryJoin queryJoin : dataSource.getQueryJoins())
      {
         rs.add(queryJoin.clone());
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer gatherData(ReportInput reportInput, QReportDataSource dataSource, QReportView tableView, List<QReportView> summaryViews, List<QReportView> variantViews) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////
      // check if this view has a transform step - if so, set it up now and run its pre-run //
      ////////////////////////////////////////////////////////////////////////////////////////
      AbstractTransformStep transformStep       = null;
      RunBackendStepInput   transformStepInput  = null;
      RunBackendStepOutput  transformStepOutput = null;
      if(tableView != null && tableView.getRecordTransformStep() != null)
      {
         transformStep = QCodeLoader.getAdHoc(AbstractTransformStep.class, tableView.getRecordTransformStep());

         transformStepInput = new RunBackendStepInput();
         transformStepInput.setValues(reportInput.getInputValues());

         transformStepOutput = new RunBackendStepOutput();

         transformStep.preRun(transformStepInput, transformStepOutput);
      }

      ////////////////////////////////////////////////////////////////////
      // create effectively-final versions of these vars for the lambda //
      ////////////////////////////////////////////////////////////////////
      AbstractTransformStep finalTransformStep       = transformStep;
      RunBackendStepInput   finalTransformStepInput  = transformStepInput;
      RunBackendStepOutput  finalTransformStepOutput = transformStepOutput;

      String        tableLabel    = ObjectUtils.tryElse(() -> QContext.getQInstance().getTable(dataSource.getSourceTable()).getLabel(), Objects.requireNonNullElse(dataSource.getSourceTable(), ""));
      AtomicInteger consumedCount = new AtomicInteger(0);

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // run a record pipe loop, over the query (or other data-supplier/source) for this data source //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      RecordPipe recordPipe = new BufferedRecordPipe(1000);
      new AsyncRecordPipeLoop().run("Report[" + reportInput.getReportName() + "]", null, recordPipe, (callback) ->
      {
         if(dataSource.getCustomRecordSource() != null)
         {
            ReportCustomRecordSourceInterface recordSource = QCodeLoader.getAdHoc(ReportCustomRecordSourceInterface.class, dataSource.getCustomRecordSource());
            recordSource.execute(reportInput, dataSource, recordPipe);
            return (true);
         }
         else if(dataSource.getSourceTable() != null)
         {
            QQueryFilter queryFilter = dataSource.getQueryFilter() == null ? new QQueryFilter() : dataSource.getQueryFilter().clone();
            setInputValuesInQueryFilter(reportInput, queryFilter);

            QueryInput queryInput = new QueryInput();
            queryInput.setRecordPipe(recordPipe);
            queryInput.setTableName(dataSource.getSourceTable());
            queryInput.setFilter(queryFilter);
            queryInput.setQueryJoins(cloneDataSourceQueryJoins(dataSource));
            queryInput.withQueryHint(QueryHint.POTENTIALLY_LARGE_NUMBER_OF_RESULTS);
            queryInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

            queryInput.setShouldTranslatePossibleValues(true);
            queryInput.setFieldsToTranslatePossibleValues(setupFieldsToTranslatePossibleValues(reportInput, dataSource));

            if(dataSource.getQueryInputCustomizer() != null)
            {
               DataSourceQueryInputCustomizer queryInputCustomizer = QCodeLoader.getAdHoc(DataSourceQueryInputCustomizer.class, dataSource.getQueryInputCustomizer());
               queryInput = queryInputCustomizer.run(reportInput, queryInput);
            }

            return (new QueryAction().execute(queryInput));
         }
         else if(dataSource.getStaticDataSupplier() != null)
         {
            @SuppressWarnings("unchecked")
            Supplier<List<List<Serializable>>> supplier = QCodeLoader.getAdHoc(Supplier.class, dataSource.getStaticDataSupplier());
            List<List<Serializable>> lists = supplier.get();
            for(List<Serializable> list : lists)
            {
               QRecord record = new QRecord();
               int     index  = 0;
               for(Serializable value : list)
               {
                  record.setValue("column" + (index++), value);
               }
               recordPipe.addRecord(record);
            }
            return (true);
         }
         else
         {
            throw (new IllegalStateException("Misconfigured data source [" + dataSource.getName() + "]."));
         }
      }, () ->
      {
         List<QRecord> records = recordPipe.consumeAvailableRecords();
         if(finalTransformStep != null)
         {
            finalTransformStepInput.setRecords(records);
            finalTransformStep.runOnePage(finalTransformStepInput, finalTransformStepOutput);
            records = finalTransformStepOutput.getRecords();
         }

         Integer total = countByDataSource.get(dataSource.getName());
         if(total != null)
         {
            reportInput.getAsyncJobCallback().updateStatus("Processing " + tableLabel + " records", consumedCount.get() + 1, total);
         }
         else
         {
            reportInput.getAsyncJobCallback().updateStatus("Processing " + tableLabel + " records (" + String.format("%,d", consumedCount.get() + 1) + ")");
         }
         consumedCount.getAndAdd(records.size());

         return (consumeRecords(dataSource, records, tableView, summaryViews, variantViews));
      });

      ////////////////////////////////////////////////
      // if there's a transformer, run its post-run //
      ////////////////////////////////////////////////
      if(transformStep != null)
      {
         transformStep.postRun(new BackendStepPostRunInput(transformStepInput), new BackendStepPostRunOutput(transformStepOutput));
      }

      return consumedCount.get();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Set<String> setupFieldsToTranslatePossibleValues(ReportInput reportInput, QReportDataSource dataSource) throws QException
   {
      Set<String> fieldsToTranslatePossibleValues = new HashSet<>();

      for(QReportView view : views)
      {
         for(QReportField column : CollectionUtils.nonNullList(view.getColumns()))
         {
            ////////////////////////////////////////////////////////////////////////////////////////
            // if this is a column marked as ShowPossibleValueLabel, then we need to translate it //
            ////////////////////////////////////////////////////////////////////////////////////////
            if(column.getShowPossibleValueLabel())
            {
               String effectiveFieldName = Objects.requireNonNullElse(column.getSourceFieldName(), column.getName());
               fieldsToTranslatePossibleValues.add(effectiveFieldName);
            }
         }

         for(String summaryFieldName : CollectionUtils.nonNullList(view.getSummaryFields()))
         {
            ///////////////////////////////////////////////////////////////////////////////
            // all pivotFields that are possible value sources are implicitly translated //
            ///////////////////////////////////////////////////////////////////////////////
            QTableMetaData    mainTable         = QContext.getQInstance().getTable(dataSource.getSourceTable());
            FieldAndJoinTable fieldAndJoinTable = FieldAndJoinTable.get(mainTable, summaryFieldName);
            if(fieldAndJoinTable.field().getPossibleValueSourceName() != null)
            {
               fieldsToTranslatePossibleValues.add(summaryFieldName);
            }
         }
      }

      return (fieldsToTranslatePossibleValues);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setInputValuesInQueryFilter(ReportInput reportInput, QQueryFilter queryFilter) throws QException
   {
      if(queryFilter == null || queryFilter.getCriteria() == null)
      {
         return;
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // for reports defined in meta-data, the established rule is, that missing input variable values are discarded. //
      // but for non-meta-data reports (e.g., user-saved), we expect an exception for missing values.                 //
      // so, set those use-cases up.                                                                                  //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      FilterUseCase filterUseCase;
      if(StringUtils.hasContent(reportInput.getReportName()) && QContext.getQInstance().getReport(reportInput.getReportName()) != null)
      {
         filterUseCase = new ReportFromMetaDataFilterUseCase();
      }
      else
      {
         filterUseCase = new ReportNotFromMetaDataFilterUseCase();
      }

      queryFilter.interpretValues(MapBuilder.of("input", reportInput.getInputValues()), filterUseCase);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class ReportFromMetaDataFilterUseCase implements FilterUseCase
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior()
      {
         return CriteriaMissingInputValueBehavior.REMOVE_FROM_FILTER;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class ReportNotFromMetaDataFilterUseCase implements FilterUseCase
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public CriteriaMissingInputValueBehavior getDefaultCriteriaMissingInputValueBehavior()
      {
         return CriteriaMissingInputValueBehavior.THROW_EXCEPTION;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer consumeRecords(QReportDataSource dataSource, List<QRecord> records, QReportView tableView, List<QReportView> summaryViews, List<QReportView> variantViews) throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(dataSource.getSourceTable());

      ////////////////////////////////////////////////////////////////////////////
      // if this record goes on a table view, add it to the report streamer now //
      ////////////////////////////////////////////////////////////////////////////
      if(tableView != null)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if any fields are 'showPossibleValueLabel', then move display values for them into the record's values map //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         for(QReportField column : CollectionUtils.nonNullList(tableView.getColumns()))
         {
            if(column.getShowPossibleValueLabel())
            {
               String effectiveFieldName = Objects.requireNonNullElse(column.getSourceFieldName(), column.getName());
               for(QRecord record : records)
               {
                  String displayValue = record.getDisplayValue(effectiveFieldName);
                  record.setValue(column.getName(), displayValue);
               }
            }
         }

         reportStreamer.addRecords(records);
      }

      /////////////////////////////////
      // do aggregates for summaries //
      /////////////////////////////////
      if(summaryViews != null)
      {
         for(QReportView summaryView : summaryViews)
         {
            addRecordsToSummaryAggregates(summaryView, table, records, summaryAggregates);
         }
      }

      if(variantViews != null)
      {
         for(QReportView variantView : variantViews)
         {
            addRecordsToSummaryAggregates(variantView, table, records, varianceAggregates);
         }
      }

      ///////////////////////////////////////////
      // do totals too, if any views want them //
      ///////////////////////////////////////////
      if(summaryViews != null && summaryViews.stream().anyMatch(QReportView::getIncludeTotalRow))
      {
         for(QRecord record : records)
         {
            addRecordToAggregatesMap(table, record, totalAggregates);
         }
      }

      if(variantViews != null && variantViews.stream().anyMatch(QReportView::getIncludeTotalRow))
      {
         for(QRecord record : records)
         {
            addRecordToAggregatesMap(table, record, varianceTotalAggregates);
         }
      }

      return (records.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addRecordsToSummaryAggregates(QReportView view, QTableMetaData table, List<QRecord> records, Map<String, Map<SummaryKey, Map<String, AggregatesInterface<?, ?>>>> aggregatesMap) throws QException
   {
      Map<SummaryKey, Map<String, AggregatesInterface<?, ?>>> viewAggregates = aggregatesMap.computeIfAbsent(view.getName(), (name) -> new HashMap<>());

      for(QRecord record : records)
      {
         SummaryKey key = new SummaryKey();
         for(String summaryFieldName : view.getSummaryFields())
         {
            FieldAndJoinTable fieldAndJoinTable = FieldAndJoinTable.get(table, summaryFieldName);
            Serializable      summaryValue      = record.getValue(summaryFieldName);
            if(fieldAndJoinTable.field().getPossibleValueSourceName() != null)
            {
               //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // so, this is kinda a thing - where we implicitly use possible-value labels (e.g., display values) for pivot fields... //
               //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               summaryValue = record.getDisplayValue(summaryFieldName);
            }
            key.add(summaryFieldName, summaryValue);

            if(view.getIncludeSummarySubTotals() && key.getKeys().size() < view.getSummaryFields().size())
            {
               /////////////////////////////////////////////////////////////////////////////////////////
               // be careful here, with these key objects, and their identity, being used as map keys //
               /////////////////////////////////////////////////////////////////////////////////////////
               SummaryKey subKey = key.clone();
               addRecordToSummaryKeyAggregates(table, record, viewAggregates, subKey);
            }
         }

         addRecordToSummaryKeyAggregates(table, record, viewAggregates, key);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addRecordToSummaryKeyAggregates(QTableMetaData table, QRecord record, Map<SummaryKey, Map<String, AggregatesInterface<?, ?>>> viewAggregates, SummaryKey key)
   {
      Map<String, AggregatesInterface<?, ?>> keyAggregates = viewAggregates.computeIfAbsent(key, (name) -> new HashMap<>());
      addRecordToAggregatesMap(table, record, keyAggregates);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addRecordToAggregatesMap(QTableMetaData table, QRecord record, Map<String, AggregatesInterface<?, ?>> aggregatesMap)
   {
      //////////////////////////////////////////////////////////////////////////////////////
      // todo - an optimization could be, to only compute aggregates that we'll need...   //
      // Only if we measure and see this to be slow - it may be, lots of BigDecimal math? //
      //////////////////////////////////////////////////////////////////////////////////////
      for(String fieldName : record.getValues().keySet())
      {
         QFieldMetaData field;
         try
         {
            //////////////////////////////////////////////////////
            // todo - memoize this, if we ever need to optimize //
            //////////////////////////////////////////////////////
            FieldAndJoinTable fieldAndJoinTable = FieldAndJoinTable.get(table, fieldName);
            field = fieldAndJoinTable.field();
         }
         catch(Exception e)
         {
            //////////////////////////////////////////////////////////////////////////////////////
            // for non-real-fields... let's skip for now - but maybe treat as string in future? //
            //////////////////////////////////////////////////////////////////////////////////////
            LOG.debug("Couldn't find field in table qInstance - won't compute aggregates for it", logPair("fieldName", fieldName));
            continue;
         }

         if(StringUtils.hasContent(field.getPossibleValueSourceName()))
         {
            @SuppressWarnings("unchecked")
            AggregatesInterface<String, ?> fieldAggregates = (AggregatesInterface<String, ?>) aggregatesMap.computeIfAbsent(fieldName, (name) -> new StringAggregates());
            fieldAggregates.add(record.getDisplayValue(fieldName));
         }
         else if(field.getType().equals(QFieldType.INTEGER))
         {
            @SuppressWarnings("unchecked")
            AggregatesInterface<Integer, ?> fieldAggregates = (AggregatesInterface<Integer, ?>) aggregatesMap.computeIfAbsent(fieldName, (name) -> new IntegerAggregates());
            fieldAggregates.add(record.getValueInteger(fieldName));
         }
         else if(field.getType().equals(QFieldType.LONG))
         {
            @SuppressWarnings("unchecked")
            AggregatesInterface<Long, ?> fieldAggregates = (AggregatesInterface<Long, ?>) aggregatesMap.computeIfAbsent(fieldName, (name) -> new LongAggregates());
            fieldAggregates.add(record.getValueLong(fieldName));
         }
         else if(field.getType().equals(QFieldType.DECIMAL))
         {
            @SuppressWarnings("unchecked")
            AggregatesInterface<BigDecimal, ?> fieldAggregates = (AggregatesInterface<BigDecimal, ?>) aggregatesMap.computeIfAbsent(fieldName, (name) -> new BigDecimalAggregates());
            fieldAggregates.add(record.getValueBigDecimal(fieldName));
         }
         else if(field.getType().equals(QFieldType.DATE_TIME))
         {
            @SuppressWarnings("unchecked")
            AggregatesInterface<Instant, ?> fieldAggregates = (AggregatesInterface<Instant, ?>) aggregatesMap.computeIfAbsent(fieldName, (name) -> new InstantAggregates());
            fieldAggregates.add(record.getValueInstant(fieldName));
         }
         else if(field.getType().equals(QFieldType.DATE))
         {
            @SuppressWarnings("unchecked")
            AggregatesInterface<LocalDate, ?> fieldAggregates = (AggregatesInterface<LocalDate, ?>) aggregatesMap.computeIfAbsent(fieldName, (name) -> new LocalDateAggregates());
            fieldAggregates.add(record.getValueLocalDate(fieldName));
         }
         if(field.getType().isStringLike())
         {
            @SuppressWarnings("unchecked")
            AggregatesInterface<String, ?> fieldAggregates = (AggregatesInterface<String, ?>) aggregatesMap.computeIfAbsent(fieldName, (name) -> new StringAggregates());
            fieldAggregates.add(record.getValueString(fieldName));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void outputSummaries(ReportInput reportInput) throws QException
   {
      List<QReportView> reportViews = views.stream().filter(v -> v.getType().equals(ReportType.SUMMARY)).toList();
      for(QReportView view : reportViews)
      {
         QReportDataSource dataSource = getDataSource(view.getDataSourceName());
         if(dataSource == null)
         {
            throw new QReportingException("Data source for summary view was not found (viewName=" + view.getName() + ", dataSourceName=" + view.getDataSourceName() + ").");
         }

         QTableMetaData table         = QContext.getQInstance().getTable(dataSource.getSourceTable());
         SummaryOutput  summaryOutput = computeSummaryRowsForView(reportInput, view, table);

         ExportInput exportInput = new ExportInput();
         exportInput.setReportDestination(reportInput.getReportDestination());
         exportInput.setTitleRow(summaryOutput.titleRow);
         exportInput.setIncludeHeaderRow(view.getIncludeHeaderRow());

         reportStreamer.setDisplayFormats(getDisplayFormatMap(view));
         reportStreamer.start(exportInput, getFields(table, view), view.getLabel(), view);

         reportStreamer.addRecords(summaryOutput.summaryRows); // todo - what if this set is huge?

         if(summaryOutput.totalRow != null)
         {
            reportStreamer.addTotalsRow(summaryOutput.totalRow);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QReportDataSource getDataSource(String dataSourceName)
   {
      for(QReportDataSource dataSource : CollectionUtils.nonNullList(dataSources))
      {
         if(dataSource.getName().equals(dataSourceName))
         {
            return (dataSource);
         }
      }

      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> getDisplayFormatMap(QReportView view)
   {
      return (view.getColumns().stream()
         .filter(c -> c.getDisplayFormat() != null)
         .collect(Collectors.toMap(QReportField::getName, QReportField::getDisplayFormat)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> getDisplayFormatMap(List<QFieldMetaData> fields)
   {
      return (fields.stream()
         .filter(f -> f.getDisplayFormat() != null)
         .collect(Collectors.toMap(QFieldMetaData::getName, QFieldMetaData::getDisplayFormat)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QFieldMetaData> getFields(QTableMetaData table, QReportView view) throws QException
   {
      List<QFieldMetaData> fields = new ArrayList<>();
      for(String summaryFieldName : view.getSummaryFields())
      {
         FieldAndJoinTable fieldAndJoinTable = FieldAndJoinTable.get(table, summaryFieldName);
         fields.add(new QFieldMetaData(summaryFieldName, fieldAndJoinTable.field().getType()).withLabel(fieldAndJoinTable.field().getLabel())); // todo do we need the type?  if so need table as input here
      }
      for(QReportField column : view.getColumns())
      {
         fields.add(new QFieldMetaData().withName(column.getName()).withLabel(column.getLabel())); // todo do we need the type?  if so need table as input here
      }
      return (fields);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SummaryOutput computeSummaryRowsForView(ReportInput reportInput, QReportView view, QTableMetaData table) throws QFormulaException
   {
      QMetaDataVariableInterpreter variableInterpreter = new QMetaDataVariableInterpreter();
      variableInterpreter.addValueMap("input", reportInput.getInputValues());
      variableInterpreter.addValueMap("total", getSummaryValuesForInterpreter(totalAggregates));

      ///////////
      // title //
      ///////////
      String title = getTitle(view, variableInterpreter);

      /////////////////////////
      // create summary rows //
      /////////////////////////
      List<QRecord> summaryRows = new ArrayList<>();
      for(Map.Entry<SummaryKey, Map<String, AggregatesInterface<?, ?>>> entry : summaryAggregates.getOrDefault(view.getName(), Collections.emptyMap()).entrySet())
      {
         SummaryKey                             summaryKey      = entry.getKey();
         Map<String, AggregatesInterface<?, ?>> fieldAggregates = entry.getValue();
         Map<String, Serializable>              summaryValues   = getSummaryValuesForInterpreter(fieldAggregates);
         variableInterpreter.addValueMap("pivot", summaryValues);
         variableInterpreter.addValueMap("summary", summaryValues);

         HashMap<String, Serializable> thisRowValues = new HashMap<>();
         variableInterpreter.addValueMap("thisRow", thisRowValues);

         if(!varianceAggregates.isEmpty())
         {
            Map<SummaryKey, Map<String, AggregatesInterface<?, ?>>> varianceMap    = varianceAggregates.getOrDefault(view.getName(), Collections.emptyMap());
            Map<String, AggregatesInterface<?, ?>>                  varianceSubMap = varianceMap.getOrDefault(summaryKey, Collections.emptyMap());
            Map<String, Serializable>                               varianceValues = getSummaryValuesForInterpreter(varianceSubMap);
            variableInterpreter.addValueMap("variancePivot", varianceValues);
            variableInterpreter.addValueMap("variance", varianceValues);
         }

         QRecord summaryRow = new QRecord();
         summaryRows.add(summaryRow);

         ////////////////////////////
         // add the summary values //
         ////////////////////////////
         for(Pair<String, Serializable> key : summaryKey.getKeys())
         {
            summaryRow.setValue(key.getA(), key.getB());
         }

         ///////////////////////////////////////////////////////////////////////////////
         // for summary subtotals, add the text "Total" to the last field in this key //
         ///////////////////////////////////////////////////////////////////////////////
         if(summaryKey.getKeys().size() < view.getSummaryFields().size())
         {
            String fieldName = summaryKey.getKeys().get(summaryKey.getKeys().size() - 1).getA();
            summaryRow.setValue(fieldName, summaryRow.getValueString(fieldName) + " Total");
         }

         ///////////////////////////
         // add the column values //
         ///////////////////////////
         for(QReportField column : view.getColumns())
         {
            Serializable serializable = getValueForColumn(variableInterpreter, column);
            summaryRow.setValue(column.getName(), serializable);
            thisRowValues.put(column.getName(), serializable);
         }
      }

      //////////////////////////////////////////////////////////////////////////////////////
      // sort the summary rows                                                            //
      // Note - this will NOT work correctly if there's more than 1 pivot field, as we're //
      // not doing anything to keep related rows them together (e.g., all MO state rows)  //
      //////////////////////////////////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(view.getOrderByFields()))
      {
         summaryRows.sort((o1, o2) -> summaryRowComparator(view, o1, o2));
      }

      ////////////////
      // totals row //
      ////////////////
      QRecord totalRow = null;
      if(view.getIncludeTotalRow())
      {
         totalRow = new QRecord();

         for(String summaryField : view.getSummaryFields())
         {
            if(totalRow.getValues().isEmpty())
            {
               totalRow.setValue(summaryField, "Totals");
            }
         }

         Map<String, Serializable> totalValues = getSummaryValuesForInterpreter(totalAggregates);
         variableInterpreter.addValueMap("pivot", totalValues);
         variableInterpreter.addValueMap("summary", totalValues);

         Map<String, Serializable> varianceTotalValues = getSummaryValuesForInterpreter(varianceTotalAggregates);
         variableInterpreter.addValueMap("variancePivot", varianceTotalValues);
         variableInterpreter.addValueMap("variance", varianceTotalValues);

         HashMap<String, Serializable> thisRowValues = new HashMap<>();
         variableInterpreter.addValueMap("thisRow", thisRowValues);

         for(QReportField column : view.getColumns())
         {
            Serializable serializable = getValueForColumn(variableInterpreter, column);
            totalRow.setValue(column.getName(), serializable);
            thisRowValues.put(column.getName(), serializable);
         }
      }

      return (new SummaryOutput(summaryRows, title, totalRow));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getTitle(QReportView view, QMetaDataVariableInterpreter variableInterpreter)
   {
      String title = null;
      if(view.getTitleFields() != null && StringUtils.hasContent(view.getTitleFormat()))
      {
         List<String> titleValues = new ArrayList<>();
         for(String titleField : view.getTitleFields())
         {
            titleValues.add(variableInterpreter.interpret(titleField));
         }

         title = QValueFormatter.formatStringWithValues(view.getTitleFormat(), titleValues);
      }
      else if(StringUtils.hasContent(view.getTitleFormat()))
      {
         title = view.getTitleFormat();
      }

      return title;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable getValueForColumn(QMetaDataVariableInterpreter variableInterpreter, QReportField column) throws QFormulaException
   {
      String       formula = column.getFormula();
      Serializable result;
      if(formula.startsWith("=") && formula.length() > 1)
      {
         result = FormulaInterpreter.interpretFormula(variableInterpreter, formula.substring(1));
      }
      else
      {
         result = variableInterpreter.interpretForObject(formula, null);
      }
      return (result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings({ "rawtypes", "unchecked" })
   private int summaryRowComparator(QReportView view, QRecord o1, QRecord o2)
   {
      if(o1 == o2)
      {
         return (0);
      }

      for(QFilterOrderBy orderByField : view.getOrderByFields())
      {
         Comparable c1 = (Comparable) o1.getValue(orderByField.getFieldName());
         Comparable c2 = (Comparable) o2.getValue(orderByField.getFieldName());

         if(c1 == null && c2 == null)
         {
            continue;
         }
         if(c1 == null)
         {
            return (orderByField.getIsAscending() ? -1 : 1);
         }
         if(c2 == null)
         {
            return (orderByField.getIsAscending() ? 1 : -1);
         }

         int comp = orderByField.getIsAscending() ? c1.compareTo(c2) : c2.compareTo(c1);
         if(comp != 0)
         {
            return (comp);
         }
      }

      return (0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, Serializable> getSummaryValuesForInterpreter(Map<String, AggregatesInterface<?, ?>> fieldAggregates)
   {
      Map<String, Serializable> summaryValuesForInterpreter = new HashMap<>();
      for(Map.Entry<String, AggregatesInterface<?, ?>> subEntry : fieldAggregates.entrySet())
      {
         String                    fieldName  = subEntry.getKey();
         AggregatesInterface<?, ?> aggregates = subEntry.getValue();
         summaryValuesForInterpreter.put("sum." + fieldName, aggregates.getSum());
         summaryValuesForInterpreter.put("count." + fieldName, aggregates.getCount());
         summaryValuesForInterpreter.put("count_nums." + fieldName, aggregates.getCount());
         summaryValuesForInterpreter.put("min." + fieldName, aggregates.getMin());
         summaryValuesForInterpreter.put("max." + fieldName, aggregates.getMax());
         summaryValuesForInterpreter.put("average." + fieldName, aggregates.getAverage());
         summaryValuesForInterpreter.put("product." + fieldName, aggregates.getProduct());
         summaryValuesForInterpreter.put("var." + fieldName, aggregates.getVariance());
         summaryValuesForInterpreter.put("varp." + fieldName, aggregates.getVarP());
         summaryValuesForInterpreter.put("std_dev." + fieldName, aggregates.getStandardDeviation());
         summaryValuesForInterpreter.put("std_devp." + fieldName, aggregates.getStdDevP());
      }
      return summaryValuesForInterpreter;
   }



   /*******************************************************************************
    ** record to serve as tuple/multi-value output of computeSummaryRowsForView method.
    *******************************************************************************/
   private record SummaryOutput(List<QRecord> summaryRows, String titleRow, QRecord totalRow)
   {
   }

}
