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

package com.kingsrook.qqq.backend.module.filesystem.base.actions;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.adapters.CsvToQRecordAdapter;
import com.kingsrook.qqq.backend.core.adapters.JsonToQRecordAdapter;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantSetting;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsUtil;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.utils.BackendQueryFilterUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeSupplier;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.exceptions.FilesystemException;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendVariantSetting;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for all Filesystem actions across all modules.
 **
 ** @param FILE The class that represents a file in the sub-module.  e.g.,
 *               a java.io.File, or an S3Object.
 *******************************************************************************/
public abstract class AbstractBaseFilesystemAction<FILE>
{
   private static final QLogger LOG = QLogger.getLogger(AbstractBaseFilesystemAction.class);

   protected QRecord backendVariantRecord = null;



   /*******************************************************************************
    ** List the files for a table - to be implemented in module-specific subclasses.
    *******************************************************************************/
   public List<FILE> listFiles(QTableMetaData table, QBackendMetaData backendBase) throws QException
   {
      return (listFiles(table, backendBase, null));
   }



   /***************************************************************************
    ** get the size of the specified file, null if not supported/available
    ***************************************************************************/
   public abstract Long getFileSize(FILE file);

   /***************************************************************************
    ** get the createDate of the specified file, null if not supported/available
    ***************************************************************************/
   public abstract Instant getFileCreateDate(FILE file);

   /***************************************************************************
    ** get the createDate of the specified file, null if not supported/available
    ***************************************************************************/
   public abstract Instant getFileModifyDate(FILE file);

   /*******************************************************************************
    ** List the files for a table - or optionally, just a single file name -
    ** to be implemented in module-specific subclasses.
    *******************************************************************************/
   public abstract List<FILE> listFiles(QTableMetaData table, QBackendMetaData backendBase, String requestedSingleFileName) throws QException;

   /*******************************************************************************
    ** Read the contents of a file - to be implemented in module-specific subclasses.
    *******************************************************************************/
   public abstract InputStream readFile(FILE file) throws IOException;

   /***************************************************************************
    ** Legacy signature for this method - before table & record params were added.
    ***************************************************************************/
   @Deprecated(since = "call the overload that takes table and record")
   public void writeFile(QBackendMetaData backend, String path, byte[] contents) throws IOException
   {
      writeFile(backend, null, null, path, contents);
   }

   /*******************************************************************************
    ** Write a file - to be implemented in module-specific subclasses.
    *******************************************************************************/
   public abstract void writeFile(QBackendMetaData backend, QTableMetaData table, QRecord record, String path, byte[] contents) throws IOException;

   /*******************************************************************************
    ** Get a string that represents the full path to a file.
    *******************************************************************************/
   public abstract String getFullPathForFile(FILE file);

   /*******************************************************************************
    ** In contrast with the DeleteAction, which deletes RECORDS - this is a
    ** filesystem-(or s3, sftp, etc)-specific extension to delete an entire FILE
    ** e.g., for post-ETL.
    **
    ** @throws FilesystemException if the delete is known to have failed, and the file is thought to still exit
    *******************************************************************************/
   public abstract void deleteFile(QTableMetaData table, String fileReference) throws FilesystemException;

   /*******************************************************************************
    ** Move a file from a source path, to a destination path.
    **
    ** @throws FilesystemException if the move is known to have failed
    *******************************************************************************/
   public abstract void moveFile(QInstance instance, QTableMetaData table, String source, String destination) throws FilesystemException;



   /*******************************************************************************
    ** e.g., with a base path of /foo/
    ** and a table path of /bar/
    ** and a file at /foo/bar/baz.txt
    ** give us just the baz.txt part.
    *******************************************************************************/
   public String stripBackendAndTableBasePathsFromFileName(String filePath, QBackendMetaData backend, QTableMetaData table)
   {
      String tablePath           = getFullBasePath(table, backend);
      String strippedPath        = filePath.replaceFirst(".*" + tablePath, "");
      String withoutLeadingSlash = stripLeadingSlash(strippedPath); // todo - dangerous, do all backends really want this??
      return (withoutLeadingSlash);
   }



   /*******************************************************************************
    ** Append together the backend's base path (if present), with a table's base
    ** path (again, if present).
    *******************************************************************************/
   public String getFullBasePath(QTableMetaData table, QBackendMetaData backendBase)
   {
      AbstractFilesystemBackendMetaData metaData = getBackendMetaData(AbstractFilesystemBackendMetaData.class, backendBase);

      String basePath = metaData.getBasePath();
      if(backendBase.getUsesVariants())
      {
         Map<BackendVariantSetting, String> fieldNameMap = backendBase.getBackendVariantsConfig().getBackendSettingSourceFieldNameMap();
         if(fieldNameMap.containsKey(SFTPBackendVariantSetting.BASE_PATH))
         {
            basePath = backendVariantRecord.getValueString(fieldNameMap.get(SFTPBackendVariantSetting.BASE_PATH));
         }
      }
      String fullPath = StringUtils.hasContent(basePath) ? basePath : "";

      AbstractFilesystemTableBackendDetails tableDetails = getTableBackendDetails(AbstractFilesystemTableBackendDetails.class, table);
      if(StringUtils.hasContent(tableDetails.getBasePath()))
      {
         fullPath += File.separatorChar + tableDetails.getBasePath();
      }

      fullPath += File.separatorChar;
      fullPath = stripDuplicatedSlashes(fullPath);

      return fullPath;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String stripDuplicatedSlashes(String path)
   {
      if(path == null)
      {
         return (null);
      }

      return (path.replaceAll("//+", "/"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String stripLeadingSlash(String path)
   {
      if(path == null)
      {
         return (null);
      }
      return (path.replaceFirst("^/+", ""));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String stripTrailingSlash(String path)
   {
      if(path == null)
      {
         return (null);
      }
      return (path.replaceFirst("/+$", ""));
   }



   /*******************************************************************************
    ** Get the backend metaData, type-checked as the requested type.
    *******************************************************************************/
   protected <T extends AbstractFilesystemBackendMetaData> T getBackendMetaData(Class<T> outputClass, QBackendMetaData metaData)
   {
      if(!(outputClass.isInstance(metaData)))
      {
         throw new IllegalArgumentException("MetaData was not of expected type (was " + metaData.getClass().getSimpleName() + ")");
      }
      return outputClass.cast(metaData);
   }



   /*******************************************************************************
    ** Get the backendDetails out of a table, type-checked as the requested type
    *******************************************************************************/
   protected <T extends AbstractFilesystemTableBackendDetails> T getTableBackendDetails(Class<T> outputClass, QTableMetaData tableMetaData)
   {
      QTableBackendDetails tableBackendDetails = tableMetaData.getBackendDetails();
      if(!(outputClass.isInstance(tableBackendDetails)))
      {
         throw new IllegalArgumentException("Table backend details was not of expected type (was " + tableBackendDetails.getClass().getSimpleName() + ")");
      }
      return outputClass.cast(tableBackendDetails);
   }



   /*******************************************************************************
    ** Generic implementation of the execute method from the QueryInterface
    *******************************************************************************/
   public QueryOutput executeQuery(QueryInput queryInput) throws QException
   {
      preAction(queryInput.getBackend());

      try
      {
         QTableMetaData                        table        = queryInput.getTable();
         AbstractFilesystemTableBackendDetails tableDetails = getTableBackendDetails(AbstractFilesystemTableBackendDetails.class, table);

         QueryOutput queryOutput = new QueryOutput(queryInput);

         String requestedPath = null;

         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if this is a query for a single file name, then get that file name in the requestedPath param for the listFiles call //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(queryInput.getFilter() != null)
         {
            for(QFilterCriteria criteria : CollectionUtils.nonNullList(queryInput.getFilter().getCriteria()))
            {
               if(criteria.getFieldName().equals(tableDetails.getFileNameFieldName()) && criteria.getOperator().equals(QCriteriaOperator.EQUALS))
               {
                  requestedPath = ValueUtils.getValueAsString(criteria.getValues().get(0));
               }
            }
         }

         List<FILE> files = listFiles(table, queryInput.getBackend(), requestedPath);

         switch(tableDetails.getCardinality())
         {
            case MANY -> completeExecuteQueryForManyTable(queryInput, queryOutput, files, table, tableDetails);
            case ONE -> completeExecuteQueryForOneTable(queryInput, queryOutput, files, table, tableDetails);
            default -> throw new IllegalStateException("Unexpected table cardinality: " + tableDetails.getCardinality());
         }

         return (queryOutput);
      }
      catch(Exception e)
      {
         LOG.warn("Error executing query", e);
         throw new QException("Error executing query", e);
      }
      finally
      {
         postAction();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void setRecordValueIfFieldNameHasContent(QRecord record, String fieldName, UnsafeSupplier<Serializable, ?> valueSupplier)
   {
      if(StringUtils.hasContent(fieldName))
      {
         try
         {
            record.setValue(fieldName, valueSupplier.get());
         }
         catch(Exception e)
         {
            LOG.warn("Error setting record value for field", e, logPair("fieldName", fieldName));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void completeExecuteQueryForOneTable(QueryInput queryInput, QueryOutput queryOutput, List<FILE> files, QTableMetaData table, AbstractFilesystemTableBackendDetails tableDetails) throws QException
   {
      int           recordCount = 0;
      List<QRecord> records     = new ArrayList<>();

      for(FILE file : files)
      {
         ////////////////////////////////////////////////////////////////////////////////
         // for one-record tables, put the entire file's contents into a single record //
         ////////////////////////////////////////////////////////////////////////////////
         String  filePathWithoutBase = stripBackendAndTableBasePathsFromFileName(getFullPathForFile(file), queryInput.getBackend(), table);
         QRecord record              = new QRecord();

         setRecordValueIfFieldNameHasContent(record, tableDetails.getFileNameFieldName(), () -> filePathWithoutBase);
         setRecordValueIfFieldNameHasContent(record, tableDetails.getBaseNameFieldName(), () -> stripAllPaths(filePathWithoutBase));
         setRecordValueIfFieldNameHasContent(record, tableDetails.getSizeFieldName(), () -> getFileSize(file));
         setRecordValueIfFieldNameHasContent(record, tableDetails.getCreateDateFieldName(), () -> getFileCreateDate(file));
         setRecordValueIfFieldNameHasContent(record, tableDetails.getModifyDateFieldName(), () -> getFileModifyDate(file));

         if(shouldHeavyFileContentsBeRead(queryInput, table, tableDetails))
         {
            try(InputStream inputStream = readFile(file))
            {
               byte[] bytes = inputStream.readAllBytes();
               record.withValue(tableDetails.getContentsFieldName(), bytes);
            }
            catch(Exception e)
            {
               record.addError(new SystemErrorStatusMessage("Error reading file contents: " + e.getMessage()));
            }
         }
         else
         {
            Long size = record.getValueLong(tableDetails.getSizeFieldName());
            if(size != null)
            {
               if(record.getBackendDetails() == null)
               {
                  record.setBackendDetails(new HashMap<>());
               }

               if(record.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS) == null)
               {
                  record.addBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, new HashMap<>());
               }

               ((Map<String, Serializable>) record.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS)).put(tableDetails.getContentsFieldName(), size);
            }
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////////////
         // the listFiles method may have used a "path" criteria.                                                //
         // if so, remove that criteria here, so that its presence doesn't cause all records to be filtered away //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////
         QQueryFilter filterForRecords = queryInput.getFilter();
         // if(filterForRecords != null)
         // {
         //    filterForRecords = filterForRecords.clone();
         //    CollectionUtils.nonNullList(filterForRecords.getCriteria())
         //       .removeIf(AbstractBaseFilesystemAction::isPathEqualsCriteria);
         // }

         if(BackendQueryFilterUtils.doesRecordMatch(filterForRecords, null, record))
         {
            records.add(record);
         }
      }

      BackendQueryFilterUtils.sortRecordList(queryInput.getFilter(), records);
      records = BackendQueryFilterUtils.applySkipAndLimit(queryInput.getFilter(), records);
      queryOutput.addRecords(records);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private Serializable stripAllPaths(String filePath)
   {
      if(filePath == null)
      {
         return null;
      }

      return (filePath.replaceFirst(".*/", ""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected static boolean isPathEqualsCriteria(QFilterCriteria criteria)
   {
      return "path".equals(criteria.getFieldName()) && QCriteriaOperator.EQUALS.equals(criteria.getOperator());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void completeExecuteQueryForManyTable(QueryInput queryInput, QueryOutput queryOutput, List<FILE> files, QTableMetaData table, AbstractFilesystemTableBackendDetails tableDetails) throws QException, IOException
   {
      int recordCount = 0;

      for(FILE file : files)
      {
         try(InputStream inputStream = readFile(file))
         {
            LOG.info("Extracting records from file", logPair("table", table.getName()), logPair("path", getFullPathForFile(file)));
            switch(tableDetails.getRecordFormat())
            {
               case CSV ->
               {
                  String fileContents = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                  fileContents = customizeFileContentsAfterReading(table, fileContents);

                  if(queryInput.getRecordPipe() != null)
                  {
                     new CsvToQRecordAdapter().buildRecordsFromCsv(queryInput.getRecordPipe(), fileContents, table, null, (record ->
                     {
                        ////////////////////////////////////////////////////////////////////////////////////////////
                        // Before the records go into the pipe, make sure their backend details are added to them //
                        ////////////////////////////////////////////////////////////////////////////////////////////
                        addBackendDetailsToRecord(record, file);
                     }));
                  }
                  else
                  {
                     List<QRecord> recordsInFile = new CsvToQRecordAdapter().buildRecordsFromCsv(fileContents, table, null);
                     addBackendDetailsToRecords(recordsInFile, file);
                     queryOutput.addRecords(recordsInFile);
                  }
               }
               case JSON ->
               {
                  String fileContents = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                  fileContents = customizeFileContentsAfterReading(table, fileContents);

                  // todo - pipe support!!
                  List<QRecord> recordsInFile = new JsonToQRecordAdapter().buildRecordsFromJson(fileContents, table, null);
                  addBackendDetailsToRecords(recordsInFile, file);

                  queryOutput.addRecords(recordsInFile);
               }
               default -> throw new IllegalStateException("Unexpected table record format: " + tableDetails.getRecordFormat());
            }
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static boolean shouldHeavyFileContentsBeRead(QueryInput queryInput, QTableMetaData table, AbstractFilesystemTableBackendDetails tableDetails)
   {
      boolean doReadContents = true;
      if(table.getField(tableDetails.getContentsFieldName()).getIsHeavy())
      {
         if(!queryInput.getShouldFetchHeavyFields())
         {
            doReadContents = false;
         }
      }
      return doReadContents;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public CountOutput executeCount(CountInput countInput) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(countInput.getTableName());

      QQueryFilter filter = countInput.getFilter();
      if(filter != null)
      {
         filter = filter.clone();
         filter.setSkip(null);
         filter.setLimit(null);
      }

      queryInput.setFilter(filter);
      QueryOutput queryOutput = executeQuery(queryInput);

      CountOutput countOutput = new CountOutput();
      countOutput.setCount(queryOutput.getRecords().size());
      return (countOutput);
   }



   /*******************************************************************************
    ** Add backend details to records about the file that they are in.
    *******************************************************************************/
   protected void addBackendDetailsToRecords(List<QRecord> recordsInFile, FILE file)
   {
      recordsInFile.forEach(r -> addBackendDetailsToRecord(r, file));
   }



   /*******************************************************************************
    ** Add backend details to a record about the file that it is in.
    *******************************************************************************/
   protected void addBackendDetailsToRecord(QRecord record, FILE file)
   {
      record.addBackendDetail(FilesystemRecordBackendDetailFields.FULL_PATH, getFullPathForFile(file));
   }



   /*******************************************************************************
    ** Method that subclasses can override to add pre-action things (e.g., setting up
    ** s3 client).
    *******************************************************************************/
   public void preAction(QBackendMetaData backendMetaData) throws QException
   {
      if(backendMetaData.getUsesVariants())
      {
         this.backendVariantRecord = BackendVariantsUtil.getVariantRecord(backendMetaData);
      }
   }



   /***************************************************************************
    ** Method that subclasses can override to add post-action things (e.g., closing resources)
    ***************************************************************************/
   public void postAction()
   {
      //////////////////
      // noop in base //
      //////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String customizeFileContentsAfterReading(QTableMetaData table, String fileContents) throws QException
   {
      try
      {
         Optional<QCodeReference> codeReference = table.getCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole());
         if(codeReference.isEmpty())
         {
            return (fileContents);
         }

         AbstractPostReadFileCustomizer tableCustomizer = QCodeLoader.getAdHoc(AbstractPostReadFileCustomizer.class, codeReference.get());
         if(tableCustomizer == null)
         {
            return (fileContents);
         }

         return tableCustomizer.customizeFileContents(fileContents);
      }
      catch(Exception e)
      {
         throw (new QException("Error customizing file contents", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected InsertOutput executeInsert(InsertInput insertInput) throws QException
   {
      try
      {
         preAction(insertInput.getBackend());

         InsertOutput     output  = new InsertOutput();
         QTableMetaData   table   = insertInput.getTable();
         QBackendMetaData backend = insertInput.getBackend();

         output.setRecords(new ArrayList<>());

         AbstractFilesystemTableBackendDetails tableDetails = getTableBackendDetails(AbstractFilesystemTableBackendDetails.class, table);
         if(tableDetails.getCardinality().equals(Cardinality.ONE))
         {
            for(QRecord record : insertInput.getRecords())
            {
               try
               {
                  String fullPath = stripDuplicatedSlashes(getFullBasePath(table, backend) + File.separator + record.getValueString(tableDetails.getFileNameFieldName()));
                  writeFile(backend, table, record, fullPath, record.getValueByteArray(tableDetails.getContentsFieldName()));
                  record.addBackendDetail(FilesystemRecordBackendDetailFields.FULL_PATH, fullPath);
                  output.addRecord(record);
               }
               catch(Exception e)
               {
                  record.addError(new SystemErrorStatusMessage("Error writing file: " + e.getMessage()));
                  output.addRecord(record);
               }
            }
         }
         else
         {
            throw (new NotImplementedException("Insert is not implemented for filesystem tables with cardinality: " + tableDetails.getCardinality()));
         }

         return (output);
      }
      catch(Exception e)
      {
         throw new QException("Error executing insert: " + e.getMessage(), e);
      }
      finally
      {
         postAction();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected DeleteOutput executeDelete(DeleteInput deleteInput) throws QException
   {
      try
      {
         preAction(deleteInput.getBackend());

         DeleteOutput output = new DeleteOutput();
         output.setRecordsWithErrors(new ArrayList<>());

         QTableMetaData   table   = deleteInput.getTable();
         QBackendMetaData backend = deleteInput.getBackend();

         AbstractFilesystemTableBackendDetails tableDetails = getTableBackendDetails(AbstractFilesystemTableBackendDetails.class, table);
         if(tableDetails.getCardinality().equals(Cardinality.ONE))
         {
            int deletedCount = 0;
            for(Serializable primaryKey : deleteInput.getPrimaryKeys())
            {
               try
               {
                  deleteFile(table, stripDuplicatedSlashes(getFullBasePath(table, backend) + "/" + primaryKey));
                  deletedCount++;
               }
               catch(Exception e)
               {
                  String message = ObjectUtils.tryElse(() -> ExceptionUtils.getRootException(e).getMessage(), "Message not available");
                  output.addRecordWithError(new QRecord().withValue(table.getPrimaryKeyField(), primaryKey).withError(new SystemErrorStatusMessage("Error deleting file: " + message)));
               }
            }
            output.setDeletedRecordCount(deletedCount);
         }
         else
         {
            throw (new NotImplementedException("Delete is not implemented for filesystem tables with cardinality: " + tableDetails.getCardinality()));
         }

         return (output);
      }
      catch(Exception e)
      {
         throw new QException("Error executing delete: " + e.getMessage(), e);
      }
      finally
      {
         postAction();
      }
   }

}
