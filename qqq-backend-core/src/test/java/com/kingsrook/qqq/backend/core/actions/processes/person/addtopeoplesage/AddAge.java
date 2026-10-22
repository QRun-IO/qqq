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

package com.kingsrook.qqq.backend.core.actions.processes.person.addtopeoplesage;


import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class AddAge implements BackendStep
{
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
   {
      int totalYearsAdded = 0;
      Integer yearsToAdd = runBackendStepInput.getValueInteger("yearsToAdd");
      for(QRecord record : runBackendStepInput.getRecords())
      {
         Integer age = record.getValueInteger("age");
         age += yearsToAdd;
         totalYearsAdded += yearsToAdd;
         record.setValue("age", age);
      }
      runBackendStepOutput.addValue("totalYearsAdded", totalYearsAdded);
   }
}
