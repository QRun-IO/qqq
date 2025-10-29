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

package com.kingsrook.qqq.middleware.javalin;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
import com.kingsrook.qqq.backend.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import com.kingsrook.qqq.middleware.javalin.routeproviders.IsolatedSpaRouteProvider;
import com.kingsrook.qqq.middleware.javalin.routeproviders.ProcessBasedRouter;
import com.kingsrook.qqq.middleware.javalin.routeproviders.SimpleFileSystemDirectoryRouter;
import com.kingsrook.qqq.middleware.javalin.routeproviders.handlers.RouteProviderBeforeHandlerInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.handlers.RouteProviderAfterHandlerInterface;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.apache.commons.lang.BooleanUtils;
import org.eclipse.jetty.util.resource.Resource;


/*******************************************************************************
 ** Second-generation qqq javalin server.
 **
 ** An evolution over the original QJavalinImplementation, which both managed
 ** the javalin instance itself, but also provided all of the endpoint handlers...
 ** This class instead just configures & starts the server.
 **
 ** Makes several setters available, to let application-developer choose what
 ** standard qqq endpoints are served (e.g., frontend-material-dashboard, the
 ** legacy-unversioned middleware, newer versioned-middleware, and additional qqq
 ** modules or application-defined services (both provided as instances of
 ** QJavalinRouteProviderInterface).
 **
 ** System property `qqq.javalin.hotSwapInstance` (defaults to false), causes the
 ** QInstance to be re-loaded every X millis, to avoid some server restarts while
 ** doing dev.
 *******************************************************************************/
public class QApplicationJavalinServer
{
   private static final QLogger LOG = QLogger.getLogger(QApplicationJavalinServer.class);

   private final AbstractQQQApplication application;

   private Integer                              port                                     = 8000;
   private boolean                              serveFrontendMaterialDashboard           = true;
   private String                               frontendMaterialDashboardHostedPath      = "/";
   private boolean                              serveLegacyUnversionedMiddlewareAPI      = true;
   private List<AbstractMiddlewareVersion>      middlewareVersionList                    = List.of(new MiddlewareVersionV1());
   private List<QJavalinRouteProviderInterface> additionalRouteProviders                 = null;
   private Consumer<Javalin>                    javalinConfigurationCustomizer           = null;
   private QJavalinMetaData                     javalinMetaData                          = null;

   private long                lastQInstanceHotSwapMillis;
   private long                millisBetweenHotSwaps = 2500;
   private Consumer<QInstance> hotSwapCustomizer     = null;

   private Javalin service;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QApplicationJavalinServer(AbstractQQQApplication application)
   {
      this.application = application;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void start() throws QException
   {
      QInstance qInstance = application.defineValidatedQInstance();

      QJavalinMetaData javalinMetaData = getJavalinMetaDataToUse(qInstance);
      if(javalinMetaData != null)
      {
         addRouteProvidersFromMetaData(javalinMetaData);
      }

      service = Javalin.create(config ->
      {
         if(serveFrontendMaterialDashboard)
         {
            if(getClass().getResource("/material-dashboard/index.html") == null)
            {
               LOG.warn("/material-dashboard/index.html resource was not found.  This might happen if you're using a local (e.g., within-IDE) snapshot version... Try updating pom.xml to reference a released version of qfmd?");
            }

            ////////////////////////////////////////////////////////////////////////////////////////
            // If you have any assets to add to the web server (e.g., logos, icons) place them at //
            // src/main/resources/material-dashboard-overlay                                      //
            // we'll use the same check that javalin (jetty?) internally uses to see if this      //
            // directory exists - because if it doesn't, then it'll fail to start the server...   //
            // note that that Resource object is auto-closable, hence the try-with-resources      //
            ////////////////////////////////////////////////////////////////////////////////////////
            try(Resource resource = Resource.newClassPathResource("/material-dashboard-overlay"))
            {
               if(resource != null)
               {
                  config.staticFiles.add("/material-dashboard-overlay");
               }
            }

            ////////////////////////////////////////////////////////////////////////////////////
            // tell javalin where to find material-dashboard static web assets                //
            // in this case, this path is coming from the qqq-frontend-material-dashboard jar //
            ////////////////////////////////////////////////////////////////////////////////////
            config.staticFiles.add(staticFileConfig ->
            {
               staticFileConfig.hostedPath = frontendMaterialDashboardHostedPath;
               staticFileConfig.directory = "/material-dashboard";
            });

            ////////////////////////////////////////////////////////////
            // set the index page for the SPA from material dashboard //
            ////////////////////////////////////////////////////////////
            config.spaRoot.addFile(frontendMaterialDashboardHostedPath, "material-dashboard/index.html");
         }

         ///////////////////////////////////////////
         // add qqq routes to the javalin service //
         ///////////////////////////////////////////
         if(serveLegacyUnversionedMiddlewareAPI)
         {
            try
            {
               QJavalinImplementation qJavalinImplementation = new QJavalinImplementation(qInstance, javalinMetaData);
               config.router.apiBuilder(qJavalinImplementation.getRoutes());
            }
            catch(QInstanceValidationException e)
            {
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // we should be pretty comfortable that this won't happen, because we've pre-validated the instance above... //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
               throw new RuntimeException(e);
            }
         }

         /////////////////////////////////////
         // versioned qqq middleware routes //
         /////////////////////////////////////
         if(CollectionUtils.nullSafeHasContents(middlewareVersionList))
         {
            config.router.apiBuilder(new QMiddlewareApiSpecHandler(middlewareVersionList).defineJavalinEndpointGroup());
            for(AbstractMiddlewareVersion version : middlewareVersionList)
            {
               version.setQInstance(qInstance);
               config.router.apiBuilder(version.getJavalinEndpointGroup(qInstance));
            }
         }

         /////////////////////////////////////////////////////////////////////////
         // application API routes (from ApiInstanceMetaData in the QInstance) //
         /////////////////////////////////////////////////////////////////////////
         try
         {
            Class<?> apiContainerClass = Class.forName("com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer");
            Object   apiContainer      = apiContainerClass.getMethod("of", com.kingsrook.qqq.backend.core.model.metadata.QInstance.class).invoke(null, qInstance);
            
            if(apiContainer != null)
            {
               @SuppressWarnings("unchecked")
               java.util.Map<String, ?> apis = (java.util.Map<String, ?>) apiContainerClass.getMethod("getApis").invoke(apiContainer);
               
               if(apis != null && !apis.isEmpty())
               {
                  Class<?> apiHandlerClass = Class.forName("com.kingsrook.qqq.api.javalin.QJavalinApiHandler");
                  Object   apiHandler      = apiHandlerClass.getConstructor(com.kingsrook.qqq.backend.core.model.metadata.QInstance.class).newInstance(qInstance);
                  io.javalin.apibuilder.EndpointGroup routes = (io.javalin.apibuilder.EndpointGroup) apiHandlerClass.getMethod("getRoutes").invoke(apiHandler);
                  
                  config.router.apiBuilder(routes);
                  LOG.info("Registered application API routes from ApiInstanceMetaDataContainer");
               }
            }
         }
         catch(ClassNotFoundException e)
         {
            ///////////////////////////////////////////////////////////////////////////////
            // qqq-middleware-api module not on classpath - no application APIs to serve //
            ///////////////////////////////////////////////////////////////////////////////
            LOG.debug("qqq-middleware-api not available - skipping application API registration");
         }
         catch(Exception e)
         {
            LOG.warn("Error registering application API routes", e);
         }

         ////////////////////////////////////////////////////////////////////////////
         // additional route providers (e.g., application-apis, other middlewares) //
         ////////////////////////////////////////////////////////////////////////////
         for(QJavalinRouteProviderInterface routeProvider : CollectionUtils.nonNullList(additionalRouteProviders))
         {
            routeProvider.setQInstance(qInstance);

            EndpointGroup javalinEndpointGroup = routeProvider.getJavalinEndpointGroup();
            if(javalinEndpointGroup != null)
            {
               config.router.apiBuilder(javalinEndpointGroup);
            }

            routeProvider.acceptJavalinConfig(config);
         }
      });

      //////////////////////////////////////////////////////////////////////
      // also pass the javalin service into any additionalRouteProviders, //
      // in case they need additional setup, e.g., before/after handlers. //
      //////////////////////////////////////////////////////////////////////
      for(QJavalinRouteProviderInterface routeProvider : CollectionUtils.nonNullList(additionalRouteProviders))
      {
         routeProvider.acceptJavalinService(service);
      }

      //////////////////////////////////////////////////////////////////////////////////////
      // per system property, set the server to hot-swap the q instance before all routes //
      //////////////////////////////////////////////////////////////////////////////////////
      String hotSwapPropertyValue = System.getProperty("qqq.javalin.hotSwapInstance", "false");
      if(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(hotSwapPropertyValue)))
      {
         LOG.info("Server will hotSwap QInstance before requests every [" + millisBetweenHotSwaps + "] millis.");
         service.before(context -> hotSwapQInstance());
      }

      service.before((Context context) -> context.header("Content-Type", "application/json"));
      service.after(QJavalinImplementation::clearQContext);

      addNullResponseCharsetFixer();

      ////////////////////////////////////////////////
      // allow a configuration-customizer to be run //
      ////////////////////////////////////////////////
      if(javalinConfigurationCustomizer != null)
      {
         javalinConfigurationCustomizer.accept(service);
      }

      service.start(port);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private QJavalinMetaData getJavalinMetaDataToUse(QInstance qInstance)
   {
      if(this.javalinMetaData != null && QJavalinMetaData.of(qInstance) != null)
      {
         LOG.warn("JavalinMetaData is defined both in the QInstance and the QApplicationJavalinServer.  The one from the QInstance will be ignored - the one from the QJavalinApplicationServer will be used.");
         return (this.javalinMetaData);
      }
      else if (this.javalinMetaData != null)
      {
         return (this.javalinMetaData);
      }
      else
      {
         return QJavalinMetaData.of(qInstance);
      }
   }



   /***************************************************************************
    ** initial tests with the SimpleFileSystemDirectoryRouter would sometimes
    ** have a Content-Type:text/html;charset=null !
    ** which doesn't seem ever valid (and at least it broke our unit test).
    ** so, if w see charset=null in contentType, replace it with the system
    ** default, which may not be 100% right, but has to be better than "null"...
    ***************************************************************************/
   private void addNullResponseCharsetFixer()
   {
      service.after((Context context) ->
      {
         String contentType = context.res().getContentType();
         if(contentType != null && contentType.contains("charset=null"))
         {
            contentType = contentType.replace("charset=null", "charset=" + Charset.defaultCharset().name());
            context.res().setContentType(contentType);
         }
      });
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void addRouteProvidersFromMetaData(QJavalinMetaData qJavalinMetaData) throws QException
   {
      if(qJavalinMetaData == null)
      {
         return;
      }

      for(JavalinRouteProviderMetaData routeProviderMetaData : CollectionUtils.nonNullList(qJavalinMetaData.getRouteProviders()))
      {
         if(StringUtils.hasContent(routeProviderMetaData.getSpaPath()) && StringUtils.hasContent(routeProviderMetaData.getStaticFilesPath()))
         {
            // IsolatedSpaRouteProvider configuration
            IsolatedSpaRouteProvider spaProvider = new IsolatedSpaRouteProvider(
               routeProviderMetaData.getSpaPath(),
               routeProviderMetaData.getStaticFilesPath()
            );
            
            if(StringUtils.hasContent(routeProviderMetaData.getSpaIndexFile()))
            {
               spaProvider.withSpaIndexFile(routeProviderMetaData.getSpaIndexFile());
            }
            
            if(routeProviderMetaData.getExcludedPaths() != null && !routeProviderMetaData.getExcludedPaths().isEmpty())
            {
               spaProvider.withExcludedPaths(routeProviderMetaData.getExcludedPaths());
            }
            
            spaProvider.withDeepLinking(routeProviderMetaData.getEnableDeepLinking());
            spaProvider.withLoadFromJar(routeProviderMetaData.getLoadFromJar());
            
            if(routeProviderMetaData.getRouteAuthenticator() != null)
            {
               spaProvider.withAuthenticator(routeProviderMetaData.getRouteAuthenticator());
            }
            
            // Add before handlers from metadata
            if(routeProviderMetaData.getBeforeHandlers() != null && !routeProviderMetaData.getBeforeHandlers().isEmpty())
            {
               for(com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference handlerRef : routeProviderMetaData.getBeforeHandlers())
               {
                  try
                  {
                     RouteProviderBeforeHandlerInterface handler = QCodeLoader.getAdHoc(RouteProviderBeforeHandlerInterface.class, handlerRef);
                     spaProvider.withBeforeHandler(handler);
                  }
                  catch(Exception e)
                  {
                     LOG.error("Error loading before handler for route provider", e, 
                        com.kingsrook.qqq.backend.core.logging.LogUtils.logPair("routeProvider", routeProviderMetaData.getName()),
                        com.kingsrook.qqq.backend.core.logging.LogUtils.logPair("handler", handlerRef.toString()));
                     throw new QException("Error loading before handler: " + e.getMessage(), e);
                  }
               }
            }
            
            // Add after handlers from metadata
            if(routeProviderMetaData.getAfterHandlers() != null && !routeProviderMetaData.getAfterHandlers().isEmpty())
            {
               for(com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference handlerRef : routeProviderMetaData.getAfterHandlers())
               {
                  try
                  {
                     RouteProviderAfterHandlerInterface handler = QCodeLoader.getAdHoc(RouteProviderAfterHandlerInterface.class, handlerRef);
                     spaProvider.withAfterHandler(handler);
                  }
                  catch(Exception e)
                  {
                     LOG.error("Error loading after handler for route provider", e,
                        com.kingsrook.qqq.backend.core.logging.LogUtils.logPair("routeProvider", routeProviderMetaData.getName()),
                        com.kingsrook.qqq.backend.core.logging.LogUtils.logPair("handler", handlerRef.toString()));
                     throw new QException("Error loading after handler: " + e.getMessage(), e);
                  }
               }
            }
            
            withAdditionalRouteProvider(spaProvider);
         }
         else if(StringUtils.hasContent(routeProviderMetaData.getProcessName()) && StringUtils.hasContent(routeProviderMetaData.getHostedPath()))
         {
            withAdditionalRouteProvider(new ProcessBasedRouter(routeProviderMetaData));
         }
         else if(StringUtils.hasContent(routeProviderMetaData.getFileSystemPath()) && StringUtils.hasContent(routeProviderMetaData.getHostedPath()))
         {
            withAdditionalRouteProvider(new SimpleFileSystemDirectoryRouter(routeProviderMetaData));
         }
         else
         {
            throw (new QException("Error processing route provider - does not have sufficient fields set."));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void stop()
   {
      if(this.service == null)
      {
         LOG.info("Stop called, but there is no javalin service, so noop.");
         return;
      }

      this.service.stop();
   }



   /*******************************************************************************
    ** If there's a qInstanceHotSwapSupplier, and its been a little while, replace
    ** the qInstance with a new one from the supplier.  Meant to be used while doing
    ** development.
    *******************************************************************************/
   public void hotSwapQInstance()
   {
      long now = System.currentTimeMillis();
      if(now - lastQInstanceHotSwapMillis < millisBetweenHotSwaps)
      {
         return;
      }

      lastQInstanceHotSwapMillis = now;

      try
      {
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // clear the cache of classes in this class, so that new classes can be found if a meta-data-producer is being used //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         ClassPathUtils.clearTopLevelClassCache();

         ///////////////////////////////////////////////////////////////
         // try to get a new, validated instance from the application //
         ///////////////////////////////////////////////////////////////
         QInstance newQInstance = application.defineValidatedQInstance();
         if(newQInstance == null)
         {
            LOG.warn("Got a null qInstance from the application.defineQInstance().  Not hot-swapping.");
            return;
         }

         ////////////////////////////////////////
         // allow a hot-swap customizer to run //
         ////////////////////////////////////////
         if(hotSwapCustomizer != null)
         {
            hotSwapCustomizer.accept(newQInstance);
         }

         ///////////////////////////////////////////////////////////////////////
         // pass the new qInstance into all of the objects serving qqq routes //
         ///////////////////////////////////////////////////////////////////////
         if(serveLegacyUnversionedMiddlewareAPI)
         {
            QJavalinImplementation.setQInstance(newQInstance);
         }

         if(CollectionUtils.nullSafeHasContents(middlewareVersionList))
         {
            for(AbstractMiddlewareVersion spec : CollectionUtils.nonNullList(middlewareVersionList))
            {
               spec.setQInstance(newQInstance);
            }
         }

         for(QJavalinRouteProviderInterface routeProvider : CollectionUtils.nonNullList(additionalRouteProviders))
         {
            routeProvider.setQInstance(newQInstance);
         }

         LOG.info("Swapped qInstance");
      }
      catch(QInstanceValidationException e)
      {
         LOG.error("Validation Error while hot-swapping QInstance", e);
      }
      catch(Exception e)
      {
         LOG.error("Error hot-swapping QInstance", e);
      }
   }



   /*******************************************************************************
    ** Getter for port
    *******************************************************************************/
   public Integer getPort()
   {
      return (this.port);
   }



   /*******************************************************************************
    ** Setter for port
    *******************************************************************************/
   public void setPort(Integer port)
   {
      this.port = port;
   }



   /*******************************************************************************
    ** Fluent setter for port
    *******************************************************************************/
   public QApplicationJavalinServer withPort(Integer port)
   {
      this.port = port;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setMillisBetweenHotSwaps(long millisBetweenHotSwaps)
   {
      this.millisBetweenHotSwaps = millisBetweenHotSwaps;
   }



   /*******************************************************************************
    ** Getter for serveFrontendMaterialDashboard
    *******************************************************************************/
   public boolean getServeFrontendMaterialDashboard()
   {
      return (this.serveFrontendMaterialDashboard);
   }



   /*******************************************************************************
    ** Setter for serveFrontendMaterialDashboard
    *******************************************************************************/
   public void setServeFrontendMaterialDashboard(boolean serveFrontendMaterialDashboard)
   {
      this.serveFrontendMaterialDashboard = serveFrontendMaterialDashboard;
   }



   /*******************************************************************************
    ** Fluent setter for serveFrontendMaterialDashboard
    *******************************************************************************/
   public QApplicationJavalinServer withServeFrontendMaterialDashboard(boolean serveFrontendMaterialDashboard)
   {
      this.serveFrontendMaterialDashboard = serveFrontendMaterialDashboard;
      return (this);
   }



   /*******************************************************************************
    ** Getter for frontendMaterialDashboardHostedPath
    *******************************************************************************/
   public String getFrontendMaterialDashboardHostedPath()
   {
      return (this.frontendMaterialDashboardHostedPath);
   }



   /*******************************************************************************
    ** Setter for frontendMaterialDashboardHostedPath
    *******************************************************************************/
   public void setFrontendMaterialDashboardHostedPath(String frontendMaterialDashboardHostedPath)
   {
      this.frontendMaterialDashboardHostedPath = frontendMaterialDashboardHostedPath;
   }



   /*******************************************************************************
    ** Fluent setter for frontendMaterialDashboardHostedPath
    *******************************************************************************/
   public QApplicationJavalinServer withFrontendMaterialDashboardHostedPath(String frontendMaterialDashboardHostedPath)
   {
      this.frontendMaterialDashboardHostedPath = frontendMaterialDashboardHostedPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for serveLegacyUnversionedMiddlewareAPI
    *******************************************************************************/
   public boolean getServeLegacyUnversionedMiddlewareAPI()
   {
      return (this.serveLegacyUnversionedMiddlewareAPI);
   }



   /*******************************************************************************
    ** Setter for serveLegacyUnversionedMiddlewareAPI
    *******************************************************************************/
   public void setServeLegacyUnversionedMiddlewareAPI(boolean serveLegacyUnversionedMiddlewareAPI)
   {
      this.serveLegacyUnversionedMiddlewareAPI = serveLegacyUnversionedMiddlewareAPI;
   }



   /*******************************************************************************
    ** Fluent setter for serveLegacyUnversionedMiddlewareAPI
    *******************************************************************************/
   public QApplicationJavalinServer withServeLegacyUnversionedMiddlewareAPI(boolean serveLegacyUnversionedMiddlewareAPI)
   {
      this.serveLegacyUnversionedMiddlewareAPI = serveLegacyUnversionedMiddlewareAPI;
      return (this);
   }



   /*******************************************************************************
    ** Getter for middlewareVersionList
    *******************************************************************************/
   public List<AbstractMiddlewareVersion> getMiddlewareVersionList()
   {
      return (this.middlewareVersionList);
   }



   /*******************************************************************************
    ** Setter for middlewareVersionList
    *******************************************************************************/
   public void setMiddlewareVersionList(List<AbstractMiddlewareVersion> middlewareVersionList)
   {
      this.middlewareVersionList = middlewareVersionList;
   }



   /*******************************************************************************
    ** Fluent setter for middlewareVersionList
    *******************************************************************************/
   public QApplicationJavalinServer withMiddlewareVersionList(List<AbstractMiddlewareVersion> middlewareVersionList)
   {
      this.middlewareVersionList = middlewareVersionList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for additionalRouteProviders
    *******************************************************************************/
   public List<QJavalinRouteProviderInterface> getAdditionalRouteProviders()
   {
      return (this.additionalRouteProviders);
   }



   /*******************************************************************************
    ** Setter for additionalRouteProviders
    *******************************************************************************/
   public void setAdditionalRouteProviders(List<QJavalinRouteProviderInterface> additionalRouteProviders)
   {
      this.additionalRouteProviders = additionalRouteProviders;
   }



   /*******************************************************************************
    ** Fluent setter for additionalRouteProviders
    *******************************************************************************/
   public QApplicationJavalinServer withAdditionalRouteProviders(List<QJavalinRouteProviderInterface> additionalRouteProviders)
   {
      this.additionalRouteProviders = additionalRouteProviders;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single additionalRouteProvider
    *******************************************************************************/
   public QApplicationJavalinServer withAdditionalRouteProvider(QJavalinRouteProviderInterface additionalRouteProvider)
   {
      if(this.additionalRouteProviders == null)
      {
         this.additionalRouteProviders = new ArrayList<>();
      }
      this.additionalRouteProviders.add(additionalRouteProvider);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add an IsolatedSpaRouteProvider
    *******************************************************************************/
   public QApplicationJavalinServer withIsolatedSpaRouteProvider(String spaPath, String staticFilesPath)
   {
      return withAdditionalRouteProvider(new IsolatedSpaRouteProvider(spaPath, staticFilesPath));
   }



   /*******************************************************************************
    ** Fluent setter to add an IsolatedSpaRouteProvider with full configuration
    *******************************************************************************/
   public QApplicationJavalinServer withIsolatedSpaRouteProvider(String spaPath, String staticFilesPath, String spaIndexFile)
   {
      return withAdditionalRouteProvider(new IsolatedSpaRouteProvider(spaPath, staticFilesPath)
         .withSpaIndexFile(spaIndexFile));
   }



   /*******************************************************************************
    ** Getter for MILLIS_BETWEEN_HOT_SWAPS
    *******************************************************************************/
   public long getMillisBetweenHotSwaps()
   {
      return (millisBetweenHotSwaps);
   }



   /*******************************************************************************
    ** Fluent setter for MILLIS_BETWEEN_HOT_SWAPS
    *******************************************************************************/
   public QApplicationJavalinServer withMillisBetweenHotSwaps(long millisBetweenHotSwaps)
   {
      this.millisBetweenHotSwaps = millisBetweenHotSwaps;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hotSwapCustomizer
    *******************************************************************************/
   public Consumer<QInstance> getHotSwapCustomizer()
   {
      return (this.hotSwapCustomizer);
   }



   /*******************************************************************************
    ** Setter for hotSwapCustomizer
    *******************************************************************************/
   public void setHotSwapCustomizer(Consumer<QInstance> hotSwapCustomizer)
   {
      this.hotSwapCustomizer = hotSwapCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for hotSwapCustomizer
    *******************************************************************************/
   public QApplicationJavalinServer withHotSwapCustomizer(Consumer<QInstance> hotSwapCustomizer)
   {
      this.hotSwapCustomizer = hotSwapCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for javalinConfigurationCustomizer
    *******************************************************************************/
   public Consumer<Javalin> getJavalinConfigurationCustomizer()
   {
      return (this.javalinConfigurationCustomizer);
   }



   /*******************************************************************************
    ** Setter for javalinConfigurationCustomizer
    *******************************************************************************/
   public void setJavalinConfigurationCustomizer(Consumer<Javalin> javalinConfigurationCustomizer)
   {
      this.javalinConfigurationCustomizer = javalinConfigurationCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for javalinConfigurationCustomizer
    *******************************************************************************/
   public QApplicationJavalinServer withJavalinConfigurationCustomizer(Consumer<Javalin> javalinConfigurationCustomizer)
   {
      this.javalinConfigurationCustomizer = javalinConfigurationCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for javalinMetaData
    *******************************************************************************/
   public QJavalinMetaData getJavalinMetaData()
   {
      return (this.javalinMetaData);
   }



   /*******************************************************************************
    ** Setter for javalinMetaData
    *******************************************************************************/
   public void setJavalinMetaData(QJavalinMetaData javalinMetaData)
   {
      this.javalinMetaData = javalinMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for javalinMetaData
    *******************************************************************************/
   public QApplicationJavalinServer withJavalinMetaData(QJavalinMetaData javalinMetaData)
   {
      this.javalinMetaData = javalinMetaData;
      return (this);
   }

}
