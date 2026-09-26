/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances.validation.plugins;


import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 * test implementation of a validator plugin, that fails if it is enabled, per
 * the param to its constructor.
 *******************************************************************************/
public class FailsIfEnabledProcessValidatorPlugin implements QInstanceValidatorPluginInterface<QProcessMetaData>
{
   private final boolean isEnabled;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FailsIfEnabledProcessValidatorPlugin(boolean isEnabled)
   {
      this.isEnabled = isEnabled;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return isEnabled;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validate(QProcessMetaData object, QInstance qInstance, QInstanceValidator qInstanceValidator)
   {
      qInstanceValidator.getErrors().add("I fail, because i am enabled.");
   }

}
