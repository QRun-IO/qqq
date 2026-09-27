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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStateMachineStep;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 * Version of QProcessMetaData that's meant for transmitting to a frontend.
 * e.g., it excludes backend-only details.
 *
 *******************************************************************************/
@JsonInclude(Include.NON_NULL)
public class QFrontendProcessMetaData
{
   private String  name;
   private String  label;
   private String  tableName;
   private boolean isHidden;
   private Integer minInputRecords;
   private Integer maxInputRecords;

   private QIcon icon;

   private List<QFrontendStepMetaData> frontendSteps;
   private String                      stepFlow;

   private boolean hasPermission;

   //////////////////////////////////////////////////////////////////////////////////
   // do not add setters.  take values from the source-object in the constructor!! //
   //////////////////////////////////////////////////////////////////////////////////



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFrontendProcessMetaData(AbstractActionInput actionInput, QProcessMetaData processMetaData, boolean includeSteps)
   {
      this.name = processMetaData.getName();
      this.label = processMetaData.getLabel();
      this.tableName = processMetaData.getTableName();
      this.isHidden = processMetaData.getIsHidden();
      this.stepFlow = processMetaData.getStepFlow().toString();
      this.minInputRecords = processMetaData.getMinInputRecords();
      this.maxInputRecords = processMetaData.getMaxInputRecords();

      if(includeSteps)
      {
         if(CollectionUtils.nullSafeHasContents(processMetaData.getStepList()))
         {
            this.frontendSteps = switch(processMetaData.getStepFlow())
            {
               case LINEAR -> processMetaData.getStepList().stream()
                  .filter(QFrontendStepMetaData.class::isInstance)
                  .map(QFrontendStepMetaData.class::cast)
                  .collect(Collectors.toList());

               case STATE_MACHINE -> processMetaData.getAllSteps().values().stream()
                  .filter(QStateMachineStep.class::isInstance)
                  .map(QStateMachineStep.class::cast)
                  .flatMap(step -> step.getSubSteps().stream())
                  .filter(QFrontendStepMetaData.class::isInstance)
                  .map(QFrontendStepMetaData.class::cast)
                  .collect(Collectors.toList());
            };
         }
         else
         {
            frontendSteps = new ArrayList<>();
         }
      }

      this.icon = processMetaData.getIcon();

      hasPermission = PermissionsHelper.hasProcessPermission(actionInput, name);
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
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Getter for primaryKeyField
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Getter for frontendSteps
    **
    *******************************************************************************/
   public List<QFrontendStepMetaData> getFrontendSteps()
   {
      return frontendSteps;
   }



   /*******************************************************************************
    ** Setter for frontendSteps
    **
    *******************************************************************************/
   public void setFrontendSteps(List<QFrontendStepMetaData> frontendSteps)
   {
      this.frontendSteps = frontendSteps;
   }



   /*******************************************************************************
    ** Getter for isHidden
    **
    *******************************************************************************/
   public boolean getIsHidden()
   {
      return isHidden;
   }



   /*******************************************************************************
    ** Getter for iconName
    **
    *******************************************************************************/
   public String getIconName()
   {
      return icon == null ? null : icon.getName();
   }



   /*******************************************************************************
    ** Getter for hasPermission
    **
    *******************************************************************************/
   public boolean getHasPermission()
   {
      return hasPermission;
   }



   /*******************************************************************************
    ** Getter for stepFlow
    **
    *******************************************************************************/
   public String getStepFlow()
   {
      return stepFlow;
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
    ** Getter for minInputRecords
    **
    *******************************************************************************/
   public Integer getMinInputRecords()
   {
      return minInputRecords;
   }



   /*******************************************************************************
    ** Getter for maxInputRecords
    **
    *******************************************************************************/
   public Integer getMaxInputRecords()
   {
      return maxInputRecords;
   }

}
