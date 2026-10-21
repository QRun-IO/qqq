/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2025-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.devtools;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;


/*******************************************************************************
 ** todo picocli this project and class
 *******************************************************************************/
public class CreateNewQBit
{
   private String name;
   private String root;

   private static ExecutorService executorService = null;

   private static String SED = "/opt/homebrew/bin/gsed"; // needs to be a version that supports -i (in-place edit)
   private static String GIT = "/usr/bin/git";
   private static String CP  = "/bin/cp";
   private static String MV  = "/bin/mv";



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args)
   {
      args = new String[] { "/Users/dkelkhoff/git/kingsrook/qbits", "webhooks" };

      if(args.length < 2)
      {
         System.out.println("Usage: java CreateNewQBit root-dir qbit-name");
         System.exit(1);
      }

      CreateNewQBit instance = new CreateNewQBit();
      instance.root = args[0];
      instance.name = args[1];
      System.exit(instance.run());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public int run()
   {
      try
      {
         String wordsName = makeWordsName(name);
         wordsName = stripQBitPrefix(wordsName);
         String dashName    = makeDashName(wordsName);
         String packageName = makePackageName(wordsName);
         String className   = makeClassName(wordsName);
         String varName     = makeVarName(wordsName);

         if(!new File(root).exists())
         {
            System.err.println("ERROR:  Root directory [" + root + "] does not exist.");
            return (1);
         }

         File template = new File(root + File.separator + "TEMPLATE");
         if(!template.exists())
         {
            System.err.println("ERROR:  Template directory [TEMPLATE] does not exist under [" + root + "].");
            return (1);
         }

         File dir = new File(root + File.separator + "qbit-" + dashName);
         if(dir.exists())
         {
            System.err.println("ERROR:  Directory [" + dashName + "] already exists under [" + root + "].");
            return (1);
         }

         System.out.println("Creating qbit-" + dashName + ":");
         System.out.printf("%13s %s\n", "packgaename:", packageName);
         System.out.printf("%13s %s\n", "ClassName:", className);
         System.out.printf("%13s %s\n", "varName:", varName);
         System.out.println();

         System.out.println("Copying template...");
         ProcessResult cpResult = run(new ProcessBuilder(CP, "-rv", template.getAbsolutePath(), dir.getAbsolutePath()));
         System.out.print(cpResult.stdout());
         System.out.println();

         System.out.println("Renaming files...");
         renameFiles(dir, packageName, className);
         System.out.println();

         System.out.println("Updating file contents...");
         replacePlaceholders(dir, dashName, packageName, className, varName);
         System.out.println();

         System.out.println("Init'ing git repo...");
         run(new ProcessBuilder(GIT, "init").directory(dir));
         System.out.println();

         // git remote add origin https://github.com/Kingsrook/${name}.git ?
         // echo https://app.circleci.com/projects/project-dashboard/github/Kingsrook after initial push
      }
      catch(Exception e)
      {
         e.printStackTrace();
         return 1;
      }
      return 0;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   void renameFiles(File dir, String packageName, String className) throws Exception
   {
      String srcPath     = dir.getAbsolutePath() + "/src/main/java/com/kingsrook/qbits";
      String packagePath = packageName.replace('.', '/');
      System.out.print(run(new ProcessBuilder(MV, "-v", srcPath + "/todo/TodoQBitConfig.java", srcPath + "/todo/" + className + "QBitConfig.java")).stdout());
      System.out.print(run(new ProcessBuilder(MV, "-v", srcPath + "/todo/TodoQBitProducer.java", srcPath + "/todo/" + className + "QBitProducer.java")).stdout());
      System.out.print(run(new ProcessBuilder(MV, "-v", srcPath + "/todo", srcPath + "/" + packagePath)).stdout());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static void replacePlaceholders(File dir, String dashName, String packageName, String className, String varName) throws Exception
   {
      for(File file : dir.listFiles())
      {
         if(file.isDirectory())
         {
            replacePlaceholders(file, dashName, packageName, className, varName);
            continue;
         }

         System.out.println("Replacing placeholders in: " + file.getAbsolutePath());
         replaceOne("dashName", dashName, file);
         replaceOne("packageName", packageName, file);
         replaceOne("className", className, file);
         replaceOne("varName", varName, file);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static void replaceOne(String from, String to, File file) throws Exception
   {
      run(new ProcessBuilder(SED, "s/\\${" + from + "}/" + to + "/g", "-i", file.getAbsolutePath()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public record ProcessResult(Integer exitCode, String stdout, String stderr)
   {

      /***************************************************************************
       *
       ***************************************************************************/
      public boolean hasStdout()
      {
         return stdout != null && !stdout.isEmpty();
      }



      /***************************************************************************
       *
       ***************************************************************************/
      public boolean hasStderr()
      {
         return stderr != null && !stderr.isEmpty();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static ProcessResult run(ProcessBuilder builder) throws Exception
   {
      StringBuilder stdout = new StringBuilder();
      StringBuilder stderr = new StringBuilder();

      Process   process      = builder.start();
      Future<?> stdoutFuture = getExecutorService().submit(new StreamGobbler(process.getInputStream(), stdout::append));
      Future<?> stderrFuture = getExecutorService().submit(new StreamGobbler(process.getErrorStream(), stderr::append));

      int exitCode = process.waitFor();
      stdoutFuture.get();
      stderrFuture.get();

      return (new ProcessResult(exitCode, stdout.toString(), stderr.toString()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static class StreamGobbler implements Runnable
   {
      private InputStream      inputStream;
      private Consumer<String> consumer;



      /***************************************************************************
       **
       ***************************************************************************/
      public StreamGobbler(InputStream inputStream, Consumer<String> consumer)
      {
         this.inputStream = inputStream;
         this.consumer = consumer;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run()
      {
         new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(s -> consumer.accept(s + System.lineSeparator()));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static ExecutorService getExecutorService()
   {
      if(executorService == null)
      {
         executorService = Executors.newCachedThreadPool();
      }
      return (executorService);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String makeWordsName(String s)
   {
      if(s.contains("-"))
      {
         return (s.toLowerCase().replace('-', ' '));
      }

      if(s.matches(".*[A-Z].*"))
      {
         return s.replaceAll("([A-Z])", "$1'").toLowerCase().trim();
      }

      return s;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static String stripQBitPrefix(String s)
   {
      return (s.replaceFirst("^qbit(s) ", ""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static String makeDashName(String s)
   {
      return (s.replace(' ', '-'));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static String makePackageName(String s)
   {
      return (s.replace(" ", ""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static String makeClassName(String s)
   {
      StringBuilder rs    = new StringBuilder();
      String[]      words = s.split(" ");
      for(String word : words)
      {
         rs.append(word.substring(0, 1).toUpperCase());
         if(word.length() > 1)
         {
            rs.append(word.substring(1));
         }
      }
      return rs.toString();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static String makeVarName(String s)
   {
      String className = makeClassName(s);
      return className.substring(0, 1).toLowerCase() + (className.length() == 1 ? "" : className.substring(1));
   }
}
