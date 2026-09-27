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

package com.kingsrook.qqq.backend.core.model.metadata;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Interface for classes that know how to produce meta data objects.  Useful with
 ** MetaDataProducerHelper, to point at a package full of these, and populate
 ** your whole QInstance.
 **
 ** See also MetaDataProducer - an implementer of this interface, which actually
 ** came first, and is fine to extend if producing a meta-data class is all your
 ** class means to do (nice and "Single-responsibility principle").
 **
 ** But, in some applications you may want to, for example, have one class that
 ** defines a process step, and also produces the meta-data for that process, so
 ** your whole process can just be one class - so then just have your step class
 ** implement this interface.  or, same idea for a QRecordEntity that provides
 ** its own TableMetaData.
 *******************************************************************************/
public interface MetaDataProducerInterface<T extends MetaDataProducerOutput>
{
   int DEFAULT_SORT_ORDER = 500;


   /*******************************************************************************
    ** Produce the metaData object.  Generally, you don't want to add it to the instance
    ** yourself - but the instance is there in case you need it to get other metaData.
    *******************************************************************************/
   T produce(QInstance qInstance) throws QException;


   /*******************************************************************************
    ** In case this producer needs to run before (or after) others, this method
    ** can control influence that (e.g., if used by MetaDataProducerHelper).
    **
    ** Smaller values run first.
    *******************************************************************************/
   default int getSortOrder()
   {
      return (DEFAULT_SORT_ORDER);
   }


   /*******************************************************************************
    ** turn this producer on or off - e.g., maybe based on an env value.
    **
    *******************************************************************************/
   default boolean isEnabled()
   {
      return (true);
   }


   /***************************************************************************
    *
    ***************************************************************************/
   default void setSourceClass(Class<?> sourceClass)
   {
      //////////
      // noop //
      //////////
   }


   /***************************************************************************
    **
    ***************************************************************************/
   default Class<?> getSourceClass()
   {
      return null;
   }
}
