/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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
// SPDX-License-Identifier: Apache-2.0

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.ObjectStreamClass;
import java.util.Base64;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.state.StateSerialization;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Server-side application state must use the real authorized process instance.
 *******************************************************************************/
class ProcessStateAccessTest extends BaseTest
{
   /*******************************************************************************
    ** A forged ordinary process value cannot redirect backend application state.
    ******************************************************************************/
   @Test
   void backendReadsActualProcessIdentity() throws Exception
   {
      QContext.getQInstance().addProcess(new QProcessMetaData().withName("identity")
         .withStep(new QBackendStepMetaData().withName("capture").withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
            output.addValue("boundProcess", output.getProcessState().getStateAccess().requireCurrentProcessUUID("identity")))))
         .withStep(new QFrontendStepMetaData().withName("wait")));
      RunProcessOutput result = new RunProcessAction().execute(new RunProcessInput().withProcessName("identity")
         .withInputSource(QInputSource.USER).withValue("processUUID", "caller-chosen"));
      assertEquals(result.getProcessUUID(), result.getValues().get("boundProcess"));
      assertNotEquals("caller-chosen", result.getValues().get("boundProcess"));
   }



   /*******************************************************************************
    ** Reading a captured identifier still rechecks its current session owner.
    ******************************************************************************/
   @Test
   void otherSessionCannotReadBoundIdentity() throws Exception
   {
      QContext.getQInstance().addProcess(new QProcessMetaData().withName("identity"));
      ProcessStateAccess access = ProcessStateAccess.capture("identity", "server-process");
      assertEquals("server-process", access.requireCurrentProcessUUID("identity"));
      QContext.setQSession(newSession("other-operator"));
      assertThrows(QPermissionDeniedException.class, () -> access.requireCurrentProcessUUID("identity"));
   }



   /*******************************************************************************
    ** A host action cannot bind its state to another process's captured identity.
    ******************************************************************************/
   @Test
   void wrongProcessCannotReadBoundIdentity() throws Exception
   {
      QContext.getQInstance().addProcess(new QProcessMetaData().withName("identity"));
      ProcessStateAccess access = ProcessStateAccess.capture("identity", "server-process");
      assertThrows(QPermissionDeniedException.class, () -> access.requireCurrentProcessUUID("another-process"));
   }



   /*******************************************************************************
    ** Resume synthetic state produced by the qualified predecessor class.
    ** The fixture contains only an ownership digest, not a session credential.
    ******************************************************************************/
   @Test
   void predecessorSerializedStateRetainsItsIdentity() throws Exception
   {
      assertEquals(348552618397485945L, ObjectStreamClass.lookup(ProcessStateAccess.class).getSerialVersionUID());
      String fixture = "H4sIAAAAAAAA/01PTU7CUBAeARMTFwRdGo/go/SHtrrR4EIiEhOCruf9gJXS1743alnoETyHia49gIfwFt7BVli4mXz5Zr6f+fiBbWtgIPSSLZJsbo3WC1YUBeMoFiqTTGijGApKdGZZbrRQ1irLrtdoQkjqTNSw9T3+ej68XDWgNYL2Rn6DJsGMLEF7dI+P2H2gJO1eYX4ygt2N2xiXimBvvU8xm3cnZKou/06m0+F5AS/QqKgq3VZlLtDe/VFlXj3QqcWsNmcDnaYTZW5f3z+PD946TdgaQpNwXuYA0HxqVRNKgp1EqowSWhHs50ZJVedoc7RJJDiNezjzncjr9zjynh+qWPh9N+Rh4HmR5C7GMyGcUAXCc0PpB7HXj3jgRNJxOQ8k/wV0W3T7XAEAAA==";
      QContext.getQInstance().addProcess(new QProcessMetaData().withName("identity"));
      QContext.getQSession().setIdReference("serialized-fixture-owner");
      ProcessStateAccess access = (ProcessStateAccess) new StateSerialization(null).fromBytes(Base64.getDecoder().decode(fixture));
      assertEquals("predecessor-process", access.requireCurrentProcessUUID("identity"));
   }
}
