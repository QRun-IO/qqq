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

package com.kingsrook.qqq.middleware.javalin.tools.codegenerators;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;


/*******************************************************************************
 **
 *******************************************************************************/
class SpecCodeGenerator
{

   /***************************************************************************
    **
    ***************************************************************************/
   public static void main(String[] args)
   {
      try
      {
         String qqqDir = "/Users/dkelkhoff/git/kingsrook/qqq/";

         /////////////////
         // normal case //
         /////////////////
         new SpecCodeGenerator().writeAllFiles(qqqDir, "v1", "ProcessMetaData");

         ///////////////////////////////////////////////////////////////////////////////
         // if the executor isn't named the same as the spec (e.g., reused executors) //
         ///////////////////////////////////////////////////////////////////////////////
         // new SpecCodeGenerator().writeAllFiles(qqqDir, "v1", "ProcessInsert", "ProcessInsertOrSetp");
      }
      catch(IOException e)
      {
         //noinspection CallToPrintStackTrace
         e.printStackTrace();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void writeOne(String fullPath, String content) throws IOException
   {
      File file      = new File(fullPath);
      File directory = file.getParentFile();

      if(!directory.exists())
      {
         throw (new RuntimeException("Directory for: " + fullPath + " does not exists, and I refuse to mkdir (do it yourself and/or fix your arguments)."));
      }

      if(file.exists())
      {
         throw (new RuntimeException("File at: " + fullPath + " already exists, and I refuse to overwrite files."));
      }

      System.out.println("Writing: " + file);
      FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   void writeAllFiles(String rootPath, String version, String baseName) throws IOException
   {
      writeAllFiles(rootPath, version, baseName, baseName);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void writeAllFiles(String rootPath, String version, String baseName, String executorBaseName) throws IOException
   {
      String basePath = rootPath + "qqq-middleware-javalin/src/main/java/com/kingsrook/qqq/middleware/javalin/";
      writeOne(basePath + "specs/" + version.toLowerCase() + "/" + baseName + "Spec" + version.toUpperCase() + ".java", makeSpec(version, baseName, executorBaseName));
      writeOne(basePath + "specs/" + version.toLowerCase() + "/responses/" + baseName + "Response" + version.toUpperCase() + ".java", makeResponse(version, baseName, executorBaseName));

      System.out.println();
      System.out.println("Hey - You probably want to add a line like:");
      System.out.println("   list.add(new " + baseName + "Spec" + version.toUpperCase() + "());");
      System.out.println("In:");
      System.out.println("   MiddlewareVersion" + version.toUpperCase() + ".java");
      System.out.println();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String makeSpec(String version, String baseName, String executorBaseName)
   {
      return """
         package com.kingsrook.qqq.middleware.javalin.specs.${version.toLowerCase};
         
         
         import java.util.List;
         import java.util.Map;
         import com.kingsrook.qqq.backend.core.utils.JsonUtils;
         import com.kingsrook.qqq.middleware.javalin.executors.${executorBaseName}Executor;
         import com.kingsrook.qqq.middleware.javalin.executors.io.${executorBaseName}Input;
         import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
         import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
         import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
         import com.kingsrook.qqq.middleware.javalin.specs.${version.toLowerCase}.responses.${executorBaseName}Response${version.toUpperCase};
         import com.kingsrook.qqq.middleware.javalin.specs.${version.toLowerCase}.utils.Tags${version.toUpperCase};
         import com.kingsrook.qqq.openapi.model.Content;
         import com.kingsrook.qqq.openapi.model.Example;
         import com.kingsrook.qqq.openapi.model.HttpMethod;
         import com.kingsrook.qqq.openapi.model.In;
         import com.kingsrook.qqq.openapi.model.Parameter;
         import com.kingsrook.qqq.openapi.model.RequestBody;
         import com.kingsrook.qqq.openapi.model.Schema;
         import com.kingsrook.qqq.openapi.model.Type;
         import io.javalin.http.ContentType;
         import io.javalin.http.Context;
         
         
         /*******************************************************************************
          **
          *******************************************************************************/
         public class ${baseName}Spec${version.toUpperCase} extends AbstractEndpointSpec<${executorBaseName}Input, ${baseName}Response${version.toUpperCase}, ${executorBaseName}Executor>
         {
         
            /***************************************************************************
             **
             ***************************************************************************/
            public BasicOperation defineBasicOperation()
            {
               return new BasicOperation()
                  .withPath(TODO)
                  .withHttpMethod(HttpMethod.TODO)
                  .withTag(Tags${version.toUpperCase}.TODO)
                  .withShortSummary(TODO)
                  .withLongDescription(""\"
                     TODO""\"
                  );
               }
         
         
            /***************************************************************************
             **
             ***************************************************************************/
            @Override
            public List<Parameter> defineRequestParameters()
            {
               return List.of(
                  new Parameter()
                     .withName(TODO)
                     .withDescription(TODO)
                     .withRequired(TODO)
                     .withSchema(new Schema().withType(Type.TODO))
                     .withExamples(Map.of("TODO", new Example().withValue(TODO)))
                     .withIn(In.TODO)
               );
            }
         
         
         
            /***************************************************************************
             **
             ***************************************************************************/
            @Override
            public RequestBody defineRequestBody()
            {
               return new RequestBody()
                  .withContent(Map.of(
                     ContentType.TODO.getMimeType(), new Content()
                        .withSchema(new Schema()
                           .withType(Type.TODO)
                           .withProperties(Map.of(
                              "TODO", new Schema()
                           ))
                        )
                  ));
            }
         
         
         
            /***************************************************************************
             **
             ***************************************************************************/
            @Override
            public ${executorBaseName}Input buildInput(Context context) throws Exception
            {
               ${executorBaseName}Input input = new ${executorBaseName}Input();
               input.setTODO
               return (input);
            }
         
         
         
            /***************************************************************************
             **
             ***************************************************************************/
            @Override
            public BasicResponse defineBasicSuccessResponse()
            {
               Map<String, Example> examples = Map.of(
         
                  "TODO", new Example()
                     .withValue(new ${baseName}Response${version.toUpperCase}()
                        .withTODO
                     )
         
               );
         
               return new BasicResponse(""\"
                  TODO""\",
         
                  new ${baseName}Response${version.toUpperCase}().toSchema(),
                  examples
               );
            }
         
         
         
            /***************************************************************************
             **
             ***************************************************************************/
            @Override
            public void handleOutput(Context context, ${baseName}Response${version.toUpperCase} output) throws Exception
            {
               context.result(JsonUtils.toJson(output));
            }
         
         }
         """
         .replaceAll("\\$\\{version.toLowerCase}", version.toLowerCase())
         .replaceAll("\\$\\{version.toUpperCase}", version.toUpperCase())
         .replaceAll("\\$\\{executorBaseName}", executorBaseName)
         .replaceAll("\\$\\{baseName}", baseName)
         ;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String makeResponse(String version, String baseName, String executorBaseName)
   {
      return """
         package com.kingsrook.qqq.middleware.javalin.specs.${version.toLowerCase}.responses;
         
         
         import com.kingsrook.qqq.middleware.javalin.executors.io.${executorBaseName}OutputInterface;
         import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
         import com.kingsrook.qqq.middleware.javalin.specs.ToSchema;
         
         
         /*******************************************************************************
          **
          *******************************************************************************/
         public class ${baseName}Response${version.toUpperCase} implements ${executorBaseName}OutputInterface, ToSchema
         {
            @OpenAPIDescription(TODO)
            private String TODO;
         
            TODO gsw
         }
         """
         .replaceAll("\\$\\{version.toLowerCase}", version.toLowerCase())
         .replaceAll("\\$\\{version.toUpperCase}", version.toUpperCase())
         .replaceAll("\\$\\{executorBaseName}", executorBaseName)
         .replaceAll("\\$\\{baseName}", baseName)
         ;
   }

}
