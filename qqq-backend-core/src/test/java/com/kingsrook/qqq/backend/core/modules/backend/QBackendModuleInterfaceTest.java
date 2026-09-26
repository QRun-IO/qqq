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

package com.kingsrook.qqq.backend.core.modules.backend;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for QBackendModuleInterface
 **
 *******************************************************************************/
class QBackendModuleInterfaceTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test()
   {
      TestClass tc = new TestClass();
      Class     c  = tc.getTableBackendDetailsClass();
      assertEquals(c.getName(), QTableBackendDetails.class.getName(), "classname should be QTableBackendDetails");

      try
      {
         tc.getCountInterface();
      }
      catch(IllegalStateException iae)
      {
         try
         {
            tc.getQueryInterface();
         }
         catch(IllegalStateException iae2)
         {
            try
            {
               tc.getInsertInterface();
            }
            catch(IllegalStateException iae3)
            {
               try
               {
                  tc.getUpdateInterface();
               }
               catch(IllegalStateException iae4)
               {
                  try
                  {
                     tc.getDeleteInterface();
                  }
                  catch(IllegalStateException iae5)
                  {
                     return;
                  }
               }
            }
         }
      }

      fail("should not get here...");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private class TestClass implements QBackendModuleInterface
   {
      @Override
      public String getBackendType()
      {
         return null;
      }



      @Override
      public Class<? extends QBackendMetaData> getBackendMetaDataClass()
      {
         return null;
      }
   }

}
