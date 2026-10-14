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

package com.kingsrook.qqq.api.middleware.specs.v1;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.middleware.javalin.tools.UiApiContract;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The committed document of the API-aware UI API routes
 ** (/qqq/v1/{applicationApiPath}/{applicationApiVersion}/..., served by
 ** ApiAwareMiddlewareVersionV1) matches the code, so clients can vendor it
 ** (Next does, as extensions/api-aware.json).  Regenerate with
 ** -Dqqq.uiApi.regenerate=true.
 *******************************************************************************/
class ApiAwareUiApiContractTest extends BaseTest
{
   private static final Path DOCUMENT = Path.of("src/main/resources/openapi/v1/api-aware.yaml");



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testCommittedDocumentMatchesCode() throws Exception
   {
      String generated = UiApiContract.generateCoreYaml(new ApiAwareMiddlewareVersionV1());
      if(Boolean.getBoolean("qqq.uiApi.regenerate") || !Files.exists(DOCUMENT))
      {
         Files.createDirectories(DOCUMENT.getParent());
         Files.writeString(DOCUMENT, generated, StandardCharsets.UTF_8);
      }

      String       committed = Files.readString(DOCUMENT, StandardCharsets.UTF_8);
      List<String> problems  = UiApiContract.check(generated, committed, committed, 1, "run this test with -Dqqq.uiApi.regenerate=true");
      assertThat(problems).as("API-aware UI API document problems").isEmpty();
      assertThat(committed).contains("/qqq/v1/{applicationApiPath}/{applicationApiVersion}/table/{tableName}/query");
   }

}
