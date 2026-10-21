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

package com.kingsrook.qqq.backend.core.instances;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.produce.TestThrowsInProduceMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producererrors.produce.TestWorkingMetaDataProducer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Unit test for AbstractMetaDataProducerBasedQQQApplication 
 *******************************************************************************/
class AbstractMetaDataProducerBasedQQQApplicationTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = new TestApplication().defineQInstance();
      assertEquals(1, qInstance.getTables().size());
      assertEquals("fromProducer", qInstance.getTables().get("fromProducer").getName());
      assertEquals(1, qInstance.getProcesses().size());
      assertEquals("fromProducer", qInstance.getProcesses().get("fromProducer").getName());
   }



   /*******************************************************************************
    ** By default, the instance an application defines logs and skips a producer
    ** that fails.
    *******************************************************************************/
   @Test
   void testFailOnMetaDataProducerErrorOffByDefault() throws QException
   {
      MetaDataProducerBasedQQQApplication application = new MetaDataProducerBasedQQQApplication(TestThrowsInProduceMetaDataProducer.class);
      assertFalse(application.getFailOnMetaDataProducerError());

      QInstance qInstance = application.defineQInstance();
      assertFalse(qInstance.getFailOnMetaDataProducerError());
      assertThat(qInstance.getTables()).containsOnlyKeys(TestWorkingMetaDataProducer.NAME);
   }



   /*******************************************************************************
    ** With the flag on, the application's instance is fail-fast: a producer that
    ** fails stops defineQInstance.
    *******************************************************************************/
   @Test
   void testFailOnMetaDataProducerErrorOn()
   {
      AbstractMetaDataProducerBasedQQQApplication application = new MetaDataProducerBasedQQQApplication(TestThrowsInProduceMetaDataProducer.class)
         .withFailOnMetaDataProducerError(true);

      assertThatThrownBy(application::defineQInstance)
         .isInstanceOf(QException.class)
         .hasMessageContaining(TestThrowsInProduceMetaDataProducer.class.getName())
         .hasRootCauseMessage(TestThrowsInProduceMetaDataProducer.MESSAGE);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class TestApplication extends AbstractMetaDataProducerBasedQQQApplication
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getMetaDataPackageName()
      {
         return getClass().getPackage().getName() + ".producers";
      }
   }
}