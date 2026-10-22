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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** Version of AbstractQQQApplication that assumes all meta-data is produced
 ** by MetaDataProducers in (or under) a single package.
 **
 ** Set failOnMetaDataProducerError to make the instance it defines fail-fast
 ** (see QInstance.withFailOnMetaDataProducerError).
 *******************************************************************************/
public abstract class AbstractMetaDataProducerBasedQQQApplication extends AbstractQQQApplication
{
   private Boolean failOnMetaDataProducerError = false;



   /***************************************************************************
    **
    ***************************************************************************/
   public abstract String getMetaDataPackageName();



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QInstance defineQInstance() throws QException
   {
      QInstance qInstance = new QInstance().withFailOnMetaDataProducerError(failOnMetaDataProducerError);
      MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, getMetaDataPackageName());
      return (qInstance);
   }



   /*******************************************************************************
    ** Getter for failOnMetaDataProducerError
    *******************************************************************************/
   public Boolean getFailOnMetaDataProducerError()
   {
      return (this.failOnMetaDataProducerError);
   }



   /*******************************************************************************
    ** Setter for failOnMetaDataProducerError
    *******************************************************************************/
   public void setFailOnMetaDataProducerError(Boolean failOnMetaDataProducerError)
   {
      this.failOnMetaDataProducerError = failOnMetaDataProducerError;
   }



   /*******************************************************************************
    ** Fluent setter for failOnMetaDataProducerError - if true, a producer that
    ** can't be used or fails stops defineQInstance, instead of being logged as a
    ** warning and skipped.
    *******************************************************************************/
   public AbstractMetaDataProducerBasedQQQApplication withFailOnMetaDataProducerError(Boolean failOnMetaDataProducerError)
   {
      this.failOnMetaDataProducerError = failOnMetaDataProducerError;
      return (this);
   }

}
