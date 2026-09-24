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


import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Owns a real packaged sample process, its empty working directory and cleanup.
 *******************************************************************************/
class PackagedSampleServer implements AutoCloseable
{
   private final Process process;
   private final Path log;



   /*******************************************************************************
    **
    *******************************************************************************/
   private PackagedSampleServer(Process process, Path log)
   {
      this.process = process;
      this.log = log;
   }



   /*******************************************************************************
    ** Run a public launcher with local mock auth and an OS-assigned ephemeral port.
    *******************************************************************************/
   static PackagedSampleServer start(Class<?> launcher, Path directory, List<String> arguments) throws Exception
   {
      Path artifact = Path.of(SampleJavalinServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      assertTrue(Files.isRegularFile(artifact), "Acceptance must load the packaged sample");
      Path bundle = artifact.resolveSibling(artifact.getFileName().toString().replace(".jar", "-jar-with-dependencies.jar"));
      assertTrue(Files.isRegularFile(bundle));
      Path work = Files.createTempDirectory(directory, "packaged-");
      Path log = work.resolve("server.log");
      List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString()));
      ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
         .filter(argument -> argument.startsWith("-javaagent:") && argument.contains("org.jacoco.agent"))
         .forEach(command::add);
      command.addAll(List.of(
         "-Dqqq.sample.mockAuthentication=true", "-Dqqq.sample.port=0",
         "-cp", bundle.toString(), launcher.getName()));
      command.addAll(arguments);
      Process process = new ProcessBuilder(command).directory(Files.createDirectory(work.resolve("cwd")).toFile())
         .redirectErrorStream(true).redirectOutput(log.toFile()).start();
      return new PackagedSampleServer(process, log);
   }



   /*******************************************************************************
    ** Read Javalin's actual bound port rather than reserving a potentially busy one.
    *******************************************************************************/
   URI awaitReady() throws Exception
   {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
      while(System.nanoTime() < deadline && process.isAlive())
      {
         Matcher listening = Pattern.compile("Listening on http://localhost:(\\d+)/").matcher(output());
         if(listening.find())
         {
            return URI.create("http://localhost:" + listening.group(1));
         }
         Thread.sleep(50);
      }
      return fail("Sample did not start: " + output());
   }



   /*******************************************************************************
    ** Return the real JVM exit code for a failed startup.
    *******************************************************************************/
   int awaitExit() throws Exception
   {
      assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Invalid configuration must terminate startup");
      return process.exitValue();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   String output() throws Exception
   {
      return Files.readString(log);
   }



   /*******************************************************************************
    ** Stop only this scenario's child, including after failed assertions.
    *******************************************************************************/
   @Override
   public void close() throws Exception
   {
      process.destroy();
      if(!process.waitFor(5, TimeUnit.SECONDS))
      {
         process.destroyForcibly();
         assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Sample process must terminate");
      }
   }
}
