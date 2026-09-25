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
import com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.session.QSession;
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

      /////////////////////////////////////////////////////////////////////////
      // a file no process registered for this session is refused, even in //
      // the temp directory                                                  //
      /////////////////////////////////////////////////////////////////////////
      HttpResponse<String> refused = Unirest.get(getBaseUrlAndPath() + "/download/test-file.txt")
         .queryString("filePath", tempFile.getAbsolutePath())
         .cookie("sessionId", "download-session")
         .asString();
      assertEquals(403, refused.getStatus());
      assertThat(refused.getBody()).doesNotContain("Hello, download test content!");

      //////////////////////////////////////////////////////////////////////////////
      // once a process registers it for the session (as process code does), it //
      // downloads, and only for that session                                     //
      //////////////////////////////////////////////////////////////////////////////
      QSession session = new QSession();
      session.setUuid("download-session");
      session.setIdReference("download-session");
      QContext.init(serverQInstance, session);
      String registeredPath = ProcessFileDownload.register(tempFile);
      QContext.clear();

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/download/test%20file.txt")
         .queryString("filePath", registeredPath)
         .cookie("sessionId", "download-session")
         .asString();

      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals("attachment; filename*=UTF-8''test%20file.txt", response.getHeaders().getFirst("Content-Disposition"));
      assertThat(response.getBody()).isEqualTo("Hello, download test content!");

      HttpResponse<String> otherSession = Unirest.get(getBaseUrlAndPath() + "/download/test-file.txt")
         .queryString("filePath", registeredPath)
         .cookie("sessionId", "another-session")
         .asString();
      assertEquals(403, otherSession.getStatus());
   }



   /*******************************************************************************
    ** A storage file is served only when its table and reference were registered
    ** for the session.
    *******************************************************************************/
   @Test
   void testUnregisteredStorageReferenceIsRefused()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/download/report.csv")
         .queryString("storageTableName", "person")
         .queryString("storageReference", "anything.csv")
         .cookie("sessionId", "download-session")
         .asString();
      assertEquals(403, response.getStatus());
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



   /*******************************************************************************
    ** Verify that a path traversal attack is blocked (file outside of tmpdir).
    *******************************************************************************/
   @Test
   void testPathTraversalAttack_etcPasswd()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/download/passwd")
         .queryString("filePath", "/etc/passwd")
         .asString();

      assertEquals(403, response.getStatus());
      assertThat(response.getBody()).doesNotContain("root:");
   }



   /*******************************************************************************
    ** Verify that a path traversal using relative segments is blocked.
    *******************************************************************************/
   @Test
   void testPathTraversalAttack_dotDot()
   {
      String tmpDir = System.getProperty("java.io.tmpdir");
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/download/passwd")
         .queryString("filePath", tmpDir + "/../etc/passwd")
         .asString();

      assertEquals(403, response.getStatus());
      assertThat(response.getBody()).doesNotContain("root:");
   }

}
