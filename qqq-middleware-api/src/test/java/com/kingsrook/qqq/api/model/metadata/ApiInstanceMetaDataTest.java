/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata;


import java.util.List;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ApiInstanceMetaData
 *******************************************************************************/
class ApiInstanceMetaDataTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidationPasses()
   {
      assertValidationErrors(makeBaselineValidApiInstanceMetaDataWithVersions(), List.of());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidationMissingThings()
   {
      assertValidationErrors(new ApiInstanceMetaData(), List.of(
         "Missing name",
         "Missing label",
         "Missing path",
         "Missing description",
         "Missing contactEmail",
         "Missing currentVersion",
         "Missing supportedVersions"
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidationInstanceVersionIssues()
   {
      assertValidationErrors(makeBaselineValidApiInstanceMetaData()
            .withCurrentVersion(new APIVersion("2023.Q1"))
            .withSupportedVersions(List.of(new APIVersion("2022.Q3"), new APIVersion("2022.Q4"))),
         List.of("supportedVersions [[2022.Q3, 2022.Q4]] does not contain currentVersion [2023.Q1]"));

      assertValidationErrors(makeBaselineValidApiInstanceMetaData()
            .withCurrentVersion(new APIVersion("2023.Q1"))
            .withSupportedVersions(List.of(new APIVersion("2023.Q1")))
            .withPastVersions(List.of(new APIVersion("2022.Q4"), new APIVersion("2023.Q1"), new APIVersion("2023.Q2"))),
         List.of(
            "pastVersion [2023.Q2] is not lexicographically before currentVersion",
            "pastVersion [2023.Q1] is not lexicographically before currentVersion"
         ));

      assertValidationErrors(makeBaselineValidApiInstanceMetaData()
            .withCurrentVersion(new APIVersion("2023.Q1"))
            .withSupportedVersions(List.of(new APIVersion("2023.Q1")))
            .withFutureVersions(List.of(new APIVersion("2022.Q4"), new APIVersion("2023.Q1"), new APIVersion("2023.Q2"))),
         List.of(
            "futureVersion [2022.Q4] is not lexicographically after currentVersion",
            "futureVersion [2023.Q1] is not lexicographically after currentVersion"
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidationTableVersionIssues()
   {
      QInstance qInstance = new QInstance();

      qInstance.addTable(new QTableMetaData()
         .withName("myValidTable")
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion("2023.Q1"))));

      qInstance.addTable(new QTableMetaData()
         .withName("myInvalidTable")
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion("notAVersion"))));

      assertValidationErrors(qInstance, makeBaselineValidApiInstanceMetaData()
            .withCurrentVersion(new APIVersion("2023.Q1"))
            .withSupportedVersions(List.of(new APIVersion("2022.Q4"), new APIVersion("2023.Q1"))),
         List.of("Table myInvalidTable's initial API version is not a recognized version"));

      qInstance.addTable(new QTableMetaData()
         .withName("myFutureValidTable")
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion("2024.Q1"))));

      assertValidationErrors(qInstance, makeBaselineValidApiInstanceMetaData()
            .withCurrentVersion(new APIVersion("2023.Q1"))
            .withSupportedVersions(List.of(new APIVersion("2023.Q1")))
            .withFutureVersions(List.of(new APIVersion("2024.Q1"))),
         List.of("Table myInvalidTable's initial API version is not a recognized version"));

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPathSlashes()
   {
      assertValidationErrors(makeBaselineValidApiInstanceMetaDataWithVersions().withPath("myPath/"), List.of("does not start with '/'"));
      assertValidationErrors(makeBaselineValidApiInstanceMetaDataWithVersions().withPath("/yourPath"), List.of("does not end with '/'"));
      assertValidationErrors(makeBaselineValidApiInstanceMetaDataWithVersions().withPath("any/path"), List.of("does not start with '/'", "does not end with '/'"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ApiInstanceMetaData makeBaselineValidApiInstanceMetaData()
   {
      return (new ApiInstanceMetaData()
         .withName(TestUtils.API_NAME)
         .withPath("/api/")
         .withLabel("QQQ API")
         .withDescription("Test API for QQQ")
         .withContactEmail("contact@kingsrook.com"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ApiInstanceMetaData makeBaselineValidApiInstanceMetaDataWithVersions()
   {
      return (makeBaselineValidApiInstanceMetaData()
         .withCurrentVersion(new APIVersion("1"))
         .withSupportedVersions(List.of(new APIVersion("1"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertValidationErrors(ApiInstanceMetaData apiInstanceMetaData, List<String> expectedErrors)
   {
      QInstance qInstance = new QInstance();
      assertValidationErrors(qInstance, apiInstanceMetaData, expectedErrors);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertValidationErrors(QInstance qInstance, ApiInstanceMetaData apiInstanceMetaData, List<String> expectedErrors)
   {
      qInstance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(apiInstanceMetaData));

      QInstanceValidator validator = new QInstanceValidator();
      apiInstanceMetaData.validate(apiInstanceMetaData.getName(), qInstance, validator);

      List<String> errors = validator.getErrors();
      assertEquals(expectedErrors.size(), errors.size(), "Expected # of validation errors (got: " + errors + ")");

      for(String expectedError : expectedErrors)
      {
         assertThat(errors).withFailMessage("Expected any of:\n   " + StringUtils.join("\n   ", errors) + "\nto contain:\n   " + expectedError).anyMatch(e -> e.contains(expectedError));
      }
   }
}