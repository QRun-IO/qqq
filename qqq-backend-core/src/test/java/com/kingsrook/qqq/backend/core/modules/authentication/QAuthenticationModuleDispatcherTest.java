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

package com.kingsrook.qqq.backend.core.modules.authentication;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QModuleDispatcher
 **
 *******************************************************************************/
class QAuthenticationModuleDispatcherTest extends BaseTest
{

   /*******************************************************************************
    ** Test success case - a valid backend.
    **
    *******************************************************************************/
   @Test
   public void test_getQAuthenticationModule() throws QModuleDispatchException
   {
      QAuthenticationModuleInterface mock = new QAuthenticationModuleDispatcher().getQModule(TestUtils.defineAuthentication());
      assertNotNull(mock);
   }



   /*******************************************************************************
    ** Test success case - a valid backend.
    **
    *******************************************************************************/
   @Test
   public void test_getQAuthenticationModuleByType_valid() throws QModuleDispatchException
   {
      QAuthenticationModuleInterface mock = new QAuthenticationModuleDispatcher().getQModule("mock");
      assertNotNull(mock);
   }



   /*******************************************************************************
    ** Test failure case, a backend name that doesn't exist
    **
    *******************************************************************************/
   @Test
   public void test_getQAuthenticationModule_typeNotFound()
   {
      assertThrows(QModuleDispatchException.class, () ->
      {
         new QAuthenticationModuleDispatcher().getQModule("aTypeThatWontExist");
      });
   }



   /*******************************************************************************
    ** Test failure case, null argument
    **
    *******************************************************************************/
   @Test
   public void test_getQAuthenticationModule_nullArgument()
   {
      assertThrows(QModuleDispatchException.class, () ->
      {
         new QAuthenticationModuleDispatcher().getQModule((QAuthenticationMetaData) null);
      });
   }




   /*******************************************************************************
    ** A provider naming its own module class gets that module (whatever the
    ** type); CUSTOM providers must name one; a missing type is reported.
    *******************************************************************************/
   @Test
   public void test_moduleCodeReference() throws QModuleDispatchException
   {
      QAuthenticationMetaData custom = new QAuthenticationMetaData().withName("custom").withType(QAuthenticationType.CUSTOM)
         .withModuleCodeReference(new QCodeReference(MockAuthenticationModule.class));
      assertInstanceOf(MockAuthenticationModule.class, new QAuthenticationModuleDispatcher().getQModule(custom));

      QAuthenticationMetaData overridden = new QAuthenticationMetaData().withName("anon").withType(QAuthenticationType.FULLY_ANONYMOUS)
         .withModuleCodeReference(new QCodeReference(MockAuthenticationModule.class));
      assertInstanceOf(MockAuthenticationModule.class, new QAuthenticationModuleDispatcher().getQModule(overridden));

      assertThrows(QModuleDispatchException.class, () -> new QAuthenticationModuleDispatcher().getQModule(new QAuthenticationMetaData().withName("none")));

      QInstance qInstance = QContext.getQInstance();
      QInstanceValidator validator = new QInstanceValidator();
      new QAuthenticationMetaData().withName("bad").withType(QAuthenticationType.CUSTOM).validate(qInstance, validator);
      assertTrue(validator.getErrors().stream().anyMatch(e -> e.contains("must set a moduleCodeReference")));
   }



   /*******************************************************************************
    ** The metadata-aware createSession defaults to the plain one.
    *******************************************************************************/
   @Test
   public void test_createSessionWithMetaData() throws Exception
   {
      QAuthenticationModuleInterface module = new MockAuthenticationModule();
      assertNotNull(module.createSession(QContext.getQInstance(), TestUtils.defineAuthentication(), java.util.Map.of()));
   }
}
