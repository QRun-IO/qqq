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

package com.kingsrook.qqq.backend.module.api;


import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.module.api.mocks.MockApiActionUtils;
import com.kingsrook.qqq.backend.module.api.model.AuthorizationType;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;
import com.kingsrook.qqq.backend.module.api.model.metadata.APITableBackendDetails;


/*******************************************************************************
 **
 *******************************************************************************/
public class TestUtils
{
   public static final String MEMORY_BACKEND_NAME   = "memory";
   public static final String EASYPOST_BACKEND_NAME = "easypost";
   public static final String MOCK_BACKEND_NAME     = "mock";
   public static final String MOCK_TABLE_NAME       = "mock";



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QInstance defineInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), defineAuthentication());

      qInstance.addBackend(defineMemoryBackend());

      qInstance.addBackend(defineMockBackend());
      qInstance.addTable(defineMockTable());

      qInstance.addBackend(defineEasypostBackend());
      qInstance.addTable(defineTableEasypostTracker());

      qInstance.addTable(defineVariant());

      return (qInstance);
   }



   /*******************************************************************************
    ** Define the in-memory backend used in standard tests
    *******************************************************************************/
   public static QBackendMetaData defineMemoryBackend()
   {
      return new QBackendMetaData()
         .withName(MEMORY_BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QBackendMetaData defineMockBackend()
   {
      return (new APIBackendMetaData()
         .withName(MOCK_BACKEND_NAME)
         .withAuthorizationType(AuthorizationType.API_KEY_HEADER)
         .withBaseUrl("http://localhost:9999/mock")
         .withContentType("application/json")
         .withActionUtil(new QCodeReference(MockApiActionUtils.class)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData defineMockTable()
   {
      return (new QTableMetaData()
         .withName(MOCK_TABLE_NAME)
         .withBackendName(MOCK_BACKEND_NAME)
         .withField(new QFieldMetaData("id", QFieldType.STRING))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withPrimaryKeyField("id")
         .withBackendDetails(new APITableBackendDetails()
            .withTablePath("mock")
            .withTableWrapperObjectName("mocks")
         ));
   }



   /*******************************************************************************
    ** Define the authentication used in standard tests - using 'mock' type.
    **
    *******************************************************************************/
   public static QAuthenticationMetaData defineAuthentication()
   {
      return new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QBackendMetaData defineEasypostBackend()
   {
      return (new APIBackendMetaData()
         .withName("easypost")
         .withUsername("local-protocol-fixture-key")
         .withPassword("")
         .withAuthorizationType(AuthorizationType.BASIC_AUTH_USERNAME_PASSWORD)
         .withBaseUrl("http://127.0.0.1:0/v2/")
         .withContentType("application/json")
         .withActionUtil(new QCodeReference(EasyPostUtils.class)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData defineVariant()
   {
      return (new QTableMetaData()
         .withName("variant")
         .withBackendName(MEMORY_BACKEND_NAME)
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("type", QFieldType.STRING))
         .withField(new QFieldMetaData("apiKey", QFieldType.STRING))
         .withField(new QFieldMetaData("username", QFieldType.STRING))
         .withField(new QFieldMetaData("password", QFieldType.STRING))
         .withPrimaryKeyField("id")
         .withBackendDetails(new APITableBackendDetails()
            .withTablePath("variant")
            .withTableWrapperObjectName("variant")
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData defineTableEasypostTracker()
   {
      return (new QTableMetaData()
         .withName("easypostTracker")
         .withBackendName("easypost")
         .withField(new QFieldMetaData("id", QFieldType.STRING))
         .withField(new QFieldMetaData("trackingNo", QFieldType.STRING).withBackendName("tracking_code"))
         .withField(new QFieldMetaData("carrier", QFieldType.STRING).withBackendName("carrier"))
         .withPrimaryKeyField("id")
         .withBackendDetails(new APITableBackendDetails()
            .withTablePath("trackers")
            .withTableWrapperObjectName("tracker")
         ));
   }
}
