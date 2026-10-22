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

package com.kingsrook.qqq.backend.core.actions.metadata;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataOutput;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for ProcessMetaDataAction
 **
 *******************************************************************************/
class ProcessMetaDataActionTest extends BaseTest
{

   /*******************************************************************************
    ** Test basic success case.
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      ProcessMetaDataInput request = new ProcessMetaDataInput();
      request.setProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE);
      ProcessMetaDataOutput result = new ProcessMetaDataAction().execute(request);
      assertNotNull(result);
      assertNotNull(result.getProcess());
      assertEquals("greetInteractive", result.getProcess().getName());
      assertEquals("Greet Interactive", result.getProcess().getLabel());
      assertEquals(2, result.getProcess().getFrontendSteps().size());
   }



   /*******************************************************************************
    ** Test exception is thrown for the "not-found" case.
    **
    *******************************************************************************/
   @Test
   public void test_notFound()
   {
      assertThrows(QNotFoundException.class, () ->
      {
         ProcessMetaDataInput request = new ProcessMetaDataInput();
         request.setProcessName("willNotBeFound");
         new ProcessMetaDataAction().execute(request);
      });
   }

}
