/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.data;


import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.ReflectiveBeanLikeClassUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for entity beans that are interoperable with QRecords.
 *******************************************************************************/
public abstract class QRecordEntity
{
   private static final QLogger LOG = QLogger.getLogger(QRecordEntity.class);

   private static final ListingHash<Class<? extends QRecordEntity>, QRecordEntityField>       fieldMapping        = new ListingHash<>();
   private static final ListingHash<Class<? extends QRecordEntity>, QRecordEntityField>       virtualFieldMapping = new ListingHash<>();
   private static final ListingHash<Class<? extends QRecordEntity>, QRecordEntityAssociation> associationMapping  = new ListingHash<>();

   private Map<String, Serializable> originalRecordValues;

   ////////////////////////////////////////////////////////////////////////////////
   // map of entity class names to QTableMetaData objects that they helped build //
   ////////////////////////////////////////////////////////////////////////////////
   private static Map<String, QTableMetaData> tableReferences = new HashMap<>();



   /*******************************************************************************
    ** Build an entity of this QRecord type from a QRecord
    **
    *******************************************************************************/
   public static <T extends QRecordEntity> T fromQRecord(Class<T> c, QRecord qRecord) throws QException
   {
      return (QRecordEntity.fromQRecord(c, qRecord, ""));
   }



   /*******************************************************************************
    ** Build an entity of this QRecord type from a QRecord - where the fields for
    ** this entity have the given prefix - e.g., if they were selected as part of a join.
    **
    *******************************************************************************/
   public static <T extends QRecordEntity> T fromQRecord(Class<T> c, QRecord qRecord, String fieldNamePrefix) throws QException
   {
      try
      {
         T entity = c.getConstructor().newInstance();
         entity.populateFromQRecord(qRecord, fieldNamePrefix);
         return (entity);
      }
      catch(Exception e)
      {
         throw (new QException("Error building entity from qRecord.", e));
      }
   }



   /***************************************************************************
    ** register a mapping between an entity class and a table that it is associated with.
    ***************************************************************************/
   public static void registerTable(Class<? extends QRecordEntity> entityClass, QTableMetaData table)
   {
      if(entityClass != null && table != null)
      {
         tableReferences.put(entityClass.getName(), table);
      }
   }



   /*******************************************************************************
    ** Build an entity of this QRecord type from a QRecord
    **
    *******************************************************************************/
   protected void populateFromQRecord(QRecord qRecord) throws QRuntimeException
   {
      populateFromQRecord(qRecord, "");
   }



   /*******************************************************************************
    ** Build an entity of this QRecord type from a QRecord - where the fields for
    ** this entity have the given prefix - e.g., if they were selected as part of a join.
    **
    *******************************************************************************/
   protected <T extends QRecordEntity> void populateFromQRecord(QRecord qRecord, String fieldNamePrefix) throws QRuntimeException
   {
      try
      {
         List<QRecordEntityField> fieldList = getFieldList(this.getClass());
         originalRecordValues = new HashMap<>();

         if(fieldNamePrefix == null)
         {
            fieldNamePrefix = "";
         }

         for(QRecordEntityField qRecordEntityField : fieldList)
         {
            Serializable value      = qRecord.getValue(fieldNamePrefix + qRecordEntityField.getFieldName());
            Object       typedValue = qRecordEntityField.convertValueType(value);
            qRecordEntityField.getSetter().invoke(this, typedValue);
            originalRecordValues.put(qRecordEntityField.getFieldName(), value);
         }

         for(QRecordEntityField qRecordEntityField : getVirtualFieldList(this.getClass()))
         {
            Serializable value      = qRecord.getValue(fieldNamePrefix + qRecordEntityField.getFieldName());
            Object       typedValue = qRecordEntityField.convertValueType(value);
            qRecordEntityField.getSetter().invoke(this, typedValue);
            originalRecordValues.put(qRecordEntityField.getFieldName(), value);
         }

         for(QRecordEntityAssociation qRecordEntityAssociation : getAssociationList(this.getClass()))
         {
            List<QRecord> associatedRecords = qRecord.getAssociatedRecords().get(qRecordEntityAssociation.getAssociationAnnotation().name());
            if(associatedRecords == null)
            {
               qRecordEntityAssociation.getSetter().invoke(this, (Object) null);
            }
            else
            {
               List<QRecordEntity> associatedEntityList = new ArrayList<>();
               for(QRecord associatedRecord : CollectionUtils.nonNullList(associatedRecords))
               {
                  associatedEntityList.add(QRecordEntity.fromQRecord(qRecordEntityAssociation.getAssociatedType(), associatedRecord));
               }
               qRecordEntityAssociation.getSetter().invoke(this, associatedEntityList);
            }
         }

         for(QRecordEntityAssociation qRecordEntityAssociation : getAssociationList(this.getClass()))
         {
            List<QRecord> associatedRecords = qRecord.getAssociatedRecords().get(qRecordEntityAssociation.getAssociationAnnotation().name());
            if(associatedRecords == null)
            {
               qRecordEntityAssociation.getSetter().invoke(this, (Object) null);
            }
            else
            {
               List<QRecordEntity> associatedEntityList = new ArrayList<>();
               for(QRecord associatedRecord : CollectionUtils.nonNullList(associatedRecords))
               {
                  associatedEntityList.add(QRecordEntity.fromQRecord(qRecordEntityAssociation.getAssociatedType(), associatedRecord));
               }
               qRecordEntityAssociation.getSetter().invoke(this, associatedEntityList);
            }
         }
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Error building entity from qRecord.", e));
      }
   }



   /*******************************************************************************
    ** Convert this entity to a QRecord.  ALL fields in the entity will be set
    ** in the QRecord.  Note that, if you're using this for an input to the UpdateAction,
    ** that this could cause values to be set to null, e.g., if you constructed
    ** a entity from scratch, and didn't set all values in it!!
    **
    *******************************************************************************/
   public QRecord toQRecord() throws QRuntimeException
   {
      try
      {
         QRecord qRecord = new QRecord();
         qRecord.setTableName(tableName());

         for(QRecordEntityField qRecordEntityField : getFieldList(this.getClass()))
         {
            qRecord.setValue(qRecordEntityField.getFieldName(), (Serializable) qRecordEntityField.getGetter().invoke(this));
         }

         for(QRecordEntityField qRecordEntityField : getVirtualFieldList(this.getClass()))
         {
            qRecord.setValue(qRecordEntityField.getFieldName(), (Serializable) qRecordEntityField.getGetter().invoke(this));
         }

         toQRecordProcessAssociations(qRecord, (entity) -> entity.toQRecord());

         return (qRecord);
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Error building qRecord from entity.", e));
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void toQRecordProcessAssociations(QRecord outputRecord, Function<QRecordEntity, QRecord> toRecordFunction) throws Exception
   {
      for(QRecordEntityAssociation qRecordEntityAssociation : getAssociationList(this.getClass()))
      {
         @SuppressWarnings("unchecked")
         List<? extends QRecordEntity> associatedEntities = (List<? extends QRecordEntity>) qRecordEntityAssociation.getGetter().invoke(this);
         String associationName = qRecordEntityAssociation.getAssociationAnnotation().name();

         if(associatedEntities != null)
         {
            outputRecord.withAssociatedRecords(associationName, new ArrayList<>());
            for(QRecordEntity associatedEntity : associatedEntities)
            {
               outputRecord.withAssociatedRecord(associationName, toRecordFunction.apply(associatedEntity));
            }
         }
      }
   }



   /*******************************************************************************
    ** Useful for the use-case of:
    ** - fetch a QRecord (e.g., QueryAction or GetAction)
    ** - build a QRecordEntity out of it
    ** - change a field (or two) in it
    ** - want to pass it into an UpdateAction, and want to see only the fields that
    **   you know you changed get passed in to UpdateAction (e.g., PATCH semantics).
    **
    ** But also - per the includePrimaryKey param, include the primaryKey in the
    ** records (e.g., to tell the Update which records to update).
    **
    ** Also, useful for:
    ** - construct new entity, calling setters to populate some fields
    ** - pass that entity into
    *******************************************************************************/
   public QRecord toQRecordOnlyChangedFields(boolean includePrimaryKey)
   {
      try
      {
         QRecord qRecord = new QRecord();

         String primaryKeyFieldName = ObjectUtils.tryElse(() -> tableReferences.get(getClass().getName()).getPrimaryKeyField(), null);

         for(QRecordEntityField qRecordEntityField : getFieldList(this.getClass()))
         {
            Serializable thisValue     = (Serializable) qRecordEntityField.getGetter().invoke(this);
            Serializable originalValue = null;
            if(originalRecordValues != null)
            {
               originalValue = originalRecordValues.get(qRecordEntityField.getFieldName());
            }

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if this value and the original value don't match - OR - this is the table's primary key field - then put the value in the record. //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!Objects.equals(thisValue, originalValue) || (includePrimaryKey && Objects.equals(primaryKeyFieldName, qRecordEntityField.getFieldName())))
            {
               qRecord.setValue(qRecordEntityField.getFieldName(), thisValue);
            }
         }

         toQRecordProcessAssociations(qRecord, (entity) -> entity.toQRecordOnlyChangedFields(includePrimaryKey));

         return (qRecord);
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Error building qRecord from entity.", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<QRecordEntityField> getFieldList(Class<? extends QRecordEntity> c)
   {
      if(!fieldMapping.containsKey(c))
      {
         List<QRecordEntityField> fieldList = new ArrayList<>();
         for(Method possibleGetter : c.getMethods())
         {
            if(ReflectiveBeanLikeClassUtils.isGetter(possibleGetter, true))
            {
               Optional<Method> setter = ReflectiveBeanLikeClassUtils.getSetterForGetter(c, possibleGetter);

               if(setter.isPresent())
               {
                  String           fieldName       = ReflectiveBeanLikeClassUtils.getFieldNameFromGetter(possibleGetter);
                  Optional<QField> fieldAnnotation = getQFieldAnnotation(c, fieldName);

                  if(fieldAnnotation.isPresent())
                  {
                     fieldList.add(new QRecordEntityField(fieldName, possibleGetter, setter.get(), possibleGetter.getReturnType(), fieldAnnotation.orElse(null)));
                  }
                  else
                  {
                     Optional<QIgnore>       ignoreAnnotation       = getQIgnoreAnnotation(c, fieldName);
                     Optional<QVirtualField> virtualFieldAnnotation = getQVirtualFieldAnnotation(c, fieldName);
                     Optional<QAssociation>  associationAnnotation  = getQAssociationAnnotation(c, fieldName);

                     if(ignoreAnnotation.isPresent() || virtualFieldAnnotation.isPresent() || associationAnnotation.isPresent())
                     {
                        ////////////////////////////////////////////////////////////
                        // silently skip if marked as an association or an ignore //
                        ////////////////////////////////////////////////////////////
                     }
                     else
                     {
                        LOG.debug("Skipping field without @QField annotation", logPair("class", c.getSimpleName()), logPair("fieldName", fieldName));
                     }
                  }
               }
               else
               {
                  LOG.info("Getter method [" + possibleGetter.getName() + "] does not have a corresponding setter.");
               }
            }
         }
         fieldMapping.put(c, fieldList);
      }
      return (fieldMapping.get(c));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<QRecordEntityField> getVirtualFieldList(Class<? extends QRecordEntity> c)
   {
      if(!virtualFieldMapping.containsKey(c))
      {
         List<QRecordEntityField> fieldList = new ArrayList<>();
         for(Method possibleGetter : c.getMethods())
         {
            if(ReflectiveBeanLikeClassUtils.isGetter(possibleGetter, true))
            {
               Optional<Method> setter = ReflectiveBeanLikeClassUtils.getSetterForGetter(c, possibleGetter);

               if(setter.isPresent())
               {
                  String                  fieldName              = ReflectiveBeanLikeClassUtils.getFieldNameFromGetter(possibleGetter);
                  Optional<QVirtualField> virtualFieldAnnotation = getQVirtualFieldAnnotation(c, fieldName);

                  if(virtualFieldAnnotation.isPresent())
                  {
                     fieldList.add(new QRecordEntityField(fieldName, possibleGetter, setter.get(), possibleGetter.getReturnType(), null));
                  }
               }
               else
               {
                  LOG.info("Getter method [" + possibleGetter.getName() + "] does not have a corresponding setter.");
               }
            }
         }
         virtualFieldMapping.put(c, fieldList);
      }
      return (virtualFieldMapping.get(c));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<QRecordEntityAssociation> getAssociationList(Class<? extends QRecordEntity> c)
   {
      if(!associationMapping.containsKey(c))
      {
         List<QRecordEntityAssociation> associationList = new ArrayList<>();
         for(Method possibleGetter : c.getMethods())
         {
            if(ReflectiveBeanLikeClassUtils.isGetter(possibleGetter, true))
            {
               Optional<Method> setter = ReflectiveBeanLikeClassUtils.getSetterForGetter(c, possibleGetter);

               if(setter.isPresent())
               {
                  String                 fieldName             = ReflectiveBeanLikeClassUtils.getFieldNameFromGetter(possibleGetter);
                  Optional<QAssociation> associationAnnotation = getQAssociationAnnotation(c, fieldName);

                  if(associationAnnotation.isPresent())
                  {
                     @SuppressWarnings("unchecked")
                     Class<? extends QRecordEntity> listTypeParam = (Class<? extends QRecordEntity>) ReflectiveBeanLikeClassUtils.getListTypeParam(possibleGetter.getReturnType(), possibleGetter.getAnnotatedReturnType());
                     associationList.add(new QRecordEntityAssociation(fieldName, possibleGetter, setter.get(), listTypeParam, associationAnnotation.orElse(null)));
                  }
               }
               else
               {
                  LOG.info("Getter method [" + possibleGetter.getName() + "] does not have a corresponding setter.");
               }
            }
         }
         associationMapping.put(c, associationList);
      }
      return (associationMapping.get(c));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<QField> getQFieldAnnotation(Class<? extends QRecordEntity> c, String fieldName)
   {
      return (getAnnotationOnField(c, QField.class, fieldName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<QVirtualField> getQVirtualFieldAnnotation(Class<? extends QRecordEntity> c, String ignoreName)
   {
      return (getAnnotationOnField(c, QVirtualField.class, ignoreName));
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<QIgnore> getQIgnoreAnnotation(Class<? extends QRecordEntity> c, String ignoreName)
   {
      return (getAnnotationOnField(c, QIgnore.class, ignoreName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Optional<QAssociation> getQAssociationAnnotation(Class<? extends QRecordEntity> c, String fieldName)
   {
      return (getAnnotationOnField(c, QAssociation.class, fieldName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <A extends Annotation> Optional<A> getAnnotationOnField(Class<? extends QRecordEntity> c, Class<A> annotationClass, String fieldName)
   {
      try
      {
         Field field = c.getDeclaredField(fieldName);
         return (Optional.ofNullable(field.getAnnotation(annotationClass)));
      }
      catch(NoSuchFieldException e)
      {
         //////////////////////////////////////////
         // ok, we just won't have an annotation //
         //////////////////////////////////////////
      }
      return (Optional.empty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getFieldNameFromGetter(Method getter)
   {
      String nameWithoutGet = getter.getName().replaceFirst("^get", "");
      if(nameWithoutGet.length() == 1)
      {
         return (nameWithoutGet.toLowerCase(Locale.ROOT));
      }
      return (nameWithoutGet.substring(0, 1).toLowerCase(Locale.ROOT) + nameWithoutGet.substring(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isGetter(Method method)
   {
      if(method.getParameterTypes().length == 0 && method.getName().matches("^get[A-Z].*"))
      {
         if(isSupportedFieldType(method.getReturnType()) || isSupportedAssociation(method.getReturnType(), method.getAnnotatedReturnType()))
         {
            return (true);
         }
         else
         {
            if(!method.getName().equals("getClass") && method.getAnnotation(QIgnore.class) == null)
            {
               LOG.debug("Method [" + method.getName() + "] in [" + method.getDeclaringClass().getSimpleName() + "] looks like a getter, but its return type, [" + method.getReturnType().getSimpleName() + "], isn't supported.");
            }
         }
      }
      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Optional<Method> getSetterForGetter(Class<? extends QRecordEntity> c, Method getter)
   {
      String setterName = getter.getName().replaceFirst("^get", "set");
      for(Method method : c.getMethods())
      {
         if(method.getName().equals(setterName))
         {
            if(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(getter.getReturnType()))
            {
               return (Optional.of(method));
            }
            else
            {
               LOG.info("Method [" + method.getName() + "] looks like a setter for [" + getter.getName() + "], but its parameters, [" + Arrays.toString(method.getParameterTypes()) + "], don't match the getter's return type [" + getter.getReturnType() + "]");
            }
         }
      }
      return (Optional.empty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isSupportedFieldType(Class<?> returnType)
   {
      // todo - more types!!
      return (returnType.equals(String.class)
              || returnType.equals(Integer.class)
              || returnType.equals(Long.class)
              || returnType.equals(int.class)
              || returnType.equals(Boolean.class)
              || returnType.equals(boolean.class)
              || returnType.equals(BigDecimal.class)
              || returnType.equals(Instant.class)
              || returnType.equals(LocalDate.class)
              || returnType.equals(LocalTime.class)
              || returnType.equals(byte[].class));
      /////////////////////////////////////////////
      // note - this list has implications upon: //
      // - QFieldType.fromClass                  //
      // - QRecordEntityField.convertValueType   //
      /////////////////////////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean isSupportedAssociation(Class<?> returnType, AnnotatedType annotatedType)
   {
      Class<?> listTypeParam = getListTypeParam(returnType, annotatedType);
      return (listTypeParam != null && QRecordEntity.class.isAssignableFrom(listTypeParam));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Class<?> getListTypeParam(Class<?> listType, AnnotatedType annotatedType)
   {
      if(listType.equals(List.class))
      {
         if(annotatedType instanceof AnnotatedParameterizedType apt)
         {
            AnnotatedType[] annotatedActualTypeArguments = apt.getAnnotatedActualTypeArguments();
            for(AnnotatedType annotatedActualTypeArgument : annotatedActualTypeArguments)
            {
               Type type = annotatedActualTypeArgument.getType();
               if(type instanceof Class<?> c)
               {
                  return (c);
               }
            }
         }
      }

      return (null);
   }


   /***************************************************************************
    **
    ***************************************************************************/
   public static String getTableName(Class<? extends QRecordEntity> entityClass) throws QException
   {
      try
      {
         Field  tableNameField = entityClass.getDeclaredField("TABLE_NAME");
         String tableNameValue = (String) tableNameField.get(null);
         return (tableNameValue);
      }
      catch(Exception e)
      {
         throw (new QException("Could not get TABLE_NAME from entity class: " + entityClass.getSimpleName(), e));
      }
   }


   /***************************************************************************
    ** named without the 'get' to avoid conflict w/ entity fields named that...
    ***************************************************************************/
   public String tableName() throws QException
   {
      return (getTableName(this.getClass()));
   }

}
