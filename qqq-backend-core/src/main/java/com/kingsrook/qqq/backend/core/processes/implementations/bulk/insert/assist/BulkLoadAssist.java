/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist;


import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.actions.ai.AiDataClassifications;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertStepUtils;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.FileToRowsInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadMappingSuggester;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadTableStructureBuilder;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfileField;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Assisted bulk load (design spec #1171 sections 3.2 and 3.3): stateless
 ** operations over an uploaded file, keyed by its upload ref, run as the
 ** current session's user.
 **
 ** - {@link #profile}: the header guess and per-column shape statistics, with
 **   sample values only where the PHI rule allows;
 ** - {@link #suggestMapping}: core's BulkLoadMappingSuggester, plus what it
 **   leaves unmapped and the fields that need value mapping;
 ** - {@link #validate}: a dry run through the table's real bulk-insert
 **   process (headless, stopped at its review step, so nothing is stored).
 **   There is no second validation path.
 **
 ** Every operation requires the table's bulk-insert process permission.
 ** Sample values and file values never go to the log.
 *******************************************************************************/
public class BulkLoadAssist
{
   private static final QLogger LOG = QLogger.getLogger(BulkLoadAssist.class);

   public static final int DEFAULT_MAX_PROFILE_ROWS = 1000;
   public static final int DEFAULT_SAMPLE_COUNT     = 5;
   public static final int MAX_SAMPLE_COUNT         = 20;
   public static final int MAX_DISTINCT_COUNTED     = 1000;
   public static final int MAX_ERRORS               = 100;

   /** The process value the headless receive-file-mapping step reads an ad-hoc profile from. */
   public static final String VALUE_BULK_LOAD_PROFILE_JSON = "bulkLoadProfileJSON";

   /** The process value BulkInsertExtractStep reads a row limit from (the SAMPLE dry run). */
   public static final String VALUE_ROW_LIMIT = "bulkLoadRowLimit";

   /** Rows a SAMPLE dry run validates. */
   public static final int SAMPLE_ROWS = 100;

   private static final Pattern INTEGER_PATTERN   = Pattern.compile("^[-+]?\\d{1,18}$");
   private static final Pattern BOOLEAN_PATTERN   = Pattern.compile("^(?i)(true|false|yes|no|y|n)$");
   private static final Pattern DATE_PATTERN      = Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}/\\d{1,2}/\\d{2,4})$");
   private static final Pattern DATE_TIME_PATTERN = Pattern.compile("^(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}/\\d{1,2}/\\d{2,4})[ T]\\d{1,2}:\\d{2}[0-9:. aApPmMZ+-]{0,24}$");
   private static final Pattern EMAIL_PATTERN     = Pattern.compile("^[^@\\s]{1,64}@[^@\\s.]{1,63}(\\.[^@\\s.]{1,63}){1,8}$");
   private static final Pattern CODE_PATTERN      = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_\\-.]{0,39}$");

   /** Operation modes of {@link #validate}. */
   public enum Mode
   {
      SAMPLE,
      FULL
   }



   /*******************************************************************************
    ** The header guess and column profiles of an upload. A column's values are
    ** included (up to sampleCount) only when the route is HIPAA, or the column
    ** maps to a field that is unclassified or PUBLIC; unmapped columns are
    ** treated as classified (fail closed).
    *******************************************************************************/
   public BulkLoadAssistModels.UploadProfile profile(String tableName, String uploadRef, BulkLoadAiRoute route, Integer sampleCount) throws QException
   {
      QTableMetaData table   = requireTable(tableName);
      int            samples = Math.max(0, Math.min(MAX_SAMPLE_COUNT, sampleCount == null ? DEFAULT_SAMPLE_COUNT : sampleCount));
      requireBulkInsertPermission(table);
      StorageInput storageInput = BulkLoadUploadSources.open(uploadRef);

      try(InputStream inputStream = new StorageAction().getInputStream(storageInput);
         FileToRowsInterface rows = FileToRowsInterface.forFile(storageInput.getReference(), inputStream))
      {
         BulkLoadFileRow firstRow = rows.hasNext() ? rows.next() : null;
         if(firstRow == null)
         {
            return (new BulkLoadAssistModels.UploadProfile(uploadRef, fileName(storageInput), true, 0, List.of()));
         }

         ////////////////////////////////////////////////////////////////////
         // header guess: if the suggester maps nothing from row 1, it is  //
         // data, and the columns are named by letter                      //
         ////////////////////////////////////////////////////////////////////
         List<String>    headers      = rowAsStrings(firstRow);
         BulkLoadProfile suggested    = suggest(tableName, headers, false);
         boolean         hasHeaderRow = CollectionUtils.nullSafeHasContents(suggested.getFieldList());
         if(!hasHeaderRow)
         {
            headers = new ArrayList<>();
            for(int i = 0; i < firstRow.size(); i++)
            {
               headers.add("Column " + columnLetter(i));
            }
         }

         Map<Integer, String> fieldByColumn = new LinkedHashMap<>();
         for(BulkLoadProfileField field : CollectionUtils.nonNullList(suggested.getFieldList()))
         {
            if(field.getColumnIndex() != null)
            {
               fieldByColumn.putIfAbsent(field.getColumnIndex(), field.getFieldName());
            }
         }

         List<ColumnStats> stats = new ArrayList<>();
         for(int i = 0; i < headers.size(); i++)
         {
            String             fieldName      = fieldByColumn.get(i);
            DataClassification classification = fieldName == null ? null : classificationOf(table, fieldName);
            boolean            allowed        = route == BulkLoadAiRoute.HIPAA || (fieldName != null && isUnclassified(classification));
            stats.add(new ColumnStats(i, headers.get(i), fieldName, classification, allowed ? samples : 0));
         }

         int rowsProfiled = 0;
         if(!hasHeaderRow)
         {
            accept(stats, firstRow);
            rowsProfiled++;
         }
         while(rows.hasNext() && rowsProfiled < DEFAULT_MAX_PROFILE_ROWS)
         {
            BulkLoadFileRow row = rows.next();
            while(stats.size() < row.size())
            {
               ////////////////////////////////////////////////////////////
               // a data row wider than the header: unnamed, unmapped,   //
               // so no samples                                          //
               ////////////////////////////////////////////////////////////
               int index = stats.size();
               stats.add(new ColumnStats(index, "Column " + columnLetter(index), null, null, route == BulkLoadAiRoute.HIPAA ? samples : 0));
            }
            accept(stats, row);
            rowsProfiled++;
         }

         List<BulkLoadAssistModels.ColumnProfile> columns = new ArrayList<>();
         for(ColumnStats columnStats : stats)
         {
            columns.add(columnStats.toProfile(rowsProfiled));
         }
         return (new BulkLoadAssistModels.UploadProfile(uploadRef, fileName(storageInput), hasHeaderRow, rowsProfiled, columns));
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         LOG.warn("Error profiling an upload", e, logPair("tableName", tableName));
         throw (new QUserFacingException("The file could not be read: " + e.getMessage(), e));
      }
   }



   /*******************************************************************************
    ** A suggested mapping for an upload's header row, with the columns it
    ** leaves unmapped, the required fields it does not fill, and the mapped
    ** fields with a possible value source (whose file values need mapping).
    *******************************************************************************/
   public BulkLoadAssistModels.Suggestion suggestMapping(String tableName, String uploadRef, boolean isBulkEdit) throws QException
   {
      QTableMetaData table = requireTable(tableName);
      requireBulkInsertPermission(table);
      StorageInput storageInput = BulkLoadUploadSources.open(uploadRef);

      List<String> headers;
      try(InputStream inputStream = new StorageAction().getInputStream(storageInput);
         FileToRowsInterface rows = FileToRowsInterface.forFile(storageInput.getReference(), inputStream))
      {
         headers = rows.hasNext() ? rowAsStrings(rows.next()) : List.of();
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QUserFacingException("The file could not be read: " + e.getMessage(), e));
      }

      BulkLoadProfile profile = suggest(tableName, headers, isBulkEdit);

      Set<Integer> mappedColumns = new HashSet<>();
      Set<String>  mappedFields  = new HashSet<>();
      List<BulkLoadAssistModels.FieldRef> valueMappingFields = new ArrayList<>();
      for(BulkLoadProfileField field : CollectionUtils.nonNullList(profile.getFieldList()))
      {
         mappedColumns.add(field.getColumnIndex());
         mappedFields.add(field.getFieldName());
         QFieldMetaData tableField = table.getFields().get(field.getFieldName());
         if(tableField != null && StringUtils.hasContent(tableField.getPossibleValueSourceName()))
         {
            valueMappingFields.add(new BulkLoadAssistModels.FieldRef(tableField.getName(), tableField.getLabel(), tableField.getPossibleValueSourceName()));
         }
      }

      List<BulkLoadAssistModels.ColumnRef> unmappedColumns = new ArrayList<>();
      for(int i = 0; i < headers.size(); i++)
      {
         if(!mappedColumns.contains(i))
         {
            unmappedColumns.add(new BulkLoadAssistModels.ColumnRef(i, headers.get(i)));
         }
      }

      List<BulkLoadAssistModels.FieldRef> unmappedRequiredFields = new ArrayList<>();
      for(QFieldMetaData field : table.getFields().values())
      {
         if(field.getIsRequired() && field.getIsEditable() && field.getDefaultValue() == null && !field.getName().equals(table.getPrimaryKeyField())
            && !mappedFields.contains(field.getName()))
         {
            unmappedRequiredFields.add(new BulkLoadAssistModels.FieldRef(field.getName(), field.getLabel(), field.getPossibleValueSourceName()));
         }
      }

      return (new BulkLoadAssistModels.Suggestion(profile, unmappedColumns, unmappedRequiredFields, valueMappingFields));
   }



   /*******************************************************************************
    ** A dry run of a profile over an upload, through the table's bulk-insert
    ** process: headless, starting after the file mapping screen and stopping at
    ** the review screen (FrontendStepBehavior.BREAK), so nothing is stored.
    ** Both run the process's full validation step (the same transform and
    ** value mapping a real load uses); SAMPLE limits it to the first
    ** SAMPLE_ROWS rows of the file.
    *******************************************************************************/
   public BulkLoadAssistModels.Validation validate(String tableName, String uploadRef, BulkLoadProfile profile, Mode mode, BulkLoadAiRoute route) throws QException
   {
      QTableMetaData table = requireTable(tableName);
      if(profile == null || CollectionUtils.nullSafeIsEmpty(profile.getFieldList()))
      {
         throw (new QUserFacingException("A profile with at least one mapped field is required."));
      }
      if(Boolean.TRUE.equals(profile.getIsBulkEdit()))
      {
         throw (new QUserFacingException("Dry runs support bulk insert profiles; this profile is for bulk edit."));
      }
      for(BulkLoadProfileField field : profile.getFieldList())
      {
         if(field.getFieldName() == null || (!field.getFieldName().contains(".") && !table.getFields().containsKey(field.getFieldName())))
         {
            throw (new QUserFacingException("The profile maps an unknown field: " + field.getFieldName() + "."));
         }
      }
      if(profile.getVersion() == null)
      {
         profile.setVersion("v1");
      }
      if(profile.getLayout() == null)
      {
         profile.setLayout("FLAT");
      }
      if(profile.getHasHeaderRow() == null)
      {
         profile.setHasHeaderRow(true);
      }

      String processName = requireBulkInsertPermission(table);
      Mode   runMode     = mode == null ? Mode.SAMPLE : mode;

      StorageInput storageInput = BulkLoadUploadSources.open(uploadRef);

      ////////////////////////////////////////////////////////////////////////
      // start the process (it stops at its upload screen), then continue   //
      // it after the file mapping screen, headless, with the profile       //
      ////////////////////////////////////////////////////////////////////////
      RunProcessInput startInput = new RunProcessInput();
      startInput.setProcessName(processName);
      startInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      startInput.addValue("tableName", table.getName());
      RunProcessOutput started = new RunProcessAction().execute(startInput);
      if(started.getException().isPresent())
      {
         throw (new QException("The dry run could not start: " + started.getException().get().getMessage(), started.getException().get()));
      }

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(processName);
      runProcessInput.setProcessUUID(started.getProcessUUID());
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      runProcessInput.setStartAfterStep("fileMapping");
      runProcessInput.addValue("tableName", table.getName());
      BulkInsertStepUtils.setStorageInputForTheFile(runProcessInput, storageInput);
      BulkInsertStepUtils.setHeadless(runProcessInput);
      runProcessInput.addValue(VALUE_BULK_LOAD_PROFILE_JSON, JsonUtils.toJson(profile));
      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      if(runMode == Mode.SAMPLE)
      {
         runProcessInput.addValue(VALUE_ROW_LIMIT, SAMPLE_ROWS);
      }

      ////////////////////////////////////////////////////////////////////////
      // seam (spec #1171 3.2): a FULL run over the durable threshold will  //
      // become a VALIDATE-mode durable job and return its jobId; until the //
      // jobs kernel lands, every run is synchronous and jobId is null.     //
      ////////////////////////////////////////////////////////////////////////
      RunProcessOutput output = new RunProcessAction().execute(runProcessInput);
      if(output.getException().isPresent())
      {
         Exception exception = output.getException().get();
         if(exception instanceof QUserFacingException userFacing)
         {
            throw (userFacing);
         }
         throw (new QException("The dry run failed: " + exception.getMessage(), exception));
      }

      Set<String> classifiedLabels = classifiedLabels(table);
      boolean     mask             = route != BulkLoadAiRoute.HIPAA && !classifiedLabels.isEmpty();

      return (summarize(table, processName, runMode, output, mask, classifiedLabels));
   }



   /*******************************************************************************
    ** The process's validation summary lines (over the whole file for FULL, the
    ** first SAMPLE_ROWS rows for SAMPLE), masked as needed.
    *******************************************************************************/
   private BulkLoadAssistModels.Validation summarize(QTableMetaData table, String processName, Mode mode, RunProcessOutput output, boolean mask, Set<String> classifiedLabels)
   {
      List<BulkLoadAssistModels.SummaryLine> lines  = new ArrayList<>();
      List<BulkLoadAssistModels.RowError>    errors = new ArrayList<>();
      int okCount    = 0;
      int errorCount = 0;

      Serializable summary = output.getValues().get(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY);
      if(summary instanceof List<?> list)
      {
         for(Object item : list)
         {
            if(!(item instanceof ProcessSummaryLineInterface line))
            {
               continue;
            }

            String       status  = line.getStatus() == null ? null : line.getStatus().name();
            Integer      count   = line instanceof ProcessSummaryLine psl ? psl.getCount() : null;
            String       message = line.getMessage();
            List<String> bullets = line instanceof ProcessSummaryLine psl && psl.getBulletsOfText() != null ? new ArrayList<>(psl.getBulletsOfText()) : List.of();

            boolean isProblem  = line.getStatus() == Status.ERROR || line.getStatus() == Status.WARNING;
            String  rowNumbers = isProblem ? rowNumbersFromBullets(bullets) : null;
            if(isProblem && mask && mentionsAny(message, classifiedLabels))
            {
               ///////////////////////////////////////////////////////////
               // the bullets quote file values: keep the count only   //
               ///////////////////////////////////////////////////////////
               bullets = List.of();
            }
            lines.add(new BulkLoadAssistModels.SummaryLine(status, count, message, bullets));

            if(line.getStatus() == Status.OK && count != null)
            {
               okCount += count;
            }
            else if(line.getStatus() == Status.ERROR && count != null)
            {
               errorCount += count;
               if(errors.size() < MAX_ERRORS)
               {
                  errors.add(new BulkLoadAssistModels.RowError(rowNumbers, fieldNameFromMessage(table, message), errorCode(message), message));
               }
            }
         }
      }

      Integer recordCount = ValueUtils.getValueAsInteger(output.getValues().get(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT));
      return (new BulkLoadAssistModels.Validation(mode.name(), processName, recordCount, okCount, errorCount, lines, errors, mode == Mode.SAMPLE ? recordCount : null, null));
   }



   /*******************************************************************************
    ** The table's bulk-insert process name, after checking the session may run
    ** it (else QPermissionDeniedException).
    *******************************************************************************/
   static String requireBulkInsertPermission(QTableMetaData table) throws QException
   {
      String processName = table.getName() + ".bulkInsert";
      if(QContext.getQInstance().getProcess(processName) == null)
      {
         throw (new QNotFoundException("The table " + table.getName() + " does not support bulk load."));
      }
      PermissionsHelper.checkProcessPermissionThrowing(new RunProcessInput().withProcessName(processName), processName);
      return (processName);
   }



   /*******************************************************************************
    ** The table, or QNotFoundException.
    *******************************************************************************/
   static QTableMetaData requireTable(String tableName) throws QException
   {
      QTableMetaData table = tableName == null ? null : QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         throw (new QNotFoundException("Could not find a table named " + tableName + "."));
      }
      return (table);
   }



   /*******************************************************************************
    ** Core's suggester over a header row.
    *******************************************************************************/
   static BulkLoadProfile suggest(String tableName, List<String> headers, boolean isBulkEdit) throws QException
   {
      return (new BulkLoadMappingSuggester().suggestBulkLoadMappingProfile(BulkLoadTableStructureBuilder.buildTableStructure(tableName, isBulkEdit), headers, isBulkEdit));
   }



   /*******************************************************************************
    ** A field's classification (falling back to the table's).
    *******************************************************************************/
   static DataClassification classificationOf(QTableMetaData table, String fieldName)
   {
      return (AiDataClassifications.ofField(table, fieldName));
   }



   /*******************************************************************************
    ** Unclassified or PUBLIC.
    *******************************************************************************/
   static boolean isUnclassified(DataClassification classification)
   {
      return (classification == null || classification == DataClassification.PUBLIC);
   }



   /*******************************************************************************
    ** Labels (and names) of the table's classified fields.
    *******************************************************************************/
   static Set<String> classifiedLabels(QTableMetaData table)
   {
      Set<String> labels = new HashSet<>();
      for(QFieldMetaData field : table.getFields().values())
      {
         if(!isUnclassified(classificationOf(table, field.getName())))
         {
            labels.add(field.getName());
            if(field.getLabel() != null)
            {
               labels.add(field.getLabel());
            }
         }
      }
      return (labels);
   }



   /*******************************************************************************
    ** Whether a message names one of the labels ("[Label]" or the bare label).
    *******************************************************************************/
   static boolean mentionsAny(String message, Set<String> labels)
   {
      if(message == null)
      {
         return (false);
      }
      for(String label : labels)
      {
         if(message.contains("[" + label + "]") || message.contains(label))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** The field a message names ("[Label]"), when one does.
    *******************************************************************************/
   static String fieldNameFromMessage(QTableMetaData table, String message)
   {
      if(message == null)
      {
         return (null);
      }
      for(QFieldMetaData field : table.getFields().values())
      {
         if((field.getLabel() != null && message.contains("[" + field.getLabel() + "]")) || message.contains("[" + field.getName() + "]"))
         {
            return (field.getName());
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** The row numbers of a summary line's "Row 3 [value]" bullets, without the
    ** quoted values ("Row 3; Rows 5-6").
    *******************************************************************************/
   static String rowNumbersFromBullets(List<String> bullets)
   {
      List<String> rows = new ArrayList<>();
      for(String bullet : CollectionUtils.nonNullList(bullets))
      {
         if(bullet != null && bullet.toLowerCase().startsWith("row"))
         {
            int bracket = bullet.indexOf('[');
            rows.add((bracket < 0 ? bullet : bullet.substring(0, bracket)).strip());
         }
      }
      return (rows.isEmpty() ? null : String.join("; ", rows));
   }



   /*******************************************************************************
    ** A stable code for an error line's kind.
    *******************************************************************************/
   static String errorCode(String message)
   {
      String lower = message == null ? "" : message.toLowerCase();
      if(lower.contains("cannot convert value"))
      {
         return ("VALUE_TYPE");
      }
      if(lower.contains("unrecognized value") || lower.contains("not a valid option"))
      {
         return ("POSSIBLE_VALUE");
      }
      if(lower.contains("unique key") || lower.contains("duplicate"))
      {
         return ("DUPLICATE");
      }
      if(lower.contains("required"))
      {
         return ("REQUIRED");
      }
      return ("OTHER");
   }



   /*******************************************************************************
    ** The row's values as strings (blank for null).
    *******************************************************************************/
   static List<String> rowAsStrings(BulkLoadFileRow row)
   {
      List<String> values = new ArrayList<>();
      for(int i = 0; i < row.size(); i++)
      {
         String value = ValueUtils.getValueAsString(row.getValueElseNull(i));
         values.add(value == null ? "" : value.strip());
      }
      return (values);
   }



   /*******************************************************************************
    ** A, B, ... Z, AA, AB ...
    *******************************************************************************/
   static String columnLetter(int index)
   {
      StringBuilder letters = new StringBuilder();
      int           n       = index + 1;
      while(n > 0)
      {
         int remainder = (n - 1) % 26;
         letters.insert(0, (char) ('A' + remainder));
         n = (n - 1) / 26;
      }
      return (letters.toString());
   }



   /*******************************************************************************
    ** The upload's file name (the last segment of its reference).
    *******************************************************************************/
   static String fileName(StorageInput storageInput)
   {
      String reference = storageInput.getReference();
      if(reference == null)
      {
         return (null);
      }
      int slash = Math.max(reference.lastIndexOf('/'), reference.lastIndexOf('\\'));
      return (slash < 0 ? reference : reference.substring(slash + 1));
   }



   /*******************************************************************************
    ** Feed one row to every column's stats.
    *******************************************************************************/
   private static void accept(List<ColumnStats> stats, BulkLoadFileRow row)
   {
      for(ColumnStats columnStats : stats)
      {
         columnStats.accept(row.getValueElseNull(columnStats.index));
      }
   }



   /*******************************************************************************
    ** Running statistics of one column. Sample values are kept only when
    ** allowed (sampleLimit greater than 0); the distinct set is capped.
    *******************************************************************************/
   private static class ColumnStats
   {
      private final int                index;
      private final String             header;
      private final String             fieldName;
      private final DataClassification classification;
      private final int                sampleLimit;

      private int     blanks;
      private int     values;
      private int     maxLength;
      private boolean allInteger  = true;
      private boolean allDecimal  = true;
      private boolean allBoolean  = true;
      private boolean allDate     = true;
      private boolean allDateTime = true;
      private boolean allEmail    = true;
      private boolean allCode     = true;
      private boolean capped;

      private final Set<String>  distinct = new HashSet<>();
      private final List<String> samples  = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      ColumnStats(int index, String header, String fieldName, DataClassification classification, int sampleLimit)
      {
         this.index = index;
         this.header = header;
         this.fieldName = fieldName;
         this.classification = classification;
         this.sampleLimit = sampleLimit;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      void accept(Serializable raw)
      {
         String value = raw == null ? null : ValueUtils.getValueAsString(raw);
         if(value == null || value.isBlank())
         {
            blanks++;
            return;
         }
         value = value.strip();
         values++;
         maxLength = Math.max(maxLength, value.length());

         if(distinct.size() < MAX_DISTINCT_COUNTED)
         {
            distinct.add(value);
         }
         else if(!distinct.contains(value))
         {
            capped = true;
         }
         if(samples.size() < sampleLimit && !samples.contains(value))
         {
            samples.add(value);
         }

         boolean typedDate     = raw instanceof LocalDate;
         boolean typedDateTime = raw instanceof LocalDateTime || (raw instanceof Temporal && !typedDate);
         boolean typedNumber   = raw instanceof Number || raw instanceof BigDecimal;

         if(allInteger && !((typedNumber && !(raw instanceof BigDecimal bd && bd.stripTrailingZeros().scale() > 0)) || INTEGER_PATTERN.matcher(value).matches()))
         {
            allInteger = false;
         }
         if(allDecimal && !(typedNumber || isDecimal(value)))
         {
            allDecimal = false;
         }
         if(allBoolean && !(raw instanceof Boolean || BOOLEAN_PATTERN.matcher(value).matches()))
         {
            allBoolean = false;
         }
         if(allDate && !(typedDate || DATE_PATTERN.matcher(value).matches()))
         {
            allDate = false;
         }
         if(allDateTime && !(typedDateTime || DATE_TIME_PATTERN.matcher(value).matches()))
         {
            allDateTime = false;
         }
         if(allEmail && !EMAIL_PATTERN.matcher(value).matches())
         {
            allEmail = false;
         }
         if(allCode && !CODE_PATTERN.matcher(value).matches())
         {
            allCode = false;
         }
      }



      /*******************************************************************************
       ** Whether a value parses as a decimal number (no regex, so no ReDOS).
       *******************************************************************************/
      private static boolean isDecimal(String value)
      {
         try
         {
            new BigDecimal(value);
            return (true);
         }
         catch(NumberFormatException e)
         {
            return (false);
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      BulkLoadAssistModels.ColumnProfile toProfile(int rows)
      {
         String type;
         String pattern;
         if(values == 0)
         {
            type = "STRING";
            pattern = "EMPTY";
         }
         else
         {
            type = allInteger ? "INTEGER" : allDecimal ? "DECIMAL" : allBoolean ? "BOOLEAN" : allDate ? "DATE" : allDateTime ? "DATE_TIME" : "STRING";
            pattern = allEmail ? "EMAIL_LIKE" : (allDate || allDateTime) ? "DATE_LIKE" : allDecimal ? "NUMERIC" : allCode ? "CODE_LIKE" : "TEXT";
         }

         double percentBlank = rows == 0 ? 0 : Math.round(1000.0 * blanks / rows) / 10.0;
         return (new BulkLoadAssistModels.ColumnProfile(index, columnLetter(index), header, type, percentBlank, distinct.size(), capped, maxLength, pattern,
            fieldName, classification == null ? null : classification.name(), sampleLimit > 0, List.copyOf(samples)));
      }
   }
}
