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
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Reflective information about a field in a QRecordEntity
 *******************************************************************************/
public class QRecordEntityField
{
   private final String   fieldName;
   private final Method   getter;
   private final Method   setter;
   private final Class<?> type;
   private final QField   fieldAnnotation;



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public QRecordEntityField(String fieldName, Method getter, Method setter, Class<?> type, QField fieldAnnotation)
   {
      this.fieldName = fieldName;
      this.getter = getter;
      this.setter = setter;
      this.type = type;
      this.fieldAnnotation = fieldAnnotation;
   }



   /*******************************************************************************
    ** Getter for fieldName
    **
    *******************************************************************************/
   public String getFieldName()
   {
      return fieldName;
   }



   /*******************************************************************************
    ** Getter for getter
    **
    *******************************************************************************/
   public Method getGetter()
   {
      return getter;
   }



   /*******************************************************************************
    ** Getter for setter
    **
    *******************************************************************************/
   public Method getSetter()
   {
      return setter;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public Class<?> getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Getter for fieldAnnotation
    **
    *******************************************************************************/
   public QField getFieldAnnotation()
   {
      return fieldAnnotation;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Object convertValueType(Serializable value)
   {
      try
      {
         if(value == null)
         {
            return (null);
         }

         if(value.getClass().equals(type))
         {
            return (value);
         }

         if(type.equals(String.class))
         {
            return (ValueUtils.getValueAsString(value));
         }

         if(type.equals(Integer.class) || type.equals(int.class))
         {
            return (ValueUtils.getValueAsInteger(value));
         }

         if(type.equals(Long.class) || type.equals(long.class))
         {
            return (ValueUtils.getValueAsLong(value));
         }

         if(type.equals(Boolean.class) || type.equals(boolean.class))
         {
            return (ValueUtils.getValueAsBoolean(value));
         }

         if(type.equals(BigDecimal.class))
         {
            return (ValueUtils.getValueAsBigDecimal(value));
         }

         if(type.equals(LocalDate.class))
         {
            return (ValueUtils.getValueAsLocalDate(value));
         }

         if(type.equals(Instant.class))
         {
            return (ValueUtils.getValueAsInstant(value));
         }

         if(type.equals(LocalTime.class))
         {
            return (ValueUtils.getValueAsLocalTime(value));
         }

         if(type.equals(byte[].class))
         {
            return (ValueUtils.getValueAsByteArray(value));
         }

         if(type.equals(Map.class))
         {
            return (ValueUtils.getValueAsMap(value));
         }
      }
      catch(Exception e)
      {
         throw (new QValueException("Exception converting value [" + value + "] for field [" + fieldName + "]", e));
      }

      throw (new QValueException("Unhandled value type [" + type + "] for field [" + fieldName + "]"));
   }
}
