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


import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 **
 *******************************************************************************/
public class StoreAssociatedScriptOutput extends AbstractActionOutput
{
   private Integer scriptId;
   private String  scriptName;
   private Integer scriptRevisionId;
   private Integer scriptRevisionSequenceNo;



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
    ** Getter for scriptName
    **
    *******************************************************************************/
   public String getScriptName()
   {
      return scriptName;
   }



   /*******************************************************************************
    ** Setter for scriptName
    **
    *******************************************************************************/
   public void setScriptName(String scriptName)
   {
      this.scriptName = scriptName;
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
    ** Getter for scriptRevisionSequenceNo
    **
    *******************************************************************************/
   public Integer getScriptRevisionSequenceNo()
   {
      return scriptRevisionSequenceNo;
   }



   /*******************************************************************************
    ** Setter for scriptRevisionSequenceNo
    **
    *******************************************************************************/
   public void setScriptRevisionSequenceNo(Integer scriptRevisionSequenceNo)
   {
      this.scriptRevisionSequenceNo = scriptRevisionSequenceNo;
   }

}
