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

package com.kingsrook.qqq.backend.core.model.metadata.qbits.testqbit;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProducer;


/*******************************************************************************
 **
 *******************************************************************************/
public class TestQBitProducer implements QBitProducer
{
   private TestQBitConfig testQBitConfig;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void produce(QInstance qInstance, String namespace) throws QException
   {
      QBitMetaData qBitMetaData = new QBitMetaData()
         .withGroupId("test.com.kingsrook.qbits")
         .withArtifactId("testQBit")
         .withVersion("0.1.0")
         .withNamespace(namespace)
         .withConfig(testQBitConfig);
      qInstance.addQBit(qBitMetaData);

      List<MetaDataProducerInterface<?>> producers = MetaDataProducerHelper.findProducers(getClass().getPackageName() + ".metadata");
      finishProducing(qInstance, qBitMetaData, testQBitConfig, producers);
   }



   /*******************************************************************************
    ** Getter for testQBitConfig
    *******************************************************************************/
   public TestQBitConfig getTestQBitConfig()
   {
      return (this.testQBitConfig);
   }



   /*******************************************************************************
    ** Setter for testQBitConfig
    *******************************************************************************/
   public void setTestQBitConfig(TestQBitConfig testQBitConfig)
   {
      this.testQBitConfig = testQBitConfig;
   }



   /*******************************************************************************
    ** Fluent setter for testQBitConfig
    *******************************************************************************/
   public TestQBitProducer withTestQBitConfig(TestQBitConfig testQBitConfig)
   {
      this.testQBitConfig = testQBitConfig;
      return (this);
   }

}
