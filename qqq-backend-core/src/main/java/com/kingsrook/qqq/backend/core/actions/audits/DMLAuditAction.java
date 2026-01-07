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

package com.kingsrook.qqq.backend.core.actions.audits;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.actions.audits.AuditAction.getRecordSecurityKeyValues;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Audit for a standard DML (Data Manipulation Language) activity - e.g.,
 ** insert, edit, or delete.
 **
 ** This action creates audit records when tables with audit rules are modified.
 ** It supports both RECORD-level auditing (simple log of changes) and FIELD-level
 ** auditing (detailed tracking of individual field changes).
 **
 ** <h2>Primary Key Type Support</h2>
 ** The audit system's support for different primary key types depends on how
 ** the audit tables are configured via {@link AuditsMetaDataProvider}:
 ** <ul>
 **    <li><b>INTEGER recordId (default):</b> Only tables with INTEGER primary
 **        keys can be audited. Tables with other PK types are silently skipped.</li>
 **    <li><b>STRING recordId:</b> Tables with any primary key type can be audited.
 **        Primary key values are converted to strings for storage.</li>
 ** </ul>
 **
 ** @see AuditsMetaDataProvider#withRecordIdType(QFieldType)
 ** @see AuditAction
 *******************************************************************************/
public class DMLAuditAction extends AbstractQActionFunction<DMLAuditInput, DMLAuditOutput>
{
   private static final QLogger LOG = QLogger.getLogger(DMLAuditAction.class);

   public static final String AUDIT_CONTEXT_FIELD_NAME = "auditContext";

   private static Set<String> loggedUnauditableTableNames = new HashSet<>();



   /*******************************************************************************
    ** Execute the DML audit action for a table operation.
    **
    ** This method creates audit records based on the configured audit level
    ** (NONE, RECORD, or FIELD) and the type of DML operation being performed.
    ** If FIELD-level auditing is enabled, individual field changes are tracked
    ** with old and new values stored in audit detail records.
    **
    ** @param input the DML audit input containing the table operation details
    **              and the records being modified
    ** @return DMLAuditOutput (currently empty, reserved for future use)
    ** @throws QException if there is an error during audit processing
    *******************************************************************************/
   @Override
   public DMLAuditOutput execute(DMLAuditInput input) throws QException
   {
      DMLAuditOutput           output           = new DMLAuditOutput();
      AbstractTableActionInput tableActionInput = input.getTableActionInput();
      List<QRecord>            oldRecordList    = input.getOldRecordList();
      QTableMetaData           table            = tableActionInput.getTable();
      long                     start            = System.currentTimeMillis();
      DMLType                  dmlType          = getDMLType(tableActionInput);

      try
      {
         List<QRecord> recordList = CollectionUtils.nonNullList(input.getRecordList()).stream()
            .filter(r -> CollectionUtils.nullSafeIsEmpty(r.getErrors())).toList();

         AuditLevel auditLevel = getAuditLevel(tableActionInput);
         if(auditLevel == null || auditLevel.equals(AuditLevel.NONE) || CollectionUtils.nullSafeIsEmpty(recordList))
         {
            /////////////////////////////////////////////
            // return with noop for null or level NONE //
            /////////////////////////////////////////////
            return (output);
         }

         ///////////////////////////////////////////////////////////////////////////////////////
         // check if the table's primary key type is compatible with the audit recordId type. //
         // if audit.recordId is INTEGER, only INTEGER PKs are supported (backwards compat).  //
         // if audit.recordId is STRING, any PK type can be audited (converted to string).    //
         ///////////////////////////////////////////////////////////////////////////////////////
         QFieldMetaData primaryKeyField = table.getFields().get(table.getPrimaryKeyField());
         if(primaryKeyField == null)
         {
            if(!loggedUnauditableTableNames.contains(table.getName()))
            {
               LOG.info("Cannot audit table without a primary key field", logPair("tableName", table.getName()));
               loggedUnauditableTableNames.add(table.getName());
            }
            return (output);
         }

         QFieldType auditRecordIdType = getAuditRecordIdFieldType();
         if(QFieldType.INTEGER.equals(auditRecordIdType) && !QFieldType.INTEGER.equals(primaryKeyField.getType()))
         {
            /////////////////////////////////////////////////////////////////////////////////////
            // audit table uses INTEGER recordId - only tables with INTEGER PKs can be audited //
            /////////////////////////////////////////////////////////////////////////////////////
            if(!loggedUnauditableTableNames.contains(table.getName()))
            {
               LOG.info("Cannot audit table with non-integer primary key when audit.recordId is INTEGER. "
                  + "Configure AuditsMetaDataProvider.withRecordIdType(QFieldType.STRING) to audit tables with non-integer PKs.",
                  logPair("tableName", table.getName()), logPair("pkType", primaryKeyField.getType()));
               loggedUnauditableTableNames.add(table.getName());
            }
            return (output);
         }

         String contextSuffix = getContentSuffix(input);

         AuditInput auditInput = new AuditInput();
         auditInput.setTransaction(input.getTransaction());
         if(auditLevel.equals(AuditLevel.RECORD) || (auditLevel.equals(AuditLevel.FIELD) && !dmlType.supportsFields))
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // make many simple audits (no details) for RECORD level                                                  //
            // or for FIELD level, but on a DML type that doesn't support field-level details (e.g., DELETE or OTHER) //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            for(QRecord record : recordList)
            {
               AuditAction.appendToInput(auditInput, table.getName(), record.getValue(table.getPrimaryKeyField()), getRecordSecurityKeyValues(table, record, Optional.empty()), "Record was " + dmlType.pastTenseVerb + contextSuffix);
            }
         }
         else if(auditLevel.equals(AuditLevel.FIELD))
         {
            Map<Serializable, QRecord> oldRecordMap = buildOldRecordMap(table, oldRecordList);

            ///////////////////////////////////////////////////////////////////
            // do many audits, all with field level details, for FIELD level //
            ///////////////////////////////////////////////////////////////////
            QPossibleValueTranslator qPossibleValueTranslator = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession());
            qPossibleValueTranslator.translatePossibleValuesInRecords(table, CollectionUtils.mergeLists(recordList, oldRecordList));

            //////////////////////////////////////////
            // sort the field names by their labels //
            //////////////////////////////////////////
            List<String> sortedFieldNames = table.getFields().keySet().stream()
               .sorted(Comparator.comparing(fieldName -> Objects.requireNonNullElse(table.getFields().get(fieldName).getLabel(), fieldName)))
               .toList();

            //////////////////////////////////////////////
            // build single audit input for each record //
            //////////////////////////////////////////////
            for(QRecord record : recordList)
            {
               QRecord oldRecord = oldRecordMap.get(ValueUtils.getValueAsFieldType(primaryKeyField.getType(), record.getValue(primaryKeyField.getName())));

               List<QRecord> details = new ArrayList<>();
               for(String fieldName : sortedFieldNames)
               {
                  makeAuditDetailRecordForField(fieldName, table, dmlType, record, oldRecord)
                     .ifPresent(details::add);
               }

               if(details.isEmpty() && DMLType.UPDATE.equals(dmlType))
               {
                  // no, let's just noop.
                  // details.add(new QRecord().withValue("message", "No fields values were changed."));
               }
               else
               {
                  AuditAction.appendToInput(auditInput, table.getName(), record.getValue(table.getPrimaryKeyField()), getRecordSecurityKeyValues(table, record, Optional.ofNullable(oldRecord)), "Record was " + dmlType.pastTenseVerb + contextSuffix, details);
               }
            }
         }

         // new AuditAction().executeAsync(auditInput); // todo async??? maybe get that from rules???
         new AuditAction().execute(auditInput);
         long end = System.currentTimeMillis();
         LOG.trace("Audit performance", logPair("auditLevel", String.valueOf(auditLevel)), logPair("recordCount", recordList.size()), logPair("millis", (end - start)));
      }
      catch(Exception e)
      {
         LOG.warn("Error performing DML audit", e, logPair("type", String.valueOf(dmlType)), logPair("table", table.getName()));
      }

      return (output);
   }



   /*******************************************************************************
    ** Build a context suffix string to append to audit messages.
    **
    ** This method collects contextual information from multiple sources to create
    ** a descriptive suffix that helps identify when/where the audit occurred:
    ** - Direct audit context from the DML input
    ** - Session-level audit context
    ** - Process name and label (if running within a process)
    ** - API version and label (if the operation came via an API call)
    **
    ** @param input the DML audit input that may contain audit context
    ** @return a suffix string to append to audit messages (may be empty)
    *******************************************************************************/
   static String getContentSuffix(DMLAuditInput input)
   {
      StringBuilder contextSuffix = new StringBuilder();

      /////////////////////////////////////////////////////////////////////////////
      // start with context from the input wrapper                               //
      // note, these contexts get propagated down from Input/Update/Delete Input //
      /////////////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(input.getAuditContext()))
      {
         contextSuffix.append(" ").append(input.getAuditContext());
      }

      //////////////////////////////////////////////////////////////
      // look for a context value place directly into the session //
      //////////////////////////////////////////////////////////////
      QSession qSession = QContext.getQSession();
      if(qSession != null)
      {
         String sessionContext = qSession.getValue(AUDIT_CONTEXT_FIELD_NAME);
         if(StringUtils.hasContent(sessionContext))
         {
            contextSuffix.append(" ").append(sessionContext);
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////
      // note process label (and a possible context from the process's state) if present //
      /////////////////////////////////////////////////////////////////////////////////////
      Optional<AbstractActionInput> actionInput = QContext.getFirstActionInStack();
      if(actionInput.isPresent() && actionInput.get() instanceof RunProcessInput runProcessInput)
      {
         String processAuditContext = ValueUtils.getValueAsString(runProcessInput.getValue(AUDIT_CONTEXT_FIELD_NAME));
         if(StringUtils.hasContent(processAuditContext))
         {
            contextSuffix.append(" ").append(processAuditContext);
         }

         String           processName = runProcessInput.getProcessName();
         QProcessMetaData process     = QContext.getQInstance().getProcess(processName);
         if(process != null)
         {
            contextSuffix.append(" during process: ").append(process.getLabel());
         }
      }

      ///////////////////////////////////////////////////
      // use api label & version if present in session //
      ///////////////////////////////////////////////////
      if(qSession != null)
      {
         String apiVersion = qSession.getValue("apiVersion");
         if(apiVersion != null)
         {
            String apiLabel = qSession.getValue("apiLabel");
            if(!StringUtils.hasContent(apiLabel))
            {
               apiLabel = "API";
            }
            contextSuffix.append(" via ").append(apiLabel).append(" Version: ").append(apiVersion);
         }
      }

      return (contextSuffix.toString());
   }



   /*******************************************************************************
    ** Create an audit detail record for a single field if its value has changed.
    **
    ** This method compares the old and new values for a field and, if different,
    ** creates a detail record describing the change. It handles special cases:
    ** - Skips createDate, modifyDate, and automationStatus fields
    ** - Handles BLOB and masked fields without exposing actual values
    ** - Uses formatted/display values when available (e.g., for possible values)
    **
    ** @param fieldName the name of the field to audit
    ** @param table the table metadata containing field definitions
    ** @param dmlType the type of DML operation (INSERT, UPDATE, DELETE, OTHER)
    ** @param record the new record with current values
    ** @param oldRecord the old record with previous values (null for inserts)
    ** @return Optional containing an audit detail record if change detected,
    **         empty Optional otherwise
    *******************************************************************************/
   static Optional<QRecord> makeAuditDetailRecordForField(String fieldName, QTableMetaData table, DMLType dmlType, QRecord record, QRecord oldRecord)
   {
      if(!record.getValues().containsKey(fieldName))
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////
         // if the stored record doesn't have this field name, then don't audit anything about it      //
         // this is to deal with our Patch style updates not looking like every field was cleared out. //
         ////////////////////////////////////////////////////////////////////////////////////////////////
         return (Optional.empty());
      }

      if(fieldName.equals("modifyDate") || fieldName.equals("createDate") || fieldName.equals("automationStatus"))
      {
         return (Optional.empty());
      }

      QFieldMetaData field        = table.getField(fieldName);
      Serializable   value        = ValueUtils.getValueAsFieldType(field.getType(), record.getValue(fieldName));
      Serializable   oldValue     = oldRecord == null ? null : ValueUtils.getValueAsFieldType(field.getType(), oldRecord.getValue(fieldName));
      QRecord        detailRecord = null;

      if(oldRecord == null)
      {
         if(DMLType.INSERT.equals(dmlType) && value == null)
         {
            return (Optional.empty());
         }

         if(field.getType().equals(QFieldType.BLOB) || field.getType().needsMasked())
         {
            detailRecord = new QRecord().withValue("message", "Set " + field.getLabel());
         }
         else
         {
            String formattedValue = getFormattedValueForAuditDetail(table, record, fieldName, field, value);
            detailRecord = new QRecord().withValue("message", "Set " + field.getLabel() + " to " + formattedValue);
            detailRecord.withValue("newValue", formattedValue);
         }
      }
      else
      {
         if(areValuesDifferentForAudit(field, value, oldValue))
         {
            if(field.getType().equals(QFieldType.BLOB) || field.getType().needsMasked())
            {
               if(oldValue == null)
               {
                  detailRecord = new QRecord().withValue("message", "Set " + field.getLabel());
               }
               else if(value == null)
               {
                  detailRecord = new QRecord().withValue("message", "Removed " + field.getLabel());
               }
               else
               {
                  detailRecord = new QRecord().withValue("message", "Changed " + field.getLabel());
               }
            }
            else
            {
               String formattedValue    = getFormattedValueForAuditDetail(table, record, fieldName, field, value);
               String formattedOldValue = getFormattedValueForAuditDetail(table, oldRecord, fieldName, field, oldValue);

               if(oldValue == null)
               {
                  detailRecord = new QRecord().withValue("message", "Set " + field.getLabel() + " to " + formatFormattedValueForDetailMessage(field, formattedValue));
                  detailRecord.withValue("newValue", formattedValue);
               }
               else if(value == null)
               {
                  detailRecord = new QRecord().withValue("message", "Removed " + formatFormattedValueForDetailMessage(field, formattedOldValue) + " from " + field.getLabel());
                  detailRecord.withValue("oldValue", formattedOldValue);
               }
               else
               {
                  detailRecord = new QRecord().withValue("message", "Changed " + field.getLabel() + " from " + formatFormattedValueForDetailMessage(field, formattedOldValue) + " to " + formatFormattedValueForDetailMessage(field, formattedValue));
                  detailRecord.withValue("oldValue", formattedOldValue);
                  detailRecord.withValue("newValue", formattedValue);
               }
            }
         }
      }

      if(detailRecord != null)
      {
         ////////////////////////////////////////////////////////////////////
         // useful if doing dev in here - but overkill for any other time. //
         ////////////////////////////////////////////////////////////////////
         // LOG.debug("Returning with message: " + detailRecord.getValueString("message"));
         detailRecord.withValue("fieldName", fieldName);
         return (Optional.of(detailRecord));
      }

      return (Optional.empty());
   }



   /*******************************************************************************
    ** Determine whether two field values are different for audit purposes.
    **
    ** This method applies type-specific comparison logic to handle edge cases:
    ** - Decimals: Uses compareTo() to ignore scale differences (10 vs 10.00)
    ** - DateTimes: Truncates to seconds to avoid millisecond precision issues
    ** - Strings: Treats null and empty string as equivalent
    ** - Other types: Uses Objects.equals() for comparison
    **
    ** @param field the field metadata describing the value type
    ** @param value the new value to compare
    ** @param oldValue the old value to compare against
    ** @return true if values are different for audit purposes, false otherwise
    *******************************************************************************/
   static boolean areValuesDifferentForAudit(QFieldMetaData field, Serializable value, Serializable oldValue)
   {
      try
      {
         ///////////////////
         // decimal rules //
         ///////////////////
         if(field.getType().equals(QFieldType.DECIMAL))
         {
            BigDecimal newBD = ValueUtils.getValueAsBigDecimal(value);
            BigDecimal oldBD = ValueUtils.getValueAsBigDecimal(oldValue);

            if(newBD == null && oldBD == null)
            {
               return (false);
            }

            if(newBD == null || oldBD == null)
            {
               return (true);
            }

            return (newBD.compareTo(oldBD) != 0);
         }

         ////////////////////
         // dateTime rules //
         ////////////////////
         if(field.getType().equals(QFieldType.DATE_TIME))
         {
            Instant newI = ValueUtils.getValueAsInstant(value);
            Instant oldI = ValueUtils.getValueAsInstant(oldValue);

            if(newI == null && oldI == null)
            {
               return (false);
            }

            if(newI == null || oldI == null)
            {
               return (true);
            }

            ////////////////////////////////
            // just compare to the second //
            ////////////////////////////////
            return (newI.truncatedTo(ChronoUnit.SECONDS).compareTo(oldI.truncatedTo(ChronoUnit.SECONDS)) != 0);
         }

         //////////////////
         // string rules //
         //////////////////
         if(field.getType().isStringLike())
         {
            String newString = ValueUtils.getValueAsString(value);
            String oldString = ValueUtils.getValueAsString(oldValue);

            boolean newIsNullOrEmpty = !StringUtils.hasContent(newString);
            boolean oldIsNullOrEmpty = !StringUtils.hasContent(oldString);

            if(newIsNullOrEmpty && oldIsNullOrEmpty)
            {
               return (false);
            }

            if(newIsNullOrEmpty || oldIsNullOrEmpty)
            {
               return (true);
            }

            return (newString.compareTo(oldString) != 0);
         }

         /////////////////////////////////////
         // default just use Objects.equals //
         /////////////////////////////////////
         return !Objects.equals(oldValue, value);
      }
      catch(Exception e)
      {
         LOG.debug("Error checking areValuesDifferentForAudit", e, logPair("fieldName", field.getName()), logPair("value", value), logPair("oldValue", oldValue));
      }

      ////////////////////////////////////
      // default to something simple... //
      ////////////////////////////////////
      return !Objects.equals(oldValue, value);
   }



   /*******************************************************************************
    ** Format a field value for display in an audit detail record.
    **
    ** This method converts raw field values to human-readable strings:
    ** - DateTimes are formatted with timezone information
    ** - Possible values use their display values (e.g., "Active" instead of 1)
    ** - Other values use standard QValueFormatter display formatting
    **
    ** @param table the table metadata
    ** @param record the record containing the value to format
    ** @param fieldName the name of the field
    ** @param field the field metadata
    ** @param value the raw value to format
    ** @return formatted string representation of the value, or null if value is null
    *******************************************************************************/
   private static String getFormattedValueForAuditDetail(QTableMetaData table, QRecord record, String fieldName, QFieldMetaData field, Serializable value)
   {
      String formattedValue = null;
      if(value != null)
      {
         if(field.getType().equals(QFieldType.DATE_TIME) && value instanceof Instant instant)
         {
            formattedValue = QValueFormatter.formatDateTimeWithZone(instant.atZone(ZoneId.of(Objects.requireNonNullElse(QContext.getQInstance().getDefaultTimeZoneId(), "UTC"))));
         }
         else if(record.getDisplayValue(fieldName) != null)
         {
            formattedValue = record.getDisplayValue(fieldName);
         }
         else
         {
            QValueFormatter.setDisplayValuesInRecord(table, table.getFields(), record);
            formattedValue = record.getDisplayValue(fieldName);
         }
      }

      return formattedValue;
   }



   /*******************************************************************************
    ** Prepare a formatted value for inclusion in an audit detail message.
    **
    ** This method wraps values in quotes where appropriate:
    ** - String values and possible value display values get quotes
    ** - Null or "null" values are replaced with "--"
    ** - Numeric values are left unquoted
    **
    ** @param field the field metadata
    ** @param formattedValue the formatted value to prepare for the message
    ** @return the value formatted for inclusion in the audit message
    *******************************************************************************/
   private static String formatFormattedValueForDetailMessage(QFieldMetaData field, String formattedValue)
   {
      if(formattedValue == null || "null".equals(formattedValue))
      {
         formattedValue = "--";
      }
      else
      {
         if(QFieldType.STRING.equals(field.getType()) || field.getPossibleValueSourceName() != null)
         {
            formattedValue = '"' + formattedValue + '"';
         }
      }

      return (formattedValue);
   }



   /*******************************************************************************
    ** Build a map of old records keyed by their primary key values.
    **
    ** This map is used during FIELD-level auditing to quickly look up the old
    ** version of a record when comparing field values.
    **
    ** @param table the table metadata containing the primary key field definition
    ** @param oldRecordList the list of old records to index
    ** @return a map from primary key value to the corresponding old record
    *******************************************************************************/
   private Map<Serializable, QRecord> buildOldRecordMap(QTableMetaData table, List<QRecord> oldRecordList)
   {
      Map<Serializable, QRecord> rs = new HashMap<>();
      for(QRecord record : CollectionUtils.nonNullList(oldRecordList))
      {
         rs.put(record.getValue(table.getPrimaryKeyField()), record);
      }
      return (rs);
   }



   /*******************************************************************************
    ** Determine the DML operation type from a table action input.
    **
    ** Maps the input class type to the corresponding DML type for audit messages.
    **
    ** @param tableActionInput the input from a table action (insert/update/delete)
    ** @return the corresponding DMLType enum value
    *******************************************************************************/
   private DMLType getDMLType(AbstractTableActionInput tableActionInput)
   {
      if(tableActionInput instanceof InsertInput)
      {
         return DMLType.INSERT;
      }
      else if(tableActionInput instanceof UpdateInput)
      {
         return DMLType.UPDATE;
      }
      else if(tableActionInput instanceof DeleteInput)
      {
         return DMLType.DELETE;
      }
      else
      {
         return DMLType.OTHER;
      }
   }



   /*******************************************************************************
    ** Get the audit level for the table being operated on.
    *******************************************************************************/
   public static AuditLevel getAuditLevel(AbstractTableActionInput tableActionInput)
   {
      QTableMetaData table = tableActionInput.getTable();
      if(table.getAuditRules() == null)
      {
         return (AuditLevel.NONE);
      }

      return (table.getAuditRules().getAuditLevel());
   }



   /*******************************************************************************
    ** Get the field type configured for the audit table's recordId field.
    ** Returns INTEGER by default if the audit table is not configured.
    *******************************************************************************/
   private static QFieldType getAuditRecordIdFieldType()
   {
      QTableMetaData auditTable = QContext.getQInstance().getTable("audit");
      if(auditTable != null)
      {
         QFieldMetaData recordIdField = auditTable.getField("recordId");
         if(recordIdField != null)
         {
            return (recordIdField.getType());
         }
      }
      return (QFieldType.INTEGER);
   }



   /*******************************************************************************
    ** Enumeration of DML operation types that can be audited.
    *******************************************************************************/
   enum DMLType
   {
      INSERT("Inserted", true),
      UPDATE("Edited", true),
      DELETE("Deleted", false),
      OTHER("Processed", false);

      private final String  pastTenseVerb;
      private final boolean supportsFields;



      /*******************************************************************************
       ** Constructor for DMLType enum values.
       **
       ** @param pastTenseVerb the past-tense verb to use in audit messages
       **                      (e.g., "Inserted", "Edited", "Deleted")
       ** @param supportsFields whether this DML type supports field-level auditing
       *******************************************************************************/
      DMLType(String pastTenseVerb, boolean supportsFields)
      {
         this.pastTenseVerb = pastTenseVerb;
         this.supportsFields = supportsFields;
      }
   }
}
