/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptOutput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordAssociatedScriptStoreOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** Response for the route that stores a new revision of a record's associated
 ** script.
 *******************************************************************************/
public class RecordAssociatedScriptStoreResponseV1 implements RecordAssociatedScriptStoreOutputInterface, ToSchema
{
   @OpenAPIDescription("Id of the script (which is inserted, and referenced from the record, if the record did not yet reference one).")
   private Integer scriptId;

   @OpenAPIDescription("Name of the script.")
   private String scriptName;

   @OpenAPIDescription("Id of the new script revision.")
   private Integer scriptRevisionId;

   @OpenAPIDescription("Sequence number of the new revision within the script (1 for its first revision).")
   private Integer scriptRevisionSequenceNo;



   /*******************************************************************************
    ** Setter for the result of storing the script revision
    *******************************************************************************/
   @Override
   public void setStoreAssociatedScriptOutput(StoreAssociatedScriptOutput storeAssociatedScriptOutput)
   {
      this.scriptId = storeAssociatedScriptOutput.getScriptId();
      this.scriptName = storeAssociatedScriptOutput.getScriptName();
      this.scriptRevisionId = storeAssociatedScriptOutput.getScriptRevisionId();
      this.scriptRevisionSequenceNo = storeAssociatedScriptOutput.getScriptRevisionSequenceNo();
   }



   /*******************************************************************************
    ** Fluent setter for the result of storing the script revision
    *******************************************************************************/
   public RecordAssociatedScriptStoreResponseV1 withStoreAssociatedScriptOutput(StoreAssociatedScriptOutput storeAssociatedScriptOutput)
   {
      setStoreAssociatedScriptOutput(storeAssociatedScriptOutput);
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptId
    *******************************************************************************/
   public Integer getScriptId()
   {
      return (this.scriptId);
   }



   /*******************************************************************************
    ** Getter for scriptName
    *******************************************************************************/
   public String getScriptName()
   {
      return (this.scriptName);
   }



   /*******************************************************************************
    ** Getter for scriptRevisionId
    *******************************************************************************/
   public Integer getScriptRevisionId()
   {
      return (this.scriptRevisionId);
   }



   /*******************************************************************************
    ** Getter for scriptRevisionSequenceNo
    *******************************************************************************/
   public Integer getScriptRevisionSequenceNo()
   {
      return (this.scriptRevisionSequenceNo);
   }

}
