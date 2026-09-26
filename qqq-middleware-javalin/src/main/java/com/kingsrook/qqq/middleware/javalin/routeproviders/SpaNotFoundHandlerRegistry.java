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


import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import io.javalin.config.JavalinConfig;
import io.javalin.config.Key;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Path-scoped SPA 404 handlers owned by one Javalin configuration.
 ** A single error handler delegates by longest matching prefix, with root last.
 ** Javalin app data keeps unrelated servers and their handler lists isolated.
 *******************************************************************************/
public class SpaNotFoundHandlerRegistry
{
   private static final QLogger LOG = QLogger.getLogger(SpaNotFoundHandlerRegistry.class);

   private static final Key<SpaNotFoundHandlerRegistry> KEY = new Key<>(SpaNotFoundHandlerRegistry.class.getName());

   ///////////////////////////////////////////////////////////////////////////
   // CopyOnWriteArrayList provides thread-safe iteration in handleNotFound //
   // without requiring synchronization on the read path.                   //
   ///////////////////////////////////////////////////////////////////////////
   private final List<SpaNotFoundHandler> handlers = new CopyOnWriteArrayList<>();

   /*******************************************************************************
    ** Instances are owned by Javalin application data.
    *******************************************************************************/
   private SpaNotFoundHandlerRegistry()
   {
   }



   /*******************************************************************************
    ** Get this configuration's registry and install its error handler once.
    ** Called during Javalin configuration, before the server accepts requests.
    *******************************************************************************/
   public static SpaNotFoundHandlerRegistry getInstance(JavalinConfig config)
   {
      SpaNotFoundHandlerRegistry candidate = new SpaNotFoundHandlerRegistry();
      config.unsafe.appDataManager.registerIfAbsent(KEY, candidate);
      SpaNotFoundHandlerRegistry registry = config.unsafe.appDataManager.get(KEY);
      if(registry == candidate)
      {
         config.routes.error(HttpStatus.NOT_FOUND, registry::handleNotFound);
         LOG.info("Registered application SPA 404 handler");
      }
      return registry;
   }



   /*******************************************************************************
    ** Register a path-scoped 404 handler
    **
    ** @param spaPath The base path for this SPA (e.g., "/admin", "/")
    ** @param handler The handler function to call when a 404 occurs under this path
    *******************************************************************************/
   public synchronized void registerSpaHandler(String spaPath, Consumer<Context> handler)
   {
      /////////////////////////////////////////////////////////////////////
      // Normalize path (ensure it starts with / and doesn't end with /) //
      /////////////////////////////////////////////////////////////////////
      String normalizedPath = SpaPathUtils.normalizePath(spaPath);

      handlers.add(new SpaNotFoundHandler(normalizedPath, handler));

      ////////////////////////////////////////////////////////////////////////////
      // Sort by path length (longest first) so more specific paths match first //
      ////////////////////////////////////////////////////////////////////////////
      handlers.sort(Comparator.comparingInt((SpaNotFoundHandler h) -> h.path.length()).reversed());

      LOG.info("Registered SPA 404 handler",
         logPair("spaPath", normalizedPath),
         logPair("totalHandlers", handlers.size()),
         logPair("allPaths", handlers.stream().map(h -> h.path).toList()));
   }



   /*******************************************************************************
    ** Clear all registered handlers (useful for testing)
    **
    ** The configuration keeps its registered error handler. Other servers'
    ** registries are unaffected.
    *******************************************************************************/
   public synchronized void clear()
   {
      handlers.clear();
      LOG.info("Cleared all SPA 404 handlers");
   }



   /*******************************************************************************
    ** Global 404 handler - delegates to the appropriate SPA provider.
    **
    ** Iterates through registered handlers (sorted by path length, longest first)
    ** to find the most specific matching SPA. When found, delegates to that SPA's
    ** handler to decide whether to serve index.html or let it 404.
    **
    ** @param ctx The Javalin request context for the 404 error
    *******************************************************************************/
   private void handleNotFound(Context ctx)
   {
      /////////////////////////////////////////////////////////////////////
      // A matched endpoint owns its response, including an explicit 404. //
      // SPA fallback applies only when no endpoint handled the request.  //
      /////////////////////////////////////////////////////////////////////
      if(ctx.endpoints().lastHttpEndpoint() != null)
      {
         return;
      }

      String requestPath = ctx.path();

      LOG.debug("Global 404 handler invoked", logPair("path", requestPath), logPair("registeredHandlers", handlers.size()));

      /////////////////////////////////////////////////////////////////////////
      // Find the handler with the longest matching path prefix              //
      // This ensures more specific paths (e.g., /admin/api) take precedence //
      // over less specific ones (e.g., /admin or /)                         //
      /////////////////////////////////////////////////////////////////////////
      for(SpaNotFoundHandler handler : handlers)
      {
         LOG.debug("Checking handler", logPair("requestPath", requestPath), logPair("handlerPath", handler.path), logPair("matches", pathMatches(requestPath, handler.path)));
         if(pathMatches(requestPath, handler.path))
         {
            LOG.debug("Delegating 404 to SPA handler",
               logPair("requestPath", requestPath),
               logPair("spaPath", handler.path));

            handler.handleNotFound.accept(ctx);
            return;
         }
      }

      ///////////////////////////////////////////////
      // No handler matched - let it 404 naturally //
      ///////////////////////////////////////////////
      LOG.debug("No SPA handler matched path, letting 404", logPair("path", requestPath), logPair("registeredHandlers", handlers.size()));
   }



   /*******************************************************************************
    ** Check if a request path should be handled by a given SPA.
    **
    ** Used by the global 404 handler to route requests to the appropriate SPA
    ** provider. The registry maintains multiple SPAs (e.g., /admin, /customer, /)
    ** and this method determines which SPA should handle a 404 for a given path.
    **
    ** SPECIAL CASE - ROOT PATH:
    ** The root path ("/") is a catch-all that matches EVERYTHING. This allows
    ** a root SPA to serve as a fallback for any path not handled by other SPAs.
    **
    ** BOUNDARY CHECKING:
    ** Uses proper prefix matching to prevent false matches like "/administrator"
    ** matching SPA path "/admin". This is critical for correct routing.
    **
    ** Examples:
    **   spaPath="/", requestPath="/anything"          → TRUE (root catches all)
    **   spaPath="/admin", requestPath="/admin"        → TRUE (exact match)
    **   spaPath="/admin", requestPath="/admin/users"  → TRUE (sub-path)
    **   spaPath="/admin", requestPath="/administrator"→ FALSE (different path)
    **
    ** @param requestPath The incoming 404 request path
    ** @param spaPath The registered SPA's base path
    ** @return true if this SPA should handle the 404 for this request
    *******************************************************************************/
   private boolean pathMatches(String requestPath, String spaPath)
   {
      ////////////////////////////////////////////////////
      // Root path is special - it matches EVERYTHING   //
      // This allows "/" SPA to be a catch-all fallback //
      ////////////////////////////////////////////////////
      if("/".equals(spaPath))
      {
         return true;
      }

      //////////////////////////////////////////////////////
      // For non-root SPAs, check if request is under SPA //
      // Uses boundary checking to prevent false matches  //
      //////////////////////////////////////////////////////
      return SpaPathUtils.isPathUnderPrefix(requestPath, spaPath);
   }



   /*******************************************************************************
    ** Internal class to hold a path and its handler
    *******************************************************************************/
   private static class SpaNotFoundHandler
   {
      private final String            path;
      private final Consumer<Context> handleNotFound;



      /***************************************************************************
       ** Constructor for SpaNotFoundHandler.
       **
       ** @param path The base path for this SPA (e.g., "/admin", "/")
       ** @param handleNotFound The handler function to invoke on 404
       ***************************************************************************/
      public SpaNotFoundHandler(String path, Consumer<Context> handleNotFound)
      {
         this.path = path;
         this.handleNotFound = handleNotFound;
      }
   }
}
