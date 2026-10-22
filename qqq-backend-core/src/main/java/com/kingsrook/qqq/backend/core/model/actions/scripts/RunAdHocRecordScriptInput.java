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

package com.kingsrook.qqq.backend.core.model.actions.scripts;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.AdHocScriptCodeReference;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunAdHocRecordScriptInput extends AbstractRunScriptInput<AdHocScriptCodeReference>
{
   private List<Serializable> recordPrimaryKeyList; // can either supply recordList, or recordPrimaryKeyList
   private List<QRecord>      recordList;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunAdHocRecordScriptInput()
   {
   }



   /*******************************************************************************
    ** Getter for recordList
    *******************************************************************************/
   public List<QRecord> getRecordList()
   {
      return (this.recordList);
   }



   /*******************************************************************************
    ** Setter for recordList
    *******************************************************************************/
   public void setRecordList(List<QRecord> recordList)
   {
      this.recordList = recordList;
   }



   /*******************************************************************************
    ** Fluent setter for recordList
    *******************************************************************************/
   public RunAdHocRecordScriptInput withRecordList(List<QRecord> recordList)
   {
      this.recordList = recordList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordPrimaryKeyList
    *******************************************************************************/
   public List<Serializable> getRecordPrimaryKeyList()
   {
      return (this.recordPrimaryKeyList);
   }



   /*******************************************************************************
    ** Setter for recordPrimaryKeyList
    *******************************************************************************/
   public void setRecordPrimaryKeyList(List<Serializable> recordPrimaryKeyList)
   {
      this.recordPrimaryKeyList = recordPrimaryKeyList;
   }



   /*******************************************************************************
    ** Fluent setter for recordPrimaryKeyList
    *******************************************************************************/
   public RunAdHocRecordScriptInput withRecordPrimaryKeyList(List<Serializable> recordPrimaryKeyList)
   {
      this.recordPrimaryKeyList = recordPrimaryKeyList;
      return (this);
   }

}
