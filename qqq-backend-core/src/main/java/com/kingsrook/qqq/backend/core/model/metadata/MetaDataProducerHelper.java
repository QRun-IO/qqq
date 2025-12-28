/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata;


import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.producers.ChildJoinFromRecordEntityGenericMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.ChildRecordListWidgetFromRecordEntityGenericMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.producers.PossibleValueSourceOfEnumGenericMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.PossibleValueSourceOfTableGenericMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.RecordEntityToTableGenericMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildRecordListWidget;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildTable;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Help work with MetaDataProducers.
 *******************************************************************************/
public class MetaDataProducerHelper
{
   private static final QLogger LOG = QLogger.getLogger(MetaDataProducerHelper.class);

   private static Map<Class<?>, Integer> comparatorValuesByType = new HashMap<>();
   private static Integer                defaultComparatorValue;

   static
   {
      ////////////////////////////////////////////////////////////////////////////////////////
      // define how we break ties in sort-order based on the meta-dta type.  e.g., do apps  //
      // after all other types (as apps often try to get other types from the instance)     //
      // also - do backends earlier than others (e.g., tables may expect backends to exist) //
      // any types not in the map get the default value.                                    //
      ////////////////////////////////////////////////////////////////////////////////////////
      comparatorValuesByType.put(QBackendMetaData.class, 1);

      /////////////////////////////////////
      // unspecified ones will come here //
      /////////////////////////////////////
      defaultComparatorValue = 10;

      comparatorValuesByType.put(QJoinMetaData.class, 21);
      comparatorValuesByType.put(QWidgetMetaData.class, 22);
      comparatorValuesByType.put(QAppMetaData.class, 23);
   }

   ////////////////////////////////////////////////////////////////////////////////////////
   // todo - maybe make this an attribute in the QInstance, or a thread-local - as being //
   // static is an anti-pattern for us (which can lead to unintended behavior, e.g., if  //
   // multiple threads are hot-swapping in parallel, etc.                                //
   ////////////////////////////////////////////////////////////////////////////////////////
   private static MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer = null;



   /*******************************************************************************
    ** Recursively find all classes in the given package, that implement MetaDataProducerInterface
    ** run them, and add their output to the given qInstance - using the provided
    ** tableMetaDataCustomizer to help with all RecordEntity's that
    ** are configured to make tables.
    **
    ** Note - they'll be sorted by the sortOrder they provide.
    *******************************************************************************/
   public static void processAllMetaDataProducersInPackage(QInstance instance, String packageName, MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer) throws QException
   {
      MetaDataProducerHelper.tableMetaDataCustomizer = tableMetaDataCustomizer;
      processAllMetaDataProducersInPackage(instance, packageName);
      MetaDataProducerHelper.tableMetaDataCustomizer = null;
   }


   /***************************************************************************
    **
    ***************************************************************************/
   public static List<MetaDataProducerInterface<?>> findProducers(String packageName) throws QException
   {
      List<Class<?>> classesInPackage;
      try
      {
         ////////////////////////////////////////////////////////////////////////
         // find all the meta data producer classes in (and under) the package //
         ////////////////////////////////////////////////////////////////////////
         classesInPackage = ClassPathUtils.getClassesInPackage(packageName);
      }
      catch(Exception e)
      {
         throw (new QException("Error getting classes in package [" + packageName + "]", e));
      }
      List<MetaDataProducerInterface<?>> producers = new ArrayList<>();

      ////////////////////////////////////////////////////////////////////////////////////////
      // loop over classes, processing them based on either their type or their annotations //
      ////////////////////////////////////////////////////////////////////////////////////////
      for(Class<?> aClass : classesInPackage)
      {
         try
         {
            if(Modifier.isAbstract(aClass.getModifiers()))
            {
               continue;
            }

            /////////////////////////////////////////////////////////////////////
            // handle classes which are themselves MetaDataProducerInterface's //
            /////////////////////////////////////////////////////////////////////
            if(MetaDataProducerInterface.class.isAssignableFrom(aClass))
            {
               CollectionUtils.addIfNotNull(producers, processMetaDataProducer(aClass));
            }

            /////////////////////////////////////////////////////////////////////////
            // handle classes that have the @QMetaDataProducingEntity annotation - //
            // record entities that should produce meta-data                       //
            /////////////////////////////////////////////////////////////////////////
            if(aClass.isAnnotationPresent(QMetaDataProducingEntity.class))
            {
               producers.addAll(processMetaDataProducingEntity(aClass));
            }

            //////////////////////////////////////////////////////////////////
            // handle classes with the @QMetaDataProducingPossibleValueEnum //
            // enums that are PVS's                                         //
            //////////////////////////////////////////////////////////////////
            if(aClass.isAnnotationPresent(QMetaDataProducingPossibleValueEnum.class))
            {
               QMetaDataProducingPossibleValueEnum qMetaDataProducingPossibleValueEnum = aClass.getAnnotation(QMetaDataProducingPossibleValueEnum.class);
               if(qMetaDataProducingPossibleValueEnum.producePossibleValueSource())
               {
                  CollectionUtils.addIfNotNull(producers, processMetaDataProducingPossibleValueEnum(aClass));
               }
            }

         }
         catch(Exception e)
         {
            LOG.warn("Error evaluating a possible meta-data producer class", e, logPair("class", aClass.getSimpleName()));
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////////
      // sort them by sort order, then by the type that they return, as set up in the static map //
      /////////////////////////////////////////////////////////////////////////////////////////////
      sortMetaDataProducers(producers);

      return (producers);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void sortMetaDataProducers(List<MetaDataProducerInterface<?>> producers)
   {
      producers.sort(Comparator
         .comparing((MetaDataProducerInterface<?> p) -> p.getSortOrder())
         .thenComparing((MetaDataProducerInterface<?> p) ->
         {
            try
            {
               Class<?> outputType = p.getClass().getMethod("produce", QInstance.class).getReturnType();
               return comparatorValuesByType.getOrDefault(outputType, defaultComparatorValue);
            }
            catch(Exception e)
            {
               return (0);
            }
         }));
   }



   /*******************************************************************************
    ** Recursively find all classes in the given package, that implement MetaDataProducerInterface
    ** run them, and add their output to the given qInstance.
    **
    ** Note - they'll be sorted by the sortOrder they provide.
    *******************************************************************************/
   public static void processAllMetaDataProducersInPackage(QInstance instance, String packageName) throws QException
   {
      List<MetaDataProducerInterface<?>> producers = findProducers(packageName);

      ///////////////////////////////////////////////////////////////////////////
      // execute each one (if enabled), adding their meta data to the instance //
      ///////////////////////////////////////////////////////////////////////////
      for(MetaDataProducerInterface<?> producer : producers)
      {
         if(producer.isEnabled())
         {
            try
            {
               MetaDataProducerOutput metaData = producer.produce(instance);
               if(metaData != null)
               {
                  metaData.addSelfToInstance(instance);
               }
            }
            catch(Exception e)
            {
               LOG.warn("error executing metaDataProducer", e, logPair("producer", producer.getClass().getSimpleName()));
            }
         }
         else
         {
            LOG.debug("Not using producer which is not enabled", logPair("producer", producer.getClass().getSimpleName()));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @SuppressWarnings("unchecked")
   private static <T extends Serializable & PossibleValueEnum<T>> MetaDataProducerInterface<?> processMetaDataProducingPossibleValueEnum(Class<?> sourceClass)
   {
      String warningPrefix = "Found a class annotated as @" + QMetaDataProducingPossibleValueEnum.class.getSimpleName();
      if(!PossibleValueEnum.class.isAssignableFrom(sourceClass))
      {
         LOG.warn(warningPrefix + ", but which is not a " + PossibleValueEnum.class.getSimpleName() + ", so it will not be used.", logPair("class", sourceClass.getSimpleName()));
         return null;
      }

      PossibleValueEnum<?>[] values = (PossibleValueEnum<?>[]) sourceClass.getEnumConstants();
      PossibleValueSourceOfEnumGenericMetaDataProducer<T> producer = new PossibleValueSourceOfEnumGenericMetaDataProducer<>(sourceClass.getSimpleName(), (PossibleValueEnum<T>[]) values);
      producer.setSourceClass(sourceClass);
      return producer;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static List<MetaDataProducerInterface<?>> processMetaDataProducingEntity(Class<?> sourceClass) throws Exception
   {
      List<MetaDataProducerInterface<?>> rs = new ArrayList<>();

      QMetaDataProducingEntity qMetaDataProducingEntity = sourceClass.getAnnotation(QMetaDataProducingEntity.class);
      String                   warningPrefix            = "Found a class annotated as @" + QMetaDataProducingEntity.class.getSimpleName();

      ///////////////////////////////////////////////////////////
      // make sures class is QRecordEntity and cast it as such //
      ///////////////////////////////////////////////////////////
      if(!QRecordEntity.class.isAssignableFrom(sourceClass))
      {
         LOG.warn(warningPrefix + ", but which is not a " + QRecordEntity.class.getSimpleName() + ", so it will not be used.", logPair("class", sourceClass.getSimpleName()));
         return (rs);
      }

      @SuppressWarnings("unchecked") // safe per the check above.
      Class<? extends QRecordEntity> recordEntityClass = (Class<? extends QRecordEntity>) sourceClass;

      ////////////////////////////////////////////////
      // get TABLE_NAME static field from the class //
      ////////////////////////////////////////////////
      Field tableNameField = recordEntityClass.getDeclaredField("TABLE_NAME");
      if(!tableNameField.getType().equals(String.class))
      {
         LOG.warn(warningPrefix + ", but whose TABLE_NAME field is not a String, so it will not be used.", logPair("class", recordEntityClass.getSimpleName()));
         return (rs);
      }

      String tableNameValue = (String) tableNameField.get(null);

      //////////////////////////////////////////
      // add table producer, if so configured //
      //////////////////////////////////////////
      if(qMetaDataProducingEntity.produceTableMetaData())
      {
         try
         {
            Class<? extends MetaDataCustomizerInterface<?>>              genericMetaProductionCustomizer   = (Class<? extends MetaDataCustomizerInterface<?>>) qMetaDataProducingEntity.tableMetaDataCustomizer();
            Class<? extends MetaDataCustomizerInterface<QTableMetaData>> tableMetaDataProductionCustomizer = null;
            if(!genericMetaProductionCustomizer.equals(MetaDataCustomizerInterface.NoopMetaDataCustomizer.class))
            {
               tableMetaDataProductionCustomizer = (Class<? extends MetaDataCustomizerInterface<QTableMetaData>>) genericMetaProductionCustomizer;
            }

            RecordEntityToTableGenericMetaDataProducer producer = new RecordEntityToTableGenericMetaDataProducer(tableNameValue, recordEntityClass, tableMetaDataProductionCustomizer);
            producer.setSourceClass(recordEntityClass);

            if(tableMetaDataCustomizer != null)
            {
               producer.addRecordEntityTableMetaDataProductionCustomizer(tableMetaDataCustomizer);
            }

            rs.add(producer);
         }
         catch(Exception e)
         {
            throw new QException("Error processing table meta data producer for entity class: " + recordEntityClass.getName(), e);
         }
      }

      ////////////////////////////////////////
      // add PVS producer, if so configured //
      ////////////////////////////////////////
      if(qMetaDataProducingEntity.producePossibleValueSource())
      {
         PossibleValueSourceOfTableGenericMetaDataProducer producer = new PossibleValueSourceOfTableGenericMetaDataProducer(tableNameValue);
         producer.setSourceClass(recordEntityClass);
         rs.add(producer);
      }

      //////////////////////////
      // process child tables //
      //////////////////////////
      for(ChildTable childTable : qMetaDataProducingEntity.childTables())
      {
         Class<? extends QRecordEntity> childEntityClass = childTable.childTableEntityClass();
         if(childTable.childJoin().enabled())
         {
            CollectionUtils.addIfNotNull(rs, processChildJoin(recordEntityClass, childTable));

            if(childTable.childRecordListWidget().enabled())
            {
               CollectionUtils.addIfNotNull(rs, processChildRecordListWidget(recordEntityClass, childTable));
            }
         }
         else
         {
            if(childTable.childRecordListWidget().enabled())
            {
               //////////////////////////////////////////////////////////////////////////
               // if not doing the join, can't do the child-widget, so warn about that //
               //////////////////////////////////////////////////////////////////////////
               LOG.warn(warningPrefix + " requested to produce a ChildRecordListWidget, but not produce a Join - which is not allowed (must do join to do widget). ", logPair("class", recordEntityClass.getSimpleName()), logPair("childEntityClass", childEntityClass.getSimpleName()));
            }
         }
      }

      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static MetaDataProducerInterface<?> processChildRecordListWidget(Class<? extends QRecordEntity> sourceClass, ChildTable childTable) throws Exception
   {
      Class<? extends QRecordEntity> childEntityClass = childTable.childTableEntityClass();
      String                         parentTableName  = getTableNameStaticFieldValue(sourceClass);
      String                         childTableName   = getTableNameStaticFieldValue(childEntityClass);

      ChildRecordListWidget childRecordListWidget = childTable.childRecordListWidget();
      ChildRecordListWidgetFromRecordEntityGenericMetaDataProducer producer = new ChildRecordListWidgetFromRecordEntityGenericMetaDataProducer(childTableName, parentTableName, childRecordListWidget);
      producer.setSourceClass(sourceClass);
      return producer;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String findPossibleValueField(Class<? extends QRecordEntity> entityClass, String possibleValueSourceName)
   {
      for(Field field : entityClass.getDeclaredFields())
      {
         if(field.isAnnotationPresent(QField.class))
         {
            QField qField = field.getAnnotation(QField.class);
            if(qField.possibleValueSourceName().equals(possibleValueSourceName))
            {
               return field.getName();
            }
         }
      }

      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static MetaDataProducerInterface<?> processChildJoin(Class<? extends QRecordEntity> entityClass, ChildTable childTable) throws Exception
   {
      Class<? extends QRecordEntity> childEntityClass = childTable.childTableEntityClass();

      String parentTableName        = getTableNameStaticFieldValue(entityClass);
      String childTableName         = getTableNameStaticFieldValue(childEntityClass);
      String possibleValueFieldName = findPossibleValueField(childEntityClass, parentTableName);
      if(!StringUtils.hasContent(possibleValueFieldName))
      {
         LOG.warn("Could not find field in [" + childEntityClass.getSimpleName() + "] with possibleValueSource referencing table [" + entityClass.getSimpleName() + "]");
         return (null);
      }

      ChildJoinFromRecordEntityGenericMetaDataProducer producer = new ChildJoinFromRecordEntityGenericMetaDataProducer(childTableName, parentTableName, possibleValueFieldName, childTable.childJoin().orderBy(), childTable.childJoin().isOneToOne());
      producer.setSourceClass(entityClass);
      return producer;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static MetaDataProducerInterface<?> processMetaDataProducer(Class<?> sourceCClass) throws Exception
   {
      for(Constructor<?> constructor : sourceCClass.getConstructors())
      {
         if(constructor.getParameterCount() == 0)
         {
            Object o = constructor.newInstance();
            MetaDataProducerInterface<?> producer = (MetaDataProducerInterface<?>) o;
            producer.setSourceClass(sourceCClass);
            return producer;
         }
      }

      LOG.warn("Found a class which implements MetaDataProducerInterface, but it does not have a no-arg constructor, so it cannot be used.", logPair("class", sourceCClass.getSimpleName()));
      return null;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String getTableNameStaticFieldValue(Class<?> aClass) throws NoSuchFieldException, IllegalAccessException
   {
      Field tableNameField = aClass.getDeclaredField("TABLE_NAME");
      if(!tableNameField.getType().equals(String.class))
      {
         return (null);
      }

      String tableNameValue = (String) tableNameField.get(null);
      return (tableNameValue);
   }



   /*******************************************************************************
    ** Getter for tableMetaDataCustomizer
    *******************************************************************************/
   public MetaDataCustomizerInterface<QTableMetaData> getTableMetaDataCustomizer()
   {
      return (MetaDataProducerHelper.tableMetaDataCustomizer);
   }



   /*******************************************************************************
    ** Setter for tableMetaDataCustomizer
    *******************************************************************************/
   public void setTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      MetaDataProducerHelper.tableMetaDataCustomizer = tableMetaDataCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for tableMetaDataCustomizer
    *******************************************************************************/
   public void withTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      MetaDataProducerHelper.tableMetaDataCustomizer = tableMetaDataCustomizer;
   }

}
