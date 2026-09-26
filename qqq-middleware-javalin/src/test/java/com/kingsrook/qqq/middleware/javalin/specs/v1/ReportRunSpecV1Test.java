/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
