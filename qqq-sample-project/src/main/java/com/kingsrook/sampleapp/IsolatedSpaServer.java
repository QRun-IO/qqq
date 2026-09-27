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

package com.kingsrook.sampleapp;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.routeproviders.IsolatedSpaRouteProvider;
import com.kingsrook.sampleapp.authentication.LocalDemoRouteAuthenticator;


/*******************************************************************************
 ** Two bundled SPAs sharing the sample database and APIs. The private SPA uses
 ** fixed demonstration credentials; applications must supply real authentication.
 *******************************************************************************/
public class IsolatedSpaServer extends SampleJavalinServer
{
   /*******************************************************************************
    ** Replace the default sample's static routes with isolated SPA routes.
    *******************************************************************************/
   public IsolatedSpaServer()
   {
      setServeFrontendMaterialDashboard(false);
      setJavalinMetaData(new QJavalinMetaData());
      setAdditionalRouteProviders(List.of(
         new IsolatedSpaRouteProvider("/private", "site/private")
            .withSpaIndexFile("site/private/index.html")
            .withLoadFromJar(true)
            .withAuthenticator(new QCodeReference(LocalDemoRouteAuthenticator.class)),
         new IsolatedSpaRouteProvider("/", "site/public")
            .withSpaIndexFile("site/public/index.html")
            .withLoadFromJar(true)
            .withExcludedPaths(List.of("/private", "/api", "/qqq-api", "/metaData", "/data", "/processes",
               "/reports", "/download", "/possibleValues", "/widget", "/manageSession", "/serverInfo", "/health"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args) throws QException
   {
      new IsolatedSpaServer().start();
   }
}
