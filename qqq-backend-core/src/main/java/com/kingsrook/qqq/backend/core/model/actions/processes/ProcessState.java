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

package com.kingsrook.qqq.backend.core.model.actions.processes;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessStateAccess;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessState implements Serializable
{
   private transient ProcessStateAccess stateAccess;

   private List<QRecord>             records      = new ArrayList<>();
   private Map<String, Serializable> values       = new HashMap<>();
   private List<String>              stepList     = new ArrayList<>();
   private Optional<String>          nextStepName = Optional.empty();
   private Optional<String>          backStepName = Optional.empty();
   private boolean                   isStepBack   = false;

   private ProcessMetaDataAdjustment processMetaDataAdjustment = null;



   /*******************************************************************************
    ** Getter for records
    **
    *******************************************************************************/
   public List<QRecord> getRecords()
   {
      return records;
   }



   /*******************************************************************************
    ** Setter for records
    **
    *******************************************************************************/
   public void setRecords(List<QRecord> records)
   {
      this.records = records;
   }



   /*******************************************************************************
    ** Getter for values
    **
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return values;
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Getter for nextStepName
    **
    *******************************************************************************/
   public Optional<String> getNextStepName()
   {
      return nextStepName;
   }



   /*******************************************************************************
    ** Setter for nextStepName
    **
    *******************************************************************************/
   public void setNextStepName(String nextStepName)
   {
      this.nextStepName = Optional.of(nextStepName);
   }



   /*******************************************************************************
    ** clear out the value of nextStepName (set the Optional to empty)
    **
    *******************************************************************************/
   public void clearNextStepName()
   {
      this.nextStepName = Optional.empty();
   }



   /*******************************************************************************
    ** Getter for backStepName
    **
    *******************************************************************************/
   public Optional<String> getBackStepName()
   {
      return backStepName;
   }



   /*******************************************************************************
    ** Setter for backStepName
    **
    *******************************************************************************/
   public void setBackStepName(String backStepName)
   {
      this.backStepName = Optional.of(backStepName);
   }



   /*******************************************************************************
    ** clear out the value of backStepName (set the Optional to empty)
    **
    *******************************************************************************/
   public void clearBackStepName()
   {
      this.backStepName = Optional.empty();
   }



   /*******************************************************************************
    ** Getter for stepList
    **
    *******************************************************************************/
   public List<String> getStepList()
   {
      return stepList;
   }



   /*******************************************************************************
    ** Setter for stepList
    **
    *******************************************************************************/
   public void setStepList(List<String> stepList)
   {
      this.stepList = stepList;
   }





   /*******************************************************************************
    ** Getter for processMetaDataAdjustment
    *******************************************************************************/
   public ProcessMetaDataAdjustment getProcessMetaDataAdjustment()
   {
      return (this.processMetaDataAdjustment);
   }



   /*******************************************************************************
    ** Setter for processMetaDataAdjustment
    *******************************************************************************/
   public void setProcessMetaDataAdjustment(ProcessMetaDataAdjustment processMetaDataAdjustment)
   {
      this.processMetaDataAdjustment = processMetaDataAdjustment;
   }



   /*******************************************************************************
    ** Fluent setter for processMetaDataAdjustment
    *******************************************************************************/
   public ProcessState withProcessMetaDataAdjustment(ProcessMetaDataAdjustment processMetaDataAdjustment)
   {
      this.processMetaDataAdjustment = processMetaDataAdjustment;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isStepBack
    *******************************************************************************/
   public boolean getIsStepBack()
   {
      return (this.isStepBack);
   }



   /*******************************************************************************
    ** Setter for isStepBack
    *******************************************************************************/
   public void setIsStepBack(boolean isStepBack)
   {
      this.isStepBack = isStepBack;
   }



   /*******************************************************************************
    ** Fluent setter for isStepBack
    *******************************************************************************/
   public ProcessState withIsStepBack(boolean isStepBack)
   {
      this.isStepBack = isStepBack;
      return (this);
   }


   /*******************************************************************************
    ** Server-owned authorization, never part of an HTTP state response.
    *******************************************************************************/
   @JsonIgnore
   public ProcessStateAccess getStateAccess()
   {
      return stateAccess;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setStateAccess(ProcessStateAccess stateAccess)
   {
      this.stateAccess = stateAccess;
   }


}
