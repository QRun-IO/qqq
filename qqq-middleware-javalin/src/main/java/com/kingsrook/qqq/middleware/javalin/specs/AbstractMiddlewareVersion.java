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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.SchemaBuilder;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BasicErrorResponseV1;
import com.kingsrook.qqq.openapi.model.Components;
import com.kingsrook.qqq.openapi.model.Contact;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.Info;
import com.kingsrook.qqq.openapi.model.Method;
import com.kingsrook.qqq.openapi.model.OAuth2;
import com.kingsrook.qqq.openapi.model.OAuth2Flow;
import com.kingsrook.qqq.openapi.model.OpenAPI;
import com.kingsrook.qqq.openapi.model.Path;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.SecuritySchemeType;
import com.kingsrook.qqq.openapi.model.Server;
import com.kingsrook.qqq.openapi.model.Tag;
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

   ////////////////////////////////////////////////////////////////////////////
   // specs registered by modules & qbits (MiddlewareSpecExtensionsMetaData) //
   // - loaded from the QInstance when setQInstance is called, until routes  //
   // have been bound (they can only be bound once).                         //
   ////////////////////////////////////////////////////////////////////////////
   private List<AbstractEndpointSpec<?, ?, ?>>  extensionSpecs = new ArrayList<>();
   private Map<String, MiddlewareSpecExtension> extensions     = new LinkedHashMap<>();
   private boolean                              routesDefined  = false;



   /***************************************************************************
    ** The semantic version (MAJOR.MINOR.PATCH) of this version of the API, as
    ** published in the OpenAPI info.version.  MAJOR matches the URL version.
    ** See docs/UI-API-CHANGELOG.md for the rules.  The default (for versions
    ** that predate semver) is the URL version itself.
    ***************************************************************************/
   public String getSemanticVersion()
   {
      return (getVersion());
   }



   /***************************************************************************
    ** Core feature names this version supports, published by the version
    ** endpoint, so clients can toggle optional areas without probing.
    ***************************************************************************/
   public List<String> getFeatures()
   {
      return (List.of());
   }



   /***************************************************************************
    ** The core specs plus the specs registered by extensions, in route order.
    ***************************************************************************/
   public List<AbstractEndpointSpec<?, ?, ?>> getAllEndpointSpecs()
   {
      List<AbstractEndpointSpec<?, ?, ?>> rs = new ArrayList<>(CollectionUtils.nonNullList(getEndpointSpecs()));
      rs.addAll(extensionSpecs);
      return (rs);
   }



   /***************************************************************************
    ** The extensions whose specs this version serves.
    ***************************************************************************/
   public Map<String, MiddlewareSpecExtension> getExtensions()
   {
      return (extensions);
   }



   /***************************************************************************
    ** Load the extension specs registered in a QInstance (replacing any loaded
    ** before).  Called by setQInstance until routes are defined.
    ***************************************************************************/
   public void loadExtensions(QInstance qInstance)
   {
      MiddlewareSpecExtensionsMetaData registry = qInstance == null ? null : MiddlewareSpecExtensionsMetaData.of(qInstance);
      if(registry == null)
      {
         this.extensionSpecs = new ArrayList<>();
         this.extensions = new LinkedHashMap<>();
         return;
      }

      try
      {
         this.extensionSpecs = registry.instantiateAllSpecs();
         this.extensions = new LinkedHashMap<>(registry.getExtensions());
      }
      catch(ReflectiveOperationException e)
      {
         throw (new QRuntimeException("Error instantiating UI API extension specs", e));
      }
   }



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
         routesDefined = true;
         for(AbstractEndpointSpec<?, ?, ?> spec : getAllEndpointSpecs())
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
    ** Paths that serve documentation. Versions with parameterized execution
    ** routes can provide concrete paths to avoid shadowing other providers.
    ***************************************************************************/
   public List<String> getDocumentationBasePaths()
   {
      return (List.of(getVersionBasePath()));
   }



   /***************************************************************************
    ** For initial setup when server boots, set the qInstance - but also,
    ** e.g., for development, to do a hot-swap.
    ***************************************************************************/
   public void setQInstance(QInstance qInstance)
   {
      /////////////////////////////////////////////////////////////////////////////
      // extension routes can only be bound once - so after that (e.g., hot-swap) //
      // keep the loaded specs, and just give them the new instance               //
      /////////////////////////////////////////////////////////////////////////////
      if(!routesDefined)
      {
         loadExtensions(qInstance);
      }
      getAllEndpointSpecs().forEach(spec -> spec.setQInstance(qInstance));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public OpenAPI generateOpenAPIModel(String basePath) throws QException
   {
      return (generateOpenAPIModel(basePath, getAllEndpointSpecs(), true, getSemanticVersion()));
   }



   /*******************************************************************************
    ** The OpenAPI document of one extension alone (its paths and the component
    ** schemas its specs define) - what a qbit commits and contract-tests.
    *******************************************************************************/
   public OpenAPI generateExtensionOpenAPIModel(String basePath, MiddlewareSpecExtension extension) throws QException
   {
      try
      {
         List<AbstractEndpointSpec<?, ?, ?>> specs = new ArrayList<>();
         for(QCodeReference reference : extension.getSpecs())
         {
            specs.add(MiddlewareSpecExtensionsMetaData.instantiateSpec(reference));
         }
         OpenAPI openAPI = generateOpenAPIModel(basePath, specs, false, extension.getSemanticVersion(), spec -> extension.getName());
         openAPI.getInfo()
            .withTitle("QQQ Middleware API extension: " + extension.getName())
            .withDescription(extension.getDescription());
         return (openAPI);
      }
      catch(ReflectiveOperationException e)
      {
         throw (new QException("Error instantiating specs for extension " + extension.getName(), e));
      }
   }



   /*******************************************************************************
    ** Build the OpenAPI model for a list of specs.
    *******************************************************************************/
   private OpenAPI generateOpenAPIModel(String basePath, List<AbstractEndpointSpec<?, ?, ?>> list, boolean includeCoreComponents, String semanticVersion) throws QException
   {
      Map<Class<?>, String> extensionNameByClass = new HashMap<>();
      for(MiddlewareSpecExtension extension : extensions.values())
      {
         for(QCodeReference reference : CollectionUtils.nonNullList(extension.getSpecs()))
         {
            extensionNameByClass.put(reference.getName() == null ? null : classForName(reference.getName()), extension.getName());
         }
      }
      return (generateOpenAPIModel(basePath, list, includeCoreComponents, semanticVersion, spec -> extensionNameByClass.get(spec.getClass())));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static Class<?> classForName(String name)
   {
      try
      {
         return (Class.forName(name));
      }
      catch(ClassNotFoundException e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** Build the OpenAPI model for a list of specs.  extensionNameOf gives the
    ** extension a spec belongs to (null for core), which tags its operations.
    *******************************************************************************/
   private OpenAPI generateOpenAPIModel(String basePath, List<AbstractEndpointSpec<?, ?, ?>> list, boolean includeCoreComponents, String semanticVersion, Function<AbstractEndpointSpec<?, ?, ?>, String> extensionNameOf) throws QException
   {

      Map<String, Path>    paths             = new LinkedHashMap<>();
      Map<String, Example> componentExamples = new LinkedHashMap<>();

      Set<Class<?>>       componentClasses = new HashSet<>();
      Map<String, Schema> componentSchemas = new TreeMap<>();
      if(includeCoreComponents)
      {
         buildComponentSchemasFromComponentsPackage(componentSchemas, componentClasses);
      }

      ///////////////////////////////////////////////////////////////////////////////
      // the one error schema of the UI API: every document carries it, and every //
      // operation declares it for its standard error statuses (below)           //
      ///////////////////////////////////////////////////////////////////////////////
      componentSchemas.put(BasicErrorResponseV1.class.getSimpleName(), new BasicErrorResponseV1().toSchema());

      Map<String, String> tagDescriptions = new TreeMap<>();
      Set<String>         operationIds    = new HashSet<>();

      for(AbstractEndpointSpec<?, ?, ?> spec : list)
      {
         CompleteOperation completeOperation = spec.defineCompleteOperation();
         String            fullPath          = ("/" + basePath + getVersionBasePath() + completeOperation.getPath()).replaceAll("/+", "/");
         Path              path              = paths.computeIfAbsent(fullPath, (k) -> new Path());
         Method            method            = completeOperation.getMethod();

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if this spec is supposed to be secured, but no security has been applied, then add our default security //
         // - a session cookie, or a bearer token (an OAuth2 access token, or a session UUID); any one of them      //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(spec.isSecured() && method.getSecurity() == null)
         {
            method.withSecurity(UiApiSecuritySchemes.DEFAULT_SECURITY);
         }

         if(method.getOperationId() == null)
         {
            method.withOperationId(UiApiOperationIds.of(completeOperation.getHttpMethod(), completeOperation.getPath()));
         }
         if(!operationIds.add(method.getOperationId()))
         {
            throw (new QException("Duplicate operationId [" + method.getOperationId() + "] at " + completeOperation.getHttpMethod() + " " + fullPath + " - set a unique one with BasicOperation.withOperationId"));
         }

         ////////////////////////////////////////////////////////////////////////
         // extension operations are tagged with their extension's name;       //
         // core operations keep their TagsV1 tag                              //
         ////////////////////////////////////////////////////////////////////////
         String extensionName = extensionNameOf == null ? null : extensionNameOf.apply(spec);
         if(extensionName != null && CollectionUtils.nullSafeIsEmpty(method.getTags()))
         {
            method.withTag(extensionName);
         }
         for(String tag : CollectionUtils.nonNullList(method.getTags()))
         {
            tagDescriptions.putIfAbsent(tag, extensionName != null && tag.equals(extensionName) ? "Operations of the " + extensionName + " extension." : null);
         }

         addStandardErrorResponses(method, spec.isSecured());

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
            if(componentSchemas.containsKey(entry.getKey()) && !BasicErrorResponseV1.class.getSimpleName().equals(entry.getKey()))
            {
               LOG.warn("More than one endpoint spec defined a componentSchema named: " + entry.getKey() + ".  The last one encountered (from " + spec.getClass().getSimpleName() + ") will be used.");
            }

            if(!BasicErrorResponseV1.class.getSimpleName().equals(entry.getKey()))
            {
               componentSchemas.put(entry.getKey(), entry.getValue());
            }
         }
      }

      OpenAPI openAPI = new OpenAPI();
      openAPI.withInfo(new Info()
         .withVersion(semanticVersion)
         .withTitle("QQQ Middleware API")
         .withDescription(getDescription())
         .withContact(new Contact().withEmail("contact@kingsrook.com"))
      );

      openAPI.withPaths(paths);

      ////////////////////////////////////////////////////////////////////////////
      // paths are absolute (/qqq/v1/...), so the server is the origin that    //
      // served this document                                                  //
      ////////////////////////////////////////////////////////////////////////////
      openAPI.withServers(List.of(new Server().withUrl("/").withDescription("The QQQ server that serves this document (paths include /qqq/v1).")));

      List<Tag> tags = new ArrayList<>();
      for(Map.Entry<String, String> entry : tagDescriptions.entrySet())
      {
         tags.add(new Tag().withName(entry.getKey()).withDescription(entry.getValue()));
      }
      openAPI.withTags(tags);

      openAPI.withComponents(new Components()
         .withSchemas(componentSchemas)
         .withExamples(componentExamples)
         .withSecuritySchemes(UiApiSecuritySchemes.SCHEMES)
      );

      return openAPI;
   }



   /***************************************************************************
    ** Declare the standard error statuses (with BasicErrorResponseV1) that an
    ** operation does not already declare: 400, 404, 409, 429 always; 401 and
    ** 403 for secured operations.
    ***************************************************************************/
   private static void addStandardErrorResponses(Method method, boolean secured)
   {
      Map<Integer, Response> responses = method.getResponses() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(method.getResponses());
      method.withResponses(responses);

      Map<Integer, String> standard = new LinkedHashMap<>();
      standard.put(400, "Bad request: a parameter or the body is invalid (code BAD_REQUEST); fieldErrors may name the fields.");
      if(secured)
      {
         standard.put(401, "Not authenticated, or the session or token is invalid or expired (code UNAUTHENTICATED). Sign in again, or refresh the token.");
         standard.put(403, "The user (or the credential's scope) may not do this (code PERMISSION_DENIED).");
      }
      standard.put(404, "Not found, or not visible to this user (code NOT_FOUND).");
      standard.put(409, "Conflict with the current state, e.g. an edit conflict or an invalid transition (code CONFLICT).");
      standard.put(429, "Too many requests (code RATE_LIMITED); retry later.");

      for(Map.Entry<Integer, String> entry : standard.entrySet())
      {
         if(!responses.containsKey(entry.getKey()))
         {
            responses.put(entry.getKey(), new Response()
               .withDescription(entry.getValue())
               .withContent(new LinkedHashMap<>(Map.of("application/json", new Content().withSchema(new Schema().withRefToSchema(BasicErrorResponseV1.class.getSimpleName()))))));
         }
      }
   }



   /***************************************************************************
    ** The package whose components sub-package holds this version's component
    ** schemas (a subclass in another package, e.g. the API-aware version, keeps
    ** its parent's components).
    ***************************************************************************/
   protected String getComponentsBasePackage()
   {
      return (getClass().getPackageName());
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
         String         packageName      = getComponentsBasePackage();
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
         
         ## Client channel
         Every request should name its client in the `X-QQQ-Client` header: `channel[/detail]`, e.g. `next-ui`, `cli`,
         or `assistant/<turnId>` (the accepted channels are listed by `GET .../version`; an unknown one is refused with 400).
         The channel is recorded in audits.  It is attribution only: the actor is always the signed-in user, and a channel
         never grants anything - but some objects are denied to some channels (e.g. the AI assistant can never see its own
         configuration, identity and permission administration, audits, API keys or secrets), exactly as if the user had
         no permission for them.
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
