/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions;


import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class Now extends AbstractFilterExpression<Serializable>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable evaluate(QFieldMetaData field) throws QException
   {
      QFieldType type = field == null ? QFieldType.DATE_TIME : field.getType();

      if(type.equals(QFieldType.DATE_TIME))
      {
         return (Instant.now());
      }
      else if(type.equals(QFieldType.DATE))
      {
         ZoneId zoneId = ValueUtils.getSessionOrInstanceZoneId();
         return (Instant.now().atZone(zoneId).toLocalDate());
      }
      else
      {
         throw (new QException("Unsupported field type [" + type + "]"));
      }
   }

}
