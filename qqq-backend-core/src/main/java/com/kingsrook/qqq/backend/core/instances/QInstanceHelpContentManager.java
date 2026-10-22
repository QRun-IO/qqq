/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.actions.help.HelpTargetCatalog;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpFormat;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility methods for working with (dynamic, from a table) HelpContent - and
 ** putting it into meta-data in a QInstance.
 *******************************************************************************/
public class QInstanceHelpContentManager
{
   private static volatile long reloadIntervalMillis = 30_000;
   private static final Map<QInstance, ReloadState> RELOAD_STATES = Collections.synchronizedMap(new WeakHashMap<>());

   private static final QLogger LOG = QLogger.getLogger(QInstanceHelpContentManager.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void loadHelpContent(QInstance qInstance)
   {
      reloadHelpContent(qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void processHelpContentRecord(QInstance qInstance, QRecord record)
   {
      try
      {
         /////////////////////////////////////////////////
         // parse the key into its parts that we expect //
         /////////////////////////////////////////////////
         String              key            = record.getValueString("key");
         Map<String, String> nameValuePairs = new HashMap<>();
         for(String part : key.split(";"))
         {
            String[] parts = part.split(":");
            if(parts.length > 1)
            {
               nameValuePairs.put(parts[0], parts[1]);
            }
            else
            {
               LOG.info("Discarding help content with key-part that does not contain name:value format", logPair("key", key), logPair("part", part), logPair("id", record.getValue("id")));
            }
         }

         String appName     = nameValuePairs.get("app");
         String tableName   = nameValuePairs.get("table");
         String processName = nameValuePairs.get("process");
         String fieldName   = nameValuePairs.get("field");
         String sectionName = nameValuePairs.get("section");
         String stepName    = nameValuePairs.get("step");
         String widgetName  = nameValuePairs.get("widget");
         String slotName    = nameValuePairs.get("slot");
         String reportName = nameValuePairs.get("report");
         String sourceName = nameValuePairs.get("possibleValueSource");

         ///////////////////////////////////////////////////////////
         // build a help content meta-data object from the record //
         ///////////////////////////////////////////////////////////
         QHelpContent helpContent = new QHelpContent()
            .withContent(record.getValueString("content"))
            .withSummary(record.getValueString("summary"))
            .withPermissionName(record.getValueString("permissionName"))
            .withClientChannel(record.getValueString("clientChannel"))
            .withFromData(true)
            .withRole(QHelpRole.valueOf(record.getValueString("role"))); // mmm, we could fall down a bit here w/ other app-defined roles...

         if(StringUtils.hasContent(record.getValueString("format")))
         {
            helpContent.setFormat(HelpFormat.valueOf(record.getValueString("format")));
         }
         Set<HelpRole> roles = helpContent.getRoles();

         ///////////////////////////////////////////////////////////////////////////////////////////////////
         // check - if there are no contents, then let's remove this help content from the container      //
         // (note pre-delete customizer will take advantage of this, passing in empty content on purpose) //
         ///////////////////////////////////////////////////////////////////////////////////////////////////
         if(!StringUtils.hasContent(helpContent.getContent()) && !StringUtils.hasContent(helpContent.getSummary()))
         {
            for(var target : HelpTargetCatalog.collect(qInstance))
            {
               Map<String, String> matchingParts = new HashMap<>(nameValuePairs);
               if(processName != null && processName.startsWith("*") && target.process() != null && target.process().endsWith(processName.substring(1)))
               {
                  matchingParts.put("process", target.process());
               }
               if(keyParts(target.key()).equals(matchingParts))
               {
                  removeDataContents(target.contents(), roles);
               }
            }
            helpContent = null;
            if(appName != null || tableName != null || processName != null || widgetName != null || reportName != null || sourceName != null || nameValuePairs.containsKey("instanceLevel"))
            {
               return;
            }
         }

         ///////////////////////////////////////////////////////////////////////////////////
         // look at what parts of the key we got, and find the meta-data object to update //
         ///////////////////////////////////////////////////////////////////////////////////
         if(StringUtils.hasContent(appName))
         {
            processHelpContentForApp(qInstance, key, appName, slotName, helpContent);
         }
         else if(StringUtils.hasContent(tableName))
         {
            processHelpContentForTable(qInstance, key, tableName, sectionName, fieldName, slotName, helpContent);
         }
         else if(StringUtils.hasContent(processName))
         {
            processHelpContentForProcess(qInstance, key, processName, fieldName, stepName, slotName, helpContent);
         }
         else if(StringUtils.hasContent(reportName) && qInstance.getReport(reportName) != null && StringUtils.hasContent(slotName))
         {
            qInstance.getReport(reportName).withHelpContent(slotName, helpContent);
         }
         else if(StringUtils.hasContent(sourceName) && qInstance.getPossibleValueSource(sourceName) != null && StringUtils.hasContent(slotName))
         {
            qInstance.getPossibleValueSource(sourceName).withHelpContent(slotName, helpContent);
         }
         else if(StringUtils.hasContent(widgetName))
         {
            processHelpContentForWidget(qInstance, key, widgetName, slotName, helpContent);
         }
         else if(nameValuePairs.containsKey("instanceLevel"))
         {
            processHelpContentForInstance(qInstance, key, slotName, helpContent);
         }
         else
         {
            for(QSupplementalInstanceMetaData supplementalInstanceMetaData : qInstance.getSupplementalMetaData().values())
            {
               if(supplementalInstanceMetaData instanceof QHelpContentPlugin helpContentPlugin)
               {
                  try
                  {
                     helpContentPlugin.acceptHelpContent(qInstance, helpContent, nameValuePairs);
                  }
                  catch(Exception e)
                  {
                     LOG.warn("Error processing a helpContent record in a helpContentPlugin", e, logPair("pluginName", supplementalInstanceMetaData.getName()), logPair("id", record.getValue("id")));
                  }
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error processing a helpContent record", e, logPair("id", record.getValue("id")));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForApp(QInstance qInstance, String key, String appName, String slotName, QHelpContent helpContent)
   {
      QAppMetaData app = qInstance.getApp(appName);
      if(!StringUtils.hasContent(slotName))
      {
         LOG.info("Missing slot name in help content", logPair("key", key));
      }
      else if(app == null)
      {
         LOG.info("Unrecognized app in help content", logPair("key", key));
      }
      else
      {
         app.withHelpContent(slotName, helpContent);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForTable(QInstance qInstance, String key, String tableName, String sectionName, String fieldName, String slotName, QHelpContent helpContent)
   {
      QTableMetaData table = qInstance.getTable(tableName);
      if(table == null)
      {
         LOG.info("Unrecognized table in help content", logPair("key", key));
      }
      else if(StringUtils.hasContent(fieldName))
      {
         //////////////////////////
         // handle a table field //
         //////////////////////////
         QFieldMetaData field = table.getFields().get(fieldName);
         if(field == null)
         {
            LOG.info("Unrecognized table field in help content", logPair("key", key));
         }
         else
         {
            field.withHelpContent(helpContent);
         }
      }
      else if(StringUtils.hasContent(sectionName))
      {
         ////////////////////////////
         // handle a table section //
         ////////////////////////////
         Optional<QFieldSection> optionalSection = table.getSections().stream().filter(s -> sectionName.equals(s.getName())).findFirst();
         if(optionalSection.isEmpty())
         {
            LOG.info("Unrecognized table section in help content", logPair("key", key));
         }
         else
         {
            optionalSection.get().withHelpContent(helpContent);
         }
      }
      else
      {
         if(!StringUtils.hasContent(slotName))
         {
            LOG.info("Missing slot name in table-level help content", logPair("key", key));
         }
         else
         {
            table.withHelpContent(slotName, helpContent);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForProcess(QInstance qInstance, String key, String processName, String fieldName, String stepName, String slotName, QHelpContent helpContent)
   {
      if(processName.startsWith("*") && processName.length() > 1)
      {
         boolean anyMatched = false;
         String  subName    = processName.substring(1);
         for(QProcessMetaData process : qInstance.getProcesses().values())
         {
            if(process.getName().endsWith(subName))
            {
               anyMatched = true;
               processHelpContentForProcess(qInstance, key, process.getName(), fieldName, stepName, slotName, helpContent);
            }
         }

         if(!anyMatched)
         {
            LOG.info("Wildcard process name did not match any processes in help content", logPair("key", key));
         }

         return;
      }

      QProcessMetaData process = qInstance.getProcess(processName);
      if(process == null)
      {
         LOG.info("Unrecognized process in help content", logPair("key", key));
      }
      else if(StringUtils.hasContent(fieldName))
      {
         ////////////////////////////
         // handle a process field //
         ////////////////////////////
         Optional<QFieldMetaData> optionalField = CollectionUtils.mergeLists(process.getInputFields(), process.getOutputFields())
            .stream().filter(f -> fieldName.equals(f.getName()))
            .findFirst();

         if(optionalField.isEmpty())
         {
            LOG.info("Unrecognized process field in help content", logPair("key", key));
         }
         else
         {
            optionalField.get().withHelpContent(helpContent);
         }
      }
      else if(StringUtils.hasContent(stepName))
      {
         /////////////////////////////
         // handle a process screen //
         /////////////////////////////
         QFrontendStepMetaData frontendStep = process.getFrontendStep(stepName);

         if(frontendStep == null)
         {
            LOG.info("Unrecognized process step in help content", logPair("key", key));
         }
         else
         {
            frontendStep.withHelpContent(helpContent);
         }
      }
      else if(StringUtils.hasContent(slotName))
      {
         process.withHelpContent(slotName, helpContent);
      }
      else
      {
         LOG.info("Unrecognized key format for process help content", logPair("key", key));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForWidget(QInstance qInstance, String key, String widgetName, String slotName, QHelpContent helpContent)
   {
      QWidgetMetaDataInterface widget = qInstance.getWidget(widgetName);
      if(!StringUtils.hasContent(slotName))
      {
         LOG.info("Missing slot name in help content", logPair("key", key));
      }
      else if(widget == null)
      {
         LOG.info("Unrecognized widget in help content", logPair("key", key));
      }
      else
      {
         widget.withHelpContent(slotName, helpContent);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForInstance(QInstance qInstance, String key, String slotName, QHelpContent helpContent)
   {
      if(!StringUtils.hasContent(slotName))
      {
         LOG.info("Missing slot name in instance-level help content", logPair("key", key));
      }
      else
      {
         qInstance.withHelpContent(slotName, helpContent);
      }
   }



   /*******************************************************************************
    ** add a help content object to a list - replacing an entry in the list with the
    ** same roles if one is found.
    *******************************************************************************/
   public static void putHelpContentInList(QHelpContent helpContent, List<QHelpContent> helpContents)
   {
      if(helpContent.getFromData())
      {
         helpContent = helpContent.clone();
      }
      ListIterator<QHelpContent> iterator = helpContents.listIterator();
      while(iterator.hasNext())
      {
         QHelpContent existingContent = iterator.next();
         if(Objects.equals(existingContent.getRoles(), helpContent.getRoles()))
         {
            if(helpContent.getFromData())
            {
               helpContent.setCodeContent(existingContent.getFromData() ? existingContent.getCodeContent() : existingContent);
            }
            iterator.set(helpContent);
            return;
         }
      }

      helpContents.add(helpContent);
   }



   /*******************************************************************************
    ** Remove any helpContent entries in a list if they have a set of roles that matches
    ** the input set.
    *******************************************************************************/
   public static void removeHelpContentByRoleSetFromList(Set<HelpRole> roles, List<QHelpContent> helpContents)
   {
      if(helpContents == null)
      {
         return;
      }

      helpContents.removeIf(existingContent -> Objects.equals(existingContent.getRoles(), roles));
   }



   /*******************************************************************************
    ** Configure the per-instance poll throttle; zero is useful for deterministic tests.
    *******************************************************************************/
   public static void setReloadIntervalMillis(long intervalMillis)
   {
      reloadIntervalMillis = Math.max(0, intervalMillis);
   }



   /*******************************************************************************
    ** Reload only after the table's count/max-modifyDate signature changes.
    ** Polling is internal metadata maintenance, independent of the viewing user's
    ** permissions on the authoring table. The user's context is always restored.
    *******************************************************************************/
   public static void reloadIfChanged(QInstance instance)
   {
      if(instance == null || instance.getTable(HelpContent.TABLE_NAME) == null)
      {
         return;
      }
      ReloadState state = RELOAD_STATES.computeIfAbsent(instance, key -> new ReloadState());
      synchronized(state)
      {
         long now = System.currentTimeMillis();
         if(now - state.lastChecked < reloadIntervalMillis)
         {
            return;
         }
         state.lastChecked = now;
         try
         {
            QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance)), () ->
            {
               String signature = signature();
               if(!Objects.equals(signature, state.signature))
               {
                  reload(instance, state, signature);
               }
            });
         }
         catch(Exception e)
         {
            LOG.warn("Error checking help content changes", e);
         }
      }
   }



   /*******************************************************************************
    ** Replace data-applied help, restoring any code declaration it had overridden.
    *******************************************************************************/
   public static void reloadHelpContent(QInstance instance)
   {
      if(instance == null || instance.getTable(HelpContent.TABLE_NAME) == null)
      {
         return;
      }
      ReloadState state = RELOAD_STATES.computeIfAbsent(instance, key -> new ReloadState());
      synchronized(state)
      {
         try
         {
            QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession(instance)), () -> reload(instance, state, signature()));
         }
         catch(Exception e)
         {
            LOG.error("Error loading help content", e);
         }
      }
   }



   /*******************************************************************************
    ** Read successfully before changing live metadata; failed reads leave help intact.
    *******************************************************************************/
   private static void reload(QInstance instance, ReloadState state, String signature) throws QException
   {
      QueryOutput output = new QueryAction().execute(new QueryInput().withTableName(HelpContent.TABLE_NAME));
      for(var target : HelpTargetCatalog.collect(instance))
      {
         removeDataContents(target.contents(), null);
      }
      for(QRecord record : output.getRecords())
      {
         processHelpContentRecord(instance, record);
      }
      state.signature = signature;
      state.lastChecked = System.currentTimeMillis();
   }



   /*******************************************************************************
    ** Two database aggregates avoid fetching content on unchanged polls.
    *******************************************************************************/
   private static String signature() throws QException
   {
      Aggregate count = new Aggregate("id", AggregateOperator.COUNT);
      Aggregate modified = new Aggregate("modifyDate", AggregateOperator.MAX);
      var results = new AggregateAction().execute(new AggregateInput().withTableName(HelpContent.TABLE_NAME).withAggregate(count).withAggregate(modified)).getResults();
      return (results.isEmpty() ? "0:null" : results.getFirst().getAggregateValue(count) + ":" + results.getFirst().getAggregateValue(modified));
   }



   /*******************************************************************************
    ** Remove data entries only. A null role set selects all data entries.
    *******************************************************************************/
   private static void removeDataContents(List<QHelpContent> contents, Set<HelpRole> roles)
   {
      if(contents == null)
      {
         return;
      }
      var iterator = contents.listIterator();
      while(iterator.hasNext())
      {
         QHelpContent content = iterator.next();
         if(content.getFromData() && (roles == null || Objects.equals(roles, content.getRoles())))
         {
            if(content.getCodeContent() == null)
            {
               iterator.remove();
            }
            else
            {
               iterator.set(content.getCodeContent());
            }
         }
      }
   }



   /*******************************************************************************
    ** Deep copy help slots when metadata is cloned.
    *******************************************************************************/
   public static Map<String, List<QHelpContent>> cloneHelpContent(Map<String, List<QHelpContent>> contents)
   {
      if(contents == null)
      {
         return (null);
      }
      Map<String, List<QHelpContent>> copy = new HashMap<>();
      contents.forEach((slot, entries) -> copy.put(slot, entries == null ? null : new ArrayList<>(entries.stream().map(QHelpContent::clone).toList())));
      return (copy);
   }



   /*******************************************************************************
    ** Weakly keyed per-instance polling state; does not retain the instance.
    *******************************************************************************/
   private static class ReloadState
   {
      private long lastChecked;
      private String signature;
   }


   /*******************************************************************************
    ** Normalize help addresses so deletion is independent of key-part order.
    *******************************************************************************/
   private static Map<String, String> keyParts(String key)
   {
      Map<String, String> parts = new HashMap<>();
      for(String part : key.split(";"))
      {
         String[] pair = part.split(":", 2);
         if(pair.length == 2)
         {
            parts.put(pair[0], pair[1]);
         }
      }
      return (parts);
   }
}
