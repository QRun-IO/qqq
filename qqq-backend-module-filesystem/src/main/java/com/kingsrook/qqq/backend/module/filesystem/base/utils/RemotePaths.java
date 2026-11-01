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

package com.kingsrook.qqq.backend.module.filesystem.base.utils;


import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;


/*******************************************************************************
 ** Host-independent POSIX path parsing and NIO glob matching for remote stores.
 ** This class owns one classloader-lifetime in-memory filesystem. It stores no
 ** files, opens no host resources and starts no watcher; callers must not close
 ** the shared filesystem obtained from a returned Path.
 *******************************************************************************/
public final class RemotePaths
{
   private static final FileSystem FILE_SYSTEM = Jimfs.newFileSystem(Configuration.unix());



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private RemotePaths()
   {
   }



   /*******************************************************************************
    ** Parse a remote path without applying host drive or separator rules.
    *******************************************************************************/
   public static Path get(String first, String... more)
   {
      return (FILE_SYSTEM.getPath(first, more));
   }



   /*******************************************************************************
    ** Preserve the complete NIO glob/regex contract with POSIX case sensitivity.
    *******************************************************************************/
   public static PathMatcher getPathMatcher(String syntaxAndPattern)
   {
      return (FILE_SYSTEM.getPathMatcher(syntaxAndPattern));
   }
}
