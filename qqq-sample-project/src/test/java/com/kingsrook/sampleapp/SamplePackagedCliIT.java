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
