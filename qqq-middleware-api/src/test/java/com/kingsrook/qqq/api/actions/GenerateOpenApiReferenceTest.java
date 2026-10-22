/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions;


import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecInput;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Generated references must address existing document nodes in both renderings.
 *******************************************************************************/
class GenerateOpenApiReferenceTest extends BaseTest
{
   /*******************************************************************************
    ** Catch malformed query-example pointers without adding a schema validator.
    *******************************************************************************/
   @Test
   void testLocalReferencesResolveInJsonAndYaml() throws Exception
   {
      var output = new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput()
         .withApiName(TestUtils.API_NAME).withVersion(TestUtils.CURRENT_API_VERSION));
      for(JsonNode document : List.of(JsonUtils.toObject(output.getJson(), JsonNode.class), new YAMLMapper().readTree(output.getYaml())))
      {
         List<String> references = document.findValuesAsText("$ref");
         assertTrue(references.contains("#/components/examples/criteriaStringEquals"));
         for(String reference : references)
         {
            assertTrue(reference.startsWith("#/"), reference);
            assertFalse(document.at(reference.substring(1)).isMissingNode(), reference);
         }
      }
   }
}
