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
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Builder pattern for creating a QProcessCallback
 *******************************************************************************/
public class QProcessCallbackBuilder
{
   private QQueryFilter                                filter;
   private BiConsumer<RunBackendStepInput, QueryInput> queryInputCustomizer;
   private Map<String, Serializable>                   fieldValues;



   /*******************************************************************************
    * Construct the callback object from the builder.
    *******************************************************************************/
   public QProcessCallback build()
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
            return (fieldValues);
         }



         /***************************************************************************
          *
          ***************************************************************************/
         @Override
         public void customizeInputPreQuery(RunBackendStepInput runBackendStepInput, QueryInput queryInput)
         {
            if(queryInputCustomizer != null)
            {
               queryInputCustomizer.accept(runBackendStepInput, queryInput);
            }
         }
      };
   }



   /*******************************************************************************
    * build a callback whose filter will return a single record
    * @param entity the record you want returned
    *******************************************************************************/
   public QProcessCallbackBuilder withRecordEntity(QRecordEntity entity)
   {
      return withRecord(entity.toQRecord());
   }



   /*******************************************************************************
    * build a callback whose filter will return a single record
    * @param record the record you want returned
    *******************************************************************************/
   public QProcessCallbackBuilder withRecord(QRecord record)
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

      return (withPrimaryKey(primaryKeyField, primaryKeyValue));
   }



   /*******************************************************************************
    * build a callback whose filter will execute fieldName = value query
    * @param fieldName to be queried for
    * @param value to be queried for
    *******************************************************************************/
   public QProcessCallbackBuilder withPrimaryKey(String fieldName, Serializable value)
   {
      this.filter = new QQueryFilter().withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, value));
      return this;
   }



   /*******************************************************************************
    * build a callback whose filter will execute fieldName IN values query
    * @param fieldName to be queried for
    * @param values to be queried for (IN)
    *******************************************************************************/
   public QProcessCallbackBuilder withPrimaryKeys(String fieldName, Collection<? extends Serializable> values)
   {
      this.filter = new QQueryFilter().withCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.IN, values));
      return (this);
   }



   /*******************************************************************************
    * Getter for queryInputCustomizer
    * @see #withQueryInputCustomizer(BiConsumer)
    *******************************************************************************/
   public BiConsumer<RunBackendStepInput, QueryInput> getQueryInputCustomizer()
   {
      return (this.queryInputCustomizer);
   }



   /*******************************************************************************
    * Setter for queryInputCustomizer
    * @see #withQueryInputCustomizer(BiConsumer)
    *******************************************************************************/
   public void setQueryInputCustomizer(BiConsumer<RunBackendStepInput, QueryInput> queryInputCustomizer)
   {
      this.queryInputCustomizer = queryInputCustomizer;
   }



   /*******************************************************************************
    * Fluent setter for queryInputCustomizer
    *
    * @param queryInputCustomizer
    * lambda that will be placed in the Callback object and used in its
    * customizeInputPreQuery method.
    * @return this
    *******************************************************************************/
   public QProcessCallbackBuilder withQueryInputCustomizer(BiConsumer<RunBackendStepInput, QueryInput> queryInputCustomizer)
   {
      this.queryInputCustomizer = queryInputCustomizer;
      return (this);
   }



   /*******************************************************************************
    * Setter for fieldValues
    * @see #withFieldValues(Map)
    *******************************************************************************/
   public void setFieldValues(Map<String, Serializable> fieldValues)
   {
      this.fieldValues = fieldValues;
   }



   /*******************************************************************************
    * Fluent setter for fieldValues
    *
    * @param fieldValues will be returned in the callback
    * @return this
    *******************************************************************************/
   public QProcessCallbackBuilder withFieldValues(Map<String, Serializable> fieldValues)
   {
      this.fieldValues = fieldValues;
      return (this);
   }



   /*******************************************************************************
    * Getter for filter
    * @see #withFilter(QQueryFilter)
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (this.filter);
   }



   /*******************************************************************************
    * Setter for filter
    * @see #withFilter(QQueryFilter)
    *******************************************************************************/
   public void setFilter(QQueryFilter filter)
   {
      this.filter = filter;
   }



   /*******************************************************************************
    * Fluent setter for filter
    *
    * @param filter to be returned by the callback
    * @return this
    *******************************************************************************/
   public QProcessCallbackBuilder withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }

}
