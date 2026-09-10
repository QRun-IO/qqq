/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
