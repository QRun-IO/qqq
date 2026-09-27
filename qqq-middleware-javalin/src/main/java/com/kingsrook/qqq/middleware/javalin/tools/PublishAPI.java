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

package com.kingsrook.qqq.middleware.javalin.tools;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import com.fasterxml.jackson.databind.MapperFeature;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.openapi.model.OpenAPI;
import picocli.CommandLine;


/*******************************************************************************
 **
 *******************************************************************************/
@CommandLine.Command(name = "publishAPI")
public class PublishAPI implements Callable<Integer>
{
   @CommandLine.Option(names = { "-r", "--repoRoot" })
   private String repoRoot;

   @CommandLine.Option(names = { "--sortFileContentsForHuman" }, description = "By default, properties in the yaml are sorted alphabetically, to help with stability (for diffing).  This option preserves the 'natural' order of the file, so may look a little bette for human consumption")
   private boolean sortFileContentsForHuman = false;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args) throws Exception
   {
      // for a run from the IDE, to override args... args = new String[] { "-r", "/Users/dkelkhoff/git/kingsrook/qqq/" };
      int exitCode = new CommandLine(new PublishAPI()).execute(args);
      System.exit(exitCode);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer call() throws Exception
   {
      AbstractMiddlewareVersion middlewareVersion = new MiddlewareVersionV1();

      if(!StringUtils.hasContent(repoRoot))
      {
         throw (new QException("Repo root argument was not given."));
      }

      if(!new File(repoRoot).exists())
      {
         throw (new QException("Repo root directory [" + repoRoot + "] was not found."));
      }

      String allApisPath = repoRoot + "/" + APIUtils.PUBLISHED_API_LOCATION + "/";
      if(!new File(allApisPath).exists())
      {
         throw (new QException("APIs directory [" + allApisPath + "] was not found."));
      }

      File versionDirectory = new File(allApisPath + middlewareVersion.getVersion() + "/");
      if(!versionDirectory.exists())
      {
         if(!versionDirectory.mkdirs())
         {
            // CTEngCliUtils.printError("Error: An error occurred creating directory [" + apiDirectory.getPath() + "].");
            System.err.println("Error: An error occurred creating directory [" + versionDirectory.getPath() + "].");
            return (1);
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // build the openapi spec - then run it through a "grouping" function, which will make several //
      // subsets of it (e.g., grouped by table mostly) - then we'll write out each such file         //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      OpenAPI openAPI = middlewareVersion.generateOpenAPIModel("qqq");
      String yaml = YamlUtils.toYamlCustomized(openAPI, mapperBuilder ->
      {
         if(sortFileContentsForHuman)
         {
            ////////////////////////////////////////////////
            // this is actually the default mapper config //
            ////////////////////////////////////////////////
         }
         else
         {
            mapperBuilder.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
         }
      });

      writeFile(yaml, versionDirectory, "openapi.yaml");

      //////////////////////////////////////////////////////////////////////////////////////
      // if we want to split up by some paths, components, we could use a version of this //
      //////////////////////////////////////////////////////////////////////////////////////
      // Map<String, Map<String, Object>> groupedPaths = APIUtils.splitUpYamlForPublishing(yaml);
      // for(String name : groupedPaths.keySet())
      // {
      //    writeFile(groupedPaths.get(name), versionDirectory, name + ".yaml");
      // }
      // CTEngCliUtils.printSuccess("Files for [" + apiInstanceMetaData.getName() + "] [" + apiVersion + "] have been successfully published.");
      // System.out.println("Files for [" + middlewareVersion.getClass().getSimpleName() + "] have been successfully published.");

      return (0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeFile(String yaml, File directory, String fileBaseName) throws IOException
   {
      String yamlFileName = directory.getAbsolutePath() + "/" + fileBaseName;
      Path   yamlPath     = Paths.get(yamlFileName);
      Files.write(yamlPath, yaml.getBytes());
      System.out.println("Wrote [" + yamlPath + "]");
   }

}
