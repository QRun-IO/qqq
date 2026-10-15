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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.util.Map;


/*******************************************************************************
 ** Explicit Material icon to client-neutral token mapping. Unknown names stay null.
 *******************************************************************************/
public class SemanticIcons
{
   private static final Map<String, String> NAMES = Map.ofEntries(
      Map.entry("add", "add"),
      Map.entry("add_circle", "add-circle"),
      Map.entry("edit", "edit"),
      Map.entry("delete", "delete"),
      Map.entry("person", "person"),
      Map.entry("person_add", "person-add"),
      Map.entry("library_add", "library-add"),
      Map.entry("settings", "settings"),
      Map.entry("search", "search"),
      Map.entry("download", "download"),
      Map.entry("upload", "upload"),
      Map.entry("refresh", "refresh"),
      Map.entry("check", "check"),
      Map.entry("close", "close"),
      Map.entry("warning", "warning"),
      Map.entry("error", "error"),
      Map.entry("info", "info"),
      Map.entry("help", "help"),
      Map.entry("home", "home"),
      Map.entry("dashboard", "dashboard"),
      Map.entry("list", "list"),
      Map.entry("table", "table"),
      Map.entry("calendar", "calendar"),
      Map.entry("email", "email"),
      Map.entry("phone", "phone"),
      Map.entry("link", "link"),
      Map.entry("lock", "lock"),
      Map.entry("visibility", "visibility"),
      Map.entry("star", "star"),
      Map.entry("notifications", "notifications"),
      Map.entry("folder", "folder"),
      Map.entry("file", "file"),
      Map.entry("image", "image"),
      Map.entry("chart", "chart"),
      Map.entry("play", "play"),
      Map.entry("pause", "pause"),
      Map.entry("stop", "stop"),
      Map.entry("sync", "sync"),
      Map.entry("schedule", "schedule"),
      Map.entry("history", "history"),
      Map.entry("cloud", "cloud"),
      Map.entry("table_chart", "table"),
      Map.entry("calendar_today", "calendar"),
      Map.entry("insert_drive_file", "file"),
      Map.entry("bar_chart", "chart"),
      Map.entry("play_arrow", "play"),
      Map.entry("file_download", "download"),
      Map.entry("file_upload", "upload"),
      Map.entry("delete_forever", "delete"));



   /*******************************************************************************
    ** Client-neutral representation for non-web clients.
    *******************************************************************************/
   public static String semanticName(String materialName)
   {
      return (materialName == null ? null : NAMES.get(materialName));
   }
}
