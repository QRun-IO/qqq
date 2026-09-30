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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;


/*******************************************************************************
 **
 *******************************************************************************/
public class APIUtils
{
   public static final String       PUBLISHED_API_LOCATION = "qqq-middleware-javalin/src/main/resources/openapi/";
   public static final List<String> FILE_FORMATS           = List.of("json", "yaml");



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getPublishedAPIFile(String apiPath, String name, String fileFormat) throws Exception
   {
      String fileLocation = apiPath + "/" + name + "." + fileFormat;
      File   file         = new File(fileLocation);
      if(!file.exists())
      {
         throw (new Exception("Error: The file [" + file.getPath() + "] could not be found."));
      }

      Path path = Paths.get(fileLocation);
      return (StringUtils.join("\n", Files.readAllLines(path)));
   }



   /*******************************************************************************
    ** get a map representation of the yaml.
    ** we'll remove things from that map, writing out specific sub-files that we know we want (e.g., per-table & process).
    ** also, there are some objects we just don't care about (e.g., tags, they'll always be in lock-step with the tables).
    ** then we'll write out everything else left in the map at the end.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   static Map<String, Map<String, Object>> splitUpYamlForPublishing(String openApiYaml) throws JsonProcessingException
   {
      Map<String, Object> apiYaml    = parseYaml(openApiYaml);
      Map<String, Object> components = (Map<String, Object>) apiYaml.get("components");
      Map<String, Object> schemas    = (Map<String, Object>) components.get("schemas");
      Map<String, Object> paths      = (Map<String, Object>) apiYaml.remove("paths");
      apiYaml.remove("tags");

      Map<String, Map<String, Object>> groupedPaths = new HashMap<>();
      for(Map.Entry<String, Object> entry : paths.entrySet())
      {
         ///////////////////////////////////////////////////////////////////////////////
         // keys here look like:  apiName/apiVersion/table-or-process/<maybe-more>    //
         // let's discard the apiName & version, and group under the table-or-process //
         ///////////////////////////////////////////////////////////////////////////////
         String   key        = entry.getKey();
         String[] parts      = key.split("/");
         String   uniquePart = parts[3];
         groupedPaths.computeIfAbsent(uniquePart, k -> new TreeMap<>());
         groupedPaths.get(uniquePart).put(entry.getKey(), entry.getValue());
      }

      for(Map.Entry<String, Map<String, Object>> entry : groupedPaths.entrySet())
      {
         String              name   = entry.getKey();
         Map<String, Object> subMap = entry.getValue();
         if(schemas.containsKey(name))
         {
            subMap.put("schema", schemas.remove(name));
         }

         name += "SearchResult";
         if(schemas.containsKey(name))
         {
            subMap.put("searchResultSchema", schemas.remove(name));
         }
      }

      ////////////////////////////////////////////////////////
      // put the left-over yaml as a final entry in the map //
      ////////////////////////////////////////////////////////
      groupedPaths.put("openapi", apiYaml);

      return groupedPaths;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static Map<String, Object> parseYaml(String yaml) throws JsonProcessingException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      // need a larger limit than you get by default (and qqq's yamlUtils doens't let us customize this...) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      LoaderOptions loaderOptions = new LoaderOptions();
      loaderOptions.setCodePointLimit(100 * 1024 * 1024); // 100 MB
      YAMLFactory yamlFactory = YAMLFactory.builder()
         .loaderOptions(loaderOptions)
         .build();
      YAMLMapper mapper = new YAMLMapper(yamlFactory);

      mapper.findAndRegisterModules();
      return (mapper.readValue(yaml, Map.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static File[] listPublishedAPIFiles(String path) throws Exception
   {
      File file = new File(path);
      if(!file.exists())
      {
         throw (new Exception("Error: API Directory [" + file.getPath() + "] could not be found."));
      }

      File[] files = file.listFiles();
      return (files);
   }

}
