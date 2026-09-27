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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.middleware.javalin.executors.MetaDataExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.MetaDataInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.MetaDataResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.MetaDataSpecUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class MetaDataSpecV1 extends AbstractEndpointSpec<MetaDataInput, MetaDataResponseV1, MetaDataExecutor>
{
   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/metaData")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.GENERAL)
         .withShortSummary("Get instance metaData")
         .withLongDescription("""
            Load the overall metadata, as is relevant to a frontend, for the entire application, with permissions applied, as per the
            authenticated user.
            
            This includes:
            - Apps (both as a map of name to AppMetaData (`apps`), but also as a tree (`appTree`), for presenting 
            hierarchical navigation),
            - Tables (but without all details, e.g., fields),
            - Processes (also without full details, e.g., screens),
            - Reports
            - Widgets
            - Branding
            - Help Contents
            - Environment values
            """
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(

         new Parameter()
            .withName("frontendName")
            .withDescription("""
               Name of the frontend requesting the meta-data.
               Generally a QQQ frontend library, unless a custom application frontend has been built.""")
            .withIn(In.QUERY)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("qqq-frontend-material-dashboard"),

         new Parameter()
            .withName("frontendVersion")
            .withDescription("Version of the frontend requesting the meta-data.")
            .withIn(In.QUERY)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("0.23.0"),

         new Parameter()
            .withName("applicationName")
            .withDescription("""
               Name of the application requesting the meta-data.  e.g., an instance of a specific frontend
               (i.e., an application might be deployed with 2 different qqq-frontend-material-dashboard frontends, 
               in which case this attribute allows differentiation between them).""")
            .withIn(In.QUERY)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("my-admin-web-app"),

         new Parameter()
            .withName("applicationVersion")
            .withDescription("Version of the application requesting the meta-data.")
            .withIn(In.QUERY)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("20241021")

      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public MetaDataInput buildInput(Context context) throws Exception
   {
      MetaDataInput input = new MetaDataInput();

      input.setMiddlewareName("qqq-middleware-javalin");
      input.setMiddlewareVersion("v1");

      input.setFrontendName(getRequestParam(context, "frontendName"));
      input.setFrontendVersion(getRequestParam(context, "frontendVersion"));

      input.setApplicationName(getRequestParam(context, "applicationName"));
      input.setApplicationVersion(getRequestParam(context, "applicationVersion"));

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(MetaDataResponseV1.class.getSimpleName(), new MetaDataResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new HashMap<>();

      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // double-wrap the context here, so the instance will exist when the system-user-session is created //
      // to avoid warnings out of system-user-session about there not being an instance in context.       //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.withTemporaryContext(new CapturedContext(MetaDataSpecUtils.getExampleInstance(), null), () ->
      {
         QContext.withTemporaryContext(new CapturedContext(MetaDataSpecUtils.getExampleInstance(), new QSystemUserSession()), () ->
         {
            try
            {
               MetaDataAction metaDataAction = new MetaDataAction();
               MetaDataOutput output         = metaDataAction.execute(new com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput());
               examples.put("Example", new Example()
                  .withValue(new MetaDataResponseV1()
                     .withMetaDataOutput(output)
                  )
               );
            }
            catch(Exception e)
            {
               examples.put("Example", new Example().withValue("Error building example: " + e.getMessage()));
            }
         });
      });

      return new BasicResponse("""
         Overall metadata for the application.""",
         MetaDataResponseV1.class.getSimpleName(),
         examples
      );
   }

}
