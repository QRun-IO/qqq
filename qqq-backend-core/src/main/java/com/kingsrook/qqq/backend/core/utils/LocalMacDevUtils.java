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

package com.kingsrook.qqq.backend.core.utils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.kingsrook.qqq.backend.core.logging.QLogger;


/*******************************************************************************
 ** Useful things to do on a mac, when doing development - that we can expect
 ** may not exist in a prod or even CI environment.  So, they'll only happen if
 **  flags are set to do them, and if we're on a mac (e.g., paths exist)
 *******************************************************************************/
public class LocalMacDevUtils
{
   private static final QLogger LOG = QLogger.getLogger(LocalMacDevUtils.class);

   public static boolean mayOpenFiles = false;

   private static final String OPEN_PROGRAM_PATH = "/usr/bin/open";



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void openFile(String path) throws IOException
   {
      if(mayOpenFiles && Files.exists(Path.of(OPEN_PROGRAM_PATH)))
      {
         Runtime.getRuntime().exec(new String[] { OPEN_PROGRAM_PATH, path });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void openFile(String path, String appPath) throws IOException
   {
      if(mayOpenFiles && Files.exists(Path.of(OPEN_PROGRAM_PATH)))
      {
         if(Files.exists(Path.of(appPath)))
         {
            Runtime.getRuntime().exec(new String[] { OPEN_PROGRAM_PATH, "-a", appPath, path });
         }
         else
         {
            LOG.warn("App at path [" + appPath + " was not found - file [" + path + "] will not be opened.");
         }
      }
   }

}
