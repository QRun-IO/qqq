/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Compile and run an external consumer using only the packaged source candidate.
 *******************************************************************************/
class SampleMigrationPackagedConsumerIT
{
   @TempDir
   Path directory;

   /*******************************************************************************
    ** The packaged candidate supplies migrated APIs outside the test classpath.
    *******************************************************************************/
   @Test
   void testPackagedCandidateCompilesAndRunsExternalConsumer() throws Exception
   {
      Path artifact = Path.of(SampleJavalinServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      Path bundle = artifact.resolveSibling(artifact.getFileName().toString().replace(".jar", "-jar-with-dependencies.jar"));
      assertTrue(Files.isRegularFile(bundle), "Package the sample before running the consumer");
      String candidateVersion = artifact.getFileName().toString()
         .replace("qqq-sample-project-", "").replace(".jar", "");
      try(ZipFile packaged = new ZipFile(bundle.toFile()))
      {
         for(String module : List.of("qqq-backend-core", "qqq-backend-module-rdbms",
            "qqq-backend-module-filesystem", "qqq-middleware-javalin", "qqq-middleware-picocli", "qqq-openapi"))
         {
            String entry = "META-INF/maven/com.kingsrook.qqq/" + module + "/pom.properties";
            assertTrue(packaged.getEntry(entry) != null, "Missing packaged candidate module " + module);
            Properties properties = new Properties();
            try(var input = packaged.getInputStream(packaged.getEntry(entry)))
            {
               properties.load(input);
            }
            assertEquals(candidateVersion, properties.getProperty("version"), module);
         }
      }
      Path source = directory.resolve("MigrationConsumer.java");
      Files.writeString(source, """
         import java.time.temporal.ChronoUnit;
         import java.util.Map;
         import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
         import com.kingsrook.qqq.backend.core.utils.JsonUtils;
         import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
         import io.javalin.Javalin;
         public class MigrationConsumer {
            public static void main(String[] args) throws Exception {
               if (!JsonUtils.toJson(Map.of("migrated", true)).contains("migrated")) {
                  throw new AssertionError("JSON replacement failed");
               }
               if (NowWithOffset.minus(7, ChronoUnit.DAYS) == null) {
                  throw new AssertionError("ChronoUnit replacement failed");
               }
               for (Class<?> type : new Class<?>[] { QJavalinImplementation.class, Javalin.class }) {
                  if (!type.getProtectionDomain().getCodeSource().getLocation().toURI()
                     .equals(new java.io.File(args[0]).toURI())) {
                     throw new AssertionError("Class escaped packaged candidate: " + type.getName());
                  }
               }
               System.out.println("MIGRATION_CONSUMER_PASS");
            }
         }
         """);
      Path java = Path.of(System.getProperty("java.home"), "bin", "java");
      Path javac = Path.of(System.getProperty("java.home"), "bin", "javac");
      assertEquals(0, command(List.of(javac.toString(), "--release", "21", "-cp", bundle.toString(),
         "-d", directory.toString(), source.toString())).exitCode());
      ProcessResult launched = command(List.of(java.toString(), "-cp", bundle + System.getProperty("path.separator") + directory,
         "MigrationConsumer", bundle.toString()));
      assertEquals(0, launched.exitCode(), launched.output());
      assertTrue(launched.output().contains("MIGRATION_CONSUMER_PASS"), launched.output());
      ProcessResult missingCandidate = command(List.of(javac.toString(), "--release", "21", "-cp",
         directory.resolve("missing-candidate.jar").toString(), "-d", directory.toString(), source.toString()));
      assertNotEquals(0, missingCandidate.exitCode(), "Consumer must need the packaged candidate");
      assertTrue(missingCandidate.output().contains("does not exist"), missingCandidate.output());
   }

   /*******************************************************************************
    ** Run an owned subprocess with a bounded wait and captured output.
    *******************************************************************************/
   private ProcessResult command(List<String> arguments) throws Exception
   {
      Process process = new ProcessBuilder(arguments).directory(directory.toFile()).redirectErrorStream(true).start();
      if(!process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS))
      {
         process.destroyForcibly();
         throw new AssertionError("Consumer process timed out");
      }
      return new ProcessResult(process.exitValue(), new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
   }

   /*******************************************************************************
    ** Preserve process status and output together for failure messages.
    *******************************************************************************/
   private record ProcessResult(int exitCode, String output) {}
}
