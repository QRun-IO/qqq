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
      String frontend = System.getProperty("qqq.javalin.frontend");
      return start(launcher, directory, arguments, frontend == null ? List.of() : List.of("-Dqqq.javalin.frontend=" + frontend));
   }



   /*******************************************************************************
    ** Run a public launcher with additional JVM options, such as a dashboard selection.
    *******************************************************************************/
   static PackagedSampleServer start(Class<?> launcher, Path directory, List<String> arguments, List<String> jvmOptions) throws Exception
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
      command.addAll(jvmOptions);
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
