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

package com.kingsrook.qqq.backend.core.instances;


import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.logging.QLogger;
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
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Utility methods for working with (dynamic, from a table) HelpContent - and
 ** putting it into meta-data in a QInstance.
 *******************************************************************************/
public class QInstanceHelpContentManager
{
   private static final QLogger LOG = QLogger.getLogger(QInstanceHelpContentManager.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void loadHelpContent(QInstance qInstance)
   {
      try
      {
         if(qInstance.getTable(HelpContent.TABLE_NAME) == null)
         {
            return;
         }

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(HelpContent.TABLE_NAME);
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         for(QRecord record : queryOutput.getRecords())
         {
            processHelpContentRecord(qInstance, record);
         }
      }
      catch(Exception e)
      {
         LOG.error("Error loading help content", e);
      }
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

         ///////////////////////////////////////////////////////////
         // build a help content meta-data object from the record //
         ///////////////////////////////////////////////////////////
         QHelpContent helpContent = new QHelpContent()
            .withContent(record.getValueString("content"))
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
         if(!StringUtils.hasContent(helpContent.getContent()))
         {
            helpContent = null;
         }

         ///////////////////////////////////////////////////////////////////////////////////
         // look at what parts of the key we got, and find the meta-data object to update //
         ///////////////////////////////////////////////////////////////////////////////////
         if(StringUtils.hasContent(appName))
         {
            processHelpContentForApp(qInstance, key, appName, slotName, roles, helpContent);
         }
         else if(StringUtils.hasContent(tableName))
         {
            processHelpContentForTable(qInstance, key, tableName, sectionName, fieldName, slotName, roles, helpContent);
         }
         else if(StringUtils.hasContent(processName))
         {
            processHelpContentForProcess(qInstance, key, processName, fieldName, stepName, roles, helpContent);
         }
         else if(StringUtils.hasContent(widgetName))
         {
            processHelpContentForWidget(qInstance, key, widgetName, slotName, roles, helpContent);
         }
         else if(nameValuePairs.containsKey("instanceLevel"))
         {
            processHelpContentForInstance(qInstance, key, slotName, roles, helpContent);
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
   private static void processHelpContentForApp(QInstance qInstance, String key, String appName, String slotName, Set<HelpRole> roles, QHelpContent helpContent)
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
         if(helpContent != null)
         {
            app.withHelpContent(slotName, helpContent);
         }
         else
         {
            app.removeHelpContent(slotName, roles);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForTable(QInstance qInstance, String key, String tableName, String sectionName, String fieldName, String slotName, Set<HelpRole> roles, QHelpContent helpContent)
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
         else if(helpContent != null)
         {
            field.withHelpContent(helpContent);
         }
         else
         {
            field.removeHelpContent(roles);
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
         else if(helpContent != null)
         {
            optionalSection.get().withHelpContent(helpContent);
         }
         else
         {
            optionalSection.get().removeHelpContent(roles);
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
            if(helpContent != null)
            {
               table.withHelpContent(slotName, helpContent);
            }
            else
            {
               table.removeHelpContent(slotName, roles);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForProcess(QInstance qInstance, String key, String processName, String fieldName, String stepName, Set<HelpRole> roles, QHelpContent helpContent)
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
               processHelpContentForProcess(qInstance, key, process.getName(), fieldName, stepName, roles, helpContent);
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
         else if(helpContent != null)
         {
            optionalField.get().withHelpContent(helpContent);
         }
         else
         {
            optionalField.get().removeHelpContent(roles);
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
         else if(helpContent != null)
         {
            frontendStep.withHelpContent(helpContent);
         }
         else
         {
            frontendStep.removeHelpContent(roles);
         }
      }
      else
      {
         LOG.info("Unrecognized key format for process help content", logPair("key", key));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForWidget(QInstance qInstance, String key, String widgetName, String slotName, Set<HelpRole> roles, QHelpContent helpContent)
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
         if(helpContent != null)
         {
            widget.withHelpContent(slotName, helpContent);
         }
         else
         {
            widget.removeHelpContent(slotName, roles);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processHelpContentForInstance(QInstance qInstance, String key, String slotName, Set<HelpRole> roles, QHelpContent helpContent)
   {
      if(!StringUtils.hasContent(slotName))
      {
         LOG.info("Missing slot name in instance-level help content", logPair("key", key));
      }
      else
      {
         if(helpContent != null)
         {
            qInstance.withHelpContent(slotName, helpContent);
         }
         else
         {
            qInstance.removeHelpContent(slotName, roles);
         }
      }
   }



   /*******************************************************************************
    ** add a help content object to a list - replacing an entry in the list with the
    ** same roles if one is found.
    *******************************************************************************/
   public static void putHelpContentInList(QHelpContent helpContent, List<QHelpContent> helpContents)
   {
      ListIterator<QHelpContent> iterator = helpContents.listIterator();
      while(iterator.hasNext())
      {
         QHelpContent existingContent = iterator.next();
         if(Objects.equals(existingContent.getRoles(), helpContent.getRoles()))
         {
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

}
