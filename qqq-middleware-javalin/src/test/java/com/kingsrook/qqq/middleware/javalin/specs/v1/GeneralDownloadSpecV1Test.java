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


import java.io.File;
import java.io.FileWriter;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for GeneralDownloadSpecV1
 *******************************************************************************/
class GeneralDownloadSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new GeneralDownloadSpecV1();
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
   void testDownloadFromFilePath() throws Exception
   {
      //////////////////////////////////////////
      // create a temp file to download       //
      //////////////////////////////////////////
      File tempFile = File.createTempFile("test-download-", ".txt");
      tempFile.deleteOnExit();

      try(FileWriter writer = new FileWriter(tempFile))
      {
         writer.write("Hello, download test content!");
      }

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/download/test-file.txt")
         .queryString("filePath", tempFile.getAbsolutePath())
         .asString();

      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("test-file.txt");
      assertThat(response.getBody()).isEqualTo("Hello, download test content!");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDownloadMissingParams()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/download/test-file.txt")
         .asString();

      assertThat(response.getStatus()).isIn(400, 500);
   }

}
