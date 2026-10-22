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
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;


/*******************************************************************************
 **
 *******************************************************************************/
public class StoreAssociatedScriptInput extends AbstractTableActionInput
{
   private String       fieldName;
   private Serializable recordPrimaryKey;

   private String code;
   private String apiName;
   private String apiVersion;
   private String commitMessage;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public StoreAssociatedScriptInput()
   {
   }



   /*******************************************************************************
    ** Getter for fieldName
    **
    *******************************************************************************/
   public String getFieldName()
   {
      return fieldName;
   }



   /*******************************************************************************
    ** Setter for fieldName
    **
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    **
    *******************************************************************************/
   public StoreAssociatedScriptInput withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordPrimaryKey
    **
    *******************************************************************************/
   public Serializable getRecordPrimaryKey()
   {
      return recordPrimaryKey;
   }



   /*******************************************************************************
    ** Setter for recordPrimaryKey
    **
    *******************************************************************************/
   public void setRecordPrimaryKey(Serializable recordPrimaryKey)
   {
      this.recordPrimaryKey = recordPrimaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for recordPrimaryKey
    **
    *******************************************************************************/
   public StoreAssociatedScriptInput withRecordPrimaryKey(Serializable recordPrimaryKey)
   {
      this.recordPrimaryKey = recordPrimaryKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for code
    **
    *******************************************************************************/
   public String getCode()
   {
      return code;
   }



   /*******************************************************************************
    ** Setter for code
    **
    *******************************************************************************/
   public void setCode(String code)
   {
      this.code = code;
   }



   /*******************************************************************************
    ** Fluent setter for code
    **
    *******************************************************************************/
   public StoreAssociatedScriptInput withCode(String code)
   {
      this.code = code;
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
   public StoreAssociatedScriptInput withCommitMessage(String commitMessage)
   {
      this.commitMessage = commitMessage;
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
   public StoreAssociatedScriptInput withApiName(String apiName)
   {
      this.apiName = apiName;
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
   public StoreAssociatedScriptInput withApiVersion(String apiVersion)
   {
      this.apiVersion = apiVersion;
      return (this);
   }

}
