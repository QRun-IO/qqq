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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.tools.OpenApiCompatibilityChecker;
import com.kingsrook.qqq.middleware.javalin.tools.UiApiContract;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** The UI API contract: the committed v1 OpenAPI document must equal the one
 ** generated from the code, must not break the release baseline, and must
 ** carry a semantic version bumped past the baseline whenever it changed.
 **
 ** On failure: regenerate with
 **   java ... com.kingsrook.qqq.middleware.javalin.tools.PublishAPI -r {repoRoot}
 ** bump MiddlewareVersionV1.SEMANTIC_VERSION (MINOR for additions, PATCH for
 ** fixes) and add an entry to docs/UI-API-CHANGELOG.md.  A breaking change
 ** needs /qqq/v2.
 *******************************************************************************/
class UiApiContractTest
{
   private static final Path   VERSION_DIRECTORY = Path.of("src/main/resources/openapi/v1");
   private static final String REGENERATE_HINT   = "run com.kingsrook.qqq.middleware.javalin.tools.PublishAPI -r <repo root>, bump MiddlewareVersionV1.SEMANTIC_VERSION and update docs/UI-API-CHANGELOG.md";



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testCommittedDocumentMatchesCodeAndBaseline() throws Exception
   {
      String generated = UiApiContract.generateCoreYaml(new MiddlewareVersionV1());
      String committed = Files.readString(VERSION_DIRECTORY.resolve("openapi.yaml"), StandardCharsets.UTF_8);
      String baseline  = Files.readString(VERSION_DIRECTORY.resolve("baseline.yaml"), StandardCharsets.UTF_8);

      List<String> problems = UiApiContract.check(generated, committed, baseline, 1, REGENERATE_HINT);
      assertThat(problems).as("UI API contract problems").isEmpty();

      Map<String, Object> committedDocument = OpenApiCompatibilityChecker.parse(committed);
      assertEquals(MiddlewareVersionV1.SEMANTIC_VERSION, OpenApiCompatibilityChecker.getInfoVersion(committedDocument));
   }



   /***************************************************************************
    ** the checks themselves, on small documents
    ***************************************************************************/
   @Test
   void testBreakingChangeDetection() throws Exception
   {
      String base = """
         openapi: "3.0.0"
         info:
           version: "1.0.0"
         paths:
           /a:
             get:
               parameters:
               - name: "x"
                 in: "query"
                 schema:
                   type: "string"
               responses:
                 "200":
                   content:
                     application/json:
                       schema:
                         $ref: "#/components/schemas/A"
         components:
           schemas:
             A:
               type: "object"
               properties:
                 description:
                   type: "string"
                 kind:
                   type: "string"
                   enum: ["ONE", "TWO"]
         """;

      //////////////////////////////
      // identical: no problems   //
      //////////////////////////////
      assertThat(UiApiContract.check(base, base, base, 1, "x")).isEmpty();

      ////////////////////////////////////////////////////////////////////
      // additive change with a minor bump: ok; without a bump: problem //
      ////////////////////////////////////////////////////////////////////
      String added = base.replace("        kind:", "        extra:\n          type: \"integer\"\n        kind:");
      assertThat(UiApiContract.check(added.replace("1.0.0", "1.1.0"), added.replace("1.0.0", "1.1.0"), base, 1, "x")).isEmpty();
      assertThat(UiApiContract.check(added, added, base, 1, "x")).anyMatch(p -> p.contains("not bumped"));

      ////////////////////////////////////////////////////
      // documentation-only change needs no bump        //
      ////////////////////////////////////////////////////
      String documented = base.replace("    get:", "    get:\n      summary: \"Get an A\"");
      assertThat(UiApiContract.check(documented, documented, base, 1, "x")).isEmpty();

      //////////////////////////
      // breaking changes     //
      //////////////////////////
      Map<String, Object> baseDocument = OpenApiCompatibilityChecker.parse(base);
      assertThat(breaking(baseDocument, base.replace("/a:", "/b:"))).anyMatch(p -> p.contains("path removed"));
      assertThat(breaking(baseDocument, base.replace("get:", "post:"))).anyMatch(p -> p.contains("operation removed"));
      assertThat(breaking(baseDocument, base.replace("enum: [\"ONE\", \"TWO\"]", "enum: [\"ONE\"]"))).anyMatch(p -> p.contains("enum value removed: TWO"));
      assertThat(breaking(baseDocument, base.replace("        description:\n          type: \"string\"\n", ""))).anyMatch(p -> p.contains("property removed: description"));
      assertThat(breaking(baseDocument, base.replace("        in: \"query\"\n", "        in: \"query\"\n        required: true\n"))).anyMatch(p -> p.contains("parameter became required"));
      assertThat(breaking(baseDocument, base.replace("type: \"object\"", "type: \"array\""))).anyMatch(p -> p.contains("type changed"));
      assertThat(breaking(baseDocument, base.replace("      type: \"object\"\n", "      type: \"object\"\n      required: [\"kind\"]\n"))).anyMatch(p -> p.contains("became required: kind"));
      assertThat(breaking(baseDocument, added)).isEmpty();
      assertThat(UiApiContract.check(base.replace("1.0.0", "2.0.0"), base.replace("1.0.0", "2.0.0"), base, 1, "x")).anyMatch(p -> p.contains("MAJOR 1"));
      assertThat(UiApiContract.check(base.replace("1.0.0", "0.9.0"), base.replace("1.0.0", "0.9.0"), base, 1, "x")).anyMatch(p -> p.contains("lower than the baseline"));
      assertThat(UiApiContract.check(base.replace("1.0.0", "v1"), base.replace("1.0.0", "v1"), null, 1, "x")).anyMatch(p -> p.contains("not a semantic version"));
      assertTrue(OpenApiCompatibilityChecker.isStructurallyEqual(baseDocument, OpenApiCompatibilityChecker.parse(documented)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static List<String> breaking(Map<String, Object> baseDocument, String newYaml) throws Exception
   {
      return (OpenApiCompatibilityChecker.findBreakingChanges(baseDocument, OpenApiCompatibilityChecker.parse(newYaml)));
   }
}
