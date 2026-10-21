/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.api.specs;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.kingsrook.qqq.esb.api.EsbJavalinMetaDataProducer;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.tools.UiApiContract;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The ESB extension of the UI API: its committed OpenAPI document
 ** (src/main/resources/openapi/qqq-v1/esb.yaml) must equal the one generated
 ** from the specs, and carry a semantic version.  Regenerate with
 ** -Dqqq.uiApi.regenerate=true.
 *******************************************************************************/
class EsbUiApiContractTest
{
   private static final Path COMMITTED = Path.of("src/main/resources/openapi/qqq-v1/esb.yaml");



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testCommittedDocumentMatchesSpecs() throws Exception
   {
      String generated = UiApiContract.generateExtensionYaml(new MiddlewareVersionV1(), EsbJavalinMetaDataProducer.defineExtension());
      if(Boolean.getBoolean("qqq.uiApi.regenerate"))
      {
         Files.createDirectories(COMMITTED.getParent());
         Files.writeString(COMMITTED, generated, StandardCharsets.UTF_8);
      }

      String       committed = Files.readString(COMMITTED, StandardCharsets.UTF_8);
      List<String> problems  = UiApiContract.check(generated, committed, null, 1, "run this test with -Dqqq.uiApi.regenerate=true and bump EsbJavalinMetaDataProducer.EXTENSION_VERSION");
      assertThat(problems).isEmpty();
      assertThat(generated).contains("/qqq/v1/esb/overview", "/qqq/v1/esb/table/{table}", "EsbOverviewResponseV1");
   }
}
