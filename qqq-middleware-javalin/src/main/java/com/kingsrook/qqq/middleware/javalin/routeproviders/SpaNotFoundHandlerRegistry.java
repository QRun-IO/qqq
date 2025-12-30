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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Global registry for SPA 404 handlers.
 **
 ** PROBLEM:
 **   Javalin's error() handlers are GLOBAL - you can only register one 404 handler
 **   per Javalin instance. When multiple IsolatedSpaRouteProviders try to register
 **   404 handlers, they conflict, and only the last one wins.
 **
 ** SOLUTION:
 **   This registry maintains a list of path-scoped 404 handlers. Each
 **   IsolatedSpaRouteProvider registers its scope and handler function.
 **   The global 404 handler (registered once with Javalin) delegates to the
 **   appropriate provider based on path matching.
 **
 ** DESIGN:
 **   1. IsolatedSpaRouteProvider registers: registerHandler("/admin", handler)
 **   2. Global 404 occurs at /admin/Content/site
 **   3. Registry finds handler for "/admin" (longest prefix match)
 **   4. Calls the handler's callback
 **   5. Handler decides whether to serve SPA index or let it 404
 **
 ** PRIORITY:
 **   Handlers are matched by LONGEST PATH PREFIX first
 **   - More specific paths (e.g., /admin/api) take precedence over /admin
 **   - Root path ("/") is always last resort
 *******************************************************************************/
public class SpaNotFoundHandlerRegistry
{
   private static final QLogger LOG = QLogger.getLogger(SpaNotFoundHandlerRegistry.class);

   private static final SpaNotFoundHandlerRegistry INSTANCE = new SpaNotFoundHandlerRegistry();

   ///////////////////////////////////////////////////////////////////////////
   // CopyOnWriteArrayList provides thread-safe iteration in handleNotFound //
   // without requiring synchronization on the read path.                   //
   ///////////////////////////////////////////////////////////////////////////
   private final List<SpaNotFoundHandler> handlers = new CopyOnWriteArrayList<>();

   ///////////////////////////////////////////////////////////////////////////
   // Track which Javalin instance has the global handler registered.       //
   // Uses identity hash code to avoid holding a reference to the instance. //
   // This allows re-registration when a new Javalin instance is created    //
   // (common in tests where each test creates its own server).             //
   ///////////////////////////////////////////////////////////////////////////
   private int registeredJavalinInstanceId = 0;



   /*******************************************************************************
    ** Private constructor for singleton
    *******************************************************************************/
   private SpaNotFoundHandlerRegistry()
   {
   }



   /*******************************************************************************
    ** Get the singleton instance of the registry.
    **
    ** @return The singleton SpaNotFoundHandlerRegistry instance
    *******************************************************************************/
   public static SpaNotFoundHandlerRegistry getInstance()
   {
      return INSTANCE;
   }



   /*******************************************************************************
    ** Register the global 404 handler with Javalin (call once per Javalin instance).
    **
    ** This method is idempotent per Javalin instance - calling it multiple times
    ** with the same instance has no effect. However, if a NEW Javalin instance is
    ** passed, the handler is registered with that instance. This is important for
    ** test isolation where each test may create its own Javalin server.
    **
    ** @param service The Javalin instance to register the global 404 handler with
    *******************************************************************************/
   public synchronized void registerGlobalHandler(Javalin service)
   {
      int currentInstanceId = System.identityHashCode(service);
      if(registeredJavalinInstanceId != currentInstanceId)
      {
         service.error(HttpStatus.NOT_FOUND, this::handleNotFound);
         registeredJavalinInstanceId = currentInstanceId;
         LOG.info("Registered global SPA 404 handler", logPair("javalinInstanceId", currentInstanceId));
      }
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
    ** This resets the registry to its initial state, allowing a new Javalin
    ** instance to register handlers. Call this in test cleanup (@AfterEach)
    ** to ensure test isolation.
    *******************************************************************************/
   public synchronized void clear()
   {
      handlers.clear();
      registeredJavalinInstanceId = 0;
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
