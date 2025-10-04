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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppChildMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.SourceQBitAware;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.BasepullConfiguration;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Meta-Data to define a process in a QQQ instance.
 **
 *******************************************************************************/
public class QProcessMetaData implements QAppChildMetaData, MetaDataWithPermissionRules, TopLevelMetaDataInterface, SourceQBitAware
{
   private String name;
   private String label;
   private String tableName;

   private String sourceQBitName;

   private boolean               isHidden = false;
   private BasepullConfiguration basepullConfiguration;
   private QPermissionRules      permissionRules;

   private Integer minInputRecords = null;
   private Integer maxInputRecords = null;

   private ProcessStepFlow            stepFlow = ProcessStepFlow.LINEAR;
   private List<QStepMetaData>        stepList; // these are the steps that are ran, by-default, in the order they are ran in
   private Map<String, QStepMetaData> steps; // this is the full map of possible steps

   private QBackendStepMetaData cancelStep;

   private QIcon icon;

   private QScheduleMetaData schedule;

   private VariantRunStrategy variantRunStrategy;
   private String             variantBackend;

   private QCodeReference processTracerCodeReference;

   private Map<String, QSupplementalProcessMetaData> supplementalMetaData;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("QProcessMetaData[" + name + "]");
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public QProcessMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   public QProcessMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public QProcessMetaData withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for stepList
    **
    *******************************************************************************/
   public List<QStepMetaData> getStepList()
   {
      return stepList;
   }



   /*******************************************************************************
    ** Setter for stepList
    **
    *******************************************************************************/
   public QProcessMetaData withStepList(List<QStepMetaData> stepList)
   {
      if(stepList != null)
      {
         stepList.forEach(this::withStep);
      }

      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public QProcessMetaData withStep(QStepMetaData step)
   {
      int index = 0;
      if(this.stepList != null)
      {
         index = this.stepList.size();
      }
      withStep(index, step);

      return (this);
   }



   /*******************************************************************************
    ** add a step to the stepList and map
    **
    *******************************************************************************/
   @Deprecated(since = "withStep was added")
   public QProcessMetaData addStep(QStepMetaData step)
   {
      return (withStep(step));
   }



   /*******************************************************************************
    ** add a step to the stepList (at the specified index) and the step map
    **
    *******************************************************************************/
   public QProcessMetaData withStep(int index, QStepMetaData step)
   {
      if(this.stepList == null)
      {
         this.stepList = new ArrayList<>();
      }
      this.stepList.add(index, step);

      if(this.steps == null)
      {
         this.steps = new LinkedHashMap<>();
      }

      if(!StringUtils.hasContent(step.getName()))
      {
         throw (new IllegalArgumentException("Attempt to add a process step without a name"));
      }

      this.steps.put(step.getName(), step);

      return (this);
   }



   /*******************************************************************************
    ** add a step to the stepList (at the specified index) and the step map
    **
    *******************************************************************************/
   @Deprecated(since = "withStep was added")
   public QProcessMetaData addStep(int index, QStepMetaData step)
   {
      return (withStep(index, step));
   }



   /*******************************************************************************
    ** add a step ONLY to the step map - NOT the list w/ default execution order.
    **
    *******************************************************************************/
   public QProcessMetaData withOptionalStep(QStepMetaData step)
   {
      if(this.steps == null)
      {
         this.steps = new LinkedHashMap<>();
      }

      if(!StringUtils.hasContent(step.getName()))
      {
         throw (new IllegalArgumentException("Attempt to add a process step without a name"));
      }

      this.steps.put(step.getName(), step);

      return (this);
   }



   /*******************************************************************************
    ** add a step ONLY to the step map - NOT the list w/ default execution order.
    **
    *******************************************************************************/
   @Deprecated(since = "withOptionalStep was added")
   public QProcessMetaData addOptionalStep(QStepMetaData step)
   {
      return (withOptionalStep(step));
   }



   /*******************************************************************************
    ** Setter for stepList - note - calling this method ALSO overwrites the steps map!
    **
    *******************************************************************************/
   public void setStepList(List<QStepMetaData> stepList)
   {
      if(stepList == null)
      {
         this.stepList = null;
         this.steps = null;
      }
      else
      {
         this.stepList = new ArrayList<>();
         this.steps = new LinkedHashMap<>();
      }

      withStepList(stepList);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QStepMetaData getStep(String stepName)
   {
      if(steps.containsKey(stepName))
      {
         return steps.get(stepName);
      }

      for(QStepMetaData step : steps.values())
      {
         if(step instanceof QStateMachineStep stateMachineStep)
         {
            for(QStepMetaData subStep : stateMachineStep.getSubSteps())
            {
               if(subStep.getName().equals(stepName))
               {
                  return (subStep);
               }
            }
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Wrapper to getStep, that internally casts to BackendStepMetaData
    *******************************************************************************/
   public QBackendStepMetaData getBackendStep(String name)
   {
      return (QBackendStepMetaData) getStep(name);
   }



   /*******************************************************************************
    ** Wrapper to getStep, that internally casts to FrontendStepMetaData
    *******************************************************************************/
   public QFrontendStepMetaData getFrontendStep(String name)
   {
      return (QFrontendStepMetaData) getStep(name);
   }



   /*******************************************************************************
    ** Get a list of all the *unique* input fields used by all the steps in this process.
    *******************************************************************************/
   @JsonIgnore
   public List<QFieldMetaData> getInputFields()
   {
      Set<String>          usedFieldNames = new HashSet<>();
      List<QFieldMetaData> rs             = new ArrayList<>();

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // first iterate over the step list - doing this so we get the fields back in a predictable order //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      if(stepList != null)
      {
         for(QStepMetaData step : stepList)
         {
            for(QFieldMetaData field : step.getInputFields())
            {
               if(!usedFieldNames.contains(field.getName()))
               {
                  rs.add(field);
                  usedFieldNames.add(field.getName());
               }
            }
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////
      // if the step map ('steps') is bigger than the stepList, that would mean          //
      // we have some optional steps in the process.  in that case, add their fields too //
      /////////////////////////////////////////////////////////////////////////////////////
      if(steps != null && steps.size() > CollectionUtils.nonNullList(stepList).size())
      {
         for(QStepMetaData step : steps.values())
         {
            if(stepList != null && stepList.stream().anyMatch(s -> s.getName().equals(step.getName())))
            {
               ////////////////////////////////////////////////////////
               // skip steps we already processed from the step list //
               ////////////////////////////////////////////////////////
               continue;
            }

            for(QFieldMetaData field : step.getInputFields())
            {
               if(!usedFieldNames.contains(field.getName()))
               {
                  rs.add(field);
                  usedFieldNames.add(field.getName());
               }
            }
         }
      }

      return (rs);
   }



   /*******************************************************************************
    ** Get the (optional, first) input field on this process with the given name.
    *
    * @param name name of the field to be returned
    * @return the first input field found in the process's steps that has that name,
    * or empty if none are found (where first is based on:  the order of steps in
    * the stepList, then any optional steps, in the order in which they come out
    * of the step map (which internally is always built as LinkedHashMap, so it
    * preserves insertion order - but - if the caller supplied their own map, then
    * results could potentially get into undefined behavior!)
    *******************************************************************************/
   public Optional<QFieldMetaData> getInputField(String name)
   {
      return getInputFields().stream().filter(f -> f.getName().equals(name)).findFirst();
   }



   /*******************************************************************************
    ** Get a list of all the *unique* output fields used by all the steps in this process.
    *******************************************************************************/
   @JsonIgnore
   public List<QFieldMetaData> getOutputFields()
   {
      Set<String>          usedFieldNames = new HashSet<>();
      List<QFieldMetaData> rs             = new ArrayList<>();
      if(steps != null)
      {
         for(QStepMetaData step : steps.values())
         {
            for(QFieldMetaData field : step.getOutputFields())
            {
               if(!usedFieldNames.contains(field.getName()))
               {
                  rs.add(field);
                  usedFieldNames.add(field.getName());
               }
            }
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** Getter for isHidden
    **
    *******************************************************************************/
   public boolean getIsHidden()
   {
      return (isHidden);
   }



   /*******************************************************************************
    ** Setter for isHidden
    **
    *******************************************************************************/
   public void setIsHidden(boolean isHidden)
   {
      this.isHidden = isHidden;
   }



   /*******************************************************************************
    ** Fluent Setter for isHidden
    **
    *******************************************************************************/
   public QProcessMetaData withIsHidden(boolean isHidden)
   {
      this.isHidden = isHidden;
      return (this);
   }



   /*******************************************************************************
    ** Getter for icon
    **
    *******************************************************************************/
   public QIcon getIcon()
   {
      return icon;
   }



   /*******************************************************************************
    ** Setter for icon
    **
    *******************************************************************************/
   public void setIcon(QIcon icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** Fluent setter for icon
    **
    *******************************************************************************/
   public QProcessMetaData withIcon(QIcon icon)
   {
      this.icon = icon;
      return (this);
   }



   /*******************************************************************************
    ** Getter for schedule
    **
    *******************************************************************************/
   public QScheduleMetaData getSchedule()
   {
      return schedule;
   }



   /*******************************************************************************
    ** Setter for schedule
    **
    *******************************************************************************/
   public void setSchedule(QScheduleMetaData schedule)
   {
      this.schedule = schedule;
   }



   /*******************************************************************************
    ** Fluent setter for schedule
    **
    *******************************************************************************/
   public QProcessMetaData withSchedule(QScheduleMetaData schedule)
   {
      this.schedule = schedule;
      return (this);
   }



   /*******************************************************************************
    ** Getter for basepullConfiguration
    **
    *******************************************************************************/
   public BasepullConfiguration getBasepullConfiguration()
   {
      return basepullConfiguration;
   }



   /*******************************************************************************
    ** Setter for basepullConfiguration
    **
    *******************************************************************************/
   public void setBasepullConfiguration(BasepullConfiguration basepullConfiguration)
   {
      this.basepullConfiguration = basepullConfiguration;
   }



   /*******************************************************************************
    ** Fluent setter for basepullConfiguration
    **
    *******************************************************************************/
   public QProcessMetaData withBasepullConfiguration(BasepullConfiguration basepullConfiguration)
   {
      this.basepullConfiguration = basepullConfiguration;
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionRules
    *******************************************************************************/
   public QPermissionRules getPermissionRules()
   {
      return (this.permissionRules);
   }



   /*******************************************************************************
    ** Setter for permissionRules
    *******************************************************************************/
   public void setPermissionRules(QPermissionRules permissionRules)
   {
      this.permissionRules = permissionRules;
   }



   /*******************************************************************************
    ** Fluent setter for permissionRules
    *******************************************************************************/
   public QProcessMetaData withPermissionRules(QPermissionRules permissionRules)
   {
      this.permissionRules = permissionRules;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addProcess(this);
   }



   /*******************************************************************************
    ** Getter for supplementalMetaData
    *******************************************************************************/
   public Map<String, QSupplementalProcessMetaData> getSupplementalMetaData()
   {
      return (this.supplementalMetaData);
   }



   /*******************************************************************************
    ** Getter for supplementalMetaData
    *******************************************************************************/
   public QSupplementalProcessMetaData getSupplementalMetaData(String type)
   {
      if(this.supplementalMetaData == null)
      {
         return (null);
      }
      return this.supplementalMetaData.get(type);
   }



   /*******************************************************************************
    ** Setter for supplementalMetaData
    *******************************************************************************/
   public void setSupplementalMetaData(Map<String, QSupplementalProcessMetaData> supplementalMetaData)
   {
      this.supplementalMetaData = supplementalMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for supplementalMetaData
    *******************************************************************************/
   public QProcessMetaData withSupplementalMetaData(Map<String, QSupplementalProcessMetaData> supplementalMetaData)
   {
      this.supplementalMetaData = supplementalMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for supplementalMetaData
    *******************************************************************************/
   public QProcessMetaData withSupplementalMetaData(QSupplementalProcessMetaData supplementalMetaData)
   {
      if(this.supplementalMetaData == null)
      {
         this.supplementalMetaData = new HashMap<>();
      }
      this.supplementalMetaData.put(supplementalMetaData.getType(), supplementalMetaData);
      return (this);
   }



   /*******************************************************************************
    ** Getter for minInputRecords
    *******************************************************************************/
   public Integer getMinInputRecords()
   {
      return (this.minInputRecords);
   }



   /*******************************************************************************
    ** Setter for minInputRecords
    *******************************************************************************/
   public void setMinInputRecords(Integer minInputRecords)
   {
      this.minInputRecords = minInputRecords;
   }



   /*******************************************************************************
    ** Fluent setter for minInputRecords
    *******************************************************************************/
   public QProcessMetaData withMinInputRecords(Integer minInputRecords)
   {
      this.minInputRecords = minInputRecords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxInputRecords
    *******************************************************************************/
   public Integer getMaxInputRecords()
   {
      return (this.maxInputRecords);
   }



   /*******************************************************************************
    ** Setter for maxInputRecords
    *******************************************************************************/
   public void setMaxInputRecords(Integer maxInputRecords)
   {
      this.maxInputRecords = maxInputRecords;
   }



   /*******************************************************************************
    ** Fluent setter for maxInputRecords
    *******************************************************************************/
   public QProcessMetaData withMaxInputRecords(Integer maxInputRecords)
   {
      this.maxInputRecords = maxInputRecords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for variantRunStrategy
    *******************************************************************************/
   public VariantRunStrategy getVariantRunStrategy()
   {
      return (this.variantRunStrategy);
   }



   /*******************************************************************************
    ** Setter for variantRunStrategy
    *******************************************************************************/
   public void setVariantRunStrategy(VariantRunStrategy variantRunStrategy)
   {
      this.variantRunStrategy = variantRunStrategy;
   }



   /*******************************************************************************
    ** Fluent setter for variantRunStrategy
    *******************************************************************************/
   public QProcessMetaData withVariantRunStrategy(VariantRunStrategy variantRunStrategy)
   {
      this.variantRunStrategy = variantRunStrategy;
      return (this);
   }



   /*******************************************************************************
    ** Getter for variantBackend
    *******************************************************************************/
   public String getVariantBackend()
   {
      return (this.variantBackend);
   }



   /*******************************************************************************
    ** Setter for variantBackend
    *******************************************************************************/
   public void setVariantBackend(String variantBackend)
   {
      this.variantBackend = variantBackend;
   }



   /*******************************************************************************
    ** Fluent setter for variantBackend
    *******************************************************************************/
   public QProcessMetaData withVariantBackend(String variantBackend)
   {
      this.variantBackend = variantBackend;
      return (this);
   }



   /*******************************************************************************
    ** Getter for the full map of all steps (not the step list!)
    **
    *******************************************************************************/
   public Map<String, QStepMetaData> getAllSteps()
   {
      return steps;
   }



   /*******************************************************************************
    ** Getter for cancelStep
    *******************************************************************************/
   public QBackendStepMetaData getCancelStep()
   {
      return (this.cancelStep);
   }



   /*******************************************************************************
    ** Setter for cancelStep
    *******************************************************************************/
   public void setCancelStep(QBackendStepMetaData cancelStep)
   {
      this.cancelStep = cancelStep;
   }



   /*******************************************************************************
    ** Fluent setter for cancelStep
    *******************************************************************************/
   public QProcessMetaData withCancelStep(QBackendStepMetaData cancelStep)
   {
      this.cancelStep = cancelStep;
      return (this);
   }



   /*******************************************************************************
    ** Getter for stepFlow
    *******************************************************************************/
   public ProcessStepFlow getStepFlow()
   {
      return (this.stepFlow);
   }



   /*******************************************************************************
    ** Setter for stepFlow
    *******************************************************************************/
   public void setStepFlow(ProcessStepFlow stepFlow)
   {
      this.stepFlow = stepFlow;
   }



   /*******************************************************************************
    ** Fluent setter for stepFlow
    *******************************************************************************/
   public QProcessMetaData withStepFlow(ProcessStepFlow stepFlow)
   {
      this.stepFlow = stepFlow;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processTracerCodeReference
    *******************************************************************************/
   public QCodeReference getProcessTracerCodeReference()
   {
      return (this.processTracerCodeReference);
   }



   /*******************************************************************************
    ** Setter for processTracerCodeReference
    *******************************************************************************/
   public void setProcessTracerCodeReference(QCodeReference processTracerCodeReference)
   {
      this.processTracerCodeReference = processTracerCodeReference;
   }



   /*******************************************************************************
    ** Fluent setter for processTracerCodeReference
    *******************************************************************************/
   public QProcessMetaData withProcessTracerCodeReference(QCodeReference processTracerCodeReference)
   {
      this.processTracerCodeReference = processTracerCodeReference;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceQBitName
    *******************************************************************************/
   @Override
   public String getSourceQBitName()
   {
      return (this.sourceQBitName);
   }



   /*******************************************************************************
    ** Setter for sourceQBitName
    *******************************************************************************/
   @Override
   public void setSourceQBitName(String sourceQBitName)
   {
      this.sourceQBitName = sourceQBitName;
   }



   /*******************************************************************************
    ** Fluent setter for sourceQBitName
    *******************************************************************************/
   @Override
   public QProcessMetaData withSourceQBitName(String sourceQBitName)
   {
      this.sourceQBitName = sourceQBitName;
      return (this);
   }

}
