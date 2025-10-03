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

package com.kingsrook.qqq.backend.javalin;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.actions.metadata.ProcessMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionCheckResult;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.adapters.QInstanceAdapter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendVariant;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeConsumer;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import com.kingsrook.qqq.middleware.javalin.misc.DownloadFileSupplementalAction;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static com.kingsrook.qqq.backend.javalin.QJavalinAccessLogger.logPairIfSlow;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;


/*******************************************************************************
 ** QQQ Javalin implementation.  Given a QInstance, defines all routes needed
 ** to respond to http requests and route down into the qqq backend.
 **
 *******************************************************************************/
public class QJavalinImplementation
{
   private static final QLogger LOG = QLogger.getLogger(QJavalinImplementation.class);

   public static final int    SESSION_COOKIE_AGE       = 60 * 60 * 24;
   public static final String SESSION_ID_COOKIE_NAME   = "sessionId";
   public static final String SESSION_UUID_COOKIE_NAME = "sessionUUID";
   public static final String API_KEY_NAME             = "apiKey";

   static QInstance        qInstance;
   static QJavalinMetaData javalinMetaData;

   private static Supplier<QInstance> qInstanceHotSwapSupplier;
   private static long                lastQInstanceHotSwapMillis;
   private static AtomicBoolean       insideHotSwap = new AtomicBoolean(false);

   private static      long MILLIS_BETWEEN_HOT_SWAPS = 2500;
   public static final long SLOW_LOG_THRESHOLD_MS    = 1000;

   private static final Integer DEFAULT_COUNT_TIMEOUT_SECONDS = 60;
   private static final Integer DEFAULT_QUERY_TIMEOUT_SECONDS = 60;

   private static int DEFAULT_PORT = 8001;

   private static Javalin             service;
   private static List<EndpointGroup> endpointGroups;

   private static long startTime = 0;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args) throws QInstanceValidationException
   {
      QInstance qInstance = new QInstance();
      // todo - parse args to look up metaData and prime instance
      // qInstance.addBackend(QMetaDataProvider.getQBackend());

      new QJavalinImplementation(qInstance).startJavalinServer(DEFAULT_PORT);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QJavalinImplementation(QInstance qInstance) throws QInstanceValidationException
   {
      this(qInstance, QJavalinMetaData.ofOrWithNew(qInstance));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QJavalinImplementation(QInstance qInstance, QJavalinMetaData javalinMetaData) throws QInstanceValidationException
   {
      QJavalinImplementation.qInstance = qInstance;
      QJavalinImplementation.javalinMetaData = javalinMetaData;
      new QInstanceValidator().validate(qInstance);
      startTime = System.currentTimeMillis();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QJavalinImplementation(String qInstanceFilePath) throws IOException
   {
      LOG.info("Loading qInstance from file (assuming json): " + qInstanceFilePath);
      String qInstanceJson = FileUtils.readFileToString(new File(qInstanceFilePath), StandardCharsets.UTF_8);
      QJavalinImplementation.qInstance = new QInstanceAdapter().jsonToQInstanceIncludingBackends(qInstanceJson);
      QJavalinImplementation.javalinMetaData = new QJavalinMetaData();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void startJavalinServer(int port)
   {
      // todo port from arg
      // todo base path from arg? - and then potentially multiple instances too (chosen based on the root path??)

      service = Javalin.create(config ->
         {
            config.router.apiBuilder(getRoutes());

            for(EndpointGroup endpointGroup : CollectionUtils.nonNullList(endpointGroups))
            {
               config.router.apiBuilder(endpointGroup);
            }
         }
      ).start(port);

      service.before(QJavalinImplementation::hotSwapQInstance);
      service.before((Context context) -> context.header("Content-Type", "application/json"));
      service.after(QJavalinImplementation::clearQContext);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Javalin getJavalinService()
   {
      return (service);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void clearQContext(Context context)
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** If there's a qInstanceHotSwapSupplier, and its been a little while, replace
    ** the qInstance with a new one from the supplier.  Meant to be used while doing
    ** development.
    *******************************************************************************/
   public static void hotSwapQInstance(Context context)
   {
      if(qInstanceHotSwapSupplier != null)
      {
         long now = System.currentTimeMillis();
         if(now - lastQInstanceHotSwapMillis < MILLIS_BETWEEN_HOT_SWAPS)
         {
            return;
         }

         if(!insideHotSwap.compareAndSet(false, true))
         {
            ///////////////////////////////////////////////
            // someone else is already running a hotswap //
            ///////////////////////////////////////////////
            LOG.debug("Caught attempted parallel hotswap - exiting this one.");
            return;
         }

         lastQInstanceHotSwapMillis = now;

         try
         {
            ////////////////////////////////////////////////////////////////////////////////
            // clear the cache of classes in this class, so that new classes can be found //
            ////////////////////////////////////////////////////////////////////////////////
            ClassPathUtils.clearTopLevelClassCache();

            /////////////////////////////////////////////////
            // try to get a new instance from the supplier //
            /////////////////////////////////////////////////
            QInstance newQInstance = qInstanceHotSwapSupplier.get();
            if(newQInstance == null)
            {
               LOG.warn("Got a null qInstance from hotSwapSupplier.  Not hot-swapping.");
               return;
            }

            ///////////////////////////////////////////////////////////////////////////////////
            // validate the instance, and only if it passes, then set it in our static field //
            ///////////////////////////////////////////////////////////////////////////////////
            new QInstanceValidator().validate(newQInstance);
            QJavalinImplementation.qInstance = newQInstance;
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
         finally
         {
            insideHotSwap.set(false);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void stopJavalinServer()
   {
      service.stop();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void setDefaultPort(int port)
   {
      QJavalinImplementation.DEFAULT_PORT = port;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public EndpointGroup getRoutes()
   {
      return (() ->
      {
         post("/manageSession", QJavalinImplementation::manageSession);

         /////////////////////
         // metadata routes //
         /////////////////////
         path("/metaData", () ->
         {
            get("/", QJavalinImplementation::metaData);
            path("/table/{table}", () ->
            {
               get("", QJavalinImplementation::tableMetaData);
            });
            path("/process/{processName}", () ->
            {
               get("", QJavalinImplementation::processMetaData);
            });
            get("/authentication", QJavalinImplementation::authenticationMetaData);
         });

         /////////////////////////
         // table (data) routes //
         /////////////////////////
         path("/data/{table}", () ->
         {
            get("/", QJavalinImplementation::dataQuery);
            post("/query", QJavalinImplementation::dataQuery);
            post("/", QJavalinImplementation::dataInsert);
            get("/count", QJavalinImplementation::dataCount);
            post("/count", QJavalinImplementation::dataCount);
            get("/variants", QJavalinImplementation::variants);
            get("/export", QJavalinImplementation::dataExportWithoutFilename);
            post("/export", QJavalinImplementation::dataExportWithoutFilename);
            get("/export/{filename}", QJavalinImplementation::dataExportWithFilename);
            post("/export/{filename}", QJavalinImplementation::dataExportWithFilename);
            get("/possibleValues/{fieldName}", QJavalinImplementation::possibleValuesForTableField);
            post("/possibleValues/{fieldName}", QJavalinImplementation::possibleValuesForTableField);

            // todo - add put and/or patch at this level (without a primaryKey) to do a bulk update based on primaryKeys in the records.
            path("/{primaryKey}", () ->
            {
               get("", QJavalinImplementation::dataGet);
               patch("", QJavalinImplementation::dataUpdate);
               put("", QJavalinImplementation::dataUpdate); // todo - want different semantics??
               delete("", QJavalinImplementation::dataDelete);

               get("/{fieldName}/{filename}", QJavalinImplementation::dataDownloadRecordField);
               post("/{fieldName}/{filename}", QJavalinImplementation::dataDownloadRecordField);

               QJavalinScriptsHandler.defineRecordRoutes();
            });
         });

         get("/possibleValues/{possibleValueSourceName}", QJavalinImplementation::possibleValuesStandalone);
         post("/possibleValues/{possibleValueSourceName}", QJavalinImplementation::possibleValuesStandalone);

         get("/widget/{name}", QJavalinImplementation::widget); // todo - can we just do a slow log here?
         post("/widget/{name}", QJavalinImplementation::widget);

         get("/serverInfo", QJavalinImplementation::serverInfo);

         ////////////////////
         // process routes //
         ////////////////////
         path("", QJavalinProcessHandler.getRoutes());
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void manageSession(Context context)
   {
      try
      {
         Map<?, ?> map = context.bodyAsClass(Map.class);

         QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();
         QAuthenticationModuleInterface  authenticationModule            = qAuthenticationModuleDispatcher.getQModule(qInstance.getAuthentication());

         Map<String, String> authContext = new HashMap<>();
         authContext.put(Auth0AuthenticationModule.DO_STORE_USER_SESSION_KEY, "true");

         //////////////////////////////////////////////////////////////////////////////////////////////////////////
         // before this code iterated the map, it had zombied uuid line, and only actually used ACCESS_TOKEN_KEY //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////
         //? authContext.put("uuid", ValueUtils.getValueAsString(map.get("uuid")));
         // authContext.put(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, ValueUtils.getValueAsString(map.get("accessToken")));
         /////////////////////////////////////////////////////////////////
         // todo - have the auth module declare what values it expects? //
         /////////////////////////////////////////////////////////////////
         for(Map.Entry<?, ?> entry : map.entrySet())
         {
            authContext.put(ValueUtils.getValueAsString(entry.getKey()), ValueUtils.getValueAsString(entry.getValue()));
         }

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // put the qInstance into context - but no session yet (since, the whole point of this call is to manage the session!) //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QContext.init(qInstance, null);
         QSession session = authenticationModule.createSession(qInstance, authContext);

         context.cookie(SESSION_UUID_COOKIE_NAME, session.getUuid(), SESSION_COOKIE_AGE);

         Map<String, Serializable> resultMap = new HashMap<>();
         resultMap.put("uuid", session.getUuid());

         if(session.getValuesForFrontend() != null)
         {
            LinkedHashMap<String, Serializable> valuesForFrontend = new LinkedHashMap<>(session.getValuesForFrontend());
            resultMap.put("values", valuesForFrontend);
         }

         context.result(JsonUtils.toJson(resultMap));
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void serverInfo(Context context)
   {
      JSONObject serverInfo = new JSONObject();
      serverInfo.put("startTimeMillis", startTime);
      serverInfo.put("startTimeHuman", Instant.ofEpochMilli(startTime));

      long uptime = System.currentTimeMillis() - startTime;
      serverInfo.put("uptimeMillis", uptime);
      serverInfo.put("uptimeHuman", Duration.ofMillis(uptime));

      serverInfo.put("buildId", System.getProperty("buildId", "Unspecified"));

      context.result(serverInfo.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void authenticationMetaData(Context context)
   {
      context.result(JsonUtils.toJson(qInstance.getAuthentication()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QSession setupSession(Context context, AbstractActionInput input) throws QModuleDispatchException, QAuthenticationException
   {
      QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();
      QAuthenticationModuleInterface  authenticationModule            = qAuthenticationModuleDispatcher.getQModule(qInstance.getAuthentication());

      try
      {
         ///////////////////////////////////////////////
         // note:  duplicated in ExecutorSessionUtils //
         ///////////////////////////////////////////////
         Map<String, String> authenticationContext = new HashMap<>();

         String sessionIdCookieValue     = context.cookie(SESSION_ID_COOKIE_NAME);
         String sessionUuidCookieValue   = context.cookie(Auth0AuthenticationModule.SESSION_UUID_KEY);
         String authorizationHeaderValue = context.header("Authorization");
         String apiKeyHeaderValue        = context.header("x-api-key");
         String codeQueryParamValue      = context.queryParam("code");
         String stateQueryParamValue     = context.queryParam("state");

         if(StringUtils.hasContent(codeQueryParamValue) && StringUtils.hasContent(stateQueryParamValue))
         {
            authenticationContext.put("code", codeQueryParamValue);
            authenticationContext.put("state", stateQueryParamValue);
         }
         else if(StringUtils.hasContent(sessionIdCookieValue))
         {
            ///////////////////////////////////////////////////////
            // sessionId - maybe used by table-based auth module //
            ///////////////////////////////////////////////////////
            authenticationContext.put(SESSION_ID_COOKIE_NAME, sessionIdCookieValue);
         }
         else if(StringUtils.hasContent(sessionUuidCookieValue))
         {
            ///////////////////////////////////////////////////////////////////////////
            // session UUID - known to be used by auth0 module (in aug. 2023 update) //
            ///////////////////////////////////////////////////////////////////////////
            authenticationContext.put(Auth0AuthenticationModule.SESSION_UUID_KEY, sessionUuidCookieValue);
         }
         else if(apiKeyHeaderValue != null)
         {
            /////////////////////////////////////////////////////////////////
            // next, look for an api key header:                           //
            // this will be used to look up auth0 values via an auth table //
            /////////////////////////////////////////////////////////////////
            authenticationContext.put(API_KEY_NAME, apiKeyHeaderValue);
         }
         else if(authorizationHeaderValue != null)
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////
            // second, look for the authorization header:                                                  //
            // either with a "Basic " prefix (for a username:password pair)                                //
            // or with a "Bearer " prefix (for a token that can be handled the same as a sessionId cookie) //
            /////////////////////////////////////////////////////////////////////////////////////////////////
            processAuthorizationValue(authenticationContext, authorizationHeaderValue);
         }
         else
         {
            try
            {
               String authorizationFormValue = context.formParam("Authorization");
               if(StringUtils.hasContent(authorizationFormValue))
               {
                  processAuthorizationValue(authenticationContext, authorizationFormValue);
               }
            }
            catch(Exception e)
            {
               LOG.info("Exception looking for Authorization formParam", e);
            }
         }

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // put the qInstance into context - but no session yet (since, the whole point of this call is to setup the session!) //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QContext.init(qInstance, null);
         QSession session = authenticationModule.createSession(qInstance, authenticationContext);
         QContext.init(qInstance, session, null, input);

         String tableVariant = QJavalinUtils.getFormParamOrQueryParam(context, "tableVariant");
         if(StringUtils.hasContent(tableVariant))
         {
            JSONObject variant = new JSONObject(tableVariant);
            QContext.getQSession().setBackendVariants(MapBuilder.of(variant.getString("type"), variant.getInt("id")));
         }

         /////////////////////////////////////////////////////////////////////////////////
         // if we got a session id cookie in, then send it back with updated cookie age //
         /////////////////////////////////////////////////////////////////////////////////
         if(authenticationModule.usesSessionIdCookie())
         {
            context.cookie(SESSION_ID_COOKIE_NAME, session.getUuid(), SESSION_COOKIE_AGE);
         }

         setUserTimezoneOffsetMinutesInSession(context, session);
         setUserTimezoneInSession(context, session);

         return (session);
      }
      catch(QAuthenticationException qae)
      {
         ////////////////////////////////////////////////////////////////////////////////
         // if exception caught, clear out the cookie so the frontend will reauthorize //
         ////////////////////////////////////////////////////////////////////////////////
         if(authenticationModule.usesSessionIdCookie())
         {
            context.removeCookie(SESSION_ID_COOKIE_NAME);
         }

         throw (qae);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processAuthorizationValue(Map<String, String> authenticationContext, String authorizationHeaderValue)
   {
      String basicPrefix  = "Basic ";
      String bearerPrefix = "Bearer ";
      if(authorizationHeaderValue.startsWith(basicPrefix))
      {
         authorizationHeaderValue = authorizationHeaderValue.replaceFirst(basicPrefix, "");
         authenticationContext.put(Auth0AuthenticationModule.BASIC_AUTH_KEY, authorizationHeaderValue);
      }
      else if(authorizationHeaderValue.startsWith(bearerPrefix))
      {
         authorizationHeaderValue = authorizationHeaderValue.replaceFirst(bearerPrefix, "");
         authenticationContext.put(Auth0AuthenticationModule.ACCESS_TOKEN_KEY, authorizationHeaderValue);
      }
      else
      {
         LOG.debug("Authorization value did not have Basic or Bearer prefix. [" + authorizationHeaderValue + "]");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setUserTimezoneOffsetMinutesInSession(Context context, QSession session)
   {
      String userTimezoneOffsetMinutes = context.header("X-QQQ-UserTimezoneOffsetMinutes");
      if(StringUtils.hasContent(userTimezoneOffsetMinutes))
      {
         try
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // even though we're putting it in the session as a string, go through parse int, to make sure it's a valid int. //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            session.setValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES, String.valueOf(Integer.parseInt(userTimezoneOffsetMinutes)));
         }
         catch(Exception e)
         {
            LOG.debug("Received non-integer value for X-QQQ-UserTimezoneOffsetMinutes header: " + userTimezoneOffsetMinutes);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setUserTimezoneInSession(Context context, QSession session)
   {
      String userTimezone = context.header("X-QQQ-UserTimezone");
      if(StringUtils.hasContent(userTimezone))
      {
         session.setValue(QSession.VALUE_KEY_USER_TIMEZONE, userTimezone);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void dataDelete(Context context)
   {
      String table      = context.pathParam("table");
      String primaryKey = context.pathParam("primaryKey");

      try
      {
         List<Serializable> primaryKeys = new ArrayList<>();
         primaryKeys.add(primaryKey);

         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setInputSource(QInputSource.USER);
         setupSession(context, deleteInput);

         QJavalinAccessLogger.logStart("delete", logPair("table", table), logPair("primaryKey", primaryKey));

         deleteInput.setTableName(table);
         deleteInput.setPrimaryKeys(primaryKeys);

         PermissionsHelper.checkTablePermissionThrowing(deleteInput, TablePermissionSubType.DELETE);

         DeleteAction deleteAction = new DeleteAction();
         DeleteOutput deleteResult = deleteAction.execute(deleteInput);

         QJavalinAccessLogger.logEndSuccess();
         context.result(JsonUtils.toJson(deleteResult));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void dataUpdate(Context context)
   {
      String tableName  = context.pathParam("table");
      String primaryKey = context.pathParam("primaryKey");

      try
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setInputSource(QInputSource.USER);
         setupSession(context, updateInput);
         updateInput.setTableName(tableName);

         PermissionsHelper.checkTablePermissionThrowing(updateInput, TablePermissionSubType.EDIT);
         QTableMetaData tableMetaData = qInstance.getTable(tableName);

         QJavalinAccessLogger.logStart("update", logPair("table", tableName), logPair("primaryKey", primaryKey));

         List<QRecord> recordList = new ArrayList<>();
         QRecord       record     = new QRecord();
         record.setTableName(tableName);
         recordList.add(record);
         updateInput.setRecords(recordList);

         record.setValue(tableMetaData.getPrimaryKeyField(), primaryKey);
         setRecordValuesForInsertOrUpdate(context, tableMetaData, record);

         UpdateAction updateAction = new UpdateAction();
         UpdateOutput updateOutput = updateAction.execute(updateInput);
         QRecord      outputRecord = updateOutput.getRecords().get(0);

         if(CollectionUtils.nullSafeHasContents(outputRecord.getErrors()))
         {
            throw (new QUserFacingException("Error updating " + tableMetaData.getLabel() + ": " + joinErrorsWithCommasAndAnd(outputRecord.getErrors())));
         }

         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         // at one time, we threw upon warning - but                                                           //
         // on insert we need to return the record (e.g., to get a generated id), so, make update do the same. //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if(CollectionUtils.nullSafeHasContents(outputRecord.getWarnings()))
         // {
         //    throw (new QUserFacingException("Warning updating " + tableMetaData.getLabel() + ": " + joinErrorsWithCommasAndAnd(outputRecord.getWarnings())));
         // }

         QJavalinAccessLogger.logEndSuccess();
         context.result(JsonUtils.toJson(updateOutput));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setRecordValuesForInsertOrUpdate(Context context, QTableMetaData tableMetaData, QRecord record) throws IOException
   {
      String  contentType       = Objects.requireNonNullElse(context.header("content-type"), "");
      boolean isContentTypeJson = contentType.toLowerCase().contains("json");

      try
      {
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the caller said they've sent JSON, or if they didn't supply a content-type, then try to read the body //
         // as JSON.  if it throws, we'll continue by trying to read form params, but if it succeeds, we'll return.  //
         //////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(isContentTypeJson || !StringUtils.hasContent(contentType))
         {
            Map<?, ?> map = context.bodyAsClass(Map.class);
            for(Map.Entry<?, ?> entry : map.entrySet())
            {
               String fieldName = ValueUtils.getValueAsString(entry.getKey());
               Object value     = entry.getValue();

               if(StringUtils.hasContent(String.valueOf(value)))
               {
                  record.setValue(fieldName, (Serializable) value);
               }
               else if("".equals(value))
               {
                  ///////////////////////////////////////////////////////////////////////////////////////////////////
                  // if frontend sent us an empty string - put a null in the record's value map.                   //
                  // this could potentially be changed to be type-specific (e.g., store an empty-string for STRING //
                  // fields, but null for INTEGER, etc) - but, who really wants empty-string in database anyway?   //
                  ///////////////////////////////////////////////////////////////////////////////////////////////////
                  record.setValue(fieldName, null);
               }
            }

            return;
         }
      }
      catch(Exception e)
      {
         LOG.info("Error trying to read body as map", e, logPair("contentType", contentType));
      }

      /////////////////////////
      // process form params //
      /////////////////////////
      for(Map.Entry<String, List<String>> formParam : context.formParamMap().entrySet())
      {
         String       fieldName = formParam.getKey();
         List<String> values    = formParam.getValue();

         if(CollectionUtils.nullSafeHasContents(values))
         {
            String value = values.get(0);

            if("associations".equals(fieldName) && StringUtils.hasContent(value))
            {
               JSONObject associationsJSON = new JSONObject(value);
               for(String key : associationsJSON.keySet())
               {
                  JSONArray     associatedRecordsJSON = associationsJSON.getJSONArray(key);
                  List<QRecord> associatedRecords     = new ArrayList<>();
                  record.withAssociatedRecords(key, associatedRecords);

                  for(int i = 0; i < associatedRecordsJSON.length(); i++)
                  {
                     QRecord    associatedRecord = new QRecord();
                     JSONObject recordJSON       = associatedRecordsJSON.getJSONObject(i);
                     for(String k : recordJSON.keySet())
                     {
                        associatedRecord.withValue(k, ValueUtils.getValueAsString(recordJSON.get(k)));
                     }
                     associatedRecords.add(associatedRecord);
                  }
               }
               continue;
            }

            if(StringUtils.hasContent(value))
            {
               record.setValue(fieldName, value);
            }
            else
            {
               record.setValue(fieldName, null);
            }
         }
         else
         {
            record.setValue(fieldName, null);
         }
      }

      ////////////////////////////
      // process uploaded files //
      ////////////////////////////
      for(Map.Entry<String, List<UploadedFile>> entry : CollectionUtils.nonNullMap(context.uploadedFileMap()).entrySet())
      {
         String             fieldName     = entry.getKey();
         List<UploadedFile> uploadedFiles = entry.getValue();
         if(uploadedFiles.size() > 0)
         {
            UploadedFile uploadedFile = uploadedFiles.get(0);
            try(InputStream content = uploadedFile.content())
            {
               record.setValue(fieldName, content.readAllBytes());
            }

            QFieldMetaData blobField = tableMetaData.getField(fieldName);
            blobField.getAdornment(AdornmentType.FILE_DOWNLOAD).ifPresent(adornment ->
            {
               adornment.getValue(AdornmentType.FileDownloadValues.FILE_NAME_FIELD).ifPresent(fileNameFieldName ->
               {
                  record.setValue(ValueUtils.getValueAsString(fileNameFieldName), uploadedFile.filename());
               });
            });
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if the record has any blob fields, and we're clearing them out (present in the values list, and set to null), //
      // and they have a  file-name field associated with them, then also clear out that file-name field               //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(QFieldMetaData field : tableMetaData.getFields().values())
      {
         if(field.getType().equals(QFieldType.BLOB))
         {
            field.getAdornment(AdornmentType.FILE_DOWNLOAD).ifPresent(adornment ->
            {
               adornment.getValue(AdornmentType.FileDownloadValues.FILE_NAME_FIELD).ifPresent(fileNameFieldName ->
               {
                  if(record.getValues().containsKey(field.getName()) && record.getValue(field.getName()) == null)
                  {
                     record.setValue(ValueUtils.getValueAsString(fileNameFieldName), null);
                  }
               });
            });
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void dataInsert(Context context)
   {
      String tableName = context.pathParam("table");
      try
      {
         InsertInput insertInput = new InsertInput();
         insertInput.setInputSource(QInputSource.USER);
         setupSession(context, insertInput);
         insertInput.setTableName(tableName);
         QJavalinAccessLogger.logStart("insert", logPair("table", tableName));

         PermissionsHelper.checkTablePermissionThrowing(insertInput, TablePermissionSubType.INSERT);
         QTableMetaData tableMetaData = qInstance.getTable(tableName);

         List<QRecord> recordList = new ArrayList<>();
         QRecord       record     = new QRecord();
         record.setTableName(tableName);
         recordList.add(record);
         setRecordValuesForInsertOrUpdate(context, tableMetaData, record);
         insertInput.setRecords(recordList);

         InsertAction insertAction = new InsertAction();
         InsertOutput insertOutput = insertAction.execute(insertInput);
         QRecord      outputRecord = insertOutput.getRecords().get(0);

         QTableMetaData table = qInstance.getTable(tableName);
         if(CollectionUtils.nullSafeHasContents(outputRecord.getErrors()))
         {
            throw (new QUserFacingException("Error inserting " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(outputRecord.getErrors())));
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         // at one time, we threw upon warning - but                                                              //
         // our use-case is, the frontend, it wants to get the record, and show a success (with the generated id) //
         // and then to also show a warning message - so - let it all be returned and handled on the frontend.    //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if(CollectionUtils.nullSafeHasContents(outputRecord.getWarnings()))
         // {
         //    throw (new QUserFacingException("Warning inserting " + table.getLabel() + ": " + joinErrorsWithCommasAndAnd(outputRecord.getWarnings())));
         // }

         QJavalinAccessLogger.logEndSuccess(logPair("primaryKey", () -> (outputRecord.getValue(table.getPrimaryKeyField()))));
         context.result(JsonUtils.toJson(insertOutput));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    ********************************************************************************/
   private static void dataGet(Context context)
   {
      String tableName  = context.pathParam("table");
      String primaryKey = context.pathParam("primaryKey");

      try
      {
         QTableMetaData table    = qInstance.getTable(tableName);
         GetInput       getInput = new GetInput();

         setupSession(context, getInput);
         QJavalinAccessLogger.logStart("get", logPair("table", tableName), logPair("primaryKey", primaryKey));

         getInput.setTableName(tableName);
         getInput.setInputSource(QInputSource.USER);
         getInput.setShouldGenerateDisplayValues(true);
         getInput.setShouldTranslatePossibleValues(true);
         getInput.setShouldFetchHeavyFields(true);

         getInput.setQueryJoins(processQueryJoinsParam(context));

         if("true".equals(context.queryParam("includeAssociations")))
         {
            getInput.setIncludeAssociations(true);
         }

         PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);

         // todo - validate that the primary key is of the proper type (e.g,. not a string for an id field)
         //  and throw a 400-series error (tell the user bad-request), rather than, we're doing a 500 (server error)

         getInput.setPrimaryKey(primaryKey);

         GetAction getAction = new GetAction();
         GetOutput getOutput = getAction.execute(getInput);

         ///////////////////////////////////////////////////////
         // throw a not found error if the record isn't found //
         ///////////////////////////////////////////////////////
         QRecord record = getOutput.getRecord();
         if(record == null)
         {
            throw (new QNotFoundException("Could not find " + table.getLabel() + " with " + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
         }

         QValueFormatter.setBlobValuesToDownloadUrls(table, List.of(record));

         QJavalinAccessLogger.logEndSuccess();
         context.result(JsonUtils.toJson(record));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void dataDownloadRecordField(Context context)
   {
      String tableName  = context.pathParam("table");
      String primaryKey = context.pathParam("primaryKey");
      String fieldName  = context.pathParam("fieldName");
      String filename   = context.pathParam("filename");

      try
      {
         QTableMetaData table    = qInstance.getTable(tableName);
         GetInput       getInput = new GetInput();

         setupSession(context, getInput);
         QJavalinAccessLogger.logStart("downloadRecordField", logPair("table", tableName), logPair("primaryKey", primaryKey), logPair("fieldName", fieldName));

         ////////////////////////////////////////////
         // validate field name - 404 if not found //
         ////////////////////////////////////////////
         QFieldMetaData fieldMetaData;
         try
         {
            fieldMetaData = table.getField(fieldName);
         }
         catch(Exception e)
         {
            throw (new QNotFoundException("Could not find field named " + fieldName + " on table " + tableName));
         }

         getInput.setTableName(tableName);
         getInput.setShouldFetchHeavyFields(true);

         PermissionsHelper.checkTablePermissionThrowing(getInput, TablePermissionSubType.READ);

         getInput.setPrimaryKey(primaryKey);

         GetAction getAction = new GetAction();
         GetOutput getOutput = getAction.execute(getInput);

         ///////////////////////////////////////////////////////
         // throw a not found error if the record isn't found //
         ///////////////////////////////////////////////////////
         if(getOutput.getRecord() == null)
         {
            throw (new QNotFoundException("Could not find " + table.getLabel() + " with " + table.getFields().get(table.getPrimaryKeyField()).getLabel() + " of " + primaryKey));
         }

         String                    mimeType              = null;
         Optional<FieldAdornment>  fileDownloadAdornment = fieldMetaData.getAdornments().stream().filter(a -> a.getType().equals(AdornmentType.FILE_DOWNLOAD)).findFirst();
         Map<String, Serializable> adornmentValues       = null;
         if(fileDownloadAdornment.isPresent())
         {
            adornmentValues = fileDownloadAdornment.get().getValues();
            mimeType = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.FileDownloadValues.DEFAULT_MIME_TYPE));
         }

         if(mimeType != null)
         {
            context.contentType(mimeType);
         }

         if(context.queryParamMap().containsKey("download") || context.formParamMap().containsKey("download"))
         {
            context.header("Content-Disposition", "attachment; filename=" + filename);
         }

         //////////////////////////////////////////////////////////////////////////////////////////////
         // if the adornment has a supplemental process name in it, or a supplemental code reference //
         // then execute that custom code - e.g., to log that the file was downloaded.               //
         //////////////////////////////////////////////////////////////////////////////////////////////
         if(fileDownloadAdornment.isPresent())
         {
            String processName = ValueUtils.getValueAsString(adornmentValues.get(AdornmentType.FileDownloadValues.SUPPLEMENTAL_PROCESS_NAME));
            if(StringUtils.hasContent(processName))
            {
               RunProcessInput input = new RunProcessInput();
               input.setProcessName(processName);
               input.setCallback(QProcessCallbackFactory.forRecord(getOutput.getRecord()));
               input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
               input.addValue("tableName", tableName);
               input.addValue("primaryKey", primaryKey);
               input.addValue("fieldName", fieldName);
               input.addValue("filename", filename);
               new RunProcessAction().execute(input);
            }
            else if(adornmentValues.containsKey(AdornmentType.FileDownloadValues.SUPPLEMENTAL_CODE_REFERENCE))
            {
               QCodeReference codeReference = (QCodeReference) adornmentValues.get(AdornmentType.FileDownloadValues.SUPPLEMENTAL_CODE_REFERENCE);

               DownloadFileSupplementalAction action = QCodeLoader.getAdHoc(DownloadFileSupplementalAction.class, codeReference);

               DownloadFileSupplementalAction.DownloadFileSupplementalActionInput input = new DownloadFileSupplementalAction.DownloadFileSupplementalActionInput()
                  .withTableName(tableName)
                  .withFieldName(fieldName)
                  .withPrimaryKey(primaryKey)
                  .withFileName(filename);

               DownloadFileSupplementalAction.DownloadFileSupplementalActionOutput output = new DownloadFileSupplementalAction.DownloadFileSupplementalActionOutput();
               action.run(input, output);
            }
         }

         /////////////////////////////////////////////////////////
         // if the field is a BLOB - send the bytes to the user //
         /////////////////////////////////////////////////////////
         if(QFieldType.BLOB.equals(fieldMetaData.getType()))
         {
            context.result(getOutput.getRecord().getValueByteArray(fieldName));
         }
         else
         {
            //////////////////////////////////////////////////////////////////
            // else - assume a string is a URL - and issue a redirect to it //
            //////////////////////////////////////////////////////////////////
            context.redirect(getOutput.getRecord().getValueString(fieldName));
         }

         QJavalinAccessLogger.logEndSuccess();
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e);
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   static void variants(Context context)
   {
      String                 table    = context.pathParam("table");
      List<QFrontendVariant> variants = new ArrayList<>();

      try
      {
         QueryInput queryInput = new QueryInput();
         setupSession(context, queryInput);

         ////////////////////////////////////
         // get the backend for this table //
         ////////////////////////////////////
         QTableMetaData   tableMetaData = QContext.getQInstance().getTable(table);
         QBackendMetaData backend       = QContext.getQInstance().getBackend(tableMetaData.getBackendName());

         /////////////////////////////////////////////////////////////////////////////////////
         // if the backend uses variants, query for all possible variants of the given type //
         /////////////////////////////////////////////////////////////////////////////////////
         if(backend != null && backend.getUsesVariants())
         {
            QTableMetaData variantsTable = QContext.getQInstance().getTable(backend.getBackendVariantsConfig().getOptionsTableName());

            queryInput.setTableName(variantsTable.getName());
            queryInput.setFilter(backend.getBackendVariantsConfig().getOptionsFilter());
            queryInput.setShouldGenerateDisplayValues(true);
            QueryOutput output = new QueryAction().execute(queryInput);
            for(QRecord qRecord : output.getRecords())
            {
               variants.add(new QFrontendVariant()
                  .withId(qRecord.getValue(variantsTable.getPrimaryKeyField()))
                  .withType(backend.getBackendVariantsConfig().getVariantTypeKey())
                  .withName(qRecord.getRecordLabel()));
            }

            QJavalinAccessLogger.logStartSilent("variants");
            PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);
         }

         QJavalinAccessLogger.logEndSuccessIfSlow(SLOW_LOG_THRESHOLD_MS, logPair("table", table), logPair("input", queryInput));
         context.result(JsonUtils.toJson(variants));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e, logPair("table", table));
         handleException(context, e);
      }
   }



   /*******************************************************************************
    *
    * Filter parameter is a serialized QQueryFilter object, that is to say:
    * <pre>
    *   filter=
    *    {"criteria":[
    *       {"fieldName":"id","operator":"EQUALS","values":[1]},
    *       {"fieldName":"name","operator":"IN","values":["Darin","James"]}
    *     ]
    *    }
    * </pre>
    *******************************************************************************/
   static void dataCount(Context context)
   {
      String table  = context.pathParam("table");
      String filter = null;

      try
      {
         CountInput countInput = new CountInput();
         setupSession(context, countInput);
         countInput.setTableName(table);
         QJavalinAccessLogger.logStartSilent("count");

         PermissionsHelper.checkTablePermissionThrowing(countInput, TablePermissionSubType.READ);

         filter = QJavalinUtils.getQueryParamOrFormParam(context, "filter");
         if(filter != null)
         {
            countInput.setFilter(JsonUtils.toObject(filter, QQueryFilter.class));
         }

         countInput.setTimeoutSeconds(DEFAULT_COUNT_TIMEOUT_SECONDS);
         countInput.setQueryJoins(processQueryJoinsParam(context));
         countInput.setIncludeDistinctCount(QJavalinUtils.queryParamIsTrue(context, "includeDistinct"));
         countInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

         CountAction countAction = new CountAction();
         CountOutput countOutput = countAction.execute(countInput);

         QJavalinAccessLogger.logEndSuccessIfSlow(SLOW_LOG_THRESHOLD_MS, logPair("table", table), logPair("count", countOutput.getCount()), logPair("filter", filter));
         context.result(JsonUtils.toJson(countOutput));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e, logPair("table", table), logPair("filter", filter));
         handleException(context, e);
      }
   }



   /*******************************************************************************
    *
    * Filter parameter is a serialized QQueryFilter object, that is to say:
    * <pre>
    *   filter=
    *    {"criteria":[
    *       {"fieldName":"id","operator":"EQUALS","values":[1]},
    *       {"fieldName":"name","operator":"IN","values":["Darin","James"]}
    *     ],
    *     "orderBys":[
    *       {"fieldName":"age","isAscending":true}
    *     ]}
    * </pre>
    *
    * queryJoins parameter is a JSONArray of objects which represent a QueryJoin object.  e.g.,
    * <pre>
    *   queryJoins=
    *    [
    *       {"joinTable":"orderLine","select":true,"type":"INNER"},
    *       {"joinTable":"customer","select":true,"type":"LEFT"}
    *    }
    * </pre>
    * Additional field names in the JSONObjects there are: baseTableOrAlias, alias, joinName.
    *******************************************************************************/
   static void dataQuery(Context context)
   {
      String table  = context.pathParam("table");
      String filter = null;

      try
      {
         QueryInput queryInput = new QueryInput();
         setupSession(context, queryInput);
         QJavalinAccessLogger.logStart("query", logPair("table", table));

         queryInput.setTableName(table);
         queryInput.setShouldGenerateDisplayValues(true);
         queryInput.setShouldTranslatePossibleValues(true);
         queryInput.setTimeoutSeconds(DEFAULT_QUERY_TIMEOUT_SECONDS);
         queryInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

         PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);

         filter = QJavalinUtils.getQueryParamOrFormParam(context, "filter");
         if(filter != null)
         {
            QQueryFilter qQueryFilter = JsonUtils.toObject(filter, QQueryFilter.class);
            queryInput.setFilter(qQueryFilter);
            qQueryFilter.interpretValues(Collections.emptyMap());
         }

         Integer skip  = QJavalinUtils.integerQueryParam(context, "skip");
         Integer limit = QJavalinUtils.integerQueryParam(context, "limit");
         if(skip != null || limit != null)
         {
            if(queryInput.getFilter() == null)
            {
               queryInput.setFilter(new QQueryFilter());
            }
            queryInput.getFilter().setSkip(skip);
            queryInput.getFilter().setLimit(limit);
         }

         if(queryInput.getFilter() == null || queryInput.getFilter().getLimit() == null)
         {
            QJavalinUtils.handleQueryNullLimit(javalinMetaData, queryInput, context);
         }

         List<QueryJoin> queryJoins = processQueryJoinsParam(context);
         queryInput.setQueryJoins(queryJoins);

         QueryAction queryAction = new QueryAction();
         QueryOutput queryOutput = queryAction.execute(queryInput);

         QValueFormatter.setBlobValuesToDownloadUrls(QContext.getQInstance().getTable(table), queryOutput.getRecords());

         QJavalinAccessLogger.logEndSuccess(logPair("recordCount", queryOutput.getRecords().size()), logPairIfSlow("filter", filter, SLOW_LOG_THRESHOLD_MS), logPairIfSlow("joins", queryJoins, SLOW_LOG_THRESHOLD_MS));
         context.result(JsonUtils.toJson(queryOutput));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e, logPair("filter", filter));
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QueryJoin> processQueryJoinsParam(Context context)
   {
      List<QueryJoin> queryJoins = null;

      String queryJoinsParam = QJavalinUtils.stringQueryParam(context, "queryJoins");
      if(StringUtils.hasContent(queryJoinsParam))
      {
         queryJoins = new ArrayList<>();

         JSONArray queryJoinsJSON = new JSONArray(queryJoinsParam);
         for(int i = 0; i < queryJoinsJSON.length(); i++)
         {
            QueryJoin queryJoin = new QueryJoin();
            queryJoins.add(queryJoin);

            JSONObject jsonObject = queryJoinsJSON.getJSONObject(i);
            queryJoin.setJoinTable(jsonObject.optString("joinTable"));

            if(jsonObject.has("baseTableOrAlias") && !jsonObject.isNull("baseTableOrAlias"))
            {
               queryJoin.setBaseTableOrAlias(jsonObject.optString("baseTableOrAlias"));
            }

            if(jsonObject.has("alias") && !jsonObject.isNull("alias"))
            {
               queryJoin.setAlias(jsonObject.optString("alias"));
            }

            queryJoin.setSelect(jsonObject.optBoolean("select"));

            if(jsonObject.has("type") && !jsonObject.isNull("type"))
            {
               queryJoin.setType(QueryJoin.Type.valueOf(jsonObject.getString("type")));
            }

            if(jsonObject.has("joinName") && !jsonObject.isNull("joinName"))
            {
               queryJoin.setJoinMetaData(QContext.getQInstance().getJoin(jsonObject.getString("joinName")));
            }
         }
      }

      return (queryJoins);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void metaData(Context context)
   {
      try
      {
         MetaDataInput metaDataInput = new MetaDataInput();
         setupSession(context, metaDataInput);
         MetaDataAction metaDataAction = new MetaDataAction();
         MetaDataOutput metaDataOutput = metaDataAction.execute(metaDataInput);

         context.result(JsonUtils.toJson(metaDataOutput));
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void tableMetaData(Context context)
   {
      try
      {
         TableMetaDataInput tableMetaDataInput = new TableMetaDataInput();
         setupSession(context, tableMetaDataInput);

         String         tableName = context.pathParam("table");
         QTableMetaData table     = qInstance.getTable(tableName);
         if(table == null)
         {
            throw (new QNotFoundException("Table [" + tableName + "] was not found."));
         }

         PermissionCheckResult permissionCheckResult = PermissionsHelper.getPermissionCheckResult(tableMetaDataInput, table);
         if(permissionCheckResult.equals(PermissionCheckResult.DENY_HIDE))
         {
            // not found?  or permission denied... hmm
            throw (new QNotFoundException("Table [" + tableName + "] was not found."));
         }

         tableMetaDataInput.setTableName(tableName);
         tableMetaDataInput.setInputSource(QInputSource.USER);

         TableMetaDataAction tableMetaDataAction = new TableMetaDataAction();
         TableMetaDataOutput tableMetaDataOutput = tableMetaDataAction.execute(tableMetaDataInput);

         context.result(JsonUtils.toJson(tableMetaDataOutput));
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processMetaData(Context context)
   {
      try
      {
         ProcessMetaDataInput processMetaDataInput = new ProcessMetaDataInput();
         setupSession(context, processMetaDataInput);

         String           processName = context.pathParam("processName");
         QProcessMetaData process     = qInstance.getProcess(processName);
         if(process == null)
         {
            throw (new QNotFoundException("Process [" + processName + "] was not found."));
         }
         PermissionsHelper.checkProcessPermissionThrowing(processMetaDataInput, processName);

         processMetaDataInput.setProcessName(processName);
         ProcessMetaDataAction processMetaDataAction = new ProcessMetaDataAction();
         ProcessMetaDataOutput processMetaDataOutput = processMetaDataAction.execute(processMetaDataInput);

         context.result(JsonUtils.toJson(processMetaDataOutput));
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    ** Load the data for a widget of a given name.
    *******************************************************************************/
   private static void widget(Context context)
   {
      String widgetName = context.pathParam("name");

      RenderWidgetInput input = new RenderWidgetInput()
         .withWidgetMetaData(qInstance.getWidget(widgetName));

      try
      {
         setupSession(context, input);
         QJavalinAccessLogger.logStartSilent("widget");

         // todo permission?

         //////////////////////////
         // process query string //
         //////////////////////////
         for(Map.Entry<String, List<String>> queryParam : context.queryParamMap().entrySet())
         {
            String       fieldName = queryParam.getKey();
            List<String> values    = queryParam.getValue();
            if(CollectionUtils.nullSafeHasContents(values))
            {
               input.addQueryParam(fieldName, values.get(0));
            }
         }

         ////////////////////////////
         // process form/post body //
         ////////////////////////////
         for(Map.Entry<String, List<String>> formParam : context.formParamMap().entrySet())
         {
            String       fieldName = formParam.getKey();
            List<String> values    = formParam.getValue();
            if(CollectionUtils.nullSafeHasContents(values))
            {
               input.addQueryParam(fieldName, values.get(0));
            }
         }

         RenderWidgetOutput output = new RenderWidgetAction().execute(input);
         QJavalinAccessLogger.logEndSuccessIfSlow(SLOW_LOG_THRESHOLD_MS, logPair("widgetName", widgetName), logPair("inputParams", input.getQueryParams()));
         context.result(JsonUtils.toJson(output.getWidgetData()));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e, logPair("widgetName", widgetName), logPair("inputParams", input.getQueryParams()));
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void dataExportWithFilename(Context context)
   {
      String filename = context.pathParam("filename");
      dataExport(context, Optional.of(filename));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void dataExportWithoutFilename(Context context)
   {
      dataExport(context, Optional.empty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void dataExport(Context context, Optional<String> optionalFilename)
   {
      String tableName = context.pathParam("table");

      try
      {
         String       format       = context.queryParam("format");
         ReportFormat reportFormat = getReportFormat(context, optionalFilename, format);
         if(reportFormat == null)
         {
            return;
         }

         /////////////////////////////////////////////
         // set up the report action's input object //
         /////////////////////////////////////////////
         ExportInput exportInput = new ExportInput();
         setupSession(context, exportInput);

         exportInput.setTableName(tableName);
         exportInput.setInputSource(QInputSource.USER);

         String filename = optionalFilename.orElse(tableName + "." + reportFormat.toString().toLowerCase(Locale.ROOT));
         exportInput.withReportDestination(new ReportDestination()
            .withReportFormat(reportFormat)
            .withFilename(filename));

         Integer limit = QJavalinUtils.integerQueryParam(context, "limit");
         exportInput.setLimit(limit);

         PermissionsHelper.checkTablePermissionThrowing(exportInput, TablePermissionSubType.READ);

         String fields = QJavalinUtils.getQueryParamOrFormParam(context, "fields");
         if(StringUtils.hasContent(fields))
         {
            exportInput.setFieldNames(List.of(fields.split(",")));
         }

         String filter = QJavalinUtils.getQueryParamOrFormParam(context, "filter");
         if(StringUtils.hasContent(filter))
         {
            exportInput.setQueryFilter(JsonUtils.toObject(filter, QQueryFilter.class));
         }

         String omitHeaderRow = QJavalinUtils.getQueryParamOrFormParam(context, "omitHeaderRow");
         if("true".equals(omitHeaderRow))
         {
            exportInput.setIncludeHeaderRow(false);
         }

         UnsafeFunction<PipedOutputStream, ExportAction, Exception> preAction = (PipedOutputStream pos) ->
         {
            exportInput.getReportDestination().setReportOutputStream(pos);

            ExportAction exportAction = new ExportAction();
            exportAction.preExecute(exportInput);
            return (exportAction);
         };

         String finalFilter = filter;
         UnsafeConsumer<ExportAction, Exception> execute = (ExportAction exportAction) ->
         {
            QJavalinAccessLogger.logStart("export", logPair("table", tableName));
            try
            {
               ExportOutput exportOutput = exportAction.execute(exportInput);
               QJavalinAccessLogger.logEndSuccess(logPair("recordCount", exportOutput.getRecordCount()), logPairIfSlow("filter", finalFilter, SLOW_LOG_THRESHOLD_MS));
            }
            catch(Exception e)
            {
               QJavalinAccessLogger.logEndFail(e, logPair("filter", finalFilter));
               throw (e);
            }
         };

         runStreamedExportOrReport(context, reportFormat, filename, preAction, execute);
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <T> void runStreamedExportOrReport(Context context, ReportFormat reportFormat, String filename, UnsafeFunction<PipedOutputStream, T, Exception> preAction, UnsafeConsumer<T, Exception> executor) throws Exception
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // set up the I/O pipe streams.                                                                      //
      // Critically, we must NOT open the outputStream in a try-with-resources.  The thread that writes to //
      // the stream must close it when it's done writing.                                                  //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      PipedOutputStream pipedOutputStream = new PipedOutputStream();
      PipedInputStream  pipedInputStream  = new PipedInputStream();
      pipedOutputStream.connect(pipedInputStream);

      T t = preAction.apply(pipedOutputStream);

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // start the async job.                                                                            //
      // Critically, this must happen before the pipedInputStream is passed to the javalin result method //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      new AsyncJobManager().startJob("Javalin>ExportAction", (o) ->
      {
         try
         {
            executor.run(t);
            return (true);
         }
         catch(Exception e)
         {
            handleExportOrReportException(context, pipedOutputStream, e);
            return (false);
         }
      });

      ////////////////////////////////////////////
      // set the response content type & stream //
      ////////////////////////////////////////////
      context.contentType(reportFormat.getMimeType());
      context.header("Content-Disposition", "filename=" + filename);
      context.result(pipedInputStream);

      ////////////////////////////////////////////////////////////////////////////////////////////
      // we'd like to check to see if the job failed, and if so, to give the user an error...   //
      // but if we "block" here, then piped streams seem to never flush, so we deadlock things. //
      ////////////////////////////////////////////////////////////////////////////////////////////
      // AsyncJobStatus asyncJobStatus = asyncJobManager.waitForJob(jobUUID);
      // if(asyncJobStatus.getState().equals(AsyncJobState.ERROR))
      // {
      //    System.out.println("Well, here we are...");
      //    throw (new QUserFacingException("Error running report: " + asyncJobStatus.getCaughtException().getMessage()));
      // }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void handleExportOrReportException(Context context, PipedOutputStream pipedOutputStream, Exception e) throws IOException
   {
      HttpStatus.Code statusCode = HttpStatus.Code.INTERNAL_SERVER_ERROR; // 500
      String          message    = e.getMessage();

      QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(e, QUserFacingException.class);
      if(userFacingException != null)
      {
         LOG.info("User-facing exception", e);
         statusCode = HttpStatus.Code.BAD_REQUEST; // 400
         message = userFacingException.getMessage();
      }
      else
      {
         QAuthenticationException authenticationException = ExceptionUtils.findClassInRootChain(e, QAuthenticationException.class);
         if(authenticationException != null)
         {
            statusCode = HttpStatus.Code.UNAUTHORIZED; // 401
         }
         else
         {
            LOG.warn("Unexpected exception in javalin report or export request", e);
         }
      }

      context.status(statusCode.getCode());
      pipedOutputStream.write(("Error generating report: " + message).getBytes());
      pipedOutputStream.close();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static ReportFormat getReportFormat(Context context, Optional<String> optionalFilename, String format)
   {
      /////////////////////////////////////////////////////////////////////////////////////////
      // if a format query param wasn't given, then try to get file extension from file name //
      /////////////////////////////////////////////////////////////////////////////////////////
      if(!StringUtils.hasContent(format) && optionalFilename.isPresent() && StringUtils.hasContent(optionalFilename.get()))
      {
         String filename = optionalFilename.get();
         if(filename.contains("."))
         {
            format = filename.substring(filename.lastIndexOf(".") + 1);
         }
      }

      ReportFormat reportFormat;
      try
      {
         reportFormat = ReportFormat.fromString(format);
      }
      catch(QUserFacingException e)
      {
         QJavalinUtils.handleException(HttpStatus.Code.BAD_REQUEST, context, e);
         return null;
      }
      return reportFormat;
   }



   /*******************************************************************************
    ** handler for a PVS that's associated with a field on a table.
    *******************************************************************************/
   private static void possibleValuesForTableField(Context context)
   {
      try
      {
         String tableName = context.pathParam("table");
         String fieldName = context.pathParam("fieldName");

         QTableMetaData table = qInstance.getTable(tableName);
         if(table == null)
         {
            throw (new QNotFoundException("Could not find table named " + tableName + " in this instance."));
         }

         QFieldMetaData field;
         try
         {
            field = table.getField(fieldName);
         }
         catch(Exception e)
         {
            throw (new QNotFoundException("Could not find field named " + fieldName + " in table " + tableName + "."));
         }

         if(!StringUtils.hasContent(field.getPossibleValueSourceName()))
         {
            throw (new QNotFoundException("Field " + fieldName + " in table " + tableName + " is not associated with a possible value source."));
         }

         finishPossibleValuesRequest(context, field);
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    ** handler for a standalone (e.g., outside of a table or process) PVS.
    *******************************************************************************/
   private static void possibleValuesStandalone(Context context)
   {
      try
      {
         String possibleValueSourceName = context.pathParam("possibleValueSourceName");

         Map<String, Serializable> otherValues = getOtherValueForPossibleValueRequest(context);

         QPossibleValueSource pvs = qInstance.getPossibleValueSource(possibleValueSourceName);
         if(pvs == null)
         {
            throw (new QNotFoundException("Could not find possible value source " + possibleValueSourceName + " in this instance."));
         }

         //////////////////////////////////////////////////
         // allow a filter to be passed in from frontend //
         //////////////////////////////////////////////////
         QQueryFilter defaultQueryFilter = null;
         List<String> filterParam        = context.formParamMap().get("filter");
         if(CollectionUtils.nullSafeHasContents(filterParam))
         {
            String possibleValueSourceFilterJSON = filterParam.get(0);
            defaultQueryFilter = JsonUtils.toObject(possibleValueSourceFilterJSON, QQueryFilter.class);

            String                           useCaseParam = QJavalinUtils.getQueryParamOrFormParam(context, "useCase");
            PossibleValueSearchFilterUseCase useCase      = ObjectUtils.tryElse(() -> PossibleValueSearchFilterUseCase.valueOf(useCaseParam.toUpperCase()), PossibleValueSearchFilterUseCase.FORM);

            defaultQueryFilter.interpretValues(useCase, otherValues);
         }

         finishPossibleValuesRequest(context, possibleValueSourceName, defaultQueryFilter, otherValues);
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    ** continuation for table or process PVS's,
    *******************************************************************************/
   static void finishPossibleValuesRequest(Context context, QFieldMetaData field) throws IOException, QException
   {
      QQueryFilter              defaultQueryFilter = null;
      Map<String, Serializable> otherValues        = getOtherValueForPossibleValueRequest(context);

      if(field.getPossibleValueSourceFilter() != null)
      {
         defaultQueryFilter = field.getPossibleValueSourceFilter().clone();

         String                           useCaseParam = QJavalinUtils.getQueryParamOrFormParam(context, "useCase");
         PossibleValueSearchFilterUseCase useCase      = ObjectUtils.tryElse(() -> PossibleValueSearchFilterUseCase.valueOf(useCaseParam.toUpperCase()), PossibleValueSearchFilterUseCase.FORM);

         Map<String, Serializable> processValues = new HashMap<>();
         if(context.queryParamMap().containsKey("processUUID") && StringUtils.hasContent(context.queryParam("processUUID")))
         {
            Optional<ProcessState> processState = RunProcessAction.getState(context.queryParam("processUUID"));
            if(processState.isPresent())
            {
               processValues = processState.get().getValues();
            }
         }

         defaultQueryFilter.interpretValues(MapBuilder.of("input", otherValues, "processValues", processValues), useCase);
      }

      finishPossibleValuesRequest(context, field.getPossibleValueSourceName(), defaultQueryFilter, otherValues);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static Map<String, Serializable> getOtherValueForPossibleValueRequest(Context context) throws IOException
   {
      Map<String, Serializable> otherValues = new HashMap<>();
      if(context.formParamMap().containsKey("values"))
      {
         List<String> valuesParamList = context.formParamMap().get("values");
         if(CollectionUtils.nullSafeHasContents(valuesParamList))
         {
            String valuesParam = valuesParamList.get(0);
            otherValues = JsonUtils.toObject(valuesParam, new TypeReference<>() {});
         }
      }
      return otherValues;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void finishPossibleValuesRequest(Context context, String possibleValueSourceName, QQueryFilter defaultFilter, Map<String, Serializable> otherValues) throws QException
   {
      String searchTerm = context.queryParam("searchTerm");
      String ids        = context.queryParam("ids");
      String labels     = context.queryParam("labels");

      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput();
      setupSession(context, input);
      input.setPossibleValueSourceName(possibleValueSourceName);
      input.setSearchTerm(searchTerm);
      input.setDefaultQueryFilter(defaultFilter);
      input.setOtherValues(otherValues);
      input.setPathParamMap(context.pathParamMap());
      input.setQueryParamMap(context.queryParamMap());

      if(StringUtils.hasContent(ids))
      {
         List<Serializable> idList = new ArrayList<>(Arrays.asList(ids.split(",")));
         input.setIdList(idList);
      }
      else if(StringUtils.hasContent(labels))
      {
         List<String> labelList = new ArrayList<>(Arrays.asList(labels.split(",")));
         input.setLabelList(labelList);
      }

      SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceAction().execute(input);

      Map<String, Object> result = new HashMap<>();
      result.put("options", output.getResults());
      context.result(JsonUtils.toJson(result));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void handleException(Context context, Exception e)
   {
      QJavalinUtils.handleException(null, context, e);
   }



   /*******************************************************************************
    ** Setter for qInstanceHotSwapSupplier
    *******************************************************************************/
   public static void setQInstanceHotSwapSupplier(Supplier<QInstance> qInstanceHotSwapSupplier)
   {
      QJavalinImplementation.qInstanceHotSwapSupplier = qInstanceHotSwapSupplier;
   }



   /*******************************************************************************
    ** Getter for javalinMetaData
    **
    *******************************************************************************/
   public static QJavalinMetaData getJavalinMetaData()
   {
      return javalinMetaData;
   }



   /*******************************************************************************
    ** Setter for javalinMetaData
    **
    *******************************************************************************/
   public void setJavalinMetaData(QJavalinMetaData javalinMetaData)
   {
      QJavalinImplementation.javalinMetaData = javalinMetaData;
   }



   /*******************************************************************************
    ** Getter for qInstanceHotSwapSupplier
    *******************************************************************************/
   public static Supplier<QInstance> getQInstanceHotSwapSupplier()
   {
      return (QJavalinImplementation.qInstanceHotSwapSupplier);
   }



   /*******************************************************************************
    ** Getter for qInstance
    *******************************************************************************/
   public static QInstance getQInstance()
   {
      return (QJavalinImplementation.qInstance);
   }



   /*******************************************************************************
    ** Setter for qInstance
    *******************************************************************************/
   public static void setQInstance(QInstance qInstance)
   {
      QJavalinImplementation.qInstance = qInstance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String joinErrorsWithCommasAndAnd(List<? extends QStatusMessage> errors)
   {
      return StringUtils.joinWithCommasAndAnd(errors.stream().map(QStatusMessage::getMessage).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void setMillisBetweenHotSwaps(long millisBetweenHotSwaps)
   {
      MILLIS_BETWEEN_HOT_SWAPS = millisBetweenHotSwaps;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static long getStartTimeMillis()
   {
      return (startTime);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void addJavalinRoutes(EndpointGroup routes)
   {
      if(endpointGroups == null)
      {
         endpointGroups = new ArrayList<>();
      }
      endpointGroups.add(routes);
   }



   /***************************************************************************
    ** if restarting this class, and you want to re-run addJavalinRoutes, but
    ** not create duplicates, well, you might want to call this method!
    ***************************************************************************/
   public void clearJavalinRoutes()
   {
      endpointGroups = null;
   }

}
