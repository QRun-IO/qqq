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

package com.kingsrook.qqq.middleware.javalin.tools;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.MapperFeature;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.openapi.model.OpenAPI;


/*******************************************************************************
 ** The UI API contract check, shared by core's UiApiContractTest and by the
 ** contract tests of modules and qbits that register extensions.
 **
 ** Given the freshly generated document, the committed document and
 ** (optionally) the release baseline, it reports:
 ** - any difference between generated and committed (regenerate and commit);
 ** - any breaking change from baseline to committed (needs /qqq/v{N+1});
 ** - a changed document whose info.version was not bumped past the baseline;
 ** - a MAJOR that does not match the URL version.
 ** See docs/UI-API-CHANGELOG.md.
 *******************************************************************************/
public class UiApiContract
{

   /*******************************************************************************
    ** The core document of a middleware version, as PublishAPI writes it
    ** (properties sorted, for stable diffs).  Extensions are not included.
    *******************************************************************************/
   public static String generateCoreYaml(AbstractMiddlewareVersion middlewareVersion) throws QException
   {
      return (toYaml(middlewareVersion.generateOpenAPIModel("qqq")));
   }



   /*******************************************************************************
    ** The document of one extension alone, as a module or qbit commits it.
    *******************************************************************************/
   public static String generateExtensionYaml(AbstractMiddlewareVersion middlewareVersion, MiddlewareSpecExtension extension) throws QException
   {
      return (toYaml(middlewareVersion.generateExtensionOpenAPIModel("qqq", extension)));
   }



   /*******************************************************************************
    ** Serialize an OpenAPI model the way the committed files are written.
    *******************************************************************************/
   public static String toYaml(OpenAPI openAPI)
   {
      return (YamlUtils.toYamlCustomized(openAPI, mapperBuilder -> mapperBuilder.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)));
   }



   /*******************************************************************************
    ** Run the contract checks.  Returns the problems found (empty when the
    ** contract holds).
    **
    ** @param generatedYaml the document generated from the code now
    ** @param committedYaml the committed document
    ** @param baselineYaml  the document as of the last release (null to skip
    **                      the compatibility and version checks)
    ** @param expectedMajor the URL major version (1 for /qqq/v1)
    ** @param regenerateHint how to regenerate, for the message
    *******************************************************************************/
   public static List<String> check(String generatedYaml, String committedYaml, String baselineYaml, int expectedMajor, String regenerateHint)
   {
      return (check(generatedYaml, committedYaml, baselineYaml, expectedMajor, regenerateHint, List.of()));
   }



   /*******************************************************************************
    ** Run the contract checks, accepting listed breaking-change reports.  An
    ** accepted change is a correction of the document to what the server always
    ** did (so no client could have relied on the old description); each entry is
    ** a substring of the reported change.  Accepted changes are cleared when the
    ** baseline is refreshed.  See docs/UI-API-CHANGELOG.md.
    *******************************************************************************/
   public static List<String> check(String generatedYaml, String committedYaml, String baselineYaml, int expectedMajor, String regenerateHint, List<String> acceptedBreakingChanges)
   {
      List<String> problems = new ArrayList<>();

      String generated = normalize(generatedYaml);
      String committed = normalize(committedYaml);
      if(!generated.equals(committed) && !isSameDocument(generated, committed))
      {
         problems.add("The committed OpenAPI document differs from the one generated from the code (" + firstDifference(committed, generated) + "). Regenerate and commit it: " + regenerateHint);
      }

      try
      {
         Map<String, Object> committedDocument = OpenApiCompatibilityChecker.parse(committedYaml);
         String              committedVersion  = OpenApiCompatibilityChecker.getInfoVersion(committedDocument);

         if(committedVersion == null || !committedVersion.matches("\\d+\\.\\d+\\.\\d+"))
         {
            problems.add("info.version [" + committedVersion + "] is not a semantic version (MAJOR.MINOR.PATCH)");
         }
         else if(OpenApiCompatibilityChecker.compareSemver(committedVersion, expectedMajor + ".0.0") < 0 || OpenApiCompatibilityChecker.compareSemver(committedVersion, (expectedMajor + 1) + ".0.0") >= 0)
         {
            problems.add("info.version [" + committedVersion + "] must have MAJOR " + expectedMajor + " to match the URL version; a new MAJOR needs a new URL version");
         }

         if(baselineYaml != null)
         {
            Map<String, Object> baselineDocument = OpenApiCompatibilityChecker.parse(baselineYaml);
            String              baselineVersion  = OpenApiCompatibilityChecker.getInfoVersion(baselineDocument);

            for(String breaking : OpenApiCompatibilityChecker.findBreakingChanges(baselineDocument, committedDocument))
            {
               if(acceptedBreakingChanges.stream().anyMatch(accepted -> !accepted.isBlank() && breaking.contains(accepted)))
               {
                  continue;
               }
               problems.add("Breaking change since " + baselineVersion + " (needs a new URL version, /qqq/v" + (expectedMajor + 1) + "): " + breaking);
            }

            int versionComparison = OpenApiCompatibilityChecker.compareSemver(committedVersion, baselineVersion);
            if(versionComparison < 0)
            {
               problems.add("info.version [" + committedVersion + "] is lower than the baseline [" + baselineVersion + "]");
            }
            else if(versionComparison == 0 && !OpenApiCompatibilityChecker.isStructurallyEqual(baselineDocument, committedDocument))
            {
               problems.add("The document changed since baseline " + baselineVersion + " but info.version was not bumped (MINOR for additions, PATCH for fixes)");
            }
         }
      }
      catch(Exception e)
      {
         problems.add("Could not parse the OpenAPI documents: " + e.getMessage());
      }

      return (problems);
   }



   /*******************************************************************************
    ** Whether two documents are equal as data - ignoring the order of object keys,
    ** which is not stable for some serialized examples (POJO property order
    ** follows reflection order, which can differ between JVM runs).
    *******************************************************************************/
   private static boolean isSameDocument(String a, String b)
   {
      try
      {
         return (OpenApiCompatibilityChecker.parse(a).equals(OpenApiCompatibilityChecker.parse(b)));
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String normalize(String yaml)
   {
      return (yaml == null ? "" : yaml.replace("\r\n", "\n").strip());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String firstDifference(String expected, String actual)
   {
      String[] expectedLines = expected.split("\n");
      String[] actualLines   = actual.split("\n");
      for(int i = 0; i < Math.max(expectedLines.length, actualLines.length); i++)
      {
         String e = i < expectedLines.length ? expectedLines[i] : "<end of file>";
         String a = i < actualLines.length ? actualLines[i] : "<end of file>";
         if(!e.equals(a))
         {
            return ("first difference at line " + (i + 1) + ": committed [" + e.strip() + "] generated [" + a.strip() + "]");
         }
      }
      return ("no line difference");
   }
}
