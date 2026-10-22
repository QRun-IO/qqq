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


import com.kingsrook.qqq.backend.core.actions.scripts.AssociatedScriptContextPrimerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.code.AssociatedScriptCodeReference;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunAssociatedScriptInput extends AbstractRunScriptInput<AssociatedScriptCodeReference>
{
   private AssociatedScriptContextPrimerInterface associatedScriptContextPrimerInterface;



   /*******************************************************************************
    ** Getter for associatedScriptContextPrimerInterface
    *******************************************************************************/
   public AssociatedScriptContextPrimerInterface getAssociatedScriptContextPrimerInterface()
   {
      return (this.associatedScriptContextPrimerInterface);
   }



   /*******************************************************************************
    ** Setter for associatedScriptContextPrimerInterface
    *******************************************************************************/
   public void setAssociatedScriptContextPrimerInterface(AssociatedScriptContextPrimerInterface associatedScriptContextPrimerInterface)
   {
      this.associatedScriptContextPrimerInterface = associatedScriptContextPrimerInterface;
   }



   /*******************************************************************************
    ** Fluent setter for associatedScriptContextPrimerInterface
    *******************************************************************************/
   public RunAssociatedScriptInput withAssociatedScriptContextPrimerInterface(AssociatedScriptContextPrimerInterface associatedScriptContextPrimerInterface)
   {
      this.associatedScriptContextPrimerInterface = associatedScriptContextPrimerInterface;
      return (this);
   }

}
