/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** File grants belong to one application and one session.
 *******************************************************************************/
class ProcessFileDownloadTest
{
   @TempDir
   Path directory;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Exact registration preserves bytes and denies another session or instance.
    *******************************************************************************/
   @Test
   void testRegisteredFileIsBoundToApplicationAndSession() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      QSession session = new QSession();
      QContext.init(instance, session);
      Path file = Files.writeString(directory.resolve("owned.txt"), "owned report");
      assertThrows(QPermissionDeniedException.class, () -> ProcessFileDownload.open(file.toString()));
      String registered = ProcessFileDownload.register(file.toFile());
      try(InputStream input = ProcessFileDownload.open(registered))
      {
         assertEquals("owned report", new String(input.readAllBytes(), StandardCharsets.UTF_8));
      }
      QContext.init(instance, new QSession());
      assertThrows(QPermissionDeniedException.class, () -> ProcessFileDownload.open(registered));
      QContext.init(TestUtils.defineInstance(), session);
      assertThrows(QPermissionDeniedException.class, () -> ProcessFileDownload.open(registered));
      QContext.init(instance, session);
      assertThrows(QPermissionDeniedException.class, () -> ProcessFileDownload.open(registered + ".other"));
      Files.delete(file);
      assertThrows(QException.class, () -> ProcessFileDownload.open(registered));
   }



   /*******************************************************************************
    ** A grant requires a live execution context and an existing regular file.
    *******************************************************************************/
   @Test
   void testMissingSessionAndInvalidFileAreRejected() throws Exception
   {
      QContext.clear();
      Path file = Files.writeString(directory.resolve("owned.txt"), "owned report");
      assertThrows(QAuthenticationException.class, () -> ProcessFileDownload.register(file.toFile()));
      assertThrows(QAuthenticationException.class, () -> ProcessFileDownload.open(file.toString()));
      QContext.init(TestUtils.defineInstance(), new QSession());
      assertThrows(QException.class, () -> ProcessFileDownload.register(directory.toFile()));
      assertThrows(QException.class, () -> ProcessFileDownload.register(directory.resolve("missing.txt").toFile()));
   }



   /*******************************************************************************
    ** Bearer authentication reconstructs sessions while retaining its reference.
    *******************************************************************************/
   @Test
   void testReconstructedSessionAndStorageScope() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      QSession original = new QSession().withBackendVariants(Map.of("tenant", 1));
      original.setIdReference("owned-test-authentication-reference");
      QContext.init(instance, original);
      Path file = Files.writeString(directory.resolve("report.txt"), "report bytes");
      String path = ProcessFileDownload.register(file.toFile());
      StorageInput storage = new StorageInput("reports").withReference("owned/report.csv");
      ProcessFileDownload.registerStorage(storage);
      assertTrue(ProcessFileDownload.isStorageRegistered(storage));
      assertFalse(ProcessFileDownload.isStorageRegistered(new StorageInput("otherTable").withReference(storage.getReference())));
      assertFalse(ProcessFileDownload.isStorageRegistered(new StorageInput("reports").withReference(path)));
      assertThrows(QPermissionDeniedException.class, () -> ProcessFileDownload.open(storage.getReference()));

      QSession restored = new QSession().withBackendVariants(Map.of("tenant", 1));
      restored.setIdReference(original.getIdReference());
      QContext.init(instance, restored);
      try(InputStream input = ProcessFileDownload.open(path))
      {
         assertEquals("report bytes", new String(input.readAllBytes(), StandardCharsets.UTF_8));
      }
      assertTrue(ProcessFileDownload.isStorageRegistered(storage));
      restored.setBackendVariants(Map.of("tenant", 2));
      assertThrows(QPermissionDeniedException.class, () -> ProcessFileDownload.open(path));
      assertFalse(ProcessFileDownload.isStorageRegistered(storage));
      restored.setBackendVariants(Map.of("tenant", 1));
      restored.setIdReference("another-test-authentication-reference");
      assertThrows(QPermissionDeniedException.class, () -> ProcessFileDownload.open(path));
      assertFalse(ProcessFileDownload.isStorageRegistered(storage));
      QContext.init(TestUtils.defineInstance(), original);
      assertFalse(ProcessFileDownload.isStorageRegistered(storage));
   }
}
