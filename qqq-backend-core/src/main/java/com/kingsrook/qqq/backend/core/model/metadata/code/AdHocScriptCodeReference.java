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

package com.kingsrook.qqq.backend.core.model.metadata.code;


import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class AdHocScriptCodeReference extends QCodeReference
{
   ////////////////////////////////////////////////////////////////////////////////
   // can supply scriptId (in which case, current revisionId will be looked up), //
   // or revisionId (in which case, record will be looked up)                    //
   // or, the record.                                                            //
   ////////////////////////////////////////////////////////////////////////////////
   private Integer scriptId;
   private Integer scriptRevisionId;
   private QRecord scriptRevisionRecord;



   /*******************************************************************************
    ** Getter for scriptId
    *******************************************************************************/
   public Integer getScriptId()
   {
      return (this.scriptId);
   }



   /*******************************************************************************
    ** Setter for scriptId
    *******************************************************************************/
   public void setScriptId(Integer scriptId)
   {
      this.scriptId = scriptId;
   }



   /*******************************************************************************
    ** Fluent setter for scriptId
    *******************************************************************************/
   public AdHocScriptCodeReference withScriptId(Integer scriptId)
   {
      this.scriptId = scriptId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptRevisionId
    *******************************************************************************/
   public Integer getScriptRevisionId()
   {
      return (this.scriptRevisionId);
   }



   /*******************************************************************************
    ** Setter for scriptRevisionId
    *******************************************************************************/
   public void setScriptRevisionId(Integer scriptRevisionId)
   {
      this.scriptRevisionId = scriptRevisionId;
   }



   /*******************************************************************************
    ** Fluent setter for scriptRevisionId
    *******************************************************************************/
   public AdHocScriptCodeReference withScriptRevisionId(Integer scriptRevisionId)
   {
      this.scriptRevisionId = scriptRevisionId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptRevisionRecord
    *******************************************************************************/
   public QRecord getScriptRevisionRecord()
   {
      return (this.scriptRevisionRecord);
   }



   /*******************************************************************************
    ** Setter for scriptRevisionRecord
    *******************************************************************************/
   public void setScriptRevisionRecord(QRecord scriptRevisionRecord)
   {
      this.scriptRevisionRecord = scriptRevisionRecord;
   }



   /*******************************************************************************
    ** Fluent setter for scriptRevisionRecord
    *******************************************************************************/
   public AdHocScriptCodeReference withScriptRevisionRecord(QRecord scriptRevisionRecord)
   {
      this.scriptRevisionRecord = scriptRevisionRecord;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "AdHocScriptCodeReference{scriptId=" + scriptId + ", scriptRevisionId=" + scriptRevisionId + ", scriptRevisionRecord=" + scriptRevisionRecord + '}';
   }
}
