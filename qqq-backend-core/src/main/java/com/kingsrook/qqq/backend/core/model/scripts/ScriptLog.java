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

package com.kingsrook.qqq.backend.core.model.scripts;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScriptLog extends QRecordEntity
{
   public static final String TABLE_NAME = "scriptLog";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(possibleValueSourceName = "script")
   private Integer scriptId;

   @QField(possibleValueSourceName = "scriptRevision")
   private Integer scriptRevisionId;

   @QField()
   private Instant startTimestamp;

   @QField()
   private Instant endTimestamp;

   @QField(displayFormat = DisplayFormat.COMMAS)
   private Integer runTimeMillis;

   @QField()
   private Boolean hadError;

   @QField()
   private String input;

   @QField()
   private String output;

   @QField()
   private String error;



   /*******************************************************************************
    ** Getter for id
    **
    *******************************************************************************/
   public Integer getId()
   {
      return id;
   }



   /*******************************************************************************
    ** Setter for id
    **
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    **
    *******************************************************************************/
   public ScriptLog withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for createDate
    **
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return createDate;
   }



   /*******************************************************************************
    ** Setter for createDate
    **
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    **
    *******************************************************************************/
   public ScriptLog withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifyDate
    **
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return modifyDate;
   }



   /*******************************************************************************
    ** Setter for modifyDate
    **
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    **
    *******************************************************************************/
   public ScriptLog withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptId
    **
    *******************************************************************************/
   public Integer getScriptId()
   {
      return scriptId;
   }



   /*******************************************************************************
    ** Setter for scriptId
    **
    *******************************************************************************/
   public void setScriptId(Integer scriptId)
   {
      this.scriptId = scriptId;
   }



   /*******************************************************************************
    ** Fluent setter for scriptId
    **
    *******************************************************************************/
   public ScriptLog withScriptId(Integer scriptId)
   {
      this.scriptId = scriptId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptRevisionId
    **
    *******************************************************************************/
   public Integer getScriptRevisionId()
   {
      return scriptRevisionId;
   }



   /*******************************************************************************
    ** Setter for scriptRevisionId
    **
    *******************************************************************************/
   public void setScriptRevisionId(Integer scriptRevisionId)
   {
      this.scriptRevisionId = scriptRevisionId;
   }



   /*******************************************************************************
    ** Fluent setter for scriptRevisionId
    **
    *******************************************************************************/
   public ScriptLog withScriptRevisionId(Integer scriptRevisionId)
   {
      this.scriptRevisionId = scriptRevisionId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startTimestamp
    **
    *******************************************************************************/
   public Instant getStartTimestamp()
   {
      return startTimestamp;
   }



   /*******************************************************************************
    ** Setter for startTimestamp
    **
    *******************************************************************************/
   public void setStartTimestamp(Instant startTimestamp)
   {
      this.startTimestamp = startTimestamp;
   }



   /*******************************************************************************
    ** Fluent setter for startTimestamp
    **
    *******************************************************************************/
   public ScriptLog withStartTimestamp(Instant startTimestamp)
   {
      this.startTimestamp = startTimestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for endTimestamp
    **
    *******************************************************************************/
   public Instant getEndTimestamp()
   {
      return endTimestamp;
   }



   /*******************************************************************************
    ** Setter for endTimestamp
    **
    *******************************************************************************/
   public void setEndTimestamp(Instant endTimestamp)
   {
      this.endTimestamp = endTimestamp;
   }



   /*******************************************************************************
    ** Fluent setter for endTimestamp
    **
    *******************************************************************************/
   public ScriptLog withEndTimestamp(Instant endTimestamp)
   {
      this.endTimestamp = endTimestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for runTimeMillis
    **
    *******************************************************************************/
   public Integer getRunTimeMillis()
   {
      return runTimeMillis;
   }



   /*******************************************************************************
    ** Setter for runTimeMillis
    **
    *******************************************************************************/
   public void setRunTimeMillis(Integer runTimeMillis)
   {
      this.runTimeMillis = runTimeMillis;
   }



   /*******************************************************************************
    ** Fluent setter for runTimeMillis
    **
    *******************************************************************************/
   public ScriptLog withRunTimeMillis(Integer runTimeMillis)
   {
      this.runTimeMillis = runTimeMillis;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hadError
    **
    *******************************************************************************/
   public Boolean getHadError()
   {
      return hadError;
   }



   /*******************************************************************************
    ** Setter for hadError
    **
    *******************************************************************************/
   public void setHadError(Boolean hadError)
   {
      this.hadError = hadError;
   }



   /*******************************************************************************
    ** Fluent setter for hadError
    **
    *******************************************************************************/
   public ScriptLog withHadError(Boolean hadError)
   {
      this.hadError = hadError;
      return (this);
   }



   /*******************************************************************************
    ** Getter for input
    **
    *******************************************************************************/
   public String getInput()
   {
      return input;
   }



   /*******************************************************************************
    ** Setter for input
    **
    *******************************************************************************/
   public void setInput(String input)
   {
      this.input = input;
   }



   /*******************************************************************************
    ** Fluent setter for input
    **
    *******************************************************************************/
   public ScriptLog withInput(String input)
   {
      this.input = input;
      return (this);
   }



   /*******************************************************************************
    ** Getter for output
    **
    *******************************************************************************/
   public String getOutput()
   {
      return output;
   }



   /*******************************************************************************
    ** Setter for output
    **
    *******************************************************************************/
   public void setOutput(String output)
   {
      this.output = output;
   }



   /*******************************************************************************
    ** Fluent setter for output
    **
    *******************************************************************************/
   public ScriptLog withOutput(String output)
   {
      this.output = output;
      return (this);
   }



   /*******************************************************************************
    ** Getter for error
    **
    *******************************************************************************/
   public String getError()
   {
      return error;
   }



   /*******************************************************************************
    ** Setter for error
    **
    *******************************************************************************/
   public void setError(String error)
   {
      this.error = error;
   }



   /*******************************************************************************
    ** Fluent setter for error
    **
    *******************************************************************************/
   public ScriptLog withError(String error)
   {
      this.error = error;
      return (this);
   }

}
