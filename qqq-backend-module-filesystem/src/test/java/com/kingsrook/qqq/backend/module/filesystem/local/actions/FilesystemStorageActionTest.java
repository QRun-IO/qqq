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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

      OutputStream outputStream = new StorageAction().createOutputStream(storageInput);
      outputStream.write(data.getBytes(StandardCharsets.UTF_8));
      outputStream.close();

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try(InputStream inputStream = new StorageAction().getInputStream(storageInput))
      {
         inputStream.transferTo(byteArrayOutputStream);
      }

      assertEquals(data, byteArrayOutputStream.toString(StandardCharsets.UTF_8));

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
      Path outsideDirectory = Files.createDirectories(directory.resolve("outside"));
      Files.createSymbolicLink(base.resolve("escape-directory"), outsideDirectory);
      StorageAction action = new StorageAction();
      for(String reference : Arrays.asList(null, "", ".", "..", "bad\0name", "../private.txt", "reports/../inside.txt", outside.toString(), "escape.txt", "escape-directory/new.txt", "escape-directory/../private.txt"))
      {
         StorageInput input = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference(reference);
         assertThrows(QBadRequestException.class, () -> action.getInputStream(input), "read: " + reference);
         assertThrows(QBadRequestException.class, () -> action.createOutputStream(input), "write: " + reference);
         assertThrows(QBadRequestException.class, () -> action.getDownloadURL(input), "URL: " + reference);
         assertThrows(QBadRequestException.class, () -> action.deleteObject(input), "delete: " + reference);
      }
      QBadRequestException invalidPath = assertThrows(QBadRequestException.class,
         () -> action.getInputStream(new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("bad\0name")));
      assertEquals("Storage reference must be a relative file path", invalidPath.getMessage());
      assertNull(invalidPath.getCause());
      assertEquals("owned-private-marker", Files.readString(outside));
      assertFalse(Files.exists(outsideDirectory.resolve("new.txt")));
      Files.createSymbolicLink(base.resolve("dangling"), directory.resolve("absent.txt"));
      StorageInput dangling = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("dangling");
      assertThrows(QException.class, () -> action.getInputStream(dangling));
      assertThrows(QException.class, () -> action.createOutputStream(dangling));
      assertThrows(QException.class, () -> action.getDownloadURL(dangling));
      assertThrows(QException.class, () -> action.deleteObject(dangling));
      assertFalse(Files.exists(directory.resolve("absent.txt")));
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
    ** An administrator-configured linked base is allowed; its real table is root.
    *******************************************************************************/
   @Test
   void testConfiguredSymlinkBase() throws Exception
   {
      Path actual = Files.createDirectories(directory.resolve("actual"));
      Path configured = Files.createSymbolicLink(directory.resolve("configured"), actual);
      Files.createDirectories(actual.resolve("child"));
      for(Path metadataPath : Arrays.asList(configured, configured.resolve("child/..")))
      {
         ((FilesystemBackendMetaData) QContext.getQInstance().getBackend(TestUtils.defineLocalFilesystemBackend().getName())).setBasePath(metadataPath.toString());
         StorageInput input = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("inside.txt");
         StorageAction action = new StorageAction();
         try(OutputStream output = action.createOutputStream(input))
         {
            output.write("within configured root".getBytes(StandardCharsets.UTF_8));
         }
         assertEquals("within configured root", Files.readString(actual.resolve("blobs/inside.txt")));
         try(InputStream stream = action.getInputStream(input))
         {
            assertEquals("within configured root", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
         }
      }
   }



   /*******************************************************************************
    ** Metadata link/.. follows native resolution, not lexical path normalization.
    *******************************************************************************/
   @Test
   void testConfiguredSymlinkParent() throws Exception
   {
      Path actualChild = Files.createDirectories(directory.resolve("other/child"));
      Path configuredRoot = Files.createDirectories(directory.resolve("configured-root"));
      Path link = Files.createSymbolicLink(configuredRoot.resolve("link"), actualChild);
      Path metadataPath = link.resolve("..");
      Path actualBase = metadataPath.toRealPath();
      FilesystemBackendMetaData backend = (FilesystemBackendMetaData) QContext.getQInstance().getBackend(TestUtils.defineLocalFilesystemBackend().getName());
      backend.setBasePath(metadataPath.toString());
      String fullBasePath = new AbstractFilesystemAction().getFullBasePath(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_BLOB_LOCAL_FS), backend);
      assertTrue(fullBasePath.contains(".."), "Metadata segments must survive until native realpath resolution");
      StorageInput input = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("native-parent.txt");
      try(OutputStream output = new StorageAction().createOutputStream(input))
      {
         output.write("native metadata parent".getBytes(StandardCharsets.UTF_8));
      }
      assertEquals("native metadata parent", Files.readString(actualBase.resolve("blobs/native-parent.txt")));
   }
}
