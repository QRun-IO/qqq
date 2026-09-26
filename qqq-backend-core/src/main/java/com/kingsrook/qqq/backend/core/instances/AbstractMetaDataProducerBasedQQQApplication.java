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
