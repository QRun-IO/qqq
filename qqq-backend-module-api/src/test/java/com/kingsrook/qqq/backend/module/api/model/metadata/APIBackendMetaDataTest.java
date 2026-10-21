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

package com.kingsrook.qqq.backend.module.api.model.metadata;


import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.module.api.BaseTest;
import com.kingsrook.qqq.backend.module.api.model.AuthorizationType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for APIBackendMetaData
 *******************************************************************************/
class APIBackendMetaDataTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingBaseUrl()
   {
      APIBackendMetaData apiBackendMetaData = new APIBackendMetaData()
         .withName("test");
      QInstanceValidator qInstanceValidator = new QInstanceValidator();
      apiBackendMetaData.performValidation(qInstanceValidator);
      assertEquals(1, qInstanceValidator.getErrors().size());
      assertThat(qInstanceValidator.getErrors()).anyMatch(e -> e.contains("Missing baseUrl"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthorizationApiKeyQueryParam()
   {
      APIBackendMetaData apiBackendMetaData = new APIBackendMetaData()
         .withAuthorizationType(AuthorizationType.API_KEY_QUERY_PARAM)
         .withBaseUrl("http://localhost:8000/")
         .withName("test");
      QInstanceValidator qInstanceValidator = new QInstanceValidator();
      apiBackendMetaData.performValidation(qInstanceValidator);
      assertEquals(2, qInstanceValidator.getErrors().size());
      assertThat(qInstanceValidator.getErrors()).anyMatch(e -> e.contains("Missing apiKey for API backend"));
      assertThat(qInstanceValidator.getErrors()).anyMatch(e -> e.contains("Missing apiKeyQueryParamName for API backend"));

      apiBackendMetaData = new APIBackendMetaData()
         .withAuthorizationType(AuthorizationType.API_KEY_QUERY_PARAM)
         .withApiKey("ABC-123")
         .withApiKeyQueryParamName("key")
         .withBaseUrl("http://localhost:8000/")
         .withName("test");
      qInstanceValidator = new QInstanceValidator();
      apiBackendMetaData.performValidation(qInstanceValidator);
      assertEquals(0, qInstanceValidator.getErrors().size());

      apiBackendMetaData = new APIBackendMetaData()
         .withAuthorizationType(AuthorizationType.API_KEY_HEADER)
         .withApiKey("ABC-123")
         .withApiKeyQueryParamName("key")
         .withBaseUrl("http://localhost:8000/")
         .withName("test");
      qInstanceValidator = new QInstanceValidator();
      apiBackendMetaData.performValidation(qInstanceValidator);
      assertEquals(1, qInstanceValidator.getErrors().size());
      assertThat(qInstanceValidator.getErrors()).anyMatch(e -> e.contains("Unexpected apiKeyQueryParamName for API backend"));

   }

}