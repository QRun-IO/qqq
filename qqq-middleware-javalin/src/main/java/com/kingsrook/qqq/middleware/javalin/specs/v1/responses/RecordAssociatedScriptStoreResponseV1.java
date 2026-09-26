/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
