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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Constructor for commonly used QProcessCallback's
 *******************************************************************************/
public class QProcessCallbackFactory
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessCallback forFilter(QQueryFilter filter)
   {
      return new QProcessCallback()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QQueryFilter getQueryFilter()
         {
            return (filter);
         }



         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
         {
            return (Collections.emptyMap());
         }
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessCallback forRecordEntity(QRecordEntity entity)
   {
      return forRecord(entity.toQRecord());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessCallback forRecord(QRecord record)
   {
      String primaryKeyField = "id";
      if(StringUtils.hasContent(record.getTableName()))
      {
         primaryKeyField = QContext.getQInstance().getTable(record.getTableName()).getPrimaryKeyField();
      }

      Serializable primaryKeyValue = record.getValue(primaryKeyField);
      if(primaryKeyValue == null)
      {
         throw (new QRuntimeException("Record did not have value in its primary key field [" + primaryKeyField + "]"));
      }

      return (forPrimaryKey(primaryKeyField, primaryKeyValue));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessCallback forPrimaryKey(String fieldName, Serializable value)
   {
      return (forFilter(new QQueryFilter().withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, value))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessCallback forPrimaryKeys(String fieldName, Collection<? extends Serializable> values)
   {
      return (forFilter(new QQueryFilter().withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.IN, values))));
   }

}
