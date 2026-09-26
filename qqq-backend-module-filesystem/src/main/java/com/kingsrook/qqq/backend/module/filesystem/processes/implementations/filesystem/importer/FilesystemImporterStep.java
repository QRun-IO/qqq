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

package com.kingsrook.qqq.backend.module.filesystem.processes.implementations.filesystem.importer;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.adapters.CsvToQRecordAdapter;
import com.kingsrook.qqq.backend.core.adapters.JsonToQRecordAdapter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemBackendModuleInterface;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** BackendStep for FilesystemImporter process
 **
 ** Job is to:
 ** - foreach file in the `source` table (e.g., a ONE-type filesystem table):
 **   - optionally create an archive/backup copy of the file
 **   - create a record in the `importFile` table
 **   - parse the file, creating many records in the `importRecord` table
 **   - remove the file from the `source` (if so configured (e.g., may turn off for Read-only FS))
 *******************************************************************************/
@SuppressWarnings("unchecked")
public class FilesystemImporterStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(FilesystemImporterStep.class);

   public static final String FIELD_SOURCE_TABLE        = "sourceTable";
   public static final String FIELD_FILE_FORMAT         = "fileFormat";
   public static final String FIELD_IMPORT_FILE_TABLE   = "importFileTable";
   public static final String FIELD_IMPORT_RECORD_TABLE = "importRecordTable";

   public static final String FIELD_IMPORT_SECURITY_FIELD_NAME     = "importSecurityFieldName";
   public static final String FIELD_IMPORT_SECURITY_FIELD_VALUE    = "importSecurityFieldValue";
   public static final String FIELD_IMPORT_SECURITY_VALUE_SUPPLIER = "importSecurityFieldSupplier";

   public static final String FIELD_ARCHIVE_FILE_ENABLED     = "archiveFileEnabled";
   public static final String FIELD_ARCHIVE_TABLE_NAME       = "archiveTableName";
   public static final String FIELD_ARCHIVE_PATH             = "archivePath";
   public static final String FIELD_REMOVE_FILE_AFTER_IMPORT = "removeFileAfterImport";

   public static final String FIELD_UPDATE_FILE_IF_NAME_EXISTS              = "updateFileIfNameExists";
   public static final String FIELD_MOVE_UNSTRUCTURED_FIELDS_TO_VALUES_JSON = "moveUnstructuredFieldsToValuesJson";

   private Function<QRecord, Serializable> securitySupplier = null;



   /*******************************************************************************
    ** Execute the step - using the request as input, and the result as output.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      // defer to a private method here, so we can add a type-parameter for that method to use              //
      // would think we could do that here, but get compiler error, since this method comes from base class //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      doRun(runBackendStepInput, runBackendStepOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private <F> void doRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      String  fileFormat             = runBackendStepInput.getValueString(FIELD_FILE_FORMAT);
      Boolean removeFileAfterImport  = runBackendStepInput.getValueBoolean(FIELD_REMOVE_FILE_AFTER_IMPORT);
      Boolean updateFileIfNameExists = runBackendStepInput.getValueBoolean(FIELD_UPDATE_FILE_IF_NAME_EXISTS);
      Boolean archiveFileEnabled     = runBackendStepInput.getValueBoolean(FIELD_ARCHIVE_FILE_ENABLED);

      QTableMetaData sourceTable     = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FIELD_SOURCE_TABLE));
      QTableMetaData importFileTable = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FIELD_IMPORT_FILE_TABLE));

      String missingFieldErrorPrefix = "Process " + runBackendStepInput.getProcessName() + " was misconfigured - missing value in field: ";
      Objects.requireNonNull(fileFormat, missingFieldErrorPrefix + FIELD_FILE_FORMAT);

      ///////////////////////////////////////////////////////////////////////////////////
      // list files in the backend system                                              //
      // todo - can we do this using query action, with this being a "ONE" type table? //
      ///////////////////////////////////////////////////////////////////////////////////
      QBackendMetaData                    sourceBackend    = QContext.getQInstance().getBackendForTable(sourceTable.getName());
      FilesystemBackendModuleInterface<F> sourceModule     = (FilesystemBackendModuleInterface<F>) new QBackendModuleDispatcher().getQBackendModule(sourceBackend);
      AbstractBaseFilesystemAction<F>     sourceActionBase = sourceModule.getActionBase();
      sourceActionBase.preAction(sourceBackend);
      Map<String, F> sourceFiles = getFileNames(sourceActionBase, sourceTable, sourceBackend);

      if(CollectionUtils.nullSafeIsEmpty(sourceFiles))
      {
         LOG.debug("No files found in import filesystem", logPair("sourceTable", sourceTable));
         return;
      }

      ////////////////////////////////////////////////////////
      // look up any existing file records with those names //
      ////////////////////////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(importFileTable.getName());
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("sourceFileName", QCriteriaOperator.IN, sourceFiles.keySet())));

      QueryOutput               queryOutput           = new QueryAction().execute(queryInput);
      Map<String, Serializable> existingImportedFiles = CollectionUtils.listToMap(queryOutput.getRecords(), r -> r.getValueString("sourceFileName"), r -> r.getValue("id"));

      for(Map.Entry<String, F> sourceEntry : sourceFiles.entrySet())
      {
         QBackendTransaction transaction = null;
         try
         {
            String sourceFileName = sourceEntry.getKey();
            LOG.info("Found file", logPair("fileName", sourceFileName));

            /////////////////////////////////////////////////////////
            // if filename was already imported, decide what to do //
            /////////////////////////////////////////////////////////
            boolean      alreadyImported = existingImportedFiles.containsKey(sourceFileName);
            Serializable idToUpdate      = null;
            if(alreadyImported)
            {
               //////////////////////////////////////////////////////////////////////////////////
               // todo - would we want to support importing multiple-times the same file name? //
               // possibly - if so, add it here, presumably w/ another boolean field           //
               //////////////////////////////////////////////////////////////////////////////////
               if(updateFileIfNameExists)
               {
                  LOG.info("Updating already-imported file", logPair("fileName", sourceFileName), logPair("id", idToUpdate));
                  idToUpdate = existingImportedFiles.get(sourceFileName);
               }
               else
               {
                  LOG.info("Skipping already-imported file", logPair("fileName", sourceFileName)); // todo - downgrade to debug?
                  removeSourceFileIfSoConfigured(removeFileAfterImport, sourceActionBase, sourceTable, sourceBackend, sourceFileName);
                  continue;
               }
            }

            ///////////////////////////////////
            // read the file as input stream //
            ///////////////////////////////////
            try(InputStream inputStream = sourceActionBase.readFile(sourceEntry.getValue()))
            {
               byte[] bytes = inputStream.readAllBytes();

               //////////////////////////////////////
               // archive the file, if so directed //
               //////////////////////////////////////
               String archivedPath = null;
               if(archiveFileEnabled)
               {
                  archivedPath = archiveFile(runBackendStepInput, sourceFileName, bytes);
               }

               /////////////////////////////////
               // build record for importFile //
               /////////////////////////////////
               LOG.info("Syncing file [" + sourceFileName + "]");
               QRecord importFileRecord = new QRecord()
                  .withValue("id", idToUpdate)
                  .withValue("sourceFileName", sourceFileName)
                  .withValue("archivedPath", archivedPath);

               addSecurityValue(runBackendStepInput, importFileRecord);

               //////////////////////////////////////
               // build child importRecord records //
               //////////////////////////////////////
               String content = new String(bytes);
               importFileRecord.withAssociatedRecords("importRecords", parseFileIntoRecords(runBackendStepInput, content));

               ///////////////////////////////////////////////////////////////////
               // insert the file & records (records as association under file) //
               ///////////////////////////////////////////////////////////////////
               InsertAction insertAction = new InsertAction();
               InsertInput  insertInput  = new InsertInput();
               insertInput.setTableName(importFileTable.getName());
               insertInput.setRecords(List.of(importFileRecord));

               transaction = QBackendTransaction.openFor(insertInput);
               insertInput.setTransaction(transaction);

               InsertOutput insertOutput = insertAction.execute(insertInput);

               LOG.info("Inserted insertFile & records", logPair("id", insertOutput.getRecords().get(0).getValue("id")));

               transaction.commit();
            }

            ///////////////////////////////////////////////////////////////////////////////////////////////
            // after the records are built, we can delete the file                                       //
            // if we are interrupted between the commit & the delete, then the file will be found again, //
            // and we'll either skip it or do an update, based on FIELD_UPDATE_FILE_IF_NAME_EXISTS flag  //
            ///////////////////////////////////////////////////////////////////////////////////////////////
            removeSourceFileIfSoConfigured(removeFileAfterImport, sourceActionBase, sourceTable, sourceBackend, sourceFileName);
         }
         catch(Exception e)
         {
            LOG.error("Error processing file: " + sourceEntry, e);
            if(transaction != null)
            {
               transaction.rollback();
            }
         }
         finally
         {
            if(transaction != null)
            {
               transaction.close();
            }
         }
      }
   }



   /*******************************************************************************
    ** if the process is configured w/ a security field & value, set it on the import
    ** File & Record records.
    *******************************************************************************/
   private void addSecurityValue(RunBackendStepInput runBackendStepInput, QRecord record)
   {
      String securityField = runBackendStepInput.getValueString(FIELD_IMPORT_SECURITY_FIELD_NAME);

      /////////////////////////////////////////////////////////////
      // if we're using a security supplier function, load it up //
      /////////////////////////////////////////////////////////////
      QCodeReference securitySupplierReference = (QCodeReference) runBackendStepInput.getValue(FIELD_IMPORT_SECURITY_VALUE_SUPPLIER);
      try
      {
         if(securitySupplierReference != null && securitySupplier == null)
         {
            securitySupplier = QCodeLoader.getAdHoc(Function.class, securitySupplierReference);
         }
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Error loading Security Supplier Function from QCodeReference [" + securitySupplierReference + "]", e));
      }

      ///////////////////////////////////////////////////////////////////////////////////////
      // either get the security value from the supplier, or the field value field's value //
      ///////////////////////////////////////////////////////////////////////////////////////
      Serializable securityValue = securitySupplier != null
         ? securitySupplier.apply(record)
         : runBackendStepInput.getValue(FIELD_IMPORT_SECURITY_FIELD_VALUE);

      ////////////////////////////////////////////////////////////////////
      // if we have a field name and a value, then add it to the record //
      ////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(securityField) && securityValue != null)
      {
         record.setValue(securityField, securityValue);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static <F> void removeSourceFileIfSoConfigured(Boolean removeFileAfterImport, AbstractBaseFilesystemAction<F> sourceActionBase, QTableMetaData sourceTable, QBackendMetaData sourceBackend, String sourceFileName) throws QException
   {
      if(removeFileAfterImport)
      {
         String fullBasePath = sourceActionBase.getFullBasePath(sourceTable, sourceBackend);
         LOG.info("Removing source file", logPair("path", fullBasePath + "/" + sourceFileName), logPair("sourceTable", sourceTable.getName()));
         sourceActionBase.deleteFile(sourceTable, fullBasePath + "/" + sourceFileName);
      }
      else
      {
         // todo - downgrade to debug
         LOG.info("Not configured to remove source file", logPair("sourceFileName", sourceFileName), logPair("sourceTable", sourceTable.getName()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String archiveFile(RunBackendStepInput runBackendStepInput, String sourceFileName, byte[] bytes) throws QException, IOException
   {
      String         archiveTableName = runBackendStepInput.getValueString(FIELD_ARCHIVE_TABLE_NAME);
      QTableMetaData archiveTable;
      try
      {
         archiveTable = QContext.getQInstance().getTable(archiveTableName);
      }
      catch(Exception e)
      {
         throw (new QException("Error getting archive table [" + archiveTableName + "]", e));
      }

      String archivePath = Objects.requireNonNullElse(runBackendStepInput.getValueString(FIELD_ARCHIVE_PATH), "");

      QBackendMetaData                    archiveBackend    = QContext.getQInstance().getBackendForTable(archiveTable.getName());
      FilesystemBackendModuleInterface<?> archiveModule     = (FilesystemBackendModuleInterface<?>) new QBackendModuleDispatcher().getQBackendModule(archiveBackend);
      AbstractBaseFilesystemAction<?>     archiveActionBase = archiveModule.getActionBase();
      archiveActionBase.preAction(archiveBackend);

      LocalDateTime now = LocalDateTime.now();
      String path = archiveActionBase.getFullBasePath(archiveTable, archiveBackend)
         + File.separator + archivePath
         + File.separator + now.getYear()
         + File.separator + now.getMonth()
         + File.separator + UUID.randomUUID()
         + "-" + sourceFileName.replaceAll(".*" + File.separator, "");
      path = AbstractBaseFilesystemAction.stripDuplicatedSlashes(path);

      LOG.info("Archiving file", logPair("path", path), logPair("archiveBackendName", archiveBackend.getName()), logPair("archiveTableName", archiveTable.getName()));
      archiveActionBase.writeFile(archiveBackend, archiveTable, null, path, bytes);

      return (path);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   List<QRecord> parseFileIntoRecords(RunBackendStepInput runBackendStepInput, String content) throws QException
   {
      Boolean moveUnstructuredFieldsToValuesJson = runBackendStepInput.getValueBoolean(FIELD_MOVE_UNSTRUCTURED_FIELDS_TO_VALUES_JSON);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // first, parse the content into records, w/ unknown field names - just whatever is in the CSV or JSON //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      String fileFormat = runBackendStepInput.getValueString(FIELD_FILE_FORMAT);

      List<QRecord> contentRecords = switch(fileFormat.toLowerCase())
      {
         case "csv" ->
         {
            CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
            csvToQRecordAdapter.buildRecordsFromCsv(new CsvToQRecordAdapter.InputWrapper()
               .withCsv(content)
               .withCaseSensitiveHeaders(true)
               .withCsvHeadersAsFieldNames(true)
            );
            yield (csvToQRecordAdapter.getRecordList());
         }

         case "json" -> new JsonToQRecordAdapter().buildRecordsFromJson(content, null, null);

         default -> throw (new QException("Unexpected file format: " + fileFormat));
      };

      /////////////////////////////////////////////////////////////////////////////
      // now, add some fields that we know about to those records, for returning //
      /////////////////////////////////////////////////////////////////////////////
      QTableMetaData importRecordTable = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FIELD_IMPORT_RECORD_TABLE));
      List<QRecord>  importRecordList  = new ArrayList<>();
      int            recordNo          = 1;
      for(QRecord record : contentRecords)
      {
         record.setValue("recordNo", recordNo++);
         addSecurityValue(runBackendStepInput, record);

         if(BooleanUtils.isTrue(moveUnstructuredFieldsToValuesJson))
         {
            moveUnstrucutredFieldsToValuesJsonBlob(importRecordTable, record);
         }

         importRecordList.add(record);
      }

      return (importRecordList);
   }



   /***************************************************************************
    * combine all unstructured fields of the record into a JSON blob in the "values" field
    ***************************************************************************/
   private void moveUnstrucutredFieldsToValuesJsonBlob(QTableMetaData table, QRecord record)
   {
      Map<String, Serializable> unstructuredValues = new HashMap<>();
      for(Map.Entry<String, Serializable> entry : record.getValues().entrySet())
      {
         String fieldName = entry.getKey();
         if(!table.getFields().containsKey(fieldName))
         {
            unstructuredValues.put(fieldName, entry.getValue());
         }
      }

      record.getValues().entrySet().removeIf(e -> unstructuredValues.containsKey(e.getKey()));

      String unstructuredValuesJson = JsonUtils.toJson(unstructuredValues);
      record.setValue("values", unstructuredValuesJson);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private <F> Map<String, F> getFileNames(AbstractBaseFilesystemAction<F> actionBase, QTableMetaData table, QBackendMetaData backend) throws QException
   {
      List<F> files = actionBase.listFiles(table, backend);

      /////////////////////////////////////////////////////
      // use a tree map, so files will be sorted by name //
      /////////////////////////////////////////////////////
      Map<String, F> rs = new TreeMap<>();

      for(F file : files)
      {
         String fileName = actionBase.stripBackendAndTableBasePathsFromFileName(actionBase.getFullPathForFile(file), backend, table);
         rs.put(fileName, file);
      }

      return (rs);
   }

}
