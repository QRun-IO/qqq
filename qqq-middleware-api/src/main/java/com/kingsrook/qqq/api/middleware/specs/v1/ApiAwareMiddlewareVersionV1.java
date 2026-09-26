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

package com.kingsrook.qqq.api.middleware.specs.v1;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.TableCountSpecV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.TableMetaDataSpecV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.TableQuerySpecV1;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiAwareMiddlewareVersionV1 extends MiddlewareVersionV1
{
   private Map<String, ApiNameAndVersions> apiNameAndVersionsByPath = new HashMap<>();

   private List<AbstractEndpointSpec<?, ?, ?>> specs;



   /***************************************************************************
    **
    ***************************************************************************/
   private record ApiNameAndVersions(String apiName, Set<String> apiVersions)
   {

   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ApiAwareMiddlewareVersionV1()
   {
      this.specs = defineEndpointSpecs();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void addVersion(String apiName, APIVersion apiVersion)
   {
      ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = ApiInstanceMetaDataContainer.of(QContext.getQInstance());
      ApiInstanceMetaData          apiInstanceMetaData          = apiInstanceMetaDataContainer.getApis().get(apiName);
      String                       apiPath                      = apiInstanceMetaData.getPath();

      apiPath = Objects.requireNonNullElse(apiPath, "").replaceFirst("^/", "").replaceFirst("/$", "");
      String apiVersionString = apiVersion.toString().replaceFirst("^/", "").replaceFirst("/$", "");

      ApiNameAndVersions apiNameAndVersions = apiNameAndVersionsByPath.computeIfAbsent(apiPath, (p) -> new ApiNameAndVersions(apiName, new HashSet<>()));
      apiNameAndVersions.apiVersions().add(apiVersionString);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public List<AbstractEndpointSpec<?, ?, ?>> defineEndpointSpecs()
   {
      List<AbstractEndpointSpec<?, ?, ?>> specs = new ArrayList<>(super.getEndpointSpecs());

      ListIterator<AbstractEndpointSpec<?, ?, ?>> listIterator = specs.listIterator();
      while(listIterator.hasNext())
      {
         AbstractEndpointSpec<?, ?, ?> spec = listIterator.next();
         if(spec.getClass().equals(TableMetaDataSpecV1.class))
         {
            listIterator.set(new ApiAwareTableMetaDataSpecV1());
         }
         else if(spec.getClass().equals(TableQuerySpecV1.class))
         {
            listIterator.set(new ApiAwareTableQuerySpecV1());
         }
         else if(spec.getClass().equals(TableCountSpecV1.class))
         {
            listIterator.set(new ApiAwareTableCountSpecV1());
         }
      }

      return (specs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<AbstractEndpointSpec<?, ?, ?>> getEndpointSpecs()
   {
      return (specs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getVersionBasePath()
   {
      // return ("/" + getVersion() + "/" + apiPath + "/" + apiVersion + "/");
      return ("/" + getVersion() + "/{applicationApiPath}/{applicationApiVersion}/");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void preExecute(Context context) throws QException
   {
      String apiPath    = context.pathParam("applicationApiPath");
      String apiVersion = context.pathParam("applicationApiVersion");

      ApiNameAndVersions apiNameAndVersions = apiNameAndVersionsByPath.get(apiPath);
      if(apiNameAndVersions != null)
      {
         Set<String> allowedVersions = apiNameAndVersions.apiVersions();
         if(allowedVersions.contains(apiVersion))
         {
            QSession session = QContext.getQSession();
            session.setValue("apiName", apiNameAndVersions.apiName());
            session.setValue("apiVersion", apiVersion);
            return;
         }
      }

      throw new QNotFoundException("No API exists at the requested path.");
   }
}
