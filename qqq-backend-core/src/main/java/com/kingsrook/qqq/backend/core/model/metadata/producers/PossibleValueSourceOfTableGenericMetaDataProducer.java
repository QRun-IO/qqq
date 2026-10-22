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

package com.kingsrook.qqq.backend.core.model.metadata.producers;


import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;


/***************************************************************************
 ** Generic meta-data-producer, which should be instantiated (e.g., by
 ** MetaDataProducer Helper), to produce a QPossibleValueSource meta-data
 ** based on a QRecordEntity class (which has corresponding QTableMetaData).
 **
 ***************************************************************************/
public class PossibleValueSourceOfTableGenericMetaDataProducer implements MetaDataProducerInterface<QPossibleValueSource>
{
   private final String tableName;

   private Class<?> sourceClass;


   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public PossibleValueSourceOfTableGenericMetaDataProducer(String tableName)
   {
      this.tableName = tableName;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QPossibleValueSource produce(QInstance qInstance)
   {
      return (QPossibleValueSource.newForTable(tableName));
   }



   /*******************************************************************************
    ** Getter for sourceClass
    **
    *******************************************************************************/
   public Class<?> getSourceClass()
   {
      return sourceClass;
   }



   /*******************************************************************************
    ** Setter for sourceClass
    **
    *******************************************************************************/
   public void setSourceClass(Class<?> sourceClass)
   {
      this.sourceClass = sourceClass;
   }


   /*******************************************************************************
    ** Fluent setter for sourceClass
    **
    *******************************************************************************/
   public PossibleValueSourceOfTableGenericMetaDataProducer withSourceClass(Class<?> sourceClass)
   {
      this.sourceClass = sourceClass;
      return (this);
   }

}
