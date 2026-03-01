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
import kong.unirest.Config;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RecordFieldDownloadSpecV1
 *******************************************************************************/
class RecordFieldDownloadSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new RecordFieldDownloadSpecV1();
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
    **
    *******************************************************************************/
   @Test
   void testDownloadBlobField()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/1/photo/darin-photo.png")
         .asString();

      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("darin-photo.png");
      assertThat(response.getHeaders().getFirst("Content-Type")).contains("image");
      assertThat(response.getBody()).isNotEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDownloadFieldNotFound()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/1/nonExistentField/file.txt")
         .asString();

      assertEquals(404, response.getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDownloadRecordNotFound()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/99999/photo/photo.png")
         .asString();

      assertEquals(404, response.getStatus());
   }



   /*******************************************************************************
    ** Test downloading a non-blob field (licenseScanPdfUrl), which should result
    ** in a redirect to the URL stored in that field.
    *******************************************************************************/
   @Test
   void testDownloadNonBlobField_redirect()
   {
      ///////////////////////////////////////////////////////////////////////
      // use a UnirestInstance with redirects disabled so we can inspect   //
      // the 3xx response rather than following the redirect automatically //
      ///////////////////////////////////////////////////////////////////////
      UnirestInstance unirest = new UnirestInstance(new Config().followRedirects(false));
      try
      {
         HttpResponse<String> response = unirest.get(getBaseUrlAndPath() + "/table/person/1/licenseScanPdfUrl/License-1.pdf")
            .asString();

         assertThat(response.getStatus()).isEqualTo(302);
         assertThat(response.getHeaders().getFirst("Location")).contains("somedomain");
      }
      finally
      {
         unirest.close();
      }
   }

}
