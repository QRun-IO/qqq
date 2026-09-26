/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Possible data types for Q-fields.
 **
 *******************************************************************************/
public enum QFieldType
{
   STRING,
   INTEGER,
   LONG,
   DECIMAL,
   BOOLEAN,
   DATE,
   TIME,
   DATE_TIME,
   TEXT,
   HTML,
   PASSWORD,
   BLOB;
   ///////////////////////////////////////////////////////////////////////
   // keep these values in sync with QFieldType.ts in qqq-frontend-core //
   ///////////////////////////////////////////////////////////////////////

   public static final Set<QFieldType> STRING_LIKE_TYPES =  Set.of(STRING, TEXT, HTML, PASSWORD);
   public static final Set<QFieldType> NUMERIC_TYPES =  Set.of(INTEGER, LONG, DECIMAL);
   public static final Set<QFieldType> INTEGRAL_TYPES =  Set.of(INTEGER, LONG);
   public static final Set<QFieldType> TEMPORAL_TYPES =  Set.of(DATE, DATE_TIME, TIME);


   /*******************************************************************************
    ** Get a field type enum constant for a java class.
    *******************************************************************************/
   public static QFieldType fromClass(Class<?> c) throws QException
   {
      if(c.equals(String.class))
      {
         return (STRING);
      }
      if(c.equals(Integer.class) || c.equals(int.class))
      {
         return (INTEGER);
      }
      if(c.equals(Long.class) || c.equals(long.class))
      {
         return (LONG);
      }
      if(c.equals(BigDecimal.class))
      {
         return (DECIMAL);
      }
      if(c.equals(Instant.class))
      {
         return (DATE_TIME);
      }
      if(c.equals(LocalDate.class))
      {
         return (DATE);
      }
      if(c.equals(LocalTime.class))
      {
         return (TIME);
      }
      if(c.equals(Boolean.class))
      {
         return (BOOLEAN);
      }
      if(c.equals(byte[].class))
      {
         return (BLOB);
      }

      throw (new QException("Unrecognized class [" + c + "]"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public String getMixedCaseLabel()
   {
      return StringUtils.allCapsToMixedCase(name());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean isStringLike()
   {
      return STRING_LIKE_TYPES.contains(this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean isNumeric()
   {
      return NUMERIC_TYPES.contains(this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean isIntegral()
   {
      return INTEGRAL_TYPES.contains(this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean isTemporal()
   {
      return TEMPORAL_TYPES.contains(this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean needsMasked()
   {
      return this == QFieldType.PASSWORD;
   }
}
