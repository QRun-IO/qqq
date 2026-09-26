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


import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.BasepullExtractStepInterface;


/*******************************************************************************
 ** instance validator plugin, to ensure that a process which is a basepull uses
 ** an extract step marked for basepulls.
 *******************************************************************************/
public class BasepullExtractStepValidator implements QInstanceValidatorPluginInterface<QProcessMetaData>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validate(QProcessMetaData process, QInstance qInstance, QInstanceValidator qInstanceValidator)
   {
      ///////////////////////////////////////////////////////////////////////////
      // if there's no basepull config on the process, don't do any validation //
      ///////////////////////////////////////////////////////////////////////////
      if(process.getBasepullConfiguration() == null)
      {
         return;
      }

      //////////////////////////////////////////////////////////////////////////////////////////
      // try to find an input field in the process, w/ a defaultValue that's a QCodeReference //
      // and is an instance of BasepullExtractStepInterface                                   //
      //////////////////////////////////////////////////////////////////////////////////////////
      boolean foundBasepullExtractStep = false;
      for(QFieldMetaData field : process.getInputFields())
      {
         if(field.getDefaultValue() != null && field.getDefaultValue() instanceof QCodeReference codeReference)
         {
            try
            {
               BasepullExtractStepInterface extractStep = QCodeLoader.getAdHoc(BasepullExtractStepInterface.class, codeReference);
               if(extractStep != null)
               {
                  foundBasepullExtractStep = true;
               }
            }
            catch(Exception e)
            {
               //////////////////////////////////////////////////////
               // ok, just means we haven't found our extract step //
               //////////////////////////////////////////////////////
            }
         }
      }

      ///////////////////////////////////////////////////////////
      // validate we could find a BasepullExtractStepInterface //
      ///////////////////////////////////////////////////////////
      qInstanceValidator.assertCondition(foundBasepullExtractStep, "Process [" + process.getName() + "] has a basepullConfiguration, but does not have a field with a default value that is a BasepullExtractStepInterface CodeReference");
   }

}
