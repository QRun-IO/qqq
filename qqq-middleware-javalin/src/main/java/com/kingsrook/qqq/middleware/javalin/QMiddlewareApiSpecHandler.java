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


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.openapi.model.OpenAPI;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.apache.commons.io.IOUtils;


/*******************************************************************************
 ** javalin-handler that serves both rapidoc static html/css/js files, and
 ** dynamically generated openapi json/yaml, for a given list of qqq middleware
 ** versions
 *******************************************************************************/
public class QMiddlewareApiSpecHandler
{
   private final List<AbstractMiddlewareVersion> middlewareVersionList;
   private final String                          basePath;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QMiddlewareApiSpecHandler(List<AbstractMiddlewareVersion> middlewareVersionList)
   {
      this(middlewareVersionList, "qqq");
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QMiddlewareApiSpecHandler(List<AbstractMiddlewareVersion> middlewareVersionList, String basePath)
   {
      this.middlewareVersionList = middlewareVersionList;
      this.basePath = basePath.replaceFirst("^/+", "").replaceFirst("/+$", "");;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public EndpointGroup defineJavalinEndpointGroup()
   {
      return (() ->
      {
         ApiBuilder.get("/api/docs/js/rapidoc.min.js", (context) -> serveResource(context, "rapidoc/rapidoc-9.3.8.min.js", MapBuilder.of("Content-Type", ContentType.JAVASCRIPT)));
         ApiBuilder.get("/api/docs/css/qqq-api-styles.css", (context) -> serveResource(context, "rapidoc/rapidoc-overrides.css", MapBuilder.of("Content-Type", ContentType.CSS)));
         ApiBuilder.get("/images/qqq-api-logo.png", (context) -> serveResource(context, "images/qqq-on-crown-trans-160x80.png", MapBuilder.of("Content-Type", ContentType.IMAGE_PNG.getMimeType())));

         //////////////////////////////////////////////
         // default page is the current version spec //
         //////////////////////////////////////////////
         ApiBuilder.get("/" + basePath + "/", context -> doSpecHtml(context));
         ApiBuilder.get("/" + basePath + "/versions.json", context -> doVersions(context));

         ////////////////////////////////////////////
         // default page for a version is its spec //
         ////////////////////////////////////////////
         for(AbstractMiddlewareVersion middlewareVersion : middlewareVersionList)
         {
            String version     = middlewareVersion.getVersion();
            String versionPath = "/" + basePath + middlewareVersion.getVersionBasePath();
            ApiBuilder.get(versionPath + "/", context -> doSpecHtml(middlewareVersion, context, version));

            ///////////////////////////////////////////
            // add known paths for specs & docs page //
            ///////////////////////////////////////////
            ApiBuilder.get(versionPath + "/openapi.yaml", context -> doSpecYaml(middlewareVersion, context, version));
            ApiBuilder.get(versionPath + "/openapi.json", context -> doSpecJson(middlewareVersion, context, version));
            ApiBuilder.get(versionPath + "/openapi.html", context -> doSpecHtml(middlewareVersion, context, version));
         }
      });
   }



   /*******************************************************************************
    ** list the versions in this api
    *******************************************************************************/
   private void doVersions(Context context)
   {
      Map<String, Object> rs = new HashMap<>();

      List<String> supportedVersions = middlewareVersionList.stream().map(msi -> msi.getVersion()).toList();
      String       currentVersion    = supportedVersions.get(supportedVersions.size() - 1);

      rs.put("supportedVersions", supportedVersions);
      rs.put("currentVersion", currentVersion);

      context.contentType(ContentType.APPLICATION_JSON);
      context.result(JsonUtils.toJson(rs));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void serveResource(Context context, String resourcePath, Map<String, String> headers)
   {
      InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
      for(Map.Entry<String, String> entry : CollectionUtils.nonNullMap(headers).entrySet())
      {
         context.header(entry.getKey(), entry.getValue());
      }
      context.result(resourceAsStream);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void doSpecYaml(AbstractMiddlewareVersion middlewareVersion, Context context, String version)
   {
      try
      {
         OpenAPI openAPI = middlewareVersion.generateOpenAPIModel(basePath);
         context.contentType(ContentType.APPLICATION_YAML);
         context.result(YamlUtils.toYaml(openAPI));
      }
      catch(Exception e)
      {
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void doSpecJson(AbstractMiddlewareVersion middlewareVersion, Context context, String version)
   {
      try
      {
         OpenAPI openAPI = middlewareVersion.generateOpenAPIModel(basePath);
         context.contentType(ContentType.APPLICATION_JSON);
         context.result(JsonUtils.toJson(openAPI));
      }
      catch(Exception e)
      {
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void doSpecHtml(Context context)
   {
      String version = null;

      try
      {
         version = context.pathParam("version");
      }
      catch(Exception e)
      {
         ////////////////
         // leave null //
         ////////////////
      }

      if(!StringUtils.hasContent(version))
      {
         List<String> supportedVersions = middlewareVersionList.stream().map(mv -> mv.getVersion()).toList();
         version = supportedVersions.get(supportedVersions.size() - 1);
      }

      String finalVersion = version;
      Optional<AbstractMiddlewareVersion> optionalMiddlewareVersion = middlewareVersionList.stream().filter(mv -> mv.getVersion().equals(finalVersion)).findFirst();

      doSpecHtml(optionalMiddlewareVersion.orElseThrow(), context, version);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void doSpecHtml(AbstractMiddlewareVersion middlewareVersion, Context context, String version)
   {
      try
      {
         //////////////////////////////////
         // read html from resource file //
         //////////////////////////////////
         InputStream resourceAsStream = QMiddlewareApiSpecHandler.class.getClassLoader().getResourceAsStream("rapidoc/rapidoc-container.html");
         String      html             = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);

         /////////////////////////////////
         // do replacements in the html //
         /////////////////////////////////
         html = html.replace("{spec-url}", "/" + basePath + "/" + version + "/openapi.json");
         html = html.replace("{version}", version);
         html = html.replace("{primaryColor}", "#444444");
         html = html.replace("{navLogoImg}", "<img id=\"navLogo\" slot=\"nav-logo\" src=\"/images/qqq-api-logo.png\" />");

         OpenAPI openAPI = middlewareVersion.generateOpenAPIModel(basePath);
         html = html.replace("{title}", openAPI.getInfo().getTitle() + " - " + version);

         StringBuilder otherVersionOptions = new StringBuilder();
         for(AbstractMiddlewareVersion otherVersionSpec : middlewareVersionList)
         {
            otherVersionOptions.append("<option value=\"/").append(basePath).append("/").append(otherVersionSpec.getVersion()).append("/openapi.html\">").append(otherVersionSpec.getVersion()).append("</option>");
         }

         html = html.replace("{otherVersionOptions}", otherVersionOptions.toString());

         context.contentType(ContentType.HTML);
         context.result(html);
      }
      catch(Exception e)
      {
         QJavalinImplementation.handleException(context, e);
      }
   }

}
