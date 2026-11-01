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

package com.kingsrook.qqq.backend.module.filesystem.base.actions;


import java.nio.file.Path;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.actions.AbstractFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.AbstractS3Action;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.sftp.actions.AbstractSFTPAction;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPTableBackendDetails;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Remote path joins and literal base stripping must not depend on host paths.
 *******************************************************************************/
class FilesystemPathSemanticsTest
{
   /*******************************************************************************
    ** A native root already has its separator and must not gain another one.
    *******************************************************************************/
   @Test
   void testLocalRootPath()
   {
      Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath().getRoot();
      FilesystemBackendMetaData backend = new FilesystemBackendMetaData().withBasePath(root.toString());
      QTableMetaData table = new QTableMetaData().withBackendDetails(new FilesystemTableBackendDetails());
      AbstractFilesystemAction action = new AbstractFilesystemAction();
      assertEquals(root.toString(), action.getFullBasePath(table, backend));
      assertEquals("record.csv", action.stripBackendAndTableBasePathsFromFileName(root.resolve("record.csv").toString(), backend, table));
   }



   /*******************************************************************************
    ** Object keys retain literal metacharacters and POSIX separators.
    *******************************************************************************/
   @Test
   void testS3BasePath()
   {
      S3BackendMetaData backend = new S3BackendMetaData().withBasePath("root+(v2)");
      QTableMetaData table = new QTableMetaData().withBackendDetails(new S3TableBackendDetails().withBasePath("files[1]"));
      AbstractS3Action action = new AbstractS3Action();
      assertEquals("root+(v2)/files[1]/", action.getFullBasePath(table, backend));
      assertEquals("literal\\name.csv", action.stripBackendAndTableBasePathsFromFileName("root+(v2)/files[1]/literal\\name.csv", backend, table));
   }



   /*******************************************************************************
    ** SFTP's shared base implementation also strips paths as literal strings.
    *******************************************************************************/
   @Test
   void testSftpBasePath()
   {
      SFTPBackendMetaData backend = new SFTPBackendMetaData().withBasePath("root+(v2)");
      QTableMetaData table = new QTableMetaData().withBackendDetails(new SFTPTableBackendDetails().withBasePath("files[1]"));
      AbstractSFTPAction action = new AbstractSFTPAction();
      assertEquals("root+(v2)/files[1]/", action.getFullBasePath(table, backend));
      assertEquals("literal\\name.csv", action.stripBackendAndTableBasePathsFromFileName("root+(v2)/files[1]/literal\\name.csv", backend, table));
   }
}
