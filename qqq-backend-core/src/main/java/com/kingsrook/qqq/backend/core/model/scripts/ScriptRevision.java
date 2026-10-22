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
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QAssociation;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScriptRevision extends QRecordEntity
{
   public static final String TABLE_NAME = "scriptRevision";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(possibleValueSourceName = "script")
   private Integer scriptId;

   @QField(possibleValueSourceName = "apiVersion", label = "API Version")
   private String apiVersion;

   @QField(possibleValueSourceName = "apiName", label = "API Name")
   private String apiName;

   @QField()
   private Integer sequenceNo;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String commitMessage;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String author;

   @QAssociation(name = "files")
   private List<ScriptRevisionFile> files;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ScriptRevision()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ScriptRevision(QRecord qRecord) throws QException
   {
      populateFromQRecord(qRecord);
   }



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
   public ScriptRevision withId(Integer id)
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
   public ScriptRevision withCreateDate(Instant createDate)
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
   public ScriptRevision withModifyDate(Instant modifyDate)
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
   public ScriptRevision withScriptId(Integer scriptId)
   {
      this.scriptId = scriptId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sequenceNo
    **
    *******************************************************************************/
   public Integer getSequenceNo()
   {
      return sequenceNo;
   }



   /*******************************************************************************
    ** Setter for sequenceNo
    **
    *******************************************************************************/
   public void setSequenceNo(Integer sequenceNo)
   {
      this.sequenceNo = sequenceNo;
   }



   /*******************************************************************************
    ** Fluent setter for sequenceNo
    **
    *******************************************************************************/
   public ScriptRevision withSequenceNo(Integer sequenceNo)
   {
      this.sequenceNo = sequenceNo;
      return (this);
   }



   /*******************************************************************************
    ** Getter for commitMessage
    **
    *******************************************************************************/
   public String getCommitMessage()
   {
      return commitMessage;
   }



   /*******************************************************************************
    ** Setter for commitMessage
    **
    *******************************************************************************/
   public void setCommitMessage(String commitMessage)
   {
      this.commitMessage = commitMessage;
   }



   /*******************************************************************************
    ** Fluent setter for commitMessage
    **
    *******************************************************************************/
   public ScriptRevision withCommitMessage(String commitMessage)
   {
      this.commitMessage = commitMessage;
      return (this);
   }



   /*******************************************************************************
    ** Getter for author
    **
    *******************************************************************************/
   public String getAuthor()
   {
      return author;
   }



   /*******************************************************************************
    ** Setter for author
    **
    *******************************************************************************/
   public void setAuthor(String author)
   {
      this.author = author;
   }



   /*******************************************************************************
    ** Fluent setter for author
    **
    *******************************************************************************/
   public ScriptRevision withAuthor(String author)
   {
      this.author = author;
      return (this);
   }



   /*******************************************************************************
    ** Getter for apiVersion
    *******************************************************************************/
   public String getApiVersion()
   {
      return (this.apiVersion);
   }



   /*******************************************************************************
    ** Setter for apiVersion
    *******************************************************************************/
   public void setApiVersion(String apiVersion)
   {
      this.apiVersion = apiVersion;
   }



   /*******************************************************************************
    ** Fluent setter for apiVersion
    *******************************************************************************/
   public ScriptRevision withApiVersion(String apiVersion)
   {
      this.apiVersion = apiVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for apiName
    *******************************************************************************/
   public String getApiName()
   {
      return (this.apiName);
   }



   /*******************************************************************************
    ** Setter for apiName
    *******************************************************************************/
   public void setApiName(String apiName)
   {
      this.apiName = apiName;
   }



   /*******************************************************************************
    ** Fluent setter for apiName
    *******************************************************************************/
   public ScriptRevision withApiName(String apiName)
   {
      this.apiName = apiName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for files
    *******************************************************************************/
   public List<ScriptRevisionFile> getFiles()
   {
      return (this.files);
   }



   /*******************************************************************************
    ** Setter for files
    *******************************************************************************/
   public void setFiles(List<ScriptRevisionFile> files)
   {
      this.files = files;
   }



   /*******************************************************************************
    ** Fluent setter for files
    *******************************************************************************/
   public ScriptRevision withFiles(List<ScriptRevisionFile> files)
   {
      this.files = files;
      return (this);
   }

}
