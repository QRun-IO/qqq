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

package com.kingsrook.qqq.backend.module.api.mocks;


import java.io.IOException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil;
import com.kingsrook.qqq.backend.module.api.actions.QHttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;


/*******************************************************************************
 **
 *******************************************************************************/
public class MockApiActionUtils extends BaseAPIActionUtil
{
   public static MockApiUtilsHelper mockApiUtilsHelper;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QHttpResponse makeRequest(QTableMetaData table, HttpRequestBase request) throws QException
   {
      return (mockApiUtilsHelper.defaultMockMakeRequest(mockApiUtilsHelper, table, request, () -> super.makeRequest(table, request)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected CloseableHttpResponse executeHttpRequest(HttpRequestBase request, CloseableHttpClient httpClient) throws IOException
   {
      runMockAsserter(request);
      return new MockHttpResponse(mockApiUtilsHelper);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void runMockAsserter(HttpRequestBase request)
   {
      if(mockApiUtilsHelper.getMockRequestAsserter() != null)
      {
         try
         {
            mockApiUtilsHelper.getMockRequestAsserter().run(request);
         }
         catch(Exception e)
         {
            throw (new RuntimeException("Error running mock request asserter", e));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected CloseableHttpResponse executeOAuthTokenRequest(CloseableHttpClient client, HttpRequestBase request) throws IOException
   {
      runMockAsserter(request);
      return new MockHttpResponse(mockApiUtilsHelper);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected int getInitialRateLimitBackoffMillis()
   {
      return (1);
   }

}
