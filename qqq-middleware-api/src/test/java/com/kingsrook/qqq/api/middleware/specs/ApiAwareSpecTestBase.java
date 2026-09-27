/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.middleware.specs;


import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.middleware.specs.v1.ApiAwareMiddlewareVersionV1;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;


/*******************************************************************************
 **
 *******************************************************************************/
public abstract class ApiAwareSpecTestBase extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      return (TestUtils.defineInstance());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected void primeTestData(QInstance qInstance) throws Exception
   {
      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession()), () ->
      {
         TestUtils.insertSimpsons();
         TestUtils.insertTim2Shoes();
      });
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractMiddlewareVersion getMiddlewareVersion()
   {
      ApiAwareMiddlewareVersionV1 apiAwareMiddlewareVersionV1 = new ApiAwareMiddlewareVersionV1();

      apiAwareMiddlewareVersionV1.addVersion(TestUtils.API_NAME, new APIVersion(TestUtils.V2022_Q4));
      apiAwareMiddlewareVersionV1.addVersion(TestUtils.API_NAME, new APIVersion(TestUtils.V2023_Q1));
      apiAwareMiddlewareVersionV1.addVersion(TestUtils.API_NAME, new APIVersion(TestUtils.V2023_Q2));
      apiAwareMiddlewareVersionV1.addVersion(TestUtils.ALTERNATIVE_API_NAME, new APIVersion(TestUtils.V2022_Q4));
      apiAwareMiddlewareVersionV1.addVersion(TestUtils.ALTERNATIVE_API_NAME, new APIVersion(TestUtils.V2023_Q1));
      apiAwareMiddlewareVersionV1.addVersion(TestUtils.ALTERNATIVE_API_NAME, new APIVersion(TestUtils.V2023_Q2));

      return apiAwareMiddlewareVersionV1;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected String getBaseUrlAndPath(String apiPath, String version)
   {
      String path = "/qqq/" + getVersion() + "/" + apiPath + "/" + version;
      return "http://localhost:" + PORT + path.replaceAll("/+", "/").replaceFirst("/$", "");
   }

}
