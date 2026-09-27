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


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import com.fasterxml.jackson.databind.MapperFeature;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.openapi.model.OpenAPI;
import picocli.CommandLine;


/*******************************************************************************
 **
 *******************************************************************************/
@CommandLine.Command(name = "validateApiVersions")
public class ValidateAPIVersions implements Callable<Integer>
{
   @CommandLine.Option(names = { "-r", "--repoRoot" })
   String repoRoot;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args) throws Exception
   {
      // for a run from the IDE, to override args... args = new String[] { "-r", "/Users/dkelkhoff/git/kingsrook/qqq/" };
      int exitCode = new CommandLine(new ValidateAPIVersions()).execute(args);
      System.out.println("Exiting with code: " + exitCode);
      System.exit(exitCode);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer call() throws Exception
   {
      String       fileFormat   = "yaml";
      boolean      hadErrors    = false;
      List<String> errorHeaders = new ArrayList<>();

      List<AbstractMiddlewareVersion> specList = List.of(new MiddlewareVersionV1());

      for(AbstractMiddlewareVersion middlewareVersion : specList)
      {
         String  version              = middlewareVersion.getVersion();
         boolean hadErrorsThisVersion = false;

         //////////
         // todo //
         //////////
         // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // // if this api version is in the list of "future" versions, consider it a "beta" and don't do any validation //
         // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // for(APIVersion futureAPIVersion : apiInstanceMetaData.getFutureVersions())
         // {
         //    if(apiVersion.equals(futureAPIVersion))
         //    {
         //       continue versionLoop;
         //    }
         // }

         try
         {
            ////////////////////////////////////////////////////////////
            // list current files - so we can tell if all get diff'ed //
            ////////////////////////////////////////////////////////////
            Set<String> existingFileNames = new HashSet<>();
            String      versionPath       = repoRoot + "/" + APIUtils.PUBLISHED_API_LOCATION + "/" + version + "/";
            versionPath = versionPath.replaceAll("/+", "/");
            for(File file : APIUtils.listPublishedAPIFiles(versionPath))
            {
               existingFileNames.add(file.getPath().replaceFirst(versionPath, ""));
            }

            ///////////////////////////////////////////////////////////
            // generate a new spec based on current code in codebase //
            ///////////////////////////////////////////////////////////
            OpenAPI openAPI = middlewareVersion.generateOpenAPIModel("qqq");
            String yaml = YamlUtils.toYamlCustomized(openAPI, mapperBuilder ->
            {
               mapperBuilder.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            });

            /////////////////////////////////////////////////////////////////////
            // get the published API file - then diff it to what we just wrote //
            /////////////////////////////////////////////////////////////////////
            String publishedAPI = APIUtils.getPublishedAPIFile(versionPath, "openapi", fileFormat);

            String newFileName       = "/tmp/" + version + "-new." + fileFormat;
            String publishedFileName = "/tmp/" + version + "-published." + fileFormat;
            Files.write(Path.of(newFileName), yaml.getBytes());
            Files.write(Path.of(publishedFileName), publishedAPI.getBytes());

            Runtime  rt       = Runtime.getRuntime();
            String[] commands = { "diff", "-w", publishedFileName, newFileName };
            Process  proc     = rt.exec(commands);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            StringBuilder diffOutput = new StringBuilder();
            String        s;
            while((s = stdInput.readLine()) != null)
            {
               diffOutput.append(s).append("\n");
            }

            if(!"".contentEquals(diffOutput))
            {
               String message = "Error: Differences were found in openapi.yaml file between the published docs and the newly generated file for API Version [" + version + "].";
               errorHeaders.add(message);
               System.out.println(message);
               System.out.println(diffOutput);
               hadErrors = true;
               hadErrorsThisVersion = true;
            }


            //////////////////////////////////////////////////////////////////////////////////////
            // if we want to split up by some paths, components, we could use a version of this //
            //////////////////////////////////////////////////////////////////////////////////////
            /*
            Map<String, Map<String, Object>> groupedPaths = APIUtils.splitUpYamlForPublishing(yaml);

            ///////////////////////////////////////////////////////////////////////////////////////
            // for each of the groupings (e.g., files), compare to what was previously published //
            ///////////////////////////////////////////////////////////////////////////////////////
            for(Map.Entry<String, Map<String, Object>> entry : groupedPaths.entrySet())
            {
               try
               {
                  String name          = entry.getKey();
                  String newFileToDiff = YamlUtils.toYaml(entry.getValue());

                  /////////////////////////////////////////////////////////////////////
                  // get the published API file - then diff it to what we just wrote //
                  /////////////////////////////////////////////////////////////////////
                  String publishedAPI = APIUtils.getPublishedAPIFile(versionPath, name, fileFormat);
                  existingFileNames.remove(name + "." + fileFormat);

                  String newFileName       = "/tmp/" + version + "-new." + fileFormat;
                  String publishedFileName = "/tmp/" + version + "-published." + fileFormat;
                  Files.write(Path.of(newFileName), newFileToDiff.getBytes());
                  Files.write(Path.of(publishedFileName), publishedAPI.getBytes());

                  Runtime  rt       = Runtime.getRuntime();
                  String[] commands = { "diff", "-w", publishedFileName, newFileName };
                  Process  proc     = rt.exec(commands);

                  BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                  StringBuilder diffOutput = new StringBuilder();
                  String        s;
                  while((s = stdInput.readLine()) != null)
                  {
                     diffOutput.append(s).append("\n");
                  }

                  if(!"".contentEquals(diffOutput))
                  {
                     String message = "Error: Differences were found in file [" + name + "] between the published docs and the newly generated " + fileFormat + " file for API Version [" + version + "].";
                     errorHeaders.add(message);
                     System.out.println(message);
                     System.out.println(diffOutput);
                     hadErrors = true;
                     hadErrorsThisVersion = true;
                  }
               }
               catch(Exception e)
               {
                  errorHeaders.add(e.getMessage());
                  System.out.println(e.getMessage());
                  hadErrors = true;
                  hadErrorsThisVersion = true;
               }
            }

            /////////////////////////////////////////////////////////////////////////////////////
            // if any existing files weren't evaluated in the loop above, then that's an error //
            // e.g., someone removed a thing that was previously in the API                    //
            /////////////////////////////////////////////////////////////////////////////////////
            if(!existingFileNames.isEmpty())
            {
               hadErrors = true;
               hadErrorsThisVersion = true;
               for(String existingFileName : existingFileNames)
               {
                  String message = "Error: Previously published file [" + existingFileName + "] was not found in the current OpenAPI object for API Version [" + version + "].";
                  errorHeaders.add(message);
                  System.out.println(message);
               }
            }
            */
         }
         catch(Exception e)
         {
            errorHeaders.add(e.getMessage());
            System.out.println(e.getMessage());
            hadErrors = true;
            hadErrorsThisVersion = true;
         }

         if(!hadErrorsThisVersion)
         {
            System.out.println("Success: No differences were found between the published docs and the newly generated " + fileFormat + " file for API Version [" + version + "].");
         }
      }

      if(!errorHeaders.isEmpty())
      {
         System.out.println("\nError summary:");
         errorHeaders.forEach(e -> System.out.println(" - " + e));
      }

      return (hadErrors ? 1 : 0);
   }

}
