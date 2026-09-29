/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.api.middleware.specs;


import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.actions.ApiImplementation;
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
      ApiImplementation.clearCaches();
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
