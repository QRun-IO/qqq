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

package com.kingsrook.qqq.backend.core.instances;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.logging.CollectedLogMessage;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


/*******************************************************************************
 ** Unit test for ${file.PATH} references in QMetaDataVariableInterpreter
 *******************************************************************************/
class QMetaDataVariableInterpreterFileReferenceTest extends BaseTest
{
   private static final String SECRET = "s3cr3t-value-never-logged";

   @TempDir
   Path tempDir;

   private Path secretsRoot;



   /*******************************************************************************
    ** A secrets root (allowed) and a sibling directory (not allowed).
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws IOException
   {
      secretsRoot = Files.createDirectories(tempDir.resolve("secrets"));
      Files.createDirectories(tempDir.resolve("outside"));
      System.setProperty(QMetaDataVariableInterpreter.FILE_REFERENCE_ROOTS_PROPERTY, secretsRoot.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      System.clearProperty(QMetaDataVariableInterpreter.FILE_REFERENCE_ROOTS_PROPERTY);
      QLogger.deactivateCollectingLoggerForClass(QMetaDataVariableInterpreter.class);
   }



   /*******************************************************************************
    ** A reference like ${file./abs/path} for a path.
    *******************************************************************************/
   private static String ref(Path path)
   {
      return ("${file." + path.toAbsolutePath() + "}");
   }



   /*******************************************************************************
    ** Write a file (creating its directory).
    *******************************************************************************/
   private static Path write(Path path, String contents) throws IOException
   {
      Files.createDirectories(path.getParent());
      return (Files.writeString(path, contents, StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReadsTrimmedContentsUnderAllowedRoot() throws IOException
   {
      Path file = write(secretsRoot.resolve("storage/access-key"), "  " + SECRET + "\n\n");
      assertEquals(SECRET, new QMetaDataVariableInterpreter().interpret(ref(file)));
   }



   /*******************************************************************************
    ** The file is read on each interpretation, so a rotated secret is seen.
    *******************************************************************************/
   @Test
   void testRotatedFileIsReRead() throws IOException
   {
      Path                         file        = write(secretsRoot.resolve("token"), "first");
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      assertEquals("first", interpreter.interpret(ref(file)));
      write(file, "second");
      assertEquals("second", interpreter.interpret(ref(file)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingFileUsesDefaultAndCoalescing() throws IOException
   {
      Path                         missing     = secretsRoot.resolve("missing");
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      assertNull(interpreter.interpret(ref(missing)));
      assertEquals("fallback", interpreter.interpretForObject(ref(missing), "fallback"));

      interpreter.setEnvironmentOverrides(Map.of("FROM_ENV", "env-value"));
      assertEquals("env-value", interpreter.interpret(ref(missing) + "??${env.FROM_ENV}"));

      Path present = write(secretsRoot.resolve("present"), "file-value");
      assertEquals("file-value", interpreter.interpret("${env.NOT_SET_ANYWHERE_1165}??" + ref(present)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRejectedPaths() throws IOException
   {
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();

      ////////////////////////////////////////
      // outside the root, even through .. //
      ////////////////////////////////////////
      Path outside = write(tempDir.resolve("outside/key"), SECRET);
      assertNull(interpreter.interpret(ref(outside)));
      assertNull(interpreter.interpret("${file." + secretsRoot.toAbsolutePath() + "/../outside/key}"));

      ////////////////////
      // not absolute  //
      ////////////////////
      assertNull(interpreter.interpret("${file.secrets/key}"));

      /////////////////
      // a directory //
      /////////////////
      Files.createDirectories(secretsRoot.resolve("dir"));
      assertNull(interpreter.interpret(ref(secretsRoot.resolve("dir"))));

      ///////////////////////////
      // larger than the limit //
      ///////////////////////////
      Path big = write(secretsRoot.resolve("big"), "x".repeat(QMetaDataVariableInterpreter.FILE_REFERENCE_MAX_BYTES + 1));
      assertNull(interpreter.interpret(ref(big)));

      ////////////////////////
      // exactly the limit //
      ////////////////////////
      Path atLimit = write(secretsRoot.resolve("at-limit"), "y".repeat(QMetaDataVariableInterpreter.FILE_REFERENCE_MAX_BYTES));
      assertEquals(QMetaDataVariableInterpreter.FILE_REFERENCE_MAX_BYTES, interpreter.interpret(ref(atLimit)).length());
   }



   /*******************************************************************************
    ** Kubernetes secret volumes link key -> ..data/key -> ..timestamp/key inside
    ** the mount; a link out of the root is refused.
    *******************************************************************************/
   @Test
   void testSymbolicLinks() throws IOException
   {
      Path target = write(secretsRoot.resolve("storage/..2026_10_15/key"), SECRET);
      Path outsideTarget = write(tempDir.resolve("outside/other"), "outside");
      Path link;
      Path escape;
      try
      {
         Files.createSymbolicLink(secretsRoot.resolve("storage/..data"), secretsRoot.resolve("storage/..2026_10_15"));
         link = Files.createSymbolicLink(secretsRoot.resolve("storage/key"), secretsRoot.resolve("storage/..data/key"));
         escape = Files.createSymbolicLink(secretsRoot.resolve("escape"), outsideTarget);
      }
      catch(UnsupportedOperationException | IOException | SecurityException e)
      {
         assumeTrue(false, "symbolic links are not available here");
         return;
      }

      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      assertEquals(SECRET, interpreter.interpret(ref(link)));
      assertEquals(SECRET, interpreter.interpret(ref(target)));
      assertNull(interpreter.interpret(ref(escape)));
   }



   /*******************************************************************************
    ** An interpreter with value maps (user-supplied values) does not read files
    ** unless explicitly enabled.
    *******************************************************************************/
   @Test
   void testValueMapsDisableFileReferences() throws IOException
   {
      Path file = write(secretsRoot.resolve("key"), SECRET);

      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      assertTrue(interpreter.isFileReferencesEnabled());
      interpreter.addValueMap("input", Map.of("x", "y"));
      assertFalse(interpreter.isFileReferencesEnabled());
      assertEquals(ref(file), interpreter.interpret(ref(file)));

      interpreter.setFileReferencesEnabled(true);
      assertEquals(SECRET, interpreter.interpret(ref(file)));

      assertEquals(ref(file), new QMetaDataVariableInterpreter().withFileReferencesEnabled(false).interpret(ref(file)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRoots()
   {
      System.clearProperty(QMetaDataVariableInterpreter.FILE_REFERENCE_ROOTS_PROPERTY);
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      interpreter.setEnvironmentOverrides(Map.of());
      assertEquals(QMetaDataVariableInterpreter.DEFAULT_FILE_REFERENCE_ROOTS, interpreter.getFileReferenceRoots());

      interpreter.setEnvironmentOverrides(Map.of(QMetaDataVariableInterpreter.FILE_REFERENCE_ROOTS_ENV, " /a , ,/b "));
      assertEquals(List.of("/a", "/b"), interpreter.getFileReferenceRoots());

      System.setProperty(QMetaDataVariableInterpreter.FILE_REFERENCE_ROOTS_PROPERTY, "/c");
      assertEquals(List.of("/c"), interpreter.getFileReferenceRoots());
   }



   /*******************************************************************************
    ** Log lines name the path, never the contents.
    *******************************************************************************/
   @Test
   void testContentsAreNeverLogged() throws IOException
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(QMetaDataVariableInterpreter.class);

      Path                         outside     = write(tempDir.resolve("outside/key"), SECRET);
      Path                         big         = write(secretsRoot.resolve("big"), SECRET + "x".repeat(QMetaDataVariableInterpreter.FILE_REFERENCE_MAX_BYTES));
      Path                         good        = write(secretsRoot.resolve("good"), SECRET);
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      interpreter.interpret(ref(outside));
      interpreter.interpret(ref(big));
      interpreter.interpret("${file.relative/key}");
      assertEquals(SECRET, interpreter.interpret(ref(good)));

      assertThat(collectingLogger.getCollectedMessages()).hasSizeGreaterThanOrEqualTo(3);
      for(CollectedLogMessage message : collectingLogger.getCollectedMessages())
      {
         assertThat(message.toString()).doesNotContain(SECRET).doesNotContain("xxxxxxxx");
      }
   }
}
