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

package com.kingsrook.qqq.backend.module.filesystem.local.actions;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for FilesystemStorageAction 
 *******************************************************************************/
class FilesystemStorageActionTest extends FilesystemActionTest
{
   @TempDir
   Path directory;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws Exception
   {
      String       data         = "Hellooo, Storage.";
      StorageInput storageInput = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("test.txt");

      try(OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
      {
         outputStream.write(data.getBytes(StandardCharsets.UTF_8));
      }

      try(InputStream inputStream = new StorageAction().getInputStream(storageInput))
      {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         inputStream.transferTo(byteArrayOutputStream);
         assertEquals(data, byteArrayOutputStream.toString(StandardCharsets.UTF_8));
      }

   }



   /*******************************************************************************
    ** Reads, writes and URLs share the same table-directory containment boundary.
    *******************************************************************************/
   @Test
   void testContainmentAndNestedStorage() throws Exception
   {
      ((FilesystemBackendMetaData) QContext.getQInstance().getBackend(TestUtils.defineLocalFilesystemBackend().getName())).setBasePath(directory.toString());
      Path base = Files.createDirectories(directory.resolve("blobs"));
      Path outside = Files.writeString(directory.resolve("private.txt"), "owned-private-marker");
      Files.createSymbolicLink(base.resolve("escape.txt"), outside);
      Files.createSymbolicLink(base.resolve("outside"), directory);
      StorageAction action = new StorageAction();
      for(String reference : Arrays.asList(null, "", ".", "..", "../private.txt", outside.toString(), "escape.txt", "outside/new.txt"))
      {
         StorageInput input = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference(reference);
         assertThrows(QBadRequestException.class, () -> action.getInputStream(input), "read: " + reference);
         assertThrows(QBadRequestException.class, () -> action.createOutputStream(input), "write: " + reference);
         assertThrows(QBadRequestException.class, () -> action.getDownloadURL(input), "URL: " + reference);
         assertThrows(QBadRequestException.class, () -> action.deleteObject(input), "delete: " + reference);
      }
      assertEquals("owned-private-marker", Files.readString(outside));
      StorageInput nested = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("reports/nested.txt");
      try(OutputStream output = action.createOutputStream(nested))
      {
         output.write("nested report".getBytes(StandardCharsets.UTF_8));
      }
      try(InputStream input = action.getInputStream(nested))
      {
         assertEquals("nested report", new String(input.readAllBytes(), StandardCharsets.UTF_8));
      }
      assertEquals("nested report", Files.readString(base.resolve("reports/nested.txt")));
   }


   /*******************************************************************************
    ** Broken links fail closed, while internal links and new directories work.
    *******************************************************************************/
   @Test
   void testLinkCompatibility() throws Exception
   {
      ((FilesystemBackendMetaData) QContext.getQInstance().getBackend(TestUtils.defineLocalFilesystemBackend().getName())).setBasePath(directory.toString());
      Path base = Files.createDirectories(directory.resolve("blobs"));
      Files.createSymbolicLink(base.resolve("dangling"), directory.resolve("missing"));
      StorageAction action = new StorageAction();
      StorageInput dangling = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("dangling/new.txt");
      assertThrows(QException.class, () -> action.createOutputStream(dangling));
      assertThrows(QException.class, () -> action.getInputStream(dangling));
      assertThrows(QException.class, () -> action.getDownloadURL(dangling));
      assertThrows(QException.class, () -> action.deleteObject(dangling));

      Files.createSymbolicLink(base.resolve("internal"), Files.createDirectories(base.resolve("actual")));
      StorageInput internal = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("internal/nested/new.txt");
      try(OutputStream output = action.createOutputStream(internal))
      {
         output.write("internal".getBytes(StandardCharsets.UTF_8));
      }
      assertEquals("internal", Files.readString(base.resolve("actual/nested/new.txt")));
      action.deleteObject(internal);
      assertEquals(false, Files.exists(base.resolve("actual/nested/new.txt")));
   }



   /*******************************************************************************
    ** Record writes use the same real-path boundary as the storage API.
    *******************************************************************************/
   @Test
   void testRecordWriteContainment() throws Exception
   {
      var backend = (FilesystemBackendMetaData) QContext.getQInstance().getBackend(TestUtils.defineLocalFilesystemBackend().getName());
      backend.setBasePath(directory.toString());
      var table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS);
      Path base = Files.createDirectories(directory.resolve("blobs"));
      Files.createSymbolicLink(base.resolve("outside"), directory);
      AbstractFilesystemAction action = new AbstractFilesystemAction();
      byte[] contents = "record".getBytes(StandardCharsets.UTF_8);
      assertThrows(IOException.class, () -> action.writeFile(backend, table, null, base.resolve("outside/new.txt").toString(), contents));
      action.writeFile(backend, table, null, base.resolve("nested/new.txt").toString(), contents);
      assertEquals("record", Files.readString(base.resolve("nested/new.txt")));
   }



   /*******************************************************************************
    ** Dot components must have the same meaning at validation and at file use.
    *******************************************************************************/
   @Test
   void testRecordLinkDotComponents() throws Exception
   {
      var backend = (FilesystemBackendMetaData) QContext.getQInstance().getBackend(TestUtils.defineLocalFilesystemBackend().getName());
      backend.setBasePath(directory.toString());
      var table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS);
      Path base = Files.createDirectories(directory.resolve("blobs"));
      Files.createSymbolicLink(base.resolve("link"), Files.createDirectories(base.resolve("a/b")));
      Path outside = Files.writeString(directory.resolve("private.txt"), "private");
      try
      {
         new AbstractFilesystemAction().writeFile(backend, table, null, base.resolve("link/../../private.txt").toString(), "changed".getBytes(StandardCharsets.UTF_8));
      }
      catch(IOException e)
      {
         // A native path that escapes the table must be rejected.
      }
      assertEquals("private", Files.readString(outside));
   }

}
