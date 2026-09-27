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


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
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
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
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
   private static final String BASIC_PREFIX = "Basic ";

   ////////////////////////////////////////////////////////////////////////////
   // request attributes set by buildInput: the session is resumed from the  //
   // sessionUUID cookie, or a sign-in replaces the browser's session        //
   ////////////////////////////////////////////////////////////////////////////
   private static final String RESUMED_FROM_COOKIE_ATTRIBUTE = "qqq.manageSession.resumedFromCookie";
   private static final String REPLACES_SESSION_ATTRIBUTE    = "qqq.manageSession.replacesSession";


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
            
            The response object will include a session identifier (`uuid`) to authenticate the user in subsequent API calls.
            
            For the `TABLE_BASED` type, send the user's credentials in an `Authorization: Basic` header (base64 of
            `username:password`, UTF-8); the body may be empty.  The password is verified against the user table and a
            session row is stored; a `401` response means the credentials were refused.

            To resume the session the browser already holds (for example after a page reload), post an empty object:
            a request with no credentials and no `sessionUUID` resumes the session named by the `sessionUUID` cookie,
            which may be `HttpOnly`.  That response carries the session's `values` but not its `uuid`; a `401`
            response means there is no valid session to resume.""");
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
         QJavalinImplementation.setSessionCookie(context, QJavalinImplementation.SESSION_UUID_COOKIE_NAME, sessionUuid);

         ////////////////////////////////////////////////////////////////////////////////
         // secured routes read sessionId before sessionUUID, so a password or OAuth2  //
         // code sign-in also points sessionId (for modules that use it) at the new    //
         // session: a sessionId left from an earlier session (the browser cannot      //
         // clear an HttpOnly cookie) must not shadow it (QRun-IO/qqq#733).            //
         ////////////////////////////////////////////////////////////////////////////////
         if(Boolean.TRUE.equals(context.attribute(REPLACES_SESSION_ATTRIBUTE)) && new QAuthenticationModuleDispatcher().getQModule(qInstance.getAuthentication()).usesSessionIdCookie())
         {
            QJavalinImplementation.setSessionCookie(context, QJavalinImplementation.SESSION_ID_COOKIE_NAME, sessionUuid);
         }
      }
      return (result);
   }



   /***************************************************************************
    ** Do not return the session token when its cookie is HttpOnly, whether
    ** signing in or resuming. A cookie-based resume also omits it in the
    ** readable-cookie mode (QRun-IO/qqq#733).
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, ManageSessionResponseV1 output) throws Exception
   {
      if(QJavalinImplementation.getSessionCookieHttpOnly() || Boolean.TRUE.equals(context.attribute(RESUMED_FROM_COOKIE_ATTRIBUTE)))
      {
         context.result(JsonUtils.toJson(new ManageSessionResponseV1().withValues(output.getValues())));
         return;
      }
      super.handleOutput(context, output);
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
               .withProperty("code", new Schema()
                  .withType(Type.STRING)
                  .withDescription("OAuth2 authorization code, for the backend to exchange with the identity provider (authorization-code + PKCE sign-in).")
               )
               .withProperty("codeVerifier", new Schema()
                  .withType(Type.STRING)
                  .withDescription("PKCE code verifier that goes with `code`.")
               )
               .withProperty("redirectUri", new Schema()
                  .withType(Type.STRING)
                  .withDescription("Redirect URI the authorization `code` was issued to.")
               )
               .withProperty("sessionUUID", new Schema()
                  .withType(Type.STRING)
                  .withDescription("UUID of an existing session, to resume it instead of signing in again.  Omit it (and all credentials) to resume the session named by the sessionUUID cookie.")
               )
            )
         ));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(new Parameter()
         .withName("Authorization")
         .withDescription("""
            For `TABLE_BASED` authentication: `Basic ` followed by the base64 encoding of `username:password` (UTF-8).
            Not used by other authentication types.""")
         .withIn(In.HEADER)
         .withSchema(new Schema().withType(Type.STRING))
         .withExample("Basic dXNlcm5hbWU6cGFzc3dvcmQ="));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ManageSessionInput buildInput(Context context) throws Exception
   {
      ManageSessionInput manageSessionInput = new ManageSessionInput();
      manageSessionInput.setAccessToken(getRequestParam(context, "accessToken"));
      manageSessionInput.setCode(getRequestParam(context, "code"));
      manageSessionInput.setCodeVerifier(getRequestParam(context, "codeVerifier"));
      manageSessionInput.setRedirectUri(getRequestParam(context, "redirectUri"));
      manageSessionInput.setSessionUUID(getRequestParam(context, "sessionUUID"));

      String authorization = context.header("Authorization");
      if(authorization != null && authorization.startsWith(BASIC_PREFIX))
      {
         manageSessionInput.setBasicAuthString(authorization.substring(BASIC_PREFIX.length()).trim());
      }

      if(StringUtils.hasContent(manageSessionInput.getBasicAuthString()) || StringUtils.hasContent(manageSessionInput.getCode()))
      {
         context.attribute(REPLACES_SESSION_ATTRIBUTE, true);
      }

      //////////////////////////////////////////////////////////////////////////////
      // a request with no credentials and no session uuid resumes the session    //
      // named by the sessionUUID cookie, so a dashboard can resume it without    //
      // reading the (HttpOnly) cookie in the browser (QRun-IO/qqq#733)           //
      //////////////////////////////////////////////////////////////////////////////
      boolean namesCredentialsOrSession = StringUtils.hasContent(manageSessionInput.getAccessToken())
         || StringUtils.hasContent(manageSessionInput.getCode())
         || StringUtils.hasContent(manageSessionInput.getCodeVerifier())
         || StringUtils.hasContent(manageSessionInput.getRedirectUri())
         || StringUtils.hasContent(manageSessionInput.getSessionUUID())
         || StringUtils.hasContent(manageSessionInput.getBasicAuthString());
      String cookieSessionUUID = context.cookie(QJavalinImplementation.SESSION_UUID_COOKIE_NAME);
      if(!namesCredentialsOrSession && StringUtils.hasContent(cookieSessionUUID))
      {
         manageSessionInput.setSessionUUID(cookieSessionUUID);
         context.attribute(RESUMED_FROM_COOKIE_ATTRIBUTE, true);
      }

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
