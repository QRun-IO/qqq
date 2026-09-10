/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
