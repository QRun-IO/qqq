/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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
 ** Unix path syntax for remote namespaces, independent of the host OS. This
 ** process-scoped filesystem models names only; no remote file data is stored.
 *******************************************************************************/
public final class RemotePathUtils
{
   private static final FileSystem FILE_SYSTEM = Jimfs.newFileSystem(Configuration.unix());



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private RemotePathUtils()
   {
   }



   /*******************************************************************************
    ** Parse a remote path without applying host filename restrictions.
    *******************************************************************************/
   public static Path getPath(String path)
   {
      return (FILE_SYSTEM.getPath(path));
   }



   /*******************************************************************************
    ** Compile the existing glob or regex syntax with case-sensitive Unix rules.
    *******************************************************************************/
   public static PathMatcher getPathMatcher(String pattern)
   {
      return (FILE_SYSTEM.getPathMatcher(pattern));
   }
}
