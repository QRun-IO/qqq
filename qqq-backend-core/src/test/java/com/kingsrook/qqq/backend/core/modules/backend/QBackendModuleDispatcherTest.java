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

package com.kingsrook.qqq.backend.core.modules.backend;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for QModuleDispatcher
 **
 *******************************************************************************/
class QBackendModuleDispatcherTest extends BaseTest
{

   /*******************************************************************************
    ** Test success case - a valid backend.
    **
    *******************************************************************************/
   @Test
   public void test_getQModule_valid() throws QModuleDispatchException
   {
      QBackendModuleInterface qModule = new QBackendModuleDispatcher().getQBackendModule(TestUtils.defineBackend());
      assertNotNull(qModule);
   }



   /*******************************************************************************
    ** Test failure case, a backend name that doesn't exist
    **
    *******************************************************************************/
   @Test
   public void test_getQModule_typeNotFound()
   {
      assertThrows(QModuleDispatchException.class, () ->
      {
         QBackendMetaData qBackendMetaData = TestUtils.defineBackend();
         qBackendMetaData.setBackendType("aTypeThatWontEverExist");
         new QBackendModuleDispatcher().getQBackendModule(qBackendMetaData);
      });
   }

}
