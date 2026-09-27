/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


/*******************************************************************************
 ** Unit test for SampleCli
 *******************************************************************************/
class SampleCliTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clearContext()
   {
      QContext.clear();
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExitSuccess() throws QException
   {
      QInstance qInstance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(qInstance, new QSession());
      int exitCode = new SampleCli().run(qInstance, new String[] { "--meta-data" });
      assertEquals(0, exitCode);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotExitSuccess() throws QException
   {
      QInstance qInstance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(qInstance, new QSession());
      int exitCode = new SampleCli().run(qInstance, new String[] { "asdfasdf" });
      assertNotEquals(0, exitCode);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHelpWithExplicitMockAuthentication()
   {
      String originalProperty = System.getProperty("qqq.sample.mockAuthentication");
      try
      {
         System.setProperty("qqq.sample.mockAuthentication", "true");
         QContext.clear();
         assertEquals(0, new SampleCli().run(new String[] { "--help" }));
      }
      finally
      {
         QContext.clear();
         if(originalProperty == null)
         {
            System.clearProperty("qqq.sample.mockAuthentication");
         }
         else
         {
            System.setProperty("qqq.sample.mockAuthentication", originalProperty);
         }
      }
   }



}
