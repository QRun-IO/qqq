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


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for NextDashboardRouteProvider
 *******************************************************************************/
class NextDashboardRouteProviderTest
{
   private Javalin service;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      if(service != null)
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Exact entries win; otherwise the "_" placeholder directory matches.
    *******************************************************************************/
   @Test
   void testResolve()
   {
      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("test-next-dashboard");
      assertEquals("index.html", provider.resolve("/"));
      assertEquals("app/index.html", provider.resolve("/app"));
      assertEquals("app/index.html", provider.resolve("/app/"));
      assertEquals("app/_/index.html", provider.resolve("/app/person"));
      assertEquals("app/_/__next._tree.txt", provider.resolve("/app/person/__next._tree.txt"));
      assertEquals("app/_/_/index.html", provider.resolve("/app/person/42/"));
      assertEquals("app/_/_/edit/index.html", provider.resolve("/app/person/42/edit"));
      assertEquals("app/developer/index.html", provider.resolve("/app/developer"));
      assertEquals("app/_/index.html", provider.resolve("/app/a%20b"));
      assertEquals("_next/static/chunks/main.js", provider.resolve("/_next/static/chunks/main.js"));
      assertNull(provider.resolve("/app/person/42/unknown/deeper"));
      assertNull(provider.resolve("/metaData"));
      assertNull(provider.resolve("/app/../index.html"));
      assertNull(provider.resolve("/app/%2e%2e/index.html"));
      assertNull(provider.resolve("/app/person/42/..%2Fedit"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingExportFailsFast()
   {
      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("no-such-next-dashboard");
      assertThrows(IllegalStateException.class, () -> Javalin.create(provider::acceptJavalinConfig));
   }



   /*******************************************************************************
    ** Served only when no endpoint matched: API routes, including their own 404s,
    ** keep their responses.
    *******************************************************************************/
   @Test
   void testServing() throws Exception
   {
      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("test-next-dashboard");
      service = Javalin.create(config ->
      {
         config.routes.get("/data/{table}/{id}", context -> context.status(404).json("{\"error\":\"not found\"}"));
         config.routes.get("/metaData", context -> context.result("metadata"));
         provider.acceptJavalinConfig(config);
      }).start(0);

      HttpClient client = HttpClient.newHttpClient();
      String     base   = "http://localhost:" + service.port();

      HttpResponse<String> record = client.send(HttpRequest.newBuilder(URI.create(base + "/app/person/1")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, record.statusCode());
      assertEquals("<html>record</html>\n", record.body());
      assertThat(record.headers().firstValue("Content-Type").orElse("")).startsWith("text/html");
      assertEquals("no-cache", record.headers().firstValue("Cache-Control").orElse(""));

      HttpResponse<String> asset = client.send(HttpRequest.newBuilder(URI.create(base + "/_next/static/chunks/main.js")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, asset.statusCode());
      assertThat(asset.headers().firstValue("Cache-Control").orElse("")).contains("immutable");
      assertThat(asset.headers().firstValue("Content-Type").orElse("")).startsWith("text/javascript");

      HttpResponse<String> api = client.send(HttpRequest.newBuilder(URI.create(base + "/data/person/99")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(404, api.statusCode());
      assertThat(api.body()).contains("not found").doesNotContain("<html>");

      assertEquals("metadata", client.send(HttpRequest.newBuilder(URI.create(base + "/metaData")).build(), HttpResponse.BodyHandlers.ofString()).body());

      HttpResponse<String> unknownPage = client.send(HttpRequest.newBuilder(URI.create(base + "/app/person/1/unknown/deeper")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(404, unknownPage.statusCode());
      assertEquals("<html>not found</html>\n", unknownPage.body());

      HttpResponse<String> unknownFile = client.send(HttpRequest.newBuilder(URI.create(base + "/missing.js")).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(404, unknownFile.statusCode());
      assertThat(unknownFile.body()).doesNotContain("<html>");

      HttpResponse<String> post = client.send(HttpRequest.newBuilder(URI.create(base + "/app/person")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(404, post.statusCode());
      assertThat(post.body()).doesNotContain("<html>");
   }



   /*******************************************************************************
    ** Documents get the strict policy with the hashes of their own inline scripts;
    ** every served file gets the other security headers (QRun-IO/qqq#695).
    *******************************************************************************/
   @Test
   void testSecurityHeaders() throws Exception
   {
      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("test-next-dashboard");
      String                     base     = start(provider);

      HttpResponse<String> login = get(base + "/login/");
      assertEquals(200, login.statusCode());
      String expectedPolicy = "default-src 'self'; "
         + "script-src 'self' " + sha256("(self.__next_f=self.__next_f||[]).push([0])") + " " + sha256("self.__next_f.push([1,\"login\"])") + "; "
         + "style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: https:; font-src 'self' data:; connect-src 'self'; frame-src 'self'; "
         + "worker-src 'self' blob:; manifest-src 'self'; media-src 'self' data: blob:; object-src 'none'; base-uri 'self'; "
         + "form-action 'self'; frame-ancestors 'none'";
      assertEquals(expectedPolicy, header(login, "Content-Security-Policy"));
      assertEquals("DENY", header(login, "X-Frame-Options"));
      assertEquals("strict-origin-when-cross-origin", header(login, "Referrer-Policy"));
      assertEquals(NextDashboardSecurityHeaders.DEFAULT_PERMISSIONS_POLICY, header(login, "Permissions-Policy"));
      assertThat(header(login, "Permissions-Policy")).contains("camera=()", "microphone=()", "geolocation=()", "payment=()");
      assertEquals("nosniff", header(login, "X-Content-Type-Options"));
      assertNull(header(login, "Content-Security-Policy-Report-Only"));

      /////////////////////////////////////////////////////////////////
      // a page without inline scripts allows none; the 404 page is  //
      // a document too                                              //
      /////////////////////////////////////////////////////////////////
      assertThat(header(get(base + "/app/person/1"), "Content-Security-Policy")).contains("script-src 'self'; ").doesNotContain("sha256");
      HttpResponse<String> notFound = get(base + "/app/person/1/unknown/deeper");
      assertEquals(404, notFound.statusCode());
      assertThat(header(notFound, "Content-Security-Policy")).contains("frame-ancestors 'none'");
      assertEquals("DENY", header(notFound, "X-Frame-Options"));

      HttpResponse<String> asset = get(base + "/_next/static/chunks/main.js");
      assertNull(header(asset, "Content-Security-Policy"));
      assertEquals("nosniff", header(asset, "X-Content-Type-Options"));
      assertEquals("strict-origin-when-cross-origin", header(asset, "Referrer-Policy"));

      HttpResponse<String> payload = get(base + "/app/person/__next._tree.txt");
      assertNull(header(payload, "Content-Security-Policy"));
      assertEquals("DENY", header(payload, "X-Frame-Options"));
   }



   /*******************************************************************************
    ** The configured identity providers may be called from the browser, and a
    ** QuickSight widget may be framed; nothing else is added.
    *******************************************************************************/
   @Test
   void testInstanceOrigins() throws Exception
   {
      QInstance qInstance = new QInstance();
      qInstance.withInstanceDefaultAuthentication(new OAuth2AuthenticationMetaData().withBaseUrl("https://IdP.example.com/realms/qqq/").withName("oauth2"));
      qInstance.registerAuthenticationProvider(AuthScope.api("reports"), new Auth0AuthenticationMetaData().withBaseUrl("https://tenant.auth0.example:8443").withName("auth0"));
      qInstance.registerAuthenticationProvider(AuthScope.api("other"), new Auth0AuthenticationMetaData().withBaseUrl("not a url").withName("broken"));

      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("test-next-dashboard");
      provider.setQInstance(qInstance);
      assertEquals(Set.of("'self'", "https://idp.example.com", "https://tenant.auth0.example:8443"), provider.getSecurityHeaders().getSources("connect-src"));
      assertEquals(Set.of("'self'"), provider.getSecurityHeaders().getSources("frame-src"));

      qInstance.addWidget(new QWidgetMetaData().withName("sales").withType(WidgetType.QUICK_SIGHT_CHART.getType()));
      qInstance.addWidget(new QWidgetMetaData().withName("extension").withType(WidgetType.CUSTOM_COMPONENT.getType())
         .withDefaultValue("componentName", "Extension").withDefaultValue("componentSourceUrl", "https://cdn.example.com/extensions/v1.js"));
      qInstance.addWidget(new QWidgetMetaData().withName("local").withType(WidgetType.CUSTOM_COMPONENT.getType())
         .withDefaultValue("componentName", "Local").withDefaultValue("componentSourceUrl", "/extensions/local.js"));
      qInstance.addWidget(new QWidgetMetaData().withName("unconfigured").withType(WidgetType.CUSTOM_COMPONENT.getType()));
      qInstance.addWidget(new QWidgetMetaData().withName("html").withType(WidgetType.HTML.getType()).withDefaultValue("componentSourceUrl", "https://ignored.example.com/x.js"));
      provider.setQInstance(qInstance);
      assertEquals(Set.of("'self'", NextDashboardRouteProvider.QUICKSIGHT_FRAME_SOURCE), provider.getSecurityHeaders().getSources("frame-src"));
      assertEquals(Set.of("'self'", "https://cdn.example.com"), provider.getSecurityHeaders().getSources("script-src"));

      String base = start(provider);
      assertThat(header(get(base + "/login/"), "Content-Security-Policy"))
         .contains("script-src 'self' https://cdn.example.com 'sha256-")
         .contains("connect-src 'self' https://idp.example.com https://tenant.auth0.example:8443;")
         .contains("frame-src 'self' https://*.quicksight.aws.amazon.com;");

      QInstance mock = new QInstance();
      mock.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));
      provider.setQInstance(mock);
      assertEquals(Set.of("'self'"), provider.getSecurityHeaders().getSources("connect-src"));
   }



   /*******************************************************************************
    ** The application override hook sees the instance additions, may change any
    ** directive or header, and is re-applied after a hot swap.
    *******************************************************************************/
   @Test
   void testCustomizer() throws Exception
   {
      QInstance qInstance = new QInstance();
      qInstance.withInstanceDefaultAuthentication(new OAuth2AuthenticationMetaData().withBaseUrl("https://idp.example.com").withName("oauth2"));

      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("test-next-dashboard").withSecurityHeadersCustomizer(headers ->
         headers.withSources("img-src", "https://cdn.example.com")
            .withoutSources("img-src", "https:")
            .withDirective("frame-ancestors", "'self'")
            .withHeader("X-Frame-Options", "SAMEORIGIN")
            .withHeader("Permissions-Policy", null)
            .withHeader("Cross-Origin-Opener-Policy", "same-origin"));
      provider.setQInstance(qInstance);

      String               base  = start(provider);
      HttpResponse<String> login = get(base + "/login/");
      assertThat(header(login, "Content-Security-Policy"))
         .contains("img-src 'self' data: blob: https://cdn.example.com;")
         .contains("connect-src 'self' https://idp.example.com;")
         .endsWith("frame-ancestors 'self'");
      assertEquals("SAMEORIGIN", header(login, "X-Frame-Options"));
      assertNull(header(login, "Permissions-Policy"));
      assertEquals("same-origin", header(login, "Cross-Origin-Opener-Policy"));

      qInstance.withInstanceDefaultAuthentication(new OAuth2AuthenticationMetaData().withBaseUrl("https://idp2.example.com").withName("oauth2"));
      provider.setQInstance(qInstance);
      assertThat(header(get(base + "/login/"), "Content-Security-Policy"))
         .contains("connect-src 'self' https://idp2.example.com;")
         .contains("https://cdn.example.com")
         .endsWith("frame-ancestors 'self'");
   }



   /*******************************************************************************
    ** Report-only and disabled policies; the other headers stay.
    *******************************************************************************/
   @Test
   void testReportOnlyAndDisabled() throws Exception
   {
      NextDashboardRouteProvider provider = new NextDashboardRouteProvider("test-next-dashboard").withSecurityHeadersCustomizer(headers -> headers.withReportOnly(true));
      String                     base     = start(provider);
      HttpResponse<String>       login    = get(base + "/login/");
      assertNull(header(login, "Content-Security-Policy"));
      assertThat(header(login, "Content-Security-Policy-Report-Only")).startsWith("default-src 'self'; script-src 'self' 'sha256-");

      provider.withSecurityHeadersCustomizer(headers -> headers.withContentSecurityPolicyEnabled(false));
      login = get(base + "/login/");
      assertNull(header(login, "Content-Security-Policy"));
      assertNull(header(login, "Content-Security-Policy-Report-Only"));
      assertEquals("DENY", header(login, "X-Frame-Options"));
   }



   /*******************************************************************************
    ** Values that would break or inject into the headers are refused.
    *******************************************************************************/
   @Test
   void testInvalidCustomizationsRejected()
   {
      NextDashboardSecurityHeaders headers = new NextDashboardSecurityHeaders();
      assertThrows(IllegalArgumentException.class, () -> headers.withSources("img-src", "https://a.example; script-src *"));
      assertThrows(IllegalArgumentException.class, () -> headers.withSources("img-src", "https://a.example,https://b.example"));
      assertThrows(IllegalArgumentException.class, () -> headers.withSources("img-src", (String) null));
      assertThrows(IllegalArgumentException.class, () -> headers.withDirective("Script-Src", "'self'"));
      assertThrows(IllegalArgumentException.class, () -> headers.withoutDirective("img src"));
      assertThrows(IllegalArgumentException.class, () -> headers.withHeader("X-Test", "a\r\nSet-Cookie: x=1"));
      assertThrows(IllegalArgumentException.class, () -> headers.withHeader("Bad Header", "x"));
      assertThrows(IllegalArgumentException.class, () -> headers.withHeader("content-security-policy", "default-src *"));
      assertThrows(IllegalArgumentException.class, () -> headers.withHeader(null, "x"));

      headers.withoutDirective("frame-src").withDirective("upgrade-insecure-requests").withoutSources("no-such-directive", "'self'");
      assertThat(headers.buildContentSecurityPolicy(List.of())).doesNotContain("frame-src").endsWith("; upgrade-insecure-requests");
      assertEquals("DENY", headers.getHeaders().get("X-Frame-Options"));
      assertThat(headers.getSources("no-such-directive")).isEmpty();
   }



   /*******************************************************************************
    ** Hashes cover exactly the inline scripts, as the browser hashes them.
    *******************************************************************************/
   @Test
   void testInlineScriptHashes() throws Exception
   {
      String html = "<script src=\"/a.js\"></script><SCRIPT type=\"text/javascript\">a()\r\nb()</SCRIPT >"
         + "<script id=\"x\" async>c()</script><script>c()</script><script>\n</script><noscript>d()</noscript>";
      assertEquals(List.of(sha256("a()\nb()"), sha256("c()"), sha256("\n")), NextDashboardRouteProvider.inlineScriptHashes(html));
      assertEquals(List.of(), NextDashboardRouteProvider.inlineScriptHashes("<html>no scripts</html>"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOriginOf()
   {
      assertEquals("https://idp.example.com", NextDashboardSecurityHeaders.originOf(" https://IDP.example.com/realms/x?y=1 "));
      assertEquals("http://127.0.0.1:18831", NextDashboardSecurityHeaders.originOf("http://127.0.0.1:18831/"));
      assertNull(NextDashboardSecurityHeaders.originOf(null));
      assertNull(NextDashboardSecurityHeaders.originOf(" "));
      assertNull(NextDashboardSecurityHeaders.originOf("/relative"));
      assertNull(NextDashboardSecurityHeaders.originOf("javascript:alert(1)"));
      assertNull(NextDashboardSecurityHeaders.originOf("https://bad host"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String start(NextDashboardRouteProvider provider)
   {
      service = Javalin.create(provider::acceptJavalinConfig).start(0);
      return ("http://localhost:" + service.port());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static HttpResponse<String> get(String url) throws Exception
   {
      return (HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String header(HttpResponse<String> response, String name)
   {
      return (response.headers().firstValue(name).orElse(null));
   }



   /*******************************************************************************
    ** The CSP hash source of a script, computed independently of the provider.
    *******************************************************************************/
   private static String sha256(String script) throws Exception
   {
      return ("'sha256-" + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(script.getBytes(StandardCharsets.UTF_8))) + "'");
   }
}
