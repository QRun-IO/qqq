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

package com.kingsrook.qqq.backend.core.context;


import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QContext
 *******************************************************************************/
class QContextTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      QContext.clear();

      QInstance qInstance    = newQInstance();
      String    instanceUuid = UUID.randomUUID().toString();
      qInstance.setEnvironmentValues(Map.of("uuid", instanceUuid));

      QSession qSession    = new QSession();
      String   sessionUuid = qSession.getUuid();

      //////////////////////////////////////////////////////
      // init the context - make sure the uuids come back //
      //////////////////////////////////////////////////////
      QContext.init(qInstance, qSession);
      assertEquals(instanceUuid, QContext.getQInstance().getEnvironmentValues().get("uuid"));
      assertEquals(sessionUuid, QContext.getQSession().getUuid());

      //////////////////////////////////////////////////////////
      // capture the context - assert we got values we expect //
      //////////////////////////////////////////////////////////
      CapturedContext capturedContext = QContext.capture();
      assertEquals(instanceUuid, capturedContext.qInstance().getEnvironmentValues().get("uuid"));
      assertEquals(sessionUuid, capturedContext.qSession().getUuid());

      ///////////////////////////////////////
      // clear context - assert we're null //
      ///////////////////////////////////////
      QContext.clear();
      assertNull(QContext.getQInstance());
      assertNull(QContext.getQSession());

      //////////////////////////////////////////////////////////////////////////
      // init context with new values - make sure uuids aren't what we expect //
      //////////////////////////////////////////////////////////////////////////
      QContext.init(newQInstance(), new QSession());
      assertNotEquals(instanceUuid, QContext.getQInstance().getEnvironmentValues().get("uuid"));
      assertNotEquals(sessionUuid, QContext.getQSession().getUuid());

      /////////////////////////////////////////////////////////////////////
      // re-init to the captured context - make sure values are expected //
      /////////////////////////////////////////////////////////////////////
      QContext.init(capturedContext);
      assertEquals(instanceUuid, QContext.getQInstance().getEnvironmentValues().get("uuid"));
      assertEquals(sessionUuid, QContext.getQSession().getUuid());

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QInstance newQInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withType(QAuthenticationType.FULLY_ANONYMOUS).withName("anonymous"));
      qInstance.addBackend(new QBackendMetaData().withName("backend"));
      qInstance.addTable(new QTableMetaData().withName("table").withBackendName("backend").withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      return qInstance;
   }

}