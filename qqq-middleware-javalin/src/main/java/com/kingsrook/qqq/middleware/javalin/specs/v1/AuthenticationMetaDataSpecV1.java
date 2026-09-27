/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.middleware.javalin.executors.AuthenticationMetaDataExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.AuthenticationMetaDataInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.AuthenticationMetaDataResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class AuthenticationMetaDataSpecV1 extends AbstractEndpointSpec<AuthenticationMetaDataInput, AuthenticationMetaDataResponseV1, AuthenticationMetaDataExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/metaData/authentication")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.AUTHENTICATION)
         .withShortSummary("Get authentication metaData")
         .withLongDescription("""
            For a frontend to determine which authentication provider or mechanism to use, it should begin its lifecycle
            by requesting this metaData object, and inspecting the `type` property in the response.
            
            Note that this endpoint is not secured, as its purpose is to be called as part of the workflow that results
            in a user being authenticated."""
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public boolean isSecured()
   {
      return (false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public AuthenticationMetaDataInput buildInput(Context context) throws Exception
   {
      AuthenticationMetaDataInput input = new AuthenticationMetaDataInput();
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(AuthenticationMetaDataResponseV1.class.getSimpleName(), new AuthenticationMetaDataResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("For FULLY_ANONYMOUS type", new Example()
         .withValue(new AuthenticationMetaDataResponseV1()
            .withType(QAuthenticationType.FULLY_ANONYMOUS.name())
            .withName("anonymous")));

      examples.put("For AUTH_0 type", new Example()
         .withValue(new AuthenticationMetaDataResponseV1()
            .withType(QAuthenticationType.AUTH_0.name())
            .withName("auth0")
            .withValues(new AuthenticationMetaDataResponseV1.Auth0Values()
               .withClientId("abcdefg1234567")
               .withBaseUrl("https://myapp.auth0.com/")
               .withAudience("myapp.mydomain.com"))));

      return new BasicResponse("Successful Response", AuthenticationMetaDataResponseV1.class.getSimpleName(), examples);
   }

}
