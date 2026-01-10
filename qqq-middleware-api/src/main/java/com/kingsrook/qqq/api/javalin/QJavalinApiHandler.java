/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.javalin;


import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.api.actions.ApiImplementation;
import com.kingsrook.qqq.api.actions.GenerateOpenApiSpecAction;
import com.kingsrook.qqq.api.model.APILog;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecInput;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecOutput;
import com.kingsrook.qqq.api.model.actions.HttpApiResponse;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataProvider;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInput;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInputFieldsContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessUtils;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthResolutionContext;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthenticationResolver;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.backend.javalin.QJavalinAccessLogger;
import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static com.kingsrook.qqq.backend.javalin.QJavalinImplementation.SLOW_LOG_THRESHOLD_MS;


/*******************************************************************************
 ** methods for handling qqq API requests in javalin.
 *******************************************************************************/
public class QJavalinApiHandler
{
   private static final QLogger LOG = QLogger.getLogger(QJavalinApiHandler.class);

   private static final ApiProcessMetaDataContainer EMPTY_API_PROCESS_META_DATA_CONTAINER = new ApiProcessMetaDataContainer().withApis(Collections.emptyMap());

   private static QInstance qInstance;

   private static Map<String, Integer> apiLogUserIdCache = new HashMap<>();



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QJavalinApiHandler(QInstance qInstance)
   {
      QJavalinApiHandler.qInstance = qInstance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void return405(Context context, HttpMethod allowedMethod)
   {
      respondWithError(context, HttpStatus.Code.METHOD_NOT_ALLOWED, "This path only supports method: " + allowedMethod, newAPILog(context)); // 405
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void handleProcessResponse(Context context, HttpApiResponse response, APILog apiLog)
   {
      if(response.getNeedsFormattedAsJson())
      {
         /////////////////////////////////////////////////////////////////////////////////////////
         // if the response object says that we should format the response as json, then do so. //
         /////////////////////////////////////////////////////////////////////////////////////////
         String resultString = toJson(Objects.requireNonNullElse(response.getResponseBodyObject(), ""));
         context.result(resultString);
         storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(resultString));
      }
      else
      {
         if(StringUtils.hasContent(response.getContentType()))
         {
            context.contentType(response.getContentType());
         }

         ///////////////////////////////////////////////////////////////////////////////////
         // if there's an input stream in the response, just send that down to the client //
         ///////////////////////////////////////////////////////////////////////////////////
         if(response.getInputStream() != null)
         {
            context.result(response.getInputStream());
            storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody("Streamed result"));
         }
         else
         {
            ////////////////////////////////////////////////////////////////////////////////////
            // else, try to return it raw - as byte[], or String, or as a converted-to-String //
            ////////////////////////////////////////////////////////////////////////////////////
            Serializable result = Objects.requireNonNullElse(response.getResponseBodyObject(), "");
            if(result instanceof byte[] ba)
            {
               context.result(ba);
               storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody("Byte array of length: " + ba.length));
            }
            else if(result instanceof String s)
            {
               context.result(s);
               storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(s));
            }
            else
            {
               String resultString = String.valueOf(result);
               context.result(resultString);
               storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(resultString));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processProcessInputFieldsContainer(Context context, Map<String, String> parameters, ApiProcessInputFieldsContainer fieldsContainer, BiFunction<Context, String, String> paramAccessor)
   {
      if(fieldsContainer != null)
      {
         List<QFieldMetaData> fields = CollectionUtils.nonNullList(fieldsContainer.getFields());
         ObjectUtils.ifNotNull(fieldsContainer.getRecordIdsField(), fields::add);
         for(QFieldMetaData field : fields)
         {
            try
            {
               String value = paramAccessor.apply(context, field.getName());
               if(value != null)
               {
                  String backendName = ObjectUtils.requireConditionElse(field.getBackendName(), StringUtils::hasContent, field.getName());
                  parameters.put(backendName, value);
               }
            }
            catch(Exception e)
            {
               LOG.info("Exception trying to process process input field", e);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doListApis(Context context)
   {
      ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = ApiInstanceMetaDataContainer.of(qInstance);

      StringBuilder html = new StringBuilder("<h1>Select an API</h1>\n<ul>\n");
      for(Map.Entry<String, ApiInstanceMetaData> entry : apiInstanceMetaDataContainer.getApis().entrySet())
      {
         html.append("""
            <li><a href="{link}">{name}</a></li>
            """
            .replace("{link}", entry.getValue().getPath())
            .replace("{name}", entry.getValue().getLabel())
         );
      }

      html.append("</ul>");

      context.header("Content-Type", ContentType.HTML);
      context.result(html.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void serveResource(Context context, String resourcePath, Map<String, String> headers)
   {
      InputStream resourceAsStream = QJavalinApiHandler.class.getClassLoader().getResourceAsStream(resourcePath);
      for(Map.Entry<String, String> entry : CollectionUtils.nonNullMap(headers).entrySet())
      {
         context.header(entry.getKey(), entry.getValue());
      }
      context.result(resourceAsStream);
   }



   /*******************************************************************************
    ** list the apis supported in this instance
    *******************************************************************************/
   private static void doGetApisJson(Context context)
   {
      Map<String, Object>       rs   = new HashMap<>();
      List<Map<String, Object>> apis = new ArrayList<>();
      rs.put("apis", apis);

      ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = ApiInstanceMetaDataContainer.of(qInstance);
      for(Map.Entry<String, ApiInstanceMetaData> entry : apiInstanceMetaDataContainer.getApis().entrySet())
      {
         Map<String, Object> thisApi = new HashMap<>();

         ApiInstanceMetaData apiInstanceMetaData = entry.getValue();
         thisApi.put("name", apiInstanceMetaData.getName());
         thisApi.put("path", apiInstanceMetaData.getPath());
         thisApi.put("label", apiInstanceMetaData.getLabel());

         String tableName = context.queryParam("tableName");
         if(tableName != null)
         {
            QTableMetaData table = qInstance.getTable(tableName);

            ///////////////////////////////////////////////////////////////
            // look for reasons we might exclude this api for this table //
            ///////////////////////////////////////////////////////////////
            ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
            if(apiTableMetaDataContainer == null)
            {
               continue;
            }

            ApiTableMetaData apiTableMetaData = apiTableMetaDataContainer.getApiTableMetaData(apiInstanceMetaData.getName());
            if(apiTableMetaData == null)
            {
               continue;
            }

            if(BooleanUtils.isTrue(apiTableMetaData.getIsExcluded()))
            {
               continue;
            }
         }

         apis.add(thisApi);
      }

      context.contentType(ContentType.APPLICATION_JSON);
      context.result(toJson(rs));
   }



   /*******************************************************************************
    ** list the versions in this api
    *******************************************************************************/
   private static void doVersions(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      Map<String, Object> rs = new HashMap<>();
      rs.put("supportedVersions", apiInstanceMetaData.getSupportedVersions().stream().map(String::valueOf).collect(Collectors.toList()));
      rs.put("currentVersion", apiInstanceMetaData.getCurrentVersion().toString());

      context.contentType(ContentType.APPLICATION_JSON);
      context.result(toJson(rs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setupCORS(Context context)
   {
      if(StringUtils.hasContent(context.header("origin")))
      {
         context.res().setHeader("access-control-allow-origin", context.header("origin"));
         context.res().setHeader("vary", "Origin");
      }
      else
      {
         context.res().setHeader("access-control-allow-origin", "*");
      }

      context.header("access-control-allow-methods", "GET, POST, DELETE, PUT, PATCH, OPTIONS");
      context.header("access-control-allow-headers", "x-requested-with, content-type, authorization, accept, content-type, authorization, accept, x-api-key");
      context.header("access-control-allow-credentials", "true");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doPathNotFound(Context context)
   {
      APILog apiLog = newAPILog(context);

      try
      {
         setupSession(context, null, null, null);
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////////////
         // if we don't have a session, we won't be able to store the api log... //
         //////////////////////////////////////////////////////////////////////////
         LOG.info("No session in a 404; will not create api log", e);
      }

      handleException(context, new QNotFoundException("Could not find any resources at path " + context.path()), apiLog);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doSpecYaml(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      try
      {
         QContext.init(qInstance, null);
         String version = context.pathParam("version");

         GenerateOpenApiSpecInput input = new GenerateOpenApiSpecInput().withVersion(version);
         input.setApiName(apiInstanceMetaData.getName());

         try
         {
            if(StringUtils.hasContent(context.pathParam("tableName")))
            {
               input.setTableName(context.pathParam("tableName"));
            }
         }
         catch(Exception e)
         {
            ///////////////////////////
            // leave table param out //
            ///////////////////////////
         }

         GenerateOpenApiSpecOutput output = new GenerateOpenApiSpecAction().execute(input);
         context.contentType(ContentType.APPLICATION_YAML);
         context.result(output.getYaml());
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doSpecJson(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      try
      {
         QContext.init(qInstance, null);
         String                   version = context.pathParam("version");
         GenerateOpenApiSpecInput input   = new GenerateOpenApiSpecInput().withVersion(version);
         input.setApiName(apiInstanceMetaData.getName());

         try
         {
            if(StringUtils.hasContent(context.pathParam("tableName")))
            {
               input.setTableName(context.pathParam("tableName"));
            }
         }
         catch(Exception e)
         {
            ///////////////////////////
            // leave table param out //
            ///////////////////////////
         }

         GenerateOpenApiSpecOutput output = new GenerateOpenApiSpecAction().execute(input);
         context.contentType(ContentType.JSON);
         context.result(output.getJson());
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doSpecHtml(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version;

      try
      {
         version = context.pathParam("version");
      }
      catch(Exception e)
      {
         version = apiInstanceMetaData.getCurrentVersion().toString();
      }

      doSpecHtml(context, version, apiInstanceMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doSpecHtml(Context context, String version, ApiInstanceMetaData apiInstanceMetaData)
   {
      try
      {
         QBrandingMetaData branding = qInstance.getBranding();

         if(!apiInstanceMetaData.getSupportedVersions().contains(new APIVersion(version)))
         {
            doPathNotFound(context);
            return;
         }

         //////////////////////////////////
         // read html from resource file //
         //////////////////////////////////
         InputStream resourceAsStream = QJavalinApiHandler.class.getClassLoader().getResourceAsStream("rapidoc/rapidoc-container.html");
         String      html             = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());

         /////////////////////////////////
         // do replacements in the html //
         /////////////////////////////////
         html = html.replace("{spec-url}", apiInstanceMetaData.getPath() + version + "/openapi.json");
         html = html.replace("{version}", version);
         html = html.replace("{primaryColor}", (branding == null || branding.getAccentColor() == null) ? "#FF791A" : branding.getAccentColor());

         if(branding != null && StringUtils.hasContent(branding.getLogo()))
         {
            html = html.replace("{navLogoImg}", "<img id=\"navLogo\" slot=\"nav-logo\" src=\"" + branding.getLogo() + "\" />");
         }
         else
         {
            html = html.replace("{navLogoImg}", "");
         }

         html = html.replace("{title}", apiInstanceMetaData.getLabel() + " - " + version);

         StringBuilder                otherApisOptions             = new StringBuilder();
         ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = ApiInstanceMetaDataContainer.of(qInstance);
         for(Map.Entry<String, ApiInstanceMetaData> entry : apiInstanceMetaDataContainer.getApis().entrySet())
         {
            otherApisOptions.append("<option value=\"").append(entry.getValue().getPath()).append("\">").append(entry.getValue().getLabel()).append("</option>");
         }
         html = html.replace("{otherApisOptions}", otherApisOptions.toString());

         StringBuilder otherVersionOptions = new StringBuilder();
         for(APIVersion supportedVersion : apiInstanceMetaData.getSupportedVersions())
         {
            otherVersionOptions.append("<option value=\"").append(apiInstanceMetaData.getPath()).append(supportedVersion).append("/openapi.html\">").append(supportedVersion).append("</option>");
         }
         html = html.replace("{otherVersionOptions}", otherVersionOptions.toString());

         context.contentType(ContentType.HTML);
         context.result(html);
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    ** Setup session for API requests using scoped authentication resolution.
    **
    ** <p>Resolves the appropriate authentication provider based on the API
    ** metadata, then creates a session using that provider. Falls back to
    ** instance default if no API-specific provider is registered.</p>
    **
    ** @param context The Javalin context
    ** @param input The action input (may be null)
    ** @param version The API version
    ** @param apiInstanceMetaData The API instance metadata (may be null)
    ** @throws QModuleDispatchException If module dispatch fails
    ** @throws QAuthenticationException If authentication fails
    *******************************************************************************/
   public static void setupSession(Context context, AbstractActionInput input, String version, ApiInstanceMetaData apiInstanceMetaData) throws QModuleDispatchException, QAuthenticationException
   {
      ///////////////////////////////////////////////////////////////////////////
      // Resolve the appropriate authentication provider for this API        //
      ///////////////////////////////////////////////////////////////////////////
      AuthResolutionContext resolutionContext = new AuthResolutionContext()
         .withRequestPath(context.path())
         .withApiMetaData(apiInstanceMetaData);

      QAuthenticationMetaData authMetaData;
      try
      {
         authMetaData = AuthenticationResolver.resolve(qInstance, resolutionContext);
      }
      catch(QException e)
      {
         // If resolver fails, fall back to instance default for backward compatibility
         LOG.warn("Failed to resolve scoped authentication, falling back to instance default", e);
         authMetaData = qInstance.getAuthentication();
      }

      ///////////////////////////////////////////////////////////////////////////
      // Setup session using the resolved authentication provider           //
      ///////////////////////////////////////////////////////////////////////////
      QSession session = QJavalinImplementation.setupSession(context, input, authMetaData);
      session.setValue("apiVersion", version);
      if(apiInstanceMetaData != null)
      {
         session.setValue("apiName", apiInstanceMetaData.getName());
         session.setValue("apiLabel", apiInstanceMetaData.getLabel());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doGet(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version      = context.pathParam("version");
      String tableApiName = context.pathParam("tableName");
      String primaryKey   = context.pathParam("primaryKey");
      APILog apiLog       = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiGet", logPair("table", tableApiName), logPair("primaryKey", primaryKey));

         Map<String, Serializable> outputRecord = ApiImplementation.get(apiInstanceMetaData, version, tableApiName, primaryKey);

         QJavalinAccessLogger.logEndSuccess();
         String resultString = toJson(outputRecord);
         context.result(resultString);
         storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(resultString));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    ** Define standard way we'll make JSON objects for the API.
    **
    ** Specifically, changes QQQ's default to include null values
    *******************************************************************************/
   private static String toJson(Object object)
   {
      return JsonUtils.toJson(object, mapper ->
      {
         mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APILog newAPILog(Context context)
   {
      APILog apiLog = new APILog()
         .withTimestamp(Instant.now())
         .withMethod(context.req().getMethod())
         .withPath(context.path())
         .withQueryString(context.queryString())
         .withRequestBody(context.body());

      try
      {
         apiLog.setVersion(context.pathParam("version"));
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////////////////////
         // pathParam throws if the param isn't found - in that case, just leave it null //
         //////////////////////////////////////////////////////////////////////////////////
      }

      return (apiLog);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void storeApiLog(APILog apiLog)
   {
      try
      {
         if(QContext.getQInstance().getTable(APILog.TABLE_NAME) != null)
         {
            QSession qSession = QContext.getQSession();
            if(qSession != null)
            {
               for(Map.Entry<String, List<Serializable>> entry : CollectionUtils.nonNullMap(qSession.getSecurityKeyValues()).entrySet())
               {
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // put the 1st entry for this key in the api log record                                                                                             //
                  // todo - might need revisited for users with multiple values...  e.g., look for the security key in records in the request?  or as part of the URL //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  if(CollectionUtils.nullSafeHasContents(entry.getValue()))
                  {
                     apiLog.withSecurityKeyValue(entry.getKey(), entry.getValue().get(0));
                  }
               }

               Integer userId = getApiLogUserId(qSession);
               apiLog.setApiLogUserId(userId);
            }

            InsertInput insertInput = new InsertInput();
            insertInput.setTableName(APILog.TABLE_NAME);
            insertInput.setRecords(List.of(apiLog.toQRecord()));
            new InsertAction().executeAsync(insertInput);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error storing API log", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Integer getApiLogUserId(QSession qSession) throws QException
   {
      String tableName = ApiInstanceMetaDataProvider.TABLE_NAME_API_LOG_USER;

      if(qSession == null)
      {
         return (null);
      }

      QUser qUser = qSession.getUser();
      if(qUser == null)
      {
         return (null);
      }

      String userName = qUser.getFullName();
      if(!StringUtils.hasContent(userName))
      {
         return (null);
      }

      /////////////////////////////////////////////////////////////////////////////
      // if we haven't cached this username to an id, query and/or insert it now //
      /////////////////////////////////////////////////////////////////////////////
      if(!apiLogUserIdCache.containsKey(userName))
      {
         //////////////////////////////////////////////////////////////
         // first try to get - if it's found, cache it and return it //
         //////////////////////////////////////////////////////////////
         Integer id = fetchApiLogUserIdFromName(userName);
         if(id != null)
         {
            apiLogUserIdCache.put(userName, id);
            return id;
         }

         try
         {
            ///////////////////////////////////////////////////////
            // if it wasn't found from a Get, then try an Insert //
            ///////////////////////////////////////////////////////
            LOG.info("Inserting " + tableName + " named " + userName);
            InsertInput insertInput = new InsertInput();
            insertInput.setTableName(tableName);
            QRecord record = new QRecord().withValue("name", userName);

            for(Map.Entry<String, List<Serializable>> entry : CollectionUtils.nonNullMap(qSession.getSecurityKeyValues()).entrySet())
            {
               //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // put the 1st entry for this key in the api log user record                                                                                        //
               // todo - might need revisited for users with multiple values...  e.g., look for the security key in records in the request?  or as part of the URL //
               //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(CollectionUtils.nullSafeHasContents(entry.getValue()))
               {
                  record.withValue(entry.getKey(), entry.getValue().get(0));
               }
            }

            insertInput.setRecords(List.of(record));
            InsertOutput insertOutput = new InsertAction().execute(insertInput);
            id = insertOutput.getRecords().get(0).getValueInteger("id");

            ////////////////////////////////////////
            // if we got an id, cache & return it //
            ////////////////////////////////////////
            if(id != null)
            {
               apiLogUserIdCache.put(userName, id);
               return id;
            }
         }
         catch(Exception e)
         {
            ////////////////////////////////////////////////////////////////////
            // assume this may mean a dupe-key - so - try another fetch below //
            ////////////////////////////////////////////////////////////////////
            LOG.info("Caught error inserting " + tableName + " named " + userName + " - will try to re-fetch", e);
         }

         //////////////////////////////////////////////////////////////////////////
         // if the insert failed, try another fetch (e.g., after a UK violation) //
         //////////////////////////////////////////////////////////////////////////
         id = fetchApiLogUserIdFromName(userName);
         if(id != null)
         {
            apiLogUserIdCache.put(userName, id);
            return id;
         }

         /////////////
         // give up //
         /////////////
         LOG.error("Unable to get id for " + tableName + " named " + userName);
         return (null);
      }

      return (apiLogUserIdCache.get(userName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Integer fetchApiLogUserIdFromName(String name) throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName(ApiInstanceMetaDataProvider.TABLE_NAME_API_LOG_USER);
      getInput.setUniqueKey(Map.of("name", name));
      GetOutput getOutput = new GetAction().execute(getInput);
      if(getOutput.getRecord() != null)
      {
         return (getOutput.getRecord().getValueInteger("id"));
      }

      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doQuery(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version      = context.pathParam("version");
      String tableApiName = context.pathParam("tableName");

      QQueryFilter filter = null;
      APILog       apiLog = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiQuery", logPair("table", tableApiName));

         Map<String, Serializable> output = ApiImplementation.query(apiInstanceMetaData, version, tableApiName, context.queryParamMap());

         QJavalinAccessLogger.logEndSuccess(logPair("recordCount", () -> ((List<?>) output.get("records")).size()), QJavalinAccessLogger.logPairIfSlow("filter", filter, SLOW_LOG_THRESHOLD_MS));
         String resultString = toJson(output);
         context.result(resultString);
         storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(resultString));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e, logPair("filter", filter));
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doInsert(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version      = context.pathParam("version");
      String tableApiName = context.pathParam("tableName");
      APILog apiLog       = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiInsert", logPair("table", tableApiName));

         Map<String, Serializable> outputRecord = ApiImplementation.insert(apiInstanceMetaData, version, tableApiName, context.body());

         QJavalinAccessLogger.logEndSuccess();
         context.status(HttpStatus.Code.CREATED.getCode());
         String resultString = toJson(outputRecord);
         context.result(resultString);
         storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(resultString));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void bulkInsert(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version      = context.pathParam("version");
      String tableApiName = context.pathParam("tableName");
      APILog apiLog       = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiBulkInsert", logPair("table", tableApiName));

         List<Map<String, Serializable>> response = ApiImplementation.bulkInsert(apiInstanceMetaData, version, tableApiName, context.body());

         context.status(HttpStatus.Code.MULTI_STATUS.getCode());
         String resultString = toJson(response);
         context.result(resultString);
         storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(resultString));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void bulkUpdate(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version      = context.pathParam("version");
      String tableApiName = context.pathParam("tableName");
      APILog apiLog       = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiBulkUpdate", logPair("table", tableApiName));

         List<Map<String, Serializable>> response = ApiImplementation.bulkUpdate(apiInstanceMetaData, version, tableApiName, context.body());

         QJavalinAccessLogger.logEndSuccess(logPair("recordCount", response.size()));
         context.status(HttpStatus.Code.MULTI_STATUS.getCode());
         String resultString = toJson(response);
         context.result(resultString);
         storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(resultString));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void bulkDelete(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version      = context.pathParam("version");
      String tableApiName = context.pathParam("tableName");
      APILog apiLog       = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiBulkDelete", logPair("table", tableApiName));

         List<Map<String, Serializable>> response = ApiImplementation.bulkDelete(apiInstanceMetaData, version, tableApiName, context.body());

         QJavalinAccessLogger.logEndSuccess(logPair("recordCount", response.size()));
         context.status(HttpStatus.Code.MULTI_STATUS.getCode());
         String resultString = toJson(response);
         context.result(resultString);
         storeApiLog(apiLog.withStatusCode(context.statusCode()).withResponseBody(resultString));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doUpdate(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version      = context.pathParam("version");
      String tableApiName = context.pathParam("tableName");
      String primaryKey   = context.pathParam("primaryKey");
      APILog apiLog       = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiUpdate", logPair("table", tableApiName));

         ApiImplementation.update(apiInstanceMetaData, version, tableApiName, primaryKey, context.body());

         QJavalinAccessLogger.logEndSuccess();
         context.status(HttpStatus.Code.NO_CONTENT.getCode());
         storeApiLog(apiLog.withStatusCode(context.statusCode()));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doDelete(Context context, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version      = context.pathParam("version");
      String tableApiName = context.pathParam("tableName");
      String primaryKey   = context.pathParam("primaryKey");
      APILog apiLog       = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiDelete", logPair("table", tableApiName));

         ApiImplementation.delete(apiInstanceMetaData, version, tableApiName, primaryKey);

         QJavalinAccessLogger.logEndSuccess();
         context.status(HttpStatus.Code.NO_CONTENT.getCode());
         storeApiLog(apiLog.withStatusCode(context.statusCode()));
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void handleException(Context context, Exception e, APILog apiLog)
   {
      handleException(null, context, e, apiLog);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void handleException(Context context, Exception e)
   {
      handleException(null, context, e, null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("UnnecessaryReturnStatement")
   public static void handleException(HttpStatus.Code statusCode, Context context, Exception e, APILog apiLog)
   {
      QBadRequestException badRequestException = ExceptionUtils.findClassInRootChain(e, QBadRequestException.class);
      if(badRequestException != null)
      {
         statusCode = Objects.requireNonNullElse(statusCode, HttpStatus.Code.BAD_REQUEST); // 400
         respondWithError(context, statusCode, badRequestException.getMessage(), apiLog);
         return;
      }

      QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(e, QUserFacingException.class);
      if(userFacingException != null)
      {
         if(userFacingException instanceof QNotFoundException)
         {
            statusCode = Objects.requireNonNullElse(statusCode, HttpStatus.Code.NOT_FOUND); // 404
            respondWithError(context, statusCode, userFacingException.getMessage(), apiLog);
            return;
         }
         else
         {
            LOG.info("User-facing exception", e);
            statusCode = Objects.requireNonNullElse(statusCode, HttpStatus.Code.INTERNAL_SERVER_ERROR); // 500
            respondWithError(context, statusCode, userFacingException.getMessage(), apiLog);
            return;
         }
      }
      else
      {
         if(e instanceof QAuthenticationException)
         {
            respondWithError(context, HttpStatus.Code.UNAUTHORIZED, "The required authentication credentials were missing or invalid.", apiLog); // 401
            return;
         }

         if(e instanceof QPermissionDeniedException)
         {
            respondWithError(context, HttpStatus.Code.FORBIDDEN, "You do not have permission to access the requested resource.", apiLog); // 403
            return;
         }

         ////////////////////////////////
         // default exception handling //
         ////////////////////////////////
         LOG.warn("Exception in javalin request", e);
         String message = e.getMessage();
         if(!StringUtils.hasContent(message))
         {
            message = e.getClass().getSimpleName();
         }
         respondWithError(context, HttpStatus.Code.INTERNAL_SERVER_ERROR, message, apiLog); // 500
         return;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void respondWithError(Context context, HttpStatus.Code statusCode, String errorMessage, APILog apiLog)
   {
      context.status(statusCode.getCode());

      try
      {
         String accept = context.header("accept");
         if(accept != null && accept.toLowerCase().startsWith(ContentType.HTML))
         {
            context.contentType(ContentType.HTML);
            context.result("Error: " + errorMessage);
            return;
         }
      }
      catch(Exception e)
      {
         ///////////////////////////
         // just do default thing //
         ///////////////////////////
      }

      String responseBody = toJson(Map.of("error", errorMessage));
      context.result(responseBody);

      if(apiLog != null)
      {
         if(QContext.getQSession() != null && QContext.getQInstance() != null)
         {
            apiLog.withStatusCode(statusCode.getCode()).withResponseBody(responseBody);
            storeApiLog(apiLog);
         }
      }
   }



   /*******************************************************************************
    ** Define the routes
    *******************************************************************************/
   public EndpointGroup getRoutes()
   {
      return (() ->
      {
         ///////////////////////////////////////////////
         // static endpoints to support rapidoc pages //
         ///////////////////////////////////////////////
         try
         {
            ApiBuilder.get("/api/docs/js/rapidoc.min.js", (context) -> QJavalinApiHandler.serveResource(context, "rapidoc/rapidoc-9.3.8.min.js", MapBuilder.of("Content-Type", ContentType.JAVASCRIPT)));
            ApiBuilder.get("/api/docs/css/qqq-api-styles.css", (context) -> QJavalinApiHandler.serveResource(context, "rapidoc/rapidoc-overrides.css", MapBuilder.of("Content-Type", ContentType.CSS)));
         }
         catch(IllegalArgumentException iae)
         {
            //////////////////////////////////////////////////////////////
            // assume a different module already registered these paths //
            //////////////////////////////////////////////////////////////
         }

         ApiBuilder.get("/apis.json", QJavalinApiHandler::doGetApisJson);

         // todo not this? ApiBuilder.get("/api/", QJavalinApiHandler::doListApis);

         ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = ApiInstanceMetaDataContainer.of(qInstance);
         for(Map.Entry<String, ApiInstanceMetaData> entry : apiInstanceMetaDataContainer.getApis().entrySet())
         {
            ApiInstanceMetaData apiInstanceMetaData = entry.getValue();
            String              rootPath            = apiInstanceMetaData.getPath();

            ApiBuilder.before(rootPath + "*", QJavalinApiHandler::setupCORS);

            //////////////////////////////////////////////
            // default page is the current version spec //
            //////////////////////////////////////////////
            ApiBuilder.get(rootPath, context -> doSpecHtml(context, apiInstanceMetaData));
            ApiBuilder.get(rootPath + "versions.json", context -> doVersions(context, apiInstanceMetaData));

            ApiBuilder.path(rootPath + "{version}", () ->
            {
               ////////////////////////////////////////////
               // default page for a version is its spec //
               ////////////////////////////////////////////
               ApiBuilder.get("/", context -> doSpecHtml(context, apiInstanceMetaData));

               ///////////////////////////////////////////
               // add known paths for specs & docs page //
               ///////////////////////////////////////////
               ApiBuilder.get("/openapi.yaml", context -> doSpecYaml(context, apiInstanceMetaData));
               ApiBuilder.get("/openapi.json", context -> doSpecJson(context, apiInstanceMetaData));
               ApiBuilder.get("/openapi.html", context -> doSpecHtml(context, apiInstanceMetaData));

               ///////////////////
               // add processes //
               ///////////////////
               for(QProcessMetaData process : qInstance.getProcesses().values())
               {
                  ApiProcessMetaDataContainer apiProcessMetaDataContainer = Objects.requireNonNullElse(ApiProcessMetaDataContainer.of(process), EMPTY_API_PROCESS_META_DATA_CONTAINER);
                  ApiProcessMetaData          apiProcessMetaData          = apiProcessMetaDataContainer.getApis().get(apiInstanceMetaData.getName());

                  if(apiProcessMetaData != null && !BooleanUtils.isTrue(apiProcessMetaData.getIsExcluded()))
                  {
                     String     path   = ApiProcessUtils.getProcessApiPath(qInstance, process, apiProcessMetaData, apiInstanceMetaData);
                     HttpMethod method = apiProcessMetaData.getMethod();
                     switch(method)
                     {
                        case GET -> ApiBuilder.get(path, context -> runProcess(context, process, apiProcessMetaData, apiInstanceMetaData));
                        case POST -> ApiBuilder.post(path, context -> runProcess(context, process, apiProcessMetaData, apiInstanceMetaData));
                        case PUT -> ApiBuilder.put(path, context -> runProcess(context, process, apiProcessMetaData, apiInstanceMetaData));
                        case PATCH -> ApiBuilder.patch(path, context -> runProcess(context, process, apiProcessMetaData, apiInstanceMetaData));
                        case DELETE -> ApiBuilder.delete(path, context -> runProcess(context, process, apiProcessMetaData, apiInstanceMetaData));
                        default -> throw (new QRuntimeException("Unrecognized http method [" + method + "] for process [" + process.getName() + "]"));
                     }

                     make405sForOtherMethods(method, path);

                     if(!ApiProcessMetaData.AsyncMode.NEVER.equals(apiProcessMetaData.getAsyncMode()))
                     {
                        ApiBuilder.get(path + "/status/{jobId}", context -> getProcessStatus(context, process, apiProcessMetaData, apiInstanceMetaData));
                     }
                  }
               }

               ///////////////////////////////////
               // add wildcard paths for tables //
               ///////////////////////////////////
               ApiBuilder.path("/{tableName}", () ->
               {
                  ApiBuilder.get("/openapi.yaml", context -> doSpecYaml(context, apiInstanceMetaData));
                  ApiBuilder.get("/openapi.json", context -> doSpecJson(context, apiInstanceMetaData));

                  ApiBuilder.post("/", context -> doInsert(context, apiInstanceMetaData));

                  ApiBuilder.get("/query", context -> doQuery(context, apiInstanceMetaData));
                  // ApiBuilder.post("/query", context -> doQuery(context, apiInstanceMetaData));

                  ApiBuilder.post("/bulk", context -> bulkInsert(context, apiInstanceMetaData));
                  ApiBuilder.patch("/bulk", context -> bulkUpdate(context, apiInstanceMetaData));
                  ApiBuilder.delete("/bulk", context -> bulkDelete(context, apiInstanceMetaData));

                  //////////////////////////////////////////////////////////////////
                  // remember to keep the wildcard paths after the specific paths //
                  //////////////////////////////////////////////////////////////////
                  ApiBuilder.get("/{primaryKey}", context -> doGet(context, apiInstanceMetaData));
                  ApiBuilder.patch("/{primaryKey}", context -> doUpdate(context, apiInstanceMetaData));
                  ApiBuilder.delete("/{primaryKey}", context -> doDelete(context, apiInstanceMetaData));
               });
            });

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // default all other requests under the root path (for the methods we support) to a standard 404 response //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ApiBuilder.get(rootPath + "*", QJavalinApiHandler::doPathNotFound);
            ApiBuilder.delete(rootPath + "*", QJavalinApiHandler::doPathNotFound);
            ApiBuilder.patch(rootPath + "*", QJavalinApiHandler::doPathNotFound);
            ApiBuilder.post(rootPath + "*", QJavalinApiHandler::doPathNotFound);
         }

         ///////////////////////////////////////////////////////////////////////////////////
         // if the main implementation class has a hot-swapper installed, use it here too //
         ///////////////////////////////////////////////////////////////////////////////////
         if(QJavalinImplementation.getQInstanceHotSwapSupplier() != null)
         {
            ApiBuilder.before((context) ->
            {
               QJavalinImplementation.hotSwapQInstance(context);
               QJavalinApiHandler.qInstance = QJavalinImplementation.getQInstance();
            });
         }
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void make405sForOtherMethods(HttpMethod allowedMethod, String path)
   {
      if(!allowedMethod.equals(HttpMethod.GET))
      {
         ApiBuilder.get(path, (Context c) -> QJavalinApiHandler.return405(c, allowedMethod));
      }

      if(!allowedMethod.equals(HttpMethod.POST))
      {
         ApiBuilder.post(path, (Context c) -> QJavalinApiHandler.return405(c, allowedMethod));
      }

      if(!allowedMethod.equals(HttpMethod.PUT))
      {
         ApiBuilder.put(path, (Context c) -> QJavalinApiHandler.return405(c, allowedMethod));
      }

      if(!allowedMethod.equals(HttpMethod.PATCH))
      {
         ApiBuilder.patch(path, (Context c) -> QJavalinApiHandler.return405(c, allowedMethod));
      }

      if(!allowedMethod.equals(HttpMethod.DELETE))
      {
         ApiBuilder.delete(path, (Context c) -> QJavalinApiHandler.return405(c, allowedMethod));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runProcess(Context context, QProcessMetaData processMetaData, ApiProcessMetaData apiProcessMetaData, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version = context.pathParam("version");
      APILog apiLog  = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiRunProcess", logPair("process", processMetaData.getName()));

         ////////////////////////////////////////////////////
         // process inputs into map for api implementation //
         ////////////////////////////////////////////////////
         Map<String, String> parameters = new LinkedHashMap<>();
         ApiProcessInput     input      = apiProcessMetaData.getInput();
         if(input != null)
         {
            processProcessInputFieldsContainer(context, parameters, input.getPathParams(), Context::pathParam);
            processProcessInputFieldsContainer(context, parameters, input.getQueryStringParams(), Context::queryParam);
            processProcessInputFieldsContainer(context, parameters, input.getFormParams(), Context::formParam);

            ApiProcessInputFieldsContainer objectBodyParams = input.getObjectBodyParams();
            if(objectBodyParams != null)
            {
               JSONObject jsonObject = new JSONObject(context.body());
               processProcessInputFieldsContainer(context, parameters, objectBodyParams, (ctx, name) -> jsonObject.optString(name, null));
            }

            if(input.getBodyField() != null)
            {
               parameters.put(input.getBodyField().getName(), context.body());
            }
         }

         if(ApiProcessMetaData.AsyncMode.OPTIONAL.equals(apiProcessMetaData.getAsyncMode()))
         {
            parameters.put("async", context.queryParam("async"));
         }

         /////////////////////
         // run the process //
         /////////////////////
         HttpApiResponse response = ApiImplementation.runProcess(apiInstanceMetaData, version, apiProcessMetaData.getApiProcessName(), parameters);

         //////////////////
         // log & return //
         //////////////////
         QJavalinAccessLogger.logEndSuccess();
         context.status(response.getStatusCode().getCode());
         handleProcessResponse(context, response, apiLog);
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void getProcessStatus(Context context, QProcessMetaData processMetaData, ApiProcessMetaData apiProcessMetaData, ApiInstanceMetaData apiInstanceMetaData)
   {
      String version = context.pathParam("version");
      APILog apiLog  = newAPILog(context);

      try
      {
         setupSession(context, null, version, apiInstanceMetaData);
         QJavalinAccessLogger.logStart("apiGetProcessStatus", logPair("process", processMetaData.getName()));

         String          jobId    = context.pathParam("jobId");
         HttpApiResponse response = ApiImplementation.getProcessStatus(apiInstanceMetaData, version, apiProcessMetaData.getApiProcessName(), jobId);

         //////////////////
         // log & return //
         //////////////////
         QJavalinAccessLogger.logEndSuccess();
         context.status(response.getStatusCode().getCode());
         handleProcessResponse(context, response, apiLog);
      }
      catch(Exception e)
      {
         QJavalinAccessLogger.logEndFail(e);
         handleException(context, e, apiLog);
      }
   }

}
