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


import com.kingsrook.qqq.backend.core.logging.QLogger;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
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

   private final List<SpaNotFoundHandler> handlers = new ArrayList<>();
   private boolean                        globalHandlerRegistered;


   /*******************************************************************************
    ** Private constructor for singleton
    *******************************************************************************/
   private SpaNotFoundHandlerRegistry()
   {
   }


   /*******************************************************************************
    ** Get the singleton instance
    *******************************************************************************/
   public static SpaNotFoundHandlerRegistry getInstance()
   {
      return INSTANCE;
   }


   /*******************************************************************************
    ** Register the global 404 handler with Javalin (call once per Javalin instance)
    *******************************************************************************/
   public synchronized void registerGlobalHandler(Javalin service)
   {
      if(!globalHandlerRegistered)
      {
         service.error(HttpStatus.NOT_FOUND, this::handleNotFound);
         globalHandlerRegistered = true;
         LOG.info("Registered global SPA 404 handler");
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
      // Normalize path (ensure it starts with / and doesn't end with /)
      String normalizedPath = normalizePath(spaPath);

      handlers.add(new SpaNotFoundHandler(normalizedPath, handler));

      // Sort by path length (longest first) so more specific paths match first
      handlers.sort(Comparator.comparingInt((SpaNotFoundHandler h) -> h.path.length()).reversed());

      LOG.info("Registered SPA 404 handler",
         logPair("spaPath", normalizedPath),
         logPair("totalHandlers", handlers.size()),
         logPair("allPaths", handlers.stream().map(h -> h.path).toList()));
   }


   /*******************************************************************************
    ** Clear all registered handlers (useful for testing)
    *******************************************************************************/
   public synchronized void clear()
   {
      handlers.clear();
      globalHandlerRegistered = false;
      LOG.info("Cleared all SPA 404 handlers");
   }


   /*******************************************************************************
    ** Global 404 handler - delegates to the appropriate SPA provider
    *******************************************************************************/
   private void handleNotFound(Context ctx)
   {
      String requestPath = ctx.path();

      LOG.debug("Global 404 handler invoked", logPair("path", requestPath), logPair("registeredHandlers", handlers.size()));

      /////////////////////////////////////////////////////////////////////////
      // Find the handler with the longest matching path prefix             //
      // This ensures more specific paths (e.g., /admin/api) take precedence //
      // over less specific ones (e.g., /admin or /)                        //
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

      ///////////////////////////////////////////////////////////////////////
      // No handler matched - let it 404 naturally                        //
      ///////////////////////////////////////////////////////////////////////
      LOG.debug("No SPA handler matched path, letting 404",
         logPair("path", requestPath), logPair("registeredHandlers", handlers.size()));
   }


   /*******************************************************************************
    ** Check if a request path matches a SPA path
    **
    ** Rules:
    ** - Root path ("/") matches everything
    ** - Other paths must start with the SPA path
    ** - /admin matches /admin/anything but not /administrator
    *******************************************************************************/
   private boolean pathMatches(String requestPath, String spaPath)
   {
      if("/".equals(spaPath))
      {
         // Root path matches everything
         return true;
      }

      // Path must start with spaPath and be followed by / or be exact match
      if(requestPath.equals(spaPath))
      {
         return true;
      }

      if(requestPath.startsWith(spaPath + "/"))
      {
         return true;
      }

      return false;
   }


   /*******************************************************************************
    ** Normalize a path (ensure it starts with / and doesn't end with /)
    *******************************************************************************/
   private String normalizePath(String path)
   {
      if(path == null || path.isEmpty())
      {
         return "/";
      }

      // Ensure starts with /
      if(!path.startsWith("/"))
      {
         path = "/" + path;
      }

      // Remove trailing / (except for root)
      if(path.length() > 1 && path.endsWith("/"))
      {
         path = path.substring(0, path.length() - 1);
      }

      return path;
   }


   /*******************************************************************************
    ** Internal class to hold a path and its handler
    *******************************************************************************/
   private static class SpaNotFoundHandler
   {
      private final String            path;
      private final Consumer<Context> handleNotFound;


      public SpaNotFoundHandler(String path, Consumer<Context> handleNotFound)
      {
         this.path = path;
         this.handleNotFound = handleNotFound;
      }
   }
}

