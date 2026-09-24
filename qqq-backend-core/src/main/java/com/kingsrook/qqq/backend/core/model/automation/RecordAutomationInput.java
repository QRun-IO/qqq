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
