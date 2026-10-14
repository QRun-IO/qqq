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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiExposurePolicy;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiDataEnvelope;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiRedactor;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AbstractAiTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiHelpText;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolContext;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolException;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolInput;
import com.kingsrook.qqq.backend.core.actions.ai.tools.CountRecordsTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.DescribeTableTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.GetRecordHistoryTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.GetRecordTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.ListTablesTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.NavigateTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.QueryRecordsTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.RenderWidgetTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.SearchHelpTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.SearchRecordsTool;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The AI tool kernel: the one permission-checked path through which an AI
 ** model (the in-app assistant, an MCP server, ...) reads QQQ data.
 **
 ** The tool set is generic and stable (list_tables, describe_table,
 ** query_records, count_records, get_record, search_records,
 ** get_record_history, render_widget, search_help, navigate), plus any READ
 ** or NAVIGATION tools that qbits contribute through {@link AiToolRegistry}:
 ** the same definitions for every user, so they cache well.  Permissions are applied
 ** when a tool runs, as the requesting user - never a system session:
 **
 ** 1. QContext is set up for the user's session (and the caller's context is
 **    restored afterwards - so execute is re-entrant).
 ** 2. Metadata visibility (DENY_HIDE objects are invisible) and the exposure
 **    policy ({@link AiExposure}, {@link AiTableMetaData}, {@link AiFieldMetaData}).
 ** 3. PermissionsHelper checks for the specific operation (READ on the table,
 **    joined tables, the widget, READ on the audit table for history) - always
 **    made by the kernel itself, as defense in depth, even when core actions
 **    enforce permissions too.
 ** 4. The QQQ action, with input source USER, hidden fields omitted and
 **    passwords masked - so record security locks apply.
 ** 5. A post-filter (only exposed field values; MASKED values masked;
 **    METADATA_ONLY never), truncation (maxRowsToModel, maxCellChars) and
 **    redaction of secrets and PII.
 ** 6. The data envelope around everything sent back to the model.
 **
 ** execute never throws: permission and validation problems are outcomes
 ** (DENIED, NOT_FOUND, INVALID_INPUT) with a polite message, and unexpected
 ** errors are an ERROR outcome with a generic message (details are logged).
 *******************************************************************************/
public class AiToolKernel
{
   private static final QLogger LOG = QLogger.getLogger(AiToolKernel.class);

   public static final String NOT_AVAILABLE = "not available";

   private final QInstance            qInstance;
   private final AiToolKernelSettings settings;
   private final AiExposurePolicy     exposurePolicy;
   private final AiRedactor           redactor;

   private final Map<String, AbstractAiTool> toolsByName;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AiToolKernel(QInstance qInstance, AiToolKernelSettings settings)
   {
      this.qInstance = qInstance;
      this.settings = settings == null ? AiToolKernelSettings.of(qInstance) : settings;
      this.exposurePolicy = new AiExposurePolicy(this.settings);
      this.redactor = new AiRedactor(this.settings.getCustomRedactionPatterns());

      Map<String, AbstractAiTool> tools = new TreeMap<>();
      for(AbstractAiTool tool : builtInTools())
      {
         tools.put(tool.getName(), tool);
      }

      //////////////////////////////////////////////////////////////////////
      // tools contributed by qbits / the application (AiToolRegistry).   //
      // registration already restricts them to READ and NAVIGATION; a    //
      // contributed tool never replaces a built-in one (the validator    //
      // reports such a collision as an instance error).                  //
      //////////////////////////////////////////////////////////////////////
      AiToolRegistry registry = qInstance == null ? null : AiToolRegistry.of(qInstance);
      if(registry != null)
      {
         for(Map.Entry<String, AbstractAiTool> entry : registry.getTools().entrySet())
         {
            if(tools.containsKey(entry.getKey()))
            {
               LOG.warn("Ignoring contributed AI tool with a built-in tool's name", logPair("toolName", entry.getKey()));
               continue;
            }
            tools.put(entry.getKey(), entry.getValue());
         }
      }
      this.toolsByName = Collections.unmodifiableMap(tools);
   }



   /*******************************************************************************
    ** New instances of the built-in tools.
    *******************************************************************************/
   private static List<AbstractAiTool> builtInTools()
   {
      return (List.of(new ListTablesTool(), new DescribeTableTool(), new QueryRecordsTool(), new CountRecordsTool(), new GetRecordTool(),
         new SearchRecordsTool(), new GetRecordHistoryTool(), new RenderWidgetTool(), new SearchHelpTool(), new NavigateTool()));
   }



   /*******************************************************************************
    ** The names of the built-in tools (sorted).  Contributed tools
    ** (AiToolRegistry) may not use them.
    *******************************************************************************/
   public static Set<String> getBuiltInToolNames()
   {
      Set<String> names = new TreeSet<>();
      for(AbstractAiTool tool : builtInTools())
      {
         names.add(tool.getName());
      }
      return (Collections.unmodifiableSet(names));
   }



   /*******************************************************************************
    ** The tool definitions - sorted by name, identical for every user and every
    ** call (no timestamps or per-user content), so they are cache-friendly.
    ** Each call returns new objects, so a caller can't change another's.
    *******************************************************************************/
   public List<AiToolDefinition> getToolDefinitions()
   {
      List<AiToolDefinition> definitions = new ArrayList<>();
      for(AbstractAiTool tool : toolsByName.values())
      {
         definitions.add(tool.getDefinition());
      }
      return (definitions);
   }



   /*******************************************************************************
    ** Run one tool call as the given (user's) session.  Never throws.
    *******************************************************************************/
   public AiToolResult execute(QSession session, AiToolCall call)
   {
      long         start  = System.currentTimeMillis();
      AiToolResult result = new AiToolResult()
         .withToolCallId(call == null ? null : call.getId())
         .withToolName(call == null ? null : call.getName())
         .withOutcome(AiToolOutcome.OK);

      AiToolContext context = new AiToolContext(qInstance, settings, exposurePolicy, redactor, result);
      String        source  = call == null ? "tool" : call.getName();
      try
      {
         Map<String, Object> data = withUserContext(session, () -> runTool(context, call));
         result.setModelContent(AiDataEnvelope.wrap(source, result.getTableName(), JsonUtils.toJson(data)));
      }
      catch(AiToolException e)
      {
         fail(result, e.getOutcome(), e.getMessage(), source);
      }
      catch(QPermissionDeniedException e)
      {
         fail(result, AiToolOutcome.DENIED, "You do not have permission to do that.", source);
      }
      catch(Exception e)
      {
         LOG.warn("Error running AI tool", e, logPair("toolName", source), logPair("toolCallId", result.getToolCallId()));
         fail(result, AiToolOutcome.ERROR, "An unexpected error occurred while running this tool.", source);
      }

      result.setRedactions(context.getRedactions());
      result.setDurationMs(System.currentTimeMillis() - start);
      return (result);
   }



   /*******************************************************************************
    ** Validate a call and run its tool (inside the user's context).
    *******************************************************************************/
   private Map<String, Object> runTool(AiToolContext context, AiToolCall call) throws Exception
   {
      AbstractAiTool tool = call == null || call.getName() == null ? null : toolsByName.get(call.getName());
      if(tool == null)
      {
         throw (AiToolException.invalidInput("Unknown tool '" + (call == null ? null : call.getName()) + "'.  Available tools are: " + String.join(", ", toolsByName.keySet()) + "."));
      }

      AiToolInput input = new AiToolInput(call.getInput());
      input.requireOnlyKeys(tool.getInputPropertyNames());
      return (tool.execute(context, input));
   }



   /*******************************************************************************
    ** Set a failed outcome, with an enveloped message for the model.
    *******************************************************************************/
   private static void fail(AiToolResult result, AiToolOutcome outcome, String message, String source)
   {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("outcome", outcome.name());
      data.put("message", message);

      result.setOutcome(outcome);
      result.setSummary(message);
      result.setRender(null);
      result.setNavigateHref(null);
      result.setRecordRefs(new ArrayList<>());
      result.setRowCount(null);
      result.setModelContent(AiDataEnvelope.wrap(source, result.getTableName(), JsonUtils.toJson(data)));
   }



   /*******************************************************************************
    ** A digest of the application for a (cached, shared) system prompt: the
    ** apps, tables, processes and widgets - names, labels, aiDescriptions and
    ** help summaries - that EVERY user may see.  So it only includes objects
    ** whose permission rules can't deny anyone (level NOT_PROTECTED, no custom
    ** permission checker - and nothing at all if the instance has a meta-data
    ** action customizer, which may hide objects per user); permission-restricted
    ** objects are described per user, in buildUserDigest.  Hidden objects,
    ** tables with exposure NONE and widgets not allowed for the assistant are
    ** excluded.  The same for every user and deterministic (sorted by name), so
    ** it can sit in a cached prompt prefix.  Contains no data.
    *******************************************************************************/
   public String buildAppDigest()
   {
      StringBuilder digest = new StringBuilder();
      String        name   = qInstance.getBranding() != null && StringUtils.hasContent(qInstance.getBranding().getAppName()) ? qInstance.getBranding().getAppName() : null;
      digest.append("Application").append(name == null ? "" : ": " + name).append('\n');

      appendDigestSection(digest, "Apps", CollectionUtils.nonNullMap(qInstance.getApps()), QAppMetaData::getLabel, app -> AiHelpText.plain(app.getHelpContent()), this::isUnrestricted);
      appendDigestSection(digest, "Tables", CollectionUtils.nonNullMap(qInstance.getTables()), QTableMetaData::getLabel, AiToolKernel::tableDigestText, table -> isDigestTable(table) && isUnrestricted(table));
      appendDigestSection(digest, "Processes", CollectionUtils.nonNullMap(qInstance.getProcesses()), QProcessMetaData::getLabel, process -> null, process -> !process.getIsHidden() && isUnrestricted(process));
      appendDigestSection(digest, "Widgets", CollectionUtils.nonNullMap(qInstance.getWidgets()), QWidgetMetaDataInterface::getLabel, widget -> AiHelpText.clean(widget.getTooltip()), widget -> settings.isWidgetAllowed(widget.getName()) && isUnrestricted(widget));

      return (redactor.redact(digest.toString(), null));
   }



   /*******************************************************************************
    ** Whether an object's permission rules can't deny anyone - so it may be named
    ** in the shared app digest.
    *******************************************************************************/
   private boolean isUnrestricted(MetaDataWithPermissionRules object)
   {
      if(qInstance.getMetaDataActionCustomizer() != null)
      {
         return (false);
      }

      QPermissionRules rules = object.getPermissionRules() != null ? object.getPermissionRules() : qInstance.getDefaultPermissionRules();
      return (rules != null && PermissionLevel.NOT_PROTECTED.equals(rules.getLevel()) && rules.getCustomPermissionChecker() == null);
   }



   /*******************************************************************************
    ** Whether a table may be described in a digest at all (not hidden, exposed).
    *******************************************************************************/
   private boolean isDigestTable(QTableMetaData table)
   {
      return (!table.getIsHidden() && exposurePolicy.getTableExposure(table) != AiExposure.NONE);
   }



   /*******************************************************************************
    ** The digest text of a table: its aiDescription and help.
    *******************************************************************************/
   private static String tableDigestText(QTableMetaData table)
   {
      AiTableMetaData aiTableMetaData = AiTableMetaData.of(table);
      String          description     = aiTableMetaData == null ? null : aiTableMetaData.getAiDescription();
      String          help            = AiHelpText.plain(table.getHelpContent());
      return (description == null ? help : (help == null ? description : description + " " + help));
   }



   /*******************************************************************************
    ** Append one section of the app digest (objects sorted by name).
    *******************************************************************************/
   private static <T> void appendDigestSection(StringBuilder digest, String title, Map<String, T> objects, Function<T, String> labelFunction, Function<T, String> textFunction, Function<T, Boolean> includeFunction)
   {
      List<String> lines = new ArrayList<>();
      for(Map.Entry<String, T> entry : new TreeMap<>(objects).entrySet())
      {
         T object = entry.getValue();
         if(object == null || !Boolean.TRUE.equals(includeFunction.apply(object)))
         {
            continue;
         }

         String label = labelFunction.apply(object);
         String text  = textFunction.apply(object);
         lines.add("- " + entry.getKey() + (label == null ? "" : ": " + label) + (text == null ? "" : " - " + AiHelpText.clean(text)));
      }

      if(!lines.isEmpty())
      {
         digest.append(title).append(":\n");
         for(String line : lines)
         {
            digest.append(line).append('\n');
         }
      }
   }



   /*******************************************************************************
    ** A digest of the user for a (per-user) prompt section: name, timezone, the
    ** tables the user can read through the assistant (with their insert / edit
    ** / delete permissions) - and descriptions of the permission-restricted
    ** tables, apps, processes and widgets this user may use (which the shared
    ** app digest leaves out).  Objects the user can't see (DENY_HIDE) or use are
    ** never named.  Sorted by name.
    *******************************************************************************/
   public String buildUserDigest(QSession session)
   {
      try
      {
         return (withUserContext(session, () ->
         {
            AiToolContext context = new AiToolContext(qInstance, settings, exposurePolicy, redactor, new AiToolResult());
            StringBuilder digest  = new StringBuilder();

            String fullName = session.getUser() == null ? null : session.getUser().getFullName();
            digest.append("User: ").append(fullName == null ? "(unknown)" : context.sanitize(fullName)).append('\n');
            digest.append("Timezone: ").append(ValueUtils.getSessionOrInstanceZoneId()).append('\n');
            digest.append("Readable tables:\n");

            for(QTableMetaData table : new TreeMap<>(CollectionUtils.nonNullMap(qInstance.getTables())).values())
            {
               if(!context.canReadTable(table))
               {
                  continue;
               }

               List<String> also = new ArrayList<>();
               if(context.hasTablePermission(table, TablePermissionSubType.INSERT))
               {
                  also.add("insert");
               }
               if(context.hasTablePermission(table, TablePermissionSubType.EDIT))
               {
                  also.add("edit");
               }
               if(context.hasTablePermission(table, TablePermissionSubType.DELETE))
               {
                  also.add("delete");
               }

               String text = isDigestTable(table) && !isUnrestricted(table) ? tableDigestText(table) : null;
               digest.append("- ").append(table.getName()).append(" (").append(table.getLabel()).append(")").append(also.isEmpty() ? "" : " [" + String.join(", ", also) + "]")
                  .append(text == null ? "" : " - " + AiHelpText.clean(text)).append('\n');
            }

            appendDigestSection(digest, "Your apps", CollectionUtils.nonNullMap(qInstance.getApps()), QAppMetaData::getLabel, app -> AiHelpText.plain(app.getHelpContent()), app -> !isUnrestricted(app) && context.isAllowed(app));
            appendDigestSection(digest, "Your processes", CollectionUtils.nonNullMap(qInstance.getProcesses()), QProcessMetaData::getLabel, process -> null, process -> !process.getIsHidden() && !isUnrestricted(process) && context.isAllowed(process));
            appendDigestSection(digest, "Your widgets", CollectionUtils.nonNullMap(qInstance.getWidgets()), QWidgetMetaDataInterface::getLabel, widget -> AiHelpText.clean(widget.getTooltip()), widget -> settings.isWidgetAllowed(widget.getName()) && !isUnrestricted(widget) && context.isAllowed(widget));

            return (redactor.redact(digest.toString(), null));
         }));
      }
      catch(Exception e)
      {
         LOG.warn("Error building AI user digest", e);
         return ("User: (unavailable)\n");
      }
   }



   /*******************************************************************************
    ** What the assistant may do with a table (and optionally one record) for a
    ** user - for the record Developer screen.  Permission-scoped: a table that
    ** is missing, invisible (DENY_HIDE) or not readable by the user - or a
    ** record the user can't read - is reported as "not available", with nothing
    ** more about it.  Hidden and password fields are omitted.  Never throws.
    *******************************************************************************/
   public AiAccessReport describeAccess(QSession session, String tableName, Serializable primaryKey)
   {
      try
      {
         return (withUserContext(session, () -> buildAccessReport(tableName, primaryKey)));
      }
      catch(Exception e)
      {
         LOG.warn("Error building AI access report", e, logPair("tableName", tableName));
         return (notAvailableReport(tableName, primaryKey));
      }
   }



   /*******************************************************************************
    ** Build the access report (inside the user's context).
    *******************************************************************************/
   private AiAccessReport buildAccessReport(String tableName, Serializable primaryKey) throws Exception
   {
      AiToolContext  context = new AiToolContext(qInstance, settings, exposurePolicy, redactor, new AiToolResult());
      QTableMetaData table   = tableName == null ? null : qInstance.getTable(tableName);
      if(table == null || !context.isVisible(table) || !context.hasTablePermission(table, TablePermissionSubType.READ))
      {
         return (notAvailableReport(tableName, primaryKey));
      }

      QTableMetaData personalized  = context.personalize(table);
      AiExposure     tableExposure = context.getTableExposure(table);
      boolean        canEdit       = context.hasTablePermission(table, TablePermissionSubType.EDIT);

      AiAccessReport report = new AiAccessReport()
         .withTableName(table.getName())
         .withTableLabel(table.getLabel())
         .withPrimaryKey(primaryKey)
         .withTableExposure(tableExposure);

      report.getUserTablePermissions().put("read", true);
      report.getUserTablePermissions().put("insert", context.hasTablePermission(table, TablePermissionSubType.INSERT));
      report.getUserTablePermissions().put("edit", canEdit);
      report.getUserTablePermissions().put("delete", context.hasTablePermission(table, TablePermissionSubType.DELETE));

      //////////////////////////////////////////////////////////////
      // the record (if one was given) - read as the user, so its //
      // record security applies                                  //
      //////////////////////////////////////////////////////////////
      if(primaryKey != null)
      {
         QRecord record = readRecordOrNull(context, table, primaryKey);
         report.setRecordReadable(record != null);
         if(record == null)
         {
            report.setReason("record " + NOT_AVAILABLE);
         }
         else if(tableExposure != AiExposure.NONE)
         {
            report.setRecordLabel(context.recordLabel(personalized, record));
         }
      }

      for(QFieldMetaData field : CollectionUtils.nonNullMap(personalized.getFields()).values())
      {
         if(field.getIsHidden() || (field.getType() != null && field.getType().needsMasked()))
         {
            continue;
         }

         AiExposure exposure = context.getFieldExposure(personalized, field);
         report.getFields().add(new AiFieldAccess()
            .withFieldName(field.getName())
            .withLabel(field.getLabel())
            .withExposure(exposure)
            .withReason(context.getFieldAccessReason(personalized, field))
            .withCanPropose(canEdit && exposure != AiExposure.NONE));
      }

      boolean canReadAudits = GetRecordHistoryTool.canReadAudits(context);
      for(AiToolDefinition definition : getToolDefinitions())
      {
         report.getTools().add(toolApplicability(context, definition, personalized, tableExposure, report.getRecordReadable(), canReadAudits));
      }

      return (report);
   }



   /*******************************************************************************
    ** Whether one tool applies to the table/record, and why not.
    *******************************************************************************/
   private AiToolApplicability toolApplicability(AiToolContext context, AiToolDefinition definition, QTableMetaData personalized, AiExposure tableExposure, Boolean recordReadable, boolean canReadAudits)
   {
      AiToolApplicability applicability = new AiToolApplicability().withToolName(definition.getName()).withKind(definition.getKind()).withApplies(true);
      String              reason        = null;

      switch(definition.getName())
      {
         case ListTablesTool.NAME, SearchHelpTool.NAME, DescribeTableTool.NAME, QueryRecordsTool.NAME, CountRecordsTool.NAME, NavigateTool.NAME ->
         {
            if(tableExposure == AiExposure.NONE)
            {
               reason = "table not exposed to the assistant";
            }
         }
         case GetRecordTool.NAME ->
         {
            reason = recordReason(tableExposure, recordReadable);
         }
         case GetRecordHistoryTool.NAME ->
         {
            reason = recordReason(tableExposure, recordReadable);
            if(reason == null && !canReadAudits)
            {
               reason = "no READ on audit table";
            }
         }
         case SearchRecordsTool.NAME ->
         {
            reason = searchReason(context, personalized, tableExposure);
         }
         case RenderWidgetTool.NAME ->
         {
            reason = "widgets are not table-specific";
         }
         default ->
         {
            reason = "not applicable";
         }
      }

      if(reason != null)
      {
         applicability.setApplies(false);
         applicability.setReason(reason);
      }
      return (applicability);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String recordReason(AiExposure tableExposure, Boolean recordReadable)
   {
      if(tableExposure == AiExposure.NONE)
      {
         return ("table not exposed to the assistant");
      }
      if(Boolean.FALSE.equals(recordReadable))
      {
         return ("record " + NOT_AVAILABLE);
      }
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String searchReason(AiToolContext context, QTableMetaData personalized, AiExposure tableExposure)
   {
      if(!tableExposure.allowsValues())
      {
         return ("table values not exposed to the assistant");
      }
      if(CollectionUtils.nullSafeIsEmpty(personalized.getSearchFields()))
      {
         return ("table has no search fields");
      }
      for(String fieldName : personalized.getSearchFields())
      {
         QFieldMetaData field = personalized.getFields().get(fieldName);
         if(field != null && context.getFieldExposure(personalized, field) == AiExposure.NONE)
         {
            return ("a search field is not exposed to the assistant");
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** Read a record as the user - null if it can't be read (or the key is invalid).
    *******************************************************************************/
   private static QRecord readRecordOrNull(AiToolContext context, QTableMetaData table, Serializable primaryKey)
   {
      try
      {
         GetInput getInput = new GetInput(table.getName());
         getInput.setPrimaryKey(ValueUtils.getValueAsFieldType(table.getField(table.getPrimaryKeyField()).getType(), primaryKey));
         getInput.setShouldGenerateDisplayValues(true);
         context.prepareInput(getInput);
         return (new GetAction().execute(getInput).getRecord());
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** The report for a table (or record) that isn't available to the user -
    ** holding nothing but what the caller asked about.
    *******************************************************************************/
   private static AiAccessReport notAvailableReport(String tableName, Serializable primaryKey)
   {
      return (new AiAccessReport()
         .withTableName(tableName)
         .withPrimaryKey(primaryKey)
         .withRecordReadable(primaryKey == null ? null : false)
         .withReason(NOT_AVAILABLE));
   }



   /*******************************************************************************
    ** Run some code in a QContext for the given (user's) session - restoring
    ** whatever context the calling thread had afterwards (re-entrant safe).
    *******************************************************************************/
   private <T> T withUserContext(QSession session, ContextBody<T> body) throws Exception
   {
      if(session == null)
      {
         throw (new AiToolException(AiToolOutcome.DENIED, "There is no user session."));
      }

      CapturedContext previous = QContext.capture();
      try
      {
         QContext.init(qInstance, session.clone().withClientChannel(ClientChannelsMetaData.CHANNEL_ASSISTANT));
         QContext.setObject(AiToolKernelSettings.NAME, settings);
         return (body.run());
      }
      finally
      {
         if(previous.qInstance() != null || previous.qSession() != null)
         {
            QContext.init(previous);
         }
         else
         {
            QContext.clear();
         }
      }
   }



   /*******************************************************************************
    ** Code to run in a user's context.
    *******************************************************************************/
   @FunctionalInterface
   private interface ContextBody<T>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      T run() throws Exception;
   }



   /*******************************************************************************
    ** Getter for settings
    *******************************************************************************/
   public AiToolKernelSettings getSettings()
   {
      return (this.settings);
   }
}
