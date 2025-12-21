/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


/*******************************************************************************
 ** Utility methods for SPA path handling.
 **
 ** This class provides common path operations used by IsolatedSpaRouteProvider
 ** and SpaNotFoundHandlerRegistry for consistent path matching behavior.
 *******************************************************************************/
public final class SpaPathUtils
{
   /*******************************************************************************
    ** Private constructor to prevent instantiation
    *******************************************************************************/
   private SpaPathUtils()
   {
   }



   /*******************************************************************************
    ** Check if a request path is under a given path prefix.
    **
    ** This is the correct way to check path prefixes with boundary checking
    ** to prevent false matches like "/administrator" matching "/admin".
    **
    ** Handles query parameters and hash fragments by stripping them before
    ** comparison, so "/admin?tab=users" correctly matches prefix "/admin".
    **
    ** SPECIAL CASE - ROOT PATH:
    ** The root path "/" matches everything when used as a prefix. This allows
    ** root-level exclusions or catch-all behaviors.
    **
    ** RULES:
    ** 1. Exact match: "/admin" matches "/admin"
    ** 2. Sub-path: "/admin/users" matches prefix "/admin"
    ** 3. Boundary check: "/administrator" does NOT match prefix "/admin"
    ** 4. Query params ignored: "/admin?tab=users" matches prefix "/admin"
    ** 5. Root matches all: anything matches prefix "/"
    **
    ** This method ensures consistent path matching behavior across the routing
    ** system. Any code that needs to check "is path X under path Y" should use
    ** this logic to avoid prefix collision bugs.
    **
    ** Examples:
    **   isPathUnderPrefix("/admin", "/admin")              -> TRUE (exact)
    **   isPathUnderPrefix("/admin/users", "/admin")        -> TRUE (sub-path)
    **   isPathUnderPrefix("/admin?tab=users", "/admin")    -> TRUE (query param ignored)
    **   isPathUnderPrefix("/administrator", "/admin")      -> FALSE (different path)
    **   isPathUnderPrefix("/api-docs", "/api")             -> FALSE (different path)
    **   isPathUnderPrefix("/anything", "/")                -> TRUE (root matches all)
    **
    ** @param requestPath The request path to check (may include query params/hash)
    ** @param pathPrefix The path prefix to match against
    ** @return true if requestPath is equal to or under pathPrefix
    *******************************************************************************/
   public static boolean isPathUnderPrefix(String requestPath, String pathPrefix)
   {
      ////////////////////////////////////////////////
      // Special case: root path matches everything //
      ////////////////////////////////////////////////
      if("/".equals(pathPrefix))
      {
         return true;
      }

      ////////////////////////////////////////////////
      // Normalize request path: strip query params //
      // and hash fragments for comparison          //
      ////////////////////////////////////////////////
      String normalizedPath = requestPath;
      int    queryIndex     = normalizedPath.indexOf('?');
      if(queryIndex != -1)
      {
         normalizedPath = normalizedPath.substring(0, queryIndex);
      }
      int hashIndex = normalizedPath.indexOf('#');
      if(hashIndex != -1)
      {
         normalizedPath = normalizedPath.substring(0, hashIndex);
      }

      //////////////////////
      // Exact match case //
      //////////////////////
      if(normalizedPath.equals(pathPrefix))
      {
         return true;
      }

      ////////////////////////////////////////////////////////////
      // Sub-path match with boundary check                     //
      // Ensures "/administrator" doesn't match prefix "/admin" //
      ////////////////////////////////////////////////////////////
      return normalizedPath.startsWith(pathPrefix + "/");
   }



   /*******************************************************************************
    ** Normalize a path to ensure it starts with / and doesn't end with /
    ** (except for root path which stays as "/")
    **
    ** Examples:
    **   normalizePath(null)        -> "/"
    **   normalizePath("")          -> "/"
    **   normalizePath("admin")     -> "/admin"
    **   normalizePath("/admin")    -> "/admin"
    **   normalizePath("/admin/")   -> "/admin"
    **   normalizePath("/")         -> "/"
    **
    ** @param path The path to normalize
    ** @return The normalized path
    *******************************************************************************/
   public static String normalizePath(String path)
   {
      if(path == null || path.isEmpty())
      {
         return "/";
      }

      ////////////////////////
      // Ensure starts with //
      ////////////////////////
      if(!path.startsWith("/"))
      {
         path = "/" + path;
      }

      /////////////////////////////////////////
      // Remove trailing / (except for root) //
      /////////////////////////////////////////
      if(path.length() > 1 && path.endsWith("/"))
      {
         path = path.substring(0, path.length() - 1);
      }

      return path;
   }
}
