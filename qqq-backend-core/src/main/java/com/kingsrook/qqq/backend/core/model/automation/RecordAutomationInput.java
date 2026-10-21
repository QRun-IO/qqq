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

package com.kingsrook.qqq.backend.core.model.automation;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;


/*******************************************************************************
 ** Input data for the RecordAutomationHandlerInterface.
 *******************************************************************************/
public class RecordAutomationInput extends AbstractTableActionInput
{
   private TableAutomationAction action;
   private List<QRecord>         recordList;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordAutomationInput()
   {
   }



   /*******************************************************************************
    ** Getter for action
    **
    *******************************************************************************/
   public TableAutomationAction getAction()
   {
      return action;
   }



   /*******************************************************************************
    ** Setter for action
    **
    *******************************************************************************/
   public void setAction(TableAutomationAction action)
   {
      this.action = action;
   }



   /*******************************************************************************
    ** Fluent setter for action
    **
    *******************************************************************************/
   public RecordAutomationInput withAction(TableAutomationAction action)
   {
      this.action = action;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordList
    **
    *******************************************************************************/
   public List<QRecord> getRecordList()
   {
      return recordList;
   }



   /*******************************************************************************
    ** Setter for recordList
    **
    *******************************************************************************/
   public void setRecordList(List<QRecord> recordList)
   {
      this.recordList = recordList;
   }



   /*******************************************************************************
    ** Fluent setter for recordList
    **
    *******************************************************************************/
   public RecordAutomationInput withRecordList(List<QRecord> recordList)
   {
      this.recordList = recordList;
      return (this);
   }

}
