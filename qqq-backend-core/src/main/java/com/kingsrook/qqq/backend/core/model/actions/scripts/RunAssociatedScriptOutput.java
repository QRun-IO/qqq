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
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunAssociatedScriptOutput extends AbstractActionOutput
{
   private Serializable output;
   private Integer      scriptRevisionId;



   /*******************************************************************************
    ** Getter for output
    **
    *******************************************************************************/
   public Serializable getOutput()
   {
      return output;
   }



   /*******************************************************************************
    ** Setter for output
    **
    *******************************************************************************/
   public void setOutput(Serializable output)
   {
      this.output = output;
   }



   /*******************************************************************************
    ** Fluent setter for output
    **
    *******************************************************************************/
   public RunAssociatedScriptOutput withOutput(Serializable output)
   {
      this.output = output;
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
   public RunAssociatedScriptOutput withScriptRevisionId(Integer scriptRevisionId)
   {
      this.scriptRevisionId = scriptRevisionId;
      return (this);
   }


}
