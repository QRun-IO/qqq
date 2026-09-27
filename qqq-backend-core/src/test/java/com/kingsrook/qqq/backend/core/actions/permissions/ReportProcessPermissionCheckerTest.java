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

package com.kingsrook.qqq.backend.core.actions.permissions;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for ReportProcessPermissionChecker
 *******************************************************************************/
class ReportProcessPermissionCheckerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws Exception
   {
      QInstance       qInstance       = QContext.getQInstance();
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.addValue("reportName", TestUtils.REPORT_NAME_SHAPES_PERSON);

      qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));

      QProcessMetaData process = new QProcessMetaData()
         .withName("testProcess");
      qInstance.addProcess(process);

      new QInstanceValidator().validate(qInstance);

      ///////////////////////////////////////////////////////
      // without permission in our session, we should fail //
      ///////////////////////////////////////////////////////
      QContext.setQSession(new QSession());
      assertThrows(QPermissionDeniedException.class, () -> new ReportProcessPermissionChecker().checkPermissionsThrowing(runProcessInput, process));

      /////////////////////////////////////////////////////////////////////////
      // add the permission - assert that we have access (e.g., don't throw) //
      /////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withPermission(TestUtils.REPORT_NAME_SHAPES_PERSON + ".hasAccess"));
      new ReportProcessPermissionChecker().checkPermissionsThrowing(runProcessInput, process);
   }

}