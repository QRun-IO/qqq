/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** The packaged CLI must bootstrap itself without a source tree or caller context.
 *******************************************************************************/
class SamplePackagedCliIT
{
   @TempDir
   Path directory;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCountAndInvalidCommandFromEmptyWorkingDirectory() throws Exception
   {
      Path artifact = Path.of(SampleCli.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      assertTrue(Files.isRegularFile(artifact), "Acceptance must load the packaged sample");
      Path bundle = artifact.resolveSibling(artifact.getFileName().toString().replace(".jar", "-jar-with-dependencies.jar"));
      assertTrue(Files.isRegularFile(bundle), "The assembled sample must be present");
      for(String table : List.of("person", "no-such-command"))
      {
         Path log = directory.resolve(table + ".log");
         Process process = new ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-Dqqq.sample.mockAuthentication=true", "-Dlog4j.configurationFile=qqq-picocli-log4j2.xml",
            "-cp", bundle.toString(), SampleCli.class.getName(), table, "count")
            .directory(directory.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
         try
         {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "The packaged CLI must terminate");
            String output = Files.readString(log);
            assertEquals(table.equals("person") ? 0 : 2, process.exitValue(), output);
            if(table.equals("person"))
            {
               assertTrue(output.contains("\"count\" : 5"), output);
            }
         }
         finally
         {
            process.destroyForcibly();
         }
      }
   }
}
