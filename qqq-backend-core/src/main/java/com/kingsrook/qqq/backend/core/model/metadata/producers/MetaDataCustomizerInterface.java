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

package com.kingsrook.qqq.backend.core.model.metadata.producers;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;


/*******************************************************************************
 ** Interface to be implemented by classes that are designed to help customize
 ** meta-data objects as they're being produced, e.g., such as a table produced
 ** via the QMetaDataProducingEntity, or maybe tables loaded by a qbit??
 *******************************************************************************/
public interface MetaDataCustomizerInterface<T extends TopLevelMetaDataInterface>
{
   /***************************************************************************
    **
    ***************************************************************************/
   T customizeMetaData(QInstance qInstance, T metaData) throws QException;


   /***************************************************************************
    ** noop version of this interface - used as default value in annotation
    **
    ***************************************************************************/
   class NoopMetaDataCustomizer<T extends TopLevelMetaDataInterface> implements MetaDataCustomizerInterface<T>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public T customizeMetaData(QInstance qInstance, T metaData) throws QException
      {
         return (metaData);
      }
   }
}
