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

package com.kingsrook.sampleapp;

import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*******************************************************************************
 ** The sample menu path must collect the duration consumed by its backend step.
 *******************************************************************************/
class SampleSleepInteractiveTest
{
   /***************************************************************************
    ** The normal form can submit a valid duration without URL-injected values.
    ***************************************************************************/
   @Test
   void testInteractiveDurationInput() throws Exception
   {
      QFrontendStepMetaData step = SampleMetaDataProvider.defineTestInstance()
         .getProcess(SampleMetaDataProvider.PROCESS_NAME_SLEEP_INTERACTIVE).getFrontendStep("screen0");
      assertTrue(step.getComponents().stream().anyMatch(component -> QComponentType.EDIT_FORM.equals(component.getType())));
      QFieldMetaData duration = step.getFormFields().stream().filter(field -> field.getName().equals("sleepMillis")).findFirst().orElseThrow();
      assertEquals(QFieldType.INTEGER, duration.getType());
      assertEquals(1000, duration.getDefaultValue());
      assertTrue(duration.getIsRequired());
   }

   /***************************************************************************
    ** Invalid direct inputs must produce useful errors, not null unboxing.
    ***************************************************************************/
   @Test
   void testMissingAndNegativeDuration()
   {
      SampleMetaDataProvider.SleeperStep step = new SampleMetaDataProvider.SleeperStep();
      RunBackendStepInput input = new RunBackendStepInput();
      assertThrows(QUserFacingException.class, () -> step.run(input, new RunBackendStepOutput()));
      input.addValue("sleepMillis", -1);
      assertThrows(QUserFacingException.class, () -> step.run(input, new RunBackendStepOutput()));
      input.addValue("sleepMillis", 0);
      assertDoesNotThrow(() -> step.run(input, new RunBackendStepOutput()));
   }
}
