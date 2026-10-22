/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.ai.output;


import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Builds the in-app (same-origin) links the AI tool kernel returns, in the
 ** route grammar of the QQQ Next UI - which addresses every table, process,
 ** report and app by name directly under /app (not under the path of the app
 ** that contains it):
 **
 **    table           /app/{table}
 **    filtered table  /app/{table}?filter={base64 of the filter JSON}
 **    record          /app/{table}/{id}
 **    process         /app/{process}
 **    report / app    /app/{report} , /app/{app}
 **
 ** (the same grammar as the Next UI's recordSearchResultPath and
 ** ProcessSummaryLines links, and its deserializeFilter accepts the base64
 ** filter parameter.)  Every path segment is URL-encoded.
 *******************************************************************************/
public class AiLinkBuilder
{
   public static final String APP_PREFIX = "/app/";

   private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("/app/[A-Za-z0-9_\\-.~%]+(/[A-Za-z0-9_\\-.~%]+)*/?(\\?[A-Za-z0-9_\\-.~%=&+]*)?(#[A-Za-z0-9_\\-.~%=/&+]*)?");



   /*******************************************************************************
    ** The link to a table's list screen (or to a process, report or app).
    *******************************************************************************/
   public static String objectHref(String name)
   {
      return (APP_PREFIX + encode(name));
   }



   /*******************************************************************************
    ** The link to a record's view screen.
    *******************************************************************************/
   public static String recordHref(String tableName, Serializable id)
   {
      return (objectHref(tableName) + "/" + encode(String.valueOf(id)));
   }



   /*******************************************************************************
    ** The link to a table's list screen, with a filter applied.
    *******************************************************************************/
   public static String filteredTableHref(String tableName, QQueryFilter filter)
   {
      if(filter == null)
      {
         return (objectHref(tableName));
      }

      Map<String, Object> filterForLink = new LinkedHashMap<>();
      filterForLink.put("criteria", filter.getCriteria());
      filterForLink.put("orderBys", filter.getOrderBys());
      filterForLink.put("subFilters", filter.getSubFilters());
      filterForLink.put("booleanOperator", filter.getBooleanOperator());

      String json   = JsonUtils.toJson(filterForLink);
      String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
      return (objectHref(tableName) + "?filter=" + URLEncoder.encode(base64, StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** Whether a path is a safe, same-origin in-app path: it starts with /app/,
    ** and contains only URL-safe (or percent-encoded) characters - so no scheme,
    ** no protocol-relative "//" host, no backslashes, no whitespace, no "..".
    *******************************************************************************/
   public static boolean isSafeAppPath(String path)
   {
      if(path == null || !path.startsWith(APP_PREFIX) || path.contains("//") || path.contains("..") || path.length() > 2000)
      {
         return (false);
      }
      return (SAFE_PATH_PATTERN.matcher(path).matches());
   }



   /*******************************************************************************
    ** URL-encode a path segment (with spaces as %20, not +).
    *******************************************************************************/
   public static String encode(String segment)
   {
      return (URLEncoder.encode(segment == null ? "" : segment, StandardCharsets.UTF_8).replace("+", "%20"));
   }
}
