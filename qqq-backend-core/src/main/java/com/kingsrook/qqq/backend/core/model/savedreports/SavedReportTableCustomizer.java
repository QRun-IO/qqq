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

package com.kingsrook.qqq.backend.core.model.savedreports;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAndJoinTable;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.SavedReportToReportMetaDataAdapter;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class SavedReportTableCustomizer implements TableCustomizerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      return (preInsertOrUpdate(records));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      Map<Serializable, QRecord> storedRecords = validateStoredOwner(records, updateInput.getTable(), updateInput.getTransaction(), "edit", true);
      for(QRecord record : records)
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            if(!storedRecords.containsKey(record.resolvePrimaryKey(updateInput.getTable())))
            {
               preValidateRecord(record);
            }
            continue;
         }
         QRecord validationRecord = new QRecord(storedRecords.get(record.resolvePrimaryKey(updateInput.getTable())));
         validationRecord.getValues().putAll(record.getValues());
         preValidateRecord(validationRecord);
         CollectionUtils.nonNullList(validationRecord.getErrors()).forEach(record::addError);
         if(record.getValues().containsKey("queryFilterJson"))
         {
            record.setValue("queryFilterJson", validationRecord.getValue("queryFilterJson"));
         }
      }
      return records;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preDelete(DeleteInput deleteInput, List<QRecord> records, boolean isPreview) throws QException
   {
      validateStoredOwner(records, deleteInput.getTable(), deleteInput.getTransaction(), "delete", false);
      return records;
   }



   /*******************************************************************************
    ** Validate saved-asset ownership using a private native snapshot. Returned
    ** read records can have hidden/transformed ownership and report content.
    ** These snapshots are only for authorization/validation, never responses.
    *******************************************************************************/
   public static Map<Serializable, QRecord> validateStoredOwner(List<QRecord> records, QTableMetaData table,
      QBackendTransaction transaction, String verb, boolean isUpdate) throws QException
   {
      QTableMetaData physicalTable = AssociatedRecordDiscovery.physicalParentTable(table);
      Set<Serializable> primaryKeys = new LinkedHashSet<>();
      for(QRecord record : records)
      {
         if(!CollectionUtils.nullSafeHasContents(record.getErrors()) && record.resolvePrimaryKey(table) != null)
         {
            primaryKeys.add(record.resolvePrimaryKey(table));
         }
      }
      Map<Serializable, QRecord> storedRecords = new HashMap<>();
      if(!primaryKeys.isEmpty())
      {
         QueryInput queryInput = new QueryInput(table.getName()).withTransaction(transaction)
            .withFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, new ArrayList<>(primaryKeys))))
            .withShouldFetchHeavyFields(true).withShouldOmitHiddenFields(false).withShouldMaskPasswords(false)
            .withFieldNamesToInclude(new HashSet<>(physicalTable.getFields().keySet()));
         queryInput.setTableMetaData(physicalTable);
         var output = new QBackendModuleDispatcher().getQBackendModule(queryInput.getBackend()).getQueryInterface().executeForDml(queryInput);
         if(output == null || output.getRecords() == null)
         {
            throw new QException("Unable to read stored saved-asset ownership.");
         }
         for(QRecord stored : output.getRecords())
         {
            if(stored == null || CollectionUtils.nullSafeHasContents(stored.getErrors()) || !stored.getValues().containsKey("userId")
               || !primaryKeys.contains(stored.resolvePrimaryKey(physicalTable)))
            {
               throw new QException("Unable to read stored saved-asset ownership.");
            }
            storedRecords.put(stored.resolvePrimaryKey(physicalTable), new QRecord(stored));
         }
      }

      String currentUserId = ObjectUtils.tryElse(() -> QContext.getQSession().getUser().getIdReference(), null);
      for(QRecord record : records)
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }
         QRecord stored = storedRecords.get(record.resolvePrimaryKey(table));
         Serializable storedOwner = stored == null ? null : stored.getValue("userId");
         boolean denied = stored == null || (storedOwner != null && !Objects.equals(storedOwner, currentUserId));
         if(isUpdate && record.getValues().containsKey("userId"))
         {
            Serializable submittedOwner = record.getValue("userId");
            denied |= (storedOwner != null && !Objects.equals(storedOwner, submittedOwner))
               || (submittedOwner != null && !Objects.equals(submittedOwner, currentUserId));
         }
         if(denied)
         {
            record.addError(new PermissionDeniedMessage("Only the owner of a " + table.getLabel() + " may " + verb + " it."));
         }
      }
      return storedRecords;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void validateOwner(List<QRecord> records, String tableName, String verb)
   {
      QTableMetaData tableMetaData = QContext.getQInstance().getTable(tableName);
      String         currentUserId = ObjectUtils.tryElse(() -> QContext.getQSession().getUser().getIdReference(), null);
      for(QRecord record : records)
      {
         if(record.getValue("userId") != null)
         {
            if(!record.getValue("userId").equals(currentUserId))
            {
               record.addError(new PermissionDeniedMessage("Only the owner of a " + tableMetaData.getLabel() + " may " + verb + " it."));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> preInsertOrUpdate(List<QRecord> records)
   {
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         preValidateRecord(record);
      }
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   void preValidateRecord(QRecord record)
   {
      try
      {
         String tableName       = record.getValueString("tableName");
         String queryFilterJson = record.getValueString("queryFilterJson");
         String columnsJson     = record.getValueString("columnsJson");
         String pivotTableJson  = record.getValueString("pivotTableJson");

         Set<String> usedColumns = new HashSet<>();

         QTableMetaData table = QContext.getQInstance().getTable(tableName);
         if(table == null)
         {
            record.addError(new BadInputStatusMessage("Unrecognized table name: " + tableName));
         }

         if(StringUtils.hasContent(queryFilterJson))
         {
            try
            {
               /////////////////////////////////////////////////////////////////////////
               // validate that we can parse the filter, then prep it for the backend //
               /////////////////////////////////////////////////////////////////////////
               QQueryFilter filter = SavedReportToReportMetaDataAdapter.getQQueryFilter(queryFilterJson);
               filter.prepForBackend();
               record.setValue("queryFilterJson", JsonUtils.toJson(filter));
            }
            catch(IOException e)
            {
               record.addError(new BadInputStatusMessage("Unable to parse queryFilterJson: " + e.getMessage()));
            }
         }

         boolean hadColumnParseError = false;
         if(StringUtils.hasContent(columnsJson))
         {
            try
            {
               /////////////////////////////////////////////////////////////////////////
               // make sure we can parse columns, and that we have at least 1 visible //
               /////////////////////////////////////////////////////////////////////////
               ReportColumns reportColumns = SavedReportToReportMetaDataAdapter.getReportColumns(columnsJson);
               for(ReportColumn column : reportColumns.extractVisibleColumns())
               {
                  usedColumns.add(column.getName());
               }
            }
            catch(IOException e)
            {
               record.addError(new BadInputStatusMessage("Unable to parse columnsJson: " + e.getMessage()));
               hadColumnParseError = true;
            }
         }

         if(usedColumns.isEmpty() && !hadColumnParseError)
         {
            record.addError(new BadInputStatusMessage("A Report must contain at least 1 column"));
         }

         if(StringUtils.hasContent(pivotTableJson))
         {
            try
            {
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // make sure we can parse pivot table, and we have ... at least 1 ... row?  maybe that's all that's needed //
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////
               PivotTableDefinition pivotTableDefinition          = SavedReportToReportMetaDataAdapter.getPivotTableDefinition(pivotTableJson);
               boolean              anyRows                       = false;
               boolean              missingAnyFieldNamesInRows    = false;
               boolean              missingAnyFieldNamesInColumns = false;
               boolean              missingAnyFieldNamesInValues  = false;
               boolean              missingAnyFunctionsInValues   = false;

               //////////////////
               // look at rows //
               //////////////////
               for(PivotTableGroupBy row : CollectionUtils.nonNullList(pivotTableDefinition.getRows()))
               {
                  anyRows = true;
                  if(StringUtils.hasContent(row.getFieldName()))
                  {
                     if(!usedColumns.contains(row.getFieldName()) && !hadColumnParseError)
                     {
                        record.addError(new BadInputStatusMessage("A pivot table row is using field (" + getFieldLabelElseName(table, row.getFieldName()) + ") which is not an active column on this report."));
                     }
                  }
                  else
                  {
                     missingAnyFieldNamesInRows = true;
                  }
               }

               if(!anyRows)
               {
                  record.addError(new BadInputStatusMessage("A Pivot Table must contain at least 1 row"));
               }

               /////////////////////
               // look at columns //
               /////////////////////
               for(PivotTableGroupBy column : CollectionUtils.nonNullList(pivotTableDefinition.getColumns()))
               {
                  if(StringUtils.hasContent(column.getFieldName()))
                  {
                     if(!usedColumns.contains(column.getFieldName()) && !hadColumnParseError)
                     {
                        record.addError(new BadInputStatusMessage("A pivot table column is using field (" + getFieldLabelElseName(table, column.getFieldName()) + ") which is not an active column on this report."));
                     }
                  }
                  else
                  {
                     missingAnyFieldNamesInColumns = true;
                  }
               }

               ////////////////////
               // look at values //
               ////////////////////
               for(PivotTableValue value : CollectionUtils.nonNullList(pivotTableDefinition.getValues()))
               {
                  if(StringUtils.hasContent(value.getFieldName()))
                  {
                     if(!usedColumns.contains(value.getFieldName()) && !hadColumnParseError)
                     {
                        record.addError(new BadInputStatusMessage("A pivot table value is using field (" + getFieldLabelElseName(table, value.getFieldName()) + ") which is not an active column on this report."));
                     }
                  }
                  else
                  {
                     missingAnyFieldNamesInValues = true;
                  }

                  if(value.getFunction() == null)
                  {
                     missingAnyFunctionsInValues = true;
                  }
               }

               ////////////////////////////////////////////////
               // errors based on missing things found above //
               ////////////////////////////////////////////////
               if(missingAnyFieldNamesInRows)
               {
                  record.addError(new BadInputStatusMessage("Missing field name for at least one pivot table row."));
               }

               if(missingAnyFieldNamesInColumns)
               {
                  record.addError(new BadInputStatusMessage("Missing field name for at least one pivot table column."));
               }

               if(missingAnyFieldNamesInValues)
               {
                  record.addError(new BadInputStatusMessage("Missing field name for at least one pivot table value."));
               }

               if(missingAnyFunctionsInValues)
               {
                  record.addError(new BadInputStatusMessage("Missing function for at least one pivot table value."));
               }
            }
            catch(IOException e)
            {
               record.addError(new BadInputStatusMessage("Unable to parse pivotTableJson: " + e.getMessage()));
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error validating a savedReport");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getFieldLabelElseName(QTableMetaData table, String fieldName)
   {
      try
      {
         FieldAndJoinTable fieldAndJoinTable = FieldAndJoinTable.get(table, fieldName);
         return (fieldAndJoinTable.getLabel(table));
      }
      catch(Exception e)
      {
         return (fieldName);
      }
   }

}
