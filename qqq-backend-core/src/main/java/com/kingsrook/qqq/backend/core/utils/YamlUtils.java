/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import java.util.Map;
import java.util.function.Consumer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kingsrook.qqq.backend.core.logging.QLogger;


/*******************************************************************************
 **
 *******************************************************************************/
public class YamlUtils
{
   private static final QLogger LOG = QLogger.getLogger(YamlUtils.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<String, Object> toMap(String yaml) throws JsonProcessingException
   {
      ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
      objectMapper.findAndRegisterModules();

      @SuppressWarnings("unchecked")
      Map<String, Object> map = objectMapper.readValue(yaml, Map.class);

      return map;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String toYaml(Object object)
   {
      return toYamlCustomized(object, null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   /*******************************************************************************
    **
    *******************************************************************************/
   public static String toYamlCustomized(Object object, Consumer<YAMLMapper.Builder> yamlMapperCustomizer)
   {
      try
      {
         YAMLFactory yamlFactory = new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

         YAMLMapper.Builder yamlMapperBuilder = YAMLMapper.builder(yamlFactory);
         yamlMapperBuilder.serializationInclusion(JsonInclude.Include.NON_NULL);
         yamlMapperBuilder.serializationInclusion(JsonInclude.Include.NON_EMPTY);

         if(yamlMapperCustomizer != null)
         {
            yamlMapperCustomizer.accept(yamlMapperBuilder);
         }

         YAMLMapper yamlMapper = yamlMapperBuilder.build();
         yamlMapper.findAndRegisterModules();
         return (yamlMapper.writeValueAsString(object));
      }
      catch(Exception e)
      {
         LOG.error("Error serializing object of type [" + object.getClass().getSimpleName() + "] to yaml", e);
         throw new IllegalArgumentException("Error in YAML Serialization", e);
      }
   }

}
