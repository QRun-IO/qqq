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

package com.kingsrook.qqq.backend.core.actions.ai.tools;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.kingsrook.qqq.backend.core.actions.ai.AiDataGateway;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiRecordRef;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKernelSettings;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolResult;
import com.kingsrook.qqq.backend.core.actions.ai.CoreAiDataGateway;
import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiExposurePolicy;
import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiValueMasker;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiRedactor;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.metadata.DefaultNoopMetaDataActionCustomizer;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataActionCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionCheckResult;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Per-call context for an AI tool: the kernel's instance, settings, exposure
 ** policy and redactor, the result being built - and the shared checks and
 ** output shaping every tool uses (so no tool can forget one):
 **
 ** - visibility (the meta-data customizer, and DENY_HIDE) and exposure checks,
 ** - the table READ permission check (recorded in the result's permissionCheck),
 ** - action-input preparation (input source USER, omit hidden fields, mask
 **   passwords, and the core-enforcement hook),
 ** - value shaping: exposure post-filter, masking, redaction and truncation.
 **
 ** Created by the kernel, inside the requesting user's QContext.
 *******************************************************************************/
public class AiToolContext
{
   public static final String TRUNCATION_MARKER = "...[truncated]";

   private final QInstance            qInstance;
   private final AiToolKernelSettings settings;
   private final AiExposurePolicy     exposurePolicy;
   private final AiRedactor           redactor;
   private final AiToolResult         result;

   private final Set<String> redactions = new TreeSet<>();

   private static final AiDataGateway CORE_DATA_GATEWAY = new CoreAiDataGateway();

   private MetaDataActionCustomizerInterface metaDataActionCustomizer;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AiToolContext(QInstance qInstance, AiToolKernelSettings settings, AiExposurePolicy exposurePolicy, AiRedactor redactor, AiToolResult result)
   {
      this.qInstance = qInstance;
      this.settings = settings;
      this.exposurePolicy = exposurePolicy;
      this.redactor = redactor;
      this.result = result;
   }



   /*******************************************************************************
    ** How the data tools read data: the settings' gateway (the assistant's UI API
    ** gateway), else the core actions.
    *******************************************************************************/
   public AiDataGateway getDataGateway()
   {
      AiDataGateway gateway = settings == null ? null : settings.getDataGateway();
      return (gateway == null ? CORE_DATA_GATEWAY : gateway);
   }



   /*******************************************************************************
    ** The table, if this session may read it through the assistant: it exists,
    ** is visible to the session (not DENY_HIDE, allowed by any meta-data action
    ** customizer), is exposed (not NONE), and the session has READ on it.
    ** Otherwise a DENIED exception - with the same message in every case, so a
    ** caller can't tell a missing table from a forbidden one.
    *******************************************************************************/
   public QTableMetaData requireReadableTable(String tableName) throws AiToolException
   {
      result.setTableName(tableName);
      QTableMetaData table = tableName == null ? null : qInstance.getTable(tableName);
      if(table == null || !isVisible(table) || exposurePolicy.getTableExposure(table) == AiExposure.NONE)
      {
         result.setPermissionCheck("table:" + tableName + ":READ:DENIED");
         throw (tableNotAvailable(tableName));
      }

      try
      {
         QueryInput permissionInput = new QueryInput(tableName);
         prepareInput(permissionInput);
         PermissionsHelper.checkTablePermissionThrowing(permissionInput, TablePermissionSubType.READ);
      }
      catch(QPermissionDeniedException e)
      {
         result.setPermissionCheck("table:" + tableName + ":READ:DENIED");
         throw (tableNotAvailable(tableName));
      }

      result.setPermissionCheck("table:" + tableName + ":READ:OK");
      return (table);
   }



   /*******************************************************************************
    ** The DENIED exception for a table that is missing, invisible, unexposed or
    ** not readable.
    *******************************************************************************/
   public static AiToolException tableNotAvailable(String tableName)
   {
      return (new AiToolException(AiToolOutcome.DENIED, "The table '" + tableName + "' does not exist, or you do not have permission to read it.  Use list_tables to see the tables you can read."));
   }



   /*******************************************************************************
    ** Whether the session may read a table through the assistant (same checks
    ** as requireReadableTable, without recording or throwing).
    *******************************************************************************/
   public boolean canReadTable(QTableMetaData table)
   {
      return (table != null
         && isVisible(table)
         && exposurePolicy.getTableExposure(table) != AiExposure.NONE
         && PermissionsHelper.hasTablePermission(new MetaDataInput(), table.getName(), TablePermissionSubType.READ));
   }



   /*******************************************************************************
    ** Whether the session has a (non-READ) permission on a table.
    *******************************************************************************/
   public boolean hasTablePermission(QTableMetaData table, TablePermissionSubType permissionSubType)
   {
      return (PermissionsHelper.hasTablePermission(new MetaDataInput(), table.getName(), permissionSubType));
   }



   /*******************************************************************************
    ** Whether a meta-data object (table, process, report, widget, app) is
    ** visible to the session - the same rule as MetaDataAction: allowed by any
    ** meta-data action customizer, and its permission check is not DENY_HIDE.
    *******************************************************************************/
   public boolean isVisible(MetaDataWithPermissionRules object)
   {
      if(object == null)
      {
         return (false);
      }

      MetaDataInput                     metaDataInput = new MetaDataInput();
      MetaDataActionCustomizerInterface customizer    = getMetaDataActionCustomizer();
      boolean                           allowed       = switch(object)
      {
         case QTableMetaData table -> customizer.allowTable(metaDataInput, table);
         case QProcessMetaData process -> customizer.allowProcess(metaDataInput, process);
         case QReportMetaData report -> customizer.allowReport(metaDataInput, report);
         case QWidgetMetaDataInterface widget -> customizer.allowWidget(metaDataInput, widget);
         case QAppMetaData app -> customizer.allowApp(metaDataInput, app);
         default -> true;
      };

      return (allowed && !PermissionCheckResult.DENY_HIDE.equals(PermissionsHelper.getPermissionCheckResult(metaDataInput, object)));
   }



   /*******************************************************************************
    ** Whether a meta-data object is visible to the session AND its permission
    ** check fully allows it (not DENY_HIDE, not DENY_DISABLE).
    *******************************************************************************/
   public boolean isAllowed(MetaDataWithPermissionRules object)
   {
      return (isVisible(object) && PermissionCheckResult.ALLOW.equals(PermissionsHelper.getPermissionCheckResult(new MetaDataInput(), object)));
   }



   /*******************************************************************************
    ** The table as personalized for this session (fields the session may not see
    ** removed by any table personalizer).
    *******************************************************************************/
   public QTableMetaData personalize(QTableMetaData table) throws QException
   {
      QTableMetaData personalized = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput()
         .withTableName(table.getName())
         .withTableMetaData(table)
         .withInputSource(QInputSource.USER));
      return (personalized == null ? table : personalized);
   }



   /*******************************************************************************
    ** The fields of a (personalized) table the assistant may name - in filters,
    ** sorts and field lists - that is, those whose exposure is not NONE.
    *******************************************************************************/
   public Map<String, QFieldMetaData> getNameableFields(QTableMetaData personalizedTable)
   {
      Map<String, QFieldMetaData> fields = new LinkedHashMap<>();
      for(QFieldMetaData field : CollectionUtils.nonNullMap(personalizedTable.getFields()).values())
      {
         if(getFieldExposure(personalizedTable, field) != AiExposure.NONE)
         {
            fields.put(field.getName(), field);
         }
      }
      return (fields);
   }



   /*******************************************************************************
    ** The fields whose values may be sent to the model by default: exposure
    ** MASKED or VALUES, and not heavy or binary.
    *******************************************************************************/
   public List<QFieldMetaData> getDefaultValueFields(QTableMetaData personalizedTable)
   {
      List<QFieldMetaData> fields = new ArrayList<>();
      for(QFieldMetaData field : getNameableFields(personalizedTable).values())
      {
         if(getFieldExposure(personalizedTable, field).allowsValues() && !field.getIsHeavy() && field.getType() != QFieldType.BLOB)
         {
            fields.add(field);
         }
      }
      return (fields);
   }



   /*******************************************************************************
    ** The effective exposure of a field.
    *******************************************************************************/
   public AiExposure getFieldExposure(QTableMetaData table, QFieldMetaData field)
   {
      return (exposurePolicy.getFieldExposure(table, field));
   }



   /*******************************************************************************
    ** The effective exposure of a table.
    *******************************************************************************/
   public AiExposure getTableExposure(QTableMetaData table)
   {
      return (exposurePolicy.getTableExposure(table));
   }



   /*******************************************************************************
    ** Prepare an action input the kernel will run: input source USER (for
    ** table, search and widget inputs) and, for
    ** query and get, omit hidden fields and mask passwords - then the core
    ** permission-enforcement hook.
    *******************************************************************************/
   public void prepareInput(AbstractActionInput input)
   {
      if(input instanceof AbstractTableActionInput tableActionInput)
      {
         tableActionInput.setInputSource(QInputSource.USER);
      }

      if(input instanceof RecordSearchInput recordSearchInput)
      {
         recordSearchInput.setInputSource(QInputSource.USER);
      }

      if(input instanceof RenderWidgetInput renderWidgetInput)
      {
         renderWidgetInput.setInputSource(QInputSource.USER);
      }

      if(input instanceof QueryInput queryInput)
      {
         queryInput.setShouldOmitHiddenFields(true);
         queryInput.setShouldMaskPasswords(true);
      }

      if(input instanceof GetInput getInput)
      {
         getInput.setShouldOmitHiddenFields(true);
         getInput.setShouldMaskPasswords(true);
      }

      applyCoreEnforcement(input);
   }



   /*******************************************************************************
    ** Hook for core permission enforcement on action inputs.
    **
    ** QQQ issue #1061 adds an opt-in PermissionEnforcement.ENFORCE flag on action
    ** inputs (planned for 4.1.0-RC.4), with which the actions themselves run the
    ** same PermissionsHelper checks.  Once it is in core, this method sets it on
    ** every input the kernel runs - in addition to (never instead of) the
    ** kernel's own checks.  Until then, there is nothing to set, so this is a
    ** no-op.
    *******************************************************************************/
   static void applyCoreEnforcement(AbstractActionInput input)
   {
      //////////////////////////////////////////////////////////////////
      // no core flag exists yet - see javadoc (QQQ #1061); the       //
      // kernel's own PermissionsHelper checks are what protect today //
      //////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    ** Redact, then truncate, a string for the model (or the UI).
    *******************************************************************************/
   public String sanitize(String value)
   {
      if(value == null)
      {
         return (null);
      }

      String redacted = redactor.redact(value, redactions);
      int    max      = settings.getMaxCellChars() == null ? AiToolKernelSettings.DEFAULT_MAX_CELL_CHARS : settings.getMaxCellChars();
      if(redacted.length() > max)
      {
         return (redacted.substring(0, max) + TRUNCATION_MARKER);
      }
      return (redacted);
   }



   /*******************************************************************************
    ** Whether sanitize would truncate a value.
    *******************************************************************************/
   public boolean wouldTruncate(String value)
   {
      int max = settings.getMaxCellChars() == null ? AiToolKernelSettings.DEFAULT_MAX_CELL_CHARS : settings.getMaxCellChars();
      return (value != null && redactor.redact(value, null).length() > max);
   }



   /*******************************************************************************
    ** Mask a value of a MASKED field (noting the masking in the redactions).
    *******************************************************************************/
   public String mask(QTableMetaData table, QFieldMetaData field, String value)
   {
      redactions.add("field:" + table.getName() + "." + field.getName() + ":MASKED");
      return (AiValueMasker.mask(value, field.getType()));
   }



   /*******************************************************************************
    ** The value of one field of a record, shaped for its exposure:
    ** - NONE / METADATA_ONLY: null (and nothing should be output),
    ** - MASKED: the masked display (or raw) value,
    ** - VALUES: the display value (or raw value), redacted and truncated.
    *******************************************************************************/
   public String displayValue(QTableMetaData table, QFieldMetaData field, QRecord record)
   {
      AiExposure exposure = getFieldExposure(table, field);
      if(!exposure.allowsValues())
      {
         return (null);
      }

      String display = record.getDisplayValue(field.getName());
      if(display == null)
      {
         display = stringify(record.getValue(field.getName()));
      }

      if(display == null)
      {
         return (null);
      }

      return (exposure == AiExposure.MASKED ? mask(table, field, display) : sanitize(display));
   }



   /*******************************************************************************
    ** Add the value of one field of a record to a map for the model: for VALUES
    ** fields, the raw value (numbers and booleans as-is, others redacted and
    ** truncated) - plus, when it differs, the display value as
    ** {field}__display; for MASKED fields, only the masked value; for others,
    ** nothing.
    *******************************************************************************/
   public void putModelValue(Map<String, Object> values, QTableMetaData table, QFieldMetaData field, QRecord record)
   {
      AiExposure exposure = getFieldExposure(table, field);
      if(!exposure.allowsValues())
      {
         return;
      }

      if(exposure == AiExposure.MASKED)
      {
         values.put(field.getName(), displayValue(table, field, record));
         return;
      }

      Serializable raw = record.getValue(field.getName());
      Object       modelRaw;
      if(raw == null || raw instanceof Boolean || (raw instanceof Number && !(raw instanceof BigDecimal)))
      {
         modelRaw = raw;
      }
      else
      {
         modelRaw = sanitize(stringify(raw));
      }
      values.put(field.getName(), modelRaw);

      String display = record.getDisplayValue(field.getName());
      if(display != null && !display.equals(stringify(raw)))
      {
         values.put(field.getName() + "__display", sanitize(display));
      }
   }



   /*******************************************************************************
    ** A label for a record that is safe to send: the record's label if every
    ** field in the table's record label is exposed with VALUES - else the table
    ** label and the record's id.
    *******************************************************************************/
   public String recordLabel(QTableMetaData personalizedTable, QRecord record)
   {
      Serializable id       = record.getValue(personalizedTable.getPrimaryKeyField());
      String       fallback = personalizedTable.getLabel() + " " + id;

      List<String> labelFields = personalizedTable.getRecordLabelFields();
      if(CollectionUtils.nullSafeIsEmpty(labelFields) || record.getRecordLabel() == null)
      {
         return (fallback);
      }

      for(String labelFieldName : labelFields)
      {
         QFieldMetaData labelField = personalizedTable.getFields().get(labelFieldName);
         if(labelField == null || getFieldExposure(personalizedTable, labelField) != AiExposure.VALUES)
         {
            return (fallback);
         }
      }

      return (sanitize(record.getRecordLabel()));
   }



   /*******************************************************************************
    ** A record reference (for a record chip) - with a safe label.
    *******************************************************************************/
   public AiRecordRef recordRef(QTableMetaData personalizedTable, QRecord record)
   {
      Serializable id = record.getValue(personalizedTable.getPrimaryKeyField());
      return (new AiRecordRef()
         .withTableName(personalizedTable.getName())
         .withTableLabel(personalizedTable.getLabel())
         .withId(id)
         .withLabel(recordLabel(personalizedTable, record))
         .withHref(AiLinkBuilder.recordHref(personalizedTable.getName(), id)));
   }



   /*******************************************************************************
    ** A value as a string (temporal values in ISO form, decimals plain).
    *******************************************************************************/
   public static String stringify(Serializable value)
   {
      if(value == null)
      {
         return (null);
      }

      return switch(value)
      {
         case BigDecimal bigDecimal -> bigDecimal.toPlainString();
         case Temporal temporal -> temporal.toString();
         case byte[] bytes -> "[binary data]";
         default -> String.valueOf(value);
      };
   }



   /*******************************************************************************
    ** The meta-data action customizer of the instance (or a no-op one).
    *******************************************************************************/
   private MetaDataActionCustomizerInterface getMetaDataActionCustomizer()
   {
      if(metaDataActionCustomizer == null)
      {
         QCodeReference reference = qInstance.getMetaDataActionCustomizer();
         if(reference != null)
         {
            metaDataActionCustomizer = QCodeLoader.getAdHoc(MetaDataActionCustomizerInterface.class, reference);
         }

         if(metaDataActionCustomizer == null)
         {
            metaDataActionCustomizer = new DefaultNoopMetaDataActionCustomizer();
         }
      }
      return (metaDataActionCustomizer);
   }



   /*******************************************************************************
    ** Getter for qInstance
    *******************************************************************************/
   public QInstance getQInstance()
   {
      return (this.qInstance);
   }



   /*******************************************************************************
    ** Getter for settings
    *******************************************************************************/
   public AiToolKernelSettings getSettings()
   {
      return (this.settings);
   }



   /*******************************************************************************
    ** The maximum rows to send to the model.
    *******************************************************************************/
   public int getMaxRowsToModel()
   {
      Integer max = settings.getMaxRowsToModel();
      return (max == null || max < 1 ? AiToolKernelSettings.DEFAULT_MAX_ROWS_TO_MODEL : max);
   }



   /*******************************************************************************
    ** Getter for result
    *******************************************************************************/
   public AiToolResult getResult()
   {
      return (this.result);
   }



   /*******************************************************************************
    ** The redactions made so far (sorted, distinct).
    *******************************************************************************/
   public List<String> getRedactions()
   {
      return (new ArrayList<>(redactions));
   }
}
