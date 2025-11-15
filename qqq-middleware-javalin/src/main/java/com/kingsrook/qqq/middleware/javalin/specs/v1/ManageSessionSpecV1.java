/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.executors.ManageSessionExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ManageSessionInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BasicErrorResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ManageSessionResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.ProcessSpecUtilsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;


/*******************************************************************************
 **
 *******************************************************************************/
public class ManageSessionSpecV1 extends AbstractEndpointSpec<ManageSessionInput, ManageSessionResponseV1, ManageSessionExecutor>
{


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/manageSession")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.AUTHENTICATION)
         .withShortSummary("Create a session")
         .withLongDescription("""
            After a frontend authenticates the user as per the requirements of the authentication provider specified by the
            `type` field in the `metaData/authentication` response, data from that authentication provider should be posted
            to this endpoint, to create a session within the QQQ application.
            
            The response object will include a session identifier (`uuid`) to authenticate the user in subsequent API calls.""");
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
   public ManageSessionResponseV1 serveRequest(AbstractMiddlewareVersion abstractMiddlewareVersion, Context context) throws Exception
   {
      ManageSessionResponseV1 result = super.serveRequest(abstractMiddlewareVersion, context);
      if(result != null)
      {
         String sessionUuid = result.getUuid();
         // Set cookie with path="/" to make it accessible from all paths (/, /admin, /dashboard, etc.)
         context.cookie(QJavalinImplementation.SESSION_UUID_COOKIE_NAME, sessionUuid, QJavalinImplementation.SESSION_COOKIE_AGE, "/");
      }
      return (result);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return new RequestBody()
         .withRequired(true)
         .withContent(MapBuilder.of(ContentType.JSON, new Content()
            .withSchema(new Schema()
               .withDescription("Data required to create the session.  Specific needs may vary based on the AuthenticationModule type in the QQQ Backend.")
               .withType(Type.OBJECT)
               .withProperty("accessToken", new Schema()
                  .withType(Type.STRING)
                  .withDescription("An access token from a downstream authentication provider (e.g., Auth0), to use as the basis for authentication and authorization.")
               )
            )
         ));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ManageSessionInput buildInput(Context context) throws Exception
   {
      ManageSessionInput manageSessionInput = new ManageSessionInput();
      manageSessionInput.setAccessToken(getRequestParam(context, "accessToken"));
      return (manageSessionInput);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();

      examples.put("With no custom values", new Example().withValue(new ManageSessionResponseV1()
         .withUuid(ProcessSpecUtilsV1.EXAMPLE_PROCESS_UUID)
      ));

      examples.put("With custom values", new Example().withValue(new ManageSessionResponseV1()
         .withUuid(ProcessSpecUtilsV1.EXAMPLE_JOB_UUID)
         .withValues(MapBuilder.of(LinkedHashMap<String, Serializable>::new)
            .with("region", "US")
            .with("userCategoryId", 47)
            .build()
         )
      ));

      return new BasicResponse("Successful response - session has been created",
         ManageSessionResponseV1.class.getSimpleName(),
         examples);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(
         ManageSessionResponseV1.class.getSimpleName(), new ManageSessionResponseV1().toSchema(),
         BasicErrorResponseV1.class.getSimpleName(), new BasicErrorResponseV1().toSchema()
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<BasicResponse> defineAdditionalBasicResponses()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("Invalid token", new Example().withValue(new BasicErrorResponseV1().withError("Unable to decode access token.")));

      return List.of(
         new BasicResponse(HttpStatus.UNAUTHORIZED,
            "Authentication error - session was not created",
            BasicErrorResponseV1.class.getSimpleName(),
            examples
         )
      );
   }

}
