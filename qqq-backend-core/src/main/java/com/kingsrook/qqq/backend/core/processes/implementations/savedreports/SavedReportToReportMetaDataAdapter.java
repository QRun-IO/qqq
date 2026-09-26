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


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumn;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumns;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** GenerateReportAction takes in ReportMetaData.
 **
 ** This class knows how to adapt from a SavedReport to a ReportMetaData, so that
 ** we can render a saved report.
 *******************************************************************************/
public class SavedReportToReportMetaDataAdapter
{
   private static final QLogger LOG = QLogger.getLogger(SavedReportToReportMetaDataAdapter.class);

   private static Consumer<ObjectMapper> jsonMapperCustomizer = om -> om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);



   /*******************************************************************************
    **
    *******************************************************************************/
   public QReportMetaData adapt(SavedReport savedReport, ReportFormat reportFormat) throws QException
   {
      try
      {
         QInstance qInstance = QContext.getQInstance();

         QReportMetaData reportMetaData = new QReportMetaData();
         reportMetaData.setName("savedReport:" + savedReport.getId());
         reportMetaData.setLabel(savedReport.getLabel());

         /////////////////////////////////////////////////////
         // set up the data-source - e.g., table and filter //
         /////////////////////////////////////////////////////
         QReportDataSource dataSource = new QReportDataSource();
         reportMetaData.setDataSources(List.of(dataSource));
         dataSource.setName("main");

         QTableMetaData table = qInstance.getTable(savedReport.getTableName());
         dataSource.setSourceTable(savedReport.getTableName());
         dataSource.setQueryFilter(getQQueryFilter(savedReport.getQueryFilterJson()));

         //////////////////////////
         // set up the main view //
         //////////////////////////
         QReportView view = new QReportView();
         reportMetaData.setViews(ListBuilder.of(view));
         view.setName("main");
         view.setType(ReportType.TABLE);
         view.setDataSourceName(dataSource.getName());
         view.setLabel(savedReport.getLabel());
         view.setIncludeHeaderRow(true);

         ///////////////////////////////////////////////////////////////////////////////////////////////
         // columns in the saved-report should look like a serialized version of ReportColumns object //
         // map them to a list of QReportField objects                                                //
         // also keep track of what joinTables we find that we need to select                         //
         ///////////////////////////////////////////////////////////////////////////////////////////////
         ReportColumns columnsObject = getReportColumns(savedReport.getColumnsJson());

         List<QReportField> reportColumns = new ArrayList<>();
         view.setColumns(reportColumns);

         Set<String> neededJoinTables = new HashSet<>();

         for(ReportColumn column : columnsObject.extractVisibleColumns())
         {
            ////////////////////////////////////////////////////
            // figure out the field being named by the column //
            ////////////////////////////////////////////////////
            String            fieldName         = column.getName();
            FieldAndJoinTable fieldAndJoinTable = getField(savedReport, fieldName, qInstance, neededJoinTables, table);
            if(fieldAndJoinTable == null)
            {
               continue;
            }

            //////////////////////////////////////////////////
            // make a QReportField based on the table field //
            //////////////////////////////////////////////////
            reportColumns.add(makeQReportField(fieldName, fieldAndJoinTable));
         }

         ///////////////////////////////////////////////////////////////////////////////////////////
         // set up joins, if we need any                                                          //
         // note - test coverage here is provided by RDBMS module's GenerateReportActionRDBMSTest //
         ///////////////////////////////////////////////////////////////////////////////////////////
         if(!neededJoinTables.isEmpty())
         {
            List<QueryJoin> queryJoins = new ArrayList<>();
            dataSource.setQueryJoins(queryJoins);

            for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
            {
               if(neededJoinTables.contains(exposedJoin.getJoinTable()))
               {
                  QueryJoin queryJoin = new QueryJoin(exposedJoin.getJoinTable())
                     .withSelect(true)
                     .withType(QueryJoin.Type.LEFT)
                     .withBaseTableOrAlias(null)
                     .withAlias(null);

                  if(exposedJoin.getJoinPath().size() == 1)
                  {
                     //////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     // Note, this is similar logic (and comment) in QFMD ...                                                    //
                     // todo - what about a join with a longer path?  it would be nice to pass such joinNames through there too, //
                     // but what, that would actually be multiple queryJoins?  needs a fair amount of thought.                   //
                     //////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     queryJoin.setJoinMetaData(qInstance.getJoin(exposedJoin.getJoinPath().get(0)));
                  }

                  queryJoins.add(queryJoin);
               }
            }
         }

         /////////////////////////////////////////
         // if it's a pivot report, handle that //
         /////////////////////////////////////////
         if(StringUtils.hasContent(savedReport.getPivotTableJson()))
         {
            PivotTableDefinition pivotTableDefinition = getPivotTableDefinition(savedReport.getPivotTableJson());

            QReportView pivotView = new QReportView();
            reportMetaData.getViews().add(pivotView);
            pivotView.setName("pivot");
            pivotView.setLabel("Pivot Table");

            if(reportFormat != null && reportFormat.getSupportsNativePivotTables())
            {
               pivotView.setType(ReportType.PIVOT);
               pivotView.setPivotTableSourceViewName(view.getName());
               pivotView.setPivotTableDefinition(pivotTableDefinition);
            }
            else
            {
               if(!CollectionUtils.nullSafeHasContents(pivotTableDefinition.getRows()))
               {
                  throw (new QUserFacingException("To generate a pivot report in " + reportFormat + " format, it must have 1 or more Pivot Rows"));
               }

               if(CollectionUtils.nullSafeHasContents(pivotTableDefinition.getColumns()))
               {
                  throw (new QUserFacingException("To generate a pivot report in " + reportFormat + " format, it may not have any Pivot Columns"));
               }

               ///////////////////////
               // handle pivot rows //
               ///////////////////////
               List<String>         summaryFields        = new ArrayList<>();
               List<QFilterOrderBy> summaryOrderByFields = new ArrayList<>();
               for(PivotTableGroupBy row : pivotTableDefinition.getRows())
               {
                  String            fieldName         = row.getFieldName();
                  FieldAndJoinTable fieldAndJoinTable = getField(savedReport, fieldName, qInstance, neededJoinTables, table);
                  if(fieldAndJoinTable == null)
                  {
                     LOG.warn("The field for a Pivot Row wasn't found, when converting to a summary...", logPair("savedReportId", savedReport.getId()), logPair("fieldName", fieldName));
                     continue;
                  }
                  summaryFields.add(fieldName);
                  summaryOrderByFields.add(new QFilterOrderBy(fieldName));
               }

               /////////////////////////
               // handle pivot values //
               /////////////////////////
               List<QReportField> summaryViewColumns = new ArrayList<>();
               for(PivotTableValue value : pivotTableDefinition.getValues())
               {
                  String            fieldName         = value.getFieldName();
                  FieldAndJoinTable fieldAndJoinTable = getField(savedReport, fieldName, qInstance, neededJoinTables, table);
                  if(fieldAndJoinTable == null)
                  {
                     LOG.warn("The field for a Pivot Value wasn't found, when converting to a summary...", logPair("savedReportId", savedReport.getId()), logPair("fieldName", fieldName));
                     continue;
                  }

                  QReportField reportField = makeQReportField(fieldName, fieldAndJoinTable);
                  reportField.setName(fieldName + "_" + value.getFunction().name());
                  reportField.setLabel(StringUtils.ucFirst(value.getFunction().name().toLowerCase()) + " Of " + reportField.getLabel());
                  reportField.setFormula("${pivot." + value.getFunction().name().toLowerCase() + "." + fieldName + "}");
                  summaryViewColumns.add(reportField);
                  summaryOrderByFields.add(new QFilterOrderBy(reportField.getName()));
               }

               pivotView.setType(ReportType.SUMMARY);
               pivotView.setDataSourceName(dataSource.getName());
               pivotView.setIncludeHeaderRow(true);
               pivotView.setIncludeTotalRow(true);
               pivotView.setColumns(summaryViewColumns);
               pivotView.setSummaryFields(summaryFields);
               pivotView.withOrderByFields(summaryOrderByFields);
            }

            ////////////////////////////////////////////////////////////////////////////////////
            // in case the reportFormat doesn't support multiple views, and we have a pivot - //
            // then remove the data view                                                      //
            ////////////////////////////////////////////////////////////////////////////////////
            if(reportFormat != null && !reportFormat.getSupportsMultipleViews())
            {
               reportMetaData.getViews().remove(0);
            }
         }

         /////////////////////////////////////////////////////
         // add input fields, if they're in the savedReport //
         /////////////////////////////////////////////////////
         if(StringUtils.hasContent(savedReport.getInputFieldsJson()))
         {
            ////////////////////////////////////
            // todo turn on when implementing //
            ////////////////////////////////////
            // reportMetaData.setInputFields(JsonUtils.toObject(savedReport.getInputFieldsJson(), new TypeReference<>() {}), objectMapperConsumer);
            throw (new IllegalStateException("Input Fields are not yet implemented"));
         }

         return (reportMetaData);
      }
      catch(Exception e)
      {
         LOG.warn("Error adapting savedReport to reportMetaData", e, logPair("savedReportId", savedReport.getId()));
         throw (new QException("Error adapting savedReport to reportMetaData", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static PivotTableDefinition getPivotTableDefinition(String pivotTableJson) throws IOException
   {
      return JsonUtils.toObject(pivotTableJson, PivotTableDefinition.class, jsonMapperCustomizer);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ReportColumns getReportColumns(String columnsJson) throws IOException
   {
      return JsonUtils.toObject(columnsJson, ReportColumns.class, jsonMapperCustomizer);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QQueryFilter getQQueryFilter(String queryFilterJson) throws IOException
   {
      return JsonUtils.toObject(queryFilterJson, QQueryFilter.class, jsonMapperCustomizer);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QReportField makeQReportField(String fieldName, FieldAndJoinTable fieldAndJoinTable)
   {
      QReportField reportField = new QReportField();

      reportField.setName(fieldName);

      if(fieldAndJoinTable.joinTable() == null)
      {
         ////////////////////////////////////////////////////////////
         // for fields from this table, just use the field's label //
         ////////////////////////////////////////////////////////////
         reportField.setLabel(fieldAndJoinTable.field().getLabel());
      }
      else
      {
         ///////////////////////////////////////////////////////////////
         // for fields from join tables, use table label: field label //
         ///////////////////////////////////////////////////////////////
         reportField.setLabel(fieldAndJoinTable.joinTable().getLabel() + ": " + fieldAndJoinTable.field().getLabel());
      }

      if(StringUtils.hasContent(fieldAndJoinTable.field().getPossibleValueSourceName()))
      {
         reportField.setShowPossibleValueLabel(true);
      }

      reportField.setType(fieldAndJoinTable.field().getType());

      return reportField;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static FieldAndJoinTable getField(SavedReport savedReport, String fieldName, QInstance qInstance, Set<String> neededJoinTables, QTableMetaData table)
   {
      QFieldMetaData field;
      if(fieldName.contains("."))
      {
         String joinTableName = fieldName.replaceAll("\\..*", "");
         String joinFieldName = fieldName.replaceAll(".*\\.", "");

         QTableMetaData joinTable = qInstance.getTable(joinTableName);
         if(joinTable == null)
         {
            LOG.warn("Saved Report has an unrecognized join table name", logPair("savedReportId", savedReport.getId()), logPair("joinTable", joinTable), logPair("fieldName", fieldName));
            return null;
         }

         neededJoinTables.add(joinTableName);

         field = joinTable.getFields().get(joinFieldName);
         if(field == null)
         {
            LOG.warn("Saved Report has an unrecognized join field name", logPair("savedReportId", savedReport.getId()), logPair("fieldName", fieldName));
            return null;
         }

         return new FieldAndJoinTable(field, joinTable);
      }
      else
      {
         field = table.getFields().get(fieldName);
         if(field == null)
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            // frontend may often pass __checked__ (or maybe other __ prefixes in the future - so - don't warn that. //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!fieldName.startsWith("__"))
            {
               LOG.warn("Saved Report has an unexpected unrecognized field name", logPair("savedReportId", savedReport.getId()), logPair("table", table.getName()), logPair("fieldName", fieldName));
            }
            return null;
         }

         return new FieldAndJoinTable(field, null);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public record FieldAndJoinTable(QFieldMetaData field, QTableMetaData joinTable) {}
}
