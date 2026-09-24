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

      OutputStream outputStream = new StorageAction().createOutputStream(storageInput);
      outputStream.write(data.getBytes(StandardCharsets.UTF_8));
      outputStream.close();

      InputStream           inputStream           = new StorageAction().getInputStream(storageInput);
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      inputStream.transferTo(byteArrayOutputStream);

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
      StorageAction action = new StorageAction();
      for(String reference : Arrays.asList(null, "", ".", "..", "../private.txt", outside.toString(), "escape.txt"))
      {
         StorageInput input = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference(reference);
         assertThrows(QBadRequestException.class, () -> action.getInputStream(input), "read: " + reference);
         assertThrows(QBadRequestException.class, () -> action.createOutputStream(input), "write: " + reference);
         assertThrows(QBadRequestException.class, () -> action.getDownloadURL(input), "URL: " + reference);
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
}
