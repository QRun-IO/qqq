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

package com.kingsrook.qqq.backend.core.model.actions.tables.replace;


import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;


/*******************************************************************************
 **
 *******************************************************************************/
public class ReplaceOutput extends AbstractActionOutput
{
   private InsertOutput insertOutput;
   private UpdateOutput updateOutput;
   private DeleteOutput deleteOutput;



   /*******************************************************************************
    ** Getter for insertOutput
    *******************************************************************************/
   public InsertOutput getInsertOutput()
   {
      return (this.insertOutput);
   }



   /*******************************************************************************
    ** Setter for insertOutput
    *******************************************************************************/
   public void setInsertOutput(InsertOutput insertOutput)
   {
      this.insertOutput = insertOutput;
   }



   /*******************************************************************************
    ** Fluent setter for insertOutput
    *******************************************************************************/
   public ReplaceOutput withInsertOutput(InsertOutput insertOutput)
   {
      this.insertOutput = insertOutput;
      return (this);
   }



   /*******************************************************************************
    ** Getter for updateOutput
    *******************************************************************************/
   public UpdateOutput getUpdateOutput()
   {
      return (this.updateOutput);
   }



   /*******************************************************************************
    ** Setter for updateOutput
    *******************************************************************************/
   public void setUpdateOutput(UpdateOutput updateOutput)
   {
      this.updateOutput = updateOutput;
   }



   /*******************************************************************************
    ** Fluent setter for updateOutput
    *******************************************************************************/
   public ReplaceOutput withUpdateOutput(UpdateOutput updateOutput)
   {
      this.updateOutput = updateOutput;
      return (this);
   }



   /*******************************************************************************
    ** Getter for deleteOutput
    *******************************************************************************/
   public DeleteOutput getDeleteOutput()
   {
      return (this.deleteOutput);
   }



   /*******************************************************************************
    ** Setter for deleteOutput
    *******************************************************************************/
   public void setDeleteOutput(DeleteOutput deleteOutput)
   {
      this.deleteOutput = deleteOutput;
   }



   /*******************************************************************************
    ** Fluent setter for deleteOutput
    *******************************************************************************/
   public ReplaceOutput withDeleteOutput(DeleteOutput deleteOutput)
   {
      this.deleteOutput = deleteOutput;
      return (this);
   }

}
