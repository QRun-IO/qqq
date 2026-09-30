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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.AbstractWidgetOutput;


/*******************************************************************************
 **
 *******************************************************************************/
public class NoCodeWidgetFrontendComponentMetaData extends QFrontendComponentMetaData
{
   private List<AbstractWidgetOutput> outputs = new ArrayList<>();



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public NoCodeWidgetFrontendComponentMetaData()
   {
      setType(QComponentType.HTML);
   }



   /*******************************************************************************
    ** Fluent setter to add a single output
    *******************************************************************************/
   public NoCodeWidgetFrontendComponentMetaData withOutput(AbstractWidgetOutput output)
   {
      if(this.outputs == null)
      {
         this.outputs = new ArrayList<>();
      }
      this.outputs.add(output);
      return (this);
   }



   /*******************************************************************************
    ** Getter for outputs
    *******************************************************************************/
   public List<AbstractWidgetOutput> getOutputs()
   {
      return (this.outputs);
   }



   /*******************************************************************************
    ** Setter for outputs
    *******************************************************************************/
   public void setOutputs(List<AbstractWidgetOutput> outputs)
   {
      this.outputs = outputs;
   }



   /*******************************************************************************
    ** Fluent setter for outputs
    *******************************************************************************/
   public NoCodeWidgetFrontendComponentMetaData withOutputs(List<AbstractWidgetOutput> outputs)
   {
      this.outputs = outputs;
      return (this);
   }

}
