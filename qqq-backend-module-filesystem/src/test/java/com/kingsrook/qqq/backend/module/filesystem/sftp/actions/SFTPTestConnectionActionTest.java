/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.filesystem.sftp.actions;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for SFTPTestConnectionAction 
 *******************************************************************************/
class SFTPTestConnectionActionTest extends BaseSFTPTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccessWithoutPath()
   {
      SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
         .withUsername(BaseSFTPTest.USERNAME)
         .withPassword(BaseSFTPTest.PASSWORD)
         .withPort(BaseSFTPTest.getCurrentPort())
         .withHostName(BaseSFTPTest.HOST_NAME);
      SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);
      assertTrue(output.getIsConnectionSuccess());
      assertNull(output.getConnectionErrorMessage());
      assertNull(output.getIsListBasePathSuccess());
      assertNull(output.getListBasePathErrorMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccessWithPath()
   {
      SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
         .withUsername(BaseSFTPTest.USERNAME)
         .withPassword(BaseSFTPTest.PASSWORD)
         .withPort(BaseSFTPTest.getCurrentPort())
         .withHostName(BaseSFTPTest.HOST_NAME)
         .withBasePath(BaseSFTPTest.BACKEND_FOLDER);
      SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);
      assertTrue(output.getIsConnectionSuccess());
      assertNull(output.getConnectionErrorMessage());
      assertTrue(output.getIsListBasePathSuccess());
      assertNull(output.getListBasePathErrorMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccessfulConnectFailedPath()
   {
      SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
         .withUsername(BaseSFTPTest.USERNAME)
         .withPassword(BaseSFTPTest.PASSWORD)
         .withPort(BaseSFTPTest.getCurrentPort())
         .withHostName(BaseSFTPTest.HOST_NAME)
         .withBasePath("no-such-path");
      SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);
      assertTrue(output.getIsConnectionSuccess());
      assertNull(output.getConnectionErrorMessage());
      assertFalse(output.getIsListBasePathSuccess());
      assertNotNull(output.getListBasePathErrorMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadUsername()
   {
      SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
         .withUsername("not-" + BaseSFTPTest.USERNAME)
         .withPassword(BaseSFTPTest.PASSWORD)
         .withPort(BaseSFTPTest.getCurrentPort())
         .withHostName(BaseSFTPTest.HOST_NAME);
      SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);
      assertFalse(output.getIsConnectionSuccess());
      assertNotNull(output.getConnectionErrorMessage());
      assertNull(output.getIsListBasePathSuccess());
      assertNull(output.getListBasePathErrorMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadPassword()
   {
      SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
         .withUsername(BaseSFTPTest.USERNAME)
         .withPassword("not-" + BaseSFTPTest.PASSWORD)
         .withPort(BaseSFTPTest.getCurrentPort())
         .withHostName(BaseSFTPTest.HOST_NAME);
      SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);
      assertFalse(output.getIsConnectionSuccess());
      assertNotNull(output.getConnectionErrorMessage());
      assertNull(output.getIsListBasePathSuccess());
      assertNull(output.getListBasePathErrorMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadHostname()
   {
      SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
         .withUsername(BaseSFTPTest.USERNAME)
         .withPassword(BaseSFTPTest.PASSWORD)
         .withPort(BaseSFTPTest.getCurrentPort())
         .withHostName("not-" + BaseSFTPTest.HOST_NAME);
      SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);
      assertFalse(output.getIsConnectionSuccess());
      assertNotNull(output.getConnectionErrorMessage());
      assertNull(output.getIsListBasePathSuccess());
      assertNull(output.getListBasePathErrorMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadPort()
   {
      SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
         .withUsername(BaseSFTPTest.USERNAME)
         .withPassword(BaseSFTPTest.PASSWORD)
         .withPort(10 * BaseSFTPTest.getCurrentPort())
         .withHostName(BaseSFTPTest.HOST_NAME);
      SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);
      assertFalse(output.getIsConnectionSuccess());
      assertNotNull(output.getConnectionErrorMessage());
      assertNull(output.getIsListBasePathSuccess());
      assertNull(output.getListBasePathErrorMessage());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testConnectViaPublicKey() throws Exception
   {
      try(InputStream resourceAsStream = getClass().getResourceAsStream("/test-only-key"))
      {
         byte[] privateKeyBytes = AbstractSFTPAction.pemStringToDecodedBytes(StringUtils.join("", IOUtils.readLines(resourceAsStream, StandardCharsets.UTF_8)));

         SFTPTestConnectionAction.SFTPTestConnectionTestInput input = new SFTPTestConnectionAction.SFTPTestConnectionTestInput()
            .withUsername(BaseSFTPTest.USERNAME)
            .withPrivateKey(privateKeyBytes)
            .withPort(BaseSFTPTest.getCurrentPort())
            .withHostName(BaseSFTPTest.HOST_NAME);
         SFTPTestConnectionAction.SFTPTestConnectionTestOutput output = new SFTPTestConnectionAction().testConnection(input);
         assertTrue(output.getIsConnectionSuccess());
         assertNull(output.getConnectionErrorMessage());
      }
   }

}