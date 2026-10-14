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

package com.kingsrook.qqq.backend.core.actions.help;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStateMachineStep;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** The canonical help addresses shared by search, coverage and data reload.
 *******************************************************************************/
public class HelpTargetCatalog
{
   /*******************************************************************************
    ** Enumerate all targets, including hidden targets and empty standard slots.
    *******************************************************************************/
   public static List<HelpTarget> collect(QInstance instance)
   {
      return (collect(instance, instance.getTables()));
   }



   /*******************************************************************************
    ** Use personalized tables for search without changing the shared instance.
    *******************************************************************************/
   public static List<HelpTarget> collect(QInstance instance, Map<String, QTableMetaData> tables)
   {
      List<HelpTarget> targets = new ArrayList<>();
      for(var app : instance.getApps().values())
      {
         slots(targets, "app", app.getName(), null, null, app.getLabel(), app.getParentAppName() == null ? null : "app:" + app.getParentAppName(), false, app.getHelpContent(), app);
      }
      for(var table : tables.values())
      {
         String parent = "table:" + table.getName();
         slots(targets, "table", table.getName(), table.getName(), null, table.getLabel(), null, table.getIsHidden(), table.getHelpContent(), table);
         for(var section : CollectionUtils.nonNullList(table.getSections()))
         {
            targets.add(new HelpTarget("section", parent + ";section:" + section.getName(), section.getName(), table.getName(), null, section.getLabel(), parent, table.getIsHidden() || section.getIsHidden(), section.getHelpContents(), table));
         }
         for(var field : table.getFields().values())
         {
            targets.add(new HelpTarget("field", parent + ";field:" + field.getName(), field.getName(), table.getName(), null, field.getLabel(), parent, table.getIsHidden() || field.getIsHidden(), field.getHelpContents(), table));
         }
      }
      for(var process : instance.getProcesses().values())
      {
         String parent = "process:" + process.getName();
         slots(targets, "process", process.getName(), process.getTableName(), process.getName(), process.getLabel(), null, process.getIsHidden(), process.getHelpContent(), process);
         for(var step : frontendSteps(new ArrayList<>(CollectionUtils.nonNullMap(process.getAllSteps()).values())))
         {
            targets.add(new HelpTarget("step", parent + ";step:" + step.getName(), step.getName(), process.getTableName(), process.getName(), step.getLabel(), parent, process.getIsHidden(), step.getHelpContents(), process));
         }
         Map<String, QFieldMetaData> fields = new LinkedHashMap<>();
         for(var field : CollectionUtils.mergeLists(process.getInputFields(), process.getOutputFields()))
         {
            fields.putIfAbsent(field.getName(), field);
         }
         for(var field : fields.values())
         {
            targets.add(new HelpTarget("processField", parent + ";field:" + field.getName(), field.getName(), process.getTableName(), process.getName(), field.getLabel(), parent, process.getIsHidden() || field.getIsHidden(), field.getHelpContents(), process));
         }
      }
      for(var report : instance.getReports().values())
      {
         slots(targets, "report", report.getName(), null, report.getProcessName(), report.getLabel(), null, false, report.getHelpContent(), report);
      }
      for(var widget : instance.getWidgets().values())
      {
         slots(targets, "widget", widget.getName(), null, null, widget.getLabel(), null, false, widget.getHelpContent(), widget);
      }
      for(var source : instance.getPossibleValueSources().values())
      {
         slots(targets, "possibleValueSource", source.getName(), source.getTableName(), null, source.getLabel(), null, false, source.getHelpContent(), source);
      }
      for(var entry : CollectionUtils.nonNullMap(instance.getHelpContent()).entrySet())
      {
         targets.add(new HelpTarget("instance", "instanceLevel:true;slot:" + entry.getKey(), entry.getKey(), null, null, entry.getKey(), null, false, entry.getValue(), instance));
      }
      return (targets);
   }



   /*******************************************************************************
    ** Include the standard slot and every explicitly declared slot exactly once.
    *******************************************************************************/
   private static void slots(List<HelpTarget> targets, String kind, String name, String table, String process, String label, String parent, boolean hidden, Map<String, List<QHelpContent>> contents, Object owner)
   {
      Set<String> slots = new LinkedHashSet<>();
      slots.add("widget".equals(kind) ? "label" : QHelpContent.SLOT_HELP);
      slots.addAll(CollectionUtils.nonNullMap(contents).keySet());
      for(String slot : slots)
      {
         targets.add(new HelpTarget(kind, kind + ":" + name + ";slot:" + slot, name, table, process, label, parent, hidden, contents == null ? null : contents.get(slot), owner));
      }
   }



   /*******************************************************************************
    ** Frontend steps, including nested state-machine substeps, without duplicates.
    *******************************************************************************/
   public static List<QFrontendStepMetaData> frontendSteps(List<QStepMetaData> steps)
   {
      Set<QFrontendStepMetaData> result = new LinkedHashSet<>();
      for(QStepMetaData step : CollectionUtils.nonNullList(steps))
      {
         if(step instanceof QFrontendStepMetaData frontend)
         {
            result.add(frontend);
         }
         else if(step instanceof QStateMachineStep state)
         {
            result.addAll(frontendSteps(state.getSubSteps()));
         }
      }
      return (new ArrayList<>(result));
   }
}
