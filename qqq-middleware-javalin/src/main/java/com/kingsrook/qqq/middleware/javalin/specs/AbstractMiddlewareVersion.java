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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.SchemaBuilder;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Components;
import com.kingsrook.qqq.openapi.model.Contact;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.Info;
import com.kingsrook.qqq.openapi.model.Method;
import com.kingsrook.qqq.openapi.model.OpenAPI;
import com.kingsrook.qqq.openapi.model.Path;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.SecuritySchemeType;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;


/*******************************************************************************
 ** Baseclass that combines multiple specs together into a single "version" of
 ** the full qqq middleware.
 *******************************************************************************/
public abstract class AbstractMiddlewareVersion
{
   public static final QLogger LOG = QLogger.getLogger(AbstractMiddlewareVersion.class);



   /***************************************************************************
    **
    ***************************************************************************/
   public abstract String getVersion();

   /***************************************************************************
    ** hey - don't re-construct the endpoint-spec objects inside this method...
    ***************************************************************************/
   public abstract List<AbstractEndpointSpec<?, ?, ?>> getEndpointSpecs();



   /***************************************************************************
    **
    ***************************************************************************/
   public EndpointGroup getJavalinEndpointGroup(QInstance qInstance)
   {
      return (() ->
      {
         for(AbstractEndpointSpec<?, ?, ?> spec : CollectionUtils.nonNullList(getEndpointSpecs()))
         {
            spec.defineRoute(this, getVersionBasePath());
         }
      });
   }


   /***************************************************************************
    **
    ***************************************************************************/
   public String getVersionBasePath()
   {
      return ("/" + getVersion() + "/");
   }



   /***************************************************************************
    ** For initial setup when server boots, set the qInstance - but also,
    ** e.g., for development, to do a hot-swap.
    ***************************************************************************/
   public void setQInstance(QInstance qInstance)
   {
      getEndpointSpecs().forEach(spec -> spec.setQInstance(qInstance));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public OpenAPI generateOpenAPIModel(String basePath) throws QException
   {
      List<AbstractEndpointSpec<?, ?, ?>> list = getEndpointSpecs();

      Map<String, Path>    paths             = new LinkedHashMap<>();
      Map<String, Example> componentExamples = new LinkedHashMap<>();

      Set<Class<?>>       componentClasses = new HashSet<>();
      Map<String, Schema> componentSchemas = new TreeMap<>();
      buildComponentSchemasFromComponentsPackage(componentSchemas, componentClasses);

      String sessionUuidCookieSchemeName = "sessionUuidCookie";
      SecurityScheme sessionUuidCookieScheme = new SecurityScheme()
         .withType(SecuritySchemeType.API_KEY)
         .withName("sessionUUID")
         .withIn("cookie");

      for(AbstractEndpointSpec<?, ?, ?> spec : list)
      {
         CompleteOperation completeOperation = spec.defineCompleteOperation();
         String            fullPath          = ("/" + basePath + getVersionBasePath() + completeOperation.getPath()).replaceAll("/+", "/");
         Path              path              = paths.computeIfAbsent(fullPath, (k) -> new Path());
         Method            method            = completeOperation.getMethod();

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if this spec is supposed to be secured, but no security has been applied, then add our default security //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(spec.isSecured() && method.getSecurity() == null)
         {
            //////////////////////////////////////////////////////////////////////////////
            // the N/A here refers to the lack of a 'scope' for this kind of permission //
            //////////////////////////////////////////////////////////////////////////////
            method.withSecurity(List.of(Map.of(sessionUuidCookieSchemeName, List.of("N/A"))));
         }

         convertMethodSchemasToRefs(method, componentClasses);

         switch(completeOperation.getHttpMethod())
         {
            case GET ->
            {
               warnIfPathMethodAlreadyUsed(path.getGet(), completeOperation, spec);
               path.withGet(method);
            }
            case POST ->
            {
               warnIfPathMethodAlreadyUsed(path.getPost(), completeOperation, spec);
               path.withPost(method);
            }
            case PUT ->
            {
               warnIfPathMethodAlreadyUsed(path.getPut(), completeOperation, spec);
               path.withPut(method);
            }
            case PATCH ->
            {
               warnIfPathMethodAlreadyUsed(path.getPatch(), completeOperation, spec);
               path.withPatch(method);
            }
            case DELETE ->
            {
               warnIfPathMethodAlreadyUsed(path.getDelete(), completeOperation, spec);
               path.withDelete(method);
            }
            default -> throw new IllegalStateException("Unexpected value: " + completeOperation.getHttpMethod());
         }

         for(Map.Entry<String, Schema> entry : CollectionUtils.nonNullMap(spec.defineComponentSchemas()).entrySet())
         {
            if(componentSchemas.containsKey(entry.getKey()))
            {
               LOG.warn("More than one endpoint spec defined a componentSchema named: " + entry.getKey() + ".  The last one encountered (from " + spec.getClass().getSimpleName() + ") will be used.");
            }

            componentSchemas.put(entry.getKey(), entry.getValue());
         }
      }

      OpenAPI openAPI = new OpenAPI();
      openAPI.withInfo(new Info()
         .withVersion(getVersion())
         .withTitle("QQQ Middleware API")
         .withDescription(getDescription())
         .withContact(new Contact().withEmail("contact@kingsrook.com"))
      );

      openAPI.withPaths(paths);

      openAPI.withComponents(new Components()
         .withSchemas(componentSchemas)
         .withExamples(componentExamples)
         .withSecuritySchemes(Map.of(sessionUuidCookieSchemeName, sessionUuidCookieScheme))
      );

      return openAPI;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void buildComponentSchemasFromComponentsPackage(Map<String, Schema> componentSchemas, Set<Class<?>> componentClasses) throws QException
   {
      try
      {
         ////////////////////////////////////////////////////
         // find all classes in the components sub-package //
         ////////////////////////////////////////////////////
         String         packageName      = getClass().getPackageName();
         List<Class<?>> classesInPackage = ClassPathUtils.getClassesInPackage(packageName);
         for(Class<?> c : classesInPackage)
         {
            if(c.getPackageName().matches(".*\\bcomponents\\b.*"))
            {
               componentClasses.add(c);
            }
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         // now that we know that full set, make any references to others schemas in those objects be via Ref //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         for(Class<?> c : componentClasses)
         {
            Object o = null;
            try
            {
               o = c.getConstructor().newInstance();
            }
            catch(Exception nsme)
            {
               ///////////////////////////////////////
               // fine, assume we can't do toSchema //
               ///////////////////////////////////////
            }

            Schema schema = null;
            if(o instanceof ToSchema toSchema)
            {
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // just in case a custom implementation of toSchema is provided (e.g., to go around a wrapped object or some-such) //
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               schema = toSchema.toSchema();
            }
            else
            {
               schema = new SchemaBuilder().classToSchema(c);
            }

            convertSchemaToRefs(schema, componentClasses);

            componentSchemas.put(c.getSimpleName(), schema);
         }
      }
      catch(Exception e)
      {
         throw (new QException("Error building component schemas from components package", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void convertMethodSchemasToRefs(Method method, Set<Class<?>> componentClasses)
   {
      for(Response response : method.getResponses().values())
      {
         for(Content content : response.getContent().values())
         {
            Schema schema = content.getSchema();
            convertSchemaToRefs(schema, componentClasses);
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void convertSchemaToRefs(Schema schema, Set<Class<?>> componentClasses)
   {
      if(schema.getItems() instanceof SchemaBuilder.SchemaFromBuilder itemSchemaFromBuilder && componentClasses.contains(itemSchemaFromBuilder.getOriginalClass()))
      {
         schema.getItems().withRefToSchema(itemSchemaFromBuilder.getOriginalClass().getSimpleName());
         schema.getItems().setProperties(null);
         schema.getItems().setType((Type) null);
      }
      else if(schema.getItems() != null)
      {
         convertSchemaToRefs(schema.getItems(), componentClasses);
      }

      if(schema.getProperties() != null)
      {
         for(Schema propertySchema : schema.getProperties().values())
         {
            if(propertySchema instanceof SchemaBuilder.SchemaFromBuilder propertySchemaFromBuilder && componentClasses.contains(propertySchemaFromBuilder.getOriginalClass()))
            {
               propertySchema.withRefToSchema(propertySchemaFromBuilder.getOriginalClass().getSimpleName());
               propertySchema.setProperties(null);
               propertySchema.setType((Type) null);
            }
            else
            {
               convertSchemaToRefs(propertySchema, componentClasses);
            }
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String getDescription()
   {
      return """
         ## Intro
         This is the definition of the standard API implemented by QQQ Middleware.
         
         Developers of QQQ Frontends (e.g., javascript libraries, or native applications) use this API to access
         a QQQ Backend server.
         
         As such, this API itself is not concerned with any of the application-level details of any particular
         application built using QQQ.  Instead, this API is all about the generic endpoints used for any application
         built on QQQ.  For example, many endpoints work with a `${table}` path parameter - whose possible values
         are defined by the application - but which are not known to this API.
         
         ## Flow
         The typical flow of a user (as implemented in a frontend that utilizes this API) looks like:
         1. Frontend calls `.../metaData/authentication`, to know what type of authentication provider is used by the backend, and display an appropriate UI to the user for authenticating.
         2. User authenticates in frontend, as required for the authentication provider.
         3. Frontend calls `.../manageSession`, providing authentication details (e.g., an accessToken or other credentials) to the backend.
         4. The response from the `manageSession` call (assuming success), sets the `sessionUUID` Cookie, which should be included in all subsequent requests for authentication.
         5. After the user is authenticated, the frontend calls `.../metaData`, to discover the apps, tables, processes, etc, that the application is made up of (and that the authenticated user has permission to access).
         6. As the user interacts with apps, tables, process, etc, the frontend utilizes the appropriate endpoints as required.
         """;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void warnIfPathMethodAlreadyUsed(Method existing, CompleteOperation completeOperation, AbstractEndpointSpec<?, ?, ?> spec)
   {
      if(existing != null)
      {
         LOG.warn("More than one endpoint spec for version " + getVersion() + " defined a " + completeOperation.getHttpMethod() + " at path: " + completeOperation.getPath() + ".  The last one encountered (from " + spec.getClass().getSimpleName() + ") will be used.");
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void preExecute(Context context) throws QException
   {
      /////////////////////
      // noop at default //
      /////////////////////
   }
}
