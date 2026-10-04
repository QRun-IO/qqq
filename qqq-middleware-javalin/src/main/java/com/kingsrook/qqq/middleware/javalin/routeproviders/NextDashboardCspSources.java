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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** Public source additions derived from configured application features. The
 ** value deliberately contains no credentials, environment values or payloads.
 *******************************************************************************/
public final class NextDashboardCspSources implements ToSchema
{
   @OpenAPIExclude
   public static final String QUICKSIGHT_FRAME_SOURCE = "https://*.quicksight.aws.amazon.com";

   @OpenAPIDescription("HTTP(S) connection sources required by configured features; no default policy sources.")
   @JsonInclude(JsonInclude.Include.ALWAYS)
   @OpenAPIListItems(String.class)
   private final List<String> connectSrc;
   @OpenAPIDescription("Script sources required by configured features; no inline hashes, nonces or unsafe keywords.")
   @JsonInclude(JsonInclude.Include.ALWAYS)
   @OpenAPIListItems(String.class)
   private final List<String> scriptSrc;
   @OpenAPIDescription("Frame sources required by configured features.")
   @JsonInclude(JsonInclude.Include.ALWAYS)
   @OpenAPIListItems(String.class)
   private final List<String> frameSrc;
   @OpenAPIDescription("Style sources required by configured features.")
   @JsonInclude(JsonInclude.Include.ALWAYS)
   @OpenAPIListItems(String.class)
   private final List<String> styleSrc;



   /*******************************************************************************
    ** Keep source ordering and prevent callers from changing a cached policy.
    *******************************************************************************/
   private NextDashboardCspSources(Set<String> connect, Set<String> script, Set<String> frame, Set<String> style)
   {
      connectSrc = List.copyOf(connect);
      scriptSrc = List.copyOf(script);
      frameSrc = List.copyOf(frame);
      styleSrc = List.copyOf(style);
   }



   /*******************************************************************************
    ** Uses the same metadata derivation for hosted and standalone dashboards.
    *******************************************************************************/
   public static NextDashboardCspSources fromInstance(QInstance qInstance)
   {
      if(qInstance == null)
      {
         return (new NextDashboardCspSources(Set.of(), Set.of(), Set.of(), Set.of()));
      }
      NextDashboardAnalyticsOrigins analytics = new NextDashboardAnalyticsOrigins(qInstance.getEnvironmentValues());

      Set<String> connect = new LinkedHashSet<>(identityProviderOrigins(qInstance));
      connect.addAll(analytics.getConnect());

      Set<String> script = new LinkedHashSet<>(customComponentOrigins(qInstance));
      script.addAll(analytics.getScript());

      Set<String> frame = new LinkedHashSet<>();
      Set<String> style = new LinkedHashSet<>();
      if(hasQuickSightWidget(qInstance))
      {
         frame.add(QUICKSIGHT_FRAME_SOURCE);
      }
      Map<String, String> environment = qInstance.getEnvironmentValues();
      if(environment != null && environment.get("GOOGLE_APP_CLIENT_ID") != null && !environment.get("GOOGLE_APP_CLIENT_ID").isBlank()
         && environment.get("GOOGLE_APP_API_KEY") != null && !environment.get("GOOGLE_APP_API_KEY").isBlank())
      {
         script.add("https://accounts.google.com/gsi/client");
         script.add("https://apis.google.com");
         connect.add("https://accounts.google.com/gsi/");
         frame.add("https://accounts.google.com/gsi/");
         frame.add("https://docs.google.com");
         style.add("https://accounts.google.com/gsi/style");
      }
      return (new NextDashboardCspSources(connect, script, frame, style));
   }


   /*******************************************************************************
    ** Origins of the instance's OAUTH2 and AUTH_0 identity providers, which the
    ** dashboard calls from the browser (OIDC discovery, the Auth0 token exchange).
    *******************************************************************************/
   private static Set<String> identityProviderOrigins(QInstance qInstance)
   {
      List<QAuthenticationMetaData> providers = new ArrayList<>();
      providers.add(qInstance.getAuthentication());
      if(qInstance.getScopedAuthenticationProviders() != null)
      {
         providers.addAll(qInstance.getScopedAuthenticationProviders().values());
      }

      Set<String> origins = new LinkedHashSet<>();
      for(QAuthenticationMetaData provider : providers)
      {
         String baseUrl = null;
         if(provider instanceof OAuth2AuthenticationMetaData oauth2)
         {
            baseUrl = oauth2.getBaseUrl();
         }
         else if(provider instanceof Auth0AuthenticationMetaData auth0)
         {
            baseUrl = auth0.getBaseUrl();
         }
         String origin = NextDashboardSecurityHeaders.originOf(baseUrl);
         if(origin != null)
         {
            origins.add(origin);
         }
      }
      return (origins);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean hasQuickSightWidget(QInstance qInstance)
   {
      return (widgets(qInstance).stream().anyMatch(widget -> WidgetType.QUICK_SIGHT_CHART.getType().equals(widget.getType())));
   }



   /*******************************************************************************
    ** Origins of the script bundles that customComponent widgets load (their
    ** componentSourceUrl default value), when they are on another origin.
    *******************************************************************************/
   private static Set<String> customComponentOrigins(QInstance qInstance)
   {
      Set<String> origins = new LinkedHashSet<>();
      for(QWidgetMetaDataInterface widget : widgets(qInstance))
      {
         if(WidgetType.CUSTOM_COMPONENT.getType().equals(widget.getType()) && widget.getDefaultValues() != null)
         {
            Object sourceUrl = widget.getDefaultValues().get("componentSourceUrl");
            String origin    = NextDashboardSecurityHeaders.originOf(sourceUrl == null ? null : String.valueOf(sourceUrl));
            if(origin != null)
            {
               origins.add(origin);
            }
         }
      }
      return (origins);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QWidgetMetaDataInterface> widgets(QInstance qInstance)
   {
      Map<String, QWidgetMetaDataInterface> widgets = qInstance.getWidgets();
      return (widgets == null ? List.of() : widgets.values().stream().filter(Objects::nonNull).toList());
   }



   /*******************************************************************************
    ** Immutable source list.
    *******************************************************************************/
   public List<String> getConnectSrc()
   {
      return (connectSrc);
   }



   /*******************************************************************************
    ** Immutable source list.
    *******************************************************************************/
   public List<String> getScriptSrc()
   {
      return (scriptSrc);
   }



   /*******************************************************************************
    ** Immutable source list.
    *******************************************************************************/
   public List<String> getFrameSrc()
   {
      return (frameSrc);
   }



   /*******************************************************************************
    ** Immutable source list.
    *******************************************************************************/
   public List<String> getStyleSrc()
   {
      return (styleSrc);
   }



}
