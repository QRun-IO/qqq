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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/***************************************************************************
 ** Generic meta-data-producer, which should be instantiated (e.g., by
 ** MetaDataProducerHelper), to produce a QPossibleValueSource meta-data
 ** based on a QRecordEntity class (which has corresponding QTableMetaData).
 **
 ***************************************************************************/
public class RecordEntityToTableGenericMetaDataProducer implements MetaDataProducerInterface<QTableMetaData>
{
   private final String                         tableName;
   private final Class<? extends QRecordEntity> entityClass;

   private final List<MetaDataCustomizerInterface<QTableMetaData>> metaDataCustomizers = new ArrayList<>();

   private static MetaDataCustomizerInterface<QTableMetaData> defaultMetaDataCustomizer = null;

   private Class<?> sourceClass;


   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RecordEntityToTableGenericMetaDataProducer(String tableName, Class<? extends QRecordEntity> entityClass, Class<? extends MetaDataCustomizerInterface<QTableMetaData>> metaDataProductionCustomizerClass) throws QException
   {
      this.tableName = tableName;
      this.entityClass = entityClass;

      if(metaDataProductionCustomizerClass != null)
      {
         metaDataCustomizers.add(getMetaDataProductionCustomizer(metaDataProductionCustomizerClass));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QTableMetaData produce(QInstance qInstance) throws QException
   {
      QTableMetaData qTableMetaData = new QTableMetaData();
      qTableMetaData.setName(tableName);
      qTableMetaData.setRecordLabelFormat("%s");
      qTableMetaData.withFieldsFromEntity(entityClass);

      ////////////////////////////////////////////////////////////////////
      // use the productionCustomizers to fill in more of the meta data //
      ////////////////////////////////////////////////////////////////////
      for(MetaDataCustomizerInterface<QTableMetaData> metaDataMetaDataCustomizer : metaDataCustomizers)
      {
         qTableMetaData = metaDataMetaDataCustomizer.customizeMetaData(qInstance, qTableMetaData);
      }

      ///////////////////////////////////////////////////////////////////////////////////
      // now if there's a default customizer, call it too - for generic, common things //
      // you might want on all of your tables, or defaults if not set otherwise        //
      ///////////////////////////////////////////////////////////////////////////////////
      if(defaultMetaDataCustomizer != null && qTableMetaData != null)
      {
         qTableMetaData = defaultMetaDataCustomizer.customizeMetaData(qInstance, qTableMetaData);
      }

      /////////////////////////////////////////////////////////////////////////
      // use primary key as record label field, if it hasn't been set so far //
      // todo - does this belong in the enricher??                           //
      /////////////////////////////////////////////////////////////////////////
      if(qTableMetaData != null && CollectionUtils.nullSafeIsEmpty(qTableMetaData.getRecordLabelFields()) && StringUtils.hasContent(qTableMetaData.getPrimaryKeyField()))
      {
         qTableMetaData.setRecordLabelFields(List.of(qTableMetaData.getPrimaryKeyField()));
      }

      return qTableMetaData;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private MetaDataCustomizerInterface<QTableMetaData> getMetaDataProductionCustomizer(Class<? extends MetaDataCustomizerInterface<QTableMetaData>> metaDataCustomizerClass) throws QException
   {
      try
      {
         return metaDataCustomizerClass.getConstructor().newInstance();
      }
      catch(Exception e)
      {
         throw (new QException("Error constructing table metadata production customizer class [" + metaDataCustomizerClass + "]: ", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void addRecordEntityTableMetaDataProductionCustomizer(MetaDataCustomizerInterface<QTableMetaData> metaDataMetaDataCustomizer)
   {
      metaDataCustomizers.add(metaDataMetaDataCustomizer);
   }



   /*******************************************************************************
    ** Getter for defaultMetaDataCustomizer
    *******************************************************************************/
   public static MetaDataCustomizerInterface<QTableMetaData> getDefaultMetaDataCustomizer()
   {
      return (RecordEntityToTableGenericMetaDataProducer.defaultMetaDataCustomizer);
   }



   /*******************************************************************************
    ** Setter for defaultMetaDataCustomizer
    *******************************************************************************/
   public static void setDefaultMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> defaultMetaDataCustomizer)
   {
      RecordEntityToTableGenericMetaDataProducer.defaultMetaDataCustomizer = defaultMetaDataCustomizer;
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
   public RecordEntityToTableGenericMetaDataProducer withSourceClass(Class<?> sourceClass)
   {
      this.sourceClass = sourceClass;
      return (this);
   }

}
