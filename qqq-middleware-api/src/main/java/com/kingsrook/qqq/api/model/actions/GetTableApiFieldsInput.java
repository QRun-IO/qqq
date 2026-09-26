/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.actions;


import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;


/*******************************************************************************
 **
 *******************************************************************************/
public class GetTableApiFieldsInput extends AbstractTableActionInput
{
   private String apiName;
   private String version;

   /////////////////////////////////////////////////////////////////////////////////////
   // by default, this action will throw if the input table isn't in the api version. //
   // but, to preserve legacy behavior where that didn't happen, allow this input.    //
   /////////////////////////////////////////////////////////////////////////////////////
   private Boolean doCheckTableApiVersion = true;



   /*******************************************************************************
    ** Getter for version
    *******************************************************************************/
   public String getVersion()
   {
      return (this.version);
   }



   /*******************************************************************************
    ** Setter for version
    *******************************************************************************/
   public void setVersion(String version)
   {
      this.version = version;
   }



   /*******************************************************************************
    ** Fluent setter for version
    *******************************************************************************/
   public GetTableApiFieldsInput withVersion(String version)
   {
      this.version = version;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public GetTableApiFieldsInput withTableName(String tableName)
   {
      super.withTableName(tableName);
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
   public GetTableApiFieldsInput withApiName(String apiName)
   {
      this.apiName = apiName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for doCheckTableApiVersion
    *******************************************************************************/
   public Boolean getDoCheckTableApiVersion()
   {
      return (this.doCheckTableApiVersion);
   }



   /*******************************************************************************
    ** Setter for doCheckTableApiVersion
    *******************************************************************************/
   public void setDoCheckTableApiVersion(Boolean doCheckTableApiVersion)
   {
      this.doCheckTableApiVersion = doCheckTableApiVersion;
   }



   /*******************************************************************************
    ** Fluent setter for doCheckTableApiVersion
    *******************************************************************************/
   public GetTableApiFieldsInput withDoCheckTableApiVersion(Boolean doCheckTableApiVersion)
   {
      this.doCheckTableApiVersion = doCheckTableApiVersion;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public GetTableApiFieldsInput withInputSource(InputSource inputSource)
   {
      super.withInputSource(inputSource);
      return (this);
   }
}
