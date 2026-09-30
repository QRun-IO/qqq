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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ReportRunSpecV1
 *******************************************************************************/
class ReportRunSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ReportRunSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    ** The report runs with its input field from the query string and streams CSV.
    *******************************************************************************/
   @Test
   void testRunReport()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/reports/personsReport?format=csv&firstNamePrefix=D").asString();

      assertEquals(200, response.getStatus(), response.getBody());
      assertThat(response.getHeaders().getFirst("Content-Type")).contains("text/csv");
      assertEquals("attachment; filename=\"personsReport.csv\"", response.getHeaders().getFirst("Content-Disposition"));
      assertThat(response.getBody()).contains("""
         "Id","First Name","Last Name\"""");
      assertThat(response.getBody()).contains("""
         "1","Darin","Kelkhoff\"""");
   }



   /*******************************************************************************
    ** Unknown reports are 404; a missing or unsupported format is a 400.
    *******************************************************************************/
   @Test
   void testRefusals()
   {
      assertEquals(404, Unirest.get(getBaseUrlAndPath() + "/reports/notAReport?format=csv").asString().getStatus());
      assertEquals(400, Unirest.get(getBaseUrlAndPath() + "/reports/personsReport?firstNamePrefix=D").asString().getStatus());
      assertEquals(400, Unirest.get(getBaseUrlAndPath() + "/reports/personsReport?format=pdfish").asString().getStatus());
   }

}
